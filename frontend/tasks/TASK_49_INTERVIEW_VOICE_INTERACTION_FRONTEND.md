## 验收反馈修正：Chrome 系统 TTS 男声优先降级提示（2026-05-31）
- 当前任务所属模块：设置中心语音偏好、模拟面试语音通话的浏览器系统 TTS 播报链路。
- 前端文件定位：`frontend/app/src/composables/useTextToSpeech.js`、`frontend/app/src/composables/useHybridTextToSpeech.js`、`frontend/app/src/views/settings/SettingsView.vue`、`frontend/app/src/views/interview/InterviewSessionView.vue` 以及对应 Vitest 回归测试。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段、SSE、AI 回复生成或音频上传链路修改。
- 本轮修改文件清单：`useTextToSpeech.js` 增加 voice 性别识别、同性别优先筛选和降级状态；`useHybridTextToSpeech.js` 透传系统 TTS voice 状态；`SettingsView.vue` 在“AI 播报声音”下显示当前实际浏览器 voice 和男/女声不可用提示；`InterviewSessionView.vue` 在语音通话状态中显示播报音色实际使用情况；`useTextToSpeech.test.js`、`SettingsView.test.js` 增加 Chrome 无中文男声回归。
- 问题原因：Chrome 的 `speechSynthesis.getVoices()` 经常只暴露一个通用中文 voice，或只有明确女声 `Microsoft Xiaoxiao Natural` 加一个通用 `Google 普通话（中国大陆）`。旧逻辑只按总分排序，用户选择“男声优先”时会选到明确女声，导致默认中文、男声、女声听起来都像同一个女声。Edge 能正常区分，是因为 Edge 通常暴露 Microsoft 中文男声/女声 Natural voice。
- 前端实现方案：当用户选择 `female` 或 `male` 时，先在当前浏览器 voice 列表中筛选同性交 voice；找不到时优先选择中性中文 voice，避免把明确女声当作男声使用。同时暴露 `voicePreferenceStatus`，设置页和语音通话页明确提示“当前浏览器没有暴露中文男声/女声，实际将使用某个 voice”。
- 数据存储方案：不新增本地存储字段，不修改 Cache API，不修改后端存储；只改变运行时 voice 选择与状态展示。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮 Chrome 系统 TTS 男声降级修复、边界和验证结果。
- 编译结果：`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/SettingsView.test.js` 通过，2 个测试文件 / 74 个用例通过；相关语音回归 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useOfflineTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，4 个测试文件 / 113 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：Chrome 没有真实中文男声时，不再把明确女声静默当作男声；设置页和语音通话页会显示当前实际播报 voice。此修复不能凭空生成 Chrome 不存在的男声，真实一致的高品质男声仍需要部署真实 Kokoro 男声资源，或使用浏览器能暴露的 Microsoft/系统男声。
- 停止说明：本轮只修复浏览器系统 TTS 的男/女声优先降级和提示，不引入真实 Kokoro 大模型、不新增后端 TTS、不上传音频、不继续扩展更多音色。

## 验收反馈修正：Kokoro 占位包试听按钮灰置（2026-05-31）
- 当前任务所属模块：设置中心离线增强高品质音色包试听入口，处理用户反馈“下载的高品质音色只有两种，并且试听按钮是灰色，根本无法试听”的问题。
- 前端文件定位：`frontend/app/src/views/settings/SettingsView.vue`、`frontend/app/src/__tests__/views/SettingsView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段、SSE、AI 回复生成或音频上传链路修改。
- 本轮修改文件清单：`SettingsView.vue` 移除占位 Kokoro 包对试听按钮的强制禁用；占位包点击试听时显示占位资源提示，并临时使用浏览器系统 TTS 按女声/男声偏好做兜底试听；`SettingsView.test.js` 将占位包回归从“按钮禁用”调整为“按钮可点、提示清楚、触发浏览器 TTS 兜底”。
- 问题原因：当前仓库提交的是可下载占位包，manifest 只声明 `zh_female` 与 `zh_male` 两个 voice，`runtime.js` 不具备真实 Kokoro 合成能力。上一轮为了避免误导用户听到浏览器 fallback 后以为是 Kokoro，直接把占位包试听按钮禁用；这会让“已缓存高品质语音包”的状态和不可点击试听产生冲突。
- 前端实现方案：真实 Kokoro 包仍按原逻辑走 `kokoroPreviewTextToSpeech` 离线合成；占位包则在用户点击时说明“正在使用浏览器系统 TTS 兜底试听”，并通过系统 TTS 播放明确包含“浏览器系统 TTS”的试听文案。这个兜底不保存 `voicePreferredType`、`voiceName`、`voiceURI` 或 `offlineTtsEngine`，不会覆盖用户语音通话里已调好的默认中文自然音色。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 结构，不改变 manifest key；仍不提交真实 tokenizer、voice、ONNX 或 runtime 大模型文件。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录占位包试听按钮灰置的根因、修复范围、当前只含两个 voice 的边界和验证结果。
- 编译结果：先调整 RED 回归并确认失败于占位包试听按钮 disabled；修复后 `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 43 个用例通过。
- 当前功能验收说明：下载当前内置 Kokoro 占位包后，“试听女声/试听男声”不会再灰置；点击会发声，但页面会明确告知这是浏览器系统 TTS 兜底，不是真实高品质 Kokoro。真实高品质试听仍需要把正式模型资源部署到 `/voice-models/kokoro/zh-cn-dual/`。
- 停止说明：本轮只修复占位包试听入口灰置，不新增更多 Kokoro 音色、不接后端 TTS、不上传音频、不提交大模型文件。

## 验收反馈修正：Chrome 默认中文自然音色试听无声（2026-05-31）
- 当前任务所属模块：设置中心语音偏好与模拟面试浏览器系统 TTS 播报链路，处理用户反馈“Edge 能听到，但 Chrome 默认中文自然音色试听无声”的问题。
- 前端文件定位：`frontend/app/src/composables/useTextToSpeech.js`、`frontend/app/src/__tests__/composables/useTextToSpeech.test.js`，设置页调用仍复用既有 `SettingsView.vue` 的系统 TTS 试听入口。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段、SSE、AI 回复生成或音频上传链路修改。
- 本轮修改文件清单：`useTextToSpeech.js` 调整默认 voice 评分；`useTextToSpeech.test.js` 更新并新增 Chrome voice 选择回归测试。
- 问题原因：Edge 通常能暴露可直接播放的 Microsoft 系统/自然中文 voice；Chrome 在部分环境会同时暴露远程 `Google 普通话（中国大陆）` voice 和本地 Windows 中文 voice。旧评分把远程 Google 中文 voice 排在本地 Windows 中文 voice 前面，在网络或服务不可用时会出现 `speechSynthesis.speak()` 被接受但试听无声。
- 前端实现方案：默认中文播报仍优先选择真正的 `Xiaoxiao/Yunxi/Natural/Neural/Premium` 等高质量中文 voice；当候选项只是普通远程 Google 中文 voice 时，优先选择本地中文 voice，保证 Chrome 设置页试听和语音面试播报更可靠。自定义浏览器 voice、系统默认、女声优先、男声优先和 Kokoro 离线偏好链路保持不变。
- 数据存储方案：不新增本地存储字段，不修改 Cache API，不改后端存储；只改变运行时 voice 选择评分。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮 Chrome TTS 选择修复和验证结果。
- 编译结果：先调整回归测试并确认 RED 失败于 Chrome 选择远程 Google voice；修复后 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js` 通过，1 个测试文件 / 29 个用例通过；`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 43 个用例通过；`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js src/__tests__/composables/useVoiceCall.test.js` 通过，2 个测试文件 / 56 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：Chrome 默认中文自然音色不再优先使用普通远程 Google 中文 voice；有本地中文 voice 时会优先走本地 voice，避免 Edge 可播但 Chrome 试听无声的情况。若某台 Chrome 完全没有可用本地中文 voice，仍需要在系统安装中文语音包、改选“系统默认/指定浏览器音色”，或部署真实 Kokoro 模型资源。
- 停止说明：本轮只修复 Chrome 浏览器系统 TTS 默认 voice 选择，不引入真实大模型、不改后端、不上传音频、不继续扩展云端 TTS。

## 验收反馈修正：系统 TTS 与 Kokoro 离线音色偏好隔离（2026-05-31）
- 当前任务所属模块：设置中心语音偏好、离线增强高品质音色包和模拟面试语音播报链路，处理用户反馈“默认中文自然音色无法试听、Kokoro 下载后覆盖原本系统男女声、Kokoro 男声女声听起来一样”的问题。
- 前端文件定位：`frontend/app/src/utils/settingsPreferences.js`、`frontend/app/src/composables/useHybridTextToSpeech.js`、`frontend/app/src/views/settings/SettingsView.vue`、`frontend/app/src/views/interview/InterviewSessionView.vue` 以及对应 Vitest 回归测试。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段、SSE、AI 回复生成或音频上传链路修改。
- 本轮修改文件清单：`settingsPreferences.js` 新增独立的 `offlineTtsVoiceType` 偏好；`useHybridTextToSpeech.js` 支持系统 TTS voice 偏好与离线 Kokoro voice 偏好分离；`SettingsView.vue` 将“AI 播报声音”的试听固定为浏览器系统 TTS，Kokoro 卡片试听使用独立离线试听实例，下载成功只缓存资源不再自动切换 `offlineTtsEngine`；`InterviewSessionView.vue` 在语音面试使用 Kokoro 时读取 `offlineTtsVoiceType`，系统 fallback 仍读取原 `voicePreferredType/voiceName/voiceURI/voiceLang`。
- 问题原因：上一轮为了快速接入 Kokoro，把 `voicePreferredType` 同时复用为浏览器系统 voice 偏好和 Kokoro 女声/男声选择，并且下载成功后自动保存 `offlineTtsEngine: 'kokoro'`。这会让设置页“默认中文自然音色”试听走离线/回退链路，也会在点击 Kokoro 男声/女声时覆盖用户原本调好的系统 voice。当前仓库仍是 Kokoro 占位资源，如果未部署真实 male voice/runtime，即使 UI 有男声选项也不可能产生真实男声音色。
- 前端实现方案：新增 `offlineTtsVoiceType: 'female' | 'male'` 专门记录 Kokoro 离线音色；系统 TTS 的 `voicePreferredType`、自定义浏览器 voice 名称与 URI 不再被 Kokoro 选择清空或覆盖。高品质音色包下载完成只更新缓存状态并提示可选择启用；点击 Kokoro 女声/男声卡片才会启用 `offlineTtsEngine: 'kokoro'` 并保存离线 voice 类型；Kokoro 卡片的“试听”只试听对应离线 voice，不改变当前语音面试启用的系统/Kokoro 引擎。
- 数据存储方案：仅在既有设置中心 localStorage 偏好中新增 `offlineTtsVoiceType` 字段，非法旧值会归一到 `female`；不新增 Cache API key，不新增后端存储，不提交真实模型二进制。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮偏好隔离、下载不自动接管和验证结果。
- 编译结果：先新增 RED 用例并确认失败于缺少 `offlineTtsVoiceType`、下载自动切换 Kokoro、Kokoro 选择覆盖系统 voice、语音面试离线播报仍读取 `voicePreferredType`；修复后 `npm.cmd test -- --run src/__tests__/utils/settingsPreferences.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 83 个用例通过；相关回归 `npm.cmd test -- --run src/__tests__/utils/settingsPreferences.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/composables/useOfflineTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，5 个测试文件 / 106 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：语音设置页的“默认中文自然音色 / 女声优先 / 男声优先 / 指定浏览器音色”试听重新只使用浏览器系统 TTS，不会被 Kokoro 下载状态劫持；Kokoro 音色选择与系统 voice 偏好独立保存。当前高品质包 manifest 仍只声明两个 Kokoro voice，真实男声需要部署真实男声音色文件和兼容 runtime，仓库占位资源不会产生真实男声。
- 停止说明：本轮只修复系统 TTS 与 Kokoro 离线音色偏好互相覆盖的问题，不引入真实大模型、不改后端、不上传音频、不继续扩展更多 Kokoro 音色。

## 验收反馈修正：Kokoro 离线 TTS 双音色选择与占位包提示（2026-05-31）
- 当前任务所属模块：设置中心离线增强页的高品质离线音色包管理，处理用户反馈“下载的 Kokoro 没有试听资源、没有选择音色选项”的问题。
- 前端文件定位：`frontend/app/src/views/settings/SettingsView.vue`、`frontend/app/src/utils/offlineVoiceModelCache.js`、`frontend/app/src/__tests__/views/SettingsView.test.js`、`frontend/app/src/__tests__/utils/offlineVoiceModelCache.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段、SSE、AI 回复生成或音频上传链路修改。
- 本轮修改文件清单：`offlineVoiceModelCache.js` 透传并持久化 Kokoro manifest 的 `placeholder` 标记，并兼容上一轮已缓存但缺少 `placeholder` 字段的旧占位状态；`SettingsView.vue` 在 Kokoro 音色包已缓存后展示“Kokoro 中文女声 / Kokoro 中文男声”两个显式选项，每个选项提供独立试听按钮；占位包状态下显示“已下载占位包，尚未部署真实 Kokoro 模型资源”，并禁用真实离线试听按钮，避免把浏览器 fallback 误认为 Kokoro 音色。
- 问题原因：上一轮只完成 Kokoro 下载缓存与 fallback 外壳，音色映射仍藏在通用“女声优先 / 男声优先”偏好里，设置页没有在离线增强卡片中显式展示 Kokoro 两个 voice；同时占位资源能完成下载，但真实 runtime/model 仍未部署，缺少明确提示会让用户误以为已经下载了可试听的高品质音色。
- 前端实现方案：下载成功且 `tts:kokoro` 状态为 ready 后，在“高品质离线音色包”卡片中显示双音色 radiogroup；点击女声/男声会保存 `offlineTtsEngine: 'kokoro'` 和对应 `voicePreferredType`，清空浏览器 custom voice 字段。独立试听按钮沿用 `useHybridTextToSpeech`，真实 Kokoro 可用时走离线合成；占位包时直接提示需要替换 tokenizer、voice、ONNX 与 runtime，不播放误导性的浏览器 fallback。
- 数据存储方案：不新增本地存储 key，不修改 Cache API 名称；仅在既有 `ai_resume_offline_voice_model_status` 模型状态中保留 `placeholder` 布尔字段。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮 Kokoro 双音色 UI、占位包边界和验证结果。
- 编译结果：先新增 RED 用例并确认失败于 `placeholder` 未透传、旧缓存占位状态不可识别、设置页没有 `.kokoro-voice-options`、占位提示缺失和 `handleKokoroVoicePreview` 不存在；实现后 `npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js` 通过，1 个测试文件 / 18 个用例通过；`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 42 个用例通过；相关回归 `npm.cmd test -- --run src/__tests__/utils/settingsPreferences.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/composables/useOfflineTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，5 个测试文件 / 105 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：Kokoro 包缓存后，用户可在离线增强卡片里直接选择中文女声或中文男声；真实模型未部署时页面会明确说明当前只是占位包，真实离线试听不可用。现阶段仍只有两个 Kokoro 音色定义，后续新增音色只需扩展 manifest/runtime voice 清单和 UI 映射。
- 停止说明：本轮只补齐 Kokoro 双音色选择、独立试听入口和占位资源提示，不引入真实大模型、不改后端、不上传音频、不继续扩展云端 TTS。

## 验收反馈修正：Kokoro 离线 TTS 下载缺失 tokenizer 资源（2026-05-31）
- 当前任务所属模块：设置中心高品质离线音色包下载与浏览器 Cache API 静态资源缓存，处理用户点击下载 Kokoro 离线引擎时报“离线语音模型文件不是有效模型资源，请确认已部署 tokenizer.json”的问题。
- 前端文件定位：`frontend/app/public/voice-models/kokoro/zh-cn-dual/manifest.json`、`frontend/app/public/voice-models/kokoro/zh-cn-dual/runtime.js`、`frontend/app/public/voice-models/kokoro/zh-cn-dual/tokenizer.json`、`frontend/app/public/voice-models/kokoro/zh-cn-dual/voices/zh_female.bin`、`frontend/app/public/voice-models/kokoro/zh-cn-dual/voices/zh_male.bin`、`frontend/app/public/voice-models/kokoro/zh-cn-dual/kokoro-zh-cn.onnx`、`frontend/app/src/__tests__/utils/offlineVoiceModelCache.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段、SSE、AI 回复生成或音频上传链路修改。
- 本轮修改文件清单：补齐 Kokoro manifest 已声明但源码静态目录缺失的 `tokenizer.json`、男女声音色文件和 ONNX 文件的轻量占位资源；`manifest.json` 增加 placeholder 标记并调整占位资源进度权重；`runtime.js` 的占位失败提示改为中文；`offlineVoiceModelCache.test.js` 新增清单本地资源必须真实存在的回归测试。
- 问题原因：上一轮为了不提交大模型二进制，只新增了 Kokoro manifest 和 runtime 占位文件，但 manifest 同时声明了 `tokenizer.json`、`voices/zh_female.bin`、`voices/zh_male.bin`、`kokoro-zh-cn.onnx`。下载器会严格按 manifest 逐个拉取资源，缺失文件在 Vite/SPA 下会返回 HTML fallback，因此被判定为“不是有效模型资源”并失败在 `tokenizer.json`。
- 前端实现方案：保持“不提交真实大模型二进制”的边界不变，给 manifest 中的同源本地资源补齐可下载占位文件，避免下载流程命中 HTML fallback；真实高品质离线合成仍需要用正式 Kokoro tokenizer、voice、ONNX 和 runtime 替换这些占位文件。当前占位 runtime 若被调用会明确报错并由 hybrid TTS 回退浏览器 TTS。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 名称；仍使用 `tts:kokoro` 写入 `ai_resume_offline_voice_model_status` 和 `ai-resume-offline-voice-models-v1`。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮 Kokoro 下载缺失资源修复、占位资源边界和验证结果。
- 编译结果：先新增 RED 回归测试并确认旧状态失败于缺失 `tokenizer.json`、男女声音色和 ONNX 文件；补齐资源后 `npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js` 通过，1 个测试文件 / 17 个用例通过。相关回归 `npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/composables/useOfflineTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，4 个测试文件 / 95 个用例通过。
- 构建结果：`npm.cmd run build` 通过；构建产物已确认包含 `dist/voice-models/kokoro/zh-cn-dual/tokenizer.json`、`dist/voice-models/kokoro/zh-cn-dual/voices/zh_female.bin` 和 `dist/voice-models/kokoro/zh-cn-dual/kokoro-zh-cn.onnx`。
- 当前功能验收说明：用户点击“下载高品质语音包”时，不会再因为 `tokenizer.json`、男女声音色或 ONNX 路径缺失而下载失败；如果尚未部署真实 Kokoro 模型，播报阶段仍会按既有策略明确失败并回退浏览器 TTS。
- 停止说明：本轮只修复 Kokoro 离线 TTS 下载清单指向缺失本地资源的问题，不引入真实大模型、不改后端、不上传音频、不继续扩展云端 TTS。

## 浏览器本地 Kokoro 离线 TTS 音色包接入（2026-05-31）
- 当前任务所属模块：模拟面试前端语音播报链路与设置中心离线增强资源管理，按用户本轮计划接入浏览器本地 Kokoro 离线 TTS 音色包下载、缓存、试听与语音面试播报优先级，不改变后端 SSE、AI 回复生成或 STT 识别链路。
- 前端文件定位：`frontend/app/src/composables/useOfflineTextToSpeech.js`、`frontend/app/src/composables/useHybridTextToSpeech.js`、`frontend/app/src/utils/settingsPreferences.js`、`frontend/app/src/views/settings/SettingsView.vue`、`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/public/voice-models/kokoro/zh-cn-dual/manifest.json`、`frontend/app/public/voice-models/kokoro/zh-cn-dual/runtime.js` 以及对应 Vitest 回归测试。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段、SSE 写出、AI 回复生成、文件上传或音频上传链路；STT 仍保持前端 sherpa-onnx 离线识别。
- 本轮修改文件清单：新增 `useOfflineTextToSpeech.js` 封装 Kokoro 本地 TTS 队列、runtime 调用、AudioContext 播放、女声/男声映射、失败状态与 stop 释放；新增 `useHybridTextToSpeech.js` 统一编排 Kokoro 与原 `speechSynthesis` fallback；扩展 `settingsPreferences.js` 的 `offlineTtsEngine` 合法值为 `system | kokoro`；设置页启用高品质离线音色包下载、进度、已缓存、失败重试、删除和下载成功自动切换 Kokoro；语音面试页改用 hybrid TTS；新增 Kokoro manifest 与 runtime 占位文件，真实模型仍需部署到静态目录。
- 前端实现方案：当 `offlineTtsEngine === 'kokoro'` 且 `tts:kokoro` 模型状态为 ready 时，设置页试听、语音面试开场白和后续分句播报优先调用 Kokoro 本地合成；女声/男声偏好分别映射到 `zh_female` 与 `zh_male`；runtime 返回 PCM、WAV、ArrayBuffer 或 AudioBuffer 后由 `AudioContext` 播放；连续分句保持应用层串行队列，整段播报结束前 `isSpeaking` 不释放；任何 runtime 初始化、合成或音频解码异常都会进入 `kokoro-failed` 并回退原浏览器 TTS。
- 设置中心实现方案：下载按钮调用 `downloadModelFromManifest('tts:kokoro', '/voice-models/kokoro/zh-cn-dual/manifest.json', onProgress)`，未缓存显示“下载高品质语音包”，下载中显示进度，ready 显示“已缓存高品质语音包”，failed 显示“重新下载高品质语音包”；下载成功保存 `offlineTtsEngine: 'kokoro'`，删除缓存后保存 `offlineTtsEngine: 'system'`。
- 后端实现方案：无后端改动；不新增 TTS 服务、不新增模型代理、不新增数据库表或字段、不上传用户音频。
- 数据存储方案：继续复用 `ai_resume_offline_voice_model_status` 与 `ai-resume-offline-voice-models-v1` Cache API；新增模型 key 为 `tts:kokoro`；大体积 tokenizer、voice、ONNX 文件不提交源码，当前仓库只提交 manifest 和 runtime 占位文件，真实高品质播报需要把模型资源部署到 `/voice-models/kokoro/zh-cn-dual/`。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮 Kokoro 离线 TTS 范围、缓存/回退行为、模型文件边界和验证结果。
- 编译结果：本轮新增/更新 RED 回归覆盖 `offlineTtsEngine: 'kokoro'` 偏好归一、`tts:kokoro` manifest 缓存、设置页下载/重试/删除、离线 TTS 女声男声/合成失败/串行播放/stop、语音面试 Kokoro 优先与失败 fallback。`npm.cmd test -- --run src/__tests__/utils/settingsPreferences.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/composables/useOfflineTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，5 个测试文件 / 100 个用例通过；语音与设置整组回归通过，10 个测试文件 / 195 个用例通过。
- 构建结果：`npm.cmd run build` 通过；不要求也不提交真实 Kokoro 大模型二进制。
- 当前功能验收说明：Chrome/Edge 在下载并缓存 Kokoro 包且用户选择离线 TTS 后，播报链路不再依赖各浏览器暴露的默认中文 voice；如果模型未 ready、runtime 仍是占位实现或合成失败，则明确回退现有浏览器 TTS，语音面试不会在离线 TTS 失败时提前恢复收音。
- 停止说明：本轮只接入浏览器本地 Kokoro TTS 缓存、播放外壳和 fallback 链路；不改后端、不上传音频、不改数据库、不提交大模型文件、不继续扩展云端 TTS 或其它语音能力。
## 验收反馈修正：问号后提前恢复聆听与离线 STT 采集警告（2026-05-30）
- 当前任务所属模块：模拟面试前端语音通话链路，覆盖浏览器 TTS 分句播报队列和离线 sherpa-onnx STT 麦克风音频采集，处理用户反馈的“念到第一个问号后直接进入聆听，后半句没有播报”和 Chrome 控制台 `ScriptProcessorNode is deprecated` 警告。
- 前端文件定位：`frontend/app/src/composables/useTextToSpeech.js`、`frontend/app/src/composables/useSpeechToText.js`、`frontend/app/public/audio-worklets/offline-stt-processor.js`、`frontend/app/src/__tests__/composables/useTextToSpeech.test.js`、`frontend/app/src/__tests__/composables/useSpeechToText.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段、AI 回复生成或 SSE 写出逻辑修改。
- 本轮修改文件清单：`useTextToSpeech.js` 将流式 TTS 从“把多条 `SpeechSynthesisUtterance` 一次性交给浏览器队列”改为前端应用层串行队列，每次只把当前一句交给 `speechSynthesis.speak()`，上一句 `onend/onerror/watchdog` 释放后才播放下一句；`pendingCount` 表示应用层待播队列加当前浏览器 utterance 的总数，确保整段播报完成前 `isSpeaking` 始终为 true，语音通话层不会在第一个问号后恢复收音。`useSpeechToText.js` 新增 AudioWorklet 优先的离线 PCM 采集路径，`ScriptProcessorNode` 仅作为旧浏览器或 worklet 加载失败时的兼容兜底。新增 `public/audio-worklets/offline-stt-processor.js` 负责从音频线程把 PCM 样本发回主线程。测试文件补充用户原句“没关系，这个知识点比较深……它们分别在什么阶段触发？”的分句串行回归，以及 AudioWorklet 采集不调用 `createScriptProcessor()` 的回归。
- 问题原因：上一次只解决了 utterance 强引用问题，但真实失败样例暴露出另一个边界：当前 TTS 会按句号/问号把流式回复拆成多条 utterance，并直接连续提交给浏览器自己的 `speechSynthesis` 队列。Chrome 对多 utterance 队列的完成事件、pending 状态和实际发声并不稳定；一旦第一个问号处的短句结束时前端状态被误判为队列已空，`useVoiceCall` 就会看到 `textToSpeech.isSpeaking=false` 并恢复“正在聆听”，导致后面的“它们分别在什么阶段触发？”没有机会播报。`ScriptProcessorNode` 警告来自离线 STT 的麦克风采集链路，它不是 TTS 中断原因，但属于浏览器已弃用 API，应该优先迁移到 AudioWorklet。
- 前端实现方案：TTS 层继续复用现有 voice 等待、开场白重试、watchdog、keep-alive 和强引用逻辑，但在业务层维护 `speechQueue`，只允许一个 active utterance 存在。已有 voice 可用时仍保持同步播报，Chrome voices 为空或需要等待更优音色时继续走原 800ms 有界等待，避免破坏设置页试听和开场白时序。STT 层抽出统一的 PCM 投递函数，AudioWorklet 可用时加载 `/audio-worklets/offline-stt-processor.js` 并用 `AudioWorkletNode` 接收样本；如果浏览器不支持或加载失败，再回退旧 `createScriptProcessor()`，保证兼容性。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 名称或模型状态结构；离线 sherpa-onnx 仍只负责 STT，面试官播报仍依赖浏览器 `speechSynthesis`，本轮不引入离线 TTS 或云端 TTS。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮真实根因、修复范围、AudioWorklet 警告处理和验证结果。
- 编译结果：先新增 RED 回归测试并确认旧实现失败于多分句一次性进入浏览器队列：`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js` 失败，用户原句场景中 `speechSynthesis.speak()` 被调用 3 次而不是 1 次；修复后同命令通过，1 个测试文件 / 29 个用例通过。随后新增 AudioWorklet RED 用例并确认旧实现未设置 `audioWorkletNode.port.onmessage`；修复后 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js` 通过，1 个测试文件 / 40 个用例通过。语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 185 个用例通过。
- 构建结果：`npm.cmd run build` 通过，构建产物包含 `dist/audio-worklets/offline-stt-processor.js`。
- 当前功能验收说明：对于“没关系，这个知识点比较深。我们换个角度，你在用Vue3开发的时候，用过哪些生命周期钩子？它们分别在什么阶段触发？”这类多问句回复，前端会先播第一句，结束后再播第二句和第三句，整段全部完成前 `isSpeaking` 不会释放，语音通话不会在第一个问号后进入聆听。离线 STT 在支持 AudioWorklet 的 Chrome 中不再优先触发 `ScriptProcessorNode` 弃用警告；旧浏览器仍可用兼容兜底。
- 停止说明：本轮只修复语音面试 TTS 分句队列提前释放和离线 STT 采集弃用 API 警告，不新增云端 TTS、不接入 Kokoro、不改后端、不上传音频、不替换模型包、不继续扩展新的语音能力。

## 验收反馈修正：Chrome 播报未念完就进入聆听状态（2026-05-30）
- 当前任务所属模块：模拟面试前端浏览器 TTS 播报生命周期，处理 Chrome/Edge 语音面试中面试官台词尚未完整播报，页面偶发提前认为 TTS 已结束并恢复“正在聆听”的不稳定问题。
- 前端文件定位：`frontend/app/src/composables/useTextToSpeech.js`、`frontend/app/src/__tests__/composables/useTextToSpeech.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或面试消息生成链路修改。
- 本轮修改文件清单：`useTextToSpeech.js` 新增播放中 `SpeechSynthesisUtterance` 的强引用集合，并在 `onend/onerror/watchdog/stop` 等统一结束路径释放；暴露 `activeUtteranceCount` 供回归测试验证播放中强引用是否存在。`useTextToSpeech.test.js` 新增 RED 回归用例，覆盖“播放中 utterance 必须被强引用持有，完成后释放”的场景。
- 问题原因：旧实现把 utterance 只作为 `WeakMap/WeakSet` 的 key 保存，函数返回后业务层没有任何强引用。Chrome 的 `speechSynthesis` 对 utterance 队列引用并不总是稳定，播放中的 `SpeechSynthesisUtterance` 可能被垃圾回收或提前结束，表现为面试官台词念到一半就中断，`isSpeaking` 被释放后语音通话层恢复收音，界面直接进入聆听状态。该问题具有随机性，所以会出现有时正常、有时中断。
- 前端实现方案：在 `enqueueNow()` 创建 utterance 后，把它加入本地 `Set` 形成强引用，确保浏览器播放期间 JS 侧一直持有对象；`markUtteranceEnd()` 作为统一结束入口，在正常结束、错误、启动超时、播报超时等路径中删除该引用；`clearSpeechState()` 在主动停止或新播报替换旧播报时清空集合，避免内存残留。现有 voices 等待、开场白重试、watchdog、keep-alive 和语音通话收音恢复逻辑保持不变。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 结构，不改变语音偏好字段；离线 sherpa-onnx 仍只负责 STT，面试官播报仍依赖浏览器 `speechSynthesis`。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮 TTS 半途停播根因、修复范围、验证结果和停止边界。
- 编译结果：先新增 RED 回归测试并确认旧实现失败于 `activeUtteranceCount` 不存在，说明当前组合函数没有可验证的播放中强引用；修复后 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js` 通过，1 个测试文件 / 28 个用例通过；语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 187 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：浏览器 TTS 播放期间，前端会持续持有当前 utterance，不再因为 JS 对象被回收而随机触发半途停播；只有浏览器真正触发结束/错误，或已有 watchdog 判定为启动失败/硬超时，语音通话层才会恢复聆听。
- 停止说明：本轮只修复浏览器 TTS 播放中 utterance 缺少强引用导致的半途停播和提前恢复收音问题，不新增云端 TTS、不接入 Kokoro、不改后端、不上传音频、不扩展新的语音能力。

## 验收反馈修正：离线语音引擎已缓存后仍显示可下载（2026-05-30）
- 当前任务所属模块：设置中心离线增强页的 sherpa-onnx 离线语音识别资源包下载与浏览器 Cache API 状态管理，处理用户已部署静态资源或已缓存资源后仍看到“下载离线语音引擎”、以及重复下载失败可能影响旧缓存删除的问题。
- 前端文件定位：`frontend/app/src/utils/offlineVoiceModelCache.js`、`frontend/app/src/views/settings/SettingsView.vue`、`frontend/app/src/__tests__/utils/offlineVoiceModelCache.test.js`、`frontend/app/src/__tests__/views/SettingsView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或用户音频上传链路修改。
- 本轮修改文件清单：`offlineVoiceModelCache.js` 在下载前先读取当前模型状态并通过 `isModelCached()` 校验浏览器 Cache API 是否已完整存在，若已经 ready 且文件齐全则直接返回现有状态，不再重复请求 manifest 和模型文件；manifest 读取失败时保留上一次状态中的 `files/runtime/version`，避免失败重试把旧文件清单清空后“删除资源包”不知道该删除哪些 Cache 条目；删除资源包时若遇到旧坏状态 `files` 为空，则按 manifest 所在目录前缀扫描当前模型 Cache 并删除残留条目。`SettingsView.vue` 新增 `offlineSttDownloadButtonText`，已缓存时按钮显示“已缓存离线语音引擎”，下载失败时显示“重新下载离线语音引擎”，下载中显示进度，避免 ready 状态仍用“下载”文案误导用户。测试文件补充已缓存跳过重复下载、失败重试保留旧 files 便于删除、旧坏状态按模型目录前缀清理残留缓存、ready 按钮文案为“已缓存”的回归用例。
- 问题原因：`npm run voice:model:download` 或手动把模型文件放进 `public/voice-models/...` 只代表项目静态资源目录已经有文件，浏览器本地 Cache API 还没有自动持有这些文件；设置页的“下载离线语音引擎”实际是把同源静态资源写入当前浏览器缓存。旧工具层没有在下载入口做“ready 且 Cache API 文件齐全”的短路保护，因此即使当前浏览器已经完整缓存，仍可能再次进入 manifest 和文件下载流程。另一个风险是：如果 ready 后用户再次触发下载但 manifest 临时不可用，旧失败分支会把 `files` 写成空数组，后续删除入口虽然仍显示 failed，但缺少旧文件 URL，无法清理之前的 Cache API 文件。
- 前端实现方案：把“静态资源是否部署”和“当前浏览器是否已缓存”明确分层。静态资源已部署时，首次点击下载会把资源复制进浏览器 Cache API；当前浏览器已经 ready 且缓存文件齐全时，下载函数直接返回，不再重复拉取。若缓存不完整或状态为 failed/pending，仍允许用户重新下载修复残缺资源。失败分支保留旧 files，让“删除资源包”能清理已知旧缓存；对历史上已经丢失 files 的旧坏状态，删除时使用 manifest 目录前缀兜底清理该模型目录下的缓存请求。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 名称；继续使用 `ai_resume_offline_voice_model_status` 保存轻量状态，继续使用 `ai-resume-offline-voice-models-v1` 保存模型文件。用户音频仍只在浏览器本地处理，不上传服务器。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮静态资源部署、浏览器缓存、重复下载短路和失败后可删除旧缓存的边界。
- 编译结果：先新增 RED 回归测试并确认旧实现失败于已 ready 仍进入 `fetch(manifest)`、失败重试后旧 `files` 被清空、旧坏状态 `files` 为空时删除不会清理 Cache API 残留；修复后 `npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js` 通过，1 个测试文件 / 15 个用例通过；`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 37 个用例通过；语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 186 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：如果当前浏览器已经完整缓存 sherpa-onnx 资源包，设置页按钮会显示已缓存且禁用，工具层也不会重复下载；如果只是项目静态目录已经有模型文件但当前浏览器未缓存，用户仍需要点击一次下载，把静态资源写入浏览器 Cache API，这是预期行为。重复下载失败不会再丢掉旧文件清单，删除资源包仍能释放已知旧缓存；旧版本已经写坏成 `failed + files: []` 的浏览器状态，也会在删除时按模型目录前缀清掉残留 Cache API 文件。
- 停止说明：本轮只修复离线 STT 资源包重复下载状态和失败后删除能力，不新增离线 TTS、不接入 Kokoro、不改后端、不上传音频、不替换模型包。

## 验收反馈修正：Chrome 播报中途被 watchdog 误取消（2026-05-30）
- 当前任务所属模块：模拟面试前端浏览器 TTS 播报生命周期，处理 Chrome 中面试官文字已经开始朗读，但旧的单条 utterance watchdog 按短句估算在约 12 秒左右主动 `cancel()`，导致“文字没播报完就暂停/念到一半中断”的问题；同时明确 Chrome 与 Edge 音色不一致的浏览器能力边界。
- 前端文件定位：`frontend/app/src/composables/useTextToSpeech.js`、`frontend/app/src/__tests__/composables/useTextToSpeech.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 本轮修改文件清单：`useTextToSpeech.js` 为已触发 `onstart` 的 utterance 记录 `startedAt`，新增已开始播报后的保守硬超时窗口；首个短句 watchdog 到点时，如果 Chrome 仍报告 `speechSynthesis.speaking/pending`，不再立刻 `cancel()`，而是续期等待，只有达到保守硬超时后才释放状态。`useTextToSpeech.test.js` 新增 Chrome 已开始播报但首个 watchdog 检查点仍在 speaking 时不得取消的回归用例，并把旧的“13 秒必须取消已开始播报”预期调整为“超过保守硬超时仍能释放状态”。
- 问题原因：前几轮为了避免 Chrome TTS 无声后页面卡在“AI 正在回复”，加入了每条 utterance 的结束 watchdog。但该 watchdog 使用 `max(12000ms, 文本长度 * 450ms)` 估算最大播报时长；在 Chrome 中某些中文音色、较慢语速或合成器调度下，真实朗读可能超过这个估算。旧逻辑即使已经收到 `onstart`、浏览器仍在 `speaking`，也会到点强制 `cancel()`，因此表现为播报到一半突然停止。
- Chrome 与 Edge 音色差异说明：当前实现仍基于浏览器 `speechSynthesis`。Edge 和 Chrome 虽同为 Chromium，但在 Windows 上暴露给网页的 TTS voice 列表、默认 voice、远程/本地 voice 策略可能不同；Edge 可用的自然中文音色不等于 Chrome 一定可见。前端只能在 Chrome 的 `speechSynthesis.getVoices()` 返回列表中评分选择，无法直接调用 Edge 专属音色，也无法让离线 sherpa-onnx 改变播报音色，因为 sherpa-onnx 在本项目中只负责 STT 识别，不负责 TTS 合成。
- 前端实现方案：保留 5 秒启动 watchdog，用于处理 Chrome 接受 `speak()` 但完全不触发 `onstart` 的场景；对于已经开始的播报，watchdog 先判断浏览器是否仍处于 `speaking/pending`，如果仍在播报队列中则续期，避免误杀正常朗读。保守硬超时保留为兜底，防止 Chrome 永久不触发 `onend/onerror` 时页面再次卡死。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 结构，不改变语音偏好字段；离线 sherpa-onnx 仍只负责 STT，面试官播报与设置页试听仍依赖浏览器 `speechSynthesis`。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮 Chrome 已开始播报后被短 watchdog 中断的根因、修复范围、浏览器音色边界和验证结果。
- 编译结果：先新增 RED 回归测试并确认旧实现失败于 13 秒时调用 `speechSynthesis.cancel()`；修复后 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js` 通过，1 个测试文件 / 27 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/SettingsView.test.js` 通过，4 个测试文件 / 117 个用例通过；语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 182 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：Chrome 中已经触发 `onstart` 的播报不再因为首个短 watchdog 检查点而被强行取消，能降低“念到一半中断”的概率；如果 Chrome 长时间既不结束也不报错，保守硬超时仍会释放页面状态，避免再次卡在 AI 回复/AI 说话中。
- 停止说明：本轮只修复 Chrome 已开始播报后被过短 watchdog 误取消的问题，并解释 Chrome/Edge 音色差异边界；不新增云端 TTS、不接入 Kokoro、不改后端、不上传音频、不扩展新的语音能力。

## 验收反馈修正：Chrome 先返回旧式机械音色导致试听仍不自然（2026-05-30）
- 当前任务所属模块：模拟面试前端浏览器 TTS 音色选择链路，处理 Chrome 中设置页试听和语音面试播报仍优先听到 `Microsoft Huihui Desktop` 等系统默认机械音色，而不是程序默认自然中文音色的问题。
- 前端文件定位：`frontend/app/src/composables/useTextToSpeech.js`、`frontend/app/src/__tests__/composables/useTextToSpeech.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 本轮修改文件清单：`useTextToSpeech.js` 增加旧式系统 voice 判断与更优 voice 等待逻辑，`prepareForUserGesture()` 之后如果当前最佳 voice 仍是 Huihui/Desktop/Zira/David 等旧式机械音色，会在既有 800ms 有界窗口内等待 Chrome 后续 `voiceschanged` 补齐 Google/自然/Neural/Premium 音色；超时仍无更优 voice 时才使用当前旧式系统音色兜底。`useTextToSpeech.test.js` 新增 Chrome voices 分批加载回归用例，覆盖“先只有 Huihui Desktop，随后出现 Google 普通话”应等待并选择 Google，以及超时后仍可回退 Huihui 避免完全不播。
- 问题原因：前一轮已解决 voices 为空时过早裸播的问题，但真实 Chrome 还会出现另一种时序：首次用户手势后 `speechSynthesis.getVoices()` 不是空，而是先返回 Windows 旧式本地 voice，例如 `Microsoft Huihui Desktop`。旧逻辑只要 voices 非空就立即创建 utterance，因此来不及等待 Chrome 后续补齐 `Google 普通话（中国大陆）` 或其它自然音色，表现为设置页试听和面试播报仍然是系统默认机械音色。
- 前端实现方案：保留现有浏览器 TTS、开场白兜底、无声 watchdog 和离线 STT 边界，只在用户点击手势触发的播报中增加一层质量判断。若当前选中的 voice 是旧式机械系统 voice 且偏好不是“系统默认”，不会立刻播报，而是复用 `waitForVoicesReady()` 等待更优的非旧式 voice；若 Chrome 在窗口内补齐更好的 voice，就按原评分选择自然/Google voice；若没有补齐，则继续使用旧式系统 voice，避免 Chrome 完全不出声或页面重新卡住。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 结构，不改变语音偏好字段；离线 sherpa-onnx 仍只负责 STT，面试官播报与设置页试听仍依赖浏览器 `speechSynthesis`。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮 Chrome voices 分批加载导致旧式机械音色被过早选中的根因、修复范围、验证结果和停止边界。
- 编译结果：先新增 RED 回归测试并确认旧实现失败于 `Microsoft Huihui Desktop` 被立即传给 `speechSynthesis.speak()`；修复后 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js` 通过，1 个测试文件 / 26 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 94 个用例通过；语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 181 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：Chrome 如果先返回 Huihui/Desktop 这类旧式机械 voice，试听和面试播报会短暂等待更好的自然/Google voice；如果 Chrome 的 voice 列表最终确实没有任何高质量 voice，前端无法凭空生成 Edge 中的自然音色，仍只能使用 Chrome 当前可用音色或后续接入独立 TTS 引擎。
- 停止说明：本轮只修复 Chrome 先返回旧式机械 voice 导致程序过早播报的问题，不新增云端 TTS、不接入 Kokoro、不改后端、不上传音频、不扩展新的语音能力。

## 验收反馈修正：设置页试听未预热导致 Chrome 音色机械（2026-05-30）
- 当前任务所属模块：设置中心面试偏好语音试听链路，处理 Chrome 中点击“试听当前 AI 播报声音”时没有先在用户点击手势内预热 `speechSynthesis`，导致 voices 列表不易加载、试听更容易落到系统默认机械音色的问题。
- 前端文件定位：`frontend/app/src/views/settings/SettingsView.vue`、`frontend/app/src/__tests__/views/SettingsView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 本轮修改文件清单：`SettingsView.vue` 在 `handleVoicePreview()` 中设置语速、音调、音量和音色偏好后，先调用 `previewTextToSpeech.prepareForUserGesture?.()` 再执行 `speak()`；`SettingsView.test.js` 为设置页补充浏览器 TTS mock，并新增 Chrome 风格的试听回归用例，模拟只有用户点击手势预热后 `getVoices()` 才返回默认自然中文音色。
- 问题原因：语音面试开始通话已经会在点击手势里预热 TTS，但设置页试听按钮原本只是直接调用 `previewTextToSpeech.speak()`。Chrome 的 voices 列表和音频合成器经常需要用户手势唤醒后才稳定返回；如果试听链路没有先预热，`getVoices()` 更可能保持为空或返回不完整，`selectedVoice` 为空时浏览器会使用系统默认 TTS 音色，表现为设置页试听和 Edge 相比明显机械。
- 前端实现方案：复用现有 TTS 封装的 `prepareForUserGesture()`，不新增设置字段、不改变音色评分、不引入新 TTS 引擎。设置页试听与语音面试开始通话保持一致：在用户点击事件内先唤醒合成器并刷新 voices，再通过已有有界等待逻辑选择默认自然中文音色；如果 Chrome 始终无法返回可用自然音色，仍由浏览器系统默认音色兜底。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 结构，不改变语音偏好字段；离线 sherpa-onnx 仍只负责 STT，面试官播报与试听仍依赖浏览器 `speechSynthesis`。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮设置页试听预热缺失的根因、修复范围、验证结果和停止边界。
- 编译结果：先新增 RED 回归测试并确认旧实现失败于试听点击后未调用 `speechSynthesis.resume()`，Chrome 风格 mock 无法返回默认自然音色；修复后 `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 36 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 91 个用例通过；语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 178 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：Chrome 设置页点击试听时会先在点击手势内唤醒 TTS，再让现有 TTS 逻辑等待 voices 并优先选择程序默认自然中文音色；如果 Chrome 的 `speechSynthesis.getVoices()` 列表本身没有 Edge 中可用的自然音色，试听仍只能在 Chrome 可用音色中选择，无法凭空获得 Edge 专属音色。
- 停止说明：本轮只修复设置页试听缺少 TTS 预热导致 Chrome 更容易使用系统默认机械音色的问题，不新增云端 TTS、不接入 Kokoro、不改后端、不上传音频、不扩展新的语音能力。

## 验收反馈修正：Chrome 预热后默认自然音色丢失（2026-05-30）
- 当前任务所属模块：模拟面试前端语音通话 Chrome TTS 默认音色选择修正，处理 Chrome 点击开始语音通话后因为 voices 列表尚未加载完成，程序配置的默认中文自然音色没有被赋给 utterance，最终落到系统默认机械音色的问题。
- 前端文件定位：`frontend/app/src/composables/useTextToSpeech.js`、`frontend/app/src/__tests__/composables/useTextToSpeech.test.js`、`frontend/app/src/__tests__/views/InterviewSessionView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 本轮修改文件清单：`useTextToSpeech.js` 将 `userGesturePrepared` 与 `allowDefaultVoice` 从“立即允许裸播系统默认音色”调整为“先等待浏览器 voices 有界加载，超时后才允许系统默认音色兜底”；`useTextToSpeech.test.js` 增加 Chrome 点击手势后 voices 延迟返回、开场白允许兜底但仍应优先等默认自然音色、voices 永不返回时超时兜底的回归用例；`InterviewSessionView.test.js` 将开场白在 Chrome voices 为空时的预期从立即播报更新为短暂等待后兜底播报。
- 问题原因：前几轮为了避免 Chrome 首次 TTS 无声，`prepareForUserGesture()` 会在用户点击开始通话时唤醒 `speechSynthesis` 并设置 `userGesturePrepared`，随后 `enqueue()` 看到该标记或 `allowDefaultVoice: true` 就会跳过 `waitForVoicesReady()` 立即创建 utterance。Chrome 此时经常还没有把 `speechSynthesis.getVoices()` 填充出来，`selectedVoice` 为空，代码只能设置 `utterance.lang = 'zh-CN'`，没有设置 `utterance.voice`，浏览器就使用系统默认 TTS 音色，听起来比程序默认的自然中文音色机械。
- 前端实现方案：保留 Chrome 首次点击手势内预热与系统默认音色兜底能力，但改变触发时机。只要当前 voices 列表为空，就进入既有 `waitForVoicesReady()` 的 800ms 有界等待；如果 `voiceschanged` 在窗口内返回，继续按程序默认评分选择自然中文音色并赋给 utterance；如果超时仍没有 voices，才带着 `allowDefaultVoice` 进入兜底播报，避免重新引入“开场白完全不播/卡住”的问题。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 结构，不改变语音偏好字段；离线 sherpa-onnx 仍只负责 STT，面试官播报仍依赖浏览器 `speechSynthesis`。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮 Chrome 默认音色丢失的根因、修复范围、验证结果和停止边界。
- 编译结果：先新增 RED 回归测试并确认旧实现失败于点击手势后立即 `speak()` 且 utterance 未携带 voice；补充 `allowDefaultVoice: true` 场景后确认旧逻辑仍会立即裸播系统默认音色。修复后 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js` 通过，1 个测试文件 / 23 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 77 个用例通过；语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 177 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：Chrome 点击开始语音通话时，如果默认中文自然音色在短时间内出现在 `getVoices()` 列表里，会优先使用程序配置的默认音色，不再直接落到系统默认机械音色；如果 Chrome 始终不返回 voices，仍会在有界等待后兜底播报，避免页面重新卡在 AI 正在回复。
- 停止说明：本轮只修复 Chrome 首次 TTS voices 加载延迟导致的默认音色丢失，不新增云端 TTS、不接入 Kokoro、不改后端、不上传音频、不扩展新的语音能力。

## 验收反馈修正：Chrome 语音播报看似开始但全程无声（2026-05-30）
- 当前任务所属模块：模拟面试前端语音通话 Chrome TTS 播报可靠性补强，处理 Chrome 中不仅开场白无声、后续 AI 返回文字也可能完全不出声，且页面把浏览器状态误判为“正在播报”从而阻塞后续收音的问题。
- 前端文件定位：`frontend/app/src/composables/useTextToSpeech.js`、`frontend/app/src/__tests__/composables/useTextToSpeech.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 本轮修改文件清单：`useTextToSpeech.js` 增加每条 utterance 的开始/结束事件上下文，保留 per-speak `onStart/onEnd` 回调；默认自然中文音色选择时提高本地系统音色权重、降低远程浏览器音色权重，避免 Chrome 优先选到可能依赖网络/浏览器语音服务的远程音色；将每条 utterance 的结束 watchdog 改为到达按文本长度估算的最大播报时长后主动 `cancel()` 并释放状态，即使 Chrome 持续报告 `speaking/pending` 也不再无限续期。`useTextToSpeech.test.js` 补充 Chrome 已触发 `onstart` 但永不 `onend/onerror`、默认优先本地中文音色、播报事件详情回调等回归用例。
- 问题原因：上一轮已覆盖 Chrome 接受 `speak()` 但没有真正 `onstart` 的分支；这次用户反馈“可能播报了但没有任何声音”对应另一条路径：Chrome 可能已经触发 `onstart`，甚至 `speechSynthesis.speaking/pending` 持续为 true，但真实音频链路无声且没有结束回调。旧结束 watchdog 在看到 `speaking/pending` 时会继续续期，导致 `isSpeaking` 长时间不释放，语音通话层就一直认为 AI 正在说话并阻止麦克风恢复。另一个风险是 Chrome 的 voices 列表可能优先暴露远程/浏览器音色，这类音色比 Windows 本地系统音色更容易受网络、语言包和浏览器策略影响而静音。
- 前端实现方案：继续保持“离线 sherpa-onnx 只负责 STT，面试官播报仍走浏览器 TTS”的边界；在 TTS 封装内把每条 utterance 的生命周期显式记录下来，`onstart` 只表示浏览器开始处理，不再作为“必然有声音”的充分条件。结束 watchdog 不再因为 Chrome 仍报告 `speaking/pending` 而无限续期，而是在 `getUtteranceTimeout()` 计算的安全时长后主动释放；短开场白最短 12 秒，长回答按文字长度延长，避免正常长播报被过早打断。默认音色评分优先本地中文音色，但用户选择“系统默认”或自定义音色时仍尊重原设置。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 结构，不改变语音偏好字段；只调整运行时音色评分和 TTS 状态机。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮 Chrome TTS 静音分支、修复范围、验证结果和停止边界。
- 编译结果：先新增 RED 回归测试并确认旧实现失败于远程音色优先、`onstart` 后无结束回调时不释放状态、播报事件详情未回调；修复后 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js` 通过，1 个测试文件 / 20 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 74 个用例通过；离线 STT 相关回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js` 通过，5 个测试文件 / 65 个用例通过；语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 174 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：Chrome 若选择了本地中文系统音色，实际出声概率会高于远程浏览器音色；如果 Chrome 仍然只是假装开始播报但没有声音、也不触发结束回调，页面会在按文本长度估算的播报窗口后释放“AI 正在回复/AI 正在说话”状态，不会一直卡住后续收音。真实能否出声仍取决于 Chrome 站点声音权限、系统输出设备、Windows 语音包、音量混音器和浏览器 `speechSynthesis` 可用性。
- 停止说明：本轮只修复 Chrome 浏览器 TTS 静音分支与状态释放，不新增云端 TTS、不接入 Kokoro、不改后端、不上传音频、不扩展新的语音能力。
## 验收反馈修正：开场白播报偶发不稳定（2026-05-30）
- 当前任务所属模块：模拟面试前端语音通话开场白 TTS 播报稳定化，修复 Chrome 偶发接受 `speechSynthesis.speak()` 但不开口、页面又把开场白提前标记为已播导致后续不再补播的问题。
- 前端文件定位：`frontend/app/src/composables/useTextToSpeech.js`、`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/src/__tests__/composables/useTextToSpeech.test.js`、`frontend/app/src/__tests__/views/InterviewSessionView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或面试消息生成逻辑修改。
- 本轮修改文件清单：`useTextToSpeech.js` 增加单次 utterance 的 `onStart/onEnd` 回调事件、`requireStartEvent` 启动确认、`runId` 过期回调防护，并调整默认音色评分优先本地中文语音；`InterviewSessionView.vue` 将 `openingSpeechPlayed` 从“调用 speak 前置 true”改为收到真实 `onstart` 后再确认，首轮 `start-timeout` 时最多补播一次；两个测试文件补充开场白被 Chrome 吞掉后重试、TTS 事件详情、旧 utterance 迟到回调不污染当前播报、本地中文音色优先等回归用例。
- 问题原因：上一轮已能在 Chrome 吞掉 utterance 后释放“AI 正在回复”，但会话页在调用 `textToSpeech.speak(openingContent)` 前就执行 `openingSpeechPlayed.value = true`。因此一旦浏览器接受了 `speak()` 却没有触发真实 `onstart`，TTS watchdog 只能结束当前播报状态，页面却认为开场白已经播过，不会再次播报，表现为“有时候开场白不播”。另一个竞态是旧 utterance 被取消后如果迟到触发 `onend`，可能误扣当前播报的 pending 状态。
- 前端实现方案：TTS 封装为每条 utterance 保存纯本地元数据和当前 `speechRunId`，`onstart/onend/onerror/timeout/start-timeout` 会带回 `reason/started/text/utterance`，且旧 run 的迟到回调直接忽略。开场白播报使用 `requireStartEvent`，只有收到浏览器 `onstart` 才设置 `openingSpeechPlayed = true`；若 5 秒内没有启动并触发 `start-timeout`，且仍在语音通话中，则自动补播一次。补播上限为 2 次，避免浏览器/系统 TTS 完全不可用时无限循环。普通 AI 回复播报不强制依赖 `onstart`，避免长播报被误取消。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 结构，不改变语音偏好字段；面试官播报仍使用浏览器 `speechSynthesis`，离线 sherpa-onnx 仍只负责 STT，不提供离线 TTS，不上传音频。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮开场白偶发不播的根因、修复范围、验证结果和停止边界。
- 编译结果：先新增 RED 回归测试并确认旧代码失败于 TTS 无单次回调、Chrome 吞掉开场白后不补播、旧 utterance 迟到回调污染当前播报；修复后 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，2 个测试文件 / 53 个用例通过；语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 175 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：Chrome 点击开始语音通话后，开场白只有在浏览器实际开始播报后才会记为已播；如果首个 utterance 被 Chrome 接受但没有真正启动，会自动补播一次，并且不会让旧回调误清掉当前播报状态。若站点声音权限、系统输出设备或浏览器 TTS 声音包不可用，页面仍会在有限重试后恢复通话状态，不会无限卡住。
- 停止说明：本轮只修复开场白 TTS 偶发不播和相关前端状态机竞态，不新增云端 TTS、不接入 Kokoro、不改后端、不上传音频、不扩展新的语音能力。

## 验收反馈修正：Chrome 开场白无声音且页面卡在 AI 正在回复（2026-05-30）
- 当前任务所属模块：模拟面试前端语音通话 Chrome TTS 播报状态稳定化，修复 Chrome 中点击开始通话后开场白没有声音、`speechSynthesis` 未触发结束回调时页面长期显示“AI 正在回复”并阻塞后续收音的问题。
- 前端文件定位：`frontend/app/src/composables/useTextToSpeech.js`、`frontend/app/src/__tests__/composables/useTextToSpeech.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 本轮修改文件清单：`useTextToSpeech.js` 调整首个空闲 utterance 的启动流程，只有存在正在播报/排队/缓冲的语音时才调用浏览器 `speechSynthesis.cancel()`；新增 utterance 启动 watchdog，Chrome 接受 `speak()` 但既不真正开始播报也不触发 `onend/onerror` 时，会在 5 秒内主动释放 TTS 播报状态。`useTextToSpeech.test.js` 增加两个回归用例，覆盖首个空闲播报前不取消浏览器语音队列，以及 Chrome 吞掉 utterance 后不再长期卡住。
- 问题原因：上一轮已把 TTS 预热放进“开始通话”的点击手势内，但 `speak()` 内部仍会先执行 `stop()`，从而对空闲的 `speechSynthesis` 调用一次 `cancel()` 后再立即 `speak()`。Chrome 对首轮用户手势、语音队列和 `cancel()/speak()` 同步序列更敏感，可能接受 utterance 但不实际发声，也不触发 `onend/onerror`。旧 watchdog 又会在浏览器持续报告 `speaking/pending` 时反复续期，最坏需要等全局 3 分钟卡死保护，页面表现就是开场白无声音且一直“AI 正在回复”。
- 前端实现方案：将“清理本地 TTS 状态”和“取消浏览器语音队列”拆开。普通首个空闲播报只清理本地 buffer/timer/runId，不调用 `cancel()`；如果已有活跃播报、排队语音或缓冲文本，仍保留原有取消逻辑，避免多段播报串音。新增 `onstart` 记录和 5 秒启动 watchdog，只针对没有历史活跃语音的首个 utterance 生效；一旦浏览器触发 `onstart/onend/onerror` 就清理 watchdog，正常长播报仍由原有按文本长度计算的结束 watchdog 和 10 秒 keep-alive 保护。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 结构，不改变语音偏好字段；面试官播报仍使用浏览器 `speechSynthesis`，离线 sherpa-onnx 仍只负责 STT，不提供离线 TTS。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮 Chrome TTS 卡死根因、修复范围、验证结果和停止边界。
- 编译结果：先新增 RED 回归测试并确认旧代码失败于首轮空闲播报前调用 `cancel()`、Chrome 吞掉 utterance 后 `isSpeaking` 不释放；修复后 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js` 通过，1 个测试文件 / 16 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 69 个用例通过；语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 169 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：Chrome 点击开始语音通话时，首个开场白不会再因为空闲状态下的 `cancel()` 被打断；如果 Chrome 仍因浏览器/系统策略吞掉本次 `speak()` 且没有任何回调，页面会在短时间内释放“AI 正在回复”状态并继续后续通话流程，不再长期卡死。真实是否能发声仍取决于 Chrome 站点声音权限、系统输出设备、浏览器 TTS 声音包和 `speechSynthesis` 可用性。
- 停止说明：本轮只修复 Chrome 浏览器 TTS 首轮播报启动和卡死释放，不新增云端 TTS、不接入 Kokoro、不改后端、不上传音频、不扩展新的语音能力。
## 验收反馈修正：离线引擎已就绪文案闪烁与面试准备耗时优化（2026-05-30）
- 当前任务所属模块：模拟面试前端语音面试启动体验优化，处理离线 sherpa-onnx 已下载后，“识别引擎：离线 sherpa-onnx 已就绪”在后台预热期间短暂闪烁，以及首次语音面试准备等待偏长的问题。
- 前端文件定位：`frontend/app/src/composables/useSpeechToText.js`、`frontend/app/src/composables/useVoiceCall.js`、`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/src/__tests__/composables/useSpeechToText.test.js`、`frontend/app/src/__tests__/composables/useVoiceCall.test.js`、`frontend/app/src/__tests__/views/InterviewSessionView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 本轮修改文件清单：`useSpeechToText.js` 保持离线模型已缓存时的“已就绪”文案稳定，后台 `prepareOfflineRecognition()` 预热 Worker 时不再把用户可见引擎状态切到 loading；`useVoiceCall.js` 将 AI 播报结束后的收音恢复尾音保护从 1.5 秒收敛到 0.8 秒，仍保留防回声保护；`InterviewSessionView.vue` 将开场白 pending 轮询从首轮固定 3 秒等待改为前 6 轮 500ms 快速探测，之后回到 3 秒常规轮询，总超时边界保持不变。
- 问题原因：当前“已就绪”文案有两层含义：模型资源已缓存、Worker 已预热。旧预热实现会在模型资源 ready 后把引擎状态短暂切到 `offline-loading`，造成视觉上像“已就绪文字消失一下再出现”。准备耗时则不只来自离线模型：首次语音面试如果需要等待开场白生成，旧前端最早 3 秒后才再次查询；开场白播报结束后还固定等待 1.5 秒再恢复收音，导致用户感觉“面试准备中”偏长。
- 前端实现方案：后台预热只作为内存预加载，不改变用户可见“离线 sherpa-onnx 已就绪”状态，避免状态闪烁；开场白生成轮询先用 500ms 快速探测覆盖常见的短生成场景，避免后端已生成但前端仍空等 3 秒；TTS 结束后的麦克风恢复延迟保留但缩短到 0.8 秒，配合离线 `getUserMedia` 已启用的回声消除、降噪和自动增益，减少播报后进入聆听的等待。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 结构，不改变离线模型 manifest/runtime 状态格式；用户音频仍只在浏览器本地 AudioContext 与 Worker 中处理，不上传服务器。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮根因、优化范围、验证结果和停止边界。
- 编译结果：先补充 RED 回归测试并确认旧代码失败于预热状态闪烁、TTS 结束后 0.8 秒未恢复收音、开场白 pending 首轮 500ms 未快速查询；修复后 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 92 个用例通过；语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 169 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：离线模型已缓存时，语音面试页后台预热 Worker 不再让“识别引擎：离线 sherpa-onnx 已就绪”文案短暂跳变；新会话开场白 pending 时，前几轮会更快拉取结果；AI 开场白播报结束后，恢复收音等待从 1.5 秒缩短到 0.8 秒。首次点击时若浏览器还没有完成麦克风授权或离线 Worker 仍未预热完，仍会存在真实等待，这是浏览器权限和本地模型加载边界。
- 停止说明：本轮只优化前端语音面试启动等待、开场白轮询节奏和离线引擎状态文案稳定性；不新增后端 API、不改数据库、不上传用户音频、不替换模型包、不推进新的语音能力。

## 验收反馈修正：Chrome 下载离线引擎后仍走浏览器/系统识别（2026-05-30）
- 当前任务所属模块：模拟面试前端离线语音识别偏好联动，修复 Chrome 中离线 sherpa-onnx 已下载后，会话页仍显示“识别引擎：浏览器/系统识别服务”并继续走 Chrome Web Speech 的问题。
- 前端文件定位：`frontend/app/src/views/settings/SettingsView.vue`、`frontend/app/src/__tests__/views/SettingsView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 本轮修改文件清单：`SettingsView.vue` 在离线 STT 下载成功后同步保存 `voiceRecognitionEngine: 'offline_sherpa'` 与 `offlineSttEngine: 'sherpa_onnx'`；`SettingsView.test.js` 增加从 `system_local` 下载离线资源包后自动切换到离线识别偏好的回归测试。
- 问题原因：离线模型缓存状态和面试识别引擎偏好是两条独立链路。旧设置页只执行 `downloadModelFromManifest()` 并把模型状态改为 ready，没有更新 `voiceRecognitionEngine`；而会话页只在偏好等于 `offline_sherpa` 时才向 `useSpeechToText()` 传入 `preferOffline: true`。因此用户即使下载了离线包，只要偏好仍是默认 `system_local`，Chrome 仍会显示并使用“浏览器/系统识别服务”。Chrome 的浏览器/系统识别依赖 Web Speech 实现、系统/浏览器语言包、网络服务、权限和浏览器策略，和 sherpa-onnx 离线包不是同一个引擎；离线 STT 也不提供离线 TTS，面试官播报仍使用浏览器 `speechSynthesis`。
- 前端实现方案：离线 STT 下载成功后立即复用现有 `saveSettingsPreferences()` 与 `syncPreferenceForms()` 保存离线识别偏好，并把成功提示改为“后续语音面试将优先使用离线引擎”。下载失败路径不修改偏好，避免模型未 ready 时强制切到不可用的离线路径。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 结构；只写入既有本地偏好字段 `voiceRecognitionEngine` 与 `offlineSttEngine`。用户音频仍只在浏览器本地处理，不上传服务器。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮根因、修复范围、验证结果和停止边界。
- 编译结果：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 35 个用例通过；`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/utils/settingsPreferences.test.js` 通过，3 个测试文件 / 70 个用例通过；语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 164 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：设置中心“离线增强”下载 sherpa-onnx 成功后，当前浏览器的后续语音面试会优先使用离线 sherpa-onnx；会话页不再因为偏好仍停留在 `system_local` 而继续显示“浏览器/系统识别服务”。若用户后续在设置中重置语音偏好，仍会按既有默认值回到浏览器/系统识别。
- 停止说明：本轮只修复离线包下载成功后的识别偏好联动和说明，不新增云端 STT/TTS、不接入 Kokoro、不改模型托管、不上传音频、不扩展新的语音能力。
## 验收反馈修正：离线 sherpa-onnx 收音前准备时间过长（2026-05-30）
- 当前任务所属模块：模拟面试前端离线语音识别启动体验稳定化，修复离线资源包已下载后，通话中仍反复从“识别引擎：离线 sherpa-onnx 已就绪”刷新到等待数秒才进入“正在聆听”的问题。
- 前端文件定位：`frontend/app/src/composables/useSpeechToText.js`、`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js`、`frontend/app/voice-models-local/sherpa-onnx/zh-cn-streaming/runtime.js`、`frontend/app/src/__tests__/composables/useSpeechToText.test.js`、`frontend/app/src/__tests__/views/InterviewSessionView.test.js`、`frontend/app/src/__tests__/utils/sherpaRuntimeAsset.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 问题原因：旧实现把离线 Worker 当作每轮收音资源处理，`stop()` 收到 final 或 stopped 后会 `terminate()` Worker；下一轮恢复收音时又重新创建 Worker、重新加载 runtime、WASM、data 和 ONNX 模型，所以页面会短暂回到离线模型启动/准备状态。即使模型包已经下载完成，“已就绪”也只代表缓存存在，旧链路没有把已加载进内存的 sherpa Worker 复用起来。另一个隐患是 runtime 的 `stop()` 对当前 stream 调用 `inputFinished()` 后，下一轮如果复用同一个 stream 可能继承已结束状态。
- 前端实现方案：`useSpeechToText.js` 拆分“本轮麦克风/AudioContext 资源”和“已加载 sherpa Worker/模型资源”的生命周期：正常 `stop()` 只释放麦克风链路和音频节点，不再销毁已 ready 的 Worker；后续收音如果 runtime URL 未变化，直接向同一个 Worker 发送 `start`，避免重新初始化模型。`cancel()`、页面卸载、Worker 错误、音频链路不可用以及 stop flush 超时仍会销毁 Worker，防止坏状态常驻。新增 `prepareOfflineRecognition()` 只预热离线 Worker/模型，不申请麦克风；`InterviewSessionView.vue` 仅对语音面试的离线 STT 实例开启后台预热，普通文本输入麦克风不预热，避免额外加载一份大模型 Worker。runtime 适配层在每轮 `start()` 时创建新的 sherpa online stream，避免上一轮 `inputFinished()` 影响下一轮识别。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 结构，不改变模型文件清单格式；用户音频仍只在浏览器本地 AudioContext 和 Worker 中处理，不上传服务器。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮根因、修复范围、验证结果和停止边界。
- 编译结果：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 163 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：语音面试页面加载到离线会话后会后台预热 sherpa Worker/模型，点击开始通话时不再把模型初始化时间算进收音准备；同一通话多轮收音会复用已 ready 的离线 Worker，只重新创建麦克风链路和 sherpa stream。首次进入页面时后台预热仍可能占用少量 CPU/内存，但不会弹麦克风权限，也不会提前录音。
- 停止说明：本轮只修复离线 sherpa-onnx 已下载后的准备时间过长、Worker 重复初始化和 stream 复用边界；不新增后端 API、不上传用户音频、不替换模型包、不推进新的语音能力。

## 验收反馈修正：离线 sherpa-onnx 模型已启动但仍不出字（2026-05-30）
- 当前任务所属模块：模拟面试前端离线语音识别稳定化，修复浏览器控制台已出现 sherpa-onnx `GetOnlineRecognizerConfig` 和 zipformer 模型初始化日志后，页面仍长时间“正在聆听”但不识别用户说话的问题。
- 前端文件定位：`frontend/app/src/composables/useSpeechToText.js`、`frontend/app/src/workers/sherpaSpeechWorker.js`、`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js`、`frontend/app/voice-models-local/sherpa-onnx/zh-cn-streaming/runtime.js`、`frontend/app/src/__tests__/composables/useSpeechToText.test.js`、`frontend/app/src/__tests__/workers/sherpaSpeechWorker.test.js`、`frontend/app/src/__tests__/utils/sherpaRuntimeAsset.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 问题原因：用户提供的新日志证明 sherpa WASM、data 包和 transducer 模型已经初始化成功，问题不再是资源下载或 Worker 启动失败。旧实现还有三处会导致“模型已启动但不出字”：一是 runtime URL 只使用模型包版本作为缓存参数，前端 runtime 适配层修复后浏览器仍可能加载旧脚本；二是离线 PCM 音频帧只投给 Worker，通话层主要依赖 analyser 的 `voiceActivityAt` 触发静音 flush，如果 analyser 阈值没有触发，就会一直聆听但不调用 `stop()` 刷出 final；三是 Worker `stop()` 无文本时不回任何确认，主线程只能等短超时，容易在低性能机器上提前或卡住。控制台里的 `LanguageDetector` 是 Edge 内置能力提示，`ScriptProcessorNode deprecated` 是浏览器弃用警告，不是本轮离线识别失败的根因；大量 `VM7 ... debug=True` 是 sherpa C++ debug 日志，不是 JS 异常。
- 前端实现方案：`useSpeechToText.js` 将 runtime URL 的版本参数改为“模型包版本 + 前端适配层版本”，强制浏览器刷新已修复的 runtime；离线 `onaudioprocess` 对 PCM 样本计算 RMS，直接更新 `isVoiceActive` 与 `voiceActivityAt`，让 `useVoiceCall` 可以在收到真实麦克风音频但没有 partial 的场景按静音规则触发 `speech.stop()`；离线 stop flush 超时延长到 5 秒，并新增处理 Worker `stopped` 确认，避免无文本 stop 没有回包导致主线程长期等待。`sherpaSpeechWorker.js` 在 stop 无文本时发送 `{ type: 'stopped' }`。runtime 默认显式传入 sherpa 在线识别配置并设置 `debug: 0`，避免 C++ 初始化日志刷屏被误认为报错。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 结构，不改变模型文件清单格式；用户音频仍只在浏览器本地 AudioContext 和 Worker 中处理，不上传服务器。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮根因、修复范围、验证结果和停止边界。
- 编译结果：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 157 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：离线模型初始化成功后，真实 PCM 麦克风输入会同时送入 sherpa Worker 和通话静音判断；即使 sherpa 不返回 partial，也能在静音后触发 stop flush。若 stop 后仍完全没有文本，会明确进入 `offline-no-transcript`，不再无限“正在聆听”。刷新后 runtime 会加载关闭 debug 日志的新适配层。
- 停止说明：本轮只修复离线 STT 已启动但不出字、stop flush 无确认和 runtime 缓存版本问题；不新增后端 API、不上传用户音频、不替换模型包、不推进新的语音能力。
## 验收反馈修正：Chrome 语音通话首轮无播报且无法收音（2026-05-30）
- 当前任务所属模块：模拟面试前端语音通话 Chrome 首轮 TTS/STT 启动稳定化，修复 Edge 正常但 Chrome 无法听到面试官播报、离线 sherpa-onnx 已安装后仍无法语音识别的问题。
- 前端文件定位：`frontend/app/src/composables/useTextToSpeech.js`、`frontend/app/src/composables/useSpeechToText.js`、`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/src/__tests__/composables/useTextToSpeech.test.js`、`frontend/app/src/__tests__/composables/useSpeechToText.test.js`、`frontend/app/src/__tests__/views/InterviewSessionView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 本轮修改文件清单：`useTextToSpeech.js` 增加用户手势内 TTS 预热能力，Chrome voices 尚未返回时允许首轮直接使用系统默认音色播报；`InterviewSessionView.vue` 在点击“开始通话”时先预热 TTS 再启动通话；`useSpeechToText.js` 将离线 STT 的 `AudioContext({ sampleRate: 16000 })` 创建和 `resume()` 前移到麦克风授权前，确保仍处于 Chrome 点击手势链路内；相关测试补充 Chrome voices 未加载、首轮开场白立即播报、离线音频上下文在授权前恢复的回归用例。
- 问题原因：Edge 对浏览器语音合成和音频上下文启动更宽松，Chrome 更严格要求首次 `speechSynthesis.speak()` / `AudioContext.resume()` 落在用户点击手势链路内。旧实现会在 Chrome voices 为空时等待 `voiceschanged`，导致开场白没有立即 `speak()`，通话又因为 TTS 状态等待而不恢复收音；离线 sherpa-onnx 路径则等 `getUserMedia` 授权完成后才创建并恢复 `AudioContext`，在 Chrome 中可能已经错过用户手势窗口，即使模型包下载成功也没有稳定 PCM 输入。
- 前端实现方案：开始通话按钮先调用 `textToSpeech.prepareForUserGesture()`，在同一次用户点击中 `resume()` 合成器并标记本轮播报可直接使用默认音色；随后开场白 `speak()` 不再因 Chrome voices 未加载而延迟。离线 STT 启动时先创建 16k 音频上下文并在 suspended 时立即 `resume()`，再申请麦克风并挂载 analyser / ScriptProcessor，保持原有音频处理和 Worker 协议不变。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 结构，不改变离线模型 manifest/runtime 状态格式；用户音频仍只在浏览器本地 Worker 中处理，不上传服务器。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮 Chrome 语音启动根因、修复范围、验证结果和停止边界。
- 编译结果：`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 74 个用例通过；语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 150 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：Chrome 点击开始语音通话时，开场白会在 voices 尚未异步加载完成的情况下直接触发系统默认 TTS 播报；离线 sherpa-onnx 启动时音频上下文会在点击手势内预热，降低 Chrome 中模型 ready 但无实际收音的问题。仍建议在 Chrome 中确认站点麦克风权限为允许，并在设置页删除后重新下载离线包以清掉旧缓存资源。
- 停止说明：本轮只修复 Chrome 首轮语音播报/离线收音启动时序，不新增云端 STT/TTS、不改模型托管、不上传音频、不扩展新的面试能力。

## 验收反馈修正：离线 sherpa-onnx 已安装但仍提示不可用（2026-05-30）
- 当前任务所属模块：模拟面试前端离线语音识别稳定化，修复已下载 sherpa-onnx 离线语音包后仍显示“识别引擎：不可用，建议下载离线语音包”和“等待继续收音”的问题。
- 前端文件定位：`frontend/app/src/composables/useSpeechToText.js`、`frontend/app/src/composables/useVoiceCall.js`、`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/src/__tests__/composables/useSpeechToText.test.js`、`frontend/app/src/__tests__/composables/useVoiceCall.test.js`、`frontend/app/src/__tests__/views/InterviewSessionView.test.js`、`frontend/app/src/__tests__/workers/sherpaSpeechWorker.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 本轮修改文件清单：`useSpeechToText.js` 调整离线 no-transcript 判定、离线错误状态和 stop flush 后无文本错误；`useVoiceCall.js` 让离线 ready 且有麦克风活动时也能触发 stop flush，并增加自动发送重入保护；`InterviewSessionView.vue` 增加离线 sherpa 异常文案；相关测试补充离线已安装但异常、不再建议下载、final-only flush 自动发送的回归用例。
- 问题原因：离线 sherpa-onnx 与浏览器 Web Speech 的出字节奏不同，可能先持续收 PCM 音频但不返回 partial，直到 endpoint 或 `stop()` 后才给 final。旧实现复用了浏览器路径的 6 秒 `no-transcript` 计时器，导致离线已安装时被误判为浏览器识别不可用；通话编排又只在 `pendingMessage` 非空时触发 stop flush，因此有麦克风活动但没有 partial 文本时不会主动让 sherpa 输出 final，最终页面进入“等待继续收音”并提示“建议下载离线语音包”。
- 前端实现方案：离线模式继续保留 2 秒无 PCM 帧的 `offline-audio-unavailable` 检查，但关闭“有音量无文字”的通用 no-transcript 降级。离线 Worker 错误、音频处理器不可用、stop flush 后仍无文本统一归类到 `offline-error`，并设置 `offlineEngineSuggested=false`，避免已安装资源包后重复建议下载。`useVoiceCall` 在离线 ready 且有 `voiceActivityAt` 时，即使 `pendingMessage` 为空也按静音超时调用 `speech.stop()`，等待 final 回填后发送。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 结构，不改变离线模型 manifest/runtime 状态格式；用户音频仍只在浏览器本地 Worker 中处理，不上传服务器。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮根因、修复范围、验证结果和停止边界。
- 编译结果：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，7 个测试文件 / 139 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：离线资源包已经安装时，识别失败会显示“识别引擎：离线 sherpa-onnx 异常”，不会再提示“不可用，建议下载离线语音包”；用户说完但 sherpa 只在 stop 后返回 final 的场景，也会在静音超时后自动 flush 并发送。
- 停止说明：本轮只修复当前离线 STT 已安装后的错误分类和自动 flush，不继续扩展后端模型托管、云端 STT/TTS、Kokoro 下载或新的面试能力。

## 验收反馈修正：离线 sherpa-onnx ready 后真实音频链路校验（2026-05-30）
- 当前任务所属模块：模拟面试前端离线语音识别稳定化，修复下载离线 sherpa-onnx 资源包后页面显示已就绪/正在聆听但实际不识别用户说话的问题。
- 前端文件定位：`frontend/app/src/composables/useSpeechToText.js`、`frontend/app/src/workers/sherpaSpeechWorker.js`、`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/src/__tests__/composables/useSpeechToText.test.js`、`frontend/app/src/__tests__/workers/sherpaSpeechWorker.test.js`、`frontend/app/src/__tests__/views/InterviewSessionView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 本轮修改文件清单：`useSpeechToText.js` 修正离线 `getUserMedia` 约束、16k `AudioContext`、suspended resume、Worker ready 后连接 `ScriptProcessor` 和无音频帧错误出口；`sherpaSpeechWorker.js` 增加 recognizer 初始化 Promise，避免 init 未完成时丢弃 `start/audio/stop`；`InterviewSessionView.vue` 将“正在聆听”恢复为只依赖真实录音状态；补充 composable、worker 和会话页回归测试。
- 问题原因：旧实现把离线 Worker 返回 `ready` 当成端到端收音成功，但 `ready` 只证明 sherpa WASM/模型资源加载成功，不证明浏览器 AudioContext 已运行，也不证明 ScriptProcessor 已产出 PCM 音频块。旧代码在 Worker ready 前就发送 `start` 并置为录音中，Worker 初始化期间早到的消息可能被忽略；页面又把 `offline-ready` 直接显示为“正在聆听”，所以模型包下载成功后会出现假收音状态。
- 前端实现方案：离线模式按 sherpa 浏览器示例申请带 `echoCancellation`、`noiseSuppression`、`autoGainControl` 的麦克风流，创建 `AudioContext({ sampleRate: 16000 })` 并在 suspended 时 resume。Worker `ready` 后才连接离线音频节点、发送 `start`、设置 `isRecording=true`；如果 `createScriptProcessor` 不可用或开始后 2 秒没有 `onaudioprocess` 音频帧，设置 `offline-audio-unavailable`，提示用户离线识别没有收到麦克风音频。Worker 内部通过 `recognizerReadyPromise` 保证 `start/audio/stop` 等待 recognizer 初始化完成再执行。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 结构，不改变离线模型 manifest/runtime 状态格式；用户音频仍只在浏览器本地 Worker 中处理，不上传服务器。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮根因、修复范围、验证结果和停止边界。
- 编译结果：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 54 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 72 个用例通过；计划内语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，6 个测试文件 / 131 个用例通过；补充含 Worker 的整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，7 个测试文件 / 132 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：离线 sherpa-onnx 已就绪时，页面仍会显示“识别引擎：离线 sherpa-onnx 已就绪”，但只有真实进入 `isRecording` 后才显示“正在聆听”；如果浏览器没有把麦克风 PCM 帧送进离线链路，会明确提示 `offline-audio-unavailable`，不会一直停在假监听状态。真实麦克风识别效果仍需要在已缓存模型、已授权麦克风的浏览器中人工复测。
- 停止说明：本轮只修复离线 STT 音频输入链路和状态展示，不继续扩展后端模型托管、云端 STT/TTS、Kokoro 下载或新的面试能力。

## 验收反馈修正：离线语音通话准备态与离线增强排版（2026-05-30）
- 当前任务所属模块：模拟面试前端离线语音通话稳定化与设置中心离线增强资源包管理。
- 前端文件定位：`frontend/app/src/composables/useVoiceCall.js`、`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/src/views/settings/SettingsView.vue`、`frontend/app/src/__tests__/composables/useVoiceCall.test.js`、`frontend/app/src/__tests__/views/InterviewSessionView.test.js`、`frontend/app/src/__tests__/views/SettingsView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 本轮修改文件清单：`useVoiceCall.js` 调整语音通话待发送文本聚合；`InterviewSessionView.vue` 修正离线 worker ready 后的通话标题；`SettingsView.vue` 重排离线增强资源卡操作区；三个测试文件补充离线 interim 自动提交、离线 ready 不显示准备中、删除资源包不挤占状态网格的回归用例。
- 问题原因：离线 sherpa-onnx 识别器在端点确认前通常先返回 partial/interim 片段，旧通话编排只在 `finalTranscript` 变化时拼接 `pendingMessage`，所以说完后可能没有可提交文本，静音自动发送无法触发，用户看到“识别引擎：离线 sherpa-onnx 已就绪”但主标题仍停在“通话准备中”。设置页视觉问题来自删除按钮被放进 `.offline-model-status` 两列状态网格，下载成功后多出的按钮改变了网格内容密度和列宽。
- 前端实现方案：`useVoiceCall` 新增 final/interim 两段状态，interim 先参与 `pendingMessage` 展示和静音自动提交；自动发送前仍会调用 `speech.stop()` 等待离线 worker flush，final 到达时清空 interim 并接管最终文本，避免重复拼接。面试页在 `voiceSttEngineStatus === 'offline-ready'` 时把通话标题视为“正在聆听”，不再继续显示准备态。设置页把下载/删除按钮移动到独立 `.offline-model-actions`，状态网格只保留“缓存能力/模型状态”等短状态项，移动端改为纵向排列。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 结构，不改变离线模型 manifest/runtime 状态格式；用户音频仍只在浏览器本地 Worker 中处理，不上传服务器。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮根因、修复范围、验证结果和停止边界。
- 编译结果：`npm.cmd test -- --run src/__tests__/composables/useVoiceCall.test.js` 通过，1 个测试文件 / 19 个用例通过；`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，1 个测试文件 / 25 个用例通过；`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 34 个用例通过；语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，6 个测试文件 / 126 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：下载并缓存离线 sherpa-onnx 后，开始语音通话不再出现“离线已就绪但一直通话准备中”的状态；离线识别只有 interim 片段时也能进入待发送文本并在静音后自动提交；设置中心离线增强卡片在显示“删除资源包”后仍保持状态项和按钮分区清晰。
- 停止说明：本轮只修复当前离线 STT 通话准备态、自动提交和离线增强页排版，不继续扩展后端模型托管、云端 STT/TTS、Kokoro 下载或新的面试能力。

## 回归修复：语音识别中断不重置通话状态（2026-05-29）
- 当前任务所属模块：模拟面试前端语音通话稳定化，修复语音面试进行多轮后说完话时偶发回到“开始通话”的状态重置问题。
- 前端文件定位：`frontend/app/src/composables/useVoiceCall.js`、`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/src/__tests__/composables/useVoiceCall.test.js`、`frontend/app/src/__tests__/views/InterviewSessionView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 本轮修改文件清单：`useVoiceCall.js` 增加可恢复 STT 错误码白名单；`InterviewSessionView.vue` 将 `useSpeechToText` 返回的 `errorCode` 传入语音通话编排；两个测试文件补充语音识别 `no-transcript` 中断时不退出通话、不回到“开始通话”的回归用例。
- 问题原因：浏览器 Web Speech 在长时间或多轮识别后可能抛出 `network`、`no-speech`、`no-transcript`、`end-without-result` 这类识别中断。旧通话编排只要监听到 `speech.error` 就调用 `endVoiceCall()`，导致语音模式被关闭、通话计时和麦克风状态清空，界面看起来像页面刷新后回到未通话状态。
- 前端实现方案：将 `network`、`no-speech`、`no-transcript`、`end-without-result` 视为可恢复识别中断，保留 `isVoiceMode`、已识别的 `pendingMessage` 和当前通话界面，并进入“等待继续收音”状态；用户可点击继续收音，或在已有识别文本时点击“停止收听并发送”。麦克风权限拒绝、浏览器不支持等致命错误仍按原逻辑退出语音模式并降级为手动输入。
- 数据存储方案：不新增本地存储、接口字段或数据库字段；不上传用户音频；不修改离线模型缓存结构。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮根因、修复范围和验证结果。
- 编译结果：`npm.cmd test -- --run src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，2 个测试文件 / 42 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：语音面试中如果浏览器识别服务在用户说完后短暂中断，页面不再把通话重置为“开始通话”；已有识别文本保留，通话浮层保持可操作。真正的权限拒绝或浏览器能力缺失仍会明确降级。
- 停止说明：本轮只修复 STT 可恢复中断导致的语音通话状态重置，不继续扩展云端 STT/TTS、后端代理、离线模型部署或新的面试能力。

## 验收反馈修正：离线模型下载 Failed to fetch 同源化（2026-05-27）
- 当前任务所属模块：模拟面试前端离线语音识别资源下载、设置中心资源包管理与模型部署脚本。
- 前端文件定位：`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json`、`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js`、`frontend/app/scripts/download-sherpa-onnx-model.mjs`、`frontend/app/src/utils/offlineVoiceModelCache.js`、`frontend/app/src/views/settings/SettingsView.vue`、`frontend/app/.gitignore` 以及对应测试。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或用户音频上传链路修改。
- 本轮修改文件清单：`manifest.json` 改为同源模型文件路径，不再让浏览器跨域请求 Hugging Face；`runtime.js` 的模型基地址同步改为 `/voice-models/sherpa-onnx/zh-cn-streaming/`；新增 `voice:model:download` 脚本，可把官方 sherpa-onnx 浏览器资源下载到前端静态目录，并支持 `SHERPA_ONNX_MODEL_BASE_URL` 指定镜像源；`offlineVoiceModelCache.js` 对模型文件请求失败和 HTML fallback 增加明确错误，并写入 failed 状态；`.gitignore` 忽略四个大体积模型文件，避免误提交；设置页删除资源包按钮改用 Element Plus，避免 Naive 测试主题缺失导致删除入口渲染失败。
- 问题原因：浏览器直接从 Hugging Face 拉取大体积模型文件会受跨域、网络、地区或镜像可用性影响，失败时表现为 `Failed to fetch`。离线模型必须通过同源静态资源或自有 CDN 提供，否则前端无法保证下载可用。
- 前端实现方案：前端下载入口只请求同源 `/voice-models/sherpa-onnx/zh-cn-streaming/*` 文件；如果文件缺失或被 SPA fallback 返回 HTML，会提示“请先将 xxx 部署到同源静态目录”，不会再缓存错误 HTML 为 ready。部署侧可先运行 `npm.cmd run voice:model:download` 下载模型资源，若默认源不可达，可设置 `SHERPA_ONNX_MODEL_BASE_URL` 指向可访问镜像后重试。
- 数据存储方案：继续使用浏览器 Cache API 缓存模型文件，状态元数据保存在 localStorage；不新增后端存储，不上传用户音频；四个大文件不纳入 Git 源码管理。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮同源化下载、脚本和验证结果。
- 编译结果：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，6 个测试文件 / 119 个用例通过。
- 构建结果：`npm.cmd run build` 通过，仍保留既有 `@vueuse/core` PURE annotation 构建提示。
- 当前功能验收说明：用户端不会再跨域请求 Hugging Face 模型文件；如果同源模型文件尚未部署，会得到明确部署提示并保留删除资源包入口。当前环境尝试默认 Hugging Face 下载超时，尝试 `hf-mirror.com/spaces/...` 返回 404，因此需要在可访问官方源或自有镜像的环境中运行下载脚本。
- 停止说明：本轮只修复离线 STT 资源下载同源化与错误提示，不继续新增后端模型代理、提交大体积模型文件或接入 Kokoro。

## 验收反馈修正：离线语音资源包删除入口（2026-05-26）
- 当前任务所属模块：模拟面试前端语音设置中心，补齐用户对已下载离线语音资源包的自主删除能力。
- 前端文件定位：`frontend/app/src/views/settings/SettingsView.vue`、`frontend/app/src/__tests__/views/SettingsView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 本轮修改文件清单：`SettingsView.vue` 将离线 STT 的“清除模型缓存”改为明确的“删除资源包”，新增删除确认弹窗、删除中状态和失败残留可删除条件；同时为已有 `tts:kokoro` 本地状态提供“删除音色包”入口，但不启用 Kokoro 下载；`SettingsView.test.js` 覆盖已安装资源包删除、确认弹窗参数、下载失败残留删除入口、Kokoro 缓存删除和取消时不清理。
- 问题原因：离线语音资源包会占用当前浏览器本地 Cache API 存储，用户应能明确看到删除入口并自主释放空间。旧入口只在 STT `ready` 状态展示“清除模型缓存”，文案不够直观，下载失败后的残留状态没有清理按钮；TTS 状态如果已有缓存也只显示“已缓存”，没有删除入口。
- 前端实现方案：新增 `canClearOfflineSttModel` 和 `canClearOfflineTtsModel`，当资源状态为 `ready` 或 `failed` 时展示红色删除按钮；STT 删除调用 `clearModelCache('stt:sherpa_onnx:zh_cn')`，TTS 删除调用 `clearModelCache('tts:kokoro')`；两者点击后均使用 `ElMessageBox.confirm` 二次确认，取消时不做任何清理。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 数据结构；仍复用既有 `clearModelCache` 清理浏览器本地模型文件和状态。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮用户自主删除资源包能力和验证结果。
- 编译结果：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 30 个用例通过。
- 构建结果：`npm.cmd run build` 通过，仍保留既有 `@vueuse/core` PURE annotation 构建提示。
- 当前功能验收说明：用户下载离线语音识别模型后，设置中心会显示“删除资源包”；下载失败但留下状态时也可主动删除；如果浏览器里已有 Kokoro 本地状态，也会显示“删除音色包”；删除前有确认弹窗，取消不会清理本地模型。
- 停止说明：本轮只补齐离线资源包删除入口，不继续推进 Kokoro 下载、后端模型托管、真实模型文件部署或新的语音交互能力。

## 回归修复：浏览器语音识别启动顺序与开场白延迟开麦（2026-05-26）
- 当前任务所属模块：模拟面试前端语音通话稳定化，修复 Web Speech 启动前抢占麦克风、语音面试开场白与收音同时启动导致互相取消的问题。
- 前端文件定位：`frontend/app/src/composables/useSpeechToText.js`、`frontend/app/src/composables/useVoiceCall.js`、`frontend/app/src/views/interview/InterviewSessionView.vue` 以及对应单元测试。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 本轮修改文件清单：`useSpeechToText.js` 调整浏览器 Web Speech 启动顺序；`useVoiceCall.js` 支持首轮暂不开麦进入通话态；`InterviewSessionView.vue` 在有开场白时延迟首轮收音；`useSpeechToText.test.js`、`useVoiceCall.test.js`、`InterviewSessionView.test.js` 补充和更新回归用例。
- 问题原因：浏览器 Web Speech 路径原先先执行自定义 `getUserMedia` 音量监测，再创建并启动 `SpeechRecognition`，这会把麦克风授权和设备占用提前交给辅助监测链路，可能与浏览器原生识别的麦克风申请互相影响；语音通话开始后又立即启动 STT，再马上为了播报开场白调用取消，形成启动/取消竞态。
- 前端实现方案：浏览器 Web Speech 路径改为先完成本地能力探测、创建 `SpeechRecognition` 并调用 `recognition.start()`，确认浏览器识别已启动后再异步开启可选音量监测；音量监测失败不再把浏览器识别判定为失败。离线 sherpa-onnx 路径仍保留原有 `getUserMedia` 先行逻辑，因为离线 Worker 需要 PCM 音频流。
- 语音通话接入方案：`startVoiceCall({ startListening: false })` 允许页面先进入通话态但暂不开麦；会话页检测到首轮开场白可播报时使用该模式，TTS 播报结束后继续复用 `onEnd -> resumeListening()` 恢复收音。若开场白是在通话启动后才生成，播报前仍会在当前正在录音时取消 STT，避免扬声器声音被识别为用户回答。
- 数据存储方案：不新增本地存储、接口字段或数据库字段；不上传用户音频；不修改离线模型缓存结构。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮回归原因、修复范围和验证结果。
- 编译结果：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，6 个测试文件 / 107 个用例通过。
- 构建结果：`npm.cmd run build` 通过，仍保留既有 `@vueuse/core` PURE annotation 构建提示。
- 当前功能验收说明：点击文字语音输入或语音面试开始通话时，浏览器原生识别会先获得启动机会；辅助音量监测失败不会导致 Web Speech 直接不可用。新语音面试有开场白时不会先开麦再立刻取消，开场白播完后再开始收音，避免卡在正在回复或收音状态。
- 停止说明：本轮只修复浏览器原生 STT 启动顺序和开场白收音竞态，不继续推进 Kokoro、后端模型托管、真实模型文件部署或新的语音交互能力。

## 回归修复：语音识别恢复浏览器/系统优先尝试（2026-05-26）
- 当前任务所属模块：模拟面试前端语音通话稳定化，修复上轮将 STT 默认强制到离线模型导致未安装模型时直接提示下载、无法先尝试客户端/浏览器识别的问题。
- 前端文件定位：`frontend/app/src/composables/useSpeechToText.js`、`frontend/app/src/composables/useVoiceCall.js`、`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/src/views/settings/SettingsView.vue`、`frontend/app/src/utils/settingsPreferences.js` 以及对应单元测试。
- 后端文件定位：本轮不新增后端 API，不修改数据库字段，不修改现有面试消息接口。
- 本轮修改文件清单：`useSpeechToText.js` 改为离线模型已缓存时优先 sherpa-onnx，离线模型缺失但浏览器 Web Speech 可用时先启动浏览器/系统识别，只有两者都不可用才提示下载离线模型；`useVoiceCall.js` 不再因为 `offline-missing` 直接阻止语音通话；`settingsPreferences.js` 将默认识别偏好恢复为 `system_local`；`SettingsView.vue` 将下载入口文案改为兜底增强而非必选前置；`InterviewSessionView.vue` 更新识别引擎状态文案。
- 问题原因：`preferOffline` 默认开启后，旧逻辑在模型状态不是 `ready` 时直接设置 `offline-missing` 并返回，导致浏览器内置 Web Speech 和可用的系统语音识别都没有机会启动；语音通话编排层又把 `isModelReady === false` 当作硬阻断，进一步放大了这个问题。
- 前端实现方案：保留真实离线 sherpa-onnx 作为已安装时的优先路径；未安装时先走 `SpeechRecognition`，仍按现有逻辑尝试 `SpeechRecognition.available({ processLocally: true })` 和 `processLocally`，失败后自动使用浏览器识别服务；如果浏览器识别报 `network`、`no-speech`、`service-not-allowed` 等错误，再建议用户下载离线语音识别模型。
- 数据存储方案：不新增后端存储；本轮仅调整本地设置默认值和离线模型缓存状态读取逻辑，用户音频仍只在浏览器本地用于识别链路。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮回归原因、修复范围和验证结果。
- 编译结果：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，6 个测试文件 / 104 个用例通过。
- 构建结果：`npm.cmd run build` 通过，仍保留既有 `@vueuse/core` PURE annotation 构建提示。
- 当前功能验收说明：未安装离线模型时，点击语音输入或开始语音面试会先请求麦克风并尝试浏览器/系统语音识别；浏览器识别不可用后才提示下载离线模型；已缓存离线模型时仍优先使用 sherpa-onnx；STT 失败只退出语音能力，不中断文字面试。
- 停止说明：本轮只修复 STT 启动优先级和设置文案，不继续推进 Kokoro、后端模型托管、真实模型文件部署或新的语音交互能力。

## 真实离线 STT 主路径与 TTS 卡死保护（2026-05-26）
- 当前任务所属模块：模拟面试前端语音通话稳定化，覆盖语音识别主路径、离线模型缓存、设置中心下载入口、TTS 播报超时释放和语音通话编排。
- 前端文件定位：`frontend/app/src/composables/useSpeechToText.js`、`frontend/app/src/workers/sherpaSpeechWorker.js`、`frontend/app/src/utils/offlineVoiceModelCache.js`、`frontend/app/src/composables/useTextToSpeech.js`、`frontend/app/src/composables/useVoiceCall.js`、`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/src/views/settings/SettingsView.vue` 以及对应测试。
- 后端文件定位：本轮不新增后端 API，不修改数据库字段，不修改现有面试消息接口。
- 本轮修改说明：STT 默认从 Web Speech 切换为离线 sherpa-onnx 模型优先；模型未缓存时明确提示“离线语音识别模型未安装”，不再打开假录音；设置中心提供真实模型下载、进度、清理缓存入口；TTS 增加 utterance watchdog，浏览器不触发 `onend/onerror` 时主动释放播报状态；语音通话在模型缺失或 TTS/STT 失败时只退出语音模式，保留文字输入。
- 数据存储方案：模型文件按 `/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json` 清单下载到浏览器 Cache API，状态元数据保存在本地缓存；用户音频只送入浏览器本机 Worker，不上传服务器。
- 验证结果：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/composables/useSpeechToText.test.js` 通过，2 个测试文件 / 25 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/utils/settingsPreferences.test.js` 通过，5 个测试文件 / 82 个用例通过。
- 停止说明：本轮不接入 Kokoro，不新增后端服务，不把大模型文件提交进源码；部署时需把 sherpa-onnx 浏览器 runtime 与模型文件放到上述静态目录。

# TASK 49 模拟面试语音通话前端

## 回归修复：STT 本地能力探测与 TTS 首次无声修复（2026-05-26）
- 当前任务所属模块：模拟面试前端语音通话稳定化，覆盖语音识别启动策略与面试官开场白 TTS 播报。
- 前端文件定位：`frontend/app/src/composables/useSpeechToText.js`、`frontend/app/src/composables/useTextToSpeech.js`、`frontend/app/src/__tests__/composables/useSpeechToText.test.js`、`frontend/app/src/__tests__/composables/useTextToSpeech.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 本轮修改文件清单：`useSpeechToText.js` 增加 `SpeechRecognition.available({ langs, processLocally: true })` 本地语言包能力探测，只有返回 `available` 时才启用 `processLocally`；`useTextToSpeech.js` 在播报前刷新并等待一次系统 voices 加载，同时调用 `speechSynthesis.resume()`；补充 STT 本地语言包不可用与 TTS voices 异步加载的回归测试。
- 问题原因：`processLocally = true` 不是“强制调用电脑/手机系统识别”的稳定开关，浏览器接受赋值也不代表本机已安装当前语言的本地识别包；当本地包不可用时强行启动会导致识别不可用。TTS 侧 Chrome/Android 首次进入页面时 `speechSynthesis.getVoices()` 可能暂时为空，立即创建 utterance 存在被吞掉或无声的风险。
- 前端实现方案：STT 启动前先通过浏览器静态能力接口确认当前语言本地识别包已可用；不可用、接口不存在、返回 `downloadable/downloading/unavailable` 或探测异常时，直接走浏览器服务识别，不再盲目设置 `processLocally`。TTS 在首次播报开场白前刷新 voices，如果 voices 还没加载则等待 `voiceschanged` 或短超时后再播报，并在调用 `speak()` 前执行 `speechSynthesis.resume()`。
- 数据存储方案：不新增本地存储、接口字段或数据库字段。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮回归原因、修复范围和验证结果。
- 编译结果：`npm.cmd run build` 通过，仅保留既有 `@vueuse/core` pure annotation 构建提示。
- 构建结果：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useTextToSpeech.test.js` 通过，2 个测试文件 / 27 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，2 个测试文件 / 36 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/settingsPreferences.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，7 个测试文件 / 98 个用例通过；`npm.cmd run build` 通过。
- 当前功能验收说明：本地中文语音包不可用时不再强行走本地识别，避免出现浏览器有麦克风图标但识别链路直接失败；面试官开场白播报会等待系统 voice 初始化并唤醒 TTS 合成器，降低 Chrome 首次无声概率。
- 停止说明：本轮只修复浏览器原生 STT/TTS 启动策略，不接入真实 sherpa-onnx / Kokoro，不新增 Worker、CDN、后端 API 或数据库字段；若浏览器服务识别因网络或地区不可达仍无法返回文字，需要进入下一阶段真实离线 STT 模型集成。

## 回归修复：本地语音包不支持 zh-CN 时自动回退浏览器识别（2026-05-26）
- 当前任务所属模块：模拟面试前端语音识别稳定化。
- 前端文件定位：`frontend/app/src/composables/useSpeechToText.js`、`frontend/app/src/__tests__/composables/useSpeechToText.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 本轮修改文件清单：`useSpeechToText.js` 增加 `language-not-supported` 本地识别失败回退；`useSpeechToText.test.js` 增加本地识别语言不支持时重启浏览器服务识别的回归测试。
- 问题原因：`processLocally = true` 赋值成功只能说明浏览器接受本地识别开关，并不代表设备已安装 `zh-CN` 本地语音包。部分浏览器会在 `recognition.start()` 后异步抛出 `language-not-supported`，原逻辑将该错误当作最终 STT 失败处理，导致语音通话直接降级为手动输入。
- 前端实现方案：当 `event.error === 'language-not-supported'` 且当前确实启用了本地识别时，清空错误状态，将 `preferLocalProcessing` 置为 `false`，释放当前本地识别实例和麦克风监听，再自动调用 `start()` 以浏览器识别服务重新启动；后续本页面生命周期内不再重复尝试本地识别，避免同一错误循环出现。
- 数据存储方案：不新增本地存储、接口字段或数据库字段。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮回归原因、修复范围和验证结果。
- 编译结果：`npm.cmd run build` 通过，仅保留既有 `@vueuse/core` pure annotation 构建提示。
- 构建结果：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js` 通过，1 个测试文件 / 15 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 51 个用例通过；`npm.cmd run build` 通过。
- 当前功能验收说明：设备本地语音识别缺少中文语音包时，不再显示“language-not-supported”并退出语音通话，而是自动回退到浏览器服务识别继续使用。
- 停止说明：本轮只修复 `language-not-supported` 回归，不继续推进真实离线模型下载、Worker、CDN 或后端能力。

## 模拟面试语音稳定化：系统本地优先与离线增强入口预留（2026-05-26）
- 当前任务所属模块：模拟面试前端语音通话稳定化，覆盖浏览器 STT、系统 TTS、语音通话状态展示与设置中心离线增强入口。
- 前端文件定位：`frontend/app/src/composables/useSpeechToText.js`、`frontend/app/src/composables/useVoiceCall.js`、`frontend/app/src/composables/useTextToSpeech.js`、`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/src/views/settings/SettingsView.vue`、`frontend/app/src/utils/settingsPreferences.js`、`frontend/app/src/utils/offlineVoiceModelCache.js` 以及对应单元测试。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或现有面试消息接口修改。
- 本轮修改文件清单：`useSpeechToText.js` 增加 `processLocally` 本地识别尝试、结构化错误状态和离线引擎建议；`useTextToSpeech.js` 增加系统 TTS 引擎状态；`settingsPreferences.js` 增加语音识别与离线增强偏好默认值及非法值归一；新增 `offlineVoiceModelCache.js` 作为离线模型元数据缓存边界；`InterviewSessionView.vue` 展示当前语音识别引擎状态；`SettingsView.vue` 增加“离线增强”设置块；补充 `useSpeechToText`、`useVoiceCall`、`useTextToSpeech`、`settingsPreferences`、`offlineVoiceModelCache`、`SettingsView` 测试。
- 前端实现方案：`SpeechRecognition` 创建后通过 `try/catch` 尝试设置 `recognition.processLocally = true`，成功时标记为系统本地优先，失败时保留浏览器服务回退；继续沿用既有 `lang = 'zh-CN'`、`continuous`、`interimResults`、麦克风音量监测、启动取消令牌和资源释放逻辑。错误状态统一暴露 `error`、`errorCode`、`engineStatus`、`supportsLocalProcessing`、`offlineEngineSuggested`，其中 `not-allowed` 提示麦克风权限，`network`、`no-speech`、`service-not-allowed`、`end-without-result` 提示系统语音引擎暂不可用并建议后续下载离线语音包。
- 语音通话接入方案：语音通话继续复用现有发送、静音、停止收听并发送、AI 播报暂停收音和播报结束恢复链路；STT 失败时退出语音通话，但不终止面试会话，用户仍可继续使用文字输入；语音浮层和折叠条展示“系统本地优先 / 浏览器云端回退 / 不可用”的识别引擎状态。
- TTS 实现方案：本轮继续使用浏览器原生 `speechSynthesis`、系统音色选择、自定义 voice、语速、音调、音量和试听能力；新增 `engineStatus` 用于区分系统 TTS 与不支持状态；不接入 Kokoro，不新增 `kokoro-js` 依赖。
- 设置与缓存边界方案：设置页新增“离线增强”块，展示 STT 当前引擎、系统本地能力检测结果、麦克风权限引导、`sherpa-onnx` 后续下载入口说明；TTS 区域保留系统音色选择与试听，并说明 Kokoro 高品质离线语音包属于后续阶段。`offlineVoiceModelCache.js` 仅提供 IndexedDB/Cache API 能力检测和模型状态元数据读写接口，字段包含 `supported`、`status`、`progress`、`modelKey`，不下载真实模型文件、不创建 Worker、不修改 Vite 静态资源托管策略。
- 数据存储方案：新增本地偏好字段 `voiceRecognitionEngine: 'system_local'`、`offlineSttEngine: 'sherpa_onnx'`、`offlineTtsEngine: 'system'`，非法缓存值会归一到默认值；离线模型缓存本轮只记录浏览器本地元数据，不上传用户音频，不新增后端数据结构。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮完成范围、验证结果和停止边界。
- 编译结果：`npm.cmd run build` 通过，仅保留既有 `@vueuse/core` pure annotation 构建提示。
- 构建结果：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/settingsPreferences.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，7 个测试文件 / 95 个用例通过；`npm.cmd run build` 通过。
- 当前功能验收说明：模拟面试语音识别会优先尝试系统本地识别能力，浏览器不支持时继续回退到既有 Web Speech 服务；识别失败会给出明确错误码和用户提示，并展示后续离线语音包建议；设置中心已能保存和重置新增语音偏好，并展示 STT/TTS 离线增强入口说明。
- 停止说明：本轮只完成系统本地优先、错误结构化、设置入口和缓存边界预留，不真实下载或接入 `sherpa-onnx` / Kokoro 模型，不新增大体积依赖、Web Worker、CDN 配置、后端 API、数据库字段或下一阶段离线识别/合成能力。

## 验收反馈修正：语音面试通话图标放大与背景弱化
- 当前任务所属模块：模拟面试前端语音通话 UI。
- 前端文件定位：`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/src/__tests__/views/InterviewSessionView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务或数据结构修改。
- 本轮修改文件清单：`InterviewSessionView.vue` 调整语音通话展开态头像、背景和圆形控制按钮图标尺寸；`InterviewSessionView.test.js` 增加静态样式回归断言。
- 前端实现方案：主通话头像由固定 `196px` 放大为 `clamp(224px, 22vw, 252px)` 响应式尺寸，移动端同步放大并保留低高度兜底；顶部浅橙背景透明度降低到 `rgba(255, 140, 66, 0.045)`，高度与头像中心更接近，避免背景分割线抢视觉焦点；圆形控制按钮内部 `FeatureIcon` 单独放大到桌面端 `32px`、移动端 `30px` 并居中。
- 后端实现方案：不涉及后端。
- 数据存储方案：不新增本地存储、接口字段或数据库字段。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮修复范围、验证结果和停止边界。
- 编译结果：`npm.cmd run build` 通过。
- 构建结果：`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，1 个测试文件 / 18 个用例通过。
- 当前功能验收说明：语音面试展开态主头像更大、更靠近视觉中心；背景更淡，底部圆形控制按钮中的图标更清晰且居中。
- 停止说明：本轮只处理语音面试图标尺寸、居中和背景弱化，不修改 STT/TTS、SSE、挂断、静音或后端能力。

## 验收反馈修正：开场白播报期间取消异步收音启动
- 当前任务所属模块：模拟面试前端语音通话 STT/TTS 协同链路。
- 前端文件定位：`frontend/app/src/composables/useSpeechToText.js`、`frontend/app/src/__tests__/composables/useSpeechToText.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务或数据结构修改。
- 本轮修改文件清单：`useSpeechToText.js` 增加异步启动取消令牌；`useSpeechToText.test.js` 增加开场白前取消收音竞态回归测试。
- 前端实现方案：`start()` 每次启动语音识别时记录启动序号，`cancel()` 和启动期 `stop()` 会递增序号并终止当前启动。若麦克风授权或音量监测在取消后才异步返回，旧启动会立即清理媒体资源并退出，不再创建 `SpeechRecognition`，避免开场白 TTS 期间继续监听并触发 `no-transcript` 降级。
- 后端实现方案：不涉及后端。
- 数据存储方案：不新增本地存储、接口字段或数据库字段。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮修复范围、验证结果和停止边界。
- 编译结果：`npm.cmd run build` 通过。
- 构建结果：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js` 通过，1 个测试文件 / 11 个用例通过；`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，1 个测试文件 / 17 个用例通过；`npm.cmd run build` 通过。
- 当前功能验收说明：开始语音通话后若开场白立即播报，前置 STT 启动过程即使尚未完成，也会被取消并清理，不会在开场白播放期间因检测到麦克风输入但无识别文字而中断 TTS 或降级为手动输入。
- 停止说明：本轮只修复开场白播报期间异步收音启动未取消的问题，不继续扩展云端 STT/TTS、实时音视频、回声消除或新的面试能力。

## 验收反馈修正：语音通话按钮与移动端布局优化
- 当前任务所属模块：模拟面试前端语音通话 UI。
- 前端文件定位：`frontend/app/src/views/interview/InterviewSessionView.vue`。
- 后端文件定位：本轮不涉及后端接口、服务或数据结构修改。
- 本轮修改文件清单：仅修改 `InterviewSessionView.vue`。
- 前端实现方案：统一展开态通话浮层圆形按钮的宽高、伸缩基准和盒模型，避免挂断、确认、静音等按钮视觉大小不一致；收紧折叠态底部通话条的最大宽度、内边距、按钮高度和按钮最小宽度，让桌面端底部控制区不再显得过大；移动端折叠通话条按钮改为 42px 圆形图标按钮，并隐藏可见文字，减少对聊天界面的占用；为“展开”按钮补充 `FullScreen` 图标，移动端只显示图标时仍有明确视觉入口；移动端顶部导航保持两行结构，标题省略显示，右侧状态与结束按钮等距排列，避免窄屏挤压变形。
- 后端实现方案：不涉及后端。
- 数据存储方案：不新增本地存储、接口字段或数据库字段。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮修正范围、验证结果和停止边界。
- 编译结果：`npm.cmd run build` 通过。
- 构建结果：`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，1 个测试文件 / 12 个用例通过；`npm.cmd run build` 通过。
- 当前功能验收说明：图一展开态语音通话控制按钮使用统一圆形尺寸；图二移动端折叠通话条按钮改为紧凑图标按钮，不再用大面积文字按钮挤占聊天区，顶部导航窄屏不再挤压变形；图三桌面端底部通话条按钮和面板宽度收紧，整体更贴合聊天界面。
- 停止说明：本轮只处理语音通话按钮尺寸、移动端折叠通话条和顶部导航响应式问题，不继续扩展 STT/TTS、SSE、通话状态或后端能力。

## 验收反馈修正：语音通话控制按钮尺寸统一
- 本轮根据最新反馈修正折叠语音通话条的按钮尺寸：`.voice-call-actions` 下所有 Element Plus 按钮统一为 `132px × 42px`，移动端统一为满宽 `44px` 高，避免新增“停止收听并发送”按钮比“静音 / 展开 / 挂断”显得更小。
- 统一按钮内联布局、居中对齐、字重和 `white-space`，保证同一控制区内按钮视觉尺寸一致，不再依赖 Element Plus 默认按文案自适应尺寸。
- 展开态圆形图标按钮仍沿用 `.voice-icon-btn` 统一尺寸，本轮不修改图标、通话、静音、挂断、SSE 或后端接口逻辑。
- 补充 `InterviewSessionView.test.js` 用例，覆盖折叠通话条启动后展示完整的 4 个语音控制按钮。
- 本轮验证结果：待执行定向测试与前端构建。

## 验收反馈修正：语音通话手动停止收听并发送
- 当前任务所属模块：模拟面试前端语音通话控制区。
- 前端文件定位：`frontend/app/src/composables/useVoiceCall.js`、`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/src/__tests__/composables/useVoiceCall.test.js`、`frontend/app/src/__tests__/views/InterviewSessionView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务或数据结构修改。
- 本轮修改文件清单：为 `useVoiceCall.js` 增加 `stopListeningAndSend()`；在语音通话展开浮层和折叠底部通话条增加“停止收听并发送”控制；补充 composable 与会话页回归测试。
- 前端实现方案：保留静音键只负责暂停/恢复麦克风收音，不再用静音承担提交回答职责；新增手动停止按钮用于跳过 3 秒静音等待，直接提交当前已识别文本。该操作会停止当前收听并进入发送流程，但不改变 `isMuted`，避免用户把“提交本轮回答”和“暂停通话”混在一起。
- 后端实现方案：不涉及后端。
- 数据存储方案：不新增本地存储、接口字段或数据库字段。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮修复范围、验证结果和停止边界。
- 编译结果：`npm.cmd run build` 通过。
- 构建结果：`npm.cmd test -- --run src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，2 个测试文件 / 20 个用例通过；`npm.cmd test` 通过，25 个测试文件 / 136 个用例通过；`npm.cmd run build` 通过。
- 当前功能验收说明：语音通话中识别到回答后，可以点击“停止收听并发送”立即提交；即使环境杂音持续刷新人声活动，也不必等待 3 秒静音。没有可发送文本时会提示“当前还没有可发送的识别文本”，不会静音、挂断或重置通话。
- 停止说明：本轮只增加语音通话手动停止收听并发送能力，不继续扩展云端 STT、实时音视频、AI 播报打断或后端能力。

## 验收反馈修正：语音面试底部空白收敛
- 本轮根据最新截图反馈修正语音面试聊天区与底部通话条之间的过大留白：语音会话不再固定给聊天容器预留 `112px` 底部空间，展开通话遮罩时收敛为 `40px` 缓冲，折叠为底部通话条时收敛为 `16px`。
- 移动端语音会话聊天容器底部预留统一降为 `12px`，减少红圈区域中的空白，避免底部通话条上方出现明显断层。
- `InterviewSessionView.vue` 为折叠状态补充 `voice-call-collapsed-stage` 样式类，仅用于布局收敛；不修改聊天消息、语音通话、静音、挂断、SSE 或后端接口逻辑。
- 补充 `InterviewSessionView.test.js` 断言折叠语音通话后存在收敛布局状态类，防止后续回归为大留白。
- 本轮验证结果：`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，1 个测试文件 / 7 个用例通过；`npm.cmd run build` 通过。

## 验收反馈修正：语音面试与每题反馈互斥
- 本轮根据实测反馈修复“每题反馈 + 语音面试”组合下反馈不可见/不可感知的问题：语音面试会过滤 `<FEEDBACK>` 结构化片段用于播报，导致即时反馈不适合作为语音通话模式的入口选项。
- `InterviewEntryView.vue` 增加入口互斥：选择语音面试时，如果当前反馈模式为“每题反馈”，自动切换为“面完复盘”并提示；语音面试状态下点击“每题反馈”不会生效，并提示“语音面试暂不支持每题反馈”。
- 创建会话 payload 增加最小保护：只要 `interactionType` 为语音面试，提交给后端的 `feedbackMode` 固定为 `after_interview`，避免通过本机默认偏好或异常状态提交不兼容组合。
- 补充 `InterviewEntryView.test.js` 用例，覆盖语音面试自动切换面完复盘、语音面试下禁止选择每题反馈。
- 本轮不修改后端接口、SSE 消息链路、TTS 播报策略、报告页回放结构或语音通话 UI 布局。
- 本轮验证结果：`npm.cmd test -- --run src/__tests__/views/InterviewEntryView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，2 个测试文件 / 14 个用例通过；`npm.cmd run build` 通过。

## 验收反馈修正：语音通话挂断按钮尺寸统一
- 本轮根据最新反馈修正语音通话界面挂断按钮尺寸：移除 `.voice-hangup-btn` 单独设置的桌面端 `62px` 和移动端 `64px` 宽高，让挂断按钮与折叠、麦克风按钮共用 `.voice-icon-btn` 尺寸。
- 挂断按钮仍保留红色危险操作样式、hover 状态和 `PhoneFilled` 图标旋转效果，仅统一按钮外框尺寸，不改通话、静音、折叠或挂断业务逻辑。
- 本轮不修改后端接口、语音识别、TTS 播报、SSE 消息链路和会话数据结构。
- 本轮验证结果：`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，1 个测试文件 / 7 个用例通过；`npm.cmd run build` 通过。

## 验收反馈修正：语音识别不可用时明确降级提示
- 本轮根据最新反馈补充语音识别失败的明确降级提示：浏览器不支持 Web Speech、Chrome/浏览器识别服务 `network` 不可用、麦克风权限拒绝、未检测到麦克风和启动失败时，均提示已降级为手动输入。
- `useSpeechToText.js` 不新增自动重试、自动重连或云端 ASR 兜底，仅将错误文案调整为用户可理解的降级状态；`network` 文案提示可切换 Edge 或检查网络后重试，避免用户误以为仍在后台持续识别。
- 根据实测反馈继续补充“无文字且无报错”的识别失败场景：`no-speech` 不再静默处理；麦克风检测到声音但 6 秒内 Web Speech 没有返回任何文本时，提示 `no-transcript`；浏览器结束识别但没有返回文字时，提示 `end-without-result`。
- `useVoiceCall.js` 在语音通话启动前发现 STT/TTS 不支持时，也提示已降级为手动输入，并保持语音通话未启动，用户可继续使用文字输入完成模拟面试。
- 补充测试覆盖：`useSpeechToText.test.js` 覆盖不支持语音识别、`network`、权限拒绝、麦克风采集失败、`no-speech`、麦克风有输入但无文本、识别结束无文本的降级提示；`useVoiceCall.test.js` 覆盖语音识别不支持时不启动通话并提示手动输入。
- 本轮不修改后端接口、会话数据结构、SSE 消息链路、TTS 播报策略、语音通话 UI 布局或云端 STT/TTS 能力。
- 本轮验证结果：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 26 个用例通过；`npm.cmd run build` 通过。

## 验收反馈修正：文字语音输入 network 错误显式提示
- 本轮根据最新反馈覆盖上一轮 `network` 静默恢复策略：语音识别 `network` 错误不再作为静默可恢复中断，而是写入明确错误文案“当前网络连接异常，语音识别服务暂时不可用，请检查网络后重试”，让文字聊天语音输入和语音通话都能提示用户当前识别服务不可用。
- `useSpeechToText.js` 保留 `no-speech`、`aborted` 的可恢复清理逻辑，避免正常停顿或浏览器主动结束识别时误弹错误；仅 `network`、权限拒绝和其他真实错误写入 `error`。
- 补充 `useSpeechToText.test.js` 用例，覆盖 `network` 写入用户可见错误，以及 `no-speech` 仍按可恢复中断处理。
- 本轮不修改后端接口、SSE 消息链路、TTS 播报链路、UI 布局和会话数据结构。
- 本轮验证结果：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 15 个用例通过；`npm.cmd run build` 通过。

## 验收反馈修正：Web Speech network 错误恢复
- 本轮修正语音识别报错 `语音识别错误: network` 的处理方式：该错误来自浏览器 Web Speech 识别服务的短暂中断，不等同于用户网络不可用，因此不再作为致命错误弹窗和挂断通话。
- `useSpeechToText.js` 将 `network`、`no-speech`、`aborted` 归为可恢复识别中断：清理当前识别实例与音量监听资源，但不写入 `error`；通话层会按已有恢复逻辑重新启动监听。
- 补充 `useSpeechToText.test.js` 用例，覆盖 `network` 不写入错误、不触发致命错误状态且释放麦克风监听资源。
- 本轮不修改后端接口、SSE 消息链路、TTS 播报链路和会话数据结构。
- 本轮验证结果：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 14 个用例通过；`npm.cmd run build` 通过。

## 验收反馈修正：语音识别灵敏度与三秒静音自动发送
- 本轮修正语音通话识别灵敏度：`useSpeechToText.js` 新增基于 `getUserMedia` + Web Audio `AnalyserNode` 的麦克风音量活动检测，实时输出 `isVoiceActive` 与 `voiceActivityAt`，避免只依赖 Web Speech 文本结果回调导致“已经说话但没有被检测到”。
- `useVoiceCall.js` 将静音自动发送阈值从 5 秒调整为 3 秒；当麦克风仍检测到人声活动时刷新静音计时，停止说话满 3 秒后再发送已识别文本。
- 通话中如果浏览器 Web Speech 自动结束监听，通话层会在非静音、非 AI 回复状态下恢复监听，降低 Chrome 识别服务短暂停止后不再收音的概率。
- `InterviewSessionView.vue` 将新增的音量活动状态接入语音通话编排，不修改后端接口、SSE 消息链路、TTS 播报链路和会话数据结构。
- 补充 `useSpeechToText.test.js`，覆盖麦克风音量活动检测与取消通话时资源清理；补充 `useVoiceCall.test.js`，覆盖 3 秒静音发送、有人声活动时延迟发送、浏览器结束监听后的自动恢复。
- 本轮验证结果：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 13 个用例通过；`npm.cmd run build` 通过。

## 验收反馈修正：语音通话展示尺寸、移动端铺满与挂断按钮

- 三次验收修正：语音通话展开态面板从小型浮窗放大为主通话面板，提升头像、状态文字、计时和底部控制区尺寸，减少页面中心孤立感。
- 桌面端保留会话内容区覆盖逻辑，但扩大通话窗口宽度与高度，并补充浅色顶部层次和更清晰的控制按钮分组。
- 移动端语音通话展开态改为铺满会话内容区，取消外边距、圆角和阴影，适配安全区，避免图二中卡片悬浮且底部留白过大的问题。
- 挂断按钮从旋转电话字符改为 Element Plus `PhoneFilled` 图标，并使用红色实心圆形按钮，触控面积同步放大。
- 本轮不修改语音链路、STT/TTS 行为、消息发送逻辑和后端接口。
- 三次验证结果：`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，1 个测试文件 / 4 个用例通过；`npm.cmd run build` 通过。

## 验收反馈修正：语音通话浮窗、折叠聊天界面与静音

- 二次验收修正：语音通话展开态改为覆盖整个会话内容区，底层聊天界面不再透出；只有折叠后才显示聊天消息列表与底部通话条。
- 语音通话浮窗按钮补充鼠标 hover 文案：折叠到聊天界面、开始通话/切换静音、挂断语音通话，并保留对应 `aria-label`。
- 补充前端测试断言语音浮窗控制按钮具备 hover 文案。
- 二次验证结果：`npm.cmd run test -- --run src/__tests__/views/InterviewSessionView.test.js src/__tests__/composables/useVoiceCall.test.js` 通过，2 个测试文件 / 9 个用例通过。
- 语音会话详情页恢复聊天消息列表作为底层界面，不再把语音面试固定成单一卡片页。
- 语音会话默认显示接近通话窗口的浮层：头像、三点状态、通话状态、计时、折叠、麦克风和挂断控制。
- 支持折叠语音通话浮层；折叠后回到底部通话条，聊天记录保持可见，呈现原会话界面效果。
- 通话中新增静音/取消静音：静音只停止麦克风收音，不退出通话；取消静音后自动恢复监听。
- 修复 `InterviewSessionView.vue` 与 `useVoiceCall.js` 中语音通话相关中文文案乱码，按钮、状态、提示均恢复正常中文。
- 补充前端测试：覆盖语音会话默认显示浮层且不自动开麦、折叠后显示底部通话条、静音状态切换和结束通话清理。
- 验证结果：`npm.cmd run test -- --run src/__tests__/views/InterviewSessionView.test.js src/__tests__/composables/useVoiceCall.test.js` 通过，2 个测试文件 / 9 个用例通过；`npm.cmd run build` 通过。
- 可视化验证说明：已读取 Browser 插件说明，但当前会话未暴露其要求的 Node REPL 浏览器控制工具，因此本轮未执行 in-app browser 截图验证。

## 验收反馈修正：语音会话界面与播报自然度

- 语音会话页不再渲染聊天消息列表，也不再显示底部文字输入区；`InterviewSessionView.vue` 在 `isVoiceSession` 为真时只展示语音通话面板，避免用户看到文字聊天界面。
- 语音通话面板从底部输入区提升为页面主体内容，并补充桌面端居中、移动端底部贴近的响应式布局。
- `useTextToSpeech.js` 默认语速调整为 `0.92`，默认音高调整为 `1.06`，并优先选择浏览器中更自然的中文 voice（如 Microsoft Xiaoxiao/Yunxi/Natural/Neural 等），降低播报机械感。
- 补充前端测试：语音会话即使存在历史聊天记录也不渲染 `.chat-messages` 和 `.input-area`；TTS 测试覆盖自然中文 voice 选择、默认语速和音高。
- 验证结果：`npm.cmd run test -- --run src/__tests__/views/InterviewSessionView.test.js src/__tests__/composables/useTextToSpeech.test.js` 通过，2 个测试文件 / 7 个用例通过；`npm.cmd run test` 通过，24 个测试文件 / 110 个用例通过；`npm.cmd run build` 通过。

## 当前任务所属模块

模拟面试前端，支持创建语音面试会话，并在语音会话页提供浏览器 Web Speech API 通话体验。

## 前端文件定位

- `frontend/app/src/constants/interview.js`
- `frontend/app/src/api/interview.js`
- `frontend/app/src/composables/useTextToSpeech.js`
- `frontend/app/src/composables/useVoiceCall.js`
- `frontend/app/src/views/interview/InterviewEntryView.vue`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/views/interview/InterviewHistoryView.vue`
- `frontend/app/src/__tests__/**`

## 后端文件定位

后端实现见 `tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_BACKEND.md`。

## 本轮修改文件清单

- 新增语音/文字交互方式常量和标签。
- 创建会话页新增“交互方式”选择，默认文字；浏览器不支持语音识别或播报时语音选项置灰并保持文字模式。
- 新增 `useTextToSpeech.js`，支持 SSE 分片逐句朗读、`flushRemaining()` 和 `<FEEDBACK>` 过滤。
- 新增 `useVoiceCall.js`，复用现有 `useSpeechToText.js`，负责通话状态、静音 5 秒自动发送、计时和清理。
- 会话页根据 `sessionData.interactionType === 1` 显示语音通话面板，不自动开麦，用户点击“开始通话”后才启动 STT/TTS。
- AI 回复流进入 TTS 前过滤即时反馈结构，AI 朗读时暂停收音，避免回声被误识别。
- 历史记录页展示文字/语音交互方式标识，不展示通话时长。

## 前端实现方案

文字模式保留现有输入框、发送按钮和麦克风草稿能力。语音会话只切换底部输入区为通话面板，消息列表、SSE 流式接收和打字机展示仍复用原链路。通话开始后，STT interim/final 任一变化都会刷新 `lastSpeechAt`，静音 5 秒且没有 AI 回复进行中时自动发送当前回答。AI 回复流式返回时同步进入 TTS 逐句队列，`done` 时调用 `flushRemaining()` 朗读剩余 buffer。

MVP 不实现真正语音打断 AI。AI 回复或 TTS 朗读期间会暂停/忽略 STT，朗读完成后再恢复监听，降低 TTS 被浏览器 STT 识别为用户语音的风险。

## 数据存储方案

前端仅传递和展示 `interactionType`，不新增本地存储，不保存通话时长。

## stage 更新说明

已同步更新 `frontend/tasks/stage.md`，记录本轮前端完成状态、验证结果和停止边界。

## 编译结果

`npm.cmd run build` 通过。

## 构建结果

- `npm.cmd run test -- --run src/__tests__/views/InterviewEntryView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useVoiceCall.test.js` 通过，4 个测试文件、15 个测试通过。
- `npm.cmd run test` 通过，24 个测试文件、109 个测试通过。
- `npm.cmd run build` 通过。

## 当前功能验收说明

- 支持创建语音面试会话并传递 `interactionType: 1`。
- 不支持 Web Speech API 的浏览器会保持文字面试。
- 语音会话页刷新后识别为语音会话，但不会自动开麦。
- 点击开始通话后可进入监听，静音 5 秒自动发送，AI 回复可逐句朗读。
- 挂断、组件卸载和异常时会清理 STT、TTS 与计时器。

## 停止说明

本轮只完成浏览器 Web Speech API 版语音通话 MVP，不接入云端语音服务、不保存通话时长、不实现实时音视频或真正语音打断 AI。
## 验收反馈修正：模拟面试语音输入隔离与网络失败不重连
- 本轮根据最新反馈修复模拟面试语音输入与语音通话共用同一 STT 实例导致的互相干扰问题：`InterviewSessionView.vue` 中将文字输入麦克风和语音通话麦克风拆成两个独立 `useSpeechToText()` 实例，文字输入只更新输入框草稿，语音通话只交给 `useVoiceCall` 编排。
- 修复点击关闭麦克风后仍可能显示录音中的问题：`useSpeechToText.stop()` 在调用浏览器识别停止后立即设置 `isRecording=false`、清理 recognition 回调并释放 Web Audio / getUserMedia 资源，避免等待浏览器异步 `onend` 才更新界面。
- 修复网络不好时自动重连造成界面卡在“正在重连”的问题：面试 SSE 回复发送链路移除自动重连循环，当前流请求失败后直接进入错误状态、停止 TTS/语音通话并释放发送锁，用户可自行重新发送。
- 补充测试覆盖：`useSpeechToText.test.js` 覆盖 stop 立即释放麦克风；`InterviewSessionView.test.js` 覆盖语音通话麦克风不触发文字输入 STT、流式失败不自动发起第二次请求。
- 本轮不修改后端接口、会话数据结构、TTS 播报策略和语音通话 UI 布局。
- 本轮验证结果：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 18 个用例通过；`npm.cmd run build` 通过。
## 验收反馈修正：语音输入后点击发送报错
- 当前任务所属模块：模拟面试前端语音输入发送链路。
- 前端文件定位：`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/src/__tests__/views/InterviewSessionView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务或数据结构修改。
- 本轮修改文件清单：修复 `InterviewSessionView.vue` 的 `sendMessage` 入参来源判断；补充 `InterviewSessionView.test.js` 回归用例。
- 前端实现方案：Element Plus 按钮点击会把点击事件对象传给组件事件处理函数，原逻辑直接对 `(overrideContent || inputMessage.value)` 调用 `.trim()`，导致语音输入文字后点击发送时事件对象被当作消息内容处理。本轮只允许字符串类型的 `overrideContent` 覆盖输入框内容，按钮点击事件统一回落到 `inputMessage.value`。
- 后端实现方案：不涉及后端。
- 数据存储方案：不新增本地存储、接口字段或数据库字段。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮修复范围、验证结果和停止边界。
- 编译结果：`npm.cmd run build` 通过。
- 构建结果：`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，1 个测试文件 / 7 个用例通过；`npm.cmd run build` 通过。
- 当前功能验收说明：语音输入识别出的文字保留在输入框后，点击发送按钮不再触发 `trim is not a function`，仍会按输入框文本发送；语音通话自动发送字符串内容的路径保持不变。
- 停止说明：本轮只修复语音输入后点击发送的参数类型问题，不继续扩展 STT/TTS、SSE、语音通话 UI 或后端能力。

## 验收反馈修正：首次语音通话开场白不播报
- 当前任务所属模块：模拟面试前端语音通话开场白播报链路。
- 前端文件定位：`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/src/__tests__/views/InterviewSessionView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务或数据结构修改。
- 本轮修改文件清单：`InterviewSessionView.vue` 增加首次语音通话开场白播报状态与触发逻辑；`InterviewSessionView.test.js` 增加首次开始通话播报开场白且不重复播报的回归测试。
- 前端实现方案：语音通话开始成功后，从当前聊天记录中读取第一条面试官消息作为开场白内容，并调用现有 `useTextToSpeech().speak()` 播报；页面内通过 `openingSpeechPlayed` 记录本次会话页是否已经播报，挂断后再次开始通话不会重复朗读。若开场白仍在生成中，则在轮询拿到聊天记录后、且语音通话仍处于开启状态时补播一次。
- 后端实现方案：不涉及后端。
- 数据存储方案：不新增本地存储、接口字段或数据库字段，播报状态仅存在当前页面生命周期内。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮修复范围、验证结果和停止边界。
- 编译结果：`npm.cmd run build` 通过。
- 构建结果：`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，1 个测试文件 / 9 个用例通过；`npm.cmd run build` 通过。
- 当前功能验收说明：首次进入语音面试并点击开始通话后，会先播报已生成的面试官开场白；挂断后再次开始通话不会重复播报同一开场白；文字面试与后续 SSE 回复播报链路保持不变。
- 停止说明：本轮只修复首次语音通话开场白不播报问题，不继续扩展云端 TTS、实时音视频、语音打断或新的面试能力。

## 验收反馈修正：开场白播报被误识别为用户回答
- 当前任务所属模块：模拟面试前端语音通话开场白播报与收音隔离链路。
- 前端文件定位：`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/src/__tests__/views/InterviewSessionView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务或数据结构修改。
- 本轮修改文件清单：`InterviewSessionView.vue` 在开场白 TTS 播报前取消通话 STT；`InterviewSessionView.test.js` 增加开场白播报前必须关闭语音识别的回归测试。
- 前端实现方案：语音通话开始时仍沿用现有启动流程；当检测到需要播报开场白时，先调用 `voiceSttCancel()` 清空并关闭当前通话识别，再调用 `textToSpeech.speak()` 播报。播报结束后继续复用 `useTextToSpeech` 的 `onEnd -> voiceCall.resumeListening()` 恢复收音，避免 TTS 声音进入用户回答内容。
- 后端实现方案：不涉及后端。
- 数据存储方案：不新增本地存储、接口字段或数据库字段。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮修复范围、验证结果和停止边界。
- 编译结果：`npm.cmd run build` 通过。
- 构建结果：`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，1 个测试文件 / 10 个用例通过；`npm.cmd run build` 通过。
- 当前功能验收说明：首次开始语音通话会播报开场白，但播报前会关闭浏览器语音识别，开场白不会再被插入为用户回答；播报结束后自动恢复收音，后续用户回答和 AI 回复播报链路保持不变。
- 停止说明：本轮只修复开场白播报被误识别为用户回答问题，不继续扩展云端 TTS、回声消除、实时音视频或新的面试能力。

## 验收反馈修正：继续历史语音面试不重复播开场白
- 当前任务所属模块：模拟面试前端语音通话开场白播报触发条件。
- 前端文件定位：`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/src/__tests__/views/InterviewSessionView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务或数据结构修改。
- 本轮修改文件清单：`InterviewSessionView.vue` 收紧开场白播报条件；`InterviewSessionView.test.js` 增加继续已有语音面试时不播报开场白的回归测试。
- 前端实现方案：读取开场白前先检查当前聊天记录中是否已经存在 `messageRole === "user"` 的用户回答。若已有用户回答，说明这是继续历史面试或已开始的会话，直接跳过开场白播报；只有还没有用户回答的首轮语音会话才会播报第一条面试官消息。
- 后端实现方案：不涉及后端。
- 数据存储方案：不新增本地存储、接口字段或数据库字段。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮修复范围、验证结果和停止边界。
- 编译结果：`npm.cmd run build` 通过。
- 构建结果：`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，1 个测试文件 / 11 个用例通过；`npm.cmd run build` 通过。
- 当前功能验收说明：从历史记录继续已有语音面试时，点击开始通话不会重复朗读第一条开场白，也不会因为开场白逻辑打断当前收音；新创建且尚未作答的语音会话仍会在首次开始通话时播报开场白。
- 停止说明：本轮只修复历史语音面试重复播报开场白问题，不继续扩展后端播报状态、云端 TTS、实时音视频或新的面试能力。

## 验收反馈修正：移动端顶部导航栏错位
- 当前任务所属模块：模拟面试前端会话页移动端顶部状态栏布局。
- 前端文件定位：`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/src/__tests__/views/InterviewSessionView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务或数据结构修改。
- 本轮修改文件清单：`InterviewSessionView.vue` 调整移动端 `.session-status-bar` 下第二行操作区布局；`InterviewSessionView.test.js` 增加移动端状态栏 grid 布局静态回归断言。
- 前端实现方案：移动端状态栏仍保持两行结构，第一行展示标题和难度；第二行从 `flex + space-between` 改为三列 grid，左侧固定状态，中间固定返回按钮宽度，右侧固定结束面试按钮，避免返回箭头被拉到中间造成明显错位。标题允许收缩省略，难度标签保持固定宽度。
- 后端实现方案：不涉及后端。
- 数据存储方案：不新增本地存储、接口字段或数据库字段。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮修复范围、验证结果和停止边界。
- 编译结果：`npm.cmd run build` 通过。
- 构建结果：`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，1 个测试文件 / 13 个用例通过；`npm.cmd run build` 通过。
- 当前功能验收说明：移动端顶部栏中“进行中”稳定靠左，返回按钮靠近右侧操作区，结束面试按钮固定在右侧，不再出现返回箭头悬在中间的错位排版。
- 停止说明：本轮只修复移动端顶部导航栏排版错位，不继续调整语音通话主体、聊天气泡、后端接口或其他页面。
# 模拟面试语音通话移动端顶部控制隐藏（2026-05-24）
- 当前任务所属模块：模拟面试前端语音通话移动端响应式 UI。
- 前端文件定位：`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/src/__tests__/views/InterviewSessionView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务或数据结构修改。
- 本轮修改文件清单：`InterviewSessionView.vue` 在 767px 以下隐藏展开态语音通话浮层顶部 `.voice-window-bar`，并收紧头像顶部间距；`InterviewSessionView.test.js` 增加移动端隐藏重复顶部控制的回归测试。
- 前端实现方案：移动端语音通话展开页已经在底部 `.voice-dock-actions` 提供折叠、麦克风、停止收听并发送、挂断等完整控制，因此小屏下隐藏顶部缩小符号和 X，避免重复操作入口挤占首屏空间。保留桌面端顶部窗口控制不变。
- 后端实现方案：不涉及后端。
- 数据存储方案：不新增本地存储、接口字段或数据库字段。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮响应式修复范围、测试与构建结果。
- 编译结果：`npm.cmd run build` 通过。
- 构建结果：`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，1 个测试文件 / 21 个用例通过；`npm.cmd run build` 通过。
- 当前功能验收说明：语音通话展开态在移动端不会再显示顶部缩小和 X 控制，用户使用底部按钮即可完成折叠和挂断，小屏视觉更干净。
- 停止说明：本轮只处理模拟面试语音通话移动端顶部控制隐藏，不修改 STT/TTS、SSE、发送、挂断、接口、路由或后端逻辑。

# 模拟面试会话页 UI 质感与动效优化（2026-05-24）
- 当前任务所属模块：模拟面试前端会话页 UI，覆盖文字面试聊天区与语音面试通话浮层的视觉表现。
- 前端文件定位：`frontend/app/src/views/interview/InterviewSessionView.vue`、`frontend/app/src/__tests__/views/InterviewSessionView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务或数据结构修改。
- 本轮修改文件清单：`InterviewSessionView.vue` 增加会话壳层、聊天表面、消息入场、角色胶囊、暖橙表面变量、语音浮层进入动效和暗色模式 `:global(html[data-theme="dark"] ...)` 覆盖；`InterviewSessionView.test.js` 增加结构、动效和暗色 scoped 选择器回归测试。
- 前端实现方案：保留现有 `FeatureIcon`、Element Plus 控件、STT/TTS、SSE 和发送链路不变，仅重构表现层。会话页增加 `.interview-session-shell`、`.session-main-surface`、`.conversation-surface` 作为后续验收锚点；聊天消息增加 `.message-entrance` 与角色标签，使用 `opacity` + `transform` 的轻量入场和 hover 反馈；输入区、折叠语音条与语音通话浮层统一暖橙白表面、细边框和柔和投影；语音浮层增加 `voiceCallEnter` 入场，按钮增加 hover/press 微交互。
- 暗色适配方案：修复 scoped CSS 下裸 `[data-theme="dark"] .xxx` 选择器不稳定的问题，统一改为 `:global(html[data-theme="dark"] .xxx)`；为会话页新增暗色变量，覆盖背景、顶部栏、消息气泡、输入区、语音浮层和反馈卡片，避免暗色模式被浅色/灰色表面割裂。
- 动效与性能边界：新增 `sessionSurfaceIn`、`messageFloatIn`、`voiceCallEnter`，均只使用 `opacity` 和 `transform`；保留语音波形与思考态动画；`prefers-reduced-motion` 下关闭新增入场、消息、语音浮层与持续动画，避免影响动效敏感用户。
- 后端实现方案：不涉及后端。
- 数据存储方案：不新增本地存储、接口字段或数据库字段。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮 UI 优化范围、测试与构建结果。
- 编译结果：`npm.cmd run build` 通过。
- 构建结果：`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，1 个测试文件 / 20 个用例通过；`npm.cmd run build` 通过。
- 当前功能验收说明：模拟面试会话页拥有更明确的聊天工作台结构、消息层级、暖橙质感、轻量进入动效、按钮反馈和暗色主题适配；语音通话浮层与折叠条视觉更统一。
- 停止说明：本轮只优化模拟面试会话页表现层，不修改 `/admin/**`、API、路由、数据库、后端业务流程、STT/TTS、SSE、发送、挂断或面试状态逻辑。
## 验收反馈修正：内置离线 STT manifest 与浏览器缓存 runtime（2026-05-27）
- 当前任务所属模块：模拟面试前端离线语音识别资源下载与 Worker 运行时加载。
- 前端文件定位：`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json`、`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js`、`frontend/app/src/utils/offlineVoiceModelCache.js`、`frontend/app/src/composables/useSpeechToText.js`、`frontend/app/src/workers/sherpaSpeechWorker.js` 以及对应测试。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或用户音频上传链路修改。
- 本轮修改文件清单：新增本地 `manifest.json`，让 `/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json` 不再命中 SPA HTML fallback；新增 `runtime.js`，负责从浏览器 Cache API 读取官方 sherpa-onnx WASM/JS/data 文件并适配现有 Worker 的 `start/acceptWaveform/stop` 接口；`offlineVoiceModelCache.js` 支持 manifest 文件项的远程 `url` 和 `runtime` 状态持久化；`useSpeechToText.js` 离线 Worker 初始化时使用缓存状态中的 runtime；`sherpaSpeechWorker.js` 等待 runtime 初始化完成后再创建识别器，并改为 classic worker 以支持 `importScripts`；`useTextToSpeech.js` 补齐每条 utterance watchdog，浏览器不触发 `onend/onerror` 时会主动释放播报状态；`InterviewSessionView.test.js` 同步覆盖 TTS 结束后 1.5 秒尾音保护再恢复收音。
- 问题原因：仅把错误从 `Unexpected token '<'` 转成“清单不是 JSON”还不够，因为项目静态目录仍没有真实 `manifest.json`。用户点击下载时依然只能得到“模型未部署”的提示，无法完成资源包下载。
- 前端实现方案：小体积 manifest/runtime 随前端静态资源发布；大体积 sherpa-onnx 浏览器包继续按需从官方 Hugging Face Space 下载并写入浏览器 Cache API，不提交进源码仓库。runtime 在 Worker 内优先从 Cache API 生成 Blob URL 加载官方 API、Emscripten 主 JS、WASM 和 data 包；音频采样在 runtime 适配层下采样到 16k，再送入 sherpa-onnx 在线识别器。
- 数据存储方案：继续使用既有 `ai-resume-offline-voice-models-v1` Cache API 缓存模型文件，状态元数据新增持久化 `runtime` 字段；不新增后端存储，不上传用户音频。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮可下载清单与 runtime 缓存修复。
- 编译结果：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，6 个测试文件 / 112 个用例通过。
- 构建结果：`npm.cmd run build` 通过，仍保留既有 `@vueuse/core` PURE annotation 构建提示。
- 当前功能验收说明：访问 `/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json` 将返回 JSON 清单；下载按钮会按清单拉取官方 sherpa-onnx 浏览器资源并缓存，下载完成后离线 Worker 使用本地缓存资源初始化识别器。
- 停止说明：本轮只补齐 sherpa-onnx STT 下载清单和 runtime 加载，不接入 Kokoro，不新增后端模型代理，不把大模型二进制文件提交进源码。

## 验收反馈修正：离线语音模型清单 HTML fallback 友好报错（2026-05-26）
- 当前任务所属模块：模拟面试前端离线语音资源下载与缓存边界。
- 前端文件定位：`frontend/app/src/utils/offlineVoiceModelCache.js`、`frontend/app/src/__tests__/utils/offlineVoiceModelCache.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段或模型托管策略修改。
- 本轮修改文件清单：`offlineVoiceModelCache.js` 将 manifest 读取从直接 `response.json()` 改为先读取文本、识别 HTML fallback、再手动解析 JSON；`offlineVoiceModelCache.test.js` 增加 manifest 返回 SPA HTML、JSON 损坏、下载清单失败写入 failed 状态的回归测试。
- 问题原因：当 `/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json` 没有真实部署时，Vite/SPA 或线上网关可能返回 `index.html`，旧逻辑直接解析 JSON 会把底层 `Unexpected token '<'` 暴露给用户，且下载状态没有稳定进入 `failed`，不利于设置页展示和用户删除残留状态。
- 前端实现方案：`readModelManifest` 读取 `Content-Type` 和响应文本，若发现 `text/html`、`<!DOCTYPE html>` 或 `<html>`，抛出“离线语音模型清单不是 JSON，请确认模型文件已部署到 ...”的明确错误；若 JSON 内容损坏，抛出“清单解析失败，请检查 ...”；`downloadModelFromManifest` 在清单加载/解析失败时写入 `failed`、`progress: 0`、`manifestUrl` 和空文件列表，设置页可继续按失败状态展示删除资源包入口。
- 数据存储方案：不新增本地存储字段，不修改 Cache API 结构；仅在清单读取失败时复用既有模型状态元数据写入 failed。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮修复原因、范围和验证结果。
- 编译结果：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js` 已通过，后续继续执行设置页回归和生产构建。
- 当前功能验收说明：离线模型目录未部署或 manifest 路径被 SPA fallback 接管时，用户不再看到 `Unexpected token '<'`，而会看到明确的模型清单部署提示；失败状态可被设置页删除资源包能力清理。
- 停止说明：本轮只修复离线语音模型清单下载错误提示与失败状态，不继续推进真实模型文件部署、CDN 托管、后端接口或 Kokoro。
## 验收反馈修正：离线 sherpa-onnx Worker 初始化 DataCloneError（2026-05-30）
- 当前任务所属模块：模拟面试前端离线语音识别稳定化，修复已下载 sherpa-onnx 离线资源包后 Worker 初始化阶段直接抛 `DataCloneError`，导致离线引擎资源完全无法进入识别流程的问题。
- 前端文件定位：`frontend/app/src/composables/useSpeechToText.js`、`frontend/app/src/utils/voiceModelDevServer.js`、`frontend/app/vite.config.js`、`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js`、`frontend/app/voice-models-local/sherpa-onnx/zh-cn-streaming/runtime.js` 以及对应测试。
- 问题原因：离线模型状态存放在 Vue `ref` 中，`modelStatus.value.files` 被 Vue 转成响应式代理数组；旧代码把这个代理数组原样放进 `offlineWorker.postMessage({ type: 'init', config })`，浏览器 structured clone 无法克隆代理数组，因此控制台报 `DataCloneError: [object Array] could not be cloned`。这发生在 Worker 初始化消息发送阶段，sherpa WASM 与识别器还没有机会启动。
- 前端实现方案：`buildOfflineWorkerConfig()` 将文件清单显式复制成只含 `path/url/size` 的普通对象数组，保证 Worker 初始化消息可 structured clone；`postMessage` 同步失败时进入 `offline-worker-error`，避免未捕获 Promise 错误。Worker runtime URL 增加版本参数，避免浏览器继续使用旧 runtime 适配脚本；开发环境模型资源服务改为按扩展名返回 MIME，防止 `.js` 被当成 `application/octet-stream` 后 `importScripts` 失败；runtime 从 Cache 读取旧资源时也会重新包装 `.js/.wasm` Blob 类型。
- 测试与验证：新增 `useSpeechToText.test.js` 回归用例，旧代码会在 structured clone 时复现 `DataCloneError`；新增 `sherpaRuntimeAsset.test.js` 和 `voiceModelDevServer.test.js` 覆盖 sherpa API wrapper 加载与模型资源 MIME。修复后 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，6 个测试文件 / 88 个用例通过。
- 停止说明：本轮只处理离线 STT Worker 初始化消息不可克隆和本地模型资源 MIME，仍不新增后端 API、不上传用户音频、不提交或替换大体积模型文件。
## 验收反馈修正：Chrome 默认中文音色与设置页排版挤压（2026-05-31）

- 当前任务所属模块：设置中心语音偏好、浏览器系统 TTS 试听与语音面试播报 fallback 链路。
- 前端文件定位：`frontend/app/src/composables/useTextToSpeech.js`、`frontend/app/src/views/settings/SettingsView.vue`、`frontend/app/src/__tests__/composables/useTextToSpeech.test.js`、`frontend/app/src/__tests__/views/SettingsView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段、SSE、AI 回复生成或音频上传链路。
- 本轮修改文件清单：`useTextToSpeech.js` 调整 Chrome voice 选择策略；`SettingsView.vue` 调整“AI 播报声音”行布局与实际音色提示文案；`useTextToSpeech.test.js`、`SettingsView.test.js` 增加 Chrome 单一性别 voice 与布局回归用例。
- 问题原因：Chrome 可能只暴露一个明确性别的本地中文 voice，例如 `Microsoft Kangkang - Chinese (Simplified, PRC)`。旧逻辑会把它强行当作“默认中文自然音色”使用，导致默认、男声、女声听起来仍像同一个 voice。同时上一轮把“当前实际 voice”提示作为第三个 flex 子项放进主行，和标题、下拉框、试听按钮争横向空间，造成截图中的挤压。
- 前端实现方案：当 `voicePreferredType` 为 `natural_zh` 且 Chrome 只返回一个明确性别的本地中文 voice 时，不再主动把它绑定到 `SpeechSynthesisUtterance.voice`，而是交回浏览器默认中文 voice 策略；当用户选择 `female/male` 但当前浏览器没有暴露对应性别 voice 时，也不再强行使用相反性别 voice。手势触发试听时仍保留 Chrome voice 异步加载等待：如果等待不到高质量 Natural/Neural voice，则允许回退到本地老式 voice，避免只剩远程 Google voice 时无声。设置页将状态提示放到下方独立一行，主行只保留说明、下拉和试听按钮。
- 数据存储方案：不新增本地存储字段，不修改 Cache API，不修改 Kokoro manifest，不修改后端存储；本轮只改变运行时浏览器 voice 选择和设置页展示结构。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮 Chrome 单一 voice 默认策略、布局修正、边界和验证结果。
- 编译结果：`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js` 通过，32 个用例通过；`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，46 个用例通过；相关回归 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useOfflineTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，4 个测试文件 / 117 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：Chrome 只有单一明确性别中文 voice 时，“默认中文自然音色”不再在设置页显示为 Kangkang 这类具体性别 voice，也不会主动覆盖用户原本浏览器默认中文发声策略；男声/女声缺失时页面会明确提示由浏览器默认或当前可用 voice 决定。设置页“AI 播报声音”的实际音色提示移到下方，不再挤压主行。
- 停止说明：本轮只修复 Chrome 系统 TTS voice 选择与设置页排版问题，不新增真实 Kokoro 模型资源、不新增后端 TTS、不上传音频、不继续扩展更多音色。
## 真实 Kokoro 本地模型接入（2026-05-31）
- 当前任务所属模块：设置中心离线增强高品质语音包、Kokoro 离线 TTS 试听与模拟面试语音播报链路。
- 前端文件定位：`frontend/app/src/composables/useOfflineTextToSpeech.js`、`frontend/app/src/utils/offlineVoiceModelCache.js`、`frontend/app/src/utils/kokoroTtsDevServer.js`、`frontend/app/vite.config.js`、`frontend/app/src/__tests__/composables/useOfflineTextToSpeech.test.js`、`frontend/app/src/__tests__/utils/offlineVoiceModelCache.test.js`、`frontend/app/src/__tests__/utils/kokoroTtsDevServer.test.js`。
- 后端文件定位：本轮不修改 Java 后端接口、服务、数据库字段、SSE 或用户音频上传链路；真实 Kokoro 合成仅接入 Vite 开发服务同源中间件。
- 本轮修改文件清单：新增 `kokoroTtsDevServer.js` 封装本地 sherpa-onnx-node Kokoro 合成、WAV 编码、speaker 映射和请求限制；`vite.config.js` 在 `voice-models-local/kokoro/zh-cn-dual/model.int8.onnx` 存在时暴露 `/voice-models/kokoro/zh-cn-dual/synthesize`；`offlineVoiceModelCache.js` 透传并持久化 `synthesisEndpoint`，且旧占位 ready 缓存不会跳过新 manifest；`useOfflineTextToSpeech.js` 在 manifest 暴露合成端点时优先 POST 获取 WAV，不再要求 placeholder Worker；本地 `voice-models-local/kokoro/zh-cn-dual/manifest.json` 标记 `placeholder: false` 并声明真实模型文件。
- 问题原因：当前 `public/voice-models/kokoro/zh-cn-dual` 仍是占位包，`runtime.js` 只会抛出“尚未部署真实模型”提示；用户下载到本地的是 sherpa-onnx Kokoro v1.1 多语言模型文件，不是浏览器可直接执行的 Kokoro Worker runtime。Chrome 无法依赖系统 TTS 提供稳定中文自然音色，因此需要把真实模型通过本地同源合成端点接入前端播放链路。
- 前端实现方案：继续复用现有 Cache API 状态和混合 TTS 编排；真实 Kokoro manifest 新增 `synthesisEndpoint` 后，离线 TTS runtime 直接向同源端点提交文本、voiceId 和语速，端点使用 `sherpa-onnx-node` 加载 `model.int8.onnx`、`voices.bin`、`tokens.txt`、中文 lexicon 与 FST，生成 24kHz WAV 返回给浏览器解码播放。女声映射 `sid=3`，男声映射 `sid=58`。
- 数据存储方案：浏览器状态元数据新增持久化 `synthesisEndpoint` 字段；真实大模型文件继续放在已忽略的 `voice-models-local/`，不提交到源码仓库，不新增后端数据库或用户数据字段。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录真实 Kokoro 本地模型接入、旧占位缓存升级、测试和构建结果。
- 编译结果：`npm.cmd test -- --run src/__tests__/utils/settingsPreferences.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/kokoroTtsDevServer.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useOfflineTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，7 个测试文件 / 147 个用例通过；`npm.cmd run build` 通过。
- 运行验证：已用本地 Vite 服务请求 `http://127.0.0.1:5174/voice-models/kokoro/zh-cn-dual/synthesize`，生成 `.tmp-kokoro/vite-endpoint-test.wav`，文件头为 `RIFF/WAVE`。
- 当前功能验收说明：开发环境中重新下载/刷新 Kokoro 资源后，`tts:kokoro` 会从占位包升级到真实 manifest，设置页 Kokoro 女声/男声试听会走真实本地模型端点，Chrome 与 Edge 不再依赖浏览器系统 TTS 的中文 voice 差异。
- 停止说明：本轮只完成真实 Kokoro 本地模型接入与开发期同源合成端点，不继续扩展更多音色、不新增云端 TTS、不上传用户音频、不改 Java 后端生产部署策略。

## 撤销 Kokoro 高品质离线音色包（2026-05-31）
- 当前任务所属模块：设置中心离线增强、模拟面试语音播报与离线语音识别资源边界。
- 前端文件定位：`frontend/app/src/views/settings/SettingsView.vue`、`frontend/app/src/composables/useTextToSpeech.js`、`frontend/app/src/utils/offlineVoiceModelCache.js`、`frontend/app/src/utils/settingsPreferences.js`、`frontend/app/vite.config.js`、`frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js` 以及对应测试。
- 本轮修改文件清单：移除设置页 Kokoro/高品质离线音色包卡片、离线 TTS 合成端点、Kokoro dev server、Kokoro composable 与相关测试；清理 `public/voice-models/kokoro`、`dist/voice-models/kokoro`、`voice-models-local/kokoro`、`.tmp-kokoro`；卸载 `kokoro-js` 与 `sherpa-onnx-node`，保留 `sherpa-onnx` 识别引擎依赖与 sherpa-onnx STT 静态资源。
- 前端实现方案：模拟面试播报回到浏览器系统 TTS，不再提供 Kokoro 下载、试听、合成或缓存入口；设置页离线增强只保留 sherpa-onnx 语音识别引擎下载/删除能力；偏好归一化会丢弃旧 `offlineTts*` 字段，避免历史本地缓存继续驱动离线 TTS 分支。
- 数据存储方案：不新增本地存储、接口字段或后端存储；保留既有 `ai-resume-offline-voice-models-v1` Cache API 给 sherpa-onnx STT 使用，旧 Kokoro/TTS 状态不再进入生产逻辑。
- 清理验证：残留扫描 `rg "Kokoro|kokoro|offlineTts|useHybridTextToSpeech|useOfflineTextToSpeech|synthesisEndpoint|tts:kokoro|高品质|音色包|model\\.int8\\.onnx|voices\\.bin"` 在生产代码、脚本、静态资源和构建产物中无命中；仅 `settingsPreferences.test.js` 保留 legacy `offlineTts*` 字段作为清洗回归样本；Kokoro 目录扫描无结果。
- 编译结果：`npm.cmd test -- --run src/__tests__/utils/settingsPreferences.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/workers/sherpaSpeechWorker.test.js` 通过，9 个测试文件 / 175 个用例通过；`npm.cmd run build` 通过。
- 当前功能验收说明：项目不再包含高品质离线音色包/Kokoro 本地合成链路；语音播报使用浏览器系统 TTS；离线刚需只保留 sherpa-onnx 语音识别引擎和浏览器本地缓存资源。
- 停止说明：本轮只做 Kokoro/离线 TTS 清退与 STT 保留，不新增云端 TTS、不新增后端语音接口、不删除 sherpa-onnx 识别模型资源。
## 设置中心短面板布局补全（2026-05-31）
- 当前任务所属模块：设置中心账号资料、面试偏好离线增强、新手引导三个已有前端区域。
- 前端文件定位：`frontend/app/src/views/settings/SettingsView.vue`、`frontend/app/src/__tests__/views/SettingsView.test.js`。
- 后端文件定位：本轮不涉及后端接口、服务、数据库字段、SSE、AI 回复生成或语音上传链路修改。
- 本轮修改文件清单：`SettingsView.vue` 补全账号资料说明卡片、离线增强双栏说明布局和新手引导入口说明；`SettingsView.test.js` 更新设置页布局回归断言。
- 前端实现方案：账号资料在基础信息下增加“账号内权益 / 本机偏好 / 常用数据”三项说明，避免页面下半区空白；离线增强保持只管理 sherpa-onnx 语音识别引擎，增加“只保留语音识别”的顶部说明和工作方式侧栏，不恢复离线 TTS 或音色包入口；新手引导增加简历诊断、模拟面试、模板与社区、会员与设置四个入口说明卡片，底部按钮继续复用原有 `showOnboarding` 弹窗逻辑。
- 数据存储方案：不新增本地存储字段，不修改 Cache API key，不修改账号数据结构；离线增强仍只使用现有 `stt:sherpa_onnx:zh_cn` 识别资源状态。
- stage 更新说明：已同步更新 `frontend/tasks/stage.md`，记录本轮设置页空白区布局补全、边界和验证结果。
- 编译结果：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 40 个用例通过。
- 构建结果：`npm.cmd run build` 通过。
- 当前功能验收说明：账号资料、新手引导和离线增强页面不再显得大面积空白；离线增强仍只保留 sherpa-onnx 语音识别资源下载/删除能力，未恢复离线音色包或离线合成入口。
- 停止说明：本轮只处理用户指出的三个设置页空白区域，不继续扩展新的设置模块、后端能力或语音合成功能。
