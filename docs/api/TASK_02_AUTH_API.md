# 认证与账号接口

本文档对应后端 `AuthController` 与 `UserAccountController` 的当前实现。

## 通用说明

- 认证前缀：`/api/auth`
- 账号管理前缀：`/api/user/account`
- 鉴权方式：除注册、登录、验证码、密码重置相关接口外，其余接口需 `Authorization: Bearer <token>`
- 密码在服务端使用 BCrypt 存储，响应体不会返回密码、密保答案或 Token 密钥。

## 接口列表

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/api/auth/register` | 否 | 注册普通用户，并初始化用户额度 |
| POST | `/api/auth/login` | 否 | 登录并返回 JWT |
| GET | `/api/auth/me` | 是 | 获取当前用户信息 |
| PUT | `/api/auth/nickname` | 是 | 修改昵称 |
| PUT | `/api/auth/password` | 是 | 修改登录密码 |
| GET | `/api/auth/captcha` | 否 | 获取验证码 |
| GET | `/api/auth/security-question` | 否 | 查询账号密保问题 |
| PUT | `/api/auth/security-question` | 是 | 设置或更新密保问题 |
| POST | `/api/auth/reset-password` | 否 | 通过密保问题重置密码 |
| GET | `/api/user/account/security-question` | 是 | 注销账号前获取当前账号密保问题 |
| POST | `/api/user/account/delete` | 是 | 注销账号 |

## 关键请求示例

### 注册

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "username": "demo_user",
  "password": "DemoPassword123"
}
```

### 登录

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "username": "demo_user",
  "password": "DemoPassword123"
}
```

响应中的 `tokenType` 为 `Bearer`，前端后续请求需要拼接为：

```http
Authorization: Bearer <token>
```

### 当前用户

```http
GET /api/auth/me
Authorization: Bearer <token>
```

当前用户响应包含用户 ID、用户名、昵称、角色、状态、会员到期时间、可用额度等前端展示字段。

### 修改密码

```http
PUT /api/auth/password
Authorization: Bearer <token>
Content-Type: application/json
```

请求体由 `PasswordUpdateRequest` 约束，包含旧密码与新密码。

### 设置密保问题

```http
PUT /api/auth/security-question
Authorization: Bearer <token>
Content-Type: application/json
```

请求体由 `SecurityQuestionUpdateRequest` 约束。密保答案仅用于校验，不会明文返回。

### 重置密码

```http
POST /api/auth/reset-password
Content-Type: application/json
```

请求体由 `ResetPasswordRequest` 约束，需要用户名、密保答案和新密码。

### 注销账号

```http
POST /api/user/account/delete
Authorization: Bearer <token>
Content-Type: application/json
```

请求体由 `AccountDeleteRequest` 约束。服务端会校验当前用户、密码、确认文本与密保信息，防止误删和越权删除。

## 失败场景

| 场景 | 典型结果 |
|------|----------|
| 用户名重复 | 注册失败 |
| 用户名或密码错误 | 登录失败 |
| 账号被封禁 | 登录或访问受限 |
| Token 缺失、过期或非法 | 返回未授权 |
| 密保答案错误 | 密码重置或注销失败 |
| 参数为空、长度不合法 | 参数校验失败 |
