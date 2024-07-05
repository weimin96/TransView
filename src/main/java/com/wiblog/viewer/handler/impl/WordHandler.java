//package com.wiblog.viewer.handler.impl;
//
//import com.aspose.words.Document;
//import com.aspose.words.License;
//import com.aspose.words.SaveFormat;
//import com.wiblog.viewer.common.StrategyTypeEnum;
//import com.wiblog.viewer.handler.ViewerHandler;
//import com.wiblog.viewer.utils.Util;
//
//import javax.servlet.ServletOutputStream;
//import javax.servlet.http.HttpServletResponse;
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.Arrays;
//import java.util.List;
//
///**
// * describe: word工具类
// *
// * @author panwm
// * @since 2024/6/28 14:45
// */
//public class WordHandler extends ViewerHandler {
//
//    public WordHandler() {
//        getLicense();
//    }
//
//    @Override
//    public void handler(InputStream inputStream, ServletOutputStream outputStream, String extension) throws Exception {
//        new Document(inputStream).save(outputStream, SaveFormat.PDF);
//    }
//
//    @Override
//    public List<StrategyTypeEnum> strategyTypeEnums() {
//        return StrategyTypeEnum.WORD_TYPES;
//    }
//
//    private static final byte[] LICENSE = ("<License>\n" +
//            "    <Data>\n" +
//            "        <Products>\n" +
//            "            <Product>Aspose.Total for Java</Product>\n" +
//            "            <Product>Aspose.Words for Java</Product>\n" +
//            "        </Products>\n" +
//            "        <EditionType>Enterprise</EditionType>\n" +
//            "        <SubscriptionExpiry>20991231</SubscriptionExpiry>\n" +
//            "        <LicenseExpiry>20991231</LicenseExpiry>\n" +
//            "        <SerialNumber>8bfe198c-7f0c-4ef8-8ff0-acc3237bf0d7</SerialNumber>\n" +
//            "    </Data>\n" +
//            "    <Signature>sNLLKGMUdF0r8O1kKilWAGdgfs2BvJb/2Xp8p5iuDVfZXmhppo+d0Ran1P9TKdjV4ABwAgKXxJ3jcQTqE/2IRfqwnPf8itN8aFZlV3TJPYeD3yWE7IT55Gz6EijUpC7aKeoohTb4w2fpox58wWoF3SNp6sK6jDfiAUGEHYJ9pjU=</Signature>\n" +
//            "</License>").getBytes();
//
//    /**
//     * 判断是否有授权文件 如果没有则会认为是试用版，转换的文件会有水印
//     */
//    private static void getLicense() {
//        try (InputStream is = new ByteArrayInputStream(LICENSE)) {
//            new License().setLicense(is);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//}
