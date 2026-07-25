# 管理端 Dashboard 单请求加载前端（2026-06-04）

## 当前任务所属模块

管理端数据看板 / 请求量优化。

## 前端文件定位

- `frontend/app/src/api/admin/dashboard.js`
- `frontend/app/src/views/admin/AdminDashboardView.vue`
- `frontend/app/src/__tests__/api/admin.dashboard.test.js`
- `frontend/app/src/__tests__/views/AdminDashboardView.test.js`

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/AdminController.java`
- `server/src/main/java/com/airesume/server/dto/admin/DashboardSummaryResponse.java`
- `server/src/main/java/com/airesume/server/service/impl/AdminDashboardServiceImpl.java`

后端联动记录见 `tasks/TASK_83_ADMIN_DASHBOARD_SUMMARY_BACKEND.md`。

## 本轮修改文件清单

- `api/admin/dashboard.js` 新增 `getAdminDashboardSummary`。
- `AdminDashboardView.vue` 从四个并发请求改为一次聚合请求加载首屏数据。
- 新增 `admin.dashboard.test.js`，锁定聚合 API 请求路径和参数。
- 新增 `AdminDashboardView.test.js`，锁定页面挂载只调用聚合接口，不再调用旧四个接口。
- 全量回归时发现 `DashboardView.test.js` 与 `SettingsView.test.js` 两个旧断言已和当前源码漂移；本轮仅更新测试断言匹配现有普通首页记录图标尺寸和设置页高度变量，不修改对应页面业务 UI。

## 前端实现方案

- 页面继续复用原有筛选栏、图表和数据归一化逻辑，只替换加载入口。
- `loadDashboardData` 调用 `getAdminDashboardSummary({ startDate, endDate, limit })`。
- `applyDashboardSummary` 将后端返回的 `overview/trends/hotJobRoles/businessDistribution` 写回原有响应式状态。
- 旧的 `getAdminDashboardOverview/getAdminDashboardTrends/getAdminDashboardHotJobRoles/getAdminDashboardBusinessDistribution` API 函数保留，避免破坏其它潜在调用方。

## 后端实现方案

后端新增聚合接口 `GET /api/admin/dashboard/summary`，一次返回四块数据；旧四个接口保留兼容。

## 数据存储方案

本轮无前端本地持久化、数据库结构或缓存数据结构变更。

## stage 更新说明

`frontend/tasks/stage.md` 已置顶补充“管理端 Dashboard 单请求加载前端”记录；后端 stage 见根目录 `tasks/stage.md`。

## 编译结果

后端编译结果见 `tasks/TASK_83_ADMIN_DASHBOARD_SUMMARY_BACKEND.md`。

## 构建结果

- RED：`npm.cmd test -- --run src/__tests__/api/admin.dashboard.test.js src/__tests__/views/AdminDashboardView.test.js` 在旧实现下失败，缺少 `getAdminDashboardSummary`，页面仍不具备单请求加载入口。
- GREEN：同一目标测试命令通过，2 个测试文件 / 2 个用例。
- 遗留测试漂移修复验证：`npm.cmd test -- --run src/__tests__/views/DashboardView.test.js src/__tests__/views/SettingsView.test.js` 通过，2 个测试文件 / 54 个用例。
- 前端全量验证：`npm.cmd test` 通过，83 个测试文件 / 584 个用例。
- 构建：`npm.cmd run build` 通过。

## 当前功能验收说明

- 管理端数据看板进入页面、刷新或应用筛选时，只发起一次 `/api/admin/dashboard/summary` 请求。
- 页面仍展示原有 overview、趋势、热门岗位和业务分布四块内容。
- 旧四个前端 API 封装仍保留，兼容旧接口。
- 本轮不修改简历诊断轮询、TTS、数据库或其它页面；仅为恢复全量测试，更新两个非管理端页面的既有测试断言，不改变页面源码。

## 停止，不继续下一个功能

本轮仅完成管理端 dashboard 单请求加载，等待验收，不继续推进其它请求量优化。
