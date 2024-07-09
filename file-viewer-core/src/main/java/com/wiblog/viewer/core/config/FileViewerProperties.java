package com.wiblog.viewer.core.config;

/**
 * 参数配置
 * @author panwm
 * @since 2024/7/10 0:02
 */
public class FileViewerProperties {

    /**
     * 字体目录
     */
    private static String fontsFolder;

    /**
     * cad shx 字体目录
     */
    private static String[] shxFontsFolder;

    public static String getFontsFolder() {
        return fontsFolder;
    }

    public static void setFontsFolder(String fontsFolder) {
        FileViewerProperties.fontsFolder = fontsFolder;
    }

    public static String[] getShxFontsFolder() {
        return shxFontsFolder;
    }

    /**
     * cad shx 字体目录
     */
    public static void setShxFontsFolder(String[] shxFontsFolder) {
        FileViewerProperties.shxFontsFolder = shxFontsFolder;
    }
}
