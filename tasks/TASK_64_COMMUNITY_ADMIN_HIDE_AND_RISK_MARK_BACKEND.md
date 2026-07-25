# 社区前台管理员下架与轻量风控标记后端任务

## 当前任务所属模块

社区内容治理、用户端管理员处置、站内通知。

## 前端文件定位

前端改动见 `frontend/tasks/TASK_64_COMMUNITY_ADMIN_HIDE_AND_RISK_MARK_FRONTEND.md`。

## 后端文件定位

- `server/src/main/java/com/airesume/server/service/CommunityTextModerationService.java`
- `server/src/main/java/com/airesume/server/service/CommunityService.java`
- `server/src/main/java/com/airesume/server/controller/CommunityController.java`
- `server/src/main/java/com/airesume/server/dto/community/AdminHidePostRequest.java`
- `server/src/test/java/com/airesume/server/service/CommunityTextModerationServiceTest.java`
- `server/src/test/java/com/airesume/server/service/CommunityServiceModerationTest.java`
- `server/src/test/java/com/airesume/server/service/CommunityServicePostQueryDeleteTest.java`

## 本轮修改文件清单

- 新增管理员前台下架帖子请求 DTO。
- 新增用户端社区管理员下架接口 `PUT /api/community/posts/{postId}/admin-hide`。
- 社区服务新增管理员角色校验、下架原因校验、帖子 `hidden` 状态写入和作者站内通知。
- 文本审核服务恢复疑似词风险标记：纯文本仍自动通过，但 `review_reason` 写入“疑似风险词命中，已自动放行”。
- 更新社区服务构造器注入测试，生产构造器增加 `NotificationService` 依赖。

## 后端实现方案

- 严重违规文本仍在入库前直接拒绝。
- 纯文本命中疑似词时不进入人工审核，保持 `approved`，但写入风险标记供后台筛查。
- 带图片内容仍保持 `pending`，等待人工审核。
- 管理员下架帖子不物理删除，统一改 `review_status = hidden`，写入 `review_reason`、`reviewed_by`、`reviewed_time`。
- 下架后通过既有 `NotificationService.createNotification(...)` 给发帖用户发送站内通知，`bizType = community_post`，`bizId = postId`。

## 数据存储方案

本轮不新增 SQL，不新增表字段，复用社区表已有审核字段和 `user_notification` 站内通知表。

## 编译结果

- `mvn.cmd -q "-Dtest=CommunityTextModerationServiceTest,CommunityServiceModerationTest,CommunityServicePostQueryDeleteTest,NotificationServiceTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。

## 当前功能验收说明

- 普通用户不能调用管理员下架。
- 管理员下架必须填写原因。
- 下架后帖子从公开社区消失，但数据和原因保留。
- 作者会收到站内通知说明下架原因。
- 疑似词纯文本不增加管理员审核负担，但后台可看到风险标记。

## 停止，不继续下一个功能

本轮只完成帖子级前台管理员下架和轻量风险标记，不扩展评论下架、举报系统、图片 AI 审核、用户信誉分或敏感词后台配置。
