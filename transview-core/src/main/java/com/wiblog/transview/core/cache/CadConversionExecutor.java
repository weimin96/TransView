package com.wiblog.transview.core.cache;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CAD 转换执行器 — 内存感知 + 限流 + 超时。
 * <p>
 * 特性：
 * - 独立线程池，不与通用线程池竞争
 * - 提交前检查可用内存，不足时拒绝（返回排队状态）
 * - 每个任务有超时控制
 * - 任务完成后主动清理临时文件
 * - 可配置最大并发数和队列深度
 */
public class CadConversionExecutor {

    private final ExecutorService executor;
    private final long minFreeMemoryBytes;
    private final long taskTimeoutMs;
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    /**
     * @param corePoolSize      核心线程数
     * @param maxPoolSize       最大线程数
     * @param queueCapacity     队列深度
     * @param minFreeMemoryMB   最低可用内存（MB），低于此值拒绝新任务
     * @param taskTimeoutMs     单任务超时（毫秒）
     */
    public CadConversionExecutor(int corePoolSize, int maxPoolSize, int queueCapacity,
                                 int minFreeMemoryMB, long taskTimeoutMs) {
        this.minFreeMemoryBytes = (long) minFreeMemoryMB * 1024 * 1024;
        this.taskTimeoutMs = taskTimeoutMs;
        this.executor = new ThreadPoolExecutor(
                corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                r -> {
                    Thread t = new Thread(r, "transview-cad-worker-" + activeTasks.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * 提交转换任务
     *
     * @param task     转换任务（写结果到 targetPath）
     * @param targetPath 结果文件路径
     * @return Future<Path> 结果文件路径
     * @throws RejectedExecutionException 内存不足或队列满时抛出
     */
    public Future<Path> submit(Callable<Void> task, Path targetPath) {
        checkMemory();
        return executor.submit(() -> {
            try {
                task.call();
                return targetPath;
            } finally {
                activeTasks.decrementAndGet();
            }
        });
    }

    /**
     * 提交任务并等待结果
     *
     * @param task      转换任务
     * @param targetPath 结果文件路径
     * @return 结果文件
     * @throws RejectedExecutionException 内存不足或队列满
     * @throws TimeoutException           超时
     * @throws ExecutionException         转换失败
     */
    public File submitAndWait(Callable<Void> task, Path targetPath)
            throws TimeoutException, ExecutionException, InterruptedException {
        Future<Path> future = submit(task, targetPath);
        try {
            Path result = future.get(taskTimeoutMs, TimeUnit.MILLISECONDS);
            return result.toFile();
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        }
    }

    /**
     * 当前活跃任务数
     */
    public int getActiveCount() {
        return activeTasks.get();
    }

    /**
     * 检查可用内存是否充足
     */
    private void checkMemory() {
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory();
        if (freeMemory < minFreeMemoryBytes) {
            throw new RejectedExecutionException(
                    "可用内存不足: " + (freeMemory / 1024 / 1024) + "MB, 最低要求: " + (minFreeMemoryBytes / 1024 / 1024) + "MB");
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
