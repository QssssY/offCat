# 用户端全量图标替换

## 当前任务所属模块
用户端视觉一致性优化：保留旧图标资源与旧 key，补齐 `new/` 图标映射，并在用户端页面与组件中统一使用 `FeatureIcon`。

## 前端文件定位
- 图标资源：`frontend/app/src/assets/feature-icons/old/`、`frontend/app/src/assets/feature-icons/new/`
- 图标映射：`frontend/app/src/utils/featureIcons.js`
- 图标入口组件：`frontend/app/src/components/common/FeatureIcon.vue`
- 相关测试：`frontend/app/src/__tests__/utils/featureIcons.test.js`

## 本轮修改范围
- 扩展 `featureIcons.js`，保留 `old/` 22 个基础业务 key，并映射当前 `new/` 目录全部 PNG，包括业务图标、通知图标、面试控制图标和 `back/close/delete/save/search/share/comment/loading/success/warning/error/microphone-on/microphone-off/fullscreen/exit-fullscreen/upload-file/image-upload` 等操作类图标。
- 用户端公共区域已替换为 `FeatureIcon`：顶部导航、历史下拉、通知下拉、移动端菜单、主题切换、用户菜单、通知中心返回/删除/空状态。
- 用户端主流程页面已覆盖：主页、Dashboard、简历上传/结果/历史、面试入口/会话/历史/报告、模板库/编辑器、社区列表/详情/我的动态、成长中心、Offer 辅助、会员页、设置页、版本日志页。
- 用户端组件已覆盖：空状态组件、Onboarding 引导与任务卡、AI 加载态业务标识、社区编辑/评论/卡片、社区图片预览、简历结果 section 中有明确业务语义的状态图标。
- 管理端 `/admin/**` 不在本轮范围内，未做替换。

## 保留策略
- 不删除 `old/` 图标、不重命名旧 key、不改变未知 key 兜底逻辑。
- 数据可视化 SVG、进度环、雷达图、AI loading 轨道等结构性图形不替换。
- 没有明确 `new/` 资源对应的时间、字段类细粒度系统图标保留现状，避免错误语义替换。
- 本轮不修改路由、API、数据结构或业务行为。

## 验证结果
- `npm.cmd test -- --run src/__tests__/utils/featureIcons.test.js src/__tests__/components/OnboardingTaskCard.test.js src/__tests__/views/SettingsView.test.js src/__tests__/utils/notificationMeta.test.js` 通过：4 个测试文件，32 个用例通过。
- `npm.cmd run build` 通过，Vite 生产构建成功，所有已映射的新旧图标资源均可解析并进入产物。

## 停止说明
本轮只完成用户端图标映射与替换，不继续扩展管理端图标、不新增业务能力、不调整接口或页面流程。
