# TASK_11_ADMIN_USER_RIGHTS_ENHANCE

## 所属模块
- 管理端模块
- 子模块：用户与权益管理增强

## 背景
上一轮已经完成：
- `TASK_08_ADMIN_JOB_ROLE_CONFIG`
- `TASK_09_ADMIN_PROMPT_JOB_ROLE_LINK`
- `TASK_10_ADMIN_AI_ENGINE_CONFIG`

当前管理端已经具备岗位配置、Prompt 管理与岗位联动、AI 引擎配置，但在“用户权益管理”上仍有缺口：
- 管理员无法一次性查看用户完整权益状态
- 管理员无法手工调整会员角色、套餐和 VIP 到期时间
- 用户权益修改没有后台留痕结构

## 本轮目标
1. 新增用户权益详情接口 `GET /api/admin/users/{userId}/rights`
2. 新增用户权益修改接口 `PUT /api/admin/users/{userId}/rights`
3. 返回用户完整权益状态，包括角色、套餐、VIP 有效性和额度状态
4. 支持管理员手工修改 `role`、`membershipPlanCode`、`vipExpireTime`
5. 补充用户权益变更日志表，为后续审计查询预留结构
6. 轻量增强现有额度调整接口，允许管理员修正 `lastRefreshDate`
7. 同步更新 task、API 文档和 stage 进度文档

## 本轮 task 拆分
1. 数据层：新增 `user_rights_change_log` 表及迁移脚本
2. 模型层：补实体、Mapper、Service、DTO
3. 聚合服务：实现用户权益详情查询与手工修改逻辑
4. 控制层：在 `AdminController` 增加权益详情 / 修改接口
5. 兼容增强：为现有额度调整接口补充 `lastRefreshDate`
6. 工程资产：更新 task、API 文档、API 索引、stage 状态
7. 验证：执行 `mvn.cmd -q -DskipTests compile`

## 实现清单
- `db/schema.sql`
- `db/migrations/TASK_11_ADMIN_USER_RIGHTS_ENHANCE.sql`
- `server/src/main/java/com/airesume/server/entity/UserRightsChangeLog.java`
- `server/src/main/java/com/airesume/server/mapper/UserRightsChangeLogMapper.java`
- `server/src/main/java/com/airesume/server/service/UserRightsChangeLogService.java`
- `server/src/main/java/com/airesume/server/service/impl/UserRightsChangeLogServiceImpl.java`
- `server/src/main/java/com/airesume/server/dto/admin/UserRightsResponse.java`
- `server/src/main/java/com/airesume/server/dto/admin/UserRightsUpdateRequest.java`
- `server/src/main/java/com/airesume/server/service/AdminUserRightsService.java`
- `server/src/main/java/com/airesume/server/service/impl/AdminUserRightsServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/MembershipPlanService.java`
- `server/src/main/java/com/airesume/server/service/impl/MembershipPlanServiceImpl.java`
- `server/src/main/java/com/airesume/server/dto/admin/UserQuotaUpdateRequest.java`
- `server/src/main/java/com/airesume/server/controller/AdminController.java`
- `docs/api/TASK_06A_ADMIN_API.md`
- `docs/api/API_INDEX.md`
- `runtime/STATE.md`

## 关键规则
- `isVipActive` 必须基于 `role=1` 且 `vipExpireTime > now`
- 用户权益详情中的 `resumeQuota` / `interviewQuota` 使用现有 quota service 的“有效剩余次数”口径
- 非 VIP 角色不应继续保留会员套餐和 VIP 到期时间
- 用户权益修改要同步写入 `user_rights_change_log`

## 验收标准
- 管理端可以查询用户完整权益详情
- 管理端可以手工修改 `role`、`membershipPlanCode`、`vipExpireTime`
- 用户权益修改会记录日志
- 现有额度调整接口支持修正 `lastRefreshDate`
- task、API 文档、stage 文档都已同步更新
