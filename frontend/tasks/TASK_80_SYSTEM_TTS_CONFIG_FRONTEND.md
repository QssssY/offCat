# 管理端系统级 TTS 配置前端（2026-06-04）

## 当前任务所属模块

管理端 AI 引擎管理、用户设置中心自定义 AI、语音面试会话播报状态。

## 前端文件定位

- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/api/admin/ttsConfig.js`
- `frontend/app/src/api/userAiConfig.js`
- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/views/interview/InterviewSessionView.vue`

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/AdminTtsConfigController.java`
- `server/src/main/java/com/airesume/server/controller/UserAiConfigController.java`
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`

## 本轮修改文件清单

- 新增 `api/admin/ttsConfig.js`，封装系统 TTS 查询、保存、连通测试、发现、试音。
- 修改 `api/userAiConfig.js`，新增 `getSystemTtsStatus`。
- 修改 `AdminAiEngineView.vue`，新增“系统 TTS 配置”Tab 和配置表单。
- 修改 `SettingsView.vue`，在自定义 AI 的 TTS 区块显示系统 TTS 兜底或自定义优先状态。
- 修改 `InterviewSessionView.vue`，根据 capability 的 `engine` 区分“自定义云端 TTS”和“系统云端 TTS”。
- 新增/调整前端测试：`admin.ttsConfig.test.js`、`userAiConfig.test.js`、`AdminAiEngineView.test.js`、`SettingsView.test.js`、`InterviewSessionView.test.js`。

## 前端实现方案

- 在管理页原有分区切换控件中新增第三个 Tab，复用 Element Plus 表单、卡片和现有管理反馈工具。
- 系统 TTS 表单字段为 `enabled/ttsProvider/baseUrl/apiKey/model/voiceId/endpointPath`，提供保存、测试连通性、获取模型/音色、预览音色按钮。
- 服务商预设只做本地回填，不自动发起网络请求；真实发现和试音必须由管理员点击触发。
- 用户设置页通过 `GET /api/user/ai-config/system-tts-status` 读取布尔状态，提示当前使用系统云端语音服务或自定义语音服务优先。
- 面试页沿用后端 `/tts` 合成接口，只调整播报来源文案，不新增播放链路。

## 数据存储方案

- 前端不新增本地持久化。
- 系统 TTS 配置由后端 `sys_tts_config` 表保存；前端不保存 API Key 到本地。

## stage 更新说明

- `frontend/tasks/stage.md` 已追加本轮前端阶段记录。
- 后端阶段记录见根目录 `tasks/stage.md`。

## 构建与测试结果

- RED：`npm.cmd test -- --run src/__tests__/api/admin.ttsConfig.test.js src/__tests__/api/userAiConfig.test.js src/__tests__/views/AdminAiEngineView.test.js src/__tests__/views/SettingsView.test.js src/__tests__/views/InterviewSessionView.test.js` 初次失败，确认缺少前端 API、管理页 Tab、设置页状态提示和面试页系统 TTS 来源文案。
- GREEN：同一目标测试命令通过，5 个测试文件 / 119 个用例。
- 构建：`npm.cmd run build` 通过。

## 当前功能验收说明

- 管理员可在“AI 引擎配置”页切换到“系统 TTS 配置”，维护系统级 TTS 并执行测试、发现和试音。
- 用户设置页可看到系统 TTS 兜底状态；若用户已有自定义 TTS，明确提示自定义优先。
- 语音面试页会显示“自定义云端 TTS”或“系统云端 TTS”，便于判断实际播报来源。

## 停止，不继续下一个功能

本轮仅完成 `develop-project.txt` 功能 2（管理端系统级 TTS 配置）的前端部分，等待验收，不继续推进浏览器音色预设扩展或其它语音能力。
