# 用户端 UI 重构与 Naive UI 迁移方案文档

## 当前任务所属模块

- 模块：用户端前端 UI 重构
- 范围：`frontend/app/src` 下用户端页面、布局、通用 UI 组件与主题样式
- 本轮性质：方案文档生成，不进入代码实现

## 前端文件定位

- 用户端布局入口：`frontend/app/src/layouts/MainLayout.vue`
- 用户端顶部导航：`frontend/app/src/components/AppHeader.vue`
- 用户端全局样式：`frontend/app/src/styles/index.css`
- 业务图标组件：`frontend/app/src/components/common/FeatureIcon.vue`
- 用户端页面目录：
  - `frontend/app/src/views/HomePageView.vue`
  - `frontend/app/src/views/DashboardView.vue`
  - `frontend/app/src/views/growth/GrowthCenterView.vue`
  - `frontend/app/src/views/MembershipView.vue`
  - `frontend/app/src/views/offer/OfferAssistView.vue`
  - `frontend/app/src/views/resume/`
  - `frontend/app/src/views/interview/`
  - `frontend/app/src/views/template/`
  - `frontend/app/src/views/community/`
  - `frontend/app/src/views/notification/`
  - `frontend/app/src/views/settings/`

## 后端文件定位

本轮仅生成前端重构方案文档，不涉及后端文件、接口、数据库结构或服务逻辑。

## 本轮修改文件清单

- 新增：`frontend/tasks/TASK_USER_UI_REFACTOR_NAIVE_UI_FRONTEND.md`
- 修改：`frontend/tasks/stage.md`

## 重构目标

1. 保留当前产品主基调：橙白底色、轻量亲和、求职助手属性明确。
2. 提升用户端页面质感：减少普通卡片堆叠感，建立更统一的页面层次、留白、表面材质、阴影和状态反馈。
3. 增强动态美化：增加克制的页面进入、区块切换、导航反馈、按钮反馈和列表状态切换动效。
4. 保留现有 `FeatureIcon` 图标体系：不替换现有业务图标，不引入新的图标库覆盖当前图标资产。
5. 将用户端 UI 组件库逐步迁移为 Naive UI 优先，同时保留 Element Plus 中迁移成本高或风险高的组件。

## 参考方向

### lvyovo-wiki.tech

- 学习方向：轻量、亲和、信息层次不压迫的页面氛围。
- 可借鉴点：
  - 页面背景保持干净，以内容和轻装饰建立温度。
  - 导航和内容区有明确但不沉重的区隔。
  - 适合 offerCat 当前橙白底色和用户端求职场景。

### room-1913.vercel.app

- 学习方向：质感、场景感、切换动效和沉浸式节奏。
- 可借鉴点：
  - 首屏或关键模块可以有更强的视觉记忆点。
  - 页面切换和内容进入需要有节奏，而不是所有元素同时出现。
  - 动效以空间关系和状态变化为核心，不做无意义循环装饰。

## 技术策略

1. “nativeUI”按当前项目依赖解释为 `naive-ui`。
2. 用户端新增或重构的基础交互组件优先使用 Naive UI：
   - `NButton`
   - `NTag`
   - `NSkeleton`
   - `NEmpty`
   - `NResult`
   - `NDrawer`
   - `NDropdown`
   - `NPopover`
   - `NTooltip`
   - `NDialog`
   - `NTabs`
   - `NCard`
3. Element Plus 暂时保留以下高风险或迁移成本较高组件：
   - 复杂表单校验链路
   - 上传组件
   - 分页组件
   - 表格组件
   - 富文本和模板编辑相关组件
   - 已稳定运行的确认弹窗链路
4. 不修改用户端 API、路由、数据结构和后端响应格式。
5. 不处理 `/admin/**` 管理端页面，避免用户端重构扩大到管理后台。

## 组件规划

### 主题基座

- 在用户端布局入口接入 Naive UI 的主题能力。
- 让 `themeStore.resolvedTheme` 同步控制 Naive UI 亮暗主题。
- 将当前橙色主色同步给 Element Plus 与 Naive UI，避免混用组件时颜色割裂。
- 在 `index.css` 中扩展用户端设计 token：
  - 背景层级
  - 表面材质
  - 边框颜色
  - 阴影层级
  - 圆角层级
  - 动效时长
  - z-index 语义层级

### 页面壳层

- 规划新增 `UserPageShell`：
  - 统一页面标题区
  - 统一页面操作区
  - 统一加载、错误、空状态承载
  - 统一页面最大宽度和响应式留白

### 统一表面组件

- 规划新增 `UserSurface`：
  - 支持普通卡片、轻质感卡片、强调区域三种视觉等级。
  - 避免卡片套卡片。
  - 使用统一 border、shadow、background token。

### 动效工具

- 规划新增用户端 motion utilities：
  - 页面进入：淡入 + 轻微位移。
  - 区块切换：只动画 `transform` 和 `opacity`。
  - 按钮反馈：不超过 200ms。
  - 列表项 hover：不造成布局位移。
  - 遵守 `prefers-reduced-motion`。

### 图标封装增强

- 保留 `FeatureIcon` 当前 key 和图片资源。
- 仅允许增加尺寸 token、可访问性说明、质感包裹样式。
- 不允许替换为 emoji 或其它图标库。

## 页面分批方案

### 第一批：用户端基础体验

- `MainLayout.vue`
- `AppHeader.vue`
- `HomePageView.vue`
- `DashboardView.vue`

目标：
- 建立统一背景、导航、页面容器和首页质感。
- 先完成最常访问路径，作为后续页面的视觉基准。

### 第二批：核心业务承接页

- `GrowthCenterView.vue`
- `MembershipView.vue`
- `OfferAssistView.vue`

目标：
- 统一数据卡、行动入口、状态提示、进度与成长反馈。
- 保持当前业务展示逻辑不变。

### 第三批：主流程页面

- 简历相关页面
- 面试相关页面
- 模板相关页面

目标：
- 统一上传、结果、历史、报告、编辑入口的视觉层次。
- 对复杂组件采取 Element Plus 保留策略，不强行迁移。

### 第四批：社区、通知、设置与公共信息页

- 社区首页、帖子详情、个人动态
- 通知中心
- 设置中心
- 版本日志

目标：
- 统一列表、评论、空状态、详情页和设置分组质感。
- 优先保证信息密度、移动端可用性和可读性。

## 动效规范

1. 每个页面最多设置一个主入场动效。
2. 动效只服务于状态理解和层级切换，不做持续装饰。
3. 交互反馈动效控制在 100ms 到 200ms。
4. 页面和模块入场动效控制在 200ms 到 360ms。
5. 禁止大面积 blur、持续发光和无意义循环动画。
6. 必须支持 `prefers-reduced-motion: reduce`。
7. 禁止动画修改 `width`、`height`、`top`、`left`、`margin`、`padding` 等布局属性。

## 停止边界

1. 不修改 `/admin/**` 管理端页面。
2. 不修改后端接口。
3. 不修改路由结构。
4. 不修改数据库结构。
5. 不新增业务功能。
6. 不替换现有 `FeatureIcon` 业务图标资源。
7. 不重写复杂表单、上传、分页、富文本和模板编辑核心链路。

## 验收方式

后续进入实现阶段时，至少需要执行：

1. 单元测试：
   - 覆盖 `FeatureIcon`
   - 覆盖 `AppHeader`
   - 覆盖 `MainLayout`
   - 覆盖新增通用 UI 组件
2. 页面回归：
   - 首页
   - Dashboard
   - 设置中心
   - 通知中心
   - 成长中心
   - 社区核心页面
3. 构建验证：
   - `npm.cmd test`
   - `npm.cmd run build`
4. 视觉检查：
   - 375px
   - 768px
   - 1024px
   - 1440px
5. 检查项：
   - 无横向滚动
   - 文本不溢出
   - 按钮和图标不重叠
   - 亮暗模式可读
   - 动效不过量
   - 减少动态模式有效

## 编译结果

本轮仅生成方案文档，不修改前端运行代码，不执行前端编译。

## 构建结果

本轮仅生成方案文档，不修改前端运行代码，不执行构建。

## 当前功能验收说明

- 已形成用户端 UI 重构方案文档。
- 已明确 Naive UI 迁移优先级和 Element Plus 保留边界。
- 已明确页面分批、组件规划、动效规范和验收方式。
- 后续实现应以本文档作为用户端 UI 重构的执行依据。

## stage 更新说明

本轮需要在 `frontend/tasks/stage.md` 中追加“用户端 UI 重构方案文档已创建，等待实现”的阶段记录。

## 停止说明

本轮只生成用户端 UI 重构方案文档，不开始代码重构，不继续扩展任何页面功能。
