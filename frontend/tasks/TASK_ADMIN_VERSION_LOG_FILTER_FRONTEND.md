# TASK_ADMIN_VERSION_LOG_FILTER_FRONTEND

## 1. 当前任务所属模块
- 管理端模块
- 子模块：版本日志筛选查看前端功能

## 2. 前端文件定位
- `frontend/app/src/views/admin/AdminVersionLogView.vue`
- `frontend/app/src/api/admin/versionLogs.js`
- `frontend/app/src/__tests__/views/AdminVersionLogView.test.js`
- `frontend/app/src/__tests__/api/admin.versionLogs.test.js`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/controller/AdminVersionLogController.java`
- `server/src/test/java/com/airesume/server/controller/AdminVersionLogControllerTest.java`
- `tasks/TASK_ADMIN_VERSION_LOG_FILTER_BACKEND.md`

## 4. 本轮修改文件清单
- `frontend/app/src/views/admin/AdminVersionLogView.vue`
- `frontend/app/src/__tests__/views/AdminVersionLogView.test.js`
- `frontend/tasks/TASK_ADMIN_VERSION_LOG_FILTER_FRONTEND.md`
- `frontend/tasks/stage.md`

## 5. 前端实现方案
- 在版本日志表格上方新增筛选区：关键词、版本类型、发布状态。
- 下拉筛选变更后回到第一页并重新请求列表；关键词支持回车或点击“筛选”触发。
- “重置筛选”清空所有条件并回到第一页。
- 筛选结果区域展示当前页条数和后端分页总数。
- 移动端沿用通知公告筛选栏的纵向排列，避免控件挤压。

## 6. 后端实现方案
- 前端传递 `type/status/keyword` 给后端分页查询。
- 筛选条件由后端参数化拼装，前端不做本地假筛选。

## 7. 数据存储方案
- 不新增本地状态持久化。
- 不新增数据库字段或接口返回字段。
- 后端已补充 `sys_version_log` 复合索引，见后端任务文件。

## 8. stage 更新说明
- 已更新 `frontend/tasks/stage.md`，记录本轮版本日志筛选查看前端完成。
- 后端阶段记录见 `tasks/stage.md`。

## 9. 编译结果
- 后端编译结果见 `tasks/TASK_ADMIN_VERSION_LOG_FILTER_BACKEND.md`。

## 10. 构建结果
- 命令：`npm.cmd run build`
- 结果：通过。

## 11. 测试结果
- RED：新增 `AdminVersionLogView.test.js` 后，旧页面没有 `filterForm` 和筛选方法，目标测试失败。
- GREEN：`npm.cmd test -- --run src/__tests__/views/AdminVersionLogView.test.js src/__tests__/api/admin.versionLogs.test.js` 通过，10 个用例通过。

## 12. 当前功能验收说明
- 管理端版本日志列表支持按版本号/标题/内容关键词、版本类型和发布状态筛选查看。
- 分页、刷新、创建、编辑、发布、批量发布、删除和批量删除沿用原交互。
- 筛选请求回到第一页，避免旧页码在新筛选条件下空页误导。

## 13. 停止，不继续下一个功能
- 本轮仅完成管理端版本日志筛选查看。
- 未继续开发公开版本日志页筛选、导出、时间范围筛选、筛选条件持久化或其它管理端页面能力。
