# AI Resume Frontend

智能模拟面试与简历诊断系统 — 基于 Vue 3 的前端单页应用，提供 AI 简历诊断、模拟面试、JD 匹配、AI 简历润色、模板编辑、会员体系及管理后台等功能。

## 技术栈

| 类别 | 技术 | 说明 |
|------|------|------|
| 框架 | Vue 3.4 | Composition API + `<script setup>` |
| 构建工具 | Vite 5 | 开发服务器 + 生产构建 |
| 路由 | Vue Router 4 | 用户端 + 管理端双路由守卫 |
| 状态管理 | Pinia 2 | user / adminUser / theme / templateEditor |
| UI 组件库 | Element Plus 2.4 | 全组件引入 + 自定义主题覆盖 |
| HTTP 客户端 | Axios 1.6 | 双实例（用户端 + 管理端），请求/响应拦截器 |
| 富文本编辑 | TipTap 3 | 基于 ProseMirror，用于简历模板编辑器 |
| 图表 | Chart.js 4 + vue-chartjs 5 | 管理后台数据可视化 |
| PDF 处理 | pdfjs-dist / html2canvas / jsPDF | 扫描件检测、导出 PDF/图片 |

## 快速开始

### 环境要求

- Node.js >= 18
- 后端服务运行在 `localhost:8080`（详见项目根目录 README）

### 安装与运行

```bash
# 安装依赖
npm install

# 启动开发服务器 (localhost:3000)
npm run dev

# 生产构建
npm run build

# 预览生产构建
npm run preview
```

开发模式下，Vite 会将 `/api` 和 `/auth` 请求代理到 `http://localhost:8080`。

## 目录结构

```
src/
├── main.js                    # 应用入口
├── App.vue                    # 根组件（动态布局切换）
├── styles/
│   └── index.css              # 全局样式：设计令牌、暗色主题、Element Plus 覆盖
├── router/
│   └── index.js               # 路由配置与守卫
├── api/                       # API 模块
│   ├── auth.js                # 认证：登录/注册/获取用户信息
│   ├── resume.js              # 简历：上传/诊断/JD匹配/AI润色
│   ├── interview.js           # 面试：创建会话/流式对话/历史记录
│   ├── membership.js          # 会员：套餐/升级
│   ├── notification.js        # 通知：列表/未读数/已读
│   ├── onboarding.js          # 新手引导状态
│   ├── growth.js              # 成长中心
│   └── admin/                 # 管理端 API
│       ├── auth.js            # 管理员登录
│       ├── dashboard.js       # 数据概览/趋势/热门职位
│       ├── monitor.js         # 系统监控
│       ├── jobRoles.js        # 职位角色 CRUD
│       ├── prompts.js         # 提示词模板 CRUD
│       ├── aiEngines.js       # AI 引擎配置 CRUD
│       └── users.js           # 用户权益/配额管理
├── stores/                    # Pinia 状态管理
│   ├── user.js                # 用户状态：登录/登出/VIP判断
│   ├── adminUser.js           # 管理员状态：角色校验(role=9)
│   ├── theme.js               # 主题：light/dark/system 跟随系统
│   └── templateEditor.js      # 模板编辑器：简历数据/段落配置 CRUD
├── utils/                     # 工具模块
│   ├── auth.js                # 用户 Token 管理
│   ├── request.js             # 用户端 Axios 实例
│   ├── adminAuth.js           # 管理员 Token 管理
│   ├── adminRequest.js        # 管理端 Axios 实例（长整数精度保留）
│   └── adminFeedback.js       # 管理端反馈工具函数
├── layouts/
│   ├── MainLayout.vue         # 用户端布局：顶部导航 + 内容区 + 新手引导
│   └── AdminLayout.vue        # 管理端布局：顶栏 + 侧边导航 + 内容区
├── views/                     # 页面视图
│   ├── HomePageView.vue       # 首页（落地页）
│   ├── DashboardView.vue      # 用户仪表盘
│   ├── MembershipView.vue     # 会员中心
│   ├── auth/
│   │   └── LoginView.vue      # 登录/注册
│   ├── resume/
│   │   ├── UploadView.vue     # 简历上传（PDF 扫描件检测）
│   │   ├── ResultView.vue     # 诊断结果（雷达图/评分/AI润色/导出）
│   │   └── HistoryView.vue    # 诊断历史
│   ├── interview/
│   │   ├── InterviewEntryView.vue     # 面试配置
│   │   ├── InterviewSessionView.vue   # 实时面试（SSE 流式）
│   │   ├── InterviewHistoryView.vue   # 面试历史
│   │   └── InterviewReportView.vue    # 面试报告
│   ├── template/
│   │   ├── TemplateLibraryView.vue    # 模板库（8 大行业 17 个模板）
│   │   └── TemplateEditorView.vue     # 模板编辑器（分栏编辑+实时预览）
│   ├── growth/
│   │   └── GrowthCenterView.vue       # 成长中心
│   ├── notification/
│   │   └── NotificationView.vue       # 通知中心
│   └── admin/                         # 管理后台页面
│       ├── AdminLoginView.vue         # 管理员登录
│       ├── AdminDashboardView.vue     # 数据看板（Chart.js）
│       ├── AdminMonitorView.vue       # 系统监控
│       ├── AdminJobRoleView.vue       # 职位角色管理
│       ├── AdminPromptView.vue        # 提示词模板管理
│       ├── AdminAiEngineView.vue      # AI 引擎配置管理
│       └── AdminUserRightsView.vue    # 用户权益管理
├── components/                # 可复用组件
│   ├── AppHeader.vue          # 全局导航栏（主题切换/通知铃铛/用户菜单）
│   ├── OnboardingGuide.vue    # 新手引导弹窗
│   ├── resume/                # 简历相关组件
│   │   ├── ResumeTemplate.vue         # 简历模板渲染（预览+导出）
│   │   ├── resumeTemplateParser.js    # AI 文本→结构化简历解析器
│   │   ├── ResumeRichBlockEditor.vue  # 富文本块编辑器
│   │   ├── RadarChart.vue             # 五维雷达图
│   │   ├── RadarScorePanel.vue        # 评分明细面板
│   │   └── ...                        # 各诊断维度展示组件
│   └── template/              # 模板相关组件
│       ├── IndustryFilter.vue         # 行业筛选器
│       ├── TemplateCard.vue           # 模板卡片
│       ├── TemplatePreviewDialog.vue  # 模板预览弹窗
│       ├── ExportToolbar.vue          # 导出工具栏（PDF/图片）
│       └── editor/                    # 编辑器子组件
├── data/                      # 静态数据
│   ├── industries.js          # 9 个行业分类
│   ├── templates.js           # 17 个模板定义
│   └── contents/              # 各模板默认简历数据（16 个文件）
└── assets/                    # 静态资源
    ├── logo.jpg
    ├── user.png
    └── assistant.png
```

## 页面路由

### 用户端

| 路径 | 页面 | 需要登录 |
|------|------|----------|
| `/` | 首页 | 否 |
| `/login` | 登录/注册 | 否 |
| `/dashboard` | 仪表盘 | 是 |
| `/membership` | 会员中心 | 是 |
| `/resume/upload` | 简历上传 | 是 |
| `/resume/result/:taskId` | 诊断结果 | 是 |
| `/resume/history` | 诊断历史 | 是 |
| `/interview/entry` | 面试配置 | 是 |
| `/interview/session/:sessionId` | 实时面试 | 是 |
| `/interview/history` | 面试历史 | 是 |
| `/interview/report/:sessionId` | 面试报告 | 是 |
| `/templates` | 模板库 | 是 |
| `/templates/editor/:templateId` | 模板编辑器 | 是 |
| `/growth` | 成长中心 | 是 |
| `/notifications` | 通知中心 | 是 |

### 管理端

| 路径 | 页面 | 需要管理员权限 |
|------|------|----------------|
| `/admin/login` | 管理员登录 | 否 |
| `/admin/dashboard` | 数据看板 | 是 (role=9) |
| `/admin/job-roles` | 职位角色管理 | 是 |
| `/admin/prompts` | 提示词模板管理 | 是 |
| `/admin/ai-engines` | AI 引擎配置 | 是 |
| `/admin/users` | 用户权益管理 | 是 |
| `/admin/monitor` | 系统监控 | 是 |

## 核心功能

### 简历诊断

- PDF 上传，客户端扫描件检测（pdfjs-dist 提取文本）
- 异步诊断：上传 → 后端消息队列 → AI 分析 → 结果入库
- 五维评分雷达图（基本信息/技能/工作经历/项目经历/教育背景）
- JD 匹配分析、AI 简历润色（支持复制/导出 PDF/图片）

### 模拟面试

- 可选职位角色、难度（1-3）、模式（普通/压力）
- 可选 JD 文本和简历关联
- SSE 流式 AI 回答，实时对话交互
- 面试报告与评分

### 模板系统

- 8 大行业 17 个简历模板，支持行业筛选
- 分栏编辑器：左侧编辑面板 + 右侧实时预览
- 段落自由增删排序、双击重命名、自定义段落类型
- 9 种段落类型：个人简介、技能清单、经历条目、证书资质、获奖荣誉、语言能力、兴趣爱好、自定义文本段、自定义经历
- 导出为 PDF 或 PNG 图片
- **CSS 特异性**：模板 CSS 通过 `import(... ?raw)` 加载并注入为非 scoped `<style>`，基线默认样式应写在 `TemplateRenderer.vue` 的 scoped styles 中，模板 CSS 仅做 `.resume-tpl-{id}` 下的覆盖

### 主题系统

- 三种模式：浅色 / 深色 / 跟随系统
- `index.html` 内联脚本防止主题闪烁
- CSS 自定义属性（40+ 设计令牌）+ Element Plus 深色变量桥接
- 响应式断点：1280px / 1024px / 768px / 480px

### 布局切换

`App.vue` 根据 `route.meta.useLayout` 动态渲染 `MainLayout` 或裸 `<div>`。面试会话、模板编辑器等全屏页面跳过主布局。

### 简历数据模型

`stores/templateEditor.js` 中 `resumeData` 的结构：

```
{ basic: { name, phone, email, ... }, summary: string, skills: string[], education: [...], work: [...], projects: [...] }
```

编辑器新增的自定义段落使用动态 key（如 `text-{timestamp}`、`experience-{timestamp}`）存储在内置字段旁边。

## 构建优化

Vite 配置了手动代码分割：

| Chunk | 包含 |
|-------|------|
| `vue-vendor` | vue / vue-router / pinia |
| `element-plus-vendor` | element-plus / @element-plus/icons-vue |
| `axios-vendor` | axios |
| `chart-vendor` | chart.js / vue-chartjs |

## 认证机制

- **用户端**：Token 存储于 `localStorage`（key: `ai_resume_token`），Axios 拦截器自动注入 `Authorization` 头
- **管理端**：独立 Token（key: `ai_resume_admin_token`），管理员角色需 role=9
- 401 响应自动跳转登录页，管理端使用互斥锁防止并发重定向循环
- 管理端请求拦截器预处理 JSON，防止 JavaScript 长整数精度丢失
- 个别请求可在 config 中设置 `skipDefaultErrorHandler = true` 跳过全局错误弹窗，适用于需要自行处理错误的长时间请求（如简历诊断轮询）
