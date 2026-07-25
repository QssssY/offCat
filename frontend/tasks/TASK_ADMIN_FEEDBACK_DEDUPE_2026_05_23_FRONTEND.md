# TASK：2026-05-23 管理端错误提示去重修复（前端）

## 当前任务所属模块
- 管理端请求封装、管理端统一反馈、设置中心测试稳定性。

## 前端文件定位
- `frontend/app/src/utils/adminRequest.js`
- `frontend/app/src/utils/adminFeedback.js`
- `frontend/app/src/__tests__/utils/adminRequest.test.js`
- `frontend/app/src/__tests__/utils/adminFeedback.test.js`
- `frontend/app/src/__tests__/views/SettingsView.test.js`

## 本轮修改文件清单
- `adminRequest.js`：普通业务错误和非 401 HTTP 错误不再由请求层直接弹错，改为抛出标准错误交给页面 catch 展示。
- `adminRequest.js`：401 会话失效仍在请求层统一清理管理端登录态、跳转登录页并提示一次。
- `adminFeedback.js`：`showAdminError(...)` 增加短时间相同文案去重，防止同一错误链路重复弹窗。
- `adminRequest.test.js`：补充业务错误、非 401 HTTP 错误、401 会话失效提示行为测试。
- `adminFeedback.test.js`：补充相同错误文案短时间去重测试。
- `SettingsView.test.js`：为两个已知慢用例补充单用例超时，避免完整套件并发时误报超时。

## 前端实现方案
- 管理端页面级 catch 继续负责普通业务错误提示，避免“请求拦截器弹一次 + 页面 catch 再弹一次”。
- 管理端 401 属于全局会话状态变化，继续由 `adminRequest` 统一处理，避免各页面重复清理和跳转。
- `showAdminError(...)` 只对短时间内完全相同文案去重，不影响不同错误文案连续展示。

## 数据存储方案
- 不新增前端持久化字段。
- 不修改后端接口或数据库结构。

## 验证记录
- 定向前端测试：`npm.cmd test -- --run src/__tests__/utils/adminFeedback.test.js src/__tests__/utils/adminRequest.test.js` 通过，4 个测试通过。
- 设置页定向测试：`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，25 个测试通过。
- 前端完整测试：`npm.cmd test` 通过，40 个测试文件 / 246 个测试通过。
- 前端构建：`npm.cmd run build` 通过。

## 停止说明
- 本轮只修复管理端重复错误提示和相关测试稳定性，不调整管理端页面布局、接口结构或业务能力。
