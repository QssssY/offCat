# TASK_34_ADMIN_NEW_PAGES_PAGINATION_BATCH_FRONTEND

## 1. 当前任务所属模块
- 管理端模块
- 子模块：新增管理端页面分页、表格样式、批量操作前端修复

## 2. 前端文件定位
- `frontend/app/src/views/admin/AdminAuditLogView.vue`
- `frontend/app/src/views/admin/AdminNotificationView.vue`
- `frontend/app/src/views/admin/AdminVersionLogView.vue`
- `frontend/app/src/views/admin/AdminMembershipOrderView.vue`
- `frontend/app/src/views/admin/AdminGrowthConfigView.vue`
- `frontend/app/src/views/admin/AdminMembershipPlanView.vue`
- `frontend/app/src/api/admin/notifications.js`
- `frontend/app/src/api/admin/versionLogs.js`
- `frontend/app/src/api/admin/growthConfig.js`
- `frontend/app/src/api/admin/membership.js`
- `frontend/app/src/__tests__/api/*`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/controller/AdminNotificationController.java`
- `server/src/main/java/com/airesume/server/controller/AdminVersionLogController.java`
- `server/src/main/java/com/airesume/server/controller/AdminGrowthConfigController.java`
- `server/src/main/java/com/airesume/server/controller/AdminMembershipController.java`
- `tasks/TASK_34_ADMIN_NEW_PAGES_PAGINATION_BATCH_BACKEND.md`

## 4. 本轮修改文件清单
- 管理端新增页面统一分页组件写法，改为 `pagination-wrap`、`background`、受控 `current-page/page-size` 和显式事件。
- 通知公告、版本日志、成长配置、会员套餐页面新增选择列和批量操作按钮。
- 审计日志、订单管理保持只读，只修复分页类型、表格铺满和视觉样式。
- 管理端 API 封装新增批量发布、批量删除、批量启停方法。
- 前端 API 单元测试补充批量接口请求格式断言。

## 5. 前端实现方案
- 分页总数统一使用 `Number(data.total || 0)` 归一化，避免 Element Plus 收到字符串 `total` 后报 prop 类型错误。
- 页面样式向已有管理端页面靠拢：`el-card.table-card`、`border`、`stripe`、`table-header`、`action-group/action-btn`。
- 可操作数据页选中后展示批量按钮；批量发布只提交草稿记录 ID，批量删除和批量启停提交当前选择行 ID。
- 删除或批量操作后刷新当前页；若当前页数据被删空，则按当前总数回退到有效页重新查询。

## 6. 后端实现方案
- 前端依赖后端新增的批量接口，不在前端循环调用单条接口。
- 批量接口保持 1 到 100 条 ID 的边界，详见后端任务文件。

## 7. 数据存储方案
- 本轮不新增数据库表和字段。
- 仅调用已有通知、版本日志、成长配置、会员套餐数据表的查询和更新能力。

## 8. stage 更新说明
- 已更新 `frontend/tasks/stage.md`，标记本轮管理端新增页面分页、样式和批量操作修复已完成，等待人工验收。

## 9. 编译结果
- 后端编译随 `mvn.cmd test` 完成，通过。

## 10. 构建结果
- 命令：`npm.cmd run build`
- 结果：通过。

## 11. 测试结果
- 命令：`npm.cmd test`
- 结果：8 个测试文件、41 个测试用例通过。

## 12. 当前功能验收说明
- 审计日志、通知公告、版本日志、订单管理、成长配置、会员套餐分页不再使用 Element Plus 弃用组合。
- 分页 `total` 强制为数字，修复 `Expected Number, got String` 报错。
- 通知公告支持批量发布草稿和批量删除。
- 版本日志支持批量发布草稿和批量删除。
- 成长配置支持批量删除。
- 会员套餐支持批量启用、批量禁用和批量删除。
- 审计日志和订单管理保持只读，不新增批量删除或状态变更。

## 13. 停止，不继续下一个功能
- 本轮仅完成管理端新增页面分页、样式和批量操作修复。
- 未继续开发新的管理端功能、导出能力或订单/审计日志批量变更能力。
