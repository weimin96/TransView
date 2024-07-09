package com.wiblog.viewer.test.web;

import com.wiblog.viewer.core.context.ViewerContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

/**
 * @author panwm
 * @since 2024/7/9 23:42
 */
@RestController
@RequestMapping("/viewer")
public class WebController {

    @GetMapping("/preview")
    public void preview(String path) {
        File file = new File("C:\\Users\\pwm\\Downloads\\新建文件夹\\" + path);
        ViewerContext.preview(file);
    }
}
