# 用户端简历诊断路由白屏性能回归修复任务记录

## 当前任务所属模块

用户端简历诊断上传页、诊断结果页、用户端主布局路由切换反馈和结果页 chunk 预取体验。

## 前端文件定位

- `frontend/app/src/layouts/MainLayout.vue`
- `frontend/app/src/views/resume/UploadView.vue`
- `frontend/app/src/__tests__/layouts/MainLayout.test.js`
- `frontend/app/src/__tests__/views/ResumeUploadView.test.js`

## 后端文件定位

本轮不修改后端接口、服务、缓存、任务轮询或数据库结构。`/api/resume/upload`、任务详情查询和结果页轮询逻辑保持不变。

## 本轮修改文件清单

- `MainLayout.vue`：为用户端简历诊断相关路由增加页面级加载占位，在路由切换超过 120ms 时除顶部进度条外显示结构化占位，避免内容区裸白。
- `MainLayout.vue`：补充 `loadingRoutePath`、`isResumeDiagnosisRoute` 和 `routeLoadingTargetText`，只对 `/resume/upload` 与 `/resume/result` 展示诊断专用占位，不影响其他用户端页面。
- `MainLayout.vue`：确认非 KeepAlive 页面切换继续使用 `Transition mode="out-in"`，避免进入页和离开页切换时出现不稳定重叠或空窗。
- `UploadView.vue`：上传请求开始后保存 `prefetchUserRoute('/resume/result')` 的 Promise，上传成功拿到任务 ID 后先等待结果页 chunk 预取完成，再跳转 `/resume/result/:taskId`。
- `MainLayout.test.js`：补充简历诊断路由加载占位、目标文案和诊断路由判断的回归断言。
- `ResumeUploadView.test.js`：补充上传页等待结果页预取 Promise 后再跳转的回归断言。

## 前端实现方案

- 根因定位：简历诊断结果页是较重的动态 import 页面，上传页此前只触发预取但没有等待预取完成；上传接口成功后立即跳转时，如果结果页 chunk 尚未下载/解析完成，用户端内容区只剩顶部细进度条，容易感知为白屏。
- 布局兜底：在 `MainLayout` 内为 `/resume/upload` 和 `/resume/result` 增加页面级占位卡片，路由加载超过 120ms 才显示，快速切换不闪烁，慢切换不裸白。
- 跳转优化：上传页在提交阶段复用上传耗时窗口预取结果页 chunk，并在任务 ID 返回后等待该 Promise，再执行 `router.push`，缩短进入结果页后的空窗。
- 范围控制：不改变上传接口、任务状态轮询、结果页状态展示、错误码、DTO 字段或后端缓存策略；不引入 Service Worker、SSR、新状态库或新 UI 库。

## 后端实现方案

无后端改动。若结果页进入后仍长期停留在 AI 分析中，那属于后端任务处理耗时或轮询链路问题，应单独基于任务状态、接口耗时和日志继续排查。

## 数据存储方案

无新增持久化。结果页预取状态只保存在当前上传页运行时变量中，页面刷新后自然失效。

## stage 更新说明

已在 `frontend/tasks/stage.md` 追加本轮用户端简历诊断路由白屏回归修复记录，说明根因、范围、验证结果和停止边界。

## 编译结果

- `npm.cmd test -- --run src/__tests__/layouts/MainLayout.test.js src/__tests__/views/ResumeUploadView.test.js` 通过，2 个测试文件，2 个用例。
- `npm.cmd test -- --run src/__tests__/layouts/MainLayout.test.js src/__tests__/views/ResumeUploadView.test.js src/__tests__/views/ResumeResultView.test.js src/__tests__/router/routeLoaders.test.js src/__tests__/components/AppHeader.test.js src/__tests__/themeTokens.test.js` 通过，6 个测试文件，25 个用例。
- `npm.cmd test -- --run` 通过，67 个测试文件，424 个用例。

## 构建结果

- `npm.cmd run build` 通过，Vite 生产构建成功，动态 import 与 gzip 产物生成正常。

## 当前功能验收说明

- 用户从简历上传页提交后，前端会利用上传等待时间预取结果页 chunk，并在跳转前等待预取完成，降低进入结果页时的白屏概率。
- 用户进入 `/resume/upload` 或 `/resume/result/:taskId` 期间，如果路由切换超过 120ms，主内容区会显示诊断专用占位卡片和顶部进度条，不再只有空白内容区。
- 结果页内部的任务排队、处理中、完成、失败和轮询逻辑保持原样。
- 本轮未执行真实浏览器性能面板人工抽样，建议在实际登录态下复测从首页/导航进入 `/resume/upload`、选择 PDF 提交、跳转 `/resume/result/:taskId` 的完整链路。

## 停止，不继续下一个功能

本轮只修复用户反馈的用户端简历诊断路由白屏体验，不继续扩展后端任务性能、接口缓存、结果页视觉重构或新的诊断功能。
