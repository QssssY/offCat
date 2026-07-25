# 模拟面试结束进入报告等待页白屏优化

## 当前任务所属模块

模拟面试结束流程、报告生成等待页、用户端路由冷加载体验。

## 前端文件定位

- `frontend/app/src/router/routeLoaders.js`
- `frontend/app/src/router/index.js`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/layouts/MainLayout.vue`
- `frontend/app/src/__tests__/router/routeLoaders.test.js`
- `frontend/app/src/__tests__/layouts/MainLayout.test.js`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`

## 后端文件定位

本轮不涉及后端文件、接口、数据库或服务层改动。

## 本轮修改文件清单

- `routeLoaders.js`：新增 `interviewReportRouteLoader` 和 `prefetchInterviewReportRoute()`，用于结束面试前预取报告等待页 chunk。
- `router/index.js`：报告页路由改用共享 `interviewReportRouteLoader`，保证预取和实际路由加载命中同一个 chunk。
- `InterviewSessionView.vue`：打开结束确认弹窗时提前预取报告页；确认结束后不再回当前页重新拉取完整会话详情，直接进入报告等待页。
- `MainLayout.vue`：路由加载占位范围扩展到 `/interview/report`，冷加载超过阈值时显示“正在打开面试报告”骨架，避免主内容区空白。
- `routeLoaders.test.js`、`MainLayout.test.js`、`InterviewSessionView.test.js`：补充回归测试，锁定报告页预取、结束后直接跳转等待页和报告页加载占位。

## 前端实现方案

- 复用现有 route loader / prefetch 模式，不新增 Service Worker 或持久缓存。
- 用户点击“结束面试”打开确认弹窗时，立即预取 `InterviewReportView` chunk，让用户确认和后端结束请求期间并行完成页面资源准备。
- 结束接口返回后，等待已启动的报告页预取完成；预取失败不阻断路由，交给 Vue Router 正常重试加载。
- 成功结束后直接 `router.push('/interview/report/:sessionId')`，报告页继续使用已有轻量状态轮询展示生成等待态。
- 删除结束成功后在面试页重新拉取完整会话详情的等待路径，减少点击确认后的额外停顿。
- MainLayout 对报告页冷加载提供局部骨架占位，减少从无布局面试页切换到带布局报告页时的白屏感。

## 后端实现方案

无后端实现。本轮不修改结束面试接口、报告生成接口或状态轮询接口。

## 数据存储方案

无数据存储改动。本轮只调整前端路由加载和跳转时机，不新增本地存储、缓存字段或数据库结构。

## stage 更新说明

`frontend/tasks/stage.md` 顶部新增“模拟面试结束进入报告等待页白屏优化”记录，说明原因、实现范围、验证结果和停止边界。

## 编译结果

- `npm.cmd test -- --run src/__tests__/router/routeLoaders.test.js src/__tests__/layouts/MainLayout.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 46 个用例。
- `npm.cmd test -- --run src/__tests__/api/interview.test.js src/__tests__/views/InterviewReportView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/router/routeLoaders.test.js src/__tests__/layouts/MainLayout.test.js` 通过，5 个测试文件 / 55 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

结束面试时，确认弹窗打开后会提前加载报告等待页；确认结束成功后直接进入报告生成等待页，不再先停留当前页重新拉完整会话详情。若报告页 chunk 仍在冷加载，主布局会显示“正在打开面试报告”的骨架占位。

## 停止，不继续下一功能

本轮只处理模拟面试结束后进入报告等待页的停顿和白屏，不继续扩展报告内容、报告导出、后端生成策略、语音能力或其它面试流程。
