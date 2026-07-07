package com.wiblog.transview.core.cache;

import com.wiblog.transview.core.bean.TransViewProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class DiskCacheManagerTest {

    private Path tempDir;
    private DiskCacheManager cache;
    private File sourceFile;

    @Before
    public void setup() throws IOException {
        tempDir = Files.createTempDirectory("transview-cache-test");
        TransViewProperties.Cache.setEnabled(true);
        TransViewProperties.Cache.setRootDir(tempDir.toString());
        TransViewProperties.Cache.setMaxEntryAge(600000);
        TransViewProperties.Cache.setMaxDiskSize(100 * 1024 * 1024);
        TransViewProperties.Cache.setMinFreeSpace(100 * 1024 * 1024);
        TransViewProperties.Cache.setCleanupInterval(0); // 禁用自动清理

        cache = DiskCacheManager.getInstance();
        cache.init();

        sourceFile = File.createTempFile("test-source-", ".dwg");
        try (FileWriter w = new FileWriter(sourceFile)) {
            w.write("dummy dwg content");
        }
    }

    @After
    public void cleanup() {
        cache.invalidateAll();
        cache.shutdown();
        sourceFile.delete();
        try (Stream<Path> stream = Files.walk(tempDir)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    @Test
    public void isReadyReturnsTrue() {
        assertThat(cache.isReady()).isTrue();
    }

    @Test
    public void getReturnsNullForMiss() {
        assertThat(cache.get("nonexistent-key")).isNull();
    }

    @Test
    public void putAndGetRoundTrip() {
        byte[] data = "test svg content".getBytes();
        cache.put("test-key", sourceFile, data, "svg");
        File result = cache.get("test-key");
        assertThat(result).isNotNull();
        assertThat(result.getName()).startsWith("result.");
    }

    @Test
    public void getThumbnailReturnsNullForMiss() {
        assertThat(cache.getThumbnail("nonexistent")).isNull();
    }

    @Test
    public void putThumbnailCreatesEntry() {
        byte[] thumbData = "fake png".getBytes();
        cache.putThumbnail("thumb-key", thumbData);
        File thumb = cache.getThumbnail("thumb-key");
        assertThat(thumb).isNotNull();
        assertThat(thumb.getName()).isEqualTo("thumbnail.png");
    }

    @Test
    public void putThumbnailCreatesEntryEvenWithoutPriorPut() {
        byte[] thumbData = "fake png".getBytes();
        cache.putThumbnail("new-key", thumbData);
        File thumb = cache.getThumbnail("new-key");
        assertThat(thumb).isNotNull();
    }

    @Test
    public void getReturnsFullResultOverThumbnail() {
        byte[] thumbData = "fake png".getBytes();
        byte[] resultData = "full svg".getBytes();
        cache.putThumbnail("key1", thumbData);
        cache.put("key1", sourceFile, resultData, "svg");
        File result = cache.get("key1");
        assertThat(result).isNotNull();
        assertThat(result.getName()).startsWith("result.");
    }

    @Test
    public void getReturnsThumbnailWhenNoFullResult() {
        byte[] thumbData = "fake png".getBytes();
        cache.putThumbnail("thumb-only", thumbData);
        File result = cache.get("thumb-only");
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("thumbnail.png");
    }

    @Test
    public void invalidateRemovesEntry() {
        cache.put("del-key", sourceFile, "data".getBytes(), "svg");
        assertThat(cache.get("del-key")).isNotNull();
        cache.invalidate("del-key");
        assertThat(cache.get("del-key")).isNull();
    }

    @Test
    public void prepareDirectReturnsUniquePaths() {
        Path p1 = cache.prepareDirect("same-key");
        Path p2 = cache.prepareDirect("same-key");
        assertThat(p1).isNotEqualTo(p2);
        assertThat(p1.getFileName().toString()).endsWith(".tmp");
    }

    @Test
    public void commitDirectMakesResultAvailable() throws IOException {
        Path tmpPath = cache.prepareDirect("commit-key");
        Files.write(tmpPath, "result content".getBytes());
        cache.commitDirect("commit-key", sourceFile, tmpPath, "svg");
        File result = cache.get("commit-key");
        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();
    }

    @Test
    public void metadataSurvivesRebuild() {
        cache.put("rebuild-key", sourceFile, "data".getBytes(), "svg");
        // 模拟重启：重新初始化
        DiskCacheManager newCache = DiskCacheManager.getInstance();
        newCache.init();
        File result = newCache.get("rebuild-key");
        assertThat(result).isNotNull();
    }

    @Test
    public void isReadyReturnsFalseWhenDisabled() {
        TransViewProperties.Cache.setEnabled(false);
        assertThat(cache.isReady()).isFalse();
        TransViewProperties.Cache.setEnabled(true);
    }
}
