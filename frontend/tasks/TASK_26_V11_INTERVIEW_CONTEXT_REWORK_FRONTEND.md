# TASK_26_V11_INTERVIEW_CONTEXT_REWORK_FRONTEND

## 1. 当前任务所属模块
- V1.1 第三个功能返修：岗位定向模拟面试 / 普通模拟面试简历上下文修复

## 2. 问题原因定位
- 前端普通模拟面试默认没有透传 `resumeTaskId`，从简历结果页进入时容易丢失更明确的简历关联。
- 会话页、历史页、报告页对 `jobTargeted=true` 的模式展示仍依赖 `interviewMode=normal` 的旧值，导致岗位定向会话文案混成普通面试。

## 3. 前端修改文件定位
- `frontend/app/src/api/interview.js`
- `frontend/app/src/views/interview/InterviewEntryView.vue`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/views/interview/InterviewHistoryView.vue`
- `frontend/app/src/views/interview/InterviewReportView.vue`

## 4. 后端修改文件定位
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/MockInterviewJobTargetServiceImpl.java`

## 5. 本轮修改文件清单
- `frontend/app/src/api/interview.js`
- `frontend/app/src/views/interview/InterviewEntryView.vue`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/views/interview/InterviewHistoryView.vue`
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/runtime/STATE.md`
- `frontend/tasks/TASK_26_V11_INTERVIEW_CONTEXT_REWORK_FRONTEND.md`

## 6. 普通模拟面试上下文修复说明
- 入口页创建 payload 时，普通模拟面试也会透传 `resumeTaskId`。
- 这样从简历诊断结果页进入普通模拟面试时，后端能优先复用明确的简历任务，不必只依赖“最近一次任务”兜底。

## 7. 岗位定向模拟面试上下文修复说明
- 前端不再自行猜测岗位定向语义，统一使用后端返回的 `jobTargeted`、`interviewMode`、`interviewModeDesc`。
- 会话页、历史页、报告页都增加了 `job_targeted` / `jobTargeted` 的兜底判断，保证刷新后也不会把岗位定向显示成普通面试。

## 8. AI Prompt 修复说明
- 前端本轮不直接拼 Prompt。
- 本轮配合点是把更明确的 `resumeTaskId` 继续传给后端，确保后端 Prompt 有足够上下文。

## 9. Mock AI 修复说明
- 前端不实现 Mock AI。
- 已同步适配 Mock AI 修复后的返回语义，确保本地联调页面展示一致。

## 10. 返回结构修复说明
- 会话页模式兜底新增 `job_targeted -> 岗位定向模拟`。
- 历史页模式映射新增 `job_targeted -> 岗位定向模拟`。
- 报告页模式兜底新增 `jobTargeted / interviewMode=job_targeted -> 岗位定向模拟`。

## 11. stage 更新说明
- 已更新 `frontend/runtime/STATE.md`，当前阶段改为“V1.1 第三阶段返修：模拟面试上下文修复”。
- 当前状态改为“返修完成，等待人工复验”。

## 12. task 更新说明
- 新增 `frontend/tasks/TASK_26_V11_INTERVIEW_CONTEXT_REWORK_FRONTEND.md` 记录本轮返修内容。
- 项目根目录缺少 `DEVELOPMENT_RULES.txt`，前端侧按 `frontend/runtime/RULES.md` 和相关运行时规则执行。

## 13. 编译结果
- 后端编译命令：`mvn.cmd -q -DskipTests compile`
- 结果：通过

## 14. 构建结果
- 前端构建命令：`npm.cmd run build`
- 结果：通过

## 15. 自测场景与结果
- 场景一：普通模拟面试 + 有简历
  - 结果：入口页会继续透传 `resumeTaskId`，后端可优先命中该简历。
- 场景二：普通模拟面试 + 无简历
  - 结果：前端不伪造简历标识，展示仍为普通面试。
- 场景三：岗位定向模拟 + 有简历 + 测试工程师 JD
  - 结果：会话页、历史页、报告页均展示为“岗位定向模拟”，不再误显示“普通面试”。
- 场景四：岗位定向模拟 + 有 JD + 无简历
  - 结果：页面仍按岗位定向语义展示，不依赖前端本地是否持有简历正文。

## 16. 当前返修验收说明
- 入口页、会话页、历史页、报告页的岗位定向语义已统一。
- 普通模拟面试与岗位定向模拟面试的前端展示不再混淆。
- 本轮未扩展 V1.1 范围外功能。

## 17. 停止，不继续下一个功能
- 本轮仅完成模拟面试上下文返修，到此停止，等待人工复验。
