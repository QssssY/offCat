# 模拟面试与社区底框友好度修复

## 当前任务所属模块

用户端模拟面试入口提示条、用户端社区页头横幅的视觉微调。

## 前端文件定位

- `frontend/app/src/views/interview/InterviewEntryView.vue`
- `frontend/app/src/views/community/CommunityView.vue`
- `frontend/app/src/__tests__/views/InterviewEntryView.test.js`
- `frontend/app/src/__tests__/views/community/CommunityView.test.js`

## 后端文件定位

本轮不涉及后端文件。

## 本轮修改文件清单

- `InterviewEntryView.vue`：将模拟面试提示条底框调整为更浅的暖白渐变与轻描边；提示图标改为 `microphone-on`，使用 `md` 尺寸并在 52px 圆形底内居中。
- `CommunityView.vue`：将社区页头横幅底框调整为更浅的暖白渐变与轻描边，并补充暗色主题变量覆盖。
- `InterviewEntryView.test.js`：新增源码级回归断言，锁定浅色提示条变量、图标尺寸和圆形容器尺寸。
- `CommunityView.test.js`：新增源码级回归断言，锁定社区横幅浅色底框变量，避免回退到过深橙色描边。

## 前端实现方案

- 仅调整用户反馈截图中对应的两个视觉区域，不改业务逻辑、接口、路由和数据结构。
- 模拟面试提示条使用局部 CSS 变量 `--interview-ready-bg`、`--interview-ready-border` 管理浅色底框，避免继续直接使用较深的 `--orange-border`。
- 社区横幅使用局部 CSS 变量 `--community-banner-bg`、`--community-banner-border` 管理浅色底框，保持与现有橙色品牌体系一致但降低压迫感。
- 麦克风图标继续复用现有 `FeatureIcon` 资源体系，不新增图片资源或第三方图标库。

## 后端实现方案

无后端改动。

## 数据存储方案

无数据存储改动。

## stage 更新说明

`frontend/tasks/stage.md` 已追加本轮模拟面试与社区底框友好度修复记录。

## 编译结果

- `npm.cmd test -- --run src/__tests__/views/InterviewEntryView.test.js src/__tests__/views/community/CommunityView.test.js` 通过，2 个测试文件 / 36 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

- 模拟面试入口顶部提示条底框应更浅、更友好，麦克风图标应明显变大并居中在圆形底内。
- 社区页头横幅底框应更浅、更柔和，不再呈现过重的橙色边框观感。

## 停止，不继续下一功能

本轮只处理用户指定的模拟面试提示条和社区页头底框视觉问题，不继续扩展社区发帖、面试配置、语音能力或其它页面视觉重构。
