# TASK_MQ_SMART_ROUTING - 消息队列智能路由与任务处理稳定性修复

## 1. 任务背景

简历诊断功能当前所有任务无条件走 RabbitMQ 消息队列。由于消费者线程被 AI API 调用阻塞（10-60+秒/任务），加上 prefetchCount=1，即使只有 2 个任务也容易卡在"排队中"状态不进入处理。此外消费者在跳过任务时不同步更新状态、不退配额，导致任务永久卡死。

## 2. 问题分析

### 2.1 任务卡死根因
- Consumer 前置检查跳过任务时直接 return，不更新 DB 状态、不退配额
- 消息被 ACK 后从队列消失，任务永远停在 STATUS_PENDING
- 无定时任务回收卡在 PROCESSING 状态的孤儿任务

### 2.2 架构瓶颈
- 所有任务强制走 MQ，即使系统完全有能力直接处理
- 消费者线程被 AI 调用阻塞，prefetchCount=1 导致串行处理
- 无死信队列，失败消息直接丢失

## 3. 修复范围

### 3.1 本轮修改文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `server/.../service/impl/ResumeDiagnosisProcessor.java` | 新建 | 提取共享处理逻辑，供 MQ Consumer 和直连异步两条路径共用 |
| `server/.../mq/ResumeDiagnosisConsumer.java` | 重写 | 精简为委托调用，只注入 Processor |
| `server/.../mq/DirectProcessRouter.java` | 新建 | 直连路由器，基于 AtomicInteger 计数器判断负载 |
| `server/.../service/impl/ResumeDiagnosisTaskServiceImpl.java` | 修改 | createTask 的 afterCommit 改为智能路由 |
| `server/.../config/RabbitMQConfig.java` | 已修改 | 添加死信队列 DLX/DLQ 配置 |
| `server/.../common/constants/ResumeDiagnosisConstants.java` | 已修改 | 添加 DLX/DLQ 常量 |
| `server/.../mq/TaskRecoveryScheduler.java` | 已修改 | 定时回收孤儿任务（每5分钟扫描） |
| `server/.../ServerApplication.java` | 已修改 | 添加 @EnableScheduling |

### 3.2 不变的部分
- RabbitMQConfig 队列/交换机基本配置
- ResumeDiagnosisProducer（MQ 回退路径仍需要）
- AsyncConfig / aiAsyncExecutor（直接复用）
- 前端和 Controller（无改动）

## 4. 实现方案

### 4.1 智能路由架构
```
createTask()  [事务内]
  → 保存任务(STATUS_PENDING)、扣配额
  → afterCommit 回调:
       当前直连任务数 < 3?  ── 否 ──> RabbitMQ ──> Consumer ──> processTask()
              │
             是
              │
       aiAsyncExecutor.execute() ──> processTask()
```

### 4.2 ResumeDiagnosisProcessor（共享处理器）
- 包含完整诊断流水线：状态校验 → PDF提取 → AI诊断 → 结果增强 → 持久化 → 通知
- 包含全部错误处理：退配额、标记失败、超时检测
- MQ Consumer 和 DirectProcessRouter 两条路径共用此处理器

### 4.3 DirectProcessRouter（直连路由器）
- `AtomicInteger inFlightCount` 跟踪当前直连处理任务数
- `direct-threshold` 配置阈值（默认3），可通过 application.yml 调整
- 复用 `aiAsyncExecutor` 线程池（core=2, max=8, queue=50）
- `submitDirect()` 中 try/finally 保证计数器始终递减

### 4.4 Consumer 精简
- 仅保留 `@RabbitListener` 注解和一行委托调用
- 队列中残留消息和 MQ 回退路径仍正常工作

### 4.5 死信队列
- 主队列配置 `x-dead-letter-exchange` 和 `x-dead-letter-routing-key`
- 被拒绝的消息进入 `queue.resume.diagnosis.dlq`，不会丢失

### 4.6 孤儿任务回收
- `TaskRecoveryScheduler` 每5分钟扫描一次
- 将超过10分钟仍为 PROCESSING 的任务标记为 FAILED
- 覆盖两条路径的服务重启场景

## 5. 数据存储方案

不涉及数据库表结构变更。`app.diagnosis.direct-threshold` 配置项通过 application.yml 管理。

## 6. 编译结果

- 后端 `mvn clean compile`：通过

## 7. 验收说明

1. 启动项目，连续上传 2 个简历诊断，日志应显示 `任务路由到直连异步处理`
2. 任务应正常从"排队中"→"解析分析中"→"已完成"
3. 配置 `app.diagnosis.direct-threshold:1` 后同时上传 2 个任务，第1个走直连、第2个走 MQ，均正常完成
4. RabbitMQ 管理后台应能看到死信队列 `queue.resume.diagnosis.dlq` 已创建
