package com.wiblog.transview.core.common;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件后缀
 *
 * @author panwm
 * @since 2024/12/11 14:30
 */
public enum ExtensionEnum {

    PNG("png"),
    JPG("jpg"),
    JPEG("jpeg"),
    GIF("gif"),
    SVG("svg"),
    PDF("pdf"),
    DOC("doc"),
    DOCX("docx");

    final String value;

    private static final Map<String, ExtensionEnum> ENUM_MAP = new HashMap<>();

    static {
        for (ExtensionEnum enumItem : ExtensionEnum.values()) {
            ENUM_MAP.put(enumItem.getValue(), enumItem);
        }
    }

    ExtensionEnum(String value) {
        this.value = value;
    }

    public static ExtensionEnum getByValue(String value) {
        return ENUM_MAP.get(value);
    }

    public String getValue() {
        return value;
    }
}
