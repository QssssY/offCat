# 简历诊断结果页轮询降噪前端（2026-06-04）

## 当前任务所属模块

用户端简历诊断结果页 / 任务状态轮询。

## 前端文件定位

- `frontend/app/src/views/resume/ResultView.vue`
- `frontend/app/src/__tests__/views/ResumeResultView.test.js`

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/ResumeDiagnosisController.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImpl.java`

本轮不修改后端文件；继续使用既有轻量状态接口 `GET /api/resume/task/{taskId}/status` 和完整详情接口 `GET /api/resume/task/{taskId}`。

## 本轮修改文件清单

- `ResultView.vue`：将 processing / AI 分析阶段轮询间隔从 2 秒调整为 6 秒，pending / 排队阶段保持 3 秒。
- `ResumeResultView.test.js`：使用 fake timer 锁定 pending 3 秒、processing 6 秒的轮询节奏，并确认任务完成后只拉取一次完整详情。

## 前端实现方案

- 继续复用现有 `startPolling -> fetchTaskStatus -> loadTaskStatus` 链路，不新增 SSE、长轮询或新的请求封装。
- 保留 pending 阶段 3 秒轮询，让排队状态仍能较快更新。
- 将 processing 阶段轮询放慢到 6 秒，减少 AI 分析长耗时期间的状态接口请求量。
- 完成状态仍通过轻量状态接口识别，识别到 `status=2` 后只调用一次 `getResumeTask` 拉完整诊断详情。

## 后端实现方案

本轮无后端实现；后端轻量状态接口已避免反复拉取 `resume_text` / `diagnosis_result` 大字段。

## 数据存储方案

本轮无数据库表、字段、索引、缓存或迁移脚本变更。

## stage 更新说明

`frontend/tasks/stage.md` 已置顶补充“简历诊断结果页轮询降噪前端”记录；根目录后端 stage 不更新，因为本轮没有后端改动。

## 编译结果

本轮无后端修改，未执行后端编译。

## 构建结果

- RED：`npm.cmd test -- --run src/__tests__/views/ResumeResultView.test.js` 在旧实现下失败，5999ms 时已经触发第 3 次状态请求，确认 processing 阶段仍按 2 秒轮询。
- GREEN：`npm.cmd test -- --run src/__tests__/views/ResumeResultView.test.js` 通过，1 个测试文件 / 7 个用例。
- 构建：`npm.cmd run build` 通过。

## 当前功能验收说明

- 用户进入简历诊断结果页后，排队状态仍每 3 秒查询一次轻量状态。
- 任务进入 AI 分析 / processing 后，每 6 秒查询一次轻量状态，降低长耗时分析期间的请求噪音。
- 任务完成后才拉取完整详情，并保持只拉一次完整诊断结果。
- 本轮不修改 dashboard 请求，不新增聚合接口，不改数据库和后端接口。

## 停止，不继续下一个功能

本轮仅完成简历诊断结果页轮询降噪，等待验收，不继续推进管理端 dashboard 聚合优化。
