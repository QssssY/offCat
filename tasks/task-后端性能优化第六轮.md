# 后端性能优化第六轮任务记录

## 当前任务所属模块

后端简历诊断直连处理路由并发保护小步优化。

## 前端文件定位

本轮不涉及前端文件，不修改页面、不修改前端接口调用、不新增前端状态。

## 后端文件定位

- `server/src/main/java/com/airesume/server/mq/DirectProcessRouter.java`
- `server/src/test/java/com/airesume/server/mq/DirectProcessRouterTest.java`

## 本轮修改文件清单

- 更新 `DirectProcessRouter`，将直连处理容量判断从单纯读取计数改为原子预占直连槽位。
- 新增 `submitDirectIfCapacity`，用于把容量检查与任务提交合并为一个原子语义入口。
- 保留旧的 `canProcessDirectly()` + `submitDirect()` 调用链，并通过当前线程预占标记保持兼容，避免本轮改动扩大到诊断任务主流程。
- 新增 `DirectProcessRouterTest`，覆盖原子预占和旧调用链兼容行为。

## 前端实现方案

本轮不涉及前端实现。

## 后端实现方案

- 使用 `AtomicInteger.compareAndSet` 对直连处理中任务数做原子预占，避免并发请求同时通过阈值检查后一起提交，导致短时超过 `app.diagnosis.direct-threshold`。
- 任务提交到本地异步线程池后，在执行完成的 `finally` 中释放直连槽位，保留原有处理链路和日志行为。
- 新增 `submitDirectIfCapacity(Long taskId, Long userId, String fileUrl)` 作为更安全的推荐入口，返回 `false` 时调用方可回退 MQ。
- 为兼容现有主流程，`canProcessDirectly()` 返回 `true` 时会为当前线程预占一个槽位，随后 `submitDirect()` 复用该预占，不重复增加计数。
- 本轮未修改 `ResumeDiagnosisTaskServiceImpl`，避免因大文件编码和主链路改动带来额外风险。

## 数据存储方案

本轮不新增表、不修改字段、不新增索引，不涉及 `db/` 或 `server/db/` SQL 脚本变更。

## stage 更新说明

已更新根目录 `stage.md`，记录后端性能优化第六轮已完成；高风险项仍保持未开始状态。

## 编译结果

`mvn compile` 通过。

## 构建结果

本轮为后端修改，已执行后端测试与编译验证；不涉及前端构建。

## 当前功能验收说明

- `mvn test -Dtest=DirectProcessRouterTest` 通过，结果为 2 个测试，0 失败，0 错误。
- `mvn test` 通过，结果为 528 个测试，0 失败，0 错误。
- `mvn compile` 通过。

## 停止，不继续下一功能

本轮仅完成 `DirectProcessRouter` 并发容量原子预占这一项低侵入优化。JPA 移除、实体大字段懒加载、RabbitMQ 增强、上传文件默认过期删除、Schema 统一、HikariCP/Tomcat 生产容量参数调优均未继续推进，等待后续单独确认。
