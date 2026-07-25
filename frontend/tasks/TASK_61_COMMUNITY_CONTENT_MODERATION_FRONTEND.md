# TASK_61 社区内容审核前端治理

## 当前任务所属模块

社区内容安全治理前端适配。目标是让用户发帖后明确知道内容进入审核，并给管理员提供最小审核入口，同时让作者能在个人动态中查看审核状态和原因。

## 前端文件定位

- `frontend/app/src/views/admin/AdminCommunityReviewView.vue`
- `frontend/app/src/api/admin/community.js`
- `frontend/app/src/router/index.js`
- `frontend/app/src/router/routeLoaders.js`
- `frontend/app/src/layouts/AdminLayout.vue`
- `frontend/app/src/components/community/PostEditor.vue`
- `frontend/app/src/views/community/MyActivity.vue`

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/AdminCommunityController.java`
- `server/src/main/java/com/airesume/server/service/AdminCommunityModerationService.java`
- `server/src/main/java/com/airesume/server/service/CommunityService.java`
- `server/src/main/java/com/airesume/server/dto/community/PostVO.java`
- `server/src/main/java/com/airesume/server/dto/community/CommentVO.java`

## 本轮修改文件清单

- 新增：`frontend/app/src/api/admin/community.js`
- 新增：`frontend/app/src/views/admin/AdminCommunityReviewView.vue`
- 修改：`frontend/app/src/router/index.js`
- 修改：`frontend/app/src/router/routeLoaders.js`
- 修改：`frontend/app/src/layouts/AdminLayout.vue`
- 修改：`frontend/app/src/components/community/PostEditor.vue`
- 修改：`frontend/app/src/views/community/MyActivity.vue`
- 新增：`frontend/app/src/__tests__/components/community/PostEditor.test.js`
- 修改：`frontend/app/src/__tests__/router/routeLoaders.test.js`
- 修改：`frontend/app/src/__tests__/layouts/AdminLayout.test.js`
- 修改：`frontend/app/src/__tests__/views/community/MyActivity.test.js`

## 前端实现方案

- 发帖成功提示改为“已提交审核，通过后将在社区展示”，避免用户误以为已公开。
- 新增 `/admin/community` 管理端页面，包含帖子审核和评论审核两个标签。
- 管理端审核页支持状态、板块、用户 ID、帖子 ID、关键词筛选，支持查看详情、通过、拒绝和隐藏。
- 管理端侧边栏“运营管理”下新增“社区审核”入口，路由加载器要求管理员身份。
- 新增管理端社区审核 API 封装，保持现有 `adminRequest` 调用风格。
- “我的帖子”卡片展示待审核、未通过、已隐藏状态；存在审核原因时回显“原因：...”。

## 后端实现方案

前端适配后端新增的 `reviewStatus` 和 `reviewReason` 字段，以及管理端审核接口：

- `GET /api/admin/community/posts`
- `GET /api/admin/community/comments`
- `PUT /api/admin/community/posts/{postId}/review`
- `PUT /api/admin/community/comments/{commentId}/review`

## 数据存储方案

前端不新增本地持久化。审核状态来自后端 `PostVO`、`CommentVO` 和管理端审核列表响应。

## 构建结果

已通过：

```bash
npm.cmd test -- --run src/__tests__/views/community/MyActivity.test.js
npm.cmd test -- --run src/__tests__/components/community/PostEditor.test.js src/__tests__/router/routeLoaders.test.js src/__tests__/layouts/AdminLayout.test.js src/__tests__/views/community/MyActivity.test.js
npm.cmd run build
```

## 当前功能验收说明

- 普通用户发帖后看到审核提示，不再误认为内容立即公开。
- 管理员可从后台进入社区审核页面处理帖子和评论。
- 作者在个人动态“我的帖子”中可看到审核状态和拒绝原因。
- 本轮 UI 使用现有 Element Plus 管理端页面模式，未引入新的设计系统或交互框架。

## 停止，不继续下一个功能

本轮只完成社区审核前端入口、审核列表和作者状态回显，不继续扩展举报中心、批量审核、图片预审、敏感词配置页或云审核接入。
