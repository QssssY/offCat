# TASK：2026-05-23 登录态过期鉴权边界修复（后端）

## 当前任务所属模块
- 认证与安全配置。

## 后端文件定位
- `server/src/main/java/com/airesume/server/config/SecurityConfig.java`
- `server/src/test/java/com/airesume/server/config/SecurityConfigTest.java`

## 本轮修改文件清单
- `SecurityConfig.java`：将 `/api/auth/**` 匿名放行收紧为只放行注册、登录、找回密码和安全问题查询入口。
- `SecurityConfig.java`：新增 `supportsPublicAuthEndpoint(...)`，用测试锁定公开认证接口白名单。
- `SecurityConfigTest.java`：补充认证接口公开边界测试，覆盖 `/api/auth/me`、改密和修改安全问题不允许匿名访问。

## 后端实现方案
- 保持现有认证 URL 和响应结构不变。
- 公开接口仅保留：
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `POST /api/auth/reset-password`
  - `GET /api/auth/security-question`
- `/api/auth/me`、`PUT /api/auth/password`、`PUT /api/auth/security-question` 等登录态接口必须经过 Spring Security 认证。
- JWT 过期或无效时不再进入 `AuthController.getCurrentUser(...)` 触发空指针，而是由 Security 返回 401。

## 数据存储方案
- 不新增数据库表、字段或迁移脚本。

## 验证记录
- 定向后端测试：`mvn.cmd test "-Dtest=SecurityConfigTest"` 通过，4 个测试通过。
- 后端完整测试：`mvn.cmd test` 通过，512 个测试通过。

## 停止说明
- 本轮只修复登录态过期后 `/api/auth/me` 空认证进入 Controller 的问题，不扩展认证体系、不新增刷新 token 流程。
