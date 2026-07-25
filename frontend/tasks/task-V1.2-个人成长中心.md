# 任务：V1.2 功能二 — 个人成长中心

## 当前任务所属模块
V1.2 用户体验增强版 — 功能二：个人成长中心

## 后端文件定位
- `server/src/main/java/com/airesume/server/dto/growth/GrowthOverviewResponse.java` — 聚合响应 DTO（含7个内部VO类）
- `server/src/main/java/com/airesume/server/service/GrowthService.java` — 服务接口
- `server/src/main/java/com/airesume/server/service/impl/GrowthServiceImpl.java` — 服务实现（核心聚合逻辑）
- `server/src/main/java/com/airesume/server/controller/GrowthController.java` — REST 控制器
- `server/src/main/java/com/airesume/server/repository/InterviewSessionRepository.java` — 追加查询方法

## 前端文件定位
- `frontend/app/src/api/growth.js` — 成长中心 API 模块
- `frontend/app/src/components/resume/LineChart.vue` — 折线图组件
- `frontend/app/src/views/growth/GrowthCenterView.vue` — 成长中心页面
- `frontend/app/src/router/index.js` — 路由配置（追加 /growth）
- `frontend/app/src/components/AppHeader.vue` — 导航栏（追加成长中心链接）
- `frontend/app/src/views/DashboardView.vue` — 首页（追加入口卡片）

## 本轮修改文件清单

### 新建文件
1. `server/.../dto/growth/GrowthOverviewResponse.java` — 成长中心聚合响应DTO
2. `server/.../service/GrowthService.java` — 服务接口
3. `server/.../service/impl/GrowthServiceImpl.java` — 服务实现
4. `server/.../controller/GrowthController.java` — REST控制器
5. `frontend/app/src/api/growth.js` — 前端API模块
6. `frontend/app/src/components/resume/LineChart.vue` — 折线图组件
7. `frontend/app/src/views/growth/GrowthCenterView.vue` — 成长中心页面

### 修改文件
8. `server/.../repository/InterviewSessionRepository.java` — 追加 `findTop10ByUserIdAndStatusAndComprehensiveScoreIsNotNullOrderByCreateTimeDesc` 方法
9. `frontend/app/src/router/index.js` — 追加 /growth 路由
10. `frontend/app/src/components/AppHeader.vue` — 桌面端和移动端导航追加成长中心链接
11. `frontend/app/src/views/DashboardView.vue` — stats-section 和 records-section 之间追加入口卡片及样式

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

## 后端实现方案

### 1. 数据聚合架构
不新建核心业务表，实时聚合5张已有表数据：
- `resume_diagnosis_task` — 简历诊断分数趋势、诊断次数
- `interview_session` — 面试评分趋势、面试次数
- `resume_job_match_record` — 最近JD匹配结果、匹配次数
- `resume_polish_record` — 最近AI润色记录、润色次数
- `mock_interview_job_target_record` — 岗位定向面试反馈

### 2. Controller 层
- `GET /api/user/growth/overview` — 获取成长中心概览
- 用户ID从 `Authentication.getPrincipal()` 获取
- 返回 `Result<GrowthOverviewResponse>`

### 3. Service 层核心逻辑
- 查询已完成简历诊断（status=2），从 diagnosis_result JSON 解析 totalScore
- 查询已结束面试（status=1, score非null），直接取 comprehensiveScore
- 查询最新JD匹配记录，解析 analysis_result JSON
- 查询最新润色记录，解析 modification_notes JSON
- 查询最新面试反馈，解析 evaluation_report JSON + 关联岗位定向反馈
- 短板建议：纯规则引擎，LinkedHashSet 去重，各维度最多5条

### 4. Repository 层
- 新增 JPA 派生查询方法，无需手写 SQL

## 数据来源与聚合方案

| 数据项 | 来源表 | 查询条件 | 排序 | 限制 | JSON解析 |
|--------|--------|----------|------|------|----------|
| 简历分数趋势 | resume_diagnosis_task | user_id, status=2 | create_time DESC | 10条 | diagnosis_result → totalScore |
| 面试评分趋势 | interview_session | user_id, status=1, score非null | create_time DESC | 10条 | 无（直接取comprehensiveScore） |
| 最近JD匹配 | resume_job_match_record | user_id | create_time DESC | 1条 | analysis_result → keywords, suggestions |
| 最近润色 | resume_polish_record | user_id | create_time DESC | 1条 | modification_notes → List |
| 最近面试反馈 | interview_session | user_id, status=1 | create_time DESC | 1条 | evaluation_report → summary |
| 岗位定向反馈 | mock_interview_job_target_record | user_id, session_id | — | 1条 | job_targeted_feedback → summary |

## 短板与建议生成规则

| 条件 | 短板输出 | 建议输出 |
|------|---------|---------|
| 简历分 < 60 | "简历整体质量较低" | "优化简历结构、项目描述和量化成果" |
| 简历分 60-69 | "简历质量有提升空间" | — |
| 面试分 < 50 | "面试表现较弱" | "加强项目表达、技术原理理解和问题复盘" |
| 面试分 50-69 | "面试技巧需打磨" | — |
| JD匹配分 < 50 | "简历与岗位匹配度低" | missingKeywords前3条转建议 |
| 简历趋势下降>5分 | "简历分数呈下降趋势" | — |
| 面试趋势下降>5分 | "面试表现呈下降趋势" | — |
| 有面试报告 | — | "回顾最近一次面试报告，针对性改进" |

去重：LinkedHashSet，建议最多5条。

## stage 更新说明
- V1.2 功能一（新手引导）：保持"已完成开发，等待验收"状态
- V1.2 功能二（个人成长中心）：开发完成，等待验收
- V1.2 功能三（消息通知）：未开始
- V1.2 功能四（暗黑模式）：未开始

## task 更新说明
- 根目录：新建 `task-V1.2-个人成长中心.md`（后端为主）
- 前端目录：新建 `frontend/task-V1.2-个人成长中心.md`（前端为主）

## 编译结果
- 后端编译通过（`mvn compile -q` 无错误输出）

## 构建结果
- 前端构建通过（`npm run build` 成功，8.01s，GrowthCenterView-Bcz8lxGS.js 已正确打包）

## 当前功能验收说明
1. 用户可通过导航栏"成长中心"链接进入页面
2. 用户可通过首页渐变橙色入口卡片进入页面
3. 有数据时展示成长概览（4个统计卡片）
4. 有数据时展示简历诊断分数趋势折线图
5. 有数据时展示模拟面试评分趋势折线图
6. 有数据时展示最近JD匹配结果（分数、关键词）
7. 有数据时展示最近AI润色记录摘要
8. 有数据时展示最近模拟面试反馈（岗位、分数、评价）
9. 有数据时展示当前主要短板与改进建议
10. 无数据用户看到友好空状态和CTA按钮
11. 部分数据缺失时各区块独立空状态，页面不报错
12. 后端成长中心接口 `GET /api/user/growth/overview` 正常返回聚合数据
13. 不影响新手引导功能
14. 不影响简历诊断功能
15. 不影响JD匹配分析功能
16. 不影响AI简历润色功能
17. 不影响普通模拟面试和岗位定向模拟面试
18. 后端编译通过
19. 前端构建通过
20. 新增或修改代码包含中文注释且无乱码

## 停止，不继续下一个功能
- 本轮完成后立即停止，等待验收
