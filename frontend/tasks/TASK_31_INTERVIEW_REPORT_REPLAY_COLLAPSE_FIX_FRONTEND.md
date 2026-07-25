# TASK_31_INTERVIEW_REPORT_REPLAY_COLLAPSE_FIX_FRONTEND

## 1. 当前任务所属模块
- Offer 加速器实施计划：第 2 部分，面试历史回放 + 逐轮复盘展示修复。

## 2. 前端文件定位
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/runtime/STATE.md`
- `frontend/tasks/stage.md`

## 3. 后端文件定位
- 本轮不涉及后端接口、DTO、Service 或数据结构修改。

## 4. 本轮修改文件清单
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/runtime/STATE.md`
- `frontend/tasks/stage.md`
- `frontend/tasks/TASK_31_INTERVIEW_REPORT_REPLAY_COLLAPSE_FIX_FRONTEND.md`

## 5. 前端实现方案
- 在“逐轮复盘”中给问题和回答分别增加“面试官：”与“求职者：”前缀，避免用户无法判断发言角色。
- 在“面试历史回放”中保留整体区块折叠，同时按左侧序号代表的一轮回放消息增加独立折叠按钮。
- 一轮回放消息包含面试官问题、求职者回答和 AI 追问/反馈，折叠状态只保存在当前报告页本地响应式状态中，不写入接口、不改变历史数据结构。
- 统一解析回放文本中的 `<FEEDBACK>...</FEEDBACK>` 标签，避免历史数据把标签原文显示在对话内容中。
- 面试官问题区只展示问题文本，不展示反馈卡片；反馈只在 AI 追问区展示，并标注为“上一回答反馈”。

## 6. 后端实现方案
- 不涉及。

## 7. 数据存储方案
- 不新增存储。
- 不修改 `interview_session` 或 `interview_chat_log`。

## 8. stage 更新说明
- 已更新 `frontend/runtime/STATE.md`。
- 已更新 `frontend/tasks/stage.md`。
- 本轮标记为面试报告回放展示修复已完成，等待人工验收。

## 9. 编译结果
- 不涉及后端编译。

## 10. 构建结果
- 命令：`npm.cmd run build`
- 结果：通过

## 11. 测试结果
- 前端 `package.json` 当前没有 test 脚本，本轮以前端构建通过作为前端验证。

## 12. 当前功能验收说明
- 逐轮复盘的问题前显示“面试官：”，回答前显示“求职者：”。
- 面试历史回放仍可整体展开/收起。
- 面试历史回放中的每个序号轮次可独立展开/收起，轮次内包含问题、回答和追问。
- 回放中不再直接显示 `<FEEDBACK>` 与 `</FEEDBACK>` 标签原文。
- 面试官问题下不展示反馈；AI 追问下的反馈标题为“上一回答反馈”，表示它对应上一条求职者回答。

## 13. 停止，不继续下一个功能
- 本轮仅修复面试报告展示问题。
- 到此停止，等待人工验收。
