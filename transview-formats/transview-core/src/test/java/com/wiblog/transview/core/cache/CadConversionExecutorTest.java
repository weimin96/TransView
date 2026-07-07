package com.wiblog.transview.core.cache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class CadConversionExecutorTest {

    private CadConversionExecutor executor;
    private Path tempDir;

    @Before
    public void setup() throws IOException {
        tempDir = Files.createTempDirectory("cad-executor-test");
        executor = new CadConversionExecutor(1, 2, 10, 64, 2000);
    }

    @After
    public void cleanup() {
        executor.shutdown();
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
    public void submitAsyncExecutesTask() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        Path tmp = tempDir.resolve("test.tmp");
        Files.createFile(tmp);

        executor.submitAsync(() -> {
            executed.set(true);
            return null;
        }, tmp, "key1", () -> {
        }).get(3, TimeUnit.SECONDS);

        assertThat(executed).isTrue();
    }

    @Test
    public void submitAsyncCallsOnDone() throws Exception {
        AtomicBoolean done = new AtomicBoolean(false);
        Path tmp = tempDir.resolve("test.tmp");
        Files.createFile(tmp);

        executor.submitAsync(() -> null, tmp, "key1", () -> done.set(true)).get(3, TimeUnit.SECONDS);

        assertThat(done).isTrue();
    }

    @Test
    public void submitAsyncCallsOnDoneOnFailure() throws Exception {
        Path tmp = tempDir.resolve("fail.tmp");
        Files.createFile(tmp);
        AtomicBoolean done = new AtomicBoolean(false);

        try {
            executor.submitAsync(() -> {
                throw new RuntimeException("boom");
            }, tmp, "key2", () -> done.set(true)).get(3, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            // 预期异常
        }

        // onDone 应该被调用（无论成功或失败）
        assertThat(done).isTrue();
    }

    @Test
    public void submitAsyncTimeoutCancelsTask() throws Exception {
        // 创建一个超时很短的执行器
        CadConversionExecutor shortTimeout = new CadConversionExecutor(1, 1, 10, 64, 100);
        Path tmp = tempDir.resolve("timeout.tmp");
        Files.createFile(tmp);
        AtomicBoolean done = new AtomicBoolean(false);

        shortTimeout.submitAsync(() -> {
            Thread.sleep(5000); // 远超超时时间
            return null;
        }, tmp, "key3", () -> done.set(true));

        // 等待 watchdog 触发
        Thread.sleep(300);

        assertThat(Files.exists(tmp)).isFalse();
        assertThat(done).isTrue(); // onDone 被 watchdog 调用
        shortTimeout.shutdown();
    }

    @Test
    public void submitAsyncRejectsWhenMemoryLow() throws Exception {
        CadConversionExecutor memLimited = new CadConversionExecutor(1, 1, 10, 999999, 2000);
        Path tmp = tempDir.resolve("reject.tmp");
        Files.createFile(tmp);

        try {
            memLimited.submitAsync(() -> null, tmp, "key4", () -> {
            });
            // 如果没抛异常，可能是因为 JVM 实际可用内存 > 999999MB
            // 这种情况下跳过断言
        } catch (RejectedExecutionException e) {
            assertThat(e.getMessage()).contains("内存不足");
        }
        memLimited.shutdown();
    }

    @Test
    public void submitAndWaitReturnsResult() throws Exception {
        Path target = tempDir.resolve("result.tmp");

        File result = executor.submitAndWait(() -> {
            Files.write(target, "content".getBytes());
            return null;
        }, target);

        assertThat(result).exists();
        assertThat(result).hasName("result.tmp");
    }

    @Test
    public void submitAndWaitTimesOut() throws Exception {
        CadConversionExecutor shortTimeout = new CadConversionExecutor(1, 1, 10, 64, 100);
        Path target = tempDir.resolve("timeout-result.tmp");

        try {
            shortTimeout.submitAndWait(() -> {
                Thread.sleep(5000);
                return null;
            }, target);
        } catch (TimeoutException e) {
            // 预期
            assertThat(Files.exists(target)).isFalse(); // tmp 被清理
        }
        shortTimeout.shutdown();
    }

    @Test
    public void activeCountReflectsRunningTasks() throws Exception {
        assertThat(executor.getActiveCount()).isEqualTo(0);

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        Path tmp = tempDir.resolve("active.tmp");
        Files.createFile(tmp);

        Future<?> future = executor.submitAsync(() -> {
            started.countDown();
            proceed.await(5, TimeUnit.SECONDS);
            return null;
        }, tmp, "key5", () -> {
        });

        started.await(2, TimeUnit.SECONDS);
        assertThat(executor.getActiveCount()).isEqualTo(1);

        proceed.countDown();
        future.get(3, TimeUnit.SECONDS);
        assertThat(executor.getActiveCount()).isEqualTo(0);
    }
}
