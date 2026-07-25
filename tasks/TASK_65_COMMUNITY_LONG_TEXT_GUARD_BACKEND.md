# 社区长文本防护后端处理

## 当前任务所属模块

社区帖子前台管理员下架与站内通知。

## 前端文件定位

- `frontend/app/src/components/community/PostCard.vue`
- `frontend/app/src/views/community/CommunityView.vue`
- `frontend/app/src/views/community/PostDetailView.vue`

## 后端文件定位

- `server/src/main/java/com/airesume/server/common/constants/CommunityConstants.java`
- `server/src/main/java/com/airesume/server/dto/community/AdminHidePostRequest.java`
- `server/src/main/java/com/airesume/server/service/CommunityService.java`
- `server/src/test/java/com/airesume/server/service/CommunityServicePostQueryDeleteTest.java`

## 本轮修改文件清单

- 新增管理员下架原因长度常量 `MAX_ADMIN_HIDE_REASON_LENGTH = 200`。
- 新增下架通知标题摘要长度常量 `ADMIN_HIDE_NOTIFICATION_TITLE_MAX_LENGTH = 60`。
- `AdminHidePostRequest` 改为复用统一长度常量。
- `CommunityService.adminHidePost` 增加服务层下架原因长度校验，避免绕过 Controller 校验后写入超长 `review_reason`。
- `CommunityService.safePostTitle` 对通知中的帖子标题做摘要截断，完整标题仍保留在帖子记录中。
- `CommunityServicePostQueryDeleteTest` 补充超长原因拒绝和通知标题截断回归用例。

## 后端实现方案

下架理由仍完整写入 `review_reason`，但最大长度限制为 200 字，低于数据库字段 `VARCHAR(255)`，给系统拼接、字符集和后续扩展留出空间。前端校验只作为体验优化，后端服务层校验作为最终边界。

通知正文中的帖子标题使用 60 字摘要加 `...`，避免用户通知列表被超长标题撑开。完整标题仍可通过帖子详情和管理留痕查询，不丢失原始数据。

## 数据存储方案

不新增 SQL，不修改表结构。继续复用 `community_post.review_reason`、`review_status`、`reviewed_by`、`reviewed_time` 和站内通知表。

## stage 更新说明

已在 `tasks/stage.md` 追加本轮后端阶段记录，并在前端 `frontend/tasks/stage.md` 同步记录前端交互处理。

## 编译结果

- `mvn.cmd -q "-Dtest=CommunityServicePostQueryDeleteTest" test` 通过。
- `mvn.cmd -q "-Dtest=CommunityServicePostQueryDeleteTest,CommunityTextModerationServiceTest,CommunityServiceModerationTest,NotificationServiceTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。

## 当前功能验收说明

- 超过 200 字的管理员下架原因会被后端拒绝。
- 下架通知中的超长帖子标题会被截断为摘要。
- 帖子标题字段本身已有 120 字限制，本轮不新增数据库字段。

## 停止，不继续下一个功能

本轮仅处理标题和下架原因的长文本防护，不继续扩展评论下架、通知详情页重构、举报系统或图片 AI 审核。
