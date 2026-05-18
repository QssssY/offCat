# AI Resume Frontend

智能模拟面试与简历诊断系统 — 基于 Vue 3 的前端单页应用，提供 AI 简历诊断、模拟面试、JD 匹配、AI 简历润色、模板编辑、会员体系、Offer 助手、成长中心及管理后台等功能。

## 技术栈

| 类别 | 技术 | 说明 |
|------|------|------|
| 框架 | Vue 3.4 | Composition API + `<script setup>` |
| 构建工具 | Vite 5 | 开发服务器 + 生产构建 |
| 路由 | Vue Router 4 | 用户端 + 管理端双路由守卫 |
| 状态管理 | Pinia 2 | user / adminUser / theme / templateEditor |
| UI 组件库 | Element Plus 2.4 | 全组件引入 + 自定义主题覆盖 |
| 辅助 UI | Naive UI 2.44 | NMessageProvider 等选择性使用 |
| HTTP 客户端 | Axios 1.6 | 双实例（用户端 + 管理端），请求/响应拦截器 |
| 富文本编辑 | TipTap 3 | 基于 ProseMirror，用于简历模板编辑器 |
| 图表 | Chart.js 4 + vue-chartjs 5 | 管理后台数据可视化 |
| PDF 处理 | pdfjs-dist 5 / html2canvas / jspdf / html2pdf.js | 扫描件检测、导出 PDF/图片 |
| HTML 净化 | DOMPurify 3 | 富文本内容安全过滤 |
| 日期处理 | dayjs | 通过 Element Plus 集成 |
| 测试 | Vitest 4 + @vue/test-utils | 单元测试 (happy-dom / jsdom) |

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

# 运行测试
npm run test

# 监听模式运行测试
npm run test:watch
```

开发模式下，Vite 会将 `/api` 和 `/auth` 请求代理到 `http://localhost:8080`（`/api` 超时 120s）。

## 目录结构

```
src/
├── main.js                    # 应用入口：Pinia + Router + Element Plus
├── App.vue                    # 根组件（动态布局切换）
├── styles/
│   └── index.css              # 全局样式：40+ 设计令牌、暗色主题、Element Plus 覆盖
├── router/
│   └── index.js               # 路由配置与守卫（用户端 + 管理端独立认证）
├── api/                       # API 模块
│   ├── auth.js                # 认证：登录/注册/获取用户信息/密码重置/密保问题
│   ├── resume.js              # 简历：上传/诊断/JD匹配/AI润色
│   ├── resumePdf.js           # 简历 PDF 导出（独立 Axios 实例，blob 响应）
│   ├── interview.js           # 面试：创建会话/流式对话/历史记录
│   ├── membership.js          # 会员：套餐/升级
│   ├── notification.js        # 通知：列表/未读数/已读/SSE 实时推送
│   ├── onboarding.js          # 新手引导状态
│   ├── growth.js              # 成长中心
│   ├── offer.js               # Offer 助手：薪资谈判模拟/话术脚本
│   ├── versionLog.js          # 版本日志：最新 N 条
│   ├── publicVersionLog.js    # 版本日志：分页查询
│   ├── stats.js               # 统计：公开平台统计 + 用户月度统计
│   └── admin/                 # 管理端 API
│       ├── auth.js            # 管理员登录
│       ├── dashboard.js       # 数据概览/趋势/热门职位/业务分布
│       ├── monitor.js         # 系统监控
│       ├── jobRoles.js        # 职位角色 CRUD + 批量操作
│       ├── prompts.js         # 提示词模板 CRUD + 批量操作
│       ├── aiEngines.js       # AI 引擎配置 CRUD + 批量操作
│       ├── users.js           # 用户权益/配额管理 + 批量操作
│       ├── membership.js      # 会员套餐 CRUD + 订单查询
│       ├── auditLogs.js       # 审计日志查询
│       ├── notifications.js   # 系统通知 CRUD + 批量发布/删除
│       ├── versionLogs.js     # 版本日志 CRUD + 批量发布/删除
│       ├── userData.js        # 用户面试/简历任务历史
│       └── growthConfig.js    # 成长中心配置 CRUD + 批量删除
├── stores/                    # Pinia 状态管理
│   ├── user.js                # 用户状态：登录/登出/VIP判断
│   ├── adminUser.js           # 管理员状态：角色校验(role=9)，独立于用户 store
│   ├── theme.js               # 主题：light/dark/system 跟随系统
│   └── templateEditor.js      # 模板编辑器：简历数据/段落配置 CRUD
├── utils/                     # 工具模块
│   ├── auth.js                # 用户 Token 管理 (localStorage)
│   ├── request.js             # 用户端 Axios 实例（30s 超时，401 自动跳转）
│   ├── adminAuth.js           # 管理员 Token/Session 管理 (localStorage)
│   ├── adminRequest.js        # 管理端 Axios 实例（长整数精度保留，互斥锁防并发重定向）
│   ├── adminFeedback.js       # 管理端统一反馈工具（成功/错误/警告/确认弹窗）
│   ├── date.js                # 日期格式化
│   └── notificationMeta.js    # 通知类型元数据（标签/图标/时间格式化）
├── constants/
│   └── interview.js           # 面试常量：难度标签/面试模式/反馈模式
├── layouts/
│   ├── MainLayout.vue         # 用户端布局：顶部导航 + 内容区 + 页脚 + 新手引导
│   └── AdminLayout.vue        # 管理端布局：顶栏 + 侧边导航（4 组） + 内容区
├── views/                     # 页面视图
│   ├── HomePageView.vue       # 首页（落地页）
│   ├── DashboardView.vue      # 用户仪表盘
│   ├── MembershipView.vue     # 会员中心
│   ├── VersionLogView.vue     # 公共版本更新日志
│   ├── auth/
│   │   └── LoginView.vue      # 登录/注册
│   ├── resume/
│   │   ├── UploadView.vue     # 简历上传（PDF 扫描件检测）
│   │   ├── ResultView.vue     # 诊断结果（雷达图/评分/AI润色/导出）
│   │   └── HistoryView.vue    # 诊断历史
│   ├── interview/
│   │   ├── InterviewEntryView.vue     # 面试配置（职位/难度/模式/JD）
│   │   ├── InterviewSessionView.vue   # 实时面试（SSE 流式，全屏布局）
│   │   ├── InterviewHistoryView.vue   # 面试历史
│   │   └── InterviewReportView.vue    # 面试报告与评分
│   ├── template/
│   │   ├── TemplateLibraryView.vue    # 模板库（8 大行业 17 个模板）
│   │   └── TemplateEditorView.vue     # 模板编辑器（分栏编辑+实时预览，全屏布局）
│   ├── growth/
│   │   └── GrowthCenterView.vue       # 成长中心
│   ├── notification/
│   │   └── NotificationView.vue       # 通知中心
│   ├── offer/
│   │   └── OfferAssistView.vue        # Offer 助手（薪资谈判模拟）
│   └── admin/                         # 管理后台页面
│       ├── AdminLoginView.vue         # 管理员登录
│       ├── AdminDashboardView.vue     # 数据看板（Chart.js 可视化）
│       ├── AdminMonitorView.vue       # 系统监控
│       ├── AdminJobRoleView.vue       # 职位角色管理
│       ├── AdminPromptView.vue        # 提示词模板管理
│       ├── AdminAiEngineView.vue      # AI 引擎配置管理
│       ├── AdminUserRightsView.vue    # 用户权益管理
│       ├── AdminMembershipPlanView.vue  # 会员套餐管理
│       ├── AdminMembershipOrderView.vue # 会员订单管理
│       ├── AdminNotificationView.vue    # 系统通知管理
│       ├── AdminVersionLogView.vue      # 版本日志管理
│       ├── AdminAuditLogView.vue        # 审计日志查看
│       └── AdminGrowthConfigView.vue    # 成长中心配置管理
├── components/                # 可复用组件
│   ├── AppHeader.vue          # 全局导航栏（主题切换/通知铃铛/用户菜单）
│   ├── AppFooter.vue          # 全局页脚
│   ├── OnboardingGuide.vue    # 新手引导弹窗
│   ├── AiLoadingState.vue     # AI 处理加载指示器
│   ├── NotificationTypeIcon.vue # 通知类型图标
│   ├── resume/                # 简历相关组件
│   │   ├── ResumeTemplate.vue         # 简历模板渲染（预览+导出）
│   │   ├── resumeTemplateParser.js    # AI 文本→结构化简历解析器（940 行正则启发式解析）
│   │   ├── resumeExportStyles.js      # PDF 导出内联 CSS（非 scoped）
│   │   ├── ResumeRichBlockEditor.vue  # TipTap 富文本块编辑器
│   │   ├── ResumeInlineRichEditor.vue # 行内富文本编辑器
│   │   ├── RadarChart.vue             # 五维雷达图 (Chart.js)
│   │   ├── RadarScorePanel.vue        # 评分明细面板
│   │   ├── LineChart.vue              # 折线图组件
│   │   ├── BasicInfoSection.vue       # 基本信息展示
│   │   ├── SkillsSection.vue          # 技能展示
│   │   ├── WorkExperienceSection.vue  # 工作经历展示
│   │   ├── HighlightsSection.vue      # 亮点展示
│   │   ├── OptimizationSection.vue    # 优化建议展示
│   │   └── OverallEvaluation.vue      # 综合评价展示
│   ├── template/              # 模板相关组件
│   │   ├── TemplateCard.vue           # 模板卡片
│   │   ├── IndustryFilter.vue         # 行业筛选器
│   │   ├── TemplatePreviewDialog.vue  # 模板预览弹窗
│   │   ├── TemplatePreviewImage.vue   # 模板预览图
│   │   ├── TemplateRenderer.vue       # 模板渲染器（CSS + HTML）
│   │   ├── ResumeEditor.vue           # 分栏编辑器（左编辑+右预览）
│   │   ├── ExportToolbar.vue          # 导出工具栏（PDF/PNG）
│   │   └── editor/                    # 编辑器子组件
│   │       ├── SectionBasicInfo.vue       # 基本信息编辑
│   │       ├── SectionSummary.vue         # 个人简介编辑
│   │       ├── SectionSkills.vue          # 技能清单编辑
│   │       ├── SectionEducation.vue       # 教育背景编辑
│   │       ├── SectionWork.vue            # 工作经历编辑
│   │       ├── SectionProject.vue         # 项目经历编辑
│   │       ├── SectionCertifications.vue  # 证书资质编辑
│   │       ├── SectionAwards.vue          # 获奖荣誉编辑
│   │       ├── SectionLanguages.vue       # 语言能力编辑
│   │       ├── SectionInterests.vue       # 兴趣爱好编辑
│   │       ├── SectionText.vue            # 自定义文本段编辑
│   │       ├── ExperienceItemEditor.vue   # 通用经历条目编辑器
│   │       └── SkillTagInput.vue          # 标签式技能输入
│   └── empty/                 # 空状态组件
│       ├── ResumeEmpty.vue
│       └── InterviewEmpty.vue
├── data/                      # 静态数据
│   ├── industries.js          # 9 个行业分类（含"全部"筛选项）
│   ├── templates.js           # 17 个模板定义（8 大行业）
│   ├── styles/                # 17 个模板 CSS 文件（运行时 import ?raw 加载）
│   └── contents/              # 各模板默认简历数据（16 个文件 + _default.js）
└── assets/                    # 静态资源
    ├── logo.jpg
    ├── user.png
    └── assistant.png
```

## 页面路由

### 用户端

| 路径 | 页面 | 布局 | 需要登录 |
|------|------|------|----------|
| `/` | 首页 | MainLayout | 否 |
| `/login` | 登录/注册 | 无 | 否 |
| `/version-logs` | 版本更新日志 | MainLayout | 否 |
| `/dashboard` | 仪表盘 | MainLayout | 是 |
| `/membership` | 会员中心 | MainLayout | 是 |
| `/growth` | 成长中心 | MainLayout | 是 |
| `/offer` | Offer 助手 | MainLayout | 是 |
| `/notifications` | 通知中心 | MainLayout | 是 |
| `/resume/upload` | 简历上传 | MainLayout | 是 |
| `/resume/result/:taskId` | 诊断结果 | MainLayout | 是 |
| `/resume/history` | 诊断历史 | MainLayout | 是 |
| `/interview/entry` | 面试配置 | MainLayout | 是 |
| `/interview/session/:sessionId` | 实时面试 | 无 (全屏) | 是 |
| `/interview/history` | 面试历史 | MainLayout | 是 |
| `/interview/report/:sessionId` | 面试报告 | MainLayout | 是 |
| `/templates` | 模板库 | MainLayout | 是 |
| `/templates/editor/:templateId` | 模板编辑器 | 无 (全屏) | 是 |

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
| `/admin/notifications` | 系统通知管理 | 是 |
| `/admin/version-logs` | 版本日志管理 | 是 |
| `/admin/audit-logs` | 审计日志查看 | 是 |
| `/admin/membership/plans` | 会员套餐管理 | 是 |
| `/admin/membership/orders` | 会员订单管理 | 是 |
| `/admin/growth-config` | 成长中心配置 | 是 |

## 核心功能

### 简历诊断

- PDF 上传，客户端扫描件检测（pdfjs-dist 提取文本）
- 异步诊断：上传 → 后端消息队列 → AI 分析 → 结果入库
- 五维评分雷达图（基本信息/技能/工作经历/项目经历/教育背景）
- JD 匹配分析、AI 简历润色（支持复制/导出 PDF/图片）

### 模拟面试

- 可选职位角色、难度（初级/中级/高级）、模式（普通/压力/岗位定向/大厂HR/技术负责人/外企面试官）
- 可选 JD 文本和简历关联
- SSE 流式 AI 回答，实时对话交互
- 面试报告与评分
- 反馈模式：面试结束后反馈 / 即时反馈

### Offer 助手

- 薪资谈判模拟演练
- 个性化薪资谈判话术脚本生成

### 模板系统

- 8 大行业 17 个简历模板，支持行业筛选
- 分栏编辑器：左侧编辑面板 + 右侧实时预览
- 段落自由增删排序、双击重命名、自定义段落类型
- 9 种段落类型：个人简介、技能清单、经历条目、证书资质、获奖荣誉、语言能力、兴趣爱好、自定义文本段、自定义经历
- 导出为 PDF 或 PNG 图片
- **CSS 特异性**：模板 CSS 通过 `import(... ?raw)` 加载并注入为非 scoped `<style>`，基线默认样式写在 `TemplateRenderer.vue` 的 scoped styles 中，模板 CSS 仅做 `.resume-tpl-{id}` 下的覆盖

### 通知系统

- SSE 实时推送（基于 `fetch` + Authorization 头，非 EventSource）
- 指数退避重连（5s ~ 60s，最多 20 次）
- 支持多种通知类型：简历诊断、AI 润色、面试、配额、系统公告、活动、更新、维护

### 主题系统

- 三种模式：浅色 / 深色 / 跟随系统
- `index.html` 内联脚本防止主题闪烁
- CSS 自定义属性（40+ 设计令牌）+ Element Plus 深色变量桥接
- `prefers-reduced-motion` 支持
- 响应式断点：1280px / 1024px / 768px / 480px

### 布局切换

`App.vue` 根据 `route.meta.useLayout` 动态渲染 `MainLayout`（含 AppHeader + AppFooter）或裸 `<div>`。面试会话、模板编辑器等全屏页面跳过主布局。

### 简历数据模型

`stores/templateEditor.js` 中 `resumeData` 的结构：

```
{
  basic: { name, phone, email, ... },
  summary: string,
  skills: string[],
  education: [...],
  work: [...],
  projects: [...],
  certifications: [],
  awards: [],
  languages: [],
  interests: []
}
```

编辑器新增的自定义段落使用动态 key（如 `text-{timestamp}`、`experience-{timestamp}`）存储在内置字段旁边。支持 8 种段落类型：text, summary, skills, experience, certifications, awards, languages, interests。

## 构建优化

Vite 配置了手动代码分割：

| Chunk | 包含 |
|-------|------|
| `vue-vendor` | vue / vue-router / pinia |
| `element-plus-vendor` | element-plus / @element-plus/icons-vue |
| `axios-vendor` | axios |
| `chart-vendor` | chart.js / vue-chartjs |

Chunk size warning limit: 1000kB。

## 认证机制

### 用户端

- Token 存储于 `localStorage`（key: `ai_resume_token`）
- Axios 请求拦截器自动注入 `Authorization: Bearer <token>` 头
- 401 响应自动清除 Token 并跳转 `/login`
- 个别请求可在 config 中设置 `skipDefaultErrorHandler = true` 跳过全局错误弹窗

### 管理端

- 独立 Token（key: `ai_resume_admin_token`），管理员角色需 role=9
- 自定义 `transformResponse` 保留 JavaScript 长整数精度（Java Long 16+ 位 ID）
- 401 处理使用互斥锁（`isHandlingUnauthorized`）防止并发重定向循环
- `adminFeedback.js` 集中管理所有管理端 UI 反馈文案

## 测试

测试文件位于 `src/__tests__/`，使用 Vitest + happy-dom/jsdom：

```bash
npm run test           # 运行所有测试
npm run test:watch     # 监听模式
```

### 测试覆盖

| 模块 | 测试文件 |
|------|----------|
| 管理端 API | `api/admin.auditLogs.test.js`, `api/admin.growthConfig.test.js`, `api/admin.membership.test.js`, `api/admin.notifications.test.js`, `api/admin.userData.test.js`, `api/admin.users.test.js`, `api/admin.versionLogs.test.js` |
| 用户端 API | `api/versionLog.test.js` |
| 工具函数 | `utils/notificationMeta.test.js` |
| 页面视图 | `views/HomePageView.test.js`, `views/VersionLogView.test.js` |
