# offcat

offcat 是一个面向求职场景的 AI 平台，覆盖简历诊断、模拟面试、JD 匹配、AI 润色、语音能力与后台管理。

## 项目简介

这是 offcat 的历史保留合并仓库：

- 前端代码位于 [frontend/app](frontend/app)
- 后端代码位于 [server](server)
- 公共说明文档位于 [docs](docs)

## 核心功能

- 简历诊断与任务跟踪
- 模拟面试、流式交互与 TTS
- JD 匹配与 AI 润色
- 通知、引导、设置与成长能力
- 管理后台、监控、反馈与版本日志

## 技术栈

- 前端：Vue 3、Vite、Vue Router、Pinia、Element Plus
- 后端：Spring Boot 3.2、JDK 21、MyBatis-Plus、Spring Data JPA
- 基础设施：MySQL、Redis、RabbitMQ、SSE、Headless Chrome

## 快速开始

### 环境要求

- Node.js >= 18
- JDK 21+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.8+
- Chrome

### 1. 初始化数据库

```bash
mysql -u root -p ai_resume < server/db/schema.sql
```

### 2. 启动后端

```bash
cd server
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`  
健康检查：`GET /actuator/health`

### 3. 启动前端

```bash
cd frontend/app
npm install
npm run dev
```

前端默认地址：`http://localhost:3000`

## 项目结构

| 路径 | 说明 |
|------|------|
| [frontend/app](frontend/app) | 前端应用 |
| [server](server) | 后端服务 |
| [server/db/schema.sql](server/db/schema.sql) | 数据库结构 |
| [docs](docs) | 对外公开文档镜像 |

## 文档导航

### 子项目说明

- [前端 README](frontend/app/README.md)
- [后端 README](server/README.md)

### API 文档

- [API 索引](docs/api/API_INDEX.md)
- [认证接口](docs/api/TASK_02_AUTH_API.md)
- [简历诊断接口](docs/api/TASK_04_RESUME_API.md)
- [模拟面试接口](docs/api/TASK_05_INTERVIEW_API.md)
- [管理端接口](docs/api/TASK_06A_ADMIN_API.md)

### 说明文档

- [演示指南](docs/DEMO_GUIDE.md)
- [演示流程](docs/DEMO_FLOW.md)
- [测试清单](docs/TEST_CHECKLIST.md)
- [验收清单](docs/acceptance/TASK_06_ACCEPTANCE_CHECKLIST.md)
- [交付总结](docs/DELIVERY_SUMMARY.md)

## 验证命令

```bash
# 后端
cd server
mvn test
mvn -DskipTests compile

# 前端
cd frontend/app
npm run test
npm run build
```

## 合并说明

本仓库在单仓库结构下保留了前端与后端原始 `master` 分支的提交历史。

## 许可证

Copyright © 2024-2026
