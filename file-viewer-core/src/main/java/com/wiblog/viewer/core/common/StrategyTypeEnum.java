package com.wiblog.viewer.core.common;

import java.util.Arrays;
import java.util.List;

/**
 * describe:
 *
 * @author panwm
 * @since 2024/6/28 16:31
 */
public enum StrategyTypeEnum {

    DWG("dwg", Constant.MediaType.PDF_VALUE),
    DXF("dxf", Constant.MediaType.PDF_VALUE),
    DOC("doc", Constant.MediaType.PDF_VALUE),
    DOCX("docx", Constant.MediaType.PDF_VALUE),
    XLSX("xlsx", Constant.MediaType.IMAGE_SVG_VALUE),
    XLS("xls", Constant.MediaType.IMAGE_SVG_VALUE),
    PDF("pdf", Constant.MediaType.PDF_VALUE),
    TXT("txt", Constant.MediaType.TXT_VALUE),
    CSV("csv", Constant.MediaType.TXT_VALUE),
    JSON("json", Constant.MediaType.JSON_VALUE),
    HTML("html", Constant.MediaType.HTML_VALUE),
    MP4("mp4", Constant.MediaType.VIDEO_MP4_VALUE),
    AVI("avi", Constant.MediaType.VIDEO_AVI_VALUE),
    JPG("jpg", Constant.MediaType.IMAGE_JPEG_VALUE),
    JPEG("jpeg", Constant.MediaType.IMAGE_JPEG_VALUE),
    PNG("png", Constant.MediaType.IMAGE_PNG_VALUE),
    GIF("gif", Constant.MediaType.IMAGE_GIF_VALUE)
    ;

    final String type;

    final String mediaType;

    public static final List<StrategyTypeEnum> PLAIN_TYPES = Arrays.asList(JPG, JPEG, PNG, GIF, PDF, TXT, JSON, CSV, HTML, MP4, AVI);

    public static final List<StrategyTypeEnum> WORD_TYPES = Arrays.asList(DOC, DOCX);

    public static final List<StrategyTypeEnum> EXCEL_TYPES = Arrays.asList(XLS, XLSX);

    public static final List<StrategyTypeEnum> CAD_TYPES = Arrays.asList(DWG, DXF);

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
