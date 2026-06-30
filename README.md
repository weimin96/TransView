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

### Supported Formats

Zero-conversion preview (direct output):
- Images: jpg, jpeg, png, gif, webp, bmp, svg
- Documents: pdf
- Text: txt, log, csv, json, xml, yaml
- Video: mp4, avi
- HTML: preview with automatic security filtering (whitelist strategy, removes script/iframe/form etc.)

Conversion preview:
- CAD: dwg, dxf (default SVG, configurable to PDF)
- Excel: xls, xlsx (output SVG)
- Word: doc, docx (default pdf, configurable to svg)
- OFD: ofd (output PDF)

Format conversion:
- svg -> png
- doc/docx -> svg/pdf

### Key Features

- **HTTP caching**: Plain formats (except HTML) and CAD cache results support Range requests, ETag, Last-Modified, Cache-Control, 304 Not Modified. HTML excludes Range/ETag due to security filtering.
- **DWG disk cache**: CAD conversion results stored on disk; same file + same config never re-converts
- **Thumbnail first**: DWG first access returns 800x600 thumbnail (PNG), async generates full SVG/PDF in background
- **CAD thread pool isolation**: Dedicated thread pool + memory-aware throttling for all CAD paths (File, InputStream, cache-enabled/disabled), large DWGs don't block other tasks
- **InputStream caching**: InputStream CAD preview now supports disk cache — source is saved to temp file, cached results streamed directly from disk on subsequent hits
- **Multi-layout caching**: Pre-cache multiple DWG layouts, layout switching hits cache instantly
- **HTML security filtering**: Jsoup whitelist strategy, automatically removes script, iframe, event handlers etc.

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

#### Preview (InputStream)

For uploaded file preview:

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

#### Preview (File)

For disk file preview, supports HTTP caching (Range/ETag/304) and DWG disk cache:

```java
@GetMapping("/preview")
public void preview(File file, HttpServletRequest request, HttpServletResponse response) {
    TransViewContext.preview(file, request, response);
}
```

Simplified version without HttpServletRequest (no Range/ETag support):

```java
TransViewContext.preview(file, response);
```

#### Format Conversion

```java
import com.wiblog.transview.core.context.TransViewContext;

// File conversion
TransViewContext.convert(File file, ExtensionEnum extensionEnum, OutputStream outputStream);

// Stream conversion
TransViewContext.convert(InputStream inputStream, String extension, File targetFile);
```

### Configuration

```yaml
transview:
  view:
    # Global timeout for preview and conversion
    timeout: 30s

    # Remove Aspose watermark from conversion results (default true)
    # Note: watermark removal buffers full result in heap, set false for large DWGs
    remove-watermark: true

    # Global font directory (TTF/OTF)
    fonts-folder: /path/to/fonts

    # CAD configuration
    cad:
      # Output format: svg (default, best for browser preview) or pdf (for print/high-fidelity)
      convert-type: svg

      # Render page dimensions (pixels)
      page-width: 2549
      page-height: 1228

      # Default layout name
      layout: Model

      # Extra layouts to pre-cache
      # After first DWG access, these layouts are cached in background
      # Switching layouts hits cache instantly
      extra-layouts:
        - Layout1
        - Layout2

      # SHX font directories (CAD-specific, supports multiple)
      shx-fonts-folder:
        - /path/to/shx-fonts

    # Excel configuration
    excel:
      calculate-formula: false    # Recalculate formulas before preview
      sheet-index: 0              # Worksheet index to preview (0-based)
      one-page-per-sheet: true    # Render each worksheet as one page
      max-rows: -1                # Max rows to render (-1 = unlimited)
      max-columns: -1             # Max columns to render (-1 = unlimited)

    # Word configuration
    word:
      # Output format: pdf (default) or svg
      convert-type: pdf
      # Aspose.Words license path; defaults to classpath:license.xml when present
      # license-path: classpath:license.xml

  # General thread pool (for non-CAD preview/conversion tasks)
  executor:
    core-pool-size: 4
    max-pool-size: 8
    queue-capacity: 200

  # CAD dedicated thread pool (isolated from general pool)
  cad-executor:
    core-pool-size: 1
    max-pool-size: 2
    queue-capacity: 20
    # Reject new CAD tasks when available JVM memory is below this (MB)
    min-free-memory-mb: 256
    # Per-task timeout (ms), cancelled and tmp cleaned up on timeout
    task-timeout-ms: 120000

  # DWG disk cache configuration
  cache:
    # Enable cache (default false)
    enabled: true

    # Cache root directory
    root-dir: /data/transview-cache

    # Max disk usage (bytes), default 20GB
    max-disk-size: 21474836480

    # Max cache entry age (ms), default 7 days
    max-entry-age: 604800000

    # Cache cleanup interval (ms), default 10 minutes
    cleanup-interval: 600000

    # Min free disk space (bytes), force cleanup when below this, default 5GB
    min-free-space: 5368709120
```

### DWG Cache Behavior

With cache enabled (`cache.enabled: true`), DWG preview flow:

```
Request arrives
  ├─ Cache hit: full result (SVG/PDF) → return directly with Range/ETag/304 support
  ├─ Cache hit: thumbnail (PNG) → return thumbnail + async generate full result
  └─ No cache → generate thumbnail + return + async generate full result
       └─ After full result, async pre-generate extraLayouts in background
```

Protection mechanisms:
- **Cache key**: SHA-256(file content) + conversion params (layout/dimensions/format/fonts/watermark), ensures different content or config never collides
- **In-flight dedup**: Concurrent requests for same file+config trigger only one conversion
- **Failure cooldown**: Failed conversions are not retried for 5 minutes, prevents bad files from hammering Aspose
- **Memory-aware**: Rejects new tasks when available memory is low, returns 503 instead of OOM
- **Timeout protection**: Per-task timeout with automatic cancellation and tmp file cleanup
