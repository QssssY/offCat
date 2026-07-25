# TASK 45 后端：账号注册时间返回与注销校验增强

## 当前任务所属模块
- 用户账号资料
- 设置中心账号注销
- 用户认证信息接口

## 后端文件定位
- `server/src/main/java/com/airesume/server/dto/auth/UserInfoResponse.java`
- `server/src/main/java/com/airesume/server/service/impl/AuthServiceImpl.java`
- `server/src/main/java/com/airesume/server/dto/user/AccountDeleteRequest.java`
- `server/src/main/java/com/airesume/server/controller/UserAccountController.java`
- `server/src/main/java/com/airesume/server/service/UserAccountService.java`
- `server/src/main/java/com/airesume/server/service/impl/UserAccountServiceImpl.java`

## 本轮修改文件清单
- `UserInfoResponse` 新增 `createTime` 返回字段。
- `AuthServiceImpl#getCurrentUserInfo` 回填 `sys_user.create_time`。
- `AccountDeleteRequest` 新增 `confirmPassword` 和 `securityAnswer`。
- `UserAccountController` 新增 `GET /api/user/account/security-question`，并保持 `POST /api/user/account/delete`。
- `UserAccountService` / `UserAccountServiceImpl` 新增当前登录用户安全问题读取，并增强账号注销校验。
- `UserAccountControllerTest`、`UserAccountServiceImplTest`、`AuthServiceImplTest` 更新测试覆盖。

## 后端实现方案
- `/api/auth/me` 返回用户注册时间，前端可直接展示，不新增数据库字段。
- 注销账号接口保持原路径不变，增强请求体：
  - `oldPassword`
  - `confirmPassword`
  - `securityAnswer`
- 服务端注销前依次校验：
  - 当前账号存在且未逻辑删除。
  - 两次当前密码一致。
  - 当前密码正确。
  - 当前账号已设置安全问题与答案。
  - 安全问题答案正确。
- 新增登录态安全问题接口，只返回当前登录账号自己的真实安全问题；不复用忘记密码公共接口，避免破坏公共接口的防枚举策略。

## 数据存储方案
- 不新增表。
- 不新增字段。
- 注册时间复用 `BaseEntity#createTime` 对应的 `sys_user.create_time`。
- 账号注销仍沿用既有逻辑删除、匿名化和关联数据清理策略。

## 当前功能验收说明
- 首页和设置中心可以通过 `/api/auth/me` 获取注册时间。
- 账号注销必须同时通过密码确认和安全问题答案验证。
- 未设置安全问题的账号不能直接注销。
- 公共忘记密码安全问题接口保持原有防枚举语义。

## 验证结果
- `mvn.cmd test` 通过，362 个测试通过。

## 停止说明
- 本轮只增强账号资料返回和账号注销验证，不继续扩展账号恢复、导出数据、管理员代注销或物理删除。
