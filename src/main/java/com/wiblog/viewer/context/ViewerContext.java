package com.wiblog.viewer.context;

import com.wiblog.viewer.common.StrategyTypeEnum;
import com.wiblog.viewer.handler.ViewerHandler;
import com.wiblog.viewer.handler.impl.CadHandler;
import com.wiblog.viewer.handler.impl.PlainHandler;
import com.wiblog.viewer.utils.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
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
        // cad
        VIEWER_HANDLER_MAP.put(StrategyTypeEnum.DWG, CadHandler::new);
        // word
//        VIEWER_HANDLER_MAP.put(StrategyTypeEnum.DOC, WordHandler::new);
//        VIEWER_HANDLER_MAP.put(StrategyTypeEnum.DOCX, WordHandler::new);
        // 普通文件
        for (StrategyTypeEnum item : StrategyTypeEnum.PLAIN_TYPES) {
            VIEWER_HANDLER_MAP.put(item, PlainHandler::new);
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
