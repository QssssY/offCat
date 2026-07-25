# TASK_44_SETTINGS_DATA_MANAGEMENT_SAVE_AND_RESPONSE_DETAIL_REMOVAL_BACKEND

## 1. 当前任务所属模块
- 用户侧设置中心
- 用户设置持久化
- 数据管理保留天数自动清理

## 2. 前端文件定位
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/utils/settingsPreferences.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`
- `frontend/app/src/__tests__/utils/settingsPreferences.test.js`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/dto/user/UserSettingsRequest.java`
- `server/src/main/java/com/airesume/server/dto/user/UserSettingsResponse.java`
- `server/src/main/java/com/airesume/server/entity/UserSettings.java`
- `server/src/main/java/com/airesume/server/service/impl/UserSettingsServiceImpl.java`
- `server/src/test/java/com/airesume/server/service/impl/UserSettingsServiceImplTest.java`
- `server/src/test/java/com/airesume/server/controller/UserSettingsControllerTest.java`
- `server/db/schema.sql`
- `server/db/migrations/alter_v4.0_add_user_settings_retention_cleanup.sql`
- `server/db/migrations/alter_v4.1_drop_user_settings_response_detail.sql`

## 4. 本轮修改文件清单
- 移除用户设置请求、响应、实体和服务实现中的 `responseDetailPreference`。
- 移除建表脚本中的 `response_detail_preference` 字段。
- 新增兼容迁移 `alter_v4.1_drop_user_settings_response_detail.sql`，用于已执行 V4.0 的环境删除旧字段。
- 更新用户设置后端单元测试，保留保留天数校验覆盖。
- 新增本任务文件并更新 `runtime/STATE.md`。

## 5. 前端实现方案
- 前端实现说明见 `frontend/tasks/TASK_44_SETTINGS_DATA_MANAGEMENT_SAVE_AND_RESPONSE_DETAIL_REMOVAL_FRONTEND.md`。

## 6. 后端实现方案
- `GET /api/user/settings` 和 `PUT /api/user/settings` 仅保留 `interviewRetentionDays`、`resumeRetentionDays`。
- 服务层继续校验保留天数只能为 `0/30/90/180/365`，`0` 表示关闭自动清理。
- 自动清理定时任务逻辑不变，仍只读取启用保留天数的用户设置。

## 7. 数据存储方案
- `user_settings` 表只保留面试记录保留天数和简历诊断保留天数。
- 已存在的 `response_detail_preference` 字段通过 V4.1 迁移删除。
- 不新增表，不改变自动清理批量策略。

## 8. stage 更新说明
- 前端阶段记录更新到 `frontend/tasks/stage.md`。
- 后端/运行状态更新到 `runtime/STATE.md`。

## 9. 编译结果
- `mvn.cmd test` 通过，357 个后端测试通过。

## 10. 构建结果
- 后端本轮通过 Maven 测试完成编译校验。
- 前端构建结果见前端任务文件。

## 11. 测试结果
- `mvn.cmd test` 通过，357 个后端测试通过。
- 覆盖用户设置默认值、合法保存和非法保留天数拒绝。
- 覆盖用户设置 Controller 从认证上下文读取当前用户并调用 Service。

## 12. 当前功能验收说明
- 回复详略偏好不再出现在用户设置 API、服务端实体和数据库新建表结构中。
- 保留天数自动清理功能保持原策略，不因移除无用字段而改变。

## 13. 停止，不继续下一个功能
- 本轮只处理数据管理显式保存和回复详略偏好移除。
- 不继续扩展 AI prompt、清理策略、按日期筛选或数据导出能力。
