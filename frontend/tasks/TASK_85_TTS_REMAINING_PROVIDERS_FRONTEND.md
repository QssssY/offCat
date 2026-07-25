# TTS 剩余厂商适配前端任务记录（2026-06-05）

## 当前任务所属模块

- 模块：设置中心自定义 AI/TTS、管理端 AI 引擎与系统 TTS 配置、语音面试 TTS 播报。
- 范围：Gemini、MiniMax、Qwen、xAI 四个 TTS Provider 的前端预设启用。
- 边界：不新增页面、不新增数据库结构、不改变现有表单交互和 API 封装结构。

## 前端文件定位

- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/__tests__/views/SettingsView.test.js`
- `frontend/app/src/__tests__/views/AdminAiEngineView.test.js`

## 后端文件定位

- `server/src/main/java/com/airesume/server/common/constants/TtsProviderConstants.java`
- `server/src/main/java/com/airesume/server/service/impl/UserTtsConnectivityTestServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/UserTtsSpeechServiceImpl.java`
- 后端任务记录：`tasks/TASK_85_TTS_REMAINING_PROVIDERS_BACKEND.md`

## 本轮修改文件清单

- `SettingsView.vue`：解除 `Gemini/MiniMax/Qwen/xAI` 禁用状态，补齐默认 `baseUrl/model/voice/endpointPath` 和预设音色。
- `AdminAiEngineView.vue`：管理端系统 TTS 使用同一组 Provider 预设，切换后自动回填默认值和候选音色。
- `SettingsView.test.js`：新增用户设置页四个 Provider 预设切换和回填测试。
- `AdminAiEngineView.test.js`：新增管理端系统 TTS 四个 Provider 预设切换和回填测试。

## 前端实现方案

- 保持现有表单、发现、试听、连通测试按钮不变。
- Provider 切换只回填预设，不触发外部网络请求。
- `endpointPath` 和 `ttsProvider` 继续通过现有 API payload 透传给后端。
- 预设值与后端常量保持一致：
  - Gemini：`https://generativelanguage.googleapis.com` / `gemini-2.5-flash-preview-tts` / `Kore` / `/v1beta/models/{model}:generateContent`
  - MiniMax：`https://api.minimax.chat` / `speech-02-turbo` / `male-qn-qingse` / `/v1/t2a_v2`
  - Qwen：`https://dashscope.aliyuncs.com` / `qwen3-tts-flash` / `Cherry` / `/api/v1/services/aigc/multimodal-generation/generation`
  - xAI：`https://api.x.ai` / `grok-tts` / `Fritz-PlayAI` / `/v1/tts`

## 后端实现方案

- 后端按 Provider 协议分发并返回动态音频媒体类型，详见 `tasks/TASK_85_TTS_REMAINING_PROVIDERS_BACKEND.md`。

## 数据存储方案

- 不新增数据库字段或迁移。
- 继续复用既有 TTS 配置字段。

## stage 更新说明

- 已更新 `frontend/tasks/stage.md` 记录本轮前端完成状态、验证命令和停止边界。
- 已同步后端 `tasks/stage.md` 和后端任务文件。

## 编译结果

- 后端编译结果见 `tasks/TASK_85_TTS_REMAINING_PROVIDERS_BACKEND.md`。

## 构建结果

- `npm.cmd run build` 通过。

## 验证结果

- 前端 RED：新增 Provider 预设测试后，旧实现因四个 Provider 仍禁用/未回填默认值失败。
- 前端 GREEN：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，2 个测试文件 / 70 个用例。
- 前端目标回归：`npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/api/admin.ttsConfig.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/AdminAiEngineView.test.js src/__tests__/api/interview.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，6 个测试文件 / 131 个用例。
- 前端构建：`npm.cmd run build` 通过。

## 当前功能验收说明

- 用户设置页和管理端系统 TTS 配置页均可选择 Gemini、MiniMax、Qwen、xAI。
- 切换 Provider 后自动填充默认地址、模型、音色、端点路径，并填充预设候选项。
- 真实厂商 Key 的端到端验收需要人工在设置页或管理端填写后完成。

## 停止，不继续下一个功能

本轮仅完成 Gemini、MiniMax、Qwen、xAI TTS Provider 前端启用和预设填充，等待验收；不继续推进 STT、流式音频、音频存储、计费统计或新语音页面。
