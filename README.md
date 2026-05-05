# 智能模拟面试与简历诊断系统 - 后端服务

## 项目简介

智能模拟面试与简历诊断系统后端服务，为求职者提供AI驱动的模拟面试和简历诊断能力。

## 技术栈

| 类别 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.3 |
| JDK版本 | 21 |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | MySQL 8.0+ |
| 缓存 | Redis 6.0+ (Lettuce) |
| 消息队列 | RabbitMQ |
| 认证 | JWT (jjwt 0.12.3) + Spring Security |
| HTTP客户端 | RestClient (非流式) + WebClient/SSE (流式) |
| PDF解析 | Apache PDFBox 3.0.1 |
| PDF导出 | Headless Chrome (`app.pdf.chrome-path`) |
| AI接入 | 多Provider支持 (豆包/DeepSeek/通义千问/OpenAI/Kimi/MiniMax/文心) |

## 环境要求

- JDK 21+
- Maven 3.6+
- MySQL 8.0+ (端口 3306)
- Redis 6.0+ (端口 6379)
- RabbitMQ 3.8+ (端口 5672)
- Chrome (仅PDF导出需要，配置 `app.pdf.chrome-path`)

## 快速开始

### 1. 配置数据库

导入数据库结构和种子数据：
```bash
mysql -u root -p ai_resume < db/schema.sql
```

### 2. 修改配置

编辑 `src/main/resources/application-dev.yml`，配置数据库、Redis、RabbitMQ 连接信息。

### 3. 配置AI Provider（可选）

在管理端 `AI引擎配置` 页面添加，或设置环境变量（如 `DEEPSEEK_API_KEY`）。

### 4. 启动服务

```bash
mvn spring-boot:run
```

服务启动后访问 `GET /api/auth/health` 验证。

## 构建命令

```bash
mvn spring-boot:run              # 启动开发服务器 (端口 8080)
mvn compile                      # 快速编译检查
mvn clean install                # 完整构建
mvn package -DskipTests          # 打包为JAR
mvn test                         # 运行所有测试
mvn test -Dtest=ClassName        # 运行单个测试类
```

## 配置说明

| 配置文件 | 用途 |
|----------|------|
| `src/main/resources/application.yml` | 主配置 (DB、Redis、RabbitMQ、JWT、AI Provider、Token限制) |
| `src/main/resources/application-dev.yml` | 开发环境覆盖 |
| `db/schema.sql` | 数据库结构 + 种子数据 |

AI运行模式通过以下配置切换：
- `app.ai.mode` — 简历诊断AI模式 (`mock` / `real`)
- `app.interview.mode` — 模拟面试AI模式 (`mock` / `real`)

## 项目结构

```
src/main/java/com/airesume/server/
├── controller/          # REST接口 (10个控制器)
├── service/             # 业务接口
│   └── impl/            # 服务实现
├── entity/              # MyBatis-Plus实体 (继承BaseEntity: snowflake ID, createTime, updateTime, isDeleted)
├── mapper/              # MyBatis-Plus数据访问
├── dto/                 # 请求/响应DTO (按领域: admin/, auth/, interview/, membership/, resume/ 等)
├── common/              # 公共组件 (常量、Result<T>统一响应、ResultCode枚举、BusinessException、工具类)
├── config/              # Spring配置类
├── infrastructure/      # 安全组件 (JwtAuthenticationFilter, JwtUtil, JwtProperties)
├── mock/                # Mock实现 (MockDiagnosisResultGenerator, MockInterviewService)
├── mq/                  # RabbitMQ (ResumeDiagnosisProducer, ResumeDiagnosisConsumer)
├── repository/          # JPA仓库 (与MyBatis-Plus并用)
└── util/                # 工具类 (TokenEstimator, AiInputCompressor)
```

## 核心架构设计

### 双AI服务模式

每个AI功能（简历诊断、面试）提供两种实现，通过接口统一调用：
- `InterviewAiServiceImpl` / `MockInterviewAiServiceImpl` → `InterviewAiService`
- `ResumeAiServiceImpl` / `MockResumeAiServiceImpl` → `ResumeAiService`

通过 `@ConditionalOnProperty` 根据 `app.interview.mode` / `app.ai.mode` 选择加载。

### 运行时AI配置解析

AI服务实现中的 `resolveRuntimeConfig()` 按三级优先级解析配置：
1. 数据库 `sys_ai_engine_config` 中的启用配置（最高优先级）
2. `application.yml` 中的属性配置
3. 环境变量

### 简历诊断异步流程

上传 → RabbitMQ消息 → `ResumeDiagnosisConsumer` 异步处理：
1. PDF文本提取 (PDFBox)
2. 文本缓存到数据库
3. AI诊断调用
4. 结果增强（后端提取基本信息：姓名、邮箱、电话等）
5. 存储结果 + 发送通知

失败时自动退还配额，任务标记为失败并返回用户友好错误信息。

### 面试SSE流式响应

`InterviewController` 使用 `ResponseBodyEmitter` + 专用线程实现Server-Sent Events。通过 `WebClient` (WebFlux) 获取AI流式响应（Reactor `Flux`），逐块转发为SSE事件。

### Token预算管理

`TokenEstimator`（按字符类型估算） → `AiInputCompressor`（文本压缩） → `InterviewContextCompressor`（对话历史摘要）。限制阈值在 `AiTokenLimitConfig` 中配置：
- 简历诊断最大Token: 6000
- 面试单轮最大Token: 1200
- 面试评估最大Token: 8000

### 优雅降级

`InterviewAiServiceImpl.shouldFallbackToLocalMock()` 在基础设施错误（网络异常、超时、缺少API Key）时自动降级到 `MockInterviewService`，业务逻辑错误不触发降级。

## AI接入说明

### 支持的AI Provider

| Provider | 环境变量Key | API地址 |
|----------|-------------|---------|
| 豆包(Doubao) | `DOUBAO_API_KEY` | `https://ark.cn-beijing.volces.com/api/v3` |
| 通义千问(Qwen) | `QWEN_API_KEY` | `https://dashscope.aliyuncs.com/compatible-mode/v3` |
| OpenAI | `OPENAI_API_KEY` | `https://api.openai.com/v1` |
| Kimi | `KIMI_API_KEY` | `https://api.moonshot.cn/v1` |
| DeepSeek | `DEEPSEEK_API_KEY` | `https://api.deepseek.com/v1` |
| MiniMax | `MINIMAX_API_KEY` | — |
| 文心(ERNIE) | `ERNIE_API_KEY` | — |

### 配置方式

1. **管理端配置（推荐）** — 访问管理端 `AI引擎配置` 页面添加，支持多引擎
2. **环境变量配置（备用）** — `export DEEPSEEK_API_KEY="your-key"`

AI配置三级优先级：数据库启用配置 > application.yml > 环境变量

## API接口文档

### 统一响应格式

所有接口返回 `Result<T>`：
```json
{"code": 200, "message": "success", "data": {...}}
```

分页使用 `PageResult<T>`，参数为 `pageNum` / `pageSize`。

### 认证方式

JWT Bearer Token，`JwtAuthenticationFilter` 每次请求校验。

### 接口权限

| 路径 | 权限 |
|------|------|
| `/api/auth/**` | 公开 |
| `/api/admin/**` | 需要 `ROLE_ADMIN` |
| `/api/resume/**`, `/api/interview/**`, `/api/user/**` | 需要登录 |
| `/api/diagnostic/**`, `/actuator/**` | 公开 |
| `GET /api/interview/job-roles` | 公开 |

### 1. 认证模块 (/api/auth)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /register | 用户注册 |
| POST | /login | 用户登录 |
| GET | /me | 获取当前用户信息 |
| PUT | /nickname | 更新昵称 |

### 2. 简历诊断模块 (/api/resume)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /upload | 上传简历文件 (multipart) |
| GET | /task/{taskId} | 诊断任务详情 |
| GET | /history | 诊断历史 |
| POST | /job-match/analyze | 岗位匹配分析 |
| POST | /polish/analyze | 简历润色分析 |
| POST | /export-pdf | 导出PDF (需要Chrome) |

### 3. 模拟面试模块 (/api/interview)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /session | 创建面试会话 |
| POST | /session/{id}/message | 发送消息 (同步) |
| POST | /session/{id}/message/stream | 发送消息 (SSE流式) |
| GET | /session/{id} | 获取会话详情 |
| POST | /session/{id}/end | 结束面试，生成评估报告 |
| GET | /history | 面试历史 |
| GET | /job-roles | 获取岗位列表 |

### 4. 管理端 (/api/admin)

#### 4.1 岗位配置 (job-roles)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /job-roles | 岗位列表 |
| POST | /job-roles | 新增岗位 |
| PUT | /job-roles | 修改岗位 |
| PUT | /job-roles/{id}/active | 启用/禁用 |
| DELETE | /job-roles/{id} | 删除岗位 |
| DELETE | /job-roles/batch | 批量删除 |
| PUT | /job-roles/batch/active | 批量启用/禁用 |

#### 4.2 Prompt管理 (prompts)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /prompts | Prompt列表 |
| POST | /prompts | 新增Prompt |
| PUT | /prompts | 修改Prompt |
| PUT | /prompts/{id}/active | 启用/禁用 |
| DELETE | /prompts/{id} | 删除Prompt |
| DELETE | /prompts/batch | 批量删除 |
| PUT | /prompts/batch/active | 批量启用/禁用 |

#### 4.3 AI引擎配置 (ai-engines)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /ai-engines | AI引擎列表 |
| POST | /ai-engines | 新增AI引擎 |
| PUT | /ai-engines | 修改AI引擎 |
| PUT | /ai-engines/{id}/active | 启用/禁用 |
| DELETE | /ai-engines/{id} | 删除AI引擎 |
| DELETE | /ai-engines/batch | 批量删除 |
| PUT | /ai-engines/batch/active | 批量启用/禁用 |

#### 4.4 用户管理 (users)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /users | 用户列表 |
| GET | /users/{userId}/rights | 用户权益详情 |
| PUT | /users/{userId}/rights | 修改用户权益 |
| PUT | /users/{userId}/status | 封禁/解封用户 |
| PUT | /users/batch/status | 批量封禁/解封 |
| GET | /users/{userId}/quota | 用户额度 |
| PUT | /users/quota | 调整用户额度 |

#### 4.5 看板统计 (dashboard)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /dashboard/overview | 总览统计 |
| GET | /dashboard/trends | 趋势统计 |
| GET | /dashboard/hot-job-roles | 热门岗位排行 |
| GET | /dashboard/business-distribution | 业务分布 |

#### 4.6 监控 (monitor)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /monitor/overview | 监控总览 |

### 5. 会员模块 (/api/membership)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /plans | 会员套餐列表 |
| POST | /upgrade | 会员升级 |

## 核心业务规则

### 1. Prompt启用规则
- 同一岗位 + 同一难度只能有一个启用状态
- 启用新Prompt时自动禁用同岗位同难度的其他Prompt

### 2. AI引擎启用规则
- 同一业务类型(interview/resume)只能有一个启用配置
- 启用新引擎时自动禁用同业务类型的其他配置

### 3. 用户额度规则
- 普通用户：有基础额度限制
- 会员用户：无额度限制（有VIP有效期）
- 每日额度重置机制

### 4. 面试会话规则
- 会话创建时需要扣除额度
- 结束时生成评价报告

### 5. 管理端删除策略
- 管理端CRUD（Prompts、岗位、AI引擎）使用物理删除
- 业务实体使用MyBatis-Plus逻辑删除（`isDeleted`字段）

### 6. API Key安全
- 管理端返回的API Key已脱敏（如 `sk-****abcd`）
- 后端校验脱敏值不会被写回数据库

## 数据库表

| 表名 | 说明 |
|------|------|
| sys_user | 用户表 |
| user_quota | 用户额度表 |
| sys_job_role | 岗位配置表 |
| sys_prompt | Prompt模板表 |
| sys_ai_engine_config | AI引擎配置表 |
| membership_plan | 会员套餐表 |
| membership_order | 会员订单表 |
| resume_diagnosis_task | 简历诊断任务表 |
| interview_session | 面试会话表 |
| interview_chat_log | 面试对话记录表 |

## 管理端功能

### 批量操作说明

| 模块 | 支持操作 |
|------|----------|
| 岗位配置 | 单条/批量删除、单条/批量启用/禁用 |
| Prompt管理 | 单条/批量删除、单条/批量启用/禁用 |
| AI引擎配置 | 单条/批量删除、单条/批量启用/禁用 |
| 用户权益 | 批量封禁/批量解封 |

### 管理端入口
- 管理端页面路径: `/admin`
- 需要管理员权限访问

## AI配置调试

提供调试接口查看AI配置状态：
```
GET /api/admin/ai-config/debug
```

返回内容：
- 当前运行模式(real/mock)
- 已配置的Provider
- API Key配置状态
- 是否可以使用真实AI
