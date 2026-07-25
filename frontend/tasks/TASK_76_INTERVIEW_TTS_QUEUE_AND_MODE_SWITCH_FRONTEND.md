# 追加修复：语音降级后禁止自动恢复语音模式（2026-06-03）

## 当前任务所属模块

前端模拟面试语音 / 文本模式状态机、语音识别不可用降级提示。

## 本轮修改文件清单

- `frontend/app/src/composables/useVoiceCall.js`
- `frontend/app/src/composables/useSpeechToText.js`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/__tests__/composables/useVoiceCall.test.js`
- `frontend/app/src/__tests__/composables/useSpeechToText.test.js`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`
- `frontend/tasks/TASK_76_INTERVIEW_TTS_QUEUE_AND_MODE_SWITCH_FRONTEND.md`
- `frontend/tasks/stage.md`

## 问题原因

语音识别服务不可用后，`useVoiceCall` 会进入文本降级模式，但旧逻辑仍保留后台定时恢复探测，并且在 AI 回复结束、TTS 播放结束的 watcher 中会立即触发一次语音恢复探测。只要探测返回成功，页面就会在用户没有主动点击的情况下退出文本降级，表现为输入文本回答后，面试官回复结束又自动切回语音模式。

## 前端实现方案

- 删除文本降级后的后台定时恢复入口；进入 `isTextFallbackMode` 后不再定时调用 `speech.start()`。
- `retrySpeechNow()` 保留为用户点击“重试语音”时的唯一恢复入口，探测失败只保持文本降级，不再继续排后台重试。
- AI 回复结束和 TTS 播放结束时，如果仍处于文本降级，只保持当前文本输入模式，不能主动恢复语音。
- 降级提示文案从“系统会自动尝试恢复语音”改为“需要恢复语音时请手动点击重试语音”，降级横幅仅保留“重试语音”按钮，不再额外显示确认提示句。

## 后端实现方案

无后端改动。

## 数据存储方案

不新增数据库结构，不新增本地持久化字段；本轮只调整当前页面运行时状态机。

## 验证结果

- RED 阶段：`npm.cmd test -- --run src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 失败，复现文本降级后后台重试调用 `speech.start()`，以及 AI 回复结束后自动退出文本降级。
- GREEN 阶段：`npm.cmd test -- --run src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，2 个测试文件 / 74 个用例。
- 语音回归：`npm.cmd test -- --run src/__tests__/utils/speechRecognitionCapability.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/SettingsView.test.js` 通过，6 个测试文件 / 166 个用例。
- 构建验证：`npm.cmd run build` 通过。

## 当前功能验收说明

当 Chrome / Edge 的语音识别服务不可用时，系统仍会自动切换到文本输入，保证面试继续进行；切到文本后不会因为后台探测、AI 回复完成或 TTS 播放完成而自动回到语音模式。用户需要恢复语音时，必须自己点击“重试语音”或“语音模式”，并且仍受播报中模式切换锁限制。

## 停止，不继续下一个功能

本轮只修复语音识别不可用后的自动回切问题，不新增云端 STT/TTS、不修改后端语音服务、不扩展其它面试能力。

# 模拟面试 TTS 队列恢复、流式播报启动兜底与播报中模式切换锁前端修复

## 当前任务所属模块

前端模拟面试语音播报、设置中心语音试听、语音/文本模式切换交互。

## 前端文件定位

- `frontend/app/src/composables/useTextToSpeech.js`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/__tests__/composables/useTextToSpeech.test.js`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`

## 后端文件定位

本轮只调整浏览器端 Web Speech TTS 队列和前端模式切换锁，不涉及后端接口、数据库、AI 调用或流式协议。

## 本轮修改文件清单

- `frontend/app/src/composables/useTextToSpeech.js`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/__tests__/composables/useTextToSpeech.test.js`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`
- `frontend/tasks/TASK_76_INTERVIEW_TTS_QUEUE_AND_MODE_SWITCH_FRONTEND.md`
- `frontend/tasks/stage.md`

## 前端实现方案

- `useTextToSpeech` 在每个新播报 run 的第一句进入浏览器 `speechSynthesis.speak()` 前先执行一次 `speechSynthesis.cancel()`，清理 Chrome/Edge 可能残留的页面级合成队列。
- `useTextToSpeech.speakStreaming()` 与 `flushRemaining()` 支持透传播报参数，让流式追问和 done 后补播的尾句也能启用 `requireStartEvent` 启动检测。
- `InterviewSessionView` 为后续流式追问新增 `INTERVIEW_STREAM_SPEECH_OPTIONS`，与开场白保持一致：允许默认 voice 兜底，并要求收到 `onstart`；Chrome 接受 `speak()` 但没有真正开始发声时，会在启动 watchdog 后用浏览器默认 voice 重试。
- `speak()` 改为只重置当前 composable 的运行时状态，真正的浏览器队列重置统一由首句入队逻辑处理，保证模拟面试播报和设置页试听都走同一恢复路径。
- `InterviewSessionView` 新增 `modeSwitchLocked`，当 AI 正在回复或 TTS 正在播报时，禁用“切换文本模式”“语音模式”“重试语音”“开始通话”等会改变语音/文本状态的入口。
- `switchToTextMode()`、`switchToVoiceMode()`、`handleRetryVoiceRecognition()`、`handleStartVoiceCall()` 增加函数级防御，避免通过非按钮路径绕过锁并打断播报。
- `sendMessage()` 在普通 SSE 成功完成后立即释放 `replyLocked` 和 `sending`，避免语音通话状态机一直把 AI 误判为“仍在回复”，影响后续播报、收音恢复和按钮禁用状态。
- `InterviewSessionView` 新增流式朗读增量提取逻辑：先基于完整 `rawContent` 剔除非朗读块，再计算本次新增的可朗读文本，保证后续普通追问仍按增量朗读。
- 播报中同步禁用语音 overlay、折叠面板和文本输入区的语音识别按钮；`handleMicControl()`、`handleToggleMute()` 增加函数级防御，避免播报过程中开启/切换语音识别。

## 后端实现方案

无后端改动。

## 数据存储方案

不新增数据库结构，不新增本地持久化字段。TTS 队列清理和模式切换锁只存在于当前页面运行时。

## stage 更新说明

`frontend/tasks/stage.md` 顶部已新增本轮修复记录，说明问题原因、实现范围、验证结果和停止边界。

## 编译结果

- RED 阶段 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js` 失败，新增 2 个用例复现 Chrome `speaking/pending` 残留时后续流式回复只调用一次 `speak()`、没有默认 voice 重试。
- GREEN 阶段 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，2 个测试文件 / 79 个用例。
- 追加流式启动兜底后 `npm.cmd test -- --run src/__tests__/utils/speechRecognitionCapability.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/SettingsView.test.js` 通过，6 个测试文件 / 160 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

Chrome/Edge 偶发 TTS 无声或队列卡住后，下一次模拟面试播报或设置中心试听会先清理浏览器合成队列再发起新播报，降低后续全部无声的概率。普通语音面试第二轮及后续助手回复会继续触发流式播报；即使 Chrome 短时间残留 `speaking/pending` 状态，后续追问也会强制等待 `onstart`，未启动时自动使用浏览器默认 voice 重试。SSE 成功结束后会释放回复锁，但只要 TTS 仍在播报，语音识别按钮仍保持禁用。面试官播报期间，“语音模式/重试语音/切换文本模式/语音识别按钮”均不可操作，必须等播报结束后才能切换或开麦。

## 停止，不继续下一个功能

本轮只修复 TTS 队列恢复和播报中禁止模式切换，不新增云端 TTS/STT，不恢复离线 TTS，不修改后端语音服务或其它面试功能。
# 追加修复：语音降级后禁止自动恢复语音模式（2026-06-03）

## 当前任务所属模块

前端模拟面试语音 / 文本模式状态机、语音识别不可用降级提示。

## 本轮修改文件清单

- `frontend/app/src/composables/useVoiceCall.js`
- `frontend/app/src/composables/useSpeechToText.js`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/__tests__/composables/useVoiceCall.test.js`
- `frontend/app/src/__tests__/composables/useSpeechToText.test.js`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`
- `frontend/tasks/TASK_76_INTERVIEW_TTS_QUEUE_AND_MODE_SWITCH_FRONTEND.md`
- `frontend/tasks/stage.md`

## 问题原因

语音识别服务不可用后，`useVoiceCall` 会进入文本降级模式，但旧逻辑仍保留后台定时恢复探测，并且在 AI 回复结束、TTS 播放结束的 watcher 中会立即触发一次语音恢复探测。只要探测返回成功，页面就会在用户没有主动点击的情况下退出文本降级，表现为输入文本回答后，面试官回复结束又自动切回语音模式。

## 前端实现方案

- 删除文本降级后的后台定时恢复入口；进入 `isTextFallbackMode` 后不再定时调用 `speech.start()`。
- `retrySpeechNow()` 保留为用户点击“重试语音”时的唯一恢复入口，探测失败只保持文本降级，不再继续排后台重试。
- AI 回复结束和 TTS 播放结束时，如果仍处于文本降级，只保持当前文本输入模式，不能主动恢复语音。
- 降级提示文案从“系统会自动尝试恢复语音”改为“需要恢复语音时请手动点击重试语音”，降级横幅仅保留“重试语音”按钮，不再额外显示确认提示句。

## 后端实现方案

无后端改动。

## 数据存储方案

不新增数据库结构，不新增本地持久化字段；本轮只调整当前页面运行时状态机。

## 验证结果

- RED 阶段：`npm.cmd test -- --run src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 失败，复现文本降级后后台重试调用 `speech.start()`，以及 AI 回复结束后自动退出文本降级。
- GREEN 阶段：`npm.cmd test -- --run src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，2 个测试文件 / 74 个用例。
- 语音回归：`npm.cmd test -- --run src/__tests__/utils/speechRecognitionCapability.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/SettingsView.test.js` 通过，6 个测试文件 / 166 个用例。
- 构建验证：`npm.cmd run build` 通过。

## 当前功能验收说明

当 Chrome / Edge 的语音识别服务不可用时，系统仍会自动切换到文本输入，保证面试继续进行；切到文本后不会因为后台探测、AI 回复完成或 TTS 播放完成而自动回到语音模式。用户需要恢复语音时，必须自己点击“重试语音”或“语音模式”，并且仍受播报中模式切换锁限制。

## 停止，不继续下一个功能

本轮只修复语音识别不可用后的自动回切问题，不新增云端 STT/TTS、不修改后端语音服务、不扩展其它面试能力。
