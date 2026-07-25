# TASK_13_ADMIN_FRONTEND_USER_RIGHTS_MODULES

## 1. 当前任务所属模块
- 管理端前端模块
- 子模块：用户与权益管理模块

## 2. 本轮 task 拆分
1. 封装用户列表、用户状态、权益详情、权益编辑 API。  
2. 新建用户与权益管理页面，完成列表展示与搜索。  
3. 打通权益详情抽屉与权益编辑弹窗。  
4. 加入用户状态封禁/解封操作。  
5. 更新路由与侧边导航，纳入管理端主流程。  
6. 构建验证并同步文档状态。

## 3. task 清单
- [x] 新增用户与权益 API 封装：`src/api/admin/users.js`
- [x] 新增用户与权益页面：`src/views/admin/AdminUserRightsView.vue`
- [x] 更新管理端路由：`src/router/index.js`
- [x] 更新管理端侧边导航：`src/layouts/AdminLayout.vue`
- [x] 更新任务文档：`frontend/tasks/TASK_13_ADMIN_FRONTEND_USER_RIGHTS_MODULES.md`
- [x] 更新阶段文档：`frontend/runtime/STATE.md`
- [x] 构建验证通过

## 4. 本轮代码改动（frontend/app）
- 新增：`src/api/admin/users.js`
- 新增：`src/views/admin/AdminUserRightsView.vue`
- 修改：`src/router/index.js`
- 修改：`src/layouts/AdminLayout.vue`

## 5. 路由与页面结果
- 新增路由：`/admin/users`
- 页面能力：
  - 用户列表展示（角色/状态/会员到期）
  - 用户名本地搜索
  - 权益详情抽屉
  - 权益编辑弹窗（role / membershipPlanCode / vipExpireTime / remark）
  - 用户状态切换（封禁/解封）

## 6. 接口联调结果
- `GET /api/admin/users`
- `PUT /api/admin/users/{userId}/status`
- `GET /api/admin/users/{userId}/rights`
- `PUT /api/admin/users/{userId}/rights`
- `GET /api/membership/plans`（编辑权益时加载套餐编码）

说明：所有请求统一通过 `adminRequest`，成功标准仍为 `body.code === 200`。

## 7. 构建验证
- 验证命令：`npm.cmd run build`
- 验证结果：通过

## 8. 下一轮建议
进入 `TASK_14_ADMIN_FRONTEND_MONITOR_ENHANCE`：
1. 管理端监控总览页（对接 `/api/admin/monitor/overview`）  
2. 看板筛选增强（日期范围、热门岗位 limit）  
3. 看板交互优化（统一筛选面板与刷新策略）
