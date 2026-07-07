### ⭐️ Features / Enhancements

文档在线预览/转换通用工具。支持 `spi` 可拔插模式，能快速集成到 `Java` 项目中，实现文件在线预览和各种格式转换功能。

在线预览支持格式：

- 图片：jpg、jpeg、png、gif、svg
- 文档：pdf、xls、xlsx、csv
- 文本：txt、json、html
- 视频：mp4、avi
- CAD：dwg、dxf

格式转换支持格式：

- svg -> png

### 📦 模块说明

| 模块                          | 说明                             |
|-----------------------------|--------------------------------|
| `transview-core`            | 核心包，无 servlet 依赖，Java 8 编译     |
| `transview-cad`             | CAD 格式处理（dwg、dxf），Java 8 编译    |
| `transview-poi`             | Excel 格式处理（xls、xlsx），Java 8 编译 |
| `transview-servlet-javax`   | javax.servlet 适配，Java 8 编译     |
| `transview-servlet-jakarta` | jakarta.servlet 适配，Java 17 编译  |
| `transview-all-jakarta`     | JDK 17+ 聚合包                    |
| `transview-all-javax`       | JDK 8+ 聚合包                     |

### 🔧 配置项

**线程池配置**

- `TransViewProperties.Executor.corePoolSize` — 核心线程数（默认 CPU 核心数）
- `TransViewProperties.Executor.maxPoolSize` — 最大线程数（默认 2x CPU 核心数）
- `TransViewProperties.Executor.queueCapacity` — 队列容量（默认 200）

**水印控制**

- `TransViewProperties.View.removeWatermark` — 是否移除水印（默认 true）

**Excel 配置**

- `TransViewProperties.View.Excel.calculateFormula` — 是否重新计算公式（默认 false）
- `TransViewProperties.View.Excel.sheetIndex` — 工作表索引（默认 0）
- `TransViewProperties.View.Excel.onePagePerSheet` — 每张工作表渲染为一页（默认 true）
- `TransViewProperties.View.Excel.maxRows` — 最大渲染行数（默认 -1 不限制）
- `TransViewProperties.View.Excel.maxColumns` — 最大渲染列数（默认 -1 不限制）

**CAD 配置**

- `TransViewProperties.View.Cad.pageWidth` — 渲染页面宽度（默认 2549）
- `TransViewProperties.View.Cad.pageHeight` — 渲染页面高度（默认 1228）
- `TransViewProperties.View.Cad.layout` — 布局名称（默认 "Model"）
