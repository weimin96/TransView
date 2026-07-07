package com.wiblog.transview.core.utils;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * HTML 安全过滤工具 — 基于 Jsoup Safelist 白名单，只保留安全的展示元素。
 * <p>
 * 白名单策略（默认 Safelist.relaxed + 扩展）：
 * - 允许基本文本标签：p, br, h1-h6, ul, ol, li, pre, code, blockquote, table, tr, td, th, thead, tbody
 * - 允许内联样式标签：b, i, u, em, strong, s, sub, sup, mark, small, span, a, img
 * - 允许布局标签：div, header, footer, nav, main, section, article, aside, figure, figcaption
 * - 允许 style 属性（用于基本排版）
 * - 允许 img src: http/https/data
 * - 允许 a href: http/https/mailto
 * <p>
 * 明确移除：
 * - script, iframe, object, embed, applet, form, base, meta, link, style 标签
 * - 所有 on* 事件属性
 * - javascript:, data:text/html, vbscript: 协议
 * @author pwm
 */
public class HtmlSanitizer {

    private static final Safelist SAFE_LIST = Safelist.relaxed()
            .addTags("header", "footer", "nav", "main", "section", "article", "aside",
                    "figure", "figcaption", "mark", "small", "s", "sub", "sup")
            .addAttributes(":all", "class", "id", "style")
            .addProtocols("img", "src", "http", "https", "data")
            .addProtocols("a", "href", "http", "https", "mailto");

    private HtmlSanitizer() {
    }

    /**
     * 清洗 HTML 输入流，将安全内容写入输出流
     */
    public static void sanitize(InputStream inputStream, OutputStream outputStream) throws IOException {
        String html = new String(IOUtils.toByteArray(inputStream), StandardCharsets.UTF_8);
        String cleaned = Jsoup.clean(html, SAFE_LIST);
        outputStream.write(cleaned.getBytes(StandardCharsets.UTF_8));
    }
}
