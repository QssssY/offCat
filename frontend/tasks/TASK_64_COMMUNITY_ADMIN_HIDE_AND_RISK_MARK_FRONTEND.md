# 社区前台管理员下架前端任务

## 当前任务所属模块

用户端社区、管理员前台处置。

## 前端文件定位

- `frontend/app/src/api/community.js`
- `frontend/app/src/components/community/PostCard.vue`
- `frontend/app/src/views/community/CommunityView.vue`
- `frontend/app/src/views/community/PostDetailView.vue`
- `frontend/app/src/__tests__/components/community/PostCard.test.js`
- `frontend/app/src/__tests__/views/community/PostDetailView.test.js`
- `frontend/app/src/__tests__/views/community/CommunityView.test.js`

## 后端文件定位

后端接口和通知逻辑见 `tasks/TASK_64_COMMUNITY_ADMIN_HIDE_AND_RISK_MARK_BACKEND.md`。

## 本轮修改文件清单

- 社区 API 新增 `adminHidePost(postId, { reason })`。
- 帖子卡片新增 `canAdminHide` 控制的管理员“下架”按钮，并通过 `admin-hide` 事件交给页面处理。
- 社区列表页在当前用户 `role === 9` 时展示下架入口，要求填写原因，下架成功后从列表移除帖子。
- 帖子详情页在管理员登录时展示下架入口，成功后返回社区首页。

## 前端实现方案

- 继续复用普通用户端登录态，不混用管理端独立 token。
- 只按 `userStore.userInfo.role === 9` 控制按钮显示，真正权限以后端校验为准。
- 下架原因通过 `ElMessageBox.prompt` 填写，前端做必填校验，避免空原因提交。
- 成功提示“帖子已下架，并已通知用户”；失败显示“下架失败，请稍后重试”。

## 数据存储方案

前端不新增本地持久化数据，不改接口缓存结构；下架成功后清理社区 API 缓存。

## 构建结果

- `npm.cmd test -- --run src/__tests__/components/community/PostCard.test.js src/__tests__/views/community/PostDetailView.test.js` 通过。
- `npm.cmd test -- --run src/__tests__/views/community/CommunityView.test.js` 通过。
- `npm.cmd run build` 通过。

## 当前功能验收说明

- 普通用户看不到下架按钮。
- 管理员在列表卡片和详情页都能下架帖子。
- 未填写原因不能提交。
- 列表页下架成功后移除当前帖子；详情页下架成功后返回社区首页。

## 停止，不继续下一个功能

本轮只完成帖子级管理员前台下架，不扩展评论下架、批量下架、举报入口或管理员专用审核列表。
