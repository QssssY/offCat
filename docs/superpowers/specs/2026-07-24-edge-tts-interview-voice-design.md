# EdgeTTS 默认晓晓与面试音色生效设计

## 背景

设置页已经提供 EdgeTTS 云端音色选择，默认选项也已有 `zh-CN-XiaoxiaoNeural`，但面试页的云端合成请求只提交文本，后端始终使用数据库中原先保存的 `ttsVoiceId`。因此用户在“AI 播报声音”中重新选择音色后，面试中的播报不会同步变化。

## 目标与边界

- 将 EdgeTTS `zh-CN-XiaoxiaoNeural`（晓晓）作为 AI 播报默认音色。
- 用户选择 EdgeTTS 音色后，当前面试的每次云端合成都使用该音色。
- 没有可用云端 TTS、请求失败或浏览器不支持云端配置时，保持现有浏览器 TTS 回退行为。
- 不修改数据库结构，不改变 AI 配置优先级，不影响非 EdgeTTS Provider。

## 方案比较

### 方案 A：仅在前端选择后重新保存用户 TTS 配置

让音色选择动作调用现有用户 AI 配置保存接口，面试继续读取后端配置。优点是接口改动少；缺点是每次选择都需要完整的 AI/TTS 配置和连通性校验，用户未点击保存或 API Key 已脱敏时仍会失败，无法解决“当前面试立即生效”的问题。

### 方案 B：面试合成请求携带当前音色（采用）

前端从本地偏好解析当前 EdgeTTS voice ID，在每次 `/api/interview/session/{sessionId}/tts` 请求中携带可选 `voiceId`。后端仅在已解析配置为 EdgeTTS 时校验该 ID 是否属于项目允许的 EdgeTTS 音色白名单，再以不可变副本覆盖本次合成的 `voiceId`；其他 Provider 忽略覆盖值并继续使用已保存配置。这样不需要重新保存密钥，选择立即对当前面试生效，且不改变配置持久化链路。

### 方案 C：把本地声音偏好持久化到面试会话

创建会话时提交声音设置并保存到 `interview_session`，之后后端按会话读取。该方案需要数据库字段、迁移和会话生命周期处理，变更面大，超出本轮最小修复范围。

## 选定设计

### 前端

- `DEFAULT_SETTINGS_PREFERENCES.voicePreferredType` 改为 EdgeTTS 默认值 `edge_cloud`。
- `synthesizeInterviewTts` 增加可选 `voiceId`，请求体始终保留 `text`，仅在有合法 EdgeTTS voice ID 时附加 `voiceId`。
- `useCloudTextToSpeech` 保存并透传 `voiceId`，不把音色值写入日志。
- `InterviewSessionView` 从当前设置解析 `getEdgeCloudTtsVoiceId(settingsPreferences.voicePreferredType)`；云端 EdgeTTS 时传入该 ID，浏览器 TTS 回退路径不改变。
- 现有“选择音色后保存本地偏好”的行为保留；不强制触发完整 AI 配置保存。

### 后端

- `TtsSpeechRequest` 增加可选、长度受限的 `voiceId` 字段。
- `InterviewController` 将请求音色传入 `UserTtsSpeechService`。
- `UserTtsSpeechService` 增加带可选 voice override 的合成入口，旧入口保留兼容调用。
- `UserTtsSpeechServiceImpl` 仅对 EdgeTTS 校验并应用 voice override；校验失败快速返回业务错误，不向上游发起请求。EdgeTTS 白名单复用现有 `TtsProviderConstants.EDGE_PRESET`，默认仍为 `zh-CN-XiaoxiaoNeural`。
- 非 EdgeTTS 配置、系统 TTS 和未传 voiceId 的请求保持原有解析和合成行为。

### 错误与回退

- 非法或不支持的 EdgeTTS voice ID 返回明确的 TTS 配置错误，不泄漏 API Key 或上游响应。
- 云端合成网络/上游失败时，前端沿用现有一次性回退到浏览器 TTS 的逻辑。
- 浏览器设置为 EdgeTTS 但当前没有可用云端配置时，继续使用现有浏览器中文自然音色回退，不阻断面试。

### 测试

- 前端 API 测试：验证可选 `voiceId` 被放入请求体，未传时仍只发送 `text`。
- 前端面试视图测试：验证面试云端合成传入当前 EdgeTTS voice ID，并验证默认偏好为 `edge_cloud`。
- 后端服务测试：验证合法 EdgeTTS voice override 使用指定 voice，非法 voice 被拒绝，非 EdgeTTS 忽略 override；现有 Provider 合成回归测试继续通过。
- 后端编译、目标测试和前端目标测试/构建全部通过后再部署。

## 不在本轮范围

- 不新增数据库字段或迁移。
- 不重构现有 TTS Provider 协议。
- 不改变管理员系统 TTS 配置保存页面的其他字段和权限。

