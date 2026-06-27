package com.wiblog.transview.core.bean;

import com.wiblog.transview.core.common.CadConvertType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;


/**
 * 全局静态配置，应在应用启动时设置，运行期间不应按请求修改。
 * 按请求修改会导致并发请求之间互相影响（串扰）。
 *
 * @author panwm
 * @since 2024/7/10 0:02
 */
public class TransViewProperties {

    /**
     * 预览配置（启动期全局配置，非请求级）
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

            /**
             * 是否每张工作表渲染为一页（默认 true）
             */
            @Getter
            @Setter
            private static boolean onePagePerSheet = true;

            /**
             * 最大渲染行数（-1 不限制）
             */
            @Getter
            @Setter
            private static int maxRows = -1;

            /**
             * 最大渲染列数（-1 不限制）
             */
            @Getter
            @Setter
            private static int maxColumns = -1;

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
             * CAD 默认布局名称
             */
            @Getter
            @Setter
            private static String layout = "Model";

            /**
             * CAD 预生成缓存的布局列表（默认布局之外的其他布局）
             * 启动后首次访问某 DWG 时，后台异步预生成这些布局的缓存
             */
            @Getter
            @Setter
            private static String[] extraLayouts;

        }
    }

    /**
     * 通用线程池配置
     */
    public static class Executor {

        @Getter
        @Setter
        private static int corePoolSize = Math.max(1, Runtime.getRuntime().availableProcessors());

        @Getter
        @Setter
        private static int maxPoolSize = Math.max(1, Runtime.getRuntime().availableProcessors() * 2);

        @Getter
        @Setter
        private static int queueCapacity = 200;
    }

    /**
     * CAD 专用线程池配置（与通用线程池隔离）
     */
    public static class CadExecutor {

        @Getter
        @Setter
        private static int corePoolSize = 1;

        @Getter
        @Setter
        private static int maxPoolSize = 2;

        @Getter
        @Setter
        private static int queueCapacity = 20;

        /** 最低可用内存（MB），低于此值拒绝新 CAD 任务 */
        @Getter
        @Setter
        private static int minFreeMemoryMB = 256;

        /** 单个 CAD 任务超时（毫秒） */
        @Getter
        @Setter
        private static long taskTimeoutMs = 120000;
    }

    /**
     * 磁盘缓存配置
     */
    public static class Cache {

        @Getter
        @Setter
        private static boolean enabled = false;

        @Getter
        @Setter
        private static String rootDir;

        /** 最大磁盘占用（字节），默认 20GB */
        @Getter
        @Setter
        private static long maxDiskSize = 20L * 1024 * 1024 * 1024;

        /** 缓存条目最大存活时间（毫秒），默认 7 天 */
        @Getter
        @Setter
        private static long maxEntryAge = 7L * 24 * 60 * 60 * 1000;

        /** 清理间隔（毫秒），默认 10 分钟 */
        @Getter
        @Setter
        private static long cleanupInterval = 10L * 60 * 1000;

        /** 磁盘最低剩余空间（字节），默认 5GB */
        @Getter
        @Setter
        private static long minFreeSpace = 5L * 1024 * 1024 * 1024;
    }

    /**
     * 转换配置
     */
    @Getter
    @Setter
    public static class Trans {

    }

}
