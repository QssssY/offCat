# 登录过期重复弹窗修复（前端）

## 当前任务所属模块
- 用户端统一请求与登录态失效处理。

## 前端文件定位
- `frontend/app/src/utils/request.js`
- `frontend/app/src/__tests__/utils/request.test.js`

## 后端文件定位
- 本轮不涉及后端接口、DTO、数据库或鉴权过滤器变更。

## 本轮修改文件清单
- `request.js`：为用户端统一请求封装增加登录过期处理去重，HTTP 401 与业务码 401 共用同一处理入口。
- `request.test.js`：新增并发 HTTP 401 和业务码 401 回归测试，确认只弹一次“登录已过期，请重新登录”。
- `frontend/tasks/TASK_AUTH_EXPIRED_PROMPT_DEDUPE_FRONTEND.md`
- `frontend/tasks/stage.md`

## 前端实现方案
- 在用户端 Axios 响应拦截器内增加 `handleUnauthorized`，集中处理清理 token、弹出登录过期提示和跳转登录页。
- 使用当前失效 token 作为去重键：同一个 token 对应的多个并发 401 只处理一次；用户重新登录后拿到新 token，后续再次过期仍可正常提示。
- 后端返回 HTTP 401 和返回 JSON 业务码 `code === 401` 时，都走同一处理入口，避免不同错误形态重复弹窗。
- 保留其它状态码的原有提示逻辑，避免扩大修复范围。

## 数据存储方案
- 不新增本地持久化字段。
- 不修改 token 存储键名、接口响应结构或数据库结构。

## stage 更新说明
- 已在 `frontend/tasks/stage.md` 追加本轮登录过期重复弹窗修复状态，包含问题原因、修复范围、验证结果和停止边界。

## 验证结果
- RED：先新增 `request.test.js`，运行 `npm.cmd test -- --run src/__tests__/utils/request.test.js` 失败，证明三个并发 HTTP 401 会触发 3 次登录过期提示。
- GREEN：修复后运行 `npm.cmd test -- --run src/__tests__/utils/request.test.js` 通过，1 个测试文件 / 2 个用例通过。
- 相关回归：`npm.cmd test -- --run src/__tests__/utils/request.test.js src/__tests__/utils/adminRequest.test.js src/__tests__/api/resumePdf.test.js` 通过，3 个测试文件 / 6 个用例通过。
- 构建：`npm.cmd run build` 通过。

## 当前功能验收说明
- 登录过期后，如果同一时间有多个接口返回 401，用户端只显示一次“登录已过期，请重新登录”。
- 跳转登录页只触发一次，redirect 保留当前页面地址。
- 业务码 401 与 HTTP 401 行为一致，不再因为不同响应形态重复提示。

## 停止说明
- 本轮只修复用户端登录过期重复弹窗问题，不继续修改登录页、后端 token 签发、管理端鉴权、其它业务提示或权限体系。
