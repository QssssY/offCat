# TASK_CORE_BUGFIX_ROUND_10_HIGH_PRIORITY_ISSUES

## 当前任务所属模块
- 后端 AI 调用保护
- 后端面试评估回写一致性
- 后端日志可观测性

## 后端文件定位
- `server/src/main/java/com/airesume/server/config/AiCircuitBreakerConfig.java`
- `server/src/main/java/com/airesume/server/service/AiCircuitBreaker.java`
- `server/src/main/java/com/airesume/server/service/AiChatClient.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/repository/InterviewSessionRepository.java`
- `server/src/main/java/com/airesume/server/service/NotificationService.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeAiServiceImpl.java`
- `server/src/main/resources/application.yml`
- `server/src/main/resources/application-dev.yml`

## 本轮修改文件清单
- 新增：`AiCircuitBreakerConfig.java`
- 新增：`AiCircuitBreaker.java`
- 新增：`AiCircuitBreakerTest.java`
- 修改：`AiChatClient.java`
- 修改：`InterviewService.java`
- 修改：`InterviewSessionRepository.java`
- 修改：`NotificationService.java`
- 修改：`InterviewAiServiceImpl.java`
- 修改：`ResumeAiServiceImpl.java`
- 修改：`InterviewServiceTest.java`
- 修改：`ResumeAiServiceImplTest.java`
- 修改：`application.yml`
- 修改：`application-dev.yml`

## 后端实现方案

### 1. AI 熔断器
- 采用仓库内轻量进程内熔断器，不引入新依赖
- 以业务维度拆分 breaker 名称，分别保护摘要、简历、面试调用链
- 连续失败达到阈值后短时间快速失败，避免每次请求都完整跑超时
- 通过单测覆盖“连续失败打开熔断”“冷却后恢复”“流式失败快速打开”

### 2. 面试评估防覆盖
- `InterviewSessionRepository` 新增条件更新：仅在 `evaluationReport` 为空时才允许回写
- `InterviewService` 异步写回时检查影响行数，若已存在结果则直接跳过
- 这样可以避免并发结束同一场面试时，后到的异步结果覆盖先落库结果

### 3. 日志补全堆栈
- `NotificationService` 的通知创建失败日志改为完整异常对象输出
- `AiChatClient`、`InterviewAiServiceImpl` 的关键失败日志补全堆栈
- 保持 `warn` / `error` 语义不变，只提升查因能力

## 数据存储影响说明
- 不新增表、不改表结构
- 仅新增一条条件更新 SQL，用于避免面试评估结果被重复覆盖
- 熔断器状态仅驻留进程内，不写入数据库和缓存

## 编译结果
- `mvn -q -DskipTests compile` 通过

## 构建结果
- `mvn -q test` 通过

## 当前功能验收说明
- AI 上游连续失败时，后续请求会快速失败，不再每次都跑满超时
- 并发结束同一场面试时，评估结果只会有一个版本写入数据库
- 关键异常场景能看到完整堆栈，排障信息更完整

## 停止点
- 本轮只处理当前真实存在的 P2 / P3 问题
- 不继续扩展到全局限流、依赖升级或更大范围的重构
