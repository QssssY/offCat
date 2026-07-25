# 后端性能优化第五轮任务记录

## 当前任务所属模块

后端静态资源访问性能小步优化。

## 前端文件定位

本轮不涉及前端文件，不修改页面、不修改前端接口调用。

## 后端文件定位

- `server/src/main/java/com/airesume/server/config/WebMvcConfig.java`
- `server/src/test/java/com/airesume/server/config/WebMvcConfigTest.java`

## 本轮修改文件清单

- 更新 `WebMvcConfig`，为 `/uploads/community/**` 社区公开图片资源增加 1 天 `Cache-Control` 响应头。
- 新增 `WebMvcConfigTest`，验证社区上传资源缓存策略包含 `public` 和 `max-age=86400`。

## 前端实现方案

本轮不涉及前端实现。

## 后端实现方案

- 保留现有 `/uploads/community/**` 静态资源映射路径不变。
- 保留仅暴露社区图片目录的安全边界，不扩大到简历、报告等敏感上传目录。
- 在资源处理器上增加 `CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic()`，降低社区图片重复访问对后端静态资源处理的压力。
- 抽出包内 `communityUploadCacheControl()` 方法，便于单元测试验证缓存策略，不需要启动 Web 容器。

## 数据存储方案

本轮不新增表、不修改字段、不新增索引，不涉及 `db/` 或 `server/db/` SQL 脚本变更。

## stage 更新说明

已更新根目录 `stage.md`，记录后端性能优化第五轮已完成；高风险项仍未开始。

## 编译结果

`mvn compile` 通过。

## 构建结果

本轮为后端修改，已执行后端测试与编译验证；不涉及前端构建。

## 当前功能验收说明

- `mvn test -Dtest=WebMvcConfigTest` 通过，结果为 1 个测试，0 失败，0 错误。
- `mvn test` 通过，结果为 526 个测试，0 失败，0 错误。
- `mvn compile` 通过。

## 停止，不继续下一功能

本轮仅完成社区公开静态资源缓存头这一项低风险优化。JPA 移除、实体大字段懒加载、RabbitMQ 增强、上传文件默认过期删除、Schema 统一、HikariCP/Tomcat 生产容量参数调优均未继续推进，等待后续单独确认。
