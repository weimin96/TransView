package com.wiblog.viewer.core.handler;

import com.wiblog.viewer.core.common.StrategyTypeEnum;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * describe: 图片处理
 *
 * @author panwm
 * @since 2024/7/1 14:49
 */
public class PlainHandler extends ViewerHandler {

    @Override
    public void handler(InputStream inputStream, ServletOutputStream outputStream, String extension) throws Exception {
        IOUtils.copy(inputStream, outputStream);
    }

    @Override
    public List<StrategyTypeEnum> strategyTypeEnums() {
        return StrategyTypeEnum.PLAIN_TYPES;
    }
}
