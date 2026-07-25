# TASK_20B_ADMIN_AI_ENGINE_TOGGLE_FEEDBACK_AND_RISK_CONFIRM

## 1. 当前任务所属模块
- 管理端前端模块
- 子模块：AI 引擎配置页完善（启停切换交互反馈与风险确认）

## 2. 本轮 task 拆分
1. 增加同业务多启用风险提示。  
2. 增强启停确认文案，展示替换影响范围。  
3. 增加启停操作按钮 loading，避免重复点击。  
4. 增加“已是当前生效项”前置拦截提示。  
5. 更新任务文档与阶段文档。  
6. 执行构建验证。

## 3. task 清单
- [x] 页面新增风险告警条
- [x] 启停确认弹窗新增影响范围提示
- [x] 启停操作增加按钮 loading 防重入
- [x] 已生效项重复启用增加前置提示
- [x] 更新任务文档：`frontend/tasks/TASK_20B_ADMIN_AI_ENGINE_TOGGLE_FEEDBACK_AND_RISK_CONFIRM.md`
- [x] 更新阶段文档：`frontend/runtime/STATE.md`
- [x] 构建验证通过

## 4. 本轮实现代码（frontend/app）
- 修改：`src/views/admin/AdminAiEngineView.vue`
  - 新增 `toggleLoadingId`
  - 新增 `getCurrentActiveEngineByBusiness`
  - 增强 `handleToggleActive` 风险提示和替换反馈
  - 页面新增 `risk-alert` 告警条与操作按钮 loading

## 5. 路由与页面变更
- 路由无新增。
- 页面增强：`/admin/ai-engines` 启停操作提示更清晰、风险反馈更明确。

## 6. 接口联调说明
- 本轮未新增接口。
- 继续复用：
  - `PUT /api/admin/ai-engines/{id}/active`
  - `GET /api/admin/ai-engines`

## 7. frontend/tasks 文档更新
- 新增：`frontend/tasks/TASK_20B_ADMIN_AI_ENGINE_TOGGLE_FEEDBACK_AND_RISK_CONFIRM.md`

## 8. frontend/runtime/STATE.md 更新
- 已追加 `TASK_20B_ADMIN_AI_ENGINE_TOGGLE_FEEDBACK_AND_RISK_CONFIRM` 到已完成任务。
- 下一轮继续 AI 引擎表单可读性与保存反馈优化。

## 9. 构建验证结果
- 命令：`npm.cmd run build`
- 结果：通过

## 10. 下一轮将继续做什么
- 进入 `TASK_20C`：AI 引擎配置表单可读性增强与保存反馈优化。
