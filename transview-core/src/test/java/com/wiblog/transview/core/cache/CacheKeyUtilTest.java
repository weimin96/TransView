package com.wiblog.transview.core.cache;

import com.wiblog.transview.core.bean.TransViewProperties;
import com.wiblog.transview.core.common.CadConvertType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CacheKeyUtilTest {

    private File tempFile;

    @Before
    public void setup() throws IOException {
        tempFile = File.createTempFile("test-dwg-", ".dwg");
        try (FileWriter w = new FileWriter(tempFile)) {
            w.write("dummy dwg content for testing");
        }
        TransViewProperties.View.Cad.setConvertType(CadConvertType.SVG);
        TransViewProperties.View.Cad.setLayout("Model");
        TransViewProperties.View.Cad.setPageWidth(2549);
        TransViewProperties.View.Cad.setPageHeight(1228);
        TransViewProperties.View.Cad.setShxFontsFolder(null);
    }

    @After
    public void cleanup() {
        if (tempFile != null) {
            tempFile.delete();
        }
    }

    @Test
    public void generateCadCacheKeyReturnsNonNull() {
        String key = CacheKeyUtil.generateCadCacheKey(tempFile);
        assertThat(key).isNotNull();
        assertThat(key).isNotEmpty();
    }

    @Test
    public void generateCadCacheKeyIsStable() {
        String key1 = CacheKeyUtil.generateCadCacheKey(tempFile);
        String key2 = CacheKeyUtil.generateCadCacheKey(tempFile);
        assertThat(key1).isEqualTo(key2);
    }

    @Test
    public void generateCadCacheKeyIsStableForSameContentInDifferentFiles() throws IOException {
        File anotherFile = File.createTempFile("test-dwg-copy-", ".dwg");
        try {
            try (FileWriter w = new FileWriter(anotherFile)) {
                w.write("dummy dwg content for testing");
            }

            String key1 = CacheKeyUtil.generateCadCacheKey(tempFile);
            String key2 = CacheKeyUtil.generateCadCacheKey(anotherFile);

            assertThat(key1).isEqualTo(key2);
        } finally {
            anotherFile.delete();
        }
    }

    @Test
    public void generateCadCacheKeyDiffersForDifferentLayouts() {
        String key1 = CacheKeyUtil.generateCadCacheKey(tempFile, "Model");
        String key2 = CacheKeyUtil.generateCadCacheKey(tempFile, "Layout1");
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    public void generateCadCacheKeyDiffersForDifferentConvertType() {
        String key1 = CacheKeyUtil.generateCadCacheKey(tempFile);
        TransViewProperties.View.Cad.setConvertType(CadConvertType.PDF);
        String key2 = CacheKeyUtil.generateCadCacheKey(tempFile);
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    public void generateCadCacheKeyDiffersForDifferentContent() throws IOException {
        String key1 = CacheKeyUtil.generateCadCacheKey(tempFile);
        try (FileWriter w = new FileWriter(tempFile)) {
            w.write("different content");
        }
        String key2 = CacheKeyUtil.generateCadCacheKey(tempFile);
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    public void sha256IsConsistent() throws IOException {
        String hash1 = CacheKeyUtil.sha256(tempFile);
        String hash2 = CacheKeyUtil.sha256(tempFile);
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 hex = 64 chars
    }

    @Test
    public void sha256StringIsConsistent() {
        String hash1 = CacheKeyUtil.sha256String("test input");
        String hash2 = CacheKeyUtil.sha256String("test input");
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64);
    }

    @Test
    public void sha256StringDiffersForDifferentInput() {
        String hash1 = CacheKeyUtil.sha256String("input1");
        String hash2 = CacheKeyUtil.sha256String("input2");
        assertThat(hash1).isNotEqualTo(hash2);
    }
}
