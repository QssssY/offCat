# 模拟面试流式回复落库后误报失败恢复前端

## 背景

用户在真实模拟面试中反馈：后端已经返回并保存了面试官回复，但前端仍显示“发送失败”；刷新页面后能看到面试官回答。结合最新 `debug.txt`，该现象符合 SSE 最后的 `done` 包在后端落库之后丢失或连接提前关闭的场景。

## 问题原因

当前前端把流式回复是否成功绑定到 SSE `done` 事件。如果服务端已经完成助手消息落库，但浏览器在最后一个 `done` 事件前后发生网络中断、连接关闭或代理缓冲异常，前端会把临时助手消息标记为失败。由于数据已经写入数据库，刷新页面重新拉取会话详情后又能看到真实回复。

## 实现内容

- `InterviewSessionView` 在普通流式异常后增加一次会话详情同步。
- 若服务端最新聊天记录中存在“本次用户回答”之后的助手回复，则以前端同步到的服务端记录为准，替换本地临时消息并取消失败提示。
- 保留鉴权、限流、自定义 AI 可恢复错误的既有处理路径，避免这些业务错误被误判为已成功回复。
- 增加中文注释说明“后端已落库但 SSE done 丢失”的恢复边界。

## 验证

- `npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，37 个用例全绿。
- `npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js src/__tests__/api/interview.test.js src/__tests__/views/InterviewReportView.test.js src/__tests__/router/routeLoaders.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/utils/speechRecognitionCapability.test.js` 通过，7 个测试文件、93 个用例全绿。
- `npm.cmd run build` 通过。

## 边界

- 本轮只修复前端误报失败后的会话恢复，不修改后端 SSE 协议、数据库结构或消息压缩逻辑。
- 如果后端实际没有保存助手回复，前端仍会按原逻辑展示发送失败，避免把真实失败伪装成成功。
