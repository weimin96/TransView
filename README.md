# TransView

[![Java CI](https://github.com/weimin96/TransView/actions/workflows/ci.yml/badge.svg)](https://github.com/weimin96/TransView/actions/workflows/ci.yml)
[![GitHub Release](https://img.shields.io/github/v/release/weimin96/TransView)](https://github.com/weimin96/TransView/releases/)
[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.weimin96/transview-all)](https://repo1.maven.org/maven2/io/github/weimin96/transview-all/)
[![GitHub repo size](https://img.shields.io/github/repo-size/weimin96/TransView)](https://github.com/weimin96/TransView/releases/)
[![License](https://img.shields.io/:license-apache-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Last Commit](https://img.shields.io/github/last-commit/weimin96/TransView.svg)](https://github.com/weimin96/TransView)
[![GitHub commit activity](https://img.shields.io/github/commit-activity/m/weimin96/TransView.svg)](https://github.com/weimin96/TransView)

README: [English](README.md) | [中文](README-zh-CN.md)

## Support

| Environment | JDK | Spring Boot | Artifact |
|-------------|-----|-------------|----------|
| JDK 8+ | 8+ | 2.x | `transview-all-jdk8` |
| JDK 17+ | 17+ | 3.x | `transview-all` |
| JDK 17+ | 17+ | 4.x | `transview-all` |

## Introduction

A universal tool for online preview/conversion of documents. Supports SPI plug-in mode, which can be quickly integrated into Java projects to achieve online file preview and various format conversion functions.

Supported preview formats:

* Images: jpg, jpeg, png, gif, svg
* Documents: pdf, xls, xlsx, csv
* Text: txt, json, html
* Video: mp4, avi
* CAD: dwg, dxf

Format conversion supports:

* svg -> png

## Components

| Component | Description |
|-----------|-------------|
| `transview-core` | Core module, no servlet dependency, compiled to Java 8 |
| `transview-cad` | CAD format module (dwg, dxf), compiled to Java 8 |
| `transview-poi` | Excel format module (xls, xlsx), compiled to Java 8 |
| `transview-servlet-javax` | javax.servlet adapter, compiled to Java 8 |
| `transview-servlet-jakarta` | jakarta.servlet adapter, compiled to Java 17 |
| `transview-all` | JDK 17+ aggregate (core + cad + poi + jakarta) |
| `transview-all-jdk8` | JDK 8+ aggregate (core + cad + poi + javax) |

## Getting Started

### Dependencies

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

**Individual modules**

```xml
<!-- Core (required) -->
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>transview-core</artifactId>
    <version>${lastVersion}</version>
</dependency>

<!-- Servlet adapter (choose one) -->
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>transview-servlet-jakarta</artifactId>
    <version>${lastVersion}</version>
</dependency>

<!-- Optional: CAD -->
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>transview-cad</artifactId>
    <version>${lastVersion}</version>
</dependency>

<!-- Optional: Excel -->
<dependency>
    <groupId>io.github.weimin96</groupId>
    <artifactId>transview-poi</artifactId>
    <version>${lastVersion}</version>
</dependency>
```

### Usage

[demo (Boot 3)](https://github.com/weimin96/TransView/tree/main/transview-demo) | [demo (Boot 2)](https://github.com/weimin96/TransView/tree/main/transview-demo-boot2)

#### Preview

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

#### Format conversion

```java
import com.wiblog.transview.core.context.TransViewContext;

TransViewContext.convert(File file, ExtensionEnum extensionEnum, OutputStream outputStream);
```
