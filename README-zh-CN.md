# TransView

[![Java CI](https://github.com/weimin96/TransView/actions/workflows/ci.yml/badge.svg)](https://github.com/weimin96/TransView/actions/workflows/ci.yml)
[![GitHub Release](https://img.shields.io/github/v/release/weimin96/TransView)](https://github.com/weimin96/TransView/releases/)
[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.weimin96/transview-all)](https://repo1.maven.org/maven2/io/github/weimin96/transview-all/)
[![GitHub repo size](https://img.shields.io/github/repo-size/weimin96/TransView)](https://github.com/weimin96/TransView/releases/)
[![License](https://img.shields.io/:license-apache-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Last Commit](https://img.shields.io/github/last-commit/weimin96/TransView.svg)](https://github.com/weimin96/TransView)
[![GitHub commit activity](https://img.shields.io/github/commit-activity/m/weimin96/TransView.svg)](https://github.com/weimin96/TransView)

README: [English](README.md) | [中文](README-zh-CN.md)

## 支持环境

| 环境 | JDK | Spring Boot | 聚合包 |
|------|-----|-------------|--------|
| JDK 8+ | 8+ | 2.x | `transview-all-jdk8` |
| JDK 17+ | 17+ | 3.x | `transview-all` |
| JDK 17+ | 17+ | 4.x | `transview-all` |

## 介绍

文档在线预览/转换通用工具。支持 `spi` 可拔插模式，能快速集成到 `Java` 项目中，实现文件在线预览和各种格式转换功能。

### 支持格式

在线预览（零转换，直接输出）：
- 图片：jpg、jpeg、png、gif、webp、bmp、svg
- 文档：pdf
- 文本：txt、log、csv、json、xml、yaml
- 视频：mp4、avi
- HTML：支持预览，自动进行安全过滤（白名单策略，移除 script/iframe/form 等危险元素）

在线预览（需要转换）：
- CAD：dwg、dxf（默认输出 SVG，可配置为 PDF）
- Excel：xls、xlsx（输出 SVG）
- Word：doc、docx（默认输出 pdf，可配置为 svg）
- OFD：ofd（输出 PDF）

格式转换：
- svg -> png
- doc/docx -> svg/pdf

### 核心特性

- **HTTP 缓存支持**：纯输出格式（HTML 除外）和 CAD 缓存结果均支持 Range 请求、ETag、Last-Modified、Cache-Control、304 Not Modified。HTML 因需安全过滤，不支持 Range/ETag
- **DWG 磁盘缓存**：CAD 转换结果落磁盘，相同文件+相同配置不会重复转换
- **缩略图优先**：DWG 首次访问返回 800x600 缩略图（PNG），后台异步生成完整 SVG/PDF
- **CAD 线程池隔离**：独立线程池 + 内存感知限流，覆盖全部 CAD 路径（File、InputStream、缓存开启/关闭），大 DWG 不阻塞其他转换任务
- **InputStream 磁盘缓存**：CAD InputStream 预览支持磁盘缓存 — 源文件落盘为临时文件，后续命中缓存直接从磁盘流式输出，无需重新加载 CadImage
- **多布局缓存**：支持预缓存多个 DWG 布局，切换布局时直接命中缓存
- **HTML 安全过滤**：基于 Jsoup 白名单策略，自动移除 script、iframe、事件处理器等

## 包含组件

| 组件名 | 描述 |
|--------|------|
| `transview-core` | 核心包，不含 servlet 依赖，编译目标 Java 8 |
| `transview-cad` | CAD 格式处理模块（dwg、dxf），编译目标 Java 8 |
| `transview-poi` | 文档格式处理模块（xls、xlsx），编译目标 Java 8 |
| `transview-servlet-javax` | javax.servlet 适配模块，编译目标 Java 8 |
| `transview-servlet-jakarta` | jakarta.servlet 适配模块，编译目标 Java 17 |
| `transview-all` | JDK 17+ 聚合包（core + cad + poi + jakarta） |
| `transview-all-jdk8` | JDK 8+ 聚合包（core + cad + poi + javax） |

## 开始使用

### 引入依赖

**JDK 17+ / Spring Boot 3.x**

```xml
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>transview-all</artifactId>
    <version>${lastVersion}</version>
</dependency>
```

**JDK 8+ / Spring Boot 2.x**

```xml
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>transview-all-jdk8</artifactId>
    <version>${lastVersion}</version>
</dependency>
```

**单独引入**

```xml
<!-- 核心模块（必须） -->
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>transview-core</artifactId>
    <version>${lastVersion}</version>
</dependency>

<!-- servlet 适配（二选一） -->
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>transview-servlet-jakarta</artifactId>
    <version>${lastVersion}</version>
</dependency>

<!-- 可选：CAD 模块 -->
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>transview-cad</artifactId>
    <version>${lastVersion}</version>
</dependency>

<!-- 可选：Excel 模块 -->
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>transview-poi</artifactId>
    <version>${lastVersion}</version>
</dependency>
```

### 使用

[demo (Boot 3)](https://github.com/weimin96/TransView/tree/main/transview-demo) | [demo (Boot 2)](https://github.com/weimin96/TransView/tree/main/transview-demo-boot2)

#### 在线预览（InputStream）

适用于上传文件预览场景：

**JDK 17+ / jakarta**

```java
import com.wiblog.transview.servlet.jakarta.TransViewContext;

@GetMapping("/preview")
public void preview(MultipartFile file, HttpServletRequest request, HttpServletResponse response) throws IOException {
    TransViewContext.preview(file.getInputStream(), file.getName(), response);
}
```

**JDK 8+ / javax**

```java
import com.wiblog.transview.servlet.javax.TransViewContext;

@GetMapping("/preview")
public void preview(MultipartFile file, HttpServletRequest request, HttpServletResponse response) throws IOException {
    TransViewContext.preview(file.getInputStream(), file.getName(), response);
}
```

#### 在线预览（File）

适用于磁盘文件预览场景，支持 HTTP 缓存（Range/ETag/304）和 DWG 磁盘缓存：

```java
@GetMapping("/preview")
public void preview(File file, HttpServletRequest request, HttpServletResponse response) {
    TransViewContext.preview(file, request, response);
}
```

无 HttpServletRequest 的简化版本（不支持 Range/ETag）：

```java
TransViewContext.preview(file, response);
```

#### 格式转换

```java
import com.wiblog.transview.core.context.TransViewContext;

// 文件转换
TransViewContext.convert(File file, ExtensionEnum extensionEnum, OutputStream outputStream);

// 流转换
TransViewContext.convert(InputStream inputStream, String extension, File targetFile);
```

### 配置

```yaml
transview:
  view:
    # 全局超时时间，预览和转换均受此约束
    timeout: 30s

    # 是否移除 Aspose 转换结果水印（默认 true）
    # 注意：去水印会将完整结果先写入堆内存，大 DWG 建议设为 false
    remove-watermark: true

    # 全局字体目录（TTF/OTF）
    fonts-folder: /path/to/fonts

    # CAD 配置
    cad:
      # 输出格式：svg（默认，适合浏览器预览）或 pdf（适合打印/高保真）
      convert-type: svg

      # 渲染页面尺寸（像素）
      page-width: 2549
      page-height: 1228

      # 默认布局名称
      layout: Model

      # 预缓存的其他布局列表
      # 首次访问 DWG 后，后台自动预生成这些布局的缓存
      # 切换布局时直接命中缓存，无需等待转换
      extra-layouts:
        - Layout1
        - Layout2

      # SHX 字体目录（CAD 专用，支持多个目录）
      shx-fonts-folder:
        - /path/to/shx-fonts

    # Excel 配置
    excel:
      calculate-formula: false    # 预览前是否重新计算公式
      sheet-index: 0              # 预览的工作表索引（从 0 开始）
      one-page-per-sheet: true    # 是否每张工作表渲染为一页
      max-rows: -1                # 最大渲染行数（-1 不限制）
      max-columns: -1             # 最大渲染列数（-1 不限制）

    # Word 配置
    word:
      # 输出格式：pdf（默认）或 svg
      convert-type: pdf
      # Aspose.Words license 路径；存在 classpath:license.xml 时会默认加载
      # license-path: classpath:license.xml

  # 通用线程池（用于非 CAD 的预览/转换任务）
  executor:
    core-pool-size: 4
    max-pool-size: 8
    queue-capacity: 200

  # CAD 专用线程池（独立于通用线程池，避免大 DWG 阻塞其他任务）
  cad-executor:
    core-pool-size: 1
    max-pool-size: 2
    queue-capacity: 20
    # 可用 JVM 内存低于此值时拒绝新的 CAD 任务（MB）
    min-free-memory-mb: 256
    # 单个 CAD 任务超时时间（毫秒），超时后自动取消并清理临时文件
    task-timeout-ms: 120000

  # DWG 磁盘缓存配置
  cache:
    # 是否启用缓存（默认 false）
    enabled: true

    # 缓存根目录
    root-dir: /data/transview-cache

    # 最大磁盘占用（字节），默认 20GB
    max-disk-size: 21474836480

    # 缓存条目最大存活时间（毫秒），默认 7 天
    max-entry-age: 604800000

    # 缓存清理间隔（毫秒），默认 10 分钟
    cleanup-interval: 600000

    # 磁盘最低剩余空间（字节），低于此值强制清理最旧缓存，默认 5GB
    min-free-space: 5368709120
```

### DWG 缓存行为

启用缓存后（`cache.enabled: true`），DWG 预览流程如下：

```
请求到达
  ├─ 缓存命中完整结果（SVG/PDF）→ 直接返回，带 Range/ETag/304 支持
  ├─ 缓存命中缩略图（PNG）→ 返回缩略图 + 后台异步生成完整结果
  └─ 无缓存 → 生成缩略图返回 + 后台异步生成完整结果
       └─ 完整结果生成后，后台自动预生成 extraLayouts 中其他布局
```

防护机制：
- **缓存 Key**：SHA-256(文件内容) + 转换参数（布局/尺寸/格式/字体/去水印），确保不同内容或配置不会误命中
- **in-flight 去重**：同一文件同一配置的并发请求只触发一次转换
- **失败冷却**：转换失败的文件 5 分钟内不再重试，避免坏文件反复打爆 Aspose
- **内存感知**：可用内存不足时拒绝新任务，返回 503 而非 OOM
- **超时保护**：单个转换任务超时后自动取消并清理临时文件
