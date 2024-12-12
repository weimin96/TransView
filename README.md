# TransView

[![Java CI](https://github.com/weimin96/TransView/actions/workflows/ci.yml/badge.svg)](https://github.com/weimin96/TransView/actions/workflows/ci.yml)
[![GitHub Release](https://img.shields.io/github/v/release/weimin96/TransView)](https://github.com/weimin96/TransView/releases/)
[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.weimin96/transview-all)](https://repo1.maven.org/maven2/io/github/weimin96/transview-all/)
[![GitHub repo size](https://img.shields.io/github/repo-size/weimin96/TransView)](https://github.com/weimin96/TransView/releases/)
[![License](https://img.shields.io/:license-apache-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Last Commit](https://img.shields.io/github/last-commit/weimin96/TransView.svg)](https://github.com/weimin96/TransView)
[![GitHub commit activity](https://img.shields.io/github/commit-activity/m/weimin96/TransView.svg)](https://github.com/weimin96/TransView)

README: [English](README.md) | [中文](README-zh-CN.md)

## support

- jdk 8
- spring boot 2.x

## Introduction

A universal tool for online preview/conversion of documents. Supports SPI plug-in mode, which can be quickly integrated into Java projects to achieve online file preview and various format conversion functions.

Supported formats:

* Images: jpg、jpeg、png、gif、svg
* Documents: doc、docx、pdf、xls、xlsx、csv
* Text: txt、json、htmL
* Video: mp4、avi
* CAD: dwg、dxf

Format conversion supports formats:

* svg -> png

## Supported components:

| components Name    | Description                                                                                                  |
|--------------------|--------------------------------------------------------------------------------------------------------------|
| `transview-core` | Core package comprising file preview entry points and generic file handling logic.(txt、json、csv、htmL、pdf、jpg、jpeg、png、svg、gif、mp4、avi) |
| `transview-cad`  | CAD format processing module (dwg、dxf)                                                                       |
| `transview-poi`  | document format processing module (doc、docx、xls、xlsx)                                                        

You can individually import each module according to your needs, or you can import all modules collectively by using the `transview-all` package.

## Getting Started

### Introduce dependency

**Mode one:To import all dependencies**

```xml
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>transview-all-jdk8</artifactId>
    <version>${lastVersion}</version>
</dependency>
```

**Mode two:Separate introduction**

core module(must)
```xml
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>transview-core-jdk8</artifactId>
    <version>${lastVersion}</version>
</dependency>
```

cad module
```xml
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>transview-cad-jdk8</artifactId>
    <version>${lastVersion}</version>
</dependency>
```

poi module
```xml
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>transview-poi-jdk8</artifactId>
    <version>${lastVersion}</version>
</dependency>
```

### Usage

[demo](https://github.com/weimin96/TransView/tree/main/transview-demo/src/main/java/com/wiblog/transview/demo)

#### preview online

```java
@GetMapping("/preview")
public void preview(MultipartFile file, HttpServletResponse response) throws IOException {
    TransViewContext.preview(file.getInputStream(), file.getName(), response);
}
```

**All supported methods**

1、Write `HttpServletResponse` through `InputStream`
```java
TransViewContext.preview(InputStream inputStream, String filenameOrExtension);
```

2、Write `HttpServletResponse` through `File`
```java
TransViewContext.preview(File file, HttpServletResponse response) ;
```

#### format conversion

```java
TransViewContext.convert(File file, ExtensionEnum extensionEnum, OutputStream outputStream);
```