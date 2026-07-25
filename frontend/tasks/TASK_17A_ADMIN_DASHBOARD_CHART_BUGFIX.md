# TASK_17A_ADMIN_DASHBOARD_CHART_BUGFIX

## 1. 当前任务所属模块
- 管理端前端模块
- 子模块：数据看板趋势图稳定性修复

## 2. 问题根因分析
1. 趋势图原始数据直接参与 `map` 构造，缺少对 `null/字段缺失` 的统一归一化。  
2. 趋势图 `legend` 未显式绑定固定 `series.name`，在异常数据场景下容易出现配置不稳定。  
3. 趋势图 `setOption` 虽使用 `notMerge`，但未显式 `replaceMerge` 指定关键组件，旧配置污染风险仍存在。  
4. 以上问题叠加时，会提高 ECharts 折线图内部拿到异常系列结构的概率，触发 `LineView` 渲染报错。

## 3. 本轮 task 拆分
1. 增加趋势数据归一化层，统一兜底字段。  
2. 固定构造两条折线 series，显式声明 `type: 'line'`。  
3. 显式对齐 `legend.data` 与 `series.name`。  
4. 调整趋势图 `setOption` 为 `notMerge + replaceMerge`。  
5. 更新文档并执行构建验证。

## 4. task 清单
- [x] 修复趋势图 option 构造：`src/views/admin/AdminDashboardView.vue`
- [x] 增加趋势数据归一化与空值兜底
- [x] 显式声明两条固定折线 series
- [x] 增加 `replaceMerge` 避免旧 option 污染
- [x] 更新任务文档：`frontend/tasks/TASK_17A_ADMIN_DASHBOARD_CHART_BUGFIX.md`
- [x] 更新阶段文档：`frontend/runtime/STATE.md`
- [x] 构建验证：`npm.cmd run build`

## 5. 修复说明
- 新增 `normalizedTrendRows` 计算属性，统一处理：
  - 非数组兜底为空数组
  - null 项过滤
  - `date` 缺失兜底为“第N项”
  - 数值字段统一 `Number(... ?? 0)`
- 趋势图 `series` 固定为两条：
  - `面试会话`
  - `简历诊断`
- 每条 `series` 均显式声明 `type: 'line'`。  
- `legend.data` 显式绑定固定系列名称，确保与 `series.name` 一致。  
- 趋势图 `setOption` 增加：
  - `notMerge: true`
  - `replaceMerge: ['series', 'xAxis', 'legend']`

## 6. 验证结论
- 正常数据：趋势图可渲染。  
- 全 0 数据：趋势图可渲染且不报错。  
- 空数组：不报错，展示空状态提示。  
- 字段缺失/null：通过归一化兜底，不报错。
- 额外逻辑验证：使用同构脚本对 4 组场景做构造验证，结果均满足：
  - `series` 无 `undefined` 项
  - 每个 `series.type === 'line'`
  - `legend.data` 与 `series.name` 一致
  - `xAxis.data` 与两条 `series.data` 长度一致

## 7. 影响文件
- `frontend/app/src/views/admin/AdminDashboardView.vue`
- `frontend/tasks/TASK_17A_ADMIN_DASHBOARD_CHART_BUGFIX.md`
- `frontend/runtime/STATE.md`
