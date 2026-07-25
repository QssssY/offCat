# 简历诊断状态轮询前端优化

## 当前任务所属模块

- 前端模块：简历上传结果页、简历诊断任务等待轮询。
- 触发原因：最新 `debug.txt` 显示简历诊断流程正常，但结果生成等待阶段前端反复调用完整详情接口，导致后端重复读取 `resume_text`、`diagnosis_result` 等大字段。

## 前端文件定位

- API 封装：`frontend/app/src/api/resume.js`
- 结果页：`frontend/app/src/views/resume/ResultView.vue`
- 测试：`frontend/app/src/__tests__/api/resume.test.js`、`frontend/app/src/__tests__/views/ResumeResultView.test.js`

## 后端文件定位

- 后端联动任务见 `tasks/TASK_73_RESUME_DIAGNOSIS_STATUS_POLLING_AND_SQL_LOG_BACKEND.md`。

## 本轮修改文件清单

- `src/api/resume.js` 新增 `getResumeTaskStatus(taskId)`，请求 `/api/resume/task/{taskId}/status`。
- `src/views/resume/ResultView.vue` 首次进入和等待轮询改为优先调用轻量状态接口。
- 任务状态变为完成后再调用一次 `getResumeTask()` 获取完整诊断详情。
- `src/__tests__/api/resume.test.js` 增加轻量状态 API 封装测试。
- `src/__tests__/views/ResumeResultView.test.js` 增加等待期间只轮询状态、完成后才拉完整详情的回归测试，并补充测试挂载组件的卸载清理。

## 前端实现方案

- 结果页把“完整详情加载”和“轻量状态加载”拆开：
  - 等待中只更新 `status`、`stage`、`errorMsg` 等轻量字段。
  - 完成后保留原有用户信息刷新、新手任务上报和完整详情展示逻辑。
- 轻量状态合并时保留已有诊断详情、岗位匹配、AI 润色等展示数据，避免完成后本地已存在的数据被状态响应覆盖。
- 手动刷新和后续 JD 匹配/AI 润色同步仍使用完整详情接口，保证展示内容完整。

## 后端实现方案

- 后端新增轻量状态接口，详见后端任务文件。

## 数据存储方案

- 不修改前端持久化数据。
- 不新增后端数据库结构。

## stage 更新说明

- 已在 `frontend/tasks/stage.md` 顶部记录本轮前端优化、验证命令和停止边界。

## 编译结果

- 前端无单独编译命令，构建结果见下方。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

- `npm.cmd test -- --run src/__tests__/api/resume.test.js src/__tests__/views/ResumeResultView.test.js` 通过，2 个测试文件、8 个用例全绿。
- 回归覆盖：
  - API 封装命中 `/api/resume/task/{taskId}/status`。
  - 等待阶段不调用完整详情接口。
  - 任务完成后只补拉一次完整详情。

## 停止，不继续下一功能

本轮只处理简历诊断结果页等待轮询降载，不继续扩展报告展示、导出、AI 生成策略或其它简历功能。
