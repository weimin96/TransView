package com.wiblog.transview.core.common;

/**
 * Word 预览输出类型
 *
 * @author panwm
 */
public enum WordConvertType {

    SVG("svg"),

    PDF("pdf");

    private final String type;

    WordConvertType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
