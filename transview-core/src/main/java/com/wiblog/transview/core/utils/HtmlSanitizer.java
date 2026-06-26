package com.wiblog.transview.core.utils;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * HTML 安全过滤工具 — 移除脚本、事件处理器等危险内容，保留基本展示元素。
 */
public class HtmlSanitizer {

    private HtmlSanitizer() {
    }

    /**
     * 清洗 HTML 输入流，将安全内容写入输出流
     */
    public static void sanitize(InputStream inputStream, OutputStream outputStream) throws IOException {
        String html = new String(IOUtils.toByteArray(inputStream), StandardCharsets.UTF_8);
        Document document = Jsoup.parse(html);
        document.select("script, iframe, object, embed, applet, meta[http-equiv=refresh]").remove();
        document.select("*").forEach(el -> {
            el.attributes().asList().stream()
                    .filter(attr -> attr.getKey().startsWith("on"))
                    .map(org.jsoup.nodes.Attribute::getKey)
                    .collect(java.util.stream.Collectors.toList())
                    .forEach(el::removeAttr);
        });
        String cleaned = document.outerHtml();
        outputStream.write(cleaned.getBytes(StandardCharsets.UTF_8));
    }
}
