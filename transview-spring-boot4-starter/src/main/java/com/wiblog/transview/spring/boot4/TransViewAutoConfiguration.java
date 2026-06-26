package com.wiblog.transview.spring.boot4;

import com.wiblog.transview.core.handler.TransViewHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * TransView 自动配置。
 * 将 application.yml 中的 transview.* 配置同步到 core 的静态属性。
 */
@AutoConfiguration
@ConditionalOnClass(name = "jakarta.servlet.http.HttpServlet")
@EnableConfigurationProperties(TransViewConfigProperties.class)
public class TransViewAutoConfiguration {

    private final TransViewConfigProperties properties;

    public TransViewAutoConfiguration(TransViewConfigProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        // View
        com.wiblog.transview.core.bean.TransViewProperties.View.setInterrupted(properties.getView().isInterrupted());
        if (properties.getView().getFontsFolder() != null) {
            com.wiblog.transview.core.bean.TransViewProperties.View.setFontsFolder(properties.getView().getFontsFolder());
        }
        if (properties.getView().getTimeout() != null) {
            com.wiblog.transview.core.bean.TransViewProperties.View.setTimeout(properties.getView().getTimeout());
        }
        com.wiblog.transview.core.bean.TransViewProperties.View.setRemoveWatermark(properties.getView().isRemoveWatermark());

        // Excel
        com.wiblog.transview.core.bean.TransViewProperties.View.Excel.setCalculateFormula(properties.getView().getExcel().isCalculateFormula());
        com.wiblog.transview.core.bean.TransViewProperties.View.Excel.setSheetIndex(properties.getView().getExcel().getSheetIndex());
        com.wiblog.transview.core.bean.TransViewProperties.View.Excel.setOnePagePerSheet(properties.getView().getExcel().isOnePagePerSheet());
        com.wiblog.transview.core.bean.TransViewProperties.View.Excel.setMaxRows(properties.getView().getExcel().getMaxRows());
        com.wiblog.transview.core.bean.TransViewProperties.View.Excel.setMaxColumns(properties.getView().getExcel().getMaxColumns());

        // Cad
        com.wiblog.transview.core.bean.TransViewProperties.View.Cad.setConvertType(properties.getView().getCad().getConvertType());
        if (properties.getView().getCad().getShxFontsFolder() != null) {
            com.wiblog.transview.core.bean.TransViewProperties.View.Cad.setShxFontsFolder(properties.getView().getCad().getShxFontsFolder());
        }
        com.wiblog.transview.core.bean.TransViewProperties.View.Cad.setPageWidth(properties.getView().getCad().getPageWidth());
        com.wiblog.transview.core.bean.TransViewProperties.View.Cad.setPageHeight(properties.getView().getCad().getPageHeight());
        com.wiblog.transview.core.bean.TransViewProperties.View.Cad.setLayout(properties.getView().getCad().getLayout());

        // Executor
        TransViewHandler.initExecutor(
                properties.getExecutor().getCorePoolSize(),
                properties.getExecutor().getMaxPoolSize(),
                properties.getExecutor().getQueueCapacity()
        );
    }
}
