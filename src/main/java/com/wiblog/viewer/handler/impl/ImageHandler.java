package com.wiblog.viewer.handler.impl;

import com.wiblog.viewer.common.StrategyTypeEnum;
import com.wiblog.viewer.handler.ViewerHandler;
import com.wiblog.viewer.utils.Util;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * describe: 图片处理
 *
 * @author panwm
 * @since 2024/7/1 14:49
 */
public class ImageHandler implements ViewerHandler {

    @Override
    public void preview(InputStream inputStream, String type) {
        HttpServletResponse response = Util.getResponse();
        String mediaType = StrategyTypeEnum.getMediaType(type);
        // 设置响应的内容类型为图片类型
        response.setContentType(mediaType);
        // 读取文件内容并写入到响应中
        try {
            IOUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
