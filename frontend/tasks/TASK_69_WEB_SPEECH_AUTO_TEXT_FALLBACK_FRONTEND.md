# Web Speech 语音不可用自动切换文本回答前端修复

## 当前任务所属模块

模拟面试语音通话、Web Speech 运行时降级、文本回答兜底入口。

## 前端文件定位

- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`

## 后端文件定位

本轮只调整前端语音面试交互，不涉及后端接口、数据库或 AI 调用链路。

## 本轮修改文件清单

- `InterviewSessionView.vue`：语音识别不可用或浏览器不支持语音能力时，自动关闭语音通话 overlay，显示原有底部文本输入框。
- `InterviewSessionView.vue`：在输入框上方展示降级提示，语音能力仍可检测时提供“重试语音”入口，用户可主动切回语音面试。
- `InterviewSessionView.test.js`：补充语音服务不可用、语音识别不支持、语音播报不支持和重试语音入口的回归断言。

## 前端实现方案

- 复用现有文本输入区，不再在语音通话窗口内插入大面积文本兜底卡片，避免继续挤压通话布局。
- `showVoiceOverlay` 在 `voiceCall.isTextFallbackMode` 或语音能力不可用时关闭，避免用户停留在只能重试的通话窗口。
- `showTextInput` 在语音会话降级时直接打开，保留已有输入、发送和字数限制能力。
- 降级横幅使用短文案和轻量按钮，保留“重试语音”入口；点击后先回到语音窗口并调用语音恢复探测，恢复失败会回到文本输入区。

## 后端实现方案

无后端改动。

## 数据存储方案

无数据存储改动。

## stage 更新说明

`frontend/tasks/stage.md` 顶部已记录本轮 Web Speech 语音不可用自动切换文本回答修复范围、验证结果和停止说明。

## 编译结果

- `npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/utils/speechRecognitionCapability.test.js` 通过，4 个测试文件 / 82 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

当浏览器语音识别服务不可用、语音识别不支持或语音播报不支持时，语音面试页会自动切换到底部文本输入区；输入框上方显示降级提示。语音识别运行时降级时，用户可点击“重试语音”主动切回语音面试。

