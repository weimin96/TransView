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

| 环境 | JDK | Spring Boot | 聚合包 |
|------|-----|-------------|--------|
| JDK 8+ | 8+ | 2.x | `transview-all-jdk8` |
| JDK 17+ | 17+ | 3.x | `transview-all` |
| JDK 17+ | 17+ | 4.x | `transview-all` |

## 介绍

文档在线预览/转换通用工具。支持 `spi` 可拔插模式，能快速集成到 `Java` 项目中，实现文件在线预览和各种格式转换功能。

在线预览支持格式：
- 图片：jpg、jpeg、png、gif、svg
- 文档：pdf、xls、xlsx、csv
- 文本：txt、json、html
- 视频：mp4、avi
- CAD：dwg、dxf

格式转换支持格式：
- svg -> png

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

#### 在线预览

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

#### 格式转换

```java
import com.wiblog.transview.core.context.TransViewContext;

TransViewContext.convert(File file, ExtensionEnum extensionEnum, OutputStream outputStream);
```
