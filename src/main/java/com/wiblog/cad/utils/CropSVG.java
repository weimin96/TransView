package com.wiblog.cad.utils;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.batik.dom.GenericDOMImplementation;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGSVGElement;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;

public class CropSVG {

    public static void main(String[] args) throws Exception {
        // 加载 SVG 文件
        String uri = new File("C:\\Users\\pwm\\Downloads\\ACadSharp-master\\ACadSharp-master\\samples\\data\\output.svg").toURI().toString();
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
        Document doc = factory.createDocument(uri);

        // 获取根元素
        Element svgRoot = doc.getDocumentElement();

        // 获取原始宽高
        float originalWidth = Float.parseFloat(svgRoot.getAttribute("width").replace("px", ""));
        float originalHeight = Float.parseFloat(svgRoot.getAttribute("height").replace("px", ""));

        // 创建新的 SVG 文档
        DOMImplementation impl = doc.getImplementation();
        Document newDoc = impl.createDocument(svgRoot.getNamespaceURI(), "svg", null);
        Element newSvgRoot = newDoc.getDocumentElement();
        newSvgRoot.setAttribute("width", String.valueOf(originalWidth));
        newSvgRoot.setAttribute("height", String.valueOf(originalHeight - 100));

        // 设置 viewBox 属性以裁剪顶部100像素
        newSvgRoot.setAttribute("viewBox", "0 100 " + originalWidth + " " + (originalHeight - 100));

        // 导入原始内容
        Element importedRoot = (Element) newDoc.importNode(svgRoot, true);
        newSvgRoot.appendChild(importedRoot);

        // 保存裁剪后的 SVG 文件
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        DOMSource source = new DOMSource(newDoc);
        StreamResult result = new StreamResult(new FileWriter("C:\\Users\\pwm\\Downloads\\ACadSharp-master\\ACadSharp-master\\samples\\data\\cropped_output.svg"));
        transformer.transform(source, result);
    }
}
