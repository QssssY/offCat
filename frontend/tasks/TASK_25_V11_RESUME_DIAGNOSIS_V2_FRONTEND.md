# TASK_25_V11_RESUME_DIAGNOSIS_V2_FRONTEND

> 2026-05-04 补充记录：本轮新增“简历分析超时与等待过长”修复，前端侧重点为结果页轮询减载、非重叠请求保护、退避式轮询调度，以及等待态文案与状态展示收敛。

## 1. 当前任务所属模块
- V1.1 简历诊断结果升级：问题诊断工作台

## 2. 前端文件定位
- `frontend/app/src/views/resume/ResultView.vue`
- `frontend/app/src/api/resume.js`

## 3. 本轮修改文件清单
- `frontend/app/src/views/resume/ResultView.vue`

## 4. 前端实现方案
- 新增数据适配函数 normalizeDiagnosisResult()：支持 V1/V2 结构转换，兼容旧数据
- 新增 computed 属性：normalizedDiagnosisResult、summary、radarDimensions、deductionDetails、sortedIssues、topProblems、filteredMatchedKeywords、filteredMissingKeywords 等
- 重构页面布局为"问题诊断工作台"：
  1. 诊断总览区：总分、等级、创建时间、AI 总评、最严重的 3 个问题、下一步建议
  2. 五维雷达图：使用 chart.js 的 Radar 组件展示
  3. 维度卡片：5 个维度根据分数显示不同颜色（>=85 绿色，70-84 蓝色/橙色，60-69 橙色，<60 红色）
  4. 维度扣分说明：来自 diagnosisResult.deductionDetails
  5. 核心问题清单：按 priority 排序，包含原文证据、改写示例、复制按钮
  6. 详细模块分析：保留原有技能、亮点、基础信息、工作经验等
  7. 岗位匹配分析：过滤无效关键词（JD、XX、岗位职责等）
  8. AI 简历润色：保留原有功能
- 复制功能：使用 Element Plus ElMessage 提示

## 5. 样式要求
- 保持橙色主题、圆角卡片、浅色背景和 Element Plus 风格
- 问题卡片：浅橙或浅红边框
- 高优先级标签：红色；中优先级：橙色；低优先级：蓝色或灰色
- 扣分标签：橙红色
- 原文证据：浅红背景，左侧竖线
- 推荐改写：浅绿色背景，左侧竖线

## 6. 验证结果
- 前端构建：成功

## 7. 验收说明
- 页面可以展示五维雷达图
- 页面可以展示每个维度的扣分说明
- 页面可以展示问题卡片，包括原文证据、问题解释、影响、修改策略和改写示例
- 用户可以复制局部改写内容
- 岗位匹配分析不再展示 JD、XX 等无效关键词
- 原有功能（岗位匹配分析、AI 简历润色、继续上传、进入模拟面试按钮）未被破坏
