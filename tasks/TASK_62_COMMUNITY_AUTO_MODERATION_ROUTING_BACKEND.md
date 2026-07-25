# TASK_62 社区自动审核分流后端治理

## 当前任务所属模块

社区内容安全治理。目标是在现有先审后发基础上增加自动分流：严重违规直接拒绝，疑似风险或图片内容进入人工审核，低风险纯文本自动通过，降低管理员处理明显正常文本内容的压力。

## 前端文件定位

- `frontend/app/src/api/community.js`
- `frontend/app/src/components/community/PostEditor.vue`
- `frontend/app/src/components/community/ShareReportDialog.vue`
- `frontend/app/src/components/community/CommentSection.vue`

## 后端文件定位

- `server/src/main/java/com/airesume/server/service/CommunityTextModerationService.java`
- `server/src/main/java/com/airesume/server/service/CommunityModerationDecision.java`
- `server/src/main/java/com/airesume/server/service/CommunityService.java`
- `server/src/main/java/com/airesume/server/controller/CommunityController.java`
- `server/src/main/java/com/airesume/server/dto/community/CreateCommunityContentResponse.java`

## 本轮修改文件清单

- 新增：`server/src/main/java/com/airesume/server/service/CommunityTextModerationService.java`
- 新增：`server/src/main/java/com/airesume/server/service/CommunityModerationDecision.java`
- 新增：`server/src/main/java/com/airesume/server/dto/community/CreateCommunityContentResponse.java`
- 修改：`server/src/main/java/com/airesume/server/service/CommunityService.java`
- 修改：`server/src/main/java/com/airesume/server/controller/CommunityController.java`
- 新增：`server/src/test/java/com/airesume/server/service/CommunityTextModerationServiceTest.java`
- 修改：`server/src/test/java/com/airesume/server/service/CommunityServiceModerationTest.java`
- 兼容修复：`server/src/test/java/com/airesume/server/controller/AdminVersionLogControllerTest.java`

## 后端实现方案

- 将社区文本审核规则抽到 `CommunityTextModerationService`。
- 严重违规文本在入库前拒绝，不进入公开区和审核队列。
- 疑似风险文本写入 `pending`，并写入通用复核原因。
- 带图片的帖子和评论继续写入 `pending`，避免未做图片识别时自动公开。
- 低风险纯文本写入 `approved`，直接公开展示。
- 评论只有在自动通过或人工通过时才计入帖子评论数。
- 创建帖子和评论接口返回 `{ id, reviewStatus }`，供前端展示准确提示。

## 数据存储方案

本轮复用 TASK_61 已新增的 `review_status` 和 `review_reason` 字段，不新增表、不新增字段、不新增迁移脚本。

## 编译结果

```bash
mvn.cmd -q "-Dtest=CommunityTextModerationServiceTest,CommunityServiceModerationTest,AdminCommunityModerationServiceTest" test
```

通过。

```bash
mvn.cmd -q "-Dtest=CommunityService*Test,AdminCommunityModerationServiceTest,CriticalEndpointRateLimitFilterTest" test
```

通过。执行前发现 `AdminVersionLogControllerTest` 与当前控制器签名不一致并缺少 `SFunction` 导入，已做最小测试兼容修复，未改版本日志生产接口。

```bash
mvn.cmd -q -DskipTests compile
```

通过。

## 当前功能验收说明

- 明显违规文本会被直接拒绝。
- 图片内容不会自动公开，继续进入人工审核。
- 疑似广告或引流文本进入人工审核。
- 普通低风险纯文本内容自动通过，减少管理员审核量。
- 前端可根据后端返回的审核状态显示准确提示。

## 停止，不继续下一个功能

本轮只完成社区自动审核分流一期，不接入云审核、图片 AI 鉴黄、举报系统、批量审核或敏感词后台配置。
