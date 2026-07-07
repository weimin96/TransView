package com.wiblog.transview.cad.handler;

import com.aspose.cad.Image;
import com.aspose.cad.fileformats.cad.CadDrawTypeMode;
import com.aspose.cad.fileformats.cad.CadImage;
import com.aspose.cad.imageoptions.CadRasterizationOptions;
import com.aspose.cad.imageoptions.PdfOptions;
import com.aspose.cad.imageoptions.SvgOptions;
import com.wiblog.transview.core.bean.TransViewProperties;
import com.wiblog.transview.core.context.TransViewContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

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
}
