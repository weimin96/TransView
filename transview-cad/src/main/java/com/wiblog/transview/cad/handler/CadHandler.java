package com.wiblog.transview.cad.handler;

import com.aspose.cad.Color;
import com.aspose.cad.Image;
import com.aspose.cad.fileformats.cad.CadDrawTypeMode;
import com.aspose.cad.fileformats.cad.CadImage;
import com.aspose.cad.imageoptions.CadRasterizationOptions;
import com.aspose.cad.imageoptions.PdfOptions;
import com.aspose.cad.imageoptions.SvgOptions;
import com.wiblog.transview.cad.utils.PdfUtil;
import com.wiblog.transview.core.common.CadConvertType;
import com.wiblog.transview.core.common.ExtensionEnum;
import com.wiblog.transview.core.common.StrategyTypeEnum;
import com.wiblog.transview.core.bean.TransViewProperties;
import com.wiblog.transview.core.handler.TransViewHandler;
import com.wiblog.transview.core.utils.SVGUtil;
import javax.servlet.http.HttpServletResponse;

import java.io.*;
import java.util.List;

/**
 * CAD 工具类
 *
 * @author panwm
 * @since 2024/6/26 22:25
 */
public class CadHandler extends TransViewHandler {

    @Override
    public void viewHandler(InputStream inputStream, OutputStream outputStream, String extension, HttpServletResponse response) throws Exception {
        CadImage cadImage = null;
        try {
            cadImage = (CadImage) Image.load(inputStream);

            CadRasterizationOptions rasterOptions = new CadRasterizationOptions();
            rasterOptions.setPageWidth(TransViewProperties.View.Cad.getPageWidth());
            rasterOptions.setPageHeight(TransViewProperties.View.Cad.getPageHeight());
            rasterOptions.setDrawType(CadDrawTypeMode.UseObjectColor);
            rasterOptions.setBackgroundColor(Color.getWhite());
            rasterOptions.setLayouts(new String[]{TransViewProperties.View.Cad.getLayout()});
            if (TransViewProperties.View.Cad.getShxFontsFolder() != null) {
                rasterOptions.setShxFonts(TransViewProperties.View.Cad.getShxFontsFolder());
            }
            if (TransViewProperties.View.Cad.getConvertType() == CadConvertType.PDF) {
                convertToPdfForResponse(outputStream, rasterOptions, cadImage);
            } else {
                convertToSvgForResponse(outputStream, rasterOptions, cadImage);
            }
        } finally {
            if (cadImage != null) {
                cadImage.close();
            }
        }
    }

    @Override
    public void convertHandler(ExtensionEnum sourceExtensionEnum, ExtensionEnum targetExtensionEnum, InputStream inputStream, OutputStream outputStream) throws Exception {

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
    public static void convertToPdfForResponse(OutputStream outputStream, CadRasterizationOptions rasterOptions, CadImage cadImage) throws Exception {
        PdfOptions pdfOptions = new PdfOptions();
        pdfOptions.setVectorRasterizationOptions(rasterOptions);

        if (TransViewProperties.View.isRemoveWatermark()) {
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            cadImage.save(byteOutputStream, pdfOptions);
            ByteArrayInputStream pdfInputStream = new ByteArrayInputStream(byteOutputStream.toByteArray());
            PdfUtil.removeWatermark(pdfInputStream, outputStream);
        } else {
            cadImage.save(outputStream, pdfOptions);
        }
    }

    /**
     * dwg转换svg
     * @param outputStream 输出流
     * @param rasterOptions 转换选项
     * @param cadImage cad图片
     * @throws IOException 异常
     */
    public static void convertToSvgForResponse(OutputStream outputStream, CadRasterizationOptions rasterOptions, CadImage cadImage) throws IOException {
        SvgOptions svgOptions = new SvgOptions();
        svgOptions.setVectorRasterizationOptions(rasterOptions);

        if (TransViewProperties.View.isRemoveWatermark()) {
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            cadImage.save(byteOutputStream, svgOptions);
            ByteArrayInputStream svgInputStream = new ByteArrayInputStream(byteOutputStream.toByteArray());
            String transformedXml = SVGUtil.removeWatermark(svgInputStream, SVGUtil.CUT_TYPE_CAD);
            outputStream.write(transformedXml.getBytes());
        } else {
            cadImage.save(outputStream, svgOptions);
        }
    }
}
