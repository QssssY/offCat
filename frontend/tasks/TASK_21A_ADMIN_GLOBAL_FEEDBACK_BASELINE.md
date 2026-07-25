# TASK_21A_ADMIN_GLOBAL_FEEDBACK_BASELINE

## 1. 当前任务所属模块
- 管理端整体收尾优化
- 子模块：全局异常与反馈统一基线

## 2. 本轮 task 拆分
1. 新增管理端统一反馈工具，集中维护成功/失败/警告文案。  
2. 收敛 `adminRequest` 未授权、无权限、网络异常等错误提示。  
3. 增加并发 401 处理锁，避免重复弹错与重复跳转。  
4. 接入登录页和管理布局，统一关键入口反馈语气。  
5. 更新任务文档与阶段文档。  
6. 执行构建验证。

## 3. task 清单
- [x] 新增统一反馈工具：`src/utils/adminFeedback.js`
- [x] 管理端请求层错误提示统一：`src/utils/adminRequest.js`
- [x] 401 并发处理防抖（防重复弹错/跳转）
- [x] 登录与退出反馈统一：`AdminLoginView.vue` / `AdminLayout.vue`
- [x] 更新任务文档：`frontend/tasks/TASK_21A_ADMIN_GLOBAL_FEEDBACK_BASELINE.md`
- [x] 更新阶段文档：`frontend/runtime/STATE.md`
- [x] 构建验证通过

## 4. 本轮实现代码（frontend/app）
- 新增：`src/utils/adminFeedback.js`
  - `showAdminSuccess/showAdminError/showAdminWarning`
  - `resolveAdminStatusErrorMessage`
  - `confirmAdminRiskAction`
- 修改：`src/utils/adminRequest.js`
  - 统一状态码错误文案
  - 业务码 401 与 HTTP 401 统一处理
  - 新增 `isHandlingUnauthorized` 防重入锁
- 修改：`src/layouts/AdminLayout.vue`
  - 接入统一失败/成功反馈
- 修改：`src/views/admin/AdminLoginView.vue`
  - 接入统一登录成功/失败反馈

## 5. 路由与页面变更
- 路由无新增。
- 页面反馈行为统一，不改变功能路径。

## 6. 接口联调说明
- 接口无新增。
- 请求异常反馈改为统一规则，不改变接口契约。

## 7. frontend/tasks 文档更新
- 新增：`frontend/tasks/TASK_21A_ADMIN_GLOBAL_FEEDBACK_BASELINE.md`

## 8. frontend/runtime/STATE.md 更新
- 已追加 `TASK_21A_ADMIN_GLOBAL_FEEDBACK_BASELINE` 到已完成任务。
- 下一轮继续列表交互一致性优化。

## 9. 构建验证结果
- 命令：`npm.cmd run build`
- 结果：通过

## 10. 下一轮将继续做什么
- 进入 `TASK_21B`：岗位配置页交互一致性收敛（刷新/重置/分页/提示风格统一）。
