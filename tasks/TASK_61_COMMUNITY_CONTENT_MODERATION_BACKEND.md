# TASK_61 社区内容审核后端治理

## 当前任务所属模块

社区内容安全治理。目标是把社区发帖、评论从“直接公开”调整为“先审后发”，并补齐最小管理端审核能力、基础文本拦截、关键写接口限流和数据库字段。

## 前端文件定位

- `frontend/app/src/views/admin/AdminCommunityReviewView.vue`
- `frontend/app/src/api/admin/community.js`
- `frontend/app/src/router/index.js`
- `frontend/app/src/router/routeLoaders.js`
- `frontend/app/src/layouts/AdminLayout.vue`
- `frontend/app/src/components/community/PostEditor.vue`
- `frontend/app/src/views/community/MyActivity.vue`

## 后端文件定位

- `server/src/main/java/com/airesume/server/service/CommunityService.java`
- `server/src/main/java/com/airesume/server/service/AdminCommunityModerationService.java`
- `server/src/main/java/com/airesume/server/controller/AdminCommunityController.java`
- `server/src/main/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilter.java`
- `server/src/main/java/com/airesume/server/entity/CommunityPost.java`
- `server/src/main/java/com/airesume/server/entity/CommunityComment.java`
- `server/src/main/java/com/airesume/server/dto/community/PostVO.java`
- `server/src/main/java/com/airesume/server/dto/community/CommentVO.java`
- `server/src/main/java/com/airesume/server/dto/admin/AdminCommunityReviewRequest.java`
- `server/src/main/java/com/airesume/server/dto/admin/AdminCommunityPostResponse.java`
- `server/src/main/java/com/airesume/server/dto/admin/AdminCommunityCommentResponse.java`

## 本轮修改文件清单

- 修改：`server/src/main/java/com/airesume/server/common/constants/CommunityConstants.java`
- 修改：`server/src/main/java/com/airesume/server/entity/CommunityPost.java`
- 修改：`server/src/main/java/com/airesume/server/entity/CommunityComment.java`
- 修改：`server/src/main/java/com/airesume/server/dto/community/PostVO.java`
- 修改：`server/src/main/java/com/airesume/server/dto/community/CommentVO.java`
- 修改：`server/src/main/java/com/airesume/server/service/CommunityService.java`
- 修改：`server/src/main/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilter.java`
- 新增：`server/src/main/java/com/airesume/server/controller/AdminCommunityController.java`
- 新增：`server/src/main/java/com/airesume/server/service/AdminCommunityModerationService.java`
- 新增：`server/src/main/java/com/airesume/server/dto/admin/AdminCommunityReviewRequest.java`
- 新增：`server/src/main/java/com/airesume/server/dto/admin/AdminCommunityPostResponse.java`
- 新增：`server/src/main/java/com/airesume/server/dto/admin/AdminCommunityCommentResponse.java`
- 新增：`server/src/test/java/com/airesume/server/service/CommunityServiceModerationTest.java`
- 新增：`server/src/test/java/com/airesume/server/service/AdminCommunityModerationServiceTest.java`
- 修改：`server/src/test/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilterTest.java`
- 修改：`server/src/test/java/com/airesume/server/service/CommunityServicePostQueryDeleteTest.java`
- 修改：`db/schema.sql`
- 修改：`server/db/schema.sql`
- 新增：`db/migrations/TASK_61_COMMUNITY_CONTENT_MODERATION.sql`
- 新增：`server/db/migrations/TASK_61_COMMUNITY_CONTENT_MODERATION.sql`

## 后端实现方案

- 新增审核状态常量：`pending`、`approved`、`rejected`、`hidden`。
- 新发帖子和新评论默认写入 `pending`，不再直接进入社区公开列表。
- 公共社区列表、详情、评论和回复读取路径只展示 `approved` 内容；作者自己的帖子列表保留全部状态，用于查看待审或拒绝原因。
- 在入库前增加最小规则拦截，对明确政治敏感、色情、辱骂、诈骗、广告等高风险文本直接抛出业务错误，剩余正常内容进入人工审核池。
- 新增管理端社区审核接口，管理员可分页查看帖子和评论，并将内容置为通过、拒绝或隐藏。
- 评论通过时才增加帖子评论数；已通过评论被拒绝或隐藏时同步回退评论数。
- 对社区写接口增加限流：发帖 5 次 / 10 分钟，评论 20 次 / 10 分钟，图片上传 20 次 / 10 分钟。
- 管理端接口复用现有管理员身份校验链路，避免普通用户访问审核队列。

## 数据存储方案

- `community_post` 新增 `review_status`、`review_reason`、`reviewed_by`、`reviewed_time`。
- `community_comment` 新增 `review_status`、`review_reason`、`reviewed_by`、`reviewed_time`。
- 历史数据默认 `approved`，避免升级后已有社区内容全部消失。
- 新增审核状态与公开列表复合索引，支撑后台审核队列和前台公开列表查询。
- `db/` 与 `server/db/` 下 schema 和迁移脚本已保持一致；迁移脚本使用可重复执行的字段/索引存在性检查。

## 编译结果

已通过：

```bash
mvn.cmd -q "-Dtest=CommunityServiceModerationTest,CriticalEndpointRateLimitFilterTest" test
mvn.cmd -q "-Dtest=AdminCommunityModerationServiceTest,CommunityServiceModerationTest,CriticalEndpointRateLimitFilterTest" test
mvn.cmd -q "-Dtest=CommunityService*Test,CriticalEndpointRateLimitFilterTest" test
mvn.cmd -q "-Dtest=SchemaConsistencyTest" test
mvn.cmd -q -DskipTests compile
```

## 当前功能验收说明

- 用户发布帖子或评论后，内容先进入待审核状态，提示前端等待审核。
- 未审核或被拒绝内容不会出现在公共社区列表、详情、评论和回复中。
- 管理员可在管理端社区审核页面查看待审内容并处理状态。
- 作者可在“我的帖子”看到待审核、未通过、已隐藏及拒绝原因。
- 写接口已有基础限流，降低刷帖、刷评论和刷图片上传风险。

## 停止，不继续下一个功能

本轮只完成社区内容审核的最小治理闭环，不接入云审核服务、不做图片内容 AI 鉴黄、不新增举报系统、不扩展多级审核流或风控后台。
