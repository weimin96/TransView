package com.wiblog.transview.servlet.jakarta;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

class TransViewContextTest {

    @Test
    void previewFileSetsCacheHeadersAndContentLength() throws Exception {
        File file = File.createTempFile("transview-plain-", ".txt");
        try {
            Files.write(file.toPath(), "hello".getBytes(StandardCharsets.UTF_8));
            MockHttpServletResponse response = new MockHttpServletResponse();

            TransViewContext.preview(file, response);

            assertThat(response.getContentLength()).isEqualTo((int) file.length());
            assertThat(response.getHeader("Cache-Control")).isEqualTo("public, max-age=3600");
            assertThat(response.getHeader("Last-Modified")).isNotBlank();
            assertThat(response.getHeader("ETag")).isEqualTo("W/\"" + file.length() + "-" + file.lastModified() + "\"");
            assertThat(response.getContentAsString()).isEqualTo("hello");
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test
    void previewFileReturns304WhenEtagMatches() throws Exception {
        File file = File.createTempFile("transview-plain-", ".txt");
        try {
            Files.write(file.toPath(), "hello".getBytes(StandardCharsets.UTF_8));
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("If-None-Match", "W/\"" + file.length() + "-" + file.lastModified() + "\"");
            MockHttpServletResponse response = new MockHttpServletResponse();

            TransViewContext.preview(file, request, response);

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_MODIFIED);
            assertThat(response.getContentAsByteArray()).isEmpty();
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test
    void previewFileReturns416ForInvalidRange() throws Exception {
        File file = File.createTempFile("transview-plain-", ".txt");
        try {
            Files.write(file.toPath(), "hello".getBytes(StandardCharsets.UTF_8));
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Range", "items=0-1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            TransViewContext.preview(file, request, response);

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            assertThat(response.getHeader("Content-Range")).isEqualTo("bytes */" + file.length());
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }
}
