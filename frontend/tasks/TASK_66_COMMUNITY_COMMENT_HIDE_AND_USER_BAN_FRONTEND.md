# 社区评论下架与账号封禁前端处理

## 当前任务所属模块

用户端社区、评论区治理入口、管理端用户权益管理。

## 前端文件定位

- `frontend/app/src/api/community.js`
- `frontend/app/src/api/admin/users.js`
- `frontend/app/src/components/admin/AdminUserBanDialog.vue`
- `frontend/app/src/components/community/CommentSection.vue`
- `frontend/app/src/components/community/PostCard.vue`
- `frontend/app/src/views/community/CommunityView.vue`
- `frontend/app/src/views/community/PostDetailView.vue`
- `frontend/app/src/views/admin/AdminUserRightsView.vue`
- `frontend/app/src/utils/adminUserBan.js`

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/CommunityController.java`
- `server/src/main/java/com/airesume/server/controller/AdminController.java`
- `server/src/main/java/com/airesume/server/service/CommunityService.java`

## 本轮修改文件清单

- 用户端社区新增管理员评论/回复下架按钮，原因必填且最多 200 字。
- 用户端帖子卡片、帖子详情、评论/回复作者旁新增管理员封禁入口。
- 新增复用封禁弹窗，支持 `1天 / 7天 / 30天 / 永久` 和必填封禁原因。
- 管理端用户页切换到新封禁/解封接口，批量封禁同样使用封禁弹窗。
- 管理端用户表新增封禁到期和封禁原因摘要，CSV 导出同步包含封禁字段。

## 前端实现方案

前台治理入口只按当前登录用户 `role === 9` 显示，真正权限仍由后端校验。评论下架成功后直接从当前评论区移除内容；顶级评论下架时按 `1 + replyCount` 通知详情页回退评论数，回复下架时只回退 1 条。

封禁弹窗封装为 `AdminUserBanDialog.vue`，所有入口共用时长选项和原因校验，避免社区页和管理端出现不一致的提交格式。

## 数据存储方案

前端不新增本地存储。封禁数据由后端 `sys_user` 字段返回后在管理端列表展示。

## stage 更新说明

已在 `frontend/tasks/stage.md` 追加本轮前端阶段记录；后端记录见 `tasks/stage.md`。

## 构建结果

- `npm.cmd test -- --run src/__tests__/components/community/CommentSection.test.js src/__tests__/components/community/PostCard.test.js src/__tests__/views/community/PostDetailView.test.js src/__tests__/views/community/CommunityView.test.js src/__tests__/views/admin/AdminUserRightsView.test.js src/__tests__/api/admin.users.test.js` 通过。
- `npm.cmd run build` 通过。

## 当前功能验收说明

- 普通用户看不到评论下架和封禁按钮。
- 管理员可下架评论/回复并填写原因。
- 管理员可从帖子、详情页、评论作者处封禁用户。
- 管理端单个/批量封禁使用新接口，解封使用新解封接口。

## 停止，不继续下一个功能

本轮只完成评论下架和账号封禁前端入口，不继续扩展举报系统、社区禁言、信誉分或图片 AI 审核。
