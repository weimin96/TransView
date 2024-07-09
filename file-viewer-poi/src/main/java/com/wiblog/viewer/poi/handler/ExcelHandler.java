package com.wiblog.viewer.poi.handler;

import com.aspose.cells.*;
import com.wiblog.viewer.core.common.Constant;
import com.wiblog.viewer.core.common.StrategyTypeEnum;
import com.wiblog.viewer.core.handler.ViewerHandler;
import com.wiblog.viewer.core.utils.SVGUtil;
import com.wiblog.viewer.core.utils.Util;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * excel 处理工具
 *
 * @author panwm
 * @since 2024/7/7 20:40
 */
public class ExcelHandler extends ViewerHandler {

    @Override
    public List<StrategyTypeEnum> strategyTypeEnums() {
        return StrategyTypeEnum.EXCEL_TYPES;
    }

    @Override
    public void handler(InputStream inputStream, ServletOutputStream outputStream, String extension) throws Exception {
        if (StrategyTypeEnum.XLS.getType().equals(extension) || StrategyTypeEnum.XLSX.getType().equals(extension)) {
            convertToSvgForResponse(inputStream, outputStream);
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
    public static void convertToSvgForResponse(InputStream inputStream, ServletOutputStream outputStream) throws Exception {
        // 加载Excel文件
        Workbook workbook = new Workbook(inputStream);
        // 计算所有公式
        workbook.calculateFormula();
        // 获取当前工作表
        Worksheet sheet = workbook.getWorksheets().get(0);

        // 检查工作表是否为空
        if (sheet.getCells().getMaxRow() == -1 && sheet.getCells().getMaxColumn() == -1) {
            handleEmptyExcel(Util.getResponse());
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
