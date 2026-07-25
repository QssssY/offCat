# 登录 token 有效期延长到 7 天后端任务记录

## 当前任务所属模块

- 后端模块：认证登录、JWT 配置、运行环境配置。
- 触发原因：当前生产环境登录 token 有效期为 1 天，默认配置更短，用户需要频繁重新登录。

## 前端文件定位

- 管理端登录：`frontend/app/src/api/admin/auth.js`
- 管理端会话存储：`frontend/app/src/utils/adminAuth.js`
- 确认结果：管理端登录复用 `/api/auth/login`，仅使用独立 localStorage key 保存管理端 token；前端未设置额外的一天过期逻辑。

## 后端文件定位

- JWT 配置类：`server/src/main/java/com/airesume/server/infrastructure/security/JwtProperties.java`
- 默认配置：`server/src/main/resources/application.yml`
- 开发配置：`server/src/main/resources/application-dev.yml`
- 生产配置：`server/src/main/resources/application-prod.yml`
- 登录服务：`server/src/main/java/com/airesume/server/service/impl/AuthServiceImpl.java`
- 接口文档：`docs/api/TASK_02_AUTH_API.md`

## 本轮修改文件清单

- `JwtProperties` 默认有效期改为 7 天，避免配置缺失时退回 1 天。
- `application.yml`、`application-dev.yml`、`application-prod.yml` 的 `jwt.expiration` 统一改为 `604800000` 毫秒。
- 登录响应文档中的 `expiresIn` 改为 `604800` 秒。
- `RuntimeProtectionConfigTest` 新增三份配置均为 7 天的断言。
- `JwtPropertiesTest` 新增默认有效期为 7 天的断言。
- `AuthServiceImplTest` 登录返回有效期断言同步为 7 天。

## 前端实现方案

- 不修改前端生产代码。
- 管理端登录已复用后端 `/api/auth/login`，管理员 token 与普通用户 token 走同一 JWT 生成配置；后端统一改为 7 天后，管理员登录 token 同步生效。

## 后端实现方案

- 将 JWT 有效期统一收敛到 7 天：`7 * 24 * 60 * 60 * 1000 = 604800000` 毫秒。
- 登录接口仍返回 `expiresIn = jwt.expiration / 1000`，因此响应为 `604800` 秒。
- 不新增 refresh token、不改变签名密钥、不改变鉴权过滤器和管理员权限校验逻辑。

## 数据存储方案

- 不修改数据库结构。
- 不新增表、字段、索引或迁移脚本。

## stage 更新说明

- 已在 `tasks/stage.md` 顶部记录登录 token 有效期延长到 7 天、管理员登录同步生效、验证命令和停止边界。

## 编译结果

- `mvn.cmd -DskipTests compile` 通过。

## 构建结果

- 后端无前端构建产物。

## 当前功能验收说明

- RED 验证：`mvn.cmd test "-Dtest=RuntimeProtectionConfigTest,JwtPropertiesTest"` 在修改前失败，失败点为默认配置仍是 4 小时，生产/dev 和类默认值仍是 1 天。
- GREEN 验证：`mvn.cmd test "-Dtest=RuntimeProtectionConfigTest,JwtPropertiesTest,JwtUtilTest,AuthServiceImplTest"` 通过，53 个用例全绿。
- 编译验证：`mvn.cmd -DskipTests compile` 通过。

## 停止，不继续下一功能

本轮只处理普通用户和管理员登录 JWT 有效期从 1 天延长到 7 天，不继续扩展 refresh token、记住登录、设备管理、强制下线或其它认证能力。
