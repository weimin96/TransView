package com.wiblog.transview.spring.boot4;

import com.wiblog.transview.core.handler.TransViewHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * TransView 自动配置。
 * 属性赋值在 @PostConstruct 同步完成（轻量）；
 * 缓存索引重建和字体目录扫描在后台线程执行（重 IO），不阻塞启动。
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
        if (properties.getView().getCad().getExtraLayouts() != null) {
            com.wiblog.transview.core.bean.TransViewProperties.View.Cad.setExtraLayouts(properties.getView().getCad().getExtraLayouts());
        }

        // Word
        com.wiblog.transview.core.bean.TransViewProperties.View.Word.setConvertType(properties.getView().getWord().getConvertType());
        if (properties.getView().getWord().getLicensePath() != null) {
            com.wiblog.transview.core.bean.TransViewProperties.View.Word.setLicensePath(properties.getView().getWord().getLicensePath());
        }

        // Executor
        TransViewHandler.initExecutor(
                properties.getExecutor().getCorePoolSize(),
                properties.getExecutor().getMaxPoolSize(),
                properties.getExecutor().getQueueCapacity()
        );

        // CAD Executor
        com.wiblog.transview.core.bean.TransViewProperties.CadExecutor.setCorePoolSize(properties.getCadExecutor().getCorePoolSize());
        com.wiblog.transview.core.bean.TransViewProperties.CadExecutor.setMaxPoolSize(properties.getCadExecutor().getMaxPoolSize());
        com.wiblog.transview.core.bean.TransViewProperties.CadExecutor.setQueueCapacity(properties.getCadExecutor().getQueueCapacity());
        com.wiblog.transview.core.bean.TransViewProperties.CadExecutor.setMinFreeMemoryMB(properties.getCadExecutor().getMinFreeMemoryMB());
        com.wiblog.transview.core.bean.TransViewProperties.CadExecutor.setTaskTimeoutMs(properties.getCadExecutor().getTaskTimeoutMs());

        // Cache 配置（仅赋值，不初始化）
        com.wiblog.transview.core.bean.TransViewProperties.Cache.setEnabled(properties.getCache().isEnabled());
        if (properties.getCache().getRootDir() != null) {
            com.wiblog.transview.core.bean.TransViewProperties.Cache.setRootDir(properties.getCache().getRootDir());
        }
        com.wiblog.transview.core.bean.TransViewProperties.Cache.setMaxDiskSize(properties.getCache().getMaxDiskSize());
        com.wiblog.transview.core.bean.TransViewProperties.Cache.setMaxEntryAge(properties.getCache().getMaxEntryAge());
        com.wiblog.transview.core.bean.TransViewProperties.Cache.setCleanupInterval(properties.getCache().getCleanupInterval());
        com.wiblog.transview.core.bean.TransViewProperties.Cache.setMinFreeSpace(properties.getCache().getMinFreeSpace());

        // 重 IO 操作放后台线程，不阻塞启动
        startAsyncInit();
    }

    private void startAsyncInit() {
        Thread initThread = new Thread(() -> {
            try {
                com.wiblog.transview.core.cache.DiskCacheManager.getInstance().init();
            } catch (Exception e) {
                java.util.logging.Logger.getLogger("TransView")
                        .warning("缓存索引初始化失败: " + e.getMessage());
            }
            try {
                com.wiblog.transview.core.cache.FontIndex.getInstance().init(
                        properties.getView().getCad().getShxFontsFolder(),
                        properties.getView().getFontsFolder()
                );
            } catch (Exception e) {
                java.util.logging.Logger.getLogger("TransView")
                        .warning("字体索引初始化失败: " + e.getMessage());
            }
        }, "transview-init");
        initThread.setDaemon(true);
        initThread.start();
    }
}
