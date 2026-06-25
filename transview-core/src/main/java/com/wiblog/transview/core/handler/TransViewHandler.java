package com.wiblog.transview.core.handler;

import com.wiblog.transview.core.bean.TransViewProperties;
import com.wiblog.transview.core.common.Constant;
import com.wiblog.transview.core.common.ExtensionEnum;
import com.wiblog.transview.core.common.StrategyTypeEnum;
import com.wiblog.transview.core.utils.Util;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.*;

/**
 * describe: 预览处理抽象类
 *
 * @author panwm
 * @since 2024/6/28 14:48
 */
public abstract class TransViewHandler {

    private static volatile ExecutorService PREVIEW_EXECUTOR;

    private static final class ExecutorHolder {
        static final ExecutorService DEFAULT = createExecutor(
                TransViewProperties.Executor.getCorePoolSize(),
                TransViewProperties.Executor.getMaxPoolSize(),
                TransViewProperties.Executor.getQueueCapacity()
        );
    }

    private static ExecutorService createExecutor(int corePoolSize, int maxPoolSize, int queueCapacity) {
        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new ThreadFactory() {
                    private final java.util.concurrent.atomic.AtomicInteger index = new java.util.concurrent.atomic.AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "transview-preview-" + index.getAndIncrement());
                        thread.setDaemon(true);
                        return thread;
                    }
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * 自定义线程池，覆盖默认配置
     */
    public static void initExecutor(int corePoolSize, int maxPoolSize, int queueCapacity) {
        PREVIEW_EXECUTOR = createExecutor(corePoolSize, maxPoolSize, queueCapacity);
    }

    private static ExecutorService getExecutor() {
        return PREVIEW_EXECUTOR != null ? PREVIEW_EXECUTOR : ExecutorHolder.DEFAULT;
    }

    protected TransViewHandler() {

    }

    /**
     * 文件转换
     * @param sourceExtensionEnum 源后缀
     * @param targetExtensionEnum 目标后缀
     * @param inputStream 文件流
     * @param outputStream 输出流
     * @throws Exception 异常
     */
    public abstract void convertHandler(ExtensionEnum sourceExtensionEnum, ExtensionEnum targetExtensionEnum, InputStream inputStream, OutputStream outputStream) throws Exception;

    public void convertHandler(ExtensionEnum sourceExtensionEnum, ExtensionEnum targetExtensionEnum, InputStream inputStream, File targetFile) throws Exception {
        try (OutputStream outputStream = Files.newOutputStream(targetFile.toPath())) {
            convertHandler(sourceExtensionEnum, targetExtensionEnum, inputStream, outputStream);
        }
    }

    /**
     * 支持的策略枚举列表
     *
     * @return List 策略枚举列表
     */
    public abstract List<StrategyTypeEnum> strategyTypeEnums();

    /**
     * 文件写入到 OutputStream
     *
     * @param inputStream  文件流
     * @param outputStream 输出流
     * @param extension    文件后缀
     * @param response     HttpServletResponse
     * @throws Exception 异常
     */
    public abstract void viewHandler(InputStream inputStream, OutputStream outputStream, String extension, HttpServletResponse response) throws Exception;

    /**
     * 处理响应结果
     *
     * @param inputStream 文件流
     * @param extension   文件后缀
     */
    public void handlerResponse(InputStream inputStream, String extension, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        handlerViewResponse(inputStream, extension, response);
    }

    private void handlerViewResponse(InputStream inputStream, String extension, HttpServletResponse response) {
        try {
            if (TransViewProperties.View.getTimeout() != null) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                Callable<Void> conversionTask = () -> {
                    viewHandler(inputStream, buffer, extension, response);
                    return null;
                };
                Future<Void> future;
                try {
                    future = getExecutor().submit(conversionTask);
                } catch (RejectedExecutionException e) {
                    response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    response.setContentType(Constant.MediaType.HTML_VALUE);
                    response.getOutputStream().write("<html><head><title>503 -busy</title></head><body><h1>服务繁忙，请稍后重试</h1></body></html>".getBytes(StandardCharsets.UTF_8));
                    return;
                }
                try {
                    future.get(TransViewProperties.View.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
                    byte[] result = buffer.toByteArray();
                    response.setContentType(StrategyTypeEnum.getMediaType(extension));
                    response.setContentLength(result.length);
                    response.getOutputStream().write(result);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    response.reset();
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType(Constant.MediaType.HTML_VALUE);
                    response.getOutputStream().write("<html><head><title>500 -timeout</title></head><body><h1>timeout</h1></body></html>".getBytes(StandardCharsets.UTF_8));
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    }
                    throw new RuntimeException("预览 " + extension + " 文件失败", cause);
                }
            } else {
                response.setContentType(StrategyTypeEnum.getMediaType(extension));
                viewHandler(inputStream, response.getOutputStream(), extension, response);
            }
            response.flushBuffer();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("预览 " + extension + " 文件失败", e);
        }
    }

    /**
     * 预览文件
     *
     * @param inputStream         文件流
     * @param filenameOrExtension 文件名或后缀
     * @param response            响应
     */
    public void preview(InputStream inputStream, String filenameOrExtension, HttpServletResponse response) {
        check(filenameOrExtension);
        handlerResponse(inputStream, Util.getExtensionOrFilename(filenameOrExtension), response);
    }

    /**
     * 预览文件
     *
     * @param file 文件
     * @param request  HttpServletRequest
     * @param response HttpServletResponse
     */
    public void preview(File file, HttpServletRequest request, HttpServletResponse response) {
        String extension = Util.getExtension(file.getName());
        check(extension);
        response.setContentType(StrategyTypeEnum.getMediaType(extension));
        if (isPlainType(extension)) {
            long lastModified = file.lastModified();
            response.setContentLengthLong(file.length());
            response.setDateHeader("Last-Modified", lastModified);
            response.setHeader("Cache-Control", "public, max-age=3600");
            response.setHeader("ETag", "W/\"" + file.length() + "-" + lastModified + "\"");
            if (isNotModified(file, request)) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }
            if (handleRange(file, request, response)) {
                return;
            }
        }
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            handlerResponse(inputStream, extension, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 文件转换
     * @param file 源文件
     * @param extensionEnum 转换类型
     * @param outputStream 输出文件流
     */
    public void convert(File file, ExtensionEnum extensionEnum, OutputStream outputStream) {
        String extension = Util.getExtension(file.getName());
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            check(extension);
            convertHandler(ExtensionEnum.getByValue(extension), extensionEnum, inputStream, outputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void convert(InputStream inputStream, String extension, File targetFile) {
        try {
            check(extension);
            ExtensionEnum extensionEnum = ExtensionEnum.getByValue(Util.getExtension(targetFile.getName()));
            if (extensionEnum == null) {
                throw new IllegalArgumentException(Constant.ERROR_MSG_ILLEGAL_TYPE + ":" + extension);
            }
            convertHandler(ExtensionEnum.getByValue(extension), extensionEnum, inputStream, targetFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 检查文件类型是否合法
     *
     * @param filenameOrExtension 文件名或后缀
     */
    protected void check(String filenameOrExtension) {
        String extension = Util.getExtensionOrFilename(filenameOrExtension);
        boolean valid = strategyTypeEnums().contains(StrategyTypeEnum.getStrategy(extension));
        if (!valid) {
            throw new IllegalArgumentException(Constant.ERROR_MSG_ILLEGAL_TYPE + ":" + extension);
        }
    }

    private static boolean isPlainType(String extension) {
        StrategyTypeEnum strategy = StrategyTypeEnum.getStrategy(extension);
        return strategy != null && StrategyTypeEnum.PLAIN_TYPES.contains(strategy);
    }

    private static final java.text.SimpleDateFormat HTTP_DATE_FORMAT;

    static {
        HTTP_DATE_FORMAT = new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US);
        HTTP_DATE_FORMAT.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
    }

    /**
     * 处理 If-None-Match / If-Modified-Since 条件请求
     * @return true 表示资源未修改，已返回 304
     */
    private static boolean isNotModified(File file, HttpServletRequest request) {
        long lastModified = file.lastModified();
        String etag = "W/\"" + file.length() + "-" + lastModified + "\"";

        String ifNoneMatch = request.getHeader("If-None-Match");
        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return true;
        }

        String ifModifiedSinceHeader = request.getHeader("If-Modified-Since");
        if (ifModifiedSinceHeader != null) {
            try {
                long ifModifiedSince = HTTP_DATE_FORMAT.parse(ifModifiedSinceHeader).getTime();
                if (ifModifiedSince > 0 && lastModified / 1000 <= ifModifiedSince / 1000) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    /**
     * 处理 Range 请求（206 Partial Content）
     * @return true 表示已处理 Range 请求
     */
    private static boolean handleRange(File file, HttpServletRequest request, HttpServletResponse response) {
        String rangeHeader = request.getHeader("Range");
        if (rangeHeader == null) {
            return false;
        }

        long fileLength = file.length();
        long start = 0;
        long end = fileLength - 1;

        try {
            String rangeValue = rangeHeader.trim().substring("bytes=".length());
            if (rangeValue.startsWith("-")) {
                start = fileLength - Long.parseLong(rangeValue.substring(1));
            } else if (rangeValue.endsWith("-")) {
                start = Long.parseLong(rangeValue.substring(0, rangeValue.length() - 1));
            } else {
                String[] parts = rangeValue.split("-");
                start = Long.parseLong(parts[0]);
                end = Long.parseLong(parts[1]);
            }
        } catch (Exception e) {
            return false;
        }

        if (start < 0 || start >= fileLength || end < start) {
            response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            response.setHeader("Content-Range", "bytes */" + fileLength);
            return true;
        }

        if (end >= fileLength) {
            end = fileLength - 1;
        }

        long contentLength = end - start + 1;
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
        response.setHeader("Accept-Ranges", "bytes");
        response.setContentLengthLong(contentLength);

        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             OutputStream out = response.getOutputStream()) {
            raf.seek(start);
            byte[] buffer = new byte[8192];
            long remaining = contentLength;
            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int read = raf.read(buffer, 0, toRead);
                if (read <= 0) break;
                out.write(buffer, 0, read);
                remaining -= read;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

}
