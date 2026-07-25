# 任务：代码审查问题修复（第三轮）

## 当前任务所属模块
- 前后端代码审查问题修复

## 本轮修改文件
- `server/src/main/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilter.java`
- `server/src/main/java/com/airesume/server/repository/InterviewMessageRepository.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/test/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilterTest.java`
- `server/src/test/java/com/airesume/server/service/InterviewServiceTest.java`
- `server/src/test/java/com/airesume/server/service/impl/InterviewAiServiceImplTest.java`
- `frontend/app/src/views/interview/InterviewSessionView.vue`

## 修复内容
1. Offer AI 接口限流
- 新增 `/api/offer/` 前缀限流策略。
- 使用 `USER_OR_IP` 作为限流维度，避免已登录用户无限刷高成本 AI 接口。
- 补充对应单测。

2. 面试 SSE 重复落库
- 前端将自动重发次数关闭为 `0`，避免消息已被服务端接受后再次自动提交。
- 后端在 `saveUserMessage` 中增加兜底防重：若最新一条消息仍是相同 `user` 内容，则直接跳过写入。
- 补充后端单测覆盖该场景。

3. AI 报告 `null` 数组兼容
- `InterviewAiServiceImpl` 在解析报告后统一归一化数组字段。
- `mapLegacyFields` 执行前再次兜底，避免 `weaknesses`、`improvementSuggestions` 等字段为 `null` 时触发 NPE。
- 补充单测验证有效报告不会因为 `null` 数组被打回默认报告。

## 数据存储方案
- 无新增数据库表或迁移。
- 面试重复落库修复采用现有聊天记录表的“最新消息防重”策略。

## 验证结果
- `mvn.cmd -q "-Dtest=CriticalEndpointRateLimitFilterTest,InterviewServiceTest,InterviewAiServiceImplTest" test` 通过。
- `npm.cmd run build` 通过。

## 说明
- 当前前端项目未配置独立单测脚本，本轮前端改动继续以生产构建通过作为验证基线。
- 本轮仅修复审查结论中的 3 个问题，不扩展到新的功能开发。
