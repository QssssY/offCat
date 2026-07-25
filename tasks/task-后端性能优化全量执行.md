# 后端性能优化后续全量执行任务记录

## 当前任务所属模块

后端性能、稳定性、数据生命周期、数据库访问链路与生产配置优化。

## 前端文件定位

本轮不涉及前端文件，不修改页面、不修改前端接口调用、不新增前端依赖。

## 后端文件定位

- `server/src/main/resources/application-prod.yml`
- `server/src/main/java/com/airesume/server/config/RabbitMQConfig.java`
- `server/src/main/java/com/airesume/server/mq/ResumeDiagnosisDlqConsumer.java`
- `server/src/main/java/com/airesume/server/service/impl/UserDataRetentionCleanupServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisProcessor.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/InterviewMessageService.java`
- `server/src/main/java/com/airesume/server/service/impl/GrowthServiceImpl.java`
- `server/src/main/java/com/airesume/server/mapper/InterviewSessionMapper.java`
- `server/src/main/java/com/airesume/server/mapper/InterviewChatLogMapper.java`
- `server/pom.xml`
- `db/schema.sql`
- `server/db/schema.sql`
- `db/migrations/TASK_58_PERFORMANCE_INDEXES.sql`
- `server/db/migrations/TASK_58_PERFORMANCE_INDEXES.sql`
- `server/CLAUDE.md`

## 本轮修改文件清单

- 生产配置补齐 HikariCP、Tomcat、Redis 小机器保守参数，并设置诊断直连阈值为 1。
- RabbitMQ 增加发布端重试、诊断消息单消息 TTL、死信队列只记录型消费者。
- 上传简历源文件增加默认 30 天终态任务清理、上传前磁盘余量保护、上传目录统计日志。
- 实体明确大字段增加 `@TableField(select = false)`，详情、重试、处理、成长中心、岗位匹配、润色、面试链路按需显式补回字段。
- 将 `ResumeTaskRepository`、`InterviewSessionRepository`、`InterviewMessageRepository` 的生产使用迁移到 MyBatis-Plus Mapper。
- 删除 3 个 JPA Repository 接口，移除实体 JPA 注解与 `spring-boot-starter-data-jpa` 依赖。
- 同步根目录与 `server/` 下 schema 文件，新增 TASK_58 索引迁移脚本。
- 更新 `server/CLAUDE.md`，将 Java 版本说明修正为 Java 17，并移除 JPA Repository 相关说明。
- 新增或扩展配置、RabbitMQ、文件生命周期、大字段、Schema 一致性等测试。

## 前端实现方案

本轮不涉及前端实现。所有 API 路径、响应 JSON 结构、前端依赖均保持不变。

## 后端实现方案

- 先完成低风险生产配置，限制连接池、线程池、Redis 等运行资源上限，面向 2 核 2G/4G 服务器保守配置。
- 再补 RabbitMQ 可靠性保护：发布失败短重试、诊断消息 1 小时单消息 TTL、DLQ 仅记录不自动重试，避免二次失败链路。
- 上传源文件生命周期只处理完成/失败终态任务，排队中和处理中任务不清理；删除路径继续限制在 `uploads/resumes` 内。
- 大字段默认不随列表查询全列加载，并在确实需要详情文本或诊断结果的链路显式选择字段，避免改变接口返回。
- JPA 移除放在最后执行，先完成 Mapper 等价迁移，再删除 Repository 与依赖，保留 MyBatis-Plus 注解和逻辑删除行为。

## 数据存储方案

- 不新增表、不删除表、不删除字段。
- 新增保守索引迁移脚本：
  - `community_comment(parent_comment_id, create_time)`。
  - `resume_diagnosis_task(status, failed_at)`。
- 不删除 `interview_chat_log(message_role)` 索引，避免线上删索引风险。
- 保留 `db/schema.sql` 与 `server/db/schema.sql` 双副本，并通过测试锁定关键内容一致。

## stage 更新说明

已更新根目录 `stage.md`，记录后续后端性能优化全量执行已完成，原先列为尚未开始的 JPA 移除、大字段优化、RabbitMQ 增强、上传文件生命周期清理、Schema 统一、生产容量参数调优已转为已完成并验证。

## 编译结果

- `mvn compile` 通过。

## 构建结果

- `mvn clean package -DskipTests` 通过，后端 jar 已生成。

## 当前功能验收说明

- `mvn test "-Dtest=RuntimeProtectionConfigTest,RabbitMQConfigTest,UserDataRetentionCleanupServiceImplTest,ResumeDiagnosisTaskServiceImplTest"` 通过，结果为 26 个测试，0 失败，0 错误。
- `mvn test` 通过，结果为 554 个测试，0 失败，0 错误。
- `mvn compile` 通过。
- `mvn clean package -DskipTests` 通过。
- 代码搜索确认未发现 `spring-boot-starter-data-jpa`、`jakarta.persistence`、`JpaRepository`、旧 Repository 名称等残留引用。

## 停止，不继续下一功能

本轮仅完成用户指定的后续后端性能优化全量执行范围。未继续新增前端功能、未新增业务模块、未继续扩大数据库结构变更，等待验收。
