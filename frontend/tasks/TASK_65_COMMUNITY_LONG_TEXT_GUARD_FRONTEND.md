# 社区长文本防护前端处理

## 当前任务所属模块

社区帖子列表、帖子详情页和管理员前台下架弹窗。

## 前端文件定位

- `frontend/app/src/components/community/PostCard.vue`
- `frontend/app/src/views/community/CommunityView.vue`
- `frontend/app/src/views/community/PostDetailView.vue`
- `frontend/app/src/utils/communityAdminHide.js`
- `frontend/app/src/__tests__/components/community/PostCard.test.js`
- `frontend/app/src/__tests__/views/community/CommunityView.test.js`
- `frontend/app/src/__tests__/views/community/PostDetailView.test.js`

## 后端文件定位

- `server/src/main/java/com/airesume/server/common/constants/CommunityConstants.java`
- `server/src/main/java/com/airesume/server/service/CommunityService.java`

## 本轮修改文件清单

- `PostCard.vue`：列表卡片标题限制为 2 行，并保留完整标题的原生悬停提示。
- `PostDetailView.vue`：详情页超长标题限制为 3 行，提供“查看完整标题”弹窗。
- `CommunityView.vue`、`PostDetailView.vue`：管理员下架原因弹窗提示 200 字以内，并使用统一校验函数。
- `communityAdminHide.js`：抽出管理员下架原因长度、详情标题折叠阈值、原因校验和原因裁剪函数。
- 前端测试覆盖列表长标题、详情长标题弹窗、下架原因超长拒绝。

## 前端实现方案

列表页只显示标题摘要，避免信息流卡片被超长标题撑高；详情页保留完整阅读能力，默认压缩长标题并通过弹窗查看完整内容。管理员下架理由继续使用现有 `ElMessageBox.prompt`，通过统一校验函数拒绝空原因和超过 200 字的原因。

## 数据存储方案

前端不新增存储。理由长度与后端 200 字限制保持一致。

## stage 更新说明

已在 `frontend/tasks/stage.md` 追加本轮前端阶段记录；后端记录同步见 `tasks/stage.md`。

## 构建结果

- `npm.cmd test -- --run src/__tests__/components/community/PostCard.test.js src/__tests__/views/community/PostDetailView.test.js src/__tests__/views/community/CommunityView.test.js` 通过。
- `npm.cmd run build` 通过。

## 当前功能验收说明

- 列表页长标题不会撑开卡片。
- 详情页长标题可以通过弹窗查看完整内容。
- 管理员下架原因空值或超过 200 字时不能提交。

## 停止，不继续下一个功能

本轮仅处理标题和下架原因的长文本防护，不扩展通知详情页、评论下架、举报系统或图片 AI 审核。
