# 重复查询优化第一轮任务记录

## 当前任务所属模块

后端用户配额服务重复 VIP 查询优化，对应 `fixes/redundant-query-fix-plan.md` 的 P0-1：`checkAndDeductPolishQuota`、`checkAndDeductJdMatchQuota`、`checkAndDeductTemplateQuota`、`checkAndDeductOfferQuota` 四个方法。

## 前端文件定位

本轮不涉及前端文件，不修改页面、不修改前端接口调用、不新增前端状态。

## 后端文件定位

- `server/src/main/java/com/airesume/server/service/impl/UserQuotaServiceImpl.java`
- `server/src/test/java/com/airesume/server/service/impl/UserQuotaServiceImplAtomicDeductionTest.java`

## 本轮修改文件清单

- 修改 `UserQuotaServiceImpl`，四个新功能配额 VIP 扣减分支统一改为调用一次 `sysUserService.getActiveMembershipPlan(userId)`。
- 扩展 `UserQuotaServiceImplAtomicDeductionTest`，新增四个回归用例，锁定 VIP AI 润色、JD 匹配、模板、Offer 扣减不再调用旧的 `isVipUser/getVipDaily*/getVipCycleLimit` 组合链路。
- 更新 `tasks/stage.md`，记录本轮 P0-1 已完成并通过验证。

## 前端实现方案

本轮不涉及前端实现。

## 后端实现方案

- 先补充 P0-1 回归测试，验证四个 VIP 扣减方法应只读取一次生效会员套餐。
- 生产代码中用 `MembershipPlan plan = sysUserService.getActiveMembershipPlan(userId)` 判断 VIP；`plan == null` 继续走非会员免费次数扣减。
- 每日额度直接从套餐字段读取：`dailyPolishLimit/dailyJdMatchLimit/dailyTemplateLimit/dailyOfferLimit`。
- 周期额度直接从套餐字段读取：`totalPolishQuota/totalJdMatchQuota/totalTemplateQuota/totalOfferQuota`。
- 保持原有原子 SQL、异常码、非会员免费次数扣减逻辑不变，避免扩大业务行为改动。

## 数据存储方案

本轮不新增表、不修改字段、不新增索引，不涉及 `db/` 或 `server/db/` SQL 脚本变更。

## stage 更新说明

已更新根目录 `tasks/stage.md`，记录重复查询优化第一轮已完成；P0-2、P0-3、P0-4、P1 和 P2 仍未开始，等待后续单独确认。

## 编译结果

`mvn.cmd -q -DskipTests compile` 通过。

## 构建结果

本轮为后端修改，已执行后端测试与编译验证；不涉及前端构建。

## 当前功能验收说明

- RED 验证：`mvn.cmd -q "-Dtest=UserQuotaServiceImplAtomicDeductionTest#shouldUseActiveMembershipPlanOnceWhenDeductingPolishQuotaForVipUser+shouldUseActiveMembershipPlanOnceWhenDeductingJdMatchQuotaForVipUser+shouldUseActiveMembershipPlanOnceWhenDeductingTemplateQuotaForVipUser+shouldUseActiveMembershipPlanOnceWhenDeductingOfferQuotaForVipUser" test` 在旧实现下失败，失败信息显示旧链路仍调用 `isVipUser`、`getVipDaily*Limit` 和 `getVipCycleLimit`，没有调用 `getActiveMembershipPlan`。
- GREEN 验证：同一组新增用例修复后通过。
- 定向回归：`mvn.cmd -q "-Dtest=UserQuotaServiceImplAtomicDeductionTest" test` 通过。
- 后端编译：`mvn.cmd -q -DskipTests compile` 通过。
- 后端全量测试：`mvn.cmd -q test` 通过。

## 停止，不继续下一功能

本轮仅完成重复查询优化方案中的 P0-1 四个 checkAndDeduct 方法修复。P0-2/3/4、Admin 查询优化、简历诊断 precheck 删除和会员升级写后读优化均未继续推进，等待后续验收或单独指示。
