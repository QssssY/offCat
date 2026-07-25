# 设置页面 Naive UI 重构与路由转场动画

## Summary
本轮按用户端功能面板方向重构设置页，并为主布局补充路由转场动画。设置页保持首页同源的橙色品牌体系，但不使用首页云朵/星空装饰；重点是信息分组、面试偏好子导航、低风险 Naive UI 控件迁移、微交互和暗色主题一致性。

## Scope
- `frontend/app/src/layouts/MainLayout.vue`
  - 为非 KeepAlive 页面增加 `page-fade` 路由转场。
  - 使用真实 DOM 包裹层 `.page-fade-route` 承载转场，避免首页根节点为 `MotionConfig` 时出现 “non-element root node cannot be animated” warning。
  - 转场只使用 `opacity` 和 `transform`，并兼容 `prefers-reduced-motion`。
- `frontend/app/src/views/settings/SettingsView.vue`
  - 面试偏好拆分为 `面试偏好 / 语音通话 / 离线增强` 三个子 tab。
  - 移除语音偏好和离线增强旧折叠状态，避免长面板堆叠。
  - 低风险控件迁移到 Naive UI：按钮、标签、开关、下拉、滑块、提示等。
  - 优化设置侧边栏选中指示、信息网格、偏好行、小卡片间距和 hover/press 反馈。
- `frontend/app/src/App.vue`
  - 补充 Naive UI `Switch`、`Slider` 橙色主题 token。

## Stop Boundary
- 不修改 `/admin/**`。
- 不修改后端、API、路由定义、数据库、鉴权和账号注销业务逻辑。
- 安全区复杂表单与账号注销弹窗保留 Element Plus，避免扩大验证和 teleport 风险。

## Verification
- `npm.cmd test -- --run src/__tests__/layouts/MainLayout.test.js src/__tests__/views/SettingsView.test.js`
  - 2 个测试文件 / 33 个用例通过。
- `npm.cmd run build`
  - 构建通过，仅保留既有 `@vueuse/core` PURE annotation 提示。

