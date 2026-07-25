# 模拟面试会话轮询与报告覆盖优化前端

## 当前任务所属模块

模拟面试会话页、模拟面试报告页。

## 前端文件定位

前端改动位于 `frontend/app/src/api/interview.js`、`frontend/app/src/views/interview/InterviewReportView.vue`、`frontend/app/src/views/interview/InterviewSessionView.vue`。

## 后端文件定位

后端联动接口位于 `server/src/main/java/com/airesume/server/controller/InterviewController.java`、`server/src/main/java/com/airesume/server/service/InterviewService.java`、`server/src/main/java/com/airesume/server/mapper/InterviewSessionMapper.java`。

## 本轮修改文件清单

- `frontend/app/src/api/interview.js`
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/__tests__/api/interview.test.js`
- `frontend/app/src/__tests__/views/InterviewReportView.test.js`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`

## 前端实现方案

- 新增 `getInterviewSessionStatus(sessionId)` API 封装，调用后端轻量状态接口。
- 报告页轮询先查轻量状态，只有 `reportReady` 为真时才重新拉取完整会话详情。
- 面试页开场白轮询先查轻量状态，只有开场白不再 pending 时才拉取完整会话和聊天记录。

## 后端实现方案

后端新增轻量状态接口并保持鉴权归属校验，详细记录见 `tasks/TASK_70_INTERVIEW_SESSION_POLLING_AND_REPORT_OPTIMIZATION_BACKEND.md`。

## 数据存储方案

本轮前端不新增本地持久化、不修改后端数据库结构。

## stage 更新说明

已在 `frontend/tasks/stage.md` 追加本轮前端完成状态，后端状态同步记录在 `tasks/stage.md`。

## 编译结果

前端无独立编译命令；构建验证见下一节。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

- `npm.cmd test -- --run src/__tests__/api/interview.test.js src/__tests__/views/InterviewReportView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/utils/export.test.js` 通过，4 个测试文件、43 个用例全绿；其中 `export.test.js` 覆盖的是仓库已有的 XLSX 按需加载提交，不属于本轮新增差异。
- 报告等待和开场白等待阶段不再反复拉取完整聊天记录与评估报告大字段。
- 前端工作区已有未跟踪 `test-results/`，本轮未修改。

## 停止，不继续下一功能

本轮仅完成模拟面试轮询降载和报告生成触发优化，等待验收，不继续扩展新的页面或导出能力。
