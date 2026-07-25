## 语音面试云端语音识别兜底前端（2026-07-25）
- 当前阶段：已在 `fix/edge-tts-voice` 分支完成语音输入的云端 STT 兜底，等待人工在语音面试中验收“浏览器识别失败自动切云端”链路。
- 问题根因：语音输入原本完全依赖浏览器 `webkitSpeechRecognition`（Chrome/Edge 把音频上传 Google 服务器识别），网络抖动即失败，Firefox 不支持；历史 TASK_67/68/69 的重启/看门狗/降级都只是绕过这条不稳定链路，空间已用尽。
- 实现方案：镜像现有云端 TTS 架构反向做 STT——`useCloudSpeechToText`（MediaRecorder 录音 + VAD 分段）上传后端 `/session/{id}/stt`，`useResilientSpeechToText` 协调器优先浏览器识别，出现 `network/service-not-allowed/start-timeout/end-without-result/no-transcript` 且云端可用时静默切云端，接口与 `useSpeechToText` 完全一致，`useVoiceCall` 零改动。
- 隔离边界：只替换语音通话侧的第二个 `useSpeechToText` 实例；文本听写实例、TTS 播报、面试对话 AI 全部不受影响。设置中心新增独立开关“浏览器识别失败时启用云端语音识别”（复用 `voiceRecognitionEngine` 枚举的 `cloud_fallback`，默认关闭），且必须后端 `stt-capability` 可用才真正生效。
- 后端实现：新增 `SttConfig`（`app.stt` 前缀，密钥走独立环境变量 `STT_API_KEY`，默认硅基流动 `FunAudioLLM/SenseVoiceSmall`，与面试对话/TTS 配置和密钥完全隔离）、`InterviewSttService`、`/session/{id}/stt-capability` 与 `/session/{id}/stt` 两个端点。未配置 `STT_API_KEY` 时能力探测返回不可用，前端保持“浏览器识别失败即降级手动输入”的现状。
- 测试验证：前端新增 `useResilientSpeechToText`/`useCloudSpeechToText` 单测并回归全部语音相关用例（`npm test` 10 文件 / 215 用例通过）；后端 `mvn test -Dtest=InterviewControllerTest,InterviewSttServiceImplTest` 25 用例通过。
- 构建验证：前端 `npm run build`、后端 `mvn clean package -DskipTests` 均通过。
- 停止说明：本轮只新增云端 STT 兜底与其独立开关，不改动浏览器识别主链路、不新增管理端 STT 配置页、不接入其它 ASR 服务商。

## 语音面试云端 STT 兜底 + 管理端配置（2026-07-25）
- 当前阶段：已完成浏览器 Web Speech 失败时的云端语音识别兜底，识别服务改由管理端在数据库中配置，等待人工在管理端“系统 STT 配置”填 Key 后验收语音面试收音链路。
- 问题根因：语音输入完全依赖浏览器 `webkitSpeechRecognition`（Chrome/Edge 上传 Google 服务器识别，网络抖动即失败，Firefox 不支持），TASK_67/68/69 的重启/看门狗只是绕同一 API 打补丁，空间已尽。
- 解决思路：镜像现有云端 TTS 架构反向做 STT——浏览器录音 → 后端 `/stt` → OpenAI 兼容 `/audio/transcriptions` → 返回文字。浏览器识别仍是主链路（零成本零延迟），失败时静默切云端。
- 完全隔离：面试页两个独立 STT 实例，只把语音通话实例换成协调器 `useResilientSpeechToText`，文本听写、TTS、面试对话 AI 一行不改；`useVoiceCall` 无感知。
- 管理端配置：新增 `sys_stt_config` 单例表（镜像 `sys_tts_config`），管理端 `AdminAiEngineView` 新增“系统 STT 配置”页（地址/模型/Key/端点 + 保存 + 连通性测试），API Key 加密存储、脱敏返回、编辑态复用旧密钥。默认硅基流动 `FunAudioLLM/SenseVoiceSmall`（免费）。
- 门控：只有“用户设置开启 cloud_fallback” + “后端 `stt-capability` 返回可用”同时成立才启用云端；任一不满足保持纯浏览器识别现状。设置开关复用 `voiceRecognitionEngine`（`cloud_fallback`/`system_local`），不新增偏好字段。
- 前端验证：`npx vitest run`（语音相关 + 管理端 STT API）通过，8 个测试文件 / 242 个用例（含新增 `useResilientSpeechToText`、`useCloudSpeechToText`、`admin.sttConfig`）。
- 后端验证：`InterviewSttServiceImplTest`(3) + `SysSttConfigServiceImplTest`(6) + `InterviewControllerTest`(19) 通过；`mvn clean package` 通过。
- 构建验证：前端 `npm run build` 通过，后端 `mvn clean package` 通过。
- 关联任务文件：`frontend/tasks/TASK_CLOUD_STT_FALLBACK_FRONTEND.md`、`tasks/TASK_CLOUD_STT_FALLBACK_BACKEND.md`。
- 停止说明：本轮只为语音面试语音输入增加云端 STT 兜底与管理端配置，不改动文本听写、不恢复离线 STT、不改动 TTS 链路、不新增其它语音服务。

## 合规分支四项回归修复前端（2026-06-07）
- 当前阶段：已继续在 `compliance/remove-community-membership` 分支完成本地 TTS capability 回归修复，等待人工在语音面试中验收本地 TTS Key 播报链路。
- 已完成内容：`InterviewSessionView.vue` 在后端 `tts-capability` 不可用时会检查浏览器本地 `buildClientTtsConfig()`；本地 TTS 配置完整则启用云端 TTS 播报，合成请求仍由 `synthesizeInterviewTts()` 临时携带 `clientTtsConfig`。
- Key 流向边界：capability GET 请求不携带本地 Key；没有本地 TTS 配置时继续回退浏览器 TTS，不误发云端合成请求。
- 前端验证：`npm.cmd test -- src/__tests__/views/InterviewSessionView.test.js src/__tests__/api/interview.test.js` 通过，2 个测试文件 / 59 个用例通过。
- 构建验证：`npm.cmd run build` 通过。
- 关键词扫描：`rg -n "sk-user-real|sk-local-secret|sk-local|tts-local|138-0000-0000|13800000000|zhangsan@example\\.com|zhangsan\\.dev|张三" frontend/app/dist frontend/app/src --glob "!**/__tests__/**"` 无命中。
- 后端联动：启动构造器、直连槽释放、源 PDF 24 小时重试窗口等后端修复见 `tasks/stage.md`。
- 停止说明：本轮只修复本地 TTS capability 回归，不继续推进历史数据清理入口、同步账号、新设置页面或其它前端功能。

## 用户 Key 本地化与简历数据脱敏合规改造前端（2026-06-07）
- 当前阶段：已继续在独立分支 `compliance/remove-community-membership` 上完成用户自定义 AI/TTS 配置本地化、隐私提示和业务请求临时传参，等待人工在设置中心、简历上传、面试和 TTS 试听链路中验收。
- 已完成内容：`localUserAiConfig` 按 `default/resume/interview` 使用 `localStorage` 保存配置；保存、删除、启停只影响当前浏览器，不再调用服务端持久化接口。
- Key 流向提示：设置中心和简历上传页展示“配置仅保存在当前浏览器；发起诊断、面试、测试或试听时会随本次请求临时发送到后端代理调用第三方，服务器不会保存或记录你的 Key。请确保当前设备安全。”
- 请求链路：简历上传、JD 匹配、AI 润色、面试创建、面试发消息、流式消息和 TTS 合成在本地配置启用时附带 `clientAiConfig` / `clientTtsConfig`；未配置时不附带，继续使用平台 AI/TTS。
- 静态样例：默认简历模板中的姓名、电话、邮箱、城市和个人链接已替换为脱敏占位，避免构建产物出现看似真实的完整个人样例。
- 后端联动：后端已退役用户 Key 持久化并接入简历脱敏保存边界，详见 `tasks/TASK_88_COMPLIANCE_LOCAL_KEYS_RESUME_PRIVACY_BACKEND.md`。
- 前端验证：`npm.cmd test -- src/__tests__/api/userAiConfig.test.js src/__tests__/api/resume.test.js src/__tests__/api/interview.test.js src/__tests__/views/SettingsView.test.js` 通过，4 个测试文件 / 67 个用例通过。
- 构建验证：`npm.cmd run build` 通过。
- 关键词复查：`rg -n "sk-user-real|sk-local-secret|sk-local|tts-local|138-0000-0000|13800000000|zhangsan@example\\.com|zhangsan\\.dev|张三" dist src --glob "!src/__tests__/**"` 无命中。
- 数据清理边界：部署环境为空库，本轮不新增历史数据清理入口或迁移。
- 关联任务文件：`frontend/tasks/TASK_88_COMPLIANCE_LOCAL_KEYS_RESUME_PRIVACY_FRONTEND.md`、`tasks/TASK_88_COMPLIANCE_LOCAL_KEYS_RESUME_PRIVACY_BACKEND.md`。
- 停止说明：本轮只处理用户 Key 本地化、隐私提示、请求临时传参和静态样例脱敏，不继续推进历史数据清理、账号同步或其它未指定功能。
---

## 合规部署社区与会员能力退役前端（2026-06-07）
- 当前阶段：本轮已在独立分支 `compliance/remove-community-membership` 上移除社区与会员前端可见入口，等待人工按部署包验收主导航、管理端菜单、设置页、报告页和首页。
- 已完成内容：移除用户侧社区/会员路由、路由预加载、顶部导航、首页卡片、仪表盘入口、设置页会员区、报告分享到社区入口和 VIP 升级触发；移除管理端社区审核、会员管理菜单及相关统计/通知/审计会员称谓。
- 管理端额度边界：`AdminUserRightsView` 继续保留用户额度查询、调整和重置能力，满足管理端仍可修改用户额度的要求。
- 构建产物边界：活跃功能图标注册表移除 `community-hub`、`community-activity`、`membership-credits`、`membership-center`，构建后 `dist` 与活跃入口文件关键词扫描无命中。
- 前端验证：`npm.cmd test -- src/__tests__/utils/featureIcons.test.js src/__tests__/compliance/removeCommunityMembership.test.js` 通过，2 个测试文件 / 7 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：后端已退役社区/会员 MVC 接口暴露并保留管理端额度接口，详见 `tasks/TASK_87_COMPLIANCE_REMOVE_COMMUNITY_MEMBERSHIP_BACKEND.md`。
- 关联任务文件：`frontend/tasks/TASK_87_COMPLIANCE_REMOVE_COMMUNITY_MEMBERSHIP_FRONTEND.md`、`tasks/TASK_87_COMPLIANCE_REMOVE_COMMUNITY_MEMBERSHIP_BACKEND.md`。
- 停止说明：本轮只处理合规部署所需的社区与会员前端入口退役，不继续物理删除历史源码、支付/订单链路或其它未指定功能。

---

## EdgeTTS AI 播报具体音色下拉前端（2026-06-05）
- 当前阶段：本轮已修复设置中心“AI 播报声音”中 EdgeTTS 只显示一个云端入口的问题，等待人工在设置中心展开下拉并逐个试听常用音色。
- 问题原因：上一轮只把 EdgeTTS 作为 `edge_cloud` provider shortcut 放进云端语音分组，没有把 EdgeTTS 内置免费 voice IDs 展开成独立偏好值。
- 已完成内容：`settingsPreferences.js` 新增共享 EdgeTTS voice 列表、云端语音下拉选项和 `edge_cloud:<voiceId>` 映射；`SettingsView.vue` 选择具体 EdgeTTS 音色时回填对应 `ttsVoiceId`，试听继续走后端音频 Blob；旧版 `edge_cloud` 偏好继续兼容默认晓晓。
- 前端验证：`npm.cmd test -- --run src/__tests__/utils/settingsPreferences.test.js src/__tests__/views/SettingsView.test.js` 通过，2 个测试文件 / 62 个用例。
- 关联任务文件：`frontend/tasks/TASK_84_EDGETTS_PROVIDER_FRONTEND.md`。
- 停止说明：本轮只修复 EdgeTTS 具体音色选择入口，不继续推进后端协议、音频存储、计费统计、流式音频或 STT。
---

## EdgeTTS AI 播报偏好代码审查修复前端（2026-06-05）
- 当前阶段：本轮已修复 EdgeTTS 云端音色偏好过早持久化问题，等待人工在设置中心保存 EdgeTTS 配置后验收刷新回显。
- 问题原因：旧实现选择“EdgeTTS 免费云端音色”时立即保存 `edge_cloud` 本地偏好，用户未保存自定义 TTS 配置也会回显为云端音色，容易造成偏好状态与后端可用配置不一致。
- 已完成内容：`SettingsView.vue` 选择 `edge_cloud` 时只回填并展开 EdgeTTS TTS 表单；`handleUserAiConfigSave()` 在 EdgeTTS 配置保存成功后才写入 `edge_cloud` 本地偏好。
- 前端验证：`npm.cmd test -- src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 54 个用例。
- 后端联动：后端本轮仅修复 `UserTtsSpeechServiceImpl` 构造器注入回归，详见 `tasks/stage.md`。
- 关联任务文件：`frontend/tasks/TASK_84_EDGETTS_PROVIDER_FRONTEND.md`、`tasks/TASK_84_EDGETTS_PROVIDER_BACKEND.md`。
- 停止说明：本轮只修复 EdgeTTS 云端音色偏好保存时机，不继续推进新语音页面、音频存储、计费统计、流式音频或 STT。

---

## EdgeTTS 放入 AI 播报声音下拉前端（2026-06-05）
- 当前阶段：本轮已把 EdgeTTS 云端音色放入设置中心“AI 播报声音”下拉，等待人工在设置中心语音偏好和语音面试播放链路中验收。
- 已完成内容：`settingsPreferences.js` 新增 `edge_cloud` 本地偏好值；`SettingsView.vue` 在“AI 播报声音”新增“云端语音 / EdgeTTS 免费云端音色”，选择或刷新回显后自动调用既有 EdgeTTS provider 预设，回填 `https://speech.platform.bing.com`、`edge-tts`、`zh-CN-XiaoxiaoNeural` 和 `/consumer/speech/synthesize/readaloud/edge/v1`。
- 交互边界：EdgeTTS 云端音色不参与浏览器 `speechSynthesis` voice 可用性判断；Chrome 本地 voice 少也不会禁用该选项，试听按钮走后端 `previewTtsVoice` 返回的音频 Blob。
- 前端 RED 验证：旧实现下 `npm.cmd test -- --run src/__tests__/utils/settingsPreferences.test.js src/__tests__/views/SettingsView.test.js` 失败，复现 `edge_cloud` 偏好值不存在且“AI 播报声音”入口未联动 EdgeTTS。
- 前端 GREEN 验证：同一目标测试命令通过，2 个测试文件 / 60 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：无新增后端改动，继续复用已完成的 EdgeTTS 合成客户端和 TTS 预览接口。
- 关联任务文件：`frontend/tasks/TASK_84_EDGETTS_PROVIDER_FRONTEND.md`、`tasks/TASK_84_EDGETTS_PROVIDER_BACKEND.md`。
- 停止说明：本轮只处理“AI 播报声音”下拉入口，不继续推进新语音页面、音频存储、计费统计、流式音频或 STT。

---

## TTS 剩余厂商适配前端（2026-06-05）
- 当前阶段：本轮已完成 Gemini、MiniMax、Qwen、xAI 作为用户自定义 TTS 和系统级 TTS Provider 的前端启用，等待人工使用真实 Key 在设置中心、管理端系统 TTS 配置和语音面试中验收。
- 已完成内容：`SettingsView.vue` 与 `AdminAiEngineView.vue` 已解除四个 Provider 禁用状态，切换后自动回填默认 Base URL、模型、音色和 endpointPath。
- 预设值：Gemini 使用 `https://generativelanguage.googleapis.com` / `gemini-2.5-flash-preview-tts` / `Kore`；MiniMax 使用 `https://api.minimax.chat` / `speech-02-turbo` / `male-qn-qingse`；Qwen 使用 `https://dashscope.aliyuncs.com` / `qwen3-tts-flash` / `Cherry`；xAI 使用 `https://api.x.ai` / `grok-tts` / `Fritz-PlayAI`。
- 交互边界：保留现有表单、发现、试听、连通测试按钮和 payload 结构，`ttsProvider/endpointPath` 继续透传给后端；切换 Provider 只回填候选项，不触发网络请求。
- 前端 RED 验证：旧实现下新增测试失败，复现四个 Provider 仍禁用或未回填默认值的问题。
- 前端 GREEN 验证：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，2 个测试文件 / 70 个用例。
- 前端目标回归：`npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/api/admin.ttsConfig.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/AdminAiEngineView.test.js src/__tests__/api/interview.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，6 个测试文件 / 131 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：后端已完成 Provider 协议分发、动态 Content-Type、Qwen 官方 OSS URL 防护和预设发现；详见 `tasks/TASK_85_TTS_REMAINING_PROVIDERS_BACKEND.md`。
- 关联任务文件：`frontend/tasks/TASK_85_TTS_REMAINING_PROVIDERS_FRONTEND.md`、`tasks/TASK_85_TTS_REMAINING_PROVIDERS_BACKEND.md`。
- 停止说明：本轮只完成 Gemini、MiniMax、Qwen、xAI TTS Provider 前端启用，不继续推进 STT、流式音频、音频存储、计费统计或新语音页面。
---

## EdgeTTS 云端语音 Provider 前端（2026-06-05）

- 当前阶段：本轮已完成 EdgeTTS 作为用户自定义 TTS 和系统级 TTS provider 的前端接入，等待人工在设置中心、管理端系统 TTS 配置和语音面试中验收。
- 已完成内容：`SettingsView.vue` 与 `AdminAiEngineView.vue` 已新增 EdgeTTS provider 预设，切换后自动回填 `https://speech.platform.bing.com`、`edge-tts`、`zh-CN-XiaoxiaoNeural` 和 `/consumer/speech/synthesize/readaloud/edge/v1`。
- 无 Key 逻辑：EdgeTTS 下保存、测试、试听、获取模型/音色均不要求 API Key；OpenAI、MiMo 等其它 provider 仍保持 Key 校验。
- 音色列表：前端内置 Edge Neural 音色，包含 `zh-CN-XiaoxiaoNeural`、`zh-CN-YunxiNeural` 等常用中文音色。
- 前端 RED 验证：旧实现下目标测试失败，复现 EdgeTTS provider 不存在且切换后未写入 provider。
- 前端 GREEN 验证：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，2 个测试文件 / 68 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：后端已新增 EdgeTTS 合成客户端、无 Key 配置解析和预设发现；详见 `tasks/TASK_84_EDGETTS_PROVIDER_BACKEND.md`。
- 关联任务文件：`frontend/tasks/TASK_84_EDGETTS_PROVIDER_FRONTEND.md`、`tasks/TASK_84_EDGETTS_PROVIDER_BACKEND.md`。
- 停止说明：本轮只完成 EdgeTTS Provider 前端接入，不继续推进音频存储、计费统计、流式音频、STT 或其它语音能力。

---

## AI 润色 PDF 近一页导出压缩阈值前端修复（2026-06-04）
- 当前阶段：本轮已修复 AI 润色结果页 `导出 PDF` 在内容略超一页时仍生成两页的问题，等待人工用真实润色记录复测 PDF 页数和尾部内容完整性。
- 已完成内容：`resumePdfPagination` 的近一页单页压缩阈值从 `0.9` 放宽到 `0.85`，中等超出一页的润色简历会等比缩小并居中写入 A4 单页；明显长简历仍按原逻辑分页，避免过度压缩影响阅读。
- 问题根因：真实 AI 润色模板在 190mm 截图宽度下可能需要超过 10% 的轻微缩放才能落入 A4 单页，旧阈值会返回 2 页，导致尾部关键内容被切到第二页。
- 前端 RED 验证：新增 `1900x3100` 截图用例后，旧实现下 `npm.cmd test -- --run src/__tests__/utils/resumePdfPagination.test.js` 失败，返回 2 页。
- 前端 GREEN 验证：`npm.cmd test -- --run src/__tests__/utils/resumePdfPagination.test.js` 通过，5 个用例通过；`npm.cmd test -- --run src/__tests__/views/ResumeResultView.test.js src/__tests__/utils/resumePdfPagination.test.js` 通过，2 个测试文件 / 12 个用例通过。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：无后端改动，无数据库结构变更。
- 关联任务文件：`frontend/tasks/TASK_23_V11_RESUME_AI_POLISH_FRONTEND.md`。
- 停止说明：本轮只处理 AI 润色 PDF 近一页分页阈值问题，不继续推进后端 PDF、DOCX、图片导出、模板视觉重构或 AI 润色内容生成调整。

## 管理端 Dashboard 单请求加载前端（2026-06-04）

- 当前阶段：本轮已完成管理端 dashboard 请求量优化，等待人工在管理端数据看板复测首屏请求数量和筛选刷新。
- 已完成内容：`api/admin/dashboard.js` 新增 `getAdminDashboardSummary`；`AdminDashboardView.vue` 进入页面、刷新和应用筛选时改为一次请求 `/api/admin/dashboard/summary`，并将返回的 `overview/trends/hotJobRoles/businessDistribution` 写回原有页面状态。
- 兼容边界：旧四个前端 API 封装和后端旧接口均保留，不破坏兼容调用方；页面展示结构和图表逻辑不重做。
- 前端 RED 验证：`npm.cmd test -- --run src/__tests__/api/admin.dashboard.test.js src/__tests__/views/AdminDashboardView.test.js` 在旧实现下失败，复现缺少聚合 API 和页面单请求加载入口。
- 前端 GREEN 验证：同一目标测试命令通过，2 个测试文件 / 2 个用例。
- 全量回归补充：`DashboardView.test.js` 与 `SettingsView.test.js` 两个旧断言已调整为匹配当前源码；`npm.cmd test -- --run src/__tests__/views/DashboardView.test.js src/__tests__/views/SettingsView.test.js` 通过，2 个测试文件 / 54 个用例。
- 前端全量验证：`npm.cmd test` 通过，83 个测试文件 / 584 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：后端已新增 `GET /api/admin/dashboard/summary`、`DashboardSummaryResponse` 和 `admin:dashboardSummary` 5 分钟缓存；详见 `tasks/TASK_83_ADMIN_DASHBOARD_SUMMARY_BACKEND.md`。
- 关联任务文件：`frontend/tasks/TASK_83_ADMIN_DASHBOARD_SUMMARY_FRONTEND.md`、`tasks/TASK_83_ADMIN_DASHBOARD_SUMMARY_BACKEND.md`。
- 停止说明：本轮只处理管理端 dashboard 单请求加载，不继续推进其它请求量优化、实时推送或新统计维度。

## 简历诊断结果页轮询降噪前端（2026-06-04）

- 当前阶段：本轮已完成简历诊断结果页轮询降噪，等待人工在简历诊断 processing 场景中验收请求节奏。
- 已完成内容：`ResultView.vue` 将 processing / AI 分析阶段轮询从 2 秒放慢到 6 秒，pending / 排队阶段保持 3 秒；仍使用既有轻量状态接口，完成后再拉完整详情。
- 请求边界：未新增 SSE 或长轮询，未修改后端接口；完成状态识别后只调用一次完整详情接口，避免反复拉取诊断结果大字段。
- 前端 RED 验证：`npm.cmd test -- --run src/__tests__/views/ResumeResultView.test.js` 在旧实现下失败，5999ms 时已经触发第 3 次状态请求，复现 processing 仍按 2 秒轮询。
- 前端 GREEN 验证：`npm.cmd test -- --run src/__tests__/views/ResumeResultView.test.js` 通过，1 个测试文件 / 7 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：无后端改动，无数据库结构变更；后端轻量状态接口继续复用既有 `/api/resume/task/{taskId}/status`。
- 关联任务文件：`frontend/tasks/TASK_82_RESUME_RESULT_POLLING_FRONTEND.md`。
- 停止说明：本轮只处理简历诊断结果页轮询间隔，不继续推进管理端 dashboard 聚合接口或其它请求量优化。

## 管理端系统级 TTS 配置前端（2026-06-04）

- 当前阶段：本轮已完成 `develop-project.txt` 功能 2 的前端部分，等待人工在管理端、设置页和语音面试中验收。
- 已完成内容：
  - `AdminAiEngineView.vue` 新增“系统 TTS 配置”Tab，提供启用开关、Provider、Base URL、API Key、模型、音色、端点路径表单。
  - 管理端支持保存系统 TTS、测试连通性、获取模型/音色和预览音色。
  - 新增 `api/admin/ttsConfig.js`，封装系统 TTS 管理端接口；`previewAdminTtsVoice` 使用 `fetch` 接收 `audio/mpeg`。
  - `api/userAiConfig.js` 新增 `getSystemTtsStatus`。
  - `SettingsView.vue` 在自定义 AI 的 TTS 区块显示“当前使用系统提供的云端语音服务”或“当前使用自定义语音服务（优先于系统配置）”。
  - `InterviewSessionView.vue` 根据 capability 的 `engine` 显示“自定义云端 TTS”或“系统云端 TTS”。
- 前端 RED 验证：目标测试初次失败，复现缺少系统 TTS API、管理页 Tab、设置页状态提示和面试页系统来源文案。
- 前端 GREEN 验证：`npm.cmd test -- --run src/__tests__/api/admin.ttsConfig.test.js src/__tests__/api/userAiConfig.test.js src/__tests__/views/AdminAiEngineView.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，5 个测试文件 / 119 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：后端已新增系统 TTS 配置表、管理端接口、用户侧状态接口和语音面试系统 TTS 兜底；详见 `tasks/TASK_80_SYSTEM_TTS_CONFIG_BACKEND.md`。
- 关联任务文件：`frontend/tasks/TASK_80_SYSTEM_TTS_CONFIG_FRONTEND.md`、`tasks/TASK_80_SYSTEM_TTS_CONFIG_BACKEND.md`。
- 停止说明：本轮只完成系统级 TTS 配置，不继续推进浏览器音色预设扩展、TTS 计费统计、音频存储或其它语音能力。

## 用户额度消费记录与展示增强（2026-06-04）
- 当前阶段：本轮已完成用户额度消费记录与展示增强的前端部分，等待人工验收。
- 已完成内容：
  - Dashboard 6 宫格改造：quota-card 从 2 项 flex 布局改为 6 宫格 CSS Grid（3列→2列响应式）
  - 成长中心 Tab：GrowthCenterView 顶部新增 Tab 切换（成长概览/额度明细），集成 ConsumptionLogPanel
  - ConsumptionLogPanel：类型筛选栏 + 消费记录列表 + el-pagination 分页
  - AdminConsumptionLog：管理端消费记录组件，el-select 筛选 + el-table + el-pagination
  - AdminUserRightsView 用户详情 Drawer 新增「消费记录」Tab
  - api/quota.js 用户端 API、api/admin/users.js 新增 getAdminConsumptionLog
- 前端构建验证：`npm run build` ✅ 通过（5182 modules, 19.17s）
- 后端联动：后端已新增消费记录表、Service、Controller，详见 `tasks/task-用户额度消费记录与展示增强.md`。
- 关联任务文件：`frontend/tasks/task-用户额度消费记录与展示增强.md`、`tasks/task-用户额度消费记录与展示增强.md`。
- 停止说明：本轮只完成用户额度消费记录与展示增强，不继续推进其他功能。

## 管理端功能分布日期范围扩展前端（2026-06-04）
- 当前阶段：本轮已完成 `develop-project.txt` 中功能 1 的前端部分，等待人工在管理端“AI 引擎管理 / 自定义 AI 用量”中复测功能分布和用户明细日期范围联动。
- 已完成内容：`AdminAiEngineView.vue` 已将功能分布单日选择改为日期范围选择，新增“今天 / 近 7 天 / 近 30 天 / 自定义”快捷按钮，默认选中近 7 天；功能分布、汇总指标、用户明细和分页共用同一个日期范围，切换范围会重置用户明细页码。
- API 联动：`api/admin/aiEngines.js` 的 `getCustomAiUsageStats` 已支持按需传递 `date/startDate/endDate/page/pageSize`，不再固定发送单日 `date`。
- 后端联动：`/api/admin/custom-ai/usage-stats` 已支持 `startDate/endDate`，旧 `date` 参数保持兼容；详见 `tasks/TASK_79_CUSTOM_AI_USAGE_STATS_DATE_RANGE_BACKEND.md`。
- 前端 RED 验证：旧实现下目标用例失败，复现默认仍为单日、请求仍发送 `date`、缺少范围切换和快捷按钮行为。
- 前端 GREEN 验证：`npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，2 个测试文件 / 20 个用例。
- 前端全量验证：`npm.cmd test` 通过，80 个测试文件 / 574 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_79_CUSTOM_AI_USAGE_STATS_DATE_RANGE_FRONTEND.md`、`tasks/TASK_79_CUSTOM_AI_USAGE_STATS_DATE_RANGE_BACKEND.md`。
- 停止说明：本轮只完成管理端功能分布日期范围扩展，不继续推进系统级 TTS 配置、TTS 计费统计、音频存储或新的语音能力。

## 自定义 TTS 播报延迟前端快修（2026-06-04）
- 当前阶段：本轮已完成用户自定义云端 TTS 播报等待体验快修，等待人工用真实 TTS Provider 在语音面试中复测首句状态和连续句播报。
- 问题原因：旧云端 TTS 队列在发起 `/tts` 合成请求前就把 `isSpeaking` 置为 true，UI 和通话层会显示“AI 正在回复/播报”，但真实音频必须等完整 Blob 返回后才会 `Audio.play()`；同时第二句只在上一句结束后才开始合成，导致句间继续等待。
- 已完成内容：`useCloudTextToSpeech` 新增 `isPreparing/isActive`，将云端合成等待期与真实播放态拆开；队列改为单路合成并在当前句播放期间提前合成下一句；`useVoiceCall` 通过 `isActive` 保持准备中暂停收音；`InterviewSessionView` 在准备期显示“AI 语音准备中”，真实播放后显示“AI 正在播报”，波形/状态点只在真实播放时进入 speaking 样式。
- 前端 RED 验证：旧实现下 `npm.cmd test -- --run src/__tests__/composables/useCloudTextToSpeech.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 失败，复现缺少 `isPreparing/isActive`、第二句未提前合成、页面等待期仍显示“AI 正在回复”。
- 前端 GREEN 验证：同一命令通过，3 个测试文件 / 82 个用例；扩展语音回归 `npm.cmd test -- --run src/__tests__/api/interview.test.js src/__tests__/composables/useCloudTextToSpeech.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，5 个测试文件 / 125 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：无后端改动，无数据库结构变更；`/api/interview/session/{sessionId}/tts` 仍返回完整 `audio/mpeg`。
- 关联任务文件：`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。
- 停止说明：本轮只处理自定义 TTS 播报延迟体验，不继续实现后端流式音频、系统级 TTS、TTS 计费统计、音频存储、STT 或新的 Provider 协议。

## 浏览器音色预设 Chrome 限制标注前端修复（2026-06-04）
- 当前阶段：本轮已修复用户反馈的 Chrome 下多个浏览器音色预设听起来都是同一声音的问题展示边界，等待人工在 Chrome 设置中心语音通话偏好中复测。
- 问题原因：Chrome 的 `speechSynthesis.getVoices()` 在部分系统上只暴露 1-2 种中文浏览器 voice，旧设置页只显示“实际音色”，没有告诉用户多个预设会共用同一真实 voice，导致用户误以为每个预设都应当有独立音色；Edge 暴露的系统 voice 较完整时表现正常。
- 已完成内容：`SettingsView.vue` 新增 Chrome 浏览器识别、中文 voice 去重计数和受限状态提示；当 Chrome 只暴露少量中文 voice 时，状态文案显示“Chrome 当前只暴露 N 种中文浏览器 voice，多个预设会共用同一音色”，下拉预设项同步标注“Chrome 共用 N 种 voice”；未匹配到的具体音色在 Chrome 下标注“Chrome 未暴露该音色”。不改变 Edge 的正常 voice 匹配逻辑，也不伪造浏览器没有暴露的音色。
- 前端 RED 验证：旧实现下 `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 失败，复现 Chrome 只暴露 1 种中文 voice 时仍只显示“实际音色：Google 普通话（中国大陆）”。
- 前端 GREEN 验证：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 46 个用例；`npm.cmd test` 通过，80 个测试文件 / 566 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：无后端改动，无数据库结构变更。
- 关联任务文件：`frontend/tasks/TASK_78_BROWSER_TTS_VOICE_PRESETS_FRONTEND.md`。
- 停止说明：本轮只处理 Chrome 浏览器 voice 数量限制的标注和下拉提示，不继续推进功能分布日期范围、系统级 TTS 配置、云端 TTS、离线 TTS 或新的语音能力。

## 浏览器音色预设扩充前端（2026-06-04）
- 当前阶段：本轮已完成 `develop-project.txt` 中功能 3（浏览器音色预设扩充），等待人工在设置中心语音通话偏好和真实浏览器 voice 环境中验收。
- 已完成内容：`settingsPreferences.js` 集中维护 15 个浏览器 TTS 预设和绑定 `rate/pitch`；`useTextToSpeech.js` 增加具体预设 voice 匹配、可用性判断和参数读取；`SettingsView.vue` 将 AI 播报声音改为女声系列、男声系列、通用、自定义四组，并对当前系统无匹配 voice 的具体预设禁用标注；选择绑定预设会同步语速/音调滑块；`InterviewSessionView.vue` 在语音面试播报时用预设参数覆盖历史滑块值。
- 前端 RED 验证：旧实现下 `npm.cmd test -- --run src/__tests__/utils/settingsPreferences.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 失败，复现缺少 15+ 预设、预设参数、可用性判断、设置页滑块联动和面试页预设参数覆盖。
- 前端 GREEN 验证：同一目标测试命令通过，4 个测试文件 / 138 个用例；`npm.cmd test` 通过，80 个测试文件 / 565 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：无后端改动，无数据库结构变更。
- 关联任务文件：`frontend/tasks/TASK_78_BROWSER_TTS_VOICE_PRESETS_FRONTEND.md`。
- 尚未开始：功能 1（功能分布日期范围）和功能 2（系统级 TTS 配置）尚未实施。
- 停止说明：本轮只完成浏览器音色预设扩充，不继续推进功能分布日期范围、系统级 TTS 配置、云端 TTS 计费统计、音频存储或新的语音能力。

## 代码审查问题修复前端联动（2026-06-04）
- 当前阶段：本轮已修复设置页自定义 AI 选择框高度测试断言与当前实现不一致的问题，并联动后端修复 TTS 自定义端点透传和 Chat Completions TTS 音频数据校验，等待验收。
- 已完成内容：`SettingsView.test.js` 改为断言当前 `.cai-form` 通用选择框高度规则、`.cai-tts-discover-btn` 和统一高度变量；页面 UI 本身无改动。
- 后端联动：`UserAiConfigServiceImpl` 保存、测试、试听前验证会保留 `endpointPath/ttsProvider`；`UserTtsConnectivityTestServiceImpl` 要求 Chat Completions 响应中存在可解码且非空的 `choices[0].message.audio.data`。
- 前端验证：`npm.cmd test -- src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 43 个用例；`npm.cmd test -- src/__tests__/api/interview.test.js src/__tests__/api/userAiConfig.test.js src/__tests__/api/admin.aiEngines.test.js src/__tests__/composables/useCloudTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，7 个测试文件 / 117 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端验证：`mvn.cmd -q "-Dtest=UserAiConfigServiceImplTest,UserTtsConnectivityTestServiceImplTest,UserTtsSpeechServiceImplTest,CriticalEndpointRateLimitFilterTest,AdminAiEngineConnectivityControllerTest,AdminCustomAiStatsControllerTest,AiModelDiscoveryServiceImplTest,UserAiUsageStatsServiceImplTest" test` 通过；`mvn.cmd -q -DskipTests compile` 通过。
- 关联任务文件：`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`、`tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。
- 停止说明：本轮只处理代码审查指出的 3 个问题，不继续扩展 TTS UI、Provider 协议、计费统计、音频存储或新的语音能力。

## TTS 配置选择框高度统一前端修复（2026-06-03）
- 当前阶段：本轮已修复设置页自定义 AI 的 TTS 配置区中 `el-select` 切换后视觉高度变小的问题，等待人工在设置页展开 TTS 配置区复测。
- 问题原因：自定义 AI 表单旧样式只覆盖了 Element Plus 旧结构里的 `.el-input__wrapper`，但当前 `el-select` 实际视觉边框使用 `.el-select__wrapper`；底部非 scoped 规则还把选择框锁定为 40px，导致字段从输入框变成选择框时内层 wrapper 仍按默认较小高度渲染。
- 已完成内容：`SettingsView.vue` 为自定义 AI 表单增加统一控件高度变量，并同步覆盖 `.el-input__wrapper`、`.el-select__wrapper`、TTS 发现行选择框和底部全局穿透规则；`SettingsView.test.js` 增加源码级回归断言，防止后续遗漏 Element Plus 选择框 wrapper。
- 前端 RED 验证：旧实现下新增 `keeps custom AI select wrappers aligned with input control height styles` 用例失败，复现选择框 wrapper 未纳入高度规则。
- 前端 GREEN 验证：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 43 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：无后端改动。
- 关联任务文件：`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。
- 停止说明：本轮只处理 TTS 配置区输入框与选择框高度一致性，不继续实现新 Provider 协议、模型发现能力扩展、TTS 播放链路或新的设置页面。

## 自定义 AI 模型列表获取前端（2026-06-03）
- 当前阶段：本轮已在用户设置页和管理端 AI 引擎弹窗接入 OpenAI 兼容模型候选获取，等待人工用真实 Provider 验收。
- 已完成内容：`userAiConfig.js` 新增 `fetchUserAiModels`，`admin/aiEngines.js` 新增 `fetchAdminAiModels`；用户设置页和管理端弹窗的模型字段改为可搜索、可手动输入的 `el-select allow-create`；按钮手动触发模型获取，成功后填充候选，当前模型为空时默认选中第一项。
- 失败边界：模型获取失败只提示错误，不清空已手动填写的模型名，不阻止保存或连通测试；API 地址或 Key 变化后清空旧候选，避免跨 Provider 误选。
- 后端联动：后端已新增 `POST /api/user/ai-config/models` 和 `POST /api/admin/ai-engines/models`，管理端编辑态支持复用已保存密钥；详见 `tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。
- 前端验证：`npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，4 个测试文件 / 60 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`、`tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。
- 停止说明：本轮只实现 OpenAI 兼容 `/models` 候选获取，不继续实现 Provider 专用协议、模型能力检测、价格信息、自动路由或更多 Provider 模板。

## 模拟面试语音降级后禁止自动回切语音模式前端修复（2026-06-03）
- 当前阶段：本轮已修复语音识别不可用自动切换文本后，后续用户用文本输入回答、AI 面试官回复结束又自动切回语音模式的问题，等待人工在真实 Chrome 语音面试中复测。
- 问题原因：`useVoiceCall` 进入文本降级后仍会排后台恢复探测，并且 AI 回复结束、TTS 播放结束的 watcher 会立即触发 `retrySpeechNow()`；当浏览器语音探测返回成功时，会在用户未主动选择的情况下退出文本降级。
- 已完成内容：删除文本降级后的后台定时恢复入口；`retrySpeechNow()` 改为只服务用户点击“重试语音”；AI 回复结束和 TTS 播放结束时如果仍处于文本降级则保持文本模式；降级文案改为提示用户手动重试，降级横幅仅保留“重试语音”按钮。
- 前端验证：RED 阶段 `npm.cmd test -- --run src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 失败并复现自动恢复；GREEN 阶段同命令通过，2 个测试文件 / 74 个用例；扩展语音回归 `npm.cmd test -- --run src/__tests__/utils/speechRecognitionCapability.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/SettingsView.test.js` 通过，6 个测试文件 / 166 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：无后端改动。
- 关联任务文件：`frontend/tasks/TASK_76_INTERVIEW_TTS_QUEUE_AND_MODE_SWITCH_FRONTEND.md`。
- 停止说明：本轮只处理语音降级后的主动恢复边界，不新增云端 STT/TTS、离线 TTS、后端语音服务或其它面试能力。

## 自定义 TTS 在语音面试中真实播放前端（2026-06-03）
- 当前阶段：本轮已将用户自定义 OpenAI 兼容 TTS 接入语音面试 AI 面试官播报，等待人工用真实 TTS Provider 在语音面试中验收开场白和后续追问播放。
- 已完成内容：`interview.js` 新增 TTS capability 查询和音频合成 API；新增 `useCloudTextToSpeech` 管理云端音频队列、Blob URL 释放和失败降级；`InterviewSessionView.vue` 在语音会话加载后查询 capability，云端可用时开场白和流式回复走后端 `/tts` 音频播放，不再调用浏览器 `speechSynthesis.speak`。
- 降级边界：没有可用自定义 TTS 时保持浏览器 TTS；云端 TTS 单句失败后本场只提示一次“云端语音暂不可用，已切回浏览器播报”，后续使用浏览器 TTS，不中断面试。
- 交互边界：本轮不新增语音设置页、不新增面试页配置面板；只在现有语音状态文案中显示云端 TTS 播放来源。
- 后端联动：后端已新增 `GET /api/interview/session/{sessionId}/tts-capability` 与 `POST /api/interview/session/{sessionId}/tts`，按 `interview -> default` 解析 TTS 配置，详见 `tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。
- 前端 RED 验证：旧实现下新增 API、`useCloudTextToSpeech` 和面试页云端 TTS 测试失败，复现缺少 capability 查询、云端播放队列和失败降级。
- 前端 GREEN 验证：`npm.cmd test -- --run src/__tests__/api/interview.test.js src/__tests__/composables/useCloudTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 55 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`、`tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。
- 停止说明：本轮只完成语音面试 AI 面试官播报接入用户自定义 TTS，不继续实现 STT、简历诊断 TTS、TTS 计费统计、音频存储、更多 Provider 协议或独立语音页面。

## TTS 配置折叠与通用兜底支持前端修正（2026-06-03）
- 当前阶段：本轮已按反馈让 TTS 支持“通用兜底”和“面试对话”，并把 TTS 配置区改为默认折叠，等待人工在设置页复测布局。
- 问题原因：TTS 当前只服务后续云端面试官播报，简历诊断不需要 TTS；通用兜底可以作为面试 TTS 的兜底配置；原先完整 TTS 表单默认展开会拉长配置页，且“未启用”胶囊文字视觉偏上。
- 已完成内容：`SettingsView.vue` 只在 `configType=default/interview` 时展示 TTS 配置区；TTS 配置区默认折叠，展开后才显示地址、模型、Key、音色和测试按钮；配置卡片“已配置 TTS”标记只在通用兜底和面试配置卡片显示；保存 payload 只在通用兜底和面试配置中携带 TTS 字段；简历配置不能触发 TTS 连通测试。
- 后端联动：后端 `UserAiConfigServiceImpl` 已同步限制只有 `default/interview` 配置可回显和保存 TTS；`resume` 保存时会清空 TTS 字段。
- 前端 RED 验证：旧实现下 `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 失败，复现通用兜底缺少 TTS 区块、TTS 区块不可折叠且状态胶囊没有专门居中样式。
- 前端 GREEN 验证：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，40 个用例。
- 前端回归验证：`npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/views/SettingsView.test.js` 通过，2 个测试文件 / 41 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`、`tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。
- 停止说明：本轮只修正 TTS 配置归属、折叠交互和状态胶囊布局，不继续实现云端 TTS 播放、音频流推送、TTS 计费统计、更多 Provider 协议或新的语音设置页面。

## 用户自定义 AI TTS 配置与连通测试前端（2026-06-03）
- 当前阶段：本轮已在设置页自定义 AI 配置中补齐 TTS 配置区和 OpenAI 兼容 `/audio/speech` 连通测试，等待人工填写真实 TTS Provider 后验收。
- 已完成内容：`userAiConfig.js` 新增 `testUserTtsConnectivity`；`SettingsView.vue` 新增 TTS Base URL、TTS API Key、TTS 模型、音色 ID 字段；保存自定义 AI 配置时会携带 TTS 字段；配置列表在 `ttsConfigured=true` 时展示“已配置 TTS”；TTS 连通测试按钮会调用后端新接口。
- 校验边界：TTS 字段采用全填或全不填规则；只要填写任一 TTS 字段，保存和连通测试前必须补齐全部 TTS 字段。后端返回的脱敏 TTS Key 只用于展示，不回填为明文提交值。
- 后端联动：后端已新增 `POST /api/user/ai-config/test-tts-connectivity`，复用 `AiCredentialCrypto` 加密 TTS Key，复用 `PublicHttpsUrlValidator` 校验公网 HTTPS URL，并接入关键端点每用户或每 IP 每分钟 5 次限流。
- 运行时边界：本轮不修改模拟面试播报链路，面试语音仍使用浏览器 `speechSynthesis`，不新增云端音频流推送或 TTS 计费。
- 前端 RED 验证：旧实现下新增 TTS API 与设置页测试失败，复现缺少 `testUserTtsConnectivity`、TTS 表单字段和保存 payload。
- 前端 GREEN 验证：`npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/views/SettingsView.test.js` 通过。
- 构建验证：`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`、`tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。
- 停止说明：本轮只完成用户自定义 AI TTS 配置 UI、API 封装和连通测试，不继续实现云端 TTS 播放、音频流推送、TTS 计费统计、更多 Provider 协议或新的语音设置页面。

## 模拟面试后续流式 TTS 启动兜底前端修复（2026-06-03）
- 当前阶段：本轮已修复“开场白能播报，但后续 AI 回复不再出声”的追加问题，等待人工在真实 Chrome 语音面试中复测第二轮及后续追问播报。
- 问题原因：开场白使用 `allowDefaultVoice + requireStartEvent`，Chrome 接受 `speechSynthesis.speak()` 但不触发 `onstart` 时会用默认 voice 重试；后续流式回复只调用 `speakStreaming()`，没有传入同一启动检测参数。当 Chrome 残留 `speaking/pending` 状态时，后续回复会绕过启动 watchdog，表现为调用了 `speak()` 但没有声音。
- 已完成内容：`useTextToSpeech.speakStreaming()` 与 `flushRemaining()` 支持透传播报参数；`InterviewSessionView` 为后续流式追问和 done 后尾句补播统一传入 `INTERVIEW_STREAM_SPEECH_OPTIONS`，强制等待 `onstart`，未启动时用浏览器默认 voice 重试；新增 composable 和面试页级别回归用例覆盖 Chrome stale 状态。
- 前端验证：RED 阶段 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js` 失败，复现后续流式回复没有第 2 次默认 voice 重试；GREEN 阶段同命令通过，2 个测试文件 / 79 个用例；扩展语音回归 `npm.cmd test -- --run src/__tests__/utils/speechRecognitionCapability.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/SettingsView.test.js` 通过，6 个测试文件 / 160 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：无后端改动。
- 关联任务文件：`frontend/tasks/TASK_76_INTERVIEW_TTS_QUEUE_AND_MODE_SWITCH_FRONTEND.md`。
- 停止说明：本轮只处理后续流式 TTS 启动兜底，不新增云端 TTS/STT、离线语音包或其它面试能力。

## 管理端 AI 引擎配置与自定义 AI 用量分区修复（2026-06-03）
- 当前阶段：本轮已将管理端 AI 引擎配置页拆为同页两个分区，默认展示“引擎配置”，自定义 AI 用量统计只在切换到“自定义 AI 用量”后展示，等待人工复测默认首屏是否聚焦配置表。
- 问题原因：上一轮虽然折叠了趋势图，但每日上限、用量统计、功能分布和用户明细仍默认排在 AI 引擎配置筛选栏前，用户调用多时会继续干扰配置页主布局。
- 已完成内容：新增“引擎配置 / 自定义 AI 用量”分区切换；每日上限、用量统计、趋势图、功能分布、用户明细移入“自定义 AI 用量”分区；用户明细分页 footer 在存在明细时展示总数和翻页控件，避免单页数据时误判没有分页能力。
- 分页边界：用户明细仍按既有接口以 `page/pageSize` 请求，初始化 `pageSize=5`，翻页重新请求指定页，不全量接收全部用户。
- 交互边界：不新增管理端首页图表，不新增独立统计页面或子路由；趋势筛选与单日统计日期、分页仍互不影响。
- 安全边界：页面仍只展示聚合调用量、用户基础标识和功能调用次数，不展示 API Key、baseUrl、model 或其它 Provider 私密配置。
- 前端验证：RED 阶段 `npm.cmd test -- --run src/__tests__/views/AdminAiEngineView.test.js` 失败，复现缺少分区切换且统计区默认显示；GREEN 阶段 `npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，2 个测试文件 / 14 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：无后端改动，继续使用既有 `GET /api/admin/custom-ai/usage-stats` 和 `GET /api/admin/custom-ai/usage-trends`。
- 关联任务文件：`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。
- 停止说明：本轮只处理 AI 引擎配置与自定义 AI 用量统计互相挤压、用户明细分页可见性问题，不继续实现管理端首页趋势图、周/月聚合、TTS UI、更多 Provider 协议或新的统计页面。

## 管理端自定义 AI 趋势图默认折叠布局修复（2026-06-03）
- 当前阶段：本轮已修复管理端 AI 引擎配置页中用户自定义 AI 趋势图默认展开导致配置筛选栏和表格被挤到下方的问题，等待人工在管理端页面复测首屏布局。
- 问题原因：上一轮按日趋势图在“用户自定义 AI 用量统计”卡片内默认直接渲染完整折线图，图表区高度约 260px，加上筛选控件后占据过多首屏空间。
- 已完成内容：趋势区域改为默认折叠摘要行，首屏只展示日期范围、区间总调用和活跃用户数；点击“展开趋势”后才展示近 7 天、近 30 天、自定义日期范围和折线图；展开后图表高度收紧为 220px，并补充窄屏纵向排布。
- 交互边界：默认近 7 天趋势请求保留；趋势筛选与单日统计日期、分页仍互不影响；不新增管理端首页图表，不新增独立统计页面。
- 安全边界：页面仍只展示聚合调用量和活跃用户数，不展示 API Key、baseUrl、model 或其它 Provider 私密配置。
- 前端验证：RED 阶段 `npm.cmd test -- --run src/__tests__/views/AdminAiEngineView.test.js` 失败，复现趋势图默认展开；GREEN 阶段 `npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，2 个测试文件 / 12 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：无后端改动，继续使用既有 `GET /api/admin/custom-ai/usage-trends`。
- 关联任务文件：`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。
- 停止说明：本轮只处理管理端自定义 AI 趋势图首屏布局挤压，不继续实现管理端首页趋势图、周/月聚合、TTS UI、更多 Provider 协议或新的统计页面。

## 模拟面试 TTS 队列恢复与播报中模式切换锁前端修复（2026-06-03）
- 当前阶段：本轮已修复 Chrome/Edge 语音播报偶发无声后后续播报/试听也无法出声的前端恢复路径，并补齐普通语音面试第二轮回复播报、AI 播报中禁止语音识别按钮和语音/文本模式切换的交互锁，等待人工在真实 Chrome 语音面试和设置页试听中复测。
- 问题原因：浏览器 `speechSynthesis` 是页面级单例，上一轮播报卡死后即使业务状态已释放，浏览器内部合成队列仍可能残留无声任务；普通 SSE 成功路径没有释放 `replyLocked/sending`，会让语音通话状态机一直把 AI 误判为“仍在回复”，影响后续收音恢复和按钮锁定；同时语音 overlay、折叠面板和文本区的语音识别按钮没有统一播报锁，可能在面试官播报中触发开麦或静音切换。
- 已完成内容：`useTextToSpeech` 每个新播报 run 的首句入队前先 `cancel()` 清理浏览器合成队列；`sendMessage()` 在 SSE 成功完成后释放 `replyLocked/sending`；`InterviewSessionView` 新增 `modeSwitchLocked`，播报/回复期间禁用并防御“切换文本模式”“语音模式”“重试语音”“开始通话”“语音识别按钮”等入口；流式朗读按可朗读增量继续处理后续追问。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，43 个用例；扩展回归 `npm.cmd test -- --run src/__tests__/utils/speechRecognitionCapability.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/SettingsView.test.js` 通过，6 个测试文件 / 158 个用例；补强普通成功路径释放后再次执行同一组语音回归，158 个用例通过。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：无后端改动。
- 关联任务文件：`frontend/tasks/TASK_76_INTERVIEW_TTS_QUEUE_AND_MODE_SWITCH_FRONTEND.md`。
- 停止说明：本轮只处理浏览器 TTS 队列恢复和播报中禁止模式切换，不继续新增云端 TTS/STT、离线语音包或其它面试能力。

## 管理端自定义 AI 按日趋势图前端（2026-06-03）
- 当前阶段：本轮已在管理端 AI 引擎配置页补齐用户自定义 AI 按日趋势图，等待人工用真实调用数据验收近 7 天、近 30 天和自定义日期范围。
- 已完成内容：新增 `getCustomAiUsageTrends({ startDate, endDate })` API 封装；在现有“用户自定义 AI 用量统计”卡片内新增趋势区域；默认请求近 7 天；支持近 30 天和自定义范围；使用现有 `chart.js` / `vue-chartjs` 展示“总调用”和“活跃用户”两条折线；无数据时展示“暂无趋势数据”。
- 交互边界：趋势筛选与现有单日统计日期、分页互不影响；不新增管理端首页图表，不新增独立统计页面。
- 安全边界：页面只展示聚合调用量和活跃用户数，不展示 API Key、baseUrl、model 或其它 Provider 私密配置。
- 前端验证：`npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：后端已新增 `GET /api/admin/custom-ai/usage-trends`，基于 `user_ai_usage_detail` 明细表聚合，默认近 7 天、单日查询、非法范围和 90 天上限均由 Service 校验，详见 `tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。
- 关联任务文件：`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。
- 停止说明：本轮只补齐管理端自定义 AI 按日趋势图，不继续实现管理端首页趋势图、周/月聚合、TTS UI、更多 Provider 协议或新的统计页面。

## Web Speech 语音不可用自动切换文本回答前端修复（2026-06-03）
- 当前阶段：本轮已修复语音识别不可用时仍停留在语音通话窗口的问题，等待人工在 Chrome/Edge 真实语音面试中复测降级和重试。
- 问题原因：`showVoiceOverlay` 未排除 `voiceCall.isTextFallbackMode` 和语音能力不可用状态，`showTextInput` 只在手动切换或折叠时显示，导致用户必须手动点“切换文本模式”。
- 已完成内容：语音运行时降级、语音识别不支持或语音播报不支持时自动关闭通话 overlay，打开底部文本输入区；输入框上方显示降级提示；语音能力仍可用时保留“重试语音”按钮，恢复失败会继续回到文本输入。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/utils/speechRecognitionCapability.test.js` 通过，4 个测试文件 / 82 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：无后端改动。
- 关联任务文件：`frontend/tasks/TASK_69_WEB_SPEECH_AUTO_TEXT_FALLBACK_FRONTEND.md`。
- 停止说明：本轮只处理语音不可用自动切换文本回答和重试入口，不恢复本地语音包安装提示，不新增云端 STT、后端音频识别或其它语音方案。

## 管理端自定义 AI 使用统计增强（2026-06-03）
- 当前阶段：本轮已在管理端 AI 引擎配置页补齐用户自定义 AI 使用统计，等待人工按日期查看真实调用数据。
- 已完成内容：新增 `getCustomAiUsageStats({ date, page, pageSize })` API 封装；AI 引擎配置页新增自定义 AI 统计卡片，展示总调用量、配置用户数、活跃用户数、功能分布和用户明细分页；日期切换、刷新和分页会重新拉取统计。
- 安全边界：页面只展示用户基础信息和调用次数，不展示 API Key、baseUrl、模型密钥或其它 Provider 私密配置。
- 前端验证：`npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：后端已新增 `user_ai_usage_detail`、按功能类型计数和 `GET /api/admin/custom-ai/usage-stats`，详见 `tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。
- 关联任务文件：`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。
- 停止说明：本轮只补齐管理端自定义 AI 使用统计，不继续实现 TTS UI、趋势图表、更多 Provider 协议或新的管理端统计页面。

## 未提交改动代码审查问题前端修复（2026-06-02）
- 当前阶段：本轮已修复设置页本地语音包入口判断和安装结果处理问题，等待人工在支持浏览器本地语音包的环境中复测设置页语音通话分组。
- 已完成内容：本地语音包安装入口只在 `detectSpeechRecognitionCapability()` 返回 `LOCAL_DOWNLOADABLE` 状态时展示；安装接口只以 `installResult.ok` 作为成功依据；能力检测失败时显式保持入口关闭，避免空 `catch` 和不可确认能力展示。
- 前端验证：`npm.cmd test -- src/__tests__/views/SettingsView.test.js` 通过，40 个用例全绿。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：后端已同步修复验证码原子消费和流式平台 fallback 扣费顺序，详见 `tasks/TASK_CODE_REVIEW_UNCOMMITTED_FIX_2026_06_02_BACKEND.md`。
- 关联任务文件：`frontend/tasks/TASK_CODE_REVIEW_UNCOMMITTED_FIX_2026_06_02_FRONTEND.md`。
- 停止说明：本轮只修复代码审查指出的设置页本地语音包入口问题，不恢复离线 STT，不新增云端 STT、后端语音识别或其它语音方案。

## 简历诊断结果维度分数展示前端修复（2026-06-02）
- 当前阶段：本轮已修复简历诊断结果页在教育维度缺失时用总分兜底显示的误导问题，等待人工用真实诊断报告复测页面展示。
- 问题原因：轻量级状态轮询只发生在等待阶段，完成后仍会拉取完整详情；本轮前端问题是展示层曾经把缺失的 `educationEvaluation.score` 推导为总分，可能掩盖后端维度缺失。
- 已完成内容：`ResultView.vue` 删除 `computeEducationFallback`，评分概览和雷达图的教育背景分数均只读取 `educationEvaluation.score`，缺失时显示 0；后端配套字段别名归一化修复见 `tasks/TASK_75_RESUME_DIAGNOSIS_RESULT_ALIAS_NORMALIZATION_BACKEND.md`。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/ResumeResultView.test.js` 通过，7 个用例全绿；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_75_RESUME_DIAGNOSIS_RESULT_SCORE_DISPLAY_FRONTEND.md`。
- 停止说明：本轮只处理简历诊断结果页维度分数展示和后端字段别名修复的配套展示边界，不继续扩展报告导出、视觉重构、AI 生成策略或其它简历功能。
## 模拟面试与社区底框友好度修复（2026-06-02）
- 当前阶段：本轮已按用户反馈完成模拟面试提示条和社区页头横幅的浅色底框修复，等待人工在对应页面视觉验收。
- 已完成内容：`InterviewEntryView.vue` 将提示条调整为浅暖白渐变和轻描边，麦克风图标改为 `microphone-on` 中号尺寸并居中放入 52px 圆形底；`CommunityView.vue` 将社区页头横幅调整为浅暖白渐变和轻描边，并补充暗色主题变量覆盖。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/InterviewEntryView.test.js src/__tests__/views/community/CommunityView.test.js` 通过，2 个测试文件 / 36 个用例；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_INTERVIEW_COMMUNITY_FRIENDLY_FRAME_ICON_FRONTEND.md`。
- 停止说明：本轮只处理用户指定的模拟面试提示条和社区页头底框视觉问题，不继续扩展社区发帖、面试配置、语音能力或其它页面视觉重构。

## 高风险路由首次跳转白屏修复（2026-06-02）
- 当前阶段：本轮已完成 `/settings`、`/interview/session/:sessionId`、`/admin` 首次进入、`/community/my`、`/templates/editor/:templateId` 这 5 个高风险入口的前端冷加载体验修复，等待人工在真实路由切换和管理端登录流程中复测。
- 问题原因：这些入口存在“首次点击才冷加载页面 chunk”的路径，其中面试会话页和模板编辑页还是 `useLayout:false`，主布局骨架兜不住；管理端首次进入时外层布局尚未挂载；设置页和个人动态页入口没有明确预取，导致第一次切换时容易出现白屏或长时间空白。
- 已完成内容：新增 `settingsRouteLoader`、`interviewSessionRouteLoader`、`communityMyRouteLoader`、`templateEditorRouteLoader`、`adminLayoutRouteLoader` 并让 router 统一使用；新增 `prefetchInterviewSessionRoute()`、`prefetchTemplateEditorRoute(templateId)`、`prefetchAdminShellRoute()`；设置入口、头像菜单个人动态、社区个人动态按钮、创建面试、模板使用、管理端登录成功跳转前均接入意图预取；`MainLayout` 补充设置页和个人动态骨架，`App.vue` 补充无布局高风险页面全屏加载反馈。
- 前端验证：新增断言先进入 RED，再实现转 GREEN；`npm.cmd test -- --run src/__tests__/router/routeLoaders.test.js src/__tests__/components/AppHeader.test.js src/__tests__/views/InterviewEntryView.test.js src/__tests__/views/community/CommunityView.test.js src/__tests__/components/template/TemplateCard.test.js src/__tests__/views/admin/AdminLoginView.test.js src/__tests__/App.test.js src/__tests__/layouts/MainLayout.test.js` 通过，8 个测试文件 / 63 个用例。
- 全量验证：`npm.cmd test -- --run` 通过，79 个测试文件 / 522 个用例通过；`npm.cmd run build` 通过。全量验证前同步修正了两个既有测试侧阻塞点：`AdminAiEngineView.test.js` 连通性用例在全量并发下的超时时间、`SettingsView.test.js` 与当前“自定义 AI 接入”“12/50”文案一致的断言。
- 关联任务文件：`frontend/tasks/TASK_74_HIGH_RISK_ROUTE_WHITE_SCREEN_FRONTEND.md`。
- 停止说明：本轮只修复用户指定的 5 个高风险入口，不继续扩大到通知、历史、仪表盘、会员、帖子详情等中低风险页面；不修改 `xlsx`，不重构设置页内部接口并发或后端缓存策略。

## 简历诊断状态轮询前端优化（2026-06-02）

- 当前阶段：本轮已将简历诊断结果页等待阶段改为优先轮询轻量状态接口，等待人工从上传简历到结果生成流程中复测数据库访问频率和日志输出。
- 问题原因：结果页原先在排队和 AI 分析阶段反复调用完整详情接口，会让后端重复读取 `resume_text`、`diagnosis_result` 等大字段；最新日志显示单轮诊断期间完整详情查询仍较多。
- 已完成内容：`getResumeTaskStatus` 已封装；`ResultView` 首次进入和等待轮询优先请求 `/api/resume/task/{taskId}/status`；任务完成后才补拉一次完整详情；手动刷新、岗位匹配和 AI 润色同步仍保留完整详情能力。
- 前端验证：`npm.cmd test -- --run src/__tests__/api/resume.test.js src/__tests__/views/ResumeResultView.test.js` 通过，2 个测试文件、8 个用例全绿。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：后端已新增简历诊断轻量状态接口并关闭默认/dev MyBatis 明细 SQL 输出，详见 `tasks/TASK_73_RESUME_DIAGNOSIS_STATUS_POLLING_AND_SQL_LOG_BACKEND.md`。
- 关联任务文件：`frontend/tasks/TASK_73_RESUME_DIAGNOSIS_STATUS_POLLING_FRONTEND.md`。
- 停止说明：本轮只处理简历诊断结果页等待轮询降载，不继续扩展报告展示、导出、AI 生成策略或其它简历功能。

## Web Speech API 软降级稳定性补强前端（2026-06-02）

- 当前阶段：本轮已完成 TASK_69 验收反馈中的稳定性补强，等待人工在 Chrome/Edge 语音面试中复测“重新启用语音”和浏览器不支持语音能力时的文本回答入口。
- 问题原因：上一版恢复探测仍可能在 `recognition.start()` 被浏览器接受、但尚未触发稳定识别事件时根据 `isRecording` 过早退出文本兜底；同时 STT 或 TTS 完全不支持时，语音面试页只提示不支持语音通话，没有正式文本回答入口。
- 已完成内容：`useSpeechToText.start({ waitForHealthyStart: true })` 增加 `onstart` 后 1000ms 观察窗口和 6000ms 首个有效事件 watchdog；`useVoiceCall.retrySpeechNow()` 只在健康探测明确成功后退出文本兜底；`InterviewSessionView` 在 STT/TTS 不支持时展示文本回答区、系统听写提示，并隐藏“重新启用语音”恢复按钮。
- 前端验证：RED 阶段新增健康探测、恢复误判和 unsupported 文本入口回归用例；GREEN 阶段 `npm.cmd test -- --run src/__tests__/utils/speechRecognitionCapability.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，4 个测试文件、84 个用例全绿。
- 构建验证：`npm.cmd run build` 通过；`dist` 单文件大小检查无超过 Cloudflare Pages 25MiB 的产物。
- 关联任务文件：`frontend/tasks/TASK_69_WEB_SPEECH_SOFT_FALLBACK_FRONTEND.md`。
- 停止说明：本轮只补强 TASK_69 前端软降级稳定性，不继续推进后端/Worker/STT 服务、云端语音识别、离线模型或其它面试功能。

## 模拟面试流式回复落库后误报失败恢复前端（2026-06-02）

- 当前阶段：本轮已修复模拟面试中“后端已保存面试官回复，但前端因未收到最终 SSE done 而显示发送失败”的前端误报问题，等待用户在真实模拟面试中复测消息压缩后的连续问答。
- 问题原因：前端原先只以 SSE `done` 事件作为成功标记；当后端已经落库助手消息、但最后的 `done` 包或连接结束事件在浏览器侧丢失时，本地临时助手消息会被标记失败，刷新页面后又能从会话详情看到真实回复。
- 已完成内容：`InterviewSessionView` 在普通流式异常后主动同步一次会话详情；若服务端聊天记录中存在本次用户回答之后的助手回复，则替换本地聊天记录并取消错误提示；鉴权、限流和自定义 AI 可恢复错误仍沿用原业务处理路径。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，37 个用例全绿；扩展回归 `npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js src/__tests__/api/interview.test.js src/__tests__/views/InterviewReportView.test.js src/__tests__/router/routeLoaders.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/utils/speechRecognitionCapability.test.js` 通过，7 个测试文件、93 个用例全绿；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_72_INTERVIEW_STREAM_DONE_RECOVERY_FRONTEND.md`。
- 停止说明：本轮只处理流式回复落库后前端误报失败的恢复，不继续修改后端 SSE 协议、数据库结构、消息压缩策略或更多面试功能。

## Web Speech API 软降级与后台恢复前端实现（2026-06-02）

- 当前阶段：本轮已完成 TASK_69 前端实现，等待人工在 Chrome/Edge 模拟语音面试中验收语音暂不可用时的文本不中断和后台恢复体验。
- 已完成内容：新增 `speechRecognitionCapability` 能力检测工具；`useSpeechToText` 增加 Web Speech 启动健康检查、本地语言包可用/可安装状态和本地包安装入口；`useVoiceCall` 增加正式文本兜底模式、15s/30s/60s 后台退避恢复和手动“重新启用语音”；`InterviewSessionView` 在语音暂不可用时展示文本回答区、系统听写提示，并保留 STT 临时识别草稿。
- 边界说明：本轮不恢复 sherpa-onnx、Whisper、Vosk 或其它离线大模型；不新增云厂商 STT；不新增后端音频上传识别接口；不修改数据库结构。
- 前端验证：`npm.cmd test -- --run src/__tests__/utils/speechRecognitionCapability.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，4 个测试文件、74 个用例全绿。
- 构建验证：`npm.cmd run build` 通过；`dist` 单文件大小检查无超过 25MiB 的产物。
- 关联任务文件：`frontend/tasks/TASK_69_WEB_SPEECH_SOFT_FALLBACK_FRONTEND.md`。
- 停止说明：本轮只处理 Web Speech API 软降级与后台恢复前端实现，不继续推进后端/Worker/STT 服务、云端语音识别或其它语音方案。

## 模拟面试结束进入报告等待页白屏优化（2026-06-02）

- 当前阶段：本轮已优化点击结束面试后进入报告生成等待页的体验，等待人工在真实模拟面试结束流程中复测。
- 问题原因：报告页路由仍是裸动态 import，首次从无布局的面试会话页跳到带主布局的报告页时，需要冷加载报告页 chunk；同时结束成功后前端还会在当前页重新拉完整会话详情，导致点击确认后先停顿，再出现白屏/刷新感，最后才显示等待页。
- 已完成内容：新增报告页共享 route loader 和预取函数；打开结束确认弹窗时提前预取报告页 chunk；确认结束成功后直接进入 `/interview/report/:sessionId`，不再额外拉取当前会话完整详情；MainLayout 对 `/interview/report` 冷加载展示“正在打开面试报告”骨架占位。
- 前端验证：`npm.cmd test -- --run src/__tests__/router/routeLoaders.test.js src/__tests__/layouts/MainLayout.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 46 个用例。
- 扩展验证：`npm.cmd test -- --run src/__tests__/api/interview.test.js src/__tests__/views/InterviewReportView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/router/routeLoaders.test.js src/__tests__/layouts/MainLayout.test.js` 通过，5 个测试文件 / 55 个用例；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_71_INTERVIEW_END_REPORT_WAITING_WHITE_SCREEN_FRONTEND.md`。
- 停止说明：本轮只处理模拟面试结束后进入报告等待页的停顿和白屏，不继续扩展报告内容、报告导出、后端生成策略、语音能力或其它面试流程。

## 模拟面试会话轮询与报告覆盖优化前端（2026-06-02）

- 当前阶段：本轮已将开场白等待和报告生成等待改为优先轮询轻量状态接口，等待人工在真实模拟面试流程中复测数据库访问频率和报告完整性。
- 已完成内容：`getInterviewSessionStatus` 已封装；报告页仅在 `reportReady` 后重新拉完整详情；面试页仅在开场白不再 pending 后拉完整聊天记录。
- 前端验证：`npm.cmd test -- --run src/__tests__/api/interview.test.js src/__tests__/views/InterviewReportView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/utils/export.test.js` 通过，4 个测试文件、43 个用例全绿，其中 `export.test.js` 覆盖仓库已有提交；`npm.cmd run build` 通过。
- 后端联动：后端已新增轻量状态接口并补充报告有效问答轮次 prompt，详见 `tasks/TASK_70_INTERVIEW_SESSION_POLLING_AND_REPORT_OPTIMIZATION_BACKEND.md`。
- 关联任务文件：`frontend/tasks/TASK_70_INTERVIEW_SESSION_POLLING_AND_REPORT_OPTIMIZATION_FRONTEND.md`。
- 停止说明：本轮仅处理模拟面试轮询降载和报告生成覆盖提示，不继续扩展新的报告页面、导出能力或面试功能。

## XLSX 导出依赖按需加载优化（2026-06-02）
- 当前阶段：本轮已将管理端导出专用 `xlsx` 从首开路径中移出，等待人工在审计日志和订单管理导出场景验收。
- 问题原因：上一轮为缓解管理端冷加载，把 `xlsx` 放入 Vite `optimizeDeps.include`，同时 `src/utils/export.js` 顶部静态导入 `xlsx`；这会让只进入管理端但不导出的用户也承担导出库的冷加载和预优化成本。
- 已完成内容：`exportToXlsx()` 改为点击导出时动态 `import('xlsx')`；审计日志和订单管理导出处理函数改为 `async/await`，确保首次导出完成后再提示成功；`vite.config.js` 从开发环境预优化列表移除 `xlsx`。
- 前端验证：`npm.cmd test -- --run src/__tests__/utils/export.test.js src/__tests__/viteConfig.test.js` 通过，2 个测试文件 / 4 个用例。
- 构建验证：`npm.cmd run build` 通过；并发执行测试和构建时曾出现一次 Vite/Rollup Windows 绝对路径产物名错误，单独复跑构建未复现且已通过。
- 关联任务文件：`frontend/tasks/TASK_XLSX_ON_DEMAND_LOADING_FRONTEND.md`。
- 停止说明：本轮只处理 `xlsx` 按需加载，不继续扩展全量依赖治理、Service Worker、导出队列、更多导出格式或其它页面性能优化。

## 管理端路由冷加载白屏修复（2026-06-01）
- 当前阶段：本轮已修复管理端首次打开和首次切换不同后台路由时，Vite 开发环境冷加载等待过长、内容区白屏的问题，等待人工在管理端多路由切换场景验收。
- 问题原因：当前运行在 Vite dev server，首次访问懒加载后台路由时会触发页面 chunk 加载和重依赖预优化；管理端 `Transition mode="out-in"` 会先移除旧页面再等待新组件，冷加载期间内容区就表现为白屏。
- 已完成内容：新增管理端高频路由空闲预热，仅预热数据看板、用户权益、AI 引擎、Prompt 管理和监控总览；管理端路由切换等待超过阈值时展示“正在打开管理模块”的轻量占位；Vite dev 提前预优化 chart、xlsx、Element Plus、Naive UI 等重依赖，减少首次进入路由时的补扫描和整页刷新概率。
- 前端验证：`npm.cmd test -- --run src/__tests__/router/routeLoaders.test.js src/__tests__/layouts/AdminLayout.test.js src/__tests__/viteConfig.test.js` 通过，3 个测试文件 / 12 个用例；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_ADMIN_ROUTE_COLD_LOAD_WHITE_SCREEN_FRONTEND.md`。
- 停止说明：本轮只处理管理端冷加载白屏，不继续扩展全量后台预加载、Service Worker、持久缓存、后端缓存策略或其它页面性能优化。

## 首页 CTA 图标文字间距修复（2026-06-01）
- 当前阶段：本轮已按用户反馈修复首页两个主 CTA 按钮中图标与文字靠得过近的问题，等待人工在首页 Hero 区验收。
- 完成内容：`HomePageView.vue` 不再使用 Naive UI 的 `#icon` 插槽承载首页 CTA 图标，改为按钮内容内的 `.cta-content` 自管布局；图标与文字使用 `gap: 12px` 控制为适中距离，并保持文字不换行。
- 前端验证：已补充 `HomePageView.test.js` 源码级断言，锁定 CTA 自管内容结构、居中布局和 12px 间距；`npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 通过，1 个测试文件 / 6 个用例；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_HOME_CTA_ICON_SPACING_FRONTEND.md`。
- 停止说明：本轮只修复首页 CTA 图标文字间距，不继续调整首页其它模块、按钮文案、图标资源或路由逻辑。

## Web Speech API 软降级与后台恢复开发方案（2026-06-01）
- 当前阶段：本轮已按用户要求生成前端开发方案文档，目标是在不恢复离线大模型、不新增云厂商 STT、不突破 Cloudflare Pages 静态资源限制的前提下，把语音链路改为“语音优先、文本不断线、后台自动恢复”。当前等待用户确认后再进入实现。
- 问题原因：Chrome/Edge 的 Web Speech API 即使在用户网络正常时，也可能因为浏览器识别服务、语言包、权限、系统策略或服务端状态而临时不可用；前端无法强行修复浏览器底层服务，只能通过能力检测、健康检查、软降级和后台恢复保障面试流程不断。
- 方案内容：新增语音能力检测层，优先尝试浏览器本地语言包；为 Web Speech 启动增加健康检查；语音不可用时不退出面试，切换为正式文本回答模式；后台按退避策略探测语音恢复；文本模式提示 Windows/macOS/移动端系统听写兜底。
- 验证说明：本轮仅生成开发文档，未修改业务代码，未执行测试或构建；实际实现阶段必须补充 `useSpeechToText`、`useVoiceCall`、`InterviewSessionView` 相关回归测试，并执行定向测试和 `npm.cmd run build`。
- 关联任务文件：`frontend/tasks/TASK_69_WEB_SPEECH_SOFT_FALLBACK_FRONTEND.md`。
- 停止说明：本轮只生成开发方案文档，不开始代码实现，不新增 Cloudflare Workers AI、自建 STT、sherpa-onnx、Whisper、Vosk 或其它语音服务。

## 设置页自定义 AI 图标替换（2026-06-01）
- 当前阶段：本轮已按用户要求将设置中心“自定义 AI”入口和面板标题图标替换为 `membership-center.webp` 对应的 `membership-center` 图标，等待人工在设置页验收。
- 已完成内容：`SettingsView.vue` 的自定义 AI 分组图标和面板标题图标均从通用 `settings` 改为 `membership-center`；`featureIcons.js` 已将 `membership-center` 纳入首屏同步图标映射，避免设置页导航入口短暂闪现兜底图标；继续复用现有 `FeatureIcon` 和本地功能图标映射，不新增图片路径硬编码。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js src/__tests__/utils/featureIcons.test.js` 通过，2 个测试文件 / 41 个用例。
- 构建验证：`npm.cmd run build` 通过，构建产物包含 `membership-center` WebP/PNG 图标资源。
- 关联任务文件：`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。
- 停止说明：本轮只替换设置页自定义 AI 图标，不继续扩展自定义 AI 配置项、会员能力、统计图表或新的设置页面。

## 用户自定义 AI Provider Review 修复（2026-06-01）
- 当前阶段：本轮已修复未提交改动 review 中发现的前端问题，等待人工在首页和模拟面试流式回退场景验收。
- 已完成内容：模拟面试 SSE 错误 payload 会保留 4090/4091 自定义 AI 错误码，并展示“使用平台 AI”手动回退卡片；点击后重发原回答并携带 `fallbackToPlatform=true`。首页两个 CTA 按钮图标恢复为中号尺寸，满足页面回归断言。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/HomePageView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/api/interview.test.js src/__tests__/utils/request.test.js` 通过，4 个测试文件 / 42 个用例。
- 扩展验证：`npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/api/admin.aiEngines.test.js src/__tests__/api/resume.test.js src/__tests__/api/interview.test.js src/__tests__/utils/request.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/AdminAiEngineView.test.js src/__tests__/views/ResumeResultView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/HomePageView.test.js` 通过，10 个测试文件 / 90 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：后端已同步修复自定义 AI 删除、异步流式用量回滚、Offer 辅助计费和简历图片页用户上下文透传，详见 `tasks/stage.md`。
- 停止说明：本轮只修复 review 发现的问题，不继续推进 TTS UI、统计图表、更多 Provider 协议适配或新的独立配置页面。

## 用户自定义 AI Provider 前端接入（2026-06-01）
- 当前阶段：本轮已完成用户设置中心自定义 AI 配置入口、用户配置 API 封装、管理端每日上限入口，以及简历/面试自定义 AI 失败后的手动平台回退 UI，等待人工在设置中心、简历结果页、模拟面试页和后台 AI 引擎页验收。
- 已完成内容：设置中心新增“自定义 AI”分组，可查看 default/resume/interview 配置、今日用量、脱敏 Key、连通状态，并支持保存、启停、删除和连通测试；后端返回脱敏 Key 不回填表单，避免误提交。
- AI 入口适配：简历上传、JD 匹配、AI 润色、面试流式消息已支持 `fallbackToPlatform`；4090/4091 自定义 AI 错误会在页面展示“使用平台 AI”手动回退入口，点击后才附带 `fallbackToPlatform=true` 并消耗平台额度。
- 管理端联动：`AdminAiEngineView.vue` 已新增用户自定义 AI 每日上限输入和保存入口，复用现有 AI 引擎配置页，不新增统计图表。
- 前端验证：`npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/api/admin.aiEngines.test.js src/__tests__/api/resume.test.js src/__tests__/api/interview.test.js src/__tests__/utils/request.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/AdminAiEngineView.test.js src/__tests__/views/ResumeResultView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 85 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`、`tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。
- 停止说明：本轮只完成 OpenAI 兼容自定义 AI 配置、用量展示、手动平台回退和管理端每日上限入口，不实现 TTS UI、统计图表、更多 Provider 协议适配或新的独立配置页面。

## 稳定 Web Speech API 语音链路（2026-06-01）
- 当前阶段：本轮已按用户要求在移除离线引擎后继续稳定浏览器 Web Speech API，等待人工在 Chrome/Edge 语音面试中重点验收 STT 自动恢复和 TTS 开播重试。
- 问题原因：只依赖 Web Speech API 后，Chrome/Edge 仍可能偶发 `no-speech`、`no-transcript`、短暂 `network` 或无结果 `onend`，旧逻辑会直接进入手动恢复；TTS 也可能接受 `speak()` 但不触发 `onstart`，旧逻辑只释放状态，容易出现开场白或 AI 回复无声。
- 已完成内容：`useSpeechToText` 启动前清理旧 recognition 和音量监测资源；`useVoiceCall` 对可恢复 STT 中断短延迟自动重启收音并保留已识别文本，连续失败后才提示手动继续；`useTextToSpeech` 对开播超时增加一次浏览器默认 voice 重试，重试失败后释放状态不锁死。
- 前端验证：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，4 个测试文件 / 91 个用例；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_68_WEB_SPEECH_STABILITY_FRONTEND.md`。
- 停止说明：本轮只稳定现有 Web Speech API 语音识别和播报链路，不恢复 sherpa-onnx 离线 STT，不新增 Deepgram、云端 STT、后端语音识别、离线 TTS 或其它语音服务。

## 移除 sherpa-onnx 离线语音识别前端链路（2026-06-01）
- 当前阶段：本轮已按用户要求删除前端 sherpa-onnx 离线 STT 功能，语音识别改为只依赖浏览器 Web Speech API，等待人工在 Chrome/Edge 语音面试中验收。
- 删除范围：移除离线资源下载脚本、`sherpa-onnx` 依赖、离线模型 manifest/runtime、AudioWorklet、sherpa Worker、离线模型 Cache API 工具、Vite 离线模型 dev server 插件及对应离线专项测试。
- 已完成内容：`useSpeechToText` 收敛为浏览器语音识别；设置中心移除“离线增强”和离线包下载/删除入口；面试页不再预热离线 Worker 或展示离线包提示；设置偏好清洗历史 `offline_sherpa/offlineSttEngine` 字段；生产构建不再包含 `voice-models`、`audio-worklets`、sherpa 或 offline-stt 产物。
- 前端验证：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/utils/settingsPreferences.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，5 个测试文件 / 98 个用例；`npm.cmd run build` 通过。
- 产物检查：`dist` 中未发现 `voice-models`、`audio-worklets`、`sherpa`、`offline-stt`、`wasm-main-asr` 相关产物；`dist` 中未发现超过 25MiB 文件。
- 关联任务文件：`frontend/tasks/TASK_67_REMOVE_OFFLINE_STT_FRONTEND.md`、`tasks/TASK_67_REMOVE_OFFLINE_STT_BACKEND.md`。
- 停止说明：本轮只删除 sherpa-onnx 离线 STT 链路，不新增 Deepgram、云端 STT、后端语音识别或其它语音服务替代方案。

## 离线 STT Emscripten 主运行时 Blob 注入修复（2026-05-31）
- 当前阶段：本轮已继续修复远程源离线 sherpa-onnx 资源重新下载后，模拟语音面试仍报 `expected magic word 00 61 73 6d, found 3c 21 44 4f` 的前端运行时问题，等待人工刷新页面、重新进入语音面试复测。
- 问题原因：真实 `sherpa-onnx-wasm-main-asr.js` 主运行时开头会声明自己的 `var Module`，并通过局部 `fetch` / `XMLHttpRequest` 加载 `.wasm/.data`；在 Blob 脚本执行形态下，它仍可能没有稳定拿到 wrapper 预先设置的 `self.Module`，导致 `wasmBinary/instantiateWasm/locateFile` 防护失效并回到默认同源 wasm 路径。
- 已完成内容：`runtime.js` 在生成 Emscripten 主运行时 Blob 前显式注入 `var Module = self.Module || {};`、`var fetch = typeof self.fetch === "function" ? self.fetch.bind(self) : undefined;`、`var XMLHttpRequest = self.XMLHttpRequest;`，让主运行时局部变量绑定到 wrapper 已注入和已桥接的对象；保留上一轮 `wasmBinary/instantiateWasm/onAbort/fetch/XHR bridge` 防护；`OFFLINE_STT_RUNTIME_VERSION` 升级为 `20260531-runtime-main-bootstrap`。
- 前端验证：新增主运行时 Blob 局部 `Module` 注入 RED 回归用例；`npm.cmd test -- --run src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，4 个测试文件 / 90 个用例；`npm.cmd run build` 通过。
- 产物检查：`dist/voice-models/sherpa-onnx/zh-cn-streaming/` 包含 `manifest.json`、`runtime.js`、`runtime.js.gz`；`dist` 未发现超过 25MiB 的大模型文件；构建产物包含 `20260531-runtime-main-bootstrap` 和主运行时 bootstrap。
- 关联任务文件：`frontend/tasks/TASK_61_OFFLINE_STT_REMOTE_SOURCE_DOWNLOAD_PLAN_FRONTEND.md`。
- 停止说明：本轮只修复离线 STT Emscripten 主运行时 Blob 没有稳定继承注入对象的问题，不改后端代理、不改模型源 manifest、不新增离线 TTS、自有模型托管或其它语音能力。

## 社区评论下架与账号封禁前端治理（2026-05-31）
- 当前阶段：本轮已完成用户端评论/回复下架、用户端管理员封禁入口、管理端用户封禁接口切换和封禁信息展示，等待人工在社区列表、帖子详情、评论区和后台用户页验收。
- 已完成内容：`CommentSection.vue` 中管理员可下架评论/回复并填写 200 字以内原因；下架顶级评论会从当前列表移除整串回复并回退详情页评论数；帖子卡片、帖子详情和评论作者旁新增封禁入口；封禁弹窗支持 1天、7天、30天、永久四种时长和必填原因。
- 管理端联动：`AdminUserRightsView.vue` 的单个封禁、批量封禁已改用新封禁接口，解封改用新解封接口；用户表和 CSV 导出展示封禁到期时间与封禁原因摘要。
- 前端验证：`npm.cmd test -- --run src/__tests__/components/community/CommentSection.test.js src/__tests__/components/community/PostCard.test.js src/__tests__/views/community/PostDetailView.test.js src/__tests__/views/community/CommunityView.test.js src/__tests__/views/admin/AdminUserRightsView.test.js src/__tests__/api/admin.users.test.js` 通过；`npm.cmd run build` 通过。
- 后端联动：后端评论下架和账号封禁接口、SQL 字段、登录/JWT 封禁拦截已完成，详见 `tasks/TASK_66_COMMUNITY_COMMENT_HIDE_AND_USER_BAN_BACKEND.md`。
- 停止说明：本轮只完成评论下架和全站账号封禁，不继续推进举报系统、社区禁言、用户信誉分或图片 AI 审核。

## 离线 STT Emscripten 默认资源路径桥接修复（2026-05-31）
- 当前阶段：本轮已继续修复远程源离线 sherpa-onnx 资源重新下载后，模拟语音面试仍报 `expected magic word 00 61 73 6d, found 3c 21 44 4f` 的前端运行时问题，等待人工刷新页面并进入语音面试复测。
- 问题原因：真实 Emscripten 主运行时在部分浏览器缓存/脚本形态下仍可能绕过 `Module.instantiateWasm` 或 `Module.locateFile`，直接按默认同源 `/voice-models/sherpa-onnx/zh-cn-streaming/sherpa-onnx-wasm-main-asr.wasm` / `.data` 请求资源；远程源下载模式下同源目录没有这些大文件，该路径会拿到 SPA HTML fallback，导致 wasm 编译读到 `<!DO`。
- 已完成内容：`runtime.js` 在导入 sherpa API wrapper 和 Emscripten 主运行时时临时桥接 `fetch` 与 `XMLHttpRequest`，把默认同源 `.wasm/.data` 路径重定向到已校验并生成的 Blob URL；保留 `wasmBinary/instantiateWasm/onAbort` 防护；`OFFLINE_STT_RUNTIME_VERSION` 升级为 `20260531-runtime-asset-bridge`，强制刷新静态 runtime。
- 前端验证：新增绕过 `instantiateWasm` 的默认 wasm fetch 和默认 data XHR 两个 RED 回归用例；`npm.cmd test -- --run src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，4 个测试文件 / 89 个用例；`npm.cmd run build` 通过。
- 产物检查：`dist/voice-models/sherpa-onnx/zh-cn-streaming/` 仅包含 `manifest.json`、`runtime.js`、`runtime.js.gz`；`dist` 未发现超过 25MiB 的大模型文件；构建产物包含 `20260531-runtime-asset-bridge` 与 `installEmscriptenRuntimeAssetBridge`。
- 关联任务文件：`frontend/tasks/TASK_61_OFFLINE_STT_REMOTE_SOURCE_DOWNLOAD_PLAN_FRONTEND.md`。
- 停止说明：本轮只修复离线 STT 前端 runtime 对 Emscripten 默认资源路径的桥接，不改后端代理、不改模型源 manifest、不新增离线 TTS、自有模型托管或其它语音能力。

## 离线 STT Emscripten instantiateWasm 运行时修复（2026-05-31）
- 当前阶段：本轮已修复离线 sherpa-onnx STT 已下载后仍可能卡在“通话准备中”的前端启动链路问题，等待人工在设置页重新下载资源包并进入模拟语音面试验收。
- 问题原因：即使前置 runtime 已经拿到并校验真实远程 `.wasm`，Emscripten 主运行时仍可能绕过缓存二进制，按默认同源 `/voice-models/...wasm` 路径再次请求并拿到 SPA HTML fallback；同时 WASM abort 没有可靠 reject，导致前端状态悬挂在准备中。
- 已完成内容：`runtime.js` 为 `Module` 注入 `instantiateWasm`，强制使用已校验的 `wasmBinary` 实例化，`locateFile` 继续只负责 `.data` 等资源定位；新增 `onAbort` 和导入异常 reject；兼容 classic 与 MODULARIZE factory 两类 Emscripten runtime；`OFFLINE_STT_RUNTIME_VERSION` 升级为 `20260531-instantiate-wasm-preload`，强制浏览器刷新静态 runtime。
- 前端状态处理：Worker 初始化失败后继续进入 `offline-error`，会停止录音并展示错误文案，不再停留在 loading/ready 中间态。
- 前端验证：新增真实 Emscripten 默认 `.wasm` fetch、防悬挂 abort、MODULARIZE factory 注入对象、Worker 初始化失败状态回归用例；`npm.cmd test -- --run src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，4 个测试文件 / 87 个用例；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_61_OFFLINE_STT_REMOTE_SOURCE_DOWNLOAD_PLAN_FRONTEND.md`。
- 停止说明：本轮只修复离线 STT 启动阶段继续命中 HTML 伪 WASM 和异常悬挂的根因，不改后端代理、不改模型源 manifest、不新增离线 TTS、自有模型托管或其它语音功能。

## 离线 STT WASM 运行时预加载与历史坏缓存清理（2026-05-31）
- 当前阶段：本轮已修复离线语音包重新下载后，模拟语音面试仍可能把同源 HTML fallback 当作 wasm 编译的问题，等待人工在设置页删除资源包、重新下载并进入模拟语音面试验收。
- 问题原因：浏览器 Cache Storage 中可能同时存在远程 ModelScope 正常模型文件，以及历史残留的 `/voice-models/sherpa-onnx/zh-cn-streaming/sherpa-onnx-wasm-main-asr.wasm` `text/html` 坏缓存；Emscripten 主运行时仍可能按默认 wasm 路径取到该同源 HTML fallback。
- 已完成内容：`runtime.js` 将已校验的 wasm 响应转成 `ArrayBuffer` 注入 `Module.wasmBinary`，让 Emscripten 直接使用真实 wasm 二进制；`clearModelCache()` 删除资源包时会同步清理 `/voice-models/sherpa-onnx/zh-cn-streaming/` 前缀下的历史同源坏缓存；`OFFLINE_STT_RUNTIME_VERSION` 升级为 `20260531-wasm-binary-preload`，强制刷新静态 runtime。
- 前端验证：RED 阶段新增远程包删除时清理同源坏缓存、runtime 导入前注入 `Module.wasmBinary`、新版 runtime 查询参数三个回归断言；修复后 `npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/views/SettingsView.test.js` 通过，5 个测试文件 / 122 个用例；`npm.cmd run build` 通过。
- 产物检查：`dist/voice-models/sherpa-onnx/zh-cn-streaming/` 仅包含 `manifest.json` 和 `runtime.js`；`dist` 中无超过 25MiB 文件；构建产物包含 `20260531-wasm-binary-preload` 和 `wasmBinary`。
- 关联任务文件：`frontend/tasks/TASK_61_OFFLINE_STT_REMOTE_SOURCE_DOWNLOAD_PLAN_FRONTEND.md`。
- 停止说明：本轮只处理离线 STT 运行时继续命中 HTML 伪 WASM 的根因，不继续推进自有模型托管、离线 TTS 或其它语音能力。

## 管理端监控总览四列布局修复（2026-05-31）
- 当前阶段：本轮已修复管理端监控总览卡片在宽屏下自动铺成六列的问题，等待人工在后台监控总览页面验收。
- 问题原因：`AdminMonitorView.vue` 的指标区使用 `repeat(auto-fit, minmax(220px, 1fr))`，在宽屏容器中会根据剩余空间自动塞入 5-6 个卡片，导致“今日业务量”一行过密。
- 已完成内容：监控指标区改为桌面固定四列，中等屏幕两列，移动端单列；保留原有分组、卡片内容、接口调用和空/错状态逻辑。
- 前端验证：新增固定四列网格回归断言，`npm.cmd test -- --run src/__tests__/views/AdminMonitorView.test.js` 通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_14_ADMIN_FRONTEND_MONITOR_ENHANCE.md`。
- 停止说明：本轮只修复监控总览卡片列数，不继续推进其它监控指标、筛选、导出或管理端页面能力。

## 社区标题与下架原因长文本防护（2026-05-31）
- 当前阶段：本轮已完成社区列表标题省略、帖子详情完整标题弹窗、管理员下架原因 200 字前端校验，等待人工在社区列表、详情页和下架弹窗验收。
- 已完成内容：`PostCard.vue` 长标题限制为 2 行并保留完整标题悬停提示；`PostDetailView.vue` 长标题限制为 3 行并提供“查看完整标题”弹窗；`CommunityView.vue` 与 `PostDetailView.vue` 统一使用 `communityAdminHide.js` 校验下架原因不能为空且不能超过 200 字。
- 后端联动：后端 `CommunityService.adminHidePost` 同步限制下架原因 200 字，通知标题摘要截断，详情见 `tasks/TASK_65_COMMUNITY_LONG_TEXT_GUARD_BACKEND.md`。
- 前端验证：`npm.cmd test -- --run src/__tests__/components/community/PostCard.test.js src/__tests__/views/community/PostDetailView.test.js src/__tests__/views/community/CommunityView.test.js` 通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_65_COMMUNITY_LONG_TEXT_GUARD_FRONTEND.md`、`tasks/TASK_65_COMMUNITY_LONG_TEXT_GUARD_BACKEND.md`。
- 停止说明：本轮只处理长标题和下架原因长文本防护，不继续推进通知详情页重构、评论下架、举报系统或图片 AI 审核。

## 社区前台管理员下架（2026-05-31）
- 当前阶段：本轮已完成管理员账号在用户端社区下架帖子能力，等待人工在社区列表和详情页验收。
- 已完成内容：`PostCard.vue` 新增管理员“下架”入口；`CommunityView.vue` 和 `PostDetailView.vue` 在 `userInfo.role === 9` 时展示下架按钮，弹窗要求填写原因；下架成功后列表移除帖子，详情页返回社区首页。
- 后端联动：前端调用 `PUT /api/community/posts/{postId}/admin-hide`，后端强校验管理员角色，写入 `hidden` 状态和原因，并给作者发送站内通知。
- 前端验证：`npm.cmd test -- --run src/__tests__/components/community/PostCard.test.js src/__tests__/views/community/PostDetailView.test.js` 通过；`npm.cmd test -- --run src/__tests__/views/community/CommunityView.test.js` 通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_64_COMMUNITY_ADMIN_HIDE_AND_RISK_MARK_FRONTEND.md`、`tasks/TASK_64_COMMUNITY_ADMIN_HIDE_AND_RISK_MARK_BACKEND.md`。
- 停止说明：本轮只完成帖子级管理员前台下架，不继续推进评论下架、批量下架、举报入口或管理员专用审核列表。

## 管理端监控总览业务链路补齐（2026-05-31）
- 当前阶段：本轮已完成管理端监控总览页面业务链路指标补齐，等待人工在后台页面验收。
- 已完成内容：`AdminMonitorView.vue` 改为按“简历任务运行态 / 今日业务量 / 待处理事项”分组展示；新增已完成简历任务、今日简历润色、今日 JD 匹配、今日社区发帖、今日反馈、今日订单、反馈待处理/处理中、社区待审总数及帖子/评论拆分。
- 后端联动：前端继续调用 `GET /api/admin/monitor/overview`，使用后端追加字段；接口仍是应用层业务表统计版，不展示 RabbitMQ、Redis、JVM、数据库连接池等基础设施深度监控。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/AdminMonitorView.test.js` 通过，覆盖完整字段分组展示、全 0 空状态和接口失败提示；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_14_ADMIN_FRONTEND_MONITOR_ENHANCE.md`、`tasks/TASK_12_ADMIN_MONITORING_AND_DASHBOARD.md`。
- 停止说明：本轮只补齐监控总览业务指标展示，不继续推进监控详情页、导出、基础设施监控图表或其它管理端页面能力。

## 离线 STT 公共源缓存写入失败继续换源（2026-05-31）
- 当前阶段：本轮已修复公共模型源响应头可用、但 200MB 大文件写入浏览器 Cache API 过程中断后不会继续尝试下一个源的问题。
- 问题原因：`fetch()` 在拿到响应头后就会返回 `Response`，旧逻辑因此提前认为第一个公共源命中；真正消耗 200MB 响应体发生在后续 `cache.put()`，如果该阶段网络流中断，异常不会回到候选源循环，用户只能看到“当前模型源不可用”。
- 已完成内容：`offlineVoiceModelCache.js` 将 `fetch + HTML 校验 + cache.put` 合并到同一个候选源循环里，只有完整写入 Cache API 后才记录为命中 URL；第一个 ModelScope 公共源中途失败时会继续尝试 `www.modelscope.cn`、同源兜底和 HuggingFace 后续候选。
- 公共源复测：ModelScope `.data` 大文件 1 字节范围请求返回 `206 Partial Content`，`Content-Range: bytes 0-0/199059238`，并带 `Access-Control-Allow-Origin: *`；这说明不是必须把模型部署到本地，但免费公共源仍可能受网络和限流影响。
- 前端验证：RED 阶段新增 `tries the next mirror when caching the first large model response fails`，旧实现直接抛出 `stream interrupted`；修复后 `npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js` 通过，25 个用例。相关回归 `npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/views/SettingsView.test.js` 通过，5 个测试文件 / 116 个用例；`npm.cmd run build` 通过，`dist` 中无超过 25MiB 文件。
- 关联任务文件：`frontend/tasks/TASK_61_OFFLINE_STT_REMOTE_SOURCE_DOWNLOAD_PLAN_FRONTEND.md`。
- 停止说明：本轮只修复离线 STT 公共源下载候选切换，不继续推进自有模型托管、离线 TTS 或其它语音能力。

## 离线 STT 公共模型镜像默认下载说明修正（2026-05-31）
- 当前阶段：本轮已按用户明确要求，把离线语音识别包下载说明修正为“默认从现成公共模型镜像源拉取”，不再把自有 OSS/R2/COS/CDN 托管描述成必需方案。
- 问题原因：上一轮新增 `VITE_OFFLINE_STT_MODEL_BASE_URL` 可选覆盖能力后，阶段记录和设置页文案容易被理解为必须自行部署模型源；这与“白嫖直接从网上已有镜像源下载”的目标不一致。
- 已确认公共源：`hf.qhduan.com` 当前返回 401，`hf-mirror.com` 对 HuggingFace Space 路径当前返回 404，不适合作为默认候选；ModelScope `api/v1/studio/k2-fsa/web-assembly-asr-sherpa-onnx-zh-en/static/` 公共源经 1 字节范围请求验证返回 206，且带 `Access-Control-Allow-Origin: *`，继续作为默认第一候选。
- 已完成内容：设置页离线增强说明改为“默认由浏览器从 ModelScope 公共模型镜像源下载并缓存到本机；无需自建模型源”；`offlineVoiceModelCache.js` 注释改为“显式配置源只是覆盖入口，未配置时按 manifest 公共镜像候选下载”。
- 前端验证：RED 阶段新增设置页断言，确认旧文案未说明 `ModelScope 公共模型镜像源` 和 `无需自建模型源`；修复后 `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，42 个用例。
- 关联任务文件：`frontend/tasks/TASK_61_OFFLINE_STT_REMOTE_SOURCE_DOWNLOAD_PLAN_FRONTEND.md`。
- 停止说明：本轮只修正文案、注释和任务记录，不新增后端代理、不上传模型、不继续推进自有源托管或其它语音能力。

## 社区自动审核分流前端适配（2026-05-31）
- 当前阶段：本轮已完成发帖、分享报告、评论和回复对创建接口 `reviewStatus` 的状态化提示。
- 已完成内容：发帖和分享报告根据 `approved/pending` 展示公开或待审提示；评论和回复只有在 `approved` 时才本地插入并更新计数，`pending` 时只提示待审并清空输入。
- 后端联动：前端使用后端创建接口返回的 `{ id, reviewStatus }`；自动分流规则和后端验证见 `tasks/stage.md`。
- 前端验证：`npm.cmd test -- --run src/__tests__/components/community/PostEditor.test.js src/__tests__/components/community/CommentSection.test.js src/__tests__/components/community/ShareReportDialog.test.js` 通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_62_COMMUNITY_AUTO_MODERATION_ROUTING_FRONTEND.md`、`tasks/TASK_62_COMMUNITY_AUTO_MODERATION_ROUTING_BACKEND.md`。
- 停止说明：本轮只完成自动审核分流状态适配，不继续推进举报系统、批量审核、图片 AI 审核、敏感词配置页或云审核服务。

## 离线 STT 显式配置源覆盖能力（可选）（2026-05-31）
- 当前阶段：本轮曾在保留轻量构建的前提下，为离线语音识别包下载增加显式配置源覆盖能力。该能力只面向高级部署场景；默认仍按 `manifest.json` 中的 ModelScope 公共镜像候选下载。
- 问题原因：公共第三方源会受网络、CORS、限流和服务可用性影响，因此保留一个可选覆盖入口，方便后续需要时切到指定线上目录；这不是当前用户必须部署的前提。
- 已完成内容：`offlineVoiceModelCache.js` 支持 `VITE_OFFLINE_STT_MODEL_BASE_URL` 和 `window.__AI_RESUME_OFFLINE_STT_MODEL_BASE_URL__`；只有显式配置时才会把该源拼接出的模型 URL 放到候选源第一位，未配置时不影响公共源顺序。
- 可选使用方式：如未来确实需要覆盖公共源，可将 `sherpa-onnx-asr.js`、`sherpa-onnx-wasm-main-asr.js`、`sherpa-onnx-wasm-main-asr.wasm`、`sherpa-onnx-wasm-main-asr.data` 放到同一个线上目录，并配置 `VITE_OFFLINE_STT_MODEL_BASE_URL=https://cdn.example.com/ai-resume/sherpa/`。
- 前端验证：RED 阶段新增 `prepends configured remote model base url before public fallback sources` 并确认旧实现失败；修复后 `npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js` 通过，24 个用例；相关回归 `npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/composables/useSpeechToText.test.js` 通过，4 个测试文件 / 73 个用例；`npm.cmd run build` 通过。
- 构建产物检查：`dist/voice-models/sherpa-onnx/zh-cn-streaming/` 仅包含 `manifest.json` 和 `runtime.js`；`dist` 中未发现超过 25MiB 的文件。
- 关联任务文件：`frontend/tasks/TASK_61_OFFLINE_STT_REMOTE_SOURCE_DOWNLOAD_PLAN_FRONTEND.md`。
- 停止说明：本轮只保留离线 STT 显式配置源覆盖能力，不继续推进模型托管落地、离线 TTS 或其它语音能力。

## 管理端版本日志筛选查看（2026-05-31）
- 当前阶段：本轮已完成管理端版本日志筛选栏，等待人工在后台页面验收。
- 已完成内容：`AdminVersionLogView.vue` 新增关键词、版本类型、发布状态筛选；筛选变更回到第一页并调用后端分页查询；重置筛选清空条件；筛选结果展示当前页条数和后端总数；移动端筛选项纵向排列。
- 后端联动：前端传递 `type/status/keyword`，后端已做白名单校验、页大小上限和 `idx_version_log_filter_time(status, type, create_time)` 复合索引补强，详见 `tasks/TASK_ADMIN_VERSION_LOG_FILTER_BACKEND.md`。
- 前端验证：RED 阶段新增页面筛选测试失败于旧页面没有 `filterForm`；修复后 `npm.cmd test -- --run src/__tests__/views/AdminVersionLogView.test.js src/__tests__/api/admin.versionLogs.test.js` 通过，2 个测试文件 / 10 个用例；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_ADMIN_VERSION_LOG_FILTER_FRONTEND.md`、`tasks/TASK_ADMIN_VERSION_LOG_FILTER_BACKEND.md`。
- 停止说明：本轮只处理管理端版本日志筛选，不继续推进公开版本日志页筛选、时间范围筛选、导出或其它管理端页面能力。

## 离线 STT 大模型下载响应校验修复（2026-05-31）
- 当前阶段：本轮已修复离线语音识别包点击下载仍提示“当前模型源不可用”且控制台无报错的前端隐藏失败点。
- 问题原因：下载器为了判断模型源是否返回 SPA HTML fallback，会对非 `text/html` 响应调用 `response.clone().text()`；约 200MB 的 `sherpa-onnx-wasm-main-asr.data` 是 `application/octet-stream` 二进制文件，被整包按文本读取时可能触发浏览器内存、解码或流读取失败，最终只被设置页统一提示为模型源不可用。
- 已完成内容：`offlineVoiceModelCache.js` 对 `application/octet-stream`、`application/wasm`、`application/javascript`、`text/javascript` 直接跳过 HTML 文本嗅探；未知小响应只读取开头 1KB；超过 1MB 的未知响应不再整包读取。设置页下载失败时会 `console.warn` 输出真实错误，便于定位 `/api` 路由、后端部署或外部源问题。
- 前端验证：RED 阶段新增大二进制响应不应调用 `text()` 的回归用例并确认失败；修复后 `npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js` 通过，23 个用例；`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，42 个用例；相关回归 `npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/views/SettingsView.test.js src/__tests__/composables/useSpeechToText.test.js` 通过，5 个测试文件 / 114 个用例；`npm.cmd run build` 通过。
- 构建产物检查：`dist` 中无超过 25MiB 文件；`dist/voice-models/sherpa-onnx/zh-cn-streaming/` 仅包含 `manifest.json` 与 `runtime.js`。
- 后端联动：本轮无后端代码改动；后端兜底接口相关测试 `mvn.cmd test "-Dtest=OfflineSttModelProxyServiceTest,OfflineSttModelProxyControllerTest,SecurityConfigTest"` 通过，11 个用例。
- 关联任务文件：`frontend/tasks/TASK_61_OFFLINE_STT_REMOTE_SOURCE_DOWNLOAD_PLAN_FRONTEND.md`、`tasks/TASK_61_OFFLINE_STT_MODEL_PROXY_BACKEND.md`。
- 停止说明：本轮只修复离线 STT 下载时的大文件响应校验问题，不继续推进 R2/OSS/COS 托管、离线 TTS 或其它语音能力。

## 社区内容审核前端治理（2026-05-31）
- 当前阶段：本轮已完成社区内容“先审后发”的前端适配，用户发帖后会收到“已提交审核，通过后将在社区展示”的提示；管理端新增社区审核入口；作者可在个人动态查看待审核、未通过、已隐藏状态和审核原因。
- 已完成内容：新增 `AdminCommunityReviewView.vue`，支持帖子/评论审核列表、筛选、详情、通过、拒绝和隐藏；新增 `api/admin/community.js` 管理端接口封装；`/admin/community` 路由和管理端侧边栏入口已接入管理员鉴权；`MyActivity.vue` 已回显我的帖子审核状态与原因。
- 后端联动：前端使用后端新增的 `reviewStatus`、`reviewReason` 字段和 `/api/admin/community/**` 审核接口；数据库迁移和后端验证见 `tasks/stage.md`。
- 前端验证：新增状态回显用例先失败于审核原因未显示，修复后 `npm.cmd test -- --run src/__tests__/views/community/MyActivity.test.js` 通过，1 个测试文件 / 4 个用例；社区审核相关目标测试 `npm.cmd test -- --run src/__tests__/components/community/PostEditor.test.js src/__tests__/router/routeLoaders.test.js src/__tests__/layouts/AdminLayout.test.js src/__tests__/views/community/MyActivity.test.js` 通过，4 个测试文件 / 15 个用例；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_61_COMMUNITY_CONTENT_MODERATION_FRONTEND.md`、`tasks/TASK_61_COMMUNITY_CONTENT_MODERATION_BACKEND.md`。
- 停止说明：本轮只完成审核提示、管理审核入口和作者状态回显，不继续推进举报系统、批量审核、图片 AI 审核、敏感词配置页或云审核服务。

## 离线 STT 远程源下载改造（2026-05-31）
- 当前阶段：本轮已完成 sherpa-onnx 离线语音识别包从前端静态大文件改为远程候选源下载，并追加修复已知不可用源站风险。Cloudflare Pages 构建产物只保留小体积 `manifest.json` 和 `runtime.js`，浏览器下载完成后继续写入 Cache API。
- 已完成内容：`manifest.json` 增加远程候选源，并已移除当前返回 401/404 的 `hf.qhduan.com` 与 `hf-mirror.com`；现保留 k2-fsa 官方文档指向的 ModelScope Studio 镜像与 HuggingFace 官方源。`offlineVoiceModelCache.js` 支持 `urls` 候选源顺序尝试、命中源持久化和实际 URL 作为 Cache key；`sherpaSpeechWorker.js` 在加载 runtime 前注入命中模型 URL；`runtime.js` 优先按 Worker 注入 URL查 Cache API，缺失时回退同源路径；设置页补充约 300MB、已配置模型源、本站不承载流量和官方源较慢时可切换网络的说明。
- 资源清理：已删除本地忽略的大模型文件 `voice-models-local/sherpa-onnx/zh-cn-streaming/sherpa-onnx-wasm-main-asr.data`、`voice-models-local/sherpa-onnx/zh-cn-streaming/sherpa-onnx-wasm-main-asr.wasm` 和 `.tmp-sherpa-pack/`；Kokoro 本地目录此前已不存在。
- 源站验证：新增并更新 `frontend/tasks/artifacts/offline-stt-remote-source-check.md`。当前 `hf.qhduan.com` 返回 401，`hf-mirror.com` 返回 404，已从生产 manifest 移除；ModelScope Studio 来源于 k2-fsa 官方文档的 HuggingFace 备用镜像说明，但当前工具环境对 ModelScope 静态文件 HEAD 验证超时；HuggingFace 官方 Space 页面可访问并列出目标文件，但本机 Node fetch 到 `huggingface.co` 超时，仍需上线前用真实浏览器 Network 面板做 CORS 与大文件下载人工验证。
- 前端验证：源站修复 RED 阶段确认生产 manifest 仍包含失败镜像且设置页仍使用旧文案；修复后 `npm.cmd run test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/views/SettingsView.test.js` 通过，4 个测试文件 / 72 个用例；`npm.cmd run build` 通过。源站修复前的全量回归 `npm.cmd run test` 通过，72 个测试文件 / 511 个用例。
- 构建产物检查：`dist/voice-models/sherpa-onnx/zh-cn-streaming/` 仅包含 `manifest.json` 和 `runtime.js`；`dist` 中无超过 25MiB 文件，无 `kokoro`、`.data` 或 `.wasm` 模型文件。
- 关联任务文件：`frontend/tasks/TASK_61_OFFLINE_STT_REMOTE_SOURCE_DOWNLOAD_PLAN_FRONTEND.md`。
- 停止说明：本轮只处理离线 STT 远程源下载与本地大模型清理，不继续推进 R2/OSS/COS 托管、后端代理、离线 TTS 或其它语音能力。

## 修复 Chrome 系统 TTS 男声优先降级提示（2026-05-31）
- 当前阶段：本轮已修复 Chrome 中“男声优先”在没有真实中文男声时静默选到明确女声的问题，并在设置中心和语音通话状态中展示实际使用的浏览器 voice。
- 问题原因：Edge 通常能暴露 Microsoft 中文男声/女声 Natural voice；Chrome 经常只暴露通用中文 voice，或没有带男声关键词的中文 voice。旧系统 TTS 评分逻辑在“男声优先”找不到男声时，仍可能按高分选中 `Xiaoxiao` 这类明确女声。
- 已完成内容：`useTextToSpeech.js` 增加 voice 性别识别、同性别优先筛选、中性中文 voice 降级和 `voicePreferenceStatus`；`useHybridTextToSpeech.js` 透传系统 voice 状态；设置页显示当前实际 voice 和缺少中文男/女声提示；语音通话浮层显示播报音色状态。
- 交互边界：本轮不伪造男声、不通过 pitch 变声、不把 Kokoro 占位包当作真实高品质音色。Chrome 没有暴露真实男声时，只能明确降级并提示实际 voice；真实高品质一致音色仍依赖正式 Kokoro 模型资源或浏览器可用的系统男声。
- 前端验证：`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/SettingsView.test.js` 通过，2 个测试文件 / 74 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useOfflineTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，4 个测试文件 / 113 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理浏览器系统 TTS 性别选择降级与状态可见性，不继续推进真实模型托管、后端代理、云端 TTS 或新增更多音色。

## 修复 Kokoro 占位包试听按钮灰置（2026-05-31）
- 当前阶段：本轮已修复设置中心“离线增强”里 Kokoro 占位音色包缓存后试听按钮仍灰置的问题。缓存到的包如果只是占位资源，按钮会保持可点击，点击后明确提示“占位资源”，并使用浏览器系统 TTS 做可听兜底；部署真实 Kokoro tokenizer、voice、ONNX 与 runtime 后仍会走真实离线合成。
- 问题原因：当前仓库内置的 `/voice-models/kokoro/zh-cn-dual/manifest.json` 明确标记 `placeholder: true`，`runtime.js` 也只返回“请部署真实模型”的错误。上一轮为了避免把浏览器 fallback 误认为 Kokoro，直接禁用了占位包试听按钮，导致用户看到“已缓存”但完全无法点试听。
- 已完成内容：`SettingsView.vue` 移除占位包对 Kokoro 试听按钮的 disabled 限制；占位包点击试听时不改用户已保存的系统 voice 偏好，只在试听实例里临时按女声/男声请求浏览器系统 TTS 兜底；`SettingsView.test.js` 用 RED 回归覆盖占位包按钮可点击、出现占位资源提示并触发浏览器 TTS。
- 交互边界：当前高品质包仍只有 manifest 声明的 `zh_female` 与 `zh_male` 两个基础 voice；这来自本轮原始范围“中文男女双音色”。没有真实 Kokoro 资源时，无法凭空产生真实高品质女声/男声，也不会新增假音色选项。
- 前端验证：先确认新增/调整的设置页测试失败于占位试听按钮 disabled；修复后 `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 43 个用例通过。
- 停止说明：本轮只修复占位 Kokoro 包试听入口灰置与提示兜底，不提交真实大模型、不新增后端 TTS、不上传音频、不扩展更多 Kokoro voice。

## 修复 Chrome 默认中文自然音色试听无声（2026-05-31）
- 当前阶段：本轮已完成 Chrome 浏览器系统 TTS 默认中文 voice 选择修复，等待人工在 Chrome 设置中心“默认中文自然音色”中复测试听。
- 问题原因：Edge 能听到是因为它可用的 Microsoft 中文系统/自然 voice 能直接播报；Chrome 在部分环境会暴露远程 `Google 普通话（中国大陆）`，旧评分会把它排在本地 Windows 中文 voice 前面。远程 Google voice 在网络或服务不可用时可能接受 `speak()` 但不出声，表现为 Chrome 试听无声。
- 已完成内容：`useTextToSpeech.js` 调整 voice 评分，默认中文播报仍优先真正的 Xiaoxiao/Yunxi/Natural/Neural/Premium 高质量 voice，但普通远程 Google 中文 voice 不再压过本地中文 voice；`useTextToSpeech.test.js` 补充 Chrome 远程 Google 与本地 Windows 中文 voice 的回归测试。
- 交互边界：本轮不改变设置页 UI、不改 Kokoro 下载/试听、不新增云端 TTS、不改后端；如果 Chrome 完全没有本地中文 voice，仍需要安装系统中文语音包或选择可用的浏览器 voice。
- 前端验证：RED 阶段确认旧逻辑会选择远程 `Google 普通话（中国大陆）`；修复后 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js` 通过，29 个用例；`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，43 个用例；`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js src/__tests__/composables/useVoiceCall.test.js` 通过，56 个用例；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只修复 Chrome 默认中文自然音色试听无声问题，不继续推进真实模型托管、后端代理、云端 TTS 或其它语音能力。

## 修复系统 TTS 与 Kokoro 离线音色偏好互相覆盖（2026-05-31）
- 当前阶段：本轮已完成语音设置中浏览器系统 TTS 偏好与 Kokoro 离线音色偏好的隔离。设置页“AI 播报声音”的默认中文自然音色、女声优先、男声优先和指定浏览器音色试听现在固定走浏览器系统 TTS，不再因为已下载或已选择 Kokoro 而切到离线/回退链路。
- 问题原因：上一轮把 `voicePreferredType` 同时用于系统 voice 偏好和 Kokoro 女声/男声选择，并且下载高品质音色包后自动保存 `offlineTtsEngine: 'kokoro'`。这会覆盖用户原本调好的系统 TTS 男声/女声或自定义 voice，也会让“默认中文自然音色”试听看起来失效。
- 已完成内容：新增本机偏好 `offlineTtsVoiceType` 专门保存 Kokoro 的 `female/male` 选择；Kokoro 下载成功只缓存资源，不再自动接管语音面试播报；点击 Kokoro 女声/男声卡片才启用 `offlineTtsEngine: 'kokoro'`，且不会清空系统自定义 voice；Kokoro 卡片试听只试听离线 voice，不改变当前启用引擎。语音面试页使用 Kokoro 时读取 `offlineTtsVoiceType`，fallback 浏览器 TTS 继续读取原系统 voice 偏好。
- 交互边界：当前 Kokoro manifest 仍只声明 `zh_female` 和 `zh_male` 两个 voice；仓库内仍是占位 tokenizer、voice、ONNX 和 runtime，不提交真实大模型。未部署真实男声资源时，无法获得真实男声音色，页面会继续通过占位包提示避免误判。
- 前端验证：RED 阶段确认目标用例失败于下载自动切换 Kokoro、缺少独立离线音色字段、Kokoro 选择覆盖系统 voice、语音面试离线播报读取错误字段；修复后 `npm.cmd test -- --run src/__tests__/utils/settingsPreferences.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，83 个用例。相关回归 `npm.cmd test -- --run src/__tests__/utils/settingsPreferences.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/composables/useOfflineTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，5 个测试文件 / 106 个用例；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理系统 TTS 与 Kokoro 离线音色偏好隔离，不继续推进真实模型托管、后端代理、云端 TTS 或新增更多音色。

## 修复 Kokoro 离线 TTS 双音色选择与占位包提示（2026-05-31）
- 当前阶段：本轮已在设置中心“离线增强”的高品质离线音色包卡片中补齐 Kokoro 双音色显式选择。`tts:kokoro` 缓存 ready 后会显示“Kokoro 中文女声”和“Kokoro 中文男声”，每个音色都有独立试听按钮。
- 问题原因：上一轮只完成 Kokoro 下载、缓存和播报 fallback 外壳，女声/男声映射仍复用通用播报偏好，离线增强卡片没有直接展示 Kokoro 两个 voice；同时当前仓库提交的是占位资源，下载成功并不代表真实 tokenizer、voice、ONNX 和 runtime 已部署。
- 已完成内容：`offlineVoiceModelCache.js` 保存 manifest 的 `placeholder` 标记，并兼容上一轮已缓存但缺少该字段的 Kokoro 占位状态；设置页缓存 ready 后展示双音色 radiogroup，点击会保存 `offlineTtsEngine: 'kokoro'` 和对应 `voicePreferredType`；占位包状态下显示“已下载占位包，尚未部署真实 Kokoro 模型资源”，并禁用真实离线试听按钮，避免把浏览器 TTS fallback 听感误认为 Kokoro。
- 交互边界：当前 Kokoro manifest 仍只声明 `zh_female` 和 `zh_male` 两个音色；本轮不提交真实模型二进制，不接后端 TTS，不上传音频。要获得真实高品质试听和播报，仍需替换 `/voice-models/kokoro/zh-cn-dual/` 下的 tokenizer、男女声音色、ONNX 和 runtime。
- 前端验证：RED 阶段确认 `placeholder` 未透传、旧缓存占位状态不可识别、设置页没有双音色 UI、占位提示缺失和独立试听方法不存在；修复后 `npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js` 通过，18 个用例；`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，42 个用例；相关回归 `npm.cmd test -- --run src/__tests__/utils/settingsPreferences.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/composables/useOfflineTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，5 个测试文件 / 105 个用例；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理 Kokoro 双音色选择、试听入口和占位包提示，不继续推进真实模型托管、后端代理、云端 TTS 或其它语音能力。

## 修复 Kokoro 离线 TTS 下载缺失 tokenizer 资源（2026-05-31）
- 当前阶段：本轮已修复设置中心下载 Kokoro 离线 TTS 音色包时失败在 `tokenizer.json` 的问题；manifest 中声明的同源本地资源现在都能从前端静态目录返回，不再命中 SPA HTML fallback。
- 问题原因：上一轮只提交 Kokoro manifest 和 runtime 占位文件，但 manifest 还声明了 `tokenizer.json`、男女声音色和 ONNX 文件。下载器按清单逐个请求时，这些文件缺失会返回 HTML，最终提示“离线语音模型文件不是有效模型资源，请确认已部署 tokenizer.json”。
- 已完成内容：补齐 `public/voice-models/kokoro/zh-cn-dual/tokenizer.json`、`voices/zh_female.bin`、`voices/zh_male.bin`、`kokoro-zh-cn.onnx` 的轻量占位资源；`manifest.json` 标记 placeholder；`runtime.js` 改为中文占位错误提示；`offlineVoiceModelCache.test.js` 增加清单本地资源必须存在的回归测试，防止后续再次出现 manifest 指向缺失文件。
- 交互边界：本轮仍不提交真实 Kokoro 大模型二进制；占位资源只保证下载和 Cache API 缓存流程可走通。真实高品质离线播报仍需要把正式 tokenizer、voice、ONNX 和 runtime 替换到同一路径；未替换前播报会走既有失败回退浏览器 TTS。
- 前端验证：RED 阶段 `npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js` 失败并列出缺失 `tokenizer.json`、男女声音色和 ONNX；修复后同命令通过，1 个测试文件 / 17 个用例通过。相关回归 `npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/composables/useOfflineTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，4 个测试文件 / 95 个用例通过；`npm.cmd run build` 通过。
- 构建产物检查：已确认 `dist/voice-models/kokoro/zh-cn-dual/tokenizer.json`、`dist/voice-models/kokoro/zh-cn-dual/voices/zh_female.bin`、`dist/voice-models/kokoro/zh-cn-dual/kokoro-zh-cn.onnx` 存在。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理 Kokoro 下载清单缺失资源问题，不继续推进真实模型托管、后端代理、云端 TTS 或其它语音能力。

## 浏览器本地 Kokoro 离线 TTS 音色包接入（2026-05-31）
- 当前阶段：本轮已完成浏览器本地 Kokoro 离线 TTS 音色包接入外壳，设置中心可下载/缓存/重试/删除 `tts:kokoro`，下载成功后自动切换 `offlineTtsEngine: 'kokoro'`；设置页试听和语音面试播报在 Kokoro ready 且用户选择离线 TTS 时优先走本地合成，异常时回退原浏览器 `speechSynthesis`。
- 已完成内容：新增 `useOfflineTextToSpeech.js` 和 `useHybridTextToSpeech.js`，保留原浏览器 TTS 为 fallback；扩展偏好 `offlineTtsEngine` 为 `system | kokoro`；设置页“高品质离线音色包”支持下载进度、已缓存、失败重试和删除；语音面试开场白与后续分句播报接入 hybrid TTS；新增 `/voice-models/kokoro/zh-cn-dual/manifest.json` 和 runtime 占位文件。
- 交互边界：本轮只处理浏览器本地 Kokoro TTS 缓存和播放链路，不新增后端 TTS 服务、不改后端 SSE、不改 AI 回复生成、不改数据库、不上传用户音频；STT 仍保持 sherpa-onnx；Chrome/Edge 浏览器 TTS voice 选择逻辑继续作为 fallback。
- 模型资源边界：不提交 tokenizer、voice、ONNX 等大体积模型二进制；真实高品质播报需要把模型资源部署到 `/voice-models/kokoro/zh-cn-dual/` 并替换当前 placeholder runtime。当前 placeholder runtime 会明确报错，hybrid 层会回退浏览器 TTS。
- 前端验证：Kokoro 相关目标测试已覆盖设置偏好归一、manifest 缓存、设置页下载/重试/删除、离线 TTS 女声男声/合成失败/串行播放/stop、语音面试 Kokoro 优先和失败回退。目标测试组通过，5 个测试文件 / 100 个用例通过；语音与设置整组回归通过，10 个测试文件 / 195 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮功能到此停止，等待用户部署真实 Kokoro 资源并人工验收 Chrome/Edge 听感一致性，不继续推进云端 TTS、模型托管代理或其它语音能力。
## 修复问号后提前恢复聆听与离线 STT 采集警告（2026-05-30）
- 当前阶段：本轮已完成模拟面试语音通话的 TTS 应用层串行队列修复，并完成离线 sherpa-onnx STT 麦克风采集从 `ScriptProcessorNode` 到 `AudioWorkletNode` 的优先迁移；等待用户在 Chrome 中硬刷新后复测“第一个问号后是否还会进入聆听”以及控制台弃用警告是否消失。
- 问题原因：上次强引用修复没有覆盖用户这次给出的确定样例。当前 TTS 会按句号/问号把流式回复拆成多条 utterance，并连续提交给浏览器自己的 `speechSynthesis` 队列；Chrome 对多 utterance 队列的 pending/结束状态并不稳定，第一段问句结束后可能让前端误以为整段播报完成，`useVoiceCall` 看到 `isSpeaking=false` 后恢复收音，导致后续“它们分别在什么阶段触发？”没有播报。控制台 `ScriptProcessorNode is deprecated` 来自离线 STT 的音频采集链路，不是 TTS 中断原因，但确实应改用 AudioWorklet。
- 已完成内容：`useTextToSpeech.js` 新增前端应用层 `speechQueue`，只允许一个 active utterance 交给浏览器，上一句结束后才播放下一句；`pendingCount` 覆盖待播队列加当前 utterance，整段播完前 `isSpeaking` 保持 true。`useSpeechToText.js` 抽出统一 PCM 投递逻辑，优先加载 `/audio-worklets/offline-stt-processor.js` 并使用 `AudioWorkletNode`，仅在不支持或加载失败时回退 `createScriptProcessor()`。新增 `frontend/app/public/audio-worklets/offline-stt-processor.js`。
- 交互边界：本轮不改变 AI 回复内容、不改后端 SSE、不新增云端/离线 TTS、不接入 Kokoro、不上传音频、不替换 sherpa-onnx 模型；离线 sherpa-onnx 仍只负责 STT，面试官播报仍依赖浏览器 `speechSynthesis`。
- 前端验证：新增 RED 用例并确认旧实现失败于用户原句被一次性提交 3 条浏览器 utterance；修复后 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js` 通过，1 个测试文件 / 29 个用例通过。新增 AudioWorklet RED 用例并确认旧实现未设置 worklet 消息处理；修复后 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js` 通过，1 个测试文件 / 40 个用例通过。语音与设置整组回归通过，9 个测试文件 / 185 个用例通过；`npm.cmd run build` 通过，构建产物包含 `dist/audio-worklets/offline-stt-processor.js`。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理语音面试播报提前进入聆听和离线 STT 采集弃用 API 警告，不继续推进模型托管、后端代理或新的语音能力。

## 修复 Chrome 播报未念完就进入聆听状态（2026-05-30）
- 当前阶段：本轮已完成浏览器 TTS 播放中 utterance 强引用修复，等待用户在 Chrome/Edge 中硬刷新后复测语音面试开场白和后续 AI 回复是否还会念到一半就进入“正在聆听”。
- 问题原因：旧 `useTextToSpeech.js` 只把 `SpeechSynthesisUtterance` 放进 `WeakMap/WeakSet` 做元数据和去重，业务层没有强引用。Chrome 的 `speechSynthesis` 队列对 utterance 的引用不够稳定时，播放对象可能被垃圾回收或提前结束，导致前端收到结束状态、释放 `isSpeaking`，语音通话层随即恢复收音，因此表现为面试官没念完台词就进入聆听。该问题和语音台词长短、浏览器调度、GC 时机有关，所以具有偶发性。
- 已完成内容：`useTextToSpeech.js` 新增播放中 utterance 的本地 `Set` 强引用集合，创建 utterance 后加入集合，统一结束入口 `markUtteranceEnd()` 以及 `stop()`/替换播报清理时释放集合，避免播放对象在浏览器完成前失去 JS 强引用。`useTextToSpeech.test.js` 新增 RED 回归测试，验证播放期间 `activeUtteranceCount` 为 1，`onend` 后释放为 0。
- 交互边界：本轮只修改浏览器 TTS 前端生命周期管理；不新增后端接口、不改数据库、不上传音频、不新增离线 TTS、不接入 Kokoro。离线 sherpa-onnx 仍只负责 STT，面试官播报仍依赖浏览器 `speechSynthesis`。
- 前端验证：先新增 RED 用例并确认旧实现失败于组合函数没有可验证的播放中强引用；修复后 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js` 通过，1 个测试文件 / 28 个用例通过；语音与设置整组回归通过，9 个测试文件 / 187 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的“面试官还没念完就中断并进入聆听”问题，不继续推进云端 TTS、模型托管、后端代理或新的语音能力。

## 修复离线语音引擎已缓存后仍显示可下载（2026-05-30）
- 当前阶段：本轮已完成设置中心离线 sherpa-onnx 资源包重复下载状态修复，等待用户在已下载离线资源包的 Chrome/Edge 中硬刷新后复测按钮是否显示“已缓存离线语音引擎”，以及删除资源包后是否释放当前浏览器缓存状态。
- 问题原因：项目静态资源目录已经有模型文件，只代表 `/voice-models/sherpa-onnx/zh-cn-streaming/` 可提供资源；设置页下载按钮的职责是把这些资源写入当前浏览器 Cache API。旧下载函数没有在 ready 且 Cache API 文件齐全时短路，因此当前浏览器已缓存后仍可能再次进入下载流程。更严重的是，ready 后如果重复下载时 manifest 临时失败，旧失败分支会把 `files` 写成空数组，导致后续删除入口缺少旧缓存 URL，可能无法清理之前的 Cache API 文件。
- 已完成内容：`offlineVoiceModelCache.js` 在下载前先用 `isModelCached()` 校验当前浏览器缓存完整性，ready 且完整时直接返回现有状态，不再请求 manifest 或模型文件；manifest 失败时保留旧 `files/runtime/version`，确保失败残留仍能删除；删除资源包时若遇到旧坏状态 `files` 为空，则按 manifest 所在目录前缀扫描当前模型 Cache 并删除残留条目。`SettingsView.vue` 将下载按钮文案改为随状态变化：未下载显示“下载离线语音引擎”，下载中显示进度，已缓存显示“已缓存离线语音引擎”，失败显示“重新下载离线语音引擎”。
- 交互边界：静态资源已部署不等于浏览器已缓存；首次使用某个浏览器/用户配置仍需要点击一次下载写入该浏览器 Cache API。已缓存且完整时不再重复下载；缓存不完整或失败状态仍允许重新下载；删除资源包只删除当前浏览器 Cache API 与本地状态，不会删除项目目录或服务器静态文件。旧版本已经写成 `failed + files: []` 的状态，也会在删除时按模型目录前缀清理残留缓存。
- 前端验证：新增 RED 回归测试并确认旧实现失败于已 ready 仍调用 `fetch(manifest)`、失败重试清空旧 `files`、旧坏状态 `files` 为空时删除不会清理 Cache API 残留；修复后 `npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js` 通过，1 个测试文件 / 15 个用例通过；`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 37 个用例通过；语音与设置整组回归通过，9 个测试文件 / 186 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理离线 STT 资源包重复下载状态和失败后删除能力，不继续推进离线 TTS、Kokoro、后端代理、模型托管或新的语音能力。

## 修复 Chrome 播报中途被 watchdog 误取消（2026-05-30）
- 当前阶段：本轮已完成 Chrome TTS 已开始播报后的 watchdog 策略修正，等待用户在 Chrome 中硬刷新后复测设置页试听、开场白和后续 AI 回复是否仍会念到一半中断。
- 问题原因：旧的单条 utterance watchdog 用 `max(12000ms, 文本长度 * 450ms)` 估算最大播报时间，即使 Chrome 已触发 `onstart` 且仍报告 `speaking/pending`，到点也会主动 `cancel()`。Chrome 某些中文音色、较慢语速或合成调度可能超过该估算，导致正常播报被误判为卡死，表现为文字没播完就暂停或中断。
- 已完成内容：`useTextToSpeech.js` 为已开始的 utterance 记录 `startedAt`，首个短 watchdog 到点时如果浏览器仍处于 speaking/pending，则续期等待，不再立即取消；达到保守硬超时后仍保留 `cancel()` 兜底，防止 Chrome 永久不触发 `onend/onerror` 时页面卡住。`useTextToSpeech.test.js` 新增 RED 回归用例覆盖“已 onstart 且 still speaking 时首个 watchdog 不得取消”，并调整旧预期为超过保守硬超时才释放。
- Chrome/Edge 音色边界：当前 TTS 仍依赖浏览器 `speechSynthesis`。Edge 和 Chrome 暴露的 voice 列表、默认 voice 和远程/本地策略不同，Chrome 如果没有返回 Edge 可用的自然中文音色，前端无法直接调用 Edge 专属音色；离线 sherpa-onnx 只负责 STT 识别，不负责面试官播报 TTS。
- 交互边界：本轮只改浏览器 TTS 前端状态机和 watchdog，不新增后端接口、不改数据库、不上传音频、不新增离线 TTS、不接入 Kokoro。启动 watchdog 仍用于处理完全未触发 `onstart` 的 Chrome 无声场景，保守硬超时仍用于释放真正卡死的播报状态。
- 前端验证：先新增 RED 用例并确认旧实现 13 秒时会调用 `speechSynthesis.cancel()`；修复后 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js` 通过，1 个测试文件 / 27 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/SettingsView.test.js` 通过，4 个测试文件 / 117 个用例通过；语音与设置整组回归通过，9 个测试文件 / 182 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的 Chrome 播报中途被截断问题和音色差异说明，不继续推进云端 TTS、模型托管、后端代理或新的语音能力。

## 修复 Chrome 先返回旧式机械音色导致试听仍不自然（2026-05-30）
- 当前阶段：本轮已完成 Chrome TTS voices 分批加载场景下的音色等待修复，等待用户在 Chrome 中硬刷新后复测设置页试听、开场白和后续 AI 回复是否能优先使用 Chrome 暴露的自然/Google 中文音色，而不是过早使用 `Microsoft Huihui Desktop` 等旧式机械音色。
- 问题原因：上一轮已处理 voices 为空时过早裸播的问题，但 Chrome 可能在首次用户手势后先返回 Windows 旧式本地 voice，例如 `Microsoft Huihui Desktop`，随后才通过 `voiceschanged` 补齐 `Google 普通话（中国大陆）` 或其它自然音色。旧逻辑只要 voices 非空就立刻播报，导致程序没有机会等到更好的 voice。
- 已完成内容：`useTextToSpeech.js` 增加旧式系统 voice 识别和更优 voice 等待逻辑；在用户手势触发的试听或开场白播报中，如果当前选中的 voice 属于 Huihui/Desktop/Zira/David 等旧式机械系统 voice，先复用 800ms 有界等待窗口等待非旧式的自然/Google/Neural/Premium voice，等不到再回退当前旧式 voice，避免完全不播。`useTextToSpeech.test.js` 新增 Chrome 分批返回 voices 的 RED 回归测试，覆盖先 Huihui 后 Google 应选择 Google，以及超时仍回退 Huihui 的边界。
- 交互边界：本轮只修改浏览器 TTS 前端音色等待和评分辅助判断；不新增后端接口、不改数据库、不上传音频、不新增离线 TTS、不接入 Kokoro。离线 sherpa-onnx 仍只负责 STT，面试官播报和设置页试听仍依赖浏览器 `speechSynthesis`。
- 前端验证：新增 RED 用例并确认旧实现失败于 `Microsoft Huihui Desktop` 被立即传给 `speechSynthesis.speak()`；修复后 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js` 通过，1 个测试文件 / 26 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 94 个用例通过；语音与设置整组回归通过，9 个测试文件 / 181 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的 Chrome 仍使用系统默认机械音色问题，不继续推进云端 TTS、模型托管、后端代理或新的语音能力。

## 修复设置页试听未预热导致 Chrome 音色机械（2026-05-30）
- 当前阶段：本轮已完成设置中心面试偏好语音试听的 Chrome TTS 预热修复，等待用户在 Chrome 中硬刷新后进入设置页试听，确认试听是否能优先使用程序默认自然中文音色。
- 问题原因：语音面试开始通话链路已在点击手势内调用 `prepareForUserGesture()`，但设置页试听按钮原本只直接调用 `previewTextToSpeech.speak()`。Chrome 的 `speechSynthesis.getVoices()` 常常需要用户手势内唤醒后才稳定返回；缺少预热时，试听更容易在 voices 为空或不完整时创建 utterance，最终落到系统默认机械音色。
- 已完成内容：`SettingsView.vue` 在 `handleVoicePreview()` 中先同步语速、音调、音量和音色偏好，再调用 `previewTextToSpeech.prepareForUserGesture?.()`，最后执行试听播报。`SettingsView.test.js` 补充浏览器 TTS mock 与 Chrome 风格回归测试，覆盖只有预热后 `getVoices()` 才返回默认自然音色的场景。
- 交互边界：本轮只修改设置页试听按钮的 TTS 预热时序；不新增后端接口、不改数据库、不上传音频、不新增离线 TTS、不接入 Kokoro。离线 sherpa-onnx 仍只负责 STT，试听和面试官播报仍依赖浏览器 `speechSynthesis`。
- 前端验证：新增 RED 用例并确认旧实现失败于试听点击后未唤醒 `speechSynthesis`、Chrome 风格 mock 无法返回默认自然音色；修复后 `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 36 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 91 个用例通过；语音与设置整组回归通过，9 个测试文件 / 178 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的设置页试听在 Chrome 中更容易使用系统默认机械音色问题，不继续推进云端 TTS、模型托管、后端代理或新的语音能力。
## 修复 Chrome 预热后默认自然音色丢失（2026-05-30）
- 当前阶段：本轮已完成 Chrome TTS 用户手势预热后的默认音色等待修复，等待用户在 Chrome 中硬刷新后复测开场白和后续 AI 回复是否优先使用程序配置的自然中文音色，而不是直接落到系统默认机械音色。
- 问题原因：`prepareForUserGesture()` 原本用于把 TTS 唤醒放进用户点击手势内，但旧 `enqueue()` 看到 `userGesturePrepared` 或开场白传入的 `allowDefaultVoice: true` 后，会跳过 voices 等待并立即创建 utterance。Chrome 在首次点击后经常仍未填充 `speechSynthesis.getVoices()`，此时 `selectedVoice` 为空，utterance 没有 `voice`，浏览器只能使用系统默认 TTS 音色。
- 已完成内容：`useTextToSpeech.js` 将用户手势预热和系统默认音色兜底改为有界等待策略：voices 为空时先走 800ms `waitForVoicesReady()`，窗口内拿到 voices 就按程序默认评分选择自然中文音色，超时仍为空才使用系统默认音色兜底。`useTextToSpeech.test.js` 补充 Chrome voices 延迟、`allowDefaultVoice` 开场白仍应优先等待默认音色、voices 永不返回时兜底的回归测试；`InterviewSessionView.test.js` 同步更新开场白 voices 为空时的时序预期。
- 交互边界：本轮只修改浏览器 TTS 前端音色等待时序；不新增后端接口、不改数据库、不上传音频、不新增离线 TTS、不接入 Kokoro。离线 sherpa-onnx 仍只负责 STT，面试官播报仍依赖浏览器 `speechSynthesis`。
- 前端验证：新增 RED 用例并确认旧实现失败于点击手势后立即 `speak()` 且 utterance 没有 voice；补充 `allowDefaultVoice: true` 场景后确认旧逻辑仍会立即裸播系统默认音色。修复后 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js` 通过，1 个测试文件 / 23 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 77 个用例通过；语音与设置整组回归通过，9 个测试文件 / 177 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的 Chrome 默认音色没有生效、落到系统默认机械音色问题，不继续推进云端 TTS、模型托管、后端代理或新的语音能力。
## 管理端通知公告筛选查看（2026-05-30）
- 当前阶段：本轮已完成管理端通知公告筛选查看功能，等待人工在 `/admin/notifications` 复测关键词、类型、状态和目标用户筛选组合。
- 已完成内容：`AdminNotificationView.vue` 在表格上方新增筛选区，支持按标题/内容关键词、公告类型、发布状态和目标用户筛选；筛选变更回到第一页并重新请求后端；重置筛选清空条件并恢复全量列表；`notifications.js` 统一剔除空查询参数，避免无意义筛选值传给后端。
- 交互边界：本轮只处理通知公告列表筛选查看；不新增公告编辑、导出、时间范围筛选、筛选条件持久化或其它管理端页面能力。
- 前端验证：新增 RED 用例并确认旧实现失败于 API 未清理空筛选参数、页面缺少 `filterForm`；修复后 `npm.cmd test -- --run src/__tests__/api/admin.notifications.test.js src/__tests__/views/AdminNotificationView.test.js` 通过，2 个测试文件 / 11 个用例通过；`npm.cmd run build` 通过。
- 后端验证：`mvn.cmd -q "-Dtest=AdminNotificationControllerTest" test` 通过；`mvn.cmd -q -DskipTests compile` 通过。
- 关联任务文件：`frontend/tasks/TASK_ADMIN_NOTIFICATION_FILTER_FRONTEND.md`；后端任务文件：`tasks/TASK_ADMIN_NOTIFICATION_FILTER_BACKEND.md`。
- 停止说明：本轮仅完成用户指定的管理端通知公告筛选查看功能，不继续推进其它管理端功能。
## 修复 Chrome 语音播报看似开始但全程无声（2026-05-30）
- 当前阶段：本轮已完成 Chrome TTS “已 onstart / speaking 但无声音且无结束回调”的补强修复，等待用户在 Chrome 中硬刷新后复测开场白和后续 AI 回复是否能出声，以及无声时页面是否能退出“AI 正在回复/AI 正在说话”并恢复收音。
- 问题原因：上一轮只覆盖 Chrome 接受 utterance 但从未 `onstart` 的分支；真实 Chrome 还可能触发 `onstart` 并持续报告 `speechSynthesis.speaking/pending`，但音频链路实际无声且不触发 `onend/onerror`。旧 watchdog 因为看到 `speaking/pending` 会无限续期，导致通话层一直认为 AI 正在播报。Chrome 还可能默认选择远程/浏览器音色，这类音色比 Windows 本地系统音色更容易受网络、语言包或浏览器策略影响而静音。
- 已完成内容：`useTextToSpeech.js` 为每条 utterance 保存开始/结束事件上下文，支持 per-speak `onStart/onEnd` 回调；默认自然中文音色评分更偏向本地系统音色，降低远程浏览器音色优先级；每条 utterance 的结束 watchdog 到达按文本长度估算的最大播报时长后会主动 `cancel()` 并释放 `isSpeaking`，不再因 Chrome 持续报告 `speaking/pending` 而无限续期。`useTextToSpeech.test.js` 增加 Chrome 已开始但永不结束、默认优先本地中文音色和事件详情回调的回归测试。
- 交互边界：本轮只修改浏览器 TTS 前端状态机和音色选择评分；不新增后端接口、不改数据库、不上传音频、不新增离线 TTS、不接入 Kokoro。离线 sherpa-onnx 仍只负责 STT，面试官播报仍依赖浏览器 `speechSynthesis`。
- 前端验证：新增 RED 用例并确认旧实现失败于远程音色优先、`onstart` 后不结束时不释放状态、事件详情回调缺失；修复后 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js` 通过，1 个测试文件 / 20 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 74 个用例通过；离线 STT 相关回归通过，5 个测试文件 / 65 个用例通过；语音与设置整组回归通过，9 个测试文件 / 174 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的 Chrome 任意播报都可能无声且卡住通话状态问题，不继续推进云端 TTS、模型托管、后端代理或新的语音能力。
## 修复开场白播报偶发不稳定（2026-05-30）
- 当前阶段：本轮已完成语音面试开场白 TTS 启动确认与有限补播修复，等待用户在 Chrome 中硬刷新后复测新建语音面试、点击开始通话、首轮开场白是否稳定发声，以及浏览器吞掉首个 utterance 时是否自动补播一次。
- 问题原因：上一轮能释放 Chrome 吞掉 utterance 后的“AI 正在回复”状态，但页面在调用 `textToSpeech.speak()` 前就把 `openingSpeechPlayed` 标为 true。Chrome 偶发接受 `speechSynthesis.speak()` 却不触发真实 `onstart` 时，TTS watchdog 会结束播报状态，页面却不会再补播开场白，所以用户看到“有时候不播”。旧 utterance 被取消后若迟到触发回调，还可能误清当前播报状态。
- 已完成内容：`useTextToSpeech.js` 为单次 utterance 增加 `onStart/onEnd` 事件详情、`requireStartEvent` 启动确认、`runId` 过期回调防护，并优先选择本地中文 TTS 音色；`InterviewSessionView.vue` 只有收到 `onstart` 才确认开场白已播，`start-timeout` 时最多补播一次，避免无限循环。补充 `useTextToSpeech.test.js` 和 `InterviewSessionView.test.js` 回归测试。
- 交互边界：本轮只修改前端浏览器 TTS 状态机和开场白触发判断；不新增后端接口、不改数据库、不上传音频、不新增离线 TTS、不接入 Kokoro。离线 sherpa-onnx 仍只负责 STT，面试官播报仍依赖浏览器 `speechSynthesis`。
- 前端验证：先新增 RED 回归测试并确认旧代码失败于 TTS 无单次启动/结束事件、Chrome 吞掉开场白后不重试、旧 utterance 迟到回调污染当前播报；修复后 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，2 个测试文件 / 53 个用例通过；语音与设置整组回归通过，9 个测试文件 / 175 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的开场白播报偶发不稳定问题，不继续推进云端 TTS、模型托管、后端代理或新的语音能力。

## 修复 Chrome 开场白无声音且页面卡在 AI 正在回复（2026-05-30）
- 当前阶段：本轮已完成 Chrome 首轮 TTS 启动与卡死释放修复，等待用户在 Chrome 中硬刷新后复测点击开始通话、开场白播报、播报失败时页面是否能退出“AI 正在回复”并继续收音。
- 问题原因：TTS 预热已经处于点击手势内，但旧 `speak()` 会先无条件 `stop()`，即使当前没有任何播报也会调用 `speechSynthesis.cancel()`，随后马上 `speak()`。Chrome 对首轮 `cancel()/speak()` 同步序列更敏感，可能接受 utterance 但不实际发声，也不触发 `onend/onerror`；旧 watchdog 在浏览器持续报告 `speaking/pending` 时会反复续期，页面就长期停在“AI 正在回复”。
- 已完成内容：`useTextToSpeech.js` 拆分本地状态清理和浏览器队列取消，首个空闲播报不再先 `cancel()`；有活跃播报、排队语音或缓冲内容时仍会取消旧语音，避免串音。新增 utterance `onstart` 记录和 5 秒启动 watchdog，Chrome 吞掉首个 utterance 且无任何回调时主动 `cancel()` 并释放 `isSpeaking`。`useTextToSpeech.test.js` 增加两个回归测试锁定该行为。
- 交互边界：本轮只修改浏览器 TTS 前端状态机；不新增后端接口、不改数据库、不上传音频、不新增离线 TTS、不接入 Kokoro。离线 sherpa-onnx 仍只负责 STT，面试官播报仍依赖浏览器 `speechSynthesis`。
- 前端验证：新增 RED 用例并确认旧实现失败于首轮空闲播报前调用 `cancel()`、Chrome 吞掉 utterance 后不释放 `isSpeaking`；修复后 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js` 通过，1 个测试文件 / 16 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 69 个用例通过；语音与设置整组回归通过，9 个测试文件 / 169 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的 Chrome 开场白无声音且页面卡在 AI 正在回复问题，不继续推进云端 TTS、模型托管、后端代理或新的语音能力。
## 修复离线引擎已就绪文案闪烁与面试准备耗时（2026-05-30）
- 当前阶段：本轮已完成离线 sherpa-onnx 后台预热状态稳定、开场白 pending 快速轮询和 TTS 播报后恢复收音等待优化，等待用户在已下载离线资源包的真实浏览器中复测“已就绪”文案是否还闪烁、开场白生成后是否更快进入播报/收音。
- 问题原因：离线模型资源 ready 后，后台预热 Worker 的旧实现仍会把用户可见引擎状态短暂切到 `offline-loading`，造成“已就绪文字消失一下又出现”的视觉闪烁。语音面试准备时间偏长还来自两个串行等待：开场白 pending 旧轮询首次固定等 3 秒，TTS 播报结束后固定等 1.5 秒才恢复收音。
- 已完成内容：`useSpeechToText.js` 在 `prepareOfflineRecognition()` 后台预热阶段保持 `offline-ready`，不再让已缓存离线引擎文案闪成 loading；`InterviewSessionView.vue` 将开场白 pending 的前 6 轮轮询改为 500ms 快速探测，之后恢复 3 秒常规轮询，总超时不变；`useVoiceCall.js` 将 TTS 播报结束后的尾音保护从 1.5 秒收敛到 0.8 秒，继续保留防止扬声器尾音被麦克风拾取的保护。
- 交互边界：本轮只优化前端状态展示和启动等待节奏；不新增后端接口、不改数据库、不上传用户音频、不替换模型包、不推进 Kokoro、云端 STT/TTS 或新的语音能力。
- 前端验证：新增 RED 回归测试并确认旧代码失败于预热状态闪烁、0.8 秒未恢复收音、开场白 pending 500ms 未快速查询；修复后 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 92 个用例通过；语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 169 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的“离线已就绪文案短暂消失”和“面试准备偏长”问题，不继续推进下一项语音能力。

## 修复 Chrome 下载离线引擎后仍走浏览器/系统识别（2026-05-30）
- 当前阶段：本轮已完成离线 STT 下载成功后的识别偏好联动修复，等待用户在 Chrome 中重新下载/确认离线资源包后复测会话页是否显示并使用“离线 sherpa-onnx”。
- 问题原因：离线模型 ready 只代表资源已缓存；旧设置页没有同步把 `voiceRecognitionEngine` 从默认 `system_local` 切到 `offline_sherpa`，而会话页只根据该偏好决定是否传入 `preferOffline: true`。因此 Chrome 即使已下载离线包，仍会继续走 Web Speech 的浏览器/系统识别路径。Chrome 浏览器/系统识别还受 Web Speech 服务、系统语言包、网络、麦克风权限和浏览器策略影响；这和 sherpa-onnx 离线 STT 缓存是两套能力。面试官播报仍使用浏览器 `speechSynthesis`，离线 STT 不提供离线 TTS。
- 已完成内容：`SettingsView.vue` 在 `downloadModelFromManifest()` 成功后复用现有本地偏好保存链路，写入 `voiceRecognitionEngine: 'offline_sherpa'` 与 `offlineSttEngine: 'sherpa_onnx'`，成功提示明确说明后续语音面试将优先使用离线引擎。`SettingsView.test.js` 增加从 `system_local` 下载成功后自动切换离线偏好的回归测试。
- 交互边界：本轮只修改设置页下载成功后的本地偏好联动；不改后端、不改数据库、不上传用户音频、不替换模型包、不新增云端 STT/TTS 或 Kokoro。
- 前端验证：先新增 RED 回归测试并确认旧代码失败于下载成功后偏好仍为 `system_local`；修复后 `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 35 个用例通过；相关偏好与会话页回归 `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/utils/settingsPreferences.test.js` 通过，3 个测试文件 / 70 个用例通过；语音与设置整组回归通过，9 个测试文件 / 164 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的“下载离线引擎后仍显示浏览器/系统识别服务”问题，不继续推进下一项语音能力。
## 修复离线 sherpa-onnx 收音前准备时间过长（2026-05-30）
- 当前阶段：本轮已完成离线 sherpa-onnx Worker/模型复用与语音面试后台预热修复，等待用户在已下载离线资源包的浏览器中复测首次开始通话、AI 播报后恢复收音、多轮回答自动提交时的准备时长。
- 问题原因：旧离线 STT 在每轮 `stop()` 后都会销毁 Worker，下一轮收音重新加载 runtime、WASM、data 和 ONNX 模型；因此即使页面文字显示“离线 sherpa-onnx 已就绪”，真正开始收音前仍会反复等待模型初始化。runtime 还存在 stop 后对 stream 调用 `inputFinished()`，下一轮复用同一个 stream 可能继承结束状态的风险。
- 已完成内容：`useSpeechToText.js` 将正常 stop 改为只释放麦克风和 AudioContext，不销毁已 ready 的离线 Worker；后续收音在 runtime URL 未变化时复用同一个 Worker 并只发送 `start`。新增 `prepareOfflineRecognition()`，只预热 Worker/模型、不申请麦克风。`InterviewSessionView.vue` 仅对语音面试的离线 STT 实例开启后台预热，普通文本输入麦克风不预热。两个 sherpa runtime 在每轮 `start()` 时创建新的 online stream，避免上一轮 `inputFinished()` 状态污染下一轮。
- 交互边界：本轮只修改前端离线 STT Worker 生命周期、语音面试预热和 runtime stream 生命周期；不新增后端接口、不改数据库、不上传用户音频、不提交或替换大体积模型文件。
- 前端验证：新增 RED 回归测试并确认旧代码失败于 stop 后销毁 Worker、下一轮重新初始化模型、无法后台预热离线 Worker、runtime 未在每轮 start 创建新 stream；修复后 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 163 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的离线 sherpa-onnx 已下载但收音前反复准备/等待问题，不继续推进模型托管、后端代理、Kokoro 或新的语音能力。

## 加载图标误显示通知铃铛修复（2026-05-30）
- 当前阶段：本轮已完成通用 `FeatureIcon name="loading"` 加载图标兜底修复，等待人工在成长中心、模拟面试会话加载态以及通知/社区相关加载态中复测视觉图标。
- 问题原因：`FeatureIcon` 对非关键图标采用异步加载，`loading` 没有加入同步关键图标清单；首次渲染时 `getFeatureIconSource('loading')` 会先回退到 `system-notifications`，因此加载态显示成通知铃铛。
- 已完成内容：`featureIcons.js` 将 `loading` 加入关键同步图标清单、PNG fallback eager glob 和 WebP eager glob，并从异步 glob 排除列表中排除，保证 `loading.png/loading.webp` 首次渲染即可命中。`featureIcons.test.js` 和 `FeatureIcon.test.js` 增加回归测试，禁止 `loading` 再回退到 `system-notifications`。
- 影响范围：统一修复成长中心、模拟面试会话、通知中心、社区帖子详情、我的动态等所有直接使用 `FeatureIcon name="loading"` 的加载态；简历诊断过程和面试报告生成中的 `ai-loading` 不属于本轮改动范围。
- 交互边界：本轮只修改通用图标资源映射和对应测试，不改后端接口、不改数据库、不重构页面、不替换图标资产。
- 前端验证：先新增 RED 回归测试并确认旧代码失败于 `loading` 同步解析为 `system-notifications.png/webp`；修复后 `npm.cmd test -- --run src/__tests__/utils/featureIcons.test.js src/__tests__/components/common/FeatureIcon.test.js` 通过，2 个测试文件 / 12 个用例通过；相关页面回归 `npm.cmd test -- --run src/__tests__/views/GrowthCenterView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/NotificationView.test.js src/__tests__/views/community/PostDetailView.test.js src/__tests__/views/community/MyActivity.test.js src/__tests__/utils/featureIcons.test.js src/__tests__/components/common/FeatureIcon.test.js` 通过，7 个测试文件 / 51 个用例通过；`npm.cmd run build` 通过，构建产物包含 `loading-*.webp` 与 `loading-*.png`。
- 关联任务文件：`frontend/tasks/TASK_LOADING_ICON_NOTIFICATION_FALLBACK_FIX_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的加载图标误显示通知铃铛问题，不继续推进页面 UI 重构、图标资产替换或其它加载态视觉改造。

## 修复离线 sherpa-onnx 模型已启动但仍不出字（2026-05-30）
- 当前阶段：本轮已完成离线 sherpa-onnx 在 WASM/模型初始化成功后仍长时间“正在聆听”但不出字的三次修复，等待用户在浏览器硬刷新后复测真实麦克风识别。
- 问题原因：用户提供的 `GetOnlineRecognizerConfig`、zipformer 和 `model_type=zipformer` 日志证明模型已加载成功；剩余问题在前端运行链路。runtime URL 只按模型包版本缓存，可能继续加载旧适配层；离线 PCM 只送 Worker，没有直接更新 `voiceActivityAt`，通话层可能永远不触发静音 flush；Worker `stop()` 无文本时不回确认，主线程只能等短超时。`LanguageDetector`、`ScriptProcessorNode deprecated` 和 `VM7 ... debug=True` 初始化日志不是识别失败根因。
- 已完成内容：`useSpeechToText.js` 将 runtime 缓存参数改为“模型包版本 + 前端适配层版本”，离线 PCM 帧直接计算 RMS 并更新语音活动时间，stop flush 超时延长到 5 秒并处理 Worker `stopped` 确认；`sherpaSpeechWorker.js` 在 stop 无文本时回 `{ type: 'stopped' }`；两个 runtime 默认传入 `debug: 0` 的 sherpa 在线识别配置，减少控制台 C++ 初始化日志干扰。
- 交互边界：本轮只修改前端离线 STT 主链路、Worker stop 确认和 runtime 适配层；不新增后端接口、不改数据库、不上传用户音频、不提交或替换大体积模型文件。
- 前端验证：新增 RED 回归测试并确认旧代码失败于 runtime 只带旧模型版本、PCM 帧不更新语音活动、Worker stop 无文本不确认；修复后 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 157 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的离线 sherpa-onnx 已加载但不识别/不出字问题，不继续推进模型托管、后端代理、Kokoro 或新的语音能力。
## Element Plus 服务弹窗样式修复（2026-05-30）
- 当前阶段：本轮已完成 `ElMessage` / `ElMessageBox` 服务组件样式缺失修复，等待人工在浏览器中复测全局错误提示、限流提示、删除确认、退出/登录过期提示等浮层展示。
- 问题原因：前端入口移除了全量 `element-plus/dist/index.css` 后，模板中的 Element Plus 组件可由 `unplugin-vue-components` 按需补样式，但项目中大量 `ElMessage`、`ElMessageBox` 通过 JS 服务方式手动导入调用，不会稳定触发模板解析器补齐服务样式，导致浮层节点挂载到 `body` 后以裸 HTML 形态显示。
- 已完成内容：`main.js` 显式导入 `element-plus/es/components/message/style/css` 和 `element-plus/es/components/message-box/style/css`，保留其它 Element Plus 组件按需加载；新增 `elementPlusServiceStyles.test.js` 锁定入口必须保留服务样式导入，避免后续误删；同步排查 `ElNotification`、JS 服务 `ElLoading`、`v-loading` 指令和手动 `ElPagination` 导入，未发现需要继续补样式的同类服务弹窗入口。
- 交互边界：本轮只修复 JS 服务浮层样式，不替换 Element Plus、不恢复全量 CSS、不调整业务弹窗结构、不改全局错误提示文案或请求拦截逻辑。
- 前端验证：`npm.cmd test -- --run src/__tests__/elementPlusServiceStyles.test.js` 通过，1 个测试文件 / 2 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_ELEMENT_PLUS_SERVICE_STYLE_FIX_FRONTEND.md`。
- 停止说明：本轮只处理当前弹窗/提示裸样式问题，不继续扩展其它 UI 修复。

## 模拟面试流式限流提醒与语音通话保留（2026-05-30）
- 当前阶段：本轮已完成流式面试 429 限流提示和语音通话状态保留修复，等待人工在语音面试中复测高频发送触发限流后的提示与继续通话体验。
- 问题原因：流式面试接口使用裸 `fetch`，旧逻辑只展示后端通用错误文案；语音面试 catch 分支会对所有非 Abort 错误执行 `voiceCall.endVoiceCall()`，导致限流这类可恢复错误也直接挂断通话。
- 已完成内容：`InterviewSessionView.vue` 新增 429 专用限流错误标记和文案“发送太频繁，请稍后继续。10 分钟内最多 60 轮对话。”；429 走 `ElMessage.warning`，且语音面试不再因限流错误结束通话。`InterviewSessionView.test.js` 增加回归测试，覆盖 429 后语音通话保持激活并展示明确提醒。
- 交互边界：本轮只修改流式消息 429 错误提示和语音通话状态；不新增页面、不改数据库、不改语音识别/TTS 主链路、不扩展动态限流配置。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，1 个测试文件 / 29 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_INTERVIEW_STREAM_RATE_LIMIT_NOTICE_FRONTEND.md`。
- 停止说明：本轮只处理限流提示和语音通话保留问题，不继续推进倒计时恢复、会员分层提示或全局错误提示改造。

## 修复 Chrome 语音通话首轮无播报且无法收音（2026-05-30）
- 当前阶段：本轮已完成 Chrome 下语音通话首轮 TTS 播报和离线 sherpa-onnx STT 收音启动时序修复，等待人工在 Chrome 中复测新建语音面试、点击开始通话、听到开场白、开场白结束后授权/恢复收音、说话后自动识别与提交。
- 问题原因：Edge 对 Web Speech / Web Audio 的首次启动更宽松，而 Chrome 更依赖用户点击手势。旧 TTS 在 `speechSynthesis.getVoices()` 为空时等待 `voiceschanged`，开场白没有立即进入 `speak()`；语音通话又会因为 TTS 播报态等待而暂不开麦，表现为“听不到面试官，也无法识别”。离线 sherpa-onnx 路径旧代码等麦克风授权完成后才创建并 `resume()` 16k `AudioContext`，在 Chrome 中可能错过点击手势窗口，导致离线模型 ready 但 PCM 输入不稳定。
- 已完成内容：`useTextToSpeech.js` 新增 `prepareForUserGesture()`，开始通话按钮先唤醒 `speechSynthesis.resume()`，并允许本次播报在 voices 尚未加载时直接使用系统默认音色；`InterviewSessionView.vue` 在 `handleStartVoiceCall()` 中接入该预热。`useSpeechToText.js` 将离线 STT 的 16k `AudioContext` 创建和 suspended `resume()` 前移到 `getUserMedia` 授权前，再复用该上下文连接 analyser / ScriptProcessor 和 Worker 音频流。
- 交互边界：本轮只修改前端语音启动时序；不新增后端接口、不改数据库、不上传用户音频、不新增云端 STT/TTS、不替换或提交大体积离线模型文件。
- 前端验证：先新增 RED 回归测试并确认旧代码失败于 Chrome voices 为空时不立即播报、会话页未预热 TTS、离线 AudioContext 未在麦克风授权前 resume；修复后 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 74 个用例通过；语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，9 个测试文件 / 150 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的 Chrome 语音通话无法播报和无法收音问题，不继续推进模型托管、后端代理、Kokoro 或新的语音能力。

## 修复离线 sherpa-onnx 已安装但仍提示不可用（2026-05-30）
- 当前阶段：本轮已完成离线 sherpa-onnx 已下载/已缓存后仍显示“识别引擎：不可用，建议下载离线语音包”和“等待继续收音”的二次修复；离线包已安装时，音频链路异常、Worker 异常或 stop flush 后无识别文本都会显示离线引擎异常，不再误导用户重复下载。
- 问题原因：离线 STT 复用了浏览器 Web Speech 的“检测到麦克风音量但 6 秒没有文字就 no-transcript”的降级计时器；而 sherpa-onnx 常常只在 endpoint 或 `stop()` 后给 final 文本，所以用户说话期间可能先没有 partial。旧通话编排又只在已有 `pendingMessage` 时才触发 stop flush，导致“有音频但无 partial”的离线场景既不会自动提交，也会被 `no-transcript` 归类成可恢复中断，页面最终显示“等待继续收音 / 建议下载离线语音包”。
- 已完成内容：`useSpeechToText.js` 对离线模式关闭通用 no-transcript 计时器，保留 2 秒无 PCM 帧的 `offline-audio-unavailable` 检查；新增 `offline-error` 引擎状态和 `offline-no-transcript` 错误码，区分“模型包已安装但运行/识别异常”和“模型未下载”。`useVoiceCall.js` 在离线 ready 且检测到麦克风活动时，即使尚无 partial 文本也会按静音超时触发 `speech.stop()`，等待 sherpa flush final 后再发送，并增加自动发送中的重入保护。`InterviewSessionView.vue` 增加“识别引擎：离线 sherpa-onnx 异常”文案，不再把已安装离线包的失败显示成“建议下载离线语音包”。
- 交互边界：本轮只修复前端离线 STT 错误分类、自动 flush 和会话页引擎文案；不新增后端接口、不改数据库、不上传用户音频、不提交大体积模型文件、不替换模型下载源。
- 前端验证：先补充 RED 回归测试并确认旧代码失败于 ready 离线识别被误判 `no-transcript`、离线 Worker 错误显示为 `unavailable`、有麦克风活动但无 partial 时不触发 stop flush、页面仍提示建议下载；修复后相关语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，7 个测试文件 / 139 个用例通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的离线包已安装但仍不可用/建议下载/等待继续收音问题，不继续推进模型托管、后端代理、Kokoro 或新的语音能力。

## 修复离线 sherpa-onnx 已就绪但不识别麦克风音频（2026-05-30）
- 当前阶段：本轮已完成离线 sherpa-onnx 引擎 ready 与真实麦克风收音态拆分，下载离线资源包后不会再仅因为 Worker ready 就显示“正在聆听”；如果浏览器没有创建音频处理节点或没有产生 PCM 音频帧，会明确报错并退出收音态，等待人工在已缓存模型的真实浏览器中复测麦克风识别。
- 问题原因：旧离线链路把“WASM/模型资源加载成功”等同于“麦克风音频已经进入解码器”，默认创建 `AudioContext()` 且没有处理 `suspended` 状态；离线 Worker `ready` 前就连接 `ScriptProcessor`、发送 `start` 并置为录音中，Worker 初始化期间早到的 `start/audio` 也可能被丢弃；页面标题又把 `offline-ready` 当成正在聆听，所以会出现“识别引擎已就绪但一直不识别”的假收音状态。
- 已完成内容：`useSpeechToText.js` 离线模式改为 `AudioContext({ sampleRate: 16000 })`，按 sherpa 浏览器示例启用回声消除、降噪和自动增益，并在 `suspended` 时显式 `resume()`；Worker 返回 `ready` 后才连接 `ScriptProcessor`、发送 `start` 和置 `isRecording=true`；新增 `offline-audio-unavailable` 错误码，覆盖无音频处理节点和开始录音后 2 秒无音频帧的场景。`sherpaSpeechWorker.js` 新增 `recognizerReadyPromise`，让 `start/audio/stop` 等待初始化完成。`InterviewSessionView.vue` 的“正在聆听”只依赖真实录音态，`offline-ready` 只保留在识别引擎状态文案里。
- 交互边界：本轮只修改前端离线 STT 音频链路、Worker 初始化时序和会话页状态标题；不新增后端接口、不改数据库、不上传用户音频、不提交或替换大体积模型文件，不继续扩展云端 STT/TTS 或 Kokoro。
- 前端验证：先补充 RED 回归测试并确认旧代码失败于 16k AudioContext、suspended resume、Worker ready 后再 recording、音频帧投递、无音频节点/无音频帧错误、Worker 早到消息排队和 offline-ready 标题误判；修复后 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 54 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 72 个用例通过；计划内语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，6 个测试文件 / 131 个用例通过；补充含 Worker 的整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，7 个测试文件 / 132 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的离线 sherpa-onnx ready 后不识别、假正在聆听和音频链路无失败出口问题，不继续推进模型托管、模型下载源、后端代理或新的语音能力。

## 模拟面试离线语音通话准备态与设置页排版修复（2026-05-30）
- 当前阶段：本轮已完成下载离线 sherpa-onnx 资源包后语音面试卡在“通话准备中”的状态展示和自动提交链路修复，并完成设置中心“离线增强”资源卡排版调整，等待人工在已缓存离线模型的浏览器中复测开始通话、说完后自动发送和删除资源包显示。
- 问题原因：离线 sherpa-onnx 更容易先持续返回 `partial/interim` 识别片段，旧 `useVoiceCall` 只把 `finalTranscript` 拼入待发送文本，导致用户说完后可能没有可发送内容，界面表现为离线引擎已就绪但通话仍像停在准备态；同时设置页把 STT 下载/删除按钮嵌进 `.offline-model-status` 状态网格，下载成功后出现“删除资源包”按钮会挤占状态卡片列，造成排版错乱。
- 已完成内容：`useVoiceCall.js` 新增 `pendingFinalText` / `pendingInterimText` 聚合，interim 先作为待发送文本参与静音自动提交，stop flush 后由 final 接管并去重；`InterviewSessionView.vue` 在离线 worker `offline-ready` 时将通话标题展示为“正在聆听”，避免“离线已就绪 + 通话准备中”的矛盾状态；`SettingsView.vue` 将离线资源操作区移出状态网格，补充 `.offline-card-heading`、独立 `.offline-model-actions` 和移动端纵向布局。
- 交互边界：本轮不改后端接口、不改数据库、不上传用户音频、不新增模型文件、不接入 Kokoro 下载；只修复当前离线 STT 通话状态、自动提交文本聚合和设置页资源卡排版。
- 前端验证：先新增回归测试并确认旧代码失败于 interim 不进入待发送文本、离线 ready 仍显示“通话准备中”、删除资源按钮嵌套在状态网格；修复后 `npm.cmd test -- --run src/__tests__/composables/useVoiceCall.test.js` 通过，1 个测试文件 / 19 个用例通过；`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，1 个测试文件 / 25 个用例通过；`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 34 个用例通过；相关语音与设置整组回归 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，6 个测试文件 / 126 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的离线语音通话准备态、自动提交和离线增强页排版问题，不继续推进离线模型托管、后端代理、Kokoro 或新的语音能力。

## 个人中心导航卡片 hover 光标修复（2026-05-30）
- 当前阶段：本轮已完成个人中心 `/dashboard` 页面成长中心入口、最近简历诊断完成记录和最近模拟面试完成记录的鼠标小手光标修复，等待人工在桌面端浏览器中复测这些卡片内部文字、图标和右侧状态区域的 hover 状态。
- 问题原因：全局只读文本基线 `body * { cursor: default; caret-color: transparent; }` 会覆盖可点击 `div` 内部子节点；个人中心旧实现中成长中心入口和最近记录项使用 `div @click` 跳转，虽然父级设置了 `cursor: pointer`，但鼠标移到内部文字或图标时仍可能被全局规则覆盖成默认箭头。
- 已完成内容：`DashboardView.vue` 将固定跳转的成长中心入口改为 `router-link`；将最近简历诊断和最近模拟面试中可跳转的完成态记录改为动态 `router-link`，非完成态仍渲染普通 `div`；补齐链接颜色继承、去下划线和 `focus-visible` 样式，保持原视觉布局并提升键盘可访问性。
- 交互边界：本轮只修复个人中心导航型点击区域的光标和语义，不修改个人中心接口、额度计算、最近记录数据、路由目标、全局 cursor 基线或其它页面。
- 前端验证：先新增 `DashboardView.test.js` 回归测试并确认旧代码失败于成长中心入口和最近记录仍为 `div @click`；修复后 `npm.cmd test -- --run src/__tests__/views/DashboardView.test.js` 通过，1 个测试文件 / 6 个用例通过；相关回归 `npm.cmd test -- --run src/__tests__/views/DashboardView.test.js src/__tests__/components/AppHeader.test.js src/__tests__/themeTokens.test.js src/__tests__/layouts/MainLayout.test.js` 通过，4 个测试文件 / 22 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_DASHBOARD_CURSOR_POINTER_FIX_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的个人中心鼠标小手问题，不继续推进个人中心视觉重构或新的数据能力。

## 消息通知栏 hover 光标修复（2026-05-30）
- 当前阶段：本轮已完成顶部消息通知铃铛、通知下拉列表项和“查看全部消息”入口的鼠标小手光标修复，等待人工在桌面端浏览器中复测通知铃铛、通知列表文字/图标区域和底部入口 hover 状态。
- 问题原因：全局样式为了避免只读文本出现输入态光标，设置了 `body * { cursor: default; caret-color: transparent; }`；通知栏旧实现使用可点击 `div`，虽然父级写了 `cursor: pointer`，但内部文字、图标等子节点仍被全局规则覆盖成默认箭头，所以鼠标移到通知项文字或图标时没有显示小手。
- 已完成内容：`AppHeader.vue` 将通知铃铛、通知列表项和底部入口改为原生 `button type="button"`，复用现有全局 `button * { cursor: inherit; }` 规则让内部节点继承点击光标；补齐 `appearance: none`、透明背景、无边框、继承字体和宽度样式，保持原视觉布局；`NotificationTypeIcon.vue` 根节点改为 `span`，避免按钮内部嵌套块级 `div`。
- 交互边界：本轮只修复消息通知栏 hover 光标和点击元素语义，不修改通知接口、SSE 实时通知、已读逻辑、跳转逻辑、全局 cursor 基线或其它页面。
- 前端验证：先新增 `AppHeader.test.js` 回归测试并确认旧代码失败于通知交互区域仍为 `div`；修复后 `npm.cmd test -- --run src/__tests__/components/AppHeader.test.js src/__tests__/components/notification/NotificationTypeIcon.test.js src/__tests__/views/NotificationView.test.js src/__tests__/utils/notificationMeta.test.js src/__tests__/themeTokens.test.js` 通过，5 个测试文件 / 24 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_NOTIFICATION_CURSOR_POINTER_FIX_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的通知栏鼠标小手问题，不继续推进新的通知能力或视觉重构。

## Offer 辅助结果面板等高滚动修复（2026-05-29）
- 当前阶段：本轮已完成 `/offer` 页面右侧生成结果面板初始高度和长内容滚动修复，等待人工在桌面端和窄屏视口复测。
- 问题原因：`OfferAssistView.vue` 的工作台 grid 使用 `align-items: start`，右侧 `output-panel` 按自身空态内容自然高度展示，空态只有 `min-height: 360px`，所以首次进入时明显短于左侧输入面板；生成内容较长时也缺少独立滚动承载层，容易继续撑高结果面板。
- 已完成内容：`workbench` 桌面端保持等高布局，`OfferAssistView.vue` 通过 `ResizeObserver` 读取左侧输入面板真实高度并同步给 `output-panel`；右侧面板改为纵向 flex 容器并隐藏外溢；在面板标题下新增 `result-scroll` 承载所有加载态、空态和结果块，设置 `flex: 1 1 auto`、`min-height: 0`、`overflow-y: auto`，内容过长时只在结果区内部滚动；1100px 以下上下堆叠时恢复自然高度和可见溢出。
- 交互边界：本轮只修改 Offer 辅助前端布局和对应回归测试；不修改接口、表单字段、提交逻辑、复制按钮策略、后端 Prompt、数据库或其它页面。
- 前端验证：先新增 `OfferAssistView.test.js` 回归测试并确认旧代码失败于缺少 `result-scroll` 与等高布局规则；修复后 `npm.cmd test -- --run src/__tests__/views/OfferAssistView.test.js` 通过，1 个测试文件 / 1 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_33_OFFER_ASSIST_PAGE_REDESIGN_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的右侧结果面板高度与内部滚动问题，不继续推进新的 Offer 辅助能力。

## 模拟面试语音通话状态重置修复：识别中断保留通话态（2026-05-29）
- 当前阶段：本轮已完成语音面试多轮对话后说完话偶发回到“开始通话”的根因修复，等待人工在 Chrome、Edge 和移动端浏览器中复测长时间语音面试、说完后自动提交、手动停止发送和识别服务短暂中断场景。
- 问题原因：浏览器 Web Speech 在长时间或多轮识别后可能抛出 `network`、`no-speech`、`no-transcript`、`end-without-result` 这类可恢复识别中断；旧的 `useVoiceCall` 只要监听到 `speech.error` 就直接 `endVoiceCall()`，把语音模式、计时、麦克风状态和通话界面清空，所以用户感知为页面刷新并回到没开始通话。
- 已完成内容：`useVoiceCall.js` 增加可恢复 STT 错误码白名单，对 `network`、`no-speech`、`no-transcript`、`end-without-result` 保留当前语音通话和已识别文本，进入“等待继续收音”；`InterviewSessionView.vue` 将 `useSpeechToText` 的 `errorCode` 传入语音通话编排；`useVoiceCall.test.js` 和 `InterviewSessionView.test.js` 增加回归测试，覆盖可恢复识别中断不退出通话、致命权限错误仍降级。
- 交互边界：本轮不改后端接口、不改数据库、不上传用户音频、不新增云端 STT/TTS、不修改 SSE 消息协议；只修复 STT 错误分类和前端通话状态保留。
- 前端验证：先运行新增 `useVoiceCall` 回归测试失败，确认当前代码在 `no-transcript` 时会退出语音模式；修复后 `npm.cmd test -- --run src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，2 个测试文件 / 42 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理语音识别中断导致通话状态重置的问题，不继续推进离线模型部署、TTS 模型增强或新的面试功能。

## 用户端简历诊断路由白屏性能回归修复（2026-05-29）
- 当前阶段：本轮已完成用户端简历诊断上传页和结果页白屏回归排查、结果页 chunk 预取等待、诊断路由页面级加载占位和相关回归测试，等待人工在真实登录态浏览器中复测从导航进入 `/resume/upload`、提交 PDF、跳转 `/resume/result/:taskId` 的完整链路。
- 问题原因：简历诊断结果页是较重的动态 import 页面，上传页此前只触发 `prefetchUserRoute('/resume/result')` 但没有等待预取完成；上传接口成功后立即跳转时，如果结果页 chunk 尚未下载和解析完成，用户端内容区只剩顶部细进度条，容易感知为白屏。
- 已完成内容：`MainLayout.vue` 增加 `loadingRoutePath`、`isResumeDiagnosisRoute` 和 `routeLoadingTargetText`，仅对 `/resume/upload` 与 `/resume/result` 在路由切换超过 120ms 时展示页面级占位卡片；`UploadView.vue` 保存结果页预取 Promise，并在拿到任务 ID 后先等待预取完成再 `router.push` 到结果页；`MainLayout.test.js` 与 `ResumeUploadView.test.js` 已补充回归断言。
- 交互边界：本轮不修改上传接口、结果页任务轮询、任务状态展示、后端缓存、数据库或用户端其他页面；占位只覆盖简历诊断相关路由，不把所有用户端页面都改成新骨架屏。
- 前端验证：先运行新增回归测试失败，确认当前代码缺少诊断路由占位和预取等待；修复后 `npm.cmd test -- --run src/__tests__/layouts/MainLayout.test.js src/__tests__/views/ResumeUploadView.test.js` 通过，2 个测试文件 / 2 个用例；`npm.cmd test -- --run src/__tests__/layouts/MainLayout.test.js src/__tests__/views/ResumeUploadView.test.js src/__tests__/views/ResumeResultView.test.js src/__tests__/router/routeLoaders.test.js src/__tests__/components/AppHeader.test.js src/__tests__/themeTokens.test.js` 通过，6 个测试文件 / 25 个用例；`npm.cmd test -- --run` 通过，67 个测试文件 / 424 个用例；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_RESUME_DIAGNOSIS_ROUTE_WHITE_SCREEN_FIX_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的用户端简历诊断白屏体验，不继续扩展后端任务性能、接口缓存、结果页视觉重构或新的诊断功能。

## 管理端导航切换白屏性能回归修复（2026-05-29）
- 当前阶段：本轮已完成管理端侧栏导航首点白屏回归排查、管理端路由 chunk 交互预取、内容区切换加载反馈和 `AdminLayout` 动效属性收敛，等待人工在真实登录态浏览器中复测 `/admin/dashboard`、`/admin/users`、`/admin/prompts`、`/admin/ai-engines` 等较重页面的首次点击体验。
- 问题原因：前一轮性能优化主要覆盖用户端高频路径，管理端子页面仍在点击导航后才下载和解析对应动态 import chunk；`AdminLayout` 内容区此前是裸 `RouterView`，没有延迟加载反馈，导致首次进入管理端子页面时容易感知为白屏加载。
- 已完成内容：`routeLoaders.js` 新增管理端页面 loader 白名单和 `prefetchAdminRoute`，`router/index.js` 的管理端子路由改为复用命名 loader；`AdminLayout.vue` 对侧栏导航补充 hover、focus、touch 预取，内容区改为 slot `RouterView` + `Transition`，并在管理端内部切换超过 120ms 时显示顶部细进度条；管理端导航项 `transition: all` 已改为明确的 `background-color`、`color`、`box-shadow`、`transform`，页面切换只使用 `opacity/transform`。
- 交互边界：本轮不修改管理端 API、后端缓存、数据库、权限守卫、页面业务结构和用户端路由；管理端不做 idle 全量预热，只在管理员对具体导航项表达意图时预取，避免后台一次性拉满所有页面 chunk。
- 前端验证：先运行新增回归测试失败，确认当前代码缺少管理端预取和切换反馈；修复后 `npm.cmd test -- --run src/__tests__/router/routeLoaders.test.js src/__tests__/layouts/AdminLayout.test.js` 通过，2 个测试文件 / 7 个用例；`npm.cmd test -- --run src/__tests__/router/routeLoaders.test.js src/__tests__/layouts/AdminLayout.test.js src/__tests__/layouts/MainLayout.test.js src/__tests__/themeTokens.test.js` 通过，4 个测试文件 / 14 个用例；`npm.cmd test -- --run` 通过，66 个测试文件 / 417 个用例；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_ADMIN_ROUTE_SWITCH_PERFORMANCE_FIX_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的管理端导航白屏和卡顿感知回归，不继续扩展管理端接口缓存、SQL 优化、后台页面视觉重构或新功能开发。

## 全局导航与子导航光标回归修复（2026-05-28）
- 当前阶段：本轮已完成全局 I-beam 光标残留二次排查、只读文本 caret 隐藏、历史记录下拉点击选区拦截和第三方组件子节点光标基线修复，等待人工在浏览器中复测桌面端“历史记录”下拉、设置页子导航、个人动态 Tab、社区文本和常见按钮/链接文字区域。
- 问题原因：前一轮主要修复了普通 hover 的 `cursor` 回退，但用户继续反馈“点击任意文字都会出现输入符号”后确认问题还包含浏览器点击/焦点/文本选区阶段的 caret 呈现；只读展示文本需要显式隐藏 `caret-color`，同时真实输入控件、文本域和 `contenteditable` 编辑区必须恢复输入 caret。
- 已完成内容：`styles/index.css` 将只读展示节点统一设为 `cursor: default` 且 `caret-color: transparent`；将 `input`、`textarea`、`contenteditable="true"`、`contenteditable="plaintext-only"` 及其内部节点恢复 `cursor: text` 与 `caret-color: auto`；将 `button`、`a`、`[role="button"]` 的子节点设为继承父级光标；将 `[role="menuitem"]`、`[role="tab"]`、Element Plus dropdown/tab、Naive UI button/tab 及其子节点统一设为点击光标，禁用态保持 `not-allowed`；`AppHeader.vue` 对历史记录触发器和两个下拉菜单项补充 `@mousedown.prevent`，避免点击菜单文字时进入文本选区或 caret 状态。
- 交互边界：本轮只修光标和 caret 交互基线、历史记录下拉点击行为，不新增页面、不改后端接口、不改数据库、不改变现有点击跳转逻辑；输入框、文本域和真实 `contenteditable` 仍保留输入光标。
- 前端验证：新增回归测试后先运行 `npm.cmd test -- --run src/__tests__/components/AppHeader.test.js src/__tests__/themeTokens.test.js` 失败于只读文本 caret 隐藏和历史菜单 `mousedown` 拦截缺失；补充 `contenteditable` 子节点保护测试后 `npm.cmd test -- --run src/__tests__/themeTokens.test.js` 失败于编辑区子节点 caret 恢复缺失；修复后 `npm.cmd test -- --run src/__tests__/components/AppHeader.test.js src/__tests__/themeTokens.test.js` 通过，2 个测试文件 / 14 个用例通过；相关回归 `npm.cmd test -- --run src/__tests__/components/AppHeader.test.js src/__tests__/themeTokens.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/community/MyActivity.test.js src/__tests__/views/community/CommunityView.test.js src/__tests__/components/community/PostCard.test.js src/__tests__/views/community/PostDetailView.test.js` 通过，7 个测试文件 / 81 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_COMMUNITY_POST_TITLE_AND_REPORT_LINK_FRONTEND.md`。
- 停止说明：本轮只处理用户反馈的莫名输入光标残留，不继续推进其它 UI 重构或社区能力。

## 社区光标、长文折叠、分享标题与骨架屏修复（2026-05-28）
- 当前阶段：本轮已完成社区展示体验修复、RED/GREEN 定向测试和生产构建，等待人工在浏览器中验收社区首页、帖子详情和面试报告分享到社区弹窗。
- 已完成内容：`styles/index.css` 增加全局 cursor 基线，普通展示文本恢复默认箭头光标，输入控件保留输入光标；`PostCard.vue` 与 `PostDetailView.vue` 对长正文默认折叠并提供展开/收起，旧报告分享帖标题为空时显示“面试报告分享”；`ShareReportDialog.vue` 增加可编辑帖子标题，标题必填并随 `sharedInterviewSessionId` 一起提交；`CommunityView.vue` 首页加载态改为结构化帖子骨架屏，`App.vue` 降低 Naive UI Skeleton 颜色噪声。
- 交互边界：本轮只修改前端展示、交互与测试；不新增后端接口、不新增 SQL、不回填旧数据、不扩展富文本、报告下载或公开报告列表能力。
- 前端验证：先运行 `npm.cmd test -- --run src/__tests__/components/community/PostCard.test.js src/__tests__/components/community/ShareReportDialog.test.js src/__tests__/views/community/PostDetailView.test.js src/__tests__/views/community/CommunityView.test.js src/__tests__/themeTokens.test.js` 失败于 9 个预期缺失行为；修复后同命令通过，5 个测试文件 / 42 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_COMMUNITY_POST_TITLE_AND_REPORT_LINK_FRONTEND.md`。
- 停止说明：本轮只处理当前社区光标、长文折叠、分享标题和骨架屏问题，不继续推进其它社区能力。

## 模拟面试离线语音下载修复：Failed to fetch 同源化（2026-05-27）
- 当前阶段：本轮已把离线 STT 模型下载从浏览器跨域拉取 Hugging Face 改为同源静态资源路径，等待在可访问官方源或自有镜像的部署环境中下载四个 sherpa-onnx 资源文件并进行浏览器实测。
- 问题原因：浏览器直接请求 Hugging Face 大体积模型文件会受跨域、网络、地区或镜像可用性影响，失败时表现为 `Failed to fetch`。前端不能依赖用户浏览器跨域拉取模型，必须改成同源静态目录或自有 CDN。
- 已完成内容：`manifest.json` 改为同源文件路径；`runtime.js` 的模型基地址改为 `/voice-models/sherpa-onnx/zh-cn-streaming/`；新增 `scripts/download-sherpa-onnx-model.mjs` 和 `npm.cmd run voice:model:download`，支持通过 `SHERPA_ONNX_MODEL_BASE_URL` 指定镜像源；`offlineVoiceModelCache.js` 对模型文件 fetch 失败、HTML fallback 增加明确错误并写入 failed；`.gitignore` 忽略四个大模型文件；设置页删除资源包按钮改为 Element Plus，避免 Naive 测试主题缺失影响删除入口。
- 交互边界：本轮不新增后端 API、不上传用户音频、不把四个大模型文件提交进 Git；如果默认官方源不可达，需要把 `sherpa-onnx-asr.js`、`sherpa-onnx-wasm-main-asr.js`、`sherpa-onnx-wasm-main-asr.wasm`、`sherpa-onnx-wasm-main-asr.data` 放入 `frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/` 后再构建/部署。
- 前端验证：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，6 个测试文件 / 119 个用例通过；`npm.cmd run build` 通过，仍保留既有 `@vueuse/core` PURE annotation 构建提示。
- 下载验证：当前环境运行默认 `npm.cmd run voice:model:download` 访问 Hugging Face 超时；使用 `hf-mirror.com/spaces/...` 返回 404。脚本已支持通过环境变量配置可用镜像，后续应在网络可达的部署环境或自有 CDN 同步环境中执行。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只解决 Failed to fetch 的前端同源化和部署边界，不继续实现后端代理、CDN 同步任务或 Kokoro。

## 模拟面试语音设置修复：离线资源包可删除（2026-05-26）
- 当前阶段：本轮已补齐设置中心离线语音资源包删除入口，等待人工在浏览器中验证下载后删除、取消删除、失败残留清理和已有 Kokoro 缓存删除。
- 问题原因：sherpa-onnx 离线语音识别模型会缓存到当前浏览器本地。旧设置页只在模型 `ready` 时显示“清除模型缓存”，文案不够明确，且下载失败残留没有主动删除入口；如果 `tts:kokoro` 已有缓存状态，也只显示“已缓存”而没有删除入口，不符合用户应自主选择是否保留资源包的要求。
- 已完成内容：`SettingsView.vue` 将离线 STT 操作改为明确的“删除资源包”，在 `ready` 和 `failed` 状态均展示删除入口；同时为 `tts:kokoro` 已缓存或失败状态展示“删除音色包”；删除前通过 `ElMessageBox.confirm` 二次确认，确认后复用 `clearModelCache` 清理本地缓存与状态，取消时不清理。`SettingsView.test.js` 增加确认删除、取消保留、失败残留可删除和 Kokoro 缓存删除回归测试。
- 交互边界：本轮不新增后端 API、不修改数据库、不启用 Kokoro 下载、不提交真实模型文件；高品质离线音色包下载仍是后续阶段，本轮只处理已有本地资源状态的删除权。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 30 个用例通过；`npm.cmd run build` 通过，仅保留既有 `@vueuse/core` PURE annotation 提示。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只完成离线资源包删除入口，不继续推进离线模型部署、TTS 模型增强或新的面试功能。

## 模拟面试语音识别回归修复：Web Speech 先启动，开场白后收音（2026-05-26）
- 当前阶段：本轮已修复浏览器原生语音识别启动顺序和语音面试开场白收音竞态，等待人工在 Chrome、Edge、移动端浏览器中复测麦克风授权、开场白播报和首轮回答识别。
- 问题原因：浏览器识别路径此前先开启自定义 `getUserMedia` 音量监测，再调用 `SpeechRecognition.start()`，辅助监测可能提前占用麦克风并改变授权表现；语音通话层在开始通话时立即启动 STT，随后开场白播报又取消 STT，形成启动/取消竞态，容易让页面停在“正在回复/收音”一类异常状态。
- 已完成内容：`useSpeechToText.js` 改为 Web Speech 先创建并启动 `SpeechRecognition`，可选音量监测改为后置异步辅助且失败不阻断识别；离线 sherpa-onnx 路径继续保留 PCM 音频流前置采集。`useVoiceCall.js` 新增首轮延迟开麦能力，避免静音检查定时器提前恢复收音；`InterviewSessionView.vue` 在有开场白时先进入通话态并播报，TTS 结束后再恢复收音。
- 交互边界：本轮不删除离线模型下载和缓存能力；不新增后端 API；不上传用户音频；不接入 Kokoro；不提交真实模型文件。浏览器服务识别仍受浏览器、网络、地区和权限策略影响，失败后保留文字输入。
- 前端验证：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 61 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，6 个测试文件 / 107 个用例通过；`npm.cmd run build` 通过，仅保留既有 `@vueuse/core` PURE annotation 提示。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只完成语音识别启动顺序与开场白延迟开麦修复，不继续推进离线模型部署、TTS 模型增强或新的面试功能。

## 模拟面试语音识别回归修复：浏览器/系统优先尝试，离线模型兜底（2026-05-26）
- 当前阶段：本轮已修复 STT 默认强制离线模型导致未安装模型时直接要求下载的问题，恢复为渐进增强路径，等待人工在 Chrome、Edge、移动端浏览器中复测麦克风授权和识别结果。
- 问题原因：上轮将 `preferOffline` 默认路径直接绑定到 sherpa-onnx 缓存状态，模型未 ready 时返回 `offline-missing`；语音通话层又将离线模型缺失作为进入语音模式的硬阻断，导致浏览器/系统原生识别没有被尝试。
- 已完成内容：`useSpeechToText.js` 改为离线模型 ready 时优先离线 Worker，模型缺失但 Web Speech 存在时先启动浏览器/系统识别，只有两者都不可用才提示下载离线模型；`useVoiceCall.js` 移除离线模型缺失的硬阻断；设置默认 `voiceRecognitionEngine` 恢复为 `system_local`；设置页与面试页文案改为“浏览器/系统优先，离线兜底”。
- 交互边界：本轮不删除离线模型下载和缓存能力；不新增后端 API；不上传用户音频；不接入 Kokoro；不提交真实模型文件。浏览器识别失败后仍会退出语音模式并保留文字输入。
- 前端验证：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，6 个测试文件 / 104 个用例通过；`npm.cmd run build` 通过，仅保留既有 `@vueuse/core` PURE annotation 提示。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只完成语音识别启动顺序回归修复和文案同步，不继续推进离线模型部署、TTS 模型增强或新的面试功能。

## 模拟面试语音可用性修复：真实离线 STT 主路径（2026-05-26）
- 当前阶段：本轮已完成模拟面试语音识别从浏览器 Web Speech 默认路径切换为离线 sherpa-onnx 优先路径，并增加 TTS 超时释放保护，等待人工在已部署模型文件的浏览器环境复测。
- 已完成内容：`useSpeechToText.js` 默认检查本地离线模型缓存，模型缺失时明确提示下载且不进入假录音；新增 `sherpaSpeechWorker.js` 作为浏览器 Worker 边界；`offlineVoiceModelCache.js` 支持 manifest 下载、Cache API 缓存、状态读取和清理；`useTextToSpeech.js` 增加播报 watchdog；`useVoiceCall.js` 在模型缺失时不启动语音通话。
- 设置与偏好：`voiceRecognitionEngine` 默认改为 `offline_sherpa`；设置中心提供真实离线 STT 下载、进度和清除缓存入口；Kokoro 仍为后续阶段，不在本轮接入。
- 交互边界：本轮不新增后端 API、不修改数据库、不上传用户音频；静态模型文件需部署到 `/voice-models/sherpa-onnx/zh-cn-streaming/`。
- 前端验证：已通过离线缓存、STT、TTS、语音通话、设置页、面试页和设置偏好定向测试；后续继续运行完整指定测试和生产构建。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只解决 STT 真实可用主路径和 TTS 卡死，不推进 Kokoro、后端模型托管或新的面试能力。

﻿## 用户端首页过渡动画与动效增强（2026-05-24）
- 当前阶段：本轮已完成首页 `/` 动效增强、定向测试和生产构建，等待人工视觉验收。
- 用户端导航路由切换流畅度优化（2026-05-26）：本轮已完成用户端高频路由切换流畅度优化。`routeLoaders.js` 的预取白名单扩展为 `/templates`、`/community`、`/growth`、`/resume/upload`、`/interview/entry`、`/offer`，并保留 `Set` 去重，避免重复加载同一路由 chunk；`AppHeader.vue` 在桌面 hover/focus 与移动端 touch/focus 触发按需预取，登录后空闲时低优先级预热模板库、社区、成长中心三个重入口，不做全量 preload。`MainLayout.vue` 增加 120ms 延迟显示的顶部细进度条，并只对 `TemplateLibraryView`、`CommunityView`、`GrowthCenterView` 使用受控 `KeepAlive`，返回列表/看板页时减少重复挂载成本；后续修复中移除布局组件里的 `onBeforeRouteUpdate`，改用 `router.beforeEach`、`router.afterEach` 和 `router.onError` 注册/注销布局级切换反馈，消除 Vue Router “No active route record” 警告。`ExportToolbar.vue` 将 `html2canvas` 与 `jspdf` 改为点击导出时动态导入；模板卡片和筛选项收敛 `transition: all`，保留 `contain: layout paint style`，不恢复 `content-visibility: auto`。验证结果：`npm.cmd test -- --run src/__tests__/components/AppHeader.test.js src/__tests__/router/routeLoaders.test.js src/__tests__/components/template/ExportToolbar.test.js src/__tests__/layouts/MainLayout.test.js src/__tests__/App.test.js src/__tests__/components/template/TemplateCard.test.js` 通过，6 个测试文件 / 18 个用例通过；警告修复后补充 `npm.cmd test -- --run src/__tests__/layouts/MainLayout.test.js src/__tests__/components/AppHeader.test.js src/__tests__/router/routeLoaders.test.js src/__tests__/App.test.js` 通过，4 个测试文件 / 13 个用例通过；`npm.cmd run build` 通过，仅保留既有 `@vueuse/core` pure annotation 提示。关联任务文件：`frontend/tasks/TASK_USER_ROUTE_SWITCH_PERFORMANCE_FRONTEND.md`。停止说明：本轮只处理用户端导航路由切换与高频页面首屏/返回流畅度，不改业务接口、路由路径、后端或管理端，不缓存详情页、编辑器、面试会话和结果页。
- 模板库轻量缩略图与真实预览视觉对齐（2026-05-26）：本轮已完成模板库轻量缩略图与真实预览的视觉对齐，并补充修复缩略图灰屏问题。新增 `templatePreviewMeta.js`，按真实模板 CSS 的主色、背景和主要版式为每个模板维护轻量预览元数据；`TemplatePreviewImage.vue` 改为读取元数据并按顶部横幅、极简头像、深色整页、侧栏、居中权威和柔和卡片等版式输出轻量 DOM；`tech-minimal` 缩略图已从旧蓝色改为真实绿色 `#5B7A2E` 与浅绿背景 `#F4F7EE`。列表卡片继续保持轻量渲染，不恢复完整 `TemplateRenderer` 或每卡 raw CSS 导入；灰屏修复中移除 `content-visibility: auto` / `contain-intrinsic-size`，并移除 `color-mix()` 运行时混色依赖，改为明确 CSS 变量。验证结果：`npm.cmd test -- --run src/__tests__/components/template/TemplateCard.test.js src/__tests__/components/template/TemplatePreviewImage.test.js` 通过，2 个测试文件 / 13 个用例通过；`npm.cmd run build` 通过，仅保留既有 `@vueuse/core` pure annotation 提示。关联任务文件：`frontend/tasks/TASK_TEMPLATE_PREVIEW_THUMBNAIL_ALIGNMENT_FRONTEND.md`。停止说明：本轮只完成缩略图与真实预览的轻量对齐和灰屏修复，不继续推进其它模板库功能或性能重构。
- 用户端导航路由切换性能优化（2026-05-25）：本轮已完成模板库、社区、成长中心三个高频导航入口的路由切换性能优化。模板库列表卡片从完整 `TemplateRenderer` 简历 HTML 改为轻量 `TemplatePreviewImage` 缩略图，完整渲染只保留在预览弹窗和编辑器；模板预览弹窗改为异步组件。社区首页 `PostEditor` 改为打开发布弹窗时按需加载，并为帖子卡片增加渲染隔离。成长中心 `LineChart`、`RadarChart`、`RadarScorePanel` 改为异步组件并补充稳定图表占位。路由层新增 `routeLoaders.js`，导航栏只对 `/templates`、`/community`、`/growth` 做 hover/focus/touch 意图预取，不做全量 preload。验证结果：`npm.cmd test -- --run src/__tests__/components/template/TemplateCard.test.js src/__tests__/views/community/CommunityView.test.js src/__tests__/views/GrowthCenterView.test.js src/__tests__/components/AppHeader.test.js` 通过，4 个测试文件 / 30 个用例通过；`npm.cmd run build` 通过，构建产物中 `TemplateLibraryView` 约 7.81KB，`TemplatePreviewDialog`、`TemplateRenderer`、`PostEditor`、`LineChart`、`RadarChart`、`RadarScorePanel` 均为独立 chunk。关联任务文件：`frontend/tasks/TASK_USER_ROUTE_SWITCH_PERFORMANCE_FRONTEND.md`。停止说明：本轮只处理用户端路由切换性能，不改业务接口、路由路径、后端或其它页面重构。
- 模板库完整预览样式回归修复（2026-05-25）：路由切换性能优化后，列表卡片不再为每张模板注入 raw CSS，但预览弹窗也缺少对应模板样式注入，导致点击“预览”只显示基础结构。本轮在 `TemplatePreviewDialog.vue` 中按 `template.id` 动态加载 `@/data/styles/${id}.css?raw` 并注入预览纸张，关闭弹窗时清空样式，保留弹窗按需加载与列表轻量化。验证结果：`npm.cmd test -- --run src/__tests__/components/template/TemplateCard.test.js` 通过，1 个测试文件 / 2 个用例通过；`npm.cmd run build` 通过。关联任务文件：`frontend/tasks/TASK_USER_ROUTE_SWITCH_PERFORMANCE_FRONTEND.md`。停止说明：本轮只修复模板预览弹窗样式回归，不恢复列表完整 HTML 渲染。
- 图片 WebP 优化后布局回归修复（2026-05-25）：本轮已修复 `OptimizedImage` 接入后 Logo、默认用户头像和面试官头像过度放大、比例失衡的问题。根因是父组件 scoped CSS 无法命中子组件内部真实 `img`，本轮在导航栏、登录页、模拟面试会话页和设置中心补充 `:deep()` 尺寸与 `object-fit` 约束，保留 WebP 优先和 PNG fallback。验证结果：`npm.cmd test -- --run src/__tests__/utils/optimizedImages.test.js src/__tests__/components/AppHeader.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/SettingsView.test.js` 通过，4 个测试文件 / 59 个用例通过；`npm.cmd run build` 通过。关联任务文件：`frontend/tasks/TASK_USER_PNG_ICON_PERFORMANCE_FRONTEND.md`。停止说明：本轮只修复图片优化后的布局回归，不继续推进视觉重构或资源策略扩展。
- 已完成内容：读取并按 `motion-vue`、`impeccable/animate`、`ui-skills/fixing-motion-performance` 执行；补强 `background.png` 猫图 reveal/呼吸、3 个云元素进入/漂移、求职路径 stagger reveal、使用路径 stagger reveal、按钮/节点 hover 与 press 微交互；补齐 `prefers-reduced-motion` 降级。
- Naive UI 暗色桥接与首页暖暗色修复（2026-05-24）：本轮已修复暗色模式下首页像灰色遮罩盖住的问题。`App.vue` 根层新增 `NConfigProvider`，根据 `themeStore.resolvedTheme` 切换 Naive UI `darkTheme` 并注入橙色品牌主题覆盖，补齐此前暗色方案主要适配 Element Plus、Naive UI 组件未统一跟随主题的问题；首页暗色覆盖从偏灰紫底调整为深橙棕暖暗色背景、暖橙光感和半透明橙棕表面，猫图亮度与饱和度恢复，云、hero 面板、CTA、路径节点、使用路径区、辅助能力入口和版本卡片同步适配。验证结果：`npm.cmd test -- --run src/__tests__/App.test.js src/__tests__/views/HomePageView.test.js` 通过，2 个测试文件 / 5 个用例通过；`npm.cmd run build` 通过。关联任务文件：`frontend/tasks/TASK_USER_HOME_UI_REFACTOR_FRONTEND.md`。停止说明：本轮只处理暗色主题桥接与首页暗色视觉，不继续推进其它页面重构。
- 首页暗色模式兼容修复（2026-05-24）：本轮已完成首页 `/` 的暗色主题适配，补充 `.theme-aware-home` 主题测试锚点，并通过 `:global([data-theme="dark"])` 覆盖首页大背景、首屏插画容器、猫图亮度、云元素、hero 面板、快捷入口、次级 CTA、路径节点、使用路径区、辅助能力入口和版本动态卡片；不修改导航栏、其它用户端页面、`/admin/**`、API、路由、数据库或后端业务流程。验证结果：`npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 通过，1 个测试文件 / 3 个用例通过；`npm.cmd run build` 通过。关联任务文件：`frontend/tasks/TASK_USER_HOME_UI_REFACTOR_FRONTEND.md`。停止说明：本轮只处理首页暗色模式兼容，不继续推进其它页面重构。
## 用户端首页暗转亮月亮落下与云朵回聚（2026-05-25）
- 当前阶段：本轮已完成首页 `/` 暗转亮 hero 回切动效、定向测试和生产构建，等待人工视觉验收。
- 已完成内容：`HomePageView.vue` 通过 `MutationObserver` 监听 `html[data-theme]` 从 `dark` 回到非 dark 的切换，添加持续 3000ms 的 `is-light-return` 状态；回切亮色时月亮执行 1.45s 的 `moon-set` 慢慢落下，星空执行 `star-field-fade` 淡出，7 片云朵延迟 1.1s 后执行 1.35s 的 `cloud-regather` 从散开位置重新聚集，避免云朵在月亮下落前抢先出现。
- 交互边界：仅修改首页表现层、首页测试和前端任务文档；不修改全局主题 token、其它用户端页面、`/admin/**`、路由、API、数据库或后端业务流程。
- 前端验证：先运行 `npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 失败于缺少更长的 `is-light-return`、云朵延迟回聚和慢速 `moon-set`；修复后同命令通过，1 个测试文件 / 5 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_USER_HOME_UI_REFACTOR_FRONTEND.md`。
- 停止说明：本轮只完成首页暗转亮表现层修正，不继续推进其它页面或主题系统改造。

## 用户端首页暗色星空、云朵散去与月亮重绘（2026-05-25）
- 当前阶段：本轮已完成首页 `/` 暗色 hero 装饰二次修正、定向测试和生产构建，等待人工视觉验收。
- 已完成内容：亮色模式 hero 云朵从 3 片扩展为 7 片；暗色模式新增 12 颗 CSS 星星和纯 CSS 绘制月亮；月亮改为多层渐变月面、暗色斑纹和高光点，不再使用黑色椭圆切割；云朵通过 `cloud-scatter` 先慢慢散去，月亮延迟到最长云朵退场完成后再执行 `moon-rise`，光晕通过 `moon-glow-breathe` 柔和呼吸。
- 交互边界：仅修改首页表现层、首页测试和前端任务文档；不修改全局主题 token、其它用户端页面、`/admin/**`、路由、API、数据库或后端业务流程。
- 前端验证：先运行 `npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 失败于月亮入场仍早于云朵退场；修复后同命令通过，1 个测试文件 / 4 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_USER_HOME_UI_REFACTOR_FRONTEND.md`。
- 停止说明：本轮只完成首页暗色云朵、星空与月亮表现层修正，不继续推进其它页面或主题系统改造。

## 用户端首页暗色模式云朵退场与月亮升起（2026-05-25）
- 当前阶段：本轮已完成首页 `/` 暗色 hero 装饰补充、定向测试和生产构建，等待人工视觉验收。
- 已完成内容：在 `HomePageView.vue` hero 云朵旁新增 CSS 绘制的 `.hero-moon`；亮色模式继续保留 3 个云朵；暗色模式下云朵停止漂移、透明并上移退场，月亮从右上区域轻微升起并保持暖杏色低强度光晕；`prefers-reduced-motion` 下关闭新增动画并直接展示最终状态。
- 交互边界：仅修改首页表现层、首页测试和前端任务文档；不修改全局主题 token、其它用户端页面、`/admin/**`、路由、API、数据库或后端业务流程。
- 前端验证：先运行 `npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 失败于 `.hero-moon` 与暗色月亮选择器缺失；修复后同命令通过，1 个测试文件 / 4 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_USER_HOME_UI_REFACTOR_FRONTEND.md`。
- 停止说明：本轮只完成首页暗色云朵/月亮切换，不继续推进其它页面或主题系统改造。

## 用户端首页暗色 scoped 选择器与变量化修复（2026-05-24）
- 当前阶段：本轮已完成首页暗色模式根因修复、定向测试和生产构建，等待人工视觉验收。
- 已完成内容：定位并修复 `HomePageView.vue` 中 scoped CSS 的暗色选择器写法问题；将无效的 `:global(html[data-theme="dark"]) .xxx` 改为 `:global(html[data-theme="dark"] .xxx)`，避免构建后目标类丢失；首页关键浅色硬编码表面改为 `--home-*` 主题变量驱动，暗色模式使用暖橙棕深色背景和高对比文字。
- 交互边界：仅修改首页 `/` 的 CSS、首页测试和前端任务文档；不修改导航栏、其它用户端页面、`/admin/**`、API、路由、数据库或后端业务流程。
- 前端验证：`npm.cmd test -- --run src/__tests__/App.test.js src/__tests__/views/HomePageView.test.js` 通过，2 个测试文件 / 6 个用例通过；`npm.cmd run build` 通过；构建产物已确认生成 `html[data-theme=dark] .theme-aware-home`、`html[data-theme=dark] .hero-main`、`html[data-theme=dark] .career-path-node`。
- 关联任务文件：`frontend/tasks/TASK_USER_HOME_UI_REFACTOR_FRONTEND.md`。
- 停止说明：本轮只修复首页暗色适配，不继续推进其它页面或新的 UI 重构。

- 交互边界：仅修改首页相关文件、首页测试和前端任务文档；不新增动画库，不修改 `AppHeader.vue`、`MainLayout.vue`、其它用户端页面、`/admin/**`、API、路由、数据库或后端业务流程。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 通过，1 个测试文件 / 2 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_USER_HOME_UI_REFACTOR_FRONTEND.md`。
- 停止说明：本轮只完成首页动效增强，不继续推进其它页面重构。
## 用户端导航栏 UI 与动效重构（2026-05-24）
- 当前阶段：本轮已完成用户端全局导航栏 `AppHeader.vue` 的视觉质感和动效增强，等待人工视觉验收。
- 已完成内容：按 `frontend-design`、`motion-vue`、`impeccable/animate`、`ui-skills/fixing-motion-performance` 约束执行；顶部导航接入 `MotionConfig` 与 `motion.header` 一次性入场；桌面导航改为橙白胶囊式入口，增加 stagger reveal、hover/active 图标反馈和柔和表面层次；移动端汉堡按钮和抽屉导航项增加轻量滑入与 press/hover 反馈；补齐 `prefers-reduced-motion` 降级。
- 交互边界：仅修改 `AppHeader.vue`、对应单测和前端任务文档；不修改 `MainLayout.vue`、首页、其它用户端页面、`/admin/**`、API、路由、鉴权、通知 SSE、数据库或后端业务流程；不新增动画库。
- 前端验证：`npm.cmd test -- --run src/__tests__/components/AppHeader.test.js` 通过，1 个测试文件 / 4 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_USER_NAVIGATION_UI_REFACTOR_FRONTEND.md`。
- 停止说明：本轮只完成用户端导航栏 UI 与动效重构，不继续推进其它页面重构。
## 用户端导航栏响应式与图标尺寸补充修复（2026-05-24）
- 当前阶段：本轮已完成移动端菜单可用性修复、头部图标放大和头像下拉图标放大，等待人工视觉验收。
- 已完成内容：为移动端 `el-drawer` 增加 `append-to-body`，避免抽屉受 `motion.header` 的 transform 定位上下文影响；汉堡按钮固定触控尺寸和层级；480px 以下隐藏头部设置快捷入口以减少挤压，设置入口仍保留在抽屉内；桌面导航、移动端导航、主题切换、通知、设置、汉堡按钮、头像下拉和历史下拉图标统一放大。
- 交互边界：仅修改用户端导航栏、全局头像下拉菜单图标样式、对应单测和前端任务文档；不修改其它页面、`/admin/**`、API、路由、数据库或后端业务流程；不新增动画库。
- 前端验证：`npm.cmd test -- --run src/__tests__/components/AppHeader.test.js` 通过，1 个测试文件 / 5 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_USER_NAVIGATION_UI_REFACTOR_FRONTEND.md`。
- 停止说明：本轮只修复导航栏响应式和图标可读性，不继续推进其它页面重构。
## 用户端导航菜单空白与路由切换性能修复（2026-05-24）
- 当前阶段：本轮已修复移动端导航抽屉内容空白，并降低全局导航栏对路由切换的性能影响，等待人工验收。
- 已完成内容：将 `AppHeader.vue` 根节点从 `motion.header` 降级回原生 `header`，移除 `MotionConfig`、`motion-v` import 和顶部入场配置；移除固定头部 `backdrop-filter` 和导航项 stagger opacity 入场动画；移动端抽屉导航项默认保持可见，避免打开菜单时内容为空；保留轻量 hover/active/press 反馈。
- 交互边界：仅修改用户端导航栏、对应单测和前端任务文档；不修改首页动效、其它用户端页面、`/admin/**`、API、路由、数据库或后端业务流程。
- 前端验证：`rg -n "motion-v|MotionConfig|motion\\.header|headerInitial|nav-item-enter|mobile-nav-item-enter|backdrop-filter|animation: .*nav" frontend/app/src/components/AppHeader.vue` 无匹配；`npm.cmd test -- --run src/__tests__/components/AppHeader.test.js` 通过，1 个测试文件 / 5 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_USER_NAVIGATION_UI_REFACTOR_FRONTEND.md`。
- 停止说明：本轮只修复导航菜单空白与路由切换卡顿，不继续推进其它页面重构。
# 椤圭洰闃舵鐘舵€?
## 鐢ㄦ埛绔椤佃儗鏅富瑙嗚涓庤矾寰勫紡閲嶆瀯锛?026-05-24锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚缂栫爜銆侀椤靛畾鍚戞祴璇曞拰鐢熶骇鏋勫缓锛岀瓑寰呬汉宸ヨ瑙夐獙鏀躲€?- 宸插畬鎴愬唴瀹癸細浠?`frontend/app/src/assets/background.png` 浣滀负棣栭〉棣栧睆鍙充晶鐙珛鐚浘灞傦紝閲嶆瀯涓烘殩鐧芥鑹叉彃鐢诲紡 hero锛涢灞忓乏渚т繚鐣欐爣棰樸€丆TA銆佺粺璁″拰杞婚噺鑳藉姏鑳跺泭锛屼笉鍐嶅拰鎶犲浘鐚噸鍙狅紱鏂板 3 涓?CSS 浜戞湹鍏冪礌鍜屾殩鑹茶儗鏅紱鏍稿績鍔熻兘浠庡崱鐗囩綉鏍兼敼涓?6 鑺傜偣姹傝亴璺緞 `.career-path-rail`锛涚户缁鐢ㄧ幇鏈?`FeatureIcon` 鍜?`motion-v` 寰氦浜掋€?- 浜や簰杈圭晫锛氭湰杞彧澶勭悊棣栭〉 `/`锛屼笉淇敼 `AppHeader.vue`銆乣MainLayout.vue`銆佸叾瀹冪敤鎴风椤甸潰銆乣/admin/**`銆丄PI銆佽矾鐢便€佹暟鎹簱銆佸悗绔笟鍔℃祦绋嬫垨鐧诲綍閴存潈閫昏緫锛涗笉寮曞叆 Lenis/locomotive-scroll銆?- 鍓嶇楠岃瘉锛氬凡鎸?TDD 鍏堣 `npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 澶辫触浜庣己灏?`.background-hero-section`锛岄殢鍚庝慨澶嶅苟閫氳繃锛? 涓祴璇曟枃浠?/ 2 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_USER_HOME_UI_REFACTOR_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧淇棣栭〉瀹＄編鏂瑰悜鍜岄灞?鍔熻兘灞曠ず缁撴瀯锛屼笉缁х画閲嶆瀯 Dashboard銆佹垚闀夸腑蹇冦€佷細鍛樸€丱ffer 鎴栧叾瀹冮〉闈€?
## 鐢ㄦ埛绔椤?Vue motion 鍔ㄦ晥涓庤川鎰熷寮猴紙2026-05-24锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚缂栫爜銆佸畾鍚戞祴璇曞拰鐢熶骇鏋勫缓锛岀瓑寰呬汉宸ヨ瑙夐獙鏀躲€?- 宸插畬鎴愬唴瀹癸細鏂板 `motion-v` 渚濊禆锛涢椤甸灞忎娇鐢?`MotionConfig` 鍜?`motion` 缁勪欢瀹屾垚鍒嗗眰杩涘叆缂栨帓锛涜矾寰勯潰鏉垮鍔?stagger reveal銆乤ctive 鐘舵€佸拰杞婚噺杩涘害绾匡紱6 涓牳蹇冭兘鍔涘崱澧炲姞杩涘叆瑙嗗彛 reveal銆乭over 鍜?press 寰氦浜掞紱4 涓緟鍔╄兘鍔涘叆鍙ｅ鍔犺交閲忔粦鍏ュ拰 press feedback锛涗繚鐣欐鐧藉簳鑹层€佺幇鏈?`FeatureIcon` 涓庨椤?Naive UI 浣跨敤杈圭晫銆?- 浜や簰杈圭晫锛氭湰杞彧澶勭悊棣栭〉 `/` 鍜屽墠绔緷璧栵紝涓嶄慨鏀?`AppHeader.vue`銆乣MainLayout.vue`銆佸叾瀹冪敤鎴风椤甸潰銆乣/admin/**`銆丄PI銆佽矾鐢便€佹暟鎹簱銆佸悗绔笟鍔℃祦绋嬫垨鐧诲綍閴存潈閫昏緫锛涗笉寮曞叆 Lenis/locomotive-scroll銆?- 鍓嶇楠岃瘉锛氬凡鎸?TDD 鍏堣 `npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 澶辫触浜庣己灏?`.motion-hero-shell`锛岄殢鍚庝慨澶嶅苟閫氳繃锛? 涓祴璇曟枃浠?/ 2 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_USER_HOME_UI_REFACTOR_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧瀹屾垚棣栭〉 Vue motion 鍔ㄦ晥涓庤川鎰熷寮猴紝涓嶇户缁噸鏋?Dashboard銆佹垚闀夸腑蹇冦€佷細鍛樸€丱ffer 鎴栧叾瀹冮〉闈€?
## 鐢ㄦ埛绔椤?UI 淇涓庣粨鏋勬敹鏁涳紙2026-05-24锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚缂栫爜銆佸畾鍚戞祴璇曞拰鐢熶骇鏋勫缓锛岀瓑寰呬汉宸ヨ瑙夐獙鏀躲€?- 宸插畬鎴愬唴瀹癸細棣栭〉棣栧睆浠庡ぇ鐣欑櫧钀ラ攢寮忓竷灞€鏀舵暃涓虹揣鍑戞眰鑱屽伐浣滃彴锛涙柊澧?`.hero-main` 宸︿晶涓讳俊鎭尯涓?`.hero-path-panel` 鍙充晶绱у噾璺緞闈㈡澘锛涙牳蹇冭兘鍔涘尯鍥哄畾涓?6 涓ぇ鍏ュ彛锛涙垚闀夸腑蹇冦€佺ぞ鍖轰氦娴併€佷細鍛樹笌棰濆害銆侀€氱煡涓庣増鏈姩鎬佹敼涓鸿交閲忚緟鍔╄兘鍔涘垪琛紱缁х画淇濈暀鐜版湁 `FeatureIcon` 鍜岄椤靛凡鏈?Naive UI 缁勪欢浣跨敤杈圭晫銆?- 浜や簰杈圭晫锛氭湰杞彧澶勭悊棣栭〉 `/`锛屼笉淇敼 `AppHeader.vue`銆乣MainLayout.vue`銆佸叾瀹冪敤鎴风椤甸潰銆乣/admin/**`銆丄PI銆佽矾鐢便€佹暟鎹簱銆佸悗绔笟鍔℃祦绋嬫垨鐧诲綍閴存潈閫昏緫銆?- 鍓嶇楠岃瘉锛氬凡鎸?TDD 鍏堣 `npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 澶辫触浜庢棫缁撴瀯缂哄皯 `.hero-main`锛岄殢鍚庝慨澶嶅苟閫氳繃锛? 涓祴璇曟枃浠?/ 2 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_USER_HOME_UI_REFACTOR_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧淇棣栭〉瑙嗚宕╁潖鍜岄椤靛姛鑳戒粙缁嶅瘑搴︼紝涓嶇户缁噸鏋?Dashboard銆佹垚闀夸腑蹇冦€佷細鍛樸€丱ffer 鎴栧叾瀹冮〉闈€?
## 鐢ㄦ埛绔椤?UI 閲嶆瀯涓庡姛鑳戒粙缁嶈ˉ鍏紙2026-05-24锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚缂栫爜鍜岄獙璇侊紝绛夊緟浜哄伐楠屾敹銆?- 宸插畬鎴愬唴瀹癸細棣栭〉棣栧睆閲嶆瀯涓衡€滀粠绠€鍘嗗埌 Offer 鐨勬眰鑱屽伐浣滃彴鈥濊〃杈撅紱鏂板鍔熻兘鍦板浘锛岃鐩栫畝鍘嗚瘖鏂€佺畝鍘嗕紭鍖栥€佸矖浣嶅尮閰嶃€佹ā鎷熼潰璇曘€侀潰璇曞鐩樸€佺畝鍘嗘ā鏉垮簱銆佹垚闀夸腑蹇冦€丱ffer 杈呭姪鍜岀ぞ鍖轰氦娴侊紱鏂板姹傝亴浣跨敤璺緞鍖猴紱棣栭〉鎸夐挳銆佹爣绛俱€侀鏋跺睆灞€閮ㄨ縼绉讳负 Naive UI锛涗繚鐣欑幇鏈?`FeatureIcon` 鍥炬爣浣撶郴銆?- 浜や簰杈圭晫锛氭湰杞彧澶勭悊棣栭〉 `/`锛屼笉淇敼 `/admin/**`銆丄PI銆佽矾鐢便€佹暟鎹簱銆佺櫥褰曢壌鏉冩垨鍏跺畠鐢ㄦ埛绔〉闈紱鍔熻兘鍗＄墖浠呰烦杞凡鏈夎矾鐢憋紝鏈櫥褰曠户缁繘鍏?`/login`銆?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 2 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛氭湰杞笉娑夊強鍚庣銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_USER_HOME_UI_REFACTOR_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧瀹屾垚棣栭〉 UI 閲嶆瀯鍜屽姛鑳戒粙缁嶈ˉ鍏紝涓嶇户缁帹杩?Dashboard銆佹垚闀夸腑蹇冦€佷細鍛樸€丱ffer 鎴栧叾瀹冮〉闈€?
## 鐢ㄦ埛绔?UI 閲嶆瀯鏂规鏂囨。锛?026-05-24锛?- 褰撳墠闃舵锛氱敤鎴风 UI 閲嶆瀯鏂规鏂囨。宸插垱寤猴紝绛夊緟瀹炵幇銆?- 宸插畬鎴愬唴瀹癸細鏂板 `frontend/tasks/TASK_USER_UI_REFACTOR_NAIVE_UI_FRONTEND.md`锛屽浐鍖栨鐧藉簳鑹层€佽川鎰熸彁鍗囥€佸姩鎬佺編鍖栥€佷繚鐣欑幇鏈?`FeatureIcon`銆丯aive UI 浼樺厛杩佺Щ涓?Element Plus 楂橀闄╃粍浠朵繚鐣欑瓥鐣ャ€?- 浜や簰杈圭晫锛氭湰杞彧鐢熸垚鏂规鏂囨。锛屼笉淇敼鐢ㄦ埛绔繍琛屼唬鐮侊紝涓嶄慨鏀?`/admin/**`銆丄PI銆佽矾鐢便€佹暟鎹簱鎴栦笟鍔℃祦绋嬨€?- 鍓嶇楠岃瘉锛氭枃妗ｅ垱寤哄悗涓嶈繍琛屽墠绔崟娴嬶紱宸叉墽琛屾枃浠跺瓨鍦ㄦ€т笌 stage 璁板綍妫€鏌ャ€?- 鍚庣楠岃瘉锛氭湰杞笉娑夊強鍚庣銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_USER_UI_REFACTOR_NAIVE_UI_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧瀹屾垚鐢ㄦ埛绔?UI 閲嶆瀯鏂囨。钀藉湴锛屼笉寮€濮嬩唬鐮侀噸鏋勩€?
## 妯℃嫙闈㈣瘯璇煶閫氳瘽鍥炬爣鏀惧ぇ涓庤儗鏅急鍖栵紙2026-05-24锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚缂栫爜鍜岄獙璇侊紝绛夊緟浜哄伐楠屾敹銆?- 宸插畬鎴愬唴瀹癸細璇煶闈㈣瘯灞曞紑鎬佷富澶村儚鏀惧ぇ涓哄搷搴斿紡灏哄锛岀Щ鍔ㄧ鍚屾鏀惧ぇ骞朵繚鐣欎綆楂樺害鍏滃簳锛涢《閮ㄦ祬姗欒儗鏅繘涓€姝ユ贰鍖栧苟璋冩暣楂樺害锛岃澶村儚鏇寸獊鍑猴紱鍦嗗舰璇煶鎺у埗鎸夐挳鍐呴儴涓氬姟鍥炬爣鏀惧ぇ骞跺眳涓€?- 浜や簰杈圭晫锛氭湰杞彧澶勭悊璇煶闈㈣瘯閫氳瘽鍥炬爣灏哄銆佸眳涓拰鑳屾櫙寮卞寲锛屼笉淇敼璇煶璇嗗埆銆乀TS 鎾姤銆丼SE 鍥炲銆侀潤闊炽€佹寕鏂€佹姌鍙犳垨鍚庣鎺ュ彛銆?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 18 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛氭湰杞笉娑夊強鍚庣銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧淇褰撳墠鎴浘鍙嶉锛屼笉缁х画鎵╁睍鍏朵粬璇煶鑳藉姏鎴栭〉闈㈠姛鑳姐€?
## 鐢ㄦ埛绔叏閲忓浘鏍囨浛鎹紙2026-05-24锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚缂栫爜鍜岄獙璇侊紝绛夊緟浜哄伐楠屾敹銆?- 宸插畬鎴愬唴瀹癸細淇濈暀 `frontend/app/src/assets/feature-icons/old/` 鏃у浘鏍囦笌鏃?key锛岃ˉ榻?`frontend/app/src/assets/feature-icons/new/` 褰撳墠鍏ㄩ儴 PNG 鏄犲皠锛涚敤鎴风鍏叡鍖哄煙銆佷富娴佺▼椤甸潰銆侀€氱煡銆佺櫥褰曘€佺畝鍘嗐€侀潰璇曘€佹ā鏉裤€佺ぞ鍖恒€佹垚闀裤€丱ffer銆佷細鍛樸€佽缃€佺増鏈棩蹇椼€佺┖鐘舵€併€丱nboarding 涓庣ぞ鍖哄浘鐗囬瑙堢瓑宸茬粺涓€鎺ュ叆 `FeatureIcon`銆?- 浜や簰杈圭晫锛氫粎鏇挎崲鏈夋槑纭?`new/` 瀵瑰簲璧勬簮鐨勪笟鍔″睍绀哄浘鏍囧拰鎿嶄綔鍥炬爣锛涙暟鎹彲瑙嗗寲 SVG銆佽繘搴︾幆銆侀浄杈惧浘銆丄I loading 杞ㄩ亾銆佹棤鏄庣‘璇箟瀵瑰簲鐨勭粏绮掑害瀛楁/鏃堕棿鍥炬爣淇濈暀鐜扮姸锛涚鐞嗙 `/admin/**` 涓嶅鐞嗐€?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/utils/featureIcons.test.js src/__tests__/components/OnboardingTaskCard.test.js src/__tests__/views/SettingsView.test.js src/__tests__/utils/notificationMeta.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 32 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛氭湰杞笉娑夊強鍚庣銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_USER_FEATURE_ICONS_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧瀹屾垚鐢ㄦ埛绔叏閲忓浘鏍囨浛鎹紝涓嶇户缁墿灞曠鐞嗙銆佷笉鏂板涓氬姟鑳藉姏銆佷笉淇敼璺敱/API/鏁版嵁缁撴瀯銆?
## 鐢ㄦ埛绔姛鑳藉浘鏍囪鍓笌鎺ュ叆锛?026-05-24锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚缂栫爜鍜岄獙璇侊紝绛夊緟浜哄伐楠屾敹銆?- 宸插畬鎴愬唴瀹癸細浠?`frontend/app/src/assets/icons.png` 鎸?6 鍒?x 4 琛岃鍓嚭鍓?22 涓熀纭€涓氬姟鍥炬爣锛屽苟鎸夊綋鍓嶆渶鏂拌祫婧愯ˉ鍏?`new/` 鐩綍涓?20 涓墿灞曚笟鍔″浘鏍囷紝鏂板 `FeatureIcon` 鍜岀粺涓€鏄犲皠锛涢《閮ㄥ鑸€佺Щ鍔ㄧ瀵艰埅銆佸ご鍍忚彍鍗曘€侀€氱煡绫诲瀷銆佽缃垎缁勩€佹柊鎵嬪紩瀵笺€佷細鍛橀〉銆佺畝鍘?闈㈣瘯绌虹姸鎬併€佸揩閫熶笂鎵嬩换鍔″崱銆佹ā鏉跨紪杈戝櫒椤堕儴鍜岄潰璇曟埧闂存爣棰?绌虹姸鎬佸凡鎺ュ叆鏈湴涓氬姟鍥炬爣銆?- 浜や簰杈圭晫锛氬彧鏇挎崲鎴栬ˉ鍏呬笟鍔″睍绀哄浘鏍囷紱涓婚鍒囨崲銆佽繑鍥炪€佸叧闂€侀害鍏嬮銆佹寕鏂€佸彂閫併€佸睍寮€绛夌郴缁熸搷浣滃浘鏍囦繚鎸佸師鐘讹紱涓嶆柊澧炴帴鍙ｃ€佷笉淇敼璺敱銆佷笉璋冩暣涓氬姟娴佺▼銆?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/utils/featureIcons.test.js src/__tests__/components/OnboardingTaskCard.test.js src/__tests__/views/SettingsView.test.js src/__tests__/utils/notificationMeta.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 32 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛氭湰杞笉娑夊強鍚庣銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_USER_FEATURE_ICONS_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧瀹屾垚鐢ㄦ埛绔姛鑳藉浘鏍囨帴鍏ワ紝涓嶇户缁墿灞?P2 椤甸潰鎴栧叾瀹冩柊鍔熻兘銆?
## 绀惧尯涓庝釜浜哄姩鎬佽櫄鎷熸粴鍔ㄦ帴鍏ワ紙2026-05-23锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚缂栫爜鍜岄獙璇侊紝绛夊緟浜哄伐楠屾敹銆?- 宸插畬鎴愬唴瀹癸細寮曞叆 `vue-virtual-scroller` 澶勭悊涓汉鍔ㄦ€佷腑蹇冣€滄垜鐨勫笘瀛愩€佺偣璧炶繃銆佹敹钘忋€佽瘎璁鸿繃銆佹敹鍒扮殑鐐硅禐銆佹敹鍒扮殑璇勮銆佹敹鍒扮殑鍥炲銆佹敹鍒扮殑鏀惰棌鈥濆垪琛紱绀惧尯棣栭〉鏈€鍒濇帴鍏?`DynamicScroller` 鍚庡嚭鐜拌Е搴曡拷鍔犲悗鍚戜笂鍥炴粴绌虹櫧锛屽洜姝ら椤靛凡鍥為€€涓烘櫘閫氬垪琛ㄦ覆鏌擄紝鍚屾椂鎶婅Е搴?`IntersectionObserver` 缁戝畾鍒?`.layout-content` 鐪熷疄婊氬姩瀹瑰櫒銆傛敹鍒扮偣璧?鏀惰棌绛夋棤鐙珛璁板綍 ID 鐨勫垪琛ㄥ湪鍓嶇琛ュ厖 `virtualKey`锛屼笉鏀瑰彉鍚庣鍝嶅簲缁撴瀯銆?- 浜や簰杈圭晫锛氫繚鐣欑ぞ鍖洪椤靛師鏈夎Е搴曞姞杞芥洿澶氾紱涓汉鍔ㄦ€佷腑蹇冪户缁娇鐢ㄦ寜閽紡鍔犺浇鏇村锛涙湰杞彧淇棣栭〉鍥炴粴绌虹櫧锛屼笉鎵╁睍绀惧尯涓氬姟鑳藉姏銆?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/community/CommunityView.test.js src/__tests__/components/community/ImageGrid.test.js src/__tests__/views/community/MyActivity.test.js` 閫氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣鍏宠仈锛氫釜浜哄姩鎬佷腑蹇冣€滆瘎璁鸿繃鐨勫笘瀛愨€濊繍琛屾椂鎶ラ敊淇瑙?`tasks/fixes/TASK_COMMUNITY_ACTIVITY_VIRTUAL_SCROLL_AND_DTO_BUILDER_2026_05_23_BACKEND.md`銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_COMMUNITY_ACTIVITY_VIRTUAL_SCROLL_2026_05_23_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊绀惧尯鍒楄〃铏氭嫙婊氬姩鍜岄椤靛洖婊氱┖鐧介棶棰橈紝涓嶇户缁墿灞曠ぞ鍖轰笟鍔¤兘鍔涖€?
## 鐧诲綍鎬佽繃鏈熼壌鏉冭竟鐣屼笌绠＄悊绔敊璇彁绀哄幓閲嶏紙2026-05-23锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚缂栫爜鍜岄獙璇侊紝绛夊緟浜哄伐楠屾敹銆?- 宸插畬鎴愬唴瀹癸細绠＄悊绔櫘閫氫笟鍔￠敊璇拰闈?401 HTTP 閿欒涓嶅啀鐢?`adminRequest` 鐩存帴寮圭獥锛岄伩鍏嶉〉闈?catch 鍐嶆鎻愮ず瀵艰嚧閲嶅寮圭獥锛?01 浼氳瘽澶辨晥缁х画缁熶竴娓呯悊绠＄悊绔櫥褰曟€併€佽烦杞櫥褰曢〉骞舵彁绀轰竴娆★紱`showAdminError(...)` 澧炲姞鐭椂闂寸浉鍚屾枃妗堝幓閲嶏紱`SettingsView.test.js` 涓や釜宸茬煡鎱㈢敤渚嬭ˉ鍏呭崟鐢ㄤ緥瓒呮椂锛屼繚璇佸畬鏁存祴璇曞浠剁ǔ瀹氳繍琛屻€傚悗绔?`/api/auth/**` 閴存潈杈圭晫淇瑙?`tasks/fixes/TASK_AUTH_TOKEN_EXPIRED_AND_ADMIN_FEEDBACK_2026_05_23_BACKEND.md`銆?- 浜や簰杈圭晫锛氭湰杞彧淇閿欒鎻愮ず閲嶅鍜屾祴璇曠ǔ瀹氭€э紝涓嶆柊澧炵鐞嗙椤甸潰鑳藉姏锛屼笉淇敼鎺ュ彛 URL 鎴栨暟鎹粨鏋勩€?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/utils/adminFeedback.test.js src/__tests__/utils/adminRequest.test.js` 閫氳繃锛? 涓祴璇曢€氳繃锛沗npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 閫氳繃锛?5 涓祴璇曢€氳繃锛沗npm.cmd test` 閫氳繃锛?0 涓祴璇曟枃浠?/ 246 涓祴璇曢€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_ADMIN_FEEDBACK_DEDUPE_2026_05_23_FRONTEND.md`銆乣tasks/fixes/TASK_AUTH_TOKEN_EXPIRED_AND_ADMIN_FEEDBACK_2026_05_23_BACKEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊绠＄悊绔噸澶嶉敊璇彁绀哄拰鐧诲綍鎬佽繃鏈熷紓甯革紝涓嶇户缁紑鍙戜笅涓€闃舵鍔熻兘銆?
## 鎴愰暱閰嶇疆鎺ュ叆鐢ㄦ埛绔垚闀夸腑蹇冿紙2026-05-22锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚缂栫爜鍜岄獙璇侊紝绛夊緟浜哄伐楠屾敹銆?- 宸插畬鎴愬唴瀹癸細鐢ㄦ埛绔釜浜烘垚闀夸腑蹇冭鍙栨垚闀挎瑙堟帴鍙ｆ柊澧炵殑 `growthConfig`锛屽湪鏈夊悗鍙伴厤缃椂灞曠ず鈥滄垚闀挎縺鍔扁€濆拰鈥滄垚闀块噷绋嬬鈥濓紱鏃犻厤缃椂涓嶅崰浣嶏紱鍚庣鎺ュ叆鍜岀紦瀛樻竻鐞嗚 `tasks/TASK_56_GROWTH_CONFIG_INTEGRATION_BACKEND.md`銆?- 浜や簰杈圭晫锛氭湰杞彧鎺ラ€氱鐞嗙鎴愰暱閰嶇疆鍒扮敤鎴风灞曠ず锛屼笉鏂板鎴愬氨瑙勫垯璁＄畻銆佺Н鍒嗐€佸窘绔犮€佽揪鎴愮姸鎬佹垨绠＄悊绔〃鍗曞瓧娈点€?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/GrowthCenterView.test.js` 閫氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛歚mvn.cmd -q "-Dtest=GrowthServiceImplTest,RedisSerializationTest,AdminGrowthConfigControllerTest" test` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_56_GROWTH_CONFIG_INTEGRATION_FRONTEND.md`銆乣tasks/TASK_56_GROWTH_CONFIG_INTEGRATION_BACKEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧瀹屾垚鎴愰暱閰嶇疆灞曠ず闂幆锛屼笉缁х画鎵╁睍鎴愰暱涓績鍏跺畠鑳藉姏銆?
## 绀惧尯 Pull 浠ｇ爜瀹℃煡淇锛?026-05-22锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚缂栫爜鍜屽畬鏁撮獙璇侊紝绛夊緟浜哄伐楠屾敹銆?- 宸插畬鎴愬唴瀹癸細淇绀惧尯鍥剧墖棰勮娴嬭瘯鐜鍏抽棴娈嬬暀涓?Escape patch 寮傚父锛涜缃腑蹇冭处鍙锋敞閿€椤电鎭㈠椋庨櫓鏂囨銆佸喎闈欐湡绂佺敤銆佺‘璁ゆ彁浜ゆ祦绋嬪拰鍙栨秷纭鍏滃簳锛涘悗绔畨鍏ㄨ竟鐣屻€佷笂浼犻潤鎬佽祫婧愬叕寮€鑼冨洿銆侀檺娴侀摼璺€佺ぞ鍖轰簰鍔?鏈缁熻銆佽瘎璁鸿繃甯栧瓙鍒嗛〉銆佽瘎璁烘牎楠屽拰涓婁紶鏂囦欢鍚嶆牎楠屼慨澶嶈 `tasks/fixes/TASK_CODE_REVIEW_COMMUNITY_PULL_2026_05_22_FIX_BACKEND.md`銆?- 浜や簰杈圭晫锛氭湰杞彧澶勭悊 `docs/CODE_REVIEW_COMMUNITY_PULL_2026_05_22.md` 涓樆鏂」锛屼笉鏂板绀惧尯涓氬姟鑳藉姏銆?- 鍓嶇楠岃瘉锛氬畾鍚戞祴璇?`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 25 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃锛涘畬鏁存祴璇?`npm.cmd test` 閫氳繃锛?8 涓祴璇曟枃浠?/ 242 涓敤渚嬮€氳繃銆?- 鍚庣楠岃瘉锛氬畾鍚戞祴璇?`mvn.cmd test "-Dtest=CommunityServicePostQueryDeleteTest,SecurityConfigTest,CommunityServiceInteractionTest"` 閫氳繃锛?2 涓祴璇曢€氳繃锛涘畬鏁存祴璇?`mvn.cmd test` 閫氳繃锛?11 涓祴璇曢€氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_CODE_REVIEW_COMMUNITY_PULL_2026_05_22_FIX_FRONTEND.md`銆乣tasks/fixes/TASK_CODE_REVIEW_COMMUNITY_PULL_2026_05_22_FIX_BACKEND.md`銆?- 鍋滄璇存槑锛氬畬鎴愬綋鍓嶅鏌ヤ慨澶嶅悗鍋滄锛屼笉缁х画寮€鍙戜笅涓€闃舵绀惧尯鍔熻兘銆?
## HIGH + MEDIUM 瀹℃煡淇锛?026-05-21锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細DOCX 瀵煎嚭涓嬭浇瑙﹀彂鏀逛负 `try/finally` 閲婃斁 Object URL锛岃ˉ鍏?`link.click()` 鎶涢敊鏃朵粛璋冪敤 `URL.revokeObjectURL` 鐨勫崟鍏冩祴璇曪紱鍚庣闆疯揪銆佺紦瀛樸€佽瘎鍒嗗綊涓€鍖栫瓑淇瑙?`runtime/STATE.md` 鍜?`tasks/TASK_54_INTERVIEW_DIMENSION_RADAR_BACKEND.md`銆?- 浜や簰杈圭晫锛氭湰杞彧澶勭悊瀹℃煡鎶ュ憡 HIGH + MEDIUM 椤癸紝涓嶅鐞?LOW 浼樺寲锛屼笉鏂板鍓嶇鐣岄潰鑳藉姏銆?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/utils/resumeDocxExport.test.js` 閫氳繃锛?4 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛歚mvn.cmd test` 閫氳繃锛?57 涓敤渚嬮€氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_53_RESUME_EXPORT_DOCX.md`銆乣tasks/TASK_54_INTERVIEW_DIMENSION_RADAR_BACKEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧淇褰撳墠瀹℃煡闂锛屼笉缁х画鎵╁睍 DOCX銆侀浄杈炬垨鎴愰暱涓績鍏跺畠鑳藉姏銆?
## 绗簩杞鏌ュ姟瀹炰慨澶嶏紙2026-05-21锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細DOCX 瀵煎嚭閾捐矾鍦?detached DOM 鍐欏叆鍓嶅姞鍏?DOMPurify 娑堟瘨锛涚畝鍘嗙粨鏋滈〉鍥剧墖瀵煎嚭鏀逛负 `try/finally` 閲婃斁 Object URL锛涙垚闀夸腑蹇冪Щ闄ゆ湰杞柊澧炵殑鐢熶骇 `console.error`锛涗换鍔″崱鐗囩幆褰㈣繘搴﹀湪 `totalCount<=0` 鏃朵笉鍐嶄骇鐢?`NaN`锛涚浉鍏冲洖褰掓祴璇曞凡琛ラ綈銆?- 浜や簰杈圭晫锛氭湰杞彧澶勭悊绗簩杞鏌ヤ腑宸茬‘璁や笖浣庨闄╃殑闂锛屼笉鎷?`InterviewService`锛屼笉鏂板鍓嶇鐣岄潰鑳藉姏銆?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/utils/resumeDocxExport.test.js src/__tests__/views/ResumeResultView.test.js src/__tests__/components/OnboardingTaskCard.test.js` 閫氳繃锛?8 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛氳 `runtime/STATE.md` 涓?`tasks/TASK_54_INTERVIEW_DIMENSION_RADAR_BACKEND.md`锛宍mvn.cmd test` 閫氳繃锛?59 涓敤渚嬮€氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_53_RESUME_EXPORT_DOCX.md`銆乣tasks/TASK_54_INTERVIEW_DIMENSION_RADAR_BACKEND.md`銆乣runtime/STATE.md`銆?- 鍋滄璇存槑锛氭湰杞彧淇绗簩杞鏌ラ棶棰橈紝涓嶇户缁墿灞?DOCX銆侀浄杈炬垨鎴愰暱涓績鍏跺畠鑳藉姏銆?
## Pull 鍚庝唬鐮佸鏌ュ墠绔慨澶嶏紙2026-05-20锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細PDF 涓嬭浇閾捐矾浠?URL query token 鏀逛负 Axios blob 涓嬭浇骞堕€氳繃 `Authorization` 璇锋眰澶磋璇侊紝閬垮厤鐧诲綍 token 杩涘叆娴忚鍣ㄥ巻鍙层€佷唬鐞嗘棩蹇楁垨 Referer锛涚畝鍘嗙紪杈戜繚瀛樻垚鍔熷悗鍚屾鏈湴 `documentJson` / `editedPlainText` 骞惰皟鐢ㄦā鏉?`markClean()`锛岄伩鍏嶄繚瀛樺悗绂诲紑椤甸潰浠嶆彁绀衡€滄湁鏈繚瀛樼殑缂栬緫鍐呭鈥濄€?- 浜や簰杈圭晫锛氭湰杞彧淇 pull 鍚庝唬鐮佸鏌ュ彂鐜扮殑 PDF 涓嬭浇璁よ瘉鏆撮湶鍜屼繚瀛樺悗 dirty 鐘舵€侀棶棰橈紝涓嶆敼 PDF 鐢熸垚娴佺▼銆佷笉鏂板涓嬭浇绁ㄦ嵁銆佷笉鎵╁睍绠€鍘嗙紪杈戣兘鍔涖€?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/api/resumePdf.test.js` 閫氳繃锛? 涓祴璇曢€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛氳 `runtime/STATE.md` 鐨勨€淧ull 鍚庝唬鐮佸鏌ュ畨鍏ㄤ慨澶嶁€濄€?- 鍋滄璇存槑锛氭湰杞彧澶勭悊褰撳墠瀹℃煡闂锛屼笉缁х画鎵╁睍鍏跺畠鍓嶇椤甸潰鎴栧鍑哄姛鑳姐€?
## 璁剧疆涓績娴忚鍣?voice 涓嬫媺绉诲姩绔€傞厤锛?026-05-20锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細璁剧疆涓績璇煶閫氳瘽鍋忓ソ涓紝娴忚鍣?voice 閫夋嫨鍣ㄥ惎鐢ㄧ瓑瀹藉脊灞傚苟澧炲姞涓撶敤 popper 鏍峰紡锛涚Щ鍔ㄧ寮瑰眰瀹藉害闄愬埗鍦ㄨ鍙ｅ畨鍏ㄨ寖鍥村唴锛岄暱 voice 鍚嶇О鐪佺暐灞曠ず骞跺湪寮瑰眰鍐呮粴鍔紝閬垮厤鐐瑰嚮涓嬫媺鍚庢拺鍧忕Щ鍔ㄧ甯冨眬銆傝瘯鍚寜閽敼涓?SVG 鍠囧彮鍥炬爣鎸夐挳锛屽苟琛ラ綈 hover銆乤ctive銆乫ocus銆佺鐢ㄥ拰 reduced-motion 鐘舵€併€?- 浜や簰杈圭晫锛氭湰杞彧淇鐢ㄦ埛璁剧疆涓績 voice 涓嬫媺鍝嶅簲寮忓拰璇曞惉鎸夐挳瑙嗚浜や簰锛屼笉淇敼璇煶鍋忓ソ瀛樺偍瀛楁銆乀TS 鎾姤閫昏緫銆侀潰璇曚細璇濋〉銆佸悗绔帴鍙ｆ垨鏁版嵁搴撶粨鏋勩€?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 24 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_40_SETTINGS_CENTER_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊褰撳墠绉诲姩绔竷灞€闂涓庤瘯鍚寜閽浘鏍囧寲锛屼笉缁х画鎵╁睍鍏朵粬璁剧疆涓績鑳藉姏銆?
## 绠＄悊绔?AI 寮曟搸杩為€氭祴璇曪紙2026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細绠＄悊绔?AI 寮曟搸鏂板/缂栬緫寮圭獥鏂板鈥滄祴璇曡繛閫氭€р€濇寜閽紱鐐瑰嚮鍚庝娇鐢ㄥ綋鍓嶈〃鍗曢厤缃皟鐢ㄥ悗绔繛閫氭祴璇曟帴鍙ｏ紱鏂板鎬佷娇鐢ㄥ綋鍓嶈緭鍏?API Key锛岀紪杈戞€佹湭杈撳叆鏂?Key 鏃剁敱鍚庣浣跨敤宸蹭繚瀛樺瘑閽ワ紱寮圭獥鍐呭睍绀烘垚鍔?澶辫触銆佽€楁椂銆佸搷搴旀憳瑕佹垨澶辫触鍘熷洜锛涘叧閿厤缃彉鏇村悗娓呯┖鏃ф祴璇曠粨鏋滐紝閬垮厤璇銆?- 浜や簰杈圭晫锛氭湰杞彧鏂板鍗曟潯閰嶇疆鐨勬墜鍔ㄨ繛閫氭祴璇曪紝涓嶅仛鎵归噺娴嬭瘯銆佷笉淇濆瓨娴嬭瘯鍘嗗彶銆佷笉鑷姩鍚敤鎴栦慨澶嶉厤缃€?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 閫氳繃锛? 涓祴璇曢€氳繃锛沗npm.cmd test` 閫氳繃锛?7 涓祴璇曟枃浠躲€?56 涓祴璇曠敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛氳 `runtime/STATE.md` 涓?`tasks/TASK_51_ADMIN_AI_ENGINE_CONNECTIVITY_TEST_BACKEND.md`銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_51_ADMIN_AI_ENGINE_CONNECTIVITY_TEST_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊绠＄悊绔?AI 寮曟搸杩為€氭祴璇曪紝涓嶇户缁墿灞曚緵搴斿晢妯℃澘銆佹祴璇曞巻鍙层€佽嚜鍔ㄤ慨澶嶆垨鎵归噺娴嬭瘯鑳藉姏銆?
## 妯℃嫙闈㈣瘯寮€鍦虹櫧鎾姤鏈熼棿寮傛鏀堕煶鍙栨秷淇锛?026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細涓鸿闊宠瘑鍒?`start()` 澧炲姞鍚姩搴忓彿锛宍cancel()` 鍜屽惎鍔ㄦ湡 `stop()` 浼氫娇褰撳墠寮傛鍚姩澶辨晥锛涘綋楹﹀厠椋庢巿鏉冩垨闊抽噺鐩戞祴鍦ㄥ紑鍦虹櫧鎾姤鍓嶇殑鍙栨秷鍔ㄤ綔涔嬪悗鎵嶈繑鍥炴椂锛屼細绔嬪嵆娓呯悊濯掍綋璧勬簮骞堕€€鍑猴紝涓嶅啀缁х画鍒涘缓娴忚鍣ㄨ闊宠瘑鍒疄渚嬨€?- 浜や簰杈圭晫锛氭湰杞彧澶勭悊璇煶閫氳瘽寮€鍦虹櫧 TTS 鎾姤鏈熼棿 STT 寮傛鍚姩鏈彇娑堝鑷寸殑 `no-transcript` 闄嶇骇鍜屾挱鎶ヤ腑鏂棶棰橈紝涓嶄慨鏀瑰悗绔帴鍙ｃ€丼SE 鍥炲鎾姤绛栫暐銆佽闊抽€氳瘽 UI銆佷簯绔?STT/TTS 鎴栦細璇濇暟鎹粨鏋勩€?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 11 涓敤渚嬮€氳繃锛沗npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 17 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧淇褰撳墠寮€鍦虹櫧鎾姤涓庢敹闊冲彇娑堢珵鎬侊紝涓嶇户缁墿灞曞叾浠栬闊宠兘鍔涖€?
## 妯℃嫙闈㈣瘯璇煶閫氳瘽鎸夐挳涓庣Щ鍔ㄧ甯冨眬浼樺寲锛?026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細缁熶竴灞曞紑鎬佽闊抽€氳瘽娴眰鍦嗗舰鎸夐挳鐨勫楂樸€佷几缂╁熀鍑嗗拰鐩掓ā鍨嬶紝鏀剁揣妗岄潰绔姌鍙犻€氳瘽鏉＄殑闈㈡澘瀹藉害銆佸唴杈硅窛鍜屾寜閽昂瀵革紱绉诲姩绔姌鍙犻€氳瘽鏉℃寜閽敼涓?42px 鍦嗗舰鍥炬爣鎸夐挳锛屽噺灏戝鑱婂ぉ鐣岄潰鐨勫崰鐢紱涓衡€滃睍寮€鈥濇寜閽ˉ鍏呭浘鏍囷紱绉诲姩绔《閮ㄥ鑸繚鎸佷袱琛岀粨鏋勫苟澧炲姞鏍囬鐪佺暐銆佸彸渚ф搷浣滅瓑璺濇帓鍒楋紝閬垮厤绐勫睆鎸ゅ帇鍙樺舰銆?- 浜や簰杈圭晫锛氭湰杞彧淇璇煶閫氳瘽 UI 灏哄涓庡搷搴斿紡甯冨眬锛屼笉淇敼 STT/TTS銆丼SE 娑堟伅銆佽嚜鍔ㄥ彂閫併€侀潤闊炽€佹姌鍙?灞曞紑銆佹寕鏂垨缁撴潫闈㈣瘯涓氬姟閫昏緫銆?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 12 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊鎴浘鍙嶉鐨勬寜閽昂瀵镐笌绉诲姩绔竷灞€闂锛屼笉缁х画鎵╁睍鍏朵粬璇煶閫氳瘽鑳藉姏銆?

## 妯℃嫙闈㈣瘯璇煶閫氳瘽鎺у埗鎸夐挳灏哄缁熶竴锛?026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚缂栫爜锛岀瓑寰呴獙璇併€?- 宸插畬鎴愬唴瀹癸細鎶樺彔璇煶閫氳瘽鏉″唴鎵€鏈夋帶鍒舵寜閽粺涓€涓?`132px 脳 42px`锛岀Щ鍔ㄧ缁熶竴涓烘弧瀹?`44px` 楂橈紱鏂板鈥滃仠姝㈡敹鍚苟鍙戦€佲€濇寜閽笌鈥滈潤闊?/ 灞曞紑 / 鎸傛柇鈥濅繚鎸佸悓涓€瑙嗚灏哄銆?- 浜や簰杈圭晫锛氫粎淇璇煶閫氳瘽鎸夐挳瑙嗚灏哄锛屼笉淇敼閫氳瘽銆侀潤闊炽€佹寕鏂€佹墜鍔ㄥ彂閫併€丼SE 娑堟伅閾捐矾銆佸悗绔帴鍙ｆ垨浼氳瘽鏁版嵁缁撴瀯銆?- 鍓嶇楠岃瘉锛氬緟鎵ц瀹氬悜娴嬭瘯涓?`npm.cmd run build`銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊璇煶閫氳瘽鎺у埗鎸夐挳灏哄涓€鑷存€э紝涓嶇户缁墿灞曞叾浠栬闊宠兘鍔涖€?
## 妯℃嫙闈㈣瘯璇煶閫氳瘽鎵嬪姩鍋滄鏀跺惉骞跺彂閫侊紙2026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細璇煶閫氳瘽灞曞紑娴眰鍜屾姌鍙犲簳閮ㄩ€氳瘽鏉℃柊澧炩€滃仠姝㈡敹鍚苟鍙戦€佲€濇帶鍒讹紱鐢ㄦ埛璇嗗埆鍒版枃鏈悗鍙墜鍔ㄨ烦杩?3 绉掗潤闊崇瓑寰呭苟绔嬪嵆鎻愪氦褰撳墠鍥炵瓟锛涢潤闊抽敭缁х画鍙礋璐ｆ殏鍋?鎭㈠楹﹀厠椋庢敹闊筹紝涓嶅啀鎵挎媴鎻愪氦璇箟锛岄伩鍏嶉噸缃垨娣锋穯褰撳墠鏀跺惉鐘舵€併€?- 浜や簰杈圭晫锛氭病鏈夊彲鍙戦€佽瘑鍒枃鏈椂鍙粰鍑烘槑纭彁绀猴紝涓嶉潤闊炽€佷笉鎸傛柇銆佷笉娓呯┖閫氳瘽锛涙湰杞笉淇敼鍚庣鎺ュ彛銆丼SE 娑堟伅閾捐矾銆乀TS 鎾姤绛栫暐銆佷簯绔?STT/TTS 鎴栦細璇濇暟鎹粨鏋勩€?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 20 涓敤渚嬮€氳繃锛沗npm.cmd test` 閫氳繃锛?5 涓祴璇曟枃浠?/ 136 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊璇煶閫氳瘽鎵嬪姩鍋滄鏀跺惉骞跺彂閫侊紝涓嶇户缁墿灞曞叾浠栬闊宠兘鍔涖€?
## 妯℃嫙闈㈣瘯璇煶闈㈣瘯搴曢儴绌虹櫧鏀舵暃锛?026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細鏀舵暃璇煶闈㈣瘯鑱婂ぉ鍖轰笌搴曢儴閫氳瘽鏉′箣闂寸殑杩囧ぇ鐣欑櫧锛涚Щ闄ゅ浐瀹?`112px` 搴曢儴棰勭暀锛屽睍寮€閫氳瘽閬僵鏃舵敼涓?`40px`锛屾姌鍙犲簳閮ㄩ€氳瘽鏉℃椂鏀逛负 `16px`锛岀Щ鍔ㄧ缁熶竴闄嶄负 `12px`銆?- 浜や簰杈圭晫锛氫粎淇璇煶闈㈣瘯甯冨眬绌虹櫧锛屼笉淇敼鑱婂ぉ娑堟伅銆佽闊抽€氳瘽銆侀潤闊炽€佹寕鏂€丼SE 娑堟伅閾捐矾銆佸悗绔帴鍙ｆ垨浼氳瘽鏁版嵁缁撴瀯銆?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 7 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊鎴浘绾㈠湀鍖哄煙鐨勫簳閮ㄧ┖鐧斤紝涓嶇户缁墿灞曞叾浠栬闊宠兘鍔涖€?
## 妯℃嫙闈㈣瘯璇煶闈㈣瘯涓庢瘡棰樺弽棣堜簰鏂ワ紙2026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細鍏ュ彛椤电姝⑩€滆闊抽潰璇?+ 姣忛鍙嶉鈥濈粍鍚堬紱閫夋嫨璇煶闈㈣瘯鏃惰嫢褰撳墠涓烘瘡棰樺弽棣堬紝鑷姩鍒囨崲涓洪潰瀹屽鐩橈紱璇煶闈㈣瘯鐘舵€佷笅鍐嶆鐐瑰嚮姣忛鍙嶉涓嶇敓鏁堝苟鎻愮ず鍘熷洜锛涘垱寤轰細璇?payload 瀵硅闊抽潰璇曞浐瀹氭彁浜?`feedbackMode=after_interview`锛岄伩鍏嶅紓甯哥姸鎬佽繘鍏ヤ笉鍏煎缁勫悎銆?- 浜や簰杈圭晫锛氭湰杞彧澶勭悊鍏ュ彛閰嶇疆浜掓枼锛屼笉淇敼鍚庣鎺ュ彛銆丼SE 娑堟伅閾捐矾銆乀TS 鎾姤绛栫暐銆佹姤鍛婇〉鍥炴斁缁撴瀯鎴栬闊抽€氳瘽 UI 甯冨眬銆?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/InterviewEntryView.test.js src/__tests__/views/InterviewSessionView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 14 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊璇煶闈㈣瘯涓庢瘡棰樺弽棣堜笉鍏煎闂锛屼笉缁х画鎵╁睍鍏朵粬鍙嶉鑳藉姏銆?
## 妯℃嫙闈㈣瘯璇煶閫氳瘽鎸傛柇鎸夐挳灏哄缁熶竴锛?026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細绉婚櫎璇煶閫氳瘽鎸傛柇鎸夐挳鍗曠嫭璁剧疆鐨勬闈㈢ `62px` 鍜岀Щ鍔ㄧ `64px` 瀹介珮锛屼娇鍏朵笌鎶樺彔銆侀害鍏嬮鎸夐挳鍏辩敤缁熶竴鎸夐挳灏哄锛涗繚鐣欑孩鑹插嵄闄╂搷浣滆瑙夈€乭over 鐘舵€佸拰鐢佃瘽鍥炬爣鏃嬭浆鏁堟灉銆?- 浜や簰杈圭晫锛氫粎淇璇煶閫氳瘽 UI 灏哄涓€鑷存€э紝涓嶄慨鏀?STT/TTS銆丼SE 娑堟伅銆侀潤闊炽€佹姌鍙犮€佹寕鏂笟鍔￠€昏緫鎴栧悗绔帴鍙ｃ€?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 7 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊鎸傛柇鎸夐挳灏哄缁熶竴锛屼笉缁х画鎵╁睍鍏朵粬璇煶閫氳瘽鑳藉姏銆?
## 妯℃嫙闈㈣瘯璇煶璇嗗埆涓嶅彲鐢ㄦ槑纭檷绾ф彁绀猴紙2026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細娴忚鍣ㄤ笉鏀寔璇煶璇嗗埆銆佹祻瑙堝櫒 Web Speech 鏈嶅姟 `network` 涓嶅彲鐢ㄣ€侀害鍏嬮鏉冮檺鎷掔粷銆佹湭妫€娴嬪埌楹﹀厠椋庛€佸惎鍔ㄨ闊宠瘑鍒け璐ユ椂锛岀粺涓€缁欏嚭鈥滃凡闄嶇骇涓烘墜鍔ㄨ緭鍏モ€濈殑鐢ㄦ埛鍙鎻愮ず锛涜ˉ鍏?`no-speech`銆侀害鍏嬮鏈夊０闊充絾鏃犺瘑鍒枃鏈殑 `no-transcript`銆佽瘑鍒粨鏉熸棤鏂囨湰鐨?`end-without-result` 鎻愮ず锛岄伩鍏嶈闊宠緭鍏ユ棤鏂囧瓧涓旀棤鎶ラ敊锛涜闊抽€氳瘽鍚姩鍓嶅彂鐜?STT/TTS 涓嶆敮鎸佹椂涓嶈繘鍏ラ€氳瘽鐘舵€侊紝骞舵彁绀虹户缁墜鍔ㄨ緭鍏ャ€?- 浜や簰杈圭晫锛氭湰杞彧琛ュ厖鏄庣‘闄嶇骇鎻愮ず锛屼笉鏂板鑷姩閲嶈瘯銆佽嚜鍔ㄩ噸杩炪€佷簯绔?ASR銆佸疄鏃堕煶瑙嗛鎴栦細璇濇暟鎹粨鏋勫彉鏇淬€?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 26 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊褰撳墠璇煶璇嗗埆涓嶅彲鐢ㄦ椂鐨勬彁绀洪檷绾э紝涓嶇户缁墿灞曞叾浠栬闊宠兘鍔涖€?
## 妯℃嫙闈㈣瘯鏂囧瓧璇煶杈撳叆 network 閿欒鏄惧紡鎻愮ず锛?026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細鏍规嵁鏈€鏂板弽棣堣鐩栦笂涓€杞?`network` 闈欓粯鎭㈠绛栫暐锛屽皢娴忚鍣?Web Speech 杩斿洖鐨?`network` 浠庨潤榛樺彲鎭㈠涓柇璋冩暣涓虹敤鎴峰彲瑙侀敊璇紝閿欒鏂囨涓衡€滃綋鍓嶇綉缁滆繛鎺ュ紓甯革紝璇煶璇嗗埆鏈嶅姟鏆傛椂涓嶅彲鐢紝璇锋鏌ョ綉缁滃悗閲嶈瘯鈥濓紱鏂囧瓧鑱婂ぉ璇煶杈撳叆鍜岃闊抽€氳瘽澶嶇敤鍚屼竴 `useSpeechToText`锛屽洜姝ら兘浼氭敹鍒拌閿欒鎻愮ず銆?- 淇濈暀杈圭晫锛歚no-speech`銆乣aborted` 浠嶆寜鍙仮澶嶄腑鏂竻鐞嗭紝涓嶆彁绀轰负缃戠粶閿欒锛涙湰杞笉淇敼鍚庣鎺ュ彛銆丼SE 娑堟伅閾捐矾銆乀TS 鎾姤閾捐矾銆乁I 甯冨眬鎴栦細璇濇暟鎹粨鏋勩€?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 15 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊 `network` 閿欒鏄惧紡鎻愮ず锛屼笉缁х画鎵╁睍浜戠璇煶鏈嶅姟銆佸疄鏃堕煶瑙嗛鎴栧叾浠栬闊宠兘鍔涖€?
## 妯℃嫙闈㈣瘯璇煶璇嗗埆 network 閿欒鎭㈠锛?026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細灏嗘祻瑙堝櫒 Web Speech 杩斿洖鐨?`network` 閿欒浠庤嚧鍛介敊璇皟鏁翠负鍙仮澶嶈瘑鍒腑鏂紱閬囧埌 `network`銆乣no-speech`銆乣aborted` 鏃朵笉鍐嶅脊鍑衡€滆闊宠瘑鍒敊璇€濆苟鎸傛柇閫氳瘽锛岃€屾槸娓呯悊褰撳墠璇嗗埆瀹炰緥鍚庣敱閫氳瘽灞傝嚜鍔ㄦ仮澶嶇洃鍚€?- 浜や簰杈圭晫锛氫粎淇鍓嶇 Web Speech 閿欒澶勭悊锛屼笉淇敼鍚庣鎺ュ彛銆丼SE 娑堟伅閾捐矾銆乀TS 鎾姤閾捐矾銆佷簯绔闊虫湇鍔℃垨浼氳瘽鏁版嵁缁撴瀯銆?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 14 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊 `network` 閿欒璇姤涓庤嚜鍔ㄦ仮澶嶏紝涓嶇户缁墿灞曞疄鏃堕煶瑙嗛銆佷簯绔闊虫湇鍔℃垨閫氳瘽鏁版嵁瀛樺偍銆?
## 妯℃嫙闈㈣瘯璇煶璇嗗埆鐏垫晱搴︿慨姝ｏ紙2026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細璇煶閫氳瘽 STT 澧炲姞楹﹀厠椋庨煶閲忔椿鍔ㄦ娴嬶紝浣跨敤 Web Audio 瀹炴椂鍒ゆ柇鏄惁鏈変汉澹帮紱鑷姩鍙戦€佷粠 5 绉掗潤闊宠皟鏁翠负 3 绉掗潤闊筹紱鏈変汉澹版椿鍔ㄦ椂鍒锋柊闈欓煶璁℃椂锛岄伩鍏嶇敤鎴蜂粛鍦ㄨ璇濇椂璇彂閫侊紱娴忚鍣?Web Speech 鐩戝惉缁撴潫鍚庡湪閫氳瘽涓嚜鍔ㄦ仮澶嶇洃鍚€?- 浜や簰杈圭晫锛氫粎浼樺寲鍓嶇娴忚鍣ㄨ闊宠瘑鍒伒鏁忓害銆侀潤闊宠鏃跺拰鐩戝惉鎭㈠锛屼笉鎺ュ叆浜戠 STT锛屼笉淇敼鍚庣鎺ュ彛銆丼SE 娑堟伅閾捐矾銆乀TS 鎾姤閾捐矾鎴栦細璇濇暟鎹粨鏋勩€?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 13 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊璇煶璇嗗埆鐏垫晱搴︿笌涓夌闈欓煶鑷姩鍙戦€侊紝涓嶇户缁墿灞曞疄鏃堕煶瑙嗛銆佷簯绔闊虫湇鍔℃垨閫氳瘽鏁版嵁瀛樺偍銆?
## 妯℃嫙闈㈣瘯璇煶閫氳瘽灞曠ず浼樺寲锛?026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細璇煶閫氳瘽灞曞紑鎬佹斁澶т负鏇村畬鏁寸殑涓婚€氳瘽闈㈡澘锛涘ご鍍忋€佷笁鐐圭姸鎬併€佺姸鎬佹枃妗堛€佽鏃跺拰搴曢儴鎺у埗鍖哄昂瀵稿寮猴紱妗岄潰绔噺灏戝皬鍗＄墖瀛ょ珛鎰燂紱绉诲姩绔睍寮€鎬侀摵婊′細璇濆唴瀹瑰尯锛屽彇娑堟偓娴崱鐗囧杈硅窛銆佸渾瑙掑拰闃村奖锛涙寕鏂寜閽敼涓?`PhoneFilled` 鍥炬爣鐨勭孩鑹插疄蹇冨渾褰㈡寜閽€?- 浜や簰杈圭晫锛氫粎浼樺寲璇煶閫氳瘽 UI 涓庡搷搴斿紡灞曠ず锛屼笉淇敼 STT/TTS銆丼SE 娑堟伅銆佽嚜鍔ㄥ彂閫併€佹姌鍙?灞曞紑鎴栫粨鏉熼潰璇曚笟鍔￠€昏緫銆?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 閫氳繃锛? 涓祴璇曟枃浠躲€? 涓祴璇曠敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊璇煶閫氳瘽灞曠ず銆佺Щ鍔ㄧ閾烘弧鍜屾寕鏂寜閽?UI锛屼笉缁х画鎵╁睍浜戠璇煶銆佸疄鏃堕煶瑙嗛鎴栭€氳瘽鏁版嵁瀛樺偍銆?
## 妯℃嫙闈㈣瘯璇煶閫氳瘽鐣岄潰楠屾敹淇锛?026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細璇煶浼氳瘽璇︽儏椤垫仮澶嶈亰澶╂秷鎭垪琛ㄤ綔涓哄簳灞傜晫闈紱榛樿灞曠ず閫氳瘽娴獥锛涘睍寮€鎬侀€氳瘽娴獥瑕嗙洊鏁翠釜浼氳瘽鍐呭鍖猴紝鑱婂ぉ鐣岄潰涓嶉€忓嚭锛涙姌鍙犲悗鍥炲埌搴曢儴閫氳瘽鏉″苟鏄剧ず鑱婂ぉ鐣岄潰锛涙敮鎸侀€氳瘽涓潤闊?鍙栨秷闈欓煶锛涜闊虫寜閽?hover 鏄剧ず鏂囧瓧鎻忚堪锛涗慨澶嶈闊抽€氳瘽鐩稿叧涓枃涔辩爜銆?- 浜や簰杈圭晫锛氳闊充細璇濆埛鏂板悗浠嶄笉浼氳嚜鍔ㄥ紑楹︼紱鐐瑰嚮娴獥楹﹀厠椋庢墠寮€濮嬮€氳瘽锛涙姌鍙犱粎鏀瑰彉灞曠ず褰㈡€侊紝涓嶅奖鍝?SSE 娑堟伅閾捐矾锛涘睍寮€鎬佸彧鏄剧ず璇煶鐣岄潰锛涢潤闊冲彧鏆傚仠楹﹀厠椋庢敹闊筹紝涓嶇粨鏉熼€氳瘽銆?- 鍓嶇楠岃瘉锛歚npm.cmd run test -- --run src/__tests__/views/InterviewSessionView.test.js src/__tests__/composables/useVoiceCall.test.js` 閫氳繃锛? 涓祴璇曟枃浠躲€? 涓祴璇曠敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍙鍖栭獙璇佽鏄庯細宸茶鍙?Browser 鎻掍欢璇存槑锛屼絾褰撳墠浼氳瘽鏈毚闇插叾瑕佹眰鐨?Node REPL 娴忚鍣ㄦ帶鍒跺伐鍏凤紝鍥犳鏈疆鏈墽琛?in-app browser 鎴浘楠岃瘉銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧淇璇煶闈㈣瘯閫氳瘽 UI銆佹姌鍙犺亰澶╃晫闈€侀潤闊冲拰涔辩爜锛屼笉缁х画鎵╁睍浜戠 STT/TTS銆佸疄鏃堕煶瑙嗛鎴栭€氳瘽鏃堕暱瀛樺偍銆?
## 妯℃嫙闈㈣瘯璇煶閫氳瘽 MVP锛?026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細鍒涘缓浼氳瘽椤垫柊澧炴枃瀛?璇煶浜や簰鏂瑰紡閫夋嫨锛涜闊充細璇濊鎯呴〉鏂板鈥滃紑濮嬮€氳瘽/鎸傛柇鈥濋€氳瘽闈㈡澘锛涙柊澧?TTS 閫愬彞鏈楄鍜岃闊抽€氳瘽缂栨帓 composable锛涘巻鍙茶褰曢〉灞曠ず鏂囧瓧/璇煶浜や簰鏂瑰紡鏍囪瘑銆?- 楠屾敹鍙嶉淇锛氳闊充細璇濊鎯呴〉涓嶅啀娓叉煋鑱婂ぉ娑堟伅鍒楄〃鍜屽簳閮ㄦ枃瀛楄緭鍏ュ尯锛屽彧灞曠ず璇煶閫氳瘽闈㈡澘锛汿TS 榛樿璇€熻皟鏁翠负 `0.92`銆侀煶楂樿皟鏁翠负 `1.06`锛屽苟浼樺厛閫夋嫨鏇磋嚜鐒剁殑涓枃 voice锛岄檷浣庢挱鎶ユ満姊版劅銆?- 鍏煎澶勭悊锛氭祻瑙堝櫒涓嶆敮鎸?Web Speech API 鏃惰闊抽€夐」缃伆锛涜闊充細璇濆埛鏂板悗涓嶄細鑷姩寮€楹︼紱AI 鏈楄鏈熼棿鏆傚仠鏀堕煶锛岄伩鍏嶆挱鎶ュ唴瀹硅璇瘑鍒紱TTS 杩囨护 `<FEEDBACK>` 缁撴瀯鍖栧弽棣堟銆?- 涓氬姟杈圭晫锛氬鐢ㄧ幇鏈?STT銆丼SE銆佹秷鎭垪琛ㄥ拰鍙戦€侀摼璺紱涓嶆帴浜戠璇煶鏈嶅姟锛屼笉淇濆瓨閫氳瘽鏃堕暱锛屼笉瀹炵幇鐪熸璇煶鎵撴柇 AI銆?- 楠岃瘉缁撴灉锛歚npm.cmd run test -- --run src/__tests__/views/InterviewSessionView.test.js src/__tests__/composables/useTextToSpeech.test.js` 閫氳繃锛? 涓祴璇曟枃浠躲€? 涓祴璇曢€氳繃锛沗npm.cmd run test` 閫氳繃锛?4 涓祴璇曟枃浠躲€?10 涓祴璇曢€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛氳 `runtime/STATE.md` 涓?`tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_BACKEND.md`銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧瀹屾垚娴忚鍣?Web Speech API 鐗堣闊抽€氳瘽 MVP锛屼笉缁х画鎵╁睍閫氳瘽鏃堕暱銆佸疄鏃堕煶瑙嗛鎴栦簯绔?STT/TTS銆?
## 妯℃嫙闈㈣瘯绛夊緟鎬佸姩鏁堜紭鍖栵紙2026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細妯℃嫙闈㈣瘯浼氳瘽椤典腑锛岄潰璇曞畼鍥炲澶勪簬 `thinking` 鐘舵€佹椂锛岀瓑寰呮皵娉′粠闈欐€佹枃妗堝崌绾т负鏂囧瓧涓庝笁鐐瑰叡鐢ㄥ悓涓€杩炵画娉㈠姩銆佽交寰懠鍚歌竟妗嗐€佹祬鑹叉壂鍏夊拰娣″叆涓婄Щ鐨勮交閲忓姩鏁堬紱鏂囧瓧鍜屽渾鐐圭粺涓€浣滀负 `thinking-motion-unit` 浠庡乏鍒板彸閿欑浉锛屾尝鍔ㄨ嚜鐒朵粠鏂囧瓧浼犲埌鍦嗙偣锛屼笉鍐嶅悇鑷姩鐢汇€?- 鍏煎澶勭悊锛氳ˉ鍏呮殫鑹叉ā寮忎綆浜害灞曠ず銆佺Щ鍔ㄧ姘旀场瀹藉害绾︽潫鍜?`prefers-reduced-motion: reduce` 闈欐€侀檷绾с€?- 涓氬姟杈圭晫锛氫笉淇敼鍚庣鎺ュ彛銆丼SE 娴佸紡閾捐矾銆佹秷鎭姸鎬佸瓧娈点€佽緭鍏ラ攣瀹氶€昏緫鎴?AI 鍥炲鐢熸垚閫昏緫銆?- 楠岃瘉缁撴灉锛歚npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 閫氳繃锛? 涓祴璇曟枃浠躲€? 涓祴璇曠敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_48_INTERVIEW_THINKING_ANIMATION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧浼樺寲妯℃嫙闈㈣瘯绛夊緟鎬佸姩鏁堬紝涓嶇户缁墿灞曢潰璇曟祦绋嬨€佹姤鍛婃垨鍏朵粬浼氳瘽鑳藉姏銆?
## 璐﹀彿娉ㄩ攢椤电甯冨眬鐣欑櫧淇锛?026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細璁剧疆涓績鈥滆处鍙峰畨鍏?> 娉ㄩ攢璐﹀彿鈥濋〉绛炬敼涓哄畬鏁村唴瀹瑰尯鍙屽垪甯冨眬锛屽乏渚у睍绀洪珮鍗辨彁绀哄拰涓夋璇存槑锛屽彸渚у睍绀哄瘑鐮併€佸畨鍏ㄩ棶棰樺拰娉ㄩ攢鎸夐挳琛ㄥ崟锛岃В闄ゆ敞閿€琛ㄥ崟缁ф壙鐨勭獎瀹藉害闄愬埗锛屼慨澶嶈緭鍏ユ鍙充晶澶ч潰绉┖鐧界殑闂銆?- 鍝嶅簲寮忓鐞嗭細妗岄潰绔乏鍙冲垎鏍忓厖鍒嗗埄鐢ㄥ唴瀹瑰搴︼紝绐勫睆鑷姩鍥炲埌鍗曞垪锛岀Щ鍔ㄧ鍗遍櫓鎸夐挳鍗犳弧瀹藉害銆?- 涓氬姟杈圭晫锛氫繚鐣?15 绉掑喎闈欐湡銆佸綋鍓嶅瘑鐮併€佸啀娆¤緭鍏ュ綋鍓嶅瘑鐮併€佸畨鍏ㄩ棶棰橀獙璇佸拰鏈€缁堢‘璁ゅ脊绐楋紝涓嶄慨鏀瑰悗绔帴鍙ｄ笌娉ㄩ攢 payload銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_46_ACCOUNT_DELETE_LAYOUT_FIX_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧淇娉ㄩ攢璐﹀彿椤电瑙嗚甯冨眬锛屼笉缁х画鎵╁睍璐﹀彿瀹夊叏鎴栨暟鎹鐞嗚兘鍔涖€?
## 璐﹀彿娉ㄥ唽鏃堕棿灞曠ず涓庢敞閿€璐﹀彿鐙珛楠岃瘉锛?026-05-18锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細棣栭〉韬唤鍗″拰璁剧疆涓績璐﹀彿璧勬枡鍖哄皢浼氬憳鍒版湡鏃堕棿灞曠ず鏀逛负鐢ㄦ埛娉ㄥ唽鏃堕棿锛涜缃腑蹇冨乏渚у鑸柊澧炵嫭绔嬧€滄敞閿€璐﹀彿鈥濋〉闈紱璐﹀彿瀹夊叏椤典笉鍐嶅湪搴曢儴灞曠ず涓嶅崗璋冪殑娉ㄩ攢鍗遍櫓琛屻€?- 娉ㄩ攢浜や簰锛氭敞閿€椤垫柊澧為珮鍗辫鍛娿€佷笁姝ラ闄╄鏄庛€佸綋鍓嶅瘑鐮併€佺‘璁ゅ瘑鐮併€佸畨鍏ㄩ棶棰樼瓟妗堝拰 15 绉掑喎闈欐湡锛涘€掕鏃剁粨鏉熷墠娉ㄩ攢鎸夐挳绂佺敤锛涙彁浜ゅ墠浠嶆湁鏈€缁堢‘璁ゅ脊绐椼€?- 鍚庣鎺ュ叆锛歚/api/auth/me` 杩斿洖 `createTime`锛涙柊澧?`GET /api/user/account/security-question` 鑾峰彇褰撳墠鐧诲綍璐﹀彿瀹夊叏闂锛沗POST /api/user/account/delete` 澧炲己涓哄瘑鐮佺‘璁?+ 瀹夊叏闂绛旀楠岃瘉銆?- 鍓嶇楠岃瘉锛歚npm.cmd test` 閫氳繃锛?8 涓祴璇曟枃浠躲€?6 涓祴璇曠敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛歚mvn.cmd test` 閫氳繃锛?62 涓祴璇曢€氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_45_ACCOUNT_PROFILE_AND_DELETE_VERIFICATION_FRONTEND.md`銆乣tasks/TASK_45_ACCOUNT_PROFILE_AND_DELETE_VERIFICATION_BACKEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊娉ㄥ唽鏃堕棿灞曠ず鍜岃处鍙锋敞閿€楠岃瘉澧炲己锛屼笉缁х画鎵╁睍璐﹀彿鏁版嵁瀵煎嚭銆佽处鍙锋仮澶嶃€佺鐞嗙娉ㄩ攢鎴栧叾浠栨柊鑳藉姏銆?
## 璁剧疆涓績鏁版嵁绠＄悊鏄惧紡淇濆瓨涓庢棤鐢ㄥ亸濂界Щ闄わ紙2026-05-18锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細璁剧疆涓績绉婚櫎鈥滃洖澶嶈鐣ュ亸濂解€濓紱闈㈣瘯鍋忓ソ浠呬繚瀛樺埌鏈満鍋忓ソ锛屼笉鍐嶈Е鍙戞湇鍔＄鐢ㄦ埛璁剧疆淇濆瓨锛涙暟鎹鐞嗗尯鏂板鈥滀繚瀛樿缃€濇寜閽紝闈㈣瘯璁板綍淇濈暀澶╂暟鍜岀畝鍘嗚瘖鏂繚鐣欏ぉ鏁板彧鏈夋樉寮忎繚瀛樺悗鎵嶅悓姝ユ湇鍔＄銆?- 鍚庣鎺ュ叆锛歚GET /api/user/settings`銆乣PUT /api/user/settings` 褰撳墠鍙繑鍥炲拰淇濆瓨 `interviewRetentionDays`銆乣resumeRetentionDays`锛涚Щ闄?`responseDetailPreference` DTO銆佸疄浣撳拰鏈嶅姟瀛楁锛涙柊澧?V4.1 鍏煎杩佺Щ鍒犻櫎鏃ф暟鎹簱鍒椼€?- 鍘嬪姏鎺у埗锛氳嚜鍔ㄦ竻鐞嗕换鍔＄瓥鐣ヤ笉鍙橈紝浠嶄负姣忔棩浣庡嘲灏忔壒閲忛€昏緫鍒犻櫎锛涚敤鎴蜂慨鏀归潰璇曞亸濂戒笉浼氫骇鐢熸湇鍔＄鍐欏叆銆?- 鍓嶇楠岃瘉锛歚npm.cmd test` 閫氳繃锛?8 涓祴璇曟枃浠躲€?5 涓祴璇曠敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛歚mvn.cmd test` 閫氳繃锛?57 涓祴璇曢€氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_44_SETTINGS_DATA_MANAGEMENT_SAVE_AND_RESPONSE_DETAIL_REMOVAL_FRONTEND.md`銆乣tasks/TASK_44_SETTINGS_DATA_MANAGEMENT_SAVE_AND_RESPONSE_DETAIL_REMOVAL_BACKEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊淇濆瓨鏃舵満鍜屾棤鐢ㄥ亸濂界Щ闄わ紝涓嶇户缁墿灞?AI prompt銆佹竻鐞嗙瓥鐣ャ€佹暟鎹鍑烘垨绠＄悊绔兘鍔涖€?
## 璁剧疆涓績鍗遍櫓鎿嶄綔鍚庣鎺ュ叆锛?026-05-18锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細璁剧疆涓績璐﹀彿娉ㄩ攢銆侀潰璇曡褰曟竻鐞嗐€佺畝鍘嗚瘖鏂竻鐞嗗凡浠庘€滃緟鍚庣鎺ュ叆鈥濈鐢ㄦ€佹敼涓虹湡瀹炴帴鍙ｆ搷浣滐紱璐﹀彿娉ㄩ攢闇€瑕佸綋鍓嶅瘑鐮佷簩娆＄‘璁わ紱鍘嗗彶娓呯悊鎿嶄綔闇€瑕佷簩娆＄‘璁ゅ苟灞曠ず鍚庣杩斿洖鐨勬竻鐞嗘暟閲忋€?- 鍚庣鎺ュ叆锛氭柊澧?`POST /api/user/account/delete`銆乣DELETE /api/interview/history`銆乣DELETE /api/resume/history`锛涜处鍙锋敞閿€閲囩敤閫昏緫鍒犻櫎鍜屾晱鎰熷瓧娈靛尶鍚嶅寲锛涢潰璇曡褰曞拰绠€鍘嗚瘖鏂褰曞潎鍙竻鐞嗗綋鍓嶇櫥褰曠敤鎴锋暟鎹€?- 鍓嶇浜や簰锛氬嵄闄╂搷浣滀繚鐣欓殧绂诲睍绀恒€佺‘璁ゅ脊绐楀拰 loading 鐘舵€侊紱璐﹀彿娉ㄩ攢鎴愬姛鍚庢竻闄ょ敤鎴风櫥褰曟€佸苟璺宠浆鐧诲綍椤碉紱鍘嗗彶娓呯悊鎴愬姛鍚庡埛鏂拌处鍙锋暟鎹瑙堬紱鏈湴缂撳瓨娓呯悊浠嶄笉鍒犻櫎鐧诲綍 token銆?- 鏈墿灞曞唴瀹癸細涓嶅疄鐜扳€滈潰璇曡褰曚繚鐣欏ぉ鏁扳€濈殑鏈嶅姟绔嚜鍔ㄦ竻鐞嗭紝涓嶆柊澧炴暟鎹簱琛紝涓嶆彁渚涙寜鏃ユ湡鎴栧嬀閫夐」娓呯悊銆?- 鍓嶇楠岃瘉锛歚npm.cmd test` 閫氳繃锛?8 涓祴璇曟枃浠躲€?2 涓祴璇曠敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛歚mvn.cmd test` 閫氳繃锛?48 涓祴璇曢€氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_42_SETTINGS_DATA_MANAGEMENT_FRONTEND.md`銆乣tasks/TASK_42_SETTINGS_DATA_MANAGEMENT_BACKEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧鎺ュ叆璐﹀彿娉ㄩ攢銆侀潰璇曡褰曟竻鐞嗐€佺畝鍘嗚瘖鏂竻鐞嗕笁涓嵄闄╂搷浣滐紝涓嶇户缁墿灞曚笅涓€椤规暟鎹鐞嗚兘鍔涖€?
## 璁剧疆涓績闈㈣瘯鍋忓ソ涓庨殣绉佹暟鎹鐞嗭紙2026-05-18锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細璁剧疆涓績鏂板闈㈣瘯鍋忓ソ銆侀殣绉佷笌鏁版嵁銆佹暟鎹鐞嗗垎鍖猴紱鏈満璁剧疆鍋忓ソ鏂板榛樿闈㈣瘯宀椾綅銆佺骇鍒€佹ā寮忋€佸弽棣堟ā寮忓拰闈㈣瘯璁板綍淇濈暀澶╂暟锛涢潰璇曞叆鍙ｉ〉璇诲彇鏈満榛樿闈㈣瘯鍋忓ソ锛屼笖璺敱 query 浼樺厛瑕嗙洊榛樿鍊笺€?- 闅愮涓庢暟鎹細澶嶇敤鎴愰暱姒傝鎺ュ彛灞曠ず璐﹀彿鏁版嵁姒傝锛涙竻绌烘湰鍦扮紦瀛樹粎娓呯悊璁剧疆鍋忓ソ銆佷富棰樺亸濂藉拰閫氱煡绛涢€夌紦瀛橈紝涓嶆竻鐞嗙敤鎴风鎴栫鐞嗙鐧诲綍 token銆?- 寰呭悗绔帴鍏ワ細璐﹀彿娉ㄩ攢銆侀潰璇曡褰曟竻鐞嗐€佺畝鍘嗚瘖鏂竻鐞嗐€佹湇鍔＄鑷姩娓呯悊鍘嗗彶璁板綍鍧囧彧灞曠ず绂佺敤鍏ュ彛锛屼笉鍋氬亣鍒犻櫎銆?- 鍓嶇楠岃瘉锛歚npm.cmd test` 閫氳繃锛?8 涓祴璇曟枃浠躲€?9 涓祴璇曠敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛氭湰杞笉娑夊強鍚庣淇敼銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_41_SETTINGS_CENTER_PRIVACY_DATA_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧鍋氳缃腑蹇冮〉闈竴鏈熷拰鏈満鍋忓ソ鎺ュ叆锛屼笉鏂板鍚庣鎺ュ彛銆佹暟鎹簱缁撴瀯鎴栫湡瀹炲垹闄よ兘鍔涖€?
## AI 绠€鍘?PDF 瀵煎嚭宸﹀彸绌虹櫧淇锛?026-05-18锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細AI 娑﹁壊缁撴灉椤?PDF 瀵煎嚭鏀逛负鎸?A4 瀹藉害閾烘弧骞剁旱鍚戝垎椤碉紝淇闀跨畝鍘嗗鍑?PDF 宸﹀彸绌虹櫧杩囧ぇ鐨勯棶棰橈紱鍥剧墖瀵煎嚭閾捐矾涓嶅彉銆?- 楠岃瘉鑼冨洿锛氭柊澧?PDF 鍒嗛〉璁＄畻鍗曞厓娴嬭瘯锛屾墽琛屽墠绔畾鍚戞祴璇曚笌鏋勫缓楠岃瘉銆?- 鍋滄璇存槑锛氭湰杞彧淇 PDF 瀵煎嚭鎺掔増锛屼笉淇敼 AI 鏂囨鐢熸垚銆佷笉鎻掑叆骞垮憡璇嶃€佷笉鎵╁睍鍏朵粬瀵煎嚭鑳藉姏銆?
## 褰撳墠鐗堟湰
- Offer 鍔犻€熷櫒瀹炴柦闃舵

## 褰撳墠闃舵
- 绗?4 閮ㄥ垎锛歄ffer 杈呭姪閾捐矾绗竴鐗堢粨鏋滃眰绾т慨澶?- 鐘舵€侊細鏈疆宸插畬鎴愶紝绛夊緟浜哄伐楠屾敹

## 宸插畬鎴愪笖宸查獙鏀剁殑鍔熻兘锛圴1.1锛?- 宀椾綅 JD 瀵规瘮鍒嗘瀽
- AI 绠€鍘嗘鼎鑹?- 宀椾綅瀹氬悜妯℃嫙闈㈣瘯

## 宸插畬鎴愪笖宸查獙鏀剁殑鍔熻兘锛圴1.2锛?- 鏂版墜寮曞
- 涓汉鎴愰暱涓績
- 娑堟伅閫氱煡

## Offer 鍔犻€熷櫒鍔熻兘鐘舵€?- 绗?0 閮ㄥ垎锛氬綋鍓嶇姸鎬佹敹鍙ｄ笌楠岃瘉锛屽凡瀹屾垚
- 绗?1 閮ㄥ垎锛氭繁搴﹂潰璇曞垎鏋愭姤鍛?V2锛屽凡瀹屾垚
- 绗?2 閮ㄥ垎锛氶潰璇曞巻鍙插洖鏀?+ 鍗虫椂鍙嶉鍙€夛紝宸插畬鎴?- 绗?3 閮ㄥ垎锛氬闈㈣瘯瀹樹汉璁剧郴缁燂紝宸插畬鎴?- 绗?4 閮ㄥ垎锛歄ffer 杈呭姪閾捐矾绗竴鐗堬紝鏈疆缁撴灉灞傜骇淇宸插畬鎴愶紝绛夊緟楠屾敹
- 鍚庣画褰曠敤鎰忓悜璇勪及銆佽儗璋冨噯澶囨寚瀵硷細灏氭湭寮€濮?- 棰樺簱銆佺儹鐐广€佹敹钘忋€佸懡涓巼缁熻锛氭槑纭笉鍋?
## 鏈疆瀹屾垚璇存槑
- 宸茶鍙?`runtime/DEVELOPMENT_RULES.txt`
- 宸茶鍙?`frontend/.claude/skills` 涓嬬殑鍓嶇 UI 浼樺寲鐩稿叧 skill
- 閲嶆瀯 Offer 杈呭姪椤甸潰 `/offer` 鐨勮瑙夊睍绀哄拰甯冨眬
- 淇濈暀钖祫璋堝垽妯℃嫙琛ㄥ崟鍜岀粨鏋滃睍绀?- 淇濈暀璋堣柂璇濇湳妯℃澘琛ㄥ崟鍜岀粨鏋滃睍绀?- 淇濈暀 Offer 杈呭姪 API 灏佽銆佽矾鐢卞拰瀵艰埅鍏ュ彛
- 鏈疆浠呬紭鍖栭〉闈綋楠岋紝涓嶆柊澧炰笟鍔¤兘鍔?- 杩藉姞淇澶嶅埗鎸夐挳瑙嗚闂锛屼娇鍏朵綔涓虹粨鏋滃潡鍙充笂瑙掓搷浣滃眰鍛堢幇锛屼笉鍐嶆尋鍗犳爣棰樻枃瀛楁帓鐗?- 鏍规嵁浜屾楠屾敹鍙嶉锛屽皢澶嶅埗鎸夐挳鏀逛负鍥炬爣鍨嬪急鍖栨搷浣滄帶浠讹紝澶嶅埗鎴愬姛鍚庣煭鏆傚垏鎹负瀹屾垚鐘舵€侊紝閬垮厤缁х画鍛堢幇娴忚鍣ㄩ粯璁ゆ枃瀛楁寜閽川鎰?- 鏍规嵁鏈疆楠屾敹鍙嶉锛屽皢澶嶅埗鎸夐挳鏀逛负甯告樉鐨勫浘鏍囧姞鏂囧瓧鎸夐挳锛屽苟鎶婄粨鏋滄爣棰樻敼涓鸿兌鍥婃爣绛撅紝鎻愬崌鍙瘑鍒€?- 鏍规嵁鏈€鏂伴獙鏀跺弽棣堬紝褰撳墠涓嶅啀灞曠ず澶嶅埗鎸夐挳锛涚Щ闄ょ粨鏋滃潡鍐呭鍒跺叆鍙ｅ拰澶嶅埗閫昏緫
- 缁熶竴鈥滃満鏅垽鏂€濃€滃缓璁洖澶嶁€濃€滄帹杩涚瓥鐣モ€濈瓑缁撴灉鍧楄儗鏅€佽竟妗嗗拰姝ｆ枃娴呭簳鍐呭鍖猴紝閬垮厤鍙湁寤鸿鍥炲鍛堢幇姘旀场
- 灏忔爣棰樻敼涓烘爣绛惧寲鏍囬鏍忥紝涓庝笅鏂瑰洖绛旀鏂囧舰鎴愮ǔ瀹氳瑙夊垎闅?- 琛ュ厖鎻愪氦鎺ュ彛寮傚父鎹曡幏锛屾帴鍙ｆ垨缃戠粶澶辫触鏃舵樉绀洪敊璇彁绀猴紝閬垮厤鏈鐞嗗紓甯稿啋娉″埌 Vue event handler
- 鏍规嵁鏈€鏂颁唬鐮佸鏌ョ粨鏋滐紝琛ュ厖鍏紑鐗堟湰鏃ュ織椤靛叆鍙ｏ紝骞朵慨澶嶇鐞嗙鎵归噺鐢ㄦ埛鐘舵€佹洿鏂板闀挎暣鍨?ID 鐨勭簿搴﹂闄?- 涓嶆帴瀹炴椂钖祫琛屾儏
- 涓嶅疄鐜板綍鐢ㄦ剰鍚戣瘎浼般€佽儗璋冨噯澶囨寚瀵?
## 楠岃瘉缁撴灉
- 鍓嶇鏋勫缓锛歚npm.cmd run build` 閫氳繃
- 鏈疆澶嶅埗鎸夐挳鍙鎬с€佺粨鏋滄爣绛惧拰鎻愪氦寮傚父澶勭悊淇鍚庯紝鍓嶇鏋勫缓锛歚npm.cmd run build` 閫氳繃
- 鏈疆绉婚櫎澶嶅埗鎸夐挳骞剁粺涓€缁撴灉鍧楁爣棰樸€佽儗鏅拰姝ｆ枃鍒嗗眰鍚庯紝鍓嶇鏋勫缓锛歚npm.cmd run build` 閫氳繃
- 鍓嶇 `package.json` 褰撳墠娌℃湁 test 鑴氭湰锛屾湰杞互鍓嶇鏋勫缓閫氳繃浣滀负楠岃瘉
- 鏈疆鍙︽湁鍚庣 Prompt 浼樺寲锛屽悗绔獙璇佽褰曡鏍圭洰褰?`runtime/STATE.md`
- 鏈疆浠ｇ爜瀹℃煡闂淇楠岃瘉锛歚npm.cmd test` 閫氳繃锛宍npm.cmd run build` 閫氳繃

## 鍏宠仈浠诲姟鏂囦欢
- `frontend/tasks/TASK_33_OFFER_ASSIST_PAGE_REDESIGN_FRONTEND.md`
- `frontend/tasks/TASK_32_OFFER_ACCELERATOR_STAGE4_OFFER_ASSIST_FRONTEND.md`
- `tasks/TASK_32_OFFER_ACCELERATOR_STAGE4_OFFER_ASSIST_BACKEND.md`

## 鍋滄璇存槑
- 鏈疆绗?4 閮ㄥ垎缁撴灉灞傜骇淇宸插畬鎴愶紝鍒版鍋滄
- 鏈粡鏄庣‘鎸囩ず锛屼笉缁х画褰曠敤鎰忓悜璇勪及銆佽儗璋冨噯澶囨寚瀵兼垨鍏朵粬鏂板姛鑳?
## 绠＄悊绔柊澧為〉闈㈠垎椤点€佹牱寮忎笌鎵归噺鎿嶄綔淇
- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細缁熶竴瀹¤鏃ュ織銆侀€氱煡鍏憡銆佺増鏈棩蹇椼€佽鍗曠鐞嗐€佹垚闀块厤缃€佷細鍛樺椁愰〉闈㈠垎椤垫牱寮忥紱淇鍒嗛〉 `total` 瀛楃涓茬被鍨嬫姤閿欙紱閫氱煡鍏憡銆佺増鏈棩蹇椼€佹垚闀块厤缃€佷細鍛樺椁愭敮鎸佹壒閲忔搷浣溿€?- 鏈墿灞曞唴瀹癸細瀹¤鏃ュ織鍜岃鍗曠鐞嗕繚鎸佸彧璇伙紝涓嶆柊澧炴壒閲忓垹闄ゆ垨鐘舵€佸彉鏇淬€?- 鍓嶇楠岃瘉锛歚npm.cmd test` 閫氳繃锛? 涓祴璇曟枃浠躲€?1 涓祴璇曠敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛歚mvn.cmd test` 閫氳繃锛?36 涓祴璇曢€氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_34_ADMIN_NEW_PAGES_PAGINATION_BATCH_FRONTEND.md`銆乣tasks/TASK_34_ADMIN_NEW_PAGES_PAGINATION_BATCH_BACKEND.md`銆?## 娑堟伅閫氱煡绫诲瀷涓庡叕寮€鐗堟湰鏃ュ織鍒嗛〉淇
- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細浼氬憳濂楅绠＄悊鍒嗛〉淇濇寔鍙冲榻愶紱鐢ㄦ埛娑堟伅閫氱煡椤佃ˉ榻愮郴缁熷叕鍛娿€佹椿鍔ㄥ叕鍛娿€佺増鏈叕鍛娿€佺淮鎶ゅ叕鍛婄殑鏍囩灞曠ず涓庣被鍨嬬瓫閫夛紱鏇村鍔ㄦ€侀〉鏀逛负鍏紑鐗堟湰鏃ュ織鍒嗛〉鏌ヨ锛屼笉鍐嶅浐瀹氬彧杩斿洖棣栭〉涓夋潯鏈€杩戞洿鏂般€?- 鏁版嵁瀛樺偍锛氫笉鏂板琛ㄥ拰瀛楁锛屽叕鍛婄被鍨嬫部鐢ㄧ幇鏈?`type` 瀛楁锛岀増鏈棩蹇楀垎椤垫部鐢?`sys_version_log` 宸插彂甯冭褰曘€?- 鍓嶇楠岃瘉锛歚npm.cmd test` 閫氳繃锛? 涓祴璇曟枃浠躲€?3 涓祴璇曠敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛歚mvn.cmd test` 閫氳繃锛?38 涓祴璇曢€氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_35_NOTIFICATION_AND_PUBLIC_VERSION_PAGINATION_FRONTEND.md`銆乣tasks/TASK_35_PUBLIC_VERSION_PAGINATION_BACKEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧淇褰撳墠鍙嶉闂锛屼笉缁х画鎵╁睍鍏憡缂栬緫銆佺増鏈棩蹇楁悳绱㈡垨鍏朵粬鏂板姛鑳姐€?
## 鐗堟湰鏃ュ織涓庨€氱煡浣撻獙閲嶆瀯
- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細鍏紑鐗堟湰鏃ュ織椤垫敼涓烘椂闂寸嚎寮忔洿鏂版祦锛涢搩閾涗笅鎷変笌鍏ㄩ儴娑堟伅閫氱煡椤靛叡鐢ㄩ€氱煡绫诲瀷鏄犲皠鍜屽浘鏍囷紱閾冮摏涓嬫媺琛ラ綈閫氱煡绫诲瀷鏍囩锛涚鐞嗙鍏憡绫婚€氱煡鏂板璇︽儏寮圭獥锛岄暱鍐呭鍒楄〃绠€鐣ュ睍绀恒€佸脊绐楀畬鏁村睍绀恒€?- 鏁版嵁瀛樺偍锛氫笉鏂板琛ㄥ拰瀛楁锛岀鐞嗙鍏憡绫婚€氱煡娌跨敤鐜版湁 `type` 瀛楁鐨?`system/activity/update/maintenance`銆?- 鍓嶇楠岃瘉锛歚npm.cmd test` 閫氳繃锛? 涓祴璇曟枃浠躲€?7 涓祴璇曠敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛氭湰杞笉娑夊強鍚庣淇敼銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_36_NOTIFICATION_DETAIL_AND_VERSION_LOG_REDESIGN_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧淇褰撳墠鍙嶉闂锛屼笉缁х画鎵╁睍鍏憡缂栬緫銆佸悗绔帴鍙ｃ€佹暟鎹簱缁撴瀯鎴栫増鏈棩蹇楁悳绱€?
## 鍏紑鐗堟湰鏃ュ織椤甸噸鏋勪慨澶?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細閲嶆柊璇诲彇 `frontend/.claude/skills` 鍚庯紝鎸夋俯鏆栦笓涓氭柟鍚戦噸鏋勫叕寮€鐗堟湰鏃ュ織椤碉紱鍏紑椤电姸鎬佷笌杈呭姪灞曠ず浣跨敤 Naive UI锛屽垎椤靛垏鍥?Element Plus锛涢粯璁ゆ瘡椤?5 鏉★紱琛ラ綈鍔犺浇銆佺┖鐘舵€併€侀敊璇噸璇曘€侀暱鏂囨湰鎶樺彔灞曞紑鍜屽垎椤典氦浜掞紱淇濈暀宸︿晶鏃ユ湡涓庡簭鍙峰垪锛屽苟鎻愬崌鍏跺姣斿害閬垮厤娴呴€忔槑鑹查毦浠ヨ鲸璁ゃ€?- 鏁版嵁瀛樺偍锛氫笉鏂板琛ㄥ拰瀛楁锛岀増鏈棩蹇楃户缁部鐢?`sys_version_log` 宸插彂甯冭褰曞拰鐜版湁鍏紑鍒嗛〉鎺ュ彛銆?- 鍓嶇楠岃瘉锛歚npm.cmd test` 閫氳繃锛?0 涓祴璇曟枃浠躲€?2 涓祴璇曠敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛氭湰杞笉娑夊強鍚庣淇敼銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_37_PUBLIC_VERSION_LOG_PAGE_REWORK_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧閲嶆瀯鍏紑鐗堟湰鏃ュ織椤碉紝涓嶇户缁墿灞曢椤垫渶杩戞洿鏂般€佺鐞嗙鐗堟湰鏃ュ織銆佸悗绔帴鍙ｃ€佹暟鎹簱缁撴瀯鎴栫増鏈棩蹇楁悳绱€?## 棣栭〉鏈€杩戞洿鏂颁笌鐗堟湰鏃ュ織鏄剧ず淇
- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細棣栭〉鏈€杩戞洿鏂板崱鐗囨爣棰樺鍔犲崟琛岀渷鐣ワ紝鎽樿缁х画闄愬埗 3 琛屽苟闃叉闀挎枃鏈拺鍧忓崱鐗囷紱鏈€杩戞洿鏂板尯鏍囬銆侀摼鎺ャ€佸崱鐗囪儗鏅€佽竟妗嗐€佹鏂囧拰鏃堕棿鍏ㄩ儴鏀逛负涓婚 token锛岃ˉ榻愭殫榛戞ā寮忛€傞厤锛涘叕寮€鐗堟湰鏃ュ織鏍囬澧炲姞鍗曡鐪佺暐锛岀Щ闄ゅ崱鐗囧ご閮ㄩ噸澶嶆棩鏈燂紝淇濈暀宸︿晶鏃ユ湡涓庡簭鍙峰垪骞惰繘涓€姝ユ彁鍗囧簭鍙峰彲璇绘€с€?- 鏁版嵁瀛樺偍锛氫笉鏂板琛ㄥ拰瀛楁锛岀户缁部鐢ㄧ幇鏈夐椤垫渶杩戞洿鏂颁笌鍏紑鐗堟湰鏃ュ織鎺ュ彛銆?- 鍓嶇楠岃瘉锛歚npm.cmd test` 閫氳繃锛?1 涓祴璇曟枃浠躲€?3 涓祴璇曠敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛氭湰杞笉娑夊強鍚庣淇敼銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_38_HOME_RECENT_UPDATES_TRUNCATION_DARKMODE_FIX_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧淇棣栭〉鏈€杩戞洿鏂板拰鍏紑鐗堟湰鏃ュ織鏄剧ず闂锛屼笉缁х画鎵╁睍鍏朵粬棣栭〉妯″潡銆佺鐞嗙椤甸潰鎴栨帴鍙ｈ兘鍔涖€?## 鍏紑鐗堟湰鏃ュ織鏍囬鎹㈣淇
- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細鎾ら攢鍏紑鐗堟湰鏃ュ織椤垫爣棰樼殑鍗曡鐪佺暐绛栫暐锛屾仮澶嶄负澶氳鑷劧鎹㈣鏄剧ず锛涗繚鐣欒秴闀胯繛缁瓧绗︽崲琛岃兘鍔涳紝閬垮厤椤甸潰琚尋鍙樺舰锛涢椤垫渶杩戞洿鏂板崱鐗囦粛淇濇寔鍗曡鐪佺暐锛屼笉鏀瑰彉鍗＄墖鍒楄〃瀵嗗害銆?- 鏁版嵁瀛樺偍锛氫笉鏂板琛ㄥ拰瀛楁锛岀户缁部鐢ㄧ幇鏈夊叕寮€鐗堟湰鏃ュ織鎺ュ彛銆?- 鍓嶇楠岃瘉锛歚npm.cmd test` 閫氳繃锛?1 涓祴璇曟枃浠躲€?3 涓祴璇曠敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛氭湰杞笉娑夊強鍚庣淇敼銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_39_VERSION_LOG_TITLE_WRAP_FIX_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧淇鍏紑鐗堟湰鏃ュ織椤垫爣棰樻樉绀虹瓥鐣ワ紝涓嶇户缁墿灞曞叾浠栭〉闈㈡垨鎺ュ彛鑳藉姏銆?
## 璁剧疆涓績
- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細鏂板鐢ㄦ埛渚?`/settings` 璁剧疆涓績锛岄泦涓彁渚涜处鍙疯祫鏂欍€佽处鍙峰畨鍏ㄣ€佸瑙傚亸濂姐€侀€氱煡鍋忓ソ銆佹柊鎵嬪紩瀵笺€佷細鍛樹笌棰濆害鍏釜鍒嗗尯锛涘ご鍍忎笅鎷夎彍鍗曚笌绉诲姩绔娊灞夋柊澧炶缃腑蹇冨叆鍙ｃ€?- 楠屾敹鍙嶉淇锛氳处鍙疯祫鏂欎笉鍐嶅睍绀虹敤鎴?ID 鍜屽椁愪唬鐮侊紝璁㈤槄濂楅鏀逛负灞曠ず濂楅鍚嶇О鎴栫敤鎴峰彲鐞嗚В鐨勫厹搴曟枃妗堬紱鏄电О淇敼鏀逛负褰撳墠鏄电О鏃佹寜閽Е鍙戝脊绐楋紱璐﹀彿瀹夊叏鏀逛负鈥滀慨鏀瑰瘑鐮?/ 淇敼瀹夊叏闂鈥濆垏鎹㈠紡琛ㄥ崟锛涘ご鍍忎笅鎷夎彍鍗曠Щ闄や慨鏀规樀绉般€佷慨鏀瑰瘑鐮併€佷慨鏀瑰畨鍏ㄩ棶棰樹笁涓噸澶嶅叆鍙ｃ€?- 浜屾楠屾敹鍙嶉淇锛氳处鍙峰畨鍏ㄥ垏鎹㈡敼涓烘帴杩戦椤靛鑸爮鐨勬爣绛惧紡鎸夐挳骞跺鍔犺交閲忚繃娓★紝琛ㄥ崟鍐呴儴绉婚櫎閲嶅鏍囬锛涙樀绉颁慨鏀规仮澶嶅埌澶村儚涓嬫媺鑿滃崟锛岃缃腑蹇冧粎灞曠ず鏄电О锛涜处鍙疯祫鏂欒ˉ鍏呬細鍛樺埌鏈熸椂闂达紝浼氬憳鏄剧ず鍏蜂綋鏃堕棿锛岄潪浼氬憳鏄剧ず鏈紑閫氫細鍛樸€?- 涓夋楠屾敹鍙嶉淇锛氭樀绉板脊绐楁仮澶嶆洿瀹屾暣鐨勫ご鍍忎笌褰撳墠鏄电О灞曠ず锛屽苟琛ラ綈绉诲姩绔搴﹀拰鎸夐挳鍫嗗彔閫傞厤锛涘瑙傚亸濂芥敼涓哄崱鐗囧紡閫夋嫨骞舵槑纭湰鍦颁繚瀛橈紱浼氬憳鍒版湡鏃堕棿缁熶竴灞曠ず鍒板勾鏈堟棩銆?- 鏈疆琛ュ厖淇锛氭樀绉板脊绐楀搴︿笉鍐嶄娇鐢ㄥ浐瀹?`440px`锛屾敼涓?`min(440px, calc(100vw - 24px))`锛岄伩鍏嶅皬灞忚 Element Plus 鍐呰仈瀹藉害鎾戝嚭瑙嗗彛銆?- 閫氱煡鍋忓ソ锛氭柊澧炴祻瑙堝櫒鏈湴鍋忓ソ瀛樺偍锛屾敮鎸侀《閮ㄥ疄鏃堕€氱煡鎻愰啋寮€鍏炽€侀€氱煡涓績榛樿鍙湅鏈銆侀€氱煡涓績榛樿绫诲瀷锛涘叧闂疄鏃舵彁閱掑悗锛岄《閮ㄤ笉鍐嶅缓绔?SSE 涓庤疆璇紝涔熶笉灞曠ず閫氱煡閾冮摏鍏ュ彛銆?- 澶嶇敤鑳藉姏锛氭樀绉颁慨鏀瑰鐢?`updateNickname`锛涘瘑鐮佷慨鏀瑰鐢?`updatePassword` 骞跺湪鎴愬姛鍚庨噸鏂扮櫥褰曪紱瀹夊叏闂澶嶇敤 `updateSecurityQuestion`锛涘瑙傚亸濂藉鐢?`themeStore` 鐨?`theme` / `followSystem` 鏈湴鎸佷箙鍖栵紱鏂版墜寮曞澶嶇敤 `OnboardingGuide`锛涗細鍛樹笌棰濆害澶嶇敤 `userStore` 褰撳墠杩斿洖瀛楁銆?- 鏁版嵁瀛樺偍锛氫笉鏂板鍚庣鎺ュ彛涓庢暟鎹簱琛紝閫氱煡鍋忓ソ鍜屽瑙傚亸濂藉潎鍐欏叆褰撳墠娴忚鍣?`localStorage`銆?- 鍓嶇楠岃瘉锛歚npm.cmd test` 閫氳繃锛?7 涓祴璇曟枃浠躲€?0 涓祴璇曠敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛氭湰杞笉娑夊強鍚庣淇敼銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_40_SETTINGS_CENTER_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧瀹炵幇骞朵慨姝ｅ墠绔缃腑蹇冿紝涓嶆墿灞曡法璁惧鍚屾銆佸悗绔敤鎴疯缃〃銆佷細鍛樻敮浠樻垨骞垮憡鏂囨鑳藉姏锛涚鐞嗙鐢ㄦ埛 ID 鍜屽椁愮紪鐮佸睍绀轰笉灞炰簬鏈疆淇敼鑼冨洿銆?# 璁剧疆涓績璁板綍淇濈暀澶╂暟鑷姩娓呯悊锛?026-05-18锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細璁剧疆涓績鏁版嵁绠＄悊鏂板鈥滅畝鍘嗚瘖鏂繚鐣欏ぉ鏁扳€濓紱闈㈣瘯璁板綍淇濈暀澶╂暟鍜岀畝鍘嗚瘖鏂繚鐣欏ぉ鏁板潎鎺ュ叆鏈嶅姟绔缃€?- 鍚庣鎺ュ叆锛氭柊澧?`GET /api/user/settings`銆乣PUT /api/user/settings`锛涙柊澧?`user_settings` 琛紱姣忔棩 03:30 浣庡嘲鎸夌敤鎴疯缃皬鎵归噺閫昏緫鍒犻櫎杩囨湡闈㈣瘯璁板綍鍜岀畝鍘嗚瘖鏂褰曘€?- 鍘嬪姏鎺у埗锛氶粯璁ゅ叧闂嚜鍔ㄦ竻鐞嗭紱姣忕敤鎴锋瘡绫绘暟鎹瘡娆℃渶澶?10 鎵广€佹瘡鎵?200 鏉★紱璺宠繃闈㈣瘯杩涜涓細璇濆拰绠€鍘嗗鐞嗕腑浠诲姟锛涗笉鍦ㄧ敤鎴疯姹傞摼璺墽琛屾竻鐞嗐€?- 鍓嶇楠岃瘉锛歚npm.cmd test` 閫氳繃锛?8 涓祴璇曟枃浠躲€?4 涓祴璇曠敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍚庣楠岃瘉锛歚mvn.cmd test` 閫氳繃锛?58 涓祴璇曢€氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_43_SETTINGS_RETENTION_AUTO_CLEANUP_FRONTEND.md`銆乣tasks/TASK_43_SETTINGS_RETENTION_AUTO_CLEANUP_BACKEND.md`銆?- 鍋滄璇存槑锛氭湰杞笉瀹炵幇鎸夋棩鏈熻寖鍥存竻鐞嗐€佸嬀閫夋竻鐞嗐€佹暟鎹鍑恒€佺墿鐞嗗垹闄ゆ垨 AI prompt 璇︾暐鍙ｅ緞鎺ュ叆銆?## 妯℃嫙闈㈣瘯璇煶杈撳叆闅旂涓庣綉缁滃け璐ヤ笉閲嶈繛锛?026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細妯℃嫙闈㈣瘯浼氳瘽椤靛皢鏂囧瓧杈撳叆璇煶璇嗗埆涓庤闊抽€氳瘽璇煶璇嗗埆鎷嗗垎涓轰袱涓嫭绔?`useSpeechToText()` 瀹炰緥锛岄伩鍏嶆枃瀛楄緭鍏ュ皬楹﹀厠椋庡拰璇煶閫氳瘽浜掔浉鎶㈠崰鐘舵€侊紱鍏抽棴楹﹀厠椋庢椂绔嬪嵆閲婃斁 recognition銆乄eb Audio 鍜屽獟浣撴祦璧勬簮锛岄伩鍏嶇偣鍑诲悗浠嶆樉绀洪害鍏嬮鏈叧闂紱闈㈣瘯 SSE 鍥炲澶辫触鍚庣洿鎺ヨ繘鍏ラ敊璇姸鎬佸苟閲婃斁鍙戦€侀攣锛屼笉鍐嶈嚜鍔ㄩ噸杩炴垨鏄剧ず鈥滄鍦ㄩ噸杩炩€濄€?- 浜や簰杈圭晫锛氭湰杞彧淇妯℃嫙闈㈣瘯璇煶杈撳叆銆佽闊抽€氳瘽闅旂鍜屾祦寮忓洖澶嶅け璐ュ鐞嗭紝涓嶄慨鏀瑰悗绔帴鍙ｃ€佷細璇濊〃缁撴瀯銆乀TS 鎾姤绛栫暐銆佽闊抽€氳瘽 UI 甯冨眬鎴栦簯绔?STT/TTS 鑳藉姏銆?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 18 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊褰撳墠璇煶杈撳叆鏁呴殰涓庣綉缁滃け璐ヤ笉閲嶈繛闂锛屼笉缁х画鎵╁睍瀹炴椂闊宠棰戙€佷簯绔闊虫湇鍔℃垨鏂扮殑闈㈣瘯鑳藉姏銆?## 妯℃嫙闈㈣瘯璇煶杈撳叆鍙戦€佹姤閿欎慨澶嶏紙2026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細淇璇煶璇嗗埆鏂囧瓧鍥炲～鍒拌緭鍏ユ鍚庣偣鍑诲彂閫佹寜閽椂鎶?`(overrideContent || inputMessage.value).trim is not a function` 鐨勯棶棰橈紱`sendMessage` 鐜板湪鍙湪鏀跺埌瀛楃涓茬被鍨嬭鐩栧唴瀹规椂浣跨敤瑕嗙洊鍊硷紝鏅€氭寜閽偣鍑讳簨浠朵細鍥炶惤鍒?`inputMessage.value` 浣滀负鍙戦€佹枃鏈€?- 浜や簰杈圭晫锛氭湰杞彧淇鍙戦€佸叆鍙ｅ弬鏁扮被鍨嬪垽鏂紝涓嶄慨鏀瑰悗绔帴鍙ｃ€丼SE 娑堟伅閾捐矾銆丼TT/TTS 璇嗗埆鎾姤閫昏緫銆佽闊抽€氳瘽 UI 鎴栦細璇濇暟鎹粨鏋勩€?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 7 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊褰撳墠璇煶杈撳叆鍙戦€佹姤閿欙紝涓嶇户缁墿灞曞叾浠栬闊宠兘鍔涖€?## 妯℃嫙闈㈣瘯棣栨璇煶閫氳瘽寮€鍦虹櫧鎾姤淇锛?026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細璇煶闈㈣瘯鐐瑰嚮寮€濮嬮€氳瘽鎴愬姛鍚庯紝浼氳鍙栧綋鍓嶄細璇濈涓€鏉￠潰璇曞畼娑堟伅骞惰皟鐢ㄧ幇鏈?TTS 鎾姤寮€鍦虹櫧锛涘悓涓€椤甸潰鐢熷懡鍛ㄦ湡鍐呭彧鎾姤涓€娆★紝閬垮厤鎸傛柇鍚庡啀娆″紑濮嬮€氳瘽閲嶅鏈楄锛涜嫢寮€鍦虹櫧浠嶅湪鐢熸垚涓紝鍒欏湪杞鎷垮埌鑱婂ぉ璁板綍涓旈€氳瘽浠嶅紑鍚椂琛ユ挱涓€娆°€?- 浜や簰杈圭晫锛氭湰杞彧淇棣栨璇煶閫氳瘽寮€鍦虹櫧涓嶆挱鎶ラ棶棰橈紝涓嶄慨鏀瑰悗绔帴鍙ｃ€丼SE 鍥炲鎾姤绛栫暐銆佽闊抽€氳瘽 UI銆佷簯绔闊宠兘鍔涙垨浼氳瘽鏁版嵁缁撴瀯銆?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 9 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊褰撳墠寮€鍦虹櫧鎾姤浣撻獙闂锛屼笉缁х画鎵╁睍鍏朵粬璇煶鑳藉姏銆?## 妯℃嫙闈㈣瘯寮€鍦虹櫧鎾姤鏀堕煶闅旂淇锛?026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細淇棣栨寮€鍦虹櫧 TTS 鎾姤鏃惰娴忚鍣?STT 璇瘑鍒负鐢ㄦ埛鍥炵瓟鐨勯棶棰橈紱寮€鍦虹櫧鎾姤鍓嶄細鍏堝彇娑堥€氳瘽璇煶璇嗗埆骞舵竻绌鸿瘑鍒崏绋匡紝鎾姤缁撴潫鍚庡鐢ㄧ幇鏈?`resumeListening` 鑷姩鎭㈠鏀堕煶銆?- 浜や簰杈圭晫锛氭湰杞彧淇寮€鍦虹櫧鎾姤涓庢敹闊抽殧绂伙紝涓嶄慨鏀瑰悗绔帴鍙ｃ€丼SE 鍥炲鎾姤绛栫暐銆佽闊抽€氳瘽 UI銆佷簯绔闊宠兘鍔涙垨浼氳瘽鏁版嵁缁撴瀯銆?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 10 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊褰撳墠 TTS 鍥炲０璇瘑鍒棶棰橈紝涓嶇户缁墿灞曞叾浠栬闊宠兘鍔涖€?## 妯℃嫙闈㈣瘯鍘嗗彶璇煶浼氳瘽寮€鍦虹櫧閲嶅鎾姤淇锛?026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細缁х画鍘嗗彶璇煶闈㈣瘯鏃讹紝濡傛灉鑱婂ぉ璁板綍涓凡缁忓瓨鍦ㄧ敤鎴峰洖绛旓紝寮€濮嬮€氳瘽涓嶅啀鎾姤绗竴鏉￠潰璇曞畼寮€鍦虹櫧锛涘彧鏈夊皻鏈綔绛旂殑棣栬疆璇煶浼氳瘽浼氭挱鎶ュ紑鍦虹櫧銆?- 浜や簰杈圭晫锛氭湰杞彧淇鍘嗗彶璇煶浼氳瘽閲嶅鎾姤寮€鍦虹櫧闂锛屼笉淇敼鍚庣鎺ュ彛銆丼SE 鍥炲鎾姤绛栫暐銆佽闊抽€氳瘽 UI銆佷簯绔闊宠兘鍔涙垨浼氳瘽鏁版嵁缁撴瀯銆?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 11 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊褰撳墠閲嶅鎾姤闂锛屼笉缁х画鎵╁睍鍏朵粬璇煶鑳藉姏銆?## 妯℃嫙闈㈣瘯绉诲姩绔《閮ㄥ鑸爮閿欎綅淇锛?026-05-19锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細绉诲姩绔細璇濋《閮ㄦ爮绗簩琛屾搷浣滃尯浠?`space-between` 鏀逛负涓夊垪 grid锛岀姸鎬侀潬宸︼紝杩斿洖鎸夐挳闈犺繎鍙充晶鎿嶄綔鍖猴紝缁撴潫闈㈣瘯鎸夐挳鍥哄畾鍦ㄦ渶鍙筹紱鏍囬鍙敹缂╃渷鐣ワ紝闅惧害鏍囩淇濇寔鍥哄畾瀹藉害銆?- 浜や簰杈圭晫锛氭湰杞彧淇绉诲姩绔《閮ㄥ鑸爮鎺掔増閿欎綅锛屼笉淇敼璇煶閫氳瘽涓讳綋銆佽亰澶╁唴瀹广€佸悗绔帴鍙ｃ€丼SE 鍥炲鎾姤绛栫暐鎴栦細璇濇暟鎹粨鏋勩€?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 13 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧澶勭悊褰撳墠绉诲姩绔鑸爮鎺掔増闂锛屼笉缁х画鎵╁睍鍏朵粬璇煶鎴栭〉闈㈣兘鍔涖€?
## 绀惧尯 ImageGrid class 缁ф壙璀﹀憡淇锛?026-05-23锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚锛岀瓑寰呬汉宸ラ獙鏀躲€?- 宸插畬鎴愬唴瀹癸細淇 `ImageGrid` 鍥犲鏍硅妭鐐规棤娉曡嚜鍔ㄧ户鎵跨埗缁勪欢 `class="card-images"` 瀵艰嚧鐨?Vue warning锛涚粍浠跺叧闂嚜鍔?attrs 缁ф壙锛屽苟灏嗗閮?class 鏄惧紡鍚堝苟鍒?`.image-grid`銆?- 鏁版嵁瀛樺偍锛氫笉娑夊強銆?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/components/community/ImageGrid.test.js src/__tests__/views/community/CommunityView.test.js src/__tests__/views/community/MyActivity.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 32 涓敤渚嬮€氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_COMMUNITY_ACTIVITY_VIRTUAL_SCROLL_2026_05_23_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧淇褰撳墠 Vue warning锛屼笉缁х画璋冩暣鍥剧墖棰勮浜や簰鎴栫ぞ鍖哄垪琛ㄥ竷灞€銆?
## 鐢ㄦ埛绔椤靛浘鏍囨斁澶т笌鑳屾櫙鍝嶅簲寮忎紭鍖栵紙2026-05-24锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚缂栫爜銆佸畾鍚戞祴璇曞拰鐢熶骇鏋勫缓锛岀瓑寰呬汉宸ヨ瑙夐獙鏀躲€?- 宸插畬鎴愬唴瀹癸細棣栭〉璺緞鑺傜偣鍥炬爣鏀逛负鏃犳澶у浘鏍囧睍绀猴紝缁х画澶嶇敤 `FeatureIcon` 骞朵娇鐢?`size="xl"`锛涜緟鍔╄兘鍔涘叆鍙ｅ浘鏍囨敼涓烘棤妗?66px 灞曠ず鍖哄苟浣跨敤 `size="lg"`锛汣TA 鍜岀澶村浘鏍囨斁澶у埌 `size="md"`锛宧over 绠ご鍙嶉澧炲己锛沗background.png` 鐚浘妗岄潰绔户缁斁澶э紝骞虫澘绔寜瑙嗗彛璋冩暣浣嶇疆鍜岄€忔槑搴︼紝520px 浠ヤ笅閫氳繃 opacity 杩囨浮娣″嚭锛岄伩鍏嶆尋鍘嬬Щ鍔ㄧ鏂囨銆?- 浜や簰杈圭晫锛氭湰杞彧澶勭悊棣栭〉 `/` 鐨勫浘鏍囧睍绀恒€佽儗鏅浘鍝嶅簲寮忓拰杩囨浮鍔ㄧ敾锛屼笉淇敼 `AppHeader.vue`銆乣MainLayout.vue`銆佸叾瀹冪敤鎴风椤甸潰銆乣/admin/**`銆丄PI銆佽矾鐢便€佹暟鎹簱銆佸悗绔笟鍔℃祦绋嬫垨鐧诲綍閴存潈閫昏緫銆?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 2 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_USER_HOME_UI_REFACTOR_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧淇褰撳墠棣栭〉瑙嗚鍙嶉锛屼笉缁х画鎵╁睍鍏跺畠椤甸潰鎴栨柊澧炰笟鍔¤兘鍔涖€?## 鐢ㄦ埛绔椤典娇鐢ㄨ矾寰勫尯鍩熷瘑搴︿慨澶嶏紙2026-05-24锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚缂栫爜銆佸畾鍚戞祴璇曞拰鐢熶骇鏋勫缓锛岀瓑寰呬汉宸ヨ瑙夐獙鏀躲€?- 宸插畬鎴愬唴瀹癸細閽堝鈥滀娇鐢ㄨ矾寰勪笅闈㈡樉寰楀お绌衡€濈殑鍙嶉锛屼粎璋冩暣棣栭〉 `.workflow-section`锛涜鍖哄煙鏀逛负娴呮鐧藉伐浣滄祦闈㈡澘锛屽乏渚у鍔?4 涓?`.workflow-mini-item` 璺緞姒傝鑳跺泭锛屽彸渚?`.workflow-step` 鍦ㄦ闈㈢鏀逛负 2x2 鐭╅樀锛屽苟琛ュ厖搴曢儴鐭繘搴︾嚎涓庤交閲?hover 鍙嶉锛涚Щ鍔ㄧ缁х画鍥炶惤鍗曞垪锛岄伩鍏嶅皬灞忔嫢鎸ゃ€?- 浜や簰杈圭晫锛氭湰杞彧澶勭悊棣栭〉 `/` 鐨勪娇鐢ㄨ矾寰勫尯鍩熻瑙夊瘑搴︼紝涓嶄慨鏀瑰叾瀹冮椤垫ā鍧椼€乣AppHeader.vue`銆乣MainLayout.vue`銆佸叾瀹冪敤鎴风椤甸潰銆乣/admin/**`銆丄PI銆佽矾鐢便€佹暟鎹簱銆佸悗绔笟鍔℃祦绋嬫垨鐧诲綍閴存潈閫昏緫銆?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 2 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_USER_HOME_UI_REFACTOR_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧淇褰撳墠棣栭〉瑙嗚鍙嶉锛屼笉缁х画鎵╁睍鍏跺畠椤甸潰鎴栨柊澧炰笟鍔¤兘鍔涖€?## 鐢ㄦ埛绔椤典娇鐢ㄨ矾寰勫尯鍩熶簩娆″幓鍗＄墖鍖栵紙2026-05-24锛?- 褰撳墠闃舵锛氭湰杞凡瀹屾垚缂栫爜銆佸畾鍚戞祴璇曞拰鐢熶骇鏋勫缓锛岀瓑寰呬汉宸ヨ瑙夐獙鏀躲€?- 宸插畬鎴愬唴瀹癸細鏍规嵁鈥滃お涓戜簡鈥濈殑鍙嶉锛岄噸鏂颁娇鐢?`frontend-design`銆乣impeccable/critique` 鍜?`impeccable/layout` 瀹¤棣栭〉浣跨敤璺緞鍖哄煙锛涙挙鎺変笂涓€鐗?`.workflow-mini-map` 鑳跺泭鍜?2x2 鍗＄墖鐭╅樀锛屾敼涓烘棤鍗＄墖杈圭晫鐨勬í鍚戣矾寰勫甫锛屾闈㈢鐢?4 涓渾褰㈠簭鍙峰拰杩炵画缁嗙嚎琛ㄨ揪姹傝亴鑺傚锛岀Щ鍔ㄧ鍥炶惤涓虹珫鍚戞椂闂寸嚎銆?- 浜や簰杈圭晫锛氭湰杞彧澶勭悊棣栭〉 `/` 鐨勪娇鐢ㄨ矾寰勫尯鍩熻瑙夐噸鏋勶紝涓嶄慨鏀瑰叾瀹冮椤垫ā鍧椼€乣AppHeader.vue`銆乣MainLayout.vue`銆佸叾瀹冪敤鎴风椤甸潰銆乣/admin/**`銆丄PI銆佽矾鐢便€佹暟鎹簱銆佸悗绔笟鍔℃祦绋嬫垨鐧诲綍閴存潈閫昏緫銆?- 鍓嶇楠岃瘉锛歚npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 閫氳繃锛? 涓祴璇曟枃浠?/ 2 涓敤渚嬮€氳繃锛沗npm.cmd run build` 閫氳繃銆?- 鍏宠仈浠诲姟鏂囦欢锛歚frontend/tasks/TASK_USER_HOME_UI_REFACTOR_FRONTEND.md`銆?- 鍋滄璇存槑锛氭湰杞彧淇褰撳墠棣栭〉瑙嗚鍙嶉锛屼笉缁х画鎵╁睍鍏跺畠椤甸潰鎴栨柊澧炰笟鍔¤兘鍔涖€?
## 模拟面试语音通话移动端顶部控制隐藏（2026-05-24）
- 当前阶段：本轮已完成语音通话展开态移动端响应式修复、定向测试和生产构建，等待人工视觉验收。
- 已完成内容：在 `InterviewSessionView.vue` 的 767px 以下媒体查询中隐藏 `.voice-window-bar`，让移动端不再显示顶部缩小符号和 X；底部 `.voice-dock-actions` 保留完整控制能力，并调整头像顶部间距让小屏布局更自然。
- 交互边界：本轮只处理模拟面试语音通话移动端顶部重复控制隐藏；不修改 STT/TTS、SSE、发送、挂断、接口、路由、数据库或后端业务流程。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，1 个测试文件 / 21 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只完成当前响应式修复，不继续推进其它页面或语音能力改造。

## 模拟面试会话页 UI 质感与动效优化（2026-05-24）
- 当前阶段：本轮已完成模拟面试会话页表现层优化、定向测试和生产构建，等待人工视觉验收。
- 已完成内容：`InterviewSessionView.vue` 新增 `.interview-session-shell`、`.session-main-surface`、`.conversation-surface` 结构锚点；聊天消息增加 `.message-entrance` 入场、角色胶囊、暖橙白表面、柔和投影和 hover 反馈；输入区、折叠语音通话条与语音通话浮层统一表面质感；新增 `sessionSurfaceIn`、`messageFloatIn`、`voiceCallEnter` 动效，并在 `prefers-reduced-motion` 下关闭新增和持续动画。
- 暗色适配：修复会话页 scoped CSS 暗色选择器，统一使用 `:global(html[data-theme="dark"] ...)`，补齐顶部栏、背景、消息气泡、输入区、语音浮层、反馈卡片等暗色变量覆盖。
- 交互边界：本轮只优化模拟面试会话页 UI 和动效；不修改 `/admin/**`、API、路由、数据库、后端业务流程、STT/TTS、SSE、发送、挂断或面试状态逻辑。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，1 个测试文件 / 20 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只完成模拟面试会话页优化，不继续推进其它页面重构。
## 模拟面试报告页 UI 与动效优化（2026-05-24）
- 当前阶段：本轮已完成模拟面试报告页视觉结构、动效、响应式、暗色模式、定向测试和生产构建，等待人工视觉验收。
- 已完成内容：将 `InterviewReportView.vue` 从长卡片堆叠调整为“面试复盘工作台”；新增 `.interview-report-shell`、`.report-hero-shell`、`.report-score-panel`、`.report-summary-panel`、`.report-priority-grid`、`.report-section-grid` 结构锚点；顶部集中展示分数、等级、岗位目标和 AI 总结，3 条立即行动建议前置，下方承接评分参考、回放、逐轮复盘、失分模式、能力维度、雷达图和逐题表现。
- 视觉与动效：新增报告页 `--report-*` 变量、暖橙半透明表面、柔和阴影、页面/区块入场、行动建议 stagger、折叠行 hover/press 反馈；动画限定在 `opacity`、`transform`、边框和阴影，并补齐 `prefers-reduced-motion` 降级。
- 暗色与响应式：报告页暗色选择器统一改为 `:global(html[data-theme="dark"] ...)`，补齐 hero、总结、行动建议、回放、评分、维度和底部按钮暗色表面；900px、768px、480px 下 hero、行动建议、评分表、回放和操作按钮可自然堆叠。
- 交互边界：仅修改模拟面试报告页、对应单测和前端任务文档；不修改 `/admin/**`、API、路由、数据库、后端、报告计算、报告轮询、分享逻辑或面试会话逻辑；不新增 UI 库。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/InterviewReportView.test.js` 通过，1 个测试文件 / 3 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_INTERVIEW_REPORT_UI_REFACTOR_FRONTEND.md`。
- 停止说明：本轮只优化模拟面试报告页，不继续推进其它页面重构。

## 模拟面试结果页展开稳定性与诊断区重构优化（2026-05-24）
- 当前阶段：本轮已完成模拟面试结果页前端展示、展开稳定性、诊断区布局、暗色模式和动效优化，等待人工视觉验收。
- 已完成内容：`InterviewReportView.vue` 新增 `.report-diagnosis-stack` 单列诊断栈，避免失分模式、不足表现、改进建议和维度详情因内容量不同产生双栏拉扯；维度详情改为更适合长评语的横向信息行，小屏自动堆叠；逐轮复盘展开内容改为稳定块状结构，问题、回答、复盘、追问失分和下次练法独立换行。
- 展示层去重：保留并扩展前端去重能力，岗位反馈、失分模式、改进建议和重复逐轮复盘项仅在显示层去重；不修改后端报告数据、不修改 AI 生成逻辑。
- 视觉与动效：新增 `reportExpandIn` 展开动效，回放、逐轮复盘和题目折叠内容使用 `opacity` / `transform` 进入；按钮、折叠行和复盘条目增加轻量 hover/press 反馈；`prefers-reduced-motion` 下关闭新增位移和长过渡。
- 暗色与响应式：补齐 `.loss-pattern-column`、`.dimension-card`、`.round-review-item`、`.question-answer`、`.question-comment` 以及 Element Plus collapse 内容区的暗色覆盖；900px 以下保持单列，避免横向滚动和展开变形。
- 交互边界：本轮只处理模拟面试结果页前端展示与样式；不修改 `/admin/**`、API、路由、数据库、后端、报告计算、面试会话、STT/TTS、SSE 或 AI 追问 prompt 逻辑。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/InterviewReportView.test.js` 通过，1 个测试文件 / 5 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_INTERVIEW_REPORT_UI_REFACTOR_FRONTEND.md`。
- 停止说明：本轮只完成模拟面试结果页优化，不继续推进其它页面或后端逻辑修复。
## 用户端暗色主题暖棕统一（2026-05-24）
- 当前阶段：本轮已完成用户端全局暗色 token 暖棕统一、首页暗色 token 精简、管理端暗色局部隔离、定向测试和生产构建，等待人工视觉验收。
- 已完成内容：`styles/index.css` 的 `[data-theme="dark"]` 从冷蓝紫改为暖橙棕体系，并同步 Element Plus 暗色桥接；`App.vue` 的 Naive UI `darkTheme` 覆盖对齐暖棕 token；`HomePageView.vue` 保留首页专属 `--home-*` 层次但移除通用文字/边框重复覆盖；`AdminLayout.vue` 增加暗色局部 token，并在管理端作用域内覆盖 Element Plus `--el-*` 暗色变量，保持 `/admin/**` 不跟随用户端暖棕视觉；简历/成长图表和新手引导移除残留旧冷色硬编码。
- 交互边界：本轮只处理主题 token 和相关测试文档；不修改路由、API、数据库、后端业务流程或新增 UI 库。
- 前端验证：`npm.cmd test -- --run src/__tests__/App.test.js src/__tests__/views/HomePageView.test.js src/__tests__/themeTokens.test.js` 通过，3 个测试文件 / 10 个用例通过；`npm.cmd test` 通过，45 个测试文件 / 275 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_USER_DARK_THEME_UNIFICATION_FRONTEND.md`。
- 停止说明：本轮只完成用户端暗色主题统一，不继续推进其它页面重构或新增功能。
## 模拟面试报告分享到社区遮罩空白修复（2026-05-24）
- 当前阶段：本轮已完成报告页“分享到社区”弹窗定位修复、红灯复现、定向测试和生产构建，等待人工验收。
- 已完成内容：定位到 `ShareReportDialog.vue` 未显式 `append-to-body`，而报告页 `.report-content` 入场动画会保留 `transform` 定位上下文，导致点击分享时出现黑色遮罩但弹窗面板不可见；本轮将分享报告弹窗挂载到 `body`，沿用社区首页发布/分享弹窗的既有模式。
- 交互边界：仅修改模拟面试报告分享到社区弹窗、对应单测和前端任务文档；不修改社区发帖接口、报告计算、报告页主展示、路由、数据库或后端业务流程。
- 前端验证：先运行 `npm.cmd test -- --run src/__tests__/components/community/ShareReportDialog.test.js` 失败于 `appendToBody` 为 `undefined`；修复后 `npm.cmd test -- --run src/__tests__/views/InterviewReportView.test.js src/__tests__/components/community/ShareReportDialog.test.js` 通过，2 个测试文件 / 6 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_INTERVIEW_REPORT_UI_REFACTOR_FRONTEND.md`。
- 停止说明：本轮只修复当前分享弹窗遮罩空白 bug，不继续推进其它页面或社区能力扩展。
## 用户端图标光晕范围收紧（2026-05-24）
- 当前阶段：本轮已完成首页、导航栏、头像下拉菜单无光晕/无淡透明图标框约束，通知类型图标改为显式 opt-in 光晕，等待人工视觉验收。
- 已完成内容：`FeatureIcon` 继续保持 `halo` 默认关闭；`NotificationTypeIcon` 新增 `halo` 参数且默认关闭；通知中心列表和顶部通知下拉的可点击通知条目显式传入 `halo`，公告弹窗等静态通知图标不传；通知栏图标尺寸保持 `sm=34px`、`md=44px`，不继续放大；首页、导航栏、移动端导航和头像下拉菜单图标不使用 `FeatureIcon halo`、`feature-icon-halo` 或全局图标光晕规则。
- 交互边界：本轮只处理用户端图标展示范围、通知类型图标 opt-in 光晕和对应测试文档；不修改 `/admin/**`、API、路由、数据库、通知 SSE、鉴权或后端业务流程。
- 前端验证：先运行 `npm.cmd test -- --run src/__tests__/components/notification/NotificationTypeIcon.test.js src/__tests__/views/NotificationView.test.js` 失败于通知类型图标默认仍带光晕和通知列表未显式传入 `halo`；修复后 `npm.cmd test -- --run src/__tests__/components/notification/NotificationTypeIcon.test.js src/__tests__/views/NotificationView.test.js src/__tests__/styles/UserIconHalo.test.js src/__tests__/components/common/FeatureIcon.test.js` 通过，4 个测试文件 / 10 个用例通过；回归 `npm.cmd test -- --run src/__tests__/components/common/FeatureIcon.test.js src/__tests__/components/notification/NotificationTypeIcon.test.js src/__tests__/components/AppHeader.test.js src/__tests__/views/HomePageView.test.js src/__tests__/views/NotificationView.test.js src/__tests__/views/InterviewReportView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/styles/UserIconHalo.test.js` 通过，8 个测试文件 / 47 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_USER_ICON_HALO_SCOPE_FRONTEND.md`。
- 停止说明：本轮只完成当前图标光晕范围收紧，不继续推进其它页面重构或图标替换。
## 模拟面试报告分享到社区成功提示重复修复（2026-05-25）
- 当前阶段：本轮已完成报告页分享成功重复 toast 修复、红灯复现、定向测试和生产构建，等待人工验收。
- 已完成内容：定位到 `ShareReportDialog.vue` 发帖成功后已弹出一次“分享成功”，而 `InterviewReportView.vue` 仍监听 `success` 事件并再次调用 `ElMessage.success("分享成功")`；本轮保留弹窗内部唯一成功提示，移除父页面重复提示监听。
- 交互边界：仅修改模拟面试报告页分享成功提示归属和对应单测；不修改社区发帖接口、分享弹窗提交逻辑、报告计算、路由、数据库或后端业务流程。
- 前端验证：先运行 `npm.cmd test -- --run src/__tests__/views/InterviewReportView.test.js` 失败于父页面仍追加第二次“分享成功”；修复后 `npm.cmd test -- --run src/__tests__/views/InterviewReportView.test.js src/__tests__/components/community/ShareReportDialog.test.js` 通过，2 个测试文件 / 7 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_INTERVIEW_REPORT_UI_REFACTOR_FRONTEND.md`。
- 停止说明：本轮只修复当前重复成功提示 bug，不继续推进其它页面或社区能力扩展。

## 个人中心图标可读性与简历结果页按钮图标修复（2026-05-25）
- 当前阶段：本轮已完成个人中心/设置中心图标尺寸优化、简历诊断结果页按钮图标补齐、定向测试和生产构建，等待人工视觉验收。
- 已完成内容：`SettingsView.vue` 将设置分组、语音试听、账号安全警告、注销确认弹窗和数据刷新入口的 `FeatureIcon` 调整为更清楚的 `md` 尺寸；左侧设置导航仅增加轻微 hover/active 位移动效，不添加光晕或淡透明图标框；语音试听和数据刷新按钮同步放大点击面积。
- 简历结果页补齐：`ResultView.vue` 在“开始分析”按钮补入 `job-match-analysis` 图标，在“去 AI 润色”按钮补入 `resume-optimization` 图标；相关资源已存在于本地图标映射中，其中简历润色资源来自 `old/`，岗位匹配资源来自 `new/`。
- 交互边界：本轮只处理用户端个人中心图标可读性和简历结果页按钮缺图标；不修改首页、导航栏、头像下拉菜单光晕策略，不修改通知栏图标尺寸，不修改 `/admin/**`、API、路由、数据库、后端业务流程或 AI 分析逻辑。
- 前端验证：先运行 `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 失败于设置中心图标仍为 `sm`；修复后 `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js src/__tests__/views/ResumeResultView.test.js src/__tests__/utils/featureIcons.test.js` 通过，3 个测试文件 / 31 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_SETTINGS_CENTER_ICON_SIZE_FRONTEND.md`。
- 停止说明：本轮只完成当前图标可读性与缺失图标修复，不继续推进其它页面或新增功能。
## 社区成功提示事件归属修复（2026-05-25）
- 当前阶段：本轮已完成社区发帖和报告分享成功提示事件归属修复、红灯复现、定向测试、静态扫描和生产构建，等待人工验收。
- 已完成内容：`PostEditor.vue` 发帖成功后仍由编辑器内部显示唯一“发布成功”，但对外事件从通用 `success` 改为业务事件 `published`；`CommunityView.vue` 改为监听 `published`，只关闭发布弹窗并刷新列表；`ShareReportDialog.vue` 移除已无生产消费者的通用 `success` emit，保留弹窗内部唯一“分享成功”和关闭弹窗逻辑。
- 交互边界：本轮只修复社区成功提示事件归属和重复 toast 复发风险；不修改社区发帖接口、评论逻辑、分享链接逻辑、报告计算、路由、数据库或后端业务流程。
- 前端验证：先运行 `npm.cmd test -- --run src/__tests__/views/community/CommunityView.test.js src/__tests__/components/community/PostEditor.test.js src/__tests__/components/community/ShareReportDialog.test.js` 失败于旧 `success` 事件仍被监听/发出；修复后 `npm.cmd test -- --run src/__tests__/views/community/CommunityView.test.js src/__tests__/components/community/PostEditor.test.js src/__tests__/components/community/ShareReportDialog.test.js src/__tests__/views/InterviewReportView.test.js` 通过，4 个测试文件 / 25 个用例通过；`npm.cmd run build` 通过；生产组件/页面静态扫描 `@success` 与 `emit('success')` 无匹配。
- 关联任务文件：`frontend/tasks/TASK_COMMUNITY_SUCCESS_EVENT_OWNERSHIP_FRONTEND.md`。
- 停止说明：本轮只完成当前重复成功提示防复发修复，不继续推进其它社区能力或页面重构。
## 社区浮动按钮图标与动效优化（2026-05-25）
- 当前阶段：本轮已完成社区首页右下角刷新/发帖浮动按钮的图标放大、浅色背景和 hover 动效优化，等待人工视觉验收。
- 已完成内容：`CommunityView.vue` 将刷新和发布帖子按钮的 `FeatureIcon` 从 `sm` 调整为 `md`；按钮底色从深橙实心渐变圆改为浅暖色表面、细边框和柔和阴影；刷新按钮在 hover 和刷新中执行图标旋转；发布帖子按钮在 hover 时轻微上移；补齐 `prefers-reduced-motion` 降级。
- 交互边界：本轮只优化社区首页两个浮动按钮的视觉和动效；不修改帖子列表、发帖弹窗业务、社区接口、评论、分享、路由、数据库或后端业务流程。
- 前端验证：先运行 `npm.cmd test -- --run src/__tests__/views/community/CommunityView.test.js` 失败于按钮仍使用 `size="sm"` 且缺少浅色 FAB 样式与 hover 动效约束；修复后同命令通过，1 个测试文件 / 18 个用例通过；`npm.cmd run build` 通过；静态扫描确认 `.fab-button` 不再使用 `var(--orange-main)` 到 `var(--orange-deep)` 的深橙渐变背景。
- 关联任务文件：`frontend/tasks/TASK_COMMUNITY_FAB_VISUAL_POLISH_FRONTEND.md`。
- 停止说明：本轮只完成当前浮动按钮可读性和动效优化，不继续推进其它社区页面重构。

## 设置中心账号注销弹窗倒计时修复（2026-05-25）
- 当前阶段：本轮已完成设置中心账号注销倒计时交互修复、红灯复现、定向测试和生产构建，等待人工验收。
- 问题原因：`accountDeleteCountdown` 初始值为 15，且注销表单按钮也使用该值禁用；但倒计时只在二次确认弹窗打开时启动，导致用户未输入密码前就看到等待状态，且因为表单按钮被禁用无法打开弹窗启动计时。
- 已完成内容：`SettingsView.vue` 将倒计时初始值改为 0；表单区“确认注销”按钮只受安全问题加载/错误状态影响；表单校验通过后直接打开二次确认弹窗；弹窗打开时从 15 秒开始倒计时，关闭弹窗或切换安全操作时清理定时器并重置倒计时。
- 交互边界：本轮只处理账号注销前端倒计时位置与按钮禁用逻辑；不修改账号注销 API、鉴权、数据库、后端清理流程、其它设置项或 `/admin/**`。
- 前端验证：先运行 `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 失败于表单按钮仍被倒计时禁用、倒计时初始仍为 15、表单无法打开弹窗；修复后同命令通过，1 个测试文件 / 27 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_SETTINGS_ACCOUNT_DELETE_COUNTDOWN_FRONTEND.md`。
- 停止说明：本轮只完成当前账号注销倒计时交互修复，不继续推进其它页面或后端逻辑。

## 设置页面 Naive UI 重构与路由转场动画（2026-05-27）
- 当前阶段：本轮已完成用户端设置页视觉重构、面试偏好子导航、低风险 Naive UI 控件迁移、路由转场动画和首页 `MotionConfig` 非元素根节点 warning 修复，等待人工浏览器复验。
- 已完成内容：`MainLayout.vue` 为非 KeepAlive 路由增加 `page-fade` 转场，并使用 `.page-fade-route` 真实 DOM 包裹层承载动画，避免首页根节点为组件时触发 Vue Transition warning；`SettingsView.vue` 将面试偏好拆成“面试偏好 / 语音通话 / 离线增强”子导航，移除旧折叠块，按钮、标签、开关、下拉、滑块、提示等低风险控件迁移到 Naive UI；侧边栏、信息网格、偏好行和子 tab 补充克制微交互；`App.vue` 补充 Naive UI Switch、Slider 橙色主题 token。
- 交互边界：本轮只处理用户端设置页展示和主布局转场；不修改 `/admin/**`、后端、API、路由定义、数据库、鉴权、账号注销业务逻辑或高风险 Element Plus 表单/弹窗。
- 前端验证：`npm.cmd test -- --run src/__tests__/layouts/MainLayout.test.js src/__tests__/views/SettingsView.test.js` 通过，2 个测试文件 / 33 个用例通过；`npm.cmd run build` 通过，仅保留既有 `@vueuse/core` PURE annotation 构建提示。
- 关联任务文件：`frontend/tasks/TASK_SETTINGS_PAGE_NAIVE_UI_ROUTE_TRANSITION_FRONTEND.md`。
- 停止说明：本轮只完成设置页和路由转场范围，不继续重构其它用户端页面。

## 登录过期重复弹窗修复（2026-05-25）
- 当前阶段：本轮已完成用户端登录过期重复提示修复、红灯复现和定向测试，等待生产构建与人工验收。
- 问题原因：用户端 `request.js` 的 HTTP 401 分支没有并发去重锁；当登录过期后页面同时发出多个接口请求，每个 401 都会各自清理 token、弹出“登录已过期，请重新登录”并跳转登录页，导致连续弹窗三次。
- 已完成内容：在用户端统一请求封装中新增按失效 token 去重的 `handleUnauthorized`；HTTP 401 和业务码 `code === 401` 共用同一登录过期处理入口；同一个失效 token 只弹一次、只清理一次、只跳转一次，用户重新登录拿到新 token 后仍可正常处理下一次过期。
- 交互边界：本轮只处理用户端登录过期重复弹窗；不修改登录页、后端 token 签发、管理端鉴权、路由结构、数据库或其它业务错误提示。
- 前端验证：先运行 `npm.cmd test -- --run src/__tests__/utils/request.test.js` 失败于三个并发 HTTP 401 触发 3 次提示；修复后同命令通过，1 个测试文件 / 2 个用例通过；相关回归 `npm.cmd test -- --run src/__tests__/utils/request.test.js src/__tests__/utils/adminRequest.test.js src/__tests__/api/resumePdf.test.js` 通过，3 个测试文件 / 6 个用例通过；`npm.cmd run build` 通过。

## 前端图片与图标性能优化（2026-05-25）
- 当前阶段：本轮已完成用户端大图 WebP 资源、首页背景移动端资源、图标按需加载、定向测试和生产构建，等待人工网络面板验收。
- 已完成内容：新增 `src/assets/optimized/` 下的 `background-desktop.webp`、`background-mobile.webp`、`logo.webp`、`assistant.webp`、`user.webp`；新增 `optimizedImages.js` 和 `OptimizedImage.vue`，让 Logo、登录页品牌图、用户头像、AI 面试官头像走 WebP 优先 + PNG fallback；首页背景改为 CSS `image-set()`，移动端切换到较小背景；`FeatureIcon` 保留现有接口，把首屏关键图标保留同步映射，非首屏业务图标通过 `loadFeatureIconSource()` 动态加载，并排除关键图标的重复动态导入。
- 交互边界：本轮只处理前端静态图片和业务插画图标加载策略；不修改路由、API、后端、数据库、业务流程、不新增 UI 库、不推进工具型图标 SVG 化、不配置 Service Worker 或服务端缓存头。
- 前端验证：`npm.cmd test -- --run src/__tests__/components/common/FeatureIcon.test.js src/__tests__/utils/featureIcons.test.js src/__tests__/utils/optimizedImages.test.js src/__tests__/views/HomePageView.test.js src/__tests__/components/AppHeader.test.js` 通过，5 个测试文件 / 23 个用例通过；`npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/SettingsView.test.js src/__tests__/components/community/PostEditor.test.js src/__tests__/views/community/CommunityView.test.js` 通过，4 个测试文件 / 68 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_USER_PNG_ICON_PERFORMANCE_FRONTEND.md`。
- 停止说明：本轮只完成当前图片与图标性能优化，不继续推进 SVG 替换、CDN 缓存策略或其它页面重构。

## OptimizedImage 比例失衡修复（2026-05-25）
- 当前阶段：本轮已完成 `<picture>` 包装层比例失衡修复、定向测试和生产构建，等待人工视觉复验。
- 问题原因：WebP 资源本身保持原始比例，但 `OptimizedImage.vue` 新增的 `<picture>` 中间盒参与布局，部分页面原样式只约束 `img` 或依赖图片作为容器直接子元素，导致 Logo、头像、面试头像等图片按固有尺寸撑大。
- 已完成内容：为 `OptimizedImage.vue` 的 `<picture>` 增加 `optimized-picture` 类，并设置 `display: contents`，让包装层不产生额外布局盒，恢复原有 `img` 尺寸与 `object-fit` 规则；补充 `optimizedImages.test.js` 静态断言防回退。
- 交互边界：本轮只修复图片包装层布局问题；不修改资源生成尺寸、不调整页面布局、不改业务逻辑、路由、API、后端或数据库。
- 前端验证：`npm.cmd test -- --run src/__tests__/utils/optimizedImages.test.js src/__tests__/components/AppHeader.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/HomePageView.test.js` 通过，5 个测试文件 / 63 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_USER_PNG_ICON_PERFORMANCE_FRONTEND.md`。
- 停止说明：本轮只修复当前图片比例失衡问题，不继续推进其它图片策略或页面重构。
- 关联任务文件：`frontend/tasks/TASK_AUTH_EXPIRED_PROMPT_DEDUPE_FRONTEND.md`。
- 停止说明：本轮只完成登录过期重复弹窗修复，不继续推进其它鉴权能力或页面改造。
## 用户端 PNG 图标性能优化（2026-05-25）
- 当前阶段：本轮已完成用户端 PNG 图标 WebP fallback、首屏加载优先级、静态测试和生产构建，等待人工网络面板验收。
- 已完成内容：`FeatureIcon` 新增 `critical`、`loading`、`fetchPriority` 可选参数，默认继续懒加载，首屏关键图标自动 eager/high priority；组件改为 `<picture>`，优先加载 WebP，PNG 保留 fallback；`featureIcons.js` 保持原有语义 key 与 `getFeatureIcon()`，新增 `featureIconSources` 和 `getFeatureIconSource()`；导航栏、Logo、首页 hero 徽标/按钮/快捷入口标记为关键资源；新增 94 个 WebP 图标资源，PNG 原图全部保留。
- PNG 收纳调整：按验收反馈，`src/assets/feature-icons/old/` 与 `src/assets/feature-icons/new/` 下的 94 个 PNG 已统一移入 `src/assets/feature-icons/png-fallback/old/` 与 `src/assets/feature-icons/png-fallback/new/`；运行主目录只保留 WebP，PNG fallback 仍由 `featureIcons.js` 统一解析。
- 性能结果：`feature-icons` 原 PNG 94 个合计约 6.01MB；新增 WebP 94 个合计约 0.73MB；生产构建中 WebP 图标大多为 5KB 到 12KB，PNG fallback 仍可被构建解析。
- 交互边界：本轮只处理用户端图标资源格式、fallback 和加载优先级；不修改 `/admin/**`、API、路由、数据库、后端缓存头、Service Worker、SVG 替换、其它大图资源或业务逻辑。
- 前端验证：`npm.cmd test -- --run src/__tests__/components/common/FeatureIcon.test.js src/__tests__/utils/featureIcons.test.js src/__tests__/components/AppHeader.test.js` 通过，3 个测试文件 / 14 个用例通过；`npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 通过，1 个测试文件 / 5 个用例通过；`npm.cmd test -- --run src/__tests__/components/common/FeatureIcon.test.js` 通过，1 个测试文件 / 6 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_USER_PNG_ICON_PERFORMANCE_FRONTEND.md`。
- 停止说明：本轮只完成当前 PNG 图标性能优化，不继续推进 CDN/缓存头落地、Service Worker、全站图片压缩或图标体系替换。

## 模拟面试语音稳定化：系统本地优先与离线增强入口预留（2026-05-26）
- 当前阶段：本轮已完成模拟面试前端语音稳定化的阶段性交付，范围限定为系统本地语音识别优先、错误结构化、系统 TTS 状态、设置中心离线增强入口和离线模型缓存边界预留，等待人工验收。
- 已完成内容：`useSpeechToText.js` 在创建 `SpeechRecognition` 后尝试启用 `processLocally`，并新增 `engineStatus`、`supportsLocalProcessing`、`offlineEngineSuggested`、`errorCode`；`useVoiceCall` 与 `InterviewSessionView.vue` 保持既有语音通话、静音、手动停止收听并发送、AI 播报暂停收音链路，STT 失败时退出语音通话但保留文字输入；`useTextToSpeech.js` 继续使用浏览器原生 `speechSynthesis`，新增系统 TTS 状态，不接入 Kokoro。
- 设置与偏好：`SettingsView.vue` 新增“离线增强”设置块，展示 STT 当前引擎、系统本地能力检测、麦克风权限提示、`sherpa-onnx` 后续入口说明和 Kokoro 高品质语音包后续说明；`settingsPreferences.js` 新增 `voiceRecognitionEngine`、`offlineSttEngine`、`offlineTtsEngine` 默认值与非法值归一；新增 `offlineVoiceModelCache.js`，仅提供本地模型状态元数据读写和存储能力检测，不下载真实模型。
- 交互边界：本轮不新增后端 API、不修改数据库字段、不修改现有面试消息接口；不真实下载或接入 `sherpa-onnx` / Kokoro，不新增大体积依赖、Worker、CDN 托管或 Vite 静态资源策略。
- 前端验证：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/settingsPreferences.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，7 个测试文件 / 95 个用例通过；`npm.cmd run build` 通过，仅保留既有 `@vueuse/core` pure annotation 提示。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只完成当前语音稳定化阶段，不继续推进真实离线 STT/TTS 模型集成、音频 Worker、CDN 配置、后端改造或新的面试能力。

## 模拟面试语音识别 language-not-supported 回归修复（2026-05-26）
- 当前阶段：本轮已完成 `processLocally` 本地识别语言包缺失导致语音识别不可用的回归修复，等待人工复测。
- 问题原因：浏览器允许设置 `recognition.processLocally = true` 不代表设备已安装 `zh-CN` 本地语音识别包；当本地包缺失时，Web Speech 会在启动后抛出 `language-not-supported`。原逻辑把该错误当成最终不可用，导致语音通话直接降级为手动输入。
- 已完成内容：`useSpeechToText.js` 在本地识别触发 `language-not-supported` 时自动切换为 `browser-service`，释放当前本地识别实例并重新启动浏览器服务识别；本页面生命周期内不再重复尝试本地识别，避免错误循环。`useSpeechToText.test.js` 增加本地语言不支持时自动回退并继续录音的回归测试。
- 交互边界：本轮只修复 STT 本地识别语言不支持的回退策略；不接入真实 sherpa-onnx / Kokoro，不新增 Worker、CDN、后端接口或数据库字段。
- 前端验证：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js` 通过，1 个测试文件 / 15 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，3 个测试文件 / 51 个用例通过；`npm.cmd run build` 通过，仅保留既有 `@vueuse/core` pure annotation 提示。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只完成当前语音识别回归修复，不继续推进下一阶段离线模型能力。

## 模拟面试 STT 本地探测与 TTS 首次无声修复（2026-05-26）
- 当前阶段：本轮已完成浏览器原生 STT/TTS 启动策略修复，等待人工在 Chrome、Edge 和移动端复测。
- 问题原因：`processLocally = true` 不是稳定的系统识别强制开关，浏览器接受赋值也不代表当前语言的本地识别包已经安装；强行启用会导致有麦克风占用标识但无识别结果。TTS 侧 Chrome/Android 首次加载时 `speechSynthesis.getVoices()` 可能为空，立即播报开场白存在无声风险。
- 已完成内容：`useSpeechToText.js` 启动前先调用 `SpeechRecognition.available({ langs: [language], processLocally: true })` 探测本地语言包，只有返回 `available` 才启用 `processLocally`，否则直接走浏览器服务识别；`useTextToSpeech.js` 在播报前刷新并等待 voices 加载，同时调用 `speechSynthesis.resume()` 唤醒合成器。补充 STT 本地语言包不可用、TTS voices 异步加载的回归测试。
- 交互边界：本轮只修复浏览器原生语音启动链路；不接入真实 sherpa-onnx / Kokoro，不新增音频 Worker、CDN 配置、后端接口或数据库字段。若浏览器服务识别因网络、地区或浏览器策略不可用，仍需要进入下一阶段真实离线 STT 模型集成。
- 前端验证：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useTextToSpeech.test.js` 通过，2 个测试文件 / 27 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，2 个测试文件 / 36 个用例通过；`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/settingsPreferences.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，7 个测试文件 / 98 个用例通过；`npm.cmd run build` 通过，仅保留既有 `@vueuse/core` pure annotation 提示。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只完成当前原生语音启动策略修复，不继续推进真实离线模型能力。
## 模拟面试离线语音下载修复：内置 manifest 与 runtime（2026-05-27）
- 当前阶段：本轮已把离线 STT 下载入口从“提示清单未部署”推进为前端静态目录内置 `manifest.json` 和 `runtime.js`，等待人工在可访问 Hugging Face 资源的浏览器环境中执行真实下载和语音识别复测。
- 问题原因：上轮虽然把 `Unexpected token '<'` 转成了明确的“清单不是 JSON”提示，但项目 `public` 目录仍没有 `/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json`，所以下载按钮必然命中 SPA HTML fallback，用户仍无法下载资源包。
- 已完成内容：新增 `frontend/app/public/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json`，声明 sherpa-onnx 官方浏览器 ASR JS、WASM、data 资源；新增 `runtime.js`，在 Worker 内从浏览器 Cache API 读取这些资源并通过 Blob URL 加载，适配现有 `start/acceptWaveform/stop` 识别接口；`offlineVoiceModelCache.js` 支持 manifest 文件项远程 `url` 和 `runtime` 状态持久化；`useSpeechToText.js` 使用状态中的 runtime 初始化 Worker；`sherpaSpeechWorker.js` 等待 runtime 初始化，并改回 classic worker 以支持 `importScripts`；`useTextToSpeech.js` 补齐每条 utterance watchdog，浏览器不触发 `onend/onerror` 时会主动释放播报状态；`InterviewSessionView.test.js` 同步覆盖 TTS 结束后 1.5 秒尾音保护再恢复收音。
- 交互边界：本轮不新增后端 API、不上传用户音频、不提交大体积模型二进制文件；大体积 sherpa-onnx 资源仍在用户点击下载时按需从官方 Hugging Face Space 拉取并缓存到当前浏览器。若部署环境无法访问该外部地址，需要后续把同一批资源镜像到自有静态目录或 CDN。
- 前端验证：`npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，6 个测试文件 / 112 个用例通过；`npm.cmd run build` 通过，仍保留既有 `@vueuse/core` PURE annotation 构建提示。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只补齐离线 STT 下载清单和 runtime 加载，不继续扩展 Kokoro、后端模型代理、CDN 镜像或新的面试能力。

## 模拟面试离线语音下载修复：模型清单 HTML fallback 友好报错（2026-05-26）
- 当前阶段：本轮已修复离线语音引擎下载时 manifest 路径返回 HTML 导致 `Unexpected token '<'` 的问题，等待人工在未部署模型目录和已部署 manifest 的浏览器环境中复测。
- 问题原因：`/voice-models/sherpa-onnx/zh-cn-streaming/manifest.json` 未部署或被 SPA fallback 接管时会返回 `index.html`，旧缓存工具直接执行 `response.json()`，导致浏览器抛出底层 JSON 解析错误，且下载状态没有稳定写入 failed。
- 已完成内容：`offlineVoiceModelCache.js` 改为先读取 manifest 响应文本并检测 `Content-Type: text/html`、`<!DOCTYPE html>`、`<html>`，命中时提示“离线语音模型清单不是 JSON，请确认模型文件已部署到 ...”；JSON 损坏时提示“清单解析失败，请检查 ...”；`downloadModelFromManifest` 在清单失败时写入 failed 状态，便于设置页展示失败并允许用户删除资源包状态。
- 交互边界：本轮不新增模型文件、不新增后端 API、不修改静态资源托管策略、不接入 Kokoro；如果要真正下载成功，仍需要把 sherpa-onnx manifest 与模型资源部署到 `/voice-models/sherpa-onnx/zh-cn-streaming/`。
- 前端验证：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js` 已通过，后续继续执行设置页回归和生产构建。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理离线模型清单错误提示和 failed 状态，不继续扩展语音识别主链路或模型托管能力。
## 设置页视觉重构、等高布局与图标预加载（2026-05-28）
- 当前阶段：已完成用户端设置页工作台式视觉重构、桌面端等高布局、设置高频图标 critical 预加载和路由承载容器伸展规则，等待人工视觉复测。
- 完成内容：`SettingsView.vue` 补齐所有设置 section 的 `settings-panel-body`，新增 `settings-workspace`、账号概览卡、短面板说明区、柔和侧边栏选中态和信息项强调线；`MainLayout.vue` 的 `.page-fade-route` 支持 flex 伸展；`featureIcons.js` 将设置页高频图标加入同步 critical 资源集，设置导航图标使用 eager 加载。
- 交互边界：本轮只处理用户端设置页、图标加载策略和低风险布局承载；不修改 `/admin/**`、API、路由定义、后端、数据库、账号注销逻辑和现有表单验证逻辑。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js src/__tests__/utils/featureIcons.test.js src/__tests__/components/common/FeatureIcon.test.js src/__tests__/layouts/MainLayout.test.js` 通过，4 个测试文件 / 44 个用例通过；`npm.cmd run build` 在清理 `dist/voice-models` 时遇到 Windows `EPERM` 权限错误；`npm.cmd run build -- --emptyOutDir=false` 通过，确认源码、模板和样式可生产构建。
- 关联任务文件：`frontend/tasks/TASK_SETTINGS_WORKSPACE_VISUAL_REFACTOR_FRONTEND.md`。
## 社区收藏反馈与个人中心视觉重构（2026-05-28）
- 当前阶段：已完成用户端社区帖子收藏 active 状态增强和个人中心 `/dashboard` 工作台式视觉重构，等待人工视觉复测。
- 完成内容：`PostCard.vue` 将收藏状态从仅文字变色升级为明显的 active pill，补充浅橙/金色背景、边框、阴影、图标放大、hover/press/focus 反馈，并保持 `favorite` 事件和帖子跳转隔离；`DashboardView.vue` 新增 `.profile-workbench`、`.quota-overview`、`.stats-section`、`.growth-entry-card` 等结构，放大本月诊断、本月面试、剩余额度、成长中心入口和查看全部箭头图标，去掉统计/额度图标硬方框，最近简历诊断和最近模拟面试记录项图标结构保持不变；低风险加载与按钮控件使用 `NSkeleton`、`NButton`。
- 交互边界：本轮只处理用户端前端展示、图标可读性和轻量动效；不修改社区收藏接口、收藏数量字段、个人中心额度计算、后端、数据库、路由定义和 `/admin/**`。
- 前端验证：`npm.cmd test -- --run src/__tests__/components/community/PostCard.test.js src/__tests__/views/DashboardView.test.js src/__tests__/views/community/CommunityView.test.js` 通过，3 个测试文件 / 25 个用例通过；`npm.cmd run build` 通过，仅保留既有 `@vueuse/core` PURE annotation Rollup 提示。
- 关联任务文件：`frontend/tasks/TASK_DASHBOARD_AND_COMMUNITY_FAVORITE_UI_REFACTOR_FRONTEND.md`。
## 会员中心视觉重构（2026-05-28）
- 当前阶段：已完成用户端 `/membership` 会员与额度工作台式视觉重构，等待人工视觉复测。
- 完成内容：`MembershipView.vue` 将顶部从大渐变 Banner 改为 `.membership-workbench-hero`，包含 `.membership-status-panel` 与 `.membership-quota-strip`；套餐区改为 `.plan-comparison-section`，保留当前套餐“续费”和非当前套餐“立即升级”逻辑；低风险展示控件迁移到 `NButton`、`NTag`、`NSkeleton`，`ElMessage` 继续保留。
- 视觉与动效：移除旧 `hero-orb` 装饰，图标提升到可读尺寸且不使用硬方框；页面进入、套餐卡和额度项只使用轻量 `opacity/transform/box-shadow/border-color` 反馈，补齐 `prefers-reduced-motion` 和暗色变量适配。
- 交互边界：本轮只处理用户端会员中心前端展示；不修改 `/api/membership/plans`、`/api/membership/upgrade/mock`、会员额度计算、到期顺延、用户信息 store、后端、数据库、路由定义和 `/admin/**`。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/MembershipView.test.js` 通过，1 个测试文件 / 5 个用例通过；生产构建待本轮最终验证记录。
- 关联任务文件：`frontend/tasks/TASK_MEMBERSHIP_CENTER_UI_REFACTOR_FRONTEND.md`。
## 会员套餐配置链路修复（2026-05-28）
- 当前阶段：已修复管理端会员套餐配置无法驱动用户端会员中心展示和实际 VIP 每日额度的问题，等待人工在管理端创建/启用套餐后复测。
- 完成内容：用户端 `/membership` 取消前端生成套餐标签和固定介绍词，改为展示管理端 `description`；公开套餐最多展示 6 个；后端公开套餐接口返回管理端配置的介绍词和简历/面试额度；会员订单快照记录套餐额度；VIP 每日额度检查、扣减、退回和用户信息剩余额度基于当前有效套餐配置计算；管理端创建、单个启用、批量启用限制最多 6 个启用套餐。
- 交互边界：不新增数据库字段，不新增 tag 字段，不改会员购买接口路径，不改路由，不重构管理端会员套餐页面结构。
- 验证：`npm.cmd test -- --run src/__tests__/views/MembershipView.test.js` 通过，6 个用例通过；`mvn.cmd -q "-Dtest=MembershipServiceImplTest,AdminMembershipControllerTest,AuthServiceImplTest" test` 通过；`mvn.cmd -q test -DskipTests` 通过；`npm.cmd run build` 通过，仅保留既有 `@vueuse/core` PURE annotation Rollup 提示。
- 关联任务文件：`frontend/tasks/TASK_MEMBERSHIP_PLAN_CONFIG_CHAIN_FIX.md`。

## 社区帖子标题与面试报告链接分享修复（2026-05-28）
- 当前阶段：已完成社区发帖标题、个人动态默认加载 5 条、面试报告社区链接分享和相关前后端验证，等待人工验收。
- 完成内容：`PostEditor` 增加帖子标题输入和必填提交；`PostCard`、`PostDetailView`、`MyActivity` 展示帖子标题；报告分享帖根据 `sharedInterviewSessionId` 渲染“查看完整面试报告”的站内链接卡片；`ShareReportDialog` 不再把完整报告摘要拼接为社区正文，只提交报告标题、简短说明和会话 ID；`MyActivity` 默认分页大小从 2 改为 5。
- 交互边界：本轮只处理社区标题、个人动态加载数量和报告链接分享；不新增富文本编辑、报告下载、公开报告列表、路由定义、管理端页面或其它社区扩展能力。
- 安全边界：后端已校验报告分享只能绑定发布者自己的面试会话；其他用户只能在报告所有者存在未删除社区分享帖时查看报告页必要字段，不返回聊天记录、复盘轮次和岗位上下文。
- 数据库执行：已执行过旧版 TASK_56 的目标库应执行 `server/db/migrations/TASK_59_COMMUNITY_POST_TITLE_AND_REPORT_LINK_INCREMENTAL.sql`；`alter_v1.4_add_community_tables.sql` 只适合建表场景，不负责给已有表补本轮字段。
- 前端验证：`npm.cmd test -- --run src/__tests__/components/community/PostEditor.test.js src/__tests__/components/community/PostCard.test.js src/__tests__/components/community/ShareReportDialog.test.js src/__tests__/views/community/MyActivity.test.js src/__tests__/views/community/PostDetailView.test.js` 通过，5 个测试文件 / 13 个用例通过；`npm.cmd run build` 通过。
- 后端验证：`mvn.cmd test "-Dtest=CommunityServiceValidationTest,CommunityServicePostQueryDeleteTest,InterviewServiceTest,CommunityServiceLikeFavoriteTest,CommunityServiceInteractionTest,CommunityServiceReceivedInteractionsEmptyTest"` 通过，79 个用例通过；`mvn.cmd compile` 通过。
- 关联任务文件：`frontend/tasks/TASK_COMMUNITY_POST_TITLE_AND_REPORT_LINK_FRONTEND.md`；后端任务文件：`task-社区帖子标题与报告分享链接修复.md`。
- 停止说明：本轮功能已停止，等待验收，不继续推进下一项社区能力。
# 前端性能缓存、路由切换与动画优化（2026-05-29）

## 已完成且已验证的功能

- 新增轻量 GET 缓存工具，公共统计、会员套餐、岗位配置、成长中心概览、通知未读数和社区列表等稳定数据已接入 TTL 缓存。
- 社区发帖、点赞、收藏、评论以及会员升级等主动操作后，已按业务域精确失效相关缓存。
- 用户端高频路由 idle 预热已扩展到 `/resume/upload`、`/interview/entry`、`/offer`、`/templates`、`/community`、`/growth`，并保留 hover、touch、focus 预加载。
- `KeepAlive` 已扩展到 `DashboardView`、`SettingsView`、`MembershipView`、`InterviewHistoryView`、`HistoryView`，并补齐路由 `meta.keepAlive` 与组件名称。
- 用户端核心页面和组件中的 `transition: all` 已收敛为明确动画属性，避免误动画布局属性造成额外 layout/paint。
- 社区列表和个人动态等长列表区域已补充 `content-visibility: auto` 或渲染隔离策略。

## 本轮完成状态

- `npm.cmd test -- --run src/__tests__/router/routeLoaders.test.js src/__tests__/layouts/MainLayout.test.js src/__tests__/utils/apiCache.test.js src/__tests__/api/performanceCache.test.js` 通过：4 个测试文件，12 个用例。
- `npm.cmd test -- --run src/__tests__/views/MembershipView.test.js src/__tests__/views/ResumeHistoryView.test.js src/__tests__/components/community/PostEditor.test.js src/__tests__/components/community/ShareReportDialog.test.js` 通过：4 个测试文件，15 个用例。
- `npm.cmd test -- --run` 通过：65 个测试文件，413 个用例。
- `npm.cmd run build` 通过，Vite 生产构建和 gzip 产物生成成功。
- 当前剩余 `transition: all` 搜索结果集中在管理端页面，本轮按用户端高频路径控制范围，未扩大到管理端视觉回归。

## 尚未开始的功能

- 未新增 Service Worker、虚拟列表库、SSR、CDN 配置、接口协议改造或新的前端状态库。

## 停止，不继续下一个功能

本轮前端性能缓存、路由切换、KeepAlive、动画属性收敛和长列表渲染隔离已完成并验证，等待验收，不继续扩大到下一阶段架构优化。
## 修复离线 sherpa-onnx Worker 初始化 DataCloneError（2026-05-30）
- 当前阶段：本轮定位到浏览器控制台硬错误 `DataCloneError: Failed to execute 'postMessage' on 'Worker': [object Array] could not be cloned`，离线 sherpa-onnx 在 Worker `init` 消息发送阶段就失败，真实识别器尚未进入初始化。
- 问题原因：`useSpeechToText.js` 将 `modelStatus.value.files` 直接放入 Worker 初始化配置；`modelStatus` 是 Vue `ref`，嵌套 `files` 数组会变成响应式代理，浏览器 Worker `postMessage` 的 structured clone 无法克隆该代理数组，导致离线资源包下载完成后仍完全不可用。
- 已完成内容：`useSpeechToText.js` 在发送 Worker `init` 前把模型文件清单转换为纯 JSON 对象数组，并捕获 `postMessage` 同步异常，统一进入 `offline-worker-error`；Worker runtime URL 增加版本参数，避免浏览器继续使用旧 runtime 脚本；`voiceModelDevServer.js` 与 `vite.config.js` 为本地模型资源返回正确 MIME，`.js` 使用 `text/javascript`、`.wasm` 使用 `application/wasm`；`runtime.js` 在从 Cache 取出旧资源时重新包装 Blob MIME，避免已缓存的 `application/octet-stream` JS 继续阻断 `importScripts`。
- 前端验证：新增 RED 回归测试复现 `structuredClone` 对响应式 `files` 数组抛 `DataCloneError`；修复后 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，6 个测试文件 / 88 个用例通过。
- 停止说明：本轮只修复前端离线 STT Worker 初始化消息不可克隆和本地模型资源 MIME 问题，不新增后端 API、不上传用户音频、不替换模型包。
## 修复 Chrome 默认中文音色与设置页排版挤压（2026-05-31）

- 当前阶段：本轮已修复 Chrome 系统 TTS 在只暴露单一明确性别中文 voice 时，默认中文自然音色被强行绑定到该 voice，导致默认、男声、女声听感无区别的问题；同时修复设置页“AI 播报声音”实际音色提示挤压主行布局的问题。
- 问题原因：Chrome 的 `speechSynthesis.getVoices()` 在部分 Windows 环境中可能只返回 `Microsoft Kangkang - Chinese (Simplified, PRC)` 这类单一性别本地 voice。旧逻辑会把它当作默认自然中文 voice 使用，并把实际 voice 提示直接放在同一 flex 行里，造成音色无感知差异和 UI 横向拥挤。
- 已完成内容：`useTextToSpeech.js` 新增单一性别 voice 默认保护，`natural_zh` 不再主动绑定唯一明确性别的本地中文 voice；`female/male` 缺失对应性别时不再强行使用相反性别 voice；手势试听等待超时后仍允许回退本地老式 voice，避免只剩远程 Google voice 时无声。`SettingsView.vue` 将实际音色提示移动到主控件下方独立行，并优化提示文案。
- 交互边界：本轮不会凭空生成 Chrome 没有暴露的男声或女声，也不通过 pitch 伪造性别；如果 Chrome 只有一个系统中文 voice，最终听感仍受浏览器和系统 voice 列表限制。真实跨 Chrome/Edge 一致音色仍依赖正式 Kokoro 模型资源或浏览器可见的系统男女声。
- 前端验证：`npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js` 通过，32 个用例通过；`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，46 个用例通过；相关回归 `npm.cmd test -- --run src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useOfflineTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，4 个测试文件 / 117 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。
- 停止说明：本轮只处理 Chrome 系统 TTS voice 选择与设置页排版，不继续推进真实模型托管、后端代理、云端 TTS 或新增更多音色。
## 真实 Kokoro 本地模型接入（2026-05-31）
- 当前阶段：已完成 Kokoro 真实本地模型开发期接入。`voice-models-local/kokoro/zh-cn-dual` 下的 sherpa-onnx Kokoro v1.1 多语言模型可通过 Vite 同源端点生成 24kHz WAV，前端 `tts:kokoro` manifest 可从占位包升级为 `placeholder: false`。
- 已完成内容：新增 Kokoro 本地合成端点 `/voice-models/kokoro/zh-cn-dual/synthesize`；新增 `synthesisEndpoint` manifest/status 字段；旧占位 ready 缓存不再跳过新 manifest；`useOfflineTextToSpeech` 优先调用同源 WAV 端点播放真实模型输出；女声映射 `sid=3`，男声映射 `sid=58`。
- 验证结果：`npm.cmd test -- --run src/__tests__/utils/settingsPreferences.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/kokoroTtsDevServer.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useOfflineTextToSpeech.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，7 个测试文件 / 147 个用例通过；`npm.cmd run build` 通过；本地请求 `http://127.0.0.1:5174/voice-models/kokoro/zh-cn-dual/synthesize` 已生成 `RIFF/WAVE` 测试文件。
- 尚未开始的内容：未做云端 TTS、未新增 Java 后端生产接口、未上传用户音频、未扩展第三个及更多 Kokoro 音色、未把 200MB+ 大模型文件纳入源码提交。
- 停止说明：本轮只处理真实 Kokoro 本地模型部署与试听链路，等待人工在 Chrome/Edge 设置页重新下载资源包并试听验收，不继续推进下一阶段生产托管方案。

## 撤销 Kokoro 高品质离线音色包，仅保留 sherpa-onnx 语音识别（2026-05-31）
- 当前阶段：本轮已按用户要求撤销高品质离线音色包/Kokoro 本地合成链路，项目只保留语音识别刚需能力。
- 已完成内容：设置页离线增强区只保留 sherpa-onnx 语音识别引擎；移除 Kokoro 下载、删除、试听、合成端点、dev server、composable、资源目录和依赖；浏览器播报回到系统 TTS；偏好清洗逻辑会丢弃旧 `offlineTts*` 字段。
- 保留内容：`public/voice-models/sherpa-onnx/zh-cn-streaming`、`voice-models-local/sherpa-onnx/zh-cn-streaming`、`scripts/download-sherpa-onnx-model.mjs`、`useSpeechToText.js`、`sherpaSpeechWorker.js` 和 `sherpa-onnx` 依赖继续保留，离线 STT 下载和 Worker 识别链路不被删除。
- 验证结果：目标回归 `npm.cmd test -- --run src/__tests__/utils/settingsPreferences.test.js src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/utils/voiceModelDevServer.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/workers/sherpaSpeechWorker.test.js` 通过，9 个测试文件 / 175 个用例；`npm.cmd run build` 通过。
- 残留扫描：Kokoro/音色包关键字在生产代码、脚本、静态资源和 `dist` 中无命中；Kokoro 资源目录扫描无结果；仅设置偏好测试保留 legacy `offlineTts*` 字段样本，用于确认旧缓存会被丢弃。
- 停止说明：本轮不继续推进任何离线 TTS、云端 TTS、后端语音合成接口或新音色能力。
## 设置中心短面板布局补全（2026-05-31）
- 当前阶段：已完成账号资料、离线增强和新手引导三个设置页短面板的前端布局补全，等待人工视觉验收。
- 已完成内容：账号资料在四个基础字段下补充账号权益、本机偏好和常用数据说明；离线增强改为“仅保留 sherpa-onnx 语音识别”的说明头部、识别引擎卡片和工作方式侧栏；新手引导补充简历诊断、模拟面试、模板与社区、会员与设置四个入口说明卡片。
- 边界说明：本轮不恢复离线 TTS、离线音色包、Kokoro、本地合成端点或后端语音接口；离线增强仍只保留语音识别引擎资源的下载、缓存状态和删除能力。
- 验证结果：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 40 个用例通过；`npm.cmd run build` 通过。
- 停止说明：本轮仅处理设置页已有区域的空白与说明密度，不继续推进新的设置项、会员能力或语音合成功能。
## 修复 AI 润色 PDF 近一页分页截断（2026-05-31）
- 当前阶段：本轮已修复 AI 简历润色结果页 PDF 导出在接近一页时被切成两页的问题，等待人工导出真实简历 PDF 复测观感。
- 问题原因：简历导出截图按 `190mm` 宽度生成，但写入 A4 PDF 时按 `210mm` 宽度铺满，接近一页的内容会被放大到略超 `297mm`，旧分页逻辑直接生成第二页，导致少量关键内容落到第二页。
- 已完成内容：`resumePdfPagination` 增加近一页轻微缩小并居中规则；只在缩放不超过 10% 时压成单页，更长简历继续分页；补充对应单元回归测试。
- 前端验证：`npm.cmd test -- --run src/__tests__/utils/resumePdfPagination.test.js` 通过；`npm.cmd test -- --run src/__tests__/utils/resumePdfPagination.test.js src/__tests__/views/ResumeResultView.test.js` 通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_23_V11_RESUME_AI_POLISH_FRONTEND.md`。
- 停止说明：本轮只处理 PDF 导出近一页截断，不继续扩展后端 PDF、DOCX、图片导出或模板重构。
## 修复 AI 润色模板预览空白（2026-05-31）
- 当前阶段：本轮已修复 AI 简历润色结果页“润色后的简历内容”区域在只有 `documentJson` / `editedPlainText`、缺少 `polishedResumeText` 时不渲染模板预览的问题，等待人工在真实记录中复测页面观感。
- 问题原因：5/29 的 AI 简历模板编辑器重构将预览区域渲染条件绑定到 `polishedResumeText`，导致保存后的结构化文档或编辑后纯文本存在时，模板组件仍可能被 `v-if` 隐藏；5/31 的 PDF 近一页分页修复只改了分页工具，不是本次截图问题的直接来源。
- 已完成内容：`ResultView.vue` 改为由 AI 原文、结构化文档、编辑后纯文本三类数据共同判断是否显示模板；`ResumeResultView.test.js` 补充对应回归测试，并 mock onboarding 完成接口以清理测试噪音。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/ResumeResultView.test.js` 通过；`npm.cmd test -- --run src/__tests__/views/ResumeResultView.test.js src/__tests__/utils/resumePdfPagination.test.js` 通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_23_V11_RESUME_AI_POLISH_FRONTEND.md`。
- 停止说明：本轮只处理 AI 润色模板预览空白，不继续扩展模板视觉、后端 PDF、DOCX、图片导出或 AI 润色内容生成。
## 修复 AI 润色模板预览 DataCloneError（2026-05-31）
- 当前阶段：本轮已修复 AI 润色结果页“润色后的简历内容”区域仍然空白的问题，等待用户在真实 AI 润色记录中复测模板预览。
- 问题原因：真实报错不是外层 `v-if`，而是 `ResumeTemplate.vue` 初始化编辑历史时把 Vue 响应式 Proxy 直接传入 `structuredClone()`，浏览器抛出 `DataCloneError` 后中断模板组件渲染。
- 已完成内容：`ResumeTemplate.vue` 的快照克隆逻辑改为先递归转成纯数据，再执行 `structuredClone()`；新增 `ResumeTemplate.test.js` 组件级回归测试，覆盖真实挂载模板时初始化历史快照并渲染姓名/求职意向。
- 前端验证：`npm.cmd test -- --run src/__tests__/components/resume/ResumeTemplate.test.js` 通过；`npm.cmd test -- --run src/__tests__/components/resume/ResumeTemplate.test.js src/__tests__/views/ResumeResultView.test.js src/__tests__/utils/resumePdfPagination.test.js` 通过，3 个测试文件 / 10 个用例通过；`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_23_V11_RESUME_AI_POLISH_FRONTEND.md`。
- 停止说明：本轮只处理 AI 润色模板预览运行时异常，不继续扩展导出、模板视觉或后端能力。
## 离线 STT 同源兜底模型源修复（2026-05-31）
- 当前阶段：已修复用户点击下载离线语音识别包仍提示“当前模型源不可用”的问题。根因是 ModelScope 静态文件可由服务端读取但缺少浏览器 CORS 响应头，HuggingFace 官方源在当前网络下可能超时，单纯增加第三方直链无法保证真实浏览器下载成功。
- 已完成内容：`manifest.json` 版本更新为 `sherpa-zh-en-remote-20260531-api-fallback`，候选源改为同源 `/api/offline-stt/models/<fileName>` 兜底地址加 HuggingFace 官方 Space；文件大小同步为当前官方 Space 资源大小；设置页文案改为约 200MB 并说明外部源不可用时使用本站同源兜底通道；Vite `/api` 开发代理超时调整为 600 秒。
- 后端配套：新增 `GET /api/offline-stt/models/{fileName}` 流式兜底接口，只允许四个 sherpa-onnx 白名单文件名，后端按 ModelScope、HuggingFace 固定顺序读取，不开放任意 URL 转发；安全配置放行该只读模型下载路径。
- 前端验证：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js` 通过，2 个测试文件 / 64 个用例；`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/views/SettingsView.test.js src/__tests__/composables/useSpeechToText.test.js` 通过，5 个测试文件 / 113 个用例；`npm.cmd run build` 通过。
- 后端验证：`mvn.cmd test "-Dtest=OfflineSttModelProxyServiceTest,OfflineSttModelProxyControllerTest,SecurityConfigTest"` 通过，11 个用例。
- 关联任务文件：`frontend/tasks/TASK_61_OFFLINE_STT_REMOTE_SOURCE_DOWNLOAD_PLAN_FRONTEND.md`、`tasks/TASK_61_OFFLINE_STT_MODEL_PROXY_BACKEND.md`。
- 停止说明：本轮只修复离线 STT 下载源不可用问题，不继续推进 R2/OSS/COS 托管、离线 TTS 或其它语音能力。
## 离线 STT ModelScope 直连备用源修复（2026-05-31）
- 当前阶段：本轮已修复离线语音识别包仍显示“当前模型源不可用”的候选源配置问题，生产 manifest 重新加入已验证 CORS 可用的 ModelScope 直连源，并保留同源 `/api` 兜底与 HuggingFace 官方末位备用。
- 问题原因：上一轮生产 manifest 只保留 `/api/offline-stt/models/<fileName>` 与 HuggingFace 官方 Space raw 地址；当前验证中 HuggingFace 官方 raw 返回 `401`，如果后端兜底接口未部署、未重启或出网异常，浏览器没有可用的第三个候选源，只能落到统一失败提示。
- 已完成内容：`manifest.json` 版本更新为 `sherpa-zh-en-remote-20260531-modelscope-direct-fallback`，候选顺序调整为 `modelscope.cn`、`www.modelscope.cn`、同源 `/api`、HuggingFace 官方；`offlineVoiceModelCache.js` 对每个失败候选源输出 `console.warn` 诊断；后端代理源列表补充非 `www` ModelScope 域名。
- 源站验证：ModelScope 小文件带 Origin 请求返回 `200` 与 `access-control-allow-origin: *`；`.data` 范围请求返回 `206 Partial Content` 与 `Content-Range: bytes 0-0/199059238`；HuggingFace 官方 raw 当前返回 `401`；`anyshu` 仓库可访问但文件包不完整或为 zip，不加入生产 manifest 以避免 runtime 不兼容。
- 前端验证：`npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/views/SettingsView.test.js` 通过，2 个测试文件 / 65 个用例；`npm.cmd run build` 通过，`dist` 未发现超过 25MiB 文件，构建产物 manifest 已包含新的 ModelScope 直连候选源。
- 后端验证：`mvn.cmd -DskipTests compile` 通过；目标测试 `mvn.cmd test "-Dtest=OfflineSttModelProxyServiceTest,OfflineSttModelProxyControllerTest,SecurityConfigTest"` 未能执行到目标用例，原因是当前仓库已有 `CommunityServiceModerationTest` 构造器参数与 `CommunityService` 不匹配导致 testCompile 失败，和本轮离线 STT 改动无关。
- 关联任务文件：`frontend/tasks/TASK_61_OFFLINE_STT_REMOTE_SOURCE_DOWNLOAD_PLAN_FRONTEND.md`、`tasks/TASK_61_OFFLINE_STT_MODEL_PROXY_BACKEND.md`。
- 停止说明：本轮只修复离线 STT 模型源候选链路，不继续推进 R2/OSS/COS 托管、离线 TTS 或其它语音能力。
## 离线 STT 旧 manifest 缺少 urls 时补公共镜像候选（2026-05-31）
- 当前阶段：本轮修复用户反馈的“无任何外网请求、一瞬间失败”问题。根因是运行时可能读到旧版或缓存的 sherpa manifest，文件项只有 `path`、没有 `urls`，下载器因此退回同源 `/voice-models/...` 大文件路径；该路径在轻量部署中不存在，会被前端路由回退成 `text/html`，随即被判定为模型源无效。
- 已完成内容：`offlineVoiceModelCache.js` 对 `/voice-models/sherpa-onnx/zh-cn-streaming/` 下四个固定 sherpa 文件增加内置公共候选源兜底；只有 manifest 自身缺少 `urls` 时才补 `modelscope.cn`、`www.modelscope.cn`、`/api/offline-stt/models/`、HuggingFace 官方源，已有 `urls` 的 manifest 仍按清单顺序执行，普通文件仍保留同源 fallback。
- 前端验证：RED 阶段新增旧版 sherpa manifest 缺少 `urls` 的解析和下载用例，旧实现失败于仍请求 `/voice-models/...wasm`；修复后 `npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js` 通过，26 个用例全部通过。
- 关联任务文件：`frontend/tasks/TASK_61_OFFLINE_STT_REMOTE_SOURCE_DOWNLOAD_PLAN_FRONTEND.md`。
- 停止说明：本轮只修复离线 STT 旧 manifest/缓存 manifest 缺少远程候选源时的下载入口，不继续推进自有模型托管、离线 TTS 或其它语音能力。
## 离线 STT 已缓存但 WASM 实为 HTML 的运行时防护（2026-05-31）
- 当前阶段：本轮修复用户反馈的“已显示已缓存，但语音通话准备中报 `expected magic word 00 61 73 6d, found 3c 21 44 4f`”问题。`3c 21 44 4f` 是 `<!DO`，说明运行时拿到的是 HTML 兜底页而不是 WASM 二进制。
- 已完成内容：`offlineVoiceModelCache.js` 下载阶段新增 `.wasm` magic word 校验，`isModelCached()` 会校验缓存内容并删除旧 HTML 坏缓存；`public/voice-models/sherpa-onnx/zh-cn-streaming/runtime.js` 在生成 Blob URL 前二次校验缓存/重取响应，避免把 HTML 重标成 `application/wasm` 后进入 WebAssembly 编译。
- 前端验证：RED 阶段新增 HTML 伪 WASM 下载、HTML 伪 WASM 缓存命中、runtime 导入前拒绝坏缓存三个回归用例；修复后 `npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js` 通过，34 个用例；`npm.cmd test -- --run src/__tests__/workers/sherpaSpeechWorker.test.js` 通过，4 个用例。
- 关联任务文件：`frontend/tasks/TASK_61_OFFLINE_STT_REMOTE_SOURCE_DOWNLOAD_PLAN_FRONTEND.md`。
- 停止说明：本轮只修复离线 STT 缓存真实性和运行时坏缓存防护，不继续推进自有模型托管、离线 TTS 或其它语音能力。
## 社区前台管理员下架前端（2026-05-31）
- 当前阶段：本轮已完成用户端社区的管理员帖子下架入口，等待验收。
- 已完成内容：社区列表卡片和帖子详情页在当前用户 `role === 9` 时显示“下架”按钮；点击后要求填写下架原因；调用 `PUT /api/community/posts/{postId}/admin-hide`；列表页成功后移除帖子，详情页成功后返回社区首页。
- 权限边界：前端只负责按钮显示，后端仍会强校验管理员角色，避免普通用户伪造请求越权下架。
- 前端验证：`npm.cmd test -- --run src/__tests__/components/community/PostCard.test.js src/__tests__/views/community/PostDetailView.test.js` 已通过；`npm.cmd test -- --run src/__tests__/views/community/CommunityView.test.js` 已通过。最终构建结果见本轮交付说明。
- 关联任务文件：`frontend/tasks/TASK_64_COMMUNITY_ADMIN_HIDE_AND_RISK_MARK_FRONTEND.md`、`tasks/TASK_64_COMMUNITY_ADMIN_HIDE_AND_RISK_MARK_BACKEND.md`。
- 停止说明：本轮只完成帖子级管理员前台下架，不继续推进评论下架、举报入口、批量处置或图片 AI 审核。
## 离线 STT runtime 静态脚本缓存破坏修复（2026-05-31）
- 当前阶段：本轮修复用户反馈的“上一轮已加 WASM 校验后，打开模拟语音面试仍直接报 `expected magic word 00 61 73 6d, found 3c 21 44 4f`”问题。
- 问题原因：`runtime.js` 位于 `public/voice-models/...` 静态目录，不会自动获得 Vite 内容 hash；上一轮虽然更新了 `runtime.js` 的坏缓存校验，但 `useSpeechToText.js` 生成的 runtime 查询参数仍是 `20260530-persistent-worker-stream-reset`，浏览器可能继续执行旧 runtime。
- 已完成内容：`OFFLINE_STT_RUNTIME_VERSION` 升级为 `20260531-runtime-asset-validation`，并补充中文注释说明 runtime 静态脚本变更必须显式升级查询参数；回归测试改为断言新的 runtime 版本参数。
- 前端验证：RED 阶段目标用例失败于 runtime URL 仍包含旧 `20260530`；修复后 `npm.cmd test -- --run src/__tests__/composables/useSpeechToText.test.js` 通过，40 个用例；相关回归 `npm.cmd test -- --run src/__tests__/utils/offlineVoiceModelCache.test.js src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/views/SettingsView.test.js` 通过，5 个测试文件 / 120 个用例；`npm.cmd run build` 通过，`dist/voice-models/sherpa-onnx/zh-cn-streaming/` 仅包含 `manifest.json` 和 `runtime.js`，`dist` 中无超过 25MiB 文件。
- 关联任务文件：`frontend/tasks/TASK_61_OFFLINE_STT_REMOTE_SOURCE_DOWNLOAD_PLAN_FRONTEND.md`。
- 停止说明：本轮只处理离线 STT runtime 静态脚本缓存破坏问题，不继续推进自有模型托管、离线 TTS 或其它语音能力。
## 离线 STT Emscripten 主运行时源码硬化（2026-06-01）
- 当前阶段：本轮已继续修复远程源离线 sherpa-onnx 资源下载后，语音面试仍报 `expected magic word 00 61 73 6d, found 3c 21 44 4f` 的前端运行时问题；等待人工刷新页面并进入语音面试复测。
- 问题原因：`blob:http://localhost:3000/...` 状态 200 只表示主运行时 JS Blob 已加载；真实失败点是 Emscripten classic runtime 内部仍可能保留默认 `.wasm/.data` 字面路径，继续请求同源 `/voice-models/...` 并命中 SPA HTML。
- 已完成内容：`runtime.js` 先生成已校验 wasm/data Blob，再对 `sherpa-onnx-wasm-main-asr.js` 源码定向 patch，把局部 `Module`、`wasmBinaryFile` 和 `.data` 包地址固定到 wrapper 注入对象与 Blob URL；`locateFile()` 对 `.wasm/.data` 缺失映射时直接失败，阻止默认同源路径；Worker 记录 runtime URL、版本号、模型文件数量和源码 patch 状态诊断；`OFFLINE_STT_RUNTIME_VERSION` 升级为 `20260601-runtime-source-patch`。
- 前端验证：`npm.cmd test -- --run src/__tests__/utils/sherpaRuntimeAsset.test.js src/__tests__/workers/sherpaSpeechWorker.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，4 个测试文件 / 93 个用例；`npm.cmd run build` 通过；`dist/voice-models/sherpa-onnx/zh-cn-streaming/` 仅包含 `manifest.json`、`runtime.js`、`runtime.js.gz`；`dist` 中无超过 25MiB 文件。
- 关联任务文件：`frontend/tasks/TASK_61_OFFLINE_STT_REMOTE_SOURCE_DOWNLOAD_PLAN_FRONTEND.md`。
- 停止说明：本轮只修复离线 STT 网络源启动链路，不改后端代理、不改模型源 manifest、不新增离线 TTS、自有模型托管或其它语音能力。
# 管理端自定义 AI 用量趋势默认展开前端联动（2026-06-03）
## 已完成且已验证的功能

- 管理端默认仍展示“引擎配置”分区，自定义 AI 用量统计不再挤压配置页布局。
- 切换到“自定义 AI 用量”分区后，用户自定义 AI 按日趋势图默认展开，管理员可继续点击按钮收起趋势区域。
- 用户明细分页保持既有 `pageSize=5`，翻页继续携带 `page/pageSize` 请求后端，不做全量用户明细接收。
- 本轮不修改后端接口、日期规则、聚合 SQL、权限校验、计费、回滚、Provider 配置解析或数据库结构。

## 本轮完成状态

- RED 验证：`npm.cmd test -- --run src/__tests__/views/AdminAiEngineView.test.js` 在旧实现下失败，复现切换后趋势图仍默认折叠。
- GREEN 验证：`npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，2 个测试文件 / 14 个用例。
- 前端构建验证：`npm.cmd run build` 通过。
- 关联任务文件：`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`、`tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。

## 尚未开始的功能

- 未在管理端首页新增趋势表或趋势图。
- 未实现周聚合、月聚合、独立统计页面、TTS 调用或 TTS 配置 UI。
- 未修改自定义 AI 扣费、失败回滚、平台手动 fallback、Provider 配置解析或加密存储规则。

## 停止，不继续下一个功能
本轮仅完成自定义 AI 用量分区内趋势默认展开和用户明细每页 5 条确认，等待验收，不继续推进其它 AI Provider 能力。
## 模拟面试语音降级后禁止自动回切语音模式前端修复（2026-06-03）
- 当前阶段：本轮已修复语音识别不可用自动切换文本后，后续用户用文本输入回答、AI 面试官回复结束又自动切回语音模式的问题，等待人工在真实 Chrome 语音面试中复测。
- 问题原因：`useVoiceCall` 进入文本降级后仍会排后台恢复探测，并且 AI 回复结束、TTS 播放结束的 watcher 会立即触发 `retrySpeechNow()`；当浏览器语音探测返回成功时，会在用户未主动选择的情况下退出文本降级。
- 已完成内容：删除文本降级后的后台定时恢复入口；`retrySpeechNow()` 改为只服务用户点击“重试语音”；AI 回复结束和 TTS 播放结束时如果仍处于文本降级则保持文本模式；降级文案改为提示用户手动重试，降级横幅仅保留“重试语音”按钮。
- 前端验证：RED 阶段 `npm.cmd test -- --run src/__tests__/composables/useVoiceCall.test.js src/__tests__/views/InterviewSessionView.test.js` 失败并复现自动恢复；GREEN 阶段同命令通过，2 个测试文件 / 74 个用例；扩展语音回归 `npm.cmd test -- --run src/__tests__/utils/speechRecognitionCapability.test.js src/__tests__/composables/useSpeechToText.test.js src/__tests__/composables/useVoiceCall.test.js src/__tests__/composables/useTextToSpeech.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/SettingsView.test.js` 通过，6 个测试文件 / 166 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：无后端改动。
- 关联任务文件：`frontend/tasks/TASK_76_INTERVIEW_TTS_QUEUE_AND_MODE_SWITCH_FRONTEND.md`。
- 停止说明：本轮只处理语音降级后的主动恢复边界，不新增云端 STT/TTS、离线 TTS、后端语音服务或其它面试能力。
## 模型列表获取错误提示规整前端联动（2026-06-04）

- 当前阶段：已完成模型列表获取失败提示的前端展示链路回归验证；本轮没有修改前端源码，错误文案由后端统一规整。
- 已完成内容：用户设置页和管理端 AI 引擎弹窗继续展示模型发现接口的 `errorMessage`，失败时仍保留手动输入模型名，不阻止保存或连通测试。
- 后端联动：`AiModelDiscoveryServiceImpl` 已不再透传上游 404 HTML 响应体，改为返回检查 API 基础地址、`/v1` 后缀、服务商 `/models` 支持情况和手动输入兜底的中文提示。
- 前端验证：`npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，4 个测试文件 / 61 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端验证：`mvn.cmd -q "-Dtest=AiModelDiscoveryServiceImplTest,AdminAiEngineConnectivityControllerTest,UserAiConfigServiceImplTest,CriticalEndpointRateLimitFilterTest" test` 与 `mvn.cmd -q -DskipTests compile` 通过。
- 关联任务文件：`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`、`tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。
- 停止说明：本轮只处理模型列表获取失败提示可读性，不继续扩展前端交互、Provider 专用协议、模型能力检测、价格信息、自动路由或自动保存。

## 日志重复访问与数据库读降噪前端 P1 修复（2026-06-06）

- 当前阶段：已完成前端 P1-1 至 P1-6 重复访问降噪，等待真实页面访问后复查后端日志请求量。
- 已完成内容：版本日志、引导状态、社区详情和社区评论接入短缓存与 pending 复用；引导状态和社区写操作成功后清理对应缓存；报告页状态轮询前 6 轮 3 秒、之后 6 秒并保持单请求在途；面试页开场白轮询通过代际 token 防止旧轮询复活，并在 `openingGenerated`、`openingPending=false` 或会话结束后停止。
- 前端 RED 验证：新增缓存和轮询用例后，旧实现下目标命令失败，覆盖缺少 TTL、GET 未缓存、报告轮询未退避和开场白生成信号未停止。
- 前端 GREEN 验证：`npm.cmd test -- --run src/__tests__/utils/apiCache.test.js src/__tests__/api/versionLog.test.js src/__tests__/api/onboarding.test.js src/__tests__/api/community.test.js src/__tests__/views/InterviewReportView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，6 个测试文件 / 78 个用例。
- 扩展验证：`npm.cmd test -- --run src/__tests__/api/performanceCache.test.js src/__tests__/layouts/MainLayout.test.js src/__tests__/views/HomePageView.test.js src/__tests__/views/community/PostDetailView.test.js src/__tests__/components/community/CommentSection.test.js` 中前 4 个相关测试文件通过；`CommentSection.test.js` 仍有既有图片大小文案断言漂移，实际为 2MB、旧断言为 5MB，非本轮改动引入。
- 构建验证：`npm.cmd run build` 通过。
- 日志复查：敏感内容 rg 复查 `logs debug.txt` 无命中；重复访问关键词在当前 `logs/ai-resume.log` 中仅命中历史样本 3 行，本轮未启动后端服务生成新日志。
- 数据边界：本轮不修改后端业务代码，不新增数据库表、字段、索引或 migration。
- 关联任务文件：`frontend/tasks/TASK_LOG_REPEAT_ACCESS_OPTIMIZATION_FRONTEND_PLAN.md`、`tasks/task-日志异常与重复访问优化修复计划.md`。
- 停止说明：本轮只完成日志重复访问优化前端 P1 范围，不继续推进新功能、后端接口或数据库优化。

## 会员充值与续费未开放提示前端修复（2026-06-06）

- 当前阶段：本轮已完成测试项目会员中心充值与续费入口拦截，等待人工在 `/membership` 页面点击“续费”和“立即升级”复测提示。
- 已完成内容：`MembershipView.vue` 保留现有套餐展示与按钮文案，但点击当前套餐续费或可购买套餐升级时统一弹出“当前未开放充值功能，请联系管理员进行升级”；不再调用 `mockUpgradeMembership`，也不再刷新用户信息或显示升级成功。
- 前端 RED 验证：`npm.cmd test -- --run src/__tests__/views/MembershipView.test.js` 在旧实现下失败，复现点击后没有未开放提示且仍走 mock 升级链路。
- 前端 GREEN 验证：`npm.cmd test -- --run src/__tests__/views/MembershipView.test.js` 通过，1 个测试文件 / 6 个用例。
- 相关回归验证：`npm.cmd test -- --run src/__tests__/views/MembershipView.test.js src/__tests__/api/performanceCache.test.js` 通过，2 个测试文件 / 12 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：无后端改动，无数据库结构改动。
- 关联任务文件：`frontend/tasks/TASK_MEMBERSHIP_RECHARGE_UNAVAILABLE_FRONTEND.md`。
- 停止说明：本轮只处理会员中心充值与续费入口提示，不继续推进真实支付、订单创建、管理员代充值、后端接口调整或会员套餐管理改造。

## 日志残留热点前端短缓存修复（2026-06-06）

- 当前阶段：本轮已补齐日志复查中仍会重复触发的通知列表和公开版本日志分页短缓存，等待真实页面访问后复查后端日志。
- 已完成内容：`getNotifications(params)` 使用 `notification:list` 参数化 key 缓存 15 秒；通知标记已读、全部已读、删除和批量删除继续清理 `notification` 前缀；`getPublicVersionLogsPage(params)` 使用 `version:page` 参数化 key 缓存，按 page/size 隔离。
- 前端 RED 验证：`npm.cmd test -- --run src/__tests__/api/performanceCache.test.js src/__tests__/api/versionLog.test.js` 在旧实现下失败，复现重复 request 调用。
- 前端 GREEN 验证：同一命令通过，2 个测试文件 / 15 个用例。
- 构建验证：`npm.cmd run build` 通过。
- 后端联动：后端仅做 SSE 断连日志降噪和 generated password 警告消除，不修改接口协议或数据库结构。
- 关联任务文件：`frontend/tasks/TASK_LOG_REPEAT_ACCESS_OPTIMIZATION_FRONTEND_PLAN.md`、`tasks/task-日志异常与重复访问优化修复计划.md`。
- 停止说明：本轮只处理通知列表与公开版本日志分页残留重复 GET，不继续扩展其它前端缓存或后端查询优化。
