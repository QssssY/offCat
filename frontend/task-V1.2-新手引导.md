# 任务：V1.2 功能一 — 新手引导（前端实现）

## 当前任务所属模块
V1.2 用户体验增强版 — 功能一：新手引导（前端部分）

## 前端文件定位
- `frontend/app/src/api/onboarding.js` — 引导状态 API 模块
- `frontend/app/src/components/OnboardingGuide.vue` — 引导弹窗组件
- `frontend/app/src/layouts/MainLayout.vue` — 引导集成入口（修改）

## 本轮修改文件清单

### 新建文件
1. `frontend/app/src/api/onboarding.js` — API 模块
2. `frontend/app/src/components/OnboardingGuide.vue` — 引导弹窗组件

### 修改文件
3. `frontend/app/src/layouts/MainLayout.vue` — 集成引导组件

## 前端实现方案

### 1. API 模块（onboarding.js）
- 使用项目现有 `request` axios 实例
- `getOnboardingStatus()` → `GET /api/user/onboarding/status`
- `updateOnboardingStatus(data)` → `POST /api/user/onboarding/status`
- 请求参数：`guideKey`, `status`, `currentStep`（可选）

### 2. 引导组件（OnboardingGuide.vue）
- **弹窗**：使用 `el-dialog`，`close-on-click-modal=false`，`close-on-press-escape=false`
- **步骤指示器**：圆点样式，当前步骤橙色高亮（`var(--el-color-primary)`），已完成步骤半透明
- **7 个引导步骤**：
  1. 欢迎使用智能简历诊断系统
  2. 上传简历，获取诊断报告
  3. 输入目标岗位 JD
  4. AI 简历润色
  5. 模拟面试练习
  6. 查看历史记录
  7. 准备就绪！
- **按钮逻辑**：
  - 第一步：主按钮"开始体验"（触发 nextStep），无"上一步"
  - 中间步骤：主按钮"下一步"，有"上一步"
  - 最后一步：主按钮"完成"（触发 completeGuide），有"上一步"
  - 所有步骤：右侧"跳过引导"按钮
- **API 调用**：
  - "下一步"：调用 `updateOnboardingStatus({ guideKey, status: 'in_progress', currentStep: nextIndex })`
  - "跳过"：调用 `updateOnboardingStatus({ guideKey, status: 'skipped' })`，关闭弹窗
  - "完成"：调用 `updateOnboardingStatus({ guideKey, status: 'completed' })`，关闭弹窗
  - "上一步"：仅本地操作（`currentStep--`），不调用 API
  - API 失败时静默处理，不阻塞用户操作
- **样式**：
  - 沿用项目橙色主题 `#FF8C42`
  - 弹窗圆角 16px
  - 主按钮渐变橙色背景
  - 移动端适配（宽度 92vw，字体和间距调整）

### 3. MainLayout 集成
- 导入 `OnboardingGuide` 组件、`getOnboardingStatus` API、`useUserStore`
- 新增 `showGuide` ref 控制弹窗可见性
- `onMounted` 时：
  - 检查 token 是否存在（`localStorage.getItem('token')`）
  - 调用 `getOnboardingStatus()` 查询引导状态
  - 如果 `res.data.showGuide === true`，设置 `showGuide = true`
  - API 失败时静默处理（`console.warn`）
- 模板中 `<OnboardingGuide v-model:visible="showGuide" />` 放在 `<main>` 外部

## 构建结果
- 前端构建通过（`npm run build` 成功）

## 当前功能验收说明
1. 首次登录后进入任何需要 MainLayout 的页面，应弹出引导弹窗
2. 引导弹窗覆盖核心使用路径（7 个步骤）
3. 可以点击"开始体验"进入引导
4. 可以点击"下一步"逐步浏览各步骤
5. 可以点击"上一步"返回查看
6. 可以点击"跳过引导"跳过整个引导
7. 可以在最后一步点击"完成"结束引导
8. 完成或跳过后再次进入不会重复弹出
9. 中途关闭浏览器再次登录会恢复到上次步骤
10. 不影响原有页面展示和交互

## 停止，不继续下一个功能
- 本轮完成后停止，等待验收
