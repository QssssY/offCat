# TASK_62 社区自动审核分流前端适配

## 当前任务所属模块

社区内容安全治理前端适配。目标是让发帖、分享报告、评论和回复根据后端返回的 `reviewStatus` 显示准确提示，并避免待审评论被本地乐观插入到公开评论区。

## 前端文件定位

- `frontend/app/src/api/community.js`
- `frontend/app/src/components/community/PostEditor.vue`
- `frontend/app/src/components/community/ShareReportDialog.vue`
- `frontend/app/src/components/community/CommentSection.vue`

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/CommunityController.java`
- `server/src/main/java/com/airesume/server/dto/community/CreateCommunityContentResponse.java`

## 本轮修改文件清单

- 修改：`frontend/app/src/api/community.js`
- 修改：`frontend/app/src/components/community/PostEditor.vue`
- 修改：`frontend/app/src/components/community/ShareReportDialog.vue`
- 修改：`frontend/app/src/components/community/CommentSection.vue`
- 修改：`frontend/app/src/__tests__/components/community/PostEditor.test.js`
- 修改：`frontend/app/src/__tests__/components/community/CommentSection.test.js`

## 前端实现方案

- 发帖返回 `approved` 时提示“发布成功，已公开展示”。
- 发帖返回 `pending` 时提示“已提交审核，通过后将在社区展示”。
- 分享报告同样根据 `reviewStatus` 展示公开或待审提示。
- 评论或回复返回 `approved` 时保留本地乐观插入和计数更新。
- 评论或回复返回 `pending` 时只清空输入并提示待审，不把内容插入公开评论区。

## 后端实现方案

后端创建接口返回 `{ id, reviewStatus }`：

- `POST /api/community/posts`
- `POST /api/community/posts/{postId}/comments`

## 数据存储方案

前端不新增本地持久化。审核状态来自后端创建接口响应。

## 构建结果

```bash
npm.cmd test -- --run src/__tests__/components/community/PostEditor.test.js src/__tests__/components/community/CommentSection.test.js src/__tests__/components/community/ShareReportDialog.test.js
```

通过。

```bash
npm.cmd run build
```

通过。

## 当前功能验收说明

- 用户能区分内容已经公开还是等待审核。
- 待审评论不会短暂出现在当前用户本地评论区，避免和真实公开状态不一致。
- 已自动通过的低风险评论仍能即时出现在评论区。

## 停止，不继续下一个功能

本轮只完成自动审核分流的前端状态适配，不继续扩展举报中心、批量审核、图片预审、敏感词配置页或云审核接入。
