# 后端性能优化第四轮任务记录

## 当前任务所属模块

后端认证稳定性与本地兜底内存清理小步优化。

## 前端文件定位

本轮不涉及前端文件，不修改页面、不修改前端接口调用。

## 后端文件定位

- `server/src/main/java/com/airesume/server/service/impl/AuthServiceImpl.java`
- `server/src/test/java/com/airesume/server/service/impl/AuthServiceImplTest.java`

## 本轮修改文件清单

- 更新 `AuthServiceImpl`，为 Redis 故障兜底的本地登录失败记录 Map 增加定时过期清理。
- 补充 `AuthServiceImplTest`，验证过期本地登录失败记录会被清理，未过期记录会被保留。

## 前端实现方案

本轮不涉及前端实现。

## 后端实现方案

- 保留现有 Redis 登录限流逻辑不变，Redis 正常时仍以 `login:attempts:{username}` 作为主路径。
- 保留 Redis 异常时的本地 `ConcurrentHashMap` 兜底逻辑不变，继续按原有 15 分钟过期时间判断登录失败次数。
- 新增 `cleanupExpiredLocalLoginAttempts()`，通过 `@Scheduled` 默认每 30 分钟清理一次过期本地记录，避免冷门账号过期记录长期留在内存中。
- 清理方法只删除 `LoginAttemptRecord.isExpired()` 为 true 的条目，不重置未过期限流记录，不改变登录接口返回结构和错误文案。

## 数据存储方案

本轮不新增表、不修改字段、不新增索引，不涉及 `db/` 或 `server/db/` SQL 脚本变更。

## stage 更新说明

已更新根目录 `stage.md`，记录后端性能优化第四轮已完成；高风险项仍未开始。

## 编译结果

`mvn compile` 通过。

## 构建结果

本轮为后端修改，已执行后端测试与编译验证；不涉及前端构建。

## 当前功能验收说明

- `mvn test -Dtest=AuthServiceImplTest` 通过，结果为 11 个测试，0 失败，0 错误。
- `mvn test` 通过，结果为 525 个测试，0 失败，0 错误。
- `mvn compile` 通过。

## 停止，不继续下一功能

本轮仅完成本地登录限流 Map 过期清理这一项低风险优化。JPA 移除、实体大字段懒加载、RabbitMQ 增强、上传文件默认过期删除、Schema 统一、HikariCP/Tomcat 生产容量参数调优均未继续推进，等待后续单独确认。
