package com.wiblog.transview.poi.handler;

import com.aspose.cells.*;
import com.wiblog.transview.core.bean.TransViewProperties;
import com.wiblog.transview.core.common.Constant;
import com.wiblog.transview.core.common.ExtensionEnum;
import com.wiblog.transview.core.common.StrategyTypeEnum;
import com.wiblog.transview.core.handler.TransViewHandler;
import com.wiblog.transview.core.utils.SVGUtil;
import javax.servlet.http.HttpServletResponse;

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
    public void viewHandler(InputStream inputStream, OutputStream outputStream, String extension, HttpServletResponse response) throws Exception {
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
    public static void convertToSvgForResponse(InputStream inputStream, OutputStream outputStream) throws Exception {
        Workbook workbook = new Workbook(inputStream);
        if (TransViewProperties.View.Excel.isCalculateFormula()) {
            workbook.calculateFormula();
        }

        int sheetIndex = TransViewProperties.View.Excel.getSheetIndex();
        WorksheetCollection worksheets = workbook.getWorksheets();
        if (sheetIndex < 0 || sheetIndex >= worksheets.getCount()) {
            sheetIndex = 0;
        }
        Worksheet sheet = worksheets.get(sheetIndex);

        if (sheet.getCells().getMaxRow() == -1 && sheet.getCells().getMaxColumn() == -1) {
            handleEmptyExcel(outputStream);
            return;
        }

        // 限制渲染行列数
        int maxRows = TransViewProperties.View.Excel.getMaxRows();
        int maxCols = TransViewProperties.View.Excel.getMaxColumns();
        if (maxRows > 0 || maxCols > 0) {
            Cells cells = sheet.getCells();
            int lastRow = cells.getMaxRow();
            int lastCol = cells.getMaxColumn();
            if (maxRows > 0 && lastRow >= maxRows) {
                for (int i = maxRows; i <= lastRow; i++) {
                    cells.getRows().get(i).setHidden(true);
                }
            }
            if (maxCols > 0 && lastCol >= maxCols) {
                for (int i = maxCols; i <= lastCol; i++) {
                    cells.getColumns().get(i).setHidden(true);
                }
            }
        }

        ImageOrPrintOptions options = new ImageOrPrintOptions();
        options.setSaveFormat(SaveFormat.SVG);
        options.setOnePagePerSheet(TransViewProperties.View.Excel.isOnePagePerSheet());

        SheetRender sr = new SheetRender(sheet, options);

        if (TransViewProperties.View.isRemoveWatermark()) {
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            sr.toImage(0, byteOutputStream);
            ByteArrayInputStream svgInputStream = new ByteArrayInputStream(byteOutputStream.toByteArray());
            String transformedXml = SVGUtil.removeWatermark(svgInputStream, SVGUtil.CUT_TYPE_EXCEL);
            outputStream.write(transformedXml.getBytes(StandardCharsets.UTF_8));
        } else {
            sr.toImage(0, outputStream);
        }
    }

    private static void handleEmptyExcel(OutputStream outputStream) throws IOException {
        byte[] emptySvg = "<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100' background='#ffffff'><text x='10' y='20'>No Data</text></svg>".getBytes(StandardCharsets.UTF_8);
        outputStream.write(emptySvg);
    }
}
