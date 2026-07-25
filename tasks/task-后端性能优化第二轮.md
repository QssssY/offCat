# 后端性能优化第二轮任务记录

## 当前任务所属模块

后端性能与稳定性运行时保护小步优化。

## 前端文件定位

本轮不涉及前端文件，不修改页面、不改接口调用。

## 后端文件定位

- `server/src/main/resources/application.yml`
- `server/src/main/resources/application-dev.yml`
- `server/src/main/resources/application-prod.yml`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`

## 本轮修改文件清单

- 新增运行时保护配置测试：`server/src/test/java/com/airesume/server/config/RuntimeProtectionConfigTest.java`
- 补充面试 AI 流式调用保护测试：`server/src/test/java/com/airesume/server/service/impl/InterviewAiServiceImplTest.java`
- 更新默认、开发、生产环境 Redis Lettuce 连接池等待时间配置。
- 更新面试 AI 流式 WebClient 响应超时与高频统计日志级别。

## 前端实现方案

本轮不涉及前端实现。

## 后端实现方案

- Redis Lettuce 连接池增加 `max-wait: 3000ms`，避免连接池耗尽时业务线程无限等待。
- 面试 AI 流式 WebClient 增加 180 秒响应超时，保留现有 DNS 解析器与流式调用链路，不改变接口返回结构。
- 面试 AI 流式请求参数校验框与最终统计 ASCII 报告从 INFO 降为 DEBUG，减少生产 INFO 日志被高频流式统计刷屏。
- 保持现有降级、熔断、Mock 兜底和 SSE 输出逻辑不变。

## 数据存储方案

本轮不新增表、不修改字段、不新增索引，不涉及 `db/` 或 `server/db/` SQL 脚本变更。

## stage 更新说明

已更新根目录 `stage.md`，记录后端性能优化第二轮已完成；JPA 移除、实体大字段懒加载、RabbitMQ 增强、上传文件默认删除、Schema 统一等高风险项仍未开始。

## 编译结果

`mvn compile` 通过。

## 构建结果

本轮为后端修改，已执行后端测试与编译验证；不涉及前端构建。

## 当前功能验收说明

- `mvn test -Dtest=RuntimeProtectionConfigTest,InterviewAiServiceImplTest` 通过，结果为 37 个测试，0 失败，0 错误。
- `mvn test` 通过，结果为 523 个测试，0 失败，0 错误。
- `mvn compile` 通过。

## 停止，不继续下一个功能

本轮仅完成后端性能优化第二轮运行时保护范围。连接池大小、Tomcat 线程池、RabbitMQ 增强、数据库结构调整、JPA 移除、实体字段懒加载等均未继续推进，等待后续单独确认。
