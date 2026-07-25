# 用户自定义 AI Provider 前端接入

## 自定义 TTS 播报延迟前端快修（2026-06-04）
## 当前任务所属模块

模拟面试语音播报、用户自定义云端 TTS 播放队列、语音通话状态机。

## 前端文件定位

- `frontend/app/src/composables/useCloudTextToSpeech.js`
- `frontend/app/src/composables/useVoiceCall.js`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/__tests__/composables/useCloudTextToSpeech.test.js`
- `frontend/app/src/__tests__/composables/useVoiceCall.test.js`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`

## 后端文件定位

无后端改动。本轮不修改 `/api/interview/session/{sessionId}/tts`，仍复用后端完整 `audio/mpeg` 返回。

## 本轮修改文件清单

- `useCloudTextToSpeech.js` 新增 `isPreparing/isActive`，将云端合成等待期与真实音频播放态拆开。
- `useCloudTextToSpeech.js` 重构队列为“单路合成 + 准备好后播放”，当前句播放期间会提前合成下一句，减少句间空白。
- `useVoiceCall.js` 改为优先读取 `textToSpeech.isActive` 判断 AI 音频占用，云端语音准备中也会暂停 STT 和模式切换。
- `InterviewSessionView.vue` 聚合云端准备态，合成等待时显示“AI 语音准备中”，真实 `Audio.play()` 后显示“AI 正在播报”；波形和状态点只在真实播报时进入 speaking 样式。
- 测试补齐云端 TTS 准备态、下一句预合成、准备中暂停收音和页面状态展示回归。

## 前端实现方案

- 不改后端音频接口，不引入音频流式协议；首句仍需等待上游 TTS 完整合成，但等待期明确展示为“AI 语音准备中”。
- 云端 TTS 只保留一个在途合成请求，避免并发压垮用户自定义 Provider；已准备好的音频通过 Blob URL 缓存到队列，播放或停止后释放。
- 云端合成失败仍沿用既有降级策略：本场关闭云端 TTS，只提示一次并切回浏览器 TTS 继续播报。

## 数据存储方案

无数据存储改动。不新增本地持久化字段、接口字段、数据库表或迁移脚本。

## stage 更新说明

`frontend/tasks/stage.md` 已记录本轮自定义 TTS 播报延迟快修、验证结果和停止范围。

## 编译结果

- RED 验证：旧实现下 `npm.cmd test -- --run src/__tests__/composables/useCloudTextToSpeech.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 失败，复现缺少 `isPreparing/isActive`、第二句未提前合成、页面等待期仍显示“AI 正在回复”。
- GREEN 验证：同一命令通过，3 个测试文件 / 82 个用例。
- 扩展语音回归：`npm.cmd test -- --run src/__tests__/api/interview.test.js src/__tests__/composables/useCloudTextToSpeech.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，5 个测试文件 / 125 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

用户接入自定义云端 TTS 后，面试官语音合成等待期间不再误显示为正在播报；真正开始播放音频后才进入播报态。连续句子播报时，上一句播放期间会提前合成下一句，降低句间等待。

## 停止，不继续下一个功能

本轮只处理自定义 TTS 播报延迟体验，不实现后端流式音频、系统级 TTS、TTS 计费统计、音频存储、STT 或新的 Provider 协议。

## 代码审查问题修复（2026-06-04）
## 当前任务所属模块

用户设置中心自定义 AI 接入、TTS 配置区测试断言、后端 TTS 连通测试联动。

## 前端文件定位

- `frontend/app/src/__tests__/views/SettingsView.test.js`

## 后端文件定位

- `server/src/main/java/com/airesume/server/service/impl/UserAiConfigServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/UserTtsConnectivityTestServiceImpl.java`
- 后端完整记录见 `tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。

## 本轮修改文件清单

- `SettingsView.test.js` 将自定义 AI 选择框高度断言从旧的 TTS 发现行专属深度选择器，调整为当前 `.cai-form` 通用选择框规则。
- `SettingsView.test.js` 增加 `.cai-tts-discover-btn` 和统一高度变量断言，锁定当前发现按钮与表单控件高度一致。

## 前端实现方案

- 本轮不修改 `SettingsView.vue` 页面结构、样式或交互，只修正测试断言与当前实现对齐。
- 继续验证 `.el-input__wrapper`、`.el-select__wrapper` 和底部非 scoped 穿透规则，避免 Element Plus 选择框高度回归。

## 后端实现方案

后端同步修复 TTS 自定义端点透传和 Chat Completions TTS 音频数据校验，详见根目录 `tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。

## 数据存储方案

无前端本地存储改动，无数据库结构改动。

## stage 更新说明

`frontend/tasks/stage.md` 已记录本轮代码审查问题修复、验证结果和停止范围；根目录 `tasks/stage.md` 同步记录后端修复。

## 编译结果

- 前端设置页测试：`npm.cmd test -- src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 43 个用例。
- 前端回归验证：`npm.cmd test -- src/__tests__/api/interview.test.js src/__tests__/api/userAiConfig.test.js src/__tests__/api/admin.aiEngines.test.js src/__tests__/composables/useCloudTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，7 个测试文件 / 117 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

设置页测试已与当前自定义 AI 表单样式结构一致，不再因为旧选择器导致回归测试失败；页面实际展示逻辑本轮未变更。

## 停止，不继续下一个功能

本轮仅修复代码审查指出的测试断言问题和对应后端联动缺陷，不继续扩展 TTS UI、Provider 模板、计费统计或新的设置页能力。

## TTS 配置选择框高度统一修复（2026-06-03）
## 当前任务所属模块

用户设置中心自定义 AI 接入、TTS 配置区、Element Plus 输入框与选择框视觉高度统一。

## 前端文件定位

- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/__tests__/views/SettingsView.test.js`

## 后端文件定位

无后端改动。本轮只调整前端表单样式和对应回归测试。

## 本轮修改文件清单

- `SettingsView.vue` 将自定义 AI 表单控件高度抽为 `--cai-form-control-height` 和 `--cai-form-control-inner-height`，输入框、选择框和 TTS 发现行按钮共用同一高度来源。
- `SettingsView.vue` 同步覆盖 Element Plus 当前选择框实际使用的 `.el-select__wrapper`，并保留旧 `.el-input__wrapper` 兼容规则。
- `SettingsView.vue` 底部非 scoped 穿透规则同步覆盖 `.el-select__wrapper` 和 `.el-select__input`，避免选择框内层仍按默认较小高度渲染。
- `SettingsView.test.js` 新增源码级回归断言，锁定自定义 AI 表单和 TTS 发现行必须覆盖 `.el-select__wrapper`。

## 前端实现方案

- 不改 TTS 表单结构、不改字段、不改保存或连通测试逻辑，只修正样式选择器覆盖面。
- 继续限制在 `.cai-form` 范围内，避免影响设置页其它 Element Plus 表单或管理端页面。
- 选择框兼容 Element Plus 新旧 DOM wrapper 命名，防止依赖升级或按需样式注入后再次出现输入框与选择框高度不一致。

## 后端实现方案

无后端改动。

## 数据存储方案

无数据存储改动。不新增本地持久化字段、接口字段或数据库结构。

## stage 更新说明

`frontend/tasks/stage.md` 顶部已记录本轮 TTS 配置区选择框高度统一修复、验证结果和停止范围。

## 编译结果

- RED 验证：旧实现下 `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 失败，新增用例复现 `.el-select__wrapper` 未被高度规则覆盖。
- GREEN 验证：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 43 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

设置页自定义 AI 的 TTS 配置区展开后，TTS 服务商、TTS 模型、音色 ID 这些选择框与同区域输入框使用同一高度规则；字段从输入框切换为选择框时不再出现视觉高度变小。

## 停止，不继续下一个功能

本轮只修复 TTS 配置区输入框与选择框高度一致性，不继续扩展模型发现、Provider 模板、TTS 播放链路、计费统计或新的设置页能力。

## 自定义 AI 模型列表获取（2026-06-03）
## 当前任务所属模块
用户设置中心自定义 AI 接入、管理端 AI 引擎配置弹窗、OpenAI 兼容 `/models` 模型发现候选。

## 前端文件定位

- `frontend/app/src/api/userAiConfig.js`
- `frontend/app/src/api/admin/aiEngines.js`
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/__tests__/api/userAiConfig.test.js`
- `frontend/app/src/__tests__/api/admin.aiEngines.test.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`
- `frontend/app/src/__tests__/views/AdminAiEngineView.test.js`

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/UserAiConfigController.java`
- `server/src/main/java/com/airesume/server/controller/AdminController.java`
- `server/src/main/java/com/airesume/server/service/AiModelDiscoveryService.java`
- 后端完整记录见 `tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。

## 本轮修改文件清单

- `userAiConfig.js` 新增 `fetchUserAiModels(data)`，请求 `POST /api/user/ai-config/models`，只提交 `baseUrl/apiKey`。
- `admin/aiEngines.js` 新增 `fetchAdminAiModels(data)`，请求 `POST /api/admin/ai-engines/models`。
- `SettingsView.vue` 将自定义 AI “模型”字段改为可搜索、可手动输入的 `el-select allow-create`，增加“获取模型”按钮。
- `AdminAiEngineView.vue` 将管理端“模型名”字段改为可搜索、可手动输入的 `el-select allow-create`，增加“获取模型”按钮；编辑态允许不填 Key，由后端复用已保存密钥。
- 页面状态会在 API 地址或 Key 变化后清空旧候选，避免跨 Provider 误选。
- 页面测试覆盖模型候选填充、空模型默认选中第一项、拉取失败保留手动输入模型名。

## 前端实现方案

- 模型候选只作为辅助输入，不强制用户从列表选择；保存和连通测试继续使用最终表单模型名。
- 用户侧按钮必须在 `baseUrl` 和真实 `apiKey` 都填写后可用；管理端新增态也必须填写 Key，编辑态可只填基础地址。
- 拉取成功后写入当前表单候选列表；当前模型名为空时自动选第一项。
- 拉取失败只展示错误提示，不清空现有模型名，不阻止保存。

## 后端实现方案

前端调用后端代理接口获取模型列表；后端负责公网 HTTPS 校验、API Key 使用、上游错误规整、管理端编辑态密钥复用和限流。

## 数据存储方案

无前端本地持久化。模型候选仅保存在当前页面内存状态中；本轮不新增数据库字段、接口保存字段或 Provider 模板。

## stage 更新说明

`frontend/tasks/stage.md` 已记录本轮前端交互、验证结果和停止范围；根目录 `tasks/stage.md` 记录后端模型发现接口与安全边界。

## 编译结果

- 前端目标测试：`npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，4 个测试文件 / 60 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

用户和管理员都可以在填写 API 基础地址与 Key 后手动获取模型候选；候选列表可搜索、可手动新增。失败时保留原有手动输入模型名，保存 payload 仍使用最终选中的或手动输入的模型名。

## 停止，不继续下一个功能

本轮只实现 OpenAI 兼容模型列表获取和前端候选输入，不实现 Provider 专用模型协议、模型能力检测、价格信息、自动路由或更多 Provider 模板。

## 自定义 TTS 在语音面试中真实播放（2026-06-03）

## 当前任务所属模块

模拟面试语音播报、用户自定义 TTS 播放层、OpenAI 兼容 `/audio/speech` 音频播放。

## 前端文件定位

- `frontend/app/src/api/interview.js`
- `frontend/app/src/composables/useCloudTextToSpeech.js`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/__tests__/api/interview.test.js`
- `frontend/app/src/__tests__/composables/useCloudTextToSpeech.test.js`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/main/java/com/airesume/server/service/UserTtsSpeechService.java`
- `server/src/main/java/com/airesume/server/service/impl/UserTtsSpeechServiceImpl.java`
- 后端完整记录见 `tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。

## 本轮修改文件清单

- `interview.js` 新增 `getInterviewTtsCapability(sessionId)`，请求 `GET /api/interview/session/{sessionId}/tts-capability`。
- `interview.js` 新增 `synthesizeInterviewTts(sessionId, text, options)`，使用 `fetch` 接收后端 `audio/mpeg` Blob。
- 新增 `useCloudTextToSpeech.js`，负责云端 TTS 句子队列、Blob URL 创建/释放、播放停止、失败降级通知。
- `InterviewSessionView.vue` 加载语音会话后查询 TTS capability；可用时 AI 开场白和流式回复优先使用云端 TTS。
- `InterviewSessionView.vue` 保留浏览器 `speechSynthesis` 作为播放层兜底；云端单句失败后本场只提示一次，并切回浏览器播报。
- `useVoiceCall` 入参改为组合后的 TTS facade，`isSpeaking` 同时覆盖云端音频和浏览器 TTS，避免播报期间误恢复收音。
- 测试补齐 API 封装、云端 TTS 队列释放、失败降级、面试页云端可用/不可用/失败回退场景。

## 前端实现方案

- 云端 TTS 只服务语音面试的 AI 面试官播报，不用于简历诊断、简历润色、JD 匹配或普通文本面试。
- 页面不新增 UI 面板或独立页面，只在现有语音面试状态文案中显示“播报音色：云端 TTS”。
- 云端播放使用后端 `/tts` 接口返回的 `audio/mpeg` Blob，通过 `Audio` 队列顺序播放；停止播报、挂断、模式切换或组件卸载时释放 Blob URL。
- 云端失败是播放层降级，不等同于平台 AI fallback，不会携带 `fallbackToPlatform`，也不影响面试消息流式请求。

## 后端实现方案

前端调用后端新增接口：

- `GET /api/interview/session/{sessionId}/tts-capability`
- `POST /api/interview/session/{sessionId}/tts`

后端按 `interview -> default` 解析用户自定义 TTS 配置，并返回音频；不可用或失败时前端降级浏览器 TTS。

## 数据存储方案

前端不新增本地持久化；本轮不新增数据库表、字段、迁移脚本或音频缓存。

## stage 更新说明

`frontend/tasks/stage.md` 与 `tasks/stage.md` 已记录本轮语音面试云端 TTS 播放接入、验证结果和停止边界。

## 编译结果

- RED 验证：旧实现下新增 API、`useCloudTextToSpeech` 和面试页云端 TTS 测试失败，复现缺少 capability 查询、云端播放队列和失败降级。
- GREEN 验证：`npm.cmd test -- --run src/__tests__/api/interview.test.js src/__tests__/composables/useCloudTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 55 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

用户配置并启用“面试对话”或“通用兜底”TTS 后，语音面试页会优先使用云端 TTS 播放 AI 面试官开场白和后续追问；没有配置或云端临时失败时，继续使用浏览器 TTS，面试不会中断。

## 停止，不继续下一个功能

本轮只完成语音面试 AI 面试官播报接入用户自定义 OpenAI 兼容 TTS，不继续实现 STT、简历诊断 TTS、TTS 计费统计、音频存储、更多 Provider 协议或独立语音设置页面。

## TTS 配置折叠与通用兜底支持（2026-06-03）

## 当前任务所属模块

用户设置中心、自定义 AI Provider 配置、TTS 配置展示边界、设置页布局修正。

## 前端文件定位

- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/api/userAiConfig.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`
- `frontend/app/src/__tests__/api/userAiConfig.test.js`

## 后端文件定位

- `server/src/main/java/com/airesume/server/service/impl/UserAiConfigServiceImpl.java`
- 后端完整记录见 `tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。

## 本轮修改文件清单

- `SettingsView.vue` 将 TTS 配置区限制为“通用兜底”和“面试对话”配置展示，“简历能力”配置不展示。
- `SettingsView.vue` 将 TTS 配置区改为默认折叠，只显示标题、说明、状态胶囊和展开按钮。
- `SettingsView.vue` 的“未启用/已填写”状态胶囊改为固定高度 flex 居中，修复文字不在胶囊正中的观感问题。
- `SettingsView.vue` 的“已配置 TTS”标记只在通用兜底和面试配置卡片上显示。
- `buildUserAiConfigPayload()` 只在 `configType=default/interview` 时携带 TTS 字段，保存“简历能力”配置时不提交 TTS 字段。
- `handleUserTtsConnectivityTest()` 增加配置类型守卫，避免简历配置触发 TTS 测试。
- `userAiConfig.js` 注释同步说明 TTS 字段只应随 `default/interview` 配置提交。
- `SettingsView.test.js` 补充 TTS 归属、折叠展开和胶囊居中样式断言。

## 前端实现方案

- TTS 当前只服务后续“云端面试官播报”，不属于简历诊断、简历润色或 JD 匹配能力。
- 通用兜底可作为面试 TTS 未单独配置时的兜底配置；面试对话可继续独立配置 TTS。
- “简历能力”表单保留模型、API Key、Vision 等相关字段，移除 TTS 大块配置后减少表单高度和视觉干扰。
- TTS 连通测试入口仅在通用兜底和面试配置中可见，并需要展开面板后操作。
- 不新增页面、不新增弹窗、不改变自定义 AI 配置卡片结构。

## 后端实现方案

后端已同步限制只有 `default/interview` 配置可保存和回显 TTS 字段；`resume` 保存时会清空 TTS 字段，防止绕过前端写入无关配置。

## 数据存储方案

无数据存储改动。不新增表、字段、索引或迁移脚本。

## stage 更新说明

`frontend/tasks/stage.md` 与 `tasks/stage.md` 已记录本轮 TTS 配置归属修正和验证结果。

## 编译结果

- RED 验证：旧实现下设置页测试失败，复现通用兜底缺少 TTS 区块、TTS 区块不可折叠且状态胶囊没有专门居中样式。
- GREEN 验证：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过。
- 前端回归验证：`npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/views/SettingsView.test.js` 通过，2 个测试文件 / 41 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

用户选择“简历能力”配置时不再看到 TTS 语音合成区；选择“通用兜底”或“面试对话”配置时可展开 TTS 面板填写和测试。保存简历配置不会提交 TTS 字段。

## 停止，不继续下一个功能

本轮只修正 TTS 配置归属、折叠交互和状态胶囊布局，不继续实现云端 TTS 播放、音频流推送、TTS 计费统计、更多 Provider 协议或新的语音设置页面。

## 用户自定义 AI TTS 配置与连通测试（2026-06-03）

## 当前任务所属模块

用户设置中心、自定义 AI Provider 配置、OpenAI 兼容 TTS 参数配置、TTS 连通测试。

## 前端文件定位

- `frontend/app/src/api/userAiConfig.js`
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/__tests__/api/userAiConfig.test.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/UserAiConfigController.java`
- `server/src/main/java/com/airesume/server/dto/user/UserAiConfigRequest.java`
- `server/src/main/java/com/airesume/server/dto/user/UserAiConfigResponse.java`
- `server/src/main/java/com/airesume/server/dto/user/UserTtsConnectivityTestRequest.java`
- `server/src/main/java/com/airesume/server/dto/user/UserTtsConnectivityTestResponse.java`
- `server/src/main/java/com/airesume/server/service/UserTtsConnectivityTestService.java`
- 后端完整记录见 `tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。

## 本轮修改文件清单

- `userAiConfig.js` 新增 `testUserTtsConnectivity(data)`，请求 `POST /api/user/ai-config/test-tts-connectivity`。
- `SettingsView.vue` 的自定义 AI 表单新增 TTS 配置区，字段为 TTS Base URL、TTS API Key、TTS 模型和音色 ID。
- `SettingsView.vue` 保存配置时携带 `ttsBaseUrl`、`ttsApiKey`、`ttsModel`、`ttsVoiceId`。
- `SettingsView.vue` 新增 TTS 连通测试交互，测试请求体映射为 `baseUrl`、`apiKey`、`model`、`voiceId`。
- 已保存配置槽在后端返回 `ttsConfigured=true` 时展示“已配置 TTS”状态。
- `userAiConfig.test.js` 与 `SettingsView.test.js` 补充 TTS API、字段渲染、保存 payload、连通测试和全填校验断言。

## 前端实现方案

- 继续复用现有设置页“自定义 AI 接入”面板，不新增独立页面、弹窗或路由。
- TTS 配置采用全填或全不填规则：只要填写任一 TTS 字段，保存和连通测试前必须补齐全部 TTS 字段。
- 后端返回的脱敏 TTS API Key 只用于展示，不作为明文提交值回填。
- TTS 连通测试只验证配置是否可用，不保存音频结果，不改变当前面试语音播报来源。
- 本轮不修改 `InterviewSessionView.vue` 的浏览器 TTS 播报链路，面试播报仍由前端 `speechSynthesis` 处理。

## 后端实现方案

前端调用后端新增接口：

- `POST /api/user/ai-config/test-tts-connectivity`

请求体：

- `baseUrl`
- `apiKey`
- `model`
- `voiceId`

后端按 OpenAI 兼容 `/audio/speech` 测试连通性，并继续负责公网 HTTPS 校验、API Key 加密脱敏和关键端点限流。

## 数据存储方案

前端不新增本地持久化。TTS 配置保存在后端既有 `user_ai_config` 预留字段中；本轮不新增数据库迁移。

## stage 更新说明

`frontend/tasks/stage.md` 顶部已记录本轮 TTS 配置和连通测试范围、验证结果和停止说明；根目录 stage 记录后端接口和安全边界。

## 编译结果

- RED 验证：旧实现下新增 TTS API 和设置页测试失败，复现缺少 `testUserTtsConnectivity`、TTS 表单字段和保存 payload。
- GREEN 验证：`npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/views/SettingsView.test.js` 通过。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

用户可在设置页为自定义 AI 配置补充 OpenAI 兼容 TTS 参数，并单独执行 TTS 连通测试。页面会展示当前配置是否已配置 TTS，但不会把模拟面试播报切换到云端 TTS。

## 停止，不继续下一个功能

本轮只完成用户自定义 AI TTS 配置 UI、API 封装和连通测试，不继续实现云端 TTS 播放、音频流推送、TTS 计费统计、更多 Provider 协议或新的语音设置页面。

## 管理端 AI 引擎配置与自定义 AI 用量分区修复（2026-06-03）

## 当前任务所属模块

管理端 AI 引擎配置页、用户自定义 AI 用量统计卡片、用户明细分页展示、同页分区切换。

## 前端文件定位

- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/__tests__/views/AdminAiEngineView.test.js`

## 后端文件定位

本轮只修复前端布局与分页可见性，不涉及后端接口、Service、Mapper 或 DTO。

## 本轮修改文件清单

- `AdminAiEngineView.vue` 新增同页分区切换：“引擎配置”和“自定义 AI 用量”。
- 默认进入“引擎配置”，只展示配置统计、筛选栏和 AI 引擎配置表格；每日上限、用量统计、趋势图和用户明细不再挤压默认配置布局。
- “自定义 AI 用量”分区集中展示每日上限、当日统计、趋势摘要/折线图、功能分布和用户明细。
- 用户明细分页 footer 改为只要存在明细就展示 `total/prev/pager/next`，避免单页数据时误以为没有分页能力。
- `AdminAiEngineView.test.js` 补充默认分区、切换后统计可见、用户明细按 `page/pageSize` 请求的回归断言。

## 前端实现方案

- 不新增独立页面，不新增子路由；使用当前页面内的分区切换满足“互不干扰布局”的目标。
- 统计数据仍按既有接口分页读取，前端继续以 `page=1/pageSize=5` 初始化，翻页时重新请求指定页，不做全量接收。
- 趋势筛选仍独立于单日用户明细筛选，切换分区不改变后端统计规则。
- 页面仍只展示聚合调用量、用户基础标识和功能调用次数，不展示 API Key、baseUrl、model 或 Provider 私密配置。

## 后端实现方案

无后端改动。继续调用既有：

- `GET /api/admin/custom-ai/usage-stats`
- `GET /api/admin/custom-ai/usage-trends`

## 数据存储方案

无数据存储改动。不新增表、不新增字段、不新增迁移脚本。

## stage 更新说明

`frontend/tasks/stage.md` 顶部已记录本轮分区切换和分页可见性修复；根目录 stage 记录后端无变更的前端联动说明。

## 编译结果

- RED 验证：旧实现下 `npm.cmd test -- --run src/__tests__/views/AdminAiEngineView.test.js` 失败，复现缺少分区切换且统计区默认显示。
- GREEN 验证：`npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，2 个测试文件 / 14 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

管理员默认看到 AI 引擎配置主流程；切到“自定义 AI 用量”后才查看每日上限、用量统计、趋势和用户明细。用户明细继续走分页接口，翻页会携带 `page/pageSize`，不会全量加载所有用户。

## 停止，不继续下一个功能

本轮只修复管理端 AI 引擎配置页与自定义 AI 用量统计的布局干扰和分页可见性，不新增管理端首页图表、独立统计页面、周/月聚合、TTS UI 或更多 Provider 协议。

## 管理端自定义 AI 趋势图默认折叠布局修复（2026-06-03）

## 当前任务所属模块

管理端 AI 引擎配置页、用户自定义 AI 用量统计卡片、按日趋势图首屏布局修复。

## 前端文件定位

- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/__tests__/views/AdminAiEngineView.test.js`

## 后端文件定位

本轮只修复前端布局，不涉及后端接口、Service、Mapper 或 DTO。

## 本轮修改文件清单

- `AdminAiEngineView.vue` 将“用户自定义 AI 按日趋势”改为默认折叠摘要行，首屏只展示日期范围、区间总调用和活跃用户数。
- `AdminAiEngineView.vue` 保留近 7 天默认趋势请求，点击“展开趋势”后才渲染近 7 天、近 30 天、自定义日期筛选和折线图。
- `AdminAiEngineView.vue` 将展开后的趋势图高度从原 260px 收紧为 220px，并补充窄屏纵向排布，避免挤压 AI 引擎配置主区域。
- `AdminAiEngineView.test.js` 补充默认折叠、展开后渲染图表、展开后空态展示的回归断言。

## 前端实现方案

- 趋势图仍保留在现有 AI 引擎配置页的“用户自定义 AI 用量统计”卡片内，不新增管理端首页图表、不新增独立统计页面。
- 默认状态只占用一行摘要，避免完整折线图把筛选栏和 AI 引擎配置表格顶出首屏；需要看明细趋势时再手动展开。
- 趋势筛选状态继续独立于单日统计表格的日期和分页状态，展开或切换趋势范围不影响下方单日明细。
- 折叠摘要只展示聚合后的调用量和活跃用户数，不展示 API Key、baseUrl、model 或 Provider 私密配置。

## 后端实现方案

无后端改动。继续调用既有 `GET /api/admin/custom-ai/usage-trends`。

## 数据存储方案

无数据存储改动。不新增表、不新增字段、不新增迁移脚本。

## stage 更新说明

`frontend/tasks/stage.md` 顶部已记录本轮趋势图默认折叠布局修复、验证结果和停止说明；根目录 stage 仅记录后端无变更的前端联动说明。

## 编译结果

- RED 验证：旧实现下 `npm.cmd test -- --run src/__tests__/views/AdminAiEngineView.test.js` 失败，复现趋势图默认展开且缺少折叠开关。
- GREEN 验证：`npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，2 个测试文件 / 12 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

管理员进入 AI 引擎配置页时，“用户自定义 AI 按日趋势”默认只显示紧凑摘要，不再把 AI 引擎配置筛选栏和表格挤到页面下方；点击“展开趋势”后仍可查看折线图、近 7 天、近 30 天和自定义日期范围。

## 停止，不继续下一个功能

本轮只修复管理端自定义 AI 趋势图默认展开造成的布局挤压，不继续实现管理端首页趋势图、周/月聚合、TTS UI、更多 Provider 协议或新的统计页面。

## 管理端自定义 AI 按日趋势图（2026-06-03）

## 当前任务所属模块

管理端 AI 引擎配置页、用户自定义 AI 用量统计卡片、按日趋势折线图展示。

## 前端文件定位

- `frontend/app/src/api/admin/aiEngines.js`
- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/__tests__/api/admin.aiEngines.test.js`
- `frontend/app/src/__tests__/views/AdminAiEngineView.test.js`

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/AdminCustomAiStatsController.java`
- `server/src/main/java/com/airesume/server/service/UserAiUsageStatsService.java`
- `server/src/main/java/com/airesume/server/service/impl/UserAiUsageStatsServiceImpl.java`
- `server/src/main/java/com/airesume/server/mapper/UserAiUsageDetailMapper.java`
- 后端完整记录见 `tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。

## 本轮修改文件清单

- `aiEngines.js` 新增 `getCustomAiUsageTrends({ startDate, endDate })`，请求 `GET /api/admin/custom-ai/usage-trends`。
- `AdminAiEngineView.vue` 在现有“用户自定义 AI 用量统计”卡片内新增按日趋势区域。
- 趋势区域支持近 7 天、近 30 天和自定义日期范围，默认近 7 天。
- 趋势图使用现有 `chart.js` / `vue-chartjs` 展示“总调用”和“活跃用户”两条折线，无调用数据时展示“暂无趋势数据”。
- `admin.aiEngines.test.js` 和 `AdminAiEngineView.test.js` 补充趋势接口参数、初始化请求、近 30 天切换、自定义范围切换、有数据图表和空态断言。

## 前端实现方案

- 继续复用 AI 引擎配置页，不新增管理端首页图表，不新增独立统计页面，避免打散用户自定义 AI 配置与用量统计入口。
- 趋势筛选状态独立于单日统计表格的日期和分页状态，切换趋势范围不会影响下方单日用户明细。
- 页面初始化时同时加载 AI 引擎配置、每日上限、单日统计和近 7 天趋势；趋势刷新只重新请求趋势接口。
- 页面不展示 API Key、baseUrl、model、providerName 等 Provider 私密配置，只展示聚合后的调用量和活跃用户数。

## 后端实现方案

前端调用后端新增接口：

- `GET /api/admin/custom-ai/usage-trends`

查询参数：

- `startDate?: yyyy-MM-dd`
- `endDate?: yyyy-MM-dd`

响应字段用于展示：

- `startDate`
- `endDate`
- `totalCalls`
- `activeUserCount`
- `days[{ date, totalCalls, activeUserCount, typeStats }]`

## 数据存储方案

前端不新增本地持久化。趋势数据以后端基于 `user_ai_usage_detail` 明细表的按日聚合结果为准；本轮不新增表、不新增迁移。

## stage 更新说明

`frontend/tasks/stage.md` 顶部已记录本轮管理端自定义 AI 按日趋势图范围、验证结果和停止说明。

## 编译结果

- `npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

管理员可在 AI 引擎配置页查看用户自定义 AI 近 7 天、近 30 天或自定义日期范围的按日趋势；图表展示总调用量与活跃用户数。现有单日统计表格和日期筛选仍保留，且与趋势筛选互不干扰。

## 停止，不继续下一个功能

本轮只完成管理端自定义 AI 按日趋势图，不继续实现管理端首页趋势图、周/月聚合、TTS UI、更多 Provider 协议或新的管理端统计页面。

## 管理端自定义 AI 使用统计增强（2026-06-03）

## 当前任务所属模块

管理端 AI 引擎配置页、用户自定义 AI 使用统计、按功能分类调用明细展示。

## 前端文件定位

- `frontend/app/src/api/admin/aiEngines.js`
- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/__tests__/api/admin.aiEngines.test.js`
- `frontend/app/src/__tests__/views/AdminAiEngineView.test.js`

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/AdminCustomAiStatsController.java`
- `server/src/main/java/com/airesume/server/service/UserAiUsageStatsService.java`
- `server/src/main/java/com/airesume/server/service/impl/UserAiUsageStatsServiceImpl.java`
- 后端完整记录见 `tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。

## 本轮修改文件清单

- `aiEngines.js` 新增 `getCustomAiUsageStats({ date, page, pageSize })`，请求 `GET /api/admin/custom-ai/usage-stats`。
- `AdminAiEngineView.vue` 在每日上限卡片附近新增自定义 AI 使用统计卡片，展示总调用量、配置用户数、活跃用户数、功能分布和用户明细表。
- `AdminAiEngineView.vue` 支持统计日期切换、刷新和用户明细分页；空数据时展示空态文案。
- `admin.aiEngines.test.js` 与 `AdminAiEngineView.test.js` 补充 API 参数、页面渲染、统计查询和分页断言。

## 前端实现方案

- 继续复用 AI 引擎配置页，不新增独立统计页面，避免扩大管理端导航结构。
- 使用现有卡片、表格、标签、分页和日期选择器风格展示统计结果；本轮只做表格/汇总，不做趋势图。
- 用户明细只展示用户 ID、邮箱、昵称、调用总量和功能分布 chips，不展示 API Key、baseUrl、模型密钥等敏感配置。
- 页面初始化时同时加载 AI 引擎配置、每日上限和当日统计；日期或分页变化时只刷新统计数据。

## 后端实现方案

前端调用后端新增接口：

- `GET /api/admin/custom-ai/usage-stats`

响应用于展示：

- `totalCalls`
- `configuredUserCount`
- `activeUserCount`
- `typeStats`
- `userStats`
- `totalUsers`

## 数据存储方案

前端不新增本地持久化。统计数据以后端 `user_ai_usage_detail` 与 `user_ai_daily_usage` 聚合结果为准。

## stage 更新说明

`frontend/tasks/stage.md` 顶部已记录本轮管理端自定义 AI 使用统计增强范围、验证结果和停止说明。

## 编译结果

- `npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

管理员可在 AI 引擎配置页按日期查看用户自定义 AI 的当日汇总、功能分布和用户调用明细；页面不会展示用户 API Key 或 Provider 私密配置。

## 停止，不继续下一个功能

本轮只补齐管理端自定义 AI 使用统计，不继续实现 TTS UI、趋势图表、更多 Provider 协议或新的管理端统计页面。

## 设置页自定义 AI 图标替换（2026-06-01）

## 当前任务所属模块

用户设置中心、自定义 AI Provider 配置入口图标。

## 前端文件定位

- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/__tests__/views/SettingsView.test.js`
- `frontend/app/src/utils/featureIcons.js`
- `frontend/app/src/__tests__/utils/featureIcons.test.js`

## 后端文件定位

本轮只调整前端设置页图标，不涉及后端文件。

## 本轮修改文件清单

- `SettingsView.vue` 将设置侧栏“自定义 AI”分组图标从通用 `settings` 替换为 `membership-center`，并将自定义 AI 面板标题图标同步替换为 `membership-center`，实际使用现有 `membership-center.webp` 资源。
- `SettingsView.test.js` 增加源码级回归断言，确保自定义 AI 设置入口继续使用 `membership-center` 图标。
- `featureIcons.js` 将 `membership-center` 纳入首屏同步图标映射，避免设置页导航入口先闪现系统通知兜底图标。
- `featureIcons.test.js` 增加 `membership-center` 首屏图标映射断言。

## 前端实现方案

- 继续复用项目现有 `FeatureIcon` 组件和 `featureIcons` 资源映射，不在页面内硬编码图片路径。
- 仅替换自定义 AI 分组和面板标题图标，不改变设置页结构、配置表单、接口调用和交互逻辑。
- 因设置页左侧导航属于首屏可见区域，同步补齐 WebP/PNG fallback 的首屏映射，保证首帧就是目标图标。

## 后端实现方案

无后端改动。

## 数据存储方案

无数据存储改动。

## stage 更新说明

`frontend/tasks/stage.md` 顶部已记录本轮设置页自定义 AI 图标替换范围、验证结果和停止说明。

## 编译结果

- `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js src/__tests__/utils/featureIcons.test.js` 通过，2 个测试文件 / 41 个用例。
- `npm.cmd run build` 通过。

## 构建结果

生产构建通过，构建产物中包含 `membership-center` WebP/PNG 图标资源。

## 当前功能验收说明

设置中心左侧“自定义 AI”入口和自定义 AI 面板标题均使用 `membership-center.webp` 对应图标，不再使用通用设置图标。

## 停止，不继续下一个功能

本轮只完成设置页自定义 AI 图标替换，不继续扩展自定义 AI 配置项、会员能力、统计图表或新的设置页面。

## Review 修复补充（2026-06-01）

## 当前任务所属模块

模拟面试流式自定义 AI 回退 UI、首页 CTA 图标渲染、用户自定义 AI 前端回归验证。

## 前端文件定位

- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/views/HomePageView.vue`

## 后端文件定位

后端 review 修复见 `tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。

## 本轮修改文件清单

- `InterviewSessionView.vue` 在 SSE `type=error` payload 中保留后端业务错误码，4090/4091 会展示“使用平台 AI”手动回退卡片，并在重发时携带 `fallbackToPlatform=true`。
- `HomePageView.vue` 修复 CTA 按钮图标尺寸属性，保证首页回归测试中两个按钮图标正常渲染为中号图标。

## 前端实现方案

- 面试流式错误不自动回退平台 AI，只在用户点击按钮后重发原回答内容并显式请求平台 AI。
- 首页只修复图标尺寸，不改变现有首页功能入口和页面结构。

## 编译结果

- `npm.cmd test -- --run src/__tests__/views/HomePageView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/api/interview.test.js src/__tests__/utils/request.test.js` 通过，4 个测试文件 / 42 个用例。
- `npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/api/admin.aiEngines.test.js src/__tests__/api/resume.test.js src/__tests__/api/interview.test.js src/__tests__/utils/request.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/AdminAiEngineView.test.js src/__tests__/views/ResumeResultView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/HomePageView.test.js` 通过，10 个测试文件 / 90 个用例。
- `npm.cmd run build` 通过。

## 当前功能验收说明

自定义 AI 流式面试失败时，页面能读取后端错误码并展示手动平台 AI 回退入口；首页 CTA 图标保持稳定渲染。

## 停止，不继续下一个功能

本轮只修复 review 发现的问题，不继续扩展 TTS UI、统计图表、更多 Provider 协议适配或新的独立配置页面。

## 当前任务所属模块

用户设置中心、自定义 AI Provider 配置、简历 AI 能力失败回退、模拟面试流式消息失败回退、管理端 AI 引擎配置。

## 前端文件定位

- `frontend/app/src/api/userAiConfig.js`
- `frontend/app/src/api/admin/aiEngines.js`
- `frontend/app/src/api/resume.js`
- `frontend/app/src/api/interview.js`
- `frontend/app/src/utils/request.js`
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/views/resume/ResultView.vue`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/views/admin/AdminAiEngineView.vue`

## 后端文件定位

后端接口、数据库和 AI 调用链路见 `tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。

## 本轮修改文件清单

- 新增 `src/api/userAiConfig.js`，封装用户自定义 AI 配置 CRUD、启停、连通测试和用量查询接口。
- 扩展 `src/api/admin/aiEngines.js`，新增用户自定义 AI 每日上限查询/保存接口。
- 扩展 `src/api/resume.js`，上传、JD 匹配、AI 润色支持 `fallbackToPlatform`，JD 匹配和润色跳过默认错误弹窗以便页面展示回退 UI。
- 扩展 `src/api/interview.js`，流式消息请求体支持 `fallbackToPlatform`。
- 修改 `src/utils/request.js`，在 `skipDefaultErrorHandler` 场景保留后端业务错误码，保证 4090/4091 可被页面识别。
- 修改 `SettingsView.vue`，在设置中心新增“自定义 AI”分组，展示配置列表、今日用量、配置表单、启停、删除和连通测试。
- 修改 `ResultView.vue`，在 JD 匹配/AI 润色遇到 4090/4091 时展示手动“使用平台 AI”回退卡片。
- 修改 `InterviewSessionView.vue`，在流式面试消息遇到 4090/4091 时展示手动“使用平台 AI”回退卡片，并重发时携带 `fallbackToPlatform=true`。
- 修改 `AdminAiEngineView.vue`，在 AI 引擎配置页新增用户自定义 AI 每日调用上限入口，不新增统计图表。
- 新增/扩展前端单测：`userAiConfig.test.js`、`admin.aiEngines.test.js`、`resume.test.js`、`interview.test.js`、`request.test.js`、`SettingsView.test.js`、`AdminAiEngineView.test.js`。

## 前端实现方案

- 设置中心复用现有左侧分组导航和 `settings-panel` 样式，不新增独立页面，避免打断已有设置中心信息架构。
- 配置类型只暴露 `default/resume/interview` 三类；保存和连通测试前做基础非空校验，后端继续负责 HTTPS、SSRF、长度和白名单最终校验。
- API Key 输入只用于保存/测试；后端返回的脱敏 Key 只展示，不会回填进编辑表单，避免把脱敏值误提交。
- 失败回退只处理自定义 AI 相关错误码 4090/4091。默认不自动回退；用户点击“使用平台 AI”才重发请求并附带 `fallbackToPlatform=true`。
- 管理端每日上限直接复用 AI 引擎配置页顶部信息卡和输入框，不实现统计图表。

## 后端实现方案

前端调用后端新增接口：

- `GET /api/user/ai-config`
- `POST /api/user/ai-config`
- `DELETE /api/user/ai-config/{configType}`
- `PUT /api/user/ai-config/{configType}/toggle`
- `POST /api/user/ai-config/test-connectivity`
- `GET /api/user/ai-config/usage`
- `GET /api/admin/custom-ai/daily-limit`
- `PUT /api/admin/custom-ai/daily-limit`

## 数据存储方案

前端不新增本地持久化。用户自定义 AI 配置、脱敏 Key、连通状态和每日用量均以后端接口返回为准。

## stage 更新说明

`frontend/tasks/stage.md` 顶部已记录本轮用户自定义 AI Provider 前端接入范围、验证结果和停止说明。

## 编译结果

- `npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/api/admin.aiEngines.test.js src/__tests__/api/resume.test.js src/__tests__/api/interview.test.js src/__tests__/utils/request.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/AdminAiEngineView.test.js src/__tests__/views/ResumeResultView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 85 个用例。
- `npm.cmd run build` 通过。

## 构建结果

生产构建通过，`dist` 已生成最新产物；本轮未新增大模型、音频包或其它静态大文件。

## 当前功能验收说明

用户可在设置中心新增、查看、启停、删除自定义 AI 配置，并查看今日独立调用次数。JD 匹配、AI 润色和模拟面试消息在用户自定义 AI 失败或超限时不会自动消耗平台额度，页面只展示手动平台回退入口。

## 停止，不继续下一个功能

本轮只完成 OpenAI 兼容自定义 AI 配置、用量展示、手动平台回退和管理端每日上限入口，不实现 TTS UI、统计图表、更多 Provider 协议适配或新的独立配置页面。
# 管理端自定义 AI 用量趋势默认展开修复（2026-06-03）
## 当前任务所属模块
管理端 AI 引擎配置页、同页“自定义 AI 用量”分区、用户自定义 AI 趋势图、用户明细分页展示。

## 前端文件定位

- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/__tests__/views/AdminAiEngineView.test.js`

## 后端文件定位

本轮只调整前端展示状态，不涉及后端 Controller、Service、Mapper、DTO 或数据库脚本。

## 本轮修改文件清单

- `AdminAiEngineView.vue` 将自定义 AI 用量趋势图展开状态初始化为 `true`，切换进入“自定义 AI 用量”分区后默认直接显示趋势图。
- `AdminAiEngineView.vue` 保持用户明细分页参数不变，继续使用 `pageSize=5`，翻页仍按 `page/pageSize` 请求接口，不全量接收用户明细。
- `AdminAiEngineView.test.js` 将趋势默认展示、点击后可收起、无数据时默认空态展示纳入回归断言。

## 前端实现方案

- 由于自定义 AI 用量已被隔离到同页独立分区，趋势图默认展开不会再挤压 AI 引擎配置主流程。
- 保留趋势图折叠按钮，管理员仍可手动收起趋势区域。
- 不新增管理端首页图表、不新增独立页面、不新增子路由，不修改趋势接口参数、单日统计日期筛选或用户明细分页规则。

## 后端实现方案

无后端改动。继续复用既有：

- `GET /api/admin/custom-ai/usage-stats`
- `GET /api/admin/custom-ai/usage-trends`

## 数据存储方案

无数据存储改动。不新增表、字段、索引或迁移脚本。

## stage 更新说明

`frontend/tasks/stage.md` 与 `tasks/stage.md` 已记录本轮趋势默认展开和用户明细分页保持 5 条的前端联动结果。

## 编译结果

- RED 验证：旧实现下 `npm.cmd test -- --run src/__tests__/views/AdminAiEngineView.test.js` 失败，复现切换后趋势图仍默认折叠。
- GREEN 验证：`npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，2 个测试文件 / 14 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

管理员默认仍进入“引擎配置”分区；切换到“自定义 AI 用量”后，按日趋势图默认展开展示。用户明细表继续按每页 5 条分页请求，不会一次性接收全部用户。

## 停止，不继续下一个功能
本轮只调整自定义 AI 用量分区内的趋势默认展开状态，并确认用户明细分页保持每页 5 条；不继续实现管理端首页趋势图、周/月聚合、TTS UI、更多 Provider 协议或新的统计页面。
## 模型列表获取错误提示规整前端联动记录（2026-06-04）

## 当前任务所属模块

用户设置中心自定义 AI 接入、管理端 AI 引擎配置弹窗、模型列表获取失败提示展示链路。

## 前端文件定位

- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/api/userAiConfig.js`
- `frontend/app/src/api/admin/aiEngines.js`
- `frontend/app/src/__tests__/api/userAiConfig.test.js`
- `frontend/app/src/__tests__/api/admin.aiEngines.test.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`
- `frontend/app/src/__tests__/views/AdminAiEngineView.test.js`

## 后端文件定位

- `server/src/main/java/com/airesume/server/service/impl/AiModelDiscoveryServiceImpl.java`
- `server/src/test/java/com/airesume/server/service/impl/AiModelDiscoveryServiceImplTest.java`
- 后端完整记录见 `tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。

## 本轮修改文件清单

- 本轮未修改前端源码。
- 前端继续读取模型获取接口返回的 `errorMessage` 并展示错误提示；后端已将 404 HTML 等原始上游响应体规整为可理解中文提示。
- 现有失败边界保持不变：模型获取失败不清空手动输入的模型名，不阻止保存或连通测试。

## 前端实现方案

无需新增 UI 或 API 封装。用户端设置页和管理端弹窗仍通过既有 `fetchUserAiModels`、`fetchAdminAiModels` 调用后端模型发现接口；错误文案由后端统一规整，前端展示链路保持原样。

## 后端实现方案

后端在 `AiModelDiscoveryServiceImpl` 中按 HTTP 状态码生成用户可读提示，不再把上游 HTML 响应体透传到 `errorMessage`。

## 数据存储方案

无前端本地持久化变更，不新增接口保存字段或数据库字段。

## stage 更新说明

`frontend/tasks/stage.md` 已记录本轮前端联动验证；根目录 `tasks/stage.md` 已记录后端错误提示规整。

## 编译结果

- 前端目标回归：`npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，4 个测试文件 / 61 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

当模型获取接口遇到上游 404 HTML 响应时，设置页和管理端弹窗会展示后端规整后的中文原因，不再出现 HTML 标签或 openresty 网关内容；用户仍可手动输入模型名继续保存。

## 停止，不继续下一个功能

本轮只处理模型列表获取失败提示的前端展示联动验证，不新增前端交互、Provider 模板、模型能力检测、价格信息或自动路由。
