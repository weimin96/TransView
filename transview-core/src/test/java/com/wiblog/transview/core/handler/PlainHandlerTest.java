package com.wiblog.transview.core.handler;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

class PlainHandlerTest {

    @Test
    void previewFileSetsValidCacheHeadersAndContentLength() throws Exception {
        File file = File.createTempFile("transview-plain-", ".txt");
        try {
            Files.write(file.toPath(), "preview".getBytes(StandardCharsets.UTF_8));
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            new PlainHandler().preview(file, request, response);

            assertThat(response.getContentLength()).isEqualTo(file.length());
            assertThat(response.getHeader("Cache-Control")).isEqualTo("public, max-age=3600");
            assertThat(response.getHeader("Last-Modified")).isNotBlank();
            assertThat(response.getHeader("ETag")).isEqualTo("W/\"" + file.length() + "-" + file.lastModified() + "\"");
            assertThat(response.getContentAsString()).isEqualTo("preview");
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }
}
