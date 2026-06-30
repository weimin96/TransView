package com.wiblog.transview.servlet.jakarta;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.wiblog.transview.core.common.ExtensionEnum;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransViewContextTest {

    private static final byte[] PNG_SIGNATURE = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

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

    @Test
    void convertInputStreamWritesToOutputStream() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"><rect width=\"10\" height=\"10\" fill=\"red\"/></svg>"
                .getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        TransViewContext.convert(new ByteArrayInputStream(svg), "sample.svg", ExtensionEnum.PNG, outputStream);

        assertThat(outputStream.toByteArray()).startsWith(PNG_SIGNATURE);
    }

    @Test
    void previewOutputStreamDetectsActualContentTypeBeforeWriting() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        OutputStream outputStream = createPreviewOutputStream(response);

        ContentTypeProbe.mark("image/png");
        outputStream.write('a');

        assertThat(response.getContentType()).isEqualTo("image/png");
        assertThat(response.getContentAsString()).isEqualTo("a");
    }

    private static OutputStream createPreviewOutputStream(MockHttpServletResponse response) throws Exception {
        Method method = TransViewContext.class.getDeclaredMethod("previewOutputStream", jakarta.servlet.http.HttpServletResponse.class);
        method.setAccessible(true);
        return (OutputStream) method.invoke(null, response);
    }

    private static class ContentTypeProbe extends com.wiblog.transview.core.handler.TransViewHandler {
        static void mark(String contentType) {
            setOutputContentType(contentType);
        }

        @Override
        public void convertHandler(ExtensionEnum sourceExtensionEnum, ExtensionEnum targetExtensionEnum, InputStream inputStream, OutputStream outputStream) {
        }

        @Override
        public List<com.wiblog.transview.core.common.StrategyTypeEnum> strategyTypeEnums() {
            return Collections.emptyList();
        }

        @Override
        public void viewHandler(InputStream inputStream, OutputStream outputStream, String extension) {
        }
    }
}
