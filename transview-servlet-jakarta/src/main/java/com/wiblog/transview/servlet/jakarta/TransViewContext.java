package com.wiblog.transview.servlet.jakarta;

import com.wiblog.transview.core.common.StrategyTypeEnum;
import com.wiblog.transview.core.utils.HttpRangeUtil;
import com.wiblog.transview.core.utils.Util;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.InputStream;

/**
 * jakarta.servlet 适配层 — 提供带 HttpServletResponse 的预览入口
 *
 * @author panwm
 * @since 2024/6/28 15:09
 */
public class TransViewContext {

    private TransViewContext() {

    }

    /**
     * 文件预览入口（支持 HTTP 缓存和 Range 请求）
     *
     * @param file     文件
     * @param request  HttpServletRequest
     * @param response HttpServletResponse
     */
    public static void preview(File file, HttpServletRequest request, HttpServletResponse response) {
        String extension = Util.getExtension(file.getName());
        if (Util.isBlank(extension)) {
            throw new RuntimeException("获取不到文件后缀");
        }
        if (com.wiblog.transview.core.handler.TransViewHandler.isPlainType(extension)) {
            previewPlain(file, extension, request, response);
        } else {
            previewConverted(file, extension, response);
        }
    }

    /**
     * 文件预览入口
     *
     * @param file     文件
     * @param response HttpServletResponse
     */
    public static void preview(File file, HttpServletResponse response) {
        preview(file, null, response);
    }

    /**
     * 文件预览入口
     *
     * @param inputStream 文件流
     * @param filename    文件名
     * @param response    HttpServletResponse
     */
    public static void preview(InputStream inputStream, String filename, HttpServletResponse response) {
        String extension = Util.getExtension(filename);
        if (Util.isBlank(extension)) {
            throw new RuntimeException("获取不到文件后缀");
        }
        previewConverted(inputStream, filename, extension, response);
    }

    private static void previewPlain(File file, String extension, HttpServletRequest request, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType(StrategyTypeEnum.getMediaType(extension));
        long lastModified = file.lastModified();
        String etag = HttpRangeUtil.generateETag(file.length(), lastModified);
        response.setDateHeader("Last-Modified", lastModified);
        response.setHeader("Cache-Control", "public, max-age=3600");
        response.setHeader("ETag", etag);
        if (request != null) {
            if (HttpRangeUtil.isNotModified(etag, request.getHeader("If-None-Match"), lastModified, request.getHeader("If-Modified-Since"))) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }
            HttpRangeUtil.Range range = HttpRangeUtil.parseRange(request.getHeader("Range"), file.length());
            if (range != null) {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + file.length());
                response.setHeader("Accept-Ranges", "bytes");
                response.setContentLengthLong(range.contentLength());
                try {
                    HttpRangeUtil.writeRange(file, range.start, range.end, response.getOutputStream());
                } catch (java.io.IOException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
            if (request.getHeader("Range") != null) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + file.length());
                return;
            }
        }
        response.setHeader("Accept-Ranges", "bytes");
        response.setContentLengthLong(file.length());
        try {
            com.wiblog.transview.core.context.TransViewContext.preview(file, response.getOutputStream());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void previewConverted(File file, String extension, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType(StrategyTypeEnum.getMediaType(extension));
        try {
            com.wiblog.transview.core.context.TransViewContext.preview(file, response.getOutputStream());
        } catch (com.wiblog.transview.core.exception.PreviewBusyException e) {
            writeBusy(response);
        } catch (com.wiblog.transview.core.exception.PreviewTimeoutException e) {
            writeTimeout(response);
        } catch (Exception e) {
            throw new RuntimeException("预览 " + extension + " 文件失败", e);
        }
    }

    private static void previewConverted(InputStream inputStream, String filename, String extension, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType(StrategyTypeEnum.getMediaType(extension));
        try {
            com.wiblog.transview.core.context.TransViewContext.preview(inputStream, filename, response.getOutputStream());
        } catch (com.wiblog.transview.core.exception.PreviewBusyException e) {
            writeBusy(response);
        } catch (com.wiblog.transview.core.exception.PreviewTimeoutException e) {
            writeTimeout(response);
        } catch (Exception e) {
            throw new RuntimeException("预览 " + extension + " 文件失败", e);
        }
    }

    private static void writeBusy(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("text/html;charset=UTF-8");
        try {
            response.getOutputStream().write("<html><head><title>503 -busy</title></head><body><h1>服务繁忙，请稍后重试</h1></body></html>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private static void writeTimeout(HttpServletResponse response) {
        response.reset();
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("text/html;charset=UTF-8");
        try {
            response.getOutputStream().write("<html><head><title>500 -timeout</title></head><body><h1>timeout</h1></body></html>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }
}
