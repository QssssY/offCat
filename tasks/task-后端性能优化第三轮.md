# 后端性能优化第三轮任务记录

## 当前任务所属模块

后端性能与稳定性缓存策略小步优化。

## 前端文件定位

本轮不涉及前端文件，不修改页面、不修改前端接口调用。

## 后端文件定位

- `server/src/main/java/com/airesume/server/config/RedisConfig.java`
- `server/src/test/java/com/airesume/server/config/RedisConfigTest.java`

## 本轮修改文件清单

- 更新 `RedisConfig`，为 `admin:dashboardTrends` 显式注册 10 分钟 TTL。
- 补充 `RedisConfigTest`，验证管理后台趋势缓存区 TTL 为 10 分钟。

## 前端实现方案

本轮不涉及前端实现。

## 后端实现方案

- 管理后台趋势接口已经在第一轮从逐日 N+1 查询改为聚合查询，并通过 `@Cacheable(value = "admin:dashboardTrends")` 使用 Spring Cache。
- 本轮只在 Redis 缓存管理器中显式注册 `admin:dashboardTrends` 缓存区，TTL 设置为 10 分钟，避免该缓存区继续使用默认 5 分钟策略。
- 为避免测试依赖真实 Redis 或 Spring 容器，将初始缓存区配置抽为包内方法，生产 `cacheManager` 和单元测试复用同一份配置。
- 不改变 `DashboardTrendResponse` 返回结构，不改变接口路径，不修改查询 SQL，不新增 Maven 依赖。

## 数据存储方案

本轮不新增表、不修改字段、不新增索引，不涉及 `db/` 或 `server/db/` SQL 脚本变更。

## stage 更新说明

已更新根目录 `stage.md`，记录后端性能优化第三轮已完成；高风险项仍未开始。

## 编译结果

`mvn compile` 通过。

## 构建结果

本轮为后端修改，已执行后端测试与编译验证；不涉及前端构建。

## 当前功能验收说明

- `mvn test -Dtest=RedisConfigTest` 通过，结果为 2 个测试，0 失败，0 错误。
- `mvn test` 通过，结果为 524 个测试，0 失败，0 错误。
- `mvn compile` 通过。

## 停止，不继续下一功能

本轮仅完成 `admin:dashboardTrends` 缓存 TTL 注册这一项低风险优化。JPA 移除、实体大字段懒加载、RabbitMQ 增强、上传文件默认过期删除、Schema 统一、HikariCP/Tomcat 生产容量参数调优均未继续推进，等待后续单独确认。
