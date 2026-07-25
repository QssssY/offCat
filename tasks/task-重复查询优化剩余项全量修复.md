# 重复查询优化剩余项全量修复任务记录

## 当前任务所属模块

后端重复查询优化，覆盖 `fixes/redundant-query-fix-plan.md` 中第一轮之后的全部剩余项：

- P0-2/P0-3/P0-4：`UserQuotaServiceImpl` 面试/简历检查、扣减、退还链路。
- P1：`AdminUserRightsServiceImpl.getUserRights` 与 `AdminController.getUserQuota`。
- P2：`ResumeDiagnosisTaskServiceImpl.createTask` 与 `MembershipServiceImpl.mockUpgrade`。

## 前端文件定位

本轮不涉及前端文件，不修改页面、不修改前端接口调用、不新增前端状态。

## 后端文件定位

- `server/src/main/java/com/airesume/server/service/impl/UserQuotaServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/AdminUserRightsServiceImpl.java`
- `server/src/main/java/com/airesume/server/controller/AdminController.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/MembershipServiceImpl.java`
- `server/src/test/java/com/airesume/server/service/impl/UserQuotaServiceImplAtomicDeductionTest.java`
- `server/src/test/java/com/airesume/server/service/impl/AdminUserRightsServiceImplTest.java`
- `server/src/test/java/com/airesume/server/controller/AdminControllerTest.java`
- `server/src/test/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImplTest.java`
- `server/src/test/java/com/airesume/server/service/impl/MembershipServiceImplTest.java`

## 本轮修改文件清单

- `UserQuotaServiceImpl`：
  - `checkInterviewQuota`、`checkResumeQuota` 改为一次读取 `getActiveMembershipPlan(userId)` 后计算 VIP 日剩余额度。
  - `deductInterviewQuota`、`deductResumeQuota` 改为一次读取生效套餐后执行 VIP 日额度原子扣减。
  - `refundResumeQuota` 改为一次读取生效套餐后决定退还 SQL 的日额度阈值。
  - 删除已无调用的旧私有 VIP 日额度 helper，避免残留旧查询入口。
- `AdminUserRightsServiceImpl`：
  - `getUserRights` 改为一次 `getByUserId` + 一次 `refreshDailyQuotaIfNeeded`，并直接复用 `SysUser` 快照判断 VIP 状态。
- `AdminController`：
  - `getUserQuota` 改为复用已查到的 `UserQuota`，不再调用两个 `getRemaining*Quota`。
- `ResumeDiagnosisTaskServiceImpl`：
  - 平台 AI 简历诊断任务创建不再先调用 `checkResumeQuota`；改由 `deductResumeQuota` 原子扣减同时负责检查。
  - `deductResumeQuota` 抛出 `RESUME_QUOTA_EXHAUSTED` 时保留额度通知逻辑并继续向上抛出。
- `MembershipServiceImpl`：
  - `mockUpgrade` 写操作后改为一次 `getByUserId` + 一次刷新，避免两个 `getRemaining*Quota` 重复读。
- 测试：
  - 扩展 `UserQuotaServiceImplAtomicDeductionTest` 覆盖 P0 剩余 VIP 查询链路。
  - 新增 `AdminUserRightsServiceImplTest` 覆盖 P1 权益聚合查询次数。
  - 扩展 `AdminControllerTest`、`ResumeDiagnosisTaskServiceImplTest`、`MembershipServiceImplTest` 覆盖 P1/P2 回归。

## 前端实现方案

本轮不涉及前端实现。

## 后端实现方案

- VIP 判断统一复用 `SysUserService.getActiveMembershipPlan(userId)`：返回 `null` 表示非 VIP 或已过期，继续走普通用户额度链路。
- 管理端只读聚合接口复用已加载实体，避免为了展示字段重复调用服务层读方法。
- 简历诊断任务依赖 `deductResumeQuota` 内部原子 SQL 做最终额度检查，避免创建前后重复执行同一条 quota 查询链。
- 会员升级写操作之后只读取一次最新 `UserQuota`，刷新后直接用于响应 DTO。
- 不修改接口签名、返回结构、原子 SQL、数据库结构和前端调用方式。

## 数据存储方案

本轮不新增表、不修改字段、不新增索引，不涉及 `db/` 或 `server/db/` SQL 脚本变更。

## stage 更新说明

已更新根目录 `tasks/stage.md`，记录重复查询优化剩余项已全量完成；第一轮 P0-1 记录保持不变。

## 编译结果

`mvn.cmd -q -DskipTests compile` 通过。

## 构建结果

本轮为后端修改，已执行后端测试与编译验证；不涉及前端构建。

## 当前功能验收说明

- RED 验证：新增/扩展测试后，旧实现下 10 个用例失败，失败点分别确认：
  - P0 剩余方法未调用 `getActiveMembershipPlan`，仍调用 `isVipUser/getVipDaily*Limit`。
  - P1 管理端仍调用 `getRemaining*Quota` 或未复用已查 quota。
  - P2 简历诊断仍调用 `checkResumeQuota`，会员升级仍调用两个 `getRemaining*Quota`。
- GREEN 验证：同一组 RED 用例修复后通过。
- 定向回归：`mvn.cmd -q "-Dtest=UserQuotaServiceImplAtomicDeductionTest,AdminUserRightsServiceImplTest,AdminControllerTest,ResumeDiagnosisTaskServiceImplTest,MembershipServiceImplTest" test` 通过。
- 后端编译：`mvn.cmd -q -DskipTests compile` 通过。
- 后端全量测试：`mvn.cmd -q test` 通过。
- 残留扫描：
  - P0/P1/P2 目标文件中不再命中 `isVipUser(userId)`、`getVipDaily*Limit(userId)`、`getVipCycleLimit(userId, ...)` 的重复组合调用。
  - P1/P2 业务调用位点中不再命中计划要求移除的 `getRemainingResumeQuota/getRemainingInterviewQuota/checkResumeQuota` 重复调用；`UserQuotaServiceImpl` 自身的公共兼容方法声明保留。

## 停止，不继续下一功能

本轮已完成重复查询优化方案中第一轮之后的全部剩余项。未继续新增性能指标上报、SQL 迁移、接口协议、前端页面或其它功能，等待验收。
