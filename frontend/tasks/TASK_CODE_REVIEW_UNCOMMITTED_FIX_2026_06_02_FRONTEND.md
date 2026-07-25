# 未提交改动代码审查问题修复前端

## 当前任务所属模块

- 前端模块：设置中心、语音面试偏好、浏览器本地语音包安装入口。
- 触发原因：本轮代码审查发现设置页本地语音包入口判断使用了不可靠布尔字段，安装接口返回对象时可能误判失败安装为成功。

## 前端文件定位

- `frontend/app/src/views/settings/SettingsView.vue`
- `frontend/app/src/__tests__/views/SettingsView.test.js`

## 后端文件定位

- 后端配套修复见 `tasks/TASK_CODE_REVIEW_UNCOMMITTED_FIX_2026_06_02_BACKEND.md`。

## 本轮修改文件清单

- `SettingsView.vue`：本地语音包入口只在能力状态为 `LOCAL_DOWNLOADABLE` 时展示；安装结果只以 `installResult.ok` 判断是否成功；能力检测失败时显式保持入口关闭。
- `SettingsView.test.js`：补充本地语音包可下载、安装失败、能力检测失败三个状态回归测试。

## 前端实现方案

- 设置页继续调用 `detectSpeechRecognitionCapability()` 获取结构化能力状态。
- `localSpeechInstallAvailable` 只在 `capability.status === SPEECH_RECOGNITION_CAPABILITY_STATUS.LOCAL_DOWNLOADABLE` 时置为 `true`。
- `handleInstallLocalSpeech()` 使用 `Boolean(installResult?.ok)` 判断安装是否成功；只有成功时才隐藏安装入口，失败时保留入口供用户稍后重试。
- 能力检测异常时不展示不可确认的安装入口，并通过中文注释说明该兜底状态，避免空 `catch`。

## 后端实现方案

- 无前端侧后端实现。

## 数据存储方案

- 不新增前端本地存储。
- 不修改后端数据库结构。

## stage 更新说明

- 已在 `frontend/tasks/stage.md` 顶部记录本轮代码审查前端修复、验证命令和停止边界。

## 编译结果

- 前端无单独编译命令，构建结果见下方。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

- 浏览器能力检测为 `LOCAL_DOWNLOADABLE` 时，设置页语音通话分组展示“浏览器本地语音包”和“安装语音包”。
- 安装接口返回 `{ ok: false }` 时不会误判为成功，也不会隐藏安装入口。
- 能力检测失败时保持安装入口关闭，不向用户展示无法确认可用性的本地语音包入口。
- 前端定向测试已通过：`npm.cmd test -- src/__tests__/views/SettingsView.test.js`，40 个用例全绿。

## 停止，不继续下一个功能
