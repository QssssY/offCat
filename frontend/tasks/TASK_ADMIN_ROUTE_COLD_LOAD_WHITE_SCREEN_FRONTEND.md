# 管理端路由冷加载白屏修复

## 当前任务所属模块

管理端路由切换、Vite 开发环境首开体验、后台页面 chunk 预热。

## 前端文件定位

- `frontend/app/src/router/routeLoaders.js`
- `frontend/app/src/layouts/AdminLayout.vue`
- `frontend/app/vite.config.js`
- `frontend/app/src/__tests__/router/routeLoaders.test.js`
- `frontend/app/src/__tests__/layouts/AdminLayout.test.js`
- `frontend/app/src/__tests__/viteConfig.test.js`

## 后端文件定位

本轮不涉及后端文件、接口、数据库或服务层改动。

## 本轮修改文件清单

- `routeLoaders.js`：新增管理端高频路由空闲预热函数，只预热数据看板、用户权益、AI 引擎、Prompt 管理和监控总览。
- `AdminLayout.vue`：管理端壳加载后触发高频路由预热；路由切换等待超过阈值时展示轻量占位反馈，避免内容区直接白屏；组件卸载时清理 idle callback / timeout。
- `vite.config.js`：新增 `optimizeDeps.include`，让 Vite 开发环境提前预优化后台高频重依赖，减少首次进入管理路由时的现场补扫描和整页刷新。
- `routeLoaders.test.js`、`AdminLayout.test.js`、`viteConfig.test.js`：补充源码级回归断言，锁定管理端预热范围、占位反馈和 Vite 预优化配置。

## 前端实现方案

- 继续复用现有 `prefetchAdminRoute(path)` 和后台导航 hover/focus/touch 预取机制，不新增独立缓存层或 Service Worker。
- 新增 `warmupHighFrequencyAdminRoutes()`，在浏览器空闲期只预热最常进入的 5 个后台页面，避免登录后台后一次性拉取全部管理端 chunk。
- 管理端 `RouterView` 外层壳保持可见，路由切换超过 120ms 后展示顶部加载条和局部骨架占位，让 `Transition mode="out-in"` 等待新 chunk 时不再表现为整块白屏。
- Vite dev 通过 `optimizeDeps.include` 提前处理 `chart.js`、`vue-chartjs`、`xlsx`、`element-plus`、`@element-plus/icons-vue`、`naive-ui`，降低首次访问后台重页面时触发依赖补优化的概率。

## 后端实现方案

无后端实现。本轮不修改登录、鉴权、管理端接口、缓存 Header 或服务端路由。

## 数据存储方案

无数据存储改动。路由预热状态仅使用当前页面内存中的去重集合，不新增本地存储、数据库字段或持久缓存。

## stage 更新说明

`frontend/tasks/stage.md` 顶部已新增“管理端路由冷加载白屏修复”记录，说明原因、修复范围、验证结果和停止边界。

## 编译结果

- `npm.cmd test -- --run src/__tests__/router/routeLoaders.test.js src/__tests__/layouts/AdminLayout.test.js src/__tests__/viteConfig.test.js` 通过，3 个测试文件 / 12 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

首次进入管理端后，浏览器空闲期会提前预热最常用后台页面；切换管理端不同路由时，如果新页面 chunk 仍在加载，后台框架、侧栏和头部保持稳定显示，内容区展示“正在打开管理模块”的轻量占位，不再直接整块白屏等待。

## 停止，不继续下一功能

本轮只处理管理端首开和路由切换冷加载白屏问题，不继续扩展全量后台预加载、Service Worker、持久缓存、后端缓存策略或其它页面性能优化。
