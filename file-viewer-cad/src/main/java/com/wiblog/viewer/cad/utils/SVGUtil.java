package com.wiblog.viewer.cad.utils;

import com.wiblog.viewer.core.common.Constant;
import com.wiblog.viewer.core.utils.Util;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * svg 工具类
 *
 * @author pwm
 */
public class SVGUtil {

    public static void previewCropSvg(InputStream inputStream, int cropHeight) {
        String transformedXml = cropSvgString(inputStream, cropHeight);
        previewSvg(transformedXml);
    }

    /**
     * 裁剪SVG
     *
     * @param inputStream 输入流
     * @param cropHeight  裁剪高度
     * @return svg内容
     */
    public static String cropSvgString(InputStream inputStream, int cropHeight) {
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
        Document doc;
        try {
            doc = factory.createDocument(null, inputStream);
            // 获取根元素
            Element svgRoot = doc.getDocumentElement();
            // 获取根元素下的直接子节点
            NodeList childNodes = svgRoot.getChildNodes();

            for (int i = childNodes.getLength() - 1; i > 0; i--) {
                Node node = childNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE && "g".equals(node.getNodeName())) {
                    svgRoot.removeChild(node);
                    break;
                }
            }

//            // 获取原始宽高
//            float originalWidth = Float.parseFloat(svgRoot.getAttribute("width").replace("px", ""));
//            float originalHeight = Float.parseFloat(svgRoot.getAttribute("height").replace("px", ""));
//
//            // 创建新的 SVG 文档
//            DOMImplementation impl = doc.getImplementation();
//            Document newDoc = impl.createDocument(svgRoot.getNamespaceURI(), "svg", null);
//            Element newSvgRoot = newDoc.getDocumentElement();
//            newSvgRoot.setAttribute("width", String.valueOf(originalWidth));
//            newSvgRoot.setAttribute("height", String.valueOf(originalHeight - cropHeight));
//
//            // 设置 viewBox 属性以裁剪顶部100像素
//            newSvgRoot.setAttribute("viewBox", "0 " + cropHeight + " " + originalWidth + " " + (originalHeight - cropHeight));
//
////             导入原始内容
//            Element importedRoot = (Element) newDoc.importNode(svgRoot, true);
//            newSvgRoot.appendChild(importedRoot);

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
     * 预览 SVG 文件
     *
     * @param svgStr svg内容
     */
    public static void previewSvg(String svgStr) {
        HttpServletResponse response = Util.getResponse();
        // 设置 HttpServletResponse 的内容类型和输出
        response.setContentType(Constant.MediaType.IMAGE_SVG_VALUE);
        response.setCharacterEncoding("UTF-8");
        try {
            response.getWriter().write(svgStr);
        } catch (IOException e) {
            throw new RuntimeException("预览 SVG 文件失败", e);
        }
    }
}
