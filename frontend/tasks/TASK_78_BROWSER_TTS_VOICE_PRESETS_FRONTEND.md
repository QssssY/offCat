# 浏览器音色预设扩充前端

## 当前任务所属模块

设置中心面试偏好、浏览器系统 TTS 音色选择、语音面试 AI 播报参数应用。

## 前端文件定位

- `frontend/app/src/utils/settingsPreferences.js`
- `frontend/app/src/composables/useTextToSpeech.js`
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/__tests__/utils/settingsPreferences.test.js`
- `frontend/app/src/__tests__/composables/useTextToSpeech.test.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`

## 后端文件定位

无后端改动。本轮为纯前端浏览器 TTS 预设扩充，不涉及 Controller、Service、Mapper、DTO 或数据库脚本。

## 本轮修改文件清单

- `settingsPreferences.js` 新增浏览器 TTS 预设分组，扩充为 15 个预设，并集中维护各预设绑定的 `rate/pitch` 参数。
- `useTextToSpeech.js` 新增具体预设的 voice 名称匹配规则、预设可用性判断和预设参数读取能力。
- `SettingsView.vue` 将 AI 播报声音选择器改为女声系列、男声系列、通用、自定义四组；当前系统无匹配 voice 的具体预设会显示“当前系统不可用”并禁用。
- `SettingsView.vue` 在选择绑定参数的预设时同步更新语速和音调滑块，试听时同样按预设参数播放。
- `SettingsView.vue` 追加 Chrome 兼容标注：当 Chrome 只暴露 1-2 种中文浏览器 voice 时，状态提示会显示当前真实可用数量，并在预设下拉项中标注“Chrome 共用 N 种 voice”，避免用户误以为每个预设都是独立音色。
- `InterviewSessionView.vue` 读取本机偏好时，如果当前音色预设绑定了 `rate/pitch`，语音面试播报优先使用预设参数，避免历史滑块值覆盖当前音色风格。
- 前端测试补充预设白名单、预设参数、voice 选择、不可用预设禁用、设置页滑块联动、Chrome 单中文 voice 标注和面试页运行时参数覆盖场景。

## 前端实现方案

- 预设元数据放在 `settingsPreferences.js`，避免页面和播报逻辑各自维护一套 key 白名单。
- voice 匹配规则放在 `useTextToSpeech.js`，由浏览器实际暴露的 `speechSynthesis.getVoices()` 决定可用性。
- `system/custom` 不绑定语速和音调，继续允许用户使用滑块保存的自定义播报参数。
- 具体预设只在找到匹配 voice 时显式选中该 voice；找不到时不强行套用错误性别或错误风格的 voice。
- Chrome 单独做前端标注，不伪造更多本地 voice，也不改变 Edge 的正常系统 voice 选择；如果 Chrome 只暴露少量中文 voice，则明确说明多个预设会共用同一真实音色，预设差异主要体现在语速和音调参数。
- 设置页不新增页面、不新增弹窗、不改变语音面试入口；只扩展现有“面试偏好 > 语音通话”里的音色选择。

## 后端实现方案

无后端实现。本轮不新增接口、不修改云端 TTS、不修改系统级 TTS 配置、不修改自定义 AI Provider 链路。

## 数据存储方案

不新增数据库结构。仍复用浏览器本机 `ai_resume_settings_preferences` 保存 `voicePreferredType`、`voiceSpeakingRate`、`voicePitch`、`voiceVolume`、自定义 voice 信息等现有字段；新增预设仅扩展合法取值范围。

## stage 更新说明

`frontend/tasks/stage.md` 顶部已追加 Chrome 浏览器 voice 数量限制标注修复、验证结果和停止范围；根目录 `tasks/stage.md` 无需更新，因为没有后端改动。

## 编译结果

- RED 验证：旧实现下 `npm.cmd test -- --run src/__tests__/utils/settingsPreferences.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 失败，复现缺少 15+ 预设、预设参数、可用性判断、设置页滑块联动和面试页预设参数覆盖。
- GREEN 目标验证：同一命令通过，4 个测试文件 / 138 个用例。
- 前端全量测试：`npm.cmd test` 通过，80 个测试文件 / 565 个用例。
- Chrome 标注修复 RED 验证：旧实现下 `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 失败，复现 Chrome 只暴露 1 种中文 voice 时仍只显示“实际音色”，未标注多个预设共用同一音色。
- Chrome 标注修复 GREEN 验证：同一设置页测试通过，1 个测试文件 / 46 个用例；前端全量测试 `npm.cmd test` 通过，80 个测试文件 / 566 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

用户可在设置中心选择 15 个浏览器音色预设；系统当前没有匹配 voice 的具体预设会禁用并标注不可用。选择慢速清晰、新闻播报、温柔女声等绑定预设后，语速和音调滑块会同步到该预设值，语音面试实际播报也会使用对应参数。

Chrome 中如果浏览器只暴露少量中文 voice，设置页会直接提示“Chrome 当前只暴露 N 种中文浏览器 voice，多个预设会共用同一音色”，并在预设选项后标注“Chrome 共用 N 种 voice”。Edge 暴露完整系统 voice 时继续按真实 voice 匹配，不额外降级。

## 停止，不继续下一个功能

本轮只完成 `develop-project.txt` 中功能 3（浏览器音色预设扩充），不继续实施功能 1（功能分布日期范围）或功能 2（系统级 TTS 配置）。
