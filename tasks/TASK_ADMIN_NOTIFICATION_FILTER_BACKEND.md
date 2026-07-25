# TASK_ADMIN_NOTIFICATION_FILTER_BACKEND

## 1. 当前任务所属模块
- 管理端模块
- 子模块：通知公告筛选查看后端支持

## 2. 前端文件定位
- `frontend/app/src/views/admin/AdminNotificationView.vue`
- `frontend/app/src/api/admin/notifications.js`
- `frontend/tasks/TASK_ADMIN_NOTIFICATION_FILTER_FRONTEND.md`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/controller/AdminNotificationController.java`
- `server/src/test/java/com/airesume/server/controller/AdminNotificationControllerTest.java`
- `server/src/test/java/com/airesume/server/db/SchemaConsistencyTest.java`
- `db/schema.sql`
- `server/db/schema.sql`
- `db/migrations/TASK_ADMIN_NOTIFICATION_FILTER_INDEXES.sql`
- `server/db/migrations/TASK_ADMIN_NOTIFICATION_FILTER_INDEXES.sql`

## 4. 本轮修改文件清单
- `server/src/main/java/com/airesume/server/controller/AdminNotificationController.java`
- `server/src/test/java/com/airesume/server/controller/AdminNotificationControllerTest.java`
- `server/src/test/java/com/airesume/server/db/SchemaConsistencyTest.java`
- `db/schema.sql`
- `server/db/schema.sql`
- `db/migrations/TASK_ADMIN_NOTIFICATION_FILTER_INDEXES.sql`
- `server/db/migrations/TASK_ADMIN_NOTIFICATION_FILTER_INDEXES.sql`
- `tasks/TASK_ADMIN_NOTIFICATION_FILTER_BACKEND.md`
- `tasks/stage.md`

## 5. 前端实现方案
- 前端新增通知公告筛选栏，筛选条件通过查询参数传给后端。
- 前端不做本地假分页筛选，避免当前页数据与总数不一致。

## 6. 后端实现方案
- `GET /api/admin/notifications` 保持原分页返回结构不变，新增可选查询参数：`type`、`status`、`targetType`、`keyword`。
- `type` 支持 `system/activity/update/maintenance`，`status` 支持 `0/1`，`targetType` 支持 `all/vip/normal`。
- `keyword` 对公告标题和内容做参数化 `LIKE` 查询。
- 查询仍按 `createTime` 倒序，保持原列表默认排序。
- 管理端筛选列表页码最小收敛为 1，单页最大收敛为 100，避免异常大分页请求直接打到数据库。

## 7. 数据存储方案
- 不新增数据库表和字段。
- 已为 `sys_admin_notification` 补充筛选查询复合索引：`idx_admin_notification_filter_time(target_type, status, type, create_time)`。
- 已新增可重复执行迁移 `TASK_ADMIN_NOTIFICATION_FILTER_INDEXES.sql`，通过 `information_schema.STATISTICS` 判断索引是否存在，避免重复执行失败。
- 已同步 `db/` 与 `server/db/` 下的 `schema.sql` 和迁移脚本，并由 `SchemaConsistencyTest` 锁定一致性。

## 8. stage 更新说明
- 已更新 `tasks/stage.md`，记录本轮通知公告筛选查看后端支持和性能索引补强完成。
- 前端阶段记录见 `frontend/tasks/stage.md`。

## 9. 编译结果
- 命令：`mvn.cmd -q -DskipTests compile`
- 结果：通过。

## 10. 构建结果
- 前端构建结果见 `frontend/tasks/TASK_ADMIN_NOTIFICATION_FILTER_FRONTEND.md`。

## 11. 测试结果
- RED：新增 `AdminNotificationControllerTest#getNotificationListShouldApplyFilterParameters` 后，旧控制器方法签名不支持筛选参数，`mvn.cmd -q "-Dtest=AdminNotificationControllerTest" test` 编译失败。
- GREEN：`mvn.cmd -q "-Dtest=AdminNotificationControllerTest" test` 通过。
- RED：新增 `AdminNotificationControllerTest#getNotificationListShouldLimitPageSizeForPerformance` 后，旧实现会把 `size=500` 直接传入分页查询，测试失败。
- RED：新增 `SchemaConsistencyTest#shouldKeepAdminNotificationFilterIndexMigrationInSyncAndRepeatable` 后，索引迁移文件不存在，测试失败。
- GREEN：`mvn.cmd -q "-Dtest=AdminNotificationControllerTest,SchemaConsistencyTest" test` 通过。

## 12. 当前功能验收说明
- 管理端通知公告列表现在可按公告类型、状态、目标用户和关键词筛选查看。
- 非法筛选参数会直接返回错误，不进入数据库查询。
- 原创建、发布、批量发布、删除和批量删除能力保持不变。
- 筛选查询使用服务端分页、页大小上限和复合索引支撑，和管理端其他性能补强页面保持同类处理方式。

## 13. 停止，不继续下一个功能
- 本轮仅完成管理端通知公告筛选查看能力。
- 未继续扩展编辑公告、导出公告、公告时间范围筛选或其它管理端功能。
