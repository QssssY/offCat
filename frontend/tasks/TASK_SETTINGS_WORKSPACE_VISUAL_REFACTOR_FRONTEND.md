# 设置页视觉重构、等高布局与图标预加载

## Summary

本轮只处理用户端设置页和必要的布局承载，目标是把设置页从短内容自然撑高的卡片堆叠，调整为稳定的设置工作台。侧边栏与主内容区在桌面端保持协调高度，账号资料、通知偏好、会员与额度、新手引导等短内容页面不再显得未完成；设置侧边栏高频图标改为同步命中，减少进入页面时的闪烁。

## Scope

- 修改 `frontend/app/src/views/settings/SettingsView.vue`。
- 修改 `frontend/app/src/layouts/MainLayout.vue` 的路由转场承载容器伸展规则。
- 修改 `frontend/app/src/utils/featureIcons.js`，把设置页高频图标加入 critical 同步资源集。
- 更新相关前端测试。
- 不修改 `/admin/**`、API、路由定义、后端、数据库、账号注销逻辑和现有表单验证逻辑。

## Key Changes

- 设置页外层新增 `settings-workspace`，并补齐所有设置 section 的 `settings-panel-body`，让短内容面板具备一致的工作区结构。
- 桌面端设置页增加 `min-height: calc(100dvh - 178px)`，侧边栏和主面板使用 `min(720px, calc(100dvh - 210px))` 建立最小视觉高度；移动端取消强制等高，保持单列和横向导航。
- 账号资料页增加账号概览卡和资料网格，通知、外观、新手引导、会员等短面板增加轻量说明区，避免下方大面积空白。
- 设置侧边栏选中态从硬 3px 竖条改为右侧柔和圆点；信息项从左侧硬竖条改为底部短强调线，减少生硬感。
- 所有新增动效限制在 `opacity`、`transform`、`box-shadow`、`border-color` 等低成本属性，并保留 `prefers-reduced-motion` 降级。
- 将 `user-profile`、`account-security`、`data-cleanup`、`data-management`、`feedback-center`、`onboarding-task`、`membership-credits`、`growth-radar`、`announcement` 加入设置页高频 critical icon 集合，并在设置导航中使用 `loading="eager"` 和 `fetch-priority="auto"`。

## Verification

- `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js src/__tests__/utils/featureIcons.test.js src/__tests__/components/common/FeatureIcon.test.js src/__tests__/layouts/MainLayout.test.js`：通过，4 个测试文件，44 个用例。
- `npm.cmd run build`：Vite 在清理 `dist/voice-models` 时遇到 Windows `EPERM` 权限错误，未进入源码编译失败。
- `npm.cmd run build -- --emptyOutDir=false`：通过，用于确认本轮源码、模板和样式可生产构建。

## Notes

- 标准构建失败点是本地 `dist/voice-models` 目录清理权限，不是设置页代码、Naive UI 或模板结构错误。
- 本轮只完成设置页视觉与布局承载优化；其它短页面如果仍有高度不协调，需要在对应页面单独处理。
