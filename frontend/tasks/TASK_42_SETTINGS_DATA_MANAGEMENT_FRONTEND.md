# TASK_42_SETTINGS_DATA_MANAGEMENT_FRONTEND

## 1. 当前任务所属模块
- 用户侧设置中心
- 隐私与数据管理

## 2. 前端文件定位
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/api/auth.js`
- `frontend/app/src/api/interview.js`
- `frontend/app/src/api/resume.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/controller/UserAccountController.java`
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/main/java/com/airesume/server/controller/ResumeDiagnosisController.java`
- `server/src/main/java/com/airesume/server/service/impl/UserAccountServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImpl.java`

## 4. 本轮修改文件清单
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/api/auth.js`
- `frontend/app/src/api/interview.js`
- `frontend/app/src/api/resume.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`
- `frontend/tasks/TASK_42_SETTINGS_DATA_MANAGEMENT_FRONTEND.md`
- `frontend/tasks/stage.md`
- 后端修改文件清单见 `tasks/TASK_42_SETTINGS_DATA_MANAGEMENT_BACKEND.md`

## 5. 前端实现方案
- 新增账号注销 API 封装 `deleteAccount`，请求 `POST /api/user/account/delete`。
- 新增面试记录清理 API 封装 `clearInterviewHistory`，请求 `DELETE /api/interview/history`。
- 新增简历诊断清理 API 封装 `clearResumeHistory`，请求 `DELETE /api/resume/history`。
- 设置中心账号注销按钮从禁用改为可操作，弹出危险确认弹窗，要求输入当前登录密码。
- 账号注销提交时展示 loading；成功后清除用户 token 与用户态，并跳转 `/login`。
- 面试记录清理、简历诊断清理按钮从禁用改为可操作，使用 Element Plus 二次确认弹窗。
- 清理成功后展示后端返回的 `deletedCount`，并刷新账号数据概览，确保页面数据与服务端清理结果同步。
- 接口失败时保留当前登录态和本机偏好，不做假成功；错误通过现有请求拦截器和设置页局部错误状态展示。
- 本地缓存清理逻辑保持不变，仍不删除用户端 token 和管理端 token。
- 继续保留危险操作的视觉隔离、说明文案和确认步骤，避免用户误触不可逆操作。

## 6. 后端实现方案
- 后端新增三个接口：
  - `POST /api/user/account/delete`
  - `DELETE /api/interview/history`
  - `DELETE /api/resume/history`
- 后端账号注销采用逻辑删除 + 匿名化，历史清理采用当前用户范围内的逻辑删除。
- 详细后端方案见 `tasks/TASK_42_SETTINGS_DATA_MANAGEMENT_BACKEND.md`。

## 7. 数据存储方案
- 前端不新增持久化字段。
- 不改变 `settingsPreferences` 本地偏好结构。
- “面试记录保留天数”仍只作为本机偏好保存，不触发服务端自动清理。
- 清理接口返回的 `deletedCount` 只用于当次成功反馈，不做本地长期缓存。

## 8. stage 更新说明
- 已更新 `frontend/tasks/stage.md`，记录设置中心账号注销、面试记录清理、简历诊断清理已完成后端接入并等待验收。

## 9. 编译结果
- 后端 `mvn.cmd test` 通过，348 个测试通过。

## 10. 构建结果
- `npm.cmd run build` 通过。

## 11. 测试结果
- `npm.cmd test` 通过，18 个测试文件、82 个测试用例通过。
- 覆盖三个危险操作按钮不再是禁用态。
- 覆盖账号注销输入密码后调用接口，成功后清除用户登录态并跳转登录页。
- 覆盖面试记录清理、简历诊断清理成功后展示清理数量并刷新账号概览。
- 覆盖接口失败时不清除登录态、不误改本机偏好。

## 12. 当前功能验收说明
- 用户可在设置中心发起账号注销，必须输入当前密码确认。
- 用户可在设置中心清理全部面试记录，成功后可看到实际清理数量。
- 用户可在设置中心清理全部简历诊断记录，成功后可看到实际清理数量。
- 账号数据概览会在清理成功后刷新，避免继续展示旧统计。
- 本地缓存清理仍只清理非登录态缓存，不会导致用户意外退出登录。

## 13. 停止，不继续下一个功能
- 本轮只接入三个显式危险操作。
- 不实现面试记录保留天数的服务端自动清理。
- 不新增批量勾选、按日期范围清理、数据导出或账号恢复能力。
