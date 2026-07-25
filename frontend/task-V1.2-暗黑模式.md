# V1.2 功能四：暗黑模式 — 前端 Task

## 当前任务所属模块
V1.2 用户体验增强版，功能四：暗黑模式（前端）

## 前端文件定位
- 项目路径：`frontend/app/src/`

## 本轮修改文件清单

### 新建文件
| 文件 | 说明 |
|------|------|
| `frontend/app/src/stores/theme.js` | Pinia 主题 Store（Composition API 风格） |

### 修改文件
| 文件 | 修改内容 |
|------|----------|
| `frontend/app/index.html` | 添加防闪烁内联脚本 |
| `frontend/app/src/styles/index.css` | 添加 `[data-theme="dark"]` 变量块 + Element Plus 覆盖变量化 + 全局过渡动画 |
| `frontend/app/src/App.vue` | 初始化 themeStore |
| `frontend/app/src/components/AppHeader.vue` | 添加主题切换按钮（桌面端+移动端）+ 颜色变量化 |
| `frontend/app/src/layouts/AdminLayout.vue` | 颜色变量化 |
| `frontend/app/src/views/HomePageView.vue` | 颜色变量化 |
| `frontend/app/src/views/DashboardView.vue` | 颜色变量化 |
| `frontend/app/src/views/auth/LoginView.vue` | 颜色变量化 |
| `frontend/app/src/views/resume/UploadView.vue` | 颜色变量化 |
| `frontend/app/src/views/resume/ResultView.vue` | 颜色变量化 |
| `frontend/app/src/views/interview/InterviewEntryView.vue` | 颜色变量化 |
| `frontend/app/src/views/interview/InterviewSessionView.vue` | 颜色变量化 |
| `frontend/app/src/views/interview/InterviewReportView.vue` | 颜色变量化 |
| `frontend/app/src/views/interview/InterviewHistoryView.vue` | 颜色变量化 |
| `frontend/app/src/views/resume/HistoryView.vue` | 颜色变量化 |
| `frontend/app/src/views/growth/GrowthCenterView.vue` | 颜色变量化 |
| `frontend/app/src/views/notification/NotificationView.vue` | 颜色变量化 |
| `frontend/app/src/views/MembershipView.vue` | 颜色变量化 |
| `frontend/app/src/components/OnboardingGuide.vue` | 颜色变量化 |
| `frontend/app/src/components/resume/LineChart.vue` | 主题感知颜色（JS 层） |
| `frontend/app/src/components/resume/RadarChart.vue` | 主题感知颜色（JS 层） |
| `frontend/app/src/components/resume/ResumeTemplate.vue` | 颜色变量化 |
| `frontend/app/src/components/resume/BasicInfoSection.vue` | 颜色变量化 |
| `frontend/app/src/components/resume/WorkExperienceSection.vue` | 颜色变量化 |
| `frontend/app/src/components/resume/OptimizationSection.vue` | 颜色变量化 |
| `frontend/app/src/components/resume/SkillsSection.vue` | 颜色变量化 |
| `frontend/app/src/components/resume/OverallEvaluation.vue` | 颜色变量化 |
| `frontend/app/src/components/resume/HighlightsSection.vue` | 颜色变量化 |
| `frontend/app/src/components/resume/RadarScorePanel.vue` | 颜色变量化 |
| `frontend/app/src/components/empty/ResumeEmpty.vue` | 颜色变量化 |
| `frontend/app/src/components/empty/InterviewEmpty.vue` | 颜色变量化 |
| `frontend/app/src/views/admin/AdminDashboardView.vue` | 颜色变量化 |
| `frontend/app/src/views/admin/AdminJobRoleView.vue` | 颜色变量化 |
| `frontend/app/src/views/admin/AdminUserRightsView.vue` | 颜色变量化 |
| `frontend/app/src/views/admin/AdminAiEngineView.vue` | 颜色变量化 |
| `frontend/app/src/views/admin/AdminPromptView.vue` | 颜色变量化 |
| `frontend/app/src/views/admin/AdminMonitorView.vue` | 颜色变量化 |
| `frontend/app/src/views/admin/AdminLoginView.vue` | 颜色变量化 |

## 前端实现方案

### 主题状态管理（stores/theme.js）
- Composition API 风格 Pinia Store，与现有 user.js 一致
- `theme` ref: `'light'` / `'dark'` / `'system'`（用户显式选择）
- `resolvedTheme` computed: 始终解析为 `'light'` 或 `'dark'`
- localStorage 持久化（key: `'theme'`，plain string）
- `matchMedia('(prefers-color-scheme: dark)')` 监听系统偏好变化
- `setTheme(value)` / `toggleTheme()` 操作方法
- 初始化时立即应用 `data-theme` 属性到 `document.documentElement`

### 防闪烁脚本（index.html）
- 在 `<head>` 中 CSS 加载前插入内联脚本
- 同步读取 localStorage 并设置 `data-theme` 属性
- 防止暗色模式下页面刷新时出现白色闪烁

### CSS 变量 Token 体系（index.css）
- 在现有 `:root` 块后添加 `[data-theme="dark"]` 覆盖块
- 完整的 Token 分类：背景色、文字色、边框色、阴影、品牌色、语义色、滚动条
- Element Plus 暗色变量桥接：在 `[data-theme="dark"]` 中覆盖 `--el-*` 变量
- 现有 Element Plus 覆盖全部改为 `var()` 引用
- 全局过渡动画：`html, body { transition: background-color 0.3s ease, color 0.3s ease; }`

### 主题切换 UI（AppHeader.vue）
- 桌面端：`.header-right` 区域添加 36x36px 圆形按钮（月亮/太阳 SVG 图标）
- 移动端：汉堡抽屉底部添加主题切换入口
- 按钮风格与通知铃铛一致

### Chart.js 适配（LineChart.vue、RadarChart.vue）
- Canvas 渲染不受 CSS 变量影响，需在 JS 层处理
- 导入 useThemeStore，创建 `isDark` computed
- `chartData` 和 `chartOptions` 改为 computed，依赖 `resolvedTheme`
- 暗色模式：网格线 `rgba(255,255,255,0.06)`、刻度文字 `#7A7A90`、点边框 `#1a1a2e`

### 颜色变量化规则
- `#fff`/`#ffffff` → `var(--bg-card)`
- `#FFF8F3`/`#FFFBF8` → `var(--bg-page)` 或 `var(--orange-light-bg)`
- `#2f2f2f`/`#1a1a1a`/`#333` → `var(--text-title)`
- `#555`/`#555555` → `var(--text-body)`
- `#888`/`#999` → `var(--text-muted)`
- `#bbb`/`#ccc`/`#ddd` → `var(--text-placeholder)`
- `#f0f0f0`/`#e0e0e0` → `var(--border-divider)`
- `#f5f7fa`/`#fafafa` → `var(--bg-elevated)`
- `#FF8C42`（强调色） → `var(--orange-main)`
- `#E67A35`（激活态） → `var(--orange-deep)`
- 不盲目替换所有十六进制值，语义色仅在语义正确时替换

## stage 更新说明
- 后端 `stage.md`：功能三更新为"已完成，已验收通过"，功能四更新为"开发中"
- 前端 `frontend/runtime/STATE.md`：同步更新

## 构建结果
- 构建通过：`cd frontend/app && npm run build` — 8.22s 完成，无错误

## 当前功能验收说明
- 主题切换按钮可见且可用（桌面端 + 移动端）
- 点击切换按钮，亮色→暗色→亮色切换流畅
- 刷新页面后主题保持（localStorage 持久化）
- system 模式下切换系统暗色模式应自动响应
- 暗色模式下刷新页面无白色闪烁
- Element Plus 组件（Dialog、Dropdown、Table、Pagination、Tag、Upload、Message、Select、Input）在暗色下正常渲染
- Chart.js 图表切换主题后颜色正确更新
- 所有页面在暗色模式下文字可读、背景协调、边框可见

## 停止，不继续下一个功能
