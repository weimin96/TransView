package com.wiblog.transview.cad.handler;

import com.aspose.cad.Color;
import com.aspose.cad.Image;
import com.aspose.cad.fileformats.cad.CadDrawTypeMode;
import com.aspose.cad.fileformats.cad.CadImage;
import com.aspose.cad.imageoptions.CadRasterizationOptions;
import com.aspose.cad.imageoptions.PngOptions;
import com.aspose.cad.imageoptions.PdfOptions;
import com.aspose.cad.imageoptions.SvgOptions;
import com.wiblog.transview.cad.utils.PdfUtil;
import com.wiblog.transview.core.bean.TransViewProperties;
import com.wiblog.transview.core.cache.CadConversionExecutor;
import com.wiblog.transview.core.cache.CacheKeyUtil;
import com.wiblog.transview.core.cache.DiskCacheManager;
import com.wiblog.transview.core.common.CadConvertType;
import com.wiblog.transview.core.common.ExtensionEnum;
import com.wiblog.transview.core.common.StrategyTypeEnum;
import com.wiblog.transview.core.handler.TransViewHandler;
import com.wiblog.transview.core.utils.SVGUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

/**
 * CAD 预览处理器。
 * <p>
 * 缓存模式下的流程：
 * 1. 检查缓存 — 命中完整结果直接返回
 * 2. 缓存有缩略图 — 返回缩略图，后台异步生成完整结果
 * 3. 无缓存 — 快速生成缩略图并返回，后台异步生成完整结果
 * <p>
 * 完整结果直接写磁盘缓存文件，不经过堆内存。
 * 首次访问后，后台自动预生成 extraLayouts 中其他布局的缓存。
 */
public class CadHandler extends TransViewHandler {

    private static volatile CadConversionExecutor conversionExecutor;

    private static CadConversionExecutor getConversionExecutor() {
        if (conversionExecutor == null) {
            synchronized (CadHandler.class) {
                if (conversionExecutor == null) {
                    conversionExecutor = new CadConversionExecutor(
                            TransViewProperties.CadExecutor.getCorePoolSize(),
                            TransViewProperties.CadExecutor.getMaxPoolSize(),
                            TransViewProperties.CadExecutor.getQueueCapacity(),
                            TransViewProperties.CadExecutor.getMinFreeMemoryMB(),
                            TransViewProperties.CadExecutor.getTaskTimeoutMs()
                    );
                }
            }
        }
        return conversionExecutor;
    }

    private static final int THUMBNAIL_WIDTH = 800;
    private static final int THUMBNAIL_HEIGHT = 600;

    @Override
    public void preview(File file, OutputStream outputStream) {
        DiskCacheManager cache = DiskCacheManager.getInstance();
        if (!cache.isReady()) {
            super.preview(file, outputStream);
            return;
        }

        String layout = TransViewProperties.View.Cad.getLayout();
        String cacheKey = CacheKeyUtil.generateCadCacheKey(file, layout);

        // 1. 命中完整结果
        File cached = cache.get(cacheKey);
        if (cached != null && cached.getName().startsWith("result.")) {
            kickOffExtraLayoutsAsync(file, cache);
            streamFile(cached, outputStream);
            return;
        }

        // 2. 命中缩略图 — 返回缩略图，后台生成完整结果
        File thumb = cache.getThumbnail(cacheKey);
        if (thumb != null) {
            kickOffAsync(file, cacheKey, layout, cache);
            kickOffExtraLayoutsAsync(file, cache);
            streamFile(thumb, outputStream);
            return;
        }

        // 3. 无缓存 — 生成缩略图 + 后台完整转换
        byte[] thumbnailData = generateThumbnail(file, layout);
        if (thumbnailData != null) {
            cache.putThumbnail(cacheKey, thumbnailData);
            kickOffAsync(file, cacheKey, layout, cache);
            kickOffExtraLayoutsAsync(file, cache);
            try {
                outputStream.write(thumbnailData);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            convertAndCacheSync(file, cacheKey, layout, cache, outputStream);
        }
    }

    @Override
    public void viewHandler(InputStream inputStream, OutputStream outputStream, String extension) throws Exception {
        CadImage cadImage = null;
        try {
            cadImage = (CadImage) Image.load(inputStream);
            CadRasterizationOptions rasterOptions = buildRasterOptions(TransViewProperties.View.Cad.getLayout());
            if (TransViewProperties.View.Cad.getConvertType() == CadConvertType.PDF) {
                convertToPdf(outputStream, rasterOptions, cadImage);
            } else {
                convertToSvg(outputStream, rasterOptions, cadImage);
            }
        } finally {
            if (cadImage != null) {
                cadImage.close();
            }
        }
    }

    // ---- 内部方法 ----

    private void kickOffAsync(File file, String cacheKey, String layout, DiskCacheManager cache) {
        try {
            Path tmpPath = cache.prepareDirect(cacheKey);
            getConversionExecutor().submit(() -> {
                convertToFile(file, tmpPath, layout);
                String ext = TransViewProperties.View.Cad.getConvertType() == CadConvertType.PDF ? "pdf" : "svg";
                cache.commitDirect(cacheKey, file, tmpPath, ext);
                return null;
            }, tmpPath);
        } catch (RejectedExecutionException ignored) {
            // 内存不足或队列满，跳过异步预生成
        }
    }

    /**
     * 后台预生成 extraLayouts 中其他布局的缓存
     */
    private void kickOffExtraLayoutsAsync(File file, DiskCacheManager cache) {
        String[] extraLayouts = TransViewProperties.View.Cad.getExtraLayouts();
        if (extraLayouts == null || extraLayouts.length == 0) {
            return;
        }
        for (String layout : extraLayouts) {
            String key = CacheKeyUtil.generateCadCacheKey(file, layout);
            if (cache.get(key) != null) {
                continue;
            }
            try {
                Path tmpPath = cache.prepareDirect(key);
                getConversionExecutor().submit(() -> {
                    convertToFile(file, tmpPath, layout);
                    String ext = TransViewProperties.View.Cad.getConvertType() == CadConvertType.PDF ? "pdf" : "svg";
                    cache.commitDirect(key, file, tmpPath, ext);
                    byte[] thumb = generateThumbnail(file, layout);
                    if (thumb != null) {
                        cache.putThumbnail(key, thumb);
                    }
                    return null;
                }, tmpPath);
            } catch (RejectedExecutionException ignored) {
            }
        }
    }

    private void convertAndCacheSync(File file, String cacheKey, String layout,
                                     DiskCacheManager cache, OutputStream outputStream) {
        String ext = TransViewProperties.View.Cad.getConvertType() == CadConvertType.PDF ? "pdf" : "svg";
        try {
            Path tmpPath = cache.prepareDirect(cacheKey);
            convertToFile(file, tmpPath, layout);
            cache.commitDirect(cacheKey, file, tmpPath, ext);
            File cached = cache.get(cacheKey);
            if (cached != null) {
                streamFile(cached, outputStream);
            }
        } catch (Exception e) {
            throw new RuntimeException("预览 CAD 文件失败", e);
        }
    }

    private void convertToFile(File sourceFile, Path targetPath, String layout) throws Exception {
        CadImage cadImage = null;
        try {
            try (InputStream in = new FileInputStream(sourceFile)) {
                cadImage = (CadImage) Image.load(in);
            }
            CadRasterizationOptions rasterOptions = buildRasterOptions(layout);
            try (OutputStream out = Files.newOutputStream(targetPath)) {
                if (TransViewProperties.View.Cad.getConvertType() == CadConvertType.PDF) {
                    convertToPdf(out, rasterOptions, cadImage);
                } else {
                    convertToSvg(out, rasterOptions, cadImage);
                }
            }
        } finally {
            if (cadImage != null) {
                cadImage.close();
            }
        }
    }

    private byte[] generateThumbnail(File file, String layout) {
        CadImage cadImage = null;
        try {
            try (InputStream in = new FileInputStream(file)) {
                cadImage = (CadImage) Image.load(in);
            }
            CadRasterizationOptions opts = new CadRasterizationOptions();
            opts.setPageWidth(THUMBNAIL_WIDTH);
            opts.setPageHeight(THUMBNAIL_HEIGHT);
            opts.setDrawType(CadDrawTypeMode.UseObjectColor);
            opts.setBackgroundColor(Color.getWhite());
            opts.setLayouts(new String[]{layout});
            if (TransViewProperties.View.Cad.getShxFontsFolder() != null) {
                opts.setShxFonts(TransViewProperties.View.Cad.getShxFontsFolder());
            }
            PngOptions pngOptions = new PngOptions();
            pngOptions.setVectorRasterizationOptions(opts);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            cadImage.save(buffer, pngOptions);
            return buffer.toByteArray();
        } catch (Exception e) {
            return null;
        } finally {
            if (cadImage != null) {
                cadImage.close();
            }
        }
    }

    private CadRasterizationOptions buildRasterOptions(String layout) {
        CadRasterizationOptions opts = new CadRasterizationOptions();
        opts.setPageWidth(TransViewProperties.View.Cad.getPageWidth());
        opts.setPageHeight(TransViewProperties.View.Cad.getPageHeight());
        opts.setDrawType(CadDrawTypeMode.UseObjectColor);
        opts.setBackgroundColor(Color.getWhite());
        opts.setLayouts(new String[]{layout});
        if (TransViewProperties.View.Cad.getShxFontsFolder() != null) {
            opts.setShxFonts(TransViewProperties.View.Cad.getShxFontsFolder());
        }
        return opts;
    }

    private void streamFile(File file, OutputStream outputStream) {
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                outputStream.write(buf, 0, n);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void convertHandler(ExtensionEnum sourceExtensionEnum, ExtensionEnum targetExtensionEnum, InputStream inputStream, OutputStream outputStream) throws Exception {
    }

    @Override
    public List<StrategyTypeEnum> strategyTypeEnums() {
        return StrategyTypeEnum.CAD_TYPES;
    }

    public static void convertToPdf(OutputStream outputStream, CadRasterizationOptions rasterOptions, CadImage cadImage) throws Exception {
        PdfOptions pdfOptions = new PdfOptions();
        pdfOptions.setVectorRasterizationOptions(rasterOptions);

        if (TransViewProperties.View.isRemoveWatermark()) {
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            cadImage.save(byteOutputStream, pdfOptions);
            ByteArrayInputStream pdfInputStream = new ByteArrayInputStream(byteOutputStream.toByteArray());
            PdfUtil.removeWatermark(pdfInputStream, outputStream);
        } else {
            cadImage.save(outputStream, pdfOptions);
        }
    }

    public static void convertToSvg(OutputStream outputStream, CadRasterizationOptions rasterOptions, CadImage cadImage) throws IOException {
        SvgOptions svgOptions = new SvgOptions();
        svgOptions.setVectorRasterizationOptions(rasterOptions);

        if (TransViewProperties.View.isRemoveWatermark()) {
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            cadImage.save(byteOutputStream, svgOptions);
            ByteArrayInputStream svgInputStream = new ByteArrayInputStream(byteOutputStream.toByteArray());
            String transformedXml = SVGUtil.removeWatermark(svgInputStream, SVGUtil.CUT_TYPE_CAD);
            outputStream.write(transformedXml.getBytes());
        } else {
            cadImage.save(outputStream, svgOptions);
        }
    }
}
