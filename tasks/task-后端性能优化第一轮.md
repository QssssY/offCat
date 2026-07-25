# 后端性能优化第一轮任务记录

## 当前任务所属模块

后端性能与稳定性小步止血优化。

## 前端文件定位

本轮不涉及前端文件。

## 后端文件定位

- `server/src/main/java/com/airesume/server/service/impl/AdminDashboardServiceImpl.java`
- `server/src/main/java/com/airesume/server/mapper/InterviewSessionMapper.java`
- `server/src/main/java/com/airesume/server/mapper/ResumeDiagnosisTaskMapper.java`
- `server/src/main/java/com/airesume/server/service/InterviewContextCompressor.java`
- `server/src/main/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilter.java`

## 本轮修改文件清单

- 新增管理后台趋势聚合查询测试：`server/src/test/java/com/airesume/server/service/impl/AdminDashboardServiceImplTest.java`
- 新增面试摘要缓存清理测试：`server/src/test/java/com/airesume/server/service/InterviewContextCompressorTest.java`
- 补充 PDF 导出限流测试：`server/src/test/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilterTest.java`
- 最小兼容既有面试测试签名：`server/src/test/java/com/airesume/server/service/InterviewServiceTest.java`
- 最小兼容 Mock 面试 AI 测试签名：`server/src/test/java/com/airesume/server/service/impl/MockInterviewAiServiceImplTest.java`

## 前端实现方案

本轮不涉及前端实现，不改页面、不改接口调用。

## 后端实现方案

- 管理后台趋势查询从逐日循环 count 改为两个按日期聚合查询，Java 端按日期升序补零组装，保持 `DashboardTrendResponse` 返回结构不变。
- 面试摘要缓存继续使用 `ConcurrentHashMap`，缓存条目增加更新时间，定时清理超过 2 小时未刷新的条目；面试结束主动 `evictCache` 行为保持不变。
- PDF 导出接口接入现有关键端点限流过滤器，`POST /api/resume/export-pdf` 限制为 5 次 / 10 分钟，按登录用户优先、未登录按 IP。

## 数据存储方案

本轮不新增表、不修改字段、不新增索引，不涉及 `db/` 或 `server/db/` SQL 脚本变更。

## stage 更新说明

已新增根目录 `stage.md`，记录后端性能优化第一轮已完成，后续高风险优化仍未开始。

## 编译结果

`mvn compile` 通过。

## 构建结果

本轮为后端修改，已执行后端测试与编译验证；不涉及前端构建。

## 当前功能验收说明

- `mvn test -Dtest=CriticalEndpointRateLimitFilterTest,InterviewContextCompressorTest,AdminDashboardServiceImplTest,ServerApplicationTests` 通过。
- `mvn test` 通过，结果为 519 个测试，0 失败，0 错误。
- `mvn compile` 通过。

## 停止，不继续下一个功能

本轮仅完成后端性能优化第一轮小步止血范围。JPA 移除、批量大字段懒加载、RabbitMQ 增强、上传文件默认过期删除、Schema 统一、连接池与 Tomcat 参数调优均未推进，等待后续单独确认。
