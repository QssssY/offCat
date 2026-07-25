# 云端语音识别兜底 + 管理端 STT 配置（后端）

## 当前任务所属模块

语音面试语音输入兜底、管理端系统级配置、语音识别运行时服务。

## 问题背景

语音面试语音输入完全依赖浏览器 Web Speech API，链路不稳定（详见前端任务文件）。
后端为其提供云端 STT 兜底：浏览器录音 → 后端 `/stt` → OpenAI 兼容 `/audio/transcriptions` → 返回文字。

识别服务地址/密钥/模型不走环境变量，而是**由管理端在数据库中维护**（镜像现有系统级 TTS 配置 `sys_tts_config`），
运维在管理界面一站式配置，无需改服务器环境变量或重启。

## 后端文件定位

- `db/schema.sql`：新增 `sys_stt_config` 单例表。
- `entity/SysSttConfig.java`（新增）
- `mapper/SysSttConfigMapper.java`（新增，`selectCurrent`/`selectEnabled`）
- `service/SysSttConfigService.java` + `service/impl/SysSttConfigServiceImpl.java`（新增）
- `controller/AdminSttConfigController.java`（新增，`/api/admin/stt-config`）
- `dto/admin/AdminSttConfigRequest.java`、`AdminSttConfigResponse.java`、`AdminSttConnectivityTestResponse.java`（新增）
- `dto/user/ResolvedSttConfig.java`（新增，运行时解密配置）
- `service/InterviewSttService.java` + `service/impl/InterviewSttServiceImpl.java`（识别运行时服务，改读 DB 配置）
- `controller/InterviewController.java`：新增 `/stt-capability` 与 `/stt` 端点。

## 本轮修改文件清单

- 新增 `sys_stt_config` 表（单例键 + base_url + api_key 密文 + model + endpoint_path + enabled）及其 DROP 语句。
- 新增 `SysSttConfig` 实体、`SysSttConfigMapper`。
- 新增 `SysSttConfigService`/impl：管理端 CRUD、API Key 加密与脱敏、编辑态复用旧密钥、连通性测试、`resolveEnabledConfig`。
- 新增 `AdminSttConfigController`：GET 查询（脱敏）/ PUT 保存 / POST 连通性测试。
- 新增管理端 DTO 三件、运行时 `ResolvedSttConfig`。
- `InterviewSttServiceImpl` 从环境变量 `SttConfig` 改为读 `SysSttConfigService.resolveEnabledConfig()`；删除 `config/SttConfig.java` 与 `application.yml` 的 `app.stt` 块。
- `InterviewController` 注入 `InterviewSttService`，新增 `/stt-capability`（返回可用性）与 `/stt`（multipart 上传音频，返回文本）。
- 新增单测：`SysSttConfigServiceImplTest`、`InterviewSttServiceImplTest`；`InterviewControllerTest` 同步构造参数。

## 后端实现方案

- **管理端配置**：镜像 `SysTtsConfigServiceImpl`——单例表、`AiCredentialCrypto` 加密存储、`maskApiKey` 脱敏返回、
  编辑态 API Key 留空或传脱敏值时复用已保存密钥、`@CacheEvict` 清理缓存、`PublicHttpsUrlValidator` 校验公网 HTTPS 地址防 SSRF。
- **连通性测试**：管理端“测试连通性”发一段极短静音 WAV 到 `/audio/transcriptions`，只验证鉴权与端点可达，识别结果为空属正常。
- **运行时识别**：`InterviewSttServiceImpl.transcribe` 读取启用配置，OpenAI 兼容 multipart（file + model + 可选 language）上传，
  解析 `{"text": "..."}`；语言 `zh-CN` 归一化为主子标签 `zh`，`auto`/空则不发送 language 交上游自动判定。
- **隔离**：STT 配置与面试对话 AI、TTS 配置完全独立，互不影响；未配置或未启用时 `stt-capability` 返回不可用，前端保持浏览器识别现状。
- **鉴权**：`/api/interview/**` 已是 `authenticated()`，`/stt` 与 `/stt-capability` 自动受保护；`/api/admin/**` 需 ADMIN 角色。

## 数据存储方案

新增 `sys_stt_config` 单例表，遵循 `id`/`create_time`/`update_time`/`is_deleted` 规范，
唯一索引 `(singleton_key, is_deleted)` 保证同时只有一条有效配置。API Key 加密存储，不落明文。
音频数据不入库、不落盘，识别后即释放。

## 编译结果

- `mvn -q -DskipTests clean compile` 通过。
- `mvn -q test -Dtest=InterviewSttServiceImplTest,SysSttConfigServiceImplTest,InterviewControllerTest` 通过，
  3 个测试类（InterviewSttServiceImplTest 3 + SysSttConfigServiceImplTest 6 + InterviewControllerTest 19）。

## 构建结果

- `mvn -q -DskipTests clean package` 通过，产出 `ai-resume-server-1.0.0-SNAPSHOT.jar`。

## 当前功能验收说明

管理员在管理端“系统 STT 配置”填写硅基流动地址、`FunAudioLLM/SenseVoiceSmall` 模型和 API Key 并启用后，
语音面试在浏览器 Web Speech 失败时会自动改用云端识别继续收音。未配置或未启用时保持原有浏览器识别行为。

## 停止，不继续下一个功能

本轮只为语音面试语音输入增加云端 STT 兜底与管理端配置，不改动文本听写、不恢复离线 STT、不改动 TTS 链路、不新增其它语音服务。
