# TASK_22A_ADMIN_RISK_CONFIRM_TEMPLATE_UNIFICATION

## 1. 当前任务所属模块
- 管理端整体收尾优化
- 子模块：风险确认文案模板统一

## 2. 本轮 task 拆分
1. 将 Prompt 页风险确认弹窗统一接入 `confirmAdminRiskAction`。  
2. 将 AI 引擎页风险确认弹窗统一接入 `confirmAdminRiskAction`。  
3. 将用户权益页封禁/解封确认统一接入 `confirmAdminRiskAction`。  
4. 将关键成功/失败/警告提示统一接入 `showAdminSuccess/showAdminError/showAdminWarning`。  
5. 更新任务文档与阶段文档。  
6. 执行构建验证。  

## 3. task 清单
- [x] Prompt 页覆盖确认/重复预警/启停确认统一模板
- [x] AI 引擎页重复预警/启停确认统一模板
- [x] 用户权益页封禁解封确认统一模板
- [x] 三页关键反馈文案统一封装
- [x] 更新任务文档：`frontend/tasks/TASK_22A_ADMIN_RISK_CONFIRM_TEMPLATE_UNIFICATION.md`
- [x] 更新阶段文档：`frontend/runtime/STATE.md`
- [x] 构建验证通过

## 4. 本轮实现代码（frontend/app）
- 修改：`src/views/admin/AdminPromptView.vue`
  - 覆盖确认、重复配置预警、启停确认改为 `confirmAdminRiskAction`
  - 关键成功/失败提示改为统一反馈方法
- 修改：`src/views/admin/AdminAiEngineView.vue`
  - 编码/模型重复预警改为统一风险确认
  - 启停确认改为统一模板并保留影响说明
  - 关键成功/失败/警告提示改为统一反馈方法
- 修改：`src/views/admin/AdminUserRightsView.vue`
  - 封禁/解封确认改为统一风险确认模板
  - 关键成功/失败/警告提示改为统一反馈方法

## 5. 路由与页面变更
- 路由无新增。
- 页面逻辑无重构，仅统一风险确认与反馈交互风格。

## 6. 接口联调说明
- 接口无新增。
- 继续复用：
  - `GET/POST/PUT /api/admin/prompts`、`PUT /api/admin/prompts/{id}/active`
  - `GET/POST/PUT /api/admin/ai-engines`、`PUT /api/admin/ai-engines/{id}/active`
  - `GET /api/admin/users`、`PUT /api/admin/users/{userId}/status`

## 7. frontend/tasks 文档更新
- 新增：`frontend/tasks/TASK_22A_ADMIN_RISK_CONFIRM_TEMPLATE_UNIFICATION.md`

## 8. frontend/runtime/STATE.md 更新
- 已追加 `TASK_22A_ADMIN_RISK_CONFIRM_TEMPLATE_UNIFICATION` 到已完成任务。
- 下一轮继续“空状态表达一致化”。

## 9. 构建验证结果
- 命令：`npm.cmd run build`
- 结果：通过（仍存在既有 chunk size warning）

## 10. 下一轮将继续做什么
- 进入 `TASK_22B`：管理端列表空状态文案与表达一致化收尾。
