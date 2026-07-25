# 合规部署社区与会员能力退役后端（2026-06-07）

## 当前任务所属模块

- 后端模块：社区控制器、会员控制器、管理端社区/会员控制器、额度不足提示、审计日志角色展示。
- 前端模块：本轮前端另行记录在 `frontend/tasks/TASK_87_COMPLIANCE_REMOVE_COMMUNITY_MEMBERSHIP_FRONTEND.md`。

## 前端文件定位

本后端任务不直接修改前端文件。前端已在独立任务中移除用户侧和管理端可见入口，继续保留管理端用户额度编辑入口。

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/CommunityController.java`
- `server/src/main/java/com/airesume/server/controller/MembershipController.java`
- `server/src/main/java/com/airesume/server/controller/AdminCommunityController.java`
- `server/src/main/java/com/airesume/server/controller/AdminMembershipController.java`
- `server/src/main/java/com/airesume/server/common/result/ResultCode.java`
- `server/src/main/java/com/airesume/server/service/NotificationService.java`
- `server/src/main/java/com/airesume/server/controller/AdminAuditLogController.java`

## 本轮修改文件清单

- `server/src/main/java/com/airesume/server/common/result/ResultCode.java`
- `server/src/main/java/com/airesume/server/controller/AdminAuditLogController.java`
- `server/src/main/java/com/airesume/server/controller/AdminCommunityController.java`
- `server/src/main/java/com/airesume/server/controller/AdminMembershipController.java`
- `server/src/main/java/com/airesume/server/controller/CommunityController.java`
- `server/src/main/java/com/airesume/server/controller/MembershipController.java`
- `server/src/main/java/com/airesume/server/service/NotificationService.java`
- `server/src/test/java/com/airesume/server/common/result/ResultCodeTest.java`
- `server/src/test/java/com/airesume/server/controller/AdminAuditLogControllerTest.java`
- `server/src/test/java/com/airesume/server/controller/RetiredFeatureRoutesTest.java`
- `server/src/test/java/com/airesume/server/service/NotificationServiceTest.java`

## 前端实现方案

本文件只记录后端方案。前端已同步移除社区、会员、VIP 升级、报告分享到社区等可见入口，并保留管理端用户额度管理。

## 后端实现方案

- 社区与会员 MVC 暴露退役：移除用户侧 `CommunityController`、`MembershipController` 以及管理端 `AdminCommunityController`、`AdminMembershipController` 的 `@RestController` 暴露，使相关 `/api/community`、`/api/membership`、`/api/admin/community`、`/api/admin/membership` 不再作为 Spring MVC 接口注册。
- 历史代码和数据保留：暂不删除实体、Mapper、Service、表结构和历史控制器方法体，避免部署前大范围物理删除引入非必要回归。
- 额度不足提示合规化：用户侧额度不足文案改为联系管理员处理额度，不再引导升级会员、VIP 或会员中心。
- 审计日志展示合规化：管理端审计日志中的 `会员用户` 展示改为 `高额度用户`，保留高额度用户识别能力但不暴露会员称谓。
- 管理端额度保留：`AdminController` 现有用户额度查询、调整、重置等接口不退役，管理端仍可修改用户额度。

## 数据存储方案

- 本轮不新增表、字段、索引或迁移脚本。
- 历史社区和会员相关表保留，避免影响已有数据备份、历史审计和后续可控迁移。
- 用户额度仍沿用既有 quota 数据链路，管理端额度编辑能力不受影响。

## stage 更新说明

- 已在 `tasks/stage.md` 顶部新增“合规部署社区与会员能力退役后端”记录。
- 前端 stage 另在 `frontend/tasks/stage.md` 顶部新增对应记录。

## 编译结果

- 后端编译：`mvn.cmd -q -DskipTests compile` 通过。

## 构建结果

本后端任务不涉及前端构建；前端构建结果记录在前端任务文件。

## 测试结果

- 后端目标回归：`mvn.cmd -q "-Dtest=AdminControllerTest,AdminMembershipControllerTest,CommunityControllerImageAccessTest,SecurityConfigTest,RetiredFeatureRoutesTest,ResultCodeTest,NotificationServiceTest,AdminAuditLogControllerTest" test` 通过。
- 覆盖场景包括退役控制器不再作为 MVC endpoint 注册、管理端用户额度接口保留、额度不足提示不再出现会员/VIP 引导、审计日志不再展示会员用户称谓。

## 当前功能验收说明

- 用户侧社区和会员接口不再通过 Spring MVC 暴露。
- 管理端社区审核和会员配置接口不再通过 Spring MVC 暴露。
- 管理端仍可通过用户额度管理能力调整用户额度。
- 额度耗尽时不再引导用户购买会员或进入会员中心。

## 停止，不继续下一个功能

本轮仅完成合规部署所需的社区与会员能力退役，不继续物理删除历史表、历史服务、历史前端源码、支付/订单链路或其它未指定功能。
