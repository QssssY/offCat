# TASK 55: 新手引导改任务式 — 前端

## 状态: 已完成

## 概述
Dashboard 新增新手任务卡片，展示 4 项任务完成进度和行动入口。核心页面静默上报任务完成，全部完成后卡片隐藏。

## 已完成内容

### API
- `onboarding.js` 新增 `getOnboardingTasks()` 和 `completeOnboardingTask(taskKey)`

### 组件
- 新建 `OnboardingTaskCard.vue`：
  - 标题 "快速上手" + 进度数/环形进度条
  - 4 个任务行：勾选状态 + 标签 + 描述 + 行动按钮/已完成
  - 已完成任务：橙色勾 + 删除线 + 透明度降低
  - 未完成任务：橙色"去完成"按钮跳转对应路由
  - 响应式适配（手机端隐藏描述文字）

### Dashboard 集成
- `DashboardView.vue`：
  - 并行请求 `getOnboardingTasks()` 数据
  - 在 stats-section 和 growth-entry-card 之间插入 OnboardingTaskCard
  - `v-if="onboardingVisible && !onboardingAllCompleted"` 控制显隐

### 静默上报
| 任务 | 文件 | 触发点 |
|------|------|--------|
| resume_uploaded | UploadView.vue | handleSubmit 成功后、router.push 之前 |
| report_viewed | ResultView.vue | fetchTaskDetail 中 status 首次变为 2 的分支 |
| jd_compared | ResultView.vue | submitJobMatchAnalysis 成功后 |
| interview_completed | InterviewReportView.vue | watch(hasReport) 首次变为 true |

所有上报采用 fire-and-forget：`completeOnboardingTask(key).catch(() => {})`

## 验证
- 前端构建：`npm run build` 通过
