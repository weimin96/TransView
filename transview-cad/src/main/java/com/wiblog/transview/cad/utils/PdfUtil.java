package com.wiblog.transview.cad.utils;

import jakarta.servlet.ServletOutputStream;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.OperatorName;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author panwm
 * @since 2024/7/11 0:14
 */
public class PdfUtil {

    public static void removeWatermark(ByteArrayInputStream pdfInputStream, ServletOutputStream outputStream) {
        try (PDDocument document = PDDocument.load(pdfInputStream)){
            removeAllText(document);
            document.save(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 去掉文档里的全部文本信息
     */
    public static void removeAllText(PDDocument pdDocument) {
        try {
            for (PDPage pdPage : pdDocument.getPages()) {
                //遍历Resources
                PDResources pdResources = pdPage.getResources();
                COSDictionary resourceDictionary = pdResources.getCOSObject();
                findStreamToRemoveText(resourceDictionary);

                //遍历Contents
                pdPage.getContentStreams().forEachRemaining(pdStream -> {
                    removeText(pdStream.getCOSObject());
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 遍历字典寻找流类型的对象进行去文字
     * 注意的是流对象也同时是字典对象
     * @param cosDictionary 字典类型
     */
    public static void findStreamToRemoveText(COSDictionary cosDictionary) {
        for (COSBase resourceBase : cosDictionary.getValues()) {
            //流
            if (resourceBase instanceof COSStream) {
                removeText((COSStream) resourceBase);
            }
            //字典
            if (resourceBase instanceof COSDictionary) {
                findStreamToRemoveText((COSDictionary) resourceBase);
            }
            //对象
            if (resourceBase instanceof COSObject) {
                if (((COSObject) resourceBase).getObject() instanceof COSStream) {
                    removeText((COSStream) ((COSObject) resourceBase).getObject());
                }
                if (((COSObject) resourceBase).getObject() instanceof COSDictionary) {
                    findStreamToRemoveText((COSDictionary) ((COSObject) resourceBase).getObject());
                }
            }
        }
    }


    /**
     * 去除流里面的文字
     *
     * @param pdStream 待修改流
     */
    public static void removeText(COSStream pdStream) {
        try {
            PDFStreamParser parser = new PDFStreamParser(pdStream);
            try {
                parser.parse();
            } catch (Exception e) {
                //如果有异常说明无法处理流，可能由于流的格式不符合要求，没有文本
                return;
            }
            //获取流解码后的内容
            List<Object> tokens = parser.getTokens();
            boolean isRemove = false;
            for (int i = 0; i < tokens.size(); i++) {
                Object token = tokens.get(i);
                if (token instanceof Operator) {
                    Operator operator = (Operator) token;
                    //流中含有Tj和DJ字样标识前面是需要展示的文字，直接置空去掉
                    if (operator.getName().equals(OperatorName.SHOW_TEXT)) {
                        COSString previous = (COSString) tokens.get(i - 1);
                        //注意指定该编码确保把流写回去，不然会出现乱码导致文档全空（由于PDF流中使用压缩格式）
                        previous.setValue("".getBytes(StandardCharsets.ISO_8859_1));
                        isRemove = true;
                    } else if (operator.getName().equals(OperatorName.SHOW_TEXT_ADJUSTED)) {
                        COSArray previous = (COSArray) tokens.get(i - 1);
                        for (int k = 0; k < previous.size(); k++) {
                            Object arrElement = previous.getObject(k);
                            if (arrElement instanceof COSString) {
                                COSString cosString = (COSString) arrElement;
                                cosString.setValue("".getBytes(StandardCharsets.ISO_8859_1));
                                isRemove = true;
                            }
                        }
                    }
                }
            }
            //删除了文字才进行流的更新
            if (isRemove) {
                OutputStream out = pdStream.createOutputStream();
                ContentStreamWriter tokenWriter = new ContentStreamWriter(out);
                tokenWriter.writeTokens(tokens);
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
