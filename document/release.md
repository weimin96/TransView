### ⭐️ Features / Enhancements

文档在线预览通用工具。基于 `Spring Boot` 框架和 `spi` 可拔插模式，能快速集成到 `Spring Boot` 项目中，实现文件在线预览功能。

- 优化 Excel/CAD 预览链路，减少不必要的公式计算、二次处理和内存中转。
- 预览超时改为共享线程池执行，线程池繁忙时返回明确的 503。
- 普通文件预览支持缓存头、条件请求和单段 Range 请求。
- 修复 CAD 响应类型动态配置、资源关闭和 README 支持格式说明。

支持格式：
- 图片：jpg、jpeg、png、gif
- 文档：pdf、xls、xlsx、csv
- 文本：txt、json、html
- 视频：mp4、avi
- CAD：dwg、dxf
