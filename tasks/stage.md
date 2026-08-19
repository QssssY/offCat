# 免费用户六项功能额度统一为 100 次（2026-08-18）
## 已完成且已验证的功能

- 简历诊断、模拟面试、AI 润色、JD 匹配、模板使用、Offer 辅助六项免费额度已统一为一次性 100 次。
- 新用户初始化常量与双份 schema 默认值保持一致；存量迁移会补足所有非管理员账号的免费基础储备，VIP 套餐日额度和超过 100 的人工额度不受损。
- 润色和 JD 匹配新增每账号每项 10 次/10 分钟突发限频，免费总额度仍保持 100 次。
- 前端接口与展示结构未修改，继续直接展示后端剩余额度。

## 本轮完成状态

- RED 验证：旧六项值 `3/1/1/1/2/1`、缺失迁移和旧 schema 默认值均被新增测试捕获。
- RED 验证：旧实现未限制润色/JD 的第 11 次突发请求，新增限频测试失败。
- GREEN 验证：`QuotaConstantsTest`、`FreeUserQuotaMigrationTest`、`SchemaConsistencyTest`、`UserQuotaServiceImplTest`、`UserQuotaServiceImplAtomicDeductionTest`、`CriticalEndpointRateLimitFilterTest` 全部通过。
- 后端全量测试在设置测试占位 `DOUBAO_API_KEY` 后 855 个用例全部通过；后端编译与 `-DskipTests package` 均通过。
- 生产数据库迁移已完成：首次补足 7 个非管理员账号，第二次执行更新 0 行；六项最低额度和数据库默认值均为 100，管理员额度保持不变。
- 已使用本地构建 Jar 部署，服务器未执行构建；旧 Jar 与 `user_quota` 完整备份位于 `/opt/offercat/backups/quota100-retry-20260818-010705`。
- `offercat.service`、`nginx` 和 `/actuator/health` 验收通过；`kelin.cyou` 首页及公开统计接口返回 200，图床上传仍返回“图床服务现在还未开放”。
- 关联任务文件：`tasks/TASK_90_FREE_USER_QUOTA_100_BACKEND.md`。

## 尚未开始的功能

- 未修改 VIP 套餐额度、支付、会员购买、前端页面或每日刷新规则。
- 未取消现有新用户 AI 冷却、原子扣减、消费日志和接口鉴权。

## 停止，不继续下一个功能

本轮只完成免费额度 100 次及其直接安全边界，生产上线验收已完成，不继续推进其它功能。

---

# 合规分支四项回归修复后端（2026-06-07）
## 已完成且已验证的功能

- 已继续在 `compliance/remove-community-membership` 分支完成回归修复，主分支未参与修改，本轮不提交 commit。
- 启动失败已修复：`UserAiConfigResolverImpl`、`DirectProcessRouter`、`InterviewController`、`ResumeDiagnosisController` 均明确生产注入构造器，保留必要测试兼容构造器，`ServerApplicationTests` 已通过。
- 自定义 Key 简历诊断直连槽泄漏已修复：额度检查失败、任务保存失败或事务回滚会释放预占槽；提交成功后直连处理继续消费同一预留槽，不入 MQ、不回退平台。
- 失败任务源 PDF 重试窗口已修复：成功完成后立即清理源 PDF，失败后保留 24 小时供重试，过期后只清理源文件并清空 `file_url`，不删除脱敏报告记录。
- 未新增历史数据清理 migration 或脚本；部署数据库为空，存量测试数据不在本轮处理范围。

## 本轮完成状态

- 后端启动回归：`mvn.cmd -q -Dtest=ServerApplicationTests test` 通过。
- 后端目标回归：`mvn.cmd -q "-Dtest=UserAiConfigServiceImplTest,UserAiConfigResolverImplLocalRequestTest,ResumePrivacySanitizerTest,ResumeDiagnosisProcessorTest,DirectProcessRouterTest,InterviewControllerTest,InterviewServiceTest,UserTtsSpeechServiceImplTest,ResumeJobMatchServiceImplTest,ResumePolishServiceImplTest,MockInterviewJobTargetServiceImplTest,ResumeDiagnosisTaskServiceImplTest,UserDataRetentionCleanupServiceImplTest" test` 通过。
- 后端编译：`mvn.cmd -q -DskipTests compile` 通过。
- 关键词扫描：`rg -n "sk-user-real|sk-local-secret|sk-local|tts-local|138-0000-0000|13800000000|zhangsan@example\\.com|zhangsan\\.dev|张三" server/src/main` 无命中。
- 前端本地 TTS capability 修复和构建结果见 `frontend/tasks/stage.md`。

## 尚未开始的功能

- 未推进历史数据库清理、生产备份/回滚脚本、社区/会员以外的新功能或其它合规扩展。

## 停止，不继续下一个功能

本轮只修复合规分支四项回归及其直接测试，等待验收，不继续推进下一阶段。

# 用户 Key 本地化与简历数据脱敏合规改造后端（2026-06-07）
## 已完成且已验证的功能

- 已继续在独立分支 `compliance/remove-community-membership` 上完成用户自定义 AI/TTS Key 服务端持久化退役，主分支不参与修改。
- 用户配置接口保留兼容响应，但保存、删除、启停不再写入 `user_ai_config`；业务请求只在本次诊断、面试、测试或试听中临时使用 `clientAiConfig` / `clientTtsConfig`。
- 简历诊断、JD 匹配、AI 润色和面试上下文保存前已接入脱敏处理，不再新增保存可恢复完整简历原文；源 PDF 仅作为临时输入，任务处理完成或自定义 Key 直连失败后清理。
- 使用本地 Key 的简历诊断无法立即直连处理时返回稍后重试，不进入 MQ、不自动回退平台、不保存 Key。
- 管理端系统 AI/TTS 配置不变，管理员用户额度查询、调整、重置能力不变。
- 部署环境为空库，本轮不新增历史数据清理 migration，不执行存量数据清理。
## 本轮完成状态

- 后端目标回归：`mvn.cmd -q "-Dtest=UserAiConfigServiceImplTest,UserAiConfigResolverImplLocalRequestTest,ResumePrivacySanitizerTest,ResumeDiagnosisProcessorTest,DirectProcessRouterTest,InterviewControllerTest,InterviewServiceTest,UserTtsSpeechServiceImplTest,ResumeJobMatchServiceImplTest,ResumePolishServiceImplTest,MockInterviewJobTargetServiceImplTest,ResumeDiagnosisTaskServiceImplTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 敏感关键词复查：`rg -n "sk-user-real|sk-local-secret|sk-local|tts-local|138-0000-0000|13800000000|zhangsan@example\\.com|zhangsan\\.dev|张三" src\main` 无命中。
- 关联任务文件：`tasks/TASK_88_COMPLIANCE_LOCAL_KEYS_RESUME_PRIVACY_BACKEND.md`。
## 尚未开始的功能

- 未新增历史数据清理脚本或迁移。
- 未物理删除旧的 `user_ai_config` 表结构和简历历史字段结构。
- 未修改支付、订单、会员、社区等其它未指定业务链路。
## 停止，不继续下一个功能

本轮仅完成用户自定义 Key 本地化后端边界和简历数据脱敏保存边界，等待验收，不继续推进历史清理或数据库结构重构。
---

# 合规部署社区与会员能力退役后端（2026-06-07）

## 已完成且已验证的功能

- 已在独立分支 `compliance/remove-community-membership` 上退役社区与会员后端 MVC 暴露，主分支不参与修改。
- 用户侧 `CommunityController`、`MembershipController` 和管理端 `AdminCommunityController`、`AdminMembershipController` 不再作为 Spring MVC 接口注册。
- 额度不足、通知和审计日志文案已移除会员/VIP 引导，改为联系管理员或高额度用户等中性表达。
- 管理端用户额度接口保留，管理员仍可查询、调整、重置用户额度。
- 历史表、实体、Mapper、Service 和控制器方法体暂不物理删除，降低部署前回归风险并保留历史数据处理边界。

## 本轮完成状态

- 后端目标验证：`mvn.cmd -q "-Dtest=AdminControllerTest,AdminMembershipControllerTest,CommunityControllerImageAccessTest,SecurityConfigTest,RetiredFeatureRoutesTest,ResultCodeTest,NotificationServiceTest,AdminAuditLogControllerTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 关联任务文件：`tasks/TASK_87_COMPLIANCE_REMOVE_COMMUNITY_MEMBERSHIP_BACKEND.md`。

## 尚未开始的功能

- 未物理删除社区/会员历史数据表和历史服务代码。
- 未修改支付、订单、充值或其它未指定业务链路。

## 停止，不继续下一个功能

本轮仅完成合规部署所需的社区与会员后端能力退役，等待验收，不继续推进物理删除或其它模块改造。

---

# Redis 缓存失败可观测性与回源压力保护 P2-1 后端（2026-06-06）

## 已完成且已验证的功能

- `RedisCacheErrorHandler` 已保留业务降级策略：缓存读取失败降级回源，缓存写入、删除、清空失败不影响主流程。
- Redis 缓存失败 warn 日志已补充 `cache`、`key`、`exceptionType`、`fallback` 字段，便于区分连接失败、命令中断、序列化不兼容等根因。
- 同一 `cacheName + key + exceptionType` 的 warn 已增加 30 秒短窗口限频，避免 Redis 故障时同一热点 key 重复刷屏。
- 序列化不兼容路径仍会尝试驱逐坏缓存；驱逐失败时记录受控 warn 并等待 TTL 自然过期。
- `RedisConfig` 已显式注册 `sys_user` 5 分钟 TTL，并通过测试锁定 `auth:userInfo` 10 分钟、`user:quota` 5 分钟等热点缓存 TTL。
- 本轮不修改业务流程、接口协议、数据库结构或前端文件。

## 本轮完成状态

- RED 验证：`mvn.cmd -q "-Dtest=RedisCacheErrorHandlerTest,RedisConfigTest" test` 在旧实现下失败，失败原因为日志缺少 `exceptionType`/`fallback`、同 key 重复 warn 未限频、`sys_user` TTL 未显式注册。
- GREEN 目标验证：`mvn.cmd -q "-Dtest=RedisCacheErrorHandlerTest,RedisConfigTest" test` 通过。
- Spring 上下文验证：`mvn.cmd -q "-Dtest=ServerApplicationTests" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 关联任务文件：`tasks/task-日志异常与重复访问优化修复计划.md`。

## 尚未开始的功能

- 未修改前端重复访问相关文件。
- 未新增数据库表、字段、索引或迁移脚本。
- 未继续推进其他优化任务。

## 停止，不继续下一个功能

本轮仅完成 P2-1「Redis 缓存失败可观测性与回源压力保护」，等待验收，不继续推进其他功能。

---

# 版本日志与引导状态后端缓存兜底 P1-2 后端（2026-06-05）

## 已完成且已验证的功能

- `SysVersionLogServiceImpl.getLatestPublished` 已增加 `config:versionLogs` 缓存兜底，使用安全 limit 作为 key，并启用 `sync = true`。
- `AdminVersionLogController` 的版本日志新增、更新、发布、批量发布、删除、批量删除写操作已驱逐 `config:versionLogs`。
- `UserOnboardingServiceImpl.getStatus` 已增加 `user:onboardingStatus` 用户级短缓存，key 为 `userId + "::" + guideKey`，并启用 `sync = true`。
- `UserOnboardingServiceImpl.updateStatus` 与 `completeTask` 已按用户和引导 key 驱逐引导状态缓存。
- `OnboardingStatusResponse` 已实现 `Serializable`，适配当前 Redis cache 序列化方式。
- `RedisConfig` 已注册 `config:versionLogs` 5 分钟 TTL 与 `user:onboardingStatus` 60 秒 TTL。
- 本轮不修改业务流程、接口协议、数据库结构或前端文件。

## 本轮完成状态

- RED 验证：`mvn.cmd -q "-Dtest=SysVersionLogServiceImplTest,VersionLogControllerTest,AdminVersionLogControllerTest,UserOnboardingServiceImplTest,RedisConfigTest" test` 在旧实现下失败，失败原因为缺少版本日志/引导状态缓存注解和 Redis TTL 注册。
- GREEN 目标验证：`mvn.cmd -q "-Dtest=SysVersionLogServiceImplTest,VersionLogControllerTest,AdminVersionLogControllerTest,UserOnboardingServiceImplTest,RedisConfigTest" test` 通过。
- Spring 上下文验证：`mvn.cmd -q "-Dtest=ServerApplicationTests" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 关联任务文件：`tasks/task-日志异常与重复访问优化修复计划.md`。

## 尚未开始的功能

- 未开始 P2 Redis 缓存失败可观测性与回源压力保护。
- 未修改前端重复访问相关文件。

## 停止，不继续下一个功能

本轮仅完成 P1-2「版本日志与引导状态后端缓存兜底」，等待验收，不继续推进 P2。

---

# 面试轻量状态轮询降噪 P1-1 后端（2026-06-05）

## 已完成且已验证的功能

- `InterviewController.getSessionStatus` 已将高频「获取会话轻量状态」轮询日志从 `info` 降为 `debug`，减少报告等待页和开场白等待阶段的 INFO 日志噪声。
- `InterviewService.getSessionStatus` 与 `InterviewSessionMapper.selectOwnedStatus` 轻量查询链路保持不变，继续不加载聊天记录、岗位上下文或完整评估报告正文。
- `InterviewControllerTest` 已补充日志级别回归测试，确认状态轮询请求只产生 DEBUG 级别日志，不产生 INFO 级别轮询日志。
- 本轮不修改接口 URL、参数、响应结构、业务流程、数据库结构或前端文件。

## 本轮完成状态

- RED 验证：`mvn.cmd -q "-Dtest=InterviewControllerTest,InterviewServiceTest" test` 在旧实现下失败，失败原因为 `shouldLogSessionStatusPollingRequestAtDebugLevel` 未捕获到 DEBUG 状态日志，且旧实现仍输出 INFO 级别状态轮询日志。
- GREEN 目标验证：`mvn.cmd -q "-Dtest=InterviewControllerTest,InterviewServiceTest" test` 通过。
- Spring 上下文验证：`mvn.cmd -q "-Dtest=ServerApplicationTests" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 关联任务文件：`tasks/task-日志异常与重复访问优化修复计划.md`。

## 尚未开始的功能

- 未开始 P1-2 版本日志与引导状态后端缓存兜底。
- 未开始 P2 Redis 缓存失败可观测性与回源压力保护。
- 未修改前端重复访问相关文件。

## 停止，不继续下一个功能

本轮仅完成 P1-1「面试轻量状态轮询降噪」，等待验收，不继续推进 P1-2 或 P2。

---

# OSS InvalidAccessKeyId 定位与安全化处理 P0-2 后端（2026-06-05）
## 已完成且已验证的功能

- `OssConfig` 已新增启用状态下的必填字段缺失列表，缺失报告只包含字段名，不包含配置值。
- `OssServiceImpl` 已区分 OSS 未启用和启用但配置缺失；配置缺失时快速失败并明确缺失字段。
- OSS 客户端初始化日志和初始化失败日志只输出 endpoint、bucket、脱敏 AccessKeyId 和错误类型，不输出 AccessKey 或 Secret 原文。
- OSS 签名 URL 生成失败时只记录 objectKey、bucket、脱敏 AccessKeyId、errorCode 和 errorType，不记录完整签名 URL 或签名参数。
- `CommunityController.getImage` 在 OSS 签名失败时返回稳定 `502 Bad Gateway`，不把上游 OSS 错误详情透出到前端。
- 本轮不修改数据库结构、不修改前端文件、不推进重复访问或 Redis 优化。

## 本轮完成状态

- RED 验证：`mvn.cmd -q "-Dtest=OssServiceImplTest,CommunityControllerImageAccessTest" test` 在旧实现下失败，失败原因为 `OssServiceImplTest` 缺少可覆盖的 `buildClient` 客户端构造边界。
- GREEN 目标验证：`mvn.cmd -q "-Dtest=OssServiceImplTest,CommunityControllerImageAccessTest" test` 通过。
- P0-1/P0-2 组合回归：`mvn.cmd -q "-Dtest=SensitiveLogMaskerTest,OssServiceImplTest,CommunityControllerImageAccessTest" test` 通过。
- Spring 上下文验证：`mvn.cmd -q "-Dtest=ServerApplicationTests" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 日志样本复查：`rg -n 'OSSAccessKeyId=[^*&\\s]{8,}|Signature=[^*&\\s]{8,}|Authorization: Bearer|\"messages\"\\s*:\\s*\\[|apiKey\\s*[:=]\\s*[^*\\s,;}&\"]{8,}|accessKeySecret\\s*[:=]\\s*[^*\\s,;}&\"]{8,}|LTAI5tSensitiveKey7890|aliyun-access-key-secret-value' logs debug.txt` 无命中。
- 关联任务文件：`tasks/task-日志异常与重复访问优化修复计划.md`。

## 尚未开始的功能

- 未开始 P1 重复访问降噪和缓存兜底。
- 未开始 P2 Redis 缓存失败可观测性与回源压力保护。
- 未修改前端重复访问相关文件。

## 停止，不继续下一个功能

本轮仅完成 P0-2「OSS `InvalidAccessKeyId` 定位与安全化处理」，等待验收，不继续推进 P1 或 P2。
---

# 日志敏感信息脱敏 P0-1 后端（2026-06-05）
## 已完成且已验证的功能

- 新增日志脱敏工具 `SensitiveLogMasker`，统一清理 OSS 签名参数、Authorization、API Key、AccessKey 和 AI 请求正文中的高风险字段。
- 新增 `SensitiveLogMessageConverter` 并接入 `logback-spring.xml` 的 `CONSOLE`、`FILE`、`ERROR_FILE`，日志消息与异常文本输出前都会先脱敏。
- 保留 provider、baseUrlHost、endpoint、model、configType、status、elapsedMs、objectKey、errorType 等排障字段。
- 本轮不修改业务流程、不修改数据库结构、不修改前端文件。

## 本轮完成状态

- RED 验证：`mvn.cmd -q "-Dtest=SensitiveLogMaskerTest" test` 在生产类不存在时失败，失败原因为 `SensitiveLogMasker` 符号不存在。
- GREEN 验证：`mvn.cmd -q "-Dtest=SensitiveLogMaskerTest" test` 通过。
- Spring 上下文验证：`mvn.cmd -q "-Dtest=ServerApplicationTests" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 日志样本复查：`rg -n 'OSSAccessKeyId=[^*&\\s]{8,}|Signature=[^*&\\s]{8,}|Authorization: Bearer|\"messages\"\\s*:\\s*\\[|apiKey\\s*[:=]\\s*[^*\\s,;}&\"]{8,}|accessKeySecret\\s*[:=]\\s*[^*\\s,;}&\"]{8,}' logs debug.txt` 无命中。
- 关联任务文件：`tasks/task-日志异常与重复访问优化修复计划.md`。

## 尚未开始的功能

- 未开始 P0-2 OSS `InvalidAccessKeyId` 定位与安全化处理。
- 未开始 P1 重复访问降噪和缓存兜底。
- 未开始 P2 Redis 缓存失败可观测性与回源压力保护。

## 停止，不继续下一个功能

本轮仅完成 P0-1「日志敏感信息脱敏」，等待验收，不继续推进 P0-2、P1 或 P2。
---

# EdgeTTS ExecutionException 合成失败修复后端（2026-06-05）
## 已完成且已验证的功能

- 修复 EdgeTTS WebSocket 协议参数过旧导致的合成失败风险：`Sec-MS-GEC-Version` 与 User-Agent 从 Edge 130 更新到 Edge 143。
- EdgeTTS 连接补充浏览器侧常用 header，包括 `Accept-Language`、`Accept-Encoding`、`Cache-Control` 和每次随机生成的 `muid` Cookie，降低上游握手拒绝概率。
- 当首次握手返回 403 且响应带 `Date` 时，后端会按上游时间重算 `Sec-MS-GEC` 并重试一次，覆盖本机时间偏差导致的 403。
- `ExecutionException` / `CompletionException` 现在会展开到真实 root cause，日志输出 `rootCauseType/rootCause`，403 场景返回更明确的“上游拒绝连接/可切换其它 TTS”提示。
- 本轮不新增 TTS Provider、不改数据库、不改前端交互、不引入音频持久化或计费统计。

## 本轮完成状态

- RED 验证：旧实现下 `mvn.cmd -q "-Dtest=EdgeTtsClientImplTest" test` 编译失败，复现缺少最新 EdgeTTS header、异步异常解包和 403 Date 重试辅助逻辑。
- GREEN 验证：`mvn.cmd -q "-Dtest=EdgeTtsClientImplTest" test` 通过。
- 后端目标回归：`mvn.cmd -q "-Dtest=EdgeTtsClientImplTest,UserTtsConnectivityTestServiceImplTest,UserTtsSpeechServiceImplTest,TtsDiscoveryServiceImplTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 关联任务文件：`tasks/TASK_84_EDGETTS_PROVIDER_BACKEND.md`、`frontend/tasks/TASK_84_EDGETTS_PROVIDER_FRONTEND.md`。

## 尚未开始的功能

- 未实现 EdgeTTS 官方 API 替代，因为 EdgeTTS 本身仍是非官方在线朗读通道，可能继续受微软上游风控、限流或协议变更影响。
- 未实现自动切换其它 TTS Provider；当前仅在错误信息中提示用户可切换。

## 停止，不继续下一个功能
本轮仅修复 EdgeTTS `ExecutionException` 合成失败的协议参数、重试和诊断问题，等待真实网络环境验收，不继续推进其它语音能力。
---

# EdgeTTS 代码审查问题修复后端（2026-06-05）
## 已完成且已验证的功能

- 修复 `UserTtsSpeechServiceImpl` 新增 `EdgeTtsClient` 后的构造器注入回归：移除旧 5 参数测试构造器，避免 Spring 上下文启动时选择到不完整构造器。
- `UserTtsSpeechServiceImplTest` 已同步使用完整构造器并传入 `EdgeTtsClient` mock，测试链路与生产依赖保持一致。
- 本轮不新增后端接口、数据库字段、TTS Provider 或语音面试交互。

## 本轮完成状态

- 后端目标验证：`mvn.cmd -q "-Dtest=ServerApplicationTests,UserTtsSpeechServiceImplTest" test` 通过。
- 关联任务文件：`tasks/TASK_84_EDGETTS_PROVIDER_BACKEND.md`、`frontend/tasks/TASK_84_EDGETTS_PROVIDER_FRONTEND.md`。

## 尚未开始的功能

- 未实现音频持久化、TTS 计费统计、流式音频、STT 或更多 EdgeTTS 管理能力。

## 停止，不继续下一个功能
本轮仅修复 EdgeTTS 未提交改动代码审查中指出的后端启动回归，等待验收，不继续推进其它语音能力。
---

# TTS 剩余厂商适配后端（2026-06-05）
## 已完成且已验证的功能

- 新增 Gemini、MiniMax、Qwen、xAI 四个 TTS Provider 预设和协议分发，保留 OpenAI、MiMo、EdgeTTS 原链路。
- TTS 合成结果已扩展为 `audioBytes + contentType`，用户试听、管理端试听和语音面试 `/tts` 均按真实媒体类型返回。
- Gemini 返回 PCM 时后端封装为 WAV；MiniMax 解码 hex MP3；Qwen 使用 DashScope Bearer 鉴权、校验官方 OSS 音频 URL 后下载并透传真实音频类型；xAI 直接返回音频字节且不向上游发送 `model` 字段。
- TTS 发现服务对 Gemini、MiniMax、Qwen、xAI 回落到官方预设模型/音色，避免无发现接口的 Provider 误走 OpenAI 探测。
- Qwen 音频 URL 只允许 `http/https` 的 `*.oss-<region>.aliyuncs.com`，防止后端下载任意外部地址。
- 本轮不涉及数据库结构、迁移脚本、音频存储、计费统计、STT 或流式音频。

## 本轮完成状态

- RED 验证：新增 Provider 协议、动态 Content-Type、Qwen URL 防护和前端 Provider 预设测试后，旧实现按预期失败。
- 后端目标测试：`mvn.cmd -q "-Dtest=UserTtsConnectivityTestServiceImplTest,UserTtsSpeechServiceImplTest,TtsDiscoveryServiceImplTest,SysTtsConfigServiceImplTest,UserAiConfigServiceImplTest,UserAiConfigControllerTest,AdminTtsConfigControllerTest,InterviewControllerTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 前端目标测试和构建结果见 `frontend/tasks/stage.md`。
- 关联任务文件：`tasks/TASK_85_TTS_REMAINING_PROVIDERS_BACKEND.md`、`frontend/tasks/TASK_85_TTS_REMAINING_PROVIDERS_FRONTEND.md`。

## 尚未开始的功能

- 未实现真实厂商 Key 的自动化端到端验收，需人工在设置页或管理端填写后验证。
- 未实现 STT、流式音频、音频存储、TTS 计费统计或独立语音页面。

## 停止，不继续下一个功能
本轮仅完成 Gemini、MiniMax、Qwen、xAI TTS Provider 后端适配，等待验收，不继续推进其它语音能力。
---

# EdgeTTS 云端语音 Provider 后端（2026-06-05）

## 已完成且已验证的功能

- 新增 EdgeTTS provider：`ttsProvider=edge`，默认地址 `https://speech.platform.bing.com`，默认模型 `edge-tts`。
- 新增 Edge Read Aloud WebSocket 客户端，后端合成 MP3 后继续复用语音面试云端 TTS Blob 播放链路。
- 用户自定义 TTS 和系统级 TTS 均支持 EdgeTTS 空 API Key 保存、测试、试听和运行时解析。
- TTS 模型/音色发现对 EdgeTTS 直接返回内置预设，不访问上游发现端点。
- 音色预设包含 `zh-CN-XiaoxiaoNeural`、`zh-CN-YunxiNeural` 等 Edge Neural 免费音色。
- 本轮不涉及数据库结构、迁移脚本、音频存储、计费统计或流式音频。

## 本轮完成状态

- RED 验证：旧实现下目标测试缺少 EdgeTTS provider、无 Key 校验和 EdgeTTS 合成客户端。
- GREEN 验证：`mvn.cmd -q "-Dtest=UserTtsSpeechServiceImplTest,UserTtsConnectivityTestServiceImplTest,TtsDiscoveryServiceImplTest,UserAiConfigServiceImplTest,SysTtsConfigServiceImplTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 前端接入和构建结果见 `frontend/tasks/stage.md`。
- 关联任务文件：`tasks/TASK_84_EDGETTS_PROVIDER_BACKEND.md`、`frontend/tasks/TASK_84_EDGETTS_PROVIDER_FRONTEND.md`。

## 尚未开始的功能

- 未实现音频持久化、TTS 计费统计、流式音频、STT 或更多 EdgeTTS 管理能力。
- 未修改浏览器本地 `speechSynthesis` voice 预设。

## 停止，不继续下一个功能

本轮仅完成 EdgeTTS Provider 后端接入，等待验收，不继续推进其它语音能力。

---

# 管理端 Dashboard 聚合接口后端（2026-06-04）

## 已完成且已验证的功能

- 新增 `GET /api/admin/dashboard/summary`，一次返回 `overview/trends/hotJobRoles/businessDistribution`。
- 新增 `DashboardSummaryResponse`，聚合复用现有四块 dashboard 响应结构。
- `AdminDashboardService` 与 `AdminDashboardServiceImpl` 已新增聚合查询方法，旧四个 dashboard 接口保持兼容。
- Redis 缓存新增 `admin:dashboardSummary`，TTL 为 5 分钟，并启用同步缓存加载。
- 本轮不涉及数据库结构、索引、连接池、简历轮询或 TTS 逻辑。

## 本轮完成状态

- RED 验证：`mvn.cmd -q "-Dtest=AdminDashboardServiceImplTest,AdminControllerTest" test` 在旧实现下编译失败，缺少聚合 DTO 和服务方法。
- GREEN 验证：`mvn.cmd -q "-Dtest=AdminDashboardServiceImplTest,AdminControllerTest,RedisConfigTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 后端全量验证：`mvn.cmd -q test` 通过；异常路径测试会输出预期堆栈日志。
- 前端单请求加载和构建结果见 `frontend/tasks/stage.md`。
- 关联任务文件：`tasks/TASK_83_ADMIN_DASHBOARD_SUMMARY_BACKEND.md`、`frontend/tasks/TASK_83_ADMIN_DASHBOARD_SUMMARY_FRONTEND.md`。

## 尚未开始的功能

- 未新增 dashboard 独立页面、实时推送、WebSocket、SSE 或新的统计维度。
- 未修改数据库 schema、SQL 索引、连接池或慢查询配置。
- 未继续推进其它请求量优化。

## 停止，不继续下一个功能

本轮仅完成管理端 dashboard 聚合接口后端能力，等待验收，不继续推进其它功能。

---

# 请求噪音与误报修复后端（2026-06-04）

## 已完成且已验证的功能

- `GlobalExceptionHandler` 已新增 405 请求方法不支持专用 handler，HTTP 状态返回 405，响应体沿用参数错误码，message 为 `请求方法不支持`。
- 405 日志降级为 `warn`，仅记录 method 和 supported methods，不打印堆栈，避免进入通用 500 系统异常日志。
- `TtsDiscoveryServiceImpl` 已新增 5 分钟本地负向缓存，仅缓存同一 `provider + normalizedBaseUrl` 下 `/audio/voices` 与 `/v1/audio/voices` 均明确 404 的音色端点不可用结果。
- TTS 缓存不保存 API Key，不缓存 401/403、网络异常或超时；API Key 验证和 `/models` 模型发现仍每次执行。
- 本轮不涉及数据库结构、连接池、索引、前端页面、简历轮询或 dashboard 请求合并。

## 本轮完成状态

- RED 验证：`mvn.cmd -q "-Dtest=GlobalExceptionHandlerTest,TtsDiscoveryServiceImplTest" test` 在旧实现下因缺少 405 handler 编译失败。
- GREEN 验证：`mvn.cmd -q "-Dtest=GlobalExceptionHandlerTest,TtsDiscoveryServiceImplTest,ServerApplicationTests,UserTtsSpeechServiceImplTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 后端全量验证：`mvn.cmd -q test` 通过。
- 关联任务文件：`tasks/TASK_81_REQUEST_NOISE_ERROR_MISREPORT_BACKEND.md`。

## 尚未开始的功能

- 未调整简历诊断结果页 processing 阶段轮询间隔，未改 SSE 或长轮询。
- 未新增管理端 dashboard 聚合接口，未改前端 dashboard 单请求加载。
- 未做数据库配置、连接池、索引或慢查询层面的修改。

## 停止，不继续下一个功能

本轮仅完成请求噪音与误报修复计划的第一轮后端最小修复，等待验收，不继续推进轮询和 dashboard 优化。

---

# 管理端系统级 TTS 配置后端（2026-06-04）

## 已完成且已验证的功能

- 新增系统级 TTS 独立配置表 `sys_tts_config`，并同步 `db/schema.sql` 与 `server/db/schema.sql`。
- 新增管理端系统 TTS 配置接口：查询、保存、连通测试、模型/音色发现、音色预览。
- 新增用户侧只读状态接口 `GET /api/user/ai-config/system-tts-status`，仅返回 `systemTtsAvailable`，不暴露系统配置明细。
- 语音面试 TTS 解析链路已支持用户自定义 `interview/default` 优先、系统 TTS 兜底。
- `InterviewController` 的 TTS capability 已返回 `systemTtsAvailable`，并按来源返回 `engine=user_custom/system/browser`。
- Redis 缓存新增 `config:systemTts`，TTL 为 30 分钟。

## 本轮完成状态

- RED 验证：`mvn.cmd -q "-Dtest=UserAiConfigControllerTest" test` 因缺少系统 TTS 状态 DTO 和接口失败。
- GREEN 验证：`mvn.cmd -q "-Dtest=UserAiConfigControllerTest,AdminTtsConfigControllerTest,SysTtsConfigServiceImplTest,UserTtsSpeechServiceImplTest,InterviewControllerTest" test` 通过。
- 后端回归：`mvn.cmd -q "-Dtest=AdminTtsConfigControllerTest,SysTtsConfigServiceImplTest,UserTtsSpeechServiceImplTest,InterviewControllerTest,UserAiConfigControllerTest,RedisConfigTest,SchemaConsistencyTest" test` 通过。
- 后端编译：`mvn.cmd -q -DskipTests compile` 通过。
- 前端配置页、设置页和面试页接入结果见 `frontend/tasks/stage.md`。
- 关联任务文件：`tasks/TASK_80_SYSTEM_TTS_CONFIG_BACKEND.md`、`frontend/tasks/TASK_80_SYSTEM_TTS_CONFIG_FRONTEND.md`。

## 尚未开始的功能

- 未实现系统 TTS 计费统计、音频存储、流式音频、更多 Provider 专用协议。
- 未继续推进 `develop-project.txt` 功能 3 的浏览器音色预设扩展。

## 停止，不继续下一个功能

本轮仅完成 `develop-project.txt` 功能 2（管理端系统级 TTS 配置）的后端部分，等待验收，不继续推进其它功能。

---

# 用户额度消费记录与展示增强（2026-06-04）

## 已完成且已验证的功能

- 新增 `user_quota_consumption_log` 表，覆盖全部 6 种额度类型的消费记录（扣减 + 退款）
- `QuotaConsumptionLogService` 服务接口和实现，提供 logConsumption / 分页查询 / 定时清理
- `UserQuotaServiceImpl` 7 个扣减/退款方法集成消费记录写入（同一事务内）
- `UserInfoResponse` 新增 freePolishLeft / freeJdMatchLeft / freeTemplateLeft / freeOfferLeft 4 个字段
- `AuthServiceImpl.getCurrentUserInfo()` 补充非 VIP 用户的 4 种免费额度计算
- 用户端 API `GET /api/user/quota/consumption-log`（分页 + 类型筛选）
- 管理端 API `GET /api/admin/users/{userId}/consumption-log`
- `@Scheduled(cron = "0 0 3 * * ?")` 定时清理过期消费记录（保留天数从 sys_config 读取，默认 90 天）
- `db/schema.sql` 和 `server/db/schema.sql` 已同步新增表定义
- 单元测试 `QuotaConsumptionLogServiceImplTest`（6 个测试用例）

## 本轮完成状态

- 后端编译验证：`mvn clean compile` ✅ 通过（exit 0）
- 前端构建验证：`npm run build` ✅ 通过（5182 modules, 19.17s）
- 数据库双副本同步：`db/schema.sql` 和 `server/db/schema.sql` 一致 ✅
- 关联任务文件：`tasks/task-用户额度消费记录与展示增强.md`、`frontend/tasks/task-用户额度消费记录与展示增强.md`

## 尚未开始的功能

- 无（本功能已完成）

## 停止，不继续下一个功能

本轮仅完成用户额度消费记录与展示增强，等待验收，不继续推进其他功能。

---

# 管理端功能分布日期范围扩展后端（2026-06-04）
## 已完成且已验证的功能

- 管理端自定义 AI 用量统计接口 `/api/admin/custom-ai/usage-stats` 已支持 `startDate/endDate` 日期范围参数。
- 旧 `date` 参数保持向后兼容；传入 `date` 时仍按单日查询，优先级高于日期范围。
- 功能分布、汇总指标、用户明细分页和用户维度类型统计已共用同一日期范围，SQL 使用 `usage_date BETWEEN #{startDate} AND #{endDate}` 聚合多天调用量。
- 服务层已增加日期范围归一化：无参数默认今天，单侧日期按单日处理，拒绝开始日期晚于结束日期和超过 90 天的范围。
- 本轮不涉及数据库表结构、写入链路或计费规则变更。

## 本轮完成状态

- RED 验证：新增/调整控制层、服务层和 Mapper 范围查询测试后，旧实现因接口签名、DTO 字段和 Mapper 方法缺失而失败。
- GREEN 验证：同一组用例修复后通过。
- 后端定向验证：`mvn.cmd -q "-Dtest=AdminCustomAiStatsControllerTest,UserAiUsageStatsServiceImplTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 后端全量验证：`mvn.cmd -q test` 通过。
- 前端日期范围选择器、快捷按钮和构建结果见 `frontend/tasks/stage.md`。
- 关联任务文件：`tasks/TASK_79_CUSTOM_AI_USAGE_STATS_DATE_RANGE_BACKEND.md`、`frontend/tasks/TASK_79_CUSTOM_AI_USAGE_STATS_DATE_RANGE_FRONTEND.md`。

## 尚未开始的功能

- 未实现系统级 TTS 配置。
- 未扩展 TTS 计费统计、音频存储、Provider 专用协议或新的统计页面。

## 停止，不继续下一个功能

本轮仅完成 `develop-project.txt` 中功能 1（功能分布日期范围扩展）的后端部分，等待验收，不继续推进系统级 TTS 配置。

# 重复查询优化剩余项全量修复（2026-06-04）
## 已完成且已验证的功能

- `UserQuotaServiceImpl` 的面试/简历检查、扣减和退还链路已统一改为一次性读取 `getActiveMembershipPlan(userId)`，不再组合调用 `isVipUser/getVipDaily*Limit`。
- `AdminUserRightsServiceImpl.getUserRights` 已改为一次用户查询、一次额度查询、一次日额度刷新，并基于已读取的 `SysUser` 快照判断 VIP 状态。
- `AdminController.getUserQuota` 已复用已查到的 `UserQuota`，不再额外调用两个 `getRemaining*Quota`。
- `ResumeDiagnosisTaskServiceImpl.createTask` 已删除平台 AI 路径的创建前 `checkResumeQuota`，由 `deductResumeQuota` 原子扣减负责最终检查；额度不足通知逻辑保留在扣减异常路径。
- `MembershipServiceImpl.mockUpgrade` 写操作后已改为一次 `getByUserId` + 一次 `refreshDailyQuotaIfNeeded`，不再分别查询剩余简历和面试额度。
- 本轮不涉及数据库结构、前端页面或接口协议变更。

## 本轮完成状态

- RED 验证：新增/扩展 10 个回归用例后，旧实现失败，分别复现 P0/P1/P2 的重复查询链路。
- GREEN 验证：同一组 RED 用例修复后通过。
- 后端定向回归：`mvn.cmd -q "-Dtest=UserQuotaServiceImplAtomicDeductionTest,AdminUserRightsServiceImplTest,AdminControllerTest,ResumeDiagnosisTaskServiceImplTest,MembershipServiceImplTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 后端全量验证：`mvn.cmd -q test` 通过。
- 残留扫描确认业务调用位点中不再存在本方案要求清理的重复查询组合；`UserQuotaServiceImpl` 自身的公共兼容方法声明保留。
- 关联任务文件：`tasks/task-重复查询优化剩余项全量修复.md`。

## 尚未开始的功能

- 未新增性能指标上报、接口协议、前端页面、数据库迁移或 SQL 索引。
- 未删除 `SysUserService` 接口上的兼容方法签名，保持其它调用方兼容。

## 停止，不继续下一个功能

本轮已完成 `fixes/redundant-query-fix-plan.md` 中第一轮之后的全部剩余项，等待验收，不继续推进其它性能优化。

# 重复查询优化第一轮（2026-06-04）
## 已完成且已验证的功能

- `UserQuotaServiceImpl` 的 AI 润色、JD 匹配、模板和 Offer 四个 VIP 配额扣减方法已统一改为一次性读取 `getActiveMembershipPlan(userId)`。
- 四个方法不再组合调用 `isVipUser(userId)`、`getVipDaily*Limit(userId)` 和 `getVipCycleLimit(userId, type)`，避免同一次扣减产生重复用户查询和套餐查询。
- 非会员免费次数扣减、原子 SQL、异常码和接口行为保持不变。
- 本轮不涉及数据库结构、前端页面或接口协议变更。

## 本轮完成状态

- RED 验证：新增 `UserQuotaServiceImplAtomicDeductionTest` 四个 P0-1 用例后，旧实现失败，失败信息确认旧链路未调用 `getActiveMembershipPlan` 且仍调用分散 VIP 查询方法。
- GREEN 验证：同一组新增用例修复后通过。
- 后端定向回归：`mvn.cmd -q "-Dtest=UserQuotaServiceImplAtomicDeductionTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 后端全量验证：`mvn.cmd -q test` 通过。
- 关联任务文件：`tasks/task-重复查询优化第一轮.md`。

## 尚未开始的功能

- 未继续修复 P0-2/P0-3/P0-4 的面试/简历扣减、退还和检查方法。
- 未修改 `AdminUserRightsServiceImpl`、`AdminController`、`ResumeDiagnosisTaskServiceImpl` 或 `MembershipServiceImpl`。
- 未新增数据库表、字段、索引或迁移脚本。

## 停止，不继续下一个功能

本轮仅完成 `fixes/redundant-query-fix-plan.md` 的 P0-1 四个 checkAndDeduct 方法重复查询优化，等待验收，不继续推进下一项优化。

# 模拟面试平台 AI 兜底路由锁定修复后端（2026-06-04）
## 已完成且已验证的功能

- `InterviewService` 新增会话级有效兜底路由判断：`request.fallbackToPlatform=true` 仍用于首次触发平台兜底，`ai_billing_source=platform/platform_fallback` 会在后续消息中持续锁定平台 AI。
- 非流式模拟面试消息和 SSE 流式模拟面试消息均改为使用 `effectiveFallbackToPlatform` 调用 AI 服务，避免已扣平台额度后下一轮又回到用户自定义 AI。
- 首次 `user_custom -> platform_fallback` 的扣费逻辑保持原子标记后只扣一次；后续 `platform_fallback` 消息不会重复扣平台面试额度。
- 新增服务层和控制层回归测试，覆盖已标记 `platform_fallback` 后续请求不带参数仍走平台、原生平台会话不会被用户自定义配置抢回去、SSE 入口继续传平台路由。

## 本轮完成状态

- RED 验证：`mvn.cmd -q "-Dtest=InterviewServiceTest#shouldKeepUsingPlatformAfterFallbackSessionAlreadyMarkedWithoutRequestFlag+shouldKeepPlatformSessionOnPlatformEvenWhenUserHasCustomAiConfig" test` 在旧实现下失败，复现 AI 服务调用仍收到 `fallback=false`。
- GREEN 验证：`mvn.cmd -q "-Dtest=InterviewServiceTest#shouldKeepUsingPlatformAfterFallbackSessionAlreadyMarkedWithoutRequestFlag+shouldKeepPlatformSessionOnPlatformEvenWhenUserHasCustomAiConfig,InterviewControllerTest#streamMessageShouldChargeFallbackQuotaBeforePersistingUserMessage+streamMessageShouldKeepUsingPlatformWhenFallbackSessionAlreadyMarkedWithoutRequestFlag" test` 通过。
- 后端回归验证：`mvn.cmd -q "-Dtest=InterviewServiceTest,InterviewControllerTest,SchemaConsistencyTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 后端全量测试：`mvn.cmd -q test` 通过。
- 关联任务文件：`tasks/TASK_74_INTERVIEW_PLATFORM_FALLBACK_BILLING_BACKEND.md`。

## 尚未开始的功能

- 未新增前端状态记忆或 UI 文案调整。
- 未新增平台兜底失败后的额度退款能力。
- 未调整简历诊断、Offer 辅助或其它 AI 链路的计费和路由规则。

## 停止，不继续下一个功能

本轮仅修复模拟面试平台 AI 兜底后的后续路由锁定问题，等待验收，不继续推进其它计费链路改造。

# 用户自定义 TTS 代码审查问题修复后端（2026-06-04）
## 已完成且已验证的功能

- `UserAiConfigServiceImpl` 的 TTS 请求归一化已保留 `endpointPath` 和 `ttsProvider`，连通测试、试听和保存前验证不再丢失前端传入的自定义 TTS 端点。
- `UserTtsConnectivityTestServiceImpl` 的 Chat Completions TTS 连通测试已严格解析 `choices[0].message.audio.data`，要求 base64 可解码且音频字节非空，避免普通文本 JSON 被误判连通成功。
- 前端设置页测试已同步当前 `.cai-form` 选择框高度规则和 `.cai-tts-discover-btn` 结构，旧选择器不再导致测试误失败。

## 本轮完成状态

- RED 验证：`mvn.cmd -q "-Dtest=UserAiConfigServiceImplTest,UserTtsConnectivityTestServiceImplTest" test` 在旧实现下失败，复现自定义端点丢失和 Chat Completions 缺少音频仍成功的问题。
- GREEN 验证：`mvn.cmd -q "-Dtest=UserAiConfigServiceImplTest,UserTtsConnectivityTestServiceImplTest" test` 通过。
- 后端回归验证：`mvn.cmd -q "-Dtest=UserAiConfigServiceImplTest,UserTtsConnectivityTestServiceImplTest,UserTtsSpeechServiceImplTest,CriticalEndpointRateLimitFilterTest,AdminAiEngineConnectivityControllerTest,AdminCustomAiStatsControllerTest,AiModelDiscoveryServiceImplTest,UserAiUsageStatsServiceImplTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 前端验证和构建结果见 `frontend/tasks/stage.md`。
- 关联任务文件：`tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`、`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

## 尚未开始的功能

- 未新增 Provider 协议、TTS 计费统计、音频存储、数据库迁移、设置页 UI 或新的语音能力。

## 停止，不继续下一个功能

本轮仅修复代码审查指出的 TTS 端点透传、Chat Completions 音频校验和前端测试断言问题，等待验收，不继续推进其它自定义 AI 能力。

# 自定义 AI 模型列表获取后端（2026-06-03）
## 已完成且已验证的功能

- 新增 OpenAI 兼容模型发现能力，后端代理请求 `GET {baseUrl}/models` 并解析 `data[].id` 为去重后的模型候选。
- 新增用户端 `POST /api/user/ai-config/models`，请求 `baseUrl/apiKey`，不保存配置，不回显完整 API Key。
- 新增管理端 `POST /api/admin/ai-engines/models`，编辑态 `apiKey` 为空时可复用已保存真实密钥。
- 用户端模型发现已接入关键端点限流，每用户或 IP 每分钟 5 次。
- 非公网 HTTPS、localhost、内网地址会在后端校验阶段被拦截，不发起外部请求。
- 前端候选输入、页面测试和构建结果见 `frontend/tasks/stage.md`。

## 本轮完成状态

- 后端目标测试：`mvn.cmd -q "-Dtest=AiModelDiscoveryServiceImplTest,AdminAiEngineConnectivityControllerTest,UserAiConfigServiceImplTest,CriticalEndpointRateLimitFilterTest" test` 通过。
- 后端编译：`mvn.cmd -q -DskipTests compile` 通过。
- 前端目标测试：`npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过。
- 前端构建：`npm.cmd run build` 通过。
- 关联任务文件：`tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`、`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

## 尚未开始的功能

- 未实现 Provider 专用模型列表协议。
- 未实现模型能力检测、价格信息、自动路由、自动保存或更多 Provider 模板。
- 未新增数据库表、字段或迁移脚本。

## 停止，不继续下一个功能

本轮仅完成 OpenAI 兼容 `/models` 模型列表获取，等待验收，不继续推进其它 AI Provider 能力。

# 自定义 TTS 在语音面试中真实播放后端（2026-06-03）
## 已完成且已验证的功能

- 新增语音面试 TTS capability 接口 `GET /api/interview/session/{sessionId}/tts-capability`，只返回可用性、引擎和配置类型，不返回 API Key、baseUrl、model 等敏感配置。
- 新增语音面试 TTS 合成接口 `POST /api/interview/session/{sessionId}/tts`，请求体为 `{ text }`，后端校验登录用户、会话归属、进行中状态、语音面试类型、文本非空和长度边界，成功返回 `audio/mpeg`。
- 新增 `UserTtsSpeechService`，按 `interview -> default` 解析启用且完整的 TTS 配置；`resume` 配置永不参与语音面试 TTS。
- TTS 调用复用 `AiCredentialCrypto` 解密 TTS Key，复用公网 HTTPS URL 校验，按 OpenAI 兼容 `/audio/speech` 请求 `model/voice/input/response_format=mp3`。
- 面试 TTS 合成接口已接入关键端点限流，匹配顺序早于通用面试 action 限流。
- 本轮不新增数据库表、迁移脚本、TTS 计费统计或音频持久化；TTS 播放失败不触发平台 AI fallback，不消耗平台额度。

## 本轮完成状态

- 后端定向验证：`mvn.cmd -q "-Dtest=UserTtsSpeechServiceImplTest,UserAiConfigServiceImplTest,CriticalEndpointRateLimitFilterTest,InterviewControllerTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 前端云端 TTS 播放接入、页面测试和构建结果见 `frontend/tasks/stage.md`。
- 关联任务文件：`tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`、`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

## 尚未开始的功能

- 未将 TTS 接入简历诊断、简历润色、JD 匹配或其它非语音面试场景。
- 未实现 STT、TTS 计费统计、音频缓存/存储、更多 Provider 协议或独立语音页面。

## 停止，不继续下一个功能

本轮仅完成用户自定义 OpenAI 兼容 TTS 在语音面试 AI 面试官播报中的真实播放，等待验收，不继续推进其它语音能力。

# TTS 配置折叠与通用兜底支持后端修正（2026-06-03）
## 已完成且已验证的功能

- 用户自定义 AI 的 TTS 配置边界已调整为 `default/interview` 可用，`resume` 不可用。
- 后端 `UserAiConfigServiceImpl` 对 `resume` 配置不再回显 TTS 字段，即使历史数据或请求体中存在 TTS 值，也会在保存时清空。
- 简历配置保存时不会执行 TTS 连通测试，避免简历诊断链路被无关 TTS 配置影响。
- 前端设置页已将 TTS 区块改为可折叠面板，并只在“通用兜底”和“面试对话”配置中展示；“未启用/已填写”胶囊已改为居中显示。

## 本轮完成状态

- RED 验证：`mvn.cmd -q "-Dtest=UserAiConfigServiceImplTest" test` 在旧实现下失败，复现通用兜底配置无法回显/保存 TTS。
- GREEN 验证：`mvn.cmd -q "-Dtest=UserAiConfigServiceImplTest" test` 通过。
- 后端回归验证：`mvn.cmd -q "-Dtest=UserAiConfigServiceImplTest,CriticalEndpointRateLimitFilterTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 前端设置页布局修正、页面测试和构建结果见 `frontend/tasks/stage.md`。
- 关联任务文件：`tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`、`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

## 尚未开始的功能

- 未切换模拟面试云端 TTS 播放链路。
- 未实现音频流推送、TTS 调用计费、TTS 统计或更多 Provider 协议。
- 未新增数据库迁移。

## 停止，不继续下一个功能

本轮仅修正 TTS 配置归属、折叠交互和状态胶囊布局，等待验收，不继续推进云端 TTS 运行时播放。

# 用户自定义 AI TTS 配置与连通测试后端（2026-06-03）
## 已完成且已验证的功能

- 用户自定义 AI 配置已支持保存 TTS 参数：`tts_base_url`、`tts_api_key`、`tts_model`、`tts_voice_id`，复用既有 `user_ai_config` 预留字段，不新增数据库迁移。
- TTS API Key 复用 `AiCredentialCrypto` 加密入库，用户配置响应只返回脱敏值，并通过 `ttsConfigured` 标记是否已配置 TTS。
- 新增用户端 `POST /api/user/ai-config/test-tts-connectivity`，按 OpenAI 兼容协议请求 `{baseUrl}/audio/speech` 做保存前连通测试。
- TTS 保存和连通测试前均复用 `PublicHttpsUrlValidator` 校验公网 HTTPS URL，防止 SSRF。
- TTS 连通测试已接入 `CriticalEndpointRateLimitFilter`，同用户或同 IP 每分钟最多 5 次。
- 本轮不修改模拟面试运行时播报链路；面试语音仍使用浏览器 `speechSynthesis`，不新增云端音频流推送或 TTS 计费。

## 本轮完成状态

- RED 验证：新增 TTS DTO、服务和限流测试后，旧实现因缺少 TTS 请求响应类型、服务方法和限流匹配失败。
- GREEN 验证：`mvn.cmd -q "-Dtest=UserAiConfigServiceImplTest,CriticalEndpointRateLimitFilterTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 前端设置页 TTS 配置区、API 封装、页面测试和构建结果见 `frontend/tasks/stage.md`。
- 关联任务文件：`tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`、`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

## 尚未开始的功能

- 未切换模拟面试云端 TTS 播放链路。
- 未实现后端 TTS 音频生成业务调用、音频缓存、音频流推送或 TTS 独立计费统计。
- 未新增数据库字段、表、索引或迁移脚本。
- 未扩展非 OpenAI 兼容 TTS Provider 协议。

## 停止，不继续下一个功能

本轮仅完成用户自定义 AI TTS 配置保存、回显和连通测试，等待验收，不继续推进云端 TTS 运行时播放或其它语音能力。

# 简历与面试 AI 日志标签统一修复后端（2026-06-03）
## 已完成且已验证的功能

- 用户自定义 AI 的日志标签统一为 `USER_CUSTOM/openai-compatible`，不再因为 OpenAI-compatible 协议 provider 显示成裸 `[OPENAI]`。
- 平台 AI 的日志标签统一为 `PLATFORM/<provider>`，启动日志和运行时平台调用不再显示裸 `[DEEPSEEK]`、`[MIMO]`、`[DOUBAO]`，避免和用户自定义请求混淆。
- 简历诊断、图片识别、JD 匹配、简历润色、模拟面试问答/报告、轻量摘要客户端等真实 AI 调用日志继续输出 `source/baseUrl/endpoint/model/configType`，排查时以这些字段判断真实路由。
- 本轮不修改 AI 路由、计费、回滚、Provider 配置解析、数据库结构、前端 UI 或摘要/报告策略，仅统一日志展示口径。

## 本轮完成状态
- RED 验证：`mvn.cmd -q "-Dtest=AiChatClientTest#shouldUsePlatformPrefixForPlatformRuntimeLogTag" test` 在旧实现下失败，复现平台标签仍为裸 `MIMO`。
- GREEN 验证：`mvn.cmd -q "-Dtest=AiChatClientTest,ResumeAiServiceImplTest,InterviewAiServiceImplTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 日志残留扫描：`rg "String tag = this\\.provider\\.toUpperCase\\(|runtimeConfig\\.provider\\(\\)\\.toUpperCase\\(|provider\\.toUpperCase\\(|\\[OPENAI\\]|\\[DEEPSEEK\\]|\\[MIMO\\]" server/src/main/java/com/airesume/server` 无命中。
- 关联任务文件：`tasks/TASK_77_AI_LOG_TAG_ROUTE_CLARITY_BACKEND.md`。

## 尚未开始的功能

- 未新增数据库字段或迁移脚本。
- 未实现新的 AI Provider 协议、自动平台回退、TTS 调用/UI、统计图表或新的前端展示能力。

## 停止，不继续下一个功能
本轮仅修复 AI 调用日志标签和路由展示口径，等待验收，不继续推进其它 AI 能力。

# 模拟面试用户自定义 AI 日志标签残留修复后端（2026-06-03）
## 已完成且已验证的功能

- 修复 `InterviewAiServiceImpl.buildConversationMessages(...)` 仍用默认 `provider.toUpperCase()` 打日志的问题；用户自定义 AI 会话的“对话消息组装完成”等请求前日志现在使用运行时 `USER_CUSTOM/openai-compatible` 标签。
- 修复报告解析 `parseEvaluationResponse(...)` 和 thinking warning `buildThinkingConfig(...)` 的同类标签残留，避免 AI 请求已走用户自定义配置但解析/提示日志仍显示 `[DEEPSEEK]`、`[MIMO]` 或默认 provider。
- 本轮不修改 AI 路由、计费、摘要压缩、报告生成策略、数据库结构或前端 UI；仅修正单次请求链路中的日志标签口径。
- 构造函数启动日志仍保留默认平台 provider 展示，它只表示服务初始化配置，不作为单次用户请求路由判断依据。

## 本轮完成状态

- RED 验证：`mvn.cmd -q "-Dtest=InterviewAiServiceImplTest" test` 在旧实现下因新签名不存在失败，覆盖消息组装、报告解析、thinking warning 三条路径。
- GREEN 验证：`mvn.cmd -q "-Dtest=InterviewAiServiceImplTest" test` 通过。
- 后端定向回归：`mvn.cmd -q "-Dtest=InterviewServiceTest,InterviewAiServiceImplTest,InterviewContextCompressorTest,AiChatClientTest,UserAiConfigResolverImplTest,UserAiUsageLimitServiceImplTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 后端全量验证：`mvn.cmd -q test` 通过。
- 关联任务文件：`tasks/TASK_76_INTERVIEW_CUSTOM_AI_REPORT_SUMMARY_BACKEND.md`。

## 尚未开始的功能

- 未修改用户自定义 AI provider 字段、数据库结构、平台 fallback 策略、摘要/报告压缩策略、TTS 配置或统计图表。
- 未处理启动日志的默认平台 provider 展示；该日志不属于单次请求路由链路。

## 停止，不继续下一个功能

本轮仅修复模拟面试用户自定义 AI 请求链路的日志标签残留问题，等待验收，不继续推进其它 AI Provider 能力。

# 管理端 AI 引擎配置与自定义 AI 用量分区修复前端联动（2026-06-03）
## 已完成且已验证的功能

- 管理端 AI 引擎配置页已新增“引擎配置 / 自定义 AI 用量”同页分区切换，默认展示引擎配置主流程。
- 每日上限、用量统计、趋势图、功能分布和用户明细已移入“自定义 AI 用量”分区，不再默认挤压配置筛选栏和 AI 引擎配置表。
- 用户明细继续按 `page/pageSize` 请求既有分页接口；分页 footer 在存在明细时展示总数和翻页控件，避免误判为全量列表。
- 本轮不修改后端接口、日期规则、聚合 SQL、权限校验、计费、回滚、Provider 配置解析或数据库结构。

## 本轮完成状态

- 前端 RED 验证：旧实现下 `npm.cmd test -- --run src/__tests__/views/AdminAiEngineView.test.js` 失败，复现缺少分区切换且统计区默认显示。
- 前端 GREEN 验证：`npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，2 个测试文件 / 14 个用例。
- 前端构建验证：`npm.cmd run build` 通过。
- 关联任务文件：`tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`、`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

## 尚未开始的功能

- 未在管理端首页新增趋势表或趋势图。
- 未实现周聚合、月聚合、独立统计页面、TTS 调用或 TTS 配置 UI。
- 未修改自定义 AI 扣费、失败回滚、平台手动 fallback、Provider 配置解析或加密存储规则。

## 停止，不继续下一个功能

本轮仅修复管理端 AI 引擎配置页与自定义 AI 用量统计的布局干扰和分页可见性，等待验收，不继续推进其它 AI Provider 能力。

# 管理端自定义 AI 趋势图布局修复前端联动（2026-06-03）
## 已完成且已验证的功能

- 管理端 AI 引擎配置页的“用户自定义 AI 按日趋势”已改为默认折叠摘要行，避免完整折线图默认占用首屏并把 AI 引擎配置筛选栏和表格挤到下方。
- 摘要行展示趋势日期范围、区间总调用和活跃用户数；点击“展开趋势”后才展示近 7 天、近 30 天、自定义日期范围和折线图。
- 本轮不修改后端接口、日期规则、聚合 SQL、权限校验、计费、回滚、Provider 配置解析或数据库结构。

## 本轮完成状态

- 前端 RED 验证：旧实现下 `npm.cmd test -- --run src/__tests__/views/AdminAiEngineView.test.js` 失败，复现趋势图默认展开且没有折叠开关。
- 前端 GREEN 验证：`npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，2 个测试文件 / 12 个用例。
- 前端构建验证：`npm.cmd run build` 通过。
- 关联任务文件：`tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`、`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

## 尚未开始的功能

- 未在管理端首页新增趋势表或趋势图。
- 未实现周聚合、月聚合、独立统计页面、TTS 调用或 TTS 配置 UI。
- 未修改自定义 AI 扣费、失败回滚、平台手动 fallback、Provider 配置解析或加密存储规则。

## 停止，不继续下一个功能

本轮仅修复管理端自定义 AI 趋势图首屏布局挤压问题，等待验收，不继续推进其它 AI Provider 能力。

# 管理端自定义 AI 按日趋势图后端（2026-06-03）
## 已完成且已验证的功能

- 新增管理端 `GET /api/admin/custom-ai/usage-trends`，继续复用管理员权限校验。
- 查询参数支持 `startDate/endDate`：默认近 7 天且包含今天；只传一个日期时按单日查询；`startDate > endDate` 返回业务错误；查询范围最大 90 天。
- 趋势数据基于既有 `user_ai_usage_detail` 明细表按日聚合，不新增表、不新增字段、不新增迁移脚本。
- Mapper 新增固定 SQL：按 `usage_date + usage_type` 汇总调用次数，按 `usage_date` 统计当日活跃用户数，复用现有 `(usage_date, usage_type)` 索引。
- Service 已补齐缺失日期 0 值、`usageType` 白名单归一和中文 label 填充，未知类型统一归入 `unknown`。
- 趋势响应只返回日期、调用量、活跃用户数和功能类型统计，不返回 API Key、baseUrl、model 或 Provider 私密配置。

## 本轮完成状态

- 后端定向验证：`mvn.cmd -q "-Dtest=UserAiUsageStatsServiceImplTest,AdminCustomAiStatsControllerTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 前端 AI 引擎配置页趋势图、页面测试和构建结果见 `frontend/tasks/stage.md`。
- 关联任务文件：`tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。

## 尚未开始的功能

- 未在管理端首页新增趋势表或趋势图。
- 未实现周聚合、月聚合、独立统计页面、TTS 调用或 TTS 配置 UI。
- 未修改自定义 AI 扣费、失败回滚、平台手动 fallback、Provider 配置解析或加密存储规则。

## 停止，不继续下一个功能

本轮仅补齐管理端用户自定义 AI 按日趋势图后端能力，等待验收，不继续推进其它 AI Provider 能力。

# 用户自定义 AI 使用统计增强后端（2026-06-03）
## 已完成且已验证的功能

- 新增 `user_ai_usage_detail` 明细表，按用户、日期、功能类型记录自定义 AI 调用次数；`user_ai_daily_usage` 仍作为每日限额来源。
- 自定义 AI 调用链路已写入明确功能类型：简历诊断、简历润色、JD 匹配、面试消息、面试报告、面试摘要、Offer 辅助和未知类型兜底。
- 自定义 AI 调用失败时会同时回滚每日总次数和功能明细次数，避免失败请求进入管理端统计。
- 新增管理端 `GET /api/admin/custom-ai/usage-stats`，支持按日期查看总调用量、配置用户数、活跃用户数、功能分布和用户明细分页。
- 管理端统计接口只返回调用次数和用户基础展示信息，不返回 API Key、baseUrl 或模型密钥类敏感配置。

## 本轮完成状态

- 后端定向验证：`mvn.cmd -q "-Dtest=UserAiUsageLimitServiceImplTest,UserAiUsageStatsServiceImplTest,AdminCustomAiStatsControllerTest,SchemaConsistencyTest,ResumeDiagnosisControllerTest,InterviewControllerTest,InterviewServiceTest,InterviewContextCompressorTest,OfferAssistServiceImplTest,ResumeDiagnosisProcessorTest" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 前端管理页统计入口、页面测试和构建结果见 `frontend/tasks/stage.md`。
- 关联任务文件：`tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`。

## 尚未开始的功能

- 未实现 TTS 调用或 TTS 配置 UI。
- 未实现按日/周/月统计图表、趋势图或新的独立统计页面。
- 未扩展非 OpenAI 兼容协议，也未新增自动平台回退策略。

## 停止，不继续下一个功能

本轮仅补齐用户自定义 AI 使用统计增强，等待验收，不继续推进其它 AI Provider 能力。

# 模拟面试报告与摘要用户自定义 AI 路由修复后端（2026-06-03）
## 已完成且已验证的功能

- 面试报告生成已根据会话 `ai_billing_source` 沿用本场面试 AI 来源：`user_custom` 会话继续走用户 `interview/default` 自定义 AI；平台会话和 `platform_fallback` 会话走平台 AI。
- 面试上下文摘要压缩和最终评价摘要压缩已透传 `userId/fallbackToPlatform`，不再因为后台摘要缺少用户上下文退回平台 MIMO。
- 用户自定义 AI 的后台摘要和最终报告调用已计入每日自定义 AI 次数；摘要调用失败、报告 AI 调用或解析失败时会回滚对应次数。
- AI 日志标签已区分用户自定义 OpenAI-compatible 调用，显示 `USER_CUSTOM/openai-compatible`，并输出 `source/baseUrl/model/configType`，避免 `[OPENAI]`、`[MIMO]` 等标签混淆真实路由。
- 摘要超长不再按普通成功路径静默硬截断；报告 prompt 超限会先使用短结构化摘要和更少最近消息重建，只有最终兜底才记录 `last_resort_truncate=true`。

## 本轮完成状态

- RED 验证：`mvn.cmd -q "-Dtest=InterviewServiceTest,InterviewContextCompressorTest" test` 在旧实现下编译失败，复现 `compressForEvaluation` 缺少用户 AI 上下文重载。
- GREEN 验证：`mvn.cmd -q "-Dtest=InterviewServiceTest,InterviewAiServiceImplTest,InterviewContextCompressorTest,AiChatClientTest,UserAiConfigResolverImplTest,UserAiUsageLimitServiceImplTest" test` 通过。
- 编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 后端完整验证：`mvn.cmd -q test` 通过。
- 关联任务文件：`tasks/TASK_76_INTERVIEW_CUSTOM_AI_REPORT_SUMMARY_BACKEND.md`。

## 尚未开始的功能

- 未修改数据库结构、用户配置 Provider 字段、前端 UI、TTS 配置、统计图表或其它 AI 业务链路。
- 未新增自动平台回退策略；只有请求显式携带 `fallbackToPlatform=true` 或会话已是平台来源时才走平台 AI。

## 停止，不继续下一个功能

本轮仅修复模拟面试报告和摘要的用户自定义 AI 路由、计数、日志和截断策略，等待验收，不继续推进其它 AI 能力。

# 未提交改动代码审查问题后端修复（2026-06-02）
## 已完成且已验证的功能

- 认证验证码校验已改为 Redis `getAndDelete` 原子消费，避免并发请求复用同一个 `captchaId`。
- `AuthServiceImplTest` 已补齐 `CaptchaService` mock 和验证码字段，注册、登录渐进式验证码、重置密码测试不再绕过构造器依赖。
- 模拟面试流式平台 AI fallback 已先完成额度校验和扣减，再保存用户消息，避免额度不足时留下无回复的半截会话。
- 后端定向验证：`mvn.cmd '-Dtest=CaptchaServiceImplTest,AuthServiceImplTest,InterviewControllerTest' test` 通过，27 个用例全绿。
- 后端完整验证：`mvn.cmd test` 通过，658 个用例全绿。
- 前端配套修复见 `frontend/tasks/stage.md`。
- 关联任务文件：`tasks/TASK_CODE_REVIEW_UNCOMMITTED_FIX_2026_06_02_BACKEND.md`。

## 停止，不继续下一个功能

本轮仅修复未提交改动代码审查指出的后端问题，等待验收，不继续扩展验证码、登录策略、面试计费或其它 AI 链路。

# 简历诊断结果字段别名归一化后端修复（2026-06-02）
## 已完成且已验证的功能

- 已确认 `debug.txt` 中任务在 `completed` 后前端仍请求了完整详情接口，因此“只有综合评价和分数”的问题不是轻量级状态轮询导致的。
- 已修复后端 `ResumeDiagnosisResult` 对 AI 常见字段别名的兼容，模型返回 `skills`、`workExperience`、`projectExperience`、`education`、`positioning`、`suggestions` 等字段时，会归一化保存到标准诊断结果字段。
- 已补充内部维度字段别名兼容，保留加分项、扣分项、建议、技能列表、经历列表和项目列表等内容。
- 最终入库结构保持前端已使用的标准字段名，不修改接口协议和数据库结构。

## 本轮完成状态

- RED 验证：`processTaskShouldPreserveCommonAiFieldAliasesDuringNormalization` 在旧实现下失败，复现别名字段被忽略导致维度数据丢失。
- GREEN 验证：`mvn.cmd test "-Dtest=ResumeDiagnosisProcessorTest#processTaskShouldPreserveCommonAiFieldAliasesDuringNormalization"` 通过。
- 回归验证：`mvn.cmd test "-Dtest=ResumeDiagnosisProcessorTest"` 通过，10 个用例全绿。
- 编译验证：`mvn.cmd -DskipTests compile` 通过。
- 前端配套修复见 `frontend/tasks/stage.md`。
- 关联任务文件：`tasks/TASK_75_RESUME_DIAGNOSIS_RESULT_ALIAS_NORMALIZATION_BACKEND.md`。

## 尚未开始的功能

- 未修改数据库结构、Prompt、AI 调用链路、状态轮询接口或完整详情接口。
- 未对已经由旧逻辑入库且字段已丢失的历史报告做批量重建；这类报告需要重新诊断或重新处理原始 AI 响应才可能恢复缺失维度。

## 停止，不继续下一个功能

本轮仅修复简历诊断结果字段别名在后端归一化时被丢弃的问题，等待验收，不继续推进其它简历功能。
# 登录 token 有效期延长到 7 天（2026-06-02）

## 已完成且已验证的功能

- 当前 `jwt.expiration` 已确认：默认配置原为 4 小时，开发/生产配置原为 1 天。
- `JwtProperties` 默认有效期、`application.yml`、`application-dev.yml`、`application-prod.yml` 已统一改为 `604800000` 毫秒，即 7 天。
- 登录接口返回的 `expiresIn` 会随配置返回 `604800` 秒，`docs/api/TASK_02_AUTH_API.md` 已同步更新。
- 管理端登录复用 `/api/auth/login`，只是前端保存到独立的 `ai_resume_admin_token`；前端没有额外一天过期逻辑，因此管理员登录 token 同步变为 7 天。

## 本轮完成状态

- RED 验证：`mvn.cmd test "-Dtest=RuntimeProtectionConfigTest,JwtPropertiesTest"` 在修改前失败，失败点为 JWT 配置和默认值仍未达到 7 天。
- GREEN 验证：`mvn.cmd test "-Dtest=RuntimeProtectionConfigTest,JwtPropertiesTest,JwtUtilTest,AuthServiceImplTest"` 通过，53 个用例全绿。
- `mvn.cmd -DskipTests compile` 通过。
- 关联任务文件：`tasks/TASK_AUTH_TOKEN_EXPIRATION_SEVEN_DAYS_BACKEND.md`。

## 尚未开始的功能

- 未新增 refresh token、记住登录、设备管理、强制下线或独立管理员 token 策略。
- 未修改数据库结构、登录接口路径、JWT 签名密钥或管理员权限校验逻辑。

## 停止，不继续下一功能

本轮仅完成普通用户和管理员登录 JWT 有效期延长到 7 天，等待验收，不继续推进其它认证能力。

# 简历诊断状态轮询与 SQL 日志脱敏后端优化（2026-06-02）

## 已完成且已验证的功能

- 新增 `GET /api/resume/task/{taskId}/status` 轻量状态接口，简历诊断等待页可只读取任务状态、阶段、失败信息和更新时间。
- 轻量状态查询保留任务归属校验，不返回 `resume_text` 和 `diagnosis_result` 大字段，降低等待阶段数据库读取压力。
- 默认和开发环境 MyBatis 日志实现已改为 `NoLoggingImpl`，避免 SQL 明细日志输出 API Key、简历原文和完整诊断 JSON。
- 已补充 Controller、Service 和运行配置回归测试。

## 本轮完成状态

- `mvn.cmd test "-Dtest=ResumeDiagnosisControllerTest,ResumeDiagnosisTaskServiceImplTest,RuntimeProtectionConfigTest"` 通过，24 个用例全绿。
- `mvn.cmd -DskipTests compile` 通过。
- 前端联动状态见 `frontend/tasks/stage.md`。
- 关联任务文件：`tasks/TASK_73_RESUME_DIAGNOSIS_STATUS_POLLING_AND_SQL_LOG_BACKEND.md`。

## 尚未开始的功能

- 未修改数据库结构、诊断报告生成 prompt、AI 调用链路或简历结果展示内容。
- 未接入真实慢查询 EXPLAIN 或生产数据库指标采样；本轮基于日志暴露的问题做应用层和日志配置最小优化。

## 停止，不继续下一功能

本轮仅完成简历诊断状态轮询降载和 MyBatis SQL 明细日志脱敏，等待验收，不继续推进其它简历功能。

# 模拟面试报告尾部未答追问评分口径修复（2026-06-02）

## 已完成且已验证的功能

- 报告生成 prompt 已明确尾部未回答规则：面试官最后一条追问如果后面没有候选人回答，只能作为上一轮回答的后续追问上下文，不得作为独立 `questionPerformance/roundReviews` 条目，也不得因此给 0 分。
- 报告原始对话明细已裁剪到最后一个候选人回答，避免模型从松散对话记录中误判“最后追问未回答”。
- 尾部追问仍保留在有效问答轮次的“后续反馈或追问”中，用于复盘下一步练习方向。

## 本轮完成状态

- `mvn.cmd test "-Dtest=InterviewAiServiceImplTest"` 通过，39 个用例全绿。
- 关联任务文件：`tasks/TASK_72_INTERVIEW_REPORT_TRAILING_UNANSWERED_BACKEND.md`。

## 停止，不继续下一功能

本轮只处理报告尾部未答追问的评分口径，不继续修改数据库结构、SSE 协议、前端报告页面或更多面试功能。

# 模拟面试会话轮询与报告覆盖优化后端（2026-06-02）

## 已完成且已验证的功能

- 新增 `GET /api/interview/session/{sessionId}/status` 轻量状态接口，前端轮询可只读取状态、开场白 pending、报告 ready、综合分和更新时间。
- 轻量状态查询保持会话归属校验，不返回聊天记录、岗位上下文和 `evaluation_report` 大字段，降低开场白等待和报告等待阶段的数据库读取压力。
- 评价报告生成 prompt 已补充“有效问答轮次总数”和逐轮“问题/回答/后续反馈或追问”摘要，减少长面试报告只覆盖少数回答的风险。
- 已补充 Controller、Service、AI prompt 相关回归测试。

## 本轮完成状态

- `mvn.cmd "-Dtest=InterviewAiServiceImplTest#buildEvaluationUserPromptShouldExposeEffectiveQuestionAnswerRounds" test` 已按 RED/GREEN 验证通过。
- `mvn.cmd "-Dtest=InterviewControllerTest,InterviewServiceTest,InterviewAiServiceImplTest" test` 通过，76 个用例全绿。
- `mvn.cmd -DskipTests compile` 通过。
- 前端联动状态见 `frontend/tasks/stage.md`。

## 尚未开始的功能

- 未新增报告导出、报告重排、面试回放新页面或新的数据库结构。
- 未接入真实慢查询 EXPLAIN 或生产库指标采样；本轮基于日志问题做应用层最小优化。

## 停止，不继续下一功能

本轮仅完成模拟面试会话轮询降载和报告有效问答轮次覆盖优化，等待验收，不继续推进其它面试功能。

# 用户自定义 AI Provider 后端接入（2026-06-01）

## 未提交改动 Review 修复（2026-06-01）

## 已完成且已验证的功能

- 用户自定义 AI 配置删除已改为物理删除当前有效记录，避免敏感 API Key 留在逻辑删除记录中，并消除重复删除/重建时的唯一键占用风险。
- 面试流式自定义 AI 异步失败时会回滚已扣的自定义 AI 次数，并通过 SSE 错误 payload 暴露 4090/4091 等自定义 AI 错误码。
- Offer 辅助已纳入用户自定义 AI 独立每日次数统计，失败时回滚。
- 简历诊断图片页多模态解析已透传 `userId/fallbackToPlatform/requireUserCustom`，确保 PDF 提取阶段与任务创建时锁定的 AI 来源一致。
- 后端测试已同步新的重载签名和回滚断言。

## 本轮完成状态

- `mvn.cmd -q "-Dtest=UserAiConfigServiceImplTest,OfferAssistServiceImplTest,InterviewServiceTest,ResumeDiagnosisProcessorTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。
- `mvn.cmd test` 通过，641 个用例，0 失败，0 错误。
- 前端 review 修复结果见 `frontend/tasks/stage.md`。

## 尚未开始的功能

- 未实现 TTS 调用或 TTS 配置 UI。
- 未新增自定义 AI 统计图表或更多 Provider 协议适配。

## 停止，不继续下一个功能

本轮仅修复 review 发现的问题，等待验收，不继续推进下一项 AI 能力。

## 已完成且已验证的功能

- 已新增用户自定义 AI 配置、每日用量、系统配置三类数据结构，并同步 `db/` 与 `server/db/` 两套 migration/schema。
- 已实现用户自定义 AI 配置 CRUD、启停、连通测试、用量查询接口，以及管理员每日上限查询/修改接口。
- 已实现用户配置解析优先级：精确业务类型、`default`、平台配置链路；`fallbackToPlatform=true` 时忽略用户配置。
- 简历诊断、JD 匹配、AI 润色、图片识别统一优先使用 `resume` 配置；面试与上下文摘要使用 `interview` 配置。
- 命中用户自定义 AI 时跳过平台额度，改扣 `user_ai_daily_usage`；调用失败回滚自定义次数。
- 自定义 AI 失败不自动回退，返回 4090/4091 等错误码；只有请求显式携带 `fallbackToPlatform=true` 才走平台 AI 并消耗平台额度。
- 简历诊断异步任务已锁定创建时 AI 计费来源和回退意图，避免执行期间配置变化导致扣费来源漂移。
- 连通测试已接入关键端点限流：每用户每分钟 5 次。

## 本轮完成状态

- `mvn.cmd -q "-Dtest=UserAiConfigResolverImplTest,UserAiUsageLimitServiceImplTest,CriticalEndpointRateLimitFilterTest,SchemaConsistencyTest,ResumeDiagnosisControllerTest,AdminControllerTest,InterviewControllerTest,AdminAiEngineConnectivityControllerTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。
- 前端设置页、失败回退 UI 和管理端入口见 `frontend/tasks/stage.md`。

## 尚未开始的功能

- 未实现 TTS 调用或 TTS 配置 UI。
- 未实现自定义 AI 使用统计图表、按功能分类明细或管理员查看所有用户使用明细。
- 未扩展非 OpenAI 兼容协议。

## 停止，不继续下一个功能

本轮仅完成用户自定义 OpenAI 兼容 Provider、独立每日用量和手动平台回退，不继续推进 TTS、统计图表、更多协议适配或其它 AI 能力。

# 移除离线 STT 后端兜底路径验证（2026-06-01）

## 已完成且已验证的功能

- 前端已删除 sherpa-onnx 离线 STT 下载、缓存、Worker 和 WASM 启动链路，后端不再需要为其提供模型下载兜底。
- 当前后端源码扫描未发现 `OfflineStt`、`offline-stt`、`sherpa` 或 `voice-models` 生产链路。
- `SecurityConfigTest` 已补充回归断言，确认安全配置源码不再公开 `/api/offline-stt`，也不存在 `supportsPublicOfflineSttModelPath`。

## 本轮完成状态

- `mvn.cmd test "-Dtest=SecurityConfigTest"` 通过，5 个用例，0 失败，0 错误。
- 前端删除范围、测试和构建验证见 `frontend/tasks/stage.md`。

## 尚未开始的功能

- 未新增 Deepgram、云端 STT、后端语音识别、模型代理或其它语音服务替代方案。

## 停止，不继续下一个功能

本轮仅锁定旧离线 STT 后端公开路径不再存在，等待验收，不继续推进下一项语音能力。

# 社区前台管理员下架与轻量风控标记（2026-05-31）

## 已完成且已验证的功能

- 纯文本命中疑似风险词时仍自动 `approved` 公开展示，但写入 `review_reason = 疑似风险词命中，已自动放行`，便于后台筛查。
- 严重违规文本仍在入库前直接拒绝；带图片帖子和评论继续进入 `pending`，等待人工审核。
- 用户端新增管理员下架帖子接口 `PUT /api/community/posts/{postId}/admin-hide`，后端强校验当前用户 `role == 9`。
- 管理员下架不物理删除，统一写入 `review_status = hidden`、下架原因、管理员 ID 和审核时间。
- 下架成功后复用 `NotificationService.createNotification(...)` 给帖子作者发送站内系统通知，说明下架原因。

## 本轮完成状态

- `mvn.cmd -q "-Dtest=CommunityTextModerationServiceTest,CommunityServiceModerationTest,CommunityServicePostQueryDeleteTest,NotificationServiceTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。
- 前端管理员下架入口、页面测试和构建验证见 `frontend/tasks/stage.md`。

## 尚未开始的功能

- 未扩展评论下架、批量下架、举报系统、图片 AI 审核、用户信誉分或敏感词后台配置。

## 停止，不继续下一个功能

本轮仅完成帖子级前台管理员下架和轻量风险标记，等待验收，不继续下一个功能。

# 管理端监控总览业务链路补齐（2026-05-31）

## 已完成且已验证的功能

- `GET /api/admin/monitor/overview` 在保留原字段基础上新增业务链路监控字段：已完成简历任务、今日简历润色、今日 JD 匹配、今日社区发帖、今日反馈、今日订单、反馈待处理/处理中、社区帖子/评论待审数与社区待审总数。
- 后端仍为应用层业务表统计版，未接入 RabbitMQ、Redis、JVM、数据库连接池或外部 AI 连通性深度监控。
- 新增统计继续使用 `dashboardExecutor` 并行查询，并复用 `admin:monitorOverview` 2 分钟缓存。
- 社区待审总数统一由后端按 `pendingCommunityPostCount + pendingCommunityCommentCount` 计算，前端不重复推导。
- 已补充 `AdminDashboardServiceImplTest`，覆盖新增字段回填、今日半开区间统计、社区待审总数和缓存同步加载要求。

## 本轮完成状态

- `mvn.cmd -q "-Dtest=AdminDashboardServiceImplTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。
- 前端分组展示、页面测试和构建验证见 `frontend/tasks/stage.md`。

## 尚未开始的功能

- 未接入 RabbitMQ 队列深度、Redis 健康、JVM/线程池、数据库连接池、外部 AI 连通性、磁盘/上传目录容量等基础设施深度指标。

## 停止，不继续下一个功能

本轮仅完成管理端监控总览业务链路补齐，不继续推进基础设施深度监控或监控详情页。

# 社区轻量化审核后端规则调整（2026-05-31）

## 已完成且已验证的功能

- 社区自动审核规则已调整为“图片必须待审、纯文本默认放行”。
- 严重违规文本仍在入库前直接拒绝，不进入公开区，也不进入人工审核队列。
- 未命中严重违规词的纯文本帖子和评论统一自动 `approved`，不再因为疑似词进入 `pending`。
- 带图片帖子和带图片评论继续进入 `pending`，等待管理员审核。
- 评论计数规则保持不变：只有审核通过评论才计入公开评论数。

## 本轮完成状态

- `mvn.cmd -q "-Dtest=CommunityTextModerationServiceTest,CommunityServiceModerationTest" test` 通过。
- `mvn.cmd -q "-Dtest=CommunityTextModerationServiceTest,CommunityServiceModerationTest,AdminCommunityModerationServiceTest" test` 通过。
- `mvn.cmd -q "-Dtest=CommunityService*Test,AdminCommunityModerationServiceTest,CriticalEndpointRateLimitFilterTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。

## 尚未开始的功能

- 未接入云审核、图片 AI 鉴黄、举报中心、敏感词配置后台、用户信誉表、批量审核或多级审核流程。

## 停止，不继续下一个功能

当前仅完成社区轻量化审核 V2 后端规则调整，等待验收，不继续下一个功能。

# 管理端版本日志筛选查看（2026-05-31）

## 已完成且已验证的功能

- `GET /api/admin/version-logs` 已新增可选筛选参数：`type`、`status`、`keyword`。
- 后端使用参数化 `LambdaQueryWrapper` 组合版本类型、发布状态和版本号/标题/内容关键词条件，保持创建时间倒序和原分页返回结构不变。
- 已对 `type`、`status` 做白名单校验，非法筛选值会直接返回错误，不进入数据库查询。
- 管理端版本日志筛选分页已收敛页码最小值和单页最大 100 条，避免异常大分页请求直接压到数据库。
- `sys_version_log` 已新增复合索引 `idx_version_log_filter_time(status, type, create_time)`，匹配状态、类型筛选和创建时间倒序列表查询。
- 已新增可重复执行迁移 `TASK_ADMIN_VERSION_LOG_FILTER_INDEXES.sql`，并同步 `db/` 与 `server/db/` 下的 schema 和迁移脚本。
- 已扩展 `AdminVersionLogControllerTest` 和 `SchemaConsistencyTest`，锁定版本日志筛选查询、非法参数拒绝、分页保护和双目录 SQL 一致性。

## 本轮完成状态

- `mvn.cmd -q "-Dtest=AdminVersionLogControllerTest,SchemaConsistencyTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。
- 前端筛选栏、页面测试和构建验证见 `frontend/tasks/stage.md`。

## 监控总览覆盖口径确认

- 当前 `/api/admin/monitor/overview` 仍是应用层业务表统计版，覆盖待处理/处理中/失败简历诊断任务、活跃面试会话、当日面试会话、当日简历诊断。
- 当前未覆盖 RabbitMQ 队列深度、Redis 健康、JVM/线程池、数据库连接池、外部 AI 连通性、磁盘/上传目录容量等基础设施深度指标。
- 本轮未扩展监控总览，避免超出“版本日志筛选”当前功能范围；如要补齐全链路监控，建议拆成独立任务。

## 停止，不继续下一个功能

本轮仅完成管理端版本日志筛选查看后端支持和性能索引补强，不继续扩展公开版本日志页筛选、时间范围筛选、导出、详情页或监控深度指标。

# 后端阶段记录

## 社区自动审核分流后端治理（2026-05-31）
## 已完成且已验证的功能

- 社区文本审核规则已从 `CommunityService` 抽到 `CommunityTextModerationService`。
- 严重违规文本在入库前拒绝；疑似风险文本进入 `pending`；带图片内容继续进入 `pending`；低风险纯文本自动 `approved`。
- 创建帖子和评论接口已返回 `{ id, reviewStatus }`，前端可按真实状态提示用户。
- 评论数仍只统计审核通过评论，待审评论不会计入公开评论数。
- 为恢复 Maven testCompile，已最小修复 `AdminVersionLogControllerTest` 与当前版本日志控制器签名不一致的问题，未改生产接口行为。

## 本轮完成状态

- `mvn.cmd -q "-Dtest=CommunityTextModerationServiceTest,CommunityServiceModerationTest,AdminCommunityModerationServiceTest" test` 通过。
- `mvn.cmd -q "-Dtest=CommunityService*Test,AdminCommunityModerationServiceTest,CriticalEndpointRateLimitFilterTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。

## 尚未开始的功能

- 未接入云审核、图片 AI 鉴黄、举报中心、敏感词配置后台、批量审核或多级审核流程。

## 停止，不继续下一个功能

当前仅完成社区自动审核分流一期，等待验收，不继续下一个功能。

## 社区审核迁移脚本 UTF-8 导入修复（2026-05-31）

## 已完成且已验证的功能

- `TASK_61_COMMUNITY_CONTENT_MODERATION.sql` 已在 `db/` 与 `server/db/` 两份迁移脚本中补充 `SET NAMES utf8mb4;`。
- `add_column_if_missing` 与 `add_index_if_missing` 的表名、列名、索引名和动态 DDL 参数已显式声明 `CHARACTER SET utf8mb4`，避免目标库默认字符集不是 UTF-8 时，中文列注释在存储过程参数传入阶段触发 `ERROR 1366 Incorrect string value`。
- 已补充 `SchemaConsistencyTest` 回归测试，锁定 TASK_61 双目录 SQL 一致性和 UTF-8 迁移安全要求。

## 本轮完成状态

- `mvn.cmd -q "-Dtest=SchemaConsistencyTest#shouldKeepCommunityModerationMigrationUtf8SafeAndInSync" test` 通过。

## 尚未开始的功能

- 未继续扩展云审核、图片 AI 鉴黄、举报中心、批量审核或多级审核流程。

## 停止，不继续下一个功能

当前仅修复社区审核迁移脚本在 MySQL 导入时的字符集兼容问题，等待验收，不继续下一个功能。

## 社区内容审核后端治理（2026-05-31）

## 已完成且已验证的功能

- 社区帖子和评论已新增审核状态字段：`pending`、`approved`、`rejected`、`hidden`。
- 新发布帖子和评论默认进入 `pending`，不再直接公开展示。
- 公共社区列表、详情、评论和回复读取路径只展示 `approved` 内容；作者自己的帖子列表保留审核状态和原因回显数据。
- 新增最小规则拦截，明确政治敏感、色情、辱骂、诈骗、广告等高风险文本会在入库前被拒绝。
- 新增管理端社区审核接口，支持帖子/评论分页查询、通过、拒绝、隐藏。
- 评论计数调整为评论通过审核后再增加，已通过评论改为拒绝或隐藏时同步回退。
- 社区发帖、评论、图片上传已接入关键端点限流。
- `db/` 与 `server/db/` 下 schema 和 `TASK_61_COMMUNITY_CONTENT_MODERATION.sql` 迁移脚本已同步，历史内容默认 `approved`。
- 已补充审核流程、管理端审核服务、公共可见性、限流和旧社区查询适配测试。

## 本轮完成状态

- `mvn.cmd -q "-Dtest=CommunityServiceModerationTest,CriticalEndpointRateLimitFilterTest" test` 通过。
- `mvn.cmd -q "-Dtest=AdminCommunityModerationServiceTest,CommunityServiceModerationTest,CriticalEndpointRateLimitFilterTest" test` 通过。
- `mvn.cmd -q "-Dtest=CommunityService*Test,CriticalEndpointRateLimitFilterTest" test` 通过。
- `mvn.cmd -q "-Dtest=SchemaConsistencyTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。
- 前端审核页面、发帖提示和个人动态状态回显见 `frontend/tasks/stage.md`。

## 尚未开始的功能

- 未接入云审核、图片 AI 鉴黄、举报中心、敏感词配置后台、批量审核或多级审核流程。

## 停止，不继续下一个功能

本轮只完成社区内容审核的最小治理闭环，等待验收，不继续扩展下一项社区风控能力。

## AI 基础地址 DNS 启动修复（2026-05-31）

## 已完成且已验证的功能

- `ResumeAiServiceImpl` 构造器已改为启动期轻量校验 `app.ai.base-url`，避免外部 DNS 暂时不可解析时抛出 `基础地址域名无法解析` 并阻断后端启动。
- `InterviewAiServiceImpl` 构造器已同步采用启动期轻量校验，避免面试 AI 基础地址遇到同类 DNS 抖动后继续触发启动失败。
- `PublicHttpsUrlValidator.validate()` 的完整 DNS 校验保持不变，运行时真正发起 AI 出网调用前仍会拦截解析到本机、内网或云元数据地址的域名，SSRF 防护边界不放松。
- 新增 `validateWithoutDnsResolution()` 作为启动期专用入口，只校验 HTTPS、用户信息、本机/内网/云元数据字面量地址，不依赖网络。
- 已补充 `PublicHttpsUrlValidatorTest`、`ResumeAiServiceImplTest`、`InterviewAiServiceImplTest` 回归测试，覆盖不可解析公网域名不应导致 Bean 构造失败。

## 本轮完成状态

- `mvn -q "-Dtest=PublicHttpsUrlValidatorTest,ResumeAiServiceImplTest,InterviewAiServiceImplTest" test` 通过。
- `mvn -q -DskipTests compile` 通过。
- `mvn -q test` 通过。
- 使用 `--app.ai.base-url=https://startup-only.invalid/v1 --app.interview.base-url=https://startup-only.invalid/v1` 启动验证时，服务已到达 `Started ServerApplication`；命令超时仅因为 Spring Boot 为长驻进程。

## 尚未开始的功能

- 未继续扩展 AI 配置管理、连通测试、模型路由、生产密钥配置或前端页面提示。

## 停止，不继续下一个功能

当前仅修复 AI 基础地址 DNS 解析失败导致的后端启动中断问题，等待验收，不继续下一个功能。

## 管理端通知公告筛选查看（2026-05-30）

## 已完成且已验证的功能

- `GET /api/admin/notifications` 已新增可选筛选参数：`type`、`status`、`targetType`、`keyword`。
- 后端使用参数化 `LambdaQueryWrapper` 组合公告类型、发布状态、目标用户和标题/内容关键词条件，保持创建时间倒序和原分页返回结构不变。
- 已对 `type`、`status`、`targetType` 做白名单校验，非法筛选值会直接返回错误，不进入数据库查询。
- 已补充 `AdminNotificationControllerTest` 回归测试，覆盖筛选条件拼装、非法状态拒绝和原分页返回。
- 管理端通知公告筛选分页已收敛页码最小值和单页最大 100 条，避免异常大分页请求直接压到数据库。
- `sys_admin_notification` 已新增复合索引 `idx_admin_notification_filter_time(target_type, status, type, create_time)`，匹配目标用户、状态、类型筛选和创建时间倒序列表查询。
- 已新增可重复执行迁移 `TASK_ADMIN_NOTIFICATION_FILTER_INDEXES.sql`，并同步 `db/` 与 `server/db/` 下的 schema 和迁移脚本。
- 已扩展 `SchemaConsistencyTest`，锁定通知公告筛选索引迁移和双目录 SQL 一致性。

## 本轮完成状态

- `mvn.cmd -q "-Dtest=AdminNotificationControllerTest,SchemaConsistencyTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。
- 前端筛选栏、API 封装和构建验证见 `frontend/tasks/stage.md`。

## 停止，不继续下一个功能

本轮仅完成管理端通知公告筛选查看后端支持和性能索引补强，不继续扩展公告编辑、导出、时间范围筛选或其它管理端功能。

## 面试流式消息限流修复（2026-05-30）

## 已完成且已验证的功能

- `interview_stream` 独立限流策略已从每用户 10 次 / 10 分钟调整为 60 次 / 10 分钟，避免正常语音面试超过 10 轮后被误拦截。
- 流式接口仍保留独立策略，优先级高于通用面试写操作，异常刷接口时第 61 次仍返回 429。
- 已补充 `CriticalEndpointRateLimitFilterTest` 回归测试，覆盖 60 轮语音面试流式发送放行和第 61 次拦截。
- 前端流式消息收到 429 时会提示“发送太频繁，请稍后继续。10 分钟内最多 60 轮对话。”，并保留当前语音通话状态。
- 已同步 `docs/api/TASK_05_INTERVIEW_API.md` 与 `server/README.md` 中的限流说明。

## 本轮完成状态

- `mvn.cmd -q "-Dtest=CriticalEndpointRateLimitFilterTest" test` 通过。
- `npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。
- `npm.cmd run build` 通过。

## 尚未开始的功能

- 未继续扩展动态限流配置、会员分层限流或全局限流重构。

## 停止，不继续下一功能

当前仅修复语音面试正常轮数被流式限流误杀以及 429 提示/状态处理问题，等待验收，不继续下一功能。

## 已完成且已验证的功能

- Redis 缓存序列化告警修复：
  - `admin:dashboardDistribution` 缓存返回对象 `BusinessDistributionResponse` 已实现 `Serializable`，修复 JDK 序列化器写入 Redis 时的 `Cannot serialize` 告警。
  - 同类管理端总览缓存返回对象 `DashboardOverviewResponse` 已同步实现 `Serializable`，避免 `admin:dashboardOverview` 后续出现同类缓存写入失败。
  - 已补充 Redis 序列化往返回归测试，覆盖管理端 dashboard 总览与业务分布 DTO。
  - 缓存区域、缓存 key、TTL、接口返回结构和数据库结构均保持不变。
- 会员套餐权益乱码修复：
  - 已确认 `/api/membership/plans` 只有 `benefits` 字段乱码，接口层只是反序列化数据库 JSON，不产生额外转码。
  - `db/migrations/fix_benefits_encoding_hex.sql` 已改为真正的 `CONVERT(0x... USING utf8mb4)` 写入，避免 SQL 客户端编码导致中文再次写坏。
  - `db/` 与 `server/db/` 下 TASK-57 会员迁移脚本已同步改为 hex 写入权益 JSON。
  - `server/db/schema.sql` 的会员套餐种子数据已同步改为 hex 写入权益 JSON。
  - 新增脚本格式回归测试，锁定修复脚本不能再使用 `\x` 字符串或 `benefits = JSON_ARRAY('中文...')` 写法。
  - 当前运行数据库需要执行 `fix_benefits_encoding_hex.sql` 后刷新缓存或重启后端，接口返回才会恢复正常中文。

- 后端性能优化第一轮小步止血：
  - 管理后台趋势查询已改为按日期聚合查询，避免逐日 N+1 count。
  - 面试摘要缓存已增加 2 小时过期清理，保留主动清理行为。
  - PDF 导出接口已接入关键端点限流，限制为 5 次 / 10 分钟。

- 后端性能优化第二轮运行时保护：
  - Redis Lettuce 连接池已配置 `max-wait: 3000ms`，避免连接池耗尽时无限等待。
  - 面试 AI 流式 WebClient 已增加 180 秒响应超时，保留原有流式与降级链路。
  - 面试 AI 流式高频统计报告已从 INFO 降为 DEBUG，减少生产日志刷屏风险。

- 后端性能优化第三轮缓存策略小步优化：
  - `admin:dashboardTrends` 缓存区已在 Redis 缓存管理器中显式注册。
  - 管理后台趋势缓存 TTL 已设置为 10 分钟，避免继续使用默认 5 分钟策略。

- 后端性能优化第四轮本地兜底清理：
  - Redis 故障兜底的本地登录失败记录 Map 已增加定时过期清理。
  - 过期本地登录失败记录会被清理，未过期限流记录保持不变。

- 后端性能优化第五轮静态资源缓存头：
  - `/uploads/community/**` 社区公开图片资源已增加 1 天 `Cache-Control` 响应头。
  - 静态资源公开范围保持不变，未扩大到简历、报告等敏感上传目录。

- 后端性能优化第六轮直连处理并发保护：
  - `DirectProcessRouter` 直连处理容量检查已改为原子预占，避免并发请求同时通过阈值检查后短时超过直连处理上限。
  - 新增 `submitDirectIfCapacity` 作为容量检查与提交合并的安全入口。
  - 保留旧的 `canProcessDirectly()` + `submitDirect()` 调用链兼容性，本轮未改诊断任务主流程。

- 后端性能优化第七轮成长中心查询字段收窄：
  - `GrowthServiceImpl.getGrowthOverview()` 相关简历诊断趋势查询已显式选择 `create_time`、`diagnosis_result`，避免加载 `resume_text` 等大字段。
  - 最近 JD 匹配查询已显式选择 `match_score`、`analysis_result`、`create_time`，避免加载简历文本和 JD 文本快照。
  - 最近 AI 润色查询已显式选择 `source_type`、`modification_notes`、`create_time`，避免加载润色全文、结构化文档 JSON 和纯文本快照。
  - 接口返回结构、缓存区域、数据库结构均保持不变。

- 后端性能优化第八轮 MyBatis-Plus 拦截器死代码清理：
  - 已确认当前源码没有实体使用 `@Version` 注解。
  - `MybatisPlusConfig` 已移除未使用的 `OptimisticLockerInnerInterceptor` 注册。
  - 分页拦截器和全表更新/删除保护拦截器保持不变。
  - 未新增慢查询拦截器，未调整业务查询和数据库结构。

- 后端性能优化后续全量执行：
  - 生产配置已补齐 HikariCP、Tomcat、Redis 小机器保守参数，并将诊断直连阈值设置为 1。
  - RabbitMQ 已增加发布端重试、诊断消息单消息 TTL 和只记录失败消息的 DLQ 消费者。
  - 上传简历源文件已增加默认 30 天终态记录清理、上传前磁盘余量保护和上传目录统计日志。
  - 简历诊断、岗位匹配、简历润色、面试会话、岗位定向面试等实体大字段已默认不全列加载，必要链路已显式补回字段。
  - `ResumeTaskRepository`、`InterviewSessionRepository`、`InterviewMessageRepository` 已迁移到 MyBatis-Plus Mapper，JPA 依赖与相关注解已移除。
  - `db/schema.sql` 与 `server/db/schema.sql` 已同步，已新增 TASK_58 保守索引迁移脚本，未删除线上既有索引。
  - `server/CLAUDE.md` Java 版本说明已修正为 Java 17。

- RabbitMQ 队列 TTL 启动修复：
  - 已移除 `queue.resume.diagnosis` 队列声明上的 `x-message-ttl` 参数，避免既有队列无 TTL 参数时 RabbitMQ 406 `PRECONDITION_FAILED`。
  - 简历诊断消息 TTL 已迁移为发送时设置单消息 `expiration=3600000`，不需要删除或重建已有队列。
  - 死信交换机、死信 routing key、DLQ 只记录消费者保持不变。

- 社区帖子标题与面试报告链接分享修复：
  - 社区帖子已补齐标题字段，发帖时标题必填，列表、详情和个人动态接口均返回标题。
  - 面试报告分享到社区时只保存报告标题、简短说明和 `sharedInterviewSessionId`，不再把完整报告正文作为社区文字内容发布。
  - 报告分享帖创建前校验面试会话归属，用户只能分享自己的面试报告。
  - 跨用户查看社区分享报告时，必须存在报告所有者发布的未删除社区分享帖；响应只返回报告页必要字段，不返回聊天记录、复盘轮次和岗位上下文。
  - `db/` 与 `server/db/` 下 schema、TASK_56 迁移脚本和社区 seed 脚本已同步标题字段、分享会话字段和索引。
  - 已新增 `TASK_59_COMMUNITY_POST_TITLE_AND_REPORT_LINK_INCREMENTAL.sql`，用于已经执行过旧版 TASK_56 的数据库补齐本轮字段和索引；`alter_v1.4_add_community_tables.sql` 不作为已有表补字段脚本。

## 本轮完成状态

已完成并通过后端验证：

- 社区帖子标题与面试报告链接分享修复已完成，范围包括标题字段与校验、报告分享会话 ID、报告分享授权边界、SQL 双副本同步和社区 seed 脚本同步。
- 针对已执行过旧版 TASK_56 的目标库，补充了专用增量迁移 `TASK_59_COMMUNITY_POST_TITLE_AND_REPORT_LINK_INCREMENTAL.sql`。
- 个人动态中心默认加载 5 条属于前端调整，前端阶段记录见 `frontend/tasks/stage.md`。
- `mvn.cmd test "-Dtest=CommunityServiceValidationTest,CommunityServicePostQueryDeleteTest,InterviewServiceTest,CommunityServiceLikeFavoriteTest,CommunityServiceInteractionTest,CommunityServiceReceivedInteractionsEmptyTest"` 通过，结果为 79 个测试，0 失败，0 错误。
- `mvn compile` 通过。
- 前端相关测试与构建已通过，见 `frontend/tasks/TASK_COMMUNITY_POST_TITLE_AND_REPORT_LINK_FRONTEND.md`。

## 尚未开始的功能

- 用户本轮指定的社区标题、个人动态加载数量、面试报告链接分享范围已完成。
- 未继续新增报告下载、富文本编辑、公开报告列表或其它社区扩展能力。

## 停止，不继续下一功能

当前社区帖子标题与面试报告链接分享修复已停止，等待验收，不继续下一功能。
# 全量性能优化缓存与索引修复（2026-05-29）

## 已完成且已验证的功能

- Redis 缓存区 TTL 已补齐：`user:interviewRadar` 5 分钟、`config:membershipPlan` 30 分钟、`interview:jobTarget` 10 分钟。
- `public:stats` 已增加短锁、stale 缓存和短重试，降低公共统计缓存 miss 时的数据库击穿风险。
- 管理端 dashboard 统计缓存已对同 key 加入同步加载，避免相同日期范围并发请求重复聚合数据库。
- `resume:task` 已从 `allEntries = true` 粗粒度驱逐改为 `taskId::userId` 精确驱逐，批量恢复路径保留短 TTL 兜底。
- 已新增 TASK_60 复合索引迁移脚本，并同步 `db/` 与 `server/db/` 两份 schema。
- 已补充 Redis TTL、防击穿、dashboard 同步加载、任务缓存精确驱逐和 schema 一致性测试。

## 本轮完成状态

- `mvn.cmd test "-Dtest=RedisConfigTest,AdminDashboardServiceImplTest,ResumeDiagnosisTaskServiceImplTest,PublicStatsServiceImplTest,SchemaConsistencyTest"` 通过：28 个测试，0 失败，0 错误。
- `mvn.cmd test "-Dtest=RuntimeProtectionConfigTest,UserQuotaServiceImplAtomicDeductionTest"` 通过：11 个测试，0 失败，0 错误。
- `mvn.cmd test` 通过：574 个测试，0 失败，0 错误。
- 当前工作区没有可用数据库连接，未执行真实 `EXPLAIN`；新增索引基于现有查询形态、分页条件和排序字段补齐，上线前仍需在目标 MySQL 环境结合 `EXPLAIN` 或慢查询日志确认命中情况。

## 尚未开始的功能

- 未继续推进读写分离、预聚合表、搜索服务、CDN、Service Worker、虚拟列表库或新的接口协议。

## 停止，不继续下一个功能

本轮全量性能优化中的后端缓存、防击穿、精确失效和数据库索引部分已完成并验证，等待验收，不继续扩大到下一阶段架构优化。
## 离线 STT 模型同源兜底源后端修复（2026-05-31）
- 当前阶段：已为前端离线 STT 下载补充后端同源兜底源，解决浏览器直连 ModelScope 被 CORS 拦截、HuggingFace 在当前网络下超时后用户无法下载模型包的问题。
- 已完成内容：新增 `OfflineSttModelProxyController` 和 `OfflineSttModelProxyService`，提供 `GET /api/offline-stt/models/{fileName}` 流式下载；仅允许四个 sherpa-onnx 白名单文件名；服务端固定尝试 ModelScope 与 HuggingFace 官方源；`SecurityConfig` 放行该只读模型下载路径。
- 安全边界：接口不接收任意 URL，不允许路径穿越，不落盘模型文件；后端只作为兜底流式通道，浏览器仍将模型写入 Cache API。
- 验证结果：`mvn.cmd test "-Dtest=OfflineSttModelProxyServiceTest,OfflineSttModelProxyControllerTest,SecurityConfigTest"` 通过，11 个用例。
- 关联任务文件：`tasks/TASK_61_OFFLINE_STT_MODEL_PROXY_BACKEND.md`、`frontend/tasks/TASK_61_OFFLINE_STT_REMOTE_SOURCE_DOWNLOAD_PLAN_FRONTEND.md`。
- 停止说明：本轮只处理离线 STT 模型源兜底，不继续推进云存储托管、后端语音识别或其它语音能力。
# 社区服务构造器启动修复（2026-05-31）

## 已完成且已验证的功能

- `CommunityService` 已改为显式 8 参数生产注入构造器，并使用 `@Autowired` 标记 Spring 注入入口，避免多构造器场景下回退默认构造器导致后端启动失败。
- 保留 7 参数测试兼容构造器，现有社区服务单元测试无需批量改造，业务行为不变。
- 新增 `CommunityServiceConstructorInjectionTest`，锁定生产注入构造器必须包含 `CommunityTextModerationService` 完整依赖。

## 本轮完成状态

- `mvn.cmd -q "-Dtest=CommunityServiceConstructorInjectionTest" test` 通过。
- `mvn.cmd -q "-Dtest=CommunityService*Test,CommunityTextModerationServiceTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。
- `mvn.cmd -q "-Dtest=ServerApplicationTests" test` 通过，Spring 上下文已能启动。

## 尚未开始的功能

- 本轮未扩展社区新功能、审核策略、数据库结构或前端页面。

## 停止，不继续下一个功能

当前仅修复 `communityService` 构造器注入导致的后端启动失败，等待验收，不继续推进下一项功能。
# 社区前台管理员下架与轻量风控标记后端（2026-05-31）
## 已完成且已验证的功能

- 纯文本疑似违规词不再进入人工审核，仍自动 `approved`，但 `review_reason` 写入“疑似风险词命中，已自动放行”，方便后台筛查。
- 严重违规文本仍在入库前直接拒绝；带图片内容仍进入 `pending` 人工审核。
- 用户端新增管理员下架帖子接口 `PUT /api/community/posts/{postId}/admin-hide`，服务层强校验当前用户 `role === 9`。
- 管理员下架不物理删除帖子，而是写入 `review_status = hidden`、下架原因、操作管理员和操作时间。
- 下架成功后复用站内通知服务通知帖子作者，通知内容包含下架原因。

## 本轮完成状态

- 后端定向测试已先红后绿：`mvn.cmd -q "-Dtest=CommunityTextModerationServiceTest,CommunityServiceModerationTest,CommunityServicePostQueryDeleteTest,CommunityServiceConstructorInjectionTest" test` 通过。
- 最终验证结果见本轮交付说明。

## 尚未开始的功能

- 未扩展评论前台下架、举报系统、图片 AI 审核、用户信誉分、敏感词后台配置或批量处置。

## 停止，不继续下一个功能

当前仅完成帖子级前台管理员下架和轻量风险标记，等待验收，不继续推进下一项社区治理能力。
# 社区标题与下架原因长文本防护（2026-05-31）
## 已完成且已验证的功能

- 社区帖子标题继续沿用后端 `MAX_TITLE_LENGTH = 120` 限制，本轮补齐下架通知中的标题摘要截断，避免超长标题撑开站内通知列表。
- 管理员前台下架原因新增统一后端限制 `MAX_ADMIN_HIDE_REASON_LENGTH = 200`，服务层直接拒绝超长原因，防止绕过前端或 Controller 校验后写入超长 `review_reason`。
- `AdminHidePostRequest` 与 `CommunityService.adminHidePost` 已统一长度边界，前端下架原因校验与后端限制保持一致。
- 本轮不新增 SQL，不修改数据库结构，继续复用已有社区审核字段和站内通知表。

## 本轮完成状态

- `mvn.cmd -q "-Dtest=CommunityServicePostQueryDeleteTest" test` 通过。
- `mvn.cmd -q "-Dtest=CommunityServicePostQueryDeleteTest,CommunityTextModerationServiceTest,CommunityServiceModerationTest,NotificationServiceTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。
- 前端标题折叠、完整标题弹窗、下架原因长度校验和构建结果见 `frontend/tasks/stage.md`。

## 尚未开始的功能

- 未扩展评论下架、通知详情页重构、举报系统、图片 AI 审核、敏感词后台配置或用户信誉分。

## 停止，不继续下一个功能

本轮仅完成社区标题和管理员下架原因的长文本防护，等待验收，不继续推进下一项社区风控能力。
# 社区评论下架与账号封禁后端治理（2026-05-31）
## 已完成且已验证的功能

- 新增用户端管理员评论/回复下架接口 `PUT /api/community/posts/{postId}/comments/{commentId}/admin-hide`，后端强校验 `role == 9`。
- 评论下架采用隐藏语义，写入 `review_status = hidden`、下架原因、操作管理员和操作时间；顶级评论下架会同步隐藏直属回复并按公开数量回退评论数。
- 新增账号封禁/解封接口，支持单个和批量，封禁时写入 `status = 0`、封禁原因、到期时间、操作人和操作时间。
- `sys_user` 已补充 `ban_reason`、`banned_until`、`banned_by`、`banned_time`，两份 schema 和 migration 已同步。
- 登录和 JWT 鉴权会拦截封禁账号；临时封禁到期后自动解封并允许继续登录/鉴权。

## 本轮完成状态

- `mvn.cmd -q "-Dtest=CommunityServicePostQueryDeleteTest,CommunityServiceModerationTest,AdminControllerTest,JwtAuthenticationFilterTest,AuthServiceImplTest,SchemaConsistencyTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。
- 前端评论下架、封禁弹窗、管理端用户页和构建结果见 `frontend/tasks/stage.md`。

## 尚未开始的功能

- 未接入举报系统、社区禁言、用户信誉分、图片 AI 审核或自动批量清理历史内容。

## 停止，不继续下一个功能

本轮只完成评论下架与账号封禁机制，等待验收，不继续推进下一项社区治理能力。
# 模拟面试平台 AI 兜底计费修复后端（2026-06-02）
## 已完成且已验证的功能

- `interview_session` 已新增 `ai_billing_source`，用于锁定模拟面试会话的 AI 计费来源。
- 自定义 AI 面试会话首次显式 `fallbackToPlatform=true` 时，会通过 CAS 原子标记为 `platform_fallback`，只有标记成功才扣 1 次平台模拟面试额度。
- 同一个 session 后续继续使用平台 AI 不再重复扣平台面试额度，避免重复点击或并发请求卡漏洞。
- 非流式消息和 SSE 流式消息已共用同一套平台兜底计费方法。
- 已同步 `db/` 与 `server/db/` 两套 schema 和 migration。

## 本轮完成状态

- RED 验证：新增 `InterviewServiceTest` 后，因缺少会话计费来源字段、`platform_fallback` 常量和 CAS Mapper 方法而编译失败。
- GREEN 验证：`mvn.cmd -q "-Dtest=InterviewServiceTest#shouldMarkCustomAiBillingSourceWhenCreatingSessionWithCustomAi+shouldDeductInterviewQuotaOnceWhenCustomAiSessionFallsBackToPlatform+shouldNotDeductInterviewQuotaAgainWhenPlatformFallbackAlreadyMarked,InterviewControllerTest,SchemaConsistencyTest" test` 通过。
- 关联任务文件：`tasks/TASK_74_INTERVIEW_PLATFORM_FALLBACK_BILLING_BACKEND.md`。

## 尚未开始的功能

- 未实现平台兜底失败后的面试额度退款能力。
- 未调整 Offer 辅助、自定义 AI 使用统计图表或其它 AI 链路计费规则。
- 未修改前端页面文案或交互。

## 停止，不继续下一个功能

本轮仅修复模拟面试自定义 AI 手动平台兜底的额度漏洞，等待验收，不继续推进其它计费链路改造。
# 管理端自定义 AI 用量趋势默认展开前端联动（2026-06-03）
## 已完成且已验证的功能

- 管理端 AI 引擎配置页默认仍停留在“引擎配置”分区，自定义 AI 用量统计不再影响配置页主布局。
- 切换到“自定义 AI 用量”分区后，用户自定义 AI 按日趋势图默认展开；管理员仍可手动收起。
- 用户明细分页保持 `pageSize=5`，继续按 `page/pageSize` 请求分页数据，不全量接收全部用户明细。
- 本轮无后端、数据库、计费、回滚、Provider 配置解析或权限规则改动。

## 本轮完成状态

- 前端 RED 验证：旧实现下 `npm.cmd test -- --run src/__tests__/views/AdminAiEngineView.test.js` 失败，复现切换后趋势图仍默认折叠。
- 前端 GREEN 验证：`npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，2 个测试文件 / 14 个用例。
- 前端构建验证：`npm.cmd run build` 通过。
- 关联任务文件：`tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`、`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

## 尚未开始的功能

- 未在管理端首页新增趋势表或趋势图。
- 未实现周聚合、月聚合、独立统计页面、TTS 调用或 TTS 配置 UI。
- 未修改自定义 AI 扣费、失败回滚、平台手动 fallback、Provider 配置解析或加密存储规则。

## 停止，不继续下一个功能
本轮仅完成自定义 AI 用量分区内趋势默认展开和用户明细每页 5 条确认，等待验收，不继续推进其它 AI Provider 能力。
## 模型列表获取错误提示规整后端修复（2026-06-04）

### 已完成且已验证的功能

- `AiModelDiscoveryServiceImpl` 已停止把上游 `/models` HTTP 错误响应体拼接进 `errorMessage`，避免 HTML 网关页、openresty 等原始内容展示给用户。
- 404 状态现在返回可行动提示：检查 API 基础地址是否需要 `/v1`、服务商是否支持 `/models`，并说明可继续手动输入模型名。
- 401/403、429、5xx 和其它 HTTP 状态也按状态码映射为用户可理解提示，不回显 API Key 或上游原始响应体。
- 新增后端回归测试覆盖 404 HTML 响应，确认 `errorMessage` 不包含 `<html>` 和 `openresty`。

### 本轮完成状态

- RED 验证：`mvn.cmd -q "-Dtest=AiModelDiscoveryServiceImplTest#shouldHideRawHtmlBodyWhenProviderModelEndpointNotFound" test` 在旧实现下失败。
- GREEN 验证：同一 RED 用例修复后通过。
- 后端目标回归：`mvn.cmd -q "-Dtest=AiModelDiscoveryServiceImplTest,AdminAiEngineConnectivityControllerTest,UserAiConfigServiceImplTest,CriticalEndpointRateLimitFilterTest" test` 通过。
- 后端编译：`mvn.cmd -q -DskipTests compile` 通过。
- 前端目标回归：`npm.cmd test -- --run src/__tests__/api/userAiConfig.test.js src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，4 个测试文件 / 61 个用例。
- 前端构建：`npm.cmd run build` 通过。
- 关联任务文件：`tasks/TASK_68_USER_CUSTOM_AI_PROVIDER.md`、`frontend/tasks/TASK_68_USER_CUSTOM_AI_PROVIDER_FRONTEND.md`。

### 尚未开始的功能

- 未扩展 Provider 专用模型列表协议。
- 未实现基础地址自动修正、模型能力检测、价格信息、自动路由、自动保存或更多 Provider 模板。
- 未新增数据库表、字段或迁移脚本。

### 停止，不继续下一个功能

本轮仅修复模型列表获取失败时的错误提示可读性和原始上游响应体暴露问题，等待验收，不继续推进其它 AI Provider 能力。

## 社区图片 OSS 接入代码审查问题修复（2026-06-05）

### 已完成且已验证的功能

- `application.yml` 中 RabbitMQ 配置已恢复到 `spring.rabbitmq` 命名空间，`server.shutdown` 保持独立顶层配置，避免 MQ 配置被挂到 `server.rabbitmq` 后失效。
- `CommunityController.getImage` 仍通过 302 返回 OSS 签名 URL，但业务日志不再记录签名 URL、`Expires` 或 `Signature` 等临时访问凭证参数。
- `CommunityService` 已补回既有 9 参数测试兼容构造器，生产注入入口继续使用带 `OssService` 的 10 参数完整构造器。
- 旧测试断言已同步到新图片代理路径 `/api/community/images/**` 和新的生产构造器依赖边界。

### 本轮完成状态

- RED 验证：新增/同步回归测试后，旧实现下 `RuntimeProtectionConfigTest.shouldKeepRabbitMqConfigurationUnderSpringNamespace` 失败于缺少 `spring.rabbitmq`，`CommunityControllerImageAccessTest.shouldRedirectToSignedUrlWithoutLoggingCredentialUrl` 失败于日志包含签名参数。
- GREEN 目标回归：`mvn.cmd -q "-Dtest=RuntimeProtectionConfigTest,CommunityControllerImageAccessTest,WebMvcConfigTest,CommunityServicePostQueryDeleteTest,SecurityConfigTest,CommunityServiceConstructorInjectionTest" test` 通过。
- 后端编译：`mvn.cmd -q -DskipTests compile` 通过。
- 后端全量测试：`mvn.cmd -q test` 通过，771 个测试。
- 前端目标回归：`npm.cmd test -- src/__tests__/utils/resumePdfPagination.test.js` 通过，1 个测试文件 / 5 个用例。
- 关联任务文件：`tasks/TASK_CODE_REVIEW_OSS_IMAGE_FIX_2026_06_05_BACKEND.md`。

### 尚未开始的功能

- 未新增数据库表、字段、索引、schema 或 migration。
- 未扩展图片审核、对象存储清理、CDN、前端上传体验或其它社区功能。
- 未修改前端实现。

### 停止，不继续下一个功能

本轮仅修复本次未提交改动代码审查中指出的社区图片 OSS 接入相关回归问题，等待验收，不继续推进其它社区能力。

## 滥用风险治理后端加固（2026-06-05）

### 已完成且已验证的功能

- 社区图片上传鉴权已收紧：只公开 `GET /api/community/images/community/**`，匿名上传接口不再被公共规则放行。
- 新增 `community_image` 图片登记、绑定与清理链路，上传后记录 `uploaded`，发帖/评论时通过条件原子更新绑定为 `bound`，超过 24 小时未绑定的对象会调用 OSS 删除能力清理。
- 社区图片访问新增生产默认防盗链规则，缺失、解析失败或非允许域名 `Referer` 默认返回 `403`；开发环境保留本地调试放行。
- 评论文本审核将广告词、导流联系方式、URL、手机号、微信号样式等疑似内容转入 `pending` 人工审核池，不直接公开。
- 新用户平台 AI 免费额度新增生产 30 分钟冷却期，冷却期内不能立刻消耗高成本面试/简历免费额度；VIP 日额度不受影响。
- 模拟面试 AI 增加提示词泄露输入拦截、系统提示补强、非流式输出扫描和流式短尾缓冲扫描。
- 面试 AI 日志已移除完整请求体 JSON，改为只记录模型、消息数量、角色分布和长度摘要。
- `db/schema.sql`、`server/db/schema.sql`、根目录 migration 与后端 migration 已同步新增 `community_image` 表。
- 本轮未修改前端实现。

### 本轮完成状态

- 目标回归：`mvn.cmd -q "-Dtest=SecurityConfigTest,CommunityControllerImageAccessTest,CriticalEndpointRateLimitFilterTest,CommunityTextModerationServiceTest,CommunityServiceModerationTest,CommunityServiceValidationTest,CommunityImageRegistryServiceTest,CommunityServiceImageBindingTest,InterviewAiServiceImplTest,AuthServiceImplTest,UserQuotaServiceImplTest,SchemaConsistencyTest" test` 通过。
- 代码审查修复：`sanitizePromptLeakOutput(null)` 已恢复旧有空字符串返回契约，避免上游 `message.content=null` 时把 null 传给报告解析或其它字符串调用方。
- 代码审查修复：微信联系方式识别已支持 `vx: resume888`、`微信 resume888` 等标签后带空格的格式，避免未命中其它风险词时被自动公开。
- 代码审查修复回归：`mvn.cmd "-Dtest=InterviewAiServiceImplTest#shouldReturnEmptyStringWhenPromptLeakOutputIsNull,CommunityTextModerationServiceTest#shouldRouteSpacedWechatIdWithoutOtherSuspiciousWordsToPendingReview" test` 通过。
- 后端编译：`mvn.cmd -q -DskipTests compile` 通过。
- 关联任务文件：`tasks/TASK_86_SECURITY_ABUSE_HARDENING_BACKEND.md`。

### 尚未开始的功能

- 未新增短信验证码、邮箱验证、设备指纹、图形验证码、信誉分、图片 AI 审核、举报系统或 CDN 防盗链配置。
- 未修改前端页面、弹窗、上传体验或审核池运营后台。
- 未扩展到简历附件、报告下载或其它文件类资源的统一对象生命周期治理。

### 停止，不继续下一个功能

本轮仅完成计划内匿名上传、在线图床、评论广告、AI 面试提示词泄露和新用户 AI 冷却治理，等待验收，不继续推进其它风控能力。

## 日志重复访问与数据库读降噪前端 P1 修复（2026-06-06）

### 已完成且已验证的功能

- 版本日志、引导状态、社区详情和社区评论已接入前端短缓存与 pending 复用，减少同页面短时间重复 GET。
- 引导状态更新、任务完成和社区写操作成功后会清理对应缓存前缀，避免写后旧数据展示。
- 报告页状态轮询已从固定 3 秒改为前 6 轮 3 秒、之后 6 秒，继续只请求轻量状态接口，报告详情成功回填后停止。
- 面试页开场白轮询增加代际 token 与在途状态，重复启动、卸载和旧请求返回不会生成多个 timer；生成或结束状态会停止后续轮询。
- 本轮不修改后端业务代码，不新增数据库表、字段、索引或 migration。

### 本轮完成状态

- RED 验证：目标前端测试在旧实现下失败，覆盖缺少短 TTL、GET 未缓存、报告轮询未退避和开场白生成信号未停止。
- GREEN 验证：`npm.cmd test -- --run src/__tests__/utils/apiCache.test.js src/__tests__/api/versionLog.test.js src/__tests__/api/onboarding.test.js src/__tests__/api/community.test.js src/__tests__/views/InterviewReportView.test.js src/__tests__/views/InterviewSessionView.test.js` 通过，6 个测试文件 / 78 个用例。
- 扩展验证：`npm.cmd test -- --run src/__tests__/api/performanceCache.test.js src/__tests__/layouts/MainLayout.test.js src/__tests__/views/HomePageView.test.js src/__tests__/views/community/PostDetailView.test.js src/__tests__/components/community/CommentSection.test.js` 中前 4 个相关测试文件通过；`CommentSection.test.js` 仍有既有图片大小文案断言漂移，实际为 2MB、旧断言为 5MB，非本轮改动引入。
- 前端构建：`npm.cmd run build` 通过。
- 日志复查：敏感内容 rg 复查 `logs debug.txt` 无命中；重复访问关键词在当前 `logs/ai-resume.log` 中仅命中历史样本 3 行，本轮未启动后端服务生成新日志。
- 关联任务文件：`frontend/tasks/TASK_LOG_REPEAT_ACCESS_OPTIMIZATION_FRONTEND_PLAN.md`、`tasks/task-日志异常与重复访问优化修复计划.md`。

### 停止，不继续下一个功能

本轮仅完成日志重复访问优化的前端 P1-1 至 P1-6 范围，等待验收，不继续推进新功能、后端接口或数据库优化。

## 日志残留热点与 SSE 断连降噪修复（2026-06-06）

### 已完成且已验证的功能

- 通知列表 `getNotifications(params)` 接入 15 秒前端短缓存和 pending 复用，按分页与筛选参数隔离缓存；通知标记已读、全部已读、删除、批量删除仍会清理通知缓存。
- 公开版本日志分页 `getPublicVersionLogsPage(params)` 接入短缓存，按 page/size 隔离缓存，减少版本日志页面短时间重复访问后端。
- SSE 客户端主动断开连接时，`NotificationService` 与 `GlobalExceptionHandler` 均改为 DEBUG 日志；非正常 IO 异常仍保留 WARN。
- `SecurityConfig` 提供空 `UserDetailsService`，项目继续使用 JWT 认证，开发启动时不再打印 Spring Boot generated password 警告。
- 本轮不新增数据库表、字段、索引或 migration，不修改接口协议。

### 本轮完成状态

- RED 验证：`npm.cmd test -- --run src/__tests__/api/performanceCache.test.js src/__tests__/api/versionLog.test.js` 在旧实现下失败，复现通知列表和公开版本日志分页未复用短缓存。
- RED 验证：`mvn.cmd -q "-Dtest=NotificationServiceTest,SecurityConfigTest" test` 在旧实现下失败，复现空 `UserDetailsService` 缺失；新增 SSE 断连日志测试后覆盖 WARN 降噪边界。
- GREEN 验证：`npm.cmd test -- --run src/__tests__/api/performanceCache.test.js src/__tests__/api/versionLog.test.js` 通过，2 个测试文件 / 15 个用例。
- GREEN 验证：`mvn.cmd -q "-Dtest=NotificationServiceTest,SecurityConfigTest,GlobalExceptionHandlerTest" test` 通过。
- 后端目标回归与 Spring 上下文验证：`mvn.cmd -q "-Dtest=NotificationServiceTest,SecurityConfigTest,GlobalExceptionHandlerTest,ServerApplicationTests" test` 通过。
- 后端编译：`mvn.cmd -q -DskipTests compile` 通过。
- 前端构建：`npm.cmd run build` 通过。
- 日志复查：`server/logs/ai-resume.log` 最新 16:11 样本未再出现 `Using generated security password`，SSE 客户端主动断连为 DEBUG；敏感 AI 请求体仍只命中 `2026-06-04` 归档旧日志。
- 关联任务文件：`tasks/task-日志异常与重复访问优化修复计划.md`、`frontend/tasks/TASK_LOG_REPEAT_ACCESS_OPTIMIZATION_FRONTEND_PLAN.md`。

### 日志排查结论

- `logs/ai-resume.log` 最新样本中未发现 `InvalidAccessKeyId`、未脱敏 `OSSAccessKeyId`、未脱敏 `Signature`、未脱敏 `Authorization`、Redis 缓存失败或 SQL 明细输出。
- 当前仍能看到的 `获取会话轻量状态` 是 DEBUG 日志，生产 profile 下不会进入 INFO/WARN 主噪声；如开发日志仍嫌多，可进一步按包级别降低 Controller DEBUG。
- 历史归档日志仍包含修复前的 AI 请求体和 OSS AccessKey 摘要样本，属于旧日志存量风险，需要单独清理历史日志文件或调整保留周期。

### 停止，不继续下一个功能

本轮只处理日志排查发现的残留热点与 SSE 断连误报，不继续推进通知后端缓存、SQL 索引、社区详情查询合并或其它性能重构。

## 公开版本日志匿名访问修复（后端，2026-07-24）
### 当前阶段
- 已完成并已验收。

### 已完成且已验收的功能
- `SecurityConfig` 已放行 `GET /api/version-logs` 与 `GET /api/version-logs/latest`。
- `SecurityConfigTest` 已补充公开版本日志接口匿名放行回归测试。
- 首页匿名访问不再因为公开版本日志接口返回 401 而误弹“登录已过期”。

### 本轮完成状态
- 本地目标测试 `mvn.cmd -q -Dtest=SecurityConfigTest test` 通过。
- 本地编译 `mvn.cmd -q -DskipTests compile` 通过。
- 服务器端目标测试 `mvn -q -Dtest=SecurityConfigTest test` 通过。
- 服务器端打包后已替换后端 jar 并重启 `offercat` 服务。
- 外部验证 `https://kelin.cyou/api/version-logs/latest?limit=5` 返回 `200`。
- 外部验证 `https://kelin.cyou/` 返回 `200`。
- 原有 `http://103.231.57.13/` 仍保持原项目。

### 尚未开始的功能
- 未继续修改前端请求层、登录页文案、管理端权限或其它公开接口。

### 停止说明
- 本轮到此停止，等待验收，不继续推进下一功能。

## 新老用户赠送一年会员（后端，2026-08-19）

### 已完成且已验证的功能
- 新注册用户直接获得启用中的 `vip_year` 年度会员，到期时间为注册时刻加一年。
- 注册用户、额度初始化和站内通知共用事务，通知写入失败时注册不会假成功。
- 存量非管理员账号通过双份幂等迁移补到至少一年有效期，并各收到一条站内通知。
- 管理员保留 `role=9`，避免会员补发覆盖角色后丢失管理权限。
- 不创建模拟付费订单，不修改免费 100 次额度、历史用量或图床策略。

### 本轮完成状态
- RED：旧实现缺少严格通知入口；通知 Mapper 返回 0 行时旧实现未抛异常。
- GREEN：`AuthServiceImplTest,NotificationServiceTest,OneYearMembershipGiftMigrationTest,SchemaConsistencyTest,MembershipServiceImplTest` 通过。
- 全量测试共执行 861 个用例，仅既有 AI 流式测试因本机缺少 `DOUBAO_API_KEY` 报错，本任务相关测试全部通过。
- `mvn.cmd -q -DskipTests package` 通过，本地 Jar 构建成功。
- 关联任务文件：`tasks/TASK_91_ONE_YEAR_MEMBERSHIP_GIFT_BACKEND.md`。

### 尚未开始的功能
- 未重构管理员与会员复合角色模型，未新增支付订单、会员前端页面或其它赠送活动。

### 停止，不继续下一个功能
- 本轮仅完成一年会员赠送和通知，等待验收，不继续推进其它会员功能。
