# TASK_30_OFFER_ACCELERATOR_STAGE3_INTERVIEWER_PERSONA_FRONTEND

## 1. 当前任务所属模块
- Offer 加速器实施计划：第 3 部分，多面试官人设系统。

## 2. 前端文件定位
- `frontend/app/src/constants/interview.js`
- `frontend/app/src/views/interview/InterviewEntryView.vue`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/app/src/views/interview/InterviewHistoryView.vue`
- `frontend/runtime/STATE.md`
- `frontend/tasks/stage.md`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/common/constants/InterviewConstants.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/MockInterviewAiServiceImpl.java`
- `tasks/TASK_30_OFFER_ACCELERATOR_STAGE3_INTERVIEWER_PERSONA_BACKEND.md`

## 4. 本轮修改文件清单
- `frontend/app/src/constants/interview.js`
- `frontend/app/src/views/interview/InterviewEntryView.vue`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/app/src/views/interview/InterviewHistoryView.vue`
- `frontend/runtime/STATE.md`
- `frontend/tasks/stage.md`
- `frontend/tasks/TASK_30_OFFER_ACCELERATOR_STAGE3_INTERVIEWER_PERSONA_FRONTEND.md`
- `server/src/main/java/com/airesume/server/common/constants/InterviewConstants.java`
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/main/java/com/airesume/server/dto/interview/CreateSessionRequest.java`
- `server/src/main/java/com/airesume/server/dto/interview/InterviewSessionResponse.java`
- `server/src/main/java/com/airesume/server/entity/InterviewSession.java`
- `server/src/main/java/com/airesume/server/service/InterviewAiService.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/MockInterviewAiServiceImpl.java`
- `server/src/test/java/com/airesume/server/service/InterviewServiceTest.java`
- `server/src/test/java/com/airesume/server/service/impl/MockInterviewAiServiceImplTest.java`
- `runtime/STATE.md`
- `tasks/TASK_30_OFFER_ACCELERATOR_STAGE3_INTERVIEWER_PERSONA_BACKEND.md`

## 5. 前端实现方案
- 在现有面试入口页 `modeOptions` 基础上扩展固定人设，不新增新路由。
- 抽取 `INTERVIEW_MODE_LABEL_MAP`、`INTERVIEW_MODE_OPTIONS`、`getInterviewModeLabel` 到面试常量文件。
- 会话页、报告页、历史页统一复用模式标签函数，避免不同页面文案不一致。
- 开始按钮根据当前选择显示“开始大厂 HR 面 / 开始技术 Leader 面 / 开始外企面试官”等文案。
- 移动端模式选项改为两列网格，控制按钮宽度和文本展示。

## 6. 后端实现方案
- 后端支持保存并回显三个固定人设。
- AI Prompt 按人设调整提问风格。
- Mock 与 fallback 保持本地联调可见的人设差异。

## 7. 数据存储方案
- 前端不新增本地存储。
- 后端复用现有 `interview_mode` 字段。
- 不新增数据库表或字段。
- 不做开放式自定义人设。
- 不做题库、热点、收藏、命中率统计。

## 8. stage 更新说明
- 已更新 `frontend/runtime/STATE.md`。
- 已更新 `frontend/tasks/stage.md`。
- 当前阶段标记为“第 3 部分：多面试官人设系统，已完成，等待人工验收”。

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
- 面试入口页可以选择三个新增面试官人设。
- 历史页、会话页、报告页应正确显示人设名称。
- 岗位定向与人设叠加时，页面仍展示岗位定向标签。
- 本轮未实现薪资谈判、谈薪模板、录用意向评估或背调指导。

## 13. 停止，不继续下一个功能
- 本轮已完成第 3 部分：多面试官人设系统。
- 到此停止，等待人工验收。
