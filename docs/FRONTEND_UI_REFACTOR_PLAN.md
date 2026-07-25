# 前端 UI 重构方案 — 质感提升与动态效果

> 参考风格：lvyovo-wiki.tech（毛玻璃/科技感）+ room-1913.vercel.app（沉浸/电影质感）
> 目标：引入 Naive UI 替代 Element Plus 作为用户端主组件库，配合毛玻璃质感、散景光斑、微交互动效，全面提升视觉品质。

---

## 一、组件库策略：Naive UI 为主 + Element Plus 保留

### 1.1 现状分析

| 组件库 | 安装状态 | 当前使用范围 | 视觉风格 |
|--------|---------|-------------|---------|
| Element Plus | `^2.4.4` | 50+ 文件，深度使用 | 偏传统、刻板，默认样式丑 |
| Naive UI | `^2.44.1` | 仅 2 个文件（`NMessageProvider`、`NButton`、`NInput`） | 现代、精致，主题系统强大 |

### 1.2 混合策略

```
用户端页面 → Naive UI（主）+ 自定义 CSS 毛玻璃增强
管理后台   → Element Plus（保留，不改）
```

**理由**：
- 管理后台功能优先，Element Plus 的表格/表单/分页够用，不在重构范围内
- 用户端是展示面，需要更好的视觉品质
- Naive UI 的主题系统（CSS-in-JS）支持一键换肤，天然适配暗色模式
- Naive UI 组件设计更现代，默认圆角、间距、配色都优于 Element Plus

### 1.3 Element Plus 保留范围

以下组件在用户端暂时保留（Naive UI 无直接替代或迁移成本过高）：

| Element Plus 组件 | 保留原因 | 所在文件 |
|-------------------|---------|---------|
| `el-upload` | Naive UI 的 `NUpload` API 差异大，需单独适配 | `UploadView.vue` |
| `el-table` + `el-pagination` | 管理后台专用 | admin/* |
| `el-date-picker` | 复杂日期选择器 | admin/* |
| `el-steps` | 步骤条 | admin/* |
| `el-slider` | 滑块 | `SettingsView.vue` |

其余用户端组件全部迁移至 Naive UI。

### 1.4 组件映射表

| Element Plus | Naive UI | 迁移难度 |
|-------------|----------|---------|
| `el-button` | `NButton` | 低 |
| `el-input` / `el-input textarea` | `NInput` / `NInput type="textarea"` | 低 |
| `el-select` + `el-option` | `NSelect` | 低 |
| `el-dialog` | `NModal` | 低 |
| `el-drawer` | `NDrawer` + `NDrawerContent` | 低 |
| `el-switch` | `NSwitch` | 低 |
| `el-tag` | `NTag` | 低 |
| `el-dropdown` | `NDropdown` | 低 |
| `el-popover` | `NPopover` | 低 |
| `el-tooltip` | `NTooltip` | 低 |
| `el-message` | `useMessage()` | 低 |
| `el-message-box` | `useDialog()` | 低 |
| `el-skeleton` | `NSkeleton` | 低 |
| `el-alert` | `NAlert` | 低 |
| `el-radio-group` | `NRadioGroup` | 低 |
| `el-input-number` | `NInputNumber` | 低 |
| `el-form` + `el-form-item` | `NForm` + `NFormItem` | 中 |
| `el-icon` | `NIcon` 或直接用 SVG | 低 |

---

## 二、设计方向定义

### 2.1 视觉关键词

- **毛玻璃** — 半透明卡片 + `backdrop-filter: blur`
- **颗粒质感** — SVG 噪声纹理覆盖，增加画面层次
- **散景光斑** — 呼吸动画的发光圆点，营造氛围
- **微交互** — hover 浮起、聚焦发光、按钮箭头滑动、页面淡入
- **深邃背景** — 多层渐变叠加，而非纯色

### 2.2 配色体系

保留橙色品牌色，Naive UI 主题覆盖：

```js
// Naive UI themeOverrides
const themeOverrides = {
  common: {
    primaryColor: '#FF8C42',
    primaryColorHover: '#FF6B1A',
    primaryColorPressed: '#E55A00',
    primaryColorSuppl: '#FFAB73',
    borderRadius: '12px',
    borderRadiusSmall: '8px',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif',
  },
  Button: {
    borderRadiusMedium: '12px',
    borderRadiusLarge: '14px',
    fontWeight: '500',
  },
  Input: {
    borderRadius: '12px',
    caretColor: '#FF8C42',
  },
  Card: {
    borderRadius: '16px',
  },
  Modal: {
    borderRadius: '20px',
  },
  Tag: {
    borderRadius: '999px',
  },
}
```

### 2.3 新增 CSS 变量（质感层专用）

在 `:root` 中追加：

```css
/* === 毛玻璃 === */
--glass-bg: rgba(255, 255, 255, 0.72);
--glass-bg-hover: rgba(255, 255, 255, 0.82);
--glass-border: rgba(255, 255, 255, 0.45);
--glass-border-hover: rgba(255, 140, 66, 0.3);
--glass-blur: 16px;
--glass-shadow: 0 8px 32px rgba(255, 140, 66, 0.08), 0 1px 3px rgba(0,0,0,0.04);
--glass-shadow-hover: 0 12px 40px rgba(255, 140, 66, 0.14), 0 2px 6px rgba(0,0,0,0.06);

/* === 圆角 === */
--radius-card: 16px;
--radius-card-lg: 20px;
--radius-btn: 12px;
--radius-pill: 999px;

/* === 输入框聚焦光效 === */
--input-focus-glow: 0 0 0 3px rgba(255, 140, 66, 0.18);
--input-focus-border: #FF8C42;

/* === 按钮 === */
--btn-glow: 0 4px 16px rgba(255, 140, 66, 0.3);
--btn-glow-hover: 0 6px 24px rgba(255, 140, 66, 0.4);

/* === 过渡 === */
--transition-fast: 0.15s ease;
--transition-normal: 0.25s ease;
--transition-slow: 0.4s ease;

/* === 散景 === */
--bokeh-color-1: rgba(255, 200, 120, 0.5);
--bokeh-color-2: rgba(255, 160, 80, 0.3);
```

暗色版本（`[data-theme="dark"]`）：

```css
--glass-bg: rgba(34, 34, 59, 0.75);
--glass-bg-hover: rgba(42, 42, 64, 0.85);
--glass-border: rgba(255, 255, 255, 0.1);
--glass-border-hover: rgba(255, 140, 66, 0.3);
--glass-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
--glass-shadow-hover: 0 12px 40px rgba(255, 140, 66, 0.1);
--input-focus-glow: 0 0 0 3px rgba(255, 140, 66, 0.25);
--bokeh-color-1: rgba(255, 180, 100, 0.3);
--bokeh-color-2: rgba(255, 140, 60, 0.2);
```

---

## 三、Phase 1 — 基础设施搭建

### 3.1 Naive UI 全局注册 + 主题注入

**文件**：`frontend/app/src/main.js`

```js
// 替换当前的 app.use(ElementPlus) 为 Naive UI
// 保留 ElementPlus 注册（管理后台仍需要）
import { createDiscreteApi, darkTheme } from 'naive-ui'

// Naive UI 主题配置
const themeOverrides = { /* 如上 */ }

// 在 App.vue 或 MainLayout 中通过 NConfigProvider 注入
```

### 3.2 新建主题配置文件

**新建文件**：`frontend/app/src/config/naiveTheme.js`

集中管理 Naive UI 的 `themeOverrides` 和 `theme`（暗色模式切换），与现有 `stores/theme.js` 联动。

### 3.3 Naive UI 暗色模式集成

**修改文件**：`frontend/app/src/stores/theme.js`

当前通过 `data-theme="dark"` 属性切换暗色。Naive UI 使用 `darkTheme` 对象。需要在主题 store 中暴露一个计算属性 `naiveTheme`，返回 `darkTheme` 或 `null`（亮色）。

### 3.4 `index.css` 新增质感全局样式

**文件**：`frontend/app/src/styles/index.css`

新增以下 class（Phase 2-6 复用）：

```css
/* 毛玻璃卡片 */
.glass-card {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-card);
  box-shadow: var(--glass-shadow);
  transition: all var(--transition-normal);
}
.glass-card:hover {
  background: var(--glass-bg-hover);
  border-color: var(--glass-border-hover);
  box-shadow: var(--glass-shadow-hover);
}

/* 颗粒纹理 */
.grain-overlay::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: 9999;
  pointer-events: none;
  opacity: 0.04;
  mix-blend-mode: overlay;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='200' height='200'><filter id='n'><feTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3'/></filter><rect width='100%' height='100%' filter='url(%23n)' opacity='0.5'/></svg>");
}

/* 暗角 */
.vignette::after {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 9998;
  background: radial-gradient(ellipse at center, transparent 60%, rgba(0,0,0,0.06) 100%);
}

/* 散景光斑容器 */
.bokeh-container {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}
.bokeh-dot {
  position: absolute;
  border-radius: 50%;
  animation: bokehPulse 8s ease-in-out infinite;
}
@keyframes bokehPulse {
  0%, 100% { opacity: var(--bokeh-opacity, 0.3); transform: scale(1); }
  50% { opacity: calc(var(--bokeh-opacity, 0.3) * 1.5); transform: scale(1.15); }
}

/* 按钮光泽扫过 */
.btn-shine {
  position: relative;
  overflow: hidden;
}
.btn-shine::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
  transition: left 0.5s ease;
}
.btn-shine:hover::after {
  left: 100%;
}

/* 页面入场动画 */
.page-enter-active { animation: pageSlideIn 0.4s ease-out; }
.page-leave-active { animation: pageSlideOut 0.2s ease-in; }
@keyframes pageSlideIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes pageSlideOut {
  from { opacity: 1; }
  to { opacity: 0; transform: translateY(-4px); }
}

/* 级联入场 */
.stagger-enter > * {
  opacity: 0;
  transform: translateY(16px);
  animation: staggerFadeIn 0.4s ease-out forwards;
}
.stagger-enter > *:nth-child(1) { animation-delay: 0.05s; }
.stagger-enter > *:nth-child(2) { animation-delay: 0.1s; }
.stagger-enter > *:nth-child(3) { animation-delay: 0.15s; }
.stagger-enter > *:nth-child(4) { animation-delay: 0.2s; }
.stagger-enter > *:nth-child(5) { animation-delay: 0.25s; }
.stagger-enter > *:nth-child(6) { animation-delay: 0.3s; }
@keyframes staggerFadeIn {
  to { opacity: 1; transform: translateY(0); }
}

/* Naive UI 组件毛玻璃增强（全局覆盖） */
.n-card {
  backdrop-filter: blur(12px) !important;
  -webkit-backdrop-filter: blur(12px) !important;
}
.n-dialog {
  backdrop-filter: blur(20px) !important;
  -webkit-backdrop-filter: blur(20px) !important;
}
.n-drawer {
  backdrop-filter: blur(16px) !important;
  -webkit-backdrop-filter: blur(16px) !important;
}

/* 减弱动态效果 */
@media (prefers-reduced-motion: reduce) {
  .bokeh-dot,
  .stagger-enter > *,
  .page-enter-active,
  .page-leave-active {
    animation: none !important;
    opacity: 1 !important;
    transform: none !important;
  }
  .btn-shine::after { display: none; }
}
```

---

## 四、Phase 2 — 全局质感层

**文件**：`frontend/app/src/layouts/MainLayout.vue`

### 4.1 结构改动

```html
<template>
  <n-config-provider :theme="naiveTheme" :theme-overrides="themeOverrides">
    <div class="main-layout grain-overlay vignette">
      <!-- 散景光斑 -->
      <div class="bokeh-container" aria-hidden="true">
        <span v-for="n in 12" :key="n" class="bokeh-dot" :style="bokehStyle(n)" />
      </div>

      <AppHeader />
      <main class="layout-main">
        <section class="layout-content">
          <n-message-provider>
            <router-view v-slot="{ Component }">
              <Transition name="page-slide" mode="out-in">
                <component :is="Component" />
              </Transition>
            </router-view>
          </n-message-provider>
        </section>
      </main>
      <AppFooter />
      <OnboardingGuide v-if="showOnboarding" @close="showOnboarding = false" />
    </div>
  </n-config-provider>
</template>
```

### 4.2 主题联动

```js
import { NConfigProvider, NMessageProvider, darkTheme } from 'naive-ui'
import { useThemeStore } from '@/stores/theme'
import { themeOverrides } from '@/config/naiveTheme'

const themeStore = useThemeStore()
const naiveTheme = computed(() => themeStore.resolvedTheme === 'dark' ? darkTheme : null)
```

### 4.3 散景样式

```js
const bokehStyle = (n) => ({
  left: `${(n * 23 + 10) % 90}%`,
  top: `${(n * 31 + 15) % 80}%`,
  width: `${4 + (n % 4) * 3}px`,
  height: `${4 + (n % 4) * 3}px`,
  background: `radial-gradient(circle, var(--bokeh-color-1), var(--bokeh-color-2) 60%, transparent 80%)`,
  opacity: 0.2 + (n % 3) * 0.1,
  filter: `blur(${2 + (n % 3)}px)`,
  animationDelay: `${n * 0.8}s`,
})
```

---

## 五、Phase 3 — 导航栏毛玻璃化

**文件**：`frontend/app/src/components/AppHeader.vue`

### 5.1 迁移 Element Plus → Naive UI

| 当前 | 替换为 |
|------|--------|
| `el-drawer` | `NDrawer` + `NDrawerContent` |
| `el-dropdown` | `NDropdown` |
| `el-popover` | `NPopover` |
| `el-badge` | `NBadge` |
| `el-icon` | `NIcon` 或直接 SVG |
| `el-tooltip` | `NTooltip` |

### 5.2 样式改动

```css
.app-header {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border-bottom: 1px solid var(--glass-border);
  box-shadow: 0 1px 12px rgba(0,0,0,0.04);
  transition: all var(--transition-normal);
}
```

---

## 六、Phase 4 — 首页 Hero 散景 + 按钮重写

**文件**：`frontend/app/src/views/HomePageView.vue`

### 6.1 迁移 Element Plus → Naive UI

| 当前 | 替换为 |
|------|--------|
| `el-button` | `NButton` |
| `el-skeleton-item` | `NSkeleton` |
| `el-tag` | `NTag` |

### 6.2 Hero 背景升级

将 `.bg-gradient` 单个圆替换为多层散景：

```css
.hero-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  z-index: 0;
}
.bg-gradient {
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255,140,66,0.15) 0%, transparent 70%);
  filter: blur(60px);
  position: absolute;
  top: -100px;
  right: -100px;
}
.bg-gradient::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255,106,26,0.1) 0%, transparent 70%);
  filter: blur(80px);
  bottom: -200px;
  left: -150px;
}
```

### 6.3 Feature Card 毛玻璃化

```css
.feature-card {
  background: var(--glass-bg);
  backdrop-filter: blur(12px);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-card);
  box-shadow: var(--glass-shadow);
  transition: all var(--transition-normal);
}
.feature-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--glass-shadow-hover);
  border-color: var(--glass-border-hover);
}
```

### 6.4 CTA 按钮（Naive UI + 自定义增强）

```html
<n-button type="primary" size="large" class="cta-btn primary-btn btn-shine" @click="...">
  开始简历诊断
</n-button>
```

```css
.primary-btn {
  background: linear-gradient(135deg, var(--orange-main), var(--orange-deep)) !important;
  border: none !important;
  border-radius: var(--radius-btn) !important;
  box-shadow: var(--btn-glow) !important;
  transition: all var(--transition-normal) !important;
}
.primary-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--btn-glow-hover) !important;
}
```

### 6.5 标题发光

```css
.hero-title {
  text-shadow: 0 0 60px rgba(255,140,66,0.15);
}
```

---

## 七、Phase 5 — 核心页面迁移 + 质感增强

### 7.1 DashboardView.vue

**迁移组件**：
- `el-skeleton` → `NSkeleton`
- `el-button` → `NButton`
- `el-tag` → `NTag`

**质感增强**：

| 区域 | 改动 |
|------|------|
| `.identity-card` | 保留橙色渐变，增加 `inset 0 0 40px rgba(255,255,255,0.1)` 内发光 |
| `.quota-card` | `.glass-card` 毛玻璃化 |
| `.stat-card` | `.glass-card` + `.stagger-enter` 级联入场 |
| `.growth-entry-card` | 增加光泽扫过 `.btn-shine` |
| `.record-item` | hover 时左边框变橙 + 微浮起 |

### 7.2 UploadView.vue

**迁移组件**：
- `el-button` → `NButton`
- `el-icon` → `NIcon` 或 SVG
- `el-message` → `useMessage()`（Naive UI）
- `el-message-box` → `useDialog()`（Naive UI）
- `el-upload` → **保留**（API 差异大）

**质感增强**：

| 区域 | 改动 |
|------|------|
| `.upload-card` | `.glass-card` |
| 拖拽区 | 聚焦发光 + hover 橙色虚线变实线 |
| 提交按钮 | `NButton` 渐变 + `.btn-shine` |
| `.info-card` | `.glass-card` |

### 7.3 InterviewEntryView.vue

**迁移组件**：
- `el-select` → `NSelect`
- `el-switch` → `NSwitch`
- `el-input` → `NInput`
- `el-button` → `NButton`
- `el-icon` → `NIcon`
- `el-tag` → `NTag`
- `el-message` → `useMessage()`

**质感增强**：

| 区域 | 改动 |
|------|------|
| `.ready-bar` | 增加微光流动动画 |
| `.config-card` | `.glass-card` |
| `.pill-button` | 毛玻璃 + active 发光 |
| 开始按钮 | `NButton` 渐变 + `.btn-shine` + 尺寸放大 |
| `.job-target-card` | `.glass-card` |

### 7.4 MembershipView.vue

**迁移组件**：
- `el-button` → `NButton`
- `el-skeleton` → `NSkeleton`
- `el-tag` → `NTag`

**质感增强**：

| 区域 | 改动 |
|------|------|
| `.hero-card` | 增加装饰光斑动画 |
| `.plan-card` | `.glass-card` + hover 浮起 -4px |
| `.upgrade-btn` | `NButton` 渐变 + `.btn-shine` |
| 骨架屏 | 毛玻璃底色 |

### 7.5 SettingsView.vue

**迁移组件**：
- `el-icon` → `NIcon`
- `el-tag` → `NTag`
- `el-switch` → `NSwitch`
- `el-select` → `NSelect`
- `el-input` → `NInput`
- `el-slider` → **保留**（Naive UI NSlider API 差异）
- `el-form` → `NForm`
- `el-button` → `NButton`
- `el-dialog` → `NModal`
- `el-tooltip` → `NTooltip`

**质感增强**：

| 区域 | 改动 |
|------|------|
| `.settings-panel` | `.glass-card` |
| `.settings-nav` | 毛玻璃侧边栏 + active 发光 |
| `.preference-row` | hover 微亮背景 |
| `.appearance-option` | `.glass-card` + 选中发光 |
| 所有输入框 | Naive UI 默认样式已优于 Element Plus |

### 7.6 LoginView.vue

**迁移组件**：
- `el-input` → `NInput`
- `el-button` → `NButton`
- `el-form` → `NForm`
- `el-message` → `useMessage()`

**质感增强**：

| 区域 | 改动 |
|------|------|
| 登录卡片 | `.glass-card` + 背景散景 |
| 输入框 | Naive UI 默认 + 聚焦光效 |
| 登录按钮 | `NButton` 渐变 + `.btn-shine` |

### 7.7 其他用户端页面（批量迁移 + 毛玻璃）

| 文件 | 迁移组件 | 毛玻璃 |
|------|---------|--------|
| `HistoryView.vue` | `NTag`, `NButton`, `NSkeleton` | 列表卡片 |
| `InterviewHistoryView.vue` | `NTag`, `NButton`, `NSkeleton` | 列表卡片 |
| `InterviewReportView.vue` | `NTag`, `NButton` | 报告卡片 |
| `GrowthCenterView.vue` | `NButton`, `NCard` | 卡片 |
| `OfferAssistView.vue` | `NButton`, `NInput` | 卡片 |
| `NotificationView.vue` | `NTag`, `NButton`, `NEmpty` | 列表项 |
| `VersionLogView.vue` | `NTag`, `NSkeleton` | 卡片 |
| `TemplateLibraryView.vue` | `NButton`, `NCard` | 模板卡片 |
| `InterviewSessionView.vue` | `NButton`, `NInput` | 聊天气泡 |
| `InterviewEntryView.vue` | `NSelect`, `NSwitch`, `NButton` | 配置卡片 |

---

## 八、Phase 6 — 管理后台保留 Element Plus

管理后台（`views/admin/*`、`AdminLayout.vue`）**不做迁移**，保留 Element Plus：
- 后台功能优先，表格/表单/分页用 Element Plus 更成熟
- 不在用户视野内，视觉优先级低
- 避免不必要的工作量

---

## 九、Phase 7 — 页面过渡动画

**文件**：`frontend/app/src/App.vue` + `MainLayout.vue`

路由过渡已在 Phase 2 的 `MainLayout.vue` 中通过 `<Transition name="page-slide">` 实现。

App.vue 中非 MainLayout 包裹的页面（登录、面试会话、模板编辑器）也添加过渡：

```html
<router-view v-slot="{ Component, route }">
  <Transition name="page-slide" mode="out-in">
    <component :is="Component" :key="route.path" />
  </Transition>
</router-view>
```

---

## 十、实施顺序与文件清单

### 执行顺序

```
Phase 1: 基础设施（main.js + naiveTheme.js + stores/theme.js + index.css）
  ↓
Phase 2: MainLayout.vue（NConfigProvider + 质感层 + 散景 + 路由过渡）
  ↓
Phase 3: AppHeader.vue（组件迁移 + 毛玻璃）
  ↓
Phase 4: HomePageView.vue（Hero 散景 + 按钮重写）
  ↓
Phase 5: 核心页面逐页迁移 + 质感增强
  ↓
Phase 6: 管理后台不动
  ↓
Phase 7: 过渡动画收尾
```

### 完整文件清单

| 优先级 | 文件路径 | 改动类型 |
|--------|---------|---------|
| P0 | `src/config/naiveTheme.js` | **新建** — Naive UI 主题配置 |
| P0 | `src/main.js` | 添加 Naive UI 注册（保留 Element Plus） |
| P0 | `src/stores/theme.js` | 暴露 `naiveTheme` 计算属性 |
| P0 | `src/styles/index.css` | 新增 ~120 行质感全局样式 |
| P0 | `src/layouts/MainLayout.vue` | NConfigProvider + 质感层 + 散景 + 过渡 |
| P1 | `src/components/AppHeader.vue` | EP → Naive UI + 毛玻璃 |
| P1 | `src/views/HomePageView.vue` | EP → Naive UI + Hero 散景 + 按钮 |
| P1 | `src/views/DashboardView.vue` | EP → Naive UI + 毛玻璃 + 级联动画 |
| P1 | `src/views/resume/UploadView.vue` | 部分迁移 + 毛玻璃 |
| P1 | `src/views/interview/InterviewEntryView.vue` | EP → Naive UI + 毛玻璃 |
| P2 | `src/views/MembershipView.vue` | EP → Naive UI + 毛玻璃 |
| P2 | `src/views/settings/SettingsView.vue` | EP → Naive UI + 毛玻璃 |
| P2 | `src/views/auth/LoginView.vue` | EP → Naive UI + 毛玻璃 |
| P2 | `src/views/resume/HistoryView.vue` | EP → Naive UI + 毛玻璃 |
| P2 | `src/views/interview/InterviewHistoryView.vue` | EP → Naive UI + 毛玻璃 |
| P2 | `src/views/interview/InterviewReportView.vue` | EP → Naive UI + 毛玻璃 |
| P2 | `src/views/growth/GrowthCenterView.vue` | EP → Naive UI + 毛玻璃 |
| P2 | `src/views/offer/OfferAssistView.vue` | EP → Naive UI + 毛玻璃 |
| P2 | `src/views/notification/NotificationView.vue` | EP → Naive UI + 毛玻璃 |
| P2 | `src/views/VersionLogView.vue` | EP → Naive UI + 毛玻璃 |
| P2 | `src/views/template/TemplateLibraryView.vue` | EP → Naive UI + 毛玻璃 |
| P2 | `src/views/interview/InterviewSessionView.vue` | 部分迁移 |
| P3 | `src/App.vue` | 路由过渡动画 |
| P3 | `src/components/AppFooter.vue` | 毛玻璃（可选） |
| P3 | `src/layouts/AdminLayout.vue` | **不动** |

### 预估工作量

| 阶段 | 文件数 | 工作量 |
|------|--------|--------|
| Phase 1 基础设施 | 4 | 中（~40min） |
| Phase 2 Layout | 1 | 中（~30min） |
| Phase 3 Header | 1 | 中（~30min） |
| Phase 4 首页 | 1 | 中（~40min） |
| Phase 5 核心页面 | 8 | 大（~2.5h） |
| Phase 5 其他页面 | 8 | 中（~1.5h） |
| Phase 7 收尾 | 1 | 小（~15min） |
| **总计** | **~24** | **~6-7h** |

---

## 十一、注意事项

1. **NConfigProvider 必须包裹整个用户端** — 主题变量通过它注入，放在 `MainLayout.vue` 最外层。
2. **暗色模式联动** — `stores/theme.js` 的 `resolvedTheme` 变化时，`naiveTheme` 计算属性自动切换 `darkTheme`。
3. **Naive UI 的 message/dialog 需要 provider** — `NMessageProvider` 和 `NDialogProvider` 放在 `MainLayout.vue` 中。
4. **Element Plus 不要删除** — 管理后台仍需要，且 `el-upload` 在用户端保留。
5. **`@element-plus/icons-vue` 逐步替换** — 用户端改用 Naive UI 的 `NIcon` + Iconify SVG，管理后台保留。
6. **构建验证** — 每个 Phase 完成后 `npm run build`。
7. **`prefers-reduced-motion`** — 所有动画自动禁用。
8. **性能** — `backdrop-filter` 在低端设备可降级为纯色背景（`@supports` 检测）。
