package com.wiblog.transview.core.handler;

import com.wiblog.transview.core.common.ExtensionEnum;
import com.wiblog.transview.core.common.StrategyTypeEnum;
import com.wiblog.transview.core.utils.SVGUtil;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * describe: 图片处理
 *
 * @author panwm
 * @since 2024/7/1 14:49
 */
public class PlainHandler extends TransViewHandler {

    @Override
    public void viewHandler(InputStream inputStream, ServletOutputStream outputStream, String extension, HttpServletResponse response) throws Exception {
        IOUtils.copy(inputStream, outputStream);
    }

    @Override
    public void convertHandler(ExtensionEnum sourceExtensionenum, ExtensionEnum targetExtensionenum, InputStream inputStream, OutputStream outputStream) {
        if (ExtensionEnum.SVG == sourceExtensionenum) {
            if (ExtensionEnum.PNG == targetExtensionenum) {
                SVGUtil.convertSvgToPng(inputStream, outputStream);
                return;
            }
        }
        throw new IllegalArgumentException("不支持的转换类型: " + sourceExtensionenum + "->" + targetExtensionenum);
    }

    @Override
    public List<StrategyTypeEnum> strategyTypeEnums() {
        return StrategyTypeEnum.PLAIN_TYPES;
    }
}
