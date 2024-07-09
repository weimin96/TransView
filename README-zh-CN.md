# file-viewer-tool

[![Java CI](https://github.com/weimin96/file-viewer-tool/actions/workflows/ci.yml/badge.svg)](https://github.com/weimin96/file-viewer-tool/actions/workflows/ci.yml)
[![GitHub Release](https://img.shields.io/github/v/release/weimin96/file-viewer-tool)](https://github.com/weimin96/file-viewer-tool/releases/)
[![Maven Central Version](https://img.shields.io/maven-central/v/weimin96/file-viewer-tool)](https://repo1.maven.org/maven2/io/github/weimin96/file-viewer-all/)
[![GitHub repo size](https://img.shields.io/github/repo-size/weimin96/file-viewer-tool)](https://github.com/weimin96/file-viewer-tool/releases/)
[![License](https://img.shields.io/:license-apache-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Last Commit](https://img.shields.io/github/last-commit/weimin96/file-viewer-tool.svg)](https://github.com/weimin96/file-viewer-tool)
[![GitHub commit activity](https://img.shields.io/github/commit-activity/m/weimin96/file-viewer-tool.svg)](https://github.com/weimin96/file-viewer-tool)

README: [English](README.md) | [中文](README-zh-CN.md)

## 介绍

文档在线预览通用工具。基于 `Spring Boot` 框架和 `spi` 可拔插模式，能快速集成到 `Spring Boot` 项目中，实现文件在线预览功能。

支持格式：
- 图片：jpg、jpeg、png、gif
- 文档：doc、docx、pdf、xls、xlsx、csv
- 文本：txt、json、htmL
- 视频：mp4、avi
- CAD：dwg、dxf

## 包含组件

| 组件名                | 描述                                                               |
|--------------------|------------------------------------------------------------------|
| `file-viewer-core` | 核心包，包含文件预览入口和普通文件处理逻辑（txt、json、csv、htmL、pdf、jpg、jpeg、png、gif、mp4、avi） |
| `file-viewer-cad`  | cad 格式处理模块（dwg、dxf）                                              |
| `file-viewer-poi`  | 文档格式处理模块（doc、docx、xls、xlsx）                                  

可以根据需求对每个模块单独引入，也可以通过引入 `file-viewer-all` 方式引入所有模块

## 开始使用

### 引入依赖

**方式一、全量引入**

```xml
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>file-viewer-all</artifactId>
    <version>${lastVersion}</version>
</dependency>
```

**方式二、单独引入**

核心模块（必须）
```xml
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>file-viewer-core</artifactId>
    <version>${lastVersion}</version>
</dependency>
```

cad模块
```xml
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>file-viewer-cad</artifactId>
    <version>${lastVersion}</version>
</dependency>
```

poi模块
```xml
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>file-viewer-poi</artifactId>
    <version>${lastVersion}</version>
</dependency>
```

### 使用

```java
@GetMapping(value = "/preview")
public void preview(String path) {
    File file = new File(path);
    ViewerContext.preview(file);
}
```

**支持的所有方法**

1、通过 `MultipartFile` 写入 `HttpServletResponse`
```java
ViewerContext.preview(MultipartFile multipartFile);
```

2、通过 `InputStream` 写入 `HttpServletResponse`
```java
ViewerContext.preview(InputStream inputStream, String filenameOrExtension);
```

2、通过 `File` 写入 `HttpServletResponse`
```java
ViewerContext.preview(File file) ;
```