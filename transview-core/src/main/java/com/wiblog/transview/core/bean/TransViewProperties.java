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
