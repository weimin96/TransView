package com.wiblog.viewer.handler.impl;

import com.aspose.cad.Image;
import com.aspose.cad.fileformats.cad.CadImage;
import com.aspose.cad.imageoptions.CadRasterizationOptions;
import com.aspose.cad.imageoptions.SvgOptions;
import com.wiblog.viewer.common.Constant;
import com.wiblog.viewer.common.StrategyTypeEnum;
import com.wiblog.viewer.handler.ViewerHandler;
import com.wiblog.viewer.utils.SVGUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * CAD 工具类
 *
 * @author panwm
 * @since 2024/6/26 22:25
 */
public class CadHandler implements ViewerHandler {

    @Override
    public void preview(InputStream inputStream, String type) {
        if (StrategyTypeEnum.DWG.getType().equals(type)) {
            convertToSvgForResponse(inputStream);
        } else {
            throw new IllegalArgumentException(Constant.ERROR_MSG_ILLEGAL_TYPE + ":" + type);
        }
    }

    /**
     * dwg转换svg
     */
    public static void convertToSvgForResponse(InputStream inputStream) {
        CadImage cadImage = (CadImage) Image.load(inputStream);

        // 设置转换选项
        CadRasterizationOptions rasterOptions = new CadRasterizationOptions();
        rasterOptions.setPageWidth(1600);
        rasterOptions.setPageHeight(1600);

        SvgOptions svgOptions = new SvgOptions();
        svgOptions.setVectorRasterizationOptions(rasterOptions);

        // 将 CadImage 转换为 SVG 字符串
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        cadImage.save(outputStream, svgOptions);

        byte[] byteArray = outputStream.toByteArray();
        ByteArrayInputStream svgInputStream = new ByteArrayInputStream(byteArray);

        SVGUtil.previewCropSvg(svgInputStream, 100);
    }
}
