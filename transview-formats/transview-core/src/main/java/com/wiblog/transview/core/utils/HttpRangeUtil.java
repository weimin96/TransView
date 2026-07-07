package com.wiblog.transview.core.utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * HTTP 条件请求和 Range 请求的纯逻辑工具，不依赖 servlet API。
 * servlet 适配层负责读写 HTTP 头，本类负责解析和校验。
 */
public class HttpRangeUtil {

    /**
     * Range 解析结果
     */
    public static class Range {
        public final long start;
        public final long end;

        public Range(long start, long end) {
            this.start = start;
            this.end = end;
        }

        public long contentLength() {
            return end - start + 1;
        }
    }

    private HttpRangeUtil() {
    }

    /**
     * 生成弱 ETag
     */
    public static String generateETag(long fileLength, long lastModified) {
        return "W/\"" + fileLength + "-" + lastModified + "\"";
    }

    /**
     * 检查条件请求是否未修改
     *
     * @param etag            当前资源 ETag
     * @param ifNoneMatch     请求头 If-None-Match
     * @param lastModified    资源最后修改时间（毫秒）
     * @param ifModifiedSince 请求头 If-Modified-Since（RFC 1123 格式）
     * @return true 表示资源未修改
     */
    public static boolean isNotModified(String etag, String ifNoneMatch, long lastModified, String ifModifiedSince) {
        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return true;
        }

        if (ifModifiedSince != null) {
            try {
                long ims = ZonedDateTime.parse(ifModifiedSince, DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toInstant()
                        .toEpochMilli();
                if (ims > 0 && lastModified / 1000 <= ims / 1000) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    /**
     * 解析 Range 请求头
     *
     * @param rangeHeader 请求头值（如 "bytes=0-499"）
     * @param fileLength  文件大小
     * @return 解析结果，null 表示无法满足（应返回 416）
     */
    public static Range parseRange(String rangeHeader, long fileLength) {
        if (rangeHeader == null) {
            return null;
        }
        if (!rangeHeader.startsWith("bytes=")) {
            return null;
        }

        long start = 0;
        long end = fileLength - 1;

        try {
            String rangeValue = rangeHeader.trim().substring("bytes=".length());
            if (rangeValue.contains(",")) {
                return null;
            }
            if (rangeValue.startsWith("-")) {
                long suffixLength = Long.parseLong(rangeValue.substring(1));
                if (suffixLength <= 0) {
                    return null;
                }
                start = Math.max(0, fileLength - suffixLength);
            } else if (rangeValue.endsWith("-")) {
                start = Long.parseLong(rangeValue.substring(0, rangeValue.length() - 1));
            } else {
                String[] parts = rangeValue.split("-");
                if (parts.length != 2) {
                    return null;
                }
                start = Long.parseLong(parts[0]);
                end = Long.parseLong(parts[1]);
            }
        } catch (Exception e) {
            return null;
        }

        if (start < 0 || start >= fileLength || end < start) {
            return null;
        }

        if (end >= fileLength) {
            end = fileLength - 1;
        }

        return new Range(start, end);
    }

    /**
     * 将文件的指定字节范围写入输出流
     */
    public static void writeRange(File file, long start, long end, OutputStream out) throws IOException {
        long contentLength = end - start + 1;
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(start);
            byte[] buffer = new byte[8192];
            long remaining = contentLength;
            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int read = raf.read(buffer, 0, toRead);
                if (read <= 0) {
                    break;
                }
                out.write(buffer, 0, read);
                remaining -= read;
            }
        }
    }
}
