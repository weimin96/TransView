package com.wiblog.transview.core.handler;

import com.wiblog.transview.core.bean.TransViewProperties;
import com.wiblog.transview.core.common.Constant;
import com.wiblog.transview.core.common.ExtensionEnum;
import com.wiblog.transview.core.common.StrategyTypeEnum;
import com.wiblog.transview.core.utils.Util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

/**
 * describe: 预览处理抽象类
 *
 * @author panwm
 * @since 2024/6/28 14:48
 */
public abstract class TransViewHandler {

    private static volatile ExecutorService PREVIEW_EXECUTOR;

    private static final class ExecutorHolder {
        static final ExecutorService DEFAULT = createExecutor(
                TransViewProperties.Executor.getCorePoolSize(),
                TransViewProperties.Executor.getMaxPoolSize(),
                TransViewProperties.Executor.getQueueCapacity()
        );
    }

    private static ExecutorService createExecutor(int corePoolSize, int maxPoolSize, int queueCapacity) {
        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new ThreadFactory() {
                    private final java.util.concurrent.atomic.AtomicInteger index = new java.util.concurrent.atomic.AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "transview-preview-" + index.getAndIncrement());
                        thread.setDaemon(true);
                        return thread;
                    }
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * 自定义线程池，覆盖默认配置
     */
    public static void initExecutor(int corePoolSize, int maxPoolSize, int queueCapacity) {
        validateExecutorConfig(corePoolSize, maxPoolSize, queueCapacity);
        ExecutorService oldExecutor = PREVIEW_EXECUTOR;
        PREVIEW_EXECUTOR = createExecutor(corePoolSize, maxPoolSize, queueCapacity);
        if (oldExecutor != null) {
            oldExecutor.shutdown();
        }
    }

    private static ExecutorService getExecutor() {
        return PREVIEW_EXECUTOR != null ? PREVIEW_EXECUTOR : ExecutorHolder.DEFAULT;
    }

    private static void validateExecutorConfig(int corePoolSize, int maxPoolSize, int queueCapacity) {
        if (corePoolSize <= 0) {
            throw new IllegalArgumentException("corePoolSize 必须大于 0");
        }
        if (maxPoolSize < corePoolSize) {
            throw new IllegalArgumentException("maxPoolSize 必须大于等于 corePoolSize");
        }
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("queueCapacity 必须大于 0");
        }
    }

    protected TransViewHandler() {

    }

    /**
     * 文件转换
     * @param sourceExtensionEnum 源后缀
     * @param targetExtensionEnum 目标后缀
     * @param inputStream 文件流
     * @param outputStream 输出流
     * @throws Exception 异常
     */
    public abstract void convertHandler(ExtensionEnum sourceExtensionEnum, ExtensionEnum targetExtensionEnum, InputStream inputStream, OutputStream outputStream) throws Exception;

    public void convertHandler(ExtensionEnum sourceExtensionEnum, ExtensionEnum targetExtensionEnum, InputStream inputStream, File targetFile) throws Exception {
        try (OutputStream outputStream = Files.newOutputStream(targetFile.toPath())) {
            convertHandler(sourceExtensionEnum, targetExtensionEnum, inputStream, outputStream);
        }
    }

    /**
     * 支持的策略枚举列表
     *
     * @return List 策略枚举列表
     */
    public abstract List<StrategyTypeEnum> strategyTypeEnums();

    /**
     * 文件写入到 OutputStream
     *
     * @param inputStream  文件流
     * @param outputStream 输出流
     * @param extension    文件后缀
     * @throws Exception 异常
     */
    public abstract void viewHandler(InputStream inputStream, OutputStream outputStream, String extension) throws Exception;

    /**
     * 核心预览逻辑（不含 HTTP 头处理，由适配层负责）
     *
     * @param inputStream 文件流
     * @param extension   文件后缀
     * @param outputStream 输出流
     */
    public void handlerCore(InputStream inputStream, String extension, OutputStream outputStream) {
        try {
            if (TransViewProperties.View.getTimeout() != null) {
                Path resultFile = Files.createTempFile("transview-preview-", "." + extension);
                Callable<Void> conversionTask = () -> {
                    try (OutputStream out = Files.newOutputStream(resultFile)) {
                        viewHandler(inputStream, out, extension);
                    }
                    return null;
                };
                Future<Void> future;
                try {
                    future = getExecutor().submit(conversionTask);
                } catch (RejectedExecutionException e) {
                    throw new com.wiblog.transview.core.exception.PreviewBusyException("预览服务繁忙");
                }
                try {
                    future.get(TransViewProperties.View.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
                    Files.copy(resultFile, outputStream);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    throw new com.wiblog.transview.core.exception.PreviewTimeoutException("预览超时");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    }
                    throw new RuntimeException("预览 " + extension + " 文件失败", cause);
                } finally {
                    try {
                        Files.deleteIfExists(resultFile);
                    } catch (IOException ignored) {
                    }
                }
            } else {
                viewHandler(inputStream, outputStream, extension);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("预览 " + extension + " 文件失败", e);
        }
    }

    /**
     * 预览文件
     *
     * @param inputStream         文件流
     * @param filenameOrExtension 文件名或后缀
     * @param outputStream        输出流
     */
    public void preview(InputStream inputStream, String filenameOrExtension, OutputStream outputStream) {
        check(filenameOrExtension);
        handlerCore(inputStream, Util.getExtensionOrFilename(filenameOrExtension), outputStream);
    }

    /**
     * 预览文件
     *
     * @param file         文件
     * @param outputStream 输出流
     */
    public void preview(File file, OutputStream outputStream) {
        String extension = Util.getExtension(file.getName());
        check(extension);
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            handlerCore(inputStream, extension, outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 文件转换
     * @param file 源文件
     * @param extensionEnum 转换类型
     * @param outputStream 输出文件流
     */
    public void convert(File file, ExtensionEnum extensionEnum, OutputStream outputStream) {
        String extension = Util.getExtension(file.getName());
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            check(extension);
            convertCore(inputStream, ExtensionEnum.getByValue(extension), extensionEnum, outputStream);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void convert(InputStream inputStream, String extension, File targetFile) {
        try {
            check(extension);
            ExtensionEnum extensionEnum = ExtensionEnum.getByValue(Util.getExtension(targetFile.getName()));
            if (extensionEnum == null) {
                throw new IllegalArgumentException(Constant.ERROR_MSG_ILLEGAL_TYPE + ":" + extension);
            }
            try (OutputStream out = Files.newOutputStream(targetFile.toPath())) {
                convertCore(inputStream, ExtensionEnum.getByValue(extension), extensionEnum, out);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 核心转换逻辑（带超时和线程池保护）
     */
    private void convertCore(InputStream inputStream, ExtensionEnum sourceExt,
                             ExtensionEnum targetExt, OutputStream outputStream) {
        try {
            if (TransViewProperties.View.getTimeout() != null) {
                Path resultFile = Files.createTempFile("transview-convert-", "." + targetExt.getValue());
                Callable<Void> task = () -> {
                    try (OutputStream out = Files.newOutputStream(resultFile)) {
                        convertHandler(sourceExt, targetExt, inputStream, out);
                    }
                    return null;
                };
                Future<Void> future;
                try {
                    future = getExecutor().submit(task);
                } catch (RejectedExecutionException e) {
                    throw new com.wiblog.transview.core.exception.PreviewBusyException("转换服务繁忙");
                }
                try {
                    future.get(TransViewProperties.View.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
                    Files.copy(resultFile, outputStream);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    throw new com.wiblog.transview.core.exception.PreviewTimeoutException("转换超时");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    }
                    throw new RuntimeException("文件转换失败", cause);
                } finally {
                    try {
                        Files.deleteIfExists(resultFile);
                    } catch (IOException ignored) {
                    }
                }
            } else {
                convertHandler(sourceExt, targetExt, inputStream, outputStream);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("文件转换失败", e);
        }
    }

    /**
     * 检查文件类型是否合法
     *
     * @param filenameOrExtension 文件名或后缀
     */
    protected void check(String filenameOrExtension) {
        String extension = Util.getExtensionOrFilename(filenameOrExtension);
        boolean valid = strategyTypeEnums().contains(StrategyTypeEnum.getStrategy(extension));
        if (!valid) {
            throw new IllegalArgumentException(Constant.ERROR_MSG_ILLEGAL_TYPE + ":" + extension);
        }
    }

    public static boolean isPlainType(String extension) {
        StrategyTypeEnum strategy = StrategyTypeEnum.getStrategy(extension);
        return strategy != null && StrategyTypeEnum.PLAIN_TYPES.contains(strategy);
    }

}
