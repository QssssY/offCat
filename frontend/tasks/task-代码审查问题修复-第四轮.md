# 任务：代码审查问题修复-第四轮

## 当前任务所属模块
- 管理端用户批量操作
- 首页公开版本日志入口

## 前端文件定位
- `frontend/app/src/views/admin/AdminUserRightsView.vue`
- `frontend/app/src/api/admin/users.js`
- `frontend/app/src/views/HomePageView.vue`
- `frontend/app/src/router/index.js`
- `frontend/app/src/views/VersionLogView.vue`
- `frontend/app/src/api/publicVersionLog.js`

## 后端文件定位
- 本轮前端任务文档不记录后端实现细节

## 本轮修改文件清单
1. `frontend/app/src/views/admin/AdminUserRightsView.vue`
2. `frontend/app/src/api/admin/users.js`
3. `frontend/app/src/views/HomePageView.vue`
4. `frontend/app/src/router/index.js`
5. `frontend/app/src/views/VersionLogView.vue`
6. `frontend/app/src/api/publicVersionLog.js`
7. `frontend/app/src/__tests__/api/versionLog.test.js`
8. `frontend/app/src/__tests__/api/admin.users.test.js`

## 前端实现方案
- 批量封禁/解封统一使用字符串 `userId`，避免超长 ID 在浏览器侧丢失精度
- 首页“更多动态”改为跳转公开版本日志页，不再导向需要登录的通知页
- 新增公开版本日志页，复用现有公开版本日志接口展示最近发布内容

## 后端实现方案
- 无

## 数据存储方案
- 无新增前端本地存储

## stage 更新说明
- 在 `frontend/tasks/stage.md` 追加记录本轮代码审查问题修复内容

## 编译结果
- 无

## 构建结果
- `npm.cmd test` 通过
- `npm.cmd run build` 通过

## 当前功能验收说明
1. 管理端批量用户状态更新不再依赖可能失真的数值型 ID
2. 首页公开入口可直接查看版本日志，不再把未登录用户引导到登录页
3. 新增公开版本日志页可展示最近发布的版本内容

## 停止，不继续下一个功能
- 本轮仅修复代码审查指出的问题，不扩展新业务能力
