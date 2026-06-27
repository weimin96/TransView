package com.wiblog.transview.core.utils;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class HtmlSanitizerTest {

    private String sanitize(String html) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlSanitizer.sanitize(in, out);
        return out.toString(StandardCharsets.UTF_8.name());
    }

    @Test
    public void removesScriptTags() throws IOException {
        String result = sanitize("<p>hello</p><script>alert('xss')</script>");
        assertThat(result).doesNotContain("<script>");
        assertThat(result).doesNotContain("alert");
        assertThat(result).contains("hello");
    }

    @Test
    public void removesIframeTags() throws IOException {
        String result = sanitize("<p>text</p><iframe src='http://evil.com'></iframe>");
        assertThat(result).doesNotContain("<iframe>");
        assertThat(result).contains("text");
    }

    @Test
    public void removesOnEventAttributes() throws IOException {
        String result = sanitize("<img src=x onerror='alert(1)'>");
        assertThat(result).doesNotContain("onerror");
        assertThat(result).doesNotContain("alert");
    }

    @Test
    public void removesOnclick() throws IOException {
        String result = sanitize("<div onclick='alert(1)'>click me</div>");
        assertThat(result).doesNotContain("onclick");
        assertThat(result).contains("click me");
    }

    @Test
    public void removesFormTags() throws IOException {
        String result = sanitize("<form action='http://evil.com'><input type='submit'></form>");
        assertThat(result).doesNotContain("<form>");
        assertThat(result).doesNotContain("<input");
    }

    @Test
    public void removesBaseTag() throws IOException {
        String result = sanitize("<base href='http://evil.com'><p>text</p>");
        assertThat(result).doesNotContain("<base");
        assertThat(result).contains("text");
    }

    @Test
    public void removesEmbedAndObject() throws IOException {
        String result = sanitize("<embed src='evil.swf'><object data='evil.swf'></object>");
        assertThat(result).doesNotContain("<embed");
        assertThat(result).doesNotContain("<object");
    }

    @Test
    public void preservesSafeTags() throws IOException {
        String html = "<p>para</p><b>bold</b><i>italic</i><a href='http://example.com'>link</a>";
        String result = sanitize(html);
        assertThat(result).contains("<p>");
        assertThat(result).contains("<b>");
        assertThat(result).contains("<i>");
        assertThat(result).contains("<a");
        assertThat(result).contains("http://example.com");
    }

    @Test
    public void preservesTableStructure() throws IOException {
        String html = "<table><thead><tr><th>h</th></tr></thead><tbody><tr><td>d</td></tr></tbody></table>";
        String result = sanitize(html);
        assertThat(result).contains("<table>");
        assertThat(result).contains("<th>");
        assertThat(result).contains("<td>");
    }

    @Test
    public void removesJavascriptHref() throws IOException {
        String result = sanitize("<a href='javascript:alert(1)'>click</a>");
        assertThat(result).doesNotContain("javascript:");
    }

    @Test
    public void preservesImgWithDataSrc() throws IOException {
        String result = sanitize("<img src='data:image/png;base64,abc'>");
        assertThat(result).contains("data:image/png");
    }

    @Test
    public void removesStyleTag() throws IOException {
        String result = sanitize("<style>body{background:red}</style><p>text</p>");
        assertThat(result).doesNotContain("<style>");
        assertThat(result).contains("text");
    }

    @Test
    public void removesMetaRefresh() throws IOException {
        String result = sanitize("<meta http-equiv='refresh' content='0;url=http://evil.com'><p>text</p>");
        assertThat(result).doesNotContain("<meta");
    }

    @Test
    public void handlesEmptyInput() throws IOException {
        String result = sanitize("");
        assertThat(result).isNotNull();
    }
}
