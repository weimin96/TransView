package com.wiblog.cad.controller;

import com.wiblog.cad.common.Constant;
import com.wiblog.cad.utils.CadUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

/**
 * @author panwm
 * @since 2024/6/27 22:33
 */
@RestController
@RequestMapping("/cad")
public class WebController {

    @GetMapping(value = "/preview", produces = Constant.IMAGE_SVG_VALUE)
    public void preview(HttpServletResponse response) throws Exception {
        CadUtil.previewDwg(response);
    }
}
