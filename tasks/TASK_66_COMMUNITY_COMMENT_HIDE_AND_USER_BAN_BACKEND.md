# 社区评论下架与账号封禁后端处理

## 当前任务所属模块

社区内容治理、用户账号治理、登录与 JWT 鉴权。

## 前端文件定位

- `frontend/app/src/components/community/CommentSection.vue`
- `frontend/app/src/views/community/CommunityView.vue`
- `frontend/app/src/views/community/PostDetailView.vue`
- `frontend/app/src/views/admin/AdminUserRightsView.vue`

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/CommunityController.java`
- `server/src/main/java/com/airesume/server/controller/AdminController.java`
- `server/src/main/java/com/airesume/server/service/CommunityService.java`
- `server/src/main/java/com/airesume/server/service/impl/AuthServiceImpl.java`
- `server/src/main/java/com/airesume/server/infrastructure/security/JwtAuthenticationFilter.java`
- `server/src/main/java/com/airesume/server/entity/SysUser.java`
- `server/src/main/java/com/airesume/server/dto/admin/*Ban*Request.java`
- `server/src/main/java/com/airesume/server/dto/community/AdminHideCommentRequest.java`
- `db/schema.sql`
- `server/db/schema.sql`
- `db/migrations/TASK_66_USER_BAN_FIELDS.sql`
- `server/db/migrations/TASK_66_USER_BAN_FIELDS.sql`

## 本轮修改文件清单

- 新增评论/回复管理员下架接口：`PUT /api/community/posts/{postId}/comments/{commentId}/admin-hide`。
- 新增账号封禁/解封接口：单个封禁、单个解封、批量封禁、批量解封。
- `sys_user` 新增 `ban_reason`、`banned_until`、`banned_by`、`banned_time`，并同步两份 schema 和 migration。
- 登录和 JWT 鉴权在拒绝禁用账号前先检查临时封禁是否到期，到期后自动清空封禁字段并放行。
- 管理员不能封禁自己，不能封禁其他管理员账号；封禁原因、时长和批量列表均由后端校验。

## 后端实现方案

评论下架采用隐藏语义，不物理删除。管理员下架顶级评论时同步隐藏其直属回复，评论数按本次被隐藏的公开评论/回复数量回退，并逐个通知作者；下架回复时只处理该回复。所有评论下架操作都会写入 `review_status = hidden`、`review_reason`、`reviewed_by`、`reviewed_time`，便于追溯。

账号封禁复用 `sys_user.status = 0` 作为全站禁用状态，同时补充封禁原因、到期时间、操作人和操作时间。`duration = permanent` 时 `banned_until = NULL`，临时封禁到期后登录和 JWT 过滤器会自动解封。通知失败不回滚封禁或下架主操作。

## 数据存储方案

本轮新增 `sys_user` 封禁追溯字段和 `idx_sys_user_banned_until(status, banned_until)` 索引。社区评论下架继续复用 `community_comment.review_status/review_reason/reviewed_by/reviewed_time`。

## stage 更新说明

已在 `tasks/stage.md` 追加本轮后端阶段记录；前端记录见 `frontend/tasks/stage.md`。

## 编译结果

- `mvn.cmd -q "-Dtest=CommunityServicePostQueryDeleteTest,CommunityServiceModerationTest,AdminControllerTest,JwtAuthenticationFilterTest,AuthServiceImplTest,SchemaConsistencyTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。

## 当前功能验收说明

- 普通用户不能调用评论下架接口。
- 管理员下架评论/回复会隐藏内容、回退评论数并通知作者。
- 管理员封禁用户支持 `1d/7d/30d/permanent`，解封会清空封禁字段。
- 封禁期间不能登录，也不能通过已有 token 继续建立认证上下文；临时封禁到期后自动恢复。

## 停止，不继续下一个功能

本轮只完成评论下架与全站账号封禁机制，不继续扩展举报系统、社区禁言、用户信誉分或图片 AI 审核。
