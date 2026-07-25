# TASK_36_NOTIFICATION_DETAIL_AND_VERSION_LOG_REDESIGN_FRONTEND

## 1. 当前任务所属模块
- 前台消息通知模块。
- 公开版本日志模块。

## 2. 前端文件定位
- `frontend/app/src/components/AppHeader.vue`
- `frontend/app/src/views/notification/NotificationView.vue`
- `frontend/app/src/views/VersionLogView.vue`
- `frontend/app/src/utils/notificationMeta.js`
- `frontend/app/src/components/notification/NotificationTypeIcon.vue`
- `frontend/app/src/__tests__/utils/notificationMeta.test.js`

## 3. 后端文件定位
- 本轮不修改后端。
- 继续沿用现有用户通知接口和公开版本日志分页接口。

## 4. 本轮修改文件清单
- 重构公开版本日志页为时间线式更新流，增强版本号、标题、内容和发布日期的层级。
- 新增通知类型共享元数据工具，统一通知类型文案、标签类型和图标标识。
- 新增通知类型图标组件，铃铛下拉和通知中心共用同一套线性图标。
- 顶部铃铛下拉补齐公告类通知图标和类型标签，长内容在面板内简略展示。
- 用户消息通知页新增管理端公告详情弹窗，公告类通知点击后展示全文。
- 通知已读状态改为不可变更新，避免直接修改通知对象。
- 新增通知元数据单元测试，覆盖类型映射、公告类型识别、未知类型兜底和时间格式化。

## 5. 前端实现方案
- 通过 `getNotificationTypeMeta(type)` 集中维护 `resume/polish/interview/quota/system/activity/update/maintenance` 的展示信息。
- 通过 `isAdminAnnouncementType(type)` 将 `system/activity/update/maintenance` 判定为管理端公告类通知。
- 公告类通知点击后只在当前页面或铃铛区域打开 `el-dialog` 展示完整内容；业务类通知继续跳转业务详情。
- 列表和铃铛面板对长内容使用行数截断，弹窗使用 `white-space: pre-wrap` 展示完整正文。
- 公开版本日志页使用现有 `getPublicVersionLogsPage({ page, size })` 分页接口，不改变后端数据结构。

## 6. 后端实现方案
- 不新增接口。
- 不新增表字段。
- 管理端公告类型继续沿用现有通知 `type` 字段。

## 7. 数据存储方案
- 本轮不新增数据存储。
- 公告详情弹窗直接使用通知列表已返回的 `title/content/type/createTime/readStatus`。

## 8. stage 更新说明
- 已更新 `frontend/tasks/stage.md`，记录本轮版本日志视觉重构、铃铛通知类型一致性和公告详情弹窗。

## 9. 编译结果
- 本轮不涉及后端编译。

## 10. 构建结果
- 命令：`npm.cmd run build`
- 结果：通过。

## 11. 测试结果
- 命令：`npm.cmd test`
- 结果：9 个测试文件、47 个测试用例通过。

## 12. 当前功能验收说明
- 版本日志页在桌面和移动端使用稳定时间线布局，长内容可读且分页正常。
- 铃铛下拉可展示通知类型标签，公告类图标与全部消息通知页一致。
- 管理端公告类通知内容过长时列表简略显示，点击后通过弹窗完整展示。
- 简历诊断、AI 润色、模拟面试等业务通知仍按原逻辑跳转，不打开公告弹窗。

## 13. 停止，不继续下一个功能
- 本轮仅修复当前反馈的版本日志展示、铃铛通知类型一致性和公告详情查看问题。
- 未扩展公告编辑、后端接口、数据库结构、版本日志搜索或其他新功能。
