package com.wiblog.transview.core.bean;

import com.wiblog.transview.core.common.CadConvertType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;


/**
 * 参数配置
 * @author panwm
 * @since 2024/7/10 0:02
 */
public class TransViewProperties {

    /**
     * 预览配置
     */
    public static class View {

        /**
         * 取消请求时是否需要中断操作（默认 false）
         */
        @Getter
        @Setter
        private static boolean isInterrupted = false;

        /**
         * 字体目录
         */
        @Getter
        @Setter
        private static String fontsFolder;

        /**
         * 超时时间 单位：秒
         */
        @Getter
        @Setter
        private static Duration timeout;

        /**
         * 是否移除转换结果水印
         */
        @Getter
        @Setter
        private static boolean removeWatermark = true;

        public static class Excel {

            /**
             * 是否在预览前重新计算公式
             */
            @Getter
            @Setter
            private static boolean calculateFormula = false;

            /**
             * 预览的工作表索引（从 0 开始）
             */
            @Getter
            @Setter
            private static int sheetIndex = 0;

        }

        public static class Cad {

            /**
             * 转换结果类型 svg|pdf
             */
            @Getter
            @Setter
            private static CadConvertType convertType = CadConvertType.SVG;

            /**
             * cad shx 字体目录
             */
            @Getter
            @Setter
            private static String[] shxFontsFolder;

            /**
             * CAD 渲染页面宽度（像素）
             */
            @Getter
            @Setter
            private static int pageWidth = 2549;

            /**
             * CAD 渲染页面高度（像素）
             */
            @Getter
            @Setter
            private static int pageHeight = 1228;

            /**
             * CAD 布局名称
             */
            @Getter
            @Setter
            private static String layout = "Model";

        }
    }

    /**
     * 转换配置
     */
    @Getter
    @Setter
    public static class Trans {

    }

}
