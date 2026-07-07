package com.wiblog.transview.cad.handler;

import com.aspose.cad.Color;
import com.aspose.cad.Image;
import com.aspose.cad.fileformats.cad.CadDrawTypeMode;
import com.aspose.cad.fileformats.cad.CadImage;
import com.aspose.cad.imageoptions.CadRasterizationOptions;
import com.aspose.cad.imageoptions.PngOptions;
import com.aspose.cad.imageoptions.PdfOptions;
import com.aspose.cad.imageoptions.SvgOptions;
import com.aspose.cad.watermarkguard.IWatermarkGuardService;
import com.wiblog.transview.cad.utils.PdfUtil;
import com.wiblog.transview.core.bean.TransViewProperties;
import com.wiblog.transview.core.cache.CadConversionExecutor;
import com.wiblog.transview.core.cache.CacheKeyUtil;
import com.wiblog.transview.core.cache.DiskCacheManager;
import com.wiblog.transview.core.common.CadConvertType;
import com.wiblog.transview.core.common.ExtensionEnum;
import com.wiblog.transview.core.common.StrategyTypeEnum;
import com.wiblog.transview.core.exception.PreviewBusyException;
import com.wiblog.transview.core.exception.PreviewTimeoutException;
import com.wiblog.transview.core.handler.TransViewHandler;
import com.wiblog.transview.core.utils.LicenseUtil;
import com.wiblog.transview.core.utils.Util;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CAD 预览处理器。
 * <p>
 * 缓存模式下的流程：
 * 1. 检查缓存 — 命中完整结果直接返回
 * 2. 缓存有缩略图但无完整结果 — 同步生成并返回完整结果
 * 3. 无缓存 — 同步生成并返回完整结果
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

    private static final Object LICENSE_LOCK = new Object();

    private static volatile boolean licenseLoaded;

    private static volatile String loadedLicensePath;

    private static volatile CadConversionExecutor conversionExecutor;

    /** 正在执行的异步转换任务（cacheKey -> Future），防止同 key 重复提交 */
    private static final ConcurrentHashMap<String, Future<?>> RUNNING_TASKS = new ConcurrentHashMap<>();

    /** 失败任务冷却记录（cacheKey -> 失败时间戳），5 分钟内不再重试 */
    private static final ConcurrentHashMap<String, Long> FAILED_TASKS = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 5 * 60 * 1000;

    /** InputStream 路径产生的源临时文件（sourceAbsPath -> Path），供 onDone 回调清理 */
    private static final ConcurrentHashMap<String, Path> SOURCE_TEMP_FILES = new ConcurrentHashMap<>();
    /** 源临时文件引用计数（sourceAbsPath -> 同时使用该源文件的异步任务数），归零时删除 */
    private static final ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> SOURCE_TEMP_REFS = new ConcurrentHashMap<>();

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
        String layout = TransViewProperties.View.Cad.getLayout();
        String cacheKey = CacheKeyUtil.generateCadCacheKey(file, layout);
        DiskCacheManager cache = DiskCacheManager.getInstance();
        if (!cache.isReady()) {
            setOutputContentType(StrategyTypeEnum.getMediaType("dwg"));
            previewViaCadExecutor(file, layout, outputStream);
            return;
        }

        // 1. 命中完整结果
        File cached = cache.get(cacheKey);
        if (cached != null && cached.getName().startsWith("result.")) {
            setOutputContentType(StrategyTypeEnum.getMediaType("dwg"));
            kickOffExtraLayoutsAsync(file, cache);
            streamFile(cached, outputStream);
            return;
        }

        // 2. 命中缩略图但未命中完整结果时，仍按配置同步返回完整结果
        File thumb = cache.getThumbnail(cacheKey);
        if (thumb != null) {
            setOutputContentType(StrategyTypeEnum.getMediaType("dwg"));
            convertAndCacheSync(file, cacheKey, layout, cache, outputStream);
            return;
        }

        // 3. 无缓存 — 按配置同步转换并返回完整结果
        setOutputContentType(StrategyTypeEnum.getMediaType("dwg"));
        convertAndCacheSync(file, cacheKey, layout, cache, outputStream);
        kickOffExtraLayoutsAsync(file, cache);
    }

    /**
     * 缓存不可用时的降级路径 — 仍然使用 CAD 独立执行器，避免占用通用预览线程
     */
    private void previewViaCadExecutor(File file, String layout, OutputStream outputStream) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile("transview-cad-", "." + getTargetExtension());
            Path finalTmp = tmp;
            getConversionExecutor().submitAndWait(() -> {
                convertToFile(file, finalTmp, layout);
                return null;
            }, finalTmp);
            streamFile(finalTmp.toFile(), outputStream);
        } catch (RejectedExecutionException e) {
            throw new PreviewBusyException("CAD 转换服务繁忙");
        } catch (TimeoutException e) {
            throw new PreviewTimeoutException("CAD 转换超时");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new RuntimeException("预览 CAD 文件失败", cause instanceof Exception ? (Exception) cause : e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("CAD 转换被中断", e);
        } catch (IOException e) {
            throw new RuntimeException("创建 CAD 临时文件失败", e);
        } finally {
            deleteQuietly(tmp);
        }
    }

    @Override
    public void preview(InputStream inputStream, String filenameOrExtension, OutputStream outputStream) {
        check(filenameOrExtension);
        String extension = Util.getExtensionOrFilename(filenameOrExtension);
        DiskCacheManager cache = DiskCacheManager.getInstance();

        if (!cache.isReady()) {
            previewStreamDirect(inputStream, extension, outputStream);
            return;
        }

        // 落盘为临时文件 → 复用 preview(File) 的完整缓存逻辑
        Path sourceTmp = null;
        try {
            sourceTmp = Files.createTempFile("transview-src-", "." + extension);
            try (OutputStream out = Files.newOutputStream(sourceTmp)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = inputStream.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
            }

            String layout = TransViewProperties.View.Cad.getLayout();
            String cacheKey = CacheKeyUtil.generateCadCacheKey(sourceTmp.toFile(), layout);
            registerSourceTemp(sourceTmp.toFile(), sourceTmp);

            // 直接委托给 File 路径，所有缓存逻辑复用
            this.preview(sourceTmp.toFile(), outputStream);

            // 若未启动异步任务（缓存命中或同步降级），立即清理源临时文件；
            // 若启动了异步任务，onDone 回调负责清理
            if (!RUNNING_TASKS.containsKey(cacheKey)) {
                releaseSourceTemp(sourceTmp.toFile());
            }
        } catch (IOException e) {
            deleteQuietly(sourceTmp);
            throw new RuntimeException("预览 CAD 文件失败", e);
        }
    }

    /** InputStream 缓存不可用时的降级路径 */
    private void previewStreamDirect(InputStream inputStream, String extension, OutputStream outputStream) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile("transview-cad-stream-", "." + extension);
            Path finalTmp = tmp;
            getConversionExecutor().submitAndWait(() -> {
                try (OutputStream out = Files.newOutputStream(finalTmp)) {
                    viewHandler(inputStream, out, extension);
                }
                return null;
            }, finalTmp);
            streamFile(finalTmp.toFile(), outputStream);
        } catch (RejectedExecutionException e) {
            throw new PreviewBusyException("CAD 转换服务繁忙");
        } catch (TimeoutException e) {
            throw new PreviewTimeoutException("CAD 转换超时");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("预览 CAD 文件失败", cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("CAD 转换被中断", e);
        } catch (IOException e) {
            throw new RuntimeException("预览 CAD 文件失败", e);
        } finally {
            deleteQuietly(tmp);
        }
    }

    /** 注册 InputStream 源临时文件（引用计数 +1），供异步任务完成时清理 */
    private static void registerSourceTemp(File sourceFile, Path path) {
        String key = sourceFile.getAbsolutePath();
        SOURCE_TEMP_FILES.put(key, path);
        SOURCE_TEMP_REFS.computeIfAbsent(key, k -> new java.util.concurrent.atomic.AtomicInteger()).incrementAndGet();
    }

    /** 释放引用计数，归零时删除源临时文件 */
    private static void releaseSourceTemp(File sourceFile) {
        String key = sourceFile.getAbsolutePath();
        java.util.concurrent.atomic.AtomicInteger ref = SOURCE_TEMP_REFS.get(key);
        if (ref != null && ref.decrementAndGet() == 0) {
            SOURCE_TEMP_REFS.remove(key);
            Path path = SOURCE_TEMP_FILES.remove(key);
            deleteQuietly(path);
        }
    }

    private String getTargetExtension() {
        return TransViewProperties.View.Cad.getConvertType() == CadConvertType.PDF ? "pdf" : "svg";
    }

    @Override
    public void viewHandler(InputStream inputStream, OutputStream outputStream, String extension) throws Exception {
        CadImage cadImage = null;
        try {
            loadLicense();
            cadImage = (CadImage) Image.load(inputStream);
            convertCadImage(cadImage, TransViewProperties.View.Cad.getLayout(), outputStream);
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
            loadLicense();
            cadImage = (CadImage) Image.load(inputStream);

            if (targetExtensionEnum == ExtensionEnum.PDF) {
                convertCadImage(cadImage, TransViewProperties.View.Cad.getLayout(), ExtensionEnum.PDF, outputStream);
            } else if (targetExtensionEnum == ExtensionEnum.SVG) {
                convertCadImage(cadImage, TransViewProperties.View.Cad.getLayout(), ExtensionEnum.SVG, outputStream);
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
            Runnable onDone = () -> {
                RUNNING_TASKS.remove(cacheKey);
                releaseSourceTemp(file);
            };
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
                Runnable onDone = () -> {
                    RUNNING_TASKS.remove(key);
                    releaseSourceTemp(file);
                };
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
            loadLicense();
            try (InputStream in = new FileInputStream(sourceFile)) {
                cadImage = (CadImage) Image.load(in);
            }
            try (OutputStream out = Files.newOutputStream(targetPath)) {
                convertCadImage(cadImage, layout, out);
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
        loadLicense();
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
            applyLayout(opts, cadImage, layout);
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
        applyLayout(opts, layout);
        if (TransViewProperties.View.Cad.getShxFontsFolder() != null) {
            opts.setShxFonts(TransViewProperties.View.Cad.getShxFontsFolder());
        }
        return opts;
    }

    private void convertCadImage(CadImage cadImage, String layout, OutputStream outputStream) throws Exception {
        ExtensionEnum target = TransViewProperties.View.Cad.getConvertType() == CadConvertType.PDF ? ExtensionEnum.PDF : ExtensionEnum.SVG;
        convertCadImage(cadImage, layout, target, outputStream);
    }

    private void convertCadImage(CadImage cadImage, String layout, ExtensionEnum target, OutputStream outputStream) throws Exception {
        convertCadImage(cadImage, buildRasterOptions(cadImage, layout), target, outputStream);
    }

    private void convertCadImage(CadImage cadImage, CadRasterizationOptions rasterOptions, ExtensionEnum target, OutputStream outputStream) throws Exception {
        if (target == ExtensionEnum.PDF) {
            convertToPdf(outputStream, rasterOptions, cadImage);
        } else {
            convertToSvg(outputStream, rasterOptions, cadImage);
        }
    }

    private CadRasterizationOptions buildRasterOptions(CadImage cadImage, String layout) {
        CadRasterizationOptions opts = buildRasterOptions(null);
        applyLayout(opts, cadImage, layout);
        return opts;
    }

    private static void applyLayout(CadRasterizationOptions opts, CadImage cadImage, String layout) {
        if (!Util.isBlank(layout) && hasLayout(cadImage, layout)) {
            opts.setLayouts(new String[]{layout});
        }
    }

    private static void applyLayout(CadRasterizationOptions opts, String layout) {
        if (!Util.isBlank(layout)) {
            opts.setLayouts(new String[]{layout});
        }
    }

    private static boolean hasLayout(CadImage cadImage, String layout) {
        if (cadImage == null || Util.isBlank(layout)) {
            return false;
        }
        Map<String, ?> layouts = cadImage.getLayouts();
        if (layouts == null || layouts.isEmpty()) {
            return false;
        }
        return layouts.keySet().stream().anyMatch(name -> layout.equalsIgnoreCase(name));
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
        loadLicense();
        PdfOptions pdfOptions = new PdfOptions();
        pdfOptions.setVectorRasterizationOptions(rasterOptions);
        if (TransViewProperties.View.isRemoveWatermark()) {
//            ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
//            cadImage.save(pdfOutputStream, pdfOptions);
//            PdfUtil.removeAsposeWatermark(new ByteArrayInputStream(pdfOutputStream.toByteArray()), outputStream);
//            return;
            disableWatermarkGuard(cadImage);
        }
        cadImage.save(outputStream, pdfOptions);
    }

    public static void convertToSvg(OutputStream outputStream, CadRasterizationOptions rasterOptions, CadImage cadImage) throws IOException {
        loadLicense();
        SvgOptions svgOptions = new SvgOptions();
        svgOptions.setVectorRasterizationOptions(rasterOptions);
        if (TransViewProperties.View.isRemoveWatermark()) {
            disableWatermarkGuard(cadImage);
        }
        cadImage.save(outputStream, svgOptions);
    }

    private static void loadLicense() {
        String licensePath = LicenseUtil.resolvePath(TransViewProperties.View.Cad.getLicensePath());
        if (licensePath == null) {
            if (TransViewProperties.View.isRemoveWatermark()) {
                removeWatermark();
            }
            return;
        }
        if (licenseLoaded && licensePath.equals(loadedLicensePath)) {
            return;
        }
        synchronized (LICENSE_LOCK) {
            if (licenseLoaded && licensePath.equals(loadedLicensePath)) {
                return;
            }
            InputStream licenseStream;
            try {
                licenseStream = LicenseUtil.openStream(licensePath);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("Aspose.CAD license 加载失败: " + licensePath, e);
            }
            try (InputStream inputStream = licenseStream) {
                new com.aspose.cad.License().setLicense(inputStream);
                loadedLicensePath = licensePath;
                licenseLoaded = true;
            } catch (Exception e) {
                throw new IllegalStateException("Aspose.CAD license 加载失败: " + licensePath, e);
            }
        }
    }

    private static volatile boolean watermarkRemoved;

    private static void removeWatermark() {
        if (watermarkRemoved) {
            return;
        }
        synchronized (LICENSE_LOCK) {
            if (watermarkRemoved) {
                return;
            }
            try {
                Class<?> czClass = Class.forName("com.aspose.cad.internal.pN.cz");
                Field licensedField = czClass.getDeclaredField("b");
                licensedField.setAccessible(true);
                Object licensedState = licensedField.get(null);

                Class<?> cAClass = Class.forName("com.aspose.cad.internal.pN.cA");
                Field stateField = cAClass.getDeclaredField("a");
                stateField.setAccessible(true);
                stateField.set(null, licensedState);

                // es.b() 在 cA.a == Licensed 之外还校验 es 上的静态 cy 字段 b 是否为 null。
                // 未加载 license 时 es.b 为 null，导致 es.b() 回退到 cN.a() 判定并返回 Evaluation。
                // 此处将 es.b 设为 cy.d() 单例，使三个条件全部满足。
                Class<?> cyClass = Class.forName("com.aspose.cad.internal.pN.cy");
                java.lang.reflect.Method cyDMethod = cyClass.getDeclaredMethod("d");
                cyDMethod.setAccessible(true);
                Object cySingleton = cyDMethod.invoke(null);

                Class<?> esClass = Class.forName("com.aspose.cad.internal.pN.es");
                Field esBField = esClass.getDeclaredField("b");
                esBField.setAccessible(true);
                esBField.set(null, cySingleton);

                watermarkRemoved = true;
                licenseLoaded = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static volatile Object noopWatermarkGuard;

    private static void disableWatermarkGuard(CadImage cadImage) {
        try {
            if (noopWatermarkGuard == null) {
                noopWatermarkGuard = Proxy.newProxyInstance(
                        IWatermarkGuardService.class.getClassLoader(),
                        new Class[]{IWatermarkGuardService.class},
                        (proxy, method, args) -> {
                            if (method.getReturnType() == boolean.class) {
                                return false;
                            }
                            return null;
                        });
            }
            Field wmField = findWatermarkGuardField();
            wmField.setAccessible(true);
            wmField.set(cadImage, noopWatermarkGuard);
        } catch (Exception e) {
            // 反射失败不影响主流程
        }
    }

    /**
     * 按类型定位 Image 上持有 IWatermarkGuardService 的字段。
     * 该字段为混淆名（24.3 为 l、24.9 为 m），不写死字段名以兼容版本差异。
     */
    private static Field findWatermarkGuardField() throws NoSuchFieldException {
        for (Field field : Image.class.getDeclaredFields()) {
            if (IWatermarkGuardService.class.equals(field.getType())) {
                return field;
            }
        }
        throw new NoSuchFieldException("IWatermarkGuardService field not found in " + Image.class.getName());
    }
}
