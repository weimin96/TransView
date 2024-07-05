package com.wiblog.viewer.handler.impl;

import com.wiblog.viewer.common.StrategyTypeEnum;
import com.wiblog.viewer.handler.ViewerHandler;
import com.wiblog.viewer.utils.Util;
import org.apache.commons.io.IOUtils;
import org.springframework.util.StringUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
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
