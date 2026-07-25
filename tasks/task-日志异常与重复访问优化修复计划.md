# 日志异常与重复访问优化修复计划

## 当前任务所属模块

日志安全、OSS 图片访问稳定性、Redis 缓存降级观测、重复请求与数据库访问降噪。

本文档是修复计划，不包含业务代码落地；后续执行时按优先级拆成独立轮次实施，完成一轮后更新对应 task / stage 文档并停止等待验收。

## P0-1 执行记录（2026-06-05）

### 本轮修改文件清单

- 新增 `server/src/main/java/com/airesume/server/infrastructure/logging/SensitiveLogMasker.java`。
- 新增 `server/src/main/java/com/airesume/server/infrastructure/logging/SensitiveLogMessageConverter.java`。
- 新增 `server/src/test/java/com/airesume/server/infrastructure/logging/SensitiveLogMaskerTest.java`。
- 修改 `server/src/main/resources/logback-spring.xml`。
- 更新 `tasks/task-日志异常与重复访问优化修复计划.md` 与 `tasks/stage.md`。

### 后端实现方案

- `SensitiveLogMasker` 统一处理日志脱敏，覆盖 OSS 签名查询参数、Authorization、API Key、AccessKey 以及 AI 请求中的 `messages`、`prompt`、`systemPrompt`、`userPrompt`、`resumeText`、`diagnosisResult` 等高风险字段。
- `SensitiveLogMessageConverter` 接入 logback，在格式化日志消息和异常文本输出前统一调用脱敏逻辑；`logback-spring.xml` 的 `CONSOLE`、`FILE`、`ERROR_FILE` 已统一改为 `%sensitiveMsg%nopex`，避免 logback 追加未脱敏异常堆栈。
- 保留排障字段：provider、baseUrlHost、endpoint、model、configType、status、elapsedMs、objectKey、errorType 等不会被脱敏规则误删。

### 数据存储方案

- 本轮不新增表、不修改字段、不新增索引，不涉及 `db/` 或 `server/db/` SQL 脚本。

### stage 更新说明

- 已更新根目录 `tasks/stage.md`。
- 本轮未修改前端文件，不更新 `frontend/tasks/stage.md`。

### 编译与测试结果

- RED 验证：`mvn.cmd -q "-Dtest=SensitiveLogMaskerTest" test` 在生产类不存在时失败，失败原因为 `SensitiveLogMasker` 符号不存在。
- GREEN 验证：`mvn.cmd -q "-Dtest=SensitiveLogMaskerTest" test` 通过。
- Spring 上下文验证：`mvn.cmd -q "-Dtest=ServerApplicationTests" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。

### 当前功能验收说明

- 已执行日志样本复查：`rg -n 'OSSAccessKeyId=[^*&\\s]{8,}|Signature=[^*&\\s]{8,}|Authorization: Bearer|\"messages\"\\s*:\\s*\\[|apiKey\\s*[:=]\\s*[^*\\s,;}&\"]{8,}|accessKeySecret\\s*[:=]\\s*[^*\\s,;}&\"]{8,}' logs debug.txt`，结果无命中。
- 本轮只完成 P0-1 日志敏感信息脱敏；未开始 P0-2 OSS 凭据定位、P1 重复访问降噪或 P2 Redis 观测优化。

### 停止，不继续下一个功能

本轮执行边界为 P0-1「日志敏感信息脱敏」，已完成后立即停止，等待验收，不继续推进 P0-2、P1 或 P2。

## P0-2 执行记录（2026-06-05）

### 本轮修改文件清单

- 修改 `server/src/main/java/com/airesume/server/config/OssConfig.java`。
- 修改 `server/src/main/java/com/airesume/server/service/impl/OssServiceImpl.java`。
- 修改 `server/src/main/java/com/airesume/server/controller/CommunityController.java`。
- 新增 `server/src/test/java/com/airesume/server/service/impl/OssServiceImplTest.java`。
- 补充 `server/src/test/java/com/airesume/server/controller/CommunityControllerImageAccessTest.java`。
- 更新 `tasks/task-日志异常与重复访问优化修复计划.md` 与 `tasks/stage.md`。

### 后端实现方案

- `OssConfig` 新增启用状态下的必填字段缺失列表，只返回字段名，不返回 endpoint、AccessKey 或 Secret 原值。
- `OssServiceImpl` 将 OSS 状态区分为“未启用”和“启用但配置缺失”；启用但缺失 endpoint、bucketName、accessKeyId 或 accessKeySecret 时快速失败并明确缺失字段。
- `OssServiceImpl` 初始化日志只输出 endpoint、bucket 和脱敏后的 AccessKeyId（前 4 位 + `****` + 后 4 位），初始化失败时只记录错误类型，不记录 SDK 原始异常消息。
- `generateSignedUrl` 捕获 OSS SDK 异常时只记录 objectKey、bucket、脱敏 AccessKeyId、errorCode 和 errorType，不记录完整签名 URL 或签名参数。
- `CommunityController.getImage` 在签名 URL 生成失败时返回稳定 `502 Bad Gateway`，不把上游 OSS 错误详情或签名 URL 透出到前端。

### 数据存储方案

- 本轮不新增表、不修改字段、不新增索引，不涉及 `db/` 或 `server/db/` SQL 脚本。

### stage 更新说明

- 已更新根目录 `tasks/stage.md`。
- 本轮未修改前端文件，不更新 `frontend/tasks/stage.md`。

### 编译与测试结果

- RED 验证：`mvn.cmd -q "-Dtest=OssServiceImplTest,CommunityControllerImageAccessTest" test` 在旧实现下失败，失败原因为 `OssServiceImplTest` 缺少可覆盖的 `buildClient` 客户端构造边界。
- GREEN 目标验证：`mvn.cmd -q "-Dtest=OssServiceImplTest,CommunityControllerImageAccessTest" test` 通过。
- P0-1/P0-2 组合回归：`mvn.cmd -q "-Dtest=SensitiveLogMaskerTest,OssServiceImplTest,CommunityControllerImageAccessTest" test` 通过。
- Spring 上下文验证：`mvn.cmd -q "-Dtest=ServerApplicationTests" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。

### 当前功能验收说明

- 已执行日志样本复查：`rg -n 'OSSAccessKeyId=[^*&\\s]{8,}|Signature=[^*&\\s]{8,}|Authorization: Bearer|\"messages\"\\s*:\\s*\\[|apiKey\\s*[:=]\\s*[^*\\s,;}&\"]{8,}|accessKeySecret\\s*[:=]\\s*[^*\\s,;}&\"]{8,}|LTAI5tSensitiveKey7890|aliyun-access-key-secret-value' logs debug.txt`，结果无命中。
- 本轮只完成 P0-2 OSS `InvalidAccessKeyId` 定位与安全化处理；未开始 P1 重复访问降噪或 P2 Redis 观测优化。

### 停止，不继续下一个功能

本轮执行边界为 P0-2「OSS `InvalidAccessKeyId` 定位与安全化处理」，已完成后立即停止，等待验收，不继续推进 P1 或 P2。

## P1-1 执行记录（2026-06-05）

### 本轮修改文件清单

- 修改 `server/src/main/java/com/airesume/server/controller/InterviewController.java`。
- 补充 `server/src/test/java/com/airesume/server/controller/InterviewControllerTest.java`。
- 更新 `tasks/task-日志异常与重复访问优化修复计划.md` 与 `tasks/stage.md`。

### 后端实现方案

- 将 `GET /api/interview/session/{sessionId}/status` 的高频轮询入口日志从 `info` 降为 `debug`，减少 `logs/ai-resume.log` 中「获取会话轻量状态」类高频噪声。
- 保持 `InterviewService.getSessionStatus` 与 `InterviewSessionMapper.selectOwnedStatus` 既有轻量查询链路不变：继续只返回状态、开场白 pending、报告 ready、综合分和更新时间，不加载聊天记录、岗位上下文或完整报告正文。
- 补充控制器日志级别回归测试，锁定状态轮询请求只产生 DEBUG 级别日志，不再产生 INFO 级别噪声。

### 数据存储方案

- 本轮不新增表、不修改字段、不新增索引，不涉及 `db/` 或 `server/db/` SQL 脚本。

### stage 更新说明

- 已更新根目录 `tasks/stage.md`。
- 本轮未修改前端文件，不更新 `frontend/tasks/stage.md`。

### 编译与测试结果

- RED 验证：`mvn.cmd -q "-Dtest=InterviewControllerTest,InterviewServiceTest" test` 在旧实现下失败，失败原因为 `shouldLogSessionStatusPollingRequestAtDebugLevel` 未捕获到 DEBUG 级别状态日志，且旧实现仍输出 INFO 级别「获取会话轻量状态」日志。
- GREEN 目标验证：`mvn.cmd -q "-Dtest=InterviewControllerTest,InterviewServiceTest" test` 通过。
- Spring 上下文验证：`mvn.cmd -q "-Dtest=ServerApplicationTests" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。

### 当前功能验收说明

- 同一状态轮询请求仍返回原有轻量响应结构，未改变接口 URL、参数、响应字段或业务流程。
- 高频状态轮询日志已降为 DEBUG；正常 INFO 日志中不再出现本轮测试覆盖的「获取会话轻量状态」轮询噪声。
- 本轮只完成 P1-1 面试轻量状态轮询降噪；未开始 P1-2 版本日志/引导状态缓存兜底或 P2 Redis 观测优化。

### 停止，不继续下一个功能

本轮执行边界为 P1-1「面试轻量状态轮询降噪」，已完成后立即停止，等待验收，不继续推进 P1-2 或 P2。

## P1-2 执行记录（2026-06-05）

### 本轮修改文件清单

- 修改 `server/src/main/java/com/airesume/server/service/impl/SysVersionLogServiceImpl.java`。
- 修改 `server/src/main/java/com/airesume/server/controller/AdminVersionLogController.java`。
- 修改 `server/src/main/java/com/airesume/server/service/impl/UserOnboardingServiceImpl.java`。
- 修改 `server/src/main/java/com/airesume/server/dto/onboarding/OnboardingStatusResponse.java`。
- 修改 `server/src/main/java/com/airesume/server/config/RedisConfig.java`。
- 补充 `server/src/test/java/com/airesume/server/service/impl/SysVersionLogServiceImplTest.java`。
- 补充 `server/src/test/java/com/airesume/server/controller/AdminVersionLogControllerTest.java`。
- 补充 `server/src/test/java/com/airesume/server/service/impl/UserOnboardingServiceImplTest.java`。
- 补充 `server/src/test/java/com/airesume/server/config/RedisConfigTest.java`。
- 更新 `tasks/task-日志异常与重复访问优化修复计划.md` 与 `tasks/stage.md`。

### 后端实现方案

- `SysVersionLogServiceImpl.getLatestPublished(int limit)` 增加 `@Cacheable(value = "config:versionLogs", key = "#limit", sync = true)`，以服务层安全 limit 作为缓存 key，减少短时间重复读取版本日志表。
- `AdminVersionLogController` 的新增、更新、发布、批量发布、删除、批量删除版本日志写操作增加 `@CacheEvict(value = "config:versionLogs", allEntries = true)`，确保管理端写入后下一次读取能看到最新版本日志。
- `UserOnboardingServiceImpl.getStatus(Long userId, String guideKey)` 增加 `@Cacheable(value = "user:onboardingStatus", key = "#userId + '::' + #guideKey", sync = true)`，按用户和引导 key 缓存短 TTL 引导状态。
- `UserOnboardingServiceImpl.updateStatus(...)` 与 `completeTask(...)` 增加对应 `@CacheEvict`，状态写入或任务完成后主动驱逐用户引导状态缓存。
- `OnboardingStatusResponse` 实现 `Serializable`，适配当前 Redis cache 使用的 JDK 序列化。
- `RedisConfig.initialCacheConfigurations` 注册 `config:versionLogs` 5 分钟 TTL 与 `user:onboardingStatus` 60 秒 TTL。

### 数据存储方案

- 本轮不新增表、不修改字段、不新增索引，不涉及 `db/` 或 `server/db/` SQL 脚本。

### stage 更新说明

- 已更新根目录 `tasks/stage.md`。
- 本轮未修改前端文件，不更新 `frontend/tasks/stage.md`。

### 编译与测试结果

- RED 验证：`mvn.cmd -q "-Dtest=SysVersionLogServiceImplTest,VersionLogControllerTest,AdminVersionLogControllerTest,UserOnboardingServiceImplTest,RedisConfigTest" test` 在旧实现下失败，失败原因为缺少版本日志/引导状态缓存注解和 Redis TTL 注册。
- GREEN 目标验证：`mvn.cmd -q "-Dtest=SysVersionLogServiceImplTest,VersionLogControllerTest,AdminVersionLogControllerTest,UserOnboardingServiceImplTest,RedisConfigTest" test` 通过。
- Spring 上下文验证：`mvn.cmd -q "-Dtest=ServerApplicationTests" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。

### 当前功能验收说明

- 版本日志最新列表读接口已具备后端 5 分钟缓存兜底，管理端版本日志写操作会主动驱逐缓存。
- 用户引导状态读接口已具备 60 秒用户级缓存兜底，状态更新和任务完成会主动驱逐缓存。
- 本轮只完成 P1-2 版本日志与引导状态后端缓存兜底；未开始 P2 Redis 缓存失败可观测性与回源压力保护。

### 停止，不继续下一个功能

本轮执行边界为 P1-2「版本日志与引导状态后端缓存兜底」，已完成后立即停止，等待验收，不继续推进 P2。

## P2-1 执行记录（2026-06-06）

### 本轮修改文件清单

- 修改 `server/src/main/java/com/airesume/server/config/RedisCacheErrorHandler.java`。
- 修改 `server/src/main/java/com/airesume/server/config/RedisConfig.java`。
- 新增 `server/src/test/java/com/airesume/server/config/RedisCacheErrorHandlerTest.java`。
- 补充 `server/src/test/java/com/airesume/server/config/RedisConfigTest.java`。
- 更新 `tasks/task-日志异常与重复访问优化修复计划.md` 与 `tasks/stage.md`。

### 后端实现方案

- `RedisCacheErrorHandler` 保留“记录日志 + 回源数据库 / 忽略缓存写删失败”的业务降级策略，不在业务层新增重复兜底。
- Redis 读取、写入、删除、清空缓存失败日志统一补充 `exceptionType` 与 `fallback` 字段；读取失败使用 `fallback=database`，写入/删除/清空失败使用 `fallback=ignore`。
- 对同一 `cacheName + key + exceptionType` 的 warn 做 30 秒短窗口限频，避免 Redis 故障时同一热点 key 重复刷屏。
- 序列化不兼容仍会尝试 `cache.evict(key)` 清理坏缓存；若坏缓存删除失败，则记录带 `exceptionType` 的受控 warn，等待 TTL 自然过期。
- `RedisConfig.initialCacheConfigurations` 显式注册 `sys_user` 5 分钟 TTL，继续保留 `auth:userInfo` 10 分钟 TTL、`user:quota` 5 分钟 TTL 等热点缓存配置。

### 数据存储方案

- 本轮不新增表、不修改字段、不新增索引，不涉及 `db/` 或 `server/db/` SQL 脚本。

### stage 更新说明

- 已更新根目录 `tasks/stage.md`。
- 本轮未修改前端文件，不更新 `frontend/tasks/stage.md`。

### 编译与测试结果

- RED 验证：`mvn.cmd -q "-Dtest=RedisCacheErrorHandlerTest,RedisConfigTest" test` 在旧实现下失败，失败原因为日志缺少 `exceptionType`/`fallback`、同 key 重复 warn 未限频、`sys_user` TTL 未显式注册。
- GREEN 目标验证：`mvn.cmd -q "-Dtest=RedisCacheErrorHandlerTest,RedisConfigTest" test` 通过。
- Spring 上下文验证：`mvn.cmd -q "-Dtest=ServerApplicationTests" test` 通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。

### 当前功能验收说明

- Redis 读取失败日志现在能定位 cache、key、根因类型和回源策略；同一 cache/key/根因 30 秒内只记录一条 warn。
- Redis 序列化不兼容路径仍会驱逐坏缓存，避免同一脏值持续命中。
- `sys_user`、`auth:userInfo`、`user:quota` 等热点缓存 TTL 已由测试锁定。
- 本轮只完成 P2-1 Redis 缓存失败可观测性与回源压力保护；未开始任何新的 P3/前端优化任务。

### 停止，不继续下一个功能

本轮执行边界为 P2-1「Redis 缓存失败可观测性与回源压力保护」，已完成后立即停止，等待验收，不继续推进其他功能。

## 前端 P1 执行记录（2026-06-06）

### 本轮修改文件清单

- 修改 `frontend/app/src/utils/apiCache.js`。
- 修改 `frontend/app/src/api/versionLog.js`。
- 修改 `frontend/app/src/api/onboarding.js`。
- 修改 `frontend/app/src/api/community.js`。
- 修改 `frontend/app/src/views/interview/InterviewReportView.vue`。
- 修改 `frontend/app/src/views/interview/InterviewSessionView.vue`。
- 补充 `frontend/app/src/__tests__/utils/apiCache.test.js`、`frontend/app/src/__tests__/api/versionLog.test.js`、`frontend/app/src/__tests__/views/InterviewReportView.test.js`、`frontend/app/src/__tests__/views/InterviewSessionView.test.js`。
- 新增 `frontend/app/src/__tests__/api/onboarding.test.js` 与 `frontend/app/src/__tests__/api/community.test.js`。
- 更新 `frontend/tasks/TASK_LOG_REPEAT_ACCESS_OPTIMIZATION_FRONTEND_PLAN.md` 与 `frontend/tasks/stage.md`。

### 前端实现方案

- 版本日志、引导状态、社区详情和社区评论接入前端短缓存，稳定 GET 在短窗口内复用 pending/value。
- 引导状态更新、任务完成以及社区写操作成功后清理对应缓存前缀，避免写后展示旧数据。
- 报告页状态轮询改为前 6 轮 3 秒、之后 6 秒，继续只请求轻量状态接口，报告详情成功回填后停止轮询。
- 面试页开场白轮询增加代际 token 和在途状态，重复启动、卸载或旧请求返回时不会复活旧 timer；生成或结束状态会停止轮询。

### 数据存储方案

- 本轮不新增表、不修改字段、不新增索引，不涉及 `db/`、`server/db/` 或 migration。
- 本轮不修改后端业务代码。

### 编译与测试结果

- RED 验证：`npm.cmd test -- --run src/__tests__/utils/apiCache.test.js src/__tests__/api/versionLog.test.js src/__tests__/api/onboarding.test.js src/__tests__/api/community.test.js src/__tests__/views/InterviewReportView.test.js src/__tests__/views/InterviewSessionView.test.js` 在旧实现下失败，失败原因为缺少短 TTL、GET 未缓存、报告轮询未退避和开场白生成信号未停止。
- GREEN 验证：同一命令通过，6 个测试文件 / 78 个用例通过。
- 扩展验证：`npm.cmd test -- --run src/__tests__/api/performanceCache.test.js src/__tests__/layouts/MainLayout.test.js src/__tests__/views/HomePageView.test.js src/__tests__/views/community/PostDetailView.test.js src/__tests__/components/community/CommentSection.test.js` 中前 4 个相关测试文件通过；`CommentSection.test.js` 存在既有图片大小文案断言漂移，实际 `2MB`、旧断言 `5MB`，与本轮无关。
- 前端构建验证：`npm.cmd run build` 通过。

### 当前功能验收说明

- 敏感日志复查命令 `rg -n 'OSSAccessKeyId=[^*&\\s]{8,}|Signature=[^*&\\s]{8,}|Authorization: Bearer|\"messages\"\\s*:\\s*\\[|apiKey\\s*[:=]\\s*[^*\\s,;}&\"]{8,}|accessKeySecret\\s*[:=]\\s*[^*\\s,;}&\"]{8,}|accessKeyId\\s*[:=]\\s*[^*\\s,;}&\"]{8,}' logs debug.txt` 无命中。
- 重复访问关键词复查 `rg -n "Get latest version logs|查询引导状态|\\[社区\\] 查询帖子详情|\\[社区\\] 查询评论列表|获取会话轻量状态" logs/ai-resume.log` 当前仅命中历史样本中的 3 行；本轮未启动后端服务生成新访问日志。
- 前端已更新 `frontend/tasks/stage.md`，根目录 `tasks/stage.md` 同步记录本轮结果。

### 停止，不继续下一个功能

本轮只完成前端 P1-1 至 P1-6 重复访问降噪与轮询稳定化；不继续推进 P2 之外的新任务、后端接口、数据库结构或其它前端功能。

## 排查结论

- 当前日志样本未发现数据库硬错误：SQL 异常、死锁、连接超时、重复键异常、MyBatis `Preparing:` / `Parameters:` SQL 明细泄漏均未形成有效命中。
- 当前主要异常不是数据库本身故障，而是日志敏感信息泄漏、OSS 凭据异常、Redis 缓存短时失败后回源数据库，以及若干页面短时间重复读取。
- 敏感日志必须优先修复：日志中出现过完整 OSS 签名参数和完整 AI 请求正文，不能继续进入 `logs/` 或 `debug.txt`。
- 重复访问优化要先从前端短缓存、请求去重、轮询停表入手；已有后端轻量状态接口不应改回完整详情轮询。

## 已确认异常与证据

- `logs/ai-resume-error.log:14`、`logs/ai-resume.log:13` 出现 OSS 签名 URL 参数，包含完整 `OSSAccessKeyId` 等敏感查询参数。
- `debug.txt:67`、`logs/ai-resume.2026-06-04.0.log:779` 出现完整 AI 请求正文 JSON，包含简历、对话、系统 prompt 等业务敏感内容。
- OSS 访问出现 `InvalidAccessKeyId`，说明当前运行环境的 OSS AccessKey 与 Bucket/Endpoint 不匹配、已失效，或签名请求使用了错误凭据。
- Redis 缓存曾在 `logs/ai-resume.2026-06-04.0.log:83` 读取 `cache=sys_user` 失败并回源数据库，在 `logs/ai-resume.2026-06-04.0.log:200` 写缓存失败；这是性能退化信号，不是业务失败。
- `logs/ai-resume.log` 中短时间重复日志计数：
  - `获取会话轻量状态`: 21 次。
  - `Get latest version logs`: 12 次。
  - `查询引导状态`: 20 次。
  - `[社区] 查询帖子详情`: 11 次。
  - `[社区] 查询评论列表`: 11 次。
  - `Get current user info request`: 9 次，`Fetching user info`: 6 次。

## 前端文件定位

前端重复访问降噪详细计划见 `frontend/tasks/TASK_LOG_REPEAT_ACCESS_OPTIMIZATION_FRONTEND_PLAN.md`。

核心落点：

- `frontend/app/src/utils/apiCache.js`
- `frontend/app/src/api/versionLog.js`
- `frontend/app/src/api/onboarding.js`
- `frontend/app/src/api/community.js`
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/__tests__/utils/apiCache.test.js`
- `frontend/app/src/__tests__/views/InterviewReportView.test.js`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`

## 后端文件定位

- `server/src/main/resources/logback-spring.xml`
- `server/src/main/java/com/airesume/server/service/impl/OssServiceImpl.java`
- `server/src/main/java/com/airesume/server/config/OssConfig.java`
- `server/src/main/java/com/airesume/server/config/RedisCacheErrorHandler.java`
- `server/src/main/java/com/airesume/server/config/RedisConfig.java`
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/mapper/InterviewSessionMapper.java`
- `server/src/main/java/com/airesume/server/controller/VersionLogController.java`
- `server/src/main/java/com/airesume/server/service/impl/SysVersionLogServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/UserOnboardingServiceImpl.java`

建议新增后端测试：

- `server/src/test/java/com/airesume/server/infrastructure/logging/SensitiveLogMaskerTest.java`
- `server/src/test/java/com/airesume/server/service/impl/OssServiceImplTest.java`
- `server/src/test/java/com/airesume/server/config/RedisCacheErrorHandlerTest.java`
- 按实际改动扩展 `VersionLogControllerTest`、`SysVersionLogServiceImplTest`、`InterviewControllerTest`、`InterviewServiceTest`。

## 本轮修改文件清单

- 新增 `tasks/task-日志异常与重复访问优化修复计划.md`。
- 新增 `frontend/tasks/TASK_LOG_REPEAT_ACCESS_OPTIMIZATION_FRONTEND_PLAN.md`。

## 修复优先级

### P0-1 日志敏感信息脱敏

目标：

- 日志中不再出现完整 OSS 签名参数、Authorization、API Key、AccessKey、AI 请求正文、简历正文、对话正文和系统 prompt。
- 保留排障必要字段：请求来源、provider、baseUrl host、endpoint、model、configType、状态码、耗时、对象 key、错误类型。

后端实现方案：

- 新增 `SensitiveLogMasker`，集中处理日志消息脱敏，覆盖以下模式：
  - URL 查询参数：`OSSAccessKeyId`、`Signature`、`Expires`、`security-token`。
  - Header 或 JSON 字段：`Authorization`、`apiKey`、`api_key`、`accessKeyId`、`accessKeySecret`、`access-key-id`、`access-key-secret`。
  - AI 请求正文：命中 `"messages":[...]`、`"system"`、`"prompt"`、`"resumeText"`、`"diagnosisResult"` 等高风险字段时，将字段值替换为 `[REDACTED]` 或只保留长度。
- 新增 `SensitiveLogMessageConverter` 并在 `logback-spring.xml` 的 `CONSOLE`、`FILE`、`ERROR_FILE` pattern 中统一使用脱敏消息转换器。
- 搜索并消除显式打印完整 AI 请求体的代码或调试过滤器；若来源是框架 HTTP debug 日志，则在 `logback-spring.xml` 中将相关包级别固定为 `WARN`，避免 body 落盘。

测试与验证：

- `SensitiveLogMaskerTest#shouldMaskOssSignedUrlCredentials`：输入带 `OSSAccessKeyId`、`Signature`、`Expires` 的 URL，输出不能包含原始值。
- `SensitiveLogMaskerTest#shouldMaskAiRequestBodyMessages`：输入包含 `messages`、`system`、`user` 的 AI 请求 JSON，输出不能包含原始 prompt 或简历片段。
- `SensitiveLogMaskerTest#shouldKeepSafeOperationalFields`：provider、model、endpoint、status、elapsedMs 仍保留。
- 执行命令：
  - `cd server && mvn.cmd -q "-Dtest=SensitiveLogMaskerTest" test`
  - `cd server && mvn.cmd -q "-Dtest=ServerApplicationTests" test`

验收标准：

- 新日志中搜索以下模式无原文命中：
  - `OSSAccessKeyId=[^*&\s]{8,}`
  - `Signature=[^*&\s]{8,}`
  - `Authorization: Bearer`
  - `"messages"\s*:\s*\[`
  - 未脱敏的 `apiKey`、`accessKeySecret`。
- 错误日志仍能看出哪个模块、哪个对象 key、哪个 provider、哪个状态码失败。

### P0-2 OSS `InvalidAccessKeyId` 定位与安全化处理

目标：

- OSS 凭据错误时不泄漏签名 URL 和 AccessKey 原文。
- 服务端能明确区分“OSS 未启用”“OSS 配置缺失”“OSS 凭据被上游拒绝”。

后端实现方案：

- 在 `OssConfig` 增加配置有效性判断方法，`enabled=true` 时校验 endpoint、bucket、accessKeyId、accessKeySecret 都不为空。
- 在 `OssServiceImpl` 初始化日志中只打印脱敏后的 AccessKey 标识，例如前后各保留 3 到 4 位，中间固定 `****`。
- 在 `generateSignedUrl` 捕获 OSS 异常时，只记录 objectKey、bucket、错误码和脱敏凭据标识，不记录完整 URL、不记录完整签名参数。
- 在 `CommunityController` 图片代理失败时返回稳定业务错误，避免把上游签名 URL 或 OSS 错误详情透出到前端。

测试与验证：

- `OssServiceImplTest#shouldMaskAccessKeyWhenClientInitializationFails`：模拟初始化失败，日志或异常消息中不能包含完整 AccessKey。
- `OssServiceImplTest#shouldRejectEnabledOssWithMissingRequiredConfig`：`enabled=true` 且关键字段为空时快速失败并给出配置缺失原因。
- `CommunityControllerImageAccessTest` 增加签名失败分支，验证响应不包含签名 URL 参数。
- 执行命令：
  - `cd server && mvn.cmd -q "-Dtest=OssServiceImplTest,CommunityControllerImageAccessTest" test`

运维验收：

- 核对当前环境变量 `OSS_ACCESS_KEY_ID`、`OSS_ACCESS_KEY_SECRET`、`OSS_BUCKET_NAME`、`OSS_ENDPOINT` 是否与同一个阿里云账号和 Bucket 匹配。
- 如果 `InvalidAccessKeyId` 仍出现，优先轮换 OSS AccessKey，并确认 Bucket 所属账号和 RAM 权限，而不是在代码中绕过错误。

### P1-1 面试轻量状态轮询降噪

目标：

- 保留 `GET /api/interview/session/{sessionId}/status` 轻量接口。
- 前端轮询不重叠、不在报告已就绪后继续请求、不因组件重复挂载创建多个定时器。
- 后端日志从高频 `info` 降为更适合排查的 `debug` 或按状态变化记录，减少日志噪音。

现有后端链路：

- `InterviewController.getSessionStatus`: `server/src/main/java/com/airesume/server/controller/InterviewController.java:267`
- `InterviewService.getSessionStatus`: `server/src/main/java/com/airesume/server/service/InterviewService.java:375`
- `InterviewSessionMapper.selectOwnedStatus`: `server/src/main/java/com/airesume/server/mapper/InterviewSessionMapper.java:54`

后端实现方案：

- `InterviewController` 高频状态查询日志改为 `debug`；仅在状态变化、访问拒绝、会话不存在时保留 `info` 或 `warn`。
- 保持 `selectOwnedStatus` 只查轻量字段，不增加聊天记录、报告 JSON、大文本字段。
- 若前端仍因多处入口重复轮询同一个 sessionId，再考虑服务端短 TTL 本地去重缓存，但本轮不优先做服务端状态缓存，避免影响报告就绪实时性。

测试与验证：

- 扩展 `InterviewControllerTest`，验证状态接口仍返回轻量响应包络。
- 扩展 `InterviewServiceTest`，验证报告存在时 `reportReady=true`，未加载完整报告正文。
- 执行命令：
  - `cd server && mvn.cmd -q "-Dtest=InterviewControllerTest,InterviewServiceTest" test`

验收标准：

- 同一报告页等待报告生成时，状态请求不并发重叠。
- 报告就绪后只触发一次完整详情读取，随后停止轮询。
- `logs/ai-resume.log` 中 `获取会话轻量状态` 不再作为高频 `info` 噪音出现。

### P1-2 版本日志与引导状态后端缓存兜底

目标：

- 前端先做短缓存和 pending 复用；后端再对稳定读接口加缓存兜底，防止多页面或刷新导致重复打 DB。
- 写操作必须主动驱逐缓存，避免页面展示旧配置。

后端实现方案：

- `SysVersionLogServiceImpl` 的最新版本日志查询增加 `@Cacheable(sync = true)`，缓存名建议 `config:versionLogs`，key 包含安全 limit。
- `RedisConfig.initialCacheConfigurations` 注册 `config:versionLogs`，TTL 建议 5 分钟。
- 管理端版本日志新增、更新、删除、发布操作时驱逐 `config:versionLogs`。
- `UserOnboardingServiceImpl.getStatus` 如确认返回只依赖用户引导状态，可增加用户级缓存 `user:onboardingStatus`，key 使用 `userId + "::" + guideKey`，TTL 建议 60 秒。
- `UserOnboardingServiceImpl.updateStatus`、任务完成等写操作主动驱逐 `user:onboardingStatus`。

测试与验证：

- 扩展 `SysVersionLogServiceImplTest`，验证重复查询使用缓存语义或通过 mapper 调用次数锁定。
- 扩展 `VersionLogControllerTest`，验证 limit 仍被安全归一化。
- 扩展 `UserOnboardingServiceImplTest`，验证状态更新后缓存驱逐路径被触发。
- 扩展 `RedisConfigTest`，验证新增缓存名 TTL 已注册。
- 执行命令：
  - `cd server && mvn.cmd -q "-Dtest=SysVersionLogServiceImplTest,VersionLogControllerTest,UserOnboardingServiceImplTest,RedisConfigTest" test`

验收标准：

- 首页短时间重复进入不再多次查询版本日志表。
- 主布局重复挂载或路由切换不再连续读取引导状态表。
- 管理端更新版本日志或用户完成引导后，下一次读取能看到最新状态。

### P2-1 Redis 缓存失败可观测性与回源压力保护

目标：

- Redis 失败时继续保持业务可用，但日志要能定位 cacheName、key、根因类型和是否已回源。
- 避免 Redis 短时故障时同一热点 key 被大量请求同时打穿到数据库。

后端实现方案：

- 保留 `RedisCacheErrorHandler` 的业务降级策略，不在业务层新增重复兜底。
- 日志中增加根因类型字段，例如 `RedisCommandInterruptedException`、`SerializationException`、`RedisConnectionFailureException`。
- 对同一 `cacheName + key + exceptionType` 的告警做短窗口限频，避免 Redis 故障时错误日志被刷爆。
- 对已使用 `@Cacheable(sync = true)` 的热点接口保持同步加载；新增缓存时优先启用 `sync = true`。
- 对 `sys_user` 缓存失败做一次配置复核：确认 `RedisConfig` 的 key/value 序列化、TTL、历史脏缓存清理路径符合当前对象结构。

测试与验证：

- `RedisCacheErrorHandlerTest#shouldLogCacheAndRootCauseWithoutThrowing`：模拟 cache get/put 失败，handler 不抛出业务异常。
- `RedisCacheErrorHandlerTest#shouldThrottleRepeatedWarningsForSameCacheKey`：同一 key 连续失败只记录受控数量的 warn。
- `RedisConfigTest` 验证 `sys_user`、`auth:userInfo`、`user:quota` 等热点缓存 TTL 保持注册。
- 执行命令：
  - `cd server && mvn.cmd -q "-Dtest=RedisCacheErrorHandlerTest,RedisConfigTest" test`

验收标准：

- Redis 中断时业务仍可回源，但同一问题不会产生大量重复 warn。
- 能从一条日志判断是连接失败、命令中断、序列化不兼容还是其他异常。
- Redis 恢复后，热点接口不再持续落回数据库。

## 数据存储方案

- 本计划默认不新增表、不修改字段、不新增索引。
- 如后续执行中发现慢查询证据，再单独建立索引优化任务，并同步更新 `db/` 与 `server/db/` SQL 脚本。
- 本轮不调整主链路数据库结构，不修改面试、社区、版本日志、引导状态的表结构。

## 编译与构建计划

后端每个轮次至少执行：

- `cd server && mvn.cmd -q "-Dtest=<本轮相关测试类>" test`
- `cd server && mvn.cmd -q -DskipTests compile`

涉及前端重复请求修复的轮次执行：

- `cd frontend/app && npm.cmd test -- --run <本轮相关测试文件>`
- `cd frontend/app && npm.cmd run build`

最终整体验收建议执行：

- `cd server && mvn.cmd -q test`
- `cd frontend/app && npm.cmd test -- --run`
- `cd frontend/app && npm.cmd run build`

## 当前功能验收说明

本轮仅完成修复计划落档，未修改业务代码，未执行编译、测试或构建。

后续执行完成后，用以下日志复查命令验证问题是否消失：

- `rg -n "OSSAccessKeyId=[^*&\\s]{8,}|Signature=[^*&\\s]{8,}|Authorization: Bearer|\\\"messages\\\"\\s*:\\s*\\[" logs debug.txt`
- `rg -n "获取会话轻量状态|Get latest version logs|查询引导状态|\\[社区\\] 查询帖子详情|\\[社区\\] 查询评论列表" logs/ai-resume.log`
- `rg -n "读取缓存失败|写入缓存失败|cache=sys_user|Redis.*interrupted|Redis.*timeout" logs`

预期：

- 第一条命令不再命中未脱敏敏感内容。
- 第二条命令中重复请求数量相比当前样本明显下降，且轮询在状态完成后停止。
- 第三条命令如仍命中 Redis 失败，应包含明确根因类型，并且不形成同一 key 的大量重复 warn。

## 停止，不继续下一功能

本轮只制定并保存修复计划，不开始 P0/P1/P2 任何代码实现。后续需按优先级单独确认执行，完成一轮后更新对应 task / stage 文档并停止等待验收。

## P1-3 残留热点与 SSE 断连降噪执行记录（2026-06-06）

### 本轮修改文件清单

- 修改 `frontend/app/src/api/notification.js`。
- 修改 `frontend/app/src/api/publicVersionLog.js`。
- 修改 `frontend/app/src/utils/apiCache.js`。
- 修改 `frontend/app/src/__tests__/api/performanceCache.test.js`。
- 修改 `frontend/app/src/__tests__/api/versionLog.test.js`。
- 修改 `server/src/main/java/com/airesume/server/service/NotificationService.java`。
- 修改 `server/src/main/java/com/airesume/server/common/exception/GlobalExceptionHandler.java`。
- 修改 `server/src/main/java/com/airesume/server/config/SecurityConfig.java`。
- 修改 `server/src/test/java/com/airesume/server/service/NotificationServiceTest.java`。
- 修改 `server/src/test/java/com/airesume/server/common/exception/GlobalExceptionHandlerTest.java`。
- 修改 `server/src/test/java/com/airesume/server/config/SecurityConfigTest.java`。
- 更新 `tasks/task-日志异常与重复访问优化修复计划.md`、`tasks/stage.md`、`frontend/tasks/TASK_LOG_REPEAT_ACCESS_OPTIMIZATION_FRONTEND_PLAN.md` 与 `frontend/tasks/stage.md`。

### 后端实现方案

- `NotificationService` 将浏览器刷新、切页或网络主动关闭 SSE 触发的常见断连 IOException 识别为正常生命周期，日志由 WARN 降为 DEBUG；非客户端主动断连仍保留 WARN。
- `GlobalExceptionHandler` 对同类 SSE/长连接 IOException 统一降为 DEBUG，避免 `logs/ai-resume.log` 中把用户关闭页面误判为异常。
- `SecurityConfig` 提供空 `UserDetailsService`，项目继续使用 JWT 过滤器认证，不启用 Spring Boot 默认表单用户，避免开发启动时反复打印 `Using generated security password` 干扰日志排查。

### 前端实现方案

- `getNotifications(params)` 增加 15 秒短缓存与 pending 复用，按分页、筛选条件隔离 cache key；通知写操作继续清理 `notification` 前缀缓存。
- `getPublicVersionLogsPage(params)` 增加版本日志分页短缓存，按 page/size 隔离 cache key，减少版本日志页短时间重复打开时的后端读取。
- `API_CACHE_TTL` 新增 `NOTIFICATION_LIST`，仅用于明确稳定的通知列表 GET，不缓存 SSE、写操作或状态轮询。

### 数据存储方案

- 本轮不新增表、不修改字段、不新增索引，不涉及 `db/` 或 `server/db/` SQL 脚本。
- 本轮只处理已确认的残留重复 GET 与日志噪声，不修改接口协议或数据库结构。

### 编译与测试结果

- RED 验证：`npm.cmd test -- --run src/__tests__/api/performanceCache.test.js src/__tests__/api/versionLog.test.js` 在旧实现下失败，失败原因为通知列表与公开版本日志分页没有复用缓存。
- RED 验证：`mvn.cmd -q "-Dtest=NotificationServiceTest,SecurityConfigTest" test` 在旧实现下失败，失败原因为 `SecurityConfig.emptyUserDetailsService()` 不存在；补充 SSE 断连日志测试后，旧错误分支仍会记录 WARN。
- GREEN 验证：`npm.cmd test -- --run src/__tests__/api/performanceCache.test.js src/__tests__/api/versionLog.test.js` 通过，2 个测试文件 / 15 个用例。
- GREEN 验证：`mvn.cmd -q "-Dtest=NotificationServiceTest,SecurityConfigTest,GlobalExceptionHandlerTest" test` 通过。
- 后端目标回归与 Spring 上下文验证：`mvn.cmd -q "-Dtest=NotificationServiceTest,SecurityConfigTest,GlobalExceptionHandlerTest,ServerApplicationTests" test` 通过；`ServerApplicationTests` 启动日志未再出现 `Using generated security password`。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 前端构建验证：`npm.cmd run build` 通过。
- 日志关键词复查：`server/logs/ai-resume.log` 最新 16:11 样本未出现新的 generated password 警告，SSE 客户端主动断连记录为 DEBUG；敏感 AI 请求体命中仍来自 `2026-06-04` 归档旧日志，属于历史存量。

### 当前功能验收说明

- 最新日志样本未发现数据库硬错误、SQL 异常、死锁、连接池超时或 MyBatis SQL 明细泄露。
- 当前残留异常主要是开发环境启动默认密码警告和 SSE 客户端主动断连 WARN；本轮已分别处理。
- 当前残留重复访问主要是通知列表和公开版本日志分页；本轮已加前端短缓存兜底。
- 历史归档日志中仍有修复前的敏感 AI 请求体和 OSS AccessKey 摘要记录，属于旧日志存量，不代表当前代码仍会继续输出；如要完全消除风险，需要单独清理或缩短历史日志保留周期。

### 停止，不继续下一个功能

本轮只完成 P1-3「残留热点与 SSE 断连降噪」，不继续推进新的数据库索引、后端聚合接口、通知列表后端缓存或社区详情查询合并。
