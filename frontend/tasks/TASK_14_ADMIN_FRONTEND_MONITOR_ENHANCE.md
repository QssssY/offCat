# TASK_14_ADMIN_FRONTEND_MONITOR_ENHANCE

## 1. 当前任务所属模块
- 管理端前端模块
- 子模块：业务监控与看板增强

## 2. 本轮 task 拆分
1. 新增监控总览 API 封装并对接监控页面。
2. 新建管理端监控页，展示应用层监控核心指标。
3. 接入 `/admin/monitor` 路由与管理端侧栏导航。
4. 补齐任务文档与阶段状态文档并执行构建验证。

## 3. task 清单
- [x] 新增监控页面：`src/views/admin/AdminMonitorView.vue`
- [x] 接入管理端路由：`src/router/index.js`
- [x] 更新管理端侧栏导航：`src/layouts/AdminLayout.vue`
- [x] 更新任务文档：`frontend/tasks/TASK_14_ADMIN_FRONTEND_MONITOR_ENHANCE.md`
- [x] 更新阶段文档：`frontend/runtime/STATE.md`
- [x] 构建验证：`npm.cmd run build`

## 4. 本轮实现说明
- 监控页接入 `GET /api/admin/monitor/overview`，并使用与后端返回字段一致的数据结构：
  - `pendingResumeTaskCount`
  - `processingResumeTaskCount`
  - `failedResumeTaskCount`
  - `activeInterviewSessionCount`
  - `todayInterviewSessionCount`
  - `todayResumeDiagnosisCount`
- 页面支持手动刷新、错误提示、移动端响应式布局。
- 导航新增“监控总览”，放在“数据看板”后，便于先看统计再看运行态。

## 5. 接口联调
- `GET /api/admin/monitor/overview`
- 成功标准统一通过 `adminRequest` 处理：`body.code === 200`

## 6. 下一轮建议
- 进入 `TASK_15_ADMIN_FRONTEND_DASHBOARD_FILTER_EXPERIENCE`：
  - 增强数据看板筛选体验（快捷日期、前端参数校验、错误前置提示）
  - 降低前端联调阶段因参数问题造成的无效请求

---

## 7. 监控总览业务链路补齐（2026-05-31）

### 当前任务所属模块
- 管理端前端模块
- 子模块：监控总览业务指标补齐

### 本轮修改文件清单
- `frontend/app/src/views/admin/AdminMonitorView.vue`
- `frontend/app/src/__tests__/views/AdminMonitorView.test.js`
- `frontend/tasks/TASK_14_ADMIN_FRONTEND_MONITOR_ENHANCE.md`
- `frontend/tasks/stage.md`

### 前端实现方案
- 继续复用 `GET /api/admin/monitor/overview`，不新增页面、不新增筛选参数。
- 页面按“简历任务运行态 / 今日业务量 / 待处理事项”三组展示：
  - 简历任务运行态：待处理、处理中、失败、已完成。
  - 今日业务量：面试、诊断、润色、JD 匹配、社区发帖、反馈、订单。
  - 待处理事项：活跃面试、反馈待处理/处理中、社区待审总数及帖子/评论拆分。
- 保留手动刷新、错误提示和空状态；所有新增字段缺失时按 `0` 回显，兼容旧后端短暂缓存。
- 卡片数字启用等宽数字样式，移动端继续单列展示。

### 后端文件定位
- 后端字段和统计实现见 `tasks/TASK_12_ADMIN_MONITORING_AND_DASHBOARD.md`。

### 数据存储方案
- 前端不涉及数据存储变更。

### stage 更新说明
- 已在 `frontend/tasks/stage.md` 增加“管理端监控总览业务链路补齐”记录。

### 构建结果
- `npm.cmd test -- --run src/__tests__/views/AdminMonitorView.test.js` 通过。
- `npm.cmd run build` 通过。

### 当前功能验收说明
- 已新增 `AdminMonitorView.test.js`，覆盖完整字段分组展示、全 0 空状态和接口失败提示。
- 本轮只处理监控总览页面展示，不继续新增监控详情页、导出或基础设施监控图表。

### 停止，不继续下一个功能
- 当前仅完成监控总览业务链路展示补齐，等待验收，不继续推进其它管理端页面能力。

---

## 8. 监控总览四列布局修复（2026-05-31）

### 当前任务所属模块
- 管理端前端模块
- 子模块：监控总览卡片布局修复

### 本轮修改文件清单
- `frontend/app/src/views/admin/AdminMonitorView.vue`
- `frontend/app/src/__tests__/views/AdminMonitorView.test.js`
- `frontend/tasks/TASK_14_ADMIN_FRONTEND_MONITOR_ENHANCE.md`
- `frontend/tasks/stage.md`

### 前端实现方案
- 将监控指标区从 `auto-fit + minmax(220px, 1fr)` 自动铺列改为桌面固定四列，避免宽屏下“今日业务量”一行显示六个卡片。
- 中等屏幕降为两列，移动端继续单列，保持原有分组、字段和接口调用不变。
- 补充前端回归测试，约束今日业务量所在指标区必须使用固定四列桌面网格样式。

### 后端文件定位
- 本轮无后端改动。

### 数据存储方案
- 本轮不涉及数据存储变更。

### stage 更新说明
- 已在 `frontend/tasks/stage.md` 增加“管理端监控总览四列布局修复”记录。

### 构建结果
- `npm.cmd test -- --run src/__tests__/views/AdminMonitorView.test.js` 通过。
- `npm.cmd run build` 通过。

### 当前功能验收说明
- 宽屏下监控卡片每行最多 4 个，“今日业务量”会按 4 + 3 换行展示，不再一行塞 6 个。

### 停止，不继续下一个功能
- 本轮只修复监控总览卡片列数，不继续推进其它监控指标或管理端页面能力。
