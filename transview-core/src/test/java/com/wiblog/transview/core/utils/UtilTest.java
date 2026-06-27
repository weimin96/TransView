package com.wiblog.transview.core.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UtilTest {

    @Test
    public void getExtensionReturnsLowercase() {
        assertThat(Util.getExtension("file.DWG")).isEqualTo("dwg");
        assertThat(Util.getExtension("file.PDF")).isEqualTo("pdf");
    }

    @Test
    public void getExtensionReturnsNullForNoDot() {
        assertThat(Util.getExtension("filename")).isNull();
    }

    @Test
    public void getExtensionReturnsNullForDotAtEnd() {
        assertThat(Util.getExtension("file.")).isNull();
    }

    @Test
    public void getExtensionReturnsNullForNull() {
        assertThat(Util.getExtension(null)).isNull();
    }

    @Test
    public void getExtensionHandlesMultipleDots() {
        assertThat(Util.getExtension("archive.tar.gz")).isEqualTo("gz");
    }

    @Test
    public void getExtensionOrFilenameReturnsExtension() {
        assertThat(Util.getExtensionOrFilename("file.txt")).isEqualTo("txt");
    }

    @Test
    public void getExtensionOrFilenameReturnsFilenameWhenNoExtension() {
        assertThat(Util.getExtensionOrFilename("Makefile")).isEqualTo("Makefile");
    }

    @Test
    public void isBlankReturnsTrueForNull() {
        assertThat(Util.isBlank(null)).isTrue();
    }

    @Test
    public void isBlankReturnsTrueForEmpty() {
        assertThat(Util.isBlank("")).isTrue();
    }

    @Test
    public void isBlankReturnsTrueForWhitespace() {
        assertThat(Util.isBlank("   ")).isTrue();
    }

    @Test
    public void isBlankReturnsFalseForContent() {
        assertThat(Util.isBlank("abc")).isFalse();
    }
}
