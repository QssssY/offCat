# TASK_15_ADMIN_FRONTEND_DASHBOARD_FILTER_EXPERIENCE

## 1. 当前任务所属模块
- 管理端前端模块
- 子模块：数据看板筛选增强与交互优化

## 2. 本轮 task 拆分
1. 重构 dashboard 筛选区，加入快捷日期范围与自定义范围切换。
2. 增加前端参数校验，提前拦截无效请求。
3. 增加筛选摘要展示，明确当前统计口径。
4. 更新任务文档与阶段文档，并执行构建验证。

## 3. task 清单
- [x] 重写看板页面筛选逻辑：`src/views/admin/AdminDashboardView.vue`
- [x] 新增快捷日期范围：后端默认/今天/近7天/近30天/自定义
- [x] 新增前端参数校验：开始日期、结束日期、90 天范围限制、limit 校验
- [x] 新增筛选摘要区域
- [x] 更新任务文档：`frontend/tasks/TASK_15_ADMIN_FRONTEND_DASHBOARD_FILTER_EXPERIENCE.md`
- [x] 更新阶段文档：`frontend/runtime/STATE.md`
- [x] 构建验证：`npm.cmd run build`

## 4. 本轮实现说明
- 本轮继续复用原有四个看板接口，不新增后端契约。
- 前端主动做参数校验，校验规则与后端保持一致：
  - `startDate` 不能大于 `endDate`
  - 日期范围不能超过 90 天
  - `limit` 必须大于 0
- 通过筛选摘要显示当前区间，降低联调排障成本。

## 5. 接口联调
- `GET /api/admin/dashboard/overview`
- `GET /api/admin/dashboard/trends`
- `GET /api/admin/dashboard/hot-job-roles`
- `GET /api/admin/dashboard/business-distribution`

## 6. 下一轮建议
- 进入 `TASK_16_ADMIN_FRONTEND_CONFIG_INTERACTION_OPTIMIZE`：
  - 优化岗位/Prompt/AI 引擎页面的筛选体验与操作反馈
  - 补齐管理端配置页常用的本地检索能力
