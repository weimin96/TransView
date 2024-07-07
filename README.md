# file-viewer-tool

[![Java CI](https://github.com/weimin96/file-viewer-tool/actions/workflows/ci.yml/badge.svg)](https://github.com/weimin96/file-viewer-tool/actions/workflows/ci.yml)
[![GitHub Release](https://img.shields.io/github/v/release/weimin96/file-viewer-tool)](https://github.com/weimin96/file-viewer-tool/releases/)
[![Maven Central Version](https://img.shields.io/maven-central/v/weimin96/file-viewer-tool)](https://repo1.maven.org/maven2/io/github/weimin96/file-viewer-tool/)
[![GitHub repo size](https://img.shields.io/github/repo-size/weimin96/file-viewer-tool)](https://github.com/weimin96/file-viewer-tool/releases/)
[![License](https://img.shields.io/:license-apache-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Last Commit](https://img.shields.io/github/last-commit/weimin96/file-viewer-tool.svg)](https://github.com/weimin96/file-viewer-tool)
[![GitHub commit activity](https://img.shields.io/github/commit-activity/m/weimin96/file-viewer-tool.svg)](https://github.com/weimin96/file-viewer-tool)

README: [English](README.md) | [中文](README-zh-CN.md)

## Introduction

Universal tool for online document preview. Based on the Spring Boot framework and SPI pluggable mode, it can be quickly integrated into Spring Boot projects to achieve file online preview functionality.

Supported formats:

* Images: jpg, jpeg, png, gif
* Documents: doc, docx, pdf
* Text: txt
* CAD: dwg

## Supported components:

| components Name    | Description                                                                  |
|--------------------|---------------------------------------------------------------------|
| `file-viewer-core` | Core package comprising file preview entry points and generic file handling logic.                                              |
| `file-viewer-cad`  | CAD format processing module                                                          |

You can individually import each module according to your needs, or you can import all modules collectively by using the `file-viewer-all` package.

## Getting Started

### Introduce dependency

**Mode one:To import all dependencies**

```xml
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>file-viewer-all</artifactId>
    <version>${lastVersion}</version>
</dependency>
```

**Mode two:Separate introduction**

core module(must)
```xml
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>file-viewer-core</artifactId>
    <version>${lastVersion}</version>
</dependency>
```

cad module
```xml
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>file-viewer-cad</artifactId>
    <version>${lastVersion}</version>
</dependency>
```

### Usage

```java
@GetMapping(value = "/preview")
public void preview(String path) {
    File file = new File(path);
    ViewerContext.preview(file);
}
```

**All supported methods**

1、Write `HttpServletResponse` through `MultipartFile`
```java
ViewerContext.preview(MultipartFile multipartFile);
```

2、Write `HttpServletResponse` through `InputStream`
```java
ViewerContext.preview(InputStream inputStream, String filenameOrExtension);
```

2、Write `HttpServletResponse` through `File`
```java
ViewerContext.preview(File file) ;
```