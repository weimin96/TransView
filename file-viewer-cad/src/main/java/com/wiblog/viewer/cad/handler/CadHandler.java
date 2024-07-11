package com.wiblog.viewer.cad.handler;

import com.aspose.cad.Color;
import com.aspose.cad.Image;
import com.aspose.cad.fileformats.cad.CadImage;
import com.aspose.cad.imageoptions.CadRasterizationOptions;
import com.aspose.cad.imageoptions.PdfOptions;
import com.aspose.cad.imageoptions.SvgOptions;
import com.wiblog.viewer.cad.utils.PdfUtil;
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
        convertToPdfForResponse(inputStream, outputStream);
    }

    @Override
    public List<StrategyTypeEnum> strategyTypeEnums() {
        return StrategyTypeEnum.CAD_TYPES;
    }

    /**
     * dwg转换pdf
     *
     * @param inputStream  输入流
     * @param outputStream 输出流
     * @throws IOException 异常
     */
    public static void convertToPdfForResponse(InputStream inputStream, ServletOutputStream outputStream) throws Exception {
        CadImage cadImage = (CadImage) Image.load(inputStream);


        if (FileViewerProperties.getTimeout() != 0) {
            handlerExecutor(cadImage, outputStream);
            return;
        }
        // Create an instance of PdfOptions
        PdfOptions pdfOptions = new PdfOptions();

        // 设置转换选项
        CadRasterizationOptions rasterizationOptions = new CadRasterizationOptions();
        rasterizationOptions.setPageWidth(1600);
        rasterizationOptions.setPageHeight(1600);
        rasterizationOptions.setLayouts(new String[]{"Model"});
        // shx字体目录
        if (FileViewerProperties.getShxFontsFolder() != null) {
            rasterizationOptions.setShxFonts(FileViewerProperties.getShxFontsFolder());
        }
        // Set rasterization options
        pdfOptions.setVectorRasterizationOptions(rasterizationOptions);
        // 将 CadImage 转换为 byteOutputStream
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        cadImage.save(byteOutputStream, pdfOptions);
        byte[] byteArray = byteOutputStream.toByteArray();
        ByteArrayInputStream pdfInputStream = new ByteArrayInputStream(byteArray);
        PdfUtil.removeWatermark(pdfInputStream, outputStream);
    }

    public static void handlerExecutor(CadImage cadImage, ServletOutputStream outputStream) throws Exception {
        // Set up a thread pool with a single thread
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Callable task for CAD to PDF conversion
        Callable<Void> conversionTask = () -> {
            // Create an instance of PdfOptions
            PdfOptions pdfOptions = new PdfOptions();

            // 设置转换选项
            CadRasterizationOptions rasterizationOptions = new CadRasterizationOptions();
            rasterizationOptions.setPageWidth(1600);
            rasterizationOptions.setPageHeight(1600);
            rasterizationOptions.setLayouts(new String[]{"Model"});
            // shx字体目录
            if (FileViewerProperties.getShxFontsFolder() != null) {
                rasterizationOptions.setShxFonts(FileViewerProperties.getShxFontsFolder());
            }
            // Set rasterization options
            pdfOptions.setVectorRasterizationOptions(rasterizationOptions);
            // 将 CadImage 转换为 byteOutputStream
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            try {
                cadImage.save(byteOutputStream, pdfOptions);
                byte[] byteArray = byteOutputStream.toByteArray();
                ByteArrayInputStream pdfInputStream = new ByteArrayInputStream(byteArray);
                PdfUtil.removeWatermark(pdfInputStream, outputStream);
                return null;
            } catch (Exception ignore) {
                return null;
            }
        };

        // Submit the task and get a Future object
        Future<Void> future = executor.submit(conversionTask);

        try {
            future.get(FileViewerProperties.getTimeout(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException();
        } finally {
            executor.shutdown();
        }
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
        // shx字体目录
        if (FileViewerProperties.getShxFontsFolder() != null) {
            rasterOptions.setShxFonts(FileViewerProperties.getShxFontsFolder());
        }

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
