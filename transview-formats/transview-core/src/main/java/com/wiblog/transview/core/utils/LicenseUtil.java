package com.wiblog.transview.core.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Aspose license 加载工具。
 * 统一处理 classpath: 前缀解析和文件流打开。
 * 仅在显式配置 license-path 时加载，未配置则不加载。
 */
public final class LicenseUtil {

    private LicenseUtil() {
    }

    /**
     * 解析 license 路径，未配置时返回 null。
     */
    public static String resolvePath(String configuredPath) {
        return trimToNull(configuredPath);
    }

    /**
     * 打开 license 文件流，支持 classpath: 前缀或绝对/相对文件路径。
     */
    public static InputStream openStream(String licensePath) throws IOException {
        if (licensePath.startsWith("classpath:")) {
            String resourcePath = licensePath.substring("classpath:".length());
            while (resourcePath.startsWith("/")) {
                resourcePath = resourcePath.substring(1);
            }
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
            if (inputStream == null) {
                throw new FileNotFoundException(licensePath);
            }
            return inputStream;
        }
        return Files.newInputStream(Paths.get(licensePath));
    }

    static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
