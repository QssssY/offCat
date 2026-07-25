# 加载图标误显示通知铃铛前端修复

## 当前任务所属模块
- 前端通用图标资源模块。
- 受影响入口：成长中心加载态、模拟面试会话加载态，以及同样使用 `FeatureIcon name="loading"` 的通知中心、社区帖子详情、我的动态加载态。

## 前端文件定位
- `frontend/app/src/utils/featureIcons.js`
- `frontend/app/src/components/common/FeatureIcon.vue`
- `frontend/app/src/views/growth/GrowthCenterView.vue`
- `frontend/app/src/views/interview/InterviewSessionView.vue`

## 后端文件定位
- 本轮不涉及后端修改。
- 不修改成长中心、模拟面试、通知或社区接口。

## 本轮修改文件清单
- `frontend/app/src/utils/featureIcons.js`
- `frontend/app/src/__tests__/utils/featureIcons.test.js`
- `frontend/app/src/__tests__/components/common/FeatureIcon.test.js`
- `frontend/tasks/stage.md`
- `frontend/tasks/TASK_LOADING_ICON_NOTIFICATION_FALLBACK_FIX_FRONTEND.md`

## 前端实现方案
- 根因是 `FeatureIcon` 对非关键图标采用异步加载；`loading` 图标没有加入 `criticalFeatureIconNames` 和对应 eager glob 清单，因此组件首次渲染 `FeatureIcon name="loading"` 时只能先从 `getFeatureIconSource()` 取系统通知图标兜底，视觉上表现为加载图标变成铃铛。
- 将 `loading` 加入关键同步图标清单，并同步加入 PNG fallback 与 WebP 的 eager glob；同时从异步 glob 排除列表中排除 `loading`，避免重复进入异步图标集合。
- 修复后 `getFeatureIcon('loading')`、`getFeatureIconSource('loading')` 和 `getCriticalFeatureIconSource('loading')` 都会直接返回 `loading.png/loading.webp`，不会再经过 `system-notifications` 兜底。

## 其他位置排查说明
- 全局排查了 `FeatureIcon name="loading"` 的直接使用点，除用户反馈的成长中心和模拟面试会话加载态外，还包括通知中心、社区帖子详情、我的动态等加载态。
- 本轮在图标资源层统一修复，因此上述所有 `loading` 调用点同步生效；简历诊断过程和面试报告生成中的 `AiLoadingState` 使用的是 `ai-loading`，本轮未改动。

## 后端实现方案
- 本轮不修改后端。

## 数据存储方案
- 本轮不新增本地存储字段。
- 本轮不修改数据库表、迁移脚本或接口 payload。

## stage 更新说明
- 已在 `frontend/tasks/stage.md` 追加“加载图标误显示通知铃铛修复（2026-05-30）”记录。
- stage 中明确本轮范围只包含通用加载图标资源映射，不继续扩展页面视觉重构或新的加载组件。

## 编译结果
- RED 验证：新增 `featureIcons.test.js` 回归测试后，旧代码失败于 `loading` 未包含在同步关键图标清单，且 `getFeatureIconSource('loading')` 返回 `system-notifications.png/webp`。
- GREEN 验证：`npm.cmd test -- --run src/__tests__/utils/featureIcons.test.js src/__tests__/components/common/FeatureIcon.test.js` 通过，2 个测试文件 / 12 个用例通过。
- 相关回归：`npm.cmd test -- --run src/__tests__/views/GrowthCenterView.test.js src/__tests__/views/InterviewSessionView.test.js src/__tests__/views/NotificationView.test.js src/__tests__/views/community/PostDetailView.test.js src/__tests__/views/community/MyActivity.test.js src/__tests__/utils/featureIcons.test.js src/__tests__/components/common/FeatureIcon.test.js` 通过，7 个测试文件 / 51 个用例通过。

## 构建结果
- 前端生产构建：`npm.cmd run build` 通过，构建产物包含 `loading-*.webp` 与 `loading-*.png`。

## 当前功能验收说明
- 成长中心加载态会直接显示 `loading.webp/png`，不再显示通知铃铛。
- 模拟面试会话加载态会直接显示 `loading.webp/png`，不再显示通知铃铛。
- 通知中心、社区帖子详情、我的动态中同样使用 `FeatureIcon name="loading"` 的加载态也同步修复。
- 简历诊断过程和面试报告生成加载态继续使用既有 `ai-loading` 图标，不属于本轮改动范围。

## 停止说明
- 本轮只处理用户反馈的加载图标误显示通知铃铛问题，不继续推进页面 UI 重构、图标资产替换或其它加载态视觉改造。
