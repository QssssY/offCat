# 全量性能优化缓存与路由动画前端任务记录

## 当前任务所属模块

用户端页面切换流畅度、路由预加载、KeepAlive 覆盖、前端 GET 请求缓存、高频页面动效渲染成本和长列表渲染隔离。

## 前端文件定位

- `frontend/app/src/utils/apiCache.js`
- `frontend/app/src/api/stats.js`
- `frontend/app/src/api/membership.js`
- `frontend/app/src/api/growth.js`
- `frontend/app/src/api/interview.js`
- `frontend/app/src/api/notification.js`
- `frontend/app/src/api/community.js`
- `frontend/app/src/router/routeLoaders.js`
- `frontend/app/src/router/index.js`
- `frontend/app/src/layouts/MainLayout.vue`
- `frontend/app/src/styles/index.css`
- `frontend/app/src/views/DashboardView.vue`
- `frontend/app/src/views/MembershipView.vue`
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/views/interview/InterviewHistoryView.vue`
- `frontend/app/src/views/resume/HistoryView.vue`
- `frontend/app/src/views/community/CommunityView.vue`
- `frontend/app/src/views/community/MyActivity.vue`
- `frontend/app/src/views/community/PostDetailView.vue`
- `frontend/app/src/views/growth/GrowthCenterView.vue`
- `frontend/app/src/views/interview/InterviewEntryView.vue`
- `frontend/app/src/views/resume/UploadView.vue`
- `frontend/app/src/views/resume/ResultView.vue`
- `frontend/app/src/components/community/CommentSection.vue`
- `frontend/app/src/components/community/PostEditor.vue`
- `frontend/app/src/components/OnboardingGuide.vue`
- `frontend/app/src/components/OnboardingTaskCard.vue`
- `frontend/app/src/components/common/AiLoadingState.vue`

## 后端文件定位

前端请求缓存调用现有 API，不修改后端接口路径、DTO 字段或响应结构。后端对应缓存与索引任务见 `tasks/TASK_60_FULL_PERFORMANCE_CACHE_INDEX_BACKEND.md`。

## 本轮修改文件清单

- 新增 `apiCache.js`，提供轻量 GET 缓存、TTL、按 key 清理和按前缀清理能力。
- 公共统计、会员套餐、成长中心、岗位配置、通知未读数和社区列表接入缓存。
- 发帖、点赞、收藏、会员升级等主动操作后精确失效相关缓存。
- `routeLoaders.js` 扩展 idle 预热范围，保留 hover、touch、focus 预加载。
- `MainLayout.vue` 扩展用户端高频页面 `KeepAlive` include。
- 高频页面补充组件 `name`，保证 KeepAlive 命中。
- 用户端核心页面将 `transition: all` 改为明确属性。
- 社区列表、个人动态等长列表补充 `content-visibility: auto` 或渲染隔离样式。
- 补充前端缓存、路由预热、KeepAlive 和性能缓存 API 测试。
- 修复既有测试隔离问题：Element Plus mock 补齐组件导出，会员套餐测试样本补齐后端实际使用的 `sort` 字段。

## 前端实现方案

- GET 缓存只包裹稳定数据，不缓存 POST、PUT、DELETE，不缓存 SSE、任务轮询、报告生成状态等实时链路。
- 默认 TTL：
  - 公共统计 5 分钟。
  - 会员套餐 30 分钟。
  - 岗位配置 30 分钟。
  - 成长中心 2 分钟。
  - 通知未读数 30 秒。
  - 社区列表 30 秒。
- 路由预热只针对白名单用户端高频路径，不做全量 preload。
- KeepAlive 只覆盖用户端高频列表/工作台页面，不缓存详情页、编辑器、面试会话和结果页。
- 动画只过渡 `opacity`、`transform`、`box-shadow`、`border-color`、`background-color`、`color` 等明确属性，避免误动画布局属性。

## 后端实现方案

无前端侧后端实现。后端缓存与数据库减压见根目录后端任务文档。

## 数据存储方案

无新增前端持久化。请求缓存仅存在于当前浏览器运行时内存，不使用 Service Worker、IndexedDB、localStorage 或新的状态库。

## stage 更新说明

已在 `frontend/tasks/stage.md` 追加本轮前端性能优化完成记录，包含路由预热、KeepAlive、GET 缓存、动画属性收敛、长列表渲染隔离和验证结果。

## 编译结果

- `npm.cmd test -- --run src/__tests__/router/routeLoaders.test.js src/__tests__/layouts/MainLayout.test.js src/__tests__/utils/apiCache.test.js src/__tests__/api/performanceCache.test.js` 通过，4 个测试文件，12 个测试。
- `npm.cmd test -- --run src/__tests__/views/MembershipView.test.js src/__tests__/views/ResumeHistoryView.test.js src/__tests__/components/community/PostEditor.test.js src/__tests__/components/community/ShareReportDialog.test.js` 通过，4 个测试文件，15 个测试。
- `npm.cmd test -- --run` 通过，65 个测试文件，413 个测试。

## 构建结果

- `npm.cmd run build` 通过。
- 构建产物保持动态 import 与 gzip 压缩输出正常。

## 当前功能验收说明

- 用户端高频页面切换已具备预加载和 KeepAlive 缓存。
- 稳定 GET 接口已按 TTL 缓存，并在主动操作后失效。
- 用户端核心页面 `transition: all` 已清理，剩余匹配项集中在管理端页面，本轮未扩大到管理端以避免视觉回归。
- 长列表渲染隔离已补充到社区相关页面。
- 未执行浏览器性能面板人工抽样，后续可在真实浏览器环境对切页、hover 动效和长列表滚动进行人工复测。

## 停止，不继续下一功能

本轮只完成用户指定的前端页面切换流畅度、请求缓存、动画渲染成本和长列表渲染隔离优化；不继续新增 Service Worker、CDN、虚拟列表库或新的前端状态库。
