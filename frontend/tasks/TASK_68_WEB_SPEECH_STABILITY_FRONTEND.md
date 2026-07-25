# 稳定 Web Speech API 语音链路

## 当前任务所属模块

前端模拟面试语音通话、浏览器语音识别 STT、浏览器语音合成 TTS。

## 前端文件定位

- `frontend/app/src/composables/useSpeechToText.js`
- `frontend/app/src/composables/useVoiceCall.js`
- `frontend/app/src/composables/useTextToSpeech.js`
- `frontend/app/src/views/interview/InterviewSessionView.vue`

## 后端文件定位

本轮不涉及后端接口、数据库或服务层改动。

## 本轮修改文件清单

- `frontend/app/src/composables/useSpeechToText.js`
- `frontend/app/src/composables/useVoiceCall.js`
- `frontend/app/src/composables/useTextToSpeech.js`
- `frontend/app/src/__tests__/composables/useSpeechToText.test.js`
- `frontend/app/src/__tests__/composables/useVoiceCall.test.js`
- `frontend/app/src/__tests__/composables/useTextToSpeech.test.js`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`
- `frontend/tasks/TASK_68_WEB_SPEECH_STABILITY_FRONTEND.md`
- `frontend/tasks/stage.md`

## 前端实现方案

- `useSpeechToText` 每次启动 Web Speech API 前先清理旧 `SpeechRecognition` 实例和麦克风音量监测资源，避免 Chrome/Edge 残留内部状态导致下一次启动后无识别文本。
- `useVoiceCall` 将 `network`、`no-speech`、`no-transcript`、`end-without-result` 作为可恢复 STT 中断处理，短延迟自动重启收音；连续恢复失败达到上限后才进入“等待继续收音”。
- STT 自动恢复期间保留已经识别到的 `pendingMessage`，并在手动发送、静音、AI 播报时清理恢复定时器，避免恢复开麦和当前用户操作互相抢状态。
- `useTextToSpeech` 对 `speak()` 已接受但迟迟不触发 `onstart` 的场景增加一次内部重试：先 `cancel()` 清空浏览器合成队列，再用浏览器默认 voice 重播同一句；重试仍失败时释放播报状态，避免面试卡住。
- 已触发 `onstart` 的长句仍沿用原保守 watchdog，不因为普通长回复播报慢而提前截断。

## 后端实现方案

无后端实现。本轮不新增云端 STT/TTS、不恢复离线引擎、不新增后端语音识别或语音合成服务。

## 数据存储方案

不新增数据库结构，不新增本地持久化字段。语音恢复次数和 TTS 重试状态只存在于当前页面运行时内存中。

## stage 更新说明

`frontend/tasks/stage.md` 顶部已新增“稳定 Web Speech API 语音链路”记录，说明本轮范围、实现、验证和停止边界。

## 编译结果

- `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，4 个测试文件 / 91 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

在 Chrome/Edge 语音面试中，如果浏览器语音识别偶发返回 `no-speech`、`no-transcript`、短暂 `network` 或无结果结束，页面会保留当前通话并自动尝试重启收音；连续失败后才提示用户手动继续收音。若浏览器 TTS 接受播报请求但未真正开播，会自动切到浏览器默认 voice 重试一次，失败时释放状态而不锁死通话。

## 停止，不继续下一个功能

本轮只稳定现有 Web Speech API 链路，不恢复 sherpa-onnx 离线 STT，不新增 Deepgram、云端 STT、后端语音识别、离线 TTS 或其它语音服务替代方案。
