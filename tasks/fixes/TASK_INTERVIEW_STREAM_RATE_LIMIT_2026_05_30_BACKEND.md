# TASK_INTERVIEW_STREAM_RATE_LIMIT_2026_05_30_BACKEND

## 当前任务所属模块
- 后端安全限流
- 模拟面试流式消息
- 语音面试多轮对话体验

## 前端文件定位
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`

## 后端文件定位
- `server/src/main/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilter.java`
- `server/src/test/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilterTest.java`
- `server/README.md`
- `docs/api/TASK_05_INTERVIEW_API.md`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`

## 本轮修改文件清单
- `server/src/main/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilter.java`
- `server/src/test/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilterTest.java`
- `server/README.md`
- `docs/api/TASK_05_INTERVIEW_API.md`
- `tasks/stage.md`
- `tasks/fixes/TASK_INTERVIEW_STREAM_RATE_LIMIT_2026_05_30_BACKEND.md`

## 问题原因
- `interview_stream` 原限流为每用户 10 次 / 10 分钟。
- 语音面试会把用户每一轮自动转写后的回答发送到 `/api/interview/session/{sessionId}/message/stream`，正常面试很容易超过 10 轮。
- 旧阈值会把正常语音面试误判为高频滥用，导致后端返回 `429 TOO_MANY_REQUESTS`，前端表现为通话过程中突然无法继续发送。

## 后端实现方案
- 保留 `interview_stream` 独立策略，仍优先于通用面试写操作匹配，便于独立观测和拦截异常刷接口。
- 将流式面试消息限流从 10 次 / 10 分钟调整为 60 次 / 10 分钟，覆盖高强度语音面试节奏。
- 补充回归测试，覆盖 60 轮语音面试流式发送不被拦截，以及第 61 次仍返回 429。
- 前端裸 `fetch` 收到 429 时改为提示“发送太频繁，请稍后继续。10 分钟内最多 60 轮对话。”，并保留当前语音通话状态，避免限流错误直接挂断。

## 数据存储方案
- 不新增数据库表
- 不修改数据库字段
- 不新增缓存结构
- Redis / 本地限流 key 名称保持 `interview_stream` 不变，仅调整阈值

## stage 更新说明
- 已在 `tasks/stage.md` 增加本轮修复记录。

## 编译结果
- `mvn.cmd -q -DskipTests compile` 已通过。
- `npm.cmd run build` 已通过。

## 构建结果
- `mvn.cmd -q "-Dtest=CriticalEndpointRateLimitFilterTest" test` 已通过。
- `npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 已通过。

## 当前功能验收说明
- 10 分钟内 60 轮语音面试流式消息会正常放行。
- 同一用户 10 分钟内第 61 次流式消息仍会被 `interview_stream` 策略拦截并返回 429。
- 语音面试触发 429 时，页面会提示用户稍后继续，不会直接结束语音通话。
- 注册、密保、重置密码、简历导出、Offer 助手和通用面试写操作限流策略保持不变。

## 停止，不继续下一功能
- 本轮只修复面试流式消息限流阈值过低和 429 提示/状态处理问题。
- 不继续扩展为动态限流配置、会员分层限流或全局限流重构。
