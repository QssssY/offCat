# offcat

offcat 是一个面向求职场景的 AI 应用，覆盖简历诊断、模拟面试、JD 匹配、AI 简历润色、简历模板编辑、社区互动、会员配额、用户自定义 AI 配置与管理后台。

本仓库是前端与后端两个主分支的历史保留合并仓库。前端代码位于 [frontend/app](frontend/app)，后端代码位于 [server](server)。

## 功能概览

- 用户认证：注册、登录、JWT 鉴权、昵称/密码/密保问题、账号注销。
- 简历能力：PDF/DOC/DOCX 上传、异步诊断、阶段进度、失败重试、JD 匹配、AI 润色、PDF 导出。
- 面试能力：岗位与难度配置、普通/压力/岗位定向等多模式面试、SSE 流式对话、语音交互、面试报告与六维评分。
- 模板能力：行业模板库、分栏编辑、实时预览、PDF/PNG/DOCX 导出。
- 用户中心：会员套餐、配额记录、通知、成长中心、Offer 助手、反馈、数据保留设置、自定义 AI 与 TTS 配置。
- 社区能力：帖子、评论、回复、图片上传、点赞、收藏、面试报告分享、互动提醒。
- 管理后台：用户权益、岗位、Prompt、AI 引擎、TTS、会员套餐、订单、通知、版本日志、审计、监控、社区审核、反馈、自定义 AI 用量。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3、Vite、Vue Router、Pinia、Element Plus、Chart.js、TipTap、Vitest |
| 后端 | Spring Boot 3.2、JDK 21、MyBatis-Plus、Spring Security、JWT、RabbitMQ、Redis、MySQL |
| AI 与文档 | OpenAI 兼容接口、多 Provider 配置、PDFBox、Headless Chrome、阿里云 OSS |

## 快速开始

### 环境要求

- Node.js >= 18
- JDK 21+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.8+
- Chrome（仅 PDF 导出需要）

### 初始化数据库

```bash
mysql -u root -p ai_resume < server/db/schema.sql
```

### 启动后端

```bash
cd server
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`

### 启动前端

```bash
cd frontend/app
npm install
npm run dev
```

前端默认地址：`http://localhost:3000`

## 项目结构

| 路径 | 说明 |
|------|------|
| [frontend/app](frontend/app) | Vue 前端应用 |
| [server](server) | Spring Boot 后端服务 |
| [server/db/schema.sql](server/db/schema.sql) | 数据库结构与基础种子数据 |
| [docs](docs) | 公开说明、API、演示、测试与验收文档 |

## 文档导航

| 文档 | 内容 |
|------|------|
| [前端说明](frontend/app/README.md) | 前端技术栈、路由、页面、组件、测试与构建说明 |
| [后端说明](server/README.md) | 后端架构、配置、业务规则、API 模块与数据库表说明 |
| [API 索引](docs/api/API_INDEX.md) | 当前 Controller 暴露接口的模块化索引 |
| [认证接口](docs/api/TASK_02_AUTH_API.md) | `/api/auth` 登录注册、当前用户、密码与密保接口 |
| [简历接口](docs/api/TASK_04_RESUME_API.md) | `/api/resume` 上传、诊断、重试、JD 匹配、润色、导出接口 |
| [面试接口](docs/api/TASK_05_INTERVIEW_API.md) | `/api/interview` 会话、流式对话、TTS、报告与历史接口 |
| [管理端接口](docs/api/TASK_06A_ADMIN_API.md) | `/api/admin` 后台配置、运营、监控、审核、TTS 与统计接口 |
| [演示指南](docs/DEMO_GUIDE.md) | 当前项目的本地演示准备、账号准备与演示要点 |
| [演示流程](docs/DEMO_FLOW.md) | 可按顺序执行的全栈功能演示路线 |
| [测试清单](docs/TEST_CHECKLIST.md) | 前后端构建、单测、接口、安全与核心链路检查项 |
| [验收清单](docs/acceptance/TASK_06_ACCEPTANCE_CHECKLIST.md) | 当前全栈项目验收项 |
| [交付总结](docs/DELIVERY_SUMMARY.md) | 当前仓库能力、边界、运行条件与交付说明 |

## 常用验证命令

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

## 安全说明

- 生产环境敏感配置应通过环境变量或安全配置中心注入，不应写入仓库。
- AI Provider Key、JWT 密钥、数据库密码、OSS 凭据等内容不得出现在提交记录或文档示例中。
- 管理端与用户自定义 AI Key 在后端以加密/脱敏方式处理。

## 许可证

Copyright © 2024-2026
