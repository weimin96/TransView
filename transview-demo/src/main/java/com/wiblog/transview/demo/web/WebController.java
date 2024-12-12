package com.wiblog.transview.demo.web;

import com.wiblog.transview.core.common.CadConvertType;
import com.wiblog.transview.core.bean.TransViewProperties;
import com.wiblog.transview.core.context.TransViewContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

/**
 * @author panwm
 * @since 2024/7/9 23:42
 */
@RestController
@RequestMapping("/viewer")
public class WebController {

    @GetMapping("/preview")
    public void preview(MultipartFile file, HttpServletResponse response) throws IOException {
        TransViewProperties.View.Cad.setConvertType(CadConvertType.SVG);
        TransViewProperties.View.setTimeout(Duration.ofSeconds(60));
        TransViewContext.preview(file.getInputStream(), file.getName(), response);
    }
}
