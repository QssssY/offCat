# 移除 sherpa-onnx 离线语音识别前端链路

## 当前任务所属模块

前端模拟面试语音交互、设置中心语音偏好、前端构建与本机语音资源管理。

## 前端文件定位

- `frontend/app/src/composables/useSpeechToText.js`
- `frontend/app/src/composables/useVoiceCall.js`
- `frontend/app/src/utils/settingsPreferences.js`
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/vite.config.js`
- `frontend/app/package.json`
- `frontend/app/package-lock.json`

## 后端文件定位

本轮前端主链路不新增后端接口；后端兜底路径清理验证见 `tasks/TASK_67_REMOVE_OFFLINE_STT_BACKEND.md`。

## 本轮修改文件清单

- 删除 `public/audio-worklets/offline-stt-processor.js`
- 删除 `public/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json`
- 删除 `public/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js`
- 删除 `scripts/download-sherpa-onnx-model.mjs`
- 删除 `src/workers/sherpaSpeechWorker.js`
- 删除 `src/utils/offlineVoiceModelCache.js`
- 删除 `src/utils/voiceModelDevServer.js`
- 删除离线 STT runtime/cache/worker/dev server 对应单测
- 修改语音识别、语音通话、设置偏好、设置页和面试页相关代码与单测
- 修改 `package.json` / `package-lock.json`，移除 `sherpa-onnx` 和离线模型下载脚本
- 修改 `vite.config.js`，移除离线模型开发期插件和大文件代理特殊配置

## 前端实现方案

- `useSpeechToText` 收敛为浏览器 Web Speech API 实现，不再创建 Worker、不再加载 WASM、不再下载或清理离线模型包。
- 设置中心移除“离线增强”子页、离线资源下载/删除入口和离线引擎状态展示，只保留浏览器 TTS 与语音识别语言等本机偏好。
- 面试页不再传入离线 STT 选项，不再预热离线 Worker；识别引擎状态统一显示为浏览器语音识别、系统本地识别或浏览器不可用。
- 设置偏好清洗只允许 `voiceRecognitionEngine: 'system_local'`，历史 `offline_sherpa` 与 `offlineSttEngine` 会被丢弃并回到默认值。
- Vite 构建不再复制离线模型清单、runtime、AudioWorklet 和本地模型目录；最终 `dist` 不包含 `voice-models`、`audio-worklets`、sherpa 或 offline-stt 产物。

## 后端实现方案

无前端新增后端依赖；当前语音识别完全依赖浏览器能力，Chrome/Edge 相对可用，Firefox 或部分环境会降级为手动输入。

## 数据存储方案

不新增数据库结构。浏览器端会在读取设置偏好时清洗旧离线字段，后续保存不会再写入 `offline_sherpa` 或 `offlineSttEngine`。

## stage 更新说明

`frontend/tasks/stage.md` 顶部已记录本轮移除离线 STT 的范围、验证结果和停止说明。

## 编译结果

- `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/utils/settingsPreferences.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，5 个测试文件 / 98 个用例。
- `npm.cmd run build` 通过。

## 构建结果

- `dist` 中未发现 `voice-models`、`audio-worklets`、`sherpa`、`offline-stt`、`wasm-main-asr` 等离线 STT 产物。
- `dist` 中未发现超过 25MiB 的文件。

## 当前功能验收说明

进入语音面试后，语音识别只走浏览器 Web Speech API。Chrome/Edge 可用时继续语音识别；浏览器不支持或服务不可用时，页面展示浏览器语音识别不可用并降级手动输入，不再要求下载离线语音包，也不会出现 sherpa-onnx WASM 加载错误。

## 停止，不继续下一个功能

本轮只删除 sherpa-onnx 离线 STT 与其下载/缓存/Worker/构建链路，不新增 Deepgram、云端 STT、后端语音识别或其它语音服务替代方案。
