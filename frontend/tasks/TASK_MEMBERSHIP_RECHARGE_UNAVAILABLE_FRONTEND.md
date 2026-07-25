# 会员充值与续费未开放提示前端修复

## 当前任务所属模块

用户端会员中心 `/membership`。

## 前端文件定位

- `frontend/app/src/views/MembershipView.vue`
- `frontend/app/src/__tests__/views/MembershipView.test.js`

## 后端文件定位

本轮不修改后端文件，不调整 `/api/membership/plans`、`/api/membership/upgrade/mock` 或会员订单逻辑。

## 本轮修改文件清单

- `frontend/app/src/views/MembershipView.vue`
- `frontend/app/src/__tests__/views/MembershipView.test.js`

## 前端实现方案

- 保留会员中心套餐展示、当前套餐判断、续费按钮和立即升级按钮文案。
- 点击当前套餐的“续费”和其它可购买套餐的“立即升级”时，不再进入 mock 升级请求。
- 统一通过 `ElMessage.warning` 提示：`当前未开放充值功能，请联系管理员进行升级`。
- 移除会员中心组件中不再使用的 `mockUpgradeMembership` 引入、升级中状态和成功刷新用户信息逻辑，避免测试项目误触发充值或续费链路。

## 后端实现方案

无后端改动。充值功能未开放的拦截仅发生在用户端会员中心按钮点击入口，不改变后端接口兼容性。

## 数据存储方案

无数据存储改动，不新增数据库表、字段、索引或 migration。

## RED 验证

在旧实现下新增前端用例后执行：

```bash
npm.cmd test -- --run src/__tests__/views/MembershipView.test.js
```

结果：失败。失败原因是点击续费/升级没有弹出未开放提示，旧逻辑仍会走 mock 升级链路。

## GREEN 与回归验证

```bash
npm.cmd test -- --run src/__tests__/views/MembershipView.test.js
npm.cmd test -- --run src/__tests__/views/MembershipView.test.js src/__tests__/api/performanceCache.test.js
npm.cmd run build
```

结果：全部通过。其中会员中心与缓存相关回归为 2 个测试文件 / 12 个用例通过，前端构建通过。

## 当前功能验收说明

在会员中心点击“续费”或“立即升级”时，页面只提示“当前未开放充值功能，请联系管理员进行升级”，不会调用 mock 会员升级接口，也不会刷新用户会员资料。

## stage 更新说明

已在 `frontend/tasks/stage.md` 追加本轮会员充值与续费未开放提示修复记录。

## 停止，不继续下一个功能

本轮只处理测试项目会员中心充值与续费入口提示，不继续推进真实支付、订单创建、管理员代充值、后端接口调整或会员套餐管理改造。
