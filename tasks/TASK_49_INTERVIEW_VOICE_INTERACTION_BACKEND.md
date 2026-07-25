# TASK 49 模拟面试语音通话后端

## 当前任务所属模块

模拟面试模块，支持会话创建时选择文字面试或语音面试，并让语音模式影响 AI Prompt 输出风格。

## 前端文件定位

前端实现见 `frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`。

## 后端文件定位

- `server/src/main/java/com/airesume/server/entity/InterviewSession.java`
- `server/src/main/java/com/airesume/server/dto/interview/*`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/InterviewAiService.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/MockInterviewAiServiceImpl.java`
- `db/migrations/TASK_49_INTERVIEW_VOICE_INTERACTION.sql`
- `server/db/migrations/TASK_49_INTERVIEW_VOICE_INTERACTION.sql`
- `server/db/schema.sql`

## 本轮修改文件清单

- 新增 `interview_session.interaction_type` 迁移脚本。
- `InterviewConstants` 新增文字/语音交互方式常量和合法值判断。
- `InterviewSession`、`CreateSessionRequest`、`InterviewSessionResponse`、`InterviewHistoryResponse` 增加 `interactionType`。
- `InterviewService` 在创建会话时校验并保存交互方式，在会话详情和历史记录中返回该字段。
- `InterviewAiService`、真实 AI 实现和 Mock AI 实现增加 `interactionType` 参数，语音模式追加口语化、简洁、适合朗读的 Prompt 指令。
- 补充后端单元测试覆盖默认文字、语音落库、非法值拒绝、详情/历史回显和 Prompt 分支。
- 追加修复 `MockInterviewService`：Mock 追问不再把简历原文、文件名、姓名、性别等元信息拼进聊天气泡或语音播报内容。
- 追加修复 `InterviewAiServiceImpl`：真实 AI Prompt 移除带具体姓名的负例样本，并明确禁止向候选人展示或朗读简历元信息。
- 新增 `MockInterviewServiceTest`，补充 `InterviewAiServiceImplTest` 回归用例，覆盖简历元信息不外露。

## 后端实现方案

交互方式在创建会话时确定，空值默认文字面试，非 `0/1` 直接抛出业务异常。AI 调用不在实现层临时查会话，而是由主服务层显式传入 `interactionType`，保证 Prompt 分支来源清晰。语音模式只调整输出口径，不改变 SSE 数据结构、消息落库和报告生成链路。

追加修复中，Mock 面试只根据简历内容判断“项目经历 / 实习经历 / 工作经历 / 相关经历”等泛化类型，不再截取简历原文作为问题锚点。真实 AI 侧保留简历上下文供模型参考，但在系统 Prompt 中明确要求不能输出或朗读简历字段串、文件名和个人元信息。

## 数据存储方案

在 `interview_session` 表新增 `interaction_type TINYINT NOT NULL DEFAULT 0`，默认文字面试，兼容历史数据。不新增索引，不保存通话时长。

## stage 更新说明

已同步更新 `runtime/STATE.md`，记录本轮后端完成状态、验证结果和停止边界。

## 编译结果

`mvn.cmd test` 已完成编译阶段并通过。

## 构建/测试结果

- `mvn.cmd test '-Dtest=InterviewServiceTest,InterviewAiServiceImplTest,MockInterviewAiServiceImplTest'` 通过，54 个测试通过。
- `mvn.cmd test` 通过，378 个测试通过。
- 追加修复目标测试：`mvn.cmd test "-Dtest=MockInterviewServiceTest,InterviewAiServiceImplTest"` 通过，31 个测试通过。
- 追加修复后端完整测试：`mvn.cmd test` 通过，381 个测试通过。

## 当前功能验收说明

- 创建文字会话时 `interactionType` 默认返回 `0`。
- 创建语音会话时 `interactionType` 保存并回显为 `1`。
- 非法交互方式会被拒绝。
- 语音模式 AI Prompt 包含口语化、简洁、适合朗读约束，文字模式不包含。
- Mock 或真实 AI Prompt 均不得把简历原文片段、文件名、姓名、性别、电话、邮箱等元信息展示给用户或交给 TTS 播报。

## 停止说明

本轮只完成模拟面试语音通话的后端最小支撑，不接入第三方 STT/TTS、不保存通话时长、不扩展实时音视频能力。
