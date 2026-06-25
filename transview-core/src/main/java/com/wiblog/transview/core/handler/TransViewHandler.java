package com.wiblog.transview.core.handler;

import com.wiblog.transview.core.bean.TransViewProperties;
import com.wiblog.transview.core.common.Constant;
import com.wiblog.transview.core.common.ExtensionEnum;
import com.wiblog.transview.core.common.StrategyTypeEnum;
import com.wiblog.transview.core.utils.Util;
import javax.servlet.ServletOutputStream;
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

    private static final ExecutorService PREVIEW_EXECUTOR = new ThreadPoolExecutor(
            Math.max(1, Runtime.getRuntime().availableProcessors()),
            Math.max(1, Runtime.getRuntime().availableProcessors() * 2),
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(200),
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
     * 文件写入到 HttpServletResponse
     *
     * @param inputStream  文件流
     * @param outputStream HttpServletResponse 输出流
     * @param extension    文件后缀
     * @throws Exception 异常
     */
    public abstract void viewHandler(InputStream inputStream, ServletOutputStream outputStream, String extension, HttpServletResponse response) throws Exception;

    /**
     * 处理响应结果
     *
     * @param inputStream 文件流
     * @param extension   文件后缀
     */
    public void handlerResponse(InputStream inputStream, String extension, HttpServletResponse response) {
        // 设置 HttpServletResponse 的内容类型和输出
        response.setCharacterEncoding("UTF-8");
        ServletOutputStream outputStream = null;
        try {
            outputStream = response.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        handlerViewResponse(inputStream, outputStream, extension, response);
    }

    private void handlerViewResponse(InputStream inputStream, ServletOutputStream outputStream, String extension, HttpServletResponse response) {
        try {
            // 超时设置
            if (TransViewProperties.View.getTimeout() != null) {
                Callable<Void> conversionTask = () -> {
                    response.setContentType(StrategyTypeEnum.getMediaType(extension));
                    viewHandler(inputStream, outputStream, extension, response);
                    return null;
                };
                Future<Void> future;
                try {
                    future = PREVIEW_EXECUTOR.submit(conversionTask);
                } catch (RejectedExecutionException e) {
                    response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    response.setContentType(Constant.MediaType.HTML_VALUE);
                    outputStream.write("<html><head><title>503 -busy</title></head><body><h1>服务繁忙，请稍后重试</h1></body></html>".getBytes(StandardCharsets.UTF_8));
                    return;
                }
                try {
                    future.get(TransViewProperties.View.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType(Constant.MediaType.HTML_VALUE);
                    outputStream.write("<html><head><title>500 -timeout</title></head><body><h1>timeout</h1></body></html>".getBytes(StandardCharsets.UTF_8));
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    }
                    throw new RuntimeException("预览 " + extension + " 文件失败", cause);
                }
            } else {
                response.setContentType(StrategyTypeEnum.getMediaType(extension));
                viewHandler(inputStream, outputStream, extension, response);
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
     */
    public void preview(File file, HttpServletResponse response) {
        String extension = Util.getExtensionOrFilename(file.getName());
        check(extension);
        // 纯文件类型直接设置 Content-Length 和缓存头
        if (StrategyTypeEnum.PLAIN_TYPES.contains(StrategyTypeEnum.getStrategy(extension))) {
            response.setContentLengthLong(file.length());
            long lastModified = file.lastModified();
            response.setDateHeader("Last-Modified", lastModified);
            response.setHeader("Cache-Control", "public, max-age=3600");
            response.setHeader("ETag", "W/\"" + file.length() + "-" + lastModified + "\"");
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


}
