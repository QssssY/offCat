# TASK_INTERVIEW_STREAM_RATE_LIMIT_NOTICE_FRONTEND

## 当前任务所属模块
- 模拟面试语音通话
- 流式消息错误处理
- 限流用户提示

## 前端文件定位
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`

## 后端文件定位
- `server/src/main/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilter.java`
- `server/src/test/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilterTest.java`

## 本轮修改文件清单
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`
- `frontend/tasks/stage.md`
- `frontend/tasks/TASK_INTERVIEW_STREAM_RATE_LIMIT_NOTICE_FRONTEND.md`

## 前端实现方案
- 裸 `fetch` 流式接口收到 HTTP 429 时，构造专用限流错误并使用 warning 提示。
- 提示文案固定为“发送太频繁，请稍后继续。10 分钟内最多 60 轮对话。”，避免只展示后端通用“请求过于频繁”。
- 语音面试收到 429 时不执行 `voiceCall.endVoiceCall()`，保留当前语音通话状态，便于用户稍后继续。
- 其他网络错误、SSE 中断、AI 错误仍沿用原错误处理，语音通话失败时退出通话态。

## 后端实现方案
- 后端限流阈值调整记录见 `tasks/fixes/TASK_INTERVIEW_STREAM_RATE_LIMIT_2026_05_30_BACKEND.md`。

## 数据存储方案
- 不新增数据库表
- 不修改数据库字段
- 不新增前端持久化字段

## stage 更新说明
- 已在 `frontend/tasks/stage.md` 增加本轮修复记录。

## 构建结果
- `npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 已通过。
- `npm.cmd run build` 已通过。

## 当前功能验收说明
- 语音面试触发 429 时，用户会看到明确的限流提醒。
- 语音通话状态不会被限流错误直接挂断。
- 普通错误仍会按原逻辑结束语音通话并显示错误提示。

## 停止，不继续下一功能
- 本轮只处理流式面试 429 提示与语音通话状态保留。
- 不继续扩展动态限流配置、会员分层提示、倒计时恢复或全局错误提示改造。
