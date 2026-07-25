# TASK_30_OFFER_ACCELERATOR_STAGE3_INTERVIEWER_PERSONA_BACKEND

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
- `frontend/tasks/TASK_30_OFFER_ACCELERATOR_STAGE3_INTERVIEWER_PERSONA_FRONTEND.md`

## 3. 后端文件定位
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
- `db/schema.sql`
- `server/db/schema.sql`
- `runtime/STATE.md`

## 4. 本轮修改文件清单
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
- `db/schema.sql`
- `server/db/schema.sql`
- `runtime/STATE.md`
- `tasks/TASK_30_OFFER_ACCELERATOR_STAGE3_INTERVIEWER_PERSONA_BACKEND.md`
- `frontend/app/src/constants/interview.js`
- `frontend/app/src/views/interview/InterviewEntryView.vue`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/app/src/views/interview/InterviewHistoryView.vue`
- `frontend/runtime/STATE.md`
- `frontend/tasks/stage.md`
- `frontend/tasks/TASK_30_OFFER_ACCELERATOR_STAGE3_INTERVIEWER_PERSONA_FRONTEND.md`

## 5. 前端实现方案
- 复用现有面试入口页“面试模式”选择区，不新增页面。
- 在 `INTERVIEW_MODE_OPTIONS` 中增加三个固定人设：
  - 大厂 HR 面：侧重行为面试、文化匹配。
  - 技术 Leader 面：侧重技术细节、项目深挖。
  - 外企面试官：侧重英文表达和逻辑。
- 会话页、历史页、报告页统一通过 `getInterviewModeLabel` 回显模式文案。
- 移动端模式选项采用两列网格，避免新增人设后按钮溢出。

## 6. 后端实现方案
- 复用现有 `interview_mode` 字段保存人设，不新增表字段。
- `InterviewConstants` 增加固定人设常量和白名单校验，拒绝任意自定义模式进入 Prompt。
- `InterviewService` 扩展模式规范化、开场白人设提示和回显文案。
- 岗位定向与人设可叠加：`jobTargeted` 继续表示岗位定向上下文，`interviewMode` 保留面试官风格。
- `InterviewAiService` 内部接口增加 `interviewMode` 参数，流式与非流式链路都传入已持久化模式。
- 真实 AI Prompt 增加三种人设指令；Mock 与真实 AI fallback 也补齐人设语气。
- 评价报告 Prompt 的模式描述同步支持人设，保证报告口径与面试过程一致。

## 7. 数据存储方案
- 不新增数据库表。
- 不新增数据库字段。
- 继续复用 `interview_session.interview_mode`，该列当前 `VARCHAR(20)` 可容纳三个新增值。
- 仅更新 `db/schema.sql` 与 `server/db/schema.sql` 中字段注释，说明可选值，不需要迁移脚本。
- 不做开放式自定义人设。
- 不做题库、热点、收藏、命中率统计。

## 8. stage 更新说明
- 已更新 `runtime/STATE.md`，标记第 3 部分本轮已完成，等待人工验收。
- 已同步更新 `frontend/runtime/STATE.md` 与 `frontend/tasks/stage.md`。
- 第 4 部分 Offer 辅助链路仍为尚未开始。

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
- 面试入口页应出现普通面试、压力面试、大厂 HR 面、技术 Leader 面、外企面试官五个模式。
- 选择大厂 HR 面后，AI 提问应更偏行为面试、团队协作和文化匹配。
- 选择技术 Leader 面后，AI 提问应更偏技术深度、项目细节和个人贡献。
- 选择外企面试官后，AI 应主要用英文面试并关注表达逻辑。
- 岗位定向开关仍可与人设叠加，历史和报告中应同时保留岗位定向标记与人设文案。

## 13. 停止，不继续下一个功能
- 本轮已完成第 3 部分：多面试官人设系统。
- 到此停止，等待人工验收。
