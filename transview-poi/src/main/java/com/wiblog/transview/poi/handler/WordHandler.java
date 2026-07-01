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
import com.wiblog.transview.core.utils.LicenseUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
            throw new UnsupportedOperationException("不支持 " + sourceExtensionEnum.getValue() + " 输入格式");
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
        String licensePath = LicenseUtil.resolvePath(TransViewProperties.View.Word.getLicensePath());
        if (licensePath == null) {
            if (TransViewProperties.View.isRemoveWatermark()) {
                removeWatermark();
            }
            return;
        }
        if (licenseLoaded && licensePath.equals(loadedLicensePath)) {
            return;
        }
        synchronized (LICENSE_LOCK) {
            if (licenseLoaded && licensePath.equals(loadedLicensePath)) {
                return;
            }
            InputStream licenseStream;
            try {
                licenseStream = LicenseUtil.openStream(licensePath);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("Aspose.Words license 加载失败: " + licensePath, e);
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

    private static volatile boolean watermarkRemoved;

    private static void removeWatermark() {
        if (watermarkRemoved) {
            return;
        }
        synchronized (LICENSE_LOCK) {
            if (watermarkRemoved) {
                return;
            }
            try {
                Class<?> zzodClass = Class.forName("com.aspose.words.zzod");
                Constructor<?> constructor = zzodClass.getDeclaredConstructors()[0];
                constructor.setAccessible(true);
                Object zzodInstance = constructor.newInstance(null, null);
                Field zzWws = zzodClass.getDeclaredField("zzWws");
                zzWws.setAccessible(true);
                zzWws.set(zzodInstance, 1);
                Field zzVZC = zzodClass.getDeclaredField("zzVZC");
                zzVZC.setAccessible(true);
                zzVZC.set(zzodInstance, 1);

                Class<?> zz83Class = Class.forName("com.aspose.words.zz83");
                Field zzZY4 = zz83Class.getDeclaredField("zzZY4");
                zzZY4.setAccessible(true);
                java.util.ArrayList<Object> licenseList = new java.util.ArrayList<>();
                licenseList.add(zzodInstance);
                zzZY4.set(null, licenseList);

                Class<?> zzXuRClass = Class.forName("com.aspose.words.zzXuR");
                Field zzWE8 = zzXuRClass.getDeclaredField("zzWE8");
                zzWE8.setAccessible(true);
                zzWE8.set(null, 128);
                Field zzZKj = zzXuRClass.getDeclaredField("zzZKj");
                zzZKj.setAccessible(true);
                zzZKj.set(null, false);

                watermarkRemoved = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
