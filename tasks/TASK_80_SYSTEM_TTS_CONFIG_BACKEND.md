# 管理端系统级 TTS 配置后端（2026-06-04）

## 当前任务所属模块

管理端 AI 引擎管理 / 语音面试 TTS 兜底能力。

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/AdminTtsConfigController.java`
- `server/src/main/java/com/airesume/server/controller/UserAiConfigController.java`
- `server/src/main/java/com/airesume/server/service/impl/SysTtsConfigServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/UserTtsSpeechServiceImpl.java`
- `server/src/main/java/com/airesume/server/dto/user/SystemTtsStatusResponse.java`
- `db/migrations/TASK_80_SYSTEM_TTS_CONFIG.sql`
- `server/db/migrations/TASK_80_SYSTEM_TTS_CONFIG.sql`

## 本轮修改文件清单

- 新增 `SysTtsConfig` 实体、Mapper、Service、管理端 Controller、管理端 DTO。
- 新增 `SystemTtsStatusResponse`，并在 `UserAiConfigController` 增加 `GET /api/user/ai-config/system-tts-status`。
- 修改 `UserTtsSpeechServiceImpl`，按用户自定义 `interview/default` 优先、系统级 TTS 兜底解析运行时配置。
- 修改 `InterviewController` 的 TTS capability 返回，增加 `systemTtsAvailable` 和系统来源识别。
- 修改 Redis 缓存配置，增加 `config:systemTts` 30 分钟 TTL。
- 同步 `db/schema.sql` 与 `server/db/schema.sql`，新增 `sys_tts_config` 表定义。
- 新增并调整后端单元测试：`AdminTtsConfigControllerTest`、`SysTtsConfigServiceImplTest`、`UserTtsSpeechServiceImplTest`、`InterviewControllerTest`、`UserAiConfigControllerTest`、`RedisConfigTest`、`SchemaConsistencyTest`。

## 后端实现方案

- 新建独立表 `sys_tts_config` 保存系统级 TTS 单例配置，避免污染 AI 引擎主配置表。
- 管理端提供查询、保存、连通测试、模型/音色发现、试音五个接口。
- API Key 入库前加密，管理端查询只返回脱敏值；用户侧状态接口只返回布尔可用性，不暴露配置详情。
- 保存配置时，启用或填写任意 TTS 字段必须具备完整的 `baseUrl/apiKey/model/voiceId`；编辑态允许空 Key 或脱敏 Key 复用已保存密钥。
- 语音面试 TTS 运行时解析顺序为用户自定义 TTS > 系统 TTS > 不可用。

## 数据存储方案

- 新增 `sys_tts_config` 表，包含 `tts_provider/base_url/api_key/model/voice_id/endpoint_path/enabled/singleton_key/is_deleted` 等字段。
- 使用 `singleton_key=1` 约束系统级配置仅保留一条有效单例。
- `db/` 与 `server/db/` 两处 SQL 已同步。

## stage 更新说明

- 根目录 `tasks/stage.md` 已补充“管理端系统级 TTS 配置后端”最新阶段记录。
- 前端阶段记录见 `frontend/tasks/stage.md`。

## 编译与测试结果

- RED：`mvn.cmd -q "-Dtest=UserAiConfigControllerTest" test` 因缺少 `SystemTtsStatusResponse` 失败，确认用户侧系统 TTS 状态接口未实现。
- GREEN：`mvn.cmd -q "-Dtest=UserAiConfigControllerTest,AdminTtsConfigControllerTest,SysTtsConfigServiceImplTest,UserTtsSpeechServiceImplTest,InterviewControllerTest" test` 通过。
- 回归：`mvn.cmd -q "-Dtest=AdminTtsConfigControllerTest,SysTtsConfigServiceImplTest,UserTtsSpeechServiceImplTest,InterviewControllerTest,UserAiConfigControllerTest,RedisConfigTest,SchemaConsistencyTest" test` 通过。
- 编译：`mvn.cmd -q -DskipTests compile` 通过。

## 当前功能验收说明

- 管理员可维护一套系统级 TTS 配置，并进行连通测试、模型/音色发现和试音。
- 未配置用户自定义 TTS 的语音面试可使用系统级 TTS 兜底。
- 用户已配置自定义 TTS 时仍优先使用用户配置。
- 用户侧只能读取系统 TTS 是否可用，不能读取系统配置详情和密钥。

## 停止，不继续下一个功能

本轮仅完成 `develop-project.txt` 功能 2（管理端系统级 TTS 配置）的后端部分，等待验收，不继续推进浏览器音色预设扩展或其它语音能力。
