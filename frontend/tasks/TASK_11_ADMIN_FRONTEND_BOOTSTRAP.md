# TASK_11_ADMIN_FRONTEND_BOOTSTRAP

## 1. 当前任务所属模块
- 管理端前端模块
- 子模块：管理端入口与看板联调（Bootstrap）

## 2. 本轮 task 拆分
1. 盘点 `frontend/app` 现有用户端路由与请求封装，确认管理端独立改造点。  
2. 建立管理端独立认证能力（token、请求拦截、角色校验）。  
3. 新增 `/admin` 路由体系和管理端独立守卫。  
4. 新增 `/admin/login`、`/admin/dashboard` 页面与基础布局。  
5. 接入看板四个管理端接口并完成最小联调。  
6. 更新前端任务文档与阶段状态文档。  
7. 执行构建验证。

## 3. task 清单
- [x] 新增管理端 token 工具：`src/utils/adminAuth.js`
- [x] 新增管理端请求封装：`src/utils/adminRequest.js`
- [x] 新增管理端用户 store：`src/stores/adminUser.js`
- [x] 新增管理端登录 API：`src/api/admin/auth.js`
- [x] 新增管理端看板 API：`src/api/admin/dashboard.js`
- [x] 新增管理端布局：`src/layouts/AdminLayout.vue`
- [x] 新增管理端登录页：`src/views/admin/AdminLoginView.vue`
- [x] 新增管理端看板页：`src/views/admin/AdminDashboardView.vue`
- [x] 更新路由并增加管理端守卫：`src/router/index.js`
- [x] 更新本任务文档：`frontend/tasks/TASK_11_ADMIN_FRONTEND_BOOTSTRAP.md`
- [x] 更新阶段状态文档：`frontend/runtime/STATE.md`
- [x] 构建验证

## 4. 本轮代码改动（frontend/app）
- 新增：`src/utils/adminAuth.js`
- 新增：`src/utils/adminRequest.js`
- 新增：`src/stores/adminUser.js`
- 新增：`src/api/admin/auth.js`
- 新增：`src/api/admin/dashboard.js`
- 新增：`src/layouts/AdminLayout.vue`
- 新增：`src/views/admin/AdminLoginView.vue`
- 新增：`src/views/admin/AdminDashboardView.vue`
- 修改：`src/router/index.js`

## 5. 路由与页面结果
- 新增路由：`/admin/login`
- 新增路由：`/admin/dashboard`
- 新增管理端父路由：`/admin`（使用 `AdminLayout`）
- 管理端鉴权规则：
  - 未登录或非管理员访问 `/admin/**` → 跳转 `/admin/login`
  - 已登录管理员访问 `/admin/login` → 跳转 `/admin/dashboard`

## 6. 接口联调结果
已接入并在 `/admin/dashboard` 统一加载以下接口：
- `GET /api/admin/dashboard/overview`
- `GET /api/admin/dashboard/trends`
- `GET /api/admin/dashboard/hot-job-roles`
- `GET /api/admin/dashboard/business-distribution`

请求成功标准统一通过 `adminRequest` 拦截器判定：`body.code === 200`。

## 7. 风险与说明
- 管理端与用户端登录态已独立存储，避免入口和鉴权互相污染。
- 管理端当前仅接入看板入口，后续模块（岗位、Prompt、AI 引擎、用户权益）可在同一 `/admin` 框架继续扩展。

## 8. 验证记录
- 验证命令：`npm run build`
- 验证结果：通过
