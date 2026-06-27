package com.wiblog.transview.core.cache;

import com.wiblog.transview.core.bean.TransViewProperties;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 磁盘缓存管理器。
 * <p>
 * 结构: {rootDir}/{prefix}/{hash}/ 下存放 result、thumbnail 和 metadata.properties。
 * 内存只维护 ConcurrentHashMap 索引（cacheKey -> CacheEntry），不缓存文件内容。
 */
public class DiskCacheManager {

    private static final DiskCacheManager INSTANCE = new DiskCacheManager();

    private final ConcurrentHashMap<String, CacheEntry> index = new ConcurrentHashMap<>();
    private final AtomicBoolean cleanupRunning = new AtomicBoolean(false);
    private volatile ScheduledExecutorService cleanupExecutor;

    private Path rootPath;

    public static DiskCacheManager getInstance() {
        return INSTANCE;
    }

    private DiskCacheManager() {
    }

    public synchronized void init() {
        if (!TransViewProperties.Cache.isEnabled() || TransViewProperties.Cache.getRootDir() == null) {
            return;
        }
        this.rootPath = Paths.get(TransViewProperties.Cache.getRootDir());
        try {
            Files.createDirectories(rootPath);
        } catch (IOException e) {
            throw new RuntimeException("无法创建缓存目录: " + rootPath, e);
        }
        rebuildIndex();
        startCleanup();
    }

    /**
     * 查找缓存文件（优先返回完整结果，否则返回缩略图）
     */
    public File get(String cacheKey) {
        if (!isReady()) {
            return null;
        }
        CacheEntry entry = index.get(cacheKey);
        if (entry == null) {
            return null;
        }
        long maxAge = TransViewProperties.Cache.getMaxEntryAge();
        if (System.currentTimeMillis() - entry.createdAt > maxAge) {
            invalidate(cacheKey);
            return null;
        }
        // 优先返回完整结果
        if (entry.resultPath != null && Files.exists(entry.resultPath)) {
            entry.lastAccessed = System.currentTimeMillis();
            writeMetadata(entry);
            return entry.resultPath.toFile();
        }
        // 降级返回缩略图
        if (entry.thumbnailPath != null && Files.exists(entry.thumbnailPath)) {
            entry.lastAccessed = System.currentTimeMillis();
            return entry.thumbnailPath.toFile();
        }
        index.remove(cacheKey);
        return null;
    }

    /**
     * 查找缩略图（用于首屏快速返回）
     */
    public File getThumbnail(String cacheKey) {
        if (!isReady()) {
            return null;
        }
        CacheEntry entry = index.get(cacheKey);
        if (entry == null || entry.thumbnailPath == null) {
            return null;
        }
        if (!Files.exists(entry.thumbnailPath)) {
            return null;
        }
        return entry.thumbnailPath.toFile();
    }

    /**
     * 从 byte[] 写入缓存
     */
    public void put(String cacheKey, File sourceFile, byte[] resultData, String extension) {
        if (!isReady()) {
            return;
        }
        try {
            Path entryDir = getEntryDir(cacheKey);
            Files.createDirectories(entryDir);
            Path resultPath = entryDir.resolve("result." + extension);
            Files.write(resultPath, resultData);
            long now = System.currentTimeMillis();
            CacheEntry entry = new CacheEntry(cacheKey, entryDir, resultPath, null,
                    sourceFile.getName(), sourceFile.length(), resultData.length, now, now);
            writeMetadata(entry);
            index.put(cacheKey, entry);
        } catch (Exception e) {
            cleanupFailedEntry(cacheKey);
            throw new RuntimeException("写入缓存失败", e);
        }
    }

    /**
     * 准备一个临时文件供调用方直接写入（避免 byte[] 占用堆内存）
     *
     * @return 临时文件路径，调用方写完后调用 commitDirect()
     */
    public Path prepareDirect(String cacheKey) {
        Path entryDir = getEntryDir(cacheKey);
        try {
            Files.createDirectories(entryDir);
            return entryDir.resolve("result.tmp");
        } catch (IOException e) {
            throw new RuntimeException("创建缓存临时文件失败", e);
        }
    }

    /**
     * 提交直接写入的缓存文件（原子移动）
     */
    public void commitDirect(String cacheKey, File sourceFile, Path tmpPath, String extension) {
        if (!isReady()) {
            return;
        }
        try {
            Path entryDir = getEntryDir(cacheKey);
            Path resultPath = entryDir.resolve("result." + extension);
            atomicMove(tmpPath, resultPath);
            long now = System.currentTimeMillis();
            long resultSize = Files.size(resultPath);
            CacheEntry entry = new CacheEntry(cacheKey, entryDir, resultPath, null,
                    sourceFile.getName(), sourceFile.length(), resultSize, now, now);
            writeMetadata(entry);
            index.put(cacheKey, entry);
        } catch (Exception e) {
            cleanupFailedEntry(cacheKey);
            throw new RuntimeException("提交缓存失败", e);
        }
    }

    /**
     * 写入缩略图到已有缓存条目
     */
    public void putThumbnail(String cacheKey, byte[] thumbnailData) {
        if (!isReady()) {
            return;
        }
        try {
            Path entryDir = getEntryDir(cacheKey);
            Files.createDirectories(entryDir);
            Path thumbPath = entryDir.resolve("thumbnail.png");
            Files.write(thumbPath, thumbnailData);
            CacheEntry entry = index.get(cacheKey);
            if (entry != null) {
                entry.thumbnailPath = thumbPath;
                writeMetadata(entry);
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * 写入缩略图（从临时文件原子移动）
     */
    public void putThumbnailDirect(String cacheKey, Path tmpThumbPath) {
        if (!isReady()) {
            return;
        }
        try {
            Path entryDir = getEntryDir(cacheKey);
            Files.createDirectories(entryDir);
            Path thumbPath = entryDir.resolve("thumbnail.png");
            atomicMove(tmpThumbPath, thumbPath);
            CacheEntry entry = index.get(cacheKey);
            if (entry != null) {
                entry.thumbnailPath = thumbPath;
                writeMetadata(entry);
            }
        } catch (IOException ignored) {
        }
    }

    public void invalidate(String cacheKey) {
        CacheEntry entry = index.remove(cacheKey);
        if (entry != null) {
            deleteRecursive(entry.entryDir);
        }
    }

    public void invalidateAll() {
        index.clear();
        if (rootPath != null && Files.exists(rootPath)) {
            try {
                Files.walk(rootPath)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                if (!p.equals(rootPath)) {
                                    Files.delete(p);
                                }
                            } catch (IOException ignored) {
                            }
                        });
            } catch (IOException ignored) {
            }
        }
    }

    public boolean isReady() {
        return rootPath != null && TransViewProperties.Cache.isEnabled();
    }

    private Path getEntryDir(String cacheKey) {
        String prefix = cacheKey.substring(0, Math.min(2, cacheKey.length()));
        return rootPath.resolve(prefix).resolve(cacheKey);
    }

    private void startCleanup() {
        long interval = TransViewProperties.Cache.getCleanupInterval();
        if (interval <= 0) {
            return;
        }
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "transview-cache-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupExecutor.scheduleWithFixedDelay(this::cleanup, interval, interval, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdownNow();
        }
    }

    void cleanup() {
        if (!cleanupRunning.compareAndSet(false, true)) {
            return;
        }
        try {
            long now = System.currentTimeMillis();
            long maxAge = TransViewProperties.Cache.getMaxEntryAge();
            long maxDisk = TransViewProperties.Cache.getMaxDiskSize();
            long minFree = TransViewProperties.Cache.getMinFreeSpace();

            Iterator<Map.Entry<String, CacheEntry>> it = index.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, CacheEntry> e = it.next();
                if (now - e.getValue().createdAt > maxAge) {
                    deleteRecursive(e.getValue().entryDir);
                    it.remove();
                }
            }

            long currentSize = calculateDirSize(rootPath);
            if (currentSize > maxDisk || getFreeSpace() < minFree) {
                List<CacheEntry> sorted = new ArrayList<>(index.values());
                sorted.sort(Comparator.comparingLong(a -> a.lastAccessed));
                for (CacheEntry entry : sorted) {
                    if (currentSize <= maxDisk && getFreeSpace() >= minFree) {
                        break;
                    }
                    long entrySize = entry.resultSize;
                    deleteRecursive(entry.entryDir);
                    index.remove(entry.cacheKey);
                    currentSize -= entrySize;
                }
            }
        } finally {
            cleanupRunning.set(false);
        }
    }

    private void rebuildIndex() {
        index.clear();
        if (!Files.exists(rootPath)) {
            return;
        }
        try {
            Files.list(rootPath).forEach(prefixDir -> {
                try {
                    Files.list(prefixDir).forEach(entryDir -> {
                        Path metaFile = entryDir.resolve("metadata.properties");
                        if (Files.exists(metaFile)) {
                            Properties props = readMetadata(metaFile);
                            if (props != null) {
                                String cacheKey = props.getProperty("cacheKey");
                                long createdAt = Long.parseLong(props.getProperty("createdAt", "0"));
                                long lastAccessed = Long.parseLong(props.getProperty("lastAccessed", "0"));
                                String sourceName = props.getProperty("sourceName", "");
                                long sourceSize = Long.parseLong(props.getProperty("sourceSize", "0"));
                                long resultSize = Long.parseLong(props.getProperty("resultSize", "0"));
                                String resultExt = props.getProperty("resultExtension", "svg");
                                Path resultPath = entryDir.resolve("result." + resultExt);
                                Path thumbPath = entryDir.resolve("thumbnail.png");
                                CacheEntry entry = new CacheEntry(cacheKey, entryDir,
                                        Files.exists(resultPath) ? resultPath : null,
                                        Files.exists(thumbPath) ? thumbPath : null,
                                        sourceName, sourceSize, resultSize, createdAt, lastAccessed);
                                if (entry.resultPath != null || entry.thumbnailPath != null) {
                                    index.put(cacheKey, entry);
                                }
                            }
                        }
                    });
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private void writeMetadata(CacheEntry entry) {
        Properties props = new Properties();
        props.setProperty("cacheKey", entry.cacheKey);
        props.setProperty("sourceName", entry.sourceName);
        props.setProperty("sourceSize", String.valueOf(entry.sourceSize));
        props.setProperty("resultSize", String.valueOf(entry.resultSize));
        props.setProperty("resultExtension", getExtension(entry.resultPath));
        props.setProperty("createdAt", String.valueOf(entry.createdAt));
        props.setProperty("lastAccessed", String.valueOf(entry.lastAccessed));
        Path metaFile = entry.entryDir.resolve("metadata.properties");
        Path tmpFile = entry.entryDir.resolve("metadata.tmp");
        try (OutputStream out = Files.newOutputStream(tmpFile)) {
            props.store(out, "TransView Cache Metadata");
        } catch (IOException ignored) {
            return;
        }
        try {
            Files.move(tmpFile, metaFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try {
                Files.move(tmpFile, metaFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
            }
        }
    }

    private Properties readMetadata(Path metaFile) {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(metaFile)) {
            props.load(in);
            return props;
        } catch (IOException e) {
            return null;
        }
    }

    private void cleanupFailedEntry(String cacheKey) {
        Path entryDir = getEntryDir(cacheKey);
        deleteRecursive(entryDir);
    }

    private static void atomicMove(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static void deleteRecursive(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    static long calculateDirSize(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return 0;
        }
        try {
            return Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            return 0;
        }
    }

    private long getFreeSpace() {
        if (rootPath == null) {
            return Long.MAX_VALUE;
        }
        try {
            FileStore store = Files.getFileStore(rootPath);
            return store.getUsableSpace();
        } catch (IOException e) {
            return Long.MAX_VALUE;
        }
    }

    private String getExtension(Path path) {
        if (path == null) {
            return "";
        }
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1) : "";
    }

    public static class CacheEntry {
        public final String cacheKey;
        public final Path entryDir;
        public volatile Path resultPath;
        public volatile Path thumbnailPath;
        public final String sourceName;
        public final long sourceSize;
        public volatile long resultSize;
        public final long createdAt;
        public volatile long lastAccessed;

        CacheEntry(String cacheKey, Path entryDir, Path resultPath, Path thumbnailPath,
                   String sourceName, long sourceSize, long resultSize, long createdAt, long lastAccessed) {
            this.cacheKey = cacheKey;
            this.entryDir = entryDir;
            this.resultPath = resultPath;
            this.thumbnailPath = thumbnailPath;
            this.sourceName = sourceName;
            this.sourceSize = sourceSize;
            this.resultSize = resultSize;
            this.createdAt = createdAt;
            this.lastAccessed = lastAccessed;
        }

        /** 是否已有完整结果（非缩略图） */
        public boolean hasFullResult() {
            return resultPath != null && Files.exists(resultPath);
        }
    }
}
