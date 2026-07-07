package com.wiblog.transview.ofd.handler;

import com.wiblog.transview.core.common.Constant;
import com.wiblog.transview.core.common.ExtensionEnum;
import com.wiblog.transview.core.common.StrategyTypeEnum;
import com.wiblog.transview.core.handler.TransViewHandler;
import org.ofdrw.converter.ConvertHelper;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * OFD 文档预览处理。
 *
 * @author panwm
 */
public class OfdHandler extends TransViewHandler {

    @Override
    public void convertHandler(ExtensionEnum sourceExtensionEnum, ExtensionEnum targetExtensionEnum, InputStream inputStream, OutputStream outputStream) throws Exception {
        if (sourceExtensionEnum != ExtensionEnum.OFD) {
            throw new IllegalArgumentException(Constant.ERROR_MSG_ILLEGAL_TYPE + ":" + sourceExtensionEnum.getValue());
        }
        if (targetExtensionEnum != ExtensionEnum.PDF) {
            throw new IllegalArgumentException(Constant.ERROR_MSG_ILLEGAL_TYPE + ":" + targetExtensionEnum.getValue());
        }
        convertToPdfForResponse(inputStream, outputStream);
    }

    @Override
    public List<StrategyTypeEnum> strategyTypeEnums() {
        return StrategyTypeEnum.OFD_TYPES;
    }

    @Override
    public void viewHandler(InputStream inputStream, OutputStream outputStream, String extension) {
        if (!StrategyTypeEnum.OFD.getType().equals(extension)) {
            throw new IllegalArgumentException(Constant.ERROR_MSG_ILLEGAL_TYPE + ":" + extension);
        }
        setOutputContentType(Constant.MediaType.PDF_VALUE);
        ConvertHelper.toPdf(inputStream, outputStream);
    }

    public static void convertToPdfForResponse(InputStream inputStream, OutputStream outputStream) {
        ConvertHelper.toPdf(inputStream, outputStream);
    }
}
