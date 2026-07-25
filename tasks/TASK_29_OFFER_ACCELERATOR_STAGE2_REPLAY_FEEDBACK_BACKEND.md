# TASK_29_OFFER_ACCELERATOR_STAGE2_REPLAY_FEEDBACK_BACKEND

## 1. 当前任务所属模块
- Offer 加速器实施计划：第 2 部分，面试历史回放 + 即时反馈可选。

## 2. 前端文件定位
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/app/src/api/interview.js`
- `frontend/runtime/STATE.md`
- `frontend/tasks/stage.md`
- `frontend/tasks/TASK_29_OFFER_ACCELERATOR_STAGE2_REPLAY_FEEDBACK_FRONTEND.md`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/dto/interview/InterviewReplayRoundResponse.java`
- `server/src/main/java/com/airesume/server/dto/interview/InterviewSessionResponse.java`
- `server/src/main/java/com/airesume/server/dto/interview/SendMessageRequest.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/InterviewAiService.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/MockInterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/main/java/com/airesume/server/common/constants/InterviewConstants.java`
- `server/src/test/java/com/airesume/server/service/InterviewServiceTest.java`
- `server/src/test/java/com/airesume/server/service/impl/MockInterviewAiServiceImplTest.java`
- `runtime/STATE.md`

## 4. 本轮修改文件清单
- `server/src/main/java/com/airesume/server/common/constants/InterviewConstants.java`
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/main/java/com/airesume/server/dto/interview/InterviewReplayRoundResponse.java`
- `server/src/main/java/com/airesume/server/dto/interview/InterviewSessionResponse.java`
- `server/src/main/java/com/airesume/server/dto/interview/SendMessageRequest.java`
- `server/src/main/java/com/airesume/server/service/InterviewAiService.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/MockInterviewAiServiceImpl.java`
- `server/src/test/java/com/airesume/server/service/InterviewServiceTest.java`
- `server/src/test/java/com/airesume/server/service/impl/MockInterviewAiServiceImplTest.java`
- `runtime/STATE.md`
- `tasks/TASK_29_OFFER_ACCELERATOR_STAGE2_REPLAY_FEEDBACK_BACKEND.md`
- `frontend/app/src/api/interview.js`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/runtime/STATE.md`
- `frontend/tasks/stage.md`
- `frontend/tasks/TASK_29_OFFER_ACCELERATOR_STAGE2_REPLAY_FEEDBACK_FRONTEND.md`

## 5. 前端实现方案
- 面试会话页新增“面完复盘 / 每题反馈”选择，默认面完复盘。
- 选择项保存为本地体验偏好，每次发送消息时通过 `feedbackMode` 透传给后端，不新增会话配置表。
- 会话等待状态文案调整为“面试官正在思考你的回答...”，继续沿用已有打字机流式展示。
- 报告页新增“面试历史回放”区块，按轮展示面试官问题、用户回答、AI 反馈/追问。
- 移动端输入区将反馈选择与发送按钮改为纵向布局，适配午休/移动端单题练习场景。

## 6. 后端实现方案
- 新增 `InterviewReplayRoundResponse`，由现有 `interview_chat_log` 派生回放轮次。
- `InterviewSessionResponse` 增加 `replayRounds` 字段，详情接口直接返回回放时间线，不新增查询接口。
- `SendMessageRequest` 增加 `feedbackMode`，只影响本次 AI 回复口径。
- 非流式和流式问答都把规范化后的 `feedbackMode` 传入 AI 服务。
- 真实 AI Prompt 在 `immediate` 模式下要求先给短反馈，再提出下一轮追问。
- Mock 与真实 AI fallback 也补齐即时反馈前缀，保证联调和故障兜底体验一致。

## 7. 数据存储方案
- 不新增数据库表。
- 不修改 `interview_session`、`interview_chat_log` 或其他核心表结构。
- `replayRounds` 完全由现有聊天记录派生。
- `feedbackMode` 不落库，只作为本次请求参数使用。
- 明确不做题库、热点、收藏、命中率统计。

## 8. stage 更新说明
- 已更新 `runtime/STATE.md`，标记第 2 部分本轮已完成，等待人工验收。
- 已同步更新 `frontend/runtime/STATE.md` 与 `frontend/tasks/stage.md`。
- 第 3 部分多面试官人设、第 4 部分 Offer 辅助链路仍为尚未开始。

## 9. 编译结果
- 命令：`mvn.cmd -q -DskipTests compile`
- 结果：通过

## 10. 构建结果
- 命令：`npm.cmd run build`
- 结果：通过

## 11. 测试结果
- 新增/相关后端测试命令：`mvn.cmd -q "-Dtest=InterviewServiceTest,MockInterviewAiServiceImplTest" test`
- 结果：通过
- 后端关键测试集合命令：`mvn.cmd -q "-Dtest=InterviewServiceTest,MockInterviewAiServiceImplTest,ResumeDiagnosisTaskServiceImplTest,ResumeDiagnosisProcessorTest,ResumeAiServiceImplTest,ResumePdfControllerTest" test`
- 结果：通过
- 测试日志中的 `DB connection lost`、简历诊断超时为既有测试刻意覆盖的异常路径，最终测试通过。
- 前端当前无 test 脚本，本轮以前端构建通过作为前端验证。

## 12. 当前功能验收说明
- 会话页可在发送前选择“面完复盘”或“每题反馈”。
- 选择“每题反馈”时，本次 AI 回复应先给简短反馈，再继续追问。
- 报告页在会话详情返回 `replayRounds` 时应展示“问题 → 回答 → AI 反馈/追问”的时间线。
- 旧数据缺少足够聊天记录时，回放区不展示，不影响现有报告。

## 13. 停止，不继续下一个功能
- 本轮已完成第 2 部分：面试历史回放 + 即时反馈可选。
- 到此停止，等待人工验收。
