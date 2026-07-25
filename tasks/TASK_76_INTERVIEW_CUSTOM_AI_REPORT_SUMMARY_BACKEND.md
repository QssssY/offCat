# 模拟面试报告与摘要用户自定义 AI 路由修复后端

## 跟进修复：问答组装与报告解析日志标签残留（2026-06-03）

- 触发原因：用户实测日志中真实请求已经显示 `USER_CUSTOM/openai-compatible`、`source=user_custom`、自定义 `baseUrl/model`，但请求前的“对话消息组装完成”仍显示 `[DEEPSEEK]`，造成“到底走了哪个 AI”的误判。
- 原因定位：`InterviewAiServiceImpl.buildConversationMessages(...)` 仍使用注入的默认 `provider.toUpperCase()` 生成日志标签；报告解析函数 `parseEvaluationResponse(...)` 和 `buildThinkingConfig(...)` 的 warning 也存在同类风险。
- 修复范围：`buildConversationMessages(...)`、`parseEvaluationResponse(...)`、`buildThinkingConfig(...)` 均改为由入口传入已解析出的运行时 `tag`，因此用户自定义 AI 请求的组装、请求、解析、thinking warning 日志口径保持一致。
- 启动日志说明：构造函数启动阶段仍展示默认平台 provider，这是服务初始化配置，不代表单次用户请求路由；单次请求判断以 `source/baseUrl/model/configType` 和运行时 tag 为准。
- 新增回归测试：`InterviewAiServiceImplTest` 覆盖自定义 AI 下消息组装日志、报告解析日志、thinking warning 日志必须使用 `USER_CUSTOM/openai-compatible`，且不能回退到默认 provider 标签。
- RED 验证：`mvn.cmd -q "-Dtest=InterviewAiServiceImplTest" test` 在旧实现下因新签名不存在失败，覆盖消息组装、报告解析、thinking warning 三条路径。
- GREEN 验证：`mvn.cmd -q "-Dtest=InterviewAiServiceImplTest" test` 通过。
- 回归验证：`mvn.cmd -q "-Dtest=InterviewServiceTest,InterviewAiServiceImplTest,InterviewContextCompressorTest,AiChatClientTest,UserAiConfigResolverImplTest,UserAiUsageLimitServiceImplTest" test` 通过。
- 编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 全量后端验证：`mvn.cmd -q test` 通过。

## 当前任务所属模块

- 后端模块：模拟面试、用户自定义 AI Provider、面试上下文摘要、面试评价报告、AI 调用日志与用量计数。
- 触发原因：`debug.txt` 暴露同一场面试中问答、摘要、报告阶段日志口径混杂，报告和摘要因为缺少 `userId/fallbackToPlatform` 透传而可能退回平台 MIMO；同时摘要和报告超限路径仍存在普通硬截断日志，影响问题定位。

## 前端文件定位

- 本轮不修改前端文件。
- 前端平台回退开关仍沿用现有 `fallbackToPlatform=true` 请求语义。

## 后端文件定位

- 面试主服务：`server/src/main/java/com/airesume/server/service/InterviewService.java`
- 面试 AI 实现：`server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- 上下文压缩服务：`server/src/main/java/com/airesume/server/service/InterviewContextCompressor.java`
- 轻量 AI 客户端：`server/src/main/java/com/airesume/server/service/AiChatClient.java`
- 相关测试：`server/src/test/java/com/airesume/server/service/InterviewServiceTest.java`、`server/src/test/java/com/airesume/server/service/InterviewContextCompressorTest.java`、`server/src/test/java/com/airesume/server/service/impl/InterviewAiServiceImplTest.java`、`server/src/test/java/com/airesume/server/service/AiChatClientTest.java`

## 本轮修改文件清单

- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/InterviewContextCompressor.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/AiChatClient.java`
- `server/src/test/java/com/airesume/server/service/InterviewServiceTest.java`
- `server/src/test/java/com/airesume/server/service/InterviewContextCompressorTest.java`
- `server/src/test/java/com/airesume/server/service/impl/InterviewAiServiceImplTest.java`
- `server/src/test/java/com/airesume/server/service/AiChatClientTest.java`

## 前端实现方案

- 本轮不改前端。
- 用户自定义 AI 会话、平台 AI 会话、手动平台回退会话的区分继续由后端 `ai_billing_source` 和请求中的 `fallbackToPlatform` 共同决定。

## 后端实现方案

- 报告生成链路改为根据 `interview_session.ai_billing_source` 推导 AI 来源：`user_custom` 会话调用 `generateEvaluationReport(..., userId, false)`，平台和 `platform_fallback` 会话调用 `generateEvaluationReport(..., userId, true)`。
- 报告阶段命中用户自定义 AI 时调用 `UserAiUsageLimitService.checkAndIncrement(userId)`；AI 调用或报告解析失败进入兜底报告时回滚本次自定义 AI 用量。
- `InterviewContextCompressor.compressHistory` 和 `compressForEvaluation` 新增 `userId/fallbackToPlatform` 重载，常规问答压缩和最终评价压缩都透传到 `AiChatClient.chat(..., userId, fallbackToPlatform)`。
- 摘要阶段命中用户自定义 AI 时独立计数；摘要 AI 调用失败、返回过短或模型摘要超长并降级时回滚本次摘要用量。
- `InterviewAiServiceImpl` 和 `AiChatClient` 增加运行时日志标签，用户自定义 OpenAI-compatible 调用显示为 `USER_CUSTOM/openai-compatible`，并输出 `source/baseUrl/model/configType`，避免再用 `[OPENAI]` 误导真实来源判断。
- 摘要输出超过上限时不再作为普通成功路径静默 substring，而是记录“AI 摘要过长”并走本地降级摘要；只有降级摘要仍超限时才记录 `last_resort_truncate=true` 并保底截断。
- 报告 prompt 超限时先使用短结构化摘要和最近消息重建，再减少最近消息保留数；最后仍超限才对 prompt 做明确标记的最终保底截断。

## 数据存储方案

- 不修改数据库结构。
- 不新增表、字段、索引或迁移脚本。
- `providerName` 不新增字段，真实路由仍以 `baseUrl + model + apiKey` 为准。

## stage 更新说明

- 已在 `tasks/stage.md` 顶部记录本轮后端修复、验证命令和停止边界。

## 编译结果

- `mvn.cmd -q -DskipTests compile` 通过。

## 构建结果

- 后端无前端构建产物。

## 当前功能验收说明

- RED 验证：`mvn.cmd -q "-Dtest=InterviewServiceTest,InterviewContextCompressorTest" test` 在旧实现下编译失败，失败点为 `compressForEvaluation` 缺少 `userId/fallbackToPlatform` 重载。
- GREEN 验证：`mvn.cmd -q "-Dtest=InterviewServiceTest,InterviewAiServiceImplTest,InterviewContextCompressorTest,AiChatClientTest,UserAiConfigResolverImplTest,UserAiUsageLimitServiceImplTest" test` 通过。
- 编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 全量后端验证：`mvn.cmd -q test` 通过。

## 停止，不继续下一个功能

本轮仅修复模拟面试报告和摘要未沿用用户自定义 AI 配置、日志来源误导、后台摘要/报告自定义 AI 用量计数和硬截断残留问题；不继续扩展数据库结构、前端 UI、TTS 配置、统计图表或其它 AI 业务链路。
