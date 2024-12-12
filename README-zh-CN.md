# TransView

[![Java CI](https://github.com/weimin96/TransView/actions/workflows/ci.yml/badge.svg)](https://github.com/weimin96/TransView/actions/workflows/ci.yml)
[![GitHub Release](https://img.shields.io/github/v/release/weimin96/TransView)](https://github.com/weimin96/TransView/releases/)
[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.weimin96/transview-all)](https://repo1.maven.org/maven2/io/github/weimin96/transview-all/)
[![GitHub repo size](https://img.shields.io/github/repo-size/weimin96/TransView)](https://github.com/weimin96/TransView/releases/)
[![License](https://img.shields.io/:license-apache-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Last Commit](https://img.shields.io/github/last-commit/weimin96/TransView.svg)](https://github.com/weimin96/TransView)
[![GitHub commit activity](https://img.shields.io/github/commit-activity/m/weimin96/TransView.svg)](https://github.com/weimin96/TransView)

README: [English](README.md) | [中文](README-zh-CN.md)

## 支持

- jdk 8
- spring boot 2.x

## 介绍

文档在线预览/转换通用工具。支持 `spi` 可拔插模式，能快速集成到 `Java` 项目中，实现文件在线预览和各种格式转换功能。

在线预览支持格式：
- 图片：jpg、jpeg、png、gif、svg
- 文档：doc、docx、pdf、xls、xlsx、csv
- 文本：txt、json、htmL
- 视频：mp4、avi
- CAD：dwg、dxf

格式转换支持格式：
- svg -> png

## 包含组件

| 组件名                | 描述                                                                        |
|--------------------|---------------------------------------------------------------------------|
| `transview-core` | 核心包，包含文件预览入口和普通文件处理逻辑（txt、json、csv、htmL、pdf、jpg、jpeg、png、gif、svg、mp4、avi） |
| `transview-cad`  | cad 格式处理模块（dwg、dxf）                                                       |
| `transview-poi`  | 文档格式处理模块（doc、docx、xls、xlsx）                                               

可以根据需求对每个模块单独引入，也可以通过引入 `transview-all` 方式引入所有模块

## 开始使用

### 引入依赖

**方式一、全量引入**

```xml
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>transview-all</artifactId>
    <version>${lastVersion}</version>
</dependency>
```

**方式二、单独引入**

核心模块（必须）
```xml
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>transview-core</artifactId>
    <version>${lastVersion}</version>
</dependency>
```

cad模块
```xml
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>transview-cad</artifactId>
    <version>${lastVersion}</version>
</dependency>
```

poi模块
```xml
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>transview-poi</artifactId>
    <version>${lastVersion}</version>
</dependency>
```

### 使用

[demo](https://github.com/weimin96/TransView/tree/main/transview-demo/src/main/java/com/wiblog/transview/demo)

#### 在线预览

```java
@GetMapping("/preview")
public void preview(MultipartFile file, HttpServletResponse response) throws IOException {
    TransViewContext.preview(file.getInputStream(), file.getName(), response);
}
```

**支持的所有方法**

1、通过 `InputStream` 写入 `HttpServletResponse`
```java
ViewerContext.preview(InputStream inputStream, String filenameOrExtension);
```

2、通过 `File` 写入 `HttpServletResponse`
```java
ViewerContext.preview(File file, HttpServletResponse response) ;
```

#### 格式转换

```java
TransViewContext.convert(File file, ExtensionEnum extensionEnum, OutputStream outputStream);
```