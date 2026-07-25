# EdgeTTS 云端语音 Provider 后端（2026-06-05）

## 当前任务所属模块

用户自定义 TTS / 系统级 TTS / 语音面试云端播报。

## 前端文件定位

- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/views/admin/AdminAiEngineView.vue`

前端联动记录见 `frontend/tasks/TASK_84_EDGETTS_PROVIDER_FRONTEND.md`。

## 后端文件定位

- `server/src/main/java/com/airesume/server/common/constants/TtsProviderConstants.java`
- `server/src/main/java/com/airesume/server/service/EdgeTtsClient.java`
- `server/src/main/java/com/airesume/server/service/impl/EdgeTtsClientImpl.java`
- `server/src/test/java/com/airesume/server/service/impl/EdgeTtsClientImplTest.java`
- `server/src/main/java/com/airesume/server/service/impl/UserTtsSpeechServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/UserTtsConnectivityTestServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/TtsDiscoveryServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/UserAiConfigServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/SysTtsConfigServiceImpl.java`

## 本轮修改文件清单

- 新增 `EdgeTtsClient` 与 `EdgeTtsClientImpl`，封装 Edge Read Aloud WebSocket 合成协议。
- `TtsProviderConstants` 新增 EdgeTTS 预设、默认端点、默认模型和中文 Edge Neural 音色。
- `UserTtsSpeechServiceImpl` 新增 EdgeTTS 合成分支，用户配置和系统配置均可走 EdgeTTS。
- `UserTtsConnectivityTestServiceImpl` 新增 EdgeTTS 无 Key 连通测试和试听分支。
- `TtsDiscoveryServiceImpl` 对 EdgeTTS 直接返回内置模型和音色，不访问上游发现端点。
- `UserAiConfigServiceImpl` 与 `SysTtsConfigServiceImpl` 支持 EdgeTTS 空 API Key 保存、测试、发现和运行时解析。
- DTO 校验放开 TTS `apiKey` 的全局必填，由服务层按 provider 决定是否需要 Key。
- 补充 `TtsAudioResult`，保留不同 Provider 的真实音频类型，避免控制器固定写死 `audio/mpeg`。

## 前端实现方案

前端在用户设置页和管理端系统 TTS 配置页新增 EdgeTTS provider 预设；EdgeTTS 选择后自动回填地址、模型、音色和端点，且不要求 API Key。

## 后端实现方案

- EdgeTTS 作为 `ttsProvider=edge` 接入现有云端 TTS 链路，不使用浏览器 `speechSynthesis`。
- 语音面试仍调用既有 `/api/interview/session/{sessionId}/tts`，后端返回音频字节，前端播放 Blob。
- EdgeTTS 使用 `https://speech.platform.bing.com` 作为配置地址，真实合成通过 Edge Read Aloud WebSocket 完成。
- EdgeTTS 不需要用户 API Key；OpenAI、MiMo、Gemini、MiniMax、Qwen、xAI 仍按各自 provider 规则处理 Key。
- 模型/音色发现对 EdgeTTS 返回项目内置预设，避免无意义外部发现请求。

## 数据存储方案

本轮无数据库结构变更。现有 `tts_provider`、`tts_base_url`、`tts_model`、`tts_voice_id`、`tts_endpoint_path` 字段可完整保存 EdgeTTS 配置；EdgeTTS 的 `tts_api_key` / 系统 `api_key` 允许为空。

## stage 更新说明

根目录 `tasks/stage.md` 已置顶补充“EdgeTTS 云端语音 Provider 后端”记录；前端 stage 见 `frontend/tasks/stage.md`。

## 编译结果

- RED：旧实现下目标测试无法通过，缺少 EdgeTTS provider、无 Key 校验和 EdgeTTS 合成客户端。
- GREEN：`mvn.cmd -q "-Dtest=UserTtsSpeechServiceImplTest,UserTtsConnectivityTestServiceImplTest,TtsDiscoveryServiceImplTest,UserAiConfigServiceImplTest,SysTtsConfigServiceImplTest" test` 通过。
- 编译：`mvn.cmd -q -DskipTests compile` 通过。

## 构建结果

本轮后端自身无前端构建；前端构建结果见 `frontend/tasks/TASK_84_EDGETTS_PROVIDER_FRONTEND.md`。

## 代码审查补充修复

- 问题：`UserTtsSpeechServiceImpl` 新增 `EdgeTtsClient` 后仍保留旧的 5 参数测试构造器，Spring 反射选择到不完整构造器时会出现 `No default constructor found`，导致应用上下文启动失败。
- 修复：移除旧测试构造器，生产代码只保留 Lombok 基于全部 `final` 字段生成的构造器；`UserTtsSpeechServiceImplTest` 改为显式传入 `mock(EdgeTtsClient.class)`。
- 验证：`mvn.cmd -q "-Dtest=ServerApplicationTests,UserTtsSpeechServiceImplTest" test` 通过。
- 范围：仅修复构造器注入回归，不新增后端接口、数据库结构或新的 TTS 能力。

## ExecutionException 合成失败补充修复

- 问题：EdgeTTS 运行时报 `ExecutionException` 时，旧日志只记录外层异常类型，未展开 WebSocket 握手或监听器 root cause，无法区分 403、超时、TLS、DNS 或协议错误。
- 根因：Edge Read Aloud WebSocket 协议参数仍停留在 Edge 130，且缺少当前上游常用的 `muid` Cookie、语言、压缩和缓存 header；遇到 403 时也没有按上游 `Date` 重新计算 `Sec-MS-GEC`。
- 修复：
  - `Sec-MS-GEC-Version` 更新为 `1-143.0.3650.75`，User-Agent 同步到 Edge 143。
  - WebSocket 握手补充 `Pragma`、`Cache-Control`、`Accept-Encoding`、`Accept-Language`、`User-Agent` 和随机 `muid` Cookie。
  - 首次握手 403 且响应带 `Date` 时，使用上游时间重算 `Sec-MS-GEC` 并重试一次。
  - `ExecutionException` / `CompletionException` 展开到 root cause 后记录 `rootCauseType/rootCause`，403 场景返回“上游拒绝连接，可切换其它 TTS”的明确业务提示。
- 验证：
  - RED：旧实现下 `mvn.cmd -q "-Dtest=EdgeTtsClientImplTest" test` 编译失败，复现缺少新协议参数、header、异常解包和 403 Date 重试辅助逻辑。
  - GREEN：`mvn.cmd -q "-Dtest=EdgeTtsClientImplTest" test` 通过。
  - 回归：`mvn.cmd -q "-Dtest=EdgeTtsClientImplTest,UserTtsConnectivityTestServiceImplTest,UserTtsSpeechServiceImplTest,TtsDiscoveryServiceImplTest" test` 通过。
  - 编译：`mvn.cmd -q -DskipTests compile` 通过。
- 范围：仅修复 EdgeTTS 协议兼容和诊断，不新增数据库字段、前端交互、音频存储、计费统计或流式音频。

## 当前功能验收说明

- 用户自定义 TTS 和管理端系统 TTS 都可以选择 EdgeTTS。
- EdgeTTS 无需填写 API Key，可以保存、测试、试听和用于语音面试播报。
- 模型/音色列表包含 `zh-CN-XiaoxiaoNeural`、`zh-CN-YunxiNeural` 等 Edge 免费音色。
- 本轮不改数据库 schema，不实现音频持久化、计费统计或流式音频。

## 停止，不继续下一个功能

本轮仅完成 EdgeTTS Provider 接入，等待验收，不继续推进其它语音能力。
