# TASK_35_NOTIFICATION_AND_PUBLIC_VERSION_PAGINATION_FRONTEND

## 1. 当前任务所属模块
- 前台消息通知模块。
- 公开版本日志模块。
- 管理端会员套餐页样式修复。

## 2. 前端文件定位
- `frontend/app/src/views/admin/AdminMembershipPlanView.vue`
- `frontend/app/src/views/notification/NotificationView.vue`
- `frontend/app/src/views/VersionLogView.vue`
- `frontend/app/src/api/publicVersionLog.js`
- `frontend/app/src/__tests__/api/versionLog.test.js`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/controller/VersionLogController.java`
- `server/src/test/java/com/airesume/server/controller/VersionLogControllerTest.java`
- `tasks/TASK_35_PUBLIC_VERSION_PAGINATION_BACKEND.md`

## 4. 本轮修改文件清单
- 修复会员套餐管理分页在窄屏下左对齐的问题，保持与其他管理页右对齐一致。
- 用户消息通知页补充系统公告、活动公告、版本公告、维护公告的筛选项、标签文案和图标配色。
- “更多动态”版本日志页改为分页查询，不再固定请求首页三条摘要数据。
- 公开版本日志 API 新增分页方法，并补充单元测试。

## 5. 前端实现方案
- 会员套餐页仅调整 `.pagination-wrap` 对齐规则，避免扩大管理端页面结构改动。
- 消息通知页复用已有 `type` 字段，不新增数据库字段；前端补齐 `activity/update/maintenance` 的筛选和展示映射。
- 版本日志更多页通过 `getPublicVersionLogsPage({ page, size })` 查询 `{ records, total, page, size }`，分页总数统一使用 `Number` 归一。

## 6. 后端实现方案
- 前端依赖后端新增的 `GET /api/version-logs` 公开分页接口。
- 首页继续使用原有 `GET /api/version-logs/latest`，只显示三条最近更新，不改变首页展示数量。

## 7. 数据存储方案
- 本轮不新增表和字段。
- 公告类型沿用现有通知 `type` 字段。
- 版本日志分页沿用现有 `sys_version_log` 表，并只查询已发布记录。

## 8. stage 更新说明
- 已更新 `frontend/tasks/stage.md`，记录本轮消息通知类型展示、公开版本日志分页和会员套餐分页对齐修复。

## 9. 编译结果
- 后端编译随 `mvn.cmd test` 完成，通过。

## 10. 构建结果
- 命令：`npm.cmd run build`
- 结果：通过。

## 11. 测试结果
- 命令：`npm.cmd test`
- 结果：8 个测试文件、43 个测试用例通过。

## 12. 当前功能验收说明
- 会员套餐管理分页在桌面和窄屏规则下都保持右对齐。
- 用户消息通知页可筛选并展示活动公告、版本公告、维护公告，系统公告标签不再只显示“系统”。
- 首页最近更新仍显示三条；点击更多动态后进入分页列表，可按页查看更多发布版本日志。

## 13. 停止，不继续下一个功能
- 本轮仅修复用户反馈的分页位置、公告类型展示筛选和公开版本日志分页问题。
- 未继续扩展公告详情页、公告编辑、版本日志搜索或其他新功能。
