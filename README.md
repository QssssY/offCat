# 智能模拟面试与简历诊断系统 - 后端服务

## 项目简介

智能模拟面试与简历诊断系统后端服务，为求职者提供 AI 驱动的模拟面试、简历诊断、JD 匹配、简历润色、薪资谈判模拟等能力，附带完整的管理后台。

## 技术栈

| 类别 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.11 |
| JDK 版本 | 21 |
| ORM | MyBatis-Plus 3.5.7（主） + Spring Data JPA（辅） |
| 数据库 | MySQL 8.0+ |
| 缓存 | Redis 6.0+ (Lettuce) |
| 消息队列 | RabbitMQ |
| 认证 | JWT (jjwt 0.12.3) + Spring Security |
| HTTP 客户端 | RestClient（非流式） + WebClient/SSE（流式，基于 WebFlux） |
| PDF 解析 | Apache PDFBox 3.0.1 |
| PDF 导出 | Headless Chrome（`app.pdf.chrome-path`） |
| AI 接入 | 多 Provider 支持 (豆包/DeepSeek/通义千问/OpenAI/Kimi/MiniMax/文心) |
| 健康监控 | Spring Boot Actuator |
| 安全审计 | OWASP dependency-check-maven（`security-audit` profile） |

## 环境要求

- JDK 21+
- Maven 3.6+
- MySQL 8.0+（端口 3306）
- Redis 6.0+（端口 6379）
- RabbitMQ 3.8+（端口 5672）
- Chrome（仅 PDF 导出需要，配置 `app.pdf.chrome-path`）
- Tesseract OCR（可选，扫描件 PDF 提取用，配置 `app.resume-parse.ocr.command`）

## 快速开始

### 1. 配置数据库

导入数据库结构和种子数据：
```bash
mysql -u root -p ai_resume < db/schema.sql
```

增量迁移脚本位于 `db/migrations/`，按版本号顺序执行。

### 2. 修改配置

编辑 `src/main/resources/application-dev.yml`，配置数据库、Redis、RabbitMQ 连接信息。

### 3. 配置 AI Provider（可选）

在管理端 `AI引擎配置` 页面添加，或设置环境变量（如 `DEEPSEEK_API_KEY`）。

### 4. 启动服务

```bash
mvn spring-boot:run
```

服务启动后访问 `GET /api/auth/health` 验证。

## 构建命令

```bash
mvn spring-boot:run              # 启动开发服务器（端口 8080）
mvn compile                      # 快速编译检查
mvn clean install                # 完整构建
mvn package -DskipTests          # 打包为 JAR
mvn test                         # 运行所有测试
mvn test -Dtest=ClassName        # 运行单个测试类
mvn verify -Psecurity-audit      # OWASP 依赖安全扫描
```

## 配置说明

| 配置文件 | 用途 |
|----------|------|
| `src/main/resources/application.yml` | 主配置（DB、Redis、RabbitMQ、JWT、AI Provider、Token 限制、熔断器、简历解析、PDF 导出） |
| `src/main/resources/application-dev.yml` | 开发环境覆盖（SQL 日志、调试级别、开发 JWT 密钥） |
| `src/main/resources/application-prod.yml` | 生产环境（所有敏感值通过环境变量注入） |
| `db/schema.sql` | 数据库结构 + 种子数据（19 张表） |
| `db/migrations/` | 增量 SQL 迁移脚本 |

### AI 运行模式

通过以下配置切换 mock / real 模式：
- `app.ai.mode` — 简历诊断 AI 模式（`mock` / `real`）
- `app.interview.mode` — 模拟面试 AI 模式（`mock` / `real`）

### 简历解析配置

- `app.resume-parse.text-threshold` — 文本提取字符阈值（低于此值触发图片模式）
- `app.resume-parse.image-priority` — 图片处理优先级（`multimodal-first` / `ocr-first`）
- `app.resume-parse.ocr.*` — OCR 配置（启用、命令、语言、DPI）

### Token 限制配置

- `app.token-limit.diagnosis` — 简历诊断最大 Token（默认 6000）
- `app.token-limit.interview-round` — 面试单轮最大 Token（默认 1200）
- `app.token-limit.evaluation` — 面试评估最大 Token（默认 12000）
- `app.token-limit.compression-enabled` — 文本压缩开关
- `app.token-limit.ai-summary-enabled` — AI 摘要开关

### 熔断器配置

- `app.circuit-breaker.enabled` — 是否启用
- `app.circuit-breaker.failure-threshold` — 失败阈值（默认 3）
- `app.circuit-breaker.open-duration` — 熔断持续时间（默认 30s）

## 项目结构

```
src/main/java/com/airesume/server/
├── controller/          # REST 接口（21 个控制器）
├── service/             # 业务接口（35+ 接口）
│   └── impl/            # 服务实现（26 个实现）
├── entity/              # MyBatis-Plus 实体（17 个，继承 BaseEntity）
├── mapper/              # MyBatis-Plus 数据访问（20 个 Mapper）
│   └── resources/mapper/ # MyBatis XML Mapper（原子配额操作）
├── dto/                 # 请求/响应 DTO（50+ 个，按领域组织）
│   ├── admin/           # 管理端 DTO
│   ├── auth/            # 认证 DTO
│   ├── interview/       # 面试 DTO
│   ├── resume/          # 简历 DTO
│   ├── membership/      # 会员 DTO
│   ├── notification/    # 通知 DTO
│   ├── offer/           # Offer 助手 DTO
│   ├── onboarding/      # 新手引导 DTO
│   └── growth/          # 成长中心 DTO
├── vo/                  # 视图对象（会员套餐/升级结果）
├── common/              # 公共组件
│   ├── entity/          # BaseEntity（雪花 ID、createTime、updateTime、isDeleted）
│   ├── result/          # Result<T> 统一响应、PageResult<T> 分页、ResultCode 枚举
│   ├── exception/       # BusinessException + GlobalExceptionHandler
│   ├── constants/       # 业务常量（用户角色、面试、Prompt、诊断、会员、配额、AI 引擎）
│   └── util/            # BatchValidator、NetworkDiagnosticUtil
├── config/              # Spring 配置类（13 个）
├── infrastructure/      # 基础设施
│   └── security/        # JWT（JwtUtil、JwtAuthenticationFilter、JwtProperties）+ 限流过滤器
├── mock/                # Mock 实现（诊断结果生成器、面试服务）
├── mq/                  # RabbitMQ（生产者/消费者、智能路由、任务恢复调度）
├── repository/          # JPA 仓库（与 MyBatis-Plus 并用）
└── util/                # 工具类（TokenEstimator、AiInputCompressor、TextNormalizeUtil）
```

## 核心架构设计

### 双 AI 服务模式

每个 AI 功能（简历诊断、面试）提供两种实现，通过接口统一调用：
- `InterviewAiServiceImpl` / `MockInterviewAiServiceImpl` → `InterviewAiService`
- `ResumeAiServiceImpl` / `MockResumeAiServiceImpl` → `ResumeAiService`

通过 `@ConditionalOnProperty` 根据 `app.interview.mode` / `app.ai.mode` 选择加载。

### 运行时 AI 配置解析

AI 服务实现中的 `resolveRuntimeConfig()` 按三级优先级解析配置：
1. 数据库 `sys_ai_engine_config` 中的启用配置（最高优先级）
2. `application.yml` 中的属性配置
3. 环境变量

### 简历诊断流程（智能路由）

上传 → `DirectProcessRouter` 智能路由：
- **低负载**（在处理数 < 阈值，默认 3）：直接提交到 `aiAsyncExecutor` 线程池（更快）
- **高负载**：回退到 RabbitMQ 消息队列（可靠排队）

处理流程（`ResumeDiagnosisProcessor`）：
1. PDF 文本提取（PDFBox）→ 文本不足时触发图片模式（多模态 AI 视觉提取 / OCR）
2. 文本缓存到数据库
3. AI 诊断调用
4. 结果增强（后端提取基本信息：姓名、邮箱、电话等）
5. 存储结果 + 发送通知

失败时自动退还配额，任务标记为失败。`TaskRecoveryScheduler` 每 5 分钟扫描超过 10 分钟未完成的卡死任务并标记为失败。

### 面试 SSE 流式响应

`InterviewController.streamMessage()` 使用 `ResponseBodyEmitter` + 专用 `aiAsyncExecutor` 线程，通过 `WebClient`（WebFlux）订阅 Reactor `Publisher<String>` 获取 AI 流式响应，逐块转发为 SSE 事件。

### Token 预算管理

`TokenEstimator`（按字符类型估算：中文 ~1.5、英文 ~0.25、代码 ~0.35 token/字符） → `AiInputCompressor`（文本压缩） → `InterviewContextCompressor`（对话历史摘要）。限制阈值在 `AiTokenLimitConfig` 中配置。

### 优雅降级

`InterviewAiServiceImpl.shouldFallbackToLocalMock()` 在基础设施错误（网络异常、超时、缺少 API Key）时自动降级到 `MockInterviewService`，业务逻辑错误不触发降级。

### AI 熔断器

`AiCircuitBreaker` 提供可配置的熔断保护：连续失败达到阈值后熔断，经过指定时间后恢复。防止 AI 服务故障时的级联影响。

### 限流保护

`CriticalEndpointRateLimitFilter` 基于 Redis 计数器（本地 ConcurrentHashMap 降级），覆盖关键端点：

| 端点 | 限流策略 |
|------|----------|
| 注册 | 5 次/15 分钟（IP 维度） |
| 密保问题 | 10 次/15 分钟（IP 维度） |
| 密码重置 | 5 次/15 分钟（IP 维度） |
| 简历上传 | 10 次/10 分钟（用户维度） |
| 创建面试 | 10 次/10 分钟（用户维度） |
| 面试操作 | 40 次/10 分钟（用户维度） |
| Offer 助手 | 10 次/10 分钟（用户维度） |

超限返回 HTTP 429 + `ResultCode.TOO_MANY_REQUESTS`。

### 通知 SSE 推送

`NotificationService` 维护每用户 `SseEmitter` 连接，30 秒心跳保活，创建通知时实时推送。

## AI 接入说明

### 支持的 AI Provider

| Provider | 环境变量 Key | API 地址 |
|----------|-------------|---------|
| 豆包 (Doubao) | `DOUBAO_API_KEY` | `https://ark.cn-beijing.volces.com/api/v3` |
| 通义千问 (Qwen) | `QWEN_API_KEY` | `https://dashscope.aliyuncs.com/compatible-mode/v3` |
| OpenAI | `OPENAI_API_KEY` | `https://api.openai.com/v1` |
| Kimi | `KIMI_API_KEY` | `https://api.moonshot.cn/v1` |
| DeepSeek | `DEEPSEEK_API_KEY` | `https://api.deepseek.com/v1` |
| MiniMax | `MINIMAX_API_KEY` | — |
| 文心 (ERNIE) | `ERNIE_API_KEY` | — |

### 配置方式

1. **管理端配置（推荐）** — 访问管理端 `AI引擎配置` 页面添加，支持多引擎、多模态、思考模式
2. **环境变量配置（备用）** — `export DEEPSEEK_API_KEY="your-key"`

AI 配置三级优先级：数据库启用配置 > application.yml > 环境变量

### API Key 安全

- 管理端返回的 API Key 已脱敏（如 `sk-****abcd`）
- 后端校验脱敏值不会被写回数据库
- API Key 使用 `AiCredentialCrypto` 加密存储

## API 接口文档

### 统一响应格式

所有接口返回 `Result<T>`：
```json
{"code": 200, "message": "success", "data": {...}}
```

分页使用 `PageResult<T>`，参数为 `pageNum` / `pageSize`。

### 认证方式

JWT Bearer Token，`JwtAuthenticationFilter` 每次请求校验。支持 ASYNC dispatch 类型以兼容 SSE。

### 接口权限

| 路径 | 权限 |
|------|------|
| `/api/auth/**` | 公开 |
| `/api/admin/**` | 需要 `ROLE_ADMIN` |
| `/api/resume/**`, `/api/interview/**`, `/api/user/**`, `/api/offer/**` | 需要登录 |
| `/api/membership/**` | 需要登录（`/api/membership/plans` 公开） |
| `/api/diagnostic/**`, `/api/stats`, `/api/version-logs/**`, `/actuator/**` | 公开 |
| `GET /api/interview/job-roles` | 公开 |

### 1. 认证模块 (/api/auth)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /register | 用户注册 |
| POST | /login | 用户登录 |
| GET | /me | 获取当前用户信息 |
| PUT | /nickname | 更新昵称 |
| PUT | /password | 修改密码 |
| GET | /security-question | 获取密保问题 |
| POST | /security-question | 设置/更新密保问题 |
| POST | /reset-password | 通过密保问题重置密码 |
| GET | /health | 健康检查 |

### 2. 简历诊断模块 (/api/resume)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /upload | 上传简历文件（multipart，支持 PDF/DOC/DOCX） |
| GET | /task/{taskId} | 诊断任务详情 |
| GET | /history | 诊断历史 |
| POST | /job-match/analyze | 岗位匹配分析 |
| POST | /polish/analyze | 简历润色分析 |
| POST | /export-pdf | 导出 PDF（需要 Chrome，含 HTML 净化） |

### 3. 模拟面试模块 (/api/interview)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /session | 创建面试会话 |
| POST | /session/{id}/message | 发送消息（同步） |
| POST | /session/{id}/message/stream | 发送消息（SSE 流式） |
| GET | /session/{id} | 获取会话详情 |
| POST | /session/{id}/end | 结束面试，生成评估报告 |
| GET | /history | 面试历史 |
| GET | /job-roles | 获取岗位列表（公开） |

### 4. Offer 助手模块 (/api/offer)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /salary-negotiation/simulate | 薪资谈判模拟 |
| POST | /salary-negotiation/script | 薪资谈判话术脚本生成 |

### 5. 会员模块 (/api/membership)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /plans | 会员套餐列表（公开） |
| POST | /upgrade/mock | 模拟升级（仅 dev 环境） |

### 6. 用户中心 (/api/user)

#### 6.1 通知 (/api/user/notifications)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | / | 通知列表（分页） |
| GET | /unread-count | 未读通知数量 |
| PUT | /{id}/read | 标记单条已读 |
| PUT | /read-all | 标记全部已读 |
| DELETE | /{id} | 删除通知 |
| GET | /stream | SSE 实时通知推送 |

#### 6.2 新手引导 (/api/user/onboarding)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /status | 获取引导状态 |
| PUT | /status | 更新引导状态 |

#### 6.3 成长中心 (/api/user/growth)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /overview | 成长中心概览数据 |

#### 6.4 统计 (/api/user/stats)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /monthly | 用户月度统计（Redis 缓存） |

### 7. 公开接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/stats | 平台公开统计（用户数/简历数/面试数） |
| GET | /api/version-logs | 版本日志分页列表 |
| GET | /api/version-logs/latest | 最新 N 条版本日志 |

### 8. 管理端 (/api/admin)

#### 8.1 岗位配置 (job-roles)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /job-roles | 岗位列表 |
| POST | /job-roles | 新增岗位 |
| PUT | /job-roles | 修改岗位 |
| PUT | /job-roles/{id}/active | 启用/禁用 |
| DELETE | /job-roles/{id} | 删除岗位 |
| DELETE | /job-roles/batch | 批量删除 |
| PUT | /job-roles/batch/active | 批量启用/禁用 |

#### 8.2 Prompt 管理 (prompts)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /prompts | Prompt 列表 |
| POST | /prompts | 新增 Prompt |
| PUT | /prompts | 修改 Prompt |
| PUT | /prompts/{id}/active | 启用/禁用 |
| DELETE | /prompts/{id} | 删除 Prompt |
| DELETE | /prompts/batch | 批量删除 |
| PUT | /prompts/batch/active | 批量启用/禁用 |

#### 8.3 AI 引擎配置 (ai-engines)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /ai-engines | AI 引擎列表 |
| POST | /ai-engines | 新增 AI 引擎 |
| PUT | /ai-engines | 修改 AI 引擎 |
| PUT | /ai-engines/{id}/active | 启用/禁用 |
| DELETE | /ai-engines/{id} | 删除 AI 引擎 |
| DELETE | /ai-engines/batch | 批量删除 |
| PUT | /ai-engines/batch/active | 批量启用/禁用 |

#### 8.4 用户管理 (users)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /users | 用户列表（分页） |
| GET | /users/{userId}/rights | 用户权益详情 |
| PUT | /users/{userId}/rights | 修改用户权益 |
| PUT | /users/{userId}/status | 封禁/解封用户 |
| PUT | /users/batch/status | 批量封禁/解封 |
| GET | /users/{userId}/quota | 用户额度 |
| PUT | /users/quota | 调整用户额度 |
| GET | /users/{userId}/interviews | 用户面试记录（分页） |
| GET | /users/{userId}/resume-tasks | 用户简历任务（分页） |

#### 8.5 看板统计 (dashboard)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /dashboard/overview | 总览统计 |
| GET | /dashboard/trends | 趋势统计 |
| GET | /dashboard/hot-job-roles | 热门岗位排行 |
| GET | /dashboard/business-distribution | 业务分布 |

#### 8.6 监控 (monitor)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /monitor/overview | 监控总览 |

#### 8.7 会员管理 (/api/admin/membership)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /plans | 会员套餐列表 |
| POST | /plans | 新增套餐 |
| PUT | /plans | 修改套餐 |
| DELETE | /plans/{id} | 删除套餐 |
| GET | /orders | 订单查询（分页） |

#### 8.8 通知管理 (/api/admin/notifications)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | / | 通知列表 |
| POST | / | 创建通知 |
| PUT | /{id} | 修改通知 |
| DELETE | /{id} | 删除通知 |
| POST | /batch/delete | 批量删除 |
| POST | /{id}/publish | 发布通知 |
| POST | /batch/publish | 批量发布 |

#### 8.9 版本日志管理 (/api/admin/version-logs)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | / | 版本日志列表 |
| POST | / | 创建版本日志 |
| PUT | /{id} | 修改版本日志 |
| DELETE | /{id} | 删除版本日志 |
| POST | /batch/delete | 批量删除 |
| POST | /{id}/publish | 发布版本日志 |
| POST | /batch/publish | 批量发布 |

#### 8.10 审计日志 (/api/admin/audit-logs)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | / | 用户权益变更审计日志（分页） |

#### 8.11 成长中心配置 (/api/admin/growth-config)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | / | 配置列表 |
| POST | / | 新增配置 |
| PUT | / | 修改配置 |
| DELETE | /{id} | 删除配置 |
| POST | /batch/delete | 批量删除 |

#### 8.12 AI 配置调试

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/debug/ai-config | 查看当前 AI 配置状态（仅 dev） |

## 核心业务规则

### 1. Prompt 启用规则
- 同一岗位 + 同一难度只能有一个启用状态
- 启用新 Prompt 时自动禁用同岗位同难度的其他 Prompt

### 2. AI 引擎启用规则
- 同一业务类型（interview/resume）只能有一个启用配置
- 启用新引擎时自动禁用同业务类型的其他配置

### 3. 用户额度规则
- 普通用户：有基础额度限制
- 会员用户：无额度限制（有 VIP 有效期）
- 每日额度重置机制
- 配额操作使用原子 SQL（`UserQuotaMapper.xml`）保证并发安全
- 诊断失败自动退还配额

### 4. 面试会话规则
- 会话创建时需要扣除额度
- 结束时生成评价报告
- 支持 6 种面试模式：普通、压力、岗位定向、大厂 HR、技术负责人、外企面试官
- 支持 2 种反馈模式：面试结束后反馈、即时反馈

### 5. 管理端删除策略
- 管理端 CRUD（Prompts、岗位、AI 引擎）使用物理删除
- 业务实体使用 MyBatis-Plus 逻辑删除（`isDeleted` 字段）

### 6. API Key 安全
- 管理端返回的 API Key 已脱敏（如 `sk-****abcd`）
- 后端校验脱敏值不会被写回数据库
- API Key 使用 `AiCredentialCrypto` 加密存储

### 7. 审计日志
- 用户权益变更（角色、会员、VIP 到期时间）自动记录到 `user_rights_change_log`

## 数据库表

| 表名 | 说明 |
|------|------|
| sys_user | 用户表（用户名、昵称、密码、角色、状态、会员、密保） |
| user_quota | 用户额度表（面试/简历剩余、每日已用、总量已用） |
| membership_plan | 会员套餐表（含种子数据：月卡/季卡/年卡） |
| membership_order | 会员订单表 |
| sys_prompt | Prompt 模板表（场景类型、岗位、难度、内容、启用状态） |
| sys_job_role | 岗位配置表（含种子数据：7 个岗位） |
| sys_ai_engine_config | AI 引擎配置表（引擎、Provider、模型、API Key 加密、多模态、思考模式） |
| user_rights_change_log | 用户权益变更审计日志 |
| resume_diagnosis_task | 简历诊断任务表（文件 URL、状态、诊断结果、解析模式） |
| resume_job_match_record | 简历-JD 匹配记录表 |
| resume_polish_record | 简历润色记录表 |
| interview_session | 面试会话表（岗位、难度、模式、评分、评估报告、反馈模式） |
| mock_interview_job_target_record | 岗位定向面试上下文表 |
| interview_chat_log | 面试对话记录表 |
| user_onboarding_state | 新手引导状态表 |
| user_notification | 用户通知表（类型、标题、内容、已读状态、广播 ID） |
| sys_admin_notification | 管理员广播通知表（目标类型：全部/VIP/普通） |
| sys_version_log | 版本更新日志表（版本号、标题、Markdown 内容、类型） |
| sys_growth_config | 成长中心配置表 |

### SQL 迁移脚本

| 文件 | 版本 | 说明 |
|------|------|------|
| `alter_v1.3_add_security_question.sql` | V1.3 | sys_user 添加密保问题/答案字段 |
| `alter_v2.0_add_interview_feedback_mode.sql` | V2.0 | interview_session 添加反馈模式字段 |
| `alter_v3.0_admin_features.sql` | V3.0 | 新建通知广播、版本日志、成长配置表 |
| `TASK_18_RESUME_PARSE_MODE_AND_MULTIMODAL.sql` | Task 18 | 简历解析模式 + 多模态支持字段 |
| `TASK_19_THINKING_MODE_ENGINE_CONFIG.sql` | Task 19 | AI 引擎思考模式字段 |

## 关键配置类

| 配置类 | 说明 |
|--------|------|
| SecurityConfig | Spring Security 过滤器链：JWT、限流、CORS、URL 授权、BCrypt、无状态会话 |
| RabbitMQConfig | 队列/交换机/绑定（含死信队列）、Jackson JSON 转换、监听器容器（prefetch=1, concurrency 1-3） |
| AsyncConfig | `aiAsyncExecutor` 线程池：core=2, max=8, queue=50, CallerRunsPolicy |
| MybatisPlusConfig | 分页插件、乐观锁、防全表更新 |
| MyMetaObjectHandler | 自动填充 createTime / updateTime |
| RedisConfig | Redis 序列化、RedisTemplate、CacheManager（按缓存区域配置 TTL） |
| CachingConfig | 启用缓存 + RedisCacheErrorHandler 优雅降级 |
| JacksonConfig | Jackson ObjectMapper 定制 |
| RestClientConfig | RestClient：10s 连接超时 / 120s 读取超时 |
| PdfConfig | Headless Chrome 路径、PDF 生成超时、no-sandbox |
| ResumeParseConfig | 简历解析：文本阈值、图片优先级、OCR 配置 |
| AiTokenLimitConfig | Token 限制：诊断/面试轮次/评估、压缩、AI 摘要 |
| AiCircuitBreakerConfig | 熔断器：启用、失败阈值、熔断时长 |

## 管理端功能

### 批量操作说明

| 模块 | 支持操作 |
|------|----------|
| 岗位配置 | 单条/批量删除、单条/批量启用/禁用 |
| Prompt 管理 | 单条/批量删除、单条/批量启用/禁用 |
| AI 引擎配置 | 单条/批量删除、单条/批量启用/禁用 |
| 用户权益 | 批量封禁/批量解封 |
| 通知管理 | 单条/批量删除、单条/批量发布 |
| 版本日志 | 单条/批量删除、单条/批量发布 |
| 成长配置 | 单条/批量删除 |

### 管理端入口

- 管理端页面路径：`/admin`
- 需要管理员权限（role=9）访问
- 侧边栏分 4 组：洞察（看板/监控）、运营（用户/审计/通知/版本）、计费（套餐/订单）、配置（岗位/Prompt/AI 引擎/成长配置）
