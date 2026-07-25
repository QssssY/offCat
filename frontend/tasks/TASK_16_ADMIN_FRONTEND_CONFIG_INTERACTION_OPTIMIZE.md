# TASK_16_ADMIN_FRONTEND_CONFIG_INTERACTION_OPTIMIZE

## 1. 当前任务所属模块
- 管理端前端模块
- 子模块：配置类模块交互体验优化

## 2. 本轮 task 拆分
1. 为岗位配置页增加本地筛选能力（关键词 + 状态）。
2. 为 Prompt 管理页增加本地筛选能力（关键词 + 场景 + 状态）。
3. 为 AI 引擎配置页增加本地筛选能力（关键词 + 业务类型 + 状态）。
4. 补充空数据提示与注释说明。
5. 更新任务文档和阶段文档并执行构建验证。

## 3. task 清单
- [x] 优化岗位配置页：`src/views/admin/AdminJobRoleView.vue`
- [x] 优化 Prompt 管理页：`src/views/admin/AdminPromptView.vue`
- [x] 优化 AI 引擎页：`src/views/admin/AdminAiEngineView.vue`
- [x] 增加表格空数据提示与本地筛选逻辑注释
- [x] 更新任务文档：`frontend/tasks/TASK_16_ADMIN_FRONTEND_CONFIG_INTERACTION_OPTIMIZE.md`
- [x] 更新阶段文档：`frontend/runtime/STATE.md`
- [x] 构建验证：`npm.cmd run build`

## 4. 本轮实现说明
- 本轮不改后端接口，仅在前端页面增加本地筛选，确保联调期和配置量增长后仍可快速定位目标配置。
- 三个页面均通过 `computed` 统一计算筛选结果，原始列表数据仍完整保留。
- 表格新增 `empty-text`，空数据场景给出明确反馈。

## 5. 接口联调
- 岗位配置页继续使用：`GET /api/admin/job-roles`
- Prompt 管理页继续使用：`GET /api/admin/prompts`
- AI 引擎配置页继续使用：`GET /api/admin/ai-engines`
- 成功标准保持：`body.code === 200`

## 6. 下一轮建议
- 进入 `TASK_17_ADMIN_FRONTEND_PERMISSION_AND_AUDIT_UX`：
  - 管理端关键操作（启停、封禁、权益修改）增加操作记录入口预留
  - 完善权限受限状态页与错误态引导
