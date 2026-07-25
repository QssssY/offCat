# Web Speech API 软降级与后台恢复开发方案

## 实现完成记录（2026-06-02）

本轮已完成 TASK_69 前端实现：在不恢复离线 STT、不新增云端 STT、不改后端接口和数据库的前提下，将模拟面试语音识别链路改为“语音优先、文本不断线、后台自动恢复”。

核心结果：

- 新增浏览器语音识别能力检测工具，统一识别 Web Speech API、麦克风权限、本地语言包可用/可安装状态。
- `useSpeechToText` 增加启动健康检查，`onstart` 超时、`service-not-allowed`、空 `onend` 等会归为语音服务暂不可用。
- `useVoiceCall` 增加正式文本兜底模式，语音服务暂不可用时不挂断面试，后台按 15s、30s、60s 退避探测恢复。
- `InterviewSessionView` 在语音暂不可用时展示文本回答区、系统听写提示、“重新启用语音”按钮和浏览器本地语音包安装入口。
- 语音暂不可用时，已有 STT 临时识别文本会带入文本输入草稿；语音恢复后不清空未提交文本。

## 稳定性补强记录（2026-06-02）

本轮针对验收发现的“语音恢复成功判定过早”和“浏览器完全不支持语音能力时缺少文本回答入口”做最小补强，仍然只修改前端，不改后端、数据库，不新增 STT/TTS 服务。

补强结果：

- `useSpeechToText.start({ waitForHealthyStart: true })` 会等浏览器触发 `onstart`，并在 1000ms 观察窗口内确认没有 `onerror` / 空 `onend` 后才返回成功。
- `useSpeechToText` 增加 `onstart` 后 6000ms 首个有效事件 watchdog；若没有 `onaudiostart`、`onsoundstart`、`onspeechstart`、`onresult`、`onend`、`onerror`，按 `start-timeout` 进入语音暂不可用。若音量监测已检测到真实麦克风输入，则交给 `no-transcript` 分支处理，避免错误码竞态。
- `useVoiceCall.retrySpeechNow()` 改为调用健康探测模式，只有探测结果明确成功才退出文本兜底；即使 `isRecording` 已被置为 `true`，探测失败也继续留在文本模式并保持后台退避重试。
- `InterviewSessionView` 在 STT 或 TTS 完全不支持时也展示正式文本回答区和系统听写提示；不支持场景不展示“重新启用语音”按钮，避免误导用户认为可以恢复。

## 当前任务所属模块

前端模拟面试语音通话、浏览器 Web Speech API 语音识别、语音不可用兜底输入、面试会话不中断体验。

## 问题背景

当前离线 sherpa-onnx 语音识别链路已移除，前端语音识别只依赖浏览器 Web Speech API。Chrome/Edge 在用户网络正常时仍可能偶发返回“当前浏览器语音识别服务不可用”，导致页面降级为手动输入。这个失败点不一定来自用户网络，也可能来自浏览器识别服务、浏览器 Profile、语言包状态、麦克风权限、系统策略或服务端限流。

在 Cloudflare Pages 免费静态部署、单文件 25MiB 限制、不恢复大体积离线模型、不新增云厂商 STT 的约束下，不能承诺让 Web Speech API 本身 100% 稳定。本方案目标是把语音识别链路改成“语音优先、文本不断线、后台自动恢复”，避免面试流程因为浏览器语音服务短暂不可用而中断。

## 前端文件定位

- `frontend/app/src/composables/useSpeechToText.js`
- `frontend/app/src/composables/useVoiceCall.js`
- `frontend/app/src/composables/useTextToSpeech.js`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/__tests__/composables/useSpeechToText.test.js`
- `frontend/app/src/__tests__/composables/useVoiceCall.test.js`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`

如实现时需要拆分能力检测，可新增：

- `frontend/app/src/utils/speechRecognitionCapability.js`
- `frontend/app/src/__tests__/utils/speechRecognitionCapability.test.js`

## 后端文件定位

本方案默认不涉及后端接口、数据库或服务层改动。

不新增云端 STT/TTS，不新增后端语音识别服务，不恢复 sherpa-onnx、Whisper、Vosk、sherpa-onnx WASM 等大模型离线引擎。

## 本轮修改文件清单

- `frontend/app/src/utils/speechRecognitionCapability.js`
- `frontend/app/src/composables/useSpeechToText.js`
- `frontend/app/src/composables/useVoiceCall.js`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/__tests__/utils/speechRecognitionCapability.test.js`
- `frontend/app/src/__tests__/composables/useSpeechToText.test.js`
- `frontend/app/src/__tests__/composables/useVoiceCall.test.js`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`
- `frontend/tasks/TASK_69_WEB_SPEECH_SOFT_FALLBACK_FRONTEND.md`
- `frontend/tasks/stage.md`

## 约束与边界

- 保持 Cloudflare Pages 免费静态部署兼容，不引入超过 Pages 单文件限制的大模型资源。
- 不恢复之前已移除的 sherpa-onnx 离线 STT 链路。
- 不接入阿里、腾讯、百度、讯飞、OpenAI、Deepgram 等云厂商 STT。
- 不新增后端音频上传识别服务。
- Web Speech API 仍作为浏览器内首选语音识别能力，但不可用时必须保证面试可继续。
- 文本输入兜底不是失败终点，而是语音暂不可用期间的连续会话模式。

## 前端实现方案

### 1. 新增语音能力检测层

在 `useSpeechToText` 前增加能力检测逻辑，优先复用现有 composable 结构，避免大范围重构。

能力检测内容：

- 检查 `window.SpeechRecognition` / `window.webkitSpeechRecognition` 是否存在。
- 检查实验能力 `SpeechRecognition.available()`、`SpeechRecognition.install()`、`recognition.processLocally` 是否可用。
- 若浏览器支持本地语言包能力，优先检测 `zh-CN` 的本地 dictation 是否可用或可安装。
- 检测麦克风权限状态，区分用户拒绝、浏览器不支持、服务暂不可用。

建议状态枚举：

- `local-ready`：浏览器本地语音识别可用。
- `local-downloadable`：浏览器支持安装本地语言包。
- `webspeech-ready`：普通 Web Speech API 可用。
- `temporarily-unavailable`：浏览器语音服务暂不可用，可后台重试。
- `permission-blocked`：麦克风权限被拒绝，需要用户手动授权。
- `unsupported`：当前浏览器不支持 Web Speech API。

### 2. 优先尝试浏览器本地语言包

如果浏览器支持 on-device speech recognition：

- 初始化识别实例时设置 `recognition.lang = 'zh-CN'`。
- 当 `processLocally` 属性存在时，优先设置 `recognition.processLocally = true`。
- 当本地语言包可安装时，在面试页展示轻量入口：“可安装浏览器本地语音包以提升稳定性”。
- 用户点击后调用浏览器提供的 `install()` 能力，安装成功后下次识别优先走本地。

注意：该能力属于浏览器实验能力，必须做特性检测；不支持时隐藏入口，不影响现有 Web Speech API 主流程。

### 3. Web Speech 启动健康检查

`useSpeechToText.start()` 不再只判断 API 是否存在，而是执行一次实际健康检查：

- 调用 `recognition.start()` 后等待 `onstart`。
- 在短窗口内观察 `onspeechstart`、`onresult`、`onerror`、`onend`。
- 如果出现 `network`、`service-not-allowed`、`audio-capture`、`not-allowed` 或无有效事件直接 `onend`，记录为健康检查失败。
- 健康检查失败时，不把面试通话置为结束，只通知 `useVoiceCall` 进入语音暂不可用模式。

健康检查建议超时：

- `onstart` 等待：1500ms 到 2500ms。
- 首次有效事件等待：5000ms 到 8000ms。
- 连续失败阈值：2 到 3 次。

### 4. 语音不可用改为软降级

`useVoiceCall` 需要把“浏览器语音识别服务不可用”从硬失败改为软降级状态。

软降级行为：

- 不退出面试通话。
- 不清空当前 AI 问题、回答草稿和会话上下文。
- 不停止后续 AI 播报。
- 用户回答区切换到文本输入。
- 页面提示“当前浏览器语音服务暂不可用，可继续输入回答，系统会自动尝试恢复语音。”
- 保留“重新启用语音”按钮。

后台恢复行为：

- 进入文本模式后，后台按退避策略尝试重新启动 Web Speech API。
- 建议退避间隔：15s、30s、60s，之后维持 60s。
- 用户手动点击“重新启用语音”时立即触发一次探测，但仍要遵守 recognition 实例清理逻辑。
- 一旦健康检查通过，状态切回语音收音；文本输入区可以保留但不抢焦点。

### 5. 文本回答区成为正式兜底路径

`InterviewSessionView` 中的文本输入不能只是错误提示后的临时入口，而要成为语音暂不可用期间的正式回答路径。

交互要求：

- 用户可以直接输入回答并提交。
- 提交后继续走现有面试回答处理链路。
- 文本模式下仍显示当前 AI 问题、AI 思考中、AI 播报中等状态。
- 语音恢复后，不丢弃文本输入框中尚未提交的内容。
- 如果已有 STT 临时识别文本，应保留并带入文本输入草稿，避免用户重复回答。

### 6. 系统听写兜底提示

在文本输入区附近增加轻量提示，但不新增页面模块：

- Windows：可按 `Win + H` 使用系统听写。
- macOS：可使用系统听写快捷键。
- 移动端：可使用系统键盘麦克风输入。

提示应只在语音暂不可用或手动输入模式下展示，避免正常语音面试时干扰用户。

### 7. TTS 链路保持现有稳定策略

本方案主目标是 STT 不可用时不中断面试。现有 `useTextToSpeech` 对开播超时、默认 voice 重试和播报结束后延迟开麦的逻辑应继续保留。

实现时只需要确认：

- AI 播报无声重试不会阻塞文本模式提交。
- TTS 结束后的延迟开麦只在语音模式可用时触发。
- 语音暂不可用时，TTS 结束后不应反复抢开 STT。

## 后端实现方案

无后端实现。

本方案不上传音频、不新增识别接口、不新增服务器资源消耗。后续如果用户明确接受 Cloudflare Workers AI 或自建 STT，再另行创建后端/Worker 任务文档。

## 数据存储方案

不新增数据库结构。

可选前端本地偏好：

- 记录用户是否关闭系统听写提示。
- 记录本地语言包安装入口是否已展示过。

如实现该偏好，应优先复用现有设置偏好存储工具，不新增新的持久化体系。

## 测试计划

### `useSpeechToText.test.js`

- 浏览器支持 `processLocally` 时优先设置本地识别。
- `available()` 返回本地语言包可用时状态为 `local-ready`。
- `available()` 返回可安装时状态为 `local-downloadable`。
- `start()` 后短时间无 `onstart` 时返回健康检查失败。
- `start({ waitForHealthyStart: true })` 等待 `onstart` 后的 1000ms 观察窗口，确认恢复探测结果。
- `onstart` 后 6000ms 内无首个有效识别事件时进入 `start-timeout`。
- `network` / `service-not-allowed` / 空 `onend` 不污染下一次启动。
- 重启识别前会清理旧 recognition 实例和旧音量监测资源。

### `useVoiceCall.test.js`

- STT 暂不可用时不退出通话。
- STT 暂不可用时切换到文本回答模式。
- 后台退避重试成功后自动回到语音收音。
- 后台/手动恢复只信任 `start({ waitForHealthyStart: true })` 的健康探测结果，不再只看 `isRecording`。
- 连续失败时维持文本模式，不无限快速重启。
- 已有临时识别文本会保留到文本输入草稿。
- AI 播报中不会触发后台恢复开麦抢状态。

### `InterviewSessionView.test.js`

- 语音面试中浏览器语音服务不可用时，页面仍展示当前面试内容。
- 文本输入框可提交回答并继续进入下一轮 AI 回复。
- 文本模式展示系统听写提示。
- 点击“重新启用语音”会触发一次语音恢复探测。
- STT 或 TTS 完全不支持时，语音面试仍展示文本输入入口并可提交回答。
- 语音恢复后提示消失或切换为正常收音状态。

### 验证命令

```bash
npm.cmd test -- --run src/__tests__/utils/speechRecognitionCapability.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js
npm.cmd run build
```

## stage 更新说明

`frontend/tasks/stage.md` 顶部已记录本轮实现完成状态、修改范围、验证结果和停止边界。

## 编译结果

前端定向测试通过：

```bash
npm.cmd test -- --run src/__tests__/utils/speechRecognitionCapability.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js
```

结果：4 个测试文件、84 个用例通过。

## 构建结果

前端构建通过：

```bash
npm.cmd run build
```

并已检查：

```bash
Get-ChildItem -Recurse -File dist | Where-Object { $_.Length -gt 25MB } | Select-Object FullName,Length
```

结果：无输出，`dist` 中未发现超过 Cloudflare Pages 25MiB 单文件限制的大资源。

## 当前功能验收说明

本轮实现完成后的验收标准：

- Web Speech API 可用时仍优先语音收音。
- 浏览器语音识别服务暂不可用时，面试通话不中断。
- 用户可以立即通过文本输入继续回答。
- 系统会后台退避探测语音恢复。
- 语音恢复成功后可回到语音收音。
- 支持本地语言包的浏览器优先走本地识别能力。
- 不恢复离线大模型，不引入云厂商 STT，不突破 Cloudflare Pages 静态资源限制。

## 停止，不继续下一个功能

本轮只完成 Web Speech API 软降级与后台恢复前端实现，不新增云端识别、不恢复离线引擎、不扩展其它语音方案，不继续推进后端/Worker/STT 服务。
