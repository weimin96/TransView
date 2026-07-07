package com.wiblog.transview.core.utils;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * svg 工具类
 *
 * @author pwm
 */
public class SVGUtil {

    /**
     * svg转png
     *
     * @param svgFileInputStream  输入文件流
     * @param pngFileOutputStream 输出文件流
     * @return boolean
     */
    public static boolean convertSvgToPng(InputStream svgFileInputStream, OutputStream pngFileOutputStream) {
        PNGTranscoder transcoder = new PNGTranscoder();
        return convertSvgToPng(svgFileInputStream, pngFileOutputStream, transcoder);
    }

    /**
     * svg转png
     *
     * @param svgFileInputStream  输入文件流
     * @param pngFileOutputStream 输出文件流
     * @param transcoder          转换器
     * @return boolean
     */
    public static boolean convertSvgToPng(InputStream svgFileInputStream, OutputStream pngFileOutputStream, PNGTranscoder transcoder) {
        TranscoderInput input = new TranscoderInput(svgFileInputStream);
        TranscoderOutput output = new TranscoderOutput(pngFileOutputStream);
        try {
            transcoder.transcode(input, output);
        } catch (TranscoderException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

}
