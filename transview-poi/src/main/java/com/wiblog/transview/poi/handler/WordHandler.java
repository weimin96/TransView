package com.wiblog.transview.poi.handler;

import com.aspose.words.Document;
import com.aspose.words.FontSettings;
import com.aspose.words.License;
import com.aspose.words.LoadOptions;
import com.aspose.words.SaveFormat;
import com.aspose.words.SvgSaveOptions;
import com.aspose.words.SvgTextOutputMode;
import com.wiblog.transview.core.bean.TransViewProperties;
import com.wiblog.transview.core.common.Constant;
import com.wiblog.transview.core.common.ExtensionEnum;
import com.wiblog.transview.core.common.StrategyTypeEnum;
import com.wiblog.transview.core.common.WordConvertType;
import com.wiblog.transview.core.handler.TransViewHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Word 文档预览处理。
 *
 * @author panwm
 */
public class WordHandler extends TransViewHandler {

    private static final Object LICENSE_LOCK = new Object();

    private static volatile boolean licenseLoaded;

    private static volatile String loadedLicensePath;

    @Override
    public void convertHandler(ExtensionEnum sourceExtensionEnum, ExtensionEnum targetExtensionEnum, InputStream inputStream, OutputStream outputStream) throws Exception {
        if (sourceExtensionEnum != ExtensionEnum.DOC && sourceExtensionEnum != ExtensionEnum.DOCX) {
            throw new UnsupportedOperationException("Aspose.Words 24.6 不支持 " + sourceExtensionEnum.getValue() + " 输入格式");
        }
        if (targetExtensionEnum == ExtensionEnum.SVG) {
            convertToSvgForResponse(inputStream, outputStream);
            return;
        }
        if (targetExtensionEnum == ExtensionEnum.PDF) {
            convertToPdfForResponse(inputStream, outputStream);
            return;
        }
        throw new IllegalArgumentException(Constant.ERROR_MSG_ILLEGAL_TYPE + ":" + targetExtensionEnum.getValue());
    }

    @Override
    public List<StrategyTypeEnum> strategyTypeEnums() {
        return StrategyTypeEnum.WORD_TYPES;
    }

    @Override
    public void viewHandler(InputStream inputStream, OutputStream outputStream, String extension) throws Exception {
        if (StrategyTypeEnum.DOC.getType().equals(extension) || StrategyTypeEnum.DOCX.getType().equals(extension)) {
            if (TransViewProperties.View.Word.getConvertType() == WordConvertType.PDF) {
                setOutputContentType(Constant.MediaType.PDF_VALUE);
                convertToPdfForResponse(inputStream, outputStream);
            } else {
                setOutputContentType(Constant.MediaType.IMAGE_SVG_VALUE);
                convertToSvgForResponse(inputStream, outputStream);
            }
            return;
        }
        throw new IllegalArgumentException(Constant.ERROR_MSG_ILLEGAL_TYPE + ":" + extension);
    }

    public static void convertToSvgForResponse(InputStream inputStream, OutputStream outputStream) throws Exception {
        Document document = loadDocument(inputStream);
        SvgSaveOptions saveOptions = new SvgSaveOptions();
        saveOptions.setSaveFormat(SaveFormat.SVG);
        saveOptions.setExportEmbeddedImages(true);
        saveOptions.setTextOutputMode(SvgTextOutputMode.USE_TARGET_MACHINE_FONTS);
        document.save(outputStream, saveOptions);
    }

    public static void convertToPdfForResponse(InputStream inputStream, OutputStream outputStream) throws Exception {
        Document document = loadDocument(inputStream);
        document.save(outputStream, SaveFormat.PDF);
    }

    private static Document loadDocument(InputStream inputStream) throws Exception {
        loadLicense();
        return new Document(inputStream, createLoadOptions());
    }

    private static void loadLicense() {
        String licensePath = trimToNull(TransViewProperties.View.Word.getLicensePath());
        boolean configured = licensePath != null;
        licensePath = configured ? licensePath : "classpath:license.xml";
        if (licenseLoaded && licensePath.equals(loadedLicensePath)) {
            return;
        }
        synchronized (LICENSE_LOCK) {
            if (licenseLoaded && licensePath.equals(loadedLicensePath)) {
                return;
            }
            InputStream licenseStream;
            try {
                licenseStream = openLicenseStream(licensePath);
            } catch (FileNotFoundException e) {
                if (configured) {
                    throw new IllegalStateException("Aspose.Words license 加载失败: " + licensePath, e);
                }
                return;
            }
            try (InputStream inputStream = licenseStream) {
                new License().setLicense(inputStream);
                loadedLicensePath = licensePath;
                licenseLoaded = true;
            } catch (Exception e) {
                throw new IllegalStateException("Aspose.Words license 加载失败: " + licensePath, e);
            }
        }
    }

    private static InputStream openLicenseStream(String licensePath) throws FileNotFoundException {
        if (licensePath.startsWith("classpath:")) {
            String resourcePath = licensePath.substring("classpath:".length());
            while (resourcePath.startsWith("/")) {
                resourcePath = resourcePath.substring(1);
            }
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
            if (inputStream == null) {
                throw new FileNotFoundException(licensePath);
            }
            return inputStream;
        }
        return new FileInputStream(licensePath);
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static LoadOptions createLoadOptions() {
        LoadOptions loadOptions = new LoadOptions();
        String fontsFolder = TransViewProperties.View.getFontsFolder();
        if (fontsFolder == null || fontsFolder.trim().isEmpty()) {
            return loadOptions;
        }

        File fontDir = new File(fontsFolder.trim());
        if (!fontDir.isDirectory()) {
            throw new IllegalArgumentException("字体目录不存在或不是目录: " + fontsFolder);
        }

        FontSettings fontSettings = new FontSettings();
        fontSettings.setFontsFolder(fontDir.getAbsolutePath(), true);
        loadOptions.setFontSettings(fontSettings);
        return loadOptions;
    }
}
