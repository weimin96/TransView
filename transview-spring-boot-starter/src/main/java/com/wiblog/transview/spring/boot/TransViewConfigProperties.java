package com.wiblog.transview.spring.boot;

import com.wiblog.transview.core.common.CadConvertType;
import com.wiblog.transview.core.common.WordConvertType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * TransView 配置属性，绑定 application.yml 中的 transview 前缀。
 * 启动时自动同步到 core 的静态配置 {@link com.wiblog.transview.core.bean.TransViewProperties}。
 */
@ConfigurationProperties(prefix = "transview")
public class TransViewConfigProperties {

    private View view = new View();
    private Executor executor = new Executor();
    private CadExecutor cadExecutor = new CadExecutor();
    private Cache cache = new Cache();

    public View getView() { return view; }
    public void setView(View view) { this.view = view; }
    public Executor getExecutor() { return executor; }
    public void setExecutor(Executor executor) { this.executor = executor; }
    public CadExecutor getCadExecutor() { return cadExecutor; }
    public void setCadExecutor(CadExecutor cadExecutor) { this.cadExecutor = cadExecutor; }
    public Cache getCache() { return cache; }
    public void setCache(Cache cache) { this.cache = cache; }

    public static class View {
        private boolean interrupted = false;
        private String fontsFolder;
        private Duration timeout;
        private boolean removeWatermark = true;
        private Excel excel = new Excel();
        private Cad cad = new Cad();
        private Word word = new Word();

        public boolean isInterrupted() { return interrupted; }
        public void setInterrupted(boolean interrupted) { this.interrupted = interrupted; }
        public String getFontsFolder() { return fontsFolder; }
        public void setFontsFolder(String fontsFolder) { this.fontsFolder = fontsFolder; }
        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }
        public boolean isRemoveWatermark() { return removeWatermark; }
        public void setRemoveWatermark(boolean removeWatermark) { this.removeWatermark = removeWatermark; }
        public Excel getExcel() { return excel; }
        public void setExcel(Excel excel) { this.excel = excel; }
        public Cad getCad() { return cad; }
        public void setCad(Cad cad) { this.cad = cad; }
        public Word getWord() { return word; }
        public void setWord(Word word) { this.word = word; }
    }

    public static class Excel {
        private boolean calculateFormula = false;
        private int sheetIndex = 0;
        private boolean onePagePerSheet = true;
        private int maxRows = -1;
        private int maxColumns = -1;

        public boolean isCalculateFormula() { return calculateFormula; }
        public void setCalculateFormula(boolean calculateFormula) { this.calculateFormula = calculateFormula; }
        public int getSheetIndex() { return sheetIndex; }
        public void setSheetIndex(int sheetIndex) { this.sheetIndex = sheetIndex; }
        public boolean isOnePagePerSheet() { return onePagePerSheet; }
        public void setOnePagePerSheet(boolean onePagePerSheet) { this.onePagePerSheet = onePagePerSheet; }
        public int getMaxRows() { return maxRows; }
        public void setMaxRows(int maxRows) { this.maxRows = maxRows; }
        public int getMaxColumns() { return maxColumns; }
        public void setMaxColumns(int maxColumns) { this.maxColumns = maxColumns; }
    }

    public static class Cad {
        private CadConvertType convertType = CadConvertType.SVG;
        private String[] shxFontsFolder;
        private int pageWidth = 2549;
        private int pageHeight = 1228;
        private String layout = "Model";
        private String[] extraLayouts;

        public CadConvertType getConvertType() { return convertType; }
        public void setConvertType(CadConvertType convertType) { this.convertType = convertType; }
        public String[] getShxFontsFolder() { return shxFontsFolder; }
        public void setShxFontsFolder(String[] shxFontsFolder) { this.shxFontsFolder = shxFontsFolder; }
        public int getPageWidth() { return pageWidth; }
        public void setPageWidth(int pageWidth) { this.pageWidth = pageWidth; }
        public int getPageHeight() { return pageHeight; }
        public void setPageHeight(int pageHeight) { this.pageHeight = pageHeight; }
        public String getLayout() { return layout; }
        public void setLayout(String layout) { this.layout = layout; }
        public String[] getExtraLayouts() { return extraLayouts; }
        public void setExtraLayouts(String[] extraLayouts) { this.extraLayouts = extraLayouts; }
    }

    public static class Word {
        private WordConvertType convertType = WordConvertType.PDF;
        private String licensePath;

        public WordConvertType getConvertType() { return convertType; }
        public void setConvertType(WordConvertType convertType) { this.convertType = convertType; }
        public String getLicensePath() { return licensePath; }
        public void setLicensePath(String licensePath) { this.licensePath = licensePath; }
    }

    public static class Executor {
        private int corePoolSize = Math.max(1, Runtime.getRuntime().availableProcessors());
        private int maxPoolSize = Math.max(1, Runtime.getRuntime().availableProcessors() * 2);
        private int queueCapacity = 200;

        public int getCorePoolSize() { return corePoolSize; }
        public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
    }

    public static class CadExecutor {
        private int corePoolSize = 1;
        private int maxPoolSize = 2;
        private int queueCapacity = 20;
        private int minFreeMemoryMB = 256;
        private long taskTimeoutMs = 120000;

        public int getCorePoolSize() { return corePoolSize; }
        public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
        public int getMinFreeMemoryMB() { return minFreeMemoryMB; }
        public void setMinFreeMemoryMB(int minFreeMemoryMB) { this.minFreeMemoryMB = minFreeMemoryMB; }
        public long getTaskTimeoutMs() { return taskTimeoutMs; }
        public void setTaskTimeoutMs(long taskTimeoutMs) { this.taskTimeoutMs = taskTimeoutMs; }
    }

    public static class Cache {
        private boolean enabled = false;
        private String rootDir;
        private long maxDiskSize = 20L * 1024 * 1024 * 1024;
        private long maxEntryAge = 7L * 24 * 60 * 60 * 1000;
        private long cleanupInterval = 10L * 60 * 1000;
        private long minFreeSpace = 5L * 1024 * 1024 * 1024;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getRootDir() { return rootDir; }
        public void setRootDir(String rootDir) { this.rootDir = rootDir; }
        public long getMaxDiskSize() { return maxDiskSize; }
        public void setMaxDiskSize(long maxDiskSize) { this.maxDiskSize = maxDiskSize; }
        public long getMaxEntryAge() { return maxEntryAge; }
        public void setMaxEntryAge(long maxEntryAge) { this.maxEntryAge = maxEntryAge; }
        public long getCleanupInterval() { return cleanupInterval; }
        public void setCleanupInterval(long cleanupInterval) { this.cleanupInterval = cleanupInterval; }
        public long getMinFreeSpace() { return minFreeSpace; }
        public void setMinFreeSpace(long minFreeSpace) { this.minFreeSpace = minFreeSpace; }
    }
}
