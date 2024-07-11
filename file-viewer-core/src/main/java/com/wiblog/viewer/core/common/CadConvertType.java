package com.wiblog.viewer.core.common;

/**
 * cad 转换类型
 * @author panwm
 * @since 2024/7/12 0:25
 */
public enum CadConvertType {


    SVG("svg"),

    PDF("pdf");


    private String type;


    CadConvertType(String type) {
        this.type = type;
    }
}
