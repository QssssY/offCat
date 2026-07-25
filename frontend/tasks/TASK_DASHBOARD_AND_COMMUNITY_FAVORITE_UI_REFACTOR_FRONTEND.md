# 社区收藏反馈与个人中心视觉重构

## 背景

本轮聚焦用户端社区帖子卡片收藏状态和个人中心 `/dashboard` 页面。目标是让“已收藏”状态不再只依赖文字颜色变化，同时把个人中心从普通卡片堆叠调整为更清晰的求职数据工作台。

设计方向沿用当前用户端首页的橙白品牌体系：温暖专业、工具导向、信息优先、轻量动效。实现只处理前端展示，不修改接口、路由、后端、额度计算、社区收藏逻辑和 `/admin/**`。

## 修改范围

- `frontend/app/src/components/community/PostCard.vue`
  - 收藏按钮在 `post.favorited=true` 时展示为更明显的 active pill。
  - 收藏、点赞、评论、分享图标从过小尺寸提升到可读尺寸。
  - 按钮保留 44px 以上触控高度，补齐 hover、press、focus-visible 反馈。
  - 收藏点击仍只触发 `favorite` 事件，不冒泡触发帖子跳转。

- `frontend/app/src/views/DashboardView.vue`
  - 顶部区域调整为 `.profile-workbench` 工作台结构。
  - 额度卡加入 `.quota-overview`，统计区和成长中心入口重新组织视觉层级。
  - “本月诊断 / 本月面试 / 剩余额度 / 成长中心入口 / 查看全部箭头”图标放大并去掉硬方框。
  - “最近简历诊断 / 最近模拟面试”记录项图标结构保持不变。
  - 低风险组件使用 Naive UI：`NSkeleton`、`NButton`。

- `frontend/app/src/__tests__/components/community/PostCard.test.js`
  - 覆盖收藏 active 状态、图标尺寸、点击事件冒泡和源码约束。

- `frontend/app/src/__tests__/views/DashboardView.test.js`
  - 覆盖个人中心工作台结构、图标放大范围、最近记录图标保持、动效约束。

## 视觉与动效约束

- 收藏 active 状态必须包含背景、边框、阴影等可见状态，不只依赖 `color`。
- 图标不使用厚重实心方框；统计和额度类图标以放大图标本体为主。
- 动效只使用 `transform`、`opacity`、`box-shadow`、`border-color`、`background-color` 等低风险属性。
- 禁止 `transition: all`。
- 保留 `prefers-reduced-motion: reduce` 降级。

## 验证结果

已通过定向测试：

```bash
npm.cmd test -- --run src/__tests__/components/community/PostCard.test.js src/__tests__/views/DashboardView.test.js src/__tests__/views/community/CommunityView.test.js
```

结果：3 个测试文件通过，25 个用例通过。

已通过生产构建：

```bash
npm.cmd run build
```

结果：构建通过，仅保留项目既有的 `@vueuse/core` PURE annotation Rollup 提示。

## 停止边界

- 不修改社区收藏接口、收藏数字段和交互业务逻辑。
- 不修改个人中心额度计算、会员规则或用户数据来源。
- 不修改后端、数据库、路由定义和 `/admin/**`。
- 不重构最近记录列表项图标结构，只放大“查看全部”箭头。
