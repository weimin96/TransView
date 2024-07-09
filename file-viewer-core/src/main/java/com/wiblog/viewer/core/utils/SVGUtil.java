package com.wiblog.viewer.core.utils;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * svg 工具类
 *
 * @author pwm
 */
public class SVGUtil {

    public static final String CUT_TYPE_CAD = "cad";

    public static final String CUT_TYPE_EXCEL = "excel";

    /**
     * svg去水印
     *
     * @param inputStream 输入流
     * @param type        文件类型
     * @return svg
     */
    public static String removeWatermark(InputStream inputStream, String type) {
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
        Document doc;
        try {
            doc = factory.createDocument(null, inputStream);
            if (CUT_TYPE_CAD.equals(type)) {
                removeWatermarkForCad(doc);
            } else if (CUT_TYPE_EXCEL.equals(type)) {
                removeWatermarkForExcel(doc);
            } else {
                throw new RuntimeException("不支持的文件类型");
            }


            // 保存裁剪后的 SVG 文件
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            // 创建一个 StringWriter 用于捕获转换的结果
            StringWriter stringWriter = new StringWriter();
            StreamResult result = new StreamResult(stringWriter);

            // 进行转换
            transformer.transform(new DOMSource(doc), result);
            // 获取转换后的字符串
            return stringWriter.toString();
        } catch (Exception e) {
            throw new RuntimeException("裁剪 SVG 文件失败", e);
        }
    }

    /**
     * 裁剪cad生成的SVG
     *
     * @param doc doc
     */
    private static void removeWatermarkForCad(Document doc) {
        // 获取根元素
        Element svgRoot = doc.getDocumentElement();
        svgRoot.setAttribute("background", "#ffffff");
        // 获取根元素下的直接子节点
        NodeList childNodes = svgRoot.getChildNodes();

        for (int i = childNodes.getLength() - 1; i > 0; i--) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && "g".equals(node.getNodeName())) {
                svgRoot.removeChild(node);
                break;
            }
        }
    }

    /**
     * 裁剪excel生成的SVG
     *
     * @param doc doc
     */
    private static void removeWatermarkForExcel(Document doc) {
        Element sContentElement = doc.getElementById("SContent");
        if (sContentElement == null) {
            return;
        }
        NodeList gNodes = sContentElement.getElementsByTagName("g");
        if (gNodes.getLength() == 0) {
            return;
        }
        Node firstNode = gNodes.item(0);
        NodeList childGNodes = firstNode.getChildNodes();

        for (int i = childGNodes.getLength() - 1; i > 0; i--) {
            Node node = childGNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && "g".equals(node.getNodeName())) {
                firstNode.removeChild(node);
                return;
            }
        }
    }

}
