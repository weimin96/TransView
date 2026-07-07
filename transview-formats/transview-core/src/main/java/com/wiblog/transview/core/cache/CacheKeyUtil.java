package com.wiblog.transview.core.cache;

import com.wiblog.transview.core.bean.TransViewProperties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

/**
 * 缓存 Key 生成工具。
 * Key 由文件内容哈希 + 转换参数组成，确保同名不同内容、不同配置不会误命中。
 *
 * @author panwm
 * @since 2024/7/10 0:02
 */
public class CacheKeyUtil {

    private static final int HASH_BUFFER_SIZE = 8192;

    private CacheKeyUtil() {
    }

    /**
     * 生成 CAD 缓存 Key（使用默认布局）
     */
    public static String generateCadCacheKey(File file) {
        return generateCadCacheKey(file, TransViewProperties.View.Cad.getLayout());
    }

    /**
     * 生成 CAD 缓存 Key（指定布局）
     *
     * @param file   源文件
     * @param layout 布局名称
     * @return 缓存 Key，格式: sha256-{hash}-{paramsHash}
     */
    public static String generateCadCacheKey(File file, String layout) {
        try {
            String fileHash = sha256(file);
            long fileSize = file.length();

            String params = fileHash
                    + "|" + fileSize
                    + "|aspose-cad"
                    + "|" + TransViewProperties.View.Cad.getConvertType().getType()
                    + "|" + layout
                    + "|" + TransViewProperties.View.Cad.getPageWidth()
                    + "|" + TransViewProperties.View.Cad.getPageHeight()
                    + "|" + TransViewProperties.View.isRemoveWatermark();

            String shx = TransViewProperties.View.Cad.getShxFontsFolder() != null
                    ? String.join(",", TransViewProperties.View.Cad.getShxFontsFolder())
                    : "";
            params += "|" + shx;

            String paramsHash = sha256String(params);
            return fileHash + "-" + paramsHash.substring(0, 16);
        } catch (Exception e) {
            throw new RuntimeException("生成缓存 Key 失败", e);
        }
    }

    static String sha256(File file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] buffer = new byte[HASH_BUFFER_SIZE];
        try (InputStream in = Files.newInputStream(file.toPath())) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return bytesToHex(digest.digest());
    }

    static String sha256String(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        try (Formatter formatter = new Formatter()) {
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }
}
