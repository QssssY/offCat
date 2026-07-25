# TASK_63 社区轻量化审核后端规则调整

## 当前任务所属模块

社区内容安全治理。本轮目标是在保留图片人工审核和严重违规文本拦截的前提下，放行未命中严重违规词的纯文本帖子和评论，减少管理员逐条审核压力。

## 前端文件定位

本轮未修改前端文件。现有前端继续根据创建接口返回的 `reviewStatus` 展示公开成功或等待审核提示。

## 后端文件定位

- `server/src/main/java/com/airesume/server/service/CommunityTextModerationService.java`
- `server/src/test/java/com/airesume/server/service/CommunityTextModerationServiceTest.java`
- `server/src/test/java/com/airesume/server/service/CommunityServiceModerationTest.java`

## 本轮修改文件清单

- 修改：`server/src/main/java/com/airesume/server/service/CommunityTextModerationService.java`
- 修改：`server/src/test/java/com/airesume/server/service/CommunityTextModerationServiceTest.java`
- 修改：`server/src/test/java/com/airesume/server/service/CommunityServiceModerationTest.java`

## 后端实现方案

- 保留严重违规词命中后直接拒绝的逻辑，违规文本不会入库，也不会进入人工审核队列。
- 保留带图片内容进入 `pending` 的逻辑，图片帖子和图片评论必须等待管理员审核。
- 移除疑似词纯文本进入 `pending` 的分支，未命中严重违规词的纯文本帖子和评论统一自动 `approved`。
- 保持创建帖子和评论接口返回结构 `{ id, reviewStatus }` 不变，避免影响前端调用方。
- 保持评论计数规则不变：只有 `approved` 评论才计入公开评论数，带图待审评论不计数。

## 数据存储方案

本轮复用已有 `review_status` 和 `review_reason` 字段，不新增表、不新增字段、不新增迁移脚本；无需重新导入 SQL。

## stage 更新说明

已在 `tasks/stage.md` 增加本轮“社区轻量化审核后端规则调整”记录，明确当前规则、验证结果和停止边界。

## 编译结果

```bash
mvn.cmd -q -DskipTests compile
```

通过。

## 构建结果

本轮未修改前端，无需执行前端构建。

## 当前功能验收说明

- 疑似但不严重的纯文本帖子会自动公开。
- 疑似但不严重的纯文本评论会自动公开并计入评论数。
- 带图片帖子和带图片评论仍进入人工审核。
- 严重违规文本即使带图片，也会在入库前直接拒绝。
- 管理端审核队列的接口不变，但待审内容会减少为图片内容和后续人工处理内容。

## 验证结果

```bash
mvn.cmd -q "-Dtest=CommunityTextModerationServiceTest,CommunityServiceModerationTest" test
```

通过。

```bash
mvn.cmd -q "-Dtest=CommunityTextModerationServiceTest,CommunityServiceModerationTest,AdminCommunityModerationServiceTest" test
```

通过。

```bash
mvn.cmd -q "-Dtest=CommunityService*Test,AdminCommunityModerationServiceTest,CriticalEndpointRateLimitFilterTest" test
```

通过。

```bash
mvn.cmd -q -DskipTests compile
```

通过。

## 停止，不继续下一个功能

本轮仅完成社区轻量化审核 V2 后端规则调整，不接入图片 AI 审核、不新增举报系统、不新增用户信誉表、不修改 SQL。
