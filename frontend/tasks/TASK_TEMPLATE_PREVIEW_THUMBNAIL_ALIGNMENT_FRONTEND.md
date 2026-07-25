# 模板库轻量缩略图与真实预览视觉对齐

## 当前任务所属模块
用户端模板库列表缩略图视觉一致性修复，属于路由切换性能优化后的表现层回归修正。

## 前端文件定位
- 缩略图元数据：`frontend/app/src/data/templatePreviewMeta.js`
- 缩略图组件：`frontend/app/src/components/template/TemplatePreviewImage.vue`
- 列表卡片：`frontend/app/src/components/template/TemplateCard.vue`
- 测试文件：`frontend/app/src/__tests__/components/template/TemplateCard.test.js`、`frontend/app/src/__tests__/components/template/TemplatePreviewImage.test.js`

## 后端文件定位
本轮不涉及后端接口、数据库、路由或服务端缓存配置。

## 本轮修改文件清单
- 新增 `frontend/app/src/data/templatePreviewMeta.js`
- 新增 `frontend/app/src/__tests__/components/template/TemplatePreviewImage.test.js`
- 修改 `frontend/app/src/components/template/TemplatePreviewImage.vue`
- 修改 `frontend/app/src/components/template/TemplateCard.vue`
- 修改 `frontend/app/src/__tests__/components/template/TemplateCard.test.js`
- 新增 `frontend/tasks/TASK_TEMPLATE_PREVIEW_THUMBNAIL_ALIGNMENT_FRONTEND.md`
- 修改 `frontend/tasks/stage.md`

## 前端实现方案
- 保留模板库列表的轻量化方案，`TemplateCard` 继续使用 `TemplatePreviewImage`，不恢复 `TemplateRenderer`、`ResizeObserver` 或每卡片 raw CSS 动态导入。
- 新增 `templatePreviewMeta.js`，按模板 id 维护轻量缩略图所需的主色、柔和色、背景、表面色、文字基调、布局、头部风格、section 风格、头像位置和暗色标记。
- `tech-minimal` 缩略图改为读取真实模板 CSS 对应的绿色主色 `#5B7A2E` 和浅绿背景 `#F4F7EE`，不再沿用 `templates.js` 中旧蓝色 `#0EA5E9`。
- `TemplatePreviewImage` 改为按 `layout/headerStyle/sectionStyle/photoPlacement` 输出轻量 DOM 骨架，覆盖顶部横幅、极简头像、深色整页、侧栏、居中权威和柔和卡片等代表版式。
- 灰屏补充修复：移除列表卡片上的 `content-visibility: auto` 和 `contain-intrinsic-size`，改为 `contain: layout paint style`，避免浏览器跳过缩略图内容绘制。
- 灰屏补充修复：`TemplatePreviewImage` 不再依赖 `color-mix()`，由 `templatePreviewMeta.js` 预先生成明确的骨架线条、边框、标签、渐变等 CSS 变量，提升浏览器兼容性。
- 完整预览链路不变，`TemplatePreviewDialog` 仍按需加载完整 `TemplateRenderer` 与 `data/styles/${templateId}.css?raw`。

## 后端实现方案
无后端改动。

## 数据存储方案
不新增数据库表、字段、接口缓存或本地持久化；缩略图元数据是前端静态展示配置。

## stage 更新说明
`frontend/tasks/stage.md` 追加本轮“模板库轻量缩略图与真实预览视觉对齐”记录，说明实现范围、验证命令和停止边界。

## 编译结果
`npm.cmd test -- --run src/__tests__/components/template/TemplateCard.test.js src/__tests__/components/template/TemplatePreviewImage.test.js` 已通过，2 个测试文件 / 13 个用例通过。

## 构建结果
`npm.cmd run build` 已通过。构建过程中仅保留既有 `@vueuse/core` pure annotation 提示，未出现本轮代码相关错误。

## 当前功能验收说明
- 模板库列表仍保持轻量渲染，不会为每张卡片挂载完整简历 HTML。
- 缩略图不再依赖浏览器 `content-visibility` 跳过绘制和 `color-mix()` 运行时混色，避免列表只显示灰色占位。
- “极简技术”缩略图主色和背景已对齐真实预览的绿色系。
- 代表模板已通过静态测试覆盖不同布局、头部和 section 风格，后续新增模板若漏配缩略图元数据会触发测试失败。

## 停止说明
本轮只处理模板库轻量缩略图与真实预览的视觉对齐；不新增截图资产、不引入虚拟列表库、不改路由、后端、业务数据或模板编辑器交互。
