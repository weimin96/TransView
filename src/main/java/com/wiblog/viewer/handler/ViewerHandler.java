package com.wiblog.viewer.handler;

import com.wiblog.viewer.common.Constant;
import com.wiblog.viewer.common.StrategyTypeEnum;
import com.wiblog.viewer.utils.Util;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

/**
 * describe:
 *
 * @author panwm
 * @since 2024/6/28 14:48
 */
public abstract class ViewerHandler {

    protected ViewerHandler() {

    }

    /**
     * 文件写入到 HttpServletResponse
     *
     * @param inputStream 文件流
     * @param extension   文件后缀
     */
    public abstract void handler(InputStream inputStream, ServletOutputStream outputStream, String extension) throws Exception;

    public void handlerResponse(InputStream inputStream, String extension) {
        HttpServletResponse response = Util.getResponse();
        // 设置 HttpServletResponse 的内容类型和输出
        response.setContentType(StrategyTypeEnum.getMediaType(extension));
        response.setCharacterEncoding("UTF-8");
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            handler(inputStream, outputStream, extension);
            response.flushBuffer();
        } catch (Exception e) {
            throw new RuntimeException("预览 " + extension + " 文件失败", e);
        }
    }

    public void preview(InputStream inputStream, String filenameOrExtension) {
        check(filenameOrExtension);
        handlerResponse(inputStream, Util.getExtension(filenameOrExtension));
    }

    public void preview(MultipartFile multipartFile) {
        String extension = Util.getExtension(multipartFile.getName());
        check(extension);
        try {
            handlerResponse(multipartFile.getInputStream(), extension);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void preview(File file) {
        String extension = Util.getExtension(file.getName());
        check(extension);
        try {
            handlerResponse(Files.newInputStream(file.toPath()), extension);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void check(String filenameOrExtension) {
        String extension = Util.getExtension(filenameOrExtension);
        boolean valid = strategyTypeEnums().contains(StrategyTypeEnum.getStrategy(extension));
        if (!valid) {
            throw new IllegalArgumentException(Constant.ERROR_MSG_ILLEGAL_TYPE + ":" + extension);
        }
    }

    public abstract List<StrategyTypeEnum> strategyTypeEnums();
}
