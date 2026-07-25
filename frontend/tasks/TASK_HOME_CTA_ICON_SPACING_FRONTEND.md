# 首页 CTA 图标文字间距修复

## 当前任务所属模块

用户端首页 Hero 区两个 CTA 按钮的视觉间距修复。

## 前端文件定位

- `frontend/app/src/views/HomePageView.vue`
- `frontend/app/src/__tests__/views/HomePageView.test.js`

## 后端文件定位

本轮不涉及后端文件。

## 本轮修改文件清单

- `HomePageView.vue`：不再使用 Naive UI 的 `#icon` 插槽承载首页 CTA 图标，改为按钮内容内的 `.cta-content` 自管布局，并使用 `12px` 间距控制图标与文字距离。
- `HomePageView.test.js`：补充源码级回归断言，锁定 CTA 自管内容结构、居中布局和图标文字间距。

## 前端实现方案

- 仅在 `.cta-btn` 范围内覆盖 Naive UI 按钮内部结构，不影响其它普通按钮、导航按钮或图标组件全局样式。
- 使用 `.cta-content` 直接包裹图标和文字，通过 `inline-flex` 与 `gap: 12px` 控制距离，绕开组件库默认图标槽间距，避免插画图标和文字再次贴近或过远。

## 后端实现方案

无后端改动。

## 数据存储方案

无数据存储改动。

## stage 更新说明

`frontend/tasks/stage.md` 已追加本轮首页 CTA 图标文字间距修复记录。

## 编译结果

- `npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 通过，1 个测试文件 / 6 个用例。
- `npm.cmd run build` 通过。

## 构建结果

生产构建通过。

## 当前功能验收说明

首页“开始简历诊断”和“开始模拟面试”两个按钮内，图标与文字之间应有明显呼吸感，不再贴近挤压。

## 停止，不继续下一功能

本轮只修复首页 CTA 图标与文字间距，不继续调整首页其它区域、按钮文案、图标资源或路由逻辑。
