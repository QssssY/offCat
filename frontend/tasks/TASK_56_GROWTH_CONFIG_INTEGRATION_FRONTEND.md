# TASK 56：成长配置接入用户端成长中心（前端）

## 背景

管理端“成长配置”已经能维护激励文案和里程碑配置，本轮将这些配置展示到用户端“个人成长中心”，形成后台配置到前台展示的闭环。

## 本轮范围

- `GrowthCenterView.vue` 读取成长概览接口返回的 `growthConfig`。
- 当存在后台配置时展示“成长激励”和“成长里程碑”区块。
- 无配置时不展示该区块，避免空状态干扰现有页面。
- 新增 `GrowthCenterView.test.js`，覆盖后台配置文案和里程碑的渲染。

## 不做范围

- 不新增用户操作入口。
- 不新增成就达成动画、积分、徽章或规则计算。
- 不改管理端成长配置页面的表单结构。

## 验证

- `npm.cmd test -- --run src/__tests__/views/GrowthCenterView.test.js` 通过。
- `npm.cmd run build` 通过。
