# 浏览器语音识别失败时的云端 STT 兜底（前端）

## 当前任务所属模块

前端模拟面试语音输入、浏览器语音识别 STT、设置中心语音偏好。

## 问题背景

语音面试的语音输入完全依赖浏览器 Web Speech API（Chrome/Edge 把麦克风音频上传 Google 服务器识别），
链路不稳定：偶发 `network` / `no-speech` / `no-transcript` / `service-not-allowed` / 启动后无结果。
TASK_67/68/69 的重启与看门狗只是围绕同一 API 打补丁，空间已基本用尽。

TTS 侧早已通过“浏览器录音之外走后端云端合成”解决同类问题；本轮为 STT 做镜像：
浏览器识别失败时，改为浏览器录音 → 后端 `/stt` → 云端 ASR → 返回文字。

## 前端文件定位

- `frontend/app/src/composables/useCloudSpeechToText.js`（新增）
- `frontend/app/src/composables/useResilientSpeechToText.js`（新增，协调器）
- `frontend/app/src/composables/useSpeechToText.js`（未改，浏览器主链路）
- `frontend/app/src/composables/useVoiceCall.js`（未改）
- `frontend/app/src/api/interview.js`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/utils/settingsPreferences.js`

## 后端文件定位

后端新增 `GET /api/interview/session/{id}/stt-capability` 与 `POST /api/interview/session/{id}/stt`。
识别服务配置由**管理端**维护（`sys_stt_config` 单例表 + `/api/admin/stt-config`），
镜像现有系统级 TTS 配置：地址、模型、API Key（加密存储、脱敏返回）、端点路径、启用开关，
默认接硅基流动 `FunAudioLLM/SenseVoiceSmall`。不再使用环境变量，运维在管理端一站式配置。

## 本轮修改文件清单

- 新增 `useCloudSpeechToText.js`：MediaRecorder 录音 + 音量 VAD 分段 + 逐段上传识别。
- 新增 `useResilientSpeechToText.js`：包住浏览器与云端两个引擎，对外暴露与 `useSpeechToText` 一致的接口。
- `api/interview.js`：新增 `getInterviewSttCapability`、`transcribeInterviewSpeech`。
- `InterviewSessionView.vue`：语音通话侧改用协调器；按“用户设置 + 后端能力”门控云端兜底。
- `SettingsView.vue`：语音识别区新增“浏览器识别失败时启用云端语音识别”开关。
- `settingsPreferences.js`：`voiceRecognitionEngine` 允许值扩展 `cloud_fallback`。
- 新增/更新单测：`useResilientSpeechToText.test.js`、`useCloudSpeechToText.test.js`、`InterviewSessionView.test.js`。

## 前端实现方案

- 完全隔离：面试页有两个独立 STT 实例——文本框听写仍用 `useSpeechToText`，只有语音通话实例换成协调器；
  其它功能一行不改。
- 协调器优先浏览器识别（能用时零成本零延迟）；浏览器出现
  `network`/`service-not-allowed`/`start-timeout`/`end-without-result`/`no-transcript`
  且云端可用时，静默切到云端识别，不把错误抛给通话层；否则错误透传，`useVoiceCall` 按原规则降级文本。
- 云端引擎用 MediaRecorder 录音，音量 VAD 检测停顿自动切段（静音 900ms 或单段满 15s），逐段上传，
  识别文本以“追加”语义写入 `finalTranscript`，与浏览器识别语义一致。
- 门控：只有“用户在设置里开启 cloud_fallback” + “后端 `stt-capability` 返回可用”同时成立才启用云端；
  任一不满足都保持纯浏览器识别现状。
- 复用 `voiceRecognitionEngine` 承载开关（`cloud_fallback` 开 / `system_local` 关），不新增偏好字段。

## 后端实现方案

见后端任务文件。前端只依赖 `stt-capability` 与 `stt` 两个接口。

## 数据存储方案

不新增数据库结构。云端开关只落本机 `voiceRecognitionEngine` 偏好；录音数据不落盘，逐段上传后即释放。

## 编译结果

- `npx vitest run src/__tests__/composables/useResilientSpeechToText.test.js src/__tests__/composables/useCloudSpeechToText.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useCloudTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/SettingsView.test.js src/__tests__/utils/settingsPreferences.test.js` 通过，10 个测试文件 / 215 个用例。

## 构建结果

- `npm run build` 通过。

## 当前功能验收说明

在 Chrome/Edge 语音面试中开启“浏览器识别失败时启用云端语音识别”，当浏览器 Web Speech 出现无结果/网络类中断时，
页面会自动改用服务端云端识别继续收音，用户无感知；后端未配置识别服务或用户未开启时，保持原有浏览器识别行为。

## 停止，不继续下一个功能

本轮只为语音面试语音输入增加云端 STT 兜底，不改动文本听写、不恢复离线 STT、不改动 TTS 链路、不新增其它语音服务。
