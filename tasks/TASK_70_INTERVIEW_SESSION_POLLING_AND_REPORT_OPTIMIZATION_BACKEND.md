# 模拟面试会话轮询与报告覆盖优化后端

## 当前任务所属模块

模拟面试会话、报告生成与日志健康性优化。

## 前端文件定位

前端联动位于 `frontend/app/src/api/interview.js`、`frontend/app/src/views/interview/InterviewReportView.vue`、`frontend/app/src/views/interview/InterviewSessionView.vue`，详细前端记录见 `frontend/tasks/TASK_70_INTERVIEW_SESSION_POLLING_AND_REPORT_OPTIMIZATION_FRONTEND.md`。

## 后端文件定位

后端改动位于 `server/src/main/java/com/airesume/server/controller/InterviewController.java`、`server/src/main/java/com/airesume/server/service/InterviewService.java`、`server/src/main/java/com/airesume/server/mapper/InterviewSessionMapper.java`、`server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`。

## 本轮修改文件清单

- `server/src/main/java/com/airesume/server/dto/interview/InterviewSessionStatusResponse.java`
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/main/java/com/airesume/server/mapper/InterviewSessionMapper.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/test/java/com/airesume/server/controller/InterviewControllerTest.java`
- `server/src/test/java/com/airesume/server/service/InterviewServiceTest.java`
- `server/src/test/java/com/airesume/server/service/impl/InterviewAiServiceImplTest.java`

## 前端实现方案

后端提供轻量状态接口供前端轮询使用，前端报告页和面试页不再通过完整会话详情接口反复读取聊天记录与评估报告大字段。

## 后端实现方案

- 新增 `GET /api/interview/session/{sessionId}/status`，只返回会话归属内的轻量状态字段。
- `InterviewSessionMapper.selectOwnedStatus` 只查询 `status`、`openingGenerated` 推导状态、`evaluationReport` 是否存在、综合分与更新时间，避免加载 `evaluation_report` JSON 和聊天记录。
- `InterviewService.getSessionStatus` 统一做归属校验、状态文案和布尔字段归一化。
- `InterviewAiServiceImpl` 在生成评价报告用户 prompt 时补充“有效问答轮次总数”和逐轮“问题/回答/后续反馈或追问”摘要，降低报告只覆盖少数回答的概率。

## 数据存储方案

本轮不新增表、不改字段、不新增索引；轻量状态接口复用 `interview_session` 现有字段。

## stage 更新说明

已在 `tasks/stage.md` 追加本轮后端完成状态，前端状态同步记录在 `frontend/tasks/stage.md`。

## 编译结果

- `mvn.cmd -DskipTests compile` 通过。

## 构建结果

后端无独立前端构建；前端构建结果见前端任务文档。

## 当前功能验收说明

- `mvn.cmd "-Dtest=InterviewAiServiceImplTest#buildEvaluationUserPromptShouldExposeEffectiveQuestionAnswerRounds" test` 先失败后通过，锁定报告 prompt 必须包含有效问答轮次。
- `mvn.cmd "-Dtest=InterviewControllerTest,InterviewServiceTest,InterviewAiServiceImplTest" test` 通过，76 个用例全绿。
- 测试日志中出现的 SSE 持久化失败和自定义 AI 异常为既有单测刻意模拟场景，不是本轮失败。

## 停止，不继续下一功能

本轮仅完成模拟面试会话轻量轮询和报告有效问答轮次覆盖优化，等待验收，不继续扩展新的报告展示、导出或面试流程功能。
