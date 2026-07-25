# TASK 68 用户自定义 AI 接入

## 二十五、代码审查问题修复记录（2026-06-04）

### 当前任务所属模块

用户自定义 AI TTS 配置、OpenAI 兼容 TTS 连通测试、Chat Completions TTS Provider 协议校验。

### 前端文件定位

- `frontend/app/src/__tests__/views/SettingsView.test.js`
- 前端完整记录见 `frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

### 后端文件定位

- `server/src/main/java/com/airesume/server/service/impl/UserAiConfigServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/UserTtsConnectivityTestServiceImpl.java`
- `server/src/test/java/com/airesume/server/service/impl/UserAiConfigServiceImplTest.java`
- `server/src/test/java/com/airesume/server/service/impl/UserTtsConnectivityTestServiceImplTest.java`

### 本轮修改文件清单

- `UserAiConfigServiceImpl` 在 TTS 请求归一化时保留 `endpointPath` 和 `ttsProvider`，避免测试、试听、保存前验证丢失前端传入的自定义端点。
- `UserTtsConnectivityTestServiceImpl` 对 Chat Completions TTS 响应解析 `choices[0].message.audio.data`，要求 base64 可解码且音频字节非空，避免普通文本 JSON 被误判为 TTS 连通成功。
- `UserAiConfigServiceImplTest` 补充保存和连通测试时自定义端点路径透传的回归断言。
- `UserTtsConnectivityTestServiceImplTest` 补充 Chat Completions TTS 缺少音频数据失败、有效音频成功的回归测试。
- `SettingsView.test.js` 将选择框高度断言同步到当前 `.cai-form` 通用选择器与 `.cai-tts-discover-btn` 结构。

### 实现方案

- TTS 表单参数仍统一经过 `normalizeTtsRequest`，只在既有对象中补齐端点路径和 Provider 标识，不新增接口字段。
- Chat Completions TTS 连通测试复用试听合成的音频解析边界，缺失、非法 base64 或空音频统一返回失败，不向前端暴露 API Key、baseUrl 或完整响应体。
- 前端本轮只修正测试断言，不调整页面结构、样式或交互逻辑。

### 数据存储方案

无数据库变更。不新增表、字段、索引或迁移脚本；继续复用既有 `user_ai_config.tts_endpoint_path` 与 `tts_provider` 字段。

### stage 更新说明

`tasks/stage.md` 与 `frontend/tasks/stage.md` 已记录本轮代码审查修复范围、验证结果和停止边界。

### 编译结果

- RED 验证：`mvn.cmd -q "-Dtest=UserAiConfigServiceImplTest,UserTtsConnectivityTestServiceImplTest" test` 在旧实现下失败，复现自定义端点丢失和 Chat Completions 缺少音频仍成功的问题。
- GREEN 验证：同命令在修复后通过。
- 后端回归验证：`mvn.cmd -q "-Dtest=UserAiConfigServiceImplTest,UserTtsConnectivityTestServiceImplTest,UserTtsSpeechServiceImplTest,CriticalEndpointRateLimitFilterTest,AdminAiEngineConnectivityControllerTest,AdminCustomAiStatsControllerTest,AiModelDiscoveryServiceImplTest,UserAiUsageStatsServiceImplTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。

### 构建结果

- 前端设置页测试：`npm.cmd test -- src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 43 个用例。
- 前端回归验证：`npm.cmd test -- src/__tests__/api/interview.test.js src/__tests__/api/userAiConfig.test.js src/__tests__/api/admin.aiEngines.test.js src/__tests__/composables/useCloudTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，7 个测试文件 / 117 个用例。
- 前端构建验证：`npm.cmd run build` 通过。

### 当前功能验收说明

用户填写自定义 TTS 端点后，连通测试、试听和保存前验证会把端点路径继续传给下游 TTS 服务；MiMo 等 Chat Completions TTS Provider 只有在返回真实可解码音频数据时才会显示连通成功。

### 停止，不继续下一个功能

本轮仅修复代码审查指出的 3 个问题，不继续扩展 Provider 协议、TTS 计费、音频存储、设置页 UI 或其它自定义 AI 能力。

## 二十四、自定义 AI 模型列表获取记录（2026-06-03）
### 当前任务所属模块
用户自定义 AI Provider 配置、管理端 AI 引擎配置、OpenAI 兼容 `/models` 模型发现、关键端点限流。

### 前端文件定位

- `frontend/app/src/api/userAiConfig.js`
- `frontend/app/src/api/admin/aiEngines.js`
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/__tests__/api/userAiConfig.test.js`
- `frontend/app/src/__tests__/api/admin.aiEngines.test.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`
- `frontend/app/src/__tests__/views/AdminAiEngineView.test.js`
- 前端完整记录见 `frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

### 后端文件定位

- `server/src/main/java/com/airesume/server/service/AiModelDiscoveryService.java`
- `server/src/main/java/com/airesume/server/service/impl/AiModelDiscoveryServiceImpl.java`
- `server/src/main/java/com/airesume/server/dto/ai/AiModelOption.java`
- `server/src/main/java/com/airesume/server/dto/ai/AiModelDiscoveryResponse.java`
- `server/src/main/java/com/airesume/server/dto/user/UserAiModelsRequest.java`
- `server/src/main/java/com/airesume/server/dto/admin/AdminAiEngineModelsRequest.java`
- `server/src/main/java/com/airesume/server/controller/UserAiConfigController.java`
- `server/src/main/java/com/airesume/server/controller/AdminController.java`
- `server/src/main/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilter.java`
- `server/src/test/java/com/airesume/server/service/impl/AiModelDiscoveryServiceImplTest.java`
- `server/src/test/java/com/airesume/server/controller/AdminAiEngineConnectivityControllerTest.java`
- `server/src/test/java/com/airesume/server/service/impl/UserAiConfigServiceImplTest.java`
- `server/src/test/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilterTest.java`

### 本轮修改文件清单

- 新增共享模型发现服务，调用 OpenAI 兼容 `GET {baseUrl}/models`，解析 `data[].id` 并返回去重模型选项。
- 新增用户端 `POST /api/user/ai-config/models`，请求体为 `baseUrl/apiKey`，不保存配置，不回显完整 Key。
- 新增管理端 `POST /api/admin/ai-engines/models`，编辑态 `apiKey` 为空时按已有连通测试逻辑复用已保存真实密钥。
- 用户端模型发现纳入 `CriticalEndpointRateLimitFilter`，沿用每用户或 IP 每分钟 5 次的强度。
- 设置页和管理端弹窗的模型名字段改为 `el-select filterable allow-create`，仍支持手动输入；“获取模型”失败不清空已填模型名。

### 实现方案

- 后端只支持 OpenAI 兼容 `/models`，复用公网 HTTPS URL 校验、统一 HTTP 客户端和超时配置；上游异常统一返回失败结果，避免把异常或 Key 透传给前端。
- 用户端必须填写真实 API Key 才能获取模型，不复用已保存用户 Key。
- 管理端编辑态允许不填 Key，由后端按引擎 `id` 读取已保存密钥，保持与现有连通测试一致。
- 前端模型下拉只作为辅助候选来源，保存和连通测试仍读取最终表单里的模型名，保证不支持 `/models` 的供应商仍可手动填写。

### 数据存储方案

无数据库变更。模型列表只用于当前表单候选项，不落库、不新增表、不新增字段、不新增迁移脚本。

### stage 更新说明

`tasks/stage.md` 与 `frontend/tasks/stage.md` 已记录本轮模型列表获取范围、验证结果和停止边界。

### 编译结果

- 后端目标测试：`mvn.cmd -q "-Dtest=AiModelDiscoveryServiceImplTest,AdminAiEngineConnectivityControllerTest,UserAiConfigServiceImplTest,CriticalEndpointRateLimitFilterTest" test` 通过。
- 后端编译：`mvn.cmd -q -DskipTests compile` 通过。

### 构建结果

- 前端目标测试：`npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，4 个测试文件 / 60 个用例。
- 前端构建：`npm.cmd run build` 通过。

### 当前功能验收说明

用户在设置页填写 API 基础地址和 API Key 后，可手动点击“获取模型”填充模型候选；管理端新增或编辑 AI 引擎时也可获取模型候选。获取成功且当前模型为空时默认选中第一项；获取失败只提示错误并保留现有手动输入，不影响后续保存或连通测试。

### 停止，不继续下一个功能

本轮只实现 OpenAI 兼容模型列表获取，不扩展 Provider 专用协议、模型能力检测、价格信息、自动路由、自动保存或更多 Provider 模板。

## 二十三、自定义 TTS 在语音面试中真实播放记录（2026-06-03）

### 当前任务所属模块

模拟面试语音播报、用户自定义 AI Provider TTS 运行时解析、OpenAI 兼容 `/audio/speech` 合成。

### 前端文件定位

- `frontend/app/src/api/interview.js`
- `frontend/app/src/composables/useCloudTextToSpeech.js`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/__tests__/api/interview.test.js`
- `frontend/app/src/__tests__/composables/useCloudTextToSpeech.test.js`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`
- 前端完整记录见 `frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

### 后端文件定位

- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/main/java/com/airesume/server/dto/interview/TtsCapabilityResponse.java`
- `server/src/main/java/com/airesume/server/dto/interview/TtsSpeechRequest.java`
- `server/src/main/java/com/airesume/server/dto/user/ResolvedTtsConfig.java`
- `server/src/main/java/com/airesume/server/service/UserTtsSpeechService.java`
- `server/src/main/java/com/airesume/server/service/impl/UserTtsSpeechServiceImpl.java`
- `server/src/main/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilter.java`
- `server/src/test/java/com/airesume/server/service/impl/UserTtsSpeechServiceImplTest.java`
- `server/src/test/java/com/airesume/server/controller/InterviewControllerTest.java`
- `server/src/test/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilterTest.java`

### 本轮修改文件清单

- 新增 `GET /api/interview/session/{sessionId}/tts-capability`，用于语音面试页判断本场是否可使用用户自定义 TTS。
- 新增 `POST /api/interview/session/{sessionId}/tts`，请求体 `{ text }`，返回 `audio/mpeg`，只允许本人、进行中、语音面试会话调用。
- 新增 `UserTtsSpeechService`，按 `interview -> default` 解析启用且完整的 TTS 配置，`resume` 永不参与 TTS 运行时解析。
- TTS 合成复用 `AiCredentialCrypto` 解密 Key、复用公网 HTTPS URL 校验，调用 OpenAI 兼容 `{ttsBaseUrl}/audio/speech`，不持久化音频，不返回 Key/baseUrl/model。
- `CriticalEndpointRateLimitFilter` 对面试 TTS 合成接口新增关键端点限流，匹配顺序早于通用面试 action 限流。
- 前端新增面试 TTS capability 与合成 API 封装，新增 `useCloudTextToSpeech` 管理云端音频队列、Blob URL 释放和失败降级。
- 语音面试页启动后查询 TTS capability；云端 TTS 可用时开场白和流式回复改走后端音频合成播放，不再调用浏览器 `speechSynthesis.speak`。
- 云端 TTS 单句失败时，本场后续关闭云端 TTS，只提示一次“云端语音暂不可用，已切回浏览器播报”，并用浏览器 TTS 继续播报，不影响面试继续。

### 实现方案

- TTS 配置解析只读取 `interview` 和 `default`，与简历诊断、简历润色、JD 匹配完全隔离。
- 后端合成接口只做播放层能力，不接入平台 AI fallback，不消耗平台 AI 额度，也不写入 `user_ai_daily_usage`。
- 前端保留既有浏览器 TTS 作为兜底；新增云端 TTS facade 与浏览器 TTS 组合后提供统一 `isSupported/isSpeaking/speak/speakStreaming/flushRemaining/stop` 给 `useVoiceCall`。
- 停止播报、切换模式、结束会话和组件卸载都会停止云端音频队列并释放 Blob URL。

### 数据存储方案

无数据库变更。继续复用既有 `user_ai_config` TTS 字段；本轮不新增表、字段、迁移脚本、TTS 计费表或音频存储。

### 验证结果

- 前端 RED 验证：新增 API、`useCloudTextToSpeech` 和面试页云端 TTS 用例后，旧实现因缺少 API/composable/capability 查询和云端播放路由失败。
- 前端 GREEN 验证：`npm.cmd test -- --run src/__tests__/api/interview.test.js src/__tests__/composables/useCloudTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 55 个用例。
- 后端定向验证：`mvn.cmd -q "-Dtest=UserTtsSpeechServiceImplTest,UserAiConfigServiceImplTest,CriticalEndpointRateLimitFilterTest,InterviewControllerTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 前端构建验证：`npm.cmd run build` 通过。

### 当前功能验收说明

用户在“面试对话”或“通用兜底”配置中完成自定义 TTS 后，语音面试的 AI 面试官开场白和后续流式追问会优先请求后端 `/tts` 合成 `audio/mpeg` 并播放；没有可用自定义 TTS 或云端 TTS 临时失败时，继续使用原浏览器 TTS。

### 停止，不继续下一个功能

本轮只把用户自定义 OpenAI 兼容 TTS 接入语音面试播报，不接入简历诊断，不实现 STT，不新增 TTS 计费统计、音频存储、数据库迁移、更多 Provider 协议或独立语音页面。

## 二十二、TTS 配置折叠与通用兜底支持记录（2026-06-03）

### 当前任务所属模块

用户设置中心、自定义 AI Provider 配置、TTS 配置展示边界、后端 TTS 保存边界。

### 前端文件定位

- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/api/userAiConfig.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`
- `frontend/app/src/__tests__/api/userAiConfig.test.js`
- 前端完整记录见 `frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

### 后端文件定位

- `server/src/main/java/com/airesume/server/service/impl/UserAiConfigServiceImpl.java`
- `server/src/test/java/com/airesume/server/service/impl/UserAiConfigServiceImplTest.java`

### 本轮修改文件清单

- 设置页 TTS 配置区在“通用兜底”和“面试对话”配置中显示，选择“简历能力”时不显示。
- TTS 配置区改为可折叠面板，默认只展示标题、说明和状态胶囊，展开后才显示地址、模型、Key、音色和测试按钮。
- “未启用/已填写”状态胶囊改为原生 flex 居中样式，避免文字视觉偏上。
- 配置卡片的“已配置 TTS”标记只在通用兜底和面试配置卡片上展示，避免简历配置误导用户。
- 前端保存 payload 只在 `configType=default/interview` 时携带 TTS 字段，简历配置保存时不提交 TTS。
- 后端 `UserAiConfigServiceImpl` 只对 `default/interview` 配置回显和保存 TTS；简历配置即使请求体携带 TTS 字段也会清空，不执行 TTS 连通测试。
- 测试补齐简历配置不回显、不保存 TTS，以及通用兜底和面试配置仍可测试和保存 TTS 的断言。

### 实现方案

- TTS 的当前产品用途是后续云端面试官播报；“面试对话”可以单独配置，未配置时可由“通用兜底”承接。
- 简历诊断、简历润色、JD 匹配等简历能力不需要 TTS；简历配置移除该区块后可以减少表单高度和视觉干扰。
- 后端保存边界与前端展示边界保持一致，避免绕过前端直接给 `resume` 写入 TTS 配置。

### 数据存储方案

无数据库变更。继续复用既有 `user_ai_config` TTS 预留字段；本轮不新增迁移脚本。

### 验证结果

- RED 验证：旧实现下 `UserAiConfigServiceImplTest` 新增用例失败，复现通用兜底配置无法回显/保存 TTS；旧前端设置页测试失败，复现通用兜底缺少 TTS 区块且 TTS 区不可折叠。
- GREEN 验证：`mvn.cmd -q "-Dtest=UserAiConfigServiceImplTest" test` 通过。
- 后端回归验证：`mvn.cmd -q "-Dtest=UserAiConfigServiceImplTest,CriticalEndpointRateLimitFilterTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- GREEN 验证：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过。
- 前端回归验证：`npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/views/SettingsView.test.js` 通过。
- 前端构建验证：`npm.cmd run build` 通过。

### 停止，不继续下一个功能

本轮只修正 TTS 配置归属、折叠交互和状态胶囊布局，不切换模拟面试云端 TTS 播放链路，不实现音频流推送、TTS 计费统计或更多 TTS Provider 协议。

## 二十一、用户自定义 AI TTS 配置与连通测试记录（2026-06-03）

### 当前任务所属模块

用户设置中心、自定义 AI Provider 配置、用户自定义 TTS 配置保存、OpenAI 兼容 `/audio/speech` 连通测试。

### 前端文件定位

- `frontend/app/src/api/userAiConfig.js`
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/__tests__/api/userAiConfig.test.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`
- 前端完整记录见 `frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

### 后端文件定位

- `server/src/main/java/com/airesume/server/controller/UserAiConfigController.java`
- `server/src/main/java/com/airesume/server/dto/user/UserAiConfigRequest.java`
- `server/src/main/java/com/airesume/server/dto/user/UserAiConfigResponse.java`
- `server/src/main/java/com/airesume/server/dto/user/UserTtsConnectivityTestRequest.java`
- `server/src/main/java/com/airesume/server/dto/user/UserTtsConnectivityTestResponse.java`
- `server/src/main/java/com/airesume/server/service/UserAiConfigService.java`
- `server/src/main/java/com/airesume/server/service/UserTtsConnectivityTestService.java`
- `server/src/main/java/com/airesume/server/service/impl/UserAiConfigServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/UserTtsConnectivityTestServiceImpl.java`
- `server/src/main/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilter.java`
- `server/src/test/java/com/airesume/server/service/impl/UserAiConfigServiceImplTest.java`
- `server/src/test/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilterTest.java`

### 本轮修改文件清单

- `UserAiConfigRequest` 新增可选 TTS 字段：`ttsBaseUrl`、`ttsApiKey`、`ttsModel`、`ttsVoiceId`。
- `UserAiConfigResponse` 新增 TTS 回显字段，`ttsApiKey` 只返回脱敏值，并通过 `ttsConfigured` 标记当前配置是否已配置 TTS。
- 新增 `POST /api/user/ai-config/test-tts-connectivity`，用于保存前独立测试 OpenAI 兼容 `/audio/speech`。
- 新增 `UserTtsConnectivityTestService`，向 `{baseUrl}/audio/speech` 发送最小语音合成请求验证配置。
- `UserAiConfigServiceImpl` 保存 TTS 配置时复用 `AiCredentialCrypto` 加密 API Key，保存和测试前均复用 `PublicHttpsUrlValidator` 校验公网 HTTPS 地址。
- `CriticalEndpointRateLimitFilter` 对 TTS 连通测试新增同用户或同 IP 每分钟 5 次限制。
- 设置页自定义 AI 表单新增 TTS 配置区，支持填写 TTS Base URL、API Key、模型和音色 ID，并提供 TTS 连通测试按钮。

### 前端实现方案

- 继续复用现有设置页“自定义 AI 接入”面板，不新增独立页面或新的设置分组。
- TTS 字段采用全填或全不填规则；只要填写任一 TTS 字段，保存和连通测试前必须补齐 `ttsBaseUrl`、`ttsApiKey`、`ttsModel`、`ttsVoiceId`。
- 后端返回的 TTS Key 只作为脱敏展示，不回填为可直接提交的明文 Key。
- 已保存配置列表展示“已配置 TTS”状态，方便用户区分只配置 LLM 与同时配置 TTS 的配置槽。
- 本轮不修改模拟面试播报运行时，现有面试语音播报仍使用浏览器 `speechSynthesis`。

### 后端实现方案

- TTS 配置仍落在既有 `user_ai_config` 表的预留字段中，不新增表或字段。
- TTS API Key 与聊天模型 API Key 采用同一套 `AiCredentialCrypto` 加密和脱敏策略，避免明文入库或接口回显。
- TTS 连通测试固定使用 OpenAI 兼容语音接口：`POST {baseUrl}/audio/speech`，请求体包含 `model`、`voice`、`input` 和 `response_format=mp3`。
- 连通测试只判断上游响应是否成功返回音频响应，不把音频内容保存到本地或数据库。
- TTS 连通测试失败返回自定义 AI 配置类错误，不自动切换平台 AI，不消耗平台额度。

### 数据存储方案

- 本轮无数据库迁移。
- 继续复用 `user_ai_config` 已存在字段：`tts_base_url`、`tts_api_key`、`tts_model`、`tts_voice_id`。
- `db/schema.sql` 与 `server/db/schema.sql` 已在前序任务中包含这些字段，本轮不新增 SQL。

### stage 更新说明

`tasks/stage.md` 与 `frontend/tasks/stage.md` 已记录本轮 TTS 配置和连通测试范围、验证结果和停止说明。

### 编译结果

- RED 验证：新增后端 TTS DTO / 服务测试后，旧实现因缺少 TTS 请求响应类型与服务方法失败。
- GREEN 验证：`mvn.cmd -q "-Dtest=UserAiConfigServiceImplTest,CriticalEndpointRateLimitFilterTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。

### 构建结果

- 前端 RED 验证：新增 API 与设置页 TTS 配置测试后，旧实现因缺少 TTS API 封装和表单字段失败。
- 前端 GREEN 验证：`npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/views/SettingsView.test.js` 通过。
- 前端构建验证：`npm.cmd run build` 通过。

### 当前功能验收说明

用户可在设置页为自定义 AI 配置补充 OpenAI 兼容 TTS 参数，并在保存前单独测试 `/audio/speech` 连通性。后端保存 TTS Key 时加密入库，接口只返回脱敏值，TTS 连通测试受关键端点限流保护。

### 停止，不继续下一个功能

本轮只完成用户自定义 AI TTS 配置保存、回显和连通测试，不切换模拟面试云端 TTS 播放链路，不实现音频流推送、不新增 TTS 调用计费、不新增数据库迁移、不扩展更多 TTS Provider 协议。

## 十九、管理端 AI 引擎配置与自定义 AI 用量分区修复记录（2026-06-03）

### 当前任务所属模块

管理端 AI 引擎配置页、用户自定义 AI 用量统计卡片、用户明细分页展示、同页分区切换。

### 前端文件定位

- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/__tests__/views/AdminAiEngineView.test.js`
- 前端完整记录见 `frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

### 后端文件定位

本轮只修复前端布局与分页可见性，不涉及后端 Controller、Service、Mapper、DTO 或数据库脚本。

### 本轮修改文件清单

- 管理端 AI 引擎配置页新增“引擎配置 / 自定义 AI 用量”同页分区切换。
- 默认展示“引擎配置”，隐藏每日上限、用量统计、趋势图和用户明细，避免统计区挤压配置筛选栏和配置表。
- “自定义 AI 用量”分区集中展示每日上限、当日统计、趋势图、功能分布和用户明细。
- 用户明细分页 footer 改为存在明细时显示总数和翻页控件，单页数据也能看出这是分页列表。
- 前端测试补充默认分区、切换后统计可见、用户明细按 `page/pageSize` 翻页请求的断言。

### 前端实现方案

- 不新增独立统计页面，不新增子路由；在现有 AI 引擎配置页内用分区切换实现布局隔离。
- 用户明细继续调用既有分页接口，初始化 `page=1/pageSize=5`，翻页时重新请求目标页，不全量接收所有用户。
- 趋势筛选仍独立于单日统计日期和分页，不改变既有接口规则。
- 页面仍只展示聚合调用量、用户基础标识和功能调用次数，不展示 API Key、baseUrl、model 或 Provider 私密配置。

### 后端实现方案

无后端改动。继续复用既有：

- `GET /api/admin/custom-ai/usage-stats`
- `GET /api/admin/custom-ai/usage-trends`

### 数据存储方案

无数据存储改动。不新增表、不新增字段、不新增迁移脚本。

### stage 更新说明

`frontend/tasks/stage.md` 顶部记录本轮分区切换和分页可见性修复；`tasks/stage.md` 顶部记录后端无变更的联动说明。

### 编译结果

- RED 验证：旧实现下 `npm.cmd test -- --run src/__tests__/views/AdminAiEngineView.test.js` 失败，复现缺少分区切换且统计区默认显示。
- GREEN 验证：`npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，2 个测试文件 / 14 个用例。

### 构建结果

- `npm.cmd run build` 通过。

### 当前功能验收说明

管理员默认看到 AI 引擎配置主流程；切换到“自定义 AI 用量”后才查看每日上限、用量统计、趋势和用户明细。用户明细继续走分页接口，翻页携带 `page/pageSize`，不会全量加载所有用户。

### 停止，不继续下一个功能

本轮只修复管理端 AI 引擎配置页与自定义 AI 用量统计的布局干扰和分页可见性，不在管理端首页新增趋势表，不实现周/月聚合、TTS UI、TTS 调用、更多 Provider 协议、自动平台回退或新的统计页面。

## 十八、管理端自定义 AI 趋势图布局修复记录（2026-06-03）

### 当前任务所属模块

管理端 AI 引擎配置页、用户自定义 AI 用量统计卡片、按日趋势图首屏布局修复。

### 前端文件定位

- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/__tests__/views/AdminAiEngineView.test.js`
- 前端完整记录见 `frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

### 后端文件定位

本轮只修复前端布局，不涉及后端 Controller、Service、Mapper、DTO 或数据库脚本。

### 本轮修改文件清单

- 将“用户自定义 AI 按日趋势”由默认完整图表改为默认折叠摘要行。
- 摘要行展示日期范围、区间总调用和活跃用户数，点击“展开趋势”后再展示筛选控件和折线图。
- 展开后的折线图高度收紧，窄屏下摘要和按钮纵向排布，避免挤压 AI 引擎配置主区域。
- 前端测试补充默认折叠、展开后图表渲染和展开后空态展示回归断言。

### 前端实现方案

- 保留 AI 引擎配置页作为统计入口，不新增管理端首页图表、不新增独立统计页面。
- 保留页面初始化近 7 天趋势请求和既有趋势查询接口；仅改变首屏展示形态。
- 趋势筛选继续独立于单日统计表格日期与分页，不影响现有统计表格交互。
- 页面仍只展示聚合调用量和活跃用户数，不展示 API Key、baseUrl、model 或 Provider 私密配置。

### 后端实现方案

无后端改动。继续复用既有 `GET /api/admin/custom-ai/usage-trends`。

### 数据存储方案

无数据存储改动。不新增表、不新增字段、不新增迁移脚本。

### stage 更新说明

`frontend/tasks/stage.md` 顶部记录本轮前端布局修复；`tasks/stage.md` 顶部记录后端无变更的联动说明。

### 编译结果

- RED 验证：旧实现下 `npm.cmd test -- --run src/__tests__/views/AdminAiEngineView.test.js` 失败，复现趋势图默认展开且缺少折叠开关。
- GREEN 验证：`npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，2 个测试文件 / 12 个用例。

### 构建结果

- `npm.cmd run build` 通过。

### 当前功能验收说明

管理员进入 AI 引擎配置页时，自定义 AI 趋势区默认只占用紧凑摘要行，不再把 AI 引擎配置筛选栏和表格顶到下方；需要查看折线图时可手动展开。

### 停止，不继续下一个功能

本轮只修复趋势图首屏布局挤压问题，不在管理端首页新增趋势表，不实现周/月聚合，不实现 TTS UI、TTS 调用、更多 Provider 协议、自动平台回退或新的统计页面。

## 十七、管理端自定义 AI 按日趋势图记录（2026-06-03）

### 当前任务所属模块

管理端 AI 引擎配置页、用户自定义 AI 独立用量统计、基于 `user_ai_usage_detail` 明细表的按日趋势聚合。

### 前端文件定位

- `frontend/app/src/api/admin/aiEngines.js`
- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/__tests__/api/admin.aiEngines.test.js`
- `frontend/app/src/__tests__/views/AdminAiEngineView.test.js`

### 后端文件定位

- `server/src/main/java/com/airesume/server/controller/AdminCustomAiStatsController.java`
- `server/src/main/java/com/airesume/server/service/UserAiUsageStatsService.java`
- `server/src/main/java/com/airesume/server/service/impl/UserAiUsageStatsServiceImpl.java`
- `server/src/main/java/com/airesume/server/mapper/UserAiUsageDetailMapper.java`
- `server/src/main/java/com/airesume/server/dto/admin/CustomAiUsageTrendResponse.java`
- `server/src/main/java/com/airesume/server/dto/admin/CustomAiUsageTrendDayResponse.java`
- `server/src/main/java/com/airesume/server/dto/admin/CustomAiUsageTrendTypeStatRow.java`
- `server/src/main/java/com/airesume/server/dto/admin/CustomAiUsageTrendActiveUserRow.java`
- `server/src/test/java/com/airesume/server/service/impl/UserAiUsageStatsServiceImplTest.java`
- `server/src/test/java/com/airesume/server/controller/AdminCustomAiStatsControllerTest.java`

### 本轮修改文件清单

- 新增管理端接口 `GET /api/admin/custom-ai/usage-trends`，查询参数为 `startDate`、`endDate`。
- `UserAiUsageStatsService` 新增趋势查询方法，Controller 继续复用管理员权限校验。
- `UserAiUsageDetailMapper` 新增两组固定 SQL：按 `usage_date + usage_type` 汇总调用次数，按 `usage_date` 统计当日活跃用户数。
- 新增趋势响应 DTO，返回 `startDate`、`endDate`、`totalCalls`、`activeUserCount`、`days`，单日 `typeStats` 继续复用 `usageType / usageTypeDesc / callCount` 结构。
- 管理端 AI 引擎配置页的“用户自定义 AI 用量统计”卡片中新增按日趋势区域，展示“总调用”和“活跃用户”两条折线。
- 后端与前端测试补齐默认近 7 天、单日查询、非法日期范围、90 天上限、缺失日期补 0、未知类型归一、趋势筛选和空态展示。

### 前端实现方案

- 继续复用 `AdminAiEngineView.vue`，将趋势图放在现有“用户自定义 AI 用量统计”卡片内，不新增管理端首页图表、不新增独立统计页面。
- `getCustomAiUsageTrends({ startDate, endDate })` 统一封装趋势接口，请求路径为 `/api/admin/custom-ai/usage-trends`。
- 页面默认加载近 7 天趋势，提供近 7 天、近 30 天、自定义日期范围三个筛选入口；趋势筛选与下方单日统计日期、分页互不影响。
- 图表继续使用项目已有 `chart.js` / `vue-chartjs`，不新增依赖；有数据时渲染折线图，无调用数据时展示“暂无趋势数据”空态。
- 页面只展示聚合调用量和活跃用户数，不展示 API Key、baseUrl、model、providerName 等 Provider 私密配置。

### 后端实现方案

- Service 统一归一化日期范围：两个日期都不传时默认近 7 天且包含今天；只传一个日期时按单日查询；`startDate > endDate` 抛业务错误；查询范围超过 90 天抛业务错误。
- 趋势数据完全基于既有 `user_ai_usage_detail` 明细表聚合，缺失日期由 Service 补 0，保证前端折线图日期连续。
- `usageType` 继续使用后端白名单归一，未知或历史类型合并为 `unknown`，并通过 `UserAiConstants.USAGE_TYPE_LABELS` 填充中文 label。
- Mapper 使用参数绑定的固定 SQL，复用现有 `(usage_date, usage_type)` 查询索引，不拼接动态 SQL，不返回敏感 Provider 配置。

### 数据存储方案

- 本轮不新增数据库表、不新增字段、不新增迁移脚本。
- 趋势聚合只读取上一轮已落地的 `user_ai_usage_detail` 明细表；`user_ai_daily_usage` 仍仅作为每日上限主计数来源。
- 不改变自定义 AI 扣费、失败回滚、平台手动 fallback、Provider 配置解析或加密存储规则。

### stage 更新说明

`tasks/stage.md` 顶部记录本轮后端趋势接口、日期规则、验证结果和停止说明；前端对应记录见 `frontend/tasks/stage.md`。

### 编译结果

- `mvn.cmd -q "-Dtest=UserAiUsageStatsServiceImplTest,AdminCustomAiStatsControllerTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。

### 构建结果

- `npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过。
- `npm.cmd run build` 通过。

### 当前功能验收说明

管理员可在 AI 引擎配置页查看用户自定义 AI 近 7 天、近 30 天或自定义日期范围的按日趋势。趋势图展示总调用量和活跃用户数，并保留现有单日统计表格与日期筛选。接口不返回 API Key、baseUrl、model 等敏感配置。

### 停止，不继续下一个功能

本轮只完成用户自定义 AI 按日趋势图，不在管理端首页新增图表，不实现周/月聚合，不实现 TTS UI、TTS 调用、更多 Provider 协议、自动平台回退或新的统计页面。

## 十六、用户自定义 AI 使用统计增强记录（2026-06-03）

### 当前任务所属模块

用户自定义 AI 独立用量统计、管理端 AI 引擎配置页统计展示、按功能分类的自定义 AI 调用明细。

### 前端文件定位

- `frontend/app/src/api/admin/aiEngines.js`
- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/__tests__/api/admin.aiEngines.test.js`
- `frontend/app/src/__tests__/views/AdminAiEngineView.test.js`

### 后端文件定位

- `server/src/main/java/com/airesume/server/common/constants/UserAiConstants.java`
- `server/src/main/java/com/airesume/server/entity/UserAiUsageDetail.java`
- `server/src/main/java/com/airesume/server/mapper/UserAiUsageDetailMapper.java`
- `server/src/main/java/com/airesume/server/service/UserAiUsageLimitService.java`
- `server/src/main/java/com/airesume/server/service/impl/UserAiUsageLimitServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/UserAiUsageStatsService.java`
- `server/src/main/java/com/airesume/server/service/impl/UserAiUsageStatsServiceImpl.java`
- `server/src/main/java/com/airesume/server/controller/AdminCustomAiStatsController.java`
- `db/migrations/TASK_68_CUSTOM_AI_USAGE_STATS.sql`
- `server/db/migrations/TASK_68_CUSTOM_AI_USAGE_STATS.sql`
- `db/schema.sql`
- `server/db/schema.sql`

### 本轮修改文件清单

- 新增 `user_ai_usage_detail` 明细表，按 `user_id + usage_date + usage_type` 记录自定义 AI 每日功能调用次数。
- 扩展 `UserAiUsageLimitService`，保留旧的 `checkAndIncrement(userId)` / `rollback(userId)` 签名，并新增带 `usageType` 的重载。
- 将简历诊断、简历润色、JD 匹配、面试消息、面试报告、面试摘要、Offer 辅助等自定义 AI 调用点改为写入明确功能类型。
- 新增管理端 `GET /api/admin/custom-ai/usage-stats`，返回指定日期的总调用量、配置用户数、活跃用户数、功能分布和用户明细分页。
- 管理端 AI 引擎配置页新增自定义 AI 使用统计卡片，展示汇总指标、功能分布、用户明细和日期筛选。

### 前端实现方案

- 复用现有 `AdminAiEngineView.vue`，将统计能力放在每日上限配置附近，不新增独立统计页面和图表页。
- `getCustomAiUsageStats({ date, page, pageSize })` 统一封装管理端统计接口；页面保留空态、刷新、日期切换和分页能力。
- 用户明细仅展示用户 ID、邮箱、昵称、总调用量和按功能分布的次数，不展示 API Key、baseUrl 等敏感配置。

### 后端实现方案

- `user_ai_daily_usage` 继续作为每日总限额来源，保证原有超限判断不变；`user_ai_usage_detail` 只用于统计明细，不改变扣费主链路。
- 总次数递增成功后同步递增明细表；调用失败回滚总次数时同步回滚对应功能明细，避免失败请求进入统计。
- `usageType` 只接受后端白名单常量，未知或旧签名调用统一归入 `unknown`，保证兼容旧测试和 mock 链路。
- 统计查询使用固定 Mapper SQL 和参数绑定，按日期聚合功能分布与用户明细，管理端 Controller 继续要求管理员权限。
- 管理端统计分页收敛 `page <= 10000`、`pageSize <= 100`，避免极端请求参数造成 offset 溢出或过大分页压力。

### 数据存储方案

- 新增 `user_ai_usage_detail`，字段包括 `user_id`、`usage_date`、`usage_type`、`call_count` 和标准时间/删除字段。
- 唯一索引 `uk_user_ai_usage_detail_user_date_type` 保证同一用户同一天同一功能只有一行，递增使用原子 `UPDATE call_count = call_count + 1`。
- 同步更新 `db/` 与 `server/db/` 两套 migration/schema，迁移脚本包含 `SET NAMES utf8mb4`。

### stage 更新说明

`tasks/stage.md` 顶部记录本轮后端统计增强范围、验证结果和停止说明；前端对应记录见 `frontend/tasks/stage.md`。

### 编译结果

- `mvn.cmd -q "-Dtest=UserAiUsageLimitServiceImplTest,UserAiUsageStatsServiceImplTest,AdminCustomAiStatsControllerTest,SchemaConsistencyTest,ResumeDiagnosisControllerTest,InterviewControllerTest,InterviewServiceTest,InterviewContextCompressorTest,OfferAssistServiceImplTest,ResumeDiagnosisProcessorTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。

### 构建结果

- `npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过。
- `npm.cmd run build` 通过。

### 当前功能验收说明

管理员可在 AI 引擎配置页按日期查看自定义 AI 当日总调用量、配置用户数、活跃用户数、按功能分类次数和用户调用明细。自定义 AI 每日限额仍以 `user_ai_daily_usage` 为准，本轮只补齐统计明细，不改变平台额度、自定义 AI 回退或 Provider 配置规则。

### 停止，不继续下一个功能

本轮只补齐用户自定义 AI 使用统计增强，不实现 TTS、更多 Provider 协议、按日/周/月图表、新独立统计页面或自动平台回退。

## 十五、未提交改动 Review 修复记录（2026-06-01）

### 当前任务所属模块

后端用户自定义 AI 计费链路、面试流式错误回传、Offer 辅助 AI 调用、简历诊断图片页多模态解析。

### 本轮修改文件清单

- `server/src/main/java/com/airesume/server/mapper/UserAiConfigMapper.java`
- `server/src/main/java/com/airesume/server/service/impl/UserAiConfigServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/main/java/com/airesume/server/service/impl/OfferAssistServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/ResumeAiService.java`
- `server/src/main/java/com/airesume/server/service/ResumeVisionExtractor.java`
- `server/src/main/java/com/airesume/server/service/ResumeContentExtractor.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisProcessor.java`
- 对应后端单元测试：`UserAiConfigServiceImplTest`、`OfferAssistServiceImplTest`、`InterviewServiceTest`、`ResumeDiagnosisProcessorTest`。

### 后端实现方案

- 用户删除自定义 AI 配置时改为物理删除当前有效记录，敏感 API Key 不再以逻辑删除记录形式长期保留，也避免历史删除记录占用唯一键导致重复删除/重建失败。
- 面试流式上游异步失败时会执行自定义 AI 用量回滚回调，并把 4090/4091 等自定义 AI 错误码写入 SSE 错误 payload，保证前端能展示手动平台回退入口。
- Offer 辅助调用复用 `interview/default` 用户自定义 AI 配置，命中自定义 AI 时纳入独立每日次数，调用失败后回滚。
- 简历诊断 PDF 图片页解析新增用户上下文透传：`ResumeContentExtractor` → `ResumeVisionExtractor` → `ResumeAiService`，多模态识别与任务创建时锁定的 `ai_billing_source/fallback_to_platform` 保持一致，避免自定义 AI 任务在提取阶段误走平台配置。

### 验证结果

- `mvn.cmd -q "-Dtest=UserAiConfigServiceImplTest,OfferAssistServiceImplTest,InterviewServiceTest,ResumeDiagnosisProcessorTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。
- `mvn.cmd test` 通过，641 个用例，0 失败，0 错误。
- 前端 review 修复与构建结果见 `frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

### 当前功能验收说明

本轮只修复 review 中发现的自定义 AI 删除、异步流式回滚、Offer 辅助计费、简历图片页用户上下文透传和测试签名同步问题，不扩展新的 AI Provider 协议、TTS 或统计能力。

### 停止，不继续下一个功能

本轮 review 修复已完成并验证，停止在当前用户自定义 AI Provider 范围内，不继续推进下一阶段能力。

## 十四、本轮实现记录（2026-06-01）

### 当前任务所属模块

后端 AI 配置解析、用户自定义 AI 配置管理、用户自定义 AI 独立用量、简历诊断/润色/JD 匹配、模拟面试 AI 调用、管理端系统配置。

### 本轮修改文件清单

- 后端新增 `user_ai_config`、`user_ai_daily_usage`、`sys_config` 实体、Mapper、Service、Resolver、DTO 与 `UserAiConfigController`。
- 后端修改 `ResumeAiServiceImpl`、`InterviewAiServiceImpl`、`AiChatClient`、简历诊断异步任务、简历 JD/润色、面试消息链路、Offer 辅助链路，透传 `userId` 与 `fallbackToPlatform`。
- 后端修改 `AdminController`，新增用户自定义 AI 每日上限查询/更新接口。
- 后端修改 `CriticalEndpointRateLimitFilter`，新增用户自定义 AI 连通测试每用户每分钟 5 次限流。
- 数据库同步更新 `db/migrations/TASK_68_USER_CUSTOM_AI_PROVIDER.sql`、`server/db/migrations/TASK_68_USER_CUSTOM_AI_PROVIDER.sql`、两份 `schema.sql`。
- 前端实现见 `frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

### 后端实现方案

- `UserAiConfigResolver` 按“精确业务类型 → default → null”解析用户配置；`fallbackToPlatform=true` 时忽略用户配置。
- `ResumeAiService` 下诊断、润色、JD 匹配、图片识别统一使用 `resume` 配置；`InterviewAiService` 与 `AiChatClient` 使用 `interview` 配置。
- 命中用户自定义 AI 时运行时构造 OpenAI 兼容 `/chat/completions` 配置，不走平台 AI 配置链，不扣平台额度。
- 自定义 AI 调用前扣 `user_ai_daily_usage`，调用失败回滚；平台 AI 仍使用原 `UserQuotaService`。
- 简历诊断任务新增计费来源与回退意图锁定字段，避免创建任务与异步执行期间配置变化导致扣费来源漂移。
- API Key 复用 `AiCredentialCrypto` 加密，响应只返回脱敏值；`baseUrl` 保存和连通测试前走 `PublicHttpsUrlValidator.validate()`。

### 数据存储方案

- `user_ai_config` 保存用户 default/resume/interview 三类 OpenAI 兼容配置，TTS 字段仅预留。
- `user_ai_daily_usage` 按用户和日期记录自定义 AI 当日调用次数。
- `sys_config.custom_ai_daily_limit` 作为唯一每日上限来源，默认 `50`。
- `resume_diagnosis_task` 新增 `ai_billing_source` 与 `fallback_to_platform`。

### 验证结果

- `mvn.cmd -q "-Dtest=UserAiConfigResolverImplTest,UserAiUsageLimitServiceImplTest,CriticalEndpointRateLimitFilterTest,SchemaConsistencyTest,ResumeDiagnosisControllerTest,AdminControllerTest,InterviewControllerTest,AdminAiEngineConnectivityControllerTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。
- 前端测试与构建结果见 `frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

### 当前功能验收说明

- 未配置自定义 AI 的用户沿用平台 AI 与原额度逻辑。
- 配置并启用用户自定义 AI 后，对应业务优先使用用户配置并扣独立每日次数。
- 自定义 AI 失败不会自动回退；前端仅在 4090/4091 等自定义 AI 错误处展示“使用平台 AI”按钮，点击后请求携带 `fallbackToPlatform=true` 并消耗平台额度。

### 停止，不继续下一个功能

本轮只完成用户自定义 OpenAI 兼容 Provider、独立每日用量、手动平台回退和管理端每日上限，不实现 TTS 调用/UI、统计图表、更多协议适配或额外页面。

## 一、功能概述

允许用户自行配置 API Key 接入任意 OpenAI 兼容协议的 AI 提供商，替代平台默认的全局 AI 配置。用户使用自定义 AI 时不消耗平台额度，但受每日调用次数限制（保护服务器资源）。

### 核心价值

- 用户无需依赖平台采购的 AI 模型，可自由选择最适合自己的模型
- 降低平台 AI 成本压力，用户自行承担 token 费用
- 支持用户为不同场景选择不同模型（如简历诊断选支持识图的模型，面试选对话流畅的模型）

---

## 二、决策记录

| 维度 | 决定 | 原因 |
|------|------|------|
| 配置粒度 | 三级：通用 + 简历诊断覆盖 + 面试覆盖 | 简历诊断需要多模态能力，面试需要对话流畅性，不同场景对模型要求不同 |
| 协议范围 | 仅 OpenAI 兼容协议（`/chat/completions`） | 覆盖 90%+ 主流提供商（DeepSeek、Qwen、Doubao、MiniMax、SiliconFlow、OpenRouter 等），实现成本最低 |
| 次数限制 | 每日固定上限，管理员后台可动态调整 | 保护服务器资源，同时运营可灵活调整 |
| TTS | 本期不实现，预留数据库字段和接口 | 各家 TTS API 协议差异大，无统一标准，后续版本单独实现 |
| 安全方案 | 复用现有 `AiCredentialCrypto`（AES/GCM）+ 保存前连通测试 | 已有成熟加密基础设施，连通测试防止无效配置 |
| 使用门槛 | 所有注册用户 | 降低使用门槛，鼓励自带 key |
| 失败处理 | 报错 + 前端提供「使用平台 AI（扣额度）」手动回退按钮 | 不自动回退，防止用户无感知消耗平台额度 |
| 前端入口 | 用户设置页新增 AI 配置区域 | 复用现有设置页框架，不增加导航复杂度 |

---

## 三、现有架构分析

### 当前 AI 配置解析链路

```
resolveRuntimeConfig():
  1. DB 全局配置 (sys_ai_engine_config WHERE is_active=1 AND business_type=?)
  2. YAML 配置 (@Value app.ai.* / app.interview.*)
  3. 环境变量 (DEEPSEEK_API_KEY, API_KEY 等)
```

### 改造后的解析优先级

```
resolveRuntimeConfig(userId, businessType):
  1. ★ 用户自定义配置 (user_ai_config WHERE user_id=? AND is_enabled=1)  ← 新增最高优先级
  2. DB 全局配置 (sys_ai_engine_config)
  3. YAML 配置
  4. 环境变量
```

### 涉及的现有 AI 调用点

| 调用点 | 文件 | businessType |
|--------|------|-------------|
| 简历诊断 | `ResumeAiServiceImpl.diagnose()` | resume |
| 简历润色 | `ResumeAiServiceImpl.polishResume()` | resume |
| JD 匹配分析 | `ResumeAiServiceImpl.diagnoseJobMatch()` | resume |
| 图片文字提取 | `ResumeAiServiceImpl.extractTextFromImage()` | resume |
| 面试回复（流式） | `InterviewAiServiceImpl.generateReplyStream()` | interview |
| 面试开场白 | `InterviewAiServiceImpl.generateOpening()` | interview |
| 面试评估报告 | `InterviewAiServiceImpl.generateEvaluationReport()` | interview |
| 上下文压缩/摘要 | `AiChatClient.chat()` | interview |

### 额度系统现状

- `UserQuotaService` 按功能分别管理每日额度：`dailyResumeUsed`、`dailyInterviewUsed`、`dailyPolishUsed`、`dailyJdMatchUsed` 等
- 使用自定义 AI 时需跳过这些额度检查，改走独立的每日次数限制

---

## 四、数据库设计

### 4.1 新表：`user_ai_config`

用户自定义 AI 提供商配置，每个用户最多 3 条记录（default / resume / interview）。

```sql
CREATE TABLE user_ai_config (
    id                  BIGINT          NOT NULL    COMMENT '雪花ID',
    user_id             BIGINT          NOT NULL    COMMENT '用户ID',
    config_type         VARCHAR(32)     NOT NULL    DEFAULT 'default'
        COMMENT '配置类型: default=通用（润色/JD匹配/Offer等）, resume=简历诊断覆盖, interview=面试覆盖',
    provider_name       VARCHAR(64)                 COMMENT '用户自定义名称，如"我的DeepSeek"、"SiliconFlow"',
    base_url            VARCHAR(512)    NOT NULL    COMMENT 'API 基础地址，如 https://api.deepseek.com',
    api_key             VARCHAR(1024)   NOT NULL    COMMENT 'API Key（AES/GCM 加密存储，使用 AiCredentialCrypto）',
    model               VARCHAR(128)    NOT NULL    COMMENT '模型标识符，如 deepseek-chat、gpt-4o',
    is_enabled          TINYINT         NOT NULL    DEFAULT 1   COMMENT '是否启用: 1=启用, 0=禁用',
    supports_multimodal TINYINT         NOT NULL    DEFAULT 0   COMMENT '是否支持多模态（图片识别），仅 resume 类型需关注',
    last_verified_at    DATETIME                    COMMENT '最后一次连通性测试通过时间',
    verification_status VARCHAR(32)     NOT NULL    DEFAULT 'pending'
        COMMENT '连通状态: pending=待验证, verified=已验证, failed=验证失败',
    -- TTS 预留字段（本期不实现，不暴露到前端）
    tts_base_url        VARCHAR(512)                COMMENT '预留: TTS 服务地址',
    tts_api_key         VARCHAR(1024)               COMMENT '预留: TTS API Key（加密存储）',
    tts_model           VARCHAR(128)                COMMENT '预留: TTS 模型标识',
    tts_voice_id        VARCHAR(128)                COMMENT '预留: TTS 音色 ID',
    -- 标准字段
    create_time         DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted          TINYINT         NOT NULL    DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_config_type (user_id, config_type, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户自定义AI配置';
```

**设计说明**：
- `uk_user_config_type` 保证每用户每类型只有一条有效记录
- `supports_multimodal` 仅对 `config_type='resume'` 有实际意义，决定是否尝试视觉提取图片型 PDF
- TTS 字段预留但前端不展示，后续 TTS 功能只需加逻辑不需改表

### 4.2 新表：`user_ai_daily_usage`

用户自定义 AI 每日使用量追踪。

```sql
CREATE TABLE user_ai_daily_usage (
    id              BIGINT      NOT NULL    COMMENT '雪花ID',
    user_id         BIGINT      NOT NULL    COMMENT '用户ID',
    usage_date      DATE        NOT NULL    COMMENT '使用日期',
    call_count      INT         NOT NULL    DEFAULT 0   COMMENT '当日已调用次数（所有功能合计）',
    create_time     DATETIME    NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME    NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT     NOT NULL    DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_date (user_id, usage_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户自定义AI每日使用量';
```

**设计说明**：
- 所有功能共享一个每日计数器（简历诊断、面试、润色等合计）
- `UPDATE call_count = call_count + 1 WHERE user_id=? AND usage_date=?` 保证并发安全
- 不存在记录时 INSERT（当天首次调用），存在时 UPDATE

### 4.3 系统配置扩展

管理员可调的每日上限值。两种方案任选其一：

**方案 A：复用现有配置机制（如 sys_ai_engine_config 体系中增加一条）**

```sql
-- 在现有系统配置中添加
INSERT INTO sys_ai_engine_config (id, business_type, provider, model, is_active, create_time, update_time, is_deleted)
VALUES (/* snowflake */, 'custom_ai_limit', 'system', '50', 0, NOW(), NOW(), 0);
-- 借用 model 字段存值，is_active=0 不影响 AI 引擎解析
```

**方案 B（推荐）：新建轻量 key-value 配置表**

```sql
CREATE TABLE sys_config (
    id              BIGINT          NOT NULL    COMMENT '雪花ID',
    config_key      VARCHAR(128)    NOT NULL    COMMENT '配置键',
    config_value    VARCHAR(512)    NOT NULL    COMMENT '配置值',
    description     VARCHAR(256)                COMMENT '配置说明',
    create_time     DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT         NOT NULL    DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';

INSERT INTO sys_config (id, config_key, config_value, description)
VALUES (1, 'custom_ai_daily_limit', '50', '用户自定义API Key每日调用上限');
```

> `sys_config` 表后续也可用于其他运营配置，具有通用性。

---

## 五、后端设计

### 5.1 新增类清单

```
server/src/main/java/com/airesume/server/
├── entity/
│   ├── UserAiConfig.java                    # 用户AI配置实体
│   ├── UserAiDailyUsage.java                # 每日用量实体
│   └── SysConfig.java                       # 系统配置实体（如选方案B）
├── mapper/
│   ├── UserAiConfigMapper.java
│   ├── UserAiDailyUsageMapper.java
│   └── SysConfigMapper.java
├── dto/
│   └── user/
│       ├── UserAiConfigRequest.java         # 创建/更新配置请求
│       ├── UserAiConfigResponse.java        # 配置查询响应（apiKey脱敏）
│       ├── AiConnectivityTestRequest.java   # 连通测试请求
│       ├── AiConnectivityTestResponse.java  # 连通测试结果
│       └── UserAiUsageResponse.java         # 用量查询响应
├── service/
│   ├── UserAiConfigService.java             # 用户配置管理接口
│   ├── UserAiUsageLimitService.java         # 每日限额管理接口
│   ├── UserAiConfigResolver.java            # ★ 核心：配置解析器（根据 userId+businessType 返回配置）
│   ├── SysConfigService.java                # 系统配置接口
│   └── impl/
│       ├── UserAiConfigServiceImpl.java
│       ├── UserAiUsageLimitServiceImpl.java
│       ├── UserAiConfigResolverImpl.java
│       └── SysConfigServiceImpl.java
├── controller/
│   ├── UserAiConfigController.java          # 用户端 API
│   └── (AdminController 中新增端点)         # 管理员调整每日上限
```

### 5.2 核心类设计

#### `UserAiConfigResolver`（配置解析器）

```java
public interface UserAiConfigResolver {

    /**
     * 根据用户ID和业务类型解析应使用的AI配置
     *
     * 解析优先级（以 businessType="resume" 为例）：
     *   1. user_ai_config WHERE config_type='resume' AND is_enabled=1
     *   2. user_ai_config WHERE config_type='default' AND is_enabled=1
     *   3. 返回 null → 调用方 fallback 到全局 resolveRuntimeConfig()
     *
     * @param userId       当前用户ID
     * @param businessType "resume" / "interview" / 其他走 default
     * @return 解析结果，包含配置信息和来源标记；null 表示用户无自定义配置
     */
    ResolvedAiConfig resolve(Long userId, String businessType);
}
```

**`ResolvedAiConfig` 内部类/DTO**：

```java
@Data
@Builder
public class ResolvedAiConfig {
    private String provider;        // 提供商标识
    private String baseUrl;         // API 基础地址
    private String apiKey;          // 已解密的 API Key
    private String model;           // 模型标识
    private boolean supportsMultimodal; // 是否支持多模态
    private String source;          // "USER_CUSTOM" 或 "PLATFORM"
    private String configType;      // 命中的配置类型: default/resume/interview
}
```

#### 解析逻辑伪代码

```
resolve(userId, businessType):
    if userId == null → return null

    // 第一优先级：精确匹配业务类型
    if businessType in ("resume", "interview"):
        config = findEnabledConfig(userId, businessType)
        if config != null → return buildResult(config, source=USER_CUSTOM)

    // 第二优先级：通用配置兜底
    defaultConfig = findEnabledConfig(userId, "default")
    if defaultConfig != null → return buildResult(defaultConfig, source=USER_CUSTOM)

    // 用户无自定义配置
    return null
```

### 5.3 现有类修改

#### `ResumeAiServiceImpl` 修改

```
修改 resolveRuntimeConfig()：
  - 新增 userId 参数
  - 方法开头调用 userAiConfigResolver.resolve(userId, "resume")
  - 命中用户配置时直接使用，跳过 DB 全局配置和 YAML 查找
  - 未命中时走原有逻辑

修改 diagnose() / polishResume() / diagnoseJobMatch()：
  - 新增 userId 参数
  - 传递 userId 到 resolveRuntimeConfig()

修改 extractTextFromImage()：
  - 新增 userId 参数
  - 当用户配置 supportsMultimodal=false 时跳过视觉提取

修改 supportsVisionExtraction()：
  - 需要感知是否使用用户自定义配置及其多模态能力
```

#### `InterviewAiServiceImpl` 修改

```
修改 resolveRuntimeConfig()：
  - 新增 userId 参数
  - 同上逻辑，businessType = "interview"

修改 generateReplyStream() / generateOpening() / generateEvaluationReport()：
  - 新增 userId 参数或从现有参数链路获取
```

#### `AiChatClient` 修改

```
修改 chat()：
  - 新增 userId 参数
  - 调用 userAiConfigResolver.resolve(userId, "interview") 获取配置
  - 未命中走原有逻辑
```

#### 调用链路 userId 传递

| 入口 | userId 来源 | 传递路径 |
|------|------------|---------|
| 简历诊断 | `ResumeDiagnosisProcessor.processTask()` 中 `task.getUserId()` | → `resumeAiService.diagnose(resumeText, userId)` |
| 简历润色 | Controller 中 `authentication.getPrincipal()` | → Service → `resumeAiService.polishResume(..., userId)` |
| JD 匹配 | Controller 中 `authentication.getPrincipal()` | → Service → `resumeAiService.diagnoseJobMatch(..., userId)` |
| 面试消息 | Controller 中 `authentication.getPrincipal()` | → `InterviewService` → `interviewAiService.generateReplyStream(..., userId)` |
| 上下文压缩 | `InterviewService` 已有 userId | → `AiChatClient.chat(..., userId)` |

#### 额度逻辑修改

各功能的 Controller / Service 调用点需区分处理：

```
// 伪代码：以简历诊断为例
resolvedConfig = userAiConfigResolver.resolve(userId, "resume")

if resolvedConfig != null && resolvedConfig.source == "USER_CUSTOM":
    // 用户自定义配置 → 检查每日次数限制，不检查平台额度
    userAiUsageLimitService.checkAndIncrement(userId)
    调用 AI（使用 resolvedConfig）
    if 调用失败:
        userAiUsageLimitService.rollback(userId)  // 失败不计入次数
        throw new BusinessException(CUSTOM_AI_CALL_FAILED, 错误详情)
else:
    // 平台配置 → 原有额度逻辑不变
    userQuotaService.checkResumeQuota(userId)
    userQuotaService.deductResumeQuota(userId)
    调用 AI（使用全局配置）
    if 调用失败:
        userQuotaService.refundResumeQuota(userId)
```

#### 手动回退机制

```
前端请求时可附带参数: fallbackToPlatform=true
后端处理逻辑:
  if fallbackToPlatform == true:
      忽略用户自定义配置，强制走全局配置 + 平台额度扣减
  else:
      正常解析优先级
```

### 5.4 API 端点设计

#### 用户端

```
GET    /api/user/ai-config
       → 返回用户所有配置列表（最多3条），apiKey 脱敏显示

POST   /api/user/ai-config
       Body: { configType, providerName, baseUrl, apiKey, model, supportsMultimodal }
       → 创建或更新指定 configType 的配置（UPSERT 语义）
       → 保存前自动执行连通测试

DELETE /api/user/ai-config/{configType}
       → 删除指定类型的配置（逻辑删除）

PUT    /api/user/ai-config/{configType}/toggle
       Body: { enabled: true/false }
       → 启用或禁用指定类型的配置

POST   /api/user/ai-config/test-connectivity
       Body: { baseUrl, apiKey, model, supportsMultimodal }
       → 独立连通测试（不保存），返回测试结果

GET    /api/user/ai-config/usage
       → 返回今日使用量和每日上限: { used: 12, limit: 50 }
```

#### 管理员端

```
GET    /api/admin/custom-ai/daily-limit
       → 查询当前全局每日上限

PUT    /api/admin/custom-ai/daily-limit
       Body: { limit: 100 }
       → 修改全局每日上限

GET    /api/admin/custom-ai/usage-stats
       Query: { date, page, pageSize }
       → 自定义 AI 使用统计，返回指定日期的总调用量、配置用户数、活跃用户数、功能分布和用户明细分页
```

### 5.5 连通测试实现

```java
/**
 * 连通测试：向用户配置的 AI 提供商发送最小请求，验证配置有效性
 *
 * 请求: POST {baseUrl}/chat/completions
 *   body: { model, messages: [{role:"user", content:"hi"}], max_tokens: 5 }
 *   header: Authorization: Bearer {apiKey}
 * 超时: 10 秒
 *
 * 如果 supportsMultimodal=true，额外发送一个带图片的请求验证视觉能力
 */
```

返回结果包含：
- `success`: boolean
- `message`: 成功时返回模型实际响应的 model 名；失败时返回人类可读的错误描述
- `errorType`: 失败时的分类（`INVALID_KEY` / `MODEL_NOT_FOUND` / `TIMEOUT` / `NETWORK_ERROR` / `MULTIMODAL_NOT_SUPPORTED`）

### 5.6 错误码定义

```java
// ResultCode 枚举新增
CUSTOM_AI_CALL_FAILED(4090, "自定义AI调用失败"),
CUSTOM_AI_DAILY_LIMIT_EXCEEDED(4091, "今日自定义AI调用次数已达上限"),
CUSTOM_AI_CONFIG_INVALID(4092, "自定义AI配置无效"),
CUSTOM_AI_CONNECTIVITY_FAILED(4093, "AI服务连通测试失败"),
```

---

## 六、前端设计

### 6.1 用户设置页 AI 配置区域

在现有设置页面（已有数据保留设置）内新增「AI 模型配置」区域。

#### 布局结构

```
┌──────────────────────────────────────────────────┐
│  AI 模型配置                          [? 帮助说明] │
├──────────────────────────────────────────────────┤
│                                                  │
│  ○ 使用平台AI（默认）                              │
│  ● 使用自定义API Key                               │
│                                                  │
│  ┌─ 通用配置（润色、JD匹配、Offer分析等）──────────┐ │
│  │  名称:     [我的DeepSeek                     ] │ │
│  │  API地址:  [https://api.deepseek.com         ] │ │
│  │  API Key:  [sk-****1234           ] [👁 显示]  │ │
│  │  模型:     [deepseek-chat                    ] │ │
│  │                                               │ │
│  │  [测试连接]  ✅ 连接正常 (2024-01-15 14:30)    │ │
│  └───────────────────────────────────────────────┘ │
│                                                  │
│  □ 为简历诊断使用不同模型                           │
│  ┌─ 简历诊断专用（勾选后展开）──────────────────────┐│
│  │  名称:     [SiliconFlow视觉模型              ] ││
│  │  API地址:  [https://api.siliconflow.cn/v1    ] ││
│  │  API Key:  [sk-****5678           ] [👁 显示]  ││
│  │  模型:     [deepseek-ai/deepseek-vl2         ] ││
│  │  ☑ 该模型支持图片识别（多模态）                  ││
│  │  ℹ 勾选后将使用该模型识别图片型PDF简历            ││
│  │                                               ││
│  │  [测试连接]                                    ││
│  └───────────────────────────────────────────────┘│
│                                                  │
│  □ 为模拟面试使用不同模型                           │
│  ┌─ 面试专用（勾选后展开）──────────────────────────┐│
│  │  名称:     [                                 ] ││
│  │  API地址:  [                                 ] ││
│  │  API Key:  [                                 ] ││
│  │  模型:     [                                 ] ││
│  │                                               ││
│  │  [测试连接]                                    ││
│  └───────────────────────────────────────────────┘│
│                                                  │
│  今日已使用: 12 / 50 次                            │
│                                                  │
│                         [保存配置]                 │
└──────────────────────────────────────────────────┘
```

#### 交互说明

1. **默认/自定义切换**：Radio 选择，选择「使用平台AI」时隐藏所有配置表单
2. **通用配置**：始终展示（当选择「自定义」时），必填
3. **简历诊断覆盖**：Checkbox 控制展开/折叠，勾选后展示独立配置表单
4. **面试覆盖**：同上
5. **测试连接**：按钮点击后 Loading 状态 → 成功/失败提示
6. **保存配置**：统一提交，保存前自动对所有填写的配置执行连通测试
7. **API Key 显示**：默认掩码显示（`sk-****xxxx`），点击眼睛图标临时显示完整值
8. **多模态勾选**：仅在简历诊断配置中显示，附带说明文字

### 6.2 失败回退 UI

当 AI 调用返回 `CUSTOM_AI_CALL_FAILED`（错误码 4090）时，在对应功能页面显示：

```
┌─────────────────────────────────────────┐
│  ⚠ 自定义AI调用失败                      │
│                                         │
│  错误原因: API Key 余额不足 (402)        │
│                                         │
│  [使用平台AI（消耗1次额度）]   [检查AI配置] │
└─────────────────────────────────────────┘
```

- 「使用平台AI」按钮：重新发起请求，附带 `fallbackToPlatform=true` 参数
- 「检查AI配置」按钮：跳转到用户设置页的 AI 配置区域

不同功能页面的展示位置：
- **简历诊断**：诊断进度区域显示错误卡片
- **简历润色**：润色结果区域显示错误卡片
- **JD匹配**：分析结果区域显示错误卡片
- **模拟面试**：聊天区域以系统消息形式显示，附带操作按钮

### 6.3 帮助说明弹窗

点击「? 帮助说明」时弹出说明，包含：
- 什么是 OpenAI 兼容协议
- 常用提供商地址速查表（DeepSeek、SiliconFlow、Doubao、Qwen、MiniMax 等的 baseUrl）
- 如何获取 API Key
- 什么是多模态模型
- 每日次数限制说明

### 6.4 新增前端文件

```
frontend/app/src/
├── api/
│   └── userAiConfig.js              # API 模块
├── components/
│   └── settings/
│       ├── AiProviderConfig.vue     # AI 配置主组件
│       ├── AiProviderForm.vue       # 单个提供商配置表单（复用于通用/简历/面试）
│       └── AiProviderHelpDialog.vue # 帮助说明弹窗
```

---

## 七、管理员后台

### 7.1 新增功能

在管理员后台「系统配置」或「AI 引擎管理」区域新增：

```
┌──────────────────────────────────────┐
│  用户自定义AI管理                      │
├──────────────────────────────────────┤
│                                      │
│  每日调用上限: [50    ] 次   [保存]    │
│                                      │
│  当前使用统计:                         │
│  - 配置了自定义AI的用户数: 23          │
│  - 今日自定义AI总调用次数: 156         │
│                                      │
└──────────────────────────────────────┘
```

---

## 八、安全设计

### 8.1 API Key 安全

- **加密存储**：复用 `AiCredentialCrypto`（AES-256-GCM），数据库存储密文 `enc:v1:...`
- **前端脱敏**：API 返回时只展示后 4 位，如 `sk-****1a2b`
- **日志屏蔽**：后端日志中禁止打印完整 API Key，仅打印脱敏值
- **传输安全**：API Key 仅通过 HTTPS POST body 传输，不出现在 URL 或 query string

### 8.2 接口安全

- 所有 `/api/user/ai-config/*` 接口需要登录态（JWT 认证）
- 用户只能操作自己的配置（Service 层强制绑定 userId）
- 连通测试接口做频率限制（同一用户每分钟最多 5 次），防止被用作代理探测工具
- 管理员端点需要 admin 角色校验

### 8.3 防滥用

- 每日调用次数限制（`user_ai_daily_usage` 表追踪）
- 连通测试频率限制
- baseUrl 校验：必须是 HTTPS（生产环境），禁止 localhost / 内网地址（防 SSRF）

---

## 九、实现阶段划分

### Phase 1：数据层基础设施

**范围**：数据库 + Entity + Mapper + 基础 Service

- [x] 编写迁移脚本：`TASK_68_USER_CUSTOM_AI_PROVIDER.sql`
- [x] 新增 `UserAiConfig` 实体
- [x] 新增 `UserAiDailyUsage` 实体
- [x] 新增 `SysConfig` 实体（如选方案B）
- [x] 新增对应 Mapper
- [x] 新增 `UserAiConfigService` + Impl（CRUD 基础操作）
- [x] 新增 `UserAiUsageLimitService` + Impl（计数 + 限额检查）
- [x] 新增 `SysConfigService` + Impl（系统配置读写）
- [x] 更新 `schema.sql`

**验证**：`mvn clean compile` 通过

### Phase 2：核心配置解析

**范围**：`UserAiConfigResolver` + 改造现有 AI 服务

- [x] 新增 `UserAiConfigResolver` + Impl
- [x] 新增 `ResolvedAiConfig` DTO
- [x] 修改 `ResumeAiServiceImpl.resolveRuntimeConfig()` 接入用户配置
- [x] 修改 `InterviewAiServiceImpl.resolveRuntimeConfig()` 接入用户配置
- [x] 修改 `AiChatClient` 接入用户配置
- [x] 各 AI 方法签名增加 userId 参数
- [x] 修改所有调用链路传递 userId（见 5.3 节表格）
- [x] 简历诊断中处理 `supportsMultimodal` 逻辑

**验证**：现有测试全部通过（`mvn test`），无用户配置时行为不变

### Phase 3：限额与安全

**范围**：额度逻辑改造 + 连通测试 + 回退机制

- [x] 各功能调用点区分 USER_CUSTOM vs PLATFORM 额度逻辑
- [x] 新增连通测试逻辑（复用 `AiEngineConnectivityTestService` 思路）
- [x] 新增错误码 `CUSTOM_AI_CALL_FAILED` 等
- [x] 实现 `fallbackToPlatform` 回退参数支持
- [x] baseUrl SSRF 校验
- [x] 连通测试频率限制

**验证**：`mvn test` 通过 + 手动测试自定义 AI 调用 + 回退场景

### Phase 4：用户端 API

**范围**：Controller + DTO

- [x] 新增 `UserAiConfigController`（CRUD + 连通测试 + 用量查询）
- [x] 新增请求/响应 DTO
- [x] API Key 脱敏逻辑
- [x] 管理员端点（每日上限 CRUD）
- [x] 编写 API 文档

**验证**：`mvn test` 通过 + Postman/curl 手动测试所有端点

### Phase 5：前端实现

**范围**：用户设置页 AI 配置 + 失败回退 UI

- [x] 新增 `api/userAiConfig.js`
- [x] 设置页集成 AI 配置区域（按本轮最小增量，未拆独立组件）
- [x] 简历诊断请求支持 `fallbackToPlatform`
- [x] 简历润色页错误回退 UI
- [x] JD匹配页错误回退 UI
- [x] 模拟面试页错误回退 UI
- [x] 管理员后台每日上限配置入口

> 说明：本轮按用户最终开发计划采用“复用设置页样式与组件体系”的最小实现，没有额外新增 `AiProviderConfig.vue`、`AiProviderForm.vue`、`AiProviderHelpDialog.vue`，避免新增无关组件层。

**验证**：`npm run build` 通过 + 浏览器手动测试全流程

---

## 十、测试要点

### 单元测试

- `UserAiConfigResolver` 三级解析逻辑（精确匹配 → default 兜底 → null）
- `UserAiUsageLimitService` 限额检查、递增、回滚
- `AiCredentialCrypto` 加密/解密正确性
- API Key 脱敏逻辑

### 集成测试

- 用户有自定义配置时 → AI 调用使用用户配置
- 用户无自定义配置时 → AI 调用使用全局配置（行为不变）
- 自定义 AI 调用失败 → 返回 4090 错误码
- `fallbackToPlatform=true` → 使用全局配置 + 扣平台额度
- 每日次数超限 → 返回 4091 错误码
- resume 配置 supportsMultimodal=false → 跳过视觉提取

### 安全测试

- API Key 数据库中为密文
- API 响应中 Key 已脱敏
- 非本人配置无法读取/修改
- baseUrl SSRF 防护生效
- 连通测试频率限制生效

---

## 十一、迁移脚本

文件路径：`db/migrations/TASK_68_USER_CUSTOM_AI_PROVIDER.sql`

该脚本在 Phase 1 实现时编写，包含：
- `user_ai_config` 建表
- `user_ai_daily_usage` 建表
- `sys_config` 建表（方案B）+ 初始数据
- 同步更新 `db/schema.sql`

---

## 十二、风险与注意事项

1. **userId 穿透改造面大**：这是最大的工程量。所有 AI 服务方法签名需要增加 userId，所有调用链路需要传递。建议 Phase 2 先改一个完整链路（如简历诊断）验证可行性，再批量改其他链路。

2. **并发安全**：`user_ai_daily_usage.call_count` 递增使用 `UPDATE SET call_count = call_count + 1` 原子操作。首次调用使用 `INSERT ... ON DUPLICATE KEY UPDATE` 或先查后插（加唯一索引兜底）。

3. **Mock 模式兼容**：`MockResumeAiServiceImpl` 和 `MockInterviewAiServiceImpl` 不做用户配置解析，始终走 mock 逻辑。确保 mock 模式不受影响。

4. **缓存一致性**：现有 `resolveRuntimeConfig()` 可能有缓存（`sys_ai_engine_config` 用了 Spring Cache）。用户配置变更时不影响全局缓存，但需考虑是否缓存用户配置（建议初期不缓存，用户配置变更不频繁）。

5. **向后兼容**：所有现有 API 行为不变。未配置自定义 AI 的用户完全不受影响。AI 服务方法增加 userId 参数时，可考虑保留无参重载方法过渡。

6. **前端兼容**：需要在 Axios 响应拦截器中识别 4090/4091 错误码，触发回退 UI 展示。不影响其他错误处理逻辑。

---

## 十三、后续扩展预留

### TTS 集成（下一版本）

数据库已预留 `tts_base_url`、`tts_api_key`、`tts_model`、`tts_voice_id` 字段。后续实现 TTS 时：
- 后端新增 TTS 调用服务（需适配多家 TTS 协议）
- 面试回复流程改为：LLM 生成文字 → TTS 转语音 → 音频流推送前端
- 前端设置页展开 TTS 配置区域
- 不需要修改数据库表结构

### 使用统计增强

- 已完成：按功能分类统计（简历诊断 x 次、面试 y 次、润色 z 次等）
- 已完成：管理员按日期查看用户自定义 AI 使用明细
- 待后续：按日/周/月统计图表
# 二十、管理端自定义 AI 用量趋势默认展开前端联动记录（2026-06-03）
## 当前任务所属模块
管理端 AI 引擎配置页、同页“自定义 AI 用量”分区、用户自定义 AI 按日趋势图、用户明细分页展示。

## 前端文件定位

- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/__tests__/views/AdminAiEngineView.test.js`
- 前端完整记录见 `frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

## 后端文件定位

本轮无后端代码改动，不涉及 Controller、Service、Mapper、DTO、SQL 或数据库迁移。

## 本轮修改文件清单

- `AdminAiEngineView.vue` 将“自定义 AI 用量”分区内的趋势图默认展开，切换进入该分区即可直接看到折线图或空态。
- `AdminAiEngineView.vue` 保持用户明细分页每页 5 条，继续通过 `page/pageSize` 拉取分页数据，不全量接收全部用户。
- `AdminAiEngineView.test.js` 补充趋势默认展开、点击收起、空态默认展示和 `pageSize=5` 分页回归断言。

## 前端实现方案

- 自定义 AI 用量统计已在上一轮隔离到独立分区，因此趋势图默认展开不会影响 AI 引擎配置主视图布局。
- 保留趋势图折叠按钮，满足需要压缩用量分区高度时的手动收起。
- 不新增管理端首页图表、不新增子路由或独立统计页面，不改变趋势筛选与单日用户明细筛选互不干扰的规则。

## 后端实现方案

无后端改动。继续复用既有：

- `GET /api/admin/custom-ai/usage-stats`
- `GET /api/admin/custom-ai/usage-trends`

## 数据存储方案

无数据存储改动。不新增表、字段、索引或迁移脚本。

## stage 更新说明

`frontend/tasks/stage.md` 与 `tasks/stage.md` 已记录本轮趋势默认展开、用户明细分页保持 5 条和验证结果。

## 编译结果

- RED 验证：旧实现下 `npm.cmd test -- --run src/__tests__/views/AdminAiEngineView.test.js` 失败，复现切换后趋势图仍默认折叠。
- GREEN 验证：`npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，2 个测试文件 / 14 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

管理员默认仍进入“引擎配置”分区；切换到“自定义 AI 用量”后，趋势图默认展开。用户明细分页默认每页 5 条，翻页请求继续携带 `pageSize=5`。

## 停止，不继续下一个功能
本轮只调整趋势默认展开与确认用户明细分页，不在管理端首页新增趋势表，不实现周/月聚合、TTS UI、TTS 调用、更多 Provider 协议或新的统计页面。
## 二十五、模型列表获取错误提示规整记录（2026-06-04）

### 当前任务所属模块

用户自定义 AI Provider 配置、管理端 AI 引擎配置、OpenAI 兼容 `/models` 模型发现错误处理。

### 前端文件定位

- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- 本轮未修改前端代码；前端继续展示后端 `errorMessage`，并保留手动输入模型名兜底。

### 后端文件定位

- `server/src/main/java/com/airesume/server/service/impl/AiModelDiscoveryServiceImpl.java`
- `server/src/test/java/com/airesume/server/service/impl/AiModelDiscoveryServiceImplTest.java`

### 本轮修改文件清单

- `AiModelDiscoveryServiceImpl`：上游 `/models` 返回 HTTP 异常时不再拼接响应体，避免把 HTML 网关错误页、openresty 等原始内容展示给用户。
- `AiModelDiscoveryServiceImpl`：按 HTTP 状态码生成可理解提示；404 明确提示检查 API 基础地址是否需要 `/v1`、服务商是否支持 `/models`，并说明仍可手动输入模型名。
- `AiModelDiscoveryServiceImplTest`：新增 404 HTML 响应回归用例，锁定 `errorMessage` 不包含 `<html>` 和 `openresty`。

### 实现方案

后端继续保持 OpenAI 兼容 `/models` 单一路径，不扩展 Provider 专用协议。`RestClientResponseException` 只读取状态码并映射为中文提示，不读取上游响应体参与用户错误信息或日志摘要；解析格式异常仍保留本地可理解的结构提示，方便判断供应商响应是否不兼容。

### 数据存储方案

无数据库变更。不新增表、字段、索引或迁移脚本。

### stage 更新说明

`tasks/stage.md` 已记录本轮错误提示规整、验证结果和停止边界；前端 stage 已记录前端无代码改动但完成展示链路回归验证。

### 编译结果

- RED 验证：`mvn.cmd -q "-Dtest=AiModelDiscoveryServiceImplTest#shouldHideRawHtmlBodyWhenProviderModelEndpointNotFound" test` 在旧实现下失败，复现 404 HTML 被透传的问题。
- GREEN 验证：同一 RED 用例修复后通过。
- 后端目标回归：`mvn.cmd -q "-Dtest=AiModelDiscoveryServiceImplTest,AdminAiEngineConnectivityControllerTest,UserAiConfigServiceImplTest,CriticalEndpointRateLimitFilterTest" test` 通过。
- 后端编译：`mvn.cmd -q -DskipTests compile` 通过。

### 构建结果

- 前端目标回归：`npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，4 个测试文件 / 61 个用例。
- 前端构建：`npm.cmd run build` 通过。

### 当前功能验收说明

当服务商 `/models` 返回 404 HTML 页面时，用户不再看到原始 HTML。界面会展示类似“上游返回 HTTP 404：模型列表接口不存在，请检查 API 基础地址是否应以 /v1 结尾，或该服务商是否支持 /models；你仍可手动输入模型名。”的可行动提示，并保留手动输入模型名和继续保存的能力。

### 停止，不继续下一个功能

本轮只修复模型列表获取失败时的错误提示可读性和上游响应体暴露问题，不扩展 Provider 模型协议、自动探测基础地址、模型能力检测、价格信息、自动路由或自动保存。
