package com.wiblog.transview.core.context;

import com.wiblog.transview.core.common.ExtensionEnum;
import com.wiblog.transview.core.common.StrategyTypeEnum;
import com.wiblog.transview.core.handler.TransViewHandler;
import com.wiblog.transview.core.utils.Util;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * describe: 处理器上下文
 *
 * @author panwm
 * @since 2024/6/28 15:09
 */
public class TransViewContext {

    /**
     * 策略键值对
     */
    private static final Map<StrategyTypeEnum, Supplier<TransViewHandler>> TRANS_VIEW_HANDLER_MAP = new EnumMap<>(StrategyTypeEnum.class);

    static {
        // spi加载
        ServiceLoader<TransViewHandler> serviceLoader = ServiceLoader.load(TransViewHandler.class);
        for (TransViewHandler handler : serviceLoader) {
            for (StrategyTypeEnum strategyTypeEnum : handler.strategyTypeEnums()) {
                TRANS_VIEW_HANDLER_MAP.put(strategyTypeEnum, () -> handler);
            }
        }
    }

    public static TransViewHandler createStrategy(String type) {
        Supplier<TransViewHandler> strategySupplier = TRANS_VIEW_HANDLER_MAP.get(StrategyTypeEnum.getStrategy(type));
        if (strategySupplier != null) {
            return strategySupplier.get();
        } else {
            throw new IllegalArgumentException("不支持的文件类型: " + type);
        }
    }

    /**
     * 文件预览入口
     *
     * @param file 文件
     */
    public static void preview(File file, HttpServletResponse response) {
        String extension = Util.getExtension(file.getName());
        if (Util.isBlank(extension)) {
            throw new RuntimeException("获取不到文件后缀");
        }
        try (FileInputStream inputStream = new FileInputStream(file)) {
            createStrategy(extension).preview(inputStream, extension, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 文件预览入口
     *
     * @param inputStream 文件流
     * @param filename    文件名
     */
    public static void preview(InputStream inputStream, String filename, HttpServletResponse response) {
        String extension = Util.getExtension(filename);
        if (Util.isBlank(extension)) {
            throw new RuntimeException("获取不到文件后缀");
        }
        createStrategy(extension).preview(inputStream, extension, response);
    }

    /**
     * 文件转换
     * @param file 源文件
     * @param extensionEnum 转换目标类型
     * @param outputStream 目标文件流
     */
    public static void convert(File file, ExtensionEnum extensionEnum, OutputStream outputStream) {
        String extension = Util.getExtension(file.getName());
        if (Util.isBlank(extension)) {
            throw new RuntimeException("获取不到文件后缀");
        }
        createStrategy(extension).convert(file, extensionEnum, outputStream);
    }

    public static void convert(InputStream inputStream, String extension, File targetFile) {
        if (Util.isBlank(extension)) {
            throw new RuntimeException("获取不到文件后缀");
        }
        createStrategy(extension).convert(inputStream, extension, targetFile);
    }
}
