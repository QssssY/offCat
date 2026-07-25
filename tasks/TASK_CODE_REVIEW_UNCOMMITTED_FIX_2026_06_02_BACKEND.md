# 未提交改动代码审查问题修复后端

## 当前任务所属模块

- 后端模块：认证验证码、模拟面试流式消息、平台 AI 兜底额度扣减。
- 触发原因：本轮代码审查发现验证码测试注入缺失、验证码消费存在并发复用窗口、流式平台兜底额度不足时可能先保存用户消息。

## 前端文件定位

- 前端配套修复见 `frontend/tasks/TASK_CODE_REVIEW_UNCOMMITTED_FIX_2026_06_02_FRONTEND.md`。

## 后端文件定位

- `server/src/main/java/com/airesume/server/service/impl/CaptchaServiceImpl.java`
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/test/java/com/airesume/server/service/impl/AuthServiceImplTest.java`
- `server/src/test/java/com/airesume/server/service/impl/CaptchaServiceImplTest.java`
- `server/src/test/java/com/airesume/server/controller/InterviewControllerTest.java`

## 本轮修改文件清单

- `CaptchaServiceImpl.java`：验证码校验改为 Redis 原子消费。
- `InterviewController.java`：流式平台 fallback 额度校验和扣减前置到用户消息落库之前。
- `AuthServiceImplTest.java`：补齐 `CaptchaService` mock 和注册/登录/重置密码验证码字段。
- `CaptchaServiceImplTest.java`：新增验证码原子消费回归测试。
- `InterviewControllerTest.java`：新增流式 fallback 扣费顺序回归测试。

## 前端实现方案

- 无后端侧前端实现，前端设置页修复另行记录。

## 后端实现方案

- 验证码校验使用 `StringRedisTemplate.opsForValue().getAndDelete(key)` 一次性读取并删除验证码，消除 `get` 后再 `delete` 的并发复用窗口。
- 注册、登录失败后触发的渐进式验证码、重置密码流程均继续走 `CaptchaService.verify(...)`，测试显式补齐依赖和验证码字段。
- 流式面试消息在自定义 AI 不命中、且用户显式 `fallbackToPlatform=true` 时，先调用 `interviewService.chargePlatformFallbackQuotaIfNeeded(...)`，额度通过后才调用 `saveUserMessage(...)`。
- 保留非流式和流式共用的兜底扣费 Service 入口，不新增数据库结构或新计费类型。

## 数据存储方案

- 不修改数据库结构。
- 验证码仍使用 `captcha:{captchaId}` Redis key，TTL 仍为 5 分钟。
- 面试平台 fallback 计费继续使用既有 `interview_session.ai_billing_source` 记录本会话是否已从用户自定义 AI 切到平台兜底。

## stage 更新说明

- 已在 `tasks/stage.md` 顶部记录本轮代码审查后端修复、验证命令和停止边界。

## 编译结果

- `mvn.cmd test` 通过，658 个用例全绿。

## 构建结果

- 后端无单独前端构建；后端完整测试已通过。

## 当前功能验收说明

- 验证码成功或失败都会原子消费 Redis 中的验证码，错误验证码不能被反复尝试，同一个 captchaId 不能被并发复用。
