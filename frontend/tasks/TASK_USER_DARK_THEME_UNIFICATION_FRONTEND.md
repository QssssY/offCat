# 用户端暗色主题暖棕统一

## 当前任务所属模块

- 模块：用户端全局主题
- 前端文件定位：
  - `frontend/app/src/styles/index.css`
  - `frontend/app/src/App.vue`
  - `frontend/app/src/views/HomePageView.vue`
  - `frontend/app/src/layouts/AdminLayout.vue`
- 后端文件定位：本轮不涉及后端。

## 本轮修改文件清单

- `frontend/app/src/styles/index.css`
- `frontend/app/src/App.vue`
- `frontend/app/src/views/HomePageView.vue`
- `frontend/app/src/layouts/AdminLayout.vue`
- `frontend/app/src/components/resume/RadarChart.vue`
- `frontend/app/src/components/resume/LineChart.vue`
- `frontend/app/src/components/OnboardingGuide.vue`
- `frontend/app/src/__tests__/App.test.js`
- `frontend/app/src/__tests__/views/HomePageView.test.js`
- `frontend/app/src/__tests__/themeTokens.test.js`
- `frontend/tasks/TASK_USER_DARK_THEME_UNIFICATION_FRONTEND.md`
- `frontend/tasks/stage.md`

## 前端实现方案

- 将全局 `[data-theme="dark"]` 从冷蓝紫色系统一调整为暖橙棕色系，覆盖 `--bg-page`、`--bg-card`、`--bg-card-hover`、`--bg-input`、`--bg-header`、`--bg-elevated`、文字、边框、阴影、滚动条和 Element Plus 暗色桥接变量。
- `App.vue` 继续使用 Naive UI `darkTheme`，并将 `NConfigProvider` 的暗色 `bodyColor`、`cardColor`、`textColor1/2/3`、`borderColor` 对齐全局暖棕 token，避免 Naive UI 组件和 Element Plus/自定义 CSS 色调不一致。
- `HomePageView.vue` 保留首页专属 `--home-*` 背景、hero、插画、云层、卡片、路径线和动效变量；移除首页暗色块对通用 `--text-*`、`--border-*` 和品牌基础色的重复覆盖，改为继承全局暖棕 token。
- `AdminLayout.vue` 在暗色模式下补充局部管理端 token，并同步覆盖管理端作用域内的 Element Plus `--el-*` 暗色变量，维持管理端偏中性冷色的运营后台视觉边界，不让用户端暖棕主题扩散到 `/admin/**`。
- 简历/成长相关 Chart.js 组件移除旧冷色硬编码，暗色点边框、坐标轴和雷达标签改为暖棕背景、暖白文字和浅杏辅助色；新手引导标题兜底色移除旧冷蓝紫。

## 后端实现方案

- 本轮不修改后端接口、路由、数据库或业务流程。

## 数据存储方案

- 本轮不涉及数据库结构或持久化数据变更。

## 测试与构建结果

- 已按 TDD 先新增失败用例，覆盖旧冷色 token、Naive UI 暗色桥接、首页通用 token 精简和管理端暗色隔离。
- 定向测试通过：`npm.cmd test -- --run src/__tests__/App.test.js src/__tests__/views/HomePageView.test.js src/__tests__/themeTokens.test.js`，3 个测试文件 / 10 个用例通过。
- 完整前端测试通过：`npm.cmd test`，45 个测试文件 / 275 个用例通过。
- 生产构建通过：`npm.cmd run build`。

## 当前功能验收说明

- 用户端暗色全局基础色已统一为暖棕色调，简历诊断、面试、成长中心、Offer 等依赖全局 token 的页面会自动获得一致暗色表面。
- 首页仍保留首屏和插画叙事所需的专属暗色层次，但不再重复定义通用文字和边框 token。
- 管理端暗色在 `AdminLayout.vue` 内局部隔离，通用 token 与 Element Plus 暗色变量都维持中性冷色，避免 `/admin/**` 被用户端暖棕主题改造。
- 停止说明：本轮只处理用户端暗色主题统一，不继续推进其它页面重构或新增业务功能。
