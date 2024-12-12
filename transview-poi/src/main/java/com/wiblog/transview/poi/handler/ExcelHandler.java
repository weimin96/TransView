package com.wiblog.transview.poi.handler;

import com.aspose.cells.*;
import com.wiblog.transview.core.common.Constant;
import com.wiblog.transview.core.common.ExtensionEnum;
import com.wiblog.transview.core.common.StrategyTypeEnum;
import com.wiblog.transview.core.handler.TransViewHandler;
import com.wiblog.transview.core.utils.SVGUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * excel 处理工具
 *
 * @author panwm
 * @since 2024/7/7 20:40
 */
public class ExcelHandler extends TransViewHandler {

    @Override
    public void convertHandler(ExtensionEnum sourceExtensionEnum, ExtensionEnum targetExtensionEnum, InputStream inputStream, OutputStream outputStream) throws Exception {

    }

    @Override
    public List<StrategyTypeEnum> strategyTypeEnums() {
        return StrategyTypeEnum.EXCEL_TYPES;
    }

    @Override
    public void viewHandler(InputStream inputStream, ServletOutputStream outputStream, String extension, HttpServletResponse response) throws Exception {
        if (StrategyTypeEnum.XLS.getType().equals(extension) || StrategyTypeEnum.XLSX.getType().equals(extension)) {
            convertToSvgForResponse(inputStream, outputStream, response);
        } else {
            throw new IllegalArgumentException(Constant.ERROR_MSG_ILLEGAL_TYPE + ":" + extension);
        }
    }

    /**
     * dwg转换svg
     *
     * @param inputStream  输入流
     * @param outputStream 输出流
     * @throws IOException 异常
     */
    public static void convertToSvgForResponse(InputStream inputStream, ServletOutputStream outputStream, HttpServletResponse response) throws Exception {
        // 加载Excel文件
        Workbook workbook = new Workbook(inputStream);
        // 计算所有公式
        workbook.calculateFormula();
        // 获取当前工作表
        Worksheet sheet = workbook.getWorksheets().get(0);

        // 检查工作表是否为空
        if (sheet.getCells().getMaxRow() == -1 && sheet.getCells().getMaxColumn() == -1) {
            handleEmptyExcel(response);
            return;
        }

        // 创建图像或打印选项
        ImageOrPrintOptions options = new ImageOrPrintOptions();
        options.setSaveFormat(SaveFormat.SVG);
        options.setOnePagePerSheet(true);

        // 渲染工作表为图像
        SheetRender sr = new SheetRender(sheet, options);
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        sr.toImage(0, byteOutputStream);


        byte[] byteArray = byteOutputStream.toByteArray();
        ByteArrayInputStream svgInputStream = new ByteArrayInputStream(byteArray);

        String transformedXml = SVGUtil.removeWatermark(svgInputStream, SVGUtil.CUT_TYPE_EXCEL);
        outputStream.write(transformedXml.getBytes());
    }

    private static void handleEmptyExcel(HttpServletResponse response) throws IOException {
        String emptySvg = "<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100' background='#ffffff'><text x='10' y='20'>No Data</text></svg>";
        response.setContentType("image/svg+xml");
        response.setContentLength(emptySvg.length());
        response.getOutputStream().write(emptySvg.getBytes(StandardCharsets.UTF_8));
    }
}
