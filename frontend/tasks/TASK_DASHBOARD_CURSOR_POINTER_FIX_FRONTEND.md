# 个人中心导航卡片 hover 光标前端修复

## 当前任务所属模块
- 前端个人中心模块。
- 页面入口：用户端 `/dashboard` 个人中心。

## 前端文件定位
- `frontend/app/src/views/DashboardView.vue`
- `frontend/app/src/styles/index.css`

## 后端文件定位
- 本轮不涉及后端修改。
- 个人中心统计、额度和最近记录接口保持现状。

## 本轮修改文件清单
- `frontend/app/src/views/DashboardView.vue`
- `frontend/app/src/__tests__/views/DashboardView.test.js`
- `frontend/tasks/stage.md`
- `frontend/tasks/TASK_DASHBOARD_CURSOR_POINTER_FIX_FRONTEND.md`

## 前端实现方案
- 根因是全局只读文本基线 `body * { cursor: default; caret-color: transparent; }` 会覆盖可点击 `div` 内部文字和图标节点的光标；个人中心旧代码只在父级 `.growth-entry-card` 和 `.record-item.clickable` 上设置 `cursor: pointer`，内部节点 hover 时仍可能回退成默认箭头。
- `DashboardView.vue` 将固定跳转的“个人成长中心”入口从 `div @click` 改为 `router-link to="/growth"`。
- 最近简历诊断记录中，只有 `record.status === 2` 的完成态记录渲染为 `router-link` 并跳转 `/resume/result/{taskId}`；非完成态仍为普通 `div`，不改变不可点击状态。
- 最近模拟面试记录中，只有 `record.status === 1` 的完成态记录渲染为 `router-link` 并跳转 `/interview/report/{sessionId}`；非完成态仍为普通 `div`。
- 为这些链接补齐 `color: inherit`、`text-decoration: none` 和 `focus-visible` 样式，保持原视觉布局，同时让键盘访问有明确焦点反馈。

## 后端实现方案
- 本轮不修改后端。
- 不修改个人中心月度统计、简历历史、面试历史、用户信息或额度接口。

## 数据存储方案
- 本轮不新增本地存储字段。
- 本轮不修改数据库表、迁移脚本或接口 payload。

## stage 更新说明
- 已在 `frontend/tasks/stage.md` 追加“个人中心导航卡片 hover 光标修复（2026-05-30）”记录。
- stage 中明确本轮范围只包含个人中心导航型点击区域光标和语义修复，不继续扩展个人中心能力。

## 编译结果
- RED 验证：新增 `DashboardView.test.js` 回归测试后，旧代码失败于成长中心入口和最近记录项仍使用 `div @click`。
- GREEN 验证：`npm.cmd test -- --run src/__tests__/views/DashboardView.test.js` 通过，1 个测试文件 / 6 个用例通过。
- 相关回归：`npm.cmd test -- --run src/__tests__/views/DashboardView.test.js src/__tests__/components/AppHeader.test.js src/__tests__/themeTokens.test.js src/__tests__/layouts/MainLayout.test.js` 通过，4 个测试文件 / 22 个用例通过。

## 构建结果
- 前端生产构建：`npm.cmd run build` 通过。

## 当前功能验收说明
- 鼠标移入个人中心“个人成长中心”入口的文字、描述、图标和箭头区域时保持小手光标。
- 最近简历诊断中已完成记录仍可跳转诊断结果页，鼠标移入记录内文字、图标和状态区域时保持小手光标；未完成记录仍不可点击。
- 最近模拟面试中已完成记录仍可跳转面试报告页，鼠标移入记录内文字、图标和分数区域时保持小手光标；未完成记录仍不可点击。
- 个人中心统计、额度、最近记录数据加载和按钮逻辑保持原行为。
- 输入框、文本域和真实可编辑区域仍保留输入光标；本轮没有移除全局只读文本 cursor 基线。

## 停止说明
- 本轮只处理用户反馈的个人中心鼠标小手问题，不继续推进个人中心视觉重构、数据能力扩展、路由重构或后端改造。
