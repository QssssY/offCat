# AI Resume Frontend

智能简历诊断与模拟面试系统 — 基于 Vue 3 的前端单页应用，提供 AI 简历诊断、模拟面试（含语音交互）、JD 匹配、AI 简历润色、简历模板编辑、社区交流、用户设置、Offer 助手、成长中心、自定义 AI 引擎配置及完整的管理后台等功能。

## 技术栈

| 类别 | 技术 | 说明 |
|------|------|------|
| 框架 | Vue 3.4 | Composition API + `<script setup>` |
| 构建工具 | Vite 5 | 开发服务器 + 生产构建 |
| 路由 | Vue Router 4 | 用户端 + 管理端双路由守卫，懒加载 |
| 状态管理 | Pinia 2 | user / adminUser / theme / templateEditor |
| UI 组件库 | Element Plus 2.4 | 全组件引入 + 自定义主题覆盖 |
| 辅助 UI | Naive UI 2.44 | NMessageProvider 等选择性使用 |
| HTTP 客户端 | Axios 1.6 | 双实例（用户端 + 管理端），请求/响应拦截器 |
| 富文本编辑 | TipTap 3 | 基于 ProseMirror，用于简历模板编辑器 |
| 图表 | Chart.js 4 + vue-chartjs 5 | 管理后台数据可视化 + 面试雷达图 |
| PDF 处理 | pdfjs-dist 5 / html2canvas / jspdf / html2pdf.js | 扫描件检测、导出 PDF/图片 |
| DOCX 导出 | docx 9 | 简历导出为 Word 文档 |
| 虚拟滚动 | vue-virtual-scroller 2 | 社区长列表性能优化 |
| 动画 | motion-v 2 | Vue Motion 动画库 |
| HTML 净化 | DOMPurify 3 | 富文本内容安全过滤 |
| Excel 导出 | xlsx 0.18 | 管理端数据导出 |
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
│   └── index.js               # 路由配置与守卫（用户端 + 管理端独立认证，懒加载）
├── api/                       # API 模块（32 个用户端 + 15 个管理端）
│   ├── auth.js                # 认证：登录/注册/获取用户信息/密码重置/密保问题
│   ├── resume.js              # 简历：上传/诊断/JD匹配/AI润色
│   ├── resumePdf.js           # 简历 PDF 导出（独立 Axios 实例，blob 响应）
│   ├── interview.js           # 面试：创建会话/流式对话/历史记录/面试报告
│   ├── membership.js          # 会员：套餐/升级
│   ├── notification.js        # 通知：列表/未读数/已读/SSE 实时推送
│   ├── onboarding.js          # 新手引导状态/任务
│   ├── growth.js              # 成长中心：概览/面试雷达
│   ├── offer.js               # Offer 助手：薪资谈判模拟/话术脚本
│   ├── versionLog.js          # 版本日志：最新 N 条
│   ├── publicVersionLog.js    # 版本日志：分页查询
│   ├── stats.js               # 统计：公开平台统计 + 用户月度统计
│   ├── community.js           # 社区：帖子/评论/点赞/收藏/图片上传（22 个接口）
│   ├── userSettings.js        # 用户设置：获取/保存
│   ├── feedback.js            # 反馈：提交反馈
│   ├── template.js            # 模板：使用配额
│   ├── userAiConfig.js        # 用户自定义 AI：配置 CRUD/连通性测试/用量查询
│   └── admin/                 # 管理端 API（15 个模块）
│       ├── auth.js            # 管理员登录
│       ├── dashboard.js       # 数据概览/趋势/热门职位/业务分布
│       ├── monitor.js         # 系统监控
│       ├── jobRoles.js        # 职位角色 CRUD + 批量操作
│       ├── prompts.js         # 提示词模板 CRUD + 批量操作
│       ├── aiEngines.js       # AI 引擎配置 CRUD + 批量操作 + 连通性测试
│       ├── users.js           # 用户权益/配额管理 + 批量操作
│       ├── membership.js      # 会员套餐 CRUD + 订单查询
│       ├── auditLogs.js       # 审计日志查询
│       ├── notifications.js   # 系统通知 CRUD + 批量发布/删除
│       ├── versionLogs.js     # 版本日志 CRUD + 批量发布/删除
│       ├── userData.js        # 用户面试/简历任务历史
│       ├── growthConfig.js    # 成长中心配置 CRUD + 批量删除
│       ├── community.js       # 社区审核：帖子/评论审核队列
│       └── feedback.js        # 反馈管理：列表/详情/状态更新/批量删除
├── stores/                    # Pinia 状态管理
│   ├── user.js                # 用户状态：登录/登出/VIP判断
│   ├── adminUser.js           # 管理员状态：角色校验(role=9)，独立于用户 store
│   ├── theme.js               # 主题：light/dark/system 跟随系统
│   └── templateEditor.js      # 模板编辑器：简历数据/段落配置 CRUD
├── composables/               # 组合式函数
│   ├── useVoiceCall.js        # 语音通话控制（TTS 播放/静音/自动提交）
│   ├── useSpeechToText.js     # 浏览器语音识别
│   ├── useTextToSpeech.js     # 浏览器 TTS
│   ├── useCloudTextToSpeech.js # 云端 TTS（用户自定义 AI 引擎）
│   └── useScrollToComment.js  # 滚动定位到指定评论
├── utils/                     # 工具模块
│   ├── auth.js                # 用户 Token 管理 (localStorage)
│   ├── request.js             # 用户端 Axios 实例（30s 超时，401 自动跳转）
│   ├── adminAuth.js           # 管理员 Token/Session 管理 (localStorage)
│   ├── adminRequest.js        # 管理端 Axios 实例（长整数精度保留，互斥锁防并发重定向）
│   ├── adminFeedback.js       # 管理端统一反馈工具（成功/错误/警告/确认弹窗）
│   ├── community.js           # 社区工具：相对时间格式化、分类标签
│   ├── date.js                # 日期格式化
│   ├── resumePhoto.js         # 简历照片处理
│   ├── resumeDocxExport.js    # DOCX 简历导出
│   └── notificationMeta.js    # 通知类型元数据（标签/图标/时间格式化）
├── constants/
│   └── interview.js           # 面试常量：难度标签/面试模式/反馈模式
├── layouts/
│   ├── MainLayout.vue         # 用户端布局：顶部导航 + 内容区 + 页脚 + 新手引导
│   └── AdminLayout.vue        # 管理端布局：顶栏 + 侧边导航（5 组） + 内容区
├── views/                     # 页面视图（36 个页面）
│   ├── HomePageView.vue       # 首页（落地页）
│   ├── DashboardView.vue      # 用户仪表盘
│   ├── MembershipView.vue     # 会员中心
│   ├── VersionLogView.vue     # 公共版本更新日志
│   ├── settings/
│   │   └── SettingsView.vue   # 设置中心（11 个分区：账号/面试偏好/自定义AI/安全/隐私/数据管理/反馈/外观/通知/引导/会员）
│   ├── auth/
│   │   └── LoginView.vue      # 登录/注册
│   ├── resume/
│   │   ├── UploadView.vue     # 简历上传（PDF 扫描件检测）
│   │   ├── ResultView.vue     # 诊断结果（雷达图/评分/AI润色/导出）
│   │   └── HistoryView.vue    # 诊断历史
│   ├── interview/
│   │   ├── InterviewEntryView.vue     # 面试配置（职位/难度/模式/JD/语音）
│   │   ├── InterviewSessionView.vue   # 实时面试（SSE 流式，全屏布局，语音交互）
│   │   ├── InterviewHistoryView.vue   # 面试历史
│   │   └── InterviewReportView.vue    # 面试报告与维度评分
│   ├── template/
│   │   ├── TemplateLibraryView.vue    # 模板库（8 大行业 17 个模板）
│   │   └── TemplateEditorView.vue     # 模板编辑器（分栏编辑+实时预览，全屏布局）
│   ├── growth/
│   │   └── GrowthCenterView.vue       # 成长中心（面试维度雷达/薄弱项分析/任务引导）
│   ├── notification/
│   │   └── NotificationView.vue       # 通知中心
│   ├── offer/
│   │   └── OfferAssistView.vue        # Offer 助手（薪资谈判模拟）
│   ├── community/                     # 社区
│   │   ├── CommunityView.vue          # 社区首页（分类/排序/无限滚动/发帖）
│   │   ├── PostDetailView.vue         # 帖子详情（评论/回复/分享面试报告）
│   │   └── MyActivity.vue             # 我的活动（8 个 Tab：发帖/点赞/收藏/评论/收到的互动）
│   └── admin/                         # 管理后台页面（16 个页面）
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
│       ├── AdminGrowthConfigView.vue    # 成长中心配置管理
│       ├── AdminCommunityReviewView.vue # 社区内容审核
│       └── AdminFeedbackView.vue        # 用户反馈管理
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
│   │   ├── ExportToolbar.vue          # 导出工具栏（PDF/PNG/DOCX）
│   │   └── editor/                    # 编辑器子组件（12 个）
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
│   ├── community/             # 社区相关组件
│   │   ├── PostCard.vue               # 帖子卡片（点赞/收藏/分享/管理操作）
│   │   ├── PostEditor.vue             # 帖子编辑器（标题/内容/图片上传/面试报告分享）
│   │   ├── CommentSection.vue         # 评论组件（嵌套回复/分页/图片）
│   │   ├── ImageGrid.vue              # 图片网格展示
│   │   └── ShareReportDialog.vue      # 分享面试报告弹窗
│   └── empty/                 # 空状态组件
│       ├── ResumeEmpty.vue
│       └── InterviewEmpty.vue
├── data/                      # 静态数据
│   ├── industries.js          # 9 个行业分类（含"全部"筛选项）
│   ├── templates.js           # 17 个模板定义（8 大行业）
│   ├── styles/                # 17 个模板 CSS 文件（运行时 import ?raw 加载）
│   └── contents/              # 各模板默认简历数据（17 个文件 + _default.js）
└── assets/                    # 静态资源
    ├── logo.png
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
| `/settings` | 设置中心 | MainLayout | 是 |
| `/resume/upload` | 简历上传 | MainLayout | 是 |
| `/resume/result/:taskId` | 诊断结果 | MainLayout | 是 |
| `/resume/history` | 诊断历史 | MainLayout | 是 |
| `/interview/entry` | 面试配置 | MainLayout | 是 |
| `/interview/session/:sessionId` | 实时面试 | 无 (全屏) | 是 |
| `/interview/history` | 面试历史 | MainLayout | 是 |
| `/interview/report/:sessionId` | 面试报告 | MainLayout | 是 |
| `/templates` | 模板库 | MainLayout | 是 |
| `/templates/editor/:templateId` | 模板编辑器 | 无 (全屏) | 是 |
| `/community` | 社区首页 | MainLayout | 是 |
| `/community/post/:postId` | 帖子详情 | MainLayout | 是 |
| `/community/my` | 我的活动 | MainLayout | 是 |

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
| `/admin/community` | 社区内容审核 | 是 |
| `/admin/feedback` | 用户反馈管理 | 是 |

## 核心功能

### 简历诊断

- PDF 上传，客户端扫描件检测（pdfjs-dist 提取文本）
- 异步诊断：上传 → 后端消息队列 → AI 分析 → 结果入库
- 五维评分雷达图（基本信息/技能/工作经历/项目经历/教育背景）
- JD 匹配分析、AI 简历润色（支持复制/导出 PDF/PNG/DOCX）
- 诊断阶段进度展示、失败任务重试

### 模拟面试

- 可选职位角色、难度（初级/中级/高级）、模式（普通/压力/岗位定向/大厂HR/技术负责人/外企面试官）
- 可选 JD 文本和简历关联
- SSE 流式 AI 回答，实时对话交互
- 面试报告与六维度评分（技术深度/项目表达/沟通/问题解决/抗压/岗位匹配）
- 反馈模式：面试结束后反馈 / 即时反馈
- 语音交互：TTS 播放 + 语音识别（浏览器原生或云端）
- 面试报告可分享至社区

### Offer 助手

- 薪资谈判模拟演练
- 个性化薪资谈判话术脚本生成

### 模板系统

- 8 大行业 17 个简历模板，支持行业筛选
- 分栏编辑器：左侧编辑面板 + 右侧实时预览
- 段落自由增删排序、双击重命名、自定义段落类型
- 9 种段落类型：个人简介、技能清单、经历条目、证书资质、获奖荣誉、语言能力、兴趣爱好、自定义文本段、自定义经历
- 导出为 PDF、PNG 图片或 DOCX 文档
- **CSS 特异性**：模板 CSS 通过 `import(... ?raw)` 加载并注入为非 scoped `<style>`，基线默认样式写在 `TemplateRenderer.vue` 的 scoped styles 中，模板 CSS 仅做 `.resume-tpl-{id}` 下的覆盖

### 社区

- 两个板块：面试经验 / 内推信息
- 帖子支持图文混排、面试报告链接分享
- 嵌套评论回复、图片上传
- 点赞/收藏互动、未读互动提醒
- 我的动态中心（8 个 Tab：发帖/点赞/收藏/评论/收到的点赞/评论/回复/收藏）
- 虚拟滚动长列表优化（vue-virtual-scroller）

### 设置中心

11 个设置分区：
1. **账号资料** — 头像、基本信息展示
2. **面试偏好** — 默认职位/难度/模式/反馈模式/语音配置（TTS 引擎选择/语速/音调/音量/静音恢复/识别语言）
3. **自定义 AI** — 用户自己的 OpenAI 兼容 API（3 个槽位：default/resume/interview）、连通性测试、用量查询、TTS 配置
4. **账号安全** — 修改密码、修改密保问题、注销账号（15s 冷却+确认输入）
5. **隐私与数据** — 数据概览、清除本地缓存
6. **数据管理** — 清除面试/简历历史、设置保留天数
7. **反馈** — 提交 bug/建议/体验/其他反馈
8. **外观** — 主题切换（system/light/dark）
9. **通知偏好** — 实时推送开关、默认筛选
10. **新手引导** — 重播引导
11. **会员与配额** — 当前套餐、VIP 到期、额度展示

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

测试文件位于 `src/__tests__/`（78 个测试文件），使用 Vitest + happy-dom/jsdom：

```bash
npm run test           # 运行所有测试
npm run test:watch     # 监听模式
```

### 测试覆盖

| 模块 | 测试文件 |
|------|----------|
| 管理端 API | `api/admin.auditLogs.test.js`, `api/admin.growthConfig.test.js`, `api/admin.membership.test.js`, `api/admin.notifications.test.js`, `api/admin.userData.test.js`, `api/admin.users.test.js`, `api/admin.versionLogs.test.js`, `api/admin.aiEngines.test.js` |
| 用户端 API | `api/versionLog.test.js`, `api/resume.test.js`, `api/interview.test.js`, `api/resumePdf.test.js`, `api/feedback.test.js`, `api/userAiConfig.test.js`, `api/performanceCache.test.js` |
| 工具函数 | `utils/notificationMeta.test.js`, `utils/resumePhoto.test.js`, `utils/resumeDocxExport.test.js`, `utils/adminFeedback.test.js`, `utils/adminRequest.test.js`, `utils/request.test.js`, `utils/settingsPreferences.test.js`, `utils/export.test.js`, `utils/apiCache.test.js`, `utils/optimizedImages.test.js`, `utils/featureIcons.test.js`, `utils/speechRecognitionCapability.test.js`, `utils/errorMessages.test.js` |
| 页面视图 | `views/HomePageView.test.js`, `views/VersionLogView.test.js`, `views/DashboardView.test.js`, `views/MembershipView.test.js`, `views/SettingsView.test.js`, `views/ResumeUploadView.test.js`, `views/ResumeResultView.test.js`, `views/ResumeHistoryView.test.js`, `views/OfferAssistView.test.js`, `views/InterviewEntryView.test.js`, `views/InterviewSessionView.test.js`, `views/InterviewReportView.test.js`, `views/GrowthCenterView.test.js`, `views/NotificationView.test.js`, `views/community/CommunityView.test.js`, `views/community/PostDetailView.test.js`, `views/community/MyActivity.test.js`, `views/admin/AdminLoginView.test.js`, `views/admin/AdminUserRightsView.test.js`, `views/admin/AdminMonitorView.test.js`, `views/admin/AdminVersionLogView.test.js`, `views/admin/AdminAiEngineView.test.js`, `views/admin/AdminNotificationView.test.js`, `views/admin/AdminFeedbackView.test.js` |
| 组件 | `components/AppHeader.test.js`, `components/OnboardingTaskCard.test.js`, `components/NotificationTypeIcon.test.js`, `components/common/FeatureIcon.test.js`, `components/community/PostCard.test.js`, `components/community/PostEditor.test.js`, `components/community/CommentSection.test.js`, `components/community/ImageGrid.test.js`, `components/community/ShareReportDialog.test.js`, `components/resume/ResumeTemplate.test.js`, `components/template/TemplateCard.test.js`, `components/template/TemplatePreviewImage.test.js`, `components/template/ExportToolbar.test.js` |
| 组合式函数 | `composables/useVoiceCall.test.js`, `composables/useSpeechToText.test.js`, `composables/useTextToSpeech.test.js`, `composables/useCloudTextToSpeech.test.js` |
| 布局 | `layouts/MainLayout.test.js`, `layouts/AdminLayout.test.js` |
| 路由 | `router/routeLoaders.test.js` |
| 其他 | `App.test.js`, `themeTokens.test.js`, `elementPlusServiceStyles.test.js`, `viteConfig.test.js`, `styles/UserIconHalo.test.js` |
