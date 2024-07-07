package com.wiblog.viewer.core.context;

import com.wiblog.viewer.core.common.StrategyTypeEnum;
import com.wiblog.viewer.core.handler.ViewerHandler;
import com.wiblog.viewer.core.utils.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * describe: 查看器上下文
 *
 * @author panwm
 * @since 2024/6/28 15:09
 */
public class ViewerContext {

    /**
     * 策略键值对
     */
    private static final Map<StrategyTypeEnum, Supplier<ViewerHandler>> VIEWER_HANDLER_MAP = new EnumMap<>(StrategyTypeEnum.class);

    static {
        ServiceLoader<ViewerHandler> serviceLoader = ServiceLoader.load(ViewerHandler.class);
        for (ViewerHandler handler : serviceLoader) {
            for (StrategyTypeEnum strategyTypeEnum: handler.strategyTypeEnums()) {
                VIEWER_HANDLER_MAP.put(strategyTypeEnum, () -> handler);
            }
        }
    }

    public static ViewerHandler createStrategy(String type) {
        Supplier<ViewerHandler> strategySupplier = VIEWER_HANDLER_MAP.get(StrategyTypeEnum.getStrategy(type));
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
    public static void preview(File file) {
        String extension = Util.getExtension(file.getName());
        if (extension == null) {
            throw new RuntimeException("获取不到文件后缀");
        }
        extension = extension.toLowerCase();
        try (FileInputStream inputStream = new FileInputStream(file)) {
            createStrategy(extension).preview(inputStream, extension);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
