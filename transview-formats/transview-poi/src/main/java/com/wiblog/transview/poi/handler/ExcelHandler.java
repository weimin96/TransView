package com.wiblog.transview.poi.handler;

import com.aspose.cells.*;
import com.wiblog.transview.core.bean.TransViewProperties;
import com.wiblog.transview.core.common.Constant;
import com.wiblog.transview.core.common.ExtensionEnum;
import com.wiblog.transview.core.common.StrategyTypeEnum;
import com.wiblog.transview.core.handler.TransViewHandler;
import com.wiblog.transview.core.utils.LicenseUtil;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * excel 处理工具
 *
 * @author panwm
 * @since 2024/7/7 20:40
 */
public class ExcelHandler extends TransViewHandler {

    private static final Object LICENSE_LOCK = new Object();

    private static volatile boolean licenseLoaded;

    private static volatile String loadedLicensePath;

    @Override
    public void convertHandler(ExtensionEnum sourceExtensionEnum, ExtensionEnum targetExtensionEnum, InputStream inputStream, OutputStream outputStream) throws Exception {
        if (sourceExtensionEnum != ExtensionEnum.XLS && sourceExtensionEnum != ExtensionEnum.XLSX) {
            throw new UnsupportedOperationException("不支持 " + sourceExtensionEnum.getValue() + " 输入格式");
        }
        if (targetExtensionEnum == ExtensionEnum.SVG) {
            convertToSvgForResponse(inputStream, outputStream);
            return;
        }
        if (targetExtensionEnum == ExtensionEnum.PDF) {
            convertToPdf(inputStream, outputStream);
            return;
        }
        throw new IllegalArgumentException(Constant.ERROR_MSG_ILLEGAL_TYPE + ":" + targetExtensionEnum.getValue());
    }

    @Override
    public List<StrategyTypeEnum> strategyTypeEnums() {
        return StrategyTypeEnum.EXCEL_TYPES;
    }

    @Override
    public void viewHandler(InputStream inputStream, OutputStream outputStream, String extension) throws Exception {
        if (StrategyTypeEnum.XLS.getType().equals(extension) || StrategyTypeEnum.XLSX.getType().equals(extension)) {
            convertToSvgForResponse(inputStream, outputStream);
        } else {
            throw new IllegalArgumentException(Constant.ERROR_MSG_ILLEGAL_TYPE + ":" + extension);
        }
    }

    /**
     * excel转换svg
     *
     * @param inputStream  输入流
     * @param outputStream 输出流
     * @throws Exception 异常
     */
    public static void convertToSvgForResponse(InputStream inputStream, OutputStream outputStream) throws Exception {
        loadLicense();
        Workbook workbook = loadWorkbook(inputStream);
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
        sr.toImage(0, outputStream);
    }

    static Workbook loadWorkbook(InputStream inputStream) throws Exception {
        byte[] inputBytes = toByteArray(inputStream);
        String fontsFolder = TransViewProperties.View.getFontsFolder();
        LoadOptions loadOptions = createLoadOptions(inputBytes);
        if (fontsFolder == null || fontsFolder.trim().isEmpty()) {
            return new Workbook(new ByteArrayInputStream(inputBytes), loadOptions);
        }

        File fontDir = new File(fontsFolder.trim());
        if (!fontDir.isDirectory()) {
            throw new IllegalArgumentException("字体目录不存在或不是目录: " + fontsFolder);
        }

        IndividualFontConfigs fontConfigs = new IndividualFontConfigs();
        fontConfigs.setFontFolder(fontDir.getAbsolutePath(), true);
        loadOptions.setFontConfigs(fontConfigs);
        return new Workbook(new ByteArrayInputStream(inputBytes), loadOptions);
    }

    private static LoadOptions createLoadOptions(byte[] inputBytes) throws Exception {
        FileFormatInfo formatInfo = FileFormatUtil.detectFileFormat(new ByteArrayInputStream(inputBytes));
        if (formatInfo.getLoadFormat() != LoadFormat.UNKNOWN) {
            return new LoadOptions();
        }
        if (!isDelimitedText(inputBytes)) {
            return new LoadOptions();
        }

        TxtLoadOptions loadOptions = new TxtLoadOptions(LoadFormat.TSV);
        loadOptions.setSeparator(containsByte(inputBytes, (byte) '\t') ? '\t' : ',');
        loadOptions.setEncoding(Encoding.getEncoding(detectTextCharset(inputBytes)));
        return loadOptions;
    }

    private static boolean isDelimitedText(byte[] inputBytes) {
        if (inputBytes.length == 0 || containsByte(inputBytes, (byte) 0)) {
            return false;
        }
        return (containsByte(inputBytes, (byte) '\t') || containsByte(inputBytes, (byte) ','))
                && (containsByte(inputBytes, (byte) '\n') || containsByte(inputBytes, (byte) '\r'));
    }

    private static boolean containsByte(byte[] inputBytes, byte value) {
        for (byte inputByte : inputBytes) {
            if (inputByte == value) {
                return true;
            }
        }
        return false;
    }

    private static Charset detectTextCharset(byte[] inputBytes) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(inputBytes));
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException e) {
            return Charset.forName("GBK");
        }
    }

    private static byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
        return outputStream.toByteArray();
    }

    /**
     * excel转换pdf
     */
    public static void convertToPdf(InputStream inputStream, OutputStream outputStream) throws Exception {
        loadLicense();
        Workbook workbook = loadWorkbook(inputStream);
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

        PdfSaveOptions options = new PdfSaveOptions();
        options.setOnePagePerSheet(TransViewProperties.View.Excel.isOnePagePerSheet());

        workbook.save(outputStream, options);
    }

    private static void handleEmptyExcel(OutputStream outputStream) throws IOException {
        byte[] emptySvg = "<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100' background='#ffffff'><text x='10' y='20'>No Data</text></svg>".getBytes(StandardCharsets.UTF_8);
        outputStream.write(emptySvg);
    }

    private static void loadLicense() {
        String licensePath = LicenseUtil.resolvePath(TransViewProperties.View.Excel.getLicensePath());
        if (licensePath == null) {
            if (TransViewProperties.View.isRemoveWatermark()) {
                removeWatermark();
            }
            return;
        }
        if (licenseLoaded && licensePath.equals(loadedLicensePath)) {
            return;
        }
        synchronized (LICENSE_LOCK) {
            if (licenseLoaded && licensePath.equals(loadedLicensePath)) {
                return;
            }
            InputStream licenseStream;
            try {
                licenseStream = LicenseUtil.openStream(licensePath);
            } catch (IOException e) {
                throw new IllegalStateException("Aspose.Cells license 加载失败: " + licensePath, e);
            }
            try (InputStream inputStream = licenseStream) {
                new License().setLicense(inputStream);
                loadedLicensePath = licensePath;
                licenseLoaded = true;
            } catch (Exception e) {
                throw new IllegalStateException("Aspose.Cells license 加载失败: " + licensePath, e);
            }
        }
    }

    private static volatile boolean watermarkRemoved;

    private static void removeWatermark() {
        if (watermarkRemoved) {
            return;
        }
        synchronized (LICENSE_LOCK) {
            if (watermarkRemoved) {
                return;
            }
            try {
                Class<?> axfClass = Class.forName("com.aspose.cells.axf");
                Constructor<?> ctor = axfClass.getDeclaredConstructor();
                ctor.setAccessible(true);
                Object axfInstance = ctor.newInstance();
                Field axfStaticField = axfClass.getDeclaredField("a");
                axfStaticField.setAccessible(true);
                axfStaticField.set(null, axfInstance);

                Field licenseField = com.aspose.cells.License.class.getDeclaredField("a");
                licenseField.setAccessible(true);
                licenseField.set(null, "29991231");

                // bre.a() 根据 axf.a 是否为 null 同步 bro.a 和 bbd.a 两个内部许可标志。
                // axf.a 设为非 null 后必须调用 bre.a() 才能使 bro.a = false（非评估模式）。
                Class<?> breClass = Class.forName("com.aspose.cells.bre");
                java.lang.reflect.Method breAMethod = breClass.getDeclaredMethod("a");
                breAMethod.setAccessible(true);
                breAMethod.invoke(null);

                watermarkRemoved = true;
                licenseLoaded = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
