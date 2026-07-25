# 用户 Key 本地化与简历数据脱敏合规改造后端（2026-06-07）

## 回归修复补充（2026-06-07）

- 本轮继续在 `compliance/remove-community-membership` 分支做最小回归修复，不切主分支、不提交 commit、不新增历史数据清理 migration。
- 启动修复：`UserAiConfigResolverImpl`、`DirectProcessRouter` 已固定 Spring 注入构造器；启动测试继续暴露的 `InterviewController`、`ResumeDiagnosisController` 同类多构造器问题也已修正为唯一 `@Autowired` 生产构造器，测试兼容构造器委托到同一运行时上下文。
- 直连容量修复：自定义 Key 简历诊断在预占直连槽后，额度检查失败、任务保存失败或事务回滚都会释放预留槽；提交成功后由直连提交消费该预留槽，自定义 Key 任务仍不入 MQ、不回退平台。
- 源 PDF 重试修复：`ResumeDiagnosisProcessor` 仅在诊断成功完成后清理源 PDF；失败任务保留源文件供 24 小时内重试，重试窗口过期时清理源文件并清空 `file_url`，不删除脱敏后的任务记录。
- 本轮不做历史数据清理：部署数据库为空，旧库测试数据不需要 migration 或脚本清理。
- 回归验证：`mvn.cmd -q -Dtest=ServerApplicationTests test` 通过；`mvn.cmd -q "-Dtest=UserAiConfigServiceImplTest,UserAiConfigResolverImplLocalRequestTest,ResumePrivacySanitizerTest,ResumeDiagnosisProcessorTest,DirectProcessRouterTest,InterviewControllerTest,InterviewServiceTest,UserTtsSpeechServiceImplTest,ResumeJobMatchServiceImplTest,ResumePolishServiceImplTest,MockInterviewJobTargetServiceImplTest,ResumeDiagnosisTaskServiceImplTest,UserDataRetentionCleanupServiceImplTest" test` 通过；`mvn.cmd -q -DskipTests compile` 通过。
- 安全复查：`rg -n "sk-user-real|sk-local-secret|sk-local|tts-local|138-0000-0000|13800000000|zhangsan@example\\.com|zhangsan\\.dev|张三" server/src/main` 无命中。

## 当前任务所属模块

- 后端模块：用户自定义 AI/TTS 配置接口、简历诊断、JD 匹配、AI 润色、模拟面试、面试 TTS、简历隐私脱敏。
- 前端模块：本轮前端改动单独记录在 `frontend/tasks/TASK_88_COMPLIANCE_LOCAL_KEYS_RESUME_PRIVACY_FRONTEND.md`。

## 前端文件定位

前端已在独立任务中实现浏览器本地配置中心、请求临时传参和隐私提示，本后端任务仅记录接口接收、运行时解析和服务端脱敏边界。

## 后端文件定位

- `server/src/main/java/com/airesume/server/dto/user/ClientAiConfigRequest.java`
- `server/src/main/java/com/airesume/server/dto/user/ClientTtsConfigRequest.java`
- `server/src/main/java/com/airesume/server/service/RuntimeUserAiConfigContext.java`
- `server/src/main/java/com/airesume/server/service/ResumePrivacySanitizer.java`
- `server/src/main/java/com/airesume/server/service/ResumeDiagnosisTaskService.java`
- `server/src/main/java/com/airesume/server/service/impl/UserAiConfigServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/UserAiConfigResolverImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisProcessor.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeJobMatchServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumePolishServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/MockInterviewJobTargetServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/UserTtsSpeechServiceImpl.java`
- `server/src/main/java/com/airesume/server/controller/ResumeDiagnosisController.java`
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`

## 本轮修改文件清单

- 新增 `ClientAiConfigRequest`、`ClientTtsConfigRequest`，仅用于单次请求内临时配置。
- 新增 `RuntimeUserAiConfigContext`，统一承载请求内用户自定义 AI/TTS 配置。
- 新增 `ResumePrivacySanitizer`，保存和响应前脱敏手机号、邮箱、身份证、详细地址、URL、微信、QQ 等字段。
- 调整 `UserAiConfigServiceImpl`，退役用户 Key 服务端持久化语义，保存、删除、启停接口不再写入 `user_ai_config`。
- 调整简历诊断、JD 匹配、AI 润色、面试创建/发消息/流式消息、TTS 合成链路，优先使用请求内临时配置，缺失时继续走平台 AI/TTS。
- 调整简历解析缓存链路，不再把完整 `resume_text`、`source_resume_text`、JD 原文快照或源 PDF 作为可恢复用户简历长期保存。
- 调整 mock 示例内容，把看似真实的姓名、电话、邮箱替换为脱敏占位。

## 前端实现方案

本后端任务不直接实现前端。前端通过 `localStorage` 保存用户自定义 AI/TTS 配置，并在诊断、面试、测试或试听请求中附带 `clientAiConfig` / `clientTtsConfig`，详见前端任务文档。

## 后端实现方案

- 用户 Key 不落库：用户自定义 AI/TTS 配置接口保留兼容响应，但不再持久化 Key、Base URL、模型等用户自定义配置；业务调用时只读取本次请求体或 multipart 中的临时配置。
- 临时配置优先：`UserAiConfigResolverImpl` 支持从 `RuntimeUserAiConfigContext` 解析请求内配置，业务链路缺失临时配置时回退平台 AI，不把用户 Key 写入数据库、Redis、文件或日志。
- 自定义 Key 简历诊断繁忙边界：使用本地 Key 的简历诊断只允许当前进程直接处理；无法立即直连处理时返回“当前自定义 Key 诊断繁忙，请稍后重试”，不入 MQ、不自动回退平台、不保存 Key。
- 简历脱敏保存：简历诊断结果、JD 匹配记录和 AI 润色记录保存前先脱敏；历史详情响应不回传完整原文；旧记录兼容读取仅用于过渡，新任务不再持久化可恢复完整简历原文。
- 源文件临时化：现有上传架构仍会短暂接收 PDF 文件作为处理输入，任务处理完成或自定义 Key 直连失败后立即清理源文件，不作为历史资料留存。
- 管理端不受影响：管理员系统 AI/TTS 配置持久化保持不变，管理员调整用户额度能力保持不变。

## 数据存储方案

- 本轮不新增历史数据清理 migration，不执行存量数据清理；原因是部署环境数据库为空，不存在需要清理的历史用户 Key 或历史简历数据。
- `user_ai_config` 表和既有 schema 暂保留兼容旧结构，但用户侧接口不再写入用户自定义 Key。
- 用户自定义 AI 使用统计继续只记录 userId、usageType、日期、次数，不记录 Key、Base URL、模型或简历原文。
- 简历报告保留任务 ID、状态、评分、问题、建议、脱敏摘要、脱敏结果、时间等非敏感元数据。

## stage 更新说明

- 已在 `tasks/stage.md` 顶部新增“用户 Key 本地化与简历数据脱敏合规改造后端”记录。
- 前端 stage 另在 `frontend/tasks/stage.md` 顶部新增对应记录。

## 编译结果

- 后端编译：`mvn.cmd -q -DskipTests compile` 通过。

## 构建结果

本后端任务不涉及前端构建，前端构建结果记录在前端任务文档。

## 测试结果

- 后端目标回归：`mvn.cmd -q "-Dtest=UserAiConfigServiceImplTest,UserAiConfigResolverImplLocalRequestTest,ResumePrivacySanitizerTest,ResumeDiagnosisProcessorTest,DirectProcessRouterTest,InterviewControllerTest,InterviewServiceTest,UserTtsSpeechServiceImplTest,ResumeJobMatchServiceImplTest,ResumePolishServiceImplTest,MockInterviewJobTargetServiceImplTest,ResumeDiagnosisTaskServiceImplTest" test` 通过。
- 关键词复查：`rg -n "sk-user-real|sk-local-secret|sk-local|tts-local|138-0000-0000|13800000000|zhangsan@example\\.com|zhangsan\\.dev|张三" src\main` 无命中。

## 当前功能验收说明

- 用户自定义 Key 不再通过用户配置接口保存到服务器。
- 业务请求可临时携带用户本地 Key，服务端只用于本次代理调用第三方。
- 简历诊断、JD 匹配、AI 润色和面试上下文不再新增保存可恢复完整简历原文。
- 源 PDF 文件处理后清理，历史详情只保留脱敏报告能力。
- 管理员系统 AI/TTS 配置和用户额度管理保持可用。

## 停止，不继续下一个功能

本轮只完成用户自定义 Key 本地化、请求内临时配置传递和简历数据脱敏保存边界；不继续推进历史数据清理、不新增迁移脚本、不重构数据库结构、不扩展支付/订单/会员或社区其它功能。
