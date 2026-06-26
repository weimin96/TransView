package com.wiblog.transview.servlet.jakarta;

import com.wiblog.transview.core.common.StrategyTypeEnum;
import com.wiblog.transview.core.utils.Util;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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
        response.setCharacterEncoding("UTF-8");
        try {
            com.wiblog.transview.core.context.TransViewContext.preview(inputStream, filename, response.getOutputStream());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void previewPlain(File file, String extension, HttpServletRequest request, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType(StrategyTypeEnum.getMediaType(extension));
        long lastModified = file.lastModified();
        response.setDateHeader("Last-Modified", lastModified);
        response.setHeader("Cache-Control", "public, max-age=3600");
        response.setHeader("ETag", "W/\"" + file.length() + "-" + lastModified + "\"");
        if (request != null) {
            if (isNotModified(file, request)) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }
            if (handleRange(file, request, response)) {
                return;
            }
        }
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
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("text/html;charset=UTF-8");
            try {
                response.getOutputStream().write("<html><head><title>503 -busy</title></head><body><h1>服务繁忙，请稍后重试</h1></body></html>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception ignored) {
            }
        } catch (com.wiblog.transview.core.exception.PreviewTimeoutException e) {
            response.reset();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/html;charset=UTF-8");
            try {
                response.getOutputStream().write("<html><head><title>500 -timeout</title></head><body><h1>timeout</h1></body></html>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            throw new RuntimeException("预览 " + extension + " 文件失败", e);
        }
    }

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
                long ifModifiedSince = ZonedDateTime.parse(ifModifiedSinceHeader, DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toInstant()
                        .toEpochMilli();
                if (ifModifiedSince > 0 && lastModified / 1000 <= ifModifiedSince / 1000) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    private static boolean handleRange(File file, HttpServletRequest request, HttpServletResponse response) {
        String rangeHeader = request.getHeader("Range");
        if (rangeHeader == null) {
            return false;
        }
        if (!rangeHeader.startsWith("bytes=")) {
            return setRangeNotSatisfiable(response, file.length());
        }

        long fileLength = file.length();
        long start = 0;
        long end = fileLength - 1;

        try {
            String rangeValue = rangeHeader.trim().substring("bytes=".length());
            if (rangeValue.contains(",")) {
                return setRangeNotSatisfiable(response, fileLength);
            }
            if (rangeValue.startsWith("-")) {
                long suffixLength = Long.parseLong(rangeValue.substring(1));
                if (suffixLength <= 0) {
                    return setRangeNotSatisfiable(response, fileLength);
                }
                start = Math.max(0, fileLength - suffixLength);
            } else if (rangeValue.endsWith("-")) {
                start = Long.parseLong(rangeValue.substring(0, rangeValue.length() - 1));
            } else {
                String[] parts = rangeValue.split("-");
                if (parts.length != 2) {
                    return setRangeNotSatisfiable(response, fileLength);
                }
                start = Long.parseLong(parts[0]);
                end = Long.parseLong(parts[1]);
            }
        } catch (Exception e) {
            return setRangeNotSatisfiable(response, fileLength);
        }

        if (start < 0 || start >= fileLength || end < start) {
            return setRangeNotSatisfiable(response, fileLength);
        }

        if (end >= fileLength) {
            end = fileLength - 1;
        }

        long contentLength = end - start + 1;
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
        response.setHeader("Accept-Ranges", "bytes");
        response.setContentLengthLong(contentLength);

        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r");
             java.io.OutputStream out = response.getOutputStream()) {
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
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    private static boolean setRangeNotSatisfiable(HttpServletResponse response, long fileLength) {
        response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
        response.setHeader("Content-Range", "bytes */" + fileLength);
        return true;
    }
}
