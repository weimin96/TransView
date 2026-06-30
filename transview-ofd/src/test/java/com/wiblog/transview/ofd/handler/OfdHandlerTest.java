package com.wiblog.transview.ofd.handler;

import com.wiblog.transview.core.common.Constant;
import com.wiblog.transview.core.common.ExtensionEnum;
import com.wiblog.transview.core.common.StrategyTypeEnum;
import org.junit.Test;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.element.Paragraph;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OfdHandlerTest {

    @Test
    public void viewHandlerShouldConvertOfdToPdf() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        new OfdHandler().viewHandler(new ByteArrayInputStream(createOfdBytes()), outputStream, "ofd");

        assertThat(outputStream.toByteArray()).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
        assertThat(StrategyTypeEnum.getMediaType("ofd")).isEqualTo("application/pdf");
    }

    @Test
    public void convertHandlerShouldSupportOfdToPdf() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        new OfdHandler().convertHandler(ExtensionEnum.OFD, ExtensionEnum.PDF,
                new ByteArrayInputStream(createOfdBytes()), outputStream);

        assertThat(outputStream.toByteArray()).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    public void convertHandlerShouldRejectUnsupportedTargetType() {
        assertThatThrownBy(() -> new OfdHandler().convertHandler(ExtensionEnum.OFD, ExtensionEnum.SVG,
                new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(Constant.ERROR_MSG_ILLEGAL_TYPE + ":svg");
    }

    private static byte[] createOfdBytes() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (OFDDoc ofdDoc = new OFDDoc(outputStream)) {
            ofdDoc.add(new Paragraph("中文OFD预览"));
        }
        return outputStream.toByteArray();
    }
}
