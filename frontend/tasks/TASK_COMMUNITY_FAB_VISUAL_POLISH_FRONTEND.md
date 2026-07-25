# TASK: 社区浮动按钮图标与动效优化（前端）

## 当前任务所属模块
- 前端社区模块。
- 页面：社区首页 `/community`。

## 前端文件定位
- `frontend/app/src/views/community/CommunityView.vue`
- `frontend/app/src/__tests__/views/community/CommunityView.test.js`

## 本轮修改文件清单
- `CommunityView.vue`：优化右下角刷新和发布帖子两个浮动按钮的图标尺寸、背景色、hover 动效和 reduced motion 降级。
- `CommunityView.test.js`：补充浮动按钮图标尺寸、浅色背景、刷新旋转、发帖轻移动效和 reduced motion 的回归测试。

## 前端实现方案
- 将刷新按钮和发布按钮的 `FeatureIcon` 从 `sm` 调整为 `md`，提升图标可读性。
- 将浮动按钮从深橙实心渐变圆改为浅暖色表面、细边框和柔和阴影，避免背景压住业务图标。
- 刷新按钮在 hover 和刷新中对图标执行 `fabRefreshSpin` 旋转动画，反馈“加载/刷新”语义。
- 发布帖子按钮在 hover 时轻微上移，不做旋转，保持新增动作的稳定感。
- 新增 `prefers-reduced-motion: reduce` 降级，关闭旋转和位移动画。

## 后端实现方案
- 本轮不涉及后端接口、DTO、数据库或服务逻辑变更。

## 数据存储方案
- 不新增前端持久化字段。
- 不修改接口响应结构或数据库结构。

## stage 更新说明
- 已在 `frontend/tasks/stage.md` 追加本轮社区浮动按钮优化状态。

## 编译与测试结果
- RED：`npm.cmd test -- --run src/__tests__/views/community/CommunityView.test.js` 失败于按钮仍使用 `size="sm"` 且缺少浅色 FAB 样式与 hover 动效约束。
- GREEN：`npm.cmd test -- --run src/__tests__/views/community/CommunityView.test.js` 通过，1 个测试文件 / 18 个用例通过。
- 构建：`npm.cmd run build` 通过。
- 静态扫描：`rg -n 'fab-button[\s\S]*linear-gradient\(135deg, var\(--orange-main\).*var\(--orange-deep\)' frontend/app/src/views/community/CommunityView.vue` 无匹配。

## 当前功能验收说明
- 社区首页右下角两个浮动按钮图标更大、更清楚。
- 按钮背景不再使用深橙色实心圆，改为浅暖色表面。
- 刷新按钮鼠标移入会旋转，发布按钮鼠标移入会轻微上移。

## 停止说明
- 本轮只优化社区首页两个浮动按钮的视觉与动效，不继续修改社区发帖、帖子列表、评论、分享或后端能力。
