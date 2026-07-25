# 合规部署社区与会员能力退役前端（2026-06-07）

## 当前任务所属模块

- 前端模块：用户导航、管理端导航、路由注册、路由预加载、首页、仪表盘、设置页、面试报告页、通知/审计展示、功能图标注册。
- 后端模块：后端接口退役另行记录在 `tasks/TASK_87_COMPLIANCE_REMOVE_COMMUNITY_MEMBERSHIP_BACKEND.md`。

## 前端文件定位

- `frontend/app/src/router/index.js`
- `frontend/app/src/router/routeLoaders.js`
- `frontend/app/src/layouts/MainLayout.vue`
- `frontend/app/src/layouts/AdminLayout.vue`
- `frontend/app/src/components/AppHeader.vue`
- `frontend/app/src/views/HomePageView.vue`
- `frontend/app/src/views/DashboardView.vue`
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/app/src/views/admin/AdminUserRightsView.vue`
- `frontend/app/src/views/admin/AdminDashboardView.vue`
- `frontend/app/src/views/admin/AdminNotificationView.vue`
- `frontend/app/src/views/admin/AdminAuditLogView.vue`
- `frontend/app/src/components/OnboardingGuide.vue`
- `frontend/app/src/utils/request.js`
- `frontend/app/src/utils/errorMessages.js`
- `frontend/app/src/utils/featureIcons.js`
- `frontend/app/src/utils/notificationMeta.js`

## 后端文件定位

后端已在独立任务中退役社区与会员 MVC 接口暴露，保留管理端用户额度接口。

## 本轮修改文件清单

- `frontend/app/src/router/index.js`
- `frontend/app/src/router/routeLoaders.js`
- `frontend/app/src/layouts/MainLayout.vue`
- `frontend/app/src/layouts/AdminLayout.vue`
- `frontend/app/src/components/AppHeader.vue`
- `frontend/app/src/components/OnboardingGuide.vue`
- `frontend/app/src/views/HomePageView.vue`
- `frontend/app/src/views/DashboardView.vue`
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/app/src/views/admin/AdminUserRightsView.vue`
- `frontend/app/src/views/admin/AdminDashboardView.vue`
- `frontend/app/src/views/admin/AdminNotificationView.vue`
- `frontend/app/src/views/admin/AdminAuditLogView.vue`
- `frontend/app/src/utils/request.js`
- `frontend/app/src/utils/errorMessages.js`
- `frontend/app/src/utils/featureIcons.js`
- `frontend/app/src/utils/notificationMeta.js`
- `frontend/app/src/__tests__/components/AppHeader.test.js`
- `frontend/app/src/__tests__/layouts/AdminLayout.test.js`
- `frontend/app/src/__tests__/layouts/MainLayout.test.js`
- `frontend/app/src/__tests__/router/routeLoaders.test.js`
- `frontend/app/src/__tests__/utils/featureIcons.test.js`
- `frontend/app/src/__tests__/views/HomePageView.test.js`
- `frontend/app/src/__tests__/views/InterviewReportView.test.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`
- `frontend/app/src/__tests__/compliance/removeCommunityMembership.test.js`

## 前端实现方案

- 路由退役：从路由表和路由预加载清单中移除 `/community`、`/membership`、`/admin/community`、`/admin/membership` 等社区和会员入口。
- 导航退役：移除用户顶部导航、用户主布局、管理端布局中的社区、会员、社区审核、会员管理菜单。
- 页面入口退役：首页、仪表盘、设置页、面试报告页不再展示社区交流、会员中心、升级会员、VIP 升级和分享到社区入口。
- 通知与错误文案合规化：额度相关提示改为联系管理员处理，不再出现会员/VIP 升级引导。
- 图标资源收敛：活跃功能图标注册表移除 `community-hub`、`community-activity`、`membership-credits`、`membership-center` 等退役功能键，替换为中性能力图标，避免构建产物继续暴露退役功能资源。
- 管理端额度保留：`AdminUserRightsView` 继续保留用户额度查询、调整、重置等管理能力，只移除会员套餐、会员身份和会员订单相关展示。

## 后端实现方案

前端不直接修改后端实现。后端已移除社区/会员控制器的 MVC 暴露，并保留管理端用户额度接口。

## 数据存储方案

- 本轮前端不新增本地存储字段，不修改后端数据库结构。
- 历史社区和会员前端源码文件未物理删除，但已从路由、导航、预加载和活跃入口断开。

## stage 更新说明

- 已在 `frontend/tasks/stage.md` 顶部新增“合规部署社区与会员能力退役前端”记录。
- 后端 stage 另在 `tasks/stage.md` 顶部新增对应记录。

## 编译结果

本前端任务不涉及后端编译；后端编译结果记录在后端任务文件。

## 构建结果

- 前端构建：`npm.cmd run build` 通过。
- 构建后关键词扫描：`rg -n "community-hub|community-activity|membership-credits|membership-center|会员中心|会员套餐|社区审核|社区交流|升级会员|VIP升级|show-vip-upgrade|/membership|/community" dist ...` 对 `dist` 和活跃入口文件扫描无命中。

## 测试结果

- 前端目标回归：`npm.cmd test -- src/__tests__/utils/featureIcons.test.js src/__tests__/compliance/removeCommunityMembership.test.js` 通过，2 个测试文件 / 7 个用例。

## 当前功能验收说明

- 用户侧不再看到社区、会员中心、升级会员、VIP 升级、分享到社区入口。
- 管理端不再看到社区审核、会员管理、会员套餐、会员订单入口。
- 管理端用户额度管理仍可使用，满足“管理端依旧可以修改用户额度”的边界。
- 退役功能历史源码保留但不参与活跃路由和构建入口，降低大范围删除导致的部署风险。

## 停止，不继续下一个功能

本轮仅完成合规部署所需的社区与会员前端入口退役，不继续物理删除历史页面/API 文件、不新增支付替代方案、不扩展其它功能模块。
