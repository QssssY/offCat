# EdgeTTS 云端语音 Provider 前端（2026-06-05）

## 追加修复：AI 播报声音展开 EdgeTTS 具体音色（2026-06-05）

- 问题：设置中心“AI 播报声音”此前只展示一个“EdgeTTS 免费云端音色”入口，实际只是 provider shortcut，没有把内置 EdgeTTS 免费 voice 展开为可选音色，用户无法直接选择云希、云扬、粤语、台湾普通话等具体 voice。
- 修复：`settingsPreferences.js` 抽出 `EDGE_CLOUD_TTS_VOICES`、`EDGE_CLOUD_TTS_VOICE_OPTIONS` 和 voice id / preference 双向映射；“云端语音”分组保留旧版 `edge_cloud` 默认项，同时新增 `edge_cloud:<voiceId>` 具体音色项，历史本地缓存继续兼容默认晓晓。
- 联动：`SettingsView.vue` 复用同一份 EdgeTTS voice 列表作为 TTS provider 内置音色来源；选择具体云端音色时自动回填 EdgeTTS 表单并写入对应 `ttsVoiceId`，试听仍走后端 `previewTtsVoice` 音频 Blob，不依赖 Chrome 本地 `speechSynthesis` voice。
- 验证：`npm.cmd test -- --run src/__tests__/utils/settingsPreferences.test.js src/__tests__/views/SettingsView.test.js` 通过，2 个测试文件 / 62 个用例。
- 范围：本轮只修复“AI 播报声音”下拉的 EdgeTTS 具体音色选择，不新增后端合成协议、不新增音频存储、计费统计、流式音频或 STT。

## 当前任务所属模块

用户设置中心 / 管理端 AI 引擎管理 / 系统级 TTS 配置。

## 前端文件定位

- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/utils/settingsPreferences.js`
- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/__tests__/utils/settingsPreferences.test.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`
- `frontend/app/src/__tests__/views/AdminAiEngineView.test.js`

## 后端文件定位

- `server/src/main/java/com/airesume/server/common/constants/TtsProviderConstants.java`
- `server/src/main/java/com/airesume/server/service/impl/UserTtsSpeechServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/UserTtsConnectivityTestServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/TtsDiscoveryServiceImpl.java`

后端联动记录见 `tasks/TASK_84_EDGETTS_PROVIDER_BACKEND.md`。

## 本轮修改文件清单

- `SettingsView.vue` 的用户自定义 TTS provider 列表新增 EdgeTTS。
- `SettingsView.vue` 的“AI 播报声音”下拉新增 EdgeTTS 云端音色入口，选择后自动回填用户自定义 TTS 的 EdgeTTS 预设并展开配置区。
- `settingsPreferences.js` 新增 `edge_cloud` 本地偏好值，用于区分云端 EdgeTTS 与浏览器本地 `speechSynthesis` voice。
- `SettingsView.vue` 在选择 EdgeTTS 时自动回填 Base URL、模型、默认音色、端点路径和内置音色列表。
- `SettingsView.vue` 的保存、测试、试听、发现校验支持 EdgeTTS 空 API Key。
- `AdminAiEngineView.vue` 的系统 TTS provider 列表新增 EdgeTTS。
- `AdminAiEngineView.vue` 在选择 EdgeTTS 时自动回填系统 TTS 表单和内置音色列表。
- `AdminAiEngineView.vue` 的保存、测试、试听、发现校验支持 EdgeTTS 空 API Key。
- `SettingsView.test.js` 与 `AdminAiEngineView.test.js` 补充 EdgeTTS provider 预设回归。

## 前端实现方案

- EdgeTTS 作为云端 TTS provider 出现在现有下拉框中，不新增页面或独立流程。
- “AI 播报声音”保留浏览器本地音色预设，同时新增“云端语音 / EdgeTTS 免费云端音色”；该选项不参与浏览器 voice 可用性判断，避免 Chrome 本地 voice 数量不足时被误禁用。
- 选择或回显 `edge_cloud` 时清空浏览器自定义 voice 绑定，调用既有 `handleTtsProviderChange('edge')` 回填云端 TTS 表单，试听按钮改走后端 `previewTtsVoice` 音频 Blob。
- 切换 provider 时直接写入 `ttsProvider=edge`，并回填：
  - `https://speech.platform.bing.com`
  - `edge-tts`
  - `zh-CN-XiaoxiaoNeural`
  - `/consumer/speech/synthesize/readaloud/edge/v1`
- 发现结果使用内置 Edge 音色列表，避免切换 provider 时隐式出网。
- EdgeTTS 下 API Key 为空仍允许保存、测试、试听和获取模型/音色。

## 后端实现方案

后端新增 Edge Read Aloud 合成客户端，用户自定义 TTS 与系统级 TTS 均通过现有 TTS 接口返回音频 Blob。

## 数据存储方案

本轮无数据库结构或服务端缓存结构变更。EdgeTTS 配置复用现有 TTS 字段，API Key 为空；“AI 播报声音”新增的 `edge_cloud` 仅写入现有本地设置偏好缓存。

## stage 更新说明

`frontend/tasks/stage.md` 已置顶补充“EdgeTTS 云端语音 Provider 前端”记录；后端 stage 见根目录 `tasks/stage.md`。

## 编译结果

后端编译结果见 `tasks/TASK_84_EDGETTS_PROVIDER_BACKEND.md`。

## 构建结果

- RED：旧实现下 `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js src/__tests__/views/AdminAiEngineView.test.js` 失败，EdgeTTS provider 不存在且切换后未写入 provider。
- GREEN：同一目标测试命令通过，2 个测试文件 / 68 个用例。
- 追加 RED：旧实现下 `npm.cmd test -- --run src/__tests__/utils/settingsPreferences.test.js src/__tests__/views/SettingsView.test.js` 失败，复现 `edge_cloud` 偏好值不存在且“AI 播报声音”入口未联动 EdgeTTS。
- 追加 GREEN：同一目标测试命令通过，2 个测试文件 / 60 个用例，覆盖选择和刷新回显两条链路。
- 构建：`npm.cmd run build` 通过。

## 代码审查补充修复

- 问题：选择“EdgeTTS 免费云端音色”时会立即把 `edge_cloud` 写入本地偏好；如果用户没有保存自定义 TTS 配置，后续页面可能显示已选择云端音色，但后端并没有可用 EdgeTTS 配置。
- 修复：`handleVoicePreferredTypeChange()` 只回填并展开 EdgeTTS 表单，不立即持久化本地偏好；`handleUserAiConfigSave()` 在 EdgeTTS 配置保存成功后才写入 `edge_cloud`。
- 验证：`npm.cmd test -- src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 54 个用例。
- 范围：仅修复 EdgeTTS 云端音色偏好保存时机，不调整浏览器本地 voice、管理端系统 TTS 或其它 Provider 交互。

## 当前功能验收说明

- 用户设置中心自定义 TTS 可以直接选择 EdgeTTS 并看到内置免费音色。
- 用户设置中心“AI 播报声音”可以直接选择 EdgeTTS 免费云端音色；Chrome 可用，因为试听和后续云端 TTS 播放走后端音频 Blob，不依赖本地 voice 列表。
- 管理端系统 TTS 可以直接选择 EdgeTTS 作为系统兜底语音服务。
- Chrome 可以使用该云端 TTS 链路，因为浏览器只负责播放后端返回的音频 Blob，不依赖 Chrome 本地 `speechSynthesis` 暴露的 voice。
- 本轮不新增浏览器本地 voice，不做音频存储、计费统计或流式音频。

## 停止，不继续下一个功能

本轮仅完成 EdgeTTS Provider 前端接入，等待验收，不继续推进其它语音能力。
