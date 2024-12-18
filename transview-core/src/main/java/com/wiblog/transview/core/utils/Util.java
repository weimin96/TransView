package com.wiblog.transview.core.utils;

/**
 * describe: 工具类
 *
 * @author panwm
 * @since 2024/6/28 14:53
 */
public class Util {


    /**
     * 截取文件拓展名
     * @param path 文件路径
     * @return ext
     */
    public static String getExtension(String path) {
        int lastIndex = path.lastIndexOf('.');
        if (lastIndex != -1 && lastIndex < path.length() - 1) {
            return path.substring(lastIndex + 1).toLowerCase();
        } else {
            return null;
        }
    }

    /**
     * 截取文件拓展名
     * @param path 文件路径
     * @return ext
     */
    public static String getExtensionOrFilename(String path) {
        String extension = getExtension(path);
        if (extension == null) {
            return path;
        }
        return extension;
    }

    public static boolean isBlank(final CharSequence cs) {
        final int strLen = length(cs);
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static int length(final CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }
}
