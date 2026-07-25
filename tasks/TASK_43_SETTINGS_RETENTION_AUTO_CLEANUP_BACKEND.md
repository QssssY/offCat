# TASK_43_SETTINGS_RETENTION_AUTO_CLEANUP_BACKEND

## 1. 当前任务所属模块
- 用户侧设置中心
- 用户设置持久化
- 面试记录与简历诊断记录保留期自动清理

## 2. 前端文件定位
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/api/userSettings.js`
- `frontend/app/src/utils/settingsPreferences.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`
- `frontend/app/src/__tests__/utils/settingsPreferences.test.js`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/controller/UserSettingsController.java`
- `server/src/main/java/com/airesume/server/service/UserSettingsService.java`
- `server/src/main/java/com/airesume/server/service/UserDataRetentionCleanupService.java`
- `server/src/main/java/com/airesume/server/service/impl/UserSettingsServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/UserDataRetentionCleanupServiceImpl.java`
- `server/src/main/java/com/airesume/server/scheduler/UserDataRetentionCleanupScheduler.java`
- `server/src/main/java/com/airesume/server/entity/UserSettings.java`
- `server/src/main/java/com/airesume/server/mapper/UserSettingsMapper.java`
- `server/db/schema.sql`
- `server/db/migrations/alter_v4.0_add_user_settings_retention_cleanup.sql`

## 4. 本轮修改文件清单
- 新增用户设置实体、DTO、Mapper、Service、Controller。
- 新增用户数据保留期自动清理 Service 与每日低峰定时任务。
- 扩展面试、简历诊断及其衍生记录的批量逻辑删除查询。
- 新增 `user_settings` 表和保留期清理复合索引。
- 新增后端单元测试：`UserSettingsServiceImplTest`、`UserSettingsControllerTest`、`UserDataRetentionCleanupServiceImplTest`。
- 新增本任务文件并更新 `runtime/STATE.md`。

## 5. 前端实现方案
- 前端实现说明见 `frontend/tasks/TASK_43_SETTINGS_RETENTION_AUTO_CLEANUP_FRONTEND.md`。
- 设置中心通过 `GET /api/user/settings` 读取服务端真实设置，通过 `PUT /api/user/settings` 保存保留天数。
- 页面仍保留本地偏好缓存，但服务端返回值优先覆盖自动清理相关字段，避免旧浏览器缓存被误认为真实清理策略。

## 6. 后端实现方案
- 新增 `GET /api/user/settings` 和 `PUT /api/user/settings`，只允许当前登录用户读取和保存自己的设置。
- `interviewRetentionDays`、`resumeRetentionDays` 仅允许 `0/30/90/180/365`，`0` 表示关闭自动清理。
- 每日 03:30 执行自动清理任务。
- 每个用户每类数据每次最多处理 10 批，每批 200 条。
- 面试记录只清理已结束且超过保留期的会话，并同步逻辑删除聊天记录和岗位定向上下文。
- 简历诊断记录只清理已完成或已失败且超过保留期的任务，并同步逻辑删除 JD 匹配、AI 润色记录，上传文件按安全路径校验后删除。
- 自动清理不在用户请求链路中执行，不做物理删除数据库行。

## 7. 数据存储方案
- 新增 `user_settings` 表，独立保存用户设置，不修改 `sys_user` 主表。
- 新增保留期清理复合索引：
  - `interview_session(user_id, status, is_deleted, create_time)`
  - `resume_diagnosis_task(user_id, status, is_deleted, create_time)`
- 自动清理沿用现有 `is_deleted` 逻辑删除字段。

## 8. stage 更新说明
- 前端阶段记录已更新到 `frontend/tasks/stage.md`。
- 后端/运行状态已更新到 `runtime/STATE.md`。

## 9. 编译结果
- `mvn.cmd test` 通过，358 个后端测试通过。

## 10. 构建结果
- 后端本轮通过 Maven 测试完成编译校验。
- 前端构建结果见前端任务文件。

## 11. 测试结果
- 覆盖用户设置默认值、合法保存、非法保留天数拒绝。
- 覆盖用户设置 Controller 从认证上下文读取当前用户并调用 Service。
- 覆盖面试自动清理同步清理聊天记录和岗位定向上下文。
- 覆盖保留天数为 0 时不触发清理。
- 覆盖简历自动清理同步清理 JD 匹配、AI 润色记录，并跳过非法文件路径。
- 覆盖清理截止时间按保留天数计算。

## 12. 当前功能验收说明
- 用户保存 30/90/180/365 天后，服务端会在每日低峰自动逻辑删除超过保留期的终态历史记录。
- 面试进行中会话不会被自动清理。
- 简历诊断处理中任务不会被自动清理。
- 用户设置接口当前只返回和保存两类记录保留天数。

## 13. 停止，不继续下一个功能
- 本轮只实现面试记录与简历诊断记录保留期自动清理、用户设置持久化。
- 不实现按日期范围筛选清理、勾选项清理、数据导出、物理删除或 AI prompt 详略口径接入。
