# 设置中心账号注销弹窗倒计时修复

## 当前任务所属模块
用户端设置中心账号安全：修复账号注销确认流程中倒计时启动位置错误的问题。

## 前端文件定位
- 页面文件：`frontend/app/src/views/settings/SettingsView.vue`
- 测试文件：`frontend/app/src/__tests__/views/SettingsView.test.js`

## 后端文件定位
本轮不涉及后端，不修改账号注销 API、鉴权、数据库、路由或数据清理逻辑。

## 本轮修改文件清单
- `SettingsView.vue`
- `SettingsView.test.js`
- `frontend/tasks/TASK_SETTINGS_ACCOUNT_DELETE_COUNTDOWN_FRONTEND.md`
- `frontend/tasks/stage.md`

## 前端实现方案
- 将 `accountDeleteCountdown` 初始值从 15 改为 0，避免用户刚进入“注销账号”表单时就被冷却状态拦住。
- 表单区“确认注销”按钮只受安全问题加载状态和安全问题错误状态影响，不再受弹窗倒计时影响。
- 表单校验通过后直接打开二次确认弹窗；倒计时只在弹窗 `open` 时启动。
- 关闭弹窗或切换安全操作时清理定时器，并将倒计时重置为 0。
- 保留弹窗内最终确认按钮的保护逻辑：确认文本不匹配或倒计时未结束时仍不可提交注销请求。

## 数据存储方案
本轮不涉及数据存储，不新增表、字段或本地持久化逻辑。

## stage 更新说明
已在 `frontend/tasks/stage.md` 追加“设置中心账号注销弹窗倒计时修复”记录，包含问题原因、修复范围、验证结果和停止边界。

## 验证结果
- RED：先更新 `SettingsView.test.js`，要求注销表单阶段不被倒计时禁用，并要求倒计时只在确认弹窗内启动；运行 `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 失败于按钮仍禁用、倒计时初始仍为 15、表单无法打开弹窗。
- GREEN：修复后 `npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，1 个测试文件 / 27 个用例通过。
- 构建：`npm.cmd run build` 通过。

## 当前功能验收说明
验收时进入设置中心 → 账号安全 → 注销账号：未输入密码前不应显示“等待 N 秒”；填完密码和安全答案并点击确认注销后，弹窗打开并从 15 秒开始倒计时；倒计时结束且确认文本正确后才允许最终注销。

## 停止说明
本轮只修复账号注销前端倒计时交互，不继续修改注销接口、账号清理逻辑、其它设置页功能或后端代码。
