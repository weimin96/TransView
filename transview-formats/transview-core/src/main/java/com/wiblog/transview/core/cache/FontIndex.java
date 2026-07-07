package com.wiblog.transview.core.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 字体索引 — 启动时扫描字体目录，建立 字体名 → 文件路径 映射。
 * 避免每次转换时重复扫描目录。
 *
 * @author panwm
 * @since 2024/7/10 0:02
 */
public class FontIndex {

    private static final FontIndex INSTANCE = new FontIndex();

    /**
     * 小写字体名(无扩展名) -> 文件路径
     */
    private final ConcurrentHashMap<String, Path> shxIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Path> ttfIndex = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    public static FontIndex getInstance() {
        return INSTANCE;
    }

    private FontIndex() {
    }

    /**
     * 初始化字体索引
     *
     * @param shxDirs SHX 字体目录列表
     * @param ttfDir  TTF/OTF 字体目录
     */
    public synchronized void init(String[] shxDirs, String ttfDir) {
        if (initialized) {
            return;
        }
        if (shxDirs != null) {
            for (String dir : shxDirs) {
                scanDirectory(dir, shxIndex, ".shx");
            }
        }
        if (ttfDir != null) {
            scanDirectory(ttfDir, ttfIndex, ".ttf", ".otf");
        }
        initialized = true;
    }

    /**
     * 查找 SHX 字体文件路径
     */
    public Path findShxFont(String fontName) {
        return shxIndex.get(fontName.toLowerCase());
    }

    /**
     * 查找 TTF/OTF 字体文件路径
     */
    public Path findTtfFont(String fontName) {
        return ttfIndex.get(fontName.toLowerCase());
    }

    /**
     * 获取所有已索引的 SHX 字体名
     */
    public Set<String> getShxFontNames() {
        return Collections.unmodifiableSet(shxIndex.keySet());
    }

    /**
     * 获取所有已索引的 TTF 字体名
     */
    public Set<String> getTtfFontNames() {
        return Collections.unmodifiableSet(ttfIndex.keySet());
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void scanDirectory(String dirPath, ConcurrentHashMap<String, Path> index, String... extensions) {
        Path dir = Paths.get(dirPath);
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(dir, 2)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        for (String ext : extensions) {
                            if (name.endsWith(ext)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .forEach(p -> {
                        String name = p.getFileName().toString();
                        String key = name.substring(0, name.lastIndexOf('.')).toLowerCase();
                        index.putIfAbsent(key, p);
                    });
        } catch (IOException ignored) {
        }
    }
}
