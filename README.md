# 智能模拟面试与简历诊断系统 - 后端服务

## 项目简介

智能模拟面试与简历诊断系统后端服务，为求职者提供AI驱动的模拟面试和简历诊断能力。

## 项目状态

✅ **所有核心模块已完成，最终交付版本 v1.0.0**

## 技术栈

| 类别 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.3 |
| JDK版本 | 21 |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | MySQL 8.0+ |
| 缓存 | Redis 6.0+ |
| 消息队列 | RabbitMQ |
| 认证 | JWT + Spring Security |
| AI接入 | 支持多Provider (豆包/DeepSeek/通义千问/OpenAI) |

## 环境要求

- JDK 21+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.8+

## 项目结构

```
server/src/main/java/com/airesume/server/
├── controller/          # 控制器层
│   ├── AuthController.java           # 认证接口
│   ├── ResumeDiagnosisController.java # 简历诊断接口
│   ├── InterviewController.java     # 模拟面试接口
│   ├── AdminController.java         # 管理端接口
│   ├── MembershipController.java    # 会员接口
│   ├── NetworkDiagnosticController.java # 网络诊断
│   └── AiConfigDebugController.java # AI配置调试
├── service/             # 业务逻辑层
│   ├── impl/            # 服务实现
│   │   ├── InterviewAiServiceImpl.java    # 真实AI面试服务
│   │   ├── MockInterviewAiServiceImpl.java # Mock面试服务
│   │   ├── ResumeAiServiceImpl.java       # 真实AI简历服务
│   │   └── MockResumeAiServiceImpl.java    # Mock简历服务
│   └── *Service.java    # 服务接口
├── entity/              # 实体类
├── mapper/              # 数据访问层
├── dto/                 # 数据传输对象
├── common/              # 公共组件
│   ├── constants/       # 常量定义
│   │   └── AiEngineConstants.java  # AI引擎常量
│   ├── result/          # 统一响应
│   ├── exception/       # 异常处理
│   └── util/            # 工具类
├── config/              # 配置类
├── infrastructure/       # 基础设施
│   └── security/         # 安全组件
└── mock/                # Mock实现(备用)
```

## AI接入说明

### 支持的AI Provider

| Provider | 环境变量Key | API地址 | 说明 |
|----------|-------------|---------|------|
| 豆包(Doubao) | DOUBAO_API_KEY | https://ark.cn-beijing.volces.com/api/v3 | 字节跳动的豆包模型 |
| 通义千问(Qwen) | QWEN_API_KEY | https://dashscope.aliyuncs.com/compatible-mode/v3 | 阿里云的通义千问 |
| OpenAI | OPENAI_API_KEY | https://api.openai.com/v1 | OpenAI官方API |
| Kimi | KIMI_API_KEY | https://api.moonshot.cn/v1 | 月之暗面Kimi |
| DeepSeek | DEEPSEEK_API_KEY | https://api.deepseek.com/v1 | DeepSeek模型 |

### 配置方式

1. **管理端配置** (推荐)
   - 访问管理端 `AI引擎配置` 页面
   - 添加AI引擎配置，填写Provider、模型名、API Key等
   - 支持多引擎配置，可以在不同业务线使用不同AI

2. **环境变量配置** (备用)
   - 在系统环境变量中设置对应的API Key
   - 例如：`export DOUBAO_API_KEY="your-api-key"`

### 运行模式

| 模式 | 配置 | 说明 |
|------|------|------|
| Mock | `app.interview.mode=mock` | 使用本地Mock回复，不调用AI |
| Real | `app.interview.mode=real` | 调用配置的AI Provider生成回复 |

- **默认模式**：Mock模式（当未配置时）
- **切换方式**：通过配置文件或环境变量修改模式

### AI调用流程

```
请求 → 检查配置 → 获取API Key → 调��对应Provider → 解析响应 → 返回结果
         ↓
    配置优先 → 环境变量备用 → Mock兜底
```

## API接口文档

### 1. 认证模块 (/api/auth)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /register | 用户注册 |
| POST | /login | 用户登录 |
| GET | /me | 获取当前用户信息 |

### 2. 简历诊断模块 (/api/resume)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /upload | 上传简历文件 |
| POST | /diagnose | 创建诊断任务 |
| GET | /history | 诊断历史 |
| GET | /result/{taskId} | 诊断结果 |

### 3. 模拟面试模块 (/api/interview)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /sessions | 创建面试会话 |
| POST | /sessions/{sessionId}/message | 发送消息 |
| GET | /sessions/{sessionId} | 获取会话详情 |
| GET | /sessions/{sessionId}/history | 对话历史 |
| POST | /sessions/{sessionId}/end | 结束面试 |
| GET | /job-roles | 获取岗位列表 |
| GET | /sessions | 我的面试列表 |

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

**规则**：同岗位 + 同难度 只能有一个启用

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

**规则**：同业务类型(interview/resume)只能有一个启用

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

## 快速开始

### 1. 配置数据库连接
修改 `application.yml` 中的数据库、Redis、RabbitMQ配置

### 2. 配置AI Provider（可选）
在管理端添加AI引擎配置，或设置环境变量

### 3. 启动服务
```bash
cd server
mvn spring-boot:run
```

### 4. 访问健康检查
```
GET /api/auth/health
```

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

## 许可证

Copyright © 2024