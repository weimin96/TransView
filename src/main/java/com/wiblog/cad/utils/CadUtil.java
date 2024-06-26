package com.wiblog.cad.utils;

import com.aspose.cad.Image;
import com.aspose.cad.fileformats.cad.CadImage;
import com.aspose.cad.imageoptions.CadRasterizationOptions;
import com.aspose.cad.imageoptions.SvgOptions;

/**
 * @author panwm
 * @since 2024/6/26 22:25
 */
public class CadUtil {

    public static void main(String[] args) {

        // 加载DWG文件
        String sourceFilePath = "C:\\Users\\pwm\\Downloads\\ACadSharp-master\\ACadSharp-master\\samples\\data\\1.dwg";
        CadImage cadImage = (CadImage) Image.load(sourceFilePath);

        // 设置转换选项
        CadRasterizationOptions rasterizationOptions = new CadRasterizationOptions();
        rasterizationOptions.setPageWidth(1600);
        rasterizationOptions.setPageHeight(1600);

        SvgOptions svgOptions = new SvgOptions();
        svgOptions.setVectorRasterizationOptions(rasterizationOptions);

        // 保存为SVG文件
        String outputFilePath = "C:\\Users\\pwm\\Downloads\\ACadSharp-master\\ACadSharp-master\\samples\\data\\output.svg";
        cadImage.save(outputFilePath, svgOptions);

        System.out.println("DWG文件成功转换为SVG格式。");
    }
}
