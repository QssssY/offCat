# TASK 60 全量性能优化缓存与索引修复后端记录

## 当前任务所属模块

后端缓存、Redis 防击穿、任务缓存失效、管理端统计缓存同步加载、数据库复合索引与 schema 同步。

## 前端文件定位

本任务文件记录后端与数据库部分；前端对应记录见 `frontend/tasks/TASK_PERFORMANCE_CACHE_ROUTE_ANIMATION_FRONTEND.md`。

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

- Redis 缓存区 TTL 补齐：`user:interviewRadar` 5 分钟、`config:membershipPlan` 30 分钟、`interview:jobTarget` 10 分钟。
- `public:stats` 增加短生命周期互斥锁、stale 缓存和短重试，降低热点统计缓存 miss 时的数据库击穿风险。
- 管理端 dashboard 同日期范围统计缓存增加 `sync = true`，避免同 key 并发 miss 同时聚合数据库。
- `resume:task` 状态更新、文本更新、阶段更新、删除和清空历史路径改为按 `taskId::userId` 精确驱逐，避免 `allEntries = true` 粗暴清空任务缓存。
- 新增 TASK_60 复合索引迁移脚本，并同步 `db/` 与 `server/db/` 两份 schema。
- 补充 Redis TTL、防击穿、dashboard 同步加载、任务缓存精确驱逐和 schema 一致性测试。
- 修正两个既有测试隔离问题：生产 Redis `min-idle` 配置断言对齐当前配置；配额原子扣减测试给 `self` 代理字段注入测试服务自身，避免 Mockito spy 下自调用代理为空。

## 前端实现方案

本任务文件不记录前端实现，前端缓存、路由预热、KeepAlive 和动画优化见前端任务文件。

## 后端实现方案

- Redis 缓存配置沿用现有 `RedisCacheManager` 分区 TTL 模式，只新增明确缓存区，不改变序列化策略和现有 key 结构。
- 公共统计读取时先查主缓存；miss 后尝试获取 `public:stats:lock`，拿到锁的请求负责回源数据库并写入主缓存和 stale 缓存；未拿到锁的请求优先返回 stale，缺少 stale 时短暂等待后重试一次。
- 管理端 dashboard 保留原有并发聚合查询，只在 `@Cacheable` 层增加同 key 同步加载，避免同一时间相同日期范围重复击穿。
- 简历诊断任务缓存驱逐不再清空整个 `resume:task` 区域，状态更新路径在可获得 `taskId` 和 `userId` 时按精确 key 驱逐；批量恢复孤儿任务路径依赖短 TTL 兜底，不扩大为全量驱逐。

## 数据存储方案

本轮只新增索引，不新增表、不修改业务字段、不删除已有索引。

新增复合索引：

- `resume_diagnosis_task`: `idx_resume_task_user_status_time (user_id, status, create_time)`
- `interview_session`: `idx_interview_session_user_status_time (user_id, status, create_time)`
- `user_notification`: `idx_notification_user_read_time (user_id, read_status, create_time)`
- `community_post`: `idx_community_post_deleted_category_time (is_deleted, category, create_time)`
- `community_comment`: `idx_community_comment_reply_user_actor_time (reply_to_user_id, user_id, create_time)`

迁移脚本采用项目既有 `information_schema.STATISTICS` 幂等判断方式，重复执行不会重复创建同名索引。

## stage 更新说明

已在根目录 `tasks/stage.md` 追加本轮完成记录，说明后端 Redis 缓存、缓存击穿保护、精确失效、dashboard 同步加载、数据库索引与验证结果。

## 编译结果

- `mvn.cmd test "-Dtest=RedisConfigTest,AdminDashboardServiceImplTest,ResumeDiagnosisTaskServiceImplTest,PublicStatsServiceImplTest,SchemaConsistencyTest"` 通过，28 个测试，0 失败，0 错误。
- `mvn.cmd test "-Dtest=RuntimeProtectionConfigTest,UserQuotaServiceImplAtomicDeductionTest"` 通过，11 个测试，0 失败，0 错误。
- `mvn.cmd test` 通过，574 个测试，0 失败，0 错误。

## 构建结果

后端本轮以 `mvn.cmd test` 覆盖编译与测试验证，未单独执行打包命令。

## 当前功能验收说明

- 缓存 TTL、公共统计防击穿、dashboard 同 key 同步加载、`resume:task` 精确驱逐均已有单元测试覆盖。
- `db/schema.sql` 与 `server/db/schema.sql` 已通过 `SchemaConsistencyTest` 锁定 TASK_60 迁移与五个新增索引同步。
- 当前工作区没有数据库连接配置，未能在真实 MySQL 环境执行 `EXPLAIN`。新增索引基于现有查询形态、分页条件和排序字段补齐；上线前仍需在目标库执行 `EXPLAIN` 或结合慢查询日志确认命中情况。

## 停止，不继续下一个功能

本轮仅完成用户指定的全量性能优化计划中的后端缓存与数据库索引部分，不继续推进读写分离、预聚合表、搜索服务、CDN、Service Worker 或新的接口协议。
