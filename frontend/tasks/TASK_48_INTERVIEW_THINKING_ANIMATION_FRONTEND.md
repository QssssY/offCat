# TASK 48 模拟面试等待态动效优化前端

## 当前任务所属模块

模拟面试会话页前端体验优化，聚焦用户发送回答后等待面试官回复的 `thinking` 状态展示。

## 前端文件定位

- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/__tests__/views/InterviewSessionView.test.js`

## 后端文件定位

本轮不涉及后端修改，不调整接口、SSE 流式响应、消息状态字段或会话数据结构。

## 本轮修改文件清单

- `InterviewSessionView.vue`：将原本简单的“面试官正在思考你的回答...”等待文案改为文字与三点共用同一连续波动、气泡呼吸边框、浅色扫光和淡入上移的轻量等待态。
- `InterviewSessionView.test.js`：新增静态源码测试，覆盖等待态类名、文字与圆点共用运动单元、关键动画、暗色模式和减少动态效果适配。
- `TASK_48_INTERVIEW_THINKING_ANIMATION_FRONTEND.md`：记录本轮前端优化范围、验证结果和停止边界。
- `frontend/tasks/stage.md`：追加本轮阶段状态。

## 前端实现方案

等待态仍复用现有 assistant 消息气泡和 `item.status === 'thinking'` 条件，不新增组件和依赖。气泡在等待状态下追加 `thinking-bubble` 类，内部保留“面试官正在思考你的回答”文案，并将文字拆为带 `aria-hidden` 的逐字动画元素，同时通过 `aria-label` 保留完整读屏文本；文字和三个圆点统一使用 `thinking-motion-unit` 与同一个 `thinkingUnitWave` keyframe，通过 `--thinking-unit-index` 从左到右错开相位，让波动从文字自然传到圆点，避免文字和圆点各自动画造成割裂。

暗色模式下使用更低亮度的橙色边框、背景光和点颜色，避免高亮扫光刺眼。移动端降低最小宽度并允许文案自然换行，保证气泡不超出视口。系统开启减少动态效果时，通过 `prefers-reduced-motion: reduce` 关闭弹跳、扫光、呼吸和入场动画，仅保留静态等待样式。

## 数据存储方案

不新增本地存储、接口字段、数据库字段或后端状态。

## 构建与测试结果

- `npm.cmd test -- --run src/__tests__/views/InterviewSessionView.test.js` 通过，1 个测试文件、1 个测试用例通过。
- `npm.cmd run build` 通过。

## 当前功能验收说明

- 用户发送回答后，等待后端返回面试官回复期间，等待气泡有稳定、克制的动态反馈。
- 输入锁定、SSE 流式接收、打字机展示、错误态和完成态逻辑保持不变。
- 暗色模式、移动端和减少动态效果偏好均有对应展示处理。

## 停止说明

本轮只优化模拟面试等待态动效，不扩展面试流程、报告、后端接口、AI 生成逻辑或其他会话状态。
