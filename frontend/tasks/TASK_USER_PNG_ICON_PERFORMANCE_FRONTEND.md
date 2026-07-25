# 用户端 PNG 图标性能优化

## 当前任务所属模块
用户端前端静态资源与图标加载性能优化。目标是在保留现有 PNG 插画风格的前提下，降低首次加载图标资源体积，并让首屏关键图标优先加载。

## 前端文件定位
- 图标组件：`frontend/app/src/components/common/FeatureIcon.vue`
- 图标映射：`frontend/app/src/utils/featureIcons.js`
- 首屏调用：`frontend/app/src/components/AppHeader.vue`、`frontend/app/src/views/HomePageView.vue`
- 图标资源：`frontend/app/src/assets/feature-icons/old/`、`frontend/app/src/assets/feature-icons/new/`
- 测试文件：`frontend/app/src/__tests__/components/common/FeatureIcon.test.js`、`frontend/app/src/__tests__/utils/featureIcons.test.js`、`frontend/app/src/__tests__/components/AppHeader.test.js`

## 后端文件定位
本轮不涉及后端文件、接口、数据库或服务端业务逻辑。

## 本轮修改文件清单
- `frontend/app/src/components/common/FeatureIcon.vue`
- `frontend/app/src/utils/featureIcons.js`
- `frontend/app/src/components/AppHeader.vue`
- `frontend/app/src/views/HomePageView.vue`
- `frontend/app/src/__tests__/components/common/FeatureIcon.test.js`
- `frontend/app/src/__tests__/utils/featureIcons.test.js`
- `frontend/app/src/__tests__/components/AppHeader.test.js`
- `frontend/app/src/assets/feature-icons/old/*.webp`
- `frontend/app/src/assets/feature-icons/new/*.webp`
- `frontend/app/src/assets/feature-icons/png-fallback/old/*.png`
- `frontend/app/src/assets/feature-icons/png-fallback/new/*.png`
- `frontend/tasks/TASK_USER_PNG_ICON_PERFORMANCE_FRONTEND.md`
- `frontend/tasks/stage.md`

## 前端实现方案
- `FeatureIcon` 新增 `critical`、`loading`、`fetchPriority` 可选参数；默认仍使用 `loading="lazy"` 和 `decoding="async"`，首屏关键图标通过 `critical` 自动使用 `loading="eager"` 和 `fetchpriority="high"`。
- `FeatureIcon` 改为 `<picture>` 渲染，优先使用 WebP，保留 PNG `img src` 作为 fallback，保证现有视觉风格和兼容性不被破坏。
- `featureIcons.js` 保持现有语义 key 和 `getFeatureIcon()` PNG 返回能力，同时新增 `featureIconSources` 与 `getFeatureIconSource()`，统一返回 `{ webp, png }` 给组件使用。
- 导航栏、Logo、首页 hero 徽标、按钮和快捷入口等首屏可见图标标记为关键资源；下拉菜单、列表、空状态、页面下方卡片等非首屏图标继续懒加载。
- 批量生成 94 个 WebP 图标资源，PNG 原图全部保留。当前 PNG 总体积约 6.01MB，新增 WebP 总体积约 0.73MB。
- 不增加全量 preload，不引入 Service Worker，不改路由、API、业务数据或 UI 库。
- 按验收反馈将 `old/`、`new/` 目录下的 PNG 全部移入 `png-fallback/old/`、`png-fallback/new/`，让运行主目录只保留 WebP，同时保留 PNG fallback 能力。

## 后端实现方案
无后端改动。

## 数据存储方案
不新增数据库表、字段或本地持久化数据。浏览器缓存依赖构建后的 hash 静态资源和部署侧缓存头。

## stage 更新说明
`frontend/tasks/stage.md` 已追加本轮“用户端 PNG 图标性能优化”记录，说明实现范围、验证结果和停止边界。

## 编译与测试结果
- `npm.cmd test -- --run src/__tests__/components/common/FeatureIcon.test.js src/__tests__/utils/featureIcons.test.js src/__tests__/components/AppHeader.test.js` 通过，3 个测试文件 / 14 个用例通过。
- `npm.cmd test -- --run src/__tests__/views/HomePageView.test.js` 通过，1 个测试文件 / 5 个用例通过。
- `npm.cmd test -- --run src/__tests__/components/common/FeatureIcon.test.js` 通过，1 个测试文件 / 6 个用例通过。
- `npm.cmd run build` 通过，Vite 生产构建成功。

## 当前功能验收说明
- 普通图标默认仍为懒加载，关键首屏图标可显式提升加载优先级。
- WebP 图标在支持的浏览器中优先加载，PNG fallback 保持可用。
- PNG fallback 已独立收纳在 `src/assets/feature-icons/png-fallback/`，`old/` 与 `new/` 下不再混放 PNG。
- 静态测试覆盖 WebP 资源存在、PNG fallback 存在、无全量图标 preload、关键图标 eager/high priority。
- 构建产物中 WebP 图标大多在 5KB 到 12KB，显著低于原 PNG 体积。

## 停止说明
本轮只完成用户端 PNG 图标加载与资源体积优化，不继续推进 SVG 替换、Service Worker、CDN 配置、后端缓存头、其它大图资源压缩或页面重构。

## 图片优化后布局回归修复（2026-05-25）
- 问题原因：`OptimizedImage` 将 `img` 包进子组件后，父级 Vue scoped 样式中的 `.logo-img`、`.brand-logo`、`.profile-avatar`、`.message-avatar img`、`.voice-avatar` 无法继续命中子组件内部真实图片，导致 Logo、默认头像和面试官头像按图片固有尺寸参与布局，表现为过度放大和比例失衡。
- 本轮修复：在 `AppHeader.vue`、`LoginView.vue`、`InterviewSessionView.vue`、`SettingsView.vue` 中补充 `:deep()` 样式，恢复原有图片尺寸、圆角和 `object-fit` 约束；保留 `OptimizedImage` 的 WebP 优先和 PNG fallback 行为。
- 验证结果：`npm.cmd test -- --run src/__tests__/utils/optimizedImages.test.js src/__tests__/components/AppHeader.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/SettingsView.test.js` 通过，4 个测试文件 / 59 个用例通过；`npm.cmd run build` 通过。
- 停止边界：本轮只修复图片优化引入的展示比例回归，不继续调整页面视觉设计、不替换图片资源、不修改路由、接口或业务逻辑。

## 追加实现：前端图片与图标性能优化（2026-05-25）

### 当前任务所属模块
用户端前端静态图片与业务插画图标加载性能优化。目标是在保留现有插画视觉风格的前提下，降低首页、导航、登录、个人中心、设置中心、社区默认头像和模拟面试页的大图首屏压力，并把非首屏业务图标从同步映射调整为按需加载。

### 前端文件定位
- 大图资源映射：`frontend/app/src/utils/optimizedImages.js`
- 通用图片组件：`frontend/app/src/components/common/OptimizedImage.vue`
- 图标组件：`frontend/app/src/components/common/FeatureIcon.vue`
- 图标映射：`frontend/app/src/utils/featureIcons.js`
- 首页背景：`frontend/app/src/views/HomePageView.vue`
- 用户端导航：`frontend/app/src/components/AppHeader.vue`
- 登录页：`frontend/app/src/views/auth/LoginView.vue`
- 模拟面试会话：`frontend/app/src/views/interview/InterviewSessionView.vue`
- 用户中心与设置中心：`frontend/app/src/views/DashboardView.vue`、`frontend/app/src/views/settings/SettingsView.vue`
- 社区默认头像：`frontend/app/src/components/community/PostCard.vue`、`frontend/app/src/components/community/CommentSection.vue`、`frontend/app/src/views/community/PostDetailView.vue`
- 测试文件：`frontend/app/src/__tests__/components/common/FeatureIcon.test.js`、`frontend/app/src/__tests__/utils/featureIcons.test.js`、`frontend/app/src/__tests__/utils/optimizedImages.test.js`、`frontend/app/src/__tests__/views/HomePageView.test.js`、`frontend/app/src/__tests__/components/AppHeader.test.js`

### 后端文件定位
本轮不涉及后端接口、数据库、路由或服务端缓存头配置。

### 本轮修改文件清单
- 新增 `frontend/app/src/assets/optimized/background-desktop.webp`
- 新增 `frontend/app/src/assets/optimized/background-mobile.webp`
- 新增 `frontend/app/src/assets/optimized/logo.webp`
- 新增 `frontend/app/src/assets/optimized/assistant.webp`
- 新增 `frontend/app/src/assets/optimized/user.webp`
- 新增 `frontend/app/src/utils/optimizedImages.js`
- 新增 `frontend/app/src/components/common/OptimizedImage.vue`
- 修改 `frontend/app/src/utils/featureIcons.js`
- 修改 `frontend/app/src/components/common/FeatureIcon.vue`
- 修改 `frontend/app/src/components/AppHeader.vue`
- 修改 `frontend/app/src/views/HomePageView.vue`
- 修改 `frontend/app/src/views/auth/LoginView.vue`
- 修改 `frontend/app/src/views/interview/InterviewSessionView.vue`
- 修改 `frontend/app/src/views/DashboardView.vue`
- 修改 `frontend/app/src/views/settings/SettingsView.vue`
- 修改 `frontend/app/src/components/community/PostCard.vue`
- 修改 `frontend/app/src/components/community/CommentSection.vue`
- 修改 `frontend/app/src/views/community/PostDetailView.vue`
- 修改并新增对应单元测试。

### 前端实现方案
- 使用 `sharp` 基于原 PNG 生成优化 WebP：背景桌面版约 186.89KB、背景移动版约 54.92KB、Logo 约 14.62KB、AI 面试官头像约 17.99KB、用户头像约 5.27KB；原 PNG 保留作为 fallback。
- 新增 `optimizedImages` 统一导出大图 `{ webp, png }` 或首页背景 `{ desktopWebp, mobileWebp, png }`，避免页面继续直接引用大体积 PNG。
- 新增 `OptimizedImage.vue` 渲染 `<picture><source type="image/webp"><img src="png fallback">`，用于 Logo、头像、登录页品牌图和面试头像。
- 首页背景改为 CSS `image-set()`，WebP 优先、PNG fallback；768px 以下切换到 `background-mobile.webp`。
- `FeatureIcon` 保留现有 `critical/loading/fetchPriority/name/size/halo/label` 接口；首屏关键图标继续同步命中，非首屏图标通过 `loadFeatureIconSource(name)` 动态加载 WebP 与 PNG fallback。
- `featureIcons.js` 的全量 WebP/PNG glob 改为非 eager，并通过负向 glob 排除首屏关键图标，避免关键图标同时静态和动态导入。
- 社区动态作者头像仍优先使用接口头像；缺省头像改为优化后的 WebP 用户头像，减少默认头像 PNG 的常规加载压力。
- 不做全量 preload，不引入 Service Worker，不新增 UI 库，不改业务逻辑或路由。

### 数据存储方案
不新增数据库表、字段或本地持久化数据。浏览器长期缓存仍依赖构建 hash 产物和部署侧缓存头。

### stage 更新说明
`frontend/tasks/stage.md` 已追加“前端图片与图标性能优化”记录，说明实现范围、验证结果和停止边界。

### 编译与测试结果
- `npm.cmd test -- --run src/__tests__/components/common/FeatureIcon.test.js src/__tests__/utils/featureIcons.test.js src/__tests__/utils/optimizedImages.test.js src/__tests__/views/HomePageView.test.js src/__tests__/components/AppHeader.test.js` 通过，5 个测试文件 / 23 个用例通过。
- `npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/SettingsView.test.js src/__tests__/components/community/PostEditor.test.js src/__tests__/views/community/CommunityView.test.js` 通过，4 个测试文件 / 68 个用例通过。
- `npm.cmd run build` 通过；构建中仅保留第三方 `@vueuse/core` pure 注释提示，无本轮新增资源导入警告。

### 当前功能验收说明
- 首页背景、Logo、assistant、user 均已有 WebP source 和 PNG fallback。
- 首页移动端背景使用更小的 `background-mobile.webp`。
- 默认图标仍 `loading="lazy"`、`decoding="async"`；`critical` 图标仍 `eager` + `high`。
- 非首屏图标不进入同步首屏映射，PNG fallback 路径仍来自 `feature-icons/png-fallback/`。
- 未实现全量 preload feature-icons。

### 停止说明
本轮只完成前端静态图片、大图 WebP 与业务图标按需加载优化；不继续推进工具型图标 SVG 化、Service Worker、CDN 缓存头、后端缓存配置或其它页面重构。

## 追加修复：OptimizedImage 比例失衡修复（2026-05-25）

### 问题原因
大图资源本身没有改变原始比例，但接入 `<picture>` 后新增了中间布局盒。部分页面原样式只约束 `img` 或依赖图片作为容器直接子元素，`picture` 盒子参与布局后会按图片固有尺寸撑开，导致 Logo、头像、面试头像等显示比例失衡。

### 修复方案
- `OptimizedImage.vue` 为 `<picture>` 增加 `optimized-picture` 基础类。
- `.optimized-picture { display: contents; }`，让 `<picture>` 不产生额外布局盒，恢复原来由 `img` 直接参与 `.logo-img`、`.avatar-img`、`.profile-avatar`、`.voice-avatar` 等尺寸规则的布局行为。
- 新增静态测试断言 `OptimizedImage.vue` 保持 `display: contents`，避免后续回退导致图片再次被错误放大。

### 验证结果
- `npm.cmd test -- --run src/__tests__/utils/optimizedImages.test.js src/__tests__/components/AppHeader.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/HomePageView.test.js` 通过，5 个测试文件 / 63 个用例通过。
- `npm.cmd run build` 通过。

### 停止说明
本次只修复 `<picture>` 包装层导致的图片布局比例问题，不继续调整图片裁剪策略、页面布局或资源生成尺寸。
