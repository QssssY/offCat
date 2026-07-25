# TASK_ADMIN_VERSION_LOG_FILTER_BACKEND

## 1. 当前任务所属模块
- 管理端模块
- 子模块：版本日志筛选查看后端支持

## 2. 前端文件定位
- `frontend/app/src/views/admin/AdminVersionLogView.vue`
- `frontend/app/src/__tests__/views/AdminVersionLogView.test.js`
- `frontend/tasks/TASK_ADMIN_VERSION_LOG_FILTER_FRONTEND.md`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/controller/AdminVersionLogController.java`
- `server/src/test/java/com/airesume/server/controller/AdminVersionLogControllerTest.java`
- `server/src/test/java/com/airesume/server/db/SchemaConsistencyTest.java`
- `db/schema.sql`
- `server/db/schema.sql`
- `db/migrations/TASK_ADMIN_VERSION_LOG_FILTER_INDEXES.sql`
- `server/db/migrations/TASK_ADMIN_VERSION_LOG_FILTER_INDEXES.sql`

## 4. 本轮修改文件清单
- `server/src/main/java/com/airesume/server/controller/AdminVersionLogController.java`
- `server/src/test/java/com/airesume/server/controller/AdminVersionLogControllerTest.java`
- `server/src/test/java/com/airesume/server/db/SchemaConsistencyTest.java`
- `db/schema.sql`
- `server/db/schema.sql`
- `db/migrations/TASK_ADMIN_VERSION_LOG_FILTER_INDEXES.sql`
- `server/db/migrations/TASK_ADMIN_VERSION_LOG_FILTER_INDEXES.sql`
- `tasks/TASK_ADMIN_VERSION_LOG_FILTER_BACKEND.md`
- `tasks/stage.md`

## 5. 前端实现方案
- 前端传递 `type/status/keyword` 给后端分页查询。
- 前端不做本地筛选，避免当前页数据和后端总数不一致。

## 6. 后端实现方案
- `GET /api/admin/version-logs` 保持原分页返回结构不变，新增可选查询参数：`type`、`status`、`keyword`。
- `type` 支持 `major/minor/patch`，`status` 支持 `0/1`。
- `keyword` 对版本号、标题和内容做参数化 `LIKE` 查询。
- 查询仍按 `createTime` 倒序，保持原列表默认排序。
- 页码最小收敛为 1，单页最大收敛为 100，避免异常大分页请求压到数据库。

## 7. 数据存储方案
- 不新增数据库表和字段。
- 已为 `sys_version_log` 补充筛选查询复合索引：`idx_version_log_filter_time(status, type, create_time)`。
- 已新增可重复执行迁移 `TASK_ADMIN_VERSION_LOG_FILTER_INDEXES.sql`，通过 `information_schema.STATISTICS` 判断索引是否存在。
- 已同步 `db/` 与 `server/db/` 下的 `schema.sql` 和迁移脚本，并由 `SchemaConsistencyTest` 锁定一致性。

## 8. stage 更新说明
- 已更新 `tasks/stage.md`，记录本轮版本日志筛选查看后端支持和性能索引补强完成。
- 前端阶段记录见 `frontend/tasks/stage.md`。

## 9. 编译结果
- 命令：`mvn.cmd -q -DskipTests compile`
- 结果：通过。

## 10. 构建结果
- 前端构建结果见 `frontend/tasks/TASK_ADMIN_VERSION_LOG_FILTER_FRONTEND.md`。

## 11. 测试结果
- RED：新增 `AdminVersionLogControllerTest` 筛选签名用例后，旧控制器方法签名不支持 `type/status/keyword`，测试编译失败。
- RED：新增 `SchemaConsistencyTest#shouldKeepAdminVersionLogFilterIndexMigrationInSyncAndRepeatable` 后，索引迁移文件不存在，测试失败。
- GREEN：`mvn.cmd -q "-Dtest=AdminVersionLogControllerTest,SchemaConsistencyTest" test` 通过。

## 12. 当前功能验收说明
- 管理端版本日志列表现在可按版本类型、发布状态和关键词筛选查看。
- 非法筛选参数会直接返回错误，不进入数据库查询。
- 原创建、编辑、发布、批量发布、删除和批量删除能力保持不变。
- 筛选查询使用服务端分页、页大小上限和复合索引支撑，和通知公告等管理端筛选页面保持同类性能处理方式。

## 13. 停止，不继续下一个功能
- 本轮仅完成管理端版本日志筛选查看能力。
- 未继续扩展公开版本日志页筛选、版本日志导出、时间范围筛选、详情页或监控总览深度指标。
