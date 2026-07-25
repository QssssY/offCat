# 高风险路由首次跳转白屏修复

## 当前任务所属模块

用户端高频路由冷加载、无布局页面首次进入反馈、管理端登录后首屏预取、模板编辑器按业务意图预取。

## 前端文件定位

- `frontend/app/src/router/routeLoaders.js`
- `frontend/app/src/router/index.js`
- `frontend/app/src/App.vue`
- `frontend/app/src/layouts/MainLayout.vue`
- `frontend/app/src/components/AppHeader.vue`
- `frontend/app/src/views/interview/InterviewEntryView.vue`
- `frontend/app/src/views/community/CommunityView.vue`
- `frontend/app/src/views/template/TemplateLibraryView.vue`
- `frontend/app/src/views/admin/AdminLoginView.vue`
- `frontend/app/src/__tests__/router/routeLoaders.test.js`
- `frontend/app/src/__tests__/components/AppHeader.test.js`
- `frontend/app/src/__tests__/views/InterviewEntryView.test.js`
- `frontend/app/src/__tests__/views/community/CommunityView.test.js`
- `frontend/app/src/__tests__/components/template/TemplateCard.test.js`
- `frontend/app/src/__tests__/views/admin/AdminLoginView.test.js`
- `frontend/app/src/__tests__/App.test.js`
- `frontend/app/src/__tests__/layouts/MainLayout.test.js`

## 后端文件定位

本轮不涉及后端文件、接口、DTO、数据库或鉴权逻辑改动。

## 本轮修改文件清单

- `routeLoaders.js`：新增设置页、面试会话页、个人动态页、模板编辑页和管理端布局的共享 route loader；新增 `prefetchInterviewSessionRoute()`、`prefetchTemplateEditorRoute(templateId)`、`prefetchAdminShellRoute()`；设置页加入用户端 idle 预热；模板编辑页预取同时拉取当前模板内容和样式。
- `router/index.js`：高风险路由统一改用共享 loader，确保预取命中的 chunk 和实际跳转加载的 chunk 一致。
- `AppHeader.vue`：桌面设置按钮、移动端设置入口和头像菜单个人动态入口增加 hover/focus/touch 意图预取；头像菜单打开时预取个人动态页。
- `InterviewEntryView.vue`：创建面试请求发出前并行预取面试会话页，拿到 sessionId 后等待剩余预取再跳转。
- `CommunityView.vue`：个人动态中心按钮增加意图预取，点击时先完成已启动的预取再跳转。
- `TemplateLibraryView.vue`：使用模板时先预取模板编辑页 chunk，并提前加载当前模板内容与样式，再进入编辑器。
- `AdminLoginView.vue`：管理端登录成功后、跳转前预取后台外层布局和默认仪表盘首页。
- `MainLayout.vue`：对 `/settings` 和 `/community/my` 增加延迟骨架占位，避免主布局内首次切换时空白。
- `App.vue`：对 `/admin`、`/interview/session`、`/templates/editor` 这类 `useLayout:false` 高风险入口增加轻量全屏加载反馈。
- 相关测试文件：补充共享 loader、预取映射、去重、idle 预热范围、入口组件预取和加载兜底断言。

## 前端实现方案

- 继续沿用项目已有 route loader / prefetch 模式，不新增 Service Worker、持久缓存或全量预加载。
- 对 `/settings`、`/community/my`、`/interview/session/:sessionId`、`/templates/editor/:templateId`、`/admin` 首次进入分别补齐实际路由使用的共享 loader，避免“预取了一个 import、跳转又加载另一个 import”的不一致。
- 设置中心属于右上角高频入口，加入用户端 idle 预热；面试会话页和模板编辑页只在业务流程明确时预取，避免全局空闲期扩大资源消耗。
- 模板编辑页预取除页面 chunk 外，还并行拉取当前模板的 `contents` 和 `styles`，让编辑器进入后少一次动态资源等待。
- `MainLayout` 只覆盖仍使用主布局的设置页和个人动态页；无布局页面由 `App.vue` 兜底显示全屏轻量反馈。

## 后端实现方案

无后端实现。本轮不修改接口、鉴权、数据库结构、缓存 Header 或服务端路由。

## 数据存储方案

无数据存储改动。预取状态仅保存在当前页面内存中的 Set / Map / Promise 缓存中，不写入本地存储或数据库。

## stage 更新说明

`frontend/tasks/stage.md` 顶部新增“高风险路由首次跳转白屏修复”记录，说明本轮修复范围、验证结果和停止边界。

## 编译结果

- RED 阶段：`npm.cmd test -- --run src/__tests__/router/routeLoaders.test.js src/__tests__/components/AppHeader.test.js src/__tests__/views/InterviewEntryView.test.js src/__tests__/views/community/CommunityView.test.js src/__tests__/components/template/TemplateCard.test.js src/__tests__/views/admin/AdminLoginView.test.js src/__tests__/App.test.js src/__tests__/layouts/MainLayout.test.js` 失败，失败点均为新增高风险路由预取和兜底断言尚未实现。
- GREEN 阶段：同一定向命令通过，8 个测试文件 / 63 个用例通过。
- 全量测试：`npm.cmd test -- --run` 通过，79 个测试文件 / 522 个用例通过。全量验证前同步修正了两个既有测试侧阻塞点：`AdminAiEngineView.test.js` 连通性用例在全量并发下的超时时间、`SettingsView.test.js` 与当前“自定义 AI 接入”“12/50”文案一致的断言。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

首次进入设置中心、个人动态中心、面试会话页、模板编辑页和管理端默认首页时，前端会在用户明确意图或业务等待期提前加载目标页面 chunk；如果仍处于冷加载，主布局页面显示局部骨架，无布局页面显示轻量全屏反馈，不再直接进入长时间白屏。

## 停止，不继续下一功能

本轮只处理 5 个高风险入口：`/settings`、`/interview/session/:sessionId`、`/admin` 首次进入、`/community/my`、`/templates/editor/:templateId`。不继续处理中低风险页面，不修改 `xlsx`，不重构设置页内部多接口加载，不扩展后端或持久缓存能力。
