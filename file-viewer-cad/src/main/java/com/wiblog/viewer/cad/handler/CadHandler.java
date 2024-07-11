package com.wiblog.viewer.cad.handler;

import com.aspose.cad.Color;
import com.aspose.cad.Image;
import com.aspose.cad.fileformats.cad.CadDrawTypeMode;
import com.aspose.cad.fileformats.cad.CadImage;
import com.aspose.cad.imageoptions.CadRasterizationOptions;
import com.aspose.cad.imageoptions.PdfOptions;
import com.aspose.cad.imageoptions.SvgOptions;
import com.wiblog.viewer.cad.utils.PdfUtil;
import com.wiblog.viewer.core.common.CadConvertType;
import com.wiblog.viewer.core.common.StrategyTypeEnum;
import com.wiblog.viewer.core.config.FileViewerProperties;
import com.wiblog.viewer.core.handler.ViewerHandler;
import com.wiblog.viewer.core.utils.SVGUtil;

import javax.servlet.ServletOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.*;

/**
 * CAD 工具类
 *
 * @author panwm
 * @since 2024/6/26 22:25
 */
public class CadHandler extends ViewerHandler {

    @Override
    public void handler(InputStream inputStream, ServletOutputStream outputStream, String extension) throws Exception {
        CadImage cadImage = (CadImage) Image.load(inputStream);

        // 设置转换选项
        CadRasterizationOptions rasterOptions = new CadRasterizationOptions();
        rasterOptions.setPageWidth(cadImage.getWidth());
        rasterOptions.setPageHeight(cadImage.getHeight());
        rasterOptions.setDrawType(CadDrawTypeMode.UseObjectColor);
        rasterOptions.setBackgroundColor(Color.getWhite());
        rasterOptions.setLayouts(new String[]{"Model"});
        // shx字体目录
        if (FileViewerProperties.Cad.getShxFontsFolder() != null) {
            rasterOptions.setShxFonts(FileViewerProperties.Cad.getShxFontsFolder());
        }
        if (FileViewerProperties.Cad.getConvertType() == CadConvertType.PDF) {
            convertToPdfForResponse(outputStream, rasterOptions, cadImage);
        } else {
            convertToSvgForResponse(outputStream, rasterOptions, cadImage);
        }
    }

    @Override
    public List<StrategyTypeEnum> strategyTypeEnums() {
        return StrategyTypeEnum.CAD_TYPES;
    }

    /**
     * dwg转换pdf
     * @param outputStream 输出流
     * @param rasterOptions 转换选项
     * @param cadImage cad图片
     * @throws Exception 异常
     */
    public static void convertToPdfForResponse(ServletOutputStream outputStream, CadRasterizationOptions rasterOptions, CadImage cadImage) throws Exception {
        // Create an instance of PdfOptions
        PdfOptions pdfOptions = new PdfOptions();

        // Set rasterization options
        pdfOptions.setVectorRasterizationOptions(rasterOptions);
        // 将 CadImage 转换为 byteOutputStream
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        cadImage.save(byteOutputStream, pdfOptions);
        byte[] byteArray = byteOutputStream.toByteArray();
        ByteArrayInputStream pdfInputStream = new ByteArrayInputStream(byteArray);
        PdfUtil.removeWatermark(pdfInputStream, outputStream);
    }

    /**
     * dwg转换svg
     * @param outputStream 输出流
     * @param rasterOptions 转换选项
     * @param cadImage cad图片
     * @throws IOException 异常
     */
    public static void convertToSvgForResponse(ServletOutputStream outputStream, CadRasterizationOptions rasterOptions, CadImage cadImage) throws IOException {

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
