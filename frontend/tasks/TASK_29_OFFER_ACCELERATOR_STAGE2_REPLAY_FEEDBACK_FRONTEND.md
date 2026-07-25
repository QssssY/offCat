# TASK_29_OFFER_ACCELERATOR_STAGE2_REPLAY_FEEDBACK_FRONTEND

## 1. 当前任务所属模块
- Offer 加速器实施计划：第 2 部分，面试历史回放 + 即时反馈可选。

## 2. 前端文件定位
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/app/src/api/interview.js`
- `frontend/runtime/STATE.md`
- `frontend/tasks/stage.md`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/dto/interview/InterviewReplayRoundResponse.java`
- `server/src/main/java/com/airesume/server/dto/interview/InterviewSessionResponse.java`
- `server/src/main/java/com/airesume/server/dto/interview/SendMessageRequest.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/InterviewAiService.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/MockInterviewAiServiceImpl.java`
- `tasks/TASK_29_OFFER_ACCELERATOR_STAGE2_REPLAY_FEEDBACK_BACKEND.md`

## 4. 本轮修改文件清单
- `frontend/app/src/api/interview.js`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/runtime/STATE.md`
- `frontend/tasks/stage.md`
- `frontend/tasks/TASK_29_OFFER_ACCELERATOR_STAGE2_REPLAY_FEEDBACK_FRONTEND.md`
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

## 5. 前端实现方案
- 会话页复用原输入区，在发送按钮旁增加反馈模式单选：
  - `after_interview`：面完复盘，保持原节奏。
  - `immediate`：每题反馈，发送时透传给后端。
- 反馈模式保存到浏览器本地偏好，方便用户下次继续使用同一体验，但不改变后端会话存储。
- 等待状态文案改为“面试官正在思考你的回答...”，并继续使用已有流式打字机展示。
- 报告页读取会话详情中的 `replayRounds`，新增“面试历史回放”区块。
- 移动端输入 footer 改为纵向布局，保证反馈选择和发送按钮不挤压。

## 6. 后端实现方案
- 会话详情接口返回 `replayRounds`。
- 发送消息接口支持 `feedbackMode`。
- 后端 AI 服务根据 `feedbackMode` 调整 Prompt 或 Mock 输出。

## 7. 数据存储方案
- 前端只保存本地反馈模式偏好。
- 后端不新增表，不新增字段。
- 回放数据由已有 `interview_chat_log` 派生。
- 不做题库、热点、收藏、命中率统计。

## 8. stage 更新说明
- 已更新 `frontend/runtime/STATE.md`。
- 已更新 `frontend/tasks/stage.md`。
- 当前阶段标记为“第 2 部分：面试历史回放 + 即时反馈可选，已完成，等待人工验收”。

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
- 前端 `package.json` 当前没有 test 脚本，本轮以前端构建通过作为前端验证。

## 12. 当前功能验收说明
- 会话页可以选择每题即时反馈或面完统一复盘。
- 报告页可以按时间线查看面试历史回放。
- 移动端输入区域不会因新增反馈选择导致按钮文字挤压或溢出。
- 本轮未实现多面试官人设、薪资谈判、谈薪模板、录用意向评估或背调指导。

## 13. 停止，不继续下一个功能
- 本轮已完成第 2 部分：面试历史回放 + 即时反馈可选。
- 到此停止，等待人工验收。
