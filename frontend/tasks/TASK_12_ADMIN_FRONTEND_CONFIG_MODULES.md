# TASK_12_ADMIN_FRONTEND_CONFIG_MODULES

## 1. 当前任务所属模块
- 管理端前端模块
- 子模块：配置类模块（岗位配置、Prompt 管理、AI 引擎配置）

## 2. 本轮 task 拆分
1. 接入岗位配置 API，完成岗位配置列表/新增/编辑/启停页面。  
2. 接入 Prompt API，并联动岗位配置数据作为岗位来源。  
3. 接入 AI 引擎 API，完成列表/新增/编辑/启停/脱敏展示。  
4. 更新 `/admin` 路由树与侧边导航，接入三个新页面。  
5. 执行构建验证并更新任务文档与阶段文档。

## 3. task 清单
- [x] 新增岗位配置 API 封装：`src/api/admin/jobRoles.js`
- [x] 新增 Prompt API 封装：`src/api/admin/prompts.js`
- [x] 新增 AI 引擎 API 封装：`src/api/admin/aiEngines.js`
- [x] 新增岗位配置页：`src/views/admin/AdminJobRoleView.vue`
- [x] 新增 Prompt 管理页：`src/views/admin/AdminPromptView.vue`
- [x] 新增 AI 引擎配置页：`src/views/admin/AdminAiEngineView.vue`
- [x] 更新管理端路由：`src/router/index.js`
- [x] 更新管理端布局导航：`src/layouts/AdminLayout.vue`
- [x] 更新任务文档：`frontend/tasks/TASK_12_ADMIN_FRONTEND_CONFIG_MODULES.md`
- [x] 更新阶段文档：`frontend/runtime/STATE.md`
- [x] 构建验证通过

## 4. 本轮代码改动（frontend/app）
- 新增：`src/api/admin/jobRoles.js`
- 新增：`src/api/admin/prompts.js`
- 新增：`src/api/admin/aiEngines.js`
- 新增：`src/views/admin/AdminJobRoleView.vue`
- 新增：`src/views/admin/AdminPromptView.vue`
- 新增：`src/views/admin/AdminAiEngineView.vue`
- 修改：`src/router/index.js`
- 修改：`src/layouts/AdminLayout.vue`

## 5. 路由与页面结果
- 新增路由：`/admin/job-roles`
- 新增路由：`/admin/prompts`
- 新增路由：`/admin/ai-engines`
- 管理端侧栏已补齐上述模块导航，高亮按路径前缀判断。

## 6. 接口联调结果
### 岗位配置模块
- `GET /api/admin/job-roles`
- `POST /api/admin/job-roles`
- `PUT /api/admin/job-roles`
- `PUT /api/admin/job-roles/{id}/active`

### Prompt 管理模块
- `GET /api/admin/prompts`
- `POST /api/admin/prompts`
- `PUT /api/admin/prompts`
- `PUT /api/admin/prompts/{id}/active`
- 联动读取岗位：`GET /api/admin/job-roles`

### AI 引擎配置模块
- `GET /api/admin/ai-engines`
- `POST /api/admin/ai-engines`
- `PUT /api/admin/ai-engines`
- `PUT /api/admin/ai-engines/{id}/active`

### 统一约束
- 仍通过 `adminRequest` 统一判定成功标准：`body.code === 200`。

## 7. 构建验证
- 验证命令：`npm.cmd run build`
- 验证结果：通过

## 8. 下一轮建议
进入 `TASK_13_ADMIN_FRONTEND_USER_RIGHTS_MODULES`：
1. 用户列表  
2. 用户权益详情  
3. 用户权益编辑（role / membershipPlanCode / vipExpireTime）
