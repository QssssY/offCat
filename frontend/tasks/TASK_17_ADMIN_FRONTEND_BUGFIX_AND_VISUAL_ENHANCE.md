# TASK_17_ADMIN_FRONTEND_BUGFIX_AND_VISUAL_ENHANCE

## 1. 当前任务所属模块
- 管理端前端模块
- 子模块：问题修复与看板可视化增强

## 2. 问题分析
1. 数据看板页面展示形态偏文本列表，信息密度高但可读性弱，且空数据提示不够直观。  
2. 岗位配置的“展示标签”仅为自由文本输入，用户无法直观看到可选标签和最终效果。  
3. 用户权益查看/编辑链路中，`userId` 经过前端默认 JSON 解析和参数拼接后可能发生超长整型精度风险，导致后端按路径查询返回“用户不存在”。  
4. dashboard 里 `el-radio-button` 使用旧写法（`label` 充当值）会触发 Element Plus 过时告警。

## 3. 本轮 task 拆分
1. 接入 ECharts，对趋势、热门岗位、业务分布进行图表化展示。  
2. 改造岗位“展示标签”交互，提供可选标签 + 可扩展输入 + 预览。  
3. 修复用户权益链路中的 `userId` 保真传递问题。  
4. 清理本轮页面中的 `el-radio` 旧写法。  
5. 完成构建验证并更新文档状态。

## 4. task 清单
- [x] 改造数据看板图表展示：`src/views/admin/AdminDashboardView.vue`
- [x] 引入图表依赖：`package.json`（新增 `echarts`）
- [x] 改造岗位标签交互：`src/views/admin/AdminJobRoleView.vue`
- [x] 修复用户权益 ID 传递链路：`src/views/admin/AdminUserRightsView.vue`
- [x] 修复用户权益 API 路径参数处理：`src/api/admin/users.js`
- [x] 增强 admin 请求解析，保留超长整型：`src/utils/adminRequest.js`
- [x] 清理 dashboard `el-radio-button` 旧写法
- [x] 构建验证：`npm.cmd run build`

## 5. 修复与增强说明
1. 看板图表化  
- 趋势数据：折线图（面试会话/简历诊断）  
- 热门岗位排行：横向柱状图  
- 业务分布：环形饼图  
- 三个图表均增加空数据提示，不改变任何后端接口契约。

2. 岗位展示标签交互  
- `展示标签` 改为多选 + 可创建（`allow-create`）的下拉组件。  
- 表单新增标签预览区域，列表页也改为标签样式展示。  
- 后端原字段仍是字符串，前端采用“逗号拼接”兼容方案，支持后续扩展。

3. 用户权益“用户不存在”修复  
- API 层对 `userId` 统一走字符串归一化与 `encodeURIComponent`。  
- 页面层统一使用 `_userId` 字段作为接口调用参数。  
- `adminRequest` 增加超长整型保真 JSON 解析，避免默认 Number 精度丢失影响路径参数。

4. Element Plus 过时告警修复  
- dashboard 的 `el-radio-button` 从 `label` 值切换为 `value` 值写法，消除过时告警。

## 6. 验证结果
1. 代码层验证  
- 已确认本轮页面中不存在 `el-radio-button label=` 旧写法。  
- 已确认用户权益链路接口调用全部走字符串 `userId`。

2. 构建验证  
- 命令：`npm.cmd run build`  
- 结果：通过。

## 7. 影响文件
- `frontend/app/package.json`
- `frontend/app/package-lock.json`
- `frontend/app/src/views/admin/AdminDashboardView.vue`
- `frontend/app/src/views/admin/AdminJobRoleView.vue`
- `frontend/app/src/views/admin/AdminUserRightsView.vue`
- `frontend/app/src/api/admin/users.js`
- `frontend/app/src/utils/adminRequest.js`

## 8. 备注
- 本轮按要求先做独立修复，不自动进入下一轮功能开发，等待确认。
