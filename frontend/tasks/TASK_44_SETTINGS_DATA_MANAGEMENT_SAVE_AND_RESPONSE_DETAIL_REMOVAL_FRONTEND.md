# TASK_44_SETTINGS_DATA_MANAGEMENT_SAVE_AND_RESPONSE_DETAIL_REMOVAL_FRONTEND

## 1. 当前任务所属模块
- 用户侧设置中心
- 面试偏好
- 数据管理

## 2. 前端文件定位
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/utils/settingsPreferences.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`
- `frontend/app/src/__tests__/utils/settingsPreferences.test.js`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/dto/user/UserSettingsRequest.java`
- `server/src/main/java/com/airesume/server/dto/user/UserSettingsResponse.java`
- `server/src/main/java/com/airesume/server/service/impl/UserSettingsServiceImpl.java`
- 后端详情见 `tasks/TASK_44_SETTINGS_DATA_MANAGEMENT_SAVE_AND_RESPONSE_DETAIL_REMOVAL_BACKEND.md`

## 4. 本轮修改文件清单
- 修改 `frontend/app/src/views/settings/SettingsView.vue`
- 修改 `frontend/app/src/utils/settingsPreferences.js`
- 修改 `frontend/app/src/__tests__/views/SettingsView.test.js`
- 修改 `frontend/app/src/__tests__/utils/settingsPreferences.test.js`
- 新增 `frontend/tasks/TASK_44_SETTINGS_DATA_MANAGEMENT_SAVE_AND_RESPONSE_DETAIL_REMOVAL_FRONTEND.md`
- 更新 `frontend/tasks/stage.md`

## 5. 前端实现方案
- 面试偏好下拉继续即时保存到本机 `settingsPreferences`，不再调用服务端用户设置接口。
- 数据管理区移除保留天数下拉的自动保存事件，新增“保存设置”按钮。
- 只有点击“保存设置”时才调用 `PUT /api/user/settings`，避免修改默认岗位、级别、面试模式、反馈模式时触发服务端写入。
- 移除“回复详略偏好”的页面行、选项、API payload 和本机偏好字段。
- 服务端保存失败时回滚到保存前本地偏好，不伪造成功。

## 6. 后端实现方案
- 后端用户设置接口同步移除回复详略偏好字段。
- 自动清理任务不变，仍按服务端持久化的保留天数每日低峰执行。

## 7. 数据存储方案
- 本机设置偏好不再保存 `responseDetailPreference`。
- 服务端真实设置只保存 `interviewRetentionDays`、`resumeRetentionDays`。
- 不新增前端缓存键，不改变登录态缓存清理范围。

## 8. stage 更新说明
- 已更新 `frontend/tasks/stage.md`，记录本轮显式保存和无用偏好移除状态。

## 9. 编译结果
- 后端验证结果见后端任务文件。

## 10. 构建结果
- `npm.cmd run build` 通过。

## 11. 测试结果
- `npm.cmd test` 通过，18 个测试文件、85 个测试用例通过。
- 覆盖面试偏好本机保存不触发服务端保存。
- 覆盖数据管理保留天数点击保存后才调用后端设置接口。
- 覆盖服务端设置保存失败时不误改本地偏好。
- 覆盖本机偏好移除回复详略字段后的默认值、保存合并与非法值归一化。

## 12. 当前功能验收说明
- 修改面试偏好不会再触发服务端用户设置保存。
- 数据管理保留天数需要用户点击保存按钮后才同步服务端。
- 回复详略偏好已从设置中心移除。

## 13. 停止，不继续下一个功能
- 本轮只修正设置中心保存时机并移除无用偏好。
- 不继续扩展 AI prompt 口径、清理策略、数据导出或管理端能力。
