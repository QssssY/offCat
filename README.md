# AI Resume — 智能简历诊断与模拟面试系统

> 本仓库已经由原 `frontend/app` 前端仓库和 `server` 后端仓库合并为一个单体 Git 仓库。前端、后端、数据库脚本、任务文档和运行规范统一在根目录管理。

## 项目简介

AI Resume 是一个面向求职者的 AI 驱动平台，提供简历诊断、模拟面试、JD 匹配、AI 简历润色、薪资谈判模拟、简历模板编辑、社区交流、用户自定义 AI 引擎等全套求职辅助能力，附带完整的管理后台。

### 核心功能

| 模块 | 能力 |
|------|------|
| 认证与安全 | JWT 认证、密保问题、密码重置、账号注销、限流防护 |
| 简历诊断 | PDF/DOC 上传、异步 AI 分析、五维评分雷达图、失败重试、多模态提取 |
| 模拟面试 | 6 种面试模式、SSE 流式对话、语音交互、面试报告与维度评分 |
| JD 匹配 | 简历与岗位描述匹配度分析 |
| AI 简历润色 | AI 驱动的简历内容优化、一键复制/导出 PDF/PNG/DOCX |
| 简历模板 | 8 大行业 17 个模板、分栏编辑器、实时预览、9 种段落类型 |
| Offer 助手 | 薪资谈判模拟、话术脚本生成 |
| 社区 | 帖子/评论/点赞/收藏、面试报告分享、内容审核 |
| 成长中心 | 面试维度雷达、薄弱项分析、任务引导 |
| 用户设置 | 自定义 AI 引擎、面试偏好、语音配置、数据管理、反馈提交 |
| 会员体系 | 月卡/季卡/年卡、配额管理、周期额度 |
| 通知系统 | SSE 实时推送、多类型通知 |
| 管理后台 | 看板/监控/用户/Prompt/AI 引擎/社区审核/反馈/版本日志/审计日志 |

## 技术栈

### 后端

| 类别 | 技术 |
|------|------|
| 框架 | Spring Boot 3.2.11, JDK 21 |
| ORM | MyBatis-Plus 3.5.7（主） + Spring Data JPA（辅） |
| 数据库 | MySQL 8.0+ |
| 缓存 | Redis 6.0+ (Lettuce) |
| 消息队列 | RabbitMQ 3.8+ |
| 认证 | JWT (jjwt 0.12.3) + Spring Security |
| HTTP 客户端 | RestClient（非流式） + WebClient/SSE（流式） |
| PDF | Apache PDFBox 3.0.1, Headless Chrome 导出 |
| AI 接入 | 多 Provider (豆包/DeepSeek/通义千问/OpenAI/Kimi/MiniMax/文心) |

### 前端

| 类别 | 技术 |
|------|------|
| 框架 | Vue 3.4 (Composition API + `<script setup>`) |
| 构建 | Vite 5 |
| 路由 | Vue Router 4 |
| 状态管理 | Pinia 2 |
| UI 组件库 | Element Plus 2.4 + Naive UI 2.44（选择性） |
| 富文本 | TipTap 3 (ProseMirror) |
| 图表 | Chart.js 4 + vue-chartjs 5 |
| 测试 | Vitest 4 + @vue/test-utils |

## 仓库合并说明

- 当前根目录是新的 Git 仓库，默认分支为 `master`。
- 前端代码位于 `frontend/app/`，来源为原前端仓库 `master` 分支快照。
- 后端代码位于 `server/`，来源为原后端仓库 `master` 分支快照。
- 原两个子仓库的 `.git` 元数据已移入 `.legacy-git/` 作为本地备份，该目录已被根 `.gitignore` 忽略，不会进入提交。
- 根 `.gitignore` 已融合前端与后端忽略规则，覆盖 Node 依赖、Vite 构建产物、Maven 构建产物、日志、上传文件、本地环境配置、IDE 状态、测试报告和缓存。
- 子目录内仍保留 `frontend/app/.gitignore` 与 `server/.gitignore`，用于保留各自历史规则；根 `.gitignore` 是合并后单仓库的主入口。

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.6+
- MySQL 8.0+（端口 3306）
- Redis 6.0+（端口 6379）
- RabbitMQ 3.8+（端口 5672）
- Node.js >= 18
- Chrome（仅 PDF 导出需要）

### 1. 初始化数据库

```bash
mysql -u root -p ai_resume < db/schema.sql
```

增量迁移脚本位于 `db/migrations/` 和 `server/db/migrations/`，按当前部署目标选择并按文件名顺序执行。涉及结构变更时，两处脚本需要保持一致。

### 2. 启动后端

```bash
cd server
# 编辑 src/main/resources/application-dev.yml 配置数据库等连接信息
mvn spring-boot:run
```

服务启动在 `:8080`，访问 `GET /api/auth/health` 验证。

### 3. 启动前端

```bash
cd frontend/app
npm install
npm run dev
```

开发服务器运行在 `:3000`，自动代理 `/api` 到 `:8080`。

详细说明参见 [server/README.md](server/README.md) 和 [frontend/app/README.md](frontend/app/README.md)。

## 项目结构

```
ai-resume/
├── .gitignore                   # 合并后的单仓库忽略规则
├── AGENTS.md                    # Agent 协作与开发要求
├── server/                      # 后端服务 (Spring Boot)
│   ├── src/                     # Java 源代码
│   ├── db/migrations/           # 后端侧数据库增量迁移脚本
│   └── pom.xml                  # Maven 配置
├── frontend/app/                # 前端应用 (Vue 3 SPA)
│   ├── src/                     # 前端源代码
│   └── package.json             # NPM 配置
├── db/
│   ├── schema.sql               # 完整数据库结构
│   └── migrations/              # 根目录数据库增量迁移脚本
├── docs/                        # 文档目录
│   └── api/                     # API 接口文档
├── tasks/                       # 后端任务、阶段和验收记录
├── frontend/                    # 前端任务和技能配置入口
├── runtime/                     # 运行时规则与状态
├── .legacy-git/                 # 原子仓库 Git 元数据本地备份（不提交）
└── README.md                    # 本文件
```

## 系统架构

```
Vue 3 SPA (:3000)  ──→  Spring Boot API (:8080)  ──→  MySQL / Redis / RabbitMQ
                                               │
                                               ├──→ AI Provider APIs (DeepSeek, 豆包, 通义千问, OpenAI...)
                                               └──→ Headless Chrome (PDF 导出)
```

### 关键架构特性

- **双 AI 服务模式**：Mock / Real 通过 `@ConditionalOnProperty` 切换
- **运行时 AI 配置**：三级优先级（数据库 > YAML > 环境变量）
- **智能路由**：低负载直接线程池，高负载回退 RabbitMQ
- **SSE 流式响应**：面试对话、通知推送
- **Token 预算管理**：估算 → 压缩 → 摘要三级管线
- **熔断器**：AI 服务故障时自动降级
- **限流保护**：Redis 计数器覆盖关键端点

## 文档索引

### API 文档

- [API 接口索引](docs/api/API_INDEX.md)
- [认证接口](docs/api/TASK_02_AUTH_API.md)
- [简历诊断接口](docs/api/TASK_04_RESUME_API.md)
- [模拟面试接口](docs/api/TASK_05_INTERVIEW_API.md)
- [管理端接口](docs/api/TASK_06A_ADMIN_API.md)

### 数据库

- [完整 Schema](db/schema.sql) — 31 张表
- [根目录数据库迁移脚本](db/migrations/) — 40 个增量迁移
- [后端数据库迁移脚本](server/db/migrations/) — 后端侧同步迁移脚本

## 开发与验证命令

- **后端**：512+ 测试用例 (JUnit 5 + Mockito)
- **前端**：78 测试文件 (Vitest + happy-dom/jsdom)

```bash
# 后端测试
cd server && mvn test

# 前端测试
cd frontend/app && npm run test

# 前端构建
cd frontend/app && npm run build
```

## Git 忽略策略

合并后的主忽略规则位于根目录 `.gitignore`。它显式覆盖：

- 前端依赖与构建产物：`node_modules/`、`dist/`、`dist-ssr/`、`test-results/`、`coverage/`。
- 后端构建产物：`target/`、`*.class`、Maven release 备份文件、Jacoco 和 Surefire/Failsafe 报告。
- 运行时数据：`logs/`、`uploads/`、`server/uploads/`。
- 本地配置：`.env*`、`application-*-local.*`、`.idea/`、本地 `.claude/settings.local.json`。
- 合并备份：`.legacy-git/`。

如后续新增构建目录、缓存目录或本地密钥文件，应优先补充根 `.gitignore`，避免只依赖子目录规则。

## 许可证

Copyright © 2024-2026
