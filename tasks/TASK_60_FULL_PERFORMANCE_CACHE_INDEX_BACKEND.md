# 全量性能优化缓存与索引修复后端任务记录

## 当前任务所属模块

后端缓存、Redis 防击穿、统计接口响应、简历诊断任务缓存驱逐、管理端看板缓存同步加载、数据库复合索引与 schema 同步。

## 前端文件定位

本任务文档只记录后端与数据库变更。前端对应任务见 `frontend/tasks/TASK_60_PERFORMANCE_CACHE_ROUTE_ANIMATION_FRONTEND.md`。

## 后端文件定位

- `server/src/main/java/com/airesume/server/config/RedisConfig.java`
- `server/src/main/java/com/airesume/server/service/impl/PublicStatsServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/AdminDashboardServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImpl.java`
- `server/src/test/java/com/airesume/server/config/RedisConfigTest.java`
- `server/src/test/java/com/airesume/server/service/impl/PublicStatsServiceImplTest.java`
- `server/src/test/java/com/airesume/server/service/impl/AdminDashboardServiceImplTest.java`
- `server/src/test/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImplTest.java`
- `server/src/test/java/com/airesume/server/db/SchemaConsistencyTest.java`
- `db/schema.sql`
- `server/db/schema.sql`
- `db/migrations/TASK_60_PERFORMANCE_COMPOSITE_INDEXES.sql`
- `server/db/migrations/TASK_60_PERFORMANCE_COMPOSITE_INDEXES.sql`

## 本轮修改文件清单

- Redis 缓存配置新增 `user:interviewRadar`、`config:membershipPlan`、`interview:jobTarget` 明确 TTL。
- 公共统计接口新增短锁、旧值兜底和短重试，降低热点 miss 时重复打库风险。
- 管理端 dashboard 缓存改为同 key 同步加载，避免同日期范围并发 miss 重复聚合。
- 简历诊断任务缓存从 `allEntries = true` 改为按 `taskId::userId` 精确驱逐。
- 新增 TASK_60 复合索引迁移，根目录 `db/` 与 `server/db/` 双目录同步。
- 扩展 schema 一致性测试，锁定迁移脚本同步和新增索引存在。
- 修复两个既有测试隔离问题：生产 Redis `min-idle` 断言与配额服务自代理字段注入。

## 前端实现方案

无。本文件只记录后端与数据库部分。

## 后端实现方案

- Redis TTL 采用现有 `RedisCacheManager` 配置方式，不新增缓存框架，不改变 key 命名规则。
- `public:stats` 在缓存 miss 时先尝试写入短生命周期互斥锁，拿到锁的请求负责查库并写入主缓存与 stale 缓存；未拿到锁的请求优先返回 stale 值，没有 stale 时短暂等待一次后重读缓存。
- dashboard 继续复用现有 `@Cacheable` 注解，只增加 `sync = true`，并移除与 Spring 同步加载不兼容的 `unless` 条件。
- `resume:task` 精确驱逐通过 `CacheManager` 手动完成，能拿到 `taskId` 与 `userId` 的路径只清理对应 key；批量恢复类路径依赖短 TTL 兜底，避免误删全缓存。

## 数据存储方案

新增 5 个幂等复合索引：

- `resume_diagnosis_task`: `idx_resume_task_user_status_time(user_id, status, create_time)`
- `interview_session`: `idx_interview_session_user_status_time(user_id, status, create_time)`
- `user_notification`: `idx_notification_user_read_time(user_id, read_status, create_time)`
- `community_post`: `idx_community_post_deleted_category_time(is_deleted, category, create_time)`
- `community_comment`: `idx_community_comment_reply_user_actor_time(reply_to_user_id, user_id, create_time)`

当前环境没有数据库连接凭据，未执行真实 `EXPLAIN`。这些索引基于现有查询条件、分页排序和高频访问路径补齐；上线前应在测试库或生产影子库执行 `EXPLAIN` / 慢查询对比后再确认最终收益。

## stage 更新说明

已在根目录 `tasks/stage.md` 追加本轮后端性能优化完成记录，包含缓存、Redis、防击穿、精确驱逐、复合索引、验证结果和未执行真实 `EXPLAIN` 的说明。

## 编译结果

- `mvn.cmd test "-Dtest=RedisConfigTest,AdminDashboardServiceImplTest,ResumeDiagnosisTaskServiceImplTest,PublicStatsServiceImplTest,SchemaConsistencyTest"` 通过，28 个测试，0 失败。
- `mvn.cmd test "-Dtest=RuntimeProtectionConfigTest,UserQuotaServiceImplAtomicDeductionTest"` 通过，11 个测试，0 失败。
- `mvn.cmd test` 通过，574 个测试，0 失败。

## 构建结果

后端本轮以完整 Maven 测试作为编译验证，`mvn.cmd test` 已完成编译与测试。

## 当前功能验收说明

- 新增缓存 TTL 已有单测覆盖。
- `public:stats` 缓存击穿保护已有单测覆盖。
- dashboard 同 key 同步加载已有注解反射测试覆盖。
- `resume:task` 精确驱逐已有单测覆盖。
- TASK_60 双目录迁移与 schema 新索引已有一致性测试覆盖。
- 未执行真实数据库 `EXPLAIN`，需在有数据库连接的环境补充验证。

## 停止，不继续下一功能

本轮只完成用户指定的全量性能优化计划中后端缓存、响应速度和数据库减压部分；不继续扩展新的 API、DTO、业务字段或数据库表结构。
