package com.wiblog.transview.cad.handler;

import com.aspose.cad.Image;
import com.aspose.cad.fileformats.cad.CadDrawTypeMode;
import com.aspose.cad.fileformats.cad.CadImage;
import com.aspose.cad.imageoptions.CadRasterizationOptions;
import com.wiblog.transview.core.bean.TransViewProperties;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CadHandlerTest {

    private String originalLicensePath;

    @Before
    public void setUp() {
        originalLicensePath = TransViewProperties.View.Cad.getLicensePath();
        TransViewProperties.View.Cad.setLicensePath(null);
        TransViewProperties.View.setRemoveWatermark(true);
    }

    @After
    public void tearDown() {
        TransViewProperties.View.Cad.setLicensePath(originalLicensePath);
    }

    @Test
    public void convertToPdf() throws Exception {
        File srcFile = new File("C:\\Users\\pwm\\Desktop\\data\\a.dwg");
        File destFile = new File("C:\\Users\\pwm\\Desktop\\data\\a_cad.pdf");
        CadImage cadImage = (CadImage) Image.load(srcFile.getAbsolutePath());
        CadRasterizationOptions options = rasterOptions(cadImage);
        try (FileOutputStream outputStream = new FileOutputStream(destFile)) {
            CadHandler.convertToPdf(outputStream, options, cadImage);
        } finally {
            cadImage.close();
        }
        assertThat(destFile).exists();
        assertThat(destFile.length()).isGreaterThan(0L);
        assertPdfHasNoAsposeWatermark(destFile);
        assertPdfHasNoTrailingWatermarkBox(destFile);
    }

    @Test
    public void convertToSvg() throws Exception {
        File srcFile = new File("C:\\Users\\pwm\\Desktop\\data\\a.dwg");
        File destFile = new File("C:\\Users\\pwm\\Desktop\\data\\a_cad.svg");
        CadImage cadImage = (CadImage) Image.load(srcFile.getAbsolutePath());
        CadRasterizationOptions options = rasterOptions(cadImage);
        try (FileOutputStream outputStream = new FileOutputStream(destFile)) {
            CadHandler.convertToSvg(outputStream, options, cadImage);
        } finally {
            cadImage.close();
        }
        assertThat(destFile).exists();
        assertThat(destFile.length()).isGreaterThan(0L);
    }

    private static CadRasterizationOptions rasterOptions(CadImage cadImage) {
        CadRasterizationOptions options = new CadRasterizationOptions();
        options.setDrawType(CadDrawTypeMode.UseObjectColor);
        options.setPageWidth(cadImage.getWidth());
        options.setPageHeight(cadImage.getHeight());
        return options;
    }

    private static void assertPdfHasNoAsposeWatermark(File pdfFile) throws IOException {
        String pdfText = extractPdfText(pdfFile);
        assertThat(pdfText).doesNotContain("Evaluation Only");
        assertThat(pdfText).doesNotContain("Created with Aspose.CAD");
    }

    private static String extractPdfText(File pdfFile) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static void assertPdfHasNoTrailingWatermarkBox(File pdfFile) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            document.getPages().forEach(page -> {
                try {
                    Iterator<PDStream> contentStreams = page.getContentStreams();
                    while (contentStreams.hasNext()) {
                        PDFStreamParser parser = new PDFStreamParser(contentStreams.next().getCOSObject());
                        parser.parse();
                        assertThat(hasTrailingStrokePathAfterLastText(parser.getTokens())).isFalse();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static boolean hasTrailingStrokePathAfterLastText(List<Object> tokens) {
        int lastTextEndOperator = findLastOperator(tokens, "ET");
        if (lastTextEndOperator < 0 || lastTextEndOperator >= tokens.size() - 1) {
            return false;
        }
        Object first = tokens.get(lastTextEndOperator + 1);
        Object last = tokens.get(tokens.size() - 1);
        if (!(first instanceof Operator) || !((Operator) first).getName().equals("q")) {
            return false;
        }
        if (!(last instanceof Operator) || !((Operator) last).getName().equals("Q")) {
            return false;
        }
        boolean hasMove = false;
        boolean hasStroke = false;
        for (int i = lastTextEndOperator + 2; i < tokens.size() - 1; i++) {
            Object token = tokens.get(i);
            if (token instanceof Operator) {
                String name = ((Operator) token).getName();
                hasMove = hasMove || name.equals("m");
                hasStroke = hasStroke || name.equals("s") || name.equals("S");
            }
        }
        return hasMove && hasStroke;
    }

    private static int findLastOperator(List<Object> tokens, String operatorName) {
        for (int i = tokens.size() - 1; i >= 0; i--) {
            Object token = tokens.get(i);
            if (token instanceof Operator && ((Operator) token).getName().equals(operatorName)) {
                return i;
            }
        }
        return -1;
    }
}
