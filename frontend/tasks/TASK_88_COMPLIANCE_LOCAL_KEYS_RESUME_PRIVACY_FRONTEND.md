# 用户 Key 本地化与简历数据脱敏合规改造前端（2026-06-07）

## 回归修复补充（2026-06-07）

- 本轮继续在 `compliance/remove-community-membership` 分支做最小回归修复，不切主分支、不提交 commit、不新增历史数据清理入口。
- 本地 TTS capability 修复：`InterviewSessionView.vue` 在后端 `tts-capability` 返回不可用时，会检查浏览器本地 `buildClientTtsConfig()`；本地 TTS 配置完整则启用云端 TTS 播报，真正合成时仍由 `synthesizeInterviewTts()` 临时携带配置。
- 安全边界：capability GET 请求不携带本地 Key；没有本地 TTS 配置时仍回退浏览器 TTS，不误触发后端云端合成。
- 回归验证：`npm.cmd test -- src/__tests__/views/InterviewSessionView.test.js src/__tests__/api/interview.test.js` 通过，2 个测试文件 / 59 个用例通过；`npm.cmd run build` 通过。
- 安全复查：`rg -n "sk-user-real|sk-local-secret|sk-local|tts-local|138-0000-0000|13800000000|zhangsan@example\\.com|zhangsan\\.dev|张三" frontend/app/dist frontend/app/src --glob "!**/__tests__/**"` 无命中。

## 当前任务所属模块

- 前端模块：设置中心自定义 AI/TTS、简历上传、JD 匹配、AI 润色、模拟面试、面试 TTS、默认简历模板示例数据。
- 后端模块：后端接口接收临时配置和脱敏保存边界单独记录在 `tasks/TASK_88_COMPLIANCE_LOCAL_KEYS_RESUME_PRIVACY_BACKEND.md`。

## 前端文件定位

- `frontend/app/src/utils/localUserAiConfig.js`
- `frontend/app/src/api/userAiConfig.js`
- `frontend/app/src/api/resume.js`
- `frontend/app/src/api/interview.js`
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/views/resume/UploadView.vue`
- `frontend/app/src/data/contents/_default.js`

## 后端文件定位

后端已新增请求内临时配置 DTO、运行时解析和简历脱敏保存逻辑，详见后端任务文档。

## 本轮修改文件清单

- `frontend/app/src/utils/localUserAiConfig.js`
- `frontend/app/src/api/userAiConfig.js`
- `frontend/app/src/api/resume.js`
- `frontend/app/src/api/interview.js`
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/views/resume/UploadView.vue`
- `frontend/app/src/data/contents/_default.js`
- `frontend/app/src/__tests__/api/userAiConfig.test.js`
- `frontend/app/src/__tests__/api/resume.test.js`
- `frontend/app/src/__tests__/api/interview.test.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`

## 前端实现方案

- 本地配置中心：新增 `localUserAiConfig`，按 `default/resume/interview` 管理用户自定义 AI 配置，并支持 TTS 配置、启用状态、删除和回显，数据只写入浏览器 `localStorage`。
- 准确隐私提示：设置中心和简历上传页展示文案“配置仅保存在当前浏览器；发起诊断、面试、测试或试听时会随本次请求临时发送到后端代理调用第三方，服务器不会保存或记录你的 Key。请确保当前设备安全。”
- 用户配置接口兼容：保存、删除、启停操作改为本地读写，不再调用后端持久化接口；连通测试、模型发现、TTS 发现和试听仍按需把本地配置临时传给后端代理。
- 业务请求临时传参：简历上传、JD 匹配、AI 润色、面试创建、面试发消息、流式消息和 TTS 合成在本地启用配置时附带 `clientAiConfig` 或 `clientTtsConfig`，未配置时不附带。
- 静态样例脱敏：默认简历模板中的姓名、电话、邮箱、城市和个人链接改为明显占位值，避免构建产物出现看似真实的完整个人样例。

## 后端实现方案

前端不直接实现后端。后端已接收请求内临时配置，不保存用户 Key，并在简历结果保存和响应前做脱敏处理。

## 数据存储方案

- 用户自定义 AI/TTS 配置只保存在当前浏览器 `localStorage`，不写入服务器。
- 请求中的 Key 只在诊断、面试、测试或试听时临时发送给后端代理，不在前端额外写入其它存储。
- 本轮不设计历史数据清理入口；部署环境为空库，不需要前端触发存量清理。

## stage 更新说明

- 已在 `frontend/tasks/stage.md` 顶部新增“用户 Key 本地化与简历数据脱敏合规改造前端”记录。
- 后端 stage 另在 `tasks/stage.md` 顶部新增对应记录。

## 编译结果

本前端任务不涉及后端编译，后端编译结果记录在后端任务文档。

## 构建结果

- 前端构建：`npm.cmd run build` 通过。

## 测试结果

- 前端目标回归：`npm.cmd test -- src/__tests__/api/userAiConfig.test.js src/__tests__/api/resume.test.js src/__tests__/api/interview.test.js src/__tests__/views/SettingsView.test.js` 通过，4 个测试文件 / 67 个用例通过。
- 关键词复查：`rg -n "sk-user-real|sk-local-secret|sk-local|tts-local|138-0000-0000|13800000000|zhangsan@example\\.com|zhangsan\\.dev|张三" dist src --glob "!src/__tests__/**"` 无命中。

## 当前功能验收说明

- 自定义 AI/TTS 保存、删除、启停只影响当前浏览器本地配置。
- 刷新页面后可从 `localStorage` 回显本地配置。
- 简历与面试请求仅在本地配置启用时附带临时配置。
- 未配置用户自定义 Key 时继续使用平台 AI/TTS。
- 用户能在设置中心和简历上传入口看到 Key 流向与设备安全提示。

## 停止，不继续下一个功能

本轮只完成用户 Key 本地化、业务请求临时传参、隐私提示和静态样例脱敏；不继续推进历史数据清理、不新增本地同步账号、不扩展新的设置页面或其它未指定功能。
