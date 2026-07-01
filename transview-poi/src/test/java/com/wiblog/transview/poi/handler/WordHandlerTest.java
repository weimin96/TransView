package com.wiblog.transview.poi.handler;

import com.aspose.words.*;
import com.wiblog.transview.core.bean.TransViewProperties;
import com.wiblog.transview.core.common.ExtensionEnum;
import com.wiblog.transview.core.common.StrategyTypeEnum;
import com.wiblog.transview.core.common.WordConvertType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class WordHandlerTest {

    private String originalFontsFolder;
    private WordConvertType originalConvertType;
    private String originalLicensePath;

    @Before
    public void setUp() {
        originalFontsFolder = TransViewProperties.View.getFontsFolder();
        originalConvertType = TransViewProperties.View.Word.getConvertType();
        originalLicensePath = TransViewProperties.View.Word.getLicensePath();
        TransViewProperties.View.setFontsFolder(null);
        TransViewProperties.View.Word.setConvertType(WordConvertType.PDF);
        TransViewProperties.View.Word.setLicensePath(null);
    }

    @After
    public void tearDown() {
        TransViewProperties.View.setFontsFolder(originalFontsFolder);
        TransViewProperties.View.Word.setConvertType(originalConvertType);
        TransViewProperties.View.Word.setLicensePath(originalLicensePath);
    }

    @Test
    public void convertToSvgForResponseShouldSupportDocx() throws Exception {
        Document document = new Document();
        DocumentBuilder builder = new DocumentBuilder(document);
        builder.write("中文Word预览");

        ByteArrayOutputStream docxOutput = new ByteArrayOutputStream();
        document.save(docxOutput, SaveFormat.DOCX);

        ByteArrayOutputStream svgOutput = new ByteArrayOutputStream();
        WordHandler.convertToSvgForResponse(new ByteArrayInputStream(docxOutput.toByteArray()), svgOutput);

        assertThat(new String(svgOutput.toByteArray(), StandardCharsets.UTF_8))
                .contains("中", "文", "W", "o", "r", "d", "预", "览")
                .doesNotContain("\uFFFD");
    }

    @Test
    public void viewHandlerShouldUseDefaultPdfPreviewType() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new WordHandler().viewHandler(new ByteArrayInputStream(createDocxBytes()), outputStream, "docx");

        assertThat(outputStream.toByteArray()).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
        assertThat(StrategyTypeEnum.getMediaType("docx")).isEqualTo("application/pdf");
    }

    @Test
    public void viewHandlerShouldRouteWpsAsWordDocument() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new WordHandler().viewHandler(new ByteArrayInputStream(createDocxBytes()), outputStream, "wps");

        assertThat(outputStream.toByteArray()).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
        assertThat(StrategyTypeEnum.getMediaType("wps")).isEqualTo("application/pdf");
    }

    @Test
    public void viewHandlerShouldUseConfiguredSvgPreviewType() throws Exception {
        TransViewProperties.View.Word.setConvertType(WordConvertType.SVG);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new WordHandler().viewHandler(new ByteArrayInputStream(createDocxBytes()), outputStream, "docx");

        assertThat(new String(outputStream.toByteArray(), StandardCharsets.UTF_8))
                .contains("<svg")
                .doesNotContain("\uFFFD");
        assertThat(StrategyTypeEnum.getMediaType("docx")).isEqualTo("image/svg+xml");
    }

    @Test
    public void convertHandlerShouldUseTargetExtension() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        new WordHandler().convertHandler(ExtensionEnum.DOCX, ExtensionEnum.PDF,
                new ByteArrayInputStream(createDocxBytes()), outputStream);

        assertThat(outputStream.toByteArray()).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    public void convertHandlerShouldAcceptWpsSourceExtension() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        new WordHandler().convertHandler(ExtensionEnum.WPS, ExtensionEnum.PDF,
                new ByteArrayInputStream(createDocxBytes()), outputStream);

        assertThat(outputStream.toByteArray()).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    public void convertToSvgForResponseShouldFailWhenConfiguredFontsFolderDoesNotExist() {
        String missingFontsFolder = new File(System.getProperty("java.io.tmpdir"),
                "transview-missing-word-fonts-" + System.nanoTime()).getAbsolutePath();
        TransViewProperties.View.setFontsFolder(missingFontsFolder);

        assertThatThrownBy(() -> WordHandler.convertToSvgForResponse(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("字体目录不存在或不是目录");
    }

    @Test
    public void convertToPdfForResponseShouldFailWhenConfiguredLicenseDoesNotExist() {
        String missingLicensePath = new File(System.getProperty("java.io.tmpdir"),
                "transview-missing-word-license-" + System.nanoTime() + ".xml").getAbsolutePath();
        TransViewProperties.View.Word.setLicensePath(missingLicensePath);

        assertThatThrownBy(() -> WordHandler.convertToPdfForResponse(new ByteArrayInputStream(createDocxBytes()), new ByteArrayOutputStream()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Aspose.Words license 加载失败");
    }

    private static byte[] createDocxBytes() throws Exception {
        Document document = new Document();
        DocumentBuilder builder = new DocumentBuilder(document);
        builder.write("中文Word预览");

        ByteArrayOutputStream docxOutput = new ByteArrayOutputStream();
        document.save(docxOutput, SaveFormat.DOCX);
        return docxOutput.toByteArray();
    }
}
