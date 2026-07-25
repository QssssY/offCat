# 用户端导航路由切换性能优化

## 当前任务所属模块
用户端前端路由切换、模板库首屏渲染、社区发布弹窗按需加载、成长中心图表按需加载、路由切换反馈与高频页面缓存性能优化。

## 前端文件定位
- 模板库列表：`frontend/app/src/components/template/TemplateCard.vue`、`frontend/app/src/components/template/IndustryFilter.vue`、`frontend/app/src/views/template/TemplateLibraryView.vue`
- 社区首页：`frontend/app/src/views/community/CommunityView.vue`
- 成长中心：`frontend/app/src/views/growth/GrowthCenterView.vue`
- 用户端导航与路由：`frontend/app/src/components/AppHeader.vue`、`frontend/app/src/router/index.js`、`frontend/app/src/router/routeLoaders.js`
- 用户端布局与导出工具：`frontend/app/src/layouts/MainLayout.vue`、`frontend/app/src/components/template/ExportToolbar.vue`
- 测试文件：`frontend/app/src/__tests__/components/AppHeader.test.js`、`frontend/app/src/__tests__/router/routeLoaders.test.js`、`frontend/app/src/__tests__/layouts/MainLayout.test.js`、`frontend/app/src/__tests__/components/template/ExportToolbar.test.js`、`frontend/app/src/__tests__/components/template/TemplateCard.test.js`

## 后端文件定位
本轮不涉及后端接口、数据库、路由或服务端缓存配置。

## 本轮修改文件清单
- 新增 `frontend/app/src/router/routeLoaders.js`
- 新增 `frontend/app/src/__tests__/router/routeLoaders.test.js`
- 新增 `frontend/app/src/__tests__/layouts/MainLayout.test.js`
- 新增 `frontend/app/src/__tests__/components/template/ExportToolbar.test.js`
- 新增 `frontend/app/src/__tests__/components/template/TemplateCard.test.js`
- 修改 `frontend/app/src/router/index.js`
- 修改 `frontend/app/src/components/AppHeader.vue`
- 修改 `frontend/app/src/layouts/MainLayout.vue`
- 修改 `frontend/app/src/components/template/ExportToolbar.vue`
- 修改 `frontend/app/src/components/template/TemplateCard.vue`
- 修改 `frontend/app/src/components/template/IndustryFilter.vue`
- 修改 `frontend/app/src/views/template/TemplateLibraryView.vue`
- 修改 `frontend/app/src/views/community/CommunityView.vue`
- 修改 `frontend/app/src/views/growth/GrowthCenterView.vue`
- 修改对应单元测试与 `frontend/tasks/stage.md`

## 前端实现方案
- 模板库列表卡片不再挂载完整 `TemplateRenderer` 和每个模板的 raw CSS，改用轻量 `TemplatePreviewImage` 生成可识别的模板缩略图；完整简历 HTML 渲染只保留在模板预览弹窗和编辑器。
- `TemplatePreviewDialog` 改为 `defineAsyncComponent`，仅在打开预览时加载完整预览能力。
- `TemplatePreviewDialog` 打开时按 `template.id` 动态加载 `@/data/styles/${id}.css?raw` 并注入到预览纸张内，保证弹窗完整预览仍展示模板真实样式；关闭弹窗时清空样式，避免模板切换串样式。
- 社区首页 `PostEditor` 改为异步组件，保持 `v-if="showEditor"`，只在用户点击发布并打开弹窗后加载；帖子卡片增加渲染隔离样式，降低长列表绘制成本。
- 成长中心 `LineChart`、`RadarChart`、`RadarScorePanel` 改为异步组件，并为图表容器保留 `min-height`，避免图表 chunk 延后加载时造成明显布局跳动。
- 扩展用户端高频路由 loader 白名单到 `/templates`、`/community`、`/growth`、`/resume/upload`、`/interview/entry`、`/offer`，`AppHeader` 在桌面 hover/focus 和移动端 touch/focus 时调用 `prefetchUserRoute`，仅按用户意图预取白名单路由，不做全量 preload。
- 登录后进入用户端导航时，通过 `requestIdleCallback` 低优先级预热 `/templates`、`/community`、`/growth`，没有浏览器原生支持时用延迟 `setTimeout` 兜底，避免抢占首页首屏资源。
- `MainLayout` 增加 120ms 延迟出现的顶部细进度条，快速切换不显示，慢切换提供轻量反馈；`prefers-reduced-motion` 下禁用扫光动画。
- 警告修复：`MainLayout` 不是 `<router-view>` 直接渲染的路由组件，因此移除 `onBeforeRouteUpdate`，改用 `router.beforeEach`、`router.afterEach` 和 `router.onError` 注册布局级切换反馈，并在卸载时注销钩子，避免 Vue Router “No active route record” 警告。
- `/templates`、`/community`、`/growth` 对应页面增加受控 `KeepAlive`，返回高频列表/看板页面时复用已挂载实例；详情页、编辑器、面试会话、结果页不加入缓存。
- `ExportToolbar.vue` 中 `html2canvas` 与 `jspdf` 改为点击导出时动态导入，避免模板编辑相关首段解析提前携带导出库。
- 模板筛选与卡片样式收敛 `transition: all`，改为只过渡颜色、背景、边框、阴影和 transform 等明确属性；列表卡片保留 `contain: layout paint style`，不使用 `content-visibility: auto`。

## 后端实现方案
无后端改动。

## 数据存储方案
不新增数据库表、字段、本地持久化、Service Worker 或缓存结构。路由预取状态仅在当前前端运行时内存中用 `Set` 去重。

## stage 更新说明
`frontend/tasks/stage.md` 追加本轮“用户端导航路由切换流畅度优化”记录，说明实现范围、验证命令和停止边界。

## 编译结果
`npm.cmd test -- --run src/__tests__/components/AppHeader.test.js src/__tests__/router/routeLoaders.test.js src/__tests__/components/template/ExportToolbar.test.js src/__tests__/layouts/MainLayout.test.js src/__tests__/App.test.js src/__tests__/components/template/TemplateCard.test.js` 通过，6 个测试文件 / 18 个用例通过。修复 `MainLayout` 路由钩子警告后，补充运行 `npm.cmd test -- --run src/__tests__/layouts/MainLayout.test.js src/__tests__/components/AppHeader.test.js src/__tests__/router/routeLoaders.test.js src/__tests__/App.test.js` 通过，4 个测试文件 / 13 个用例通过。

## 构建结果
`npm.cmd run build` 通过。构建产物中 `TemplateLibraryView` 约 12.18KB，`CommunityView` 约 9.45KB，`GrowthCenterView` 约 16.06KB；`TemplatePreviewDialog`、`TemplateRenderer`、`PostEditor`、`LineChart`、`RadarChart`、`RadarScorePanel`、`html2canvas`、`jspdf` 均保持独立 chunk；构建仅保留既有第三方 `@vueuse/core` pure annotation 提示。

## 当前功能验收说明
- 切换到模板库时不再一次性渲染十几份完整简历 HTML，列表先展示轻量缩略图。
- 社区页首屏不再同步加载发帖编辑器。
- 成长中心首屏不再无条件同步加载 Chart.js 图表组件。
- 导航栏只对白名单用户端高频入口做用户意图触发的按需预取，不会全量预加载路由。
- 已登录用户空闲时会预热模板库、社区、成长中心三个重入口；再次返回这三个页面时通过受控 `KeepAlive` 减少重复挂载成本。
- 慢路由切换会在 120ms 后出现顶部细进度条，快速切换不闪烁。
- 模板导出依赖只在用户实际点击导出时加载，不阻塞模板编辑/预览首段解析。

## 停止说明
本轮只处理用户端导航路由切换与高频页面首屏/返回流畅度；不新增虚拟列表库、不改路由路径、不改业务接口、不改后端、不缓存详情页/编辑器/面试会话/结果页、不继续推进其它页面重构。
