package com.wiblog.transview.core.cache;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CAD 转换执行器 — 内存感知 + 限流 + 超时 + 临时文件清理。
 */
public class CadConversionExecutor {

    private final ExecutorService executor;
    private final ScheduledExecutorService watchdog;
    private final long minFreeMemoryBytes;
    private final long taskTimeoutMs;
    private final AtomicInteger threadIndex = new AtomicInteger(1);
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    /**
     * @param corePoolSize    核心线程数
     * @param maxPoolSize     最大线程数
     * @param queueCapacity   队列深度
     * @param minFreeMemoryMB 最低可用内存（MB），低于此值拒绝新任务
     * @param taskTimeoutMs   单任务超时（毫秒）
     */
    public CadConversionExecutor(int corePoolSize, int maxPoolSize, int queueCapacity,
                                 int minFreeMemoryMB, long taskTimeoutMs) {
        this.minFreeMemoryBytes = (long) minFreeMemoryMB * 1024 * 1024;
        this.taskTimeoutMs = taskTimeoutMs;
        this.executor = new ThreadPoolExecutor(
                corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                r -> {
                    Thread t = new Thread(r, "transview-cad-worker-" + threadIndex.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
        this.watchdog = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "transview-cad-watchdog");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 提交异步转换任务，自动超时 + 失败时清理临时文件
     *
     * @param task       转换任务
     * @param tmpPath    临时文件路径（超时/失败时自动删除）
     * @param cacheKey   缓存 Key（用于失败日志）
     * @throws RejectedExecutionException 内存不足或队列满
     */
    public Future<?> submitAsync(Callable<Void> task, Path tmpPath, String cacheKey) {
        checkMemory();
        Future<?> future = executor.submit(() -> {
            activeTasks.incrementAndGet();
            try {
                task.call();
            } catch (Exception e) {
                deleteQuietly(tmpPath);
            } finally {
                activeTasks.decrementAndGet();
            }
        });
        // 超时监控：超时后 cancel + 清理临时文件
        watchdog.schedule(() -> {
            if (!future.isDone()) {
                future.cancel(true);
                deleteQuietly(tmpPath);
            }
        }, taskTimeoutMs, TimeUnit.MILLISECONDS);
        return future;
    }

    /**
     * 提交任务并等待结果（同步模式）
     */
    public File submitAndWait(Callable<Void> task, Path targetPath)
            throws TimeoutException, ExecutionException, InterruptedException {
        checkMemory();
        Future<Path> future = executor.submit(() -> {
            activeTasks.incrementAndGet();
            try {
                task.call();
                return targetPath;
            } finally {
                activeTasks.decrementAndGet();
            }
        });
        try {
            Path result = future.get(taskTimeoutMs, TimeUnit.MILLISECONDS);
            return result.toFile();
        } catch (TimeoutException e) {
            future.cancel(true);
            deleteQuietly(targetPath);
            throw e;
        }
    }

    public int getActiveCount() {
        return activeTasks.get();
    }

    private void checkMemory() {
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory();
        if (freeMemory < minFreeMemoryBytes) {
            throw new RejectedExecutionException(
                    "可用内存不足: " + (freeMemory / 1024 / 1024) + "MB, 最低要求: " + (minFreeMemoryBytes / 1024 / 1024) + "MB");
        }
    }

    private static void deleteQuietly(Path path) {
        if (path != null) {
            try { Files.deleteIfExists(path); } catch (IOException ignored) {}
        }
    }

    public void shutdown() {
        executor.shutdownNow();
        watchdog.shutdownNow();
    }
}
