package com.wiblog.viewer.core.utils;

import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletResponse;

/**
 * describe: 工具类
 *
 * @author panwm
 * @since 2024/6/28 14:53
 */
public class Util {

    public static ServletRequestAttributes getRequestAttributes() {
        try {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            return (ServletRequestAttributes) attributes;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取响应体
     * @return Response
     */
    public static HttpServletResponse getResponse() {
        try {
            return getRequestAttributes().getResponse();
        } catch (Exception e) {
            return null;
        }
    }


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
}
