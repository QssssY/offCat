# TASK_28_OFFER_ACCELERATOR_STAGE1_DEEP_REPORT_BACKEND

## 1. 当前任务所属模块
- Offer 加速器实施计划：第 1 部分，深度面试分析报告 V2。

## 2. 前端文件定位
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/runtime/STATE.md`
- `frontend/tasks/stage.md`
- `frontend/tasks/TASK_28_OFFER_ACCELERATOR_STAGE1_DEEP_REPORT_FRONTEND.md`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/dto/interview/InterviewEvaluationReport.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/MockInterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/test/java/com/airesume/server/service/impl/MockInterviewAiServiceImplTest.java`
- `server/src/test/java/com/airesume/server/service/InterviewServiceTest.java`
- `runtime/STATE.md`

## 4. 本轮修改文件清单
- `server/src/main/java/com/airesume/server/dto/interview/InterviewEvaluationReport.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/MockInterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/test/java/com/airesume/server/service/impl/MockInterviewAiServiceImplTest.java`
- `server/src/test/java/com/airesume/server/service/InterviewServiceTest.java`
- `runtime/STATE.md`
- `tasks/TASK_28_OFFER_ACCELERATOR_STAGE1_DEEP_REPORT_BACKEND.md`
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/runtime/STATE.md`
- `frontend/tasks/stage.md`
- `frontend/tasks/TASK_28_OFFER_ACCELERATOR_STAGE1_DEEP_REPORT_FRONTEND.md`

## 5. 前端实现方案
- 前端复用现有面试报告页，不新增路由、不新增题库、不新增独立业务模块。
- 报告页新增 V2 深度分析区块：3 条立即行动建议、逐轮复盘、失分模式。
- 维度详情和雷达评分新增 `projectExpression` 项目表达维度。

## 6. 后端实现方案
- 复用现有 `InterviewEvaluationReport`，新增以下字段：
  - `roundReviews`：逐轮回放式复盘。
  - `followUpLossPoints`：追问失分点。
  - `commonLossPatterns`：常见失分模式。
  - `immediateActions`：3 条立即行动建议。
  - `projectExpression`：项目表达能力维度评分。
- 真实 AI 报告 Prompt 增加 V2 输出 JSON 字段和生成规则。
- Mock 报告生成补齐 V2 字段，覆盖普通、压力、岗位定向和空历史场景。
- AI 失败 fallback 报告补齐 V2 字段，保证报告页不因真实 AI 失败而缺少关键结构。
- 旧版兼容字段 `dimensions`、`suggestions`、`improvements` 保留。

## 7. 数据存储方案
- 不新增数据库表。
- 不修改 `interview_session` 或 `interview_chat_log` 表结构。
- V2 报告字段继续随现有 `evaluation_report` JSON 存储。
- 明确不做题库、题库热点、收藏、命中率等数据结构。

## 8. stage 更新说明
- 已更新 `runtime/STATE.md`，标记第 1 部分已完成，等待验收。
- 已同步前端 `frontend/runtime/STATE.md` 与 `frontend/tasks/stage.md`。
- 已新增前后端第 1 部分 task 文件。

## 9. 编译结果
- 命令：`mvn.cmd -q -DskipTests compile`
- 结果：通过

## 10. 构建结果
- 命令：`npm.cmd run build`
- 结果：通过

## 11. 测试结果
- 新增/相关后端测试命令：`mvn.cmd -q "-Dtest=InterviewServiceTest,MockInterviewAiServiceImplTest" test`
- 结果：通过
- 关键测试集合命令：`mvn.cmd -q "-Dtest=InterviewServiceTest,MockInterviewAiServiceImplTest,ResumeDiagnosisTaskServiceImplTest,ResumeDiagnosisProcessorTest,ResumeAiServiceImplTest,ResumePdfControllerTest" test`
- 结果：通过
- 前端当前无 test 脚本，本轮以前端构建通过作为前端验证。

## 12. 当前功能验收说明
- 结束面试后生成的结构化报告应包含 V2 深度分析字段。
- 报告页应能展示用户明天可练的 3 条行动建议。
- 报告页应能展示逐轮复盘、追问失分点、常见失分模式。
- 雷达与维度详情应包含项目表达维度。
- 本轮不包含面试历史回放、即时反馈可选、多面试官人设、Offer 辅助链路。

## 13. 停止，不继续下一个功能
- 本轮已完成第 1 部分：深度面试分析报告 V2。
- 到此停止，等待人工验收。
