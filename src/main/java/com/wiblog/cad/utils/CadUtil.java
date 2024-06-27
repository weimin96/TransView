package com.wiblog.cad.utils;

import com.aspose.cad.Image;
import com.aspose.cad.fileformats.cad.CadImage;
import com.aspose.cad.imageoptions.CadRasterizationOptions;
import com.aspose.cad.imageoptions.SvgOptions;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * CAD 工具类
 * @author panwm
 * @since 2024/6/26 22:25
 */
public class CadUtil {

    public static void previewDwg(HttpServletResponse response) throws Exception {
        File file = new File("C:\\Users\\pwm\\Downloads\\ACadSharp-master\\ACadSharp-master\\samples\\data\\1.dwg");
        convertToSvg(Files.newInputStream(file.toPath()), response);
    }

    public static void convertToSvg(InputStream inputStream, HttpServletResponse response) {
        CadImage cadImage = (CadImage) Image.load(inputStream);

        // 设置转换选项
        CadRasterizationOptions rasterizationOptions = new CadRasterizationOptions();
        rasterizationOptions.setPageWidth(1600);
        rasterizationOptions.setPageHeight(1600);

        SvgOptions svgOptions = new SvgOptions();
        svgOptions.setVectorRasterizationOptions(rasterizationOptions);

        // 将 CadImage 转换为 SVG 字符串
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        cadImage.save(outputStream, svgOptions);

        byte[] byteArray = outputStream.toByteArray();
        ByteArrayInputStream svgInputStream = new ByteArrayInputStream(byteArray);

        SVGUtil.previewCropSvg(svgInputStream, 100, response);
    }
}
