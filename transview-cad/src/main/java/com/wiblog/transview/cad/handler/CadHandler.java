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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CAD 预览处理器。
 * <p>
 * 缓存模式下的流程：
 * 1. 检查缓存 — 命中完整结果直接返回
 * 2. 缓存有缩略图 — 返回缩略图，后台异步生成完整结果
 * 3. 无缓存 — 快速生成缩略图并返回，后台异步生成完整结果
 * <p>
 * 完整结果直接写磁盘缓存文件，不经过堆内存。
 * 注意：removeWatermark=true 时，PDF/SVG 会先写入 ByteArrayOutputStream 再处理水印，
 * 此路径会占用较多堆内存。大 DWG 建议 removeWatermark=false。
 * <p>
 * 防护机制：
 * - 同 cacheKey 在飞行中时不会重复提交后台任务（in-flight 去重）
 * - 转换失败的 cacheKey 有 5 分钟冷却期，避免坏文件反复打爆 Aspose
 * - 所有异步任务有 watchdog 超时监控
 */
public class CadHandler extends TransViewHandler {

    private static volatile CadConversionExecutor conversionExecutor;

    /** 正在执行的异步转换任务（cacheKey -> Future），防止同 key 重复提交 */
    private static final ConcurrentHashMap<String, Future<?>> RUNNING_TASKS = new ConcurrentHashMap<>();

    /** 失败任务冷却记录（cacheKey -> 失败时间戳），5 分钟内不再重试 */
    private static final ConcurrentHashMap<String, Long> FAILED_TASKS = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 5 * 60 * 1000;

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

    /**
     * 重建 CAD 执行器（配置变更后调用）
     */
    public static void initCadExecutor() {
        synchronized (CadHandler.class) {
            CadConversionExecutor old = conversionExecutor;
            conversionExecutor = new CadConversionExecutor(
                    TransViewProperties.CadExecutor.getCorePoolSize(),
                    TransViewProperties.CadExecutor.getMaxPoolSize(),
                    TransViewProperties.CadExecutor.getQueueCapacity(),
                    TransViewProperties.CadExecutor.getMinFreeMemoryMB(),
                    TransViewProperties.CadExecutor.getTaskTimeoutMs()
            );
            if (old != null) {
                old.shutdown();
            }
        }
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
        // 缩略图和完整转换各自独立加载 CadImage（Aspose CadImage 非线程安全，不能跨线程共享）
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

    @Override
    public void convertHandler(ExtensionEnum sourceExtensionEnum, ExtensionEnum targetExtensionEnum,
                               InputStream inputStream, OutputStream outputStream) throws Exception {
        if (sourceExtensionEnum == null) {
            throw new IllegalArgumentException("源格式不能为空");
        }
        StrategyTypeEnum sourceType = StrategyTypeEnum.getStrategy(sourceExtensionEnum.getValue());
        if (sourceType == null || !strategyTypeEnums().contains(sourceType)) {
            throw new IllegalArgumentException("不支持的 CAD 源格式: " + sourceExtensionEnum);
        }

        CadImage cadImage = null;
        try {
            cadImage = (CadImage) Image.load(inputStream);
            CadRasterizationOptions rasterOptions = buildRasterOptions(TransViewProperties.View.Cad.getLayout());

            if (targetExtensionEnum == ExtensionEnum.PDF) {
                convertToPdf(outputStream, rasterOptions, cadImage);
            } else if (targetExtensionEnum == ExtensionEnum.SVG) {
                convertToSvg(outputStream, rasterOptions, cadImage);
            } else {
                throw new IllegalArgumentException("CAD 仅支持转换为 PDF 或 SVG: " + targetExtensionEnum);
            }
        } finally {
            if (cadImage != null) {
                cadImage.close();
            }
        }
    }

    @Override
    public List<StrategyTypeEnum> strategyTypeEnums() {
        return StrategyTypeEnum.CAD_TYPES;
    }

    // ---- 内部方法 ----

    /**
     * 异步生成完整结果（先占位再提交，确保 in-flight 原子去重）
     */
    private void kickOffAsync(File file, String cacheKey, String layout, DiskCacheManager cache) {
        if (isCooledDown(cacheKey) || RUNNING_TASKS.containsKey(cacheKey)) {
            return;
        }

        // 先用 CompletableFuture 占位，保证原子性
        CompletableFuture<Void> placeholder = new CompletableFuture<>();
        if (RUNNING_TASKS.putIfAbsent(cacheKey, placeholder) != null) {
            return;
        }

        Path tmpPath = null;
        try {
            tmpPath = cache.prepareDirect(cacheKey);
            Path finalTmpPath = tmpPath;
            Runnable onDone = () -> RUNNING_TASKS.remove(cacheKey);
            Future<?> realFuture = getConversionExecutor().submitAsync(() -> {
                try {
                    convertToFile(file, finalTmpPath, layout);
                    String ext = TransViewProperties.View.Cad.getConvertType() == CadConvertType.PDF ? "pdf" : "svg";
                    cache.commitDirect(cacheKey, file, finalTmpPath, ext);
                    FAILED_TASKS.remove(cacheKey);
                } catch (Exception e) {
                    FAILED_TASKS.put(cacheKey, System.currentTimeMillis());
                    deleteQuietly(finalTmpPath);
                    throw e;
                }
                return null;
            }, tmpPath, cacheKey, onDone);
            RUNNING_TASKS.replace(cacheKey, placeholder, realFuture);
        } catch (RejectedExecutionException e) {
            RUNNING_TASKS.remove(cacheKey, placeholder);
            deleteQuietly(tmpPath);
        } catch (RuntimeException e) {
            RUNNING_TASKS.remove(cacheKey, placeholder);
            deleteQuietly(tmpPath);
            throw e;
        }
    }

    private void kickOffExtraLayoutsAsync(File file, DiskCacheManager cache) {
        String[] extraLayouts = TransViewProperties.View.Cad.getExtraLayouts();
        if (extraLayouts == null || extraLayouts.length == 0) {
            return;
        }
        for (String layout : extraLayouts) {
            String key = CacheKeyUtil.generateCadCacheKey(file, layout);
            if (cache.get(key) != null || RUNNING_TASKS.containsKey(key) || isCooledDown(key)) {
                continue;
            }

            CompletableFuture<Void> placeholder = new CompletableFuture<>();
            if (RUNNING_TASKS.putIfAbsent(key, placeholder) != null) {
                continue;
            }

            Path tmpPath = null;
            try {
                tmpPath = cache.prepareDirect(key);
                Path finalTmpPath = tmpPath;
                Runnable onDone = () -> RUNNING_TASKS.remove(key);
                Future<?> realFuture = getConversionExecutor().submitAsync(() -> {
                    try {
                        convertToFile(file, finalTmpPath, layout);
                        String ext = TransViewProperties.View.Cad.getConvertType() == CadConvertType.PDF ? "pdf" : "svg";
                        cache.commitDirect(key, file, finalTmpPath, ext);
                        byte[] thumb = generateThumbnail(file, layout);
                        if (thumb != null) {
                            cache.putThumbnail(key, thumb);
                        }
                        FAILED_TASKS.remove(key);
                    } catch (Exception e) {
                        FAILED_TASKS.put(key, System.currentTimeMillis());
                        deleteQuietly(finalTmpPath);
                        throw e;
                    }
                    return null;
                }, tmpPath, key, onDone);
                RUNNING_TASKS.replace(key, placeholder, realFuture);
            } catch (RejectedExecutionException e) {
                RUNNING_TASKS.remove(key, placeholder);
                deleteQuietly(tmpPath);
            } catch (RuntimeException e) {
                RUNNING_TASKS.remove(key, placeholder);
                deleteQuietly(tmpPath);
            }
        }
    }

    /**
     * 检查失败冷却（顺便清理过期记录）
     */
    private static boolean isCooledDown(String cacheKey) {
        Long failedAt = FAILED_TASKS.get(cacheKey);
        if (failedAt == null) {
            return false;
        }
        if (System.currentTimeMillis() - failedAt < COOLDOWN_MS) {
            return true;
        }
        FAILED_TASKS.remove(cacheKey);
        return false;
    }

    /**
     * 同步降级路径（缩略图生成失败时）— 走受控执行器
     */
    private void convertAndCacheSync(File file, String cacheKey, String layout,
                                     DiskCacheManager cache, OutputStream outputStream) {
        String ext = TransViewProperties.View.Cad.getConvertType() == CadConvertType.PDF ? "pdf" : "svg";
        try {
            Path tmpPath = cache.prepareDirect(cacheKey);
            try {
                getConversionExecutor().submitAndWait(() -> {
                    convertToFile(file, tmpPath, layout);
                    return null;
                }, tmpPath);
            } catch (TimeoutException e) {
                deleteQuietly(tmpPath);
                throw new RuntimeException("CAD 转换超时", e);
            } catch (ExecutionException e) {
                deleteQuietly(tmpPath);
                Throwable cause = e.getCause();
                throw new RuntimeException("预览 CAD 文件失败", cause instanceof Exception ? (Exception) cause : e);
            }
            cache.commitDirect(cacheKey, file, tmpPath, ext);
            File cached = cache.get(cacheKey);
            if (cached != null) {
                streamFile(cached, outputStream);
            }
        } catch (RuntimeException e) {
            throw e;
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

    /**
     * 从文件独立加载 CadImage（供缩略图渲染使用）
     */
    private static CadImage loadCadImage(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return (CadImage) Image.load(in);
        }
    }

    /**
     * 从已加载的 CadImage 渲染缩略图
     */
    private byte[] renderThumbnail(CadImage cadImage, String layout) {
        try {
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
        }
    }

    private static void closeQuietly(CadImage cadImage) {
        if (cadImage != null) {
            try { cadImage.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * 保留的旧方法（供 viewHandler/convertHandler 等独立路径使用）
     */
    private byte[] generateThumbnail(File file, String layout) {
        CadImage cadImage = null;
        try {
            cadImage = loadCadImage(file);
            return renderThumbnail(cadImage, layout);
        } catch (Exception e) {
            return null;
        } finally {
            closeQuietly(cadImage);
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

    private static void deleteQuietly(Path path) {
        if (path != null) {
            try { Files.deleteIfExists(path); } catch (IOException ignored) {}
        }
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
            outputStream.write(transformedXml.getBytes(StandardCharsets.UTF_8));
        } else {
            cadImage.save(outputStream, svgOptions);
        }
    }
}
