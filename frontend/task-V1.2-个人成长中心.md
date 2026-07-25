# 任务：V1.2 功能二 — 个人成长中心（前端）

## 当前任务所属模块
V1.2 用户体验增强版 — 功能二：个人成长中心

## 前端文件定位
- `frontend/app/src/api/growth.js` — 成长中心 API 模块
- `frontend/app/src/components/resume/LineChart.vue` — 折线图组件
- `frontend/app/src/views/growth/GrowthCenterView.vue` — 成长中心页面
- `frontend/app/src/router/index.js` — 路由配置（追加 /growth）
- `frontend/app/src/components/AppHeader.vue` — 导航栏（追加成长中心链接）
- `frontend/app/src/views/DashboardView.vue` — 首页（追加入口卡片）

## 本轮修改文件清单

### 新建文件
1. `frontend/app/src/api/growth.js` — 前端API模块
2. `frontend/app/src/components/resume/LineChart.vue` — 折线图组件
3. `frontend/app/src/views/growth/GrowthCenterView.vue` — 成长中心页面

### 修改文件
4. `frontend/app/src/router/index.js` — 追加 /growth 路由
5. `frontend/app/src/components/AppHeader.vue` — 桌面端和移动端导航追加成长中心链接
6. `frontend/app/src/views/DashboardView.vue` — stats-section 和 records-section 之间追加入口卡片及样式

## 前端实现方案

### 1. API 模块（growth.js）
- `getGrowthOverview()` → `GET /api/user/growth/overview`
- 使用项目现有的 `request` axios 实例，自动携带 JWT

### 2. 折线图组件（LineChart.vue）
- 基于 vue-chartjs 的 Line 组件
- 注册 CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend, Filler
- Props: labels(String[]), datasets(Array)
- Y轴范围 0-100，主色 #FF8C42，填充区域，tension 0.3 平滑曲线
- 沿用 RadarChart.vue 的代码模式

### 3. 成长中心页面（GrowthCenterView.vue）
- 页面结构：标题 → 加载状态 → 全量空状态 → 7个数据区块
- 成长概览：4个统计卡片（最新简历分/面试分/诊断次数/面试次数）
- 趋势图表：双列布局，简历分数趋势 + 面试评分趋势（LineChart）
- 详情卡片：3列布局，JD匹配结果 + AI润色记录 + 面试反馈
- 短板建议：3列网格展示各类短板 + 编号建议列表
- 空状态：全量无数据显示CTA按钮，局部无数据显示引导链接
- 样式沿用 DashboardView 设计语言（#fff8f3背景、白色卡片、#ff8c42主色、16px圆角）
- 响应式：1279px/1023px/767px 三个断点

### 4. 路由和导航
- 路由：`/growth`，meta: { requiresAuth: true, useLayout: true }
- 导航：桌面端在"模拟面试"后、"历史记录"前；移动端在"模拟面试"后
- 首页入口：渐变橙色横条卡片，hover 上浮效果

## 并行开发冲突处理说明
- router/index.js：在 /membership 路由后追加，不影响已有路由
- AppHeader.vue：在已有 nav-link 后追加，不影响已有导航
- DashboardView.vue：在 stats-section 和 records-section 之间插入，不修改已有 CSS class
- 不修改新手引导相关业务逻辑

## 构建结果
- 前端构建通过（`npm run build` 成功，8.01s）

## 当前功能验收说明
1. 用户可通过导航栏"成长中心"链接进入页面
2. 用户可通过首页渐变橙色入口卡片进入页面
3. 有数据时展示成长概览、趋势图表、详情卡片、短板建议
4. 无数据用户看到友好空状态和CTA按钮
5. 部分数据缺失时各区块独立空状态，页面不报错
6. 响应式布局在不同屏幕宽度下正常显示
7. 前端构建通过
8. 新增代码包含中文注释且无乱码

## 停止，不继续下一个功能
- 本轮完成后立即停止，等待验收
