# 会员中心视觉重构

## 背景

本轮重构用户端 `/membership`，目标是把原先的“大渐变 Banner + 套餐卡片堆叠”改成更像会员与额度工作台的页面。

设计方向沿用当前用户端的橙白暖色体系：温暖专业、工具导向、信息优先、轻量动效，不做营销页式夸张装饰，不加云朵/星空，不扩大到 `/admin/**`。

## 修改范围

- `frontend/app/src/views/MembershipView.vue`
  - 顶部重构为 `.membership-workbench-hero`：左侧会员状态、当前套餐、到期时间和权益说明，右侧额度概览。
  - 套餐区重构为 `.plan-comparison-section`，以套餐对比卡呈现价格、周期、每日简历/面试次数、场景说明和 CTA。
  - 当前套餐继续允许“续费”，保留现有 mock 升级逻辑和用户信息刷新逻辑。
  - 去掉 `hero-orb` 旧装饰，改用更克制的表面、边框、阴影和层级。
  - 加入 `prefers-reduced-motion`，动效只使用轻量进入、hover 和 press 反馈。

- `frontend/app/src/__tests__/views/MembershipView.test.js`
  - 覆盖工作台结构、套餐文案、续费/升级文案、升级调用、空状态、加载状态和源码约束。

## 视觉与动效约束

- 会员状态、套餐、额度、权益点继续使用现有 `FeatureIcon`。
- 图标不使用硬方框；允许轻透明承托或裸图标。
- 禁止 `transition: all`。
- 暗色模式必须可读，不能出现灰色蒙层覆盖感。
- 1100px 以下套餐可降为两列，768px 以下单列，避免横向滚动。

## 验证结果

已通过定向测试：

```bash
npm.cmd test -- --run src/__tests__/views/MembershipView.test.js
```

结果：1 个测试文件通过，5 个用例通过。

## 停止边界

- 不改 `/api/membership/plans` 和 `/api/membership/upgrade/mock`。
- 不改会员额度计算、到期顺延、用户信息 store、后端、数据库、路由和 `/admin/**`。
- 不把会员中心改成首页式营销页。
