# TTS 剩余厂商适配后端任务记录（2026-06-05）

## 当前任务所属模块

- 模块：用户自定义 TTS、系统级 TTS、语音面试云端 TTS 播报。
- 范围：Gemini、MiniMax、Qwen、xAI 四个前端已列但未启用的 TTS Provider。
- 边界：不新增数据库字段、迁移、STT、流式音频、音频存储、计费统计或独立语音页面。

## 前端文件定位

- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/__tests__/views/SettingsView.test.js`
- `frontend/app/src/__tests__/views/AdminAiEngineView.test.js`
- 前端任务记录：`frontend/tasks/TASK_85_TTS_REMAINING_PROVIDERS_FRONTEND.md`

## 后端文件定位

- `server/src/main/java/com/airesume/server/common/constants/TtsProviderConstants.java`
- `server/src/main/java/com/airesume/server/dto/user/TtsAudioResult.java`
- `server/src/main/java/com/airesume/server/service/impl/UserTtsConnectivityTestServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/UserTtsSpeechServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/TtsDiscoveryServiceImpl.java`
- `server/src/main/java/com/airesume/server/controller/UserAiConfigController.java`
- `server/src/main/java/com/airesume/server/controller/AdminTtsConfigController.java`
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`

## 本轮修改文件清单

- 新增 `TtsAudioResult`，将 TTS 合成结果从单纯 `byte[]` 扩展为 `audioBytes + contentType`。
- 扩展 TTS 服务接口和实现，新增 `previewVoiceAudio`、`previewTtsVoiceAudio`、`synthesizeInterviewSpeechAudio` 等保留媒体类型的方法。
- 扩展 `TtsProviderConstants`，新增 `gemini/minimax/qwen/xai` Provider 和对应 API 协议枚举、默认地址、模型、音色、端点。
- 更新用户试听、管理端试听、语音面试 `/tts` 控制器，按真实 `contentType` 返回音频。
- 更新 `TtsDiscoveryServiceImpl`，对无法实时发现的 Provider 直接返回预设模型/音色。
- 补充单元测试覆盖请求头、请求体、响应解析、空音频拒绝、密钥不泄漏、动态 Content-Type、Qwen 官方 OSS 音频 URL 防护和 xAI 不发送 model 字段。

## 后端实现方案

- Gemini：调用 `generateContent`，解析 `inlineData.data` 的 base64 PCM；当 MIME 为 `audio/L16` 时封装为 WAV 并返回 `audio/wav`。
- MiniMax：调用 `/v1/t2a_v2`，解析响应中的 hex 音频并解码为 MP3 字节。
- Qwen：使用 DashScope Bearer 鉴权调用 TTS 获取音频 URL，只允许官方 `http/https` 的 `*.oss-<region>.aliyuncs.com` 音频地址，校验后下载音频字节，并按下载响应或 URL 后缀返回 `audio/wav`/`audio/mpeg`。
- xAI：调用 `/v1/tts` 直接获取音频字节，请求体只发送 `input/voice`，不发送现有表单中的 `model` 占位字段。
- OpenAI、MiMo 和 EdgeTTS 原链路保持兼容，默认仍按现有 Provider 协议分发。

## 前端实现方案

- 前端只解除 `Gemini/MiniMax/Qwen/xAI` 禁用状态并补齐 Provider 预设。
- 继续复用现有设置页、管理端系统 TTS 配置页、连通测试、模型/音色发现、试听和语音面试播报链路。

## 数据存储方案

- 不新增数据库字段或迁移。
- 继续复用 `tts_provider/base_url/api_key/model/voice_id/endpoint_path`。

## stage 更新说明

- 已更新 `tasks/stage.md` 记录本轮后端完成状态、验证命令和停止边界。
- 已同步前端 `frontend/tasks/stage.md` 和前端任务文件。

## 编译结果

- `mvn.cmd -q -DskipTests compile` 通过。

## 构建结果

- 前端构建结果见 `frontend/tasks/TASK_85_TTS_REMAINING_PROVIDERS_FRONTEND.md`。

## 验证结果

- 后端 RED：新增/调整 Provider 协议、Content-Type 和 SSRF 防护测试后，旧实现缺少结果对象和厂商分发，测试失败。
- 后端 GREEN：`mvn.cmd -q "-Dtest=UserTtsConnectivityTestServiceImplTest,UserTtsSpeechServiceImplTest,TtsDiscoveryServiceImplTest,SysTtsConfigServiceImplTest,UserAiConfigServiceImplTest,UserAiConfigControllerTest,AdminTtsConfigControllerTest,InterviewControllerTest" test` 通过。
- 后端编译：`mvn.cmd -q -DskipTests compile` 通过。

## 当前功能验收说明

- 用户自定义 TTS、系统 TTS、用户试听、管理端试听和语音面试 `/tts` 均可按 Provider 真实音频格式返回。
- 自动化测试使用 mock 上游响应验证协议适配；真实厂商 Key 的端到端验收仍需人工在设置页或管理端填写后完成。

## 停止，不继续下一个功能

本轮仅完成 Gemini、MiniMax、Qwen、xAI TTS Provider 适配，等待验收；不继续推进 STT、流式音频、音频存储、计费统计或新语音页面。
