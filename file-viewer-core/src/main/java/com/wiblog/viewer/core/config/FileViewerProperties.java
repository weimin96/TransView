package com.wiblog.viewer.core.config;

import com.wiblog.viewer.core.common.CadConvertType;

import java.time.Duration;


/**
 * 参数配置
 * @author panwm
 * @since 2024/7/10 0:02
 */
public class FileViewerProperties {

    /**
     * 取消请求时是否需要中断操作
     */
    private static boolean isInterrupted = false;

    /**
     * 字体目录
     */
    private static String fontsFolder;

    /**
     * 超时 单位：秒
     */
    private static Duration timeout;

    public static String getFontsFolder() {
        return fontsFolder;
    }

    public static void setFontsFolder(String fontsFolder) {
        FileViewerProperties.fontsFolder = fontsFolder;
    }

    public static Duration getTimeout() {
        return timeout;
    }

    /**
     * 超时 单位：秒
     * @param timeout 超时
     */
    public static void setTimeout(Duration timeout) {
        FileViewerProperties.timeout = timeout;
    }

    public static boolean getIsInterrupted() {
        return isInterrupted;
    }

    /**
     * 取消请求时是否需要中断操作
     * @param isInterrupted 取消请求时是否需要中断操作
     */
    public static void setIsInterrupted(boolean isInterrupted) {
        FileViewerProperties.isInterrupted = isInterrupted;
    }

    public static class Cad {

        /**
         * 转换结果类型 svg|pdf
         */
        private static CadConvertType convertType = CadConvertType.SVG;

        /**
         * cad shx 字体目录
         */
        private static String[] shxFontsFolder;

        public static String[] getShxFontsFolder() {
            return shxFontsFolder;
        }

        /**
         *  cad shx 字体目录
         * @param shxFontsFolder shx 字体目录
         */
        public static void setShxFontsFolder(String[] shxFontsFolder) {
            Cad.shxFontsFolder = shxFontsFolder;
        }

        /**
         * 获取CAD转换结果类型
         * @return CAD转换结果类型
         */
        public static CadConvertType getConvertType() {
            return convertType;
        }

        /**
         * 设置CAD转换结果类型
         * @param convertType CAD转换结果类型
         */
        public static void setConvertType(CadConvertType convertType) {
            Cad.convertType = convertType;
        }
    }
}
