package com.wiblog.viewer.common;

import java.util.Arrays;

/**
 * describe:
 *
 * @author panwm
 * @since 2024/6/28 16:31
 */
public enum StrategyTypeEnum {

    DWG("dwg", Constant.MediaType.IMAGE_SVG_VALUE),
    DOC("doc", Constant.MediaType.PDF_VALUE),
    DOCX("docx", Constant.MediaType.PDF_VALUE),
    JPG("jpg", Constant.MediaType.IMAGE_JPEG_VALUE),
    JPEG("jpeg", Constant.MediaType.IMAGE_JPEG_VALUE),
    PNG("png", Constant.MediaType.IMAGE_PNG_VALUE),
    GIF("gif", Constant.MediaType.IMAGE_GIF_VALUE);

    final String type;

    final String mediaType;

    public static final StrategyTypeEnum[] PICTURE_TYPES = {JPG, JPEG, PNG, GIF};

    StrategyTypeEnum(String type, String mediaType) {
        this.type = type;
        this.mediaType = mediaType;
    }

    public static StrategyTypeEnum getStrategy(String type) {
        return Arrays.stream(StrategyTypeEnum.values())
                .filter(e -> e.type.equals(type))
                .findFirst()
                .orElse(null);
    }

    public static String getMediaType(String type) {
        return Arrays.stream(StrategyTypeEnum.values())
                .filter(e -> e.type.equals(type))
                .findFirst()
                .map(majorEnum -> majorEnum.mediaType)
                .orElse(null);
    }

    public String getType() {
        return type;
    }
}
