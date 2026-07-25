# 管理端导航切换白屏性能回归修复任务记录

## 当前任务所属模块

管理端前端路由切换体验、路由 chunk 预取、切换加载反馈和低成本动效收敛。

## 前端文件定位

- `frontend/app/src/router/routeLoaders.js`
- `frontend/app/src/router/index.js`
- `frontend/app/src/layouts/AdminLayout.vue`
- `frontend/app/src/__tests__/router/routeLoaders.test.js`
- `frontend/app/src/__tests__/layouts/AdminLayout.test.js`

## 后端文件定位

本轮不修改后端接口、服务、缓存或数据库结构。管理端接口路径、请求参数和响应结构保持不变。

## 本轮修改文件清单

- `routeLoaders.js` 新增管理端页面 loader 白名单和 `prefetchAdminRoute`，覆盖看板、监控、用户权益、审计日志、通知公告、反馈、版本日志、会员套餐、订单、岗位、Prompt、AI 引擎和成长配置。
- `router/index.js` 将管理端子路由从内联动态导入切换为命名 loader，保证路由表和导航预取复用同一批 chunk 入口。
- `AdminLayout.vue` 给侧栏导航补充 `mouseenter`、`focus`、`touchstart` 预取事件；将内容区从裸 `RouterView` 改为 slot + `Transition`，并增加 120ms 延迟显示的顶部加载进度条。
- `AdminLayout.vue` 将导航项 `transition: all` 收敛为明确的 `background-color`、`color`、`box-shadow`、`transform`，并新增仅使用 `opacity/transform` 的页面淡入淡出。
- 新增 `AdminLayout.test.js`，补充管理端导航预取、切换反馈和动效属性回归测试。
- 更新 `routeLoaders.test.js`，确认管理端只做交互触发预取，不进入用户端 idle 全量预热。

## 前端实现方案

- 根因定位：此前性能优化主要覆盖用户端路由，管理端页面仍在点击导航后才下载和解析对应页面 chunk；`AdminLayout` 内容区没有加载反馈，首次进入管理端子页面时容易出现内容区白屏。
- 预取策略：管理端不做 idle 全量预热，只在管理员对某个导航项 hover、键盘 focus 或触摸时预取该页面 chunk，减少点击后的空窗，同时避免登录后台后立刻拉满所有管理页资源。
- 切换反馈：管理端 shell、头部和侧栏保持可见；当管理端内部路由切换超过 120ms 时，在内容区顶部显示细进度条，完成后延迟 180ms 隐藏，避免快速切换闪烁。
- 动效策略：页面切换只使用 `opacity` 和 `transform`；侧栏导航只过渡颜色、阴影和 transform，避免误动画布局属性导致复杂页面额外 layout/paint。
- 兼容策略：保留现有路由路径、路由名称、权限 meta、管理端登录跳转和页面组件结构，不引入新 UI 库或新状态库。

## 后端实现方案

无后端改动。若后续仍存在管理端接口响应慢，需要单独基于接口耗时和 SQL/缓存命中率继续排查，本轮只处理点击导航后白屏加载的前端感知回归。

## 数据存储方案

无新增数据存储。管理端预取去重仅使用当前浏览器运行时内存 `Set`，刷新页面后自然失效。

## stage 更新说明

已在 `frontend/tasks/stage.md` 追加本轮管理端导航切换白屏回归修复记录，说明根因、范围、验证结果和停止边界。

## 编译结果

- `npm.cmd test -- --run src/__tests__/router/routeLoaders.test.js src/__tests__/layouts/AdminLayout.test.js` 通过，2 个测试文件，7 个用例。
- `npm.cmd test -- --run src/__tests__/router/routeLoaders.test.js src/__tests__/layouts/AdminLayout.test.js src/__tests__/layouts/MainLayout.test.js src/__tests__/themeTokens.test.js` 通过，4 个测试文件，14 个用例。
- `npm.cmd test -- --run` 通过，66 个测试文件，417 个用例。

## 构建结果

- `npm.cmd run build` 通过，Vite 生产构建成功，管理端页面继续按动态 import 拆分为独立 chunk，gzip 产物生成成功。

## 当前功能验收说明

- 管理端侧栏 hover、focus、touch 后会预取目标页面 chunk，首次点击导航后的白屏空窗应明显缩短。
- 管理端内部导航切换时不再直接暴露裸白内容区；超过 120ms 的加载会显示轻量顶部进度条，头部和侧栏保持可见。
- `AdminLayout.vue` 内已移除 `transition: all`，新增动效只使用低成本属性。
- 本轮未执行真实浏览器性能面板人工抽样；后续可在实际登录态下复测 `/admin/dashboard`、`/admin/users`、`/admin/prompts`、`/admin/ai-engines` 等较重页面首点切换体验。

## 停止，不继续下一个功能

本轮只修复用户反馈的管理端导航白屏和卡顿感知回归，不继续扩展管理端接口缓存、SQL 优化、后台页面重构或新功能开发。
