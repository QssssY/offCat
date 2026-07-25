# 用户端图标光晕范围收紧

## 当前任务所属模块
用户端图标展示一致性修复：根据最新反馈，首页、导航栏和头像下拉菜单不添加光晕，也不添加淡透明图标框；通知类图标只在可点击通知条目中显式启用光晕，静态展示不再默认带光晕。

## 前端文件定位
- 图标入口组件：`frontend/app/src/components/common/FeatureIcon.vue`
- 通知类型图标组件：`frontend/app/src/components/notification/NotificationTypeIcon.vue`
- 用户端导航与通知下拉：`frontend/app/src/components/AppHeader.vue`
- 通知中心页面：`frontend/app/src/views/notification/NotificationView.vue`
- 首页约束检查：`frontend/app/src/views/HomePageView.vue`
- 图标范围测试：`frontend/app/src/__tests__/components/common/FeatureIcon.test.js`
- 通知图标测试：`frontend/app/src/__tests__/components/notification/NotificationTypeIcon.test.js`
- 通知中心测试：`frontend/app/src/__tests__/views/NotificationView.test.js`
- 首页、导航、头像菜单光晕约束测试：`frontend/app/src/__tests__/styles/UserIconHalo.test.js`

## 本轮修改文件清单
- `FeatureIcon.vue`：保持 `halo` 为显式 opt-in，默认不加光晕。
- `NotificationTypeIcon.vue`：新增 `halo` 布尔参数，默认 `false`；只有传入 `halo` 时才追加 `notification-icon-halo`。
- `NotificationView.vue`：通知列表中的可点击通知条目传入 `halo`，公告弹窗标题等静态展示不传。
- `AppHeader.vue`：通知下拉中的可点击通知条目传入 `halo`；导航图标、移动端导航图标和头像下拉菜单图标不传 `FeatureIcon halo`。
- `UserIconHalo.test.js`、`FeatureIcon.test.js`、`NotificationTypeIcon.test.js`、`NotificationView.test.js`：补齐“首页/导航/头像下拉无光晕、通知静态图标无光晕、通知交互项显式光晕”的约束。

## 前端实现方案
- 首页图标、导航栏图标、移动端导航图标、主题切换图标、头像下拉菜单图标保持裸图标和轻微 transform 动效，不使用 `FeatureIcon halo`、`feature-icon-halo` 或额外图标光晕框。
- 全局样式不再通过 `.main-layout :where(...)` 批量给用户端图标加光晕，避免静态图标被统一套框。
- 通知类型图标的光晕从“组件默认”改为“使用方显式声明”，只给可点击通知条目使用，通知弹窗标题等静态场景不加。
- 通知栏图标尺寸保持紧凑：`sm` 为 34px，`md` 为 44px，不继续放大。

## 后端实现方案
本轮不涉及后端，不修改 API、路由、数据库、通知 SSE、鉴权或业务逻辑。

## 数据存储方案
本轮不涉及数据存储，不新增表、字段或本地持久化逻辑。

## stage 更新说明
已在 `frontend/tasks/stage.md` 追加“用户端图标光晕范围收紧”记录，包含修改范围、验证结果和停止边界。

## 验证结果
- RED：先修改通知图标测试，`npm.cmd test -- --run src/__tests__/components/notification/NotificationTypeIcon.test.js src/__tests__/views/NotificationView.test.js` 失败于 `NotificationTypeIcon` 默认仍带 `notification-icon-halo`，以及通知列表未显式传入 `halo`。
- GREEN：修复后 `npm.cmd test -- --run src/__tests__/components/notification/NotificationTypeIcon.test.js src/__tests__/views/NotificationView.test.js src/__tests__/styles/UserIconHalo.test.js src/__tests__/components/common/FeatureIcon.test.js` 通过，4 个测试文件 / 10 个用例通过。
- 回归：`npm.cmd test -- --run src/__tests__/components/common/FeatureIcon.test.js src/__tests__/components/notification/NotificationTypeIcon.test.js src/__tests__/components/AppHeader.test.js src/__tests__/views/HomePageView.test.js src/__tests__/views/NotificationView.test.js src/__tests__/views/InterviewReportView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/styles/UserIconHalo.test.js` 通过，8 个测试文件 / 47 个用例通过。
- 构建：`npm.cmd run build` 通过。

## 当前功能验收说明
验收时重点检查：首页、导航栏、移动端导航和头像下拉菜单图标没有被光晕或淡透明图标框包裹；通知栏图标不再继续放大；通知列表可点击条目保留轻量光晕反馈，静态通知图标不带光晕。

## 停止说明
本轮只收紧用户端图标光晕与通知图标展示范围，不修改 `/admin/**`、API、路由、数据库、后端业务流程，不继续推进新的页面重构。
