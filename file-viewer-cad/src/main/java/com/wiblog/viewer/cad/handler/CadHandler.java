package com.wiblog.viewer.cad.handler;

import com.aspose.cad.Color;
import com.aspose.cad.Image;
import com.aspose.cad.fileformats.cad.CadImage;
import com.aspose.cad.imageoptions.CadRasterizationOptions;
import com.aspose.cad.imageoptions.SvgOptions;
import com.wiblog.viewer.core.common.Constant;
import com.wiblog.viewer.core.common.StrategyTypeEnum;
import com.wiblog.viewer.core.handler.ViewerHandler;
import com.wiblog.viewer.core.utils.SVGUtil;

import javax.servlet.ServletOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * CAD 工具类
 *
 * @author panwm
 * @since 2024/6/26 22:25
 */
public class CadHandler extends ViewerHandler {

    @Override
    public void handler(InputStream inputStream, ServletOutputStream outputStream, String extension) throws Exception {
        if (StrategyTypeEnum.DWG.getType().equals(extension)) {
            convertToSvgForResponse(inputStream, outputStream);
        } else {
            throw new IllegalArgumentException(Constant.ERROR_MSG_ILLEGAL_TYPE + ":" + extension);
        }
    }

    @Override
    public List<StrategyTypeEnum> strategyTypeEnums() {
        return StrategyTypeEnum.CAD_TYPES;
    }

    /**
     * dwg转换svg
     *
     * @param inputStream  输入流
     * @param outputStream 输出流
     * @throws IOException 异常
     */
    public static void convertToSvgForResponse(InputStream inputStream, ServletOutputStream outputStream) throws IOException {
        CadImage cadImage = (CadImage) Image.load(inputStream);

        // 设置转换选项
        CadRasterizationOptions rasterOptions = new CadRasterizationOptions();
        rasterOptions.setPageWidth(1400);
        rasterOptions.setPageHeight(1400);
        rasterOptions.setBackgroundColor(Color.getWhite());

        SvgOptions svgOptions = new SvgOptions();
        svgOptions.setVectorRasterizationOptions(rasterOptions);

        // 将 CadImage 转换为 SVG 字符串
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        cadImage.save(byteOutputStream, svgOptions);

        byte[] byteArray = byteOutputStream.toByteArray();
        ByteArrayInputStream svgInputStream = new ByteArrayInputStream(byteArray);

        String transformedXml = SVGUtil.removeWatermark(svgInputStream, SVGUtil.CUT_TYPE_CAD);
        outputStream.write(transformedXml.getBytes());
    }
}
