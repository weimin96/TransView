package com.wiblog.viewer.controller;

import com.wiblog.viewer.context.ViewerContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

/**
 * @author panwm
 * @since 2024/6/27 22:33
 */
@RestController
@RequestMapping("/viewer")
public class WebController {

    @GetMapping(value = "/preview")
    public void preview() {
        File file = new File("");
        ViewerContext.preview(file);
    }
}
