- # 智能模拟面试与简历诊断系统

  ## 项目简介

  智能模拟面试与简历诊断系统 - 后端服务，为求职者提供AI驱动的模拟面试和简历诊断能力。

  ### 核心功能

  - 用户认证与授权（JWT）
  - 用户额度管理（普通用户/会员）
  - 简历诊断（异步处理）
  - 模拟面试（多轮对话）
  - 历史记录查询
  - 管理端基础能力

  ## 项目状态

  ✅ **所有核心模块已完成，最终交付版本 v1.0.0**

  ## 快速开始

  ### 环境要求

  - JDK 21+
  - Maven 3.6+
  - MySQL 8.0+
  - Redis 6.0+
  - RabbitMQ 3.8+

  ### 启动步骤

  1. 创建数据库并初始化表结构
  2. 配置数据库、Redis、RabbitMQ连接
  3. 启动后端服务

  详细步骤请参考 [server/README.md](server/README.md)

  ## 模块进度

  | 模块             | 状态           |
  | ---------------- | -------------- |
  | TASK_01 基础工程 | ✅ 完成         |
  | TASK_02 认证模块 | ✅ 完成         |
  | TASK_03 额度模块 | ✅ 完成         |
  | TASK_04 简历诊断 | ✅ 完成（Mock） |
  | TASK_05 模拟面试 | ✅ 完成（Mock） |
  | TASK_06A 管理端  | ✅ 完成         |

  ## 技术栈

  - **后端框架**: Spring Boot 3.2.3
  - **JDK版本**: 21
  - **ORM**: MyBatis-Plus 3.5.7
  - **数据库**: MySQL 8.0+
  - **缓存**: Redis 6.0+
  - **消息队列**: RabbitMQ
  - **认证**: JWT + Spring Security

  ## 文档索引

  ### 交付文档

  - [交付总结](docs/DELIVERY_SUMMARY.md) - 项目完整交付总结
  - [测试验收清单](docs/TEST_CHECKLIST.md) - 测试与验收检查清单
  - [演示流程指南](docs/DEMO_GUIDE.md) - 项目演示流程建议

  ### API文档

  - [API接口索引](docs/api/API_INDEX.md) - 所有接口文档索引
  - [认证接口](docs/api/TASK_02_AUTH_API.md) - 用户认证相关接口
  - [简历诊断接口](docs/api/TASK_04_RESUME_API.md) - 简历诊断相关接口
  - [模拟面试接口](docs/api/TASK_05_INTERVIEW_API.md) - 模拟面试相关接口
  - [管理端接口](docs/api/TASK_06A_ADMIN_API.md) - 管理端相关接口

  ### 数据库设计

  - [数据库Schema](db/schema.sql) - 完整的数据库建表语句
  - [认证模块数据库](db/DB_AUTH.md) - 认证模块表结构说明
  - [简历诊断模块数据库](db/DB_RESUME.md) - 简历诊断模块表结构说明
  - [模拟面试模块数据库](db/DB_INTERVIEW.md) - 模拟面试模块表结构说明

  ## 当前版本说明

  ### 核心已完成 ✓

  | 模块     | 功能                                             |
  | -------- | ------------------------------------------------ |
  | 认证模块 | 用户注册、登录、JWT、获取当前用户                |
  | 额度模块 | 普通用户/会员用户次数校验与扣减                  |
  | 简历诊断 | PDF上传、任务创建、MQ入队、消费、Mock结果        |
  | 模拟面试 | 会话创建、消息记录、Mock回复、结束面试、历史查询 |
  | 管理端   | Prompt管理、用户管理、额度管理                   |

  ### Mock实现说明

  当前版本使用Mock实现替代真实大模型，后续接入真实大模型API后可替换：

  - 简历诊断结果：MockDiagnosisResultGenerator
  - 模拟面试回复：MockInterviewService
  - 模拟面试评分：MockInterviewService

  ### 后续增强项（可选）

  - TASK_04A 简历诊断AI接入（需要大模型API Key）
  - TASK_05A 模拟面试AI接入（需要大模型API Key）

  ## 项目结构

  ```
  ai-resume/
  ├── server/              # 后端服务
  │   ├── src/            # 源代码
  │   └── pom.xml         # Maven配置
  ├── db/                 # 数据库相关
  │   ├── schema.sql      # 建表语句
  │   └── DB_*.md         # 数据库设计文档
  ├── docs/               # 文档目录
  │   ├── api/            # API接口文档
  │   ├── DELIVERY_SUMMARY.md
  │   ├── TEST_CHECKLIST.md
  │   └── DEMO_GUIDE.md
  ├── tasks/              # 任务文件（开发中）
  ├── archive/            # 归档任务（已完成）
  ├── runtime/            # 运行时规则与状态
  └── README.md           # 本文件
  ```

  ## 许可证

  Copyright © 2024
