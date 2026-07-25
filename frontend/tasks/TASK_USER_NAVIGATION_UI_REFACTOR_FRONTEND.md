# 用户端导航栏 UI 与动效重构记录

## 当前任务所属模块

- 模块：用户端全局导航栏
- 前端文件定位：`frontend/app/src/components/AppHeader.vue`
- 测试文件定位：`frontend/app/src/__tests__/components/AppHeader.test.js`
- 后端文件定位：本轮不涉及后端、接口、路由、数据库或业务流程。

## 本轮修改文件清单

- `frontend/app/src/components/AppHeader.vue`
- `frontend/app/src/__tests__/components/AppHeader.test.js`
- `frontend/tasks/TASK_USER_NAVIGATION_UI_REFACTOR_FRONTEND.md`
- `frontend/tasks/stage.md`

## 前端实现方案

1. 沿用首页重构方向，导航栏保持橙白底色、轻量亲和和克制质感，不引入新图标库，继续复用现有 `FeatureIcon`。
2. 使用项目已有 `motion-v`，为顶部导航根节点增加一次性轻量入场动效，动效只使用 `opacity` 和 `transform`。
3. 桌面导航改为圆角胶囊式入口，通过细边框、浅橙白表面、柔和投影和 active 状态提升层次，不再使用普通下划线式导航。
4. 桌面导航项增加 stagger reveal、hover 位移、图标轻微放大和 active 胶囊反馈，避免所有元素同时出现导致生硬。
5. 移动端汉堡按钮改为暖橙白圆形按钮，移动端抽屉导航项增加轻量滑入和 hover/press 反馈，保持小屏可用性。
6. 主题切换、设置入口、登录入口补齐一致的 hover/press 动效，保证头部右侧操作和主导航质感一致。
7. 增加 `prefers-reduced-motion` 降级：关闭导航项入场动画和位移反馈，保留静态可读状态。

## 技术边界

- 不修改 `/admin/**`。
- 不修改 `MainLayout.vue`、首页、Dashboard 或其它用户端页面。
- 不修改 API、路由、鉴权、通知 SSE、头像菜单命令、数据库和后端业务流程。
- 不新增动画库，不引入 Lenis/locomotive-scroll。
- Element Plus 下拉、通知、抽屉和弹窗链路暂时保留，仅调整外层导航视觉和动效。

## 测试与构建结果

- 已执行：`npm.cmd test -- --run src/__tests__/components/AppHeader.test.js`
- 结果：通过，1 个测试文件 / 4 个用例通过。
- 已执行：`npm.cmd run build`
- 结果：通过。

## 本轮补充修正：移动端抽屉与图标尺寸

- 问题背景：导航栏接入 `motion.header` 后，移动端抽屉仍挂在 header 内部，容易受固定头部和 transform 定位上下文影响，导致小屏点击菜单后无法正常打开。
- 修复方案：为 `el-drawer` 增加 `append-to-body`，让移动端菜单脱离 header 定位上下文；补充测试覆盖点击 `.motion-hamburger-btn` 后 `drawerVisible` 变为 `true`。
- 响应式修复：汉堡按钮设置固定 flex-basis 和更高层级，480px 以下隐藏桌面设置图标入口，设置仍保留在移动端抽屉中，减少小屏头部挤压。
- 图标尺寸修复：桌面导航图标从 21px 提升到 24px；移动端导航图标统一提升到 30px；主题切换、通知、设置和汉堡按钮图标统一到 28-30px，避免太阳/月亮切换后视觉大小不一致。
- 头像下拉修复：全局放大 `user-dropdown-menu` 和 `history-dropdown-menu` 内的 `FeatureIcon` 到 26px，并增加菜单项最小高度，确保头像下拉里的图标能看清。
- 补充验证：`npm.cmd test -- --run src/__tests__/components/AppHeader.test.js` 通过，1 个测试文件 / 5 个用例通过；`npm.cmd run build` 通过。

## 本轮补充修正：导航菜单空白与路由切换卡顿

- 问题背景：移动端抽屉打开后内容为空，核心原因是抽屉导航项仍使用进入动画的初始透明态；同时全局固定头部接入 `motion-v` 运行时动画和大面积 `backdrop-filter`，在路由切换时会增加合成与重绘压力。
- 修复方案：导航栏根节点从 `motion.header` 降级回原生 `header`，移除 `MotionConfig`、`motion-v` import 和 header 入场配置；保留现有 class 作为测试锚点和样式锚点，不再让全局头部参与运行时动画。
- 性能优化：移除固定头部上的 `backdrop-filter` / `-webkit-backdrop-filter`，降低阴影强度；移除桌面导航和移动端抽屉导航项的 stagger opacity 入场动画，确保抽屉打开时导航内容立即可见。
- 保留交互：继续保留 hover、active、press 的轻量反馈，动效仍限制在短时 `transform` 和颜色/边框状态切换，不做路由级动画。
- 补充验证：`rg -n "motion-v|MotionConfig|motion\\.header|headerInitial|nav-item-enter|mobile-nav-item-enter|backdrop-filter|animation: .*nav" frontend/app/src/components/AppHeader.vue` 无匹配；`npm.cmd test -- --run src/__tests__/components/AppHeader.test.js` 通过，1 个测试文件 / 5 个用例通过；`npm.cmd run build` 通过。

## 当前功能验收说明

- 导航栏存在 `.motion-app-header`、`.motion-desktop-nav`、`.motion-brand-mark`、`.motion-hamburger-btn` 和 `.motion-mobile-nav` 结构锚点。
- 桌面端主导航和历史记录入口仍按登录态展示。
- 移动端导航入口仍保留首页、核心业务、历史、个人中心、通知、设置和主题切换。
- 动效遵守性能边界，只使用 `opacity`、`transform` 和静态视觉状态过渡。

## stage 更新说明

本轮已在 `frontend/tasks/stage.md` 追加“用户端导航栏 UI 与动效重构”的阶段记录，后续验收可依据本文件和 stage 记录核对范围。

## 停止说明

本轮只完成用户端导航栏 UI 与动效重构，不继续推进其它页面或新增业务功能。
