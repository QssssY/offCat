# 前端性能缓存、路由切换与动画优化任务记录

## 当前任务所属模块

用户端前端页面切换流畅度、路由预热、KeepAlive、轻量 GET 请求缓存、高频动画渲染成本和长列表渲染隔离。

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
- `frontend/app/src/views/resume/HistoryView.vue`
- `frontend/app/src/views/resume/UploadView.vue`
- `frontend/app/src/views/resume/ResultView.vue`
- `frontend/app/src/views/interview/InterviewEntryView.vue`
- `frontend/app/src/views/interview/InterviewHistoryView.vue`
- `frontend/app/src/views/community/CommunityView.vue`
- `frontend/app/src/views/community/MyActivity.vue`
- `frontend/app/src/views/community/PostDetailView.vue`
- `frontend/app/src/views/growth/GrowthCenterView.vue`
- `frontend/app/src/components/community/CommentSection.vue`
- `frontend/app/src/components/community/PostEditor.vue`
- `frontend/app/src/components/OnboardingGuide.vue`
- `frontend/app/src/components/OnboardingTaskCard.vue`
- `frontend/app/src/components/common/AiLoadingState.vue`

## 后端文件定位

本任务文件记录前端部分；后端缓存与数据库索引对应记录见 `tasks/TASK_60_FULL_PERFORMANCE_CACHE_AND_INDEX_BACKEND.md`。

## 本轮修改文件清单

- 新增 `apiCache.js`，提供轻量 GET 缓存、TTL、精确清理和前缀清理能力。
- 公共统计、会员套餐、成长中心概览、岗位配置、通知未读数、社区列表等稳定 GET 接口接入缓存。
- 社区发帖、点赞、收藏、评论等写操作后清理社区相关缓存；会员升级后清理会员套餐缓存并刷新用户信息。
- 扩展用户端高频路由 idle 预热范围到 `/resume/upload`、`/interview/entry`、`/offer`、`/templates`、`/community`、`/growth`。
- 扩大 `KeepAlive` 覆盖到 `DashboardView`、`SettingsView`、`MembershipView`、`InterviewHistoryView`、`HistoryView`，并补齐组件 `name` 与路由 `meta.keepAlive`。
- 高频用户端页面和组件中的 `transition: all` 改为明确属性，避免误动画布局属性造成不必要 layout/paint。
- 社区列表和个人动态等长列表区域补充 `content-visibility: auto` 或渲染隔离策略。
- 补充缓存工具、API 缓存接入、路由预热和 KeepAlive 回归测试；修正社区组件测试里 Element Plus mock 缺少组件导出的隔离问题；会员页测试数据补齐后端排序字段，避免缓存测试间状态影响断言。

## 前端实现方案

- 请求缓存只封装 GET 请求，并要求调用方传入稳定缓存 key 和 TTL；POST、PUT、DELETE、SSE、轮询和实时任务状态不进入缓存。
- TTL 策略：公共统计 5 分钟、会员套餐 30 分钟、岗位配置 30 分钟、成长中心 2 分钟、通知未读数 30 秒、社区列表短 TTL。
- 路由预热沿用现有 `routeLoaders.js` 动态 import 模式，保留 hover、touch、focus 触发，并在空闲时间预热高频入口，不做全量 preload。
- `MainLayout` 继续使用受控 `KeepAlive` 列表，只缓存用户端列表、看板和设置类页面；详情页、编辑器、面试会话、结果页不纳入缓存。
- 动画优化只修改用户端核心页面，把 `transition: all` 收敛到 `opacity`、`transform`、`box-shadow`、`border-color`、`background-color`、`color` 等明确属性；管理端遗留 `transition: all` 暂不纳入本轮，避免扩大视觉回归范围。
- 长列表渲染隔离优先使用 CSS 原生能力，不引入虚拟列表库或新的状态管理库。

## 后端实现方案

无前端任务内的后端实现。接口路径、请求参数和响应结构保持不变。

## 数据存储方案

前端本轮不新增本地持久化、不新增 Service Worker、不写入 IndexedDB。请求缓存仅存在当前浏览器运行时内存中，刷新页面后自然失效。

## stage 更新说明

已在 `frontend/tasks/stage.md` 追加本轮完成记录，说明前端 GET 缓存、路由预热、KeepAlive、动画属性收敛、长列表渲染隔离与验证结果。

## 编译结果

- `npm.cmd test -- --run src/__tests__/router/routeLoaders.test.js src/__tests__/layouts/MainLayout.test.js src/__tests__/utils/apiCache.test.js src/__tests__/api/performanceCache.test.js` 通过，4 个测试文件，12 个用例。
- `npm.cmd test -- --run src/__tests__/views/MembershipView.test.js src/__tests__/views/ResumeHistoryView.test.js src/__tests__/components/community/PostEditor.test.js src/__tests__/components/community/ShareReportDialog.test.js` 通过，4 个测试文件，15 个用例。
- `npm.cmd test -- --run` 通过，65 个测试文件，413 个用例。

## 构建结果

- `npm.cmd run build` 通过，Vite 生产构建成功，gzip 产物生成成功。

## 当前功能验收说明

- 高频用户端页面返回时可复用已缓存组件实例，减少重复挂载成本。
- 稳定 GET 接口在 TTL 内可复用前端内存缓存，用户主动写操作后按业务域精确失效。
- 核心用户端页面的 `transition: all` 已收敛为明确属性；当前剩余 `transition: all` 搜索结果集中在管理端页面，本轮按范围控制未改动。
- 社区与个人动态长列表已补充低风险渲染隔离，降低复杂页面滚动和切换时的绘制压力。

## 停止，不继续下一个功能

本轮仅完成用户指定的前端性能缓存、路由切换与动画优化，不继续新增 Service Worker、虚拟列表库、CDN 配置、SSR、接口协议改造或新的页面功能。
