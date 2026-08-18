# OfferCat 项目介绍与 GitHub 支持入口前端

## 当前任务所属模块

全局用户端顶部导航、移动端导航抽屉和项目介绍弹窗。

## 前端文件定位

- `frontend/app/src/components/AppHeader.vue`
- `frontend/app/src/__tests__/components/AppHeader.test.js`

## 后端文件定位

本轮不修改后端文件，不新增接口或调整鉴权、业务数据链路。

## 本轮修改文件清单

- `frontend/app/src/components/AppHeader.vue`
- `frontend/app/src/__tests__/components/AppHeader.test.js`
- `frontend/tasks/TASK_GITHUB_PROJECT_INTRO_FRONTEND.md`
- `frontend/tasks/stage.md`

## 前端实现方案

- 顶部入口由生硬的“开源 · 求 Star”改为“了解 OfferCat”，点击后打开项目介绍弹窗。
- 弹窗先说明 OfferCat 是完全开源的 AI 求职准备平台，再用四项能力说明具体价值：简历诊断、岗位匹配、模拟面试和 Offer 辅助。
- 弹窗主行动改为“前往 GitHub，给项目一个 Star”，只有真正的 GitHub CTA 才打开外部新窗口；次要操作为“先看看”。
- 介绍弹窗放在登录状态模板之外，未登录访客也可以打开；移动端入口关闭导航抽屉后打开同一弹窗，避免重复维护内容。
- 复用现有 `GitHubIcon`、`FeatureIcon`、Element Plus `ElDialog` 和主题变量，新增响应式布局、焦点样式和移动端单列能力列表。

## 后端实现方案

无后端改动。项目介绍和 GitHub 链接均为前端静态内容。

## 数据存储方案

无数据存储改动，不新增数据库表、字段、索引或 migration。

## RED 验证

在仅有原始 GitHub 入口的实现下，新增测试会失败：桌面入口没有项目介绍弹窗、移动入口没有打开介绍状态、未登录模板中不存在介绍弹窗和“给项目一个 Star”CTA。

## GREEN 与回归验证

```bash
npm.cmd test -- --run src/__tests__/components/AppHeader.test.js src/__tests__/components/common/GitHubIcon.test.js
npm.cmd run build
git diff --check
```

结果：2 个测试文件 / 12 个用例通过，前端生产构建通过；页面验收确认 1440px 桌面弹窗宽度 600px、四项能力完整、主 CTA 使用新窗口且无横向溢出。

## 当前功能验收说明

访客点击“了解 OfferCat”后先看到项目定位与核心能力，再决定是否前往 GitHub 支持项目；未登录状态也可使用该介绍入口。移动端视觉点击验收受本地后端错误 toast 覆盖汉堡按钮影响，状态切换由组件测试覆盖，待线上人工点击确认。

## stage 更新说明

已在 `frontend/tasks/stage.md` 顶部追加本轮项目介绍与 GitHub 支持入口记录。

## 停止，不继续下一个功能

本轮只优化开源入口文案、增加项目介绍弹窗和 GitHub 支持 CTA，不继续增加 Star 数量 API、README 展示、页脚入口或其它营销模块。
