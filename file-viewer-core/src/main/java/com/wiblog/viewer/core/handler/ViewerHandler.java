package com.wiblog.viewer.core.handler;

import com.wiblog.viewer.core.common.Constant;
import com.wiblog.viewer.core.common.StrategyTypeEnum;
import com.wiblog.viewer.core.config.FileViewerProperties;
import com.wiblog.viewer.core.utils.Util;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.*;

/**
 * describe: 预览处理抽象类
 *
 * @author panwm
 * @since 2024/6/28 14:48
 */
public abstract class ViewerHandler {

    protected ViewerHandler() {

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
    public abstract void handler(InputStream inputStream, ServletOutputStream outputStream, String extension) throws Exception;


    /**
     * 处理响应结果
     *
     * @param inputStream 文件流
     * @param extension   文件后缀
     */
    public void handlerResponse(InputStream inputStream, String extension) {
        HttpServletResponse response = Util.getResponse();
        // 设置 HttpServletResponse 的内容类型和输出
        response.setCharacterEncoding("UTF-8");
        ServletOutputStream outputStream = null;
        try {
            outputStream = response.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        handlerResponse(inputStream, outputStream, extension, response);
    }

    private void handlerResponse(InputStream inputStream, ServletOutputStream outputStream, String extension, HttpServletResponse response) {
        try {
            // 超时设置
            if (FileViewerProperties.getTimeout() != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Callable<Void> conversionTask = () -> {
                    try {
                        response.setContentType(StrategyTypeEnum.getMediaType(extension));
                        handler(inputStream, outputStream, extension);

                    } catch (Exception e) {
                        return null;
                    }
                    return null;
                };
                // 超时取消
                Future<Void> future = executor.submit(conversionTask);
                try {
                    future.get(FileViewerProperties.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
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
                handler(inputStream, outputStream, extension);
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
     */
    public void preview(InputStream inputStream, String filenameOrExtension) {
        check(filenameOrExtension);
        handlerResponse(inputStream, Util.getExtensionOrFilename(filenameOrExtension));
    }

    /**
     * 预览文件
     *
     * @param multipartFile 文件
     */
    public void preview(MultipartFile multipartFile) {
        String extension = Util.getExtensionOrFilename(multipartFile.getName());
        check(extension);
        try {
            handlerResponse(multipartFile.getInputStream(), extension);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 预览文件
     *
     * @param file 文件
     */
    public void preview(File file) {
        String extension = Util.getExtensionOrFilename(file.getName());
        check(extension);
        try {
            handlerResponse(Files.newInputStream(file.toPath()), extension);
        } catch (IOException e) {
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
