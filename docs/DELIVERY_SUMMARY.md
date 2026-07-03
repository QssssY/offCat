# offcat 交付总结

## 项目概述

offcat 是一个面向求职场景的 AI 全栈应用。当前仓库整合了前端应用与后端服务，并保留两个原始主分支的提交历史。

## 当前交付范围

| 范围 | 路径 | 说明 |
|------|------|------|
| 前端应用 | `frontend/app` | Vue 3 + Vite 用户端与管理端 |
| 后端服务 | `server` | Spring Boot 3.2 + JDK 21 API 服务 |
| 数据库结构 | `server/db/schema.sql` | MySQL 表结构与基础数据 |
| 公开文档 | `docs` | API、演示、测试、验收文档 |

## 已具备能力

### 用户端

- 注册、登录、JWT 鉴权、昵称、密码、密保问题、账号注销。
- 简历上传、异步诊断、阶段进度、失败重试、历史记录。
- JD 匹配、AI 简历润色、结构化文档保存、PDF 导出。
- 模拟面试会话、SSE 流式对话、语音交互、面试报告、六维评分、历史记录。
- 简历模板库、模板编辑器、PDF/PNG/DOCX 导出。
- Offer 助手、成长中心、会员套餐、配额消耗记录。
- 通知中心和 SSE 实时推送。
- 社区帖子、评论、回复、图片上传、点赞、收藏、面试报告分享。
- 用户设置、自定义 AI 配置、自定义 TTS、反馈、主题切换、数据保留。

### 管理端

- 独立管理员登录和角色校验。
- 数据看板、趋势统计、业务监控。
- 用户权益、封禁、解封、配额、消耗记录。
- 岗位、Prompt、AI 引擎、TTS 配置。
- 会员套餐、订单、系统通知、版本日志。
- 审计日志、成长配置、社区审核、反馈管理。
- 自定义 AI 用量统计、趋势和每日限额。

### 后端工程能力

- Spring Security + JWT 无状态鉴权。
- MyBatis-Plus 为主，部分 JPA 仓库辅助。
- Redis 缓存、限流与降级。
- RabbitMQ 简历诊断排队与兜底。
- AI Provider 多配置来源：数据库、配置文件、环境变量。
- 平台 AI Key 与用户自定义 AI Key 加密存储和脱敏返回。
- PDFBox 文本解析、OCR/多模态解析配置、Headless Chrome PDF 导出。
- 统一异常处理、统一响应、分页响应、审计日志。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3、Vite、Vue Router、Pinia、Element Plus、Naive UI、Chart.js、TipTap、Vitest |
| 后端 | Spring Boot 3.2.11、JDK 21、MyBatis-Plus、Spring Security、WebFlux、JWT |
| 基础设施 | MySQL、Redis、RabbitMQ、Headless Chrome、阿里云 OSS |
| AI | OpenAI 兼容接口，多 Provider 配置，SSE 流式响应，TTS |

## 运行条件

- Node.js >= 18
- JDK 21+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.8+
- Chrome（PDF 导出）
- 外部 AI、TTS、OSS 能力按实际演示或生产环境配置

## 关键文档

| 文档 | 说明 |
|------|------|
| [根 README](../README.md) | 项目介绍、启动方式和文档导航 |
| [前端 README](../frontend/app/README.md) | 前端架构、路由、页面、组件、测试 |
| [后端 README](../server/README.md) | 后端架构、配置、API、业务规则 |
| [API 索引](./api/API_INDEX.md) | 当前 API 模块导航 |
| [演示指南](./DEMO_GUIDE.md) | 本地演示准备和要点 |
| [演示流程](./DEMO_FLOW.md) | 可执行演示路线 |
| [测试清单](./TEST_CHECKLIST.md) | 提交前检查项 |
| [验收清单](./acceptance/TASK_06_ACCEPTANCE_CHECKLIST.md) | 全栈验收项 |

## 交付边界

- 当前仓库不应提交生产密钥、生产密码、真实 Token 或私有凭据。
- 外部 AI、TTS、OSS 的可用性取决于运行环境配置和网络。
- PDF 导出依赖后端可执行 Chrome。
- 本文档只描述当前代码已包含或已预留的能力，不代表所有外部服务已在本机配置完成。

## 建议验收命令

```bash
cd server
mvn -DskipTests compile
mvn test
```

```bash
cd frontend/app
npm run test
npm run build
```

## 交付结论

当前仓库具备前端、后端、数据库结构和公开说明文档，能够作为 offcat 的合并单仓库继续开发、演示和部署。提交前仍需按目标环境执行构建、测试、安全和敏感内容检查。
