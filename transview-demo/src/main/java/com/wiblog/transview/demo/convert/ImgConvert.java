package com.wiblog.transview.demo.convert;

import com.wiblog.transview.core.common.ExtensionEnum;
import com.wiblog.transview.core.context.TransViewContext;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author panwm
 * @since 2024/12/11 15:19
 */
public class ImgConvert {

    /**
     * 测试文件夹
     */
    private static final String TEST_FOLDER_NAME = "data";

    /**
     * 测试文件svg
     */
    private static final String TEST_SVG_FILE_NAME = "test.svg";

    public static void convertSVGToPNG() throws IOException {
        File file = ResourceUtils.getFile(ResourceUtils.CLASSPATH_URL_PREFIX + TEST_FOLDER_NAME + "/" + TEST_SVG_FILE_NAME);
        String targetPath = file.getParentFile().getAbsolutePath();
        File targetFile = new File(targetPath + File.separator + "test.png");
        TransViewContext.convert(file, ExtensionEnum.PNG, Files.newOutputStream(targetFile.toPath()));
    }

    public static void main(String[] args) throws IOException {
        convertSVGToPNG();
    }
}
