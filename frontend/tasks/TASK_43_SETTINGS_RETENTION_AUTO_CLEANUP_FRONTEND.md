# TASK_43_SETTINGS_RETENTION_AUTO_CLEANUP_FRONTEND

## 1. 当前任务所属模块
- 用户侧设置中心
- 数据管理
- 面试偏好与服务端用户设置

## 2. 前端文件定位
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/api/userSettings.js`
- `frontend/app/src/utils/settingsPreferences.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`
- `frontend/app/src/__tests__/utils/settingsPreferences.test.js`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/controller/UserSettingsController.java`
- `server/src/main/java/com/airesume/server/service/impl/UserSettingsServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/UserDataRetentionCleanupServiceImpl.java`
- `server/src/main/java/com/airesume/server/scheduler/UserDataRetentionCleanupScheduler.java`
- 后端详情见 `tasks/TASK_43_SETTINGS_RETENTION_AUTO_CLEANUP_BACKEND.md`

## 4. 本轮修改文件清单
- 新增 `frontend/app/src/api/userSettings.js`
- 修改 `frontend/app/src/views/settings/SettingsView.vue`
- 修改 `frontend/app/src/utils/settingsPreferences.js`
- 修改 `frontend/app/src/__tests__/views/SettingsView.test.js`
- 修改 `frontend/app/src/__tests__/utils/settingsPreferences.test.js`
- 新增 `frontend/tasks/TASK_43_SETTINGS_RETENTION_AUTO_CLEANUP_FRONTEND.md`
- 更新 `frontend/tasks/stage.md`

## 5. 前端实现方案
- 设置中心加载时调用 `GET /api/user/settings`，读取服务端真实生效的保留天数。
- 数据管理区新增“简历诊断保留天数”，与面试记录保留天数使用同一组选项。
- 保存面试记录保留天数、简历诊断保留天数时调用 `PUT /api/user/settings`。
- 保存成功后同步写入本地 `settingsPreferences`，用于当前浏览器展示和旧入口兼容。
- 服务端设置加载失败时保留本机偏好展示，并提示用户服务端设置暂时不可用。
- 服务端设置保存失败时回滚到保存前本地偏好，不伪造成功。

## 6. 后端实现方案
- 后端新增用户设置接口和每日低峰自动清理任务。
- 自动清理按用户设置生效，默认关闭，旧浏览器本地保留天数不会自动迁移成真实删除策略。
- 面试与简历诊断记录均采用逻辑删除和小批量处理，避免给服务器造成高压力。

## 7. 数据存储方案
- 前端新增 `resumeRetentionDays` 本地偏好字段。
- 服务端真实设置保存在 `user_settings` 表。
- 前端本地缓存只用于展示和兼容，不作为自动清理任务的数据来源。

## 8. stage 更新说明
- 已更新 `frontend/tasks/stage.md`，记录本轮用户设置服务端持久化和自动清理接入状态。

## 9. 编译结果
- 后端 `mvn.cmd test` 通过，358 个测试通过。

## 10. 构建结果
- `npm.cmd run build` 通过。

## 11. 测试结果
- `npm.cmd test` 通过，18 个测试文件、84 个测试用例通过。
- 覆盖新增简历诊断保留天数渲染。
- 覆盖设置页加载服务端设置并同步本地偏好。
- 覆盖保存设置时调用后端接口。
- 覆盖接口失败时不误改本地偏好。
- 覆盖本地偏好新增字段默认值、保存合并与非法值归一化。

## 12. 当前功能验收说明
- 用户在设置中心保存面试记录保留天数后，后端每日低峰自动清理超过保留期的已结束面试记录。
- 用户在设置中心保存简历诊断保留天数后，后端每日低峰自动清理超过保留期的已完成或失败诊断记录。
- 设置中心数据管理区只保存面试记录和简历诊断记录保留天数。

## 13. 停止，不继续下一个功能
- 本轮只做保留天数自动清理和用户设置持久化。
- 不继续实现按日期范围清理、勾选清理、数据导出、物理删除或 AI prompt 详略接入。
