# 后端性能优化计划

> 生成时间: 2026-05-26
> 状态: 计划阶段，待实施

## Context

AI Resume 后端基于 Spring Boot 3.2 + MyBatis-Plus + Redis + RabbitMQ，当前可以正常运行，但在高并发场景下存在多处性能隐患。主要包括：数据库查询效率低（全列加载、N+1 查询）、连接池未调优、内存泄漏风险、以及部分模块缺少缓存。本计划按优先级从高到低排列，分阶段实施。

---

## 第一阶段：高优先级（直接影响性能与稳定性）

### 1.1 实体字段懒加载 — 避免加载 MEDIUMTEXT/LONGTEXT 大字段

**问题**: 所有实体在查询时加载全部列，包括 `resumeText`(MEDIUMTEXT)、`documentJson`(LONGTEXT)、`evaluationReport`(JSON) 等大字段。列表页/状态查询只需 ID 和状态，却加载了几百 KB 的文本数据。

**方案**: 在以下实体的重字段上添加 `@TableField(select = false)`，仅在需要时手动 select 指定列：

| 实体文件 | 需标记的字段 |
|---------|------------|
| `entity/ResumeDiagnosisTask.java` | `resumeText`, `diagnosisResult` |
| `entity/ResumeJobMatchRecord.java` | `resumeText`, `jdText` |
| `entity/ResumePolishRecord.java` | `sourceResumeText`, `jdText`, `polishedResumeText`, `documentJson` |
| `entity/MockInterviewJobTargetRecord.java` | `jdText`, `generatedQuestions` |
| `entity/InterviewSession.java` | `evaluationReport` |

**影响范围**: 需要同时修改 Service 层中使用到这些字段的 `selectList`/`selectPage` 调用，改用 `.select()` 指定需要的列，或使用自定义查询方法。

### 1.2 修复 AdminDashboard N+1 查询

**问题**: `AdminDashboardServiceImpl.getDashboardTrends()` (line 99-119) 在循环中逐天查询，7 天 = 14 条 SQL，90 天 = 180 条 SQL。

**方案**: 改为两条 `GROUP BY DATE(create_time)` 的聚合查询，一次返回所有日期的数据，Java 端组装结果。涉及文件：
- `service/impl/AdminDashboardServiceImpl.java`
- `mapper/InterviewSessionMapper.java`（添加自定义方法）
- `mapper/ResumeDiagnosisTaskMapper.java`（添加自定义方法）

### 1.3 配置 HikariCP 连接池

**问题**: 未配置任何 `spring.datasource.hikari.*` 参数，使用默认 10 连接。AI 异步处理 + RabbitMQ 消费者 + Tomcat 请求线程可能耗尽连接。

**方案**: 在 `application-prod.yml` 中添加：
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

### 1.4 配置 Tomcat 线程池（SSE 场景关键）

**问题**: SSE 流式面试通过 `ResponseBodyEmitter` 占用 Tomcat 线程长达 120 秒，默认 200 线程容易被占满。

**方案**: 在 `application-prod.yml` 中添加：
```yaml
server:
  tomcat:
    threads:
      max: 300
      min-spare: 20
    max-connections: 10000
    accept-count: 200
    connection-timeout: 20000
```

### 1.5 修复 InterviewContextCompressor 内存泄漏

**问题**: `InterviewContextCompressor.summaryCache` (line 42) 是无界 ConcurrentHashMap，用户放弃面试后条目永远不会被清理。

**方案**: 添加定时清理任务，清理超过 2 小时的缓存条目。可用 `@Scheduled` 或引入 Caffeine 本地缓存（带 TTL）替代 ConcurrentHashMap。

### 1.6 PDF 导出端点添加限流

**问题**: `POST /api/resume/export-pdf` 每次请求启动一个 Chrome 进程，极度消耗 CPU/内存，但没有限流保护。

**方案**: 在 `CriticalEndpointRateLimitFilter` 中添加 PDF 导出的限流策略，建议 5 次/10 分钟。

### 1.7 移除 spring-boot-starter-data-jpa 依赖

**问题**: 项目完全使用 MyBatis-Plus 进行数据访问，但 pom.xml 中引入了 `spring-boot-starter-data-jpa`。Hibernate 启动时扫描实体、创建 EntityManagerFactory，增加启动时间和内存占用。7 个实体同时携带 JPA 和 MyBatis-Plus 注解。

**方案**:
1. 从 `pom.xml` 移除 `spring-boot-starter-data-jpa`
2. 清理实体上的 JPA 注解（`@Entity`, `@Table`, `@Id`, `@MappedSuperclass`）
3. 清理 `BaseEntity` 上的 JPA 注解
4. 验证无 `JpaRepository` 在使用

---

## 第二阶段：中优先级（提升健壮性与效率）

### 2.1 Redis Lettuce 连接池调优

**问题**: `max-wait: -1ms` 意味着线程在连接池耗尽时无限期阻塞，可能导致线程饥饿。

**方案**: 设置 `max-wait: 3000ms`，超时后抛异常而非无限等待。修改 `application.yml` 和 `application-prod.yml`。

### 2.2 为 Streaming WebClient 添加 responseTimeout

**问题**: `InterviewAiServiceImpl` (line 131-137) 创建的 `HttpClient` 没有设置 `responseTimeout`，虽然 `ResponseBodyEmitter` 有 120 秒超时兜底，但 WebClient 层面连接可能永远挂起。

**方案**: 在 HttpClient 创建时添加 `.responseTimeout(Duration.ofSeconds(180))`。

### 2.3 RabbitMQ 增强

**问题**:
- `RabbitTemplate` 没有配置发布端重试，Broker 短暂不可用时消息丢失
- 死信队列没有消费者，失败消息静默堆积
- 主队列没有消息 TTL

**方案**:
1. 为 `RabbitTemplate` 配置 `RetryTemplate`（3 次重试，间隔递增）
2. 添加死信队列消费者，记录失败原因到日志/数据库
3. 为 `resumeDiagnosisQueue` 设置 `x-message-ttl`（如 1 小时）

### 2.4 缓存覆盖优化

**问题**:
- `admin:dashboardTrends` 缓存区域未在 `RedisConfig.cacheConfigMap` 中注册，使用默认 5 分钟 TTL（偏短）
- `sys_user` 和 `auth:userInfo` 两个缓存区域可能数据不一致（如会员升级时只清了 `auth:userInfo` 没清 `sys_user`）
- `PublicStatsServiceImpl` 使用 JDK 序列化存储统计数据，Redis 中不可读

**方案**:
1. 在 `RedisConfig` 中为 `admin:dashboardTrends` 注册 10 分钟 TTL
2. 统一用户信息缓存为单一区域，消除交叉不一致
3. `PublicStatsServiceImpl` 改用 `StringRedisTemplate` + JSON 序列化

### 2.5 GrowthService 查询优化

**问题**: `GrowthServiceImpl.getGrowthOverview()` 发起 6+ 条查询，部分查询加载了完整的 MEDIUMTEXT/JSON 列仅为了提取分数。

**方案**:
1. 对只需要计数的查询使用 `selectCount` + `.select()` 指定列
2. 考虑将聚合结果缓存（已有 `user:growthOverview` 缓存 5 分钟，但首次计算开销大）
3. `queryCompletedResumeTasks()` 使用 `.select("id, score")` 代替加载全量数据

### 2.6 日志生产环境优化

**问题**: `InterviewAiServiceImpl` 每次流式请求输出 20+ 行 `log.info()` ASCII 统计报告，生产环境 I/O 开销大。

**方案**:
1. 统计报告日志改为 `log.debug()` 级别，仅在开发环境或显式开启 debug 时输出
2. 盒绘格式日志（`══════`）改为 `log.isDebugEnabled()` 守卫

### 2.7 社区图片静态资源优化

**问题**: `WebMvcConfig` 将 `/uploads/community/**` 映射到本地文件系统，Spring Boot 直接提供静态文件，无缓存头、无 CDN。

**方案**:
1. 为静态资源添加 `Cache-Control: max-age=86400` 响应头
2. 长期：迁移到 OSS/S3 + CDN，Spring Boot 只返回 CDN URL

### 2.8 上传简历源文件生命周期管理

**问题**: 用户上传的简历 PDF 源文件（存储在 `uploads/resumes/` 目录）**不会自动删除**。当前只有 3 种清理路径：
1. 用户手动清除诊断历史 → `ResumeDiagnosisTaskServiceImpl.clearHistory()` 同步删除物理文件
2. 用户配置了数据保留策略 → `UserDataRetentionCleanupServiceImpl` 每天 3:30 AM 批量清理过期记录并删除对应文件
3. 用户注销账号 → 调用 `clearHistory()` 清理

**核心风险**: 如果用户从未配置保留策略，PDF 文件将永久堆积。没有磁盘空间监控，没有总容量上限保护，没有过期自动清理机制。长期运行后磁盘会被占满。

**方案**:
1. **添加默认过期策略**: 为上传文件设置最大保留天数（如 30 天），定时任务自动清理超期文件（无论用户是否配置保留策略）
2. **磁盘空间监控**: 上传前检查磁盘剩余空间，低于阈值（如 1GB）时拒绝上传并告警
3. **定期统计**: 添加定时任务统计 `uploads/resumes/` 目录大小和文件数量，超过阈值时告警
4. **长期方案**: 迁移到 OSS/S3 对象存储，利用云服务的生命周期管理自动过期

---

## 第三阶段：低优先级（改善代码质量）

### 3.1 添加慢查询监控

**问题**: 没有配置 `PerformanceInnerInterceptor`，开发时无法发现慢查询。

**方案**: 在 `MybatisPlusConfig` 中添加 `PerformanceInnerInterceptor`（仅 dev profile 激活），阈值 200ms。

### 3.2 清理 OptimisticLockerInnerInterceptor

**问题**: `MybatisPlusConfig` 注册了 `OptimisticLockerInnerInterceptor`，但没有任何实体使用 `@Version` 注解，属于死代码。

**方案**: 移除 `OptimisticLockerInnerInterceptor` 注册。

### 3.3 数据库索引优化

| 操作 | 索引 | 原因 |
|------|------|------|
| 新增 | `community_comment(parent_comment_id, create_time)` | 加速回复楼层加载 |
| 移除 | `interview_chat_log(message_role)` | 低基数列（3 个值），索引无效 |
| 新增 | `resume_diagnosis_task(status, failed_at)` | 支撑失败重试查询 |

### 3.4 修复 DirectProcessRouter TOCTOU 竞态

**问题**: `canProcessDirectly()` 和 `submitDirect()` 是两个独立操作，高并发时可能超过阈值。

**方案**: 将检查和递增合并为一个原子操作（使用 `compareAndSet` 或同步块）。

### 3.5 本地登录限流 Map 内存清理

**问题**: `AuthServiceImpl.localLoginAttemptMap` 中过期条目永不清理（仅在被访问时惰性清理）。

**方案**: 添加 `@Scheduled` 定时任务每 30 分钟清理过期条目。

### 3.6 统一 Schema 文件

**问题**: `db/schema.sql` 和 `server/db/schema.sql` 两个副本已产生分歧（后者缺少 `user_settings` 表等）。

**方案**: 保留 `db/schema.sql` 作为唯一真实来源，删除 `server/db/schema.sql`。

### 3.7 Java 版本文档修正

**问题**: CLAUDE.md 写 Java 21，pom.xml 实际是 Java 17。

**方案**: 确认实际使用的 JDK 版本，统一 CLAUDE.md 和 pom.xml。

---

## 关键文件清单

| 文件路径 | 修改内容 |
|---------|---------|
| `server/pom.xml` | 移除 data-jpa 依赖 |
| `server/src/main/resources/application-prod.yml` | HikariCP、Tomcat 线程池配置 |
| `server/src/main/resources/application.yml` | Redis pool max-wait 修改 |
| `server/src/main/java/.../entity/*.java` | 添加 `@TableField(select = false)`，移除 JPA 注解 |
| `server/src/main/java/.../entity/BaseEntity.java` | 移除 JPA 注解 |
| `server/src/main/java/.../service/impl/AdminDashboardServiceImpl.java` | N+1 查询重构 |
| `server/src/main/java/.../service/InterviewContextCompressor.java` | 缓存 TTL/定时清理 |
| `server/src/main/java/.../config/RedisConfig.java` | 补充缓存区域 TTL |
| `server/src/main/java/.../config/RabbitMQConfig.java` | 添加重试策略、DLQ 消费者 |
| `server/src/main/java/.../config/MybatisPlusConfig.java` | 添加慢查询拦截器，移除乐观锁 |
| `server/src/main/java/.../service/impl/InterviewAiServiceImpl.java` | 日志级别降级、WebClient timeout |
| `server/src/main/java/.../infrastructure/security/CriticalEndpointRateLimitFilter.java` | 添加 PDF 导出限流 |
| `server/src/main/java/.../config/WebMvcConfig.java` | 静态资源缓存头 |
| `server/src/main/java/.../service/impl/GrowthServiceImpl.java` | 查询字段优化 |
| `server/src/main/java/.../mq/DirectProcessRouter.java` | 原子化并发控制 |
| `server/src/main/java/.../service/impl/AuthServiceImpl.java` | 本地 Map 定时清理 |
| `server/src/main/java/.../service/impl/ResumeDiagnosisTaskServiceImpl.java` | N+1 写入优化 |

---

## 验证方案

1. **数据库查询性能**: 开启 `PerformanceInnerInterceptor`，验证 Dashboard、Growth 页面查询耗时 < 200ms
2. **连接池**: 启动后通过 HikariCP JMX 或 `/actuator/health` 确认连接池配置生效
3. **内存泄漏**: 压测模拟 100 个用户同时开始面试，观察 `InterviewContextCompressor.summaryCache` 大小是否稳定
4. **PDF 导出限流**: 连续发送 6 次 export-pdf 请求，第 6 次应被限流拒绝
5. **实体字段优化**: 对比优化前后同一接口的响应时间和内存占用
6. **JPA 移除**: `mvn clean compile` 编译通过，启动时间应减少 1-2 秒
7. **全量构建**: `mvn clean package -DskipTests` 确保编译通过

---

## 当前已有的缓存覆盖（无需修改）

| 模块 | 缓存区域 | TTL | 缓存方式 |
|------|---------|-----|---------|
| 用户信息 | `auth:userInfo` | 10 分钟 | `@Cacheable(sync=true)` |
| 未读通知数 | `notification:unreadCount` | 2 分钟 | `@Cacheable` |
| 职位角色配置 | `config:jobRoles` | 30 分钟 | `@Cacheable` |
| 会员套餐配置 | `config:membershipPlans` | 30 分钟 | `@Cacheable` |
| 简历诊断任务 | `resume:task` | 10 秒 | `@Cacheable` |
| 用户月度统计 | `user:monthlyStats` | 5 分钟 | `@Cacheable` |
| 用户成长概览 | `user:growthOverview` | 5 分钟 | `@Cacheable` |
| AI 引擎配置 | `config:aiEngine` | 30 分钟 | `@Cacheable` |
| Prompt 配置 | `config:prompt` | 30 分钟 | `@Cacheable` |
| 登录限流 | `login:attempts:{username}` | 15 分钟 | 手动 Redis |
| API 限流 | `rate-limit:{policy}::{key}` | 10-15 分钟 | 手动 Redis |
| 公开统计 | `public:stats` | 5 分钟 | 手动 Redis |
