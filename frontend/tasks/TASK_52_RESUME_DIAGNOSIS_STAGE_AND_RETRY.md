# TASK-52：简历诊断进度可视化 + 失败重试 — 前端

## 所属模块
简历诊断模块

## 功能目标
前端使用后端 stage 字段驱动 4 阶段进度展示，失败任务支持一键重试（复用原文件）。

## 前端变更清单

### API
- `api/resume.js` — 新增 retryResumeTask(taskId)

### ResultView
- 使用后端 stage 映射 currentStageIndex（queued=0, extracting=1, ai_analyzing=2, enhancing=3）
- 新增 stageProgressPercent 计算属性，传递给 AiLoadingState
- 失败区域增加"重新诊断"按钮（调用 retryResumeTask → 跳转新任务页面）
- 移除空 onMounted，明确由 immediate taskId watcher 负责首次数据加载

### HistoryView
- 失败卡片：显示 errorMsg 提示 + 内联"重新诊断"按钮（调用 retryResumeTask）
- 重试后跳转到新任务 ResultView
- 重试响应缺少新任务 ID 时提示异常并停止跳转，避免进入错误结果页

### 错误码映射
- `errorMessages.js` — 新增 2011（不可重试）/ 2012（重试时效已过）

## 验证状态
- [x] npm run build 通过
- [x] 修复回归测试通过：`npm.cmd test -- --run src/__tests__/views/ResumeResultView.test.js src/__tests__/views/ResumeHistoryView.test.js`
