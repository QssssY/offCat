# 滥用风险治理后端加固（2026-06-05）

## 当前任务所属模块

- 后端模块：社区图片上传与访问、社区发帖/评论图片绑定、社区文本审核、注册后平台 AI 免费额度消费、模拟面试 AI 调用与日志。
- 前端模块：本轮不修改前端页面或接口调用形态，仅依赖后端继续返回既有状态码和审核状态。

## 前端文件定位

本轮未修改前端实现。若后续验收发现现有 UI 对 `401`、`403` 或 `pending` 展示不足，再单独开前端任务处理。

## 后端文件定位

- `server/src/main/java/com/airesume/server/config/SecurityConfig.java`
- `server/src/main/java/com/airesume/server/controller/CommunityController.java`
- `server/src/main/java/com/airesume/server/service/CommunityService.java`
- `server/src/main/java/com/airesume/server/service/CommunityTextModerationService.java`
- `server/src/main/java/com/airesume/server/service/CommunityImageRegistryService.java`
- `server/src/main/java/com/airesume/server/scheduler/CommunityImageCleanupScheduler.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/UserQuotaServiceImpl.java`

## 本轮修改文件清单

- `server/src/main/java/com/airesume/server/config/SecurityConfig.java`
- `server/src/main/java/com/airesume/server/controller/CommunityController.java`
- `server/src/main/java/com/airesume/server/entity/CommunityImage.java`
- `server/src/main/java/com/airesume/server/mapper/CommunityImageMapper.java`
- `server/src/main/java/com/airesume/server/scheduler/CommunityImageCleanupScheduler.java`
- `server/src/main/java/com/airesume/server/service/CommunityImageRegistryService.java`
- `server/src/main/java/com/airesume/server/service/CommunityService.java`
- `server/src/main/java/com/airesume/server/service/CommunityTextModerationService.java`
- `server/src/main/java/com/airesume/server/service/OssService.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/OssServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/UserQuotaServiceImpl.java`
- `server/src/main/resources/application.yml`
- `server/src/main/resources/application-dev.yml`
- `server/src/main/resources/application-prod.yml`
- `server/db/schema.sql`
- `server/db/migrations/TASK_86_SECURITY_ABUSE_HARDENING.sql`
- `db/schema.sql`
- `db/migrations/TASK_86_SECURITY_ABUSE_HARDENING.sql`
- `server/src/test/java/com/airesume/server/config/SecurityConfigTest.java`
- `server/src/test/java/com/airesume/server/controller/CommunityControllerImageAccessTest.java`
- `server/src/test/java/com/airesume/server/db/SchemaConsistencyTest.java`
- `server/src/test/java/com/airesume/server/service/CommunityImageRegistryServiceTest.java`
- `server/src/test/java/com/airesume/server/service/CommunityServiceImageBindingTest.java`
- `server/src/test/java/com/airesume/server/service/CommunityServiceModerationTest.java`
- `server/src/test/java/com/airesume/server/service/CommunityTextModerationServiceTest.java`
- `server/src/test/java/com/airesume/server/service/impl/InterviewAiServiceImplTest.java`
- `server/src/test/java/com/airesume/server/service/impl/UserQuotaServiceImplTest.java`

## 前端实现方案

本轮不做前端变更。后端保持现有接口路径和请求字段，发帖/评论继续接收既有图片 URL 列表；当后端返回未登录、禁止访问或审核中状态时，复用现有前端错误处理和审核状态展示。

## 后端实现方案

- 图片上传鉴权：`SecurityConfig` 只公开 `GET /api/community/images/community/**`，匿名 `POST /api/community/images/upload` 不再命中公共放行规则。
- 图片访问防盗链：`CommunityController.getImage` 在生产默认拒绝缺失、解析失败或非允许域名 `Referer`；`application-dev.yml` 允许本地调试缺失 `Referer` 和 localhost 来源。
- 图片绑定登记：新增 `community_image` 表与 `CommunityImageRegistryService`，上传成功记录 `uploaded`；发帖或评论时只允许绑定当前用户上传且未绑定的图片，并通过 `status=uploaded` 条件原子更新为 `bound`，防止并发重复绑定。
- 图片清理：新增 `CommunityImageCleanupScheduler`，定期清理超过 24 小时未绑定图片，并通过 `OssService.deleteObject` 删除 OSS 对象。
- 评论广告治理：`CommunityTextModerationService` 将疑似广告词、URL、手机号、微信号样式和联系方式引导改为 `pending`，进入人工审核池，不直接公开。
- 注册薅额度治理：新增 `app.security.new-user-ai-cooldown-minutes`，生产默认 30 分钟；`UserQuotaServiceImpl` 在扣减平台免费面试/简历额度前检查用户创建时间，冷却期内拒绝高成本 AI 消耗，VIP 日额度不受影响。
- AI 面试防提示词泄露：`InterviewAiServiceImpl` 对索要系统提示词、内部规则、developer message、ignore previous 等输入直接安全转向；非流式结果做泄露标记扫描并替换；流式输出保留短尾缓冲扫描，避免跨 chunk 泄露。
- AI 日志脱敏：移除完整请求体 JSON 日志，改为记录模型、消息数量、角色分布和长度摘要，避免系统提示词、简历内容和候选人个人信息进入日志。

## 数据存储方案

- 新增 `community_image` 表，字段包括 `user_id`、`object_key`、`proxy_url`、`status`、`bound_type`、`bound_id`、`create_time`、`update_time`。
- `status` 使用 `uploaded` 和 `bound` 表达图片生命周期；`bound_type` 区分 `post` 与 `comment`。
- 根目录 `db/schema.sql`、后端目录 `server/db/schema.sql` 以及两边同名 migration 已同步，保证本地初始化和生产迁移入口一致。

## stage 更新说明

- 已在 `tasks/stage.md` 追加“滥用风险治理后端加固”记录，包含修复范围、验证命令和停止边界。
- 代码审查修复已补充到 `tasks/stage.md`：覆盖 AI 空响应非 null 合约回归与微信联系方式空格格式识别回归。
- 本轮无前端修改，因此不更新 `frontend/tasks/stage.md`。

## 编译结果

- 后端编译：`mvn.cmd -q -DskipTests compile` 通过。

## 构建结果

本轮未修改前端，不执行前端构建。

## 测试结果

- 目标回归：`mvn.cmd -q "-Dtest=SecurityConfigTest,CommunityControllerImageAccessTest,CriticalEndpointRateLimitFilterTest,CommunityTextModerationServiceTest,CommunityServiceModerationTest,CommunityServiceValidationTest,CommunityImageRegistryServiceTest,CommunityServiceImageBindingTest,InterviewAiServiceImplTest,AuthServiceImplTest,UserQuotaServiceImplTest,SchemaConsistencyTest" test` 通过。
- 代码审查修复回归：`mvn.cmd "-Dtest=InterviewAiServiceImplTest#shouldReturnEmptyStringWhenPromptLeakOutputIsNull,CommunityTextModerationServiceTest#shouldRouteSpacedWechatIdWithoutOtherSuspiciousWordsToPendingReview" test` 通过。
- 覆盖场景包括匿名上传不再公开、生产防盗链拒绝异常 `Referer`、未绑定图片清理、并发状态已变时拒绝重复绑定、广告评论进入待审、新用户 AI 冷却、面试提示词索取拦截、AI 日志不记录完整 messages、schema 和 migration 同步。

## 当前功能验收说明

- 匿名用户不能再上传社区图片。
- 已上传但未绑定的图片会进入登记表，并在超时后被清理，降低被当作在线图床的风险。
- 外站或缺失 `Referer` 的图片访问在生产配置下默认返回 `403`。
- 评论区疑似广告、联系方式和导流内容默认进入人工审核，不直接展示。
- 新注册账号在生产冷却期内不能立刻消耗平台免费 AI 面试/简历额度。
- 模拟面试中索要系统提示词、内部规则或开发者消息不会返回内部提示内容。
- 面试 AI 日志只保留摘要，不再输出完整请求消息体。

## 停止，不继续下一个功能

本轮仅完成计划内 P1 滥用面与 P2 新用户 AI 冷却治理；不新增短信/邮箱验证、前端交互改造、图片 AI 审核、举报系统、信誉分、CDN 或更复杂的反作弊系统。
