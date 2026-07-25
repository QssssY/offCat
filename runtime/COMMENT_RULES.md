# 项目注释规范（强制执行）

## 生效时间
2026-04-06 起，所有开发任务必须执行

## 一、注释范围

### 必须补充注释的代码
- ✅ 所有新增代码
- ✅ 所有修改过的旧代码
- ✅ 后续开发中触达的关键旧代码（原先缺少注释的）
- ✅ 后端和前端全覆盖（Java/Vue/TS/JS/SQL）

### 优先补充注释的模块
| 优先级 | 模块 | 说明 |
|--------|------|------|
| P0 | Controller | API 入口层 |
| P0 | Service / ServiceImpl | 业务逻辑层 |
| P0 | AI 接入类 | DoubaoResumeAiServiceImpl、DoubaoInterviewAiServiceImpl 等 |
| P0 | 流式响应处理 | Reactor、SSE 处理 |
| P0 | Security 配置 | Spring Security、JWT、Filter、Interceptor |
| P1 | DTO / VO / Entity | 数据传输对象 |
| P1 | Mapper 和复杂 SQL | 数据访问层 |
| P1 | 前端 API 请求 | 前端 API 层 |
| P1 | 前端 SSE 解析 | 流式处理 |
| P1 | 前端状态管理 | Pinia/Vuex |
| P1 | 前端消息渲染 | 消息展示逻辑 |

---

## 二、注释层级要求

### 1. 类注释
必须包含：
- 类职责说明
- 用途/所属模块
- 与其他类的关系（如继承、依赖）
- 条件激活注解说明（如 @ConditionalOnProperty）
- 典型使用场景

**示例**：
```java
/**
 * 豆包简历诊断 AI 服务实现类（真实 AI 模式）
 *
 * 所属模块：简历诊断模块
 * 职责：调用豆包大模型 API 生成简历诊断结果
 * 激活条件：当 app.ai.mode=real 时激活，替代 MockResumeAiServiceImpl
 * 依赖：PdfTextExtractor（PDF 提取）、SysPromptService（Prompt 管理）
 *
 * 典型使用场景：用户上传 PDF 简历后，异步任务调用此服务生成诊断结果
 */
```

---

### 2. 方法注释
必须包含：
- 功能说明
- 参数说明（每个参数的含义、约束、可为空情况）
- 返回值说明
- 异常说明（抛出什么异常、什么情况会抛）
- 调用时机/上下文
- 副作用（如写库、发消息、扣额度等）

**示例**：
```java
/**
 * 创建简历诊断任务
 *
 * 功能：上传简历后创建任务记录、扣减额度、发送 MQ 消息
 *
 * @param userId   提交用户 ID（不能为空，必须是已登录用户）
 * @param fileUrl  简历文件相对路径（如 /uploads/resumes/xxx.pdf）
 * @return 任务 ID（雪花 ID，Long 类型）
 * @throws BusinessException 用户额度不足时抛出，message="简历诊断次数已用完"
 * @throws RuntimeException 文件保存失败时抛出
 *
 * 调用时机：用户在前端点击"开始诊断"按钮时
 * 副作用：
 *   - 写入 resume_diagnosis_task 表
 *   - 扣减 user_quota 表的 resume_count
 *   - 发送消息到 RabbitMQ 队列 QUEUE_RESUME_DIAGNOSIS
 */
```

---

### 3. 关键逻辑注释
必须说明：
- 为什么这么做（背景、原因）
- 边界条件/什么情况会走这个分支
- 容易踩坑的地方（风险点）
- 状态流转说明
- 判空处理原因
- 异步/流式处理说明

**示例**：
```java
// 【为什么这么做】
// Reactor 的 map() 不允许返回 null，即使后面有 filter() 也不行
// 异常会在 map 阶段直接抛出，不会走到 filter
// 所以改用 handle()，只在有内容时才调用 sink.next()
//
// 【边界条件】
// - 空行、注释行(: ping)直接跳过
// - [DONE] 标记结束流但不发射 null
// - 无 content 的 chunk 也跳过
//
// 【风险点】
// 如果改用 map()，会出现 "The mapper returned a null value" 异常
.handle((line, sink) -> {
    ...
})
```

---

## 三、注释要求

### 禁止敷衍式注释
❌ 错误示例：
```java
// 这是一个方法
// 获取用户
public User getUser(Long id) { ... }
```

✅ 正确示例：
```java
/**
 * 根据用户 ID 查询用户信息
 *
 * @param id 用户 ID（主键，不能为空）
 * @return 用户实体，不存在时返回 null
 *
 * 【判空说明】
 * id 为 null 时 MyBatis-Plus 会查询 id=null，这不是预期行为
 * 所以调用方需要保证 id 不为 null
 */
```

### 注释要解释"为什么"，不只写表面
❌ 错误示例：
```java
// 设置状态为处理中
task.setStatus(1);
```

✅ 正确示例：
```java
// 【状态流转说明】
// 设置为处理中（STATUS_PROCESSING=1），前端会显示"解析分析中"
// 必须在开始 AI 调用前更新状态，避免用户看到"排队中"却已在处理
// 如果更新失败，任务会被 MQ 重新投递（前提是开启了重试）
resumeDiagnosisTaskService.updateStatusToProcessing(taskId);
```

---

## 四、任务输出要求

每次开发任务完成后，除功能结果外，必须说明：

### 1. 本次补充了哪些类的注释
```
本次补充注释的类：
- ResumeDiagnosisController
- ResumeDiagnosisTaskServiceImpl
- DoubaoResumeAiServiceImpl
```

### 2. 哪些旧代码已补齐注释
```
已补齐注释的旧代码：
- ResumeDiagnosisConsumer.handleResumeDiagnosisTask() 方法
- MockDiagnosisResultGenerator 类
- 状态流转相关常量
```

### 3. 哪些复杂逻辑增加了说明注释
```
增加说明注释的复杂逻辑：
- SSE 流式处理的 handle() 替代 map() 的原因
- 状态机流转的每一步说明
- PDF 提取的判空处理原因
- AI 调用失败时的降级策略
```

---

## 五、前端注释要求

### Vue 组件注释
```vue
<script setup>
/**
 * 简历诊断结果页组件
 *
 * 职责：展示简历诊断任务状态、轮询状态、结构化展示诊断结果
 * 路由：/resume/result/:taskId
 * 依赖：resume.js API、Pinia user store
 *
 * 【轮询逻辑说明】
 * - 任务处理中时每 3 秒轮询一次
 * - 完成或失败后停止轮询
 * - 完成后自动刷新用户额度信息
 */
</script>
```

### 前端 API 层注释
```javascript
/**
 * 上传简历 PDF 文件并创建诊断任务
 *
 * @param {File} file - PDF 文件对象（必须是 PDF 格式，不超过 10MB）
 * @returns {Promise<string>} taskId - 任务 ID（字符串类型，因为 Long 超出 JS 安全整数范围）
 *
 * @throws {Error} 文件格式错误时抛出
 * @throws {Error} 额度不足时抛出（code=400）
 *
 * 【注意】
 * taskId 是雪花 ID，必须用 String 接收，不能用 Number，否则会丢失精度
 */
export function uploadResume(file) { ... }
```

---

## 六、SQL 注释要求

```sql
-- 简历诊断任务表
-- 用途：存储简历诊断任务记录、状态、结果
-- 状态流转：0(排队中) -> 1(处理中) -> 2(完成) 或 3(失败)
-- 索引：idx_user_id (用户历史查询)、idx_status (任务调度)
CREATE TABLE resume_diagnosis_task (
    id BIGINT PRIMARY KEY COMMENT '雪花 ID',
    user_id BIGINT NOT NULL COMMENT '提交用户 ID',
    file_url VARCHAR(500) NOT NULL COMMENT 'PDF 文件相对路径',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0排队 1处理中 2完成 3失败',
    diagnosis_result JSON COMMENT 'AI 诊断结果（JSON 格式）',
    error_msg VARCHAR(1000) COMMENT '失败原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历诊断任务表';
```

---

## 七、检查清单

每次提交代码前确认：

- [ ] 新增类有类注释
- [ ] 新增 public 方法有方法注释
- [ ] 修改的方法补充/更新了注释
- [ ] 关键分支逻辑有说明注释
- [ ] 状态流转有说明注释
- [ ] 判空处理有原因说明
- [ ] 容易踩坑的地方注明了风险点
- [ ] 本次修改的注释补充情况已在任务输出中说明

---

**本规范自 2026-04-06 起强制执行。**
