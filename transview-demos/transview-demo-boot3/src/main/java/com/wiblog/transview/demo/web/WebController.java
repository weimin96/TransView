package com.wiblog.transview.demo.web;

import com.wiblog.transview.servlet.jakarta.TransViewContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author panwm
 * @since 2024/7/9 23:42
 */
@RestController
@RequestMapping("/viewer")
public class WebController {

    @GetMapping("/preview")
    public void preview(MultipartFile file, HttpServletRequest request, HttpServletResponse response) throws IOException {
        TransViewContext.preview(file.getInputStream(), file.getName(), response);
    }
}
