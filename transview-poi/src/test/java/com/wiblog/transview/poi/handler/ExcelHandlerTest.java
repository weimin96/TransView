package com.wiblog.transview.poi.handler;

import com.aspose.cells.SaveFormat;
import com.aspose.cells.Workbook;
import com.wiblog.transview.core.bean.TransViewProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExcelHandlerTest {

    private String originalFontsFolder;
    private String originalLicensePath;

    @Before
    public void setUp() {
        originalFontsFolder = TransViewProperties.View.getFontsFolder();
        originalLicensePath = TransViewProperties.View.Excel.getLicensePath();
        TransViewProperties.View.setFontsFolder(null);
        TransViewProperties.View.Excel.setLicensePath(null);
    }

    @After
    public void tearDown() {
        TransViewProperties.View.setFontsFolder(originalFontsFolder);
        TransViewProperties.View.Excel.setLicensePath(originalLicensePath);
    }

    @Test
    public void convertToSvgForResponseShouldKeepChineseText() throws Exception {
        Workbook workbook = new Workbook();
        workbook.getWorksheets().get(0).getCells().get("A1").putValue("中文预览");

        ByteArrayOutputStream excelOutput = new ByteArrayOutputStream();
        workbook.save(excelOutput, SaveFormat.XLSX);

        ByteArrayOutputStream svgOutput = new ByteArrayOutputStream();
        ExcelHandler.convertToSvgForResponse(new ByteArrayInputStream(excelOutput.toByteArray()), svgOutput);

        assertThat(new String(svgOutput.toByteArray(), StandardCharsets.UTF_8))
                .contains("中", "文", "预", "览")
                .doesNotContain("\uFFFD");
    }

    @Test
    public void convertToSvgForResponseShouldSupportGbkTabDelimitedContentWithExcelExtension() throws Exception {
        byte[] inputBytes = ("温热\t有预热\t有人特\t也让他也\r\n"
                + "太热\t\t\t\r\n"
                + "高温热m\t\t\t\r\n").getBytes(Charset.forName("GBK"));

        ByteArrayOutputStream svgOutput = new ByteArrayOutputStream();
        ExcelHandler.convertToSvgForResponse(new ByteArrayInputStream(inputBytes), svgOutput);

        assertThat(new String(svgOutput.toByteArray(), StandardCharsets.UTF_8))
                .contains("温", "热", "有", "预", "人", "特", "太", "高", "m")
                .doesNotContain("\uFFFD");
    }

    @Test
    public void loadWorkbookShouldFailWhenConfiguredFontsFolderDoesNotExist() {
        String missingFontsFolder = new File(System.getProperty("java.io.tmpdir"),
                "transview-missing-fonts-" + System.nanoTime()).getAbsolutePath();
        TransViewProperties.View.setFontsFolder(missingFontsFolder);

        assertThatThrownBy(() -> ExcelHandler.loadWorkbook(new ByteArrayInputStream(new byte[0])))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("字体目录不存在或不是目录");
    }

    @Test
    public void convertToSvgShouldNotContainEvaluationWatermark() throws Exception {
        Workbook workbook = new Workbook();
        workbook.getWorksheets().get(0).getCells().get("A1").putValue("test");

        ByteArrayOutputStream excelOutput = new ByteArrayOutputStream();
        workbook.save(excelOutput, SaveFormat.XLSX);

        ByteArrayOutputStream svgOutput = new ByteArrayOutputStream();
        ExcelHandler.convertToSvgForResponse(new ByteArrayInputStream(excelOutput.toByteArray()), svgOutput);

        String svgText = new String(svgOutput.toByteArray(), StandardCharsets.UTF_8);
        assertThat(svgText)
                .doesNotContain("Evaluation Only")
                .doesNotContain("Aspose.Cells")
                .doesNotContain("Copyright");
    }

    @Test
    public void convertToSvg() throws Exception {
        File srcFile = new File("C:\\Users\\pwm\\Desktop\\data\\a.xlsx");
        File destFile = new File("C:\\Users\\pwm\\Desktop\\data\\a_excel.svg");
        try (FileInputStream inputStream = new FileInputStream(srcFile);
             FileOutputStream outputStream = new FileOutputStream(destFile)) {
            ExcelHandler.convertToSvgForResponse(inputStream, outputStream);
        }
        assertThat(destFile).exists();
        assertThat(destFile.length()).isGreaterThan(0L);
    }
}
