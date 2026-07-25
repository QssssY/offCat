# 会员套餐配置链路修复

## 背景

用户端会员中心已经完成视觉重构，但反馈指出管理端会员套餐仍无法真正控制用户端展示：套餐介绍词、标签和额度看起来是前端固定生成，管理端调整额度后用户端与实际扣减逻辑也没有完全同步。

## 修改范围

- `frontend/app/src/views/MembershipView.vue`
  - 套餐介绍优先展示接口返回的 `description`，不再按套餐名称生成固定介绍词。
  - 移除前端硬编码套餐标签，仅保留“当前套餐”这类真实状态标签。
  - 用户端最多展示 6 个套餐，避免套餐区变成长列表。
- `server/src/main/java/com/airesume/server/service/impl/MembershipServiceImpl.java`
  - `/api/membership/plans` 返回管理端配置的 `description`、`resumeQuota`、`interviewQuota`。
  - 会员订单快照记录购买时套餐配置的额度。
- `server/src/main/java/com/airesume/server/controller/AdminMembershipController.java`
  - 创建、单个启用、批量启用时限制最多 6 个启用套餐。
- `server/src/main/java/com/airesume/server/service/impl/SysUserServiceImpl.java`
  - 增加当前 VIP 用户套餐每日简历/面试额度读取入口。
- `server/src/main/java/com/airesume/server/service/impl/UserQuotaServiceImpl.java`
  - VIP 每日额度扣减、检查、退回改为读取当前套餐配置。
- `server/src/main/java/com/airesume/server/service/impl/AuthServiceImpl.java`
  - 用户信息中的 VIP 每日剩余额度改为基于当前套餐配置计算。

## 验证

```bash
npm.cmd test -- --run src/__tests__/views/MembershipView.test.js
mvn.cmd -q "-Dtest=MembershipServiceImplTest,AdminMembershipControllerTest,AuthServiceImplTest" test
mvn.cmd -q test -DskipTests
npm.cmd run build
```

结果：全部通过。前端构建仅保留项目既有的 `@vueuse/core` PURE annotation Rollup 提示。

## 边界

- 不新增数据库字段，不新增套餐 tag 字段。
- 不改会员购买接口路径和路由。
- 不改管理端套餐页面结构，只让现有配置真正生效。
- “最多 6 个套餐”按最多 6 个启用套餐处理，禁用套餐仍可留作草稿或历史配置。
