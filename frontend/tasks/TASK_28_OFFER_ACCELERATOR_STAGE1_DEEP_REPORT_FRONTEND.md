# TASK_28_OFFER_ACCELERATOR_STAGE1_DEEP_REPORT_FRONTEND

## 1. 当前任务所属模块
- Offer 加速器实施计划：第 1 部分，深度面试分析报告 V2。

## 2. 前端文件定位
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/runtime/STATE.md`
- `frontend/tasks/stage.md`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/dto/interview/InterviewEvaluationReport.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/MockInterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `tasks/TASK_28_OFFER_ACCELERATOR_STAGE1_DEEP_REPORT_BACKEND.md`

## 4. 本轮修改文件清单
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/runtime/STATE.md`
- `frontend/tasks/stage.md`
- `frontend/tasks/TASK_28_OFFER_ACCELERATOR_STAGE1_DEEP_REPORT_FRONTEND.md`
- `server/src/main/java/com/airesume/server/dto/interview/InterviewEvaluationReport.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/MockInterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/test/java/com/airesume/server/service/impl/MockInterviewAiServiceImplTest.java`
- `server/src/test/java/com/airesume/server/service/InterviewServiceTest.java`
- `runtime/STATE.md`
- `tasks/TASK_28_OFFER_ACCELERATOR_STAGE1_DEEP_REPORT_BACKEND.md`

## 5. 前端实现方案
- 基于现有 `InterviewReportView.vue` 扩展展示，不改路由和接口封装。
- 新增“3 条立即能做的事”区块，优先展示 `immediateActions` 前 3 条。
- 新增“逐轮复盘”区块，展示 `roundReviews` 的问题、回答、评分、复盘、追问失分、下次练法。
- 新增“失分模式”区块，展示 `followUpLossPoints` 与 `commonLossPatterns`。
- 在维度详情和雷达评分中增加 `projectExpression` 项目表达维度。
- 保持现有优势、不足、建议、逐题表现、岗位相关反馈等区块兼容旧报告。

## 6. 后端实现方案
- 后端报告 DTO 增加 V2 字段。
- 真实 AI Prompt 约束输出 V2 字段。
- Mock 与 fallback 报告补齐字段，确保前端不需要额外兜底新链路。

## 7. 数据存储方案
- 前端不新增本地存储。
- 后端不新增数据库表或字段。
- 新字段继续存入现有 `evaluation_report` JSON。
- 不做题库、热点、收藏、命中率统计。

## 8. stage 更新说明
- 已更新 `frontend/runtime/STATE.md`。
- 已更新 `frontend/tasks/stage.md`。
- 当前阶段标记为“第 1 部分：深度面试分析报告 V2，已完成，等待人工验收”。

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
- 打开面试报告页时，若报告包含 V2 字段，应展示立即行动建议、逐轮复盘和失分模式。
- 旧报告缺少 V2 字段时，页面仍保留原有报告展示，不影响历史报告可读性。
- 本轮未实现面试历史回放、即时反馈、多面试官人设、薪资谈判或谈薪模板。

## 13. 停止，不继续下一个功能
- 本轮已完成第 1 部分：深度面试分析报告 V2。
- 到此停止，等待人工验收。
