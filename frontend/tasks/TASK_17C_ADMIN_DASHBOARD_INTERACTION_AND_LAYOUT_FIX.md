# TASK_17C_ADMIN_DASHBOARD_INTERACTION_AND_LAYOUT_FIX

## 1. 当前任务所属模块
- 管理端前端模块
- 子模块：dashboard 趋势图交互稳定性与布局修复

## 2. 问题根因分析
1. 趋势图在图例点击时报错的核心风险点是“图表配置更新不稳定”：  
- series 与 legend 在多次刷新/切换后可能出现旧配置污染。  
- 当图例切换触发内部重绘时，如果 series 模型有残缺项，就可能触发 `LineView` 报错。  

2. 页面布局阅读顺序不合理：  
- 趋势图作为时间序列主图，横向空间不足。  
- 业务分布占比过大，不利于后台首页快速阅读。

## 3. 本轮 task 拆分
1. 将趋势图改为固定两条完整 series，确保结构稳定。  
2. 调整趋势图 setOption 策略，避免旧 option 污染。  
3. 重排 dashboard 布局：中间热门+分布，底部整行趋势。  
4. 保持热门岗位和业务分布图逻辑不被破坏。  
5. 更新任务文档与阶段文档，构建验证后停止。

## 4. task 清单
- [x] 修改文件：`frontend/app/src/views/admin/AdminDashboardView.vue`
- [x] 趋势图改为固定两条 line series（完整字段）
- [x] 趋势图图例与 series 名称显式对齐
- [x] 趋势图更新改为 clear + 全量 setOption（notMerge + replaceMerge）
- [x] dashboard 布局重排为“中间并排 + 底部整行趋势”
- [x] 更新任务文档：`frontend/tasks/TASK_17C_ADMIN_DASHBOARD_INTERACTION_AND_LAYOUT_FIX.md`
- [x] 更新阶段文档：`frontend/runtime/STATE.md`
- [x] 构建验证：`npm.cmd run build`

## 5. 修复说明
1. 趋势图稳定性  
- 使用 `normalizedTrendRows` 对趋势数据做统一兜底，避免 null/缺字段导致结构异常。  
- 固定两条 series：
  - 面试会话
  - 简历诊断
- 每条 series 始终显式包含：
  - `type`
  - `name`
  - `data`
  - `smooth`
  - `symbol`
  - `lineStyle`
  - `areaStyle`
- `legend.data` 固定绑定上述两个系列名称。  
- 渲染时对趋势图执行 `clear()` 后再 `setOption`，并设置 `notMerge + replaceMerge`，防止旧配置污染。

2. 布局调整  
- 顶部保持：筛选区 + 汇总卡片。  
- 中间第一行：左热门岗位，右业务分布。  
- 底部整行：趋势图，提升时间序列可读性。

## 6. 验证要求映射
- 初始加载：趋势图可渲染。  
- 图例点击：代码层确保 series 始终完整且稳定。  
- 筛选刷新：每次均全量重建趋势图 option。  
- 空数据/全 0：通过归一化与空数组兜底，不生成非法 series。

## 7. 影响文件
- `frontend/app/src/views/admin/AdminDashboardView.vue`
- `frontend/tasks/TASK_17C_ADMIN_DASHBOARD_INTERACTION_AND_LAYOUT_FIX.md`
- `frontend/runtime/STATE.md`
