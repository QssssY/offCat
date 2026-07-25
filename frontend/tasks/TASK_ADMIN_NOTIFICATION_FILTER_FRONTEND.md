# TASK_ADMIN_NOTIFICATION_FILTER_FRONTEND

## 1. 当前任务所属模块
- 管理端模块
- 子模块：通知公告筛选查看前端功能

## 2. 前端文件定位
- `frontend/app/src/views/admin/AdminNotificationView.vue`
- `frontend/app/src/api/admin/notifications.js`
- `frontend/app/src/__tests__/api/admin.notifications.test.js`
- `frontend/app/src/__tests__/views/AdminNotificationView.test.js`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/controller/AdminNotificationController.java`
- `server/src/test/java/com/airesume/server/controller/AdminNotificationControllerTest.java`
- `tasks/TASK_ADMIN_NOTIFICATION_FILTER_BACKEND.md`

## 4. 本轮修改文件清单
- `frontend/app/src/views/admin/AdminNotificationView.vue`
- `frontend/app/src/api/admin/notifications.js`
- `frontend/app/src/__tests__/api/admin.notifications.test.js`
- `frontend/app/src/__tests__/views/AdminNotificationView.test.js`
- `frontend/tasks/TASK_ADMIN_NOTIFICATION_FILTER_FRONTEND.md`
- `frontend/tasks/stage.md`

## 5. 前端实现方案
- 在通知公告表格上方新增筛选区：关键词、公告类型、发布状态、目标用户。
- 下拉筛选变更后回到第一页并重新请求列表；关键词支持回车或点击“筛选”触发。
- “重置筛选”清空所有条件并回到第一页。
- API 封装统一剔除空字符串、`null`、`undefined` 查询参数，避免后端收到无意义筛选值。
- 筛选结果区域展示当前页条数和后端分页总数。

## 6. 后端实现方案
- 前端传递 `type/status/targetType/keyword` 给后端分页查询。
- `targetType` 中“全部用户”映射为后端已有枚举值 `all`，与“全部目标用户”筛选项区分。

## 7. 数据存储方案
- 不新增本地状态持久化。
- 不新增数据库字段或接口返回字段。

## 8. stage 更新说明
- 已更新 `frontend/tasks/stage.md`，记录本轮通知公告筛选查看前端完成。
- 后端阶段记录见 `tasks/stage.md`。

## 9. 编译结果
- 后端编译结果见 `tasks/TASK_ADMIN_NOTIFICATION_FILTER_BACKEND.md`。

## 10. 构建结果
- 命令：`npm.cmd run build`
- 结果：通过。

## 11. 测试结果
- RED：新增前端 API 和页面测试后，旧实现未清理空筛选参数且页面没有 `filterForm`，`npm.cmd test -- --run src/__tests__/api/admin.notifications.test.js src/__tests__/views/AdminNotificationView.test.js` 失败。
- GREEN：`npm.cmd test -- --run src/__tests__/api/admin.notifications.test.js src/__tests__/views/AdminNotificationView.test.js` 通过，11 个用例通过。

## 12. 当前功能验收说明
- 管理端通知公告列表支持按标题/内容关键词、类型、状态和目标用户筛选查看。
- 分页、刷新、创建、发布、批量发布、删除和批量删除沿用原交互。
- 移动端筛选项改为纵向排列，避免控件挤压。

## 13. 停止，不继续下一个功能
- 本轮仅完成通知公告筛选查看。
- 未继续开发公告编辑、导出、时间范围筛选、筛选条件持久化或其它管理端页面能力。
