# 模拟面试平台 AI 兜底计费修复后端

## 当前任务所属模块

模拟面试、自定义 AI Provider、平台 AI 兜底计费、用户额度扣减。

## 本轮修改文件清单

- `server/src/main/java/com/airesume/server/common/constants/UserAiConstants.java`
- `server/src/main/java/com/airesume/server/entity/InterviewSession.java`
- `server/src/main/java/com/airesume/server/mapper/InterviewSessionMapper.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/test/java/com/airesume/server/service/InterviewServiceTest.java`
- `server/src/test/java/com/airesume/server/db/SchemaConsistencyTest.java`
- `db/schema.sql`
- `server/db/schema.sql`
- `db/migrations/TASK_74_INTERVIEW_PLATFORM_FALLBACK_BILLING.sql`
- `server/db/migrations/TASK_74_INTERVIEW_PLATFORM_FALLBACK_BILLING.sql`

## 后端实现方案

- 新增 `interview_session.ai_billing_source` 字段，记录面试会话计费来源：`platform`、`user_custom`、`platform_fallback`。
- 创建面试会话时按实际 AI 来源写入计费来源：平台 AI 会话写 `platform`，自定义 AI 会话写 `user_custom`。
- 当用户在自定义 AI 面试会话内显式携带 `fallbackToPlatform=true` 时，通过 `markPlatformFallbackBillingIfCustom` 原子更新把会话从 `user_custom` 标记为 `platform_fallback`。
- 只有原子标记成功的请求才调用 `userQuotaService.deductInterviewQuota(userId)` 扣 1 次平台模拟面试额度，避免同一会话重复点击或并发请求重复扣费。
- 非流式消息和 SSE 流式消息共用 `InterviewService.chargePlatformFallbackQuotaIfNeeded`，保持两条入口计费口径一致。

## 2026-06-04 追加路由锁定修复

- 原问题：`ai_billing_source=platform_fallback` 只参与防重复扣费，后续消息如果前端没有继续传 `fallbackToPlatform=true`，后端仍可能重新解析到用户自定义 AI。
- 本轮修复：新增 `InterviewService.resolveEffectiveFallbackToPlatform`，统一把“请求显式兜底”与“会话已锁定为 `platform` / `platform_fallback`”合并为本轮实际 AI 路由。
- 非流式 `sendMessage` 与 SSE `streamMessage` 都改为使用 `effectiveFallbackToPlatform` 调用 AI 服务；`requestFallbackToPlatform` 仍只用于触发首次 `user_custom -> platform_fallback` 的补扣逻辑。
- 修复后，同一 session 一旦创建为平台 AI 或首次兜底后标记为 `platform_fallback`，后续请求即使不再携带兜底参数也会继续走平台 AI，且不会重复扣平台面试额度。

## 数据存储方案

- `interview_session` 新增 `ai_billing_source VARCHAR(32) NOT NULL DEFAULT 'platform'`。
- 同步更新 `db/schema.sql` 与 `server/db/schema.sql`。
- 新增双目录迁移脚本 `TASK_74_INTERVIEW_PLATFORM_FALLBACK_BILLING.sql`，使用 `information_schema.COLUMNS` 做可重复执行的字段补齐。

## 验证结果

- RED：新增 `InterviewServiceTest` 兜底计费用例后，生产代码缺少 `aiBillingSource`、`BILLING_SOURCE_PLATFORM_FALLBACK` 和 `markPlatformFallbackBillingIfCustom`，测试编译失败。
- GREEN：`mvn.cmd -q "-Dtest=InterviewServiceTest#shouldMarkCustomAiBillingSourceWhenCreatingSessionWithCustomAi+shouldDeductInterviewQuotaOnceWhenCustomAiSessionFallsBackToPlatform+shouldNotDeductInterviewQuotaAgainWhenPlatformFallbackAlreadyMarked,InterviewControllerTest,SchemaConsistencyTest" test` 通过。
- 2026-06-04 RED：`mvn.cmd -q "-Dtest=InterviewServiceTest#shouldKeepUsingPlatformAfterFallbackSessionAlreadyMarkedWithoutRequestFlag+shouldKeepPlatformSessionOnPlatformEvenWhenUserHasCustomAiConfig" test` 在旧实现下失败，复现后续消息仍以 `fallback=false` 调用 AI 服务的问题。
- 2026-06-04 GREEN：`mvn.cmd -q "-Dtest=InterviewServiceTest#shouldKeepUsingPlatformAfterFallbackSessionAlreadyMarkedWithoutRequestFlag+shouldKeepPlatformSessionOnPlatformEvenWhenUserHasCustomAiConfig,InterviewControllerTest#streamMessageShouldChargeFallbackQuotaBeforePersistingUserMessage+streamMessageShouldKeepUsingPlatformWhenFallbackSessionAlreadyMarkedWithoutRequestFlag" test` 通过。
- 2026-06-04 后端回归：`mvn.cmd -q "-Dtest=InterviewServiceTest,InterviewControllerTest,SchemaConsistencyTest" test` 通过。
- 2026-06-04 后端编译：`mvn.cmd -q -DskipTests compile` 通过。
- 2026-06-04 后端全量测试：`mvn.cmd -q test` 通过。

## 当前功能验收说明

- 自定义 AI 面试会话失败后，用户点击“使用平台 AI”会消耗 1 次平台模拟面试额度。
- 同一个面试 session 后续继续使用平台 AI 不会重复扣平台面试额度。
- 同一个面试 session 一旦标记为 `platform_fallback`，后续消息即使前端不再携带 `fallbackToPlatform=true`，后端也会继续锁定平台 AI 路由。
- 普通平台 AI 创建会话仍沿用原创建时扣费逻辑。
- 本轮不扩展前端 UI、不新增退款能力、不修改其它 AI 链路计费规则。

## 停止，不继续下一个功能

本轮仅修复模拟面试自定义 AI 手动平台兜底的额度漏洞，等待验收，不继续推进 Offer 辅助或其它计费链路重构。
