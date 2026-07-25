# 管理端 Dashboard 聚合接口后端（2026-06-04）

## 当前任务所属模块

管理端数据看板 / 请求量优化。

## 前端文件定位

- `frontend/app/src/api/admin/dashboard.js`
- `frontend/app/src/views/admin/AdminDashboardView.vue`

前端联动记录见 `frontend/tasks/TASK_83_ADMIN_DASHBOARD_SUMMARY_FRONTEND.md`。

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/AdminController.java`
- `server/src/main/java/com/airesume/server/service/AdminDashboardService.java`
- `server/src/main/java/com/airesume/server/service/impl/AdminDashboardServiceImpl.java`
- `server/src/main/java/com/airesume/server/dto/admin/DashboardSummaryResponse.java`
- `server/src/main/java/com/airesume/server/config/RedisConfig.java`

## 本轮修改文件清单

- 新增 `DashboardSummaryResponse`，聚合 `overview/trends/hotJobRoles/businessDistribution` 四块数据。
- `AdminDashboardService` 新增 `getDashboardSummary(startDate, endDate, hotRoleLimit)`。
- `AdminDashboardServiceImpl` 新增聚合实现，并为 `admin:dashboardSummary` 启用 5 分钟同步缓存。
- `AdminController` 新增 `GET /api/admin/dashboard/summary`，参数兼容 `startDate/endDate/limit`。
- `RedisConfig` 注册 `admin:dashboardSummary` 5 分钟 TTL。
- 新增/调整后端测试：`AdminDashboardServiceImplTest`、`AdminControllerTest`、`RedisConfigTest`。

## 前端实现方案

本轮后端提供聚合接口供前端单请求加载；前端保留旧四个接口 API 封装，不破坏兼容调用方。

## 后端实现方案

- 聚合接口只编排现有四块看板数据，不复制统计 SQL，不改变旧四个接口的响应结构。
- 旧接口 `/dashboard/overview`、`/dashboard/trends`、`/dashboard/hot-job-roles`、`/dashboard/business-distribution` 保留兼容。
- 新接口统一返回：
  - `overview`
  - `trends`
  - `hotJobRoles`
  - `businessDistribution`
- 聚合接口使用 `limit` 参数控制热门岗位数量，内部传给既有热门岗位查询。
- 新增 `admin:dashboardSummary` 缓存区，TTL 5 分钟，与现有 dashboard 聚合缓存策略保持一致。

## 数据存储方案

本轮无数据库表、字段、索引或迁移脚本变更；只新增后端响应 DTO 和缓存配置。

## stage 更新说明

根目录 `tasks/stage.md` 已置顶补充“管理端 Dashboard 聚合接口后端”记录；前端 stage 见 `frontend/tasks/stage.md`。

## 编译结果

- RED：`mvn.cmd -q "-Dtest=AdminDashboardServiceImplTest,AdminControllerTest" test` 在旧实现下编译失败，缺少 `DashboardSummaryResponse` 和 `getDashboardSummary`。
- GREEN：`mvn.cmd -q "-Dtest=AdminDashboardServiceImplTest,AdminControllerTest,RedisConfigTest" test` 通过。
- 编译：`mvn.cmd -q -DskipTests compile` 通过。
- 后端全量：`mvn.cmd -q test` 通过；日志中的 `GlobalExceptionHandlerTest` 与 `InterviewServiceTest` 堆栈为测试主动覆盖异常路径，不是失败。

## 构建结果

本轮后端自身无前端构建；前端构建结果见 `frontend/tasks/TASK_83_ADMIN_DASHBOARD_SUMMARY_FRONTEND.md`。

## 当前功能验收说明

- 管理端可通过 `GET /api/admin/dashboard/summary` 一次获取 dashboard 首屏四块数据。
- 旧四个 dashboard 接口仍可继续使用，避免破坏兼容调用方。
- 新聚合接口具备 5 分钟缓存，降低页面切换/刷新时的重复聚合压力。
- 本轮无数据库变更，不涉及简历诊断轮询、TTS 或其它模块。

## 停止，不继续下一个功能

本轮仅完成管理端 dashboard 聚合接口后端能力，等待验收，不继续推进其它请求量优化。
