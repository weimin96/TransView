package com.wiblog.transview.core.handler;

import com.wiblog.transview.core.bean.TransViewProperties;
import com.wiblog.transview.core.common.Constant;
import com.wiblog.transview.core.common.ExtensionEnum;
import com.wiblog.transview.core.common.StrategyTypeEnum;
import com.wiblog.transview.core.utils.Util;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
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
        convertHandler(sourceExtensionEnum, targetExtensionEnum, inputStream, Files.newOutputStream(targetFile.toPath()));
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
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Callable<Void> conversionTask = () -> {
                    try {
                        response.setContentType(StrategyTypeEnum.getMediaType(extension));
                        viewHandler(inputStream, outputStream, extension, response);

                    } catch (Exception e) {
                        return null;
                    }
                    return null;
                };
                // 超时取消
                Future<Void> future = executor.submit(conversionTask);
                try {
                    future.get(TransViewProperties.View.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType(Constant.MediaType.HTML_VALUE);
                    // 文件不存在
                    outputStream.write("<html><head><title>500 -timeout</title></head><body><h1>timeout</h1></body></html>".getBytes());
                } finally {
                    executor.shutdown();
                }
            } else {
                response.setContentType(StrategyTypeEnum.getMediaType(extension));
                viewHandler(inputStream, outputStream, extension, response);
            }
            response.flushBuffer();
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
        try {
            handlerResponse(Files.newInputStream(file.toPath()), extension, response);
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
        try {
            String extension = Util.getExtension(file.getName());
            check(extension);
            convertHandler(ExtensionEnum.getByValue(extension), extensionEnum, Files.newInputStream(file.toPath()), outputStream);
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
