# TASK_19C_ADMIN_PROMPT_FORM_UX_AND_VALIDATION_ENHANCE

## 1. 当前任务所属模块
- 管理端前端模块
- 子模块：Prompt 管理与岗位联动完善（表单体验与校验反馈）

## 2. 本轮 task 拆分
1. 优化 Prompt 表单岗位选择可读性（仅启用/全部切换、联动状态提示）。  
2. 增加 Prompt 内容质量校验（最小长度约束）。  
3. 增加一键填充场景模板能力，降低创建成本。  
4. 增加重复配置预警与编辑改动字段反馈。  
5. 更新任务文档与阶段文档。  
6. 执行构建验证。

## 3. task 清单
- [x] 表单增加岗位候选显示策略切换
- [x] 表单增加岗位联动状态提示
- [x] Prompt 内容校验升级为自定义校验（最少 20 字）
- [x] 新增“一键填充场景模板”能力
- [x] 新增重复配置预警与变更字段反馈
- [x] 更新任务文档：`frontend/tasks/TASK_19C_ADMIN_PROMPT_FORM_UX_AND_VALIDATION_ENHANCE.md`
- [x] 更新阶段文档：`frontend/runtime/STATE.md`
- [x] 构建验证通过

## 4. 本轮实现代码（frontend/app）
- 修改：`src/views/admin/AdminPromptView.vue`
  - 新增 `onlyActiveJobRoleOption` 与 `selectableJobRoleOptions`
  - 新增 `selectedJobRoleLinkageStatus` / `selectedJobRoleLinkageText`
  - 新增 `validatePromptContent` 内容校验器
  - 新增 `buildScenarioTemplate` 与 `fillPromptTemplateByScenario`
  - 新增 `findDuplicatePrompt` 重复配置预警
  - 新增 `collectChangedFields` 编辑反馈

## 5. 路由与页面变更
- 路由无新增。
- 页面增强：`/admin/prompts` 的新增/编辑弹窗具备更强校验、提示与快捷填充能力。

## 6. 接口联调说明
- 本轮未新增接口。
- 继续复用：
  - `GET /api/admin/prompts`
  - `POST /api/admin/prompts`
  - `PUT /api/admin/prompts`
  - `GET /api/admin/job-roles`

## 7. frontend/tasks 文档更新
- 新增：`frontend/tasks/TASK_19C_ADMIN_PROMPT_FORM_UX_AND_VALIDATION_ENHANCE.md`

## 8. frontend/runtime/STATE.md 更新
- 已追加 `TASK_19C_ADMIN_PROMPT_FORM_UX_AND_VALIDATION_ENHANCE` 到已完成任务。
- 本次连续执行已完成 3 个轮次，下一组优先进入 AI 引擎配置页完善。

## 9. 构建验证结果
- 命令：`npm.cmd run build`
- 结果：通过

## 10. 下一轮将继续做什么
- 下一组连续轮次将进入 AI 引擎配置页完善（筛选、联动校验、配置反馈和异常可读性优化）。
