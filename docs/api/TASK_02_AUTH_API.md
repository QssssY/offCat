# TASK_02_AUTH 接口文档

## 1. 用户注册

### 基本信息
- 请求路径：`/api/auth/register`
- 请求方法：`POST`
- 接口说明：注册普通用户账号，注册成功后自动初始化用户额度记录
- 是否鉴权：否

### 请求头
- 无特殊请求头要求

### 请求参数

| 字段名 | 类型 | 是否必填 | 说明 |
|---|---|---|---|
| username | String | 是 | 用户名，长度3-50个字符 |
| password | String | 是 | 密码，长度6-100个字符 |

### 示例请求
```json
{
  "username": "testuser",
  "password": "123456"
}
```

### 返回结果说明
- `code`：业务状态码，200表示成功
- `message`：响应信息
- `data`：无数据返回

### 示例响应
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

---

## 2. 用户登录

### 基本信息
- 请求路径：`/api/auth/login`
- 请求方法：`POST`
- 接口说明：用户登录验证，验证成功后返回 JWT token
- 是否鉴权：否

### 请求头
- 无特殊请求头要求

### 请求参数

| 字段名 | 类型 | 是否必填 | 说明 |
|---|---|---|---|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

### 示例请求
```json
{
  "username": "testuser",
  "password": "123456"
}
```

### 返回结果说明
- `code`：业务状态码，200表示成功
- `message`：响应信息
- `data`：登录成功信息
  - `token`：JWT token 字符串
  - `tokenType`：token 类型，固定为 "Bearer"
  - `expiresIn`：token 有效期，单位秒（默认 604800 秒 = 7 天）

### 示例响应
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 604800
  }
}
```

---

## 3. 获取当前登录用户信息

### 基本信息
- 请求路径：`/api/auth/me`
- 请求方法：`GET`
- 接口说明：获取当前登录用户的详细信息
- 是否鉴权：是

### 请求头

| 名称          | 是否必填 | 说明         |
| ------------- | -------- | ------------ |
| Authorization | 是       | Bearer Token，格式为 "Bearer {token} |

### 请求参数
- 无请求参数

### 示例请求
```
GET /api/auth/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 返回结果说明
- `code`：业务状态码，200表示成功
- `message`：响应信息
- `data`：用户信息对象
  - `id`：用户ID（雪花算法生成的Long类型）
  - `username`：用户名
  - `role`：用户角色（0-普通用户，1-会员用户，9-管理员）
  - `status`：用户状态（1-正常，0-封禁）
  - `vipExpireTime`：会员到期时间（可为null）

### 示例响应
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1234567890123456789,
    "username": "testuser",
    "role": 0,
    "status": 1,
    "vipExpireTime": null
  }
}
```

---

## 失败响应示例

### 用户名已存在
```json
{
  "code": 500,
  "message": "用户名已存在",
  "data": null
}
```

### 用户名或密码错误
```json
{
  "code": 400,
  "message": "用户名或密码错误",
  "data": null
}
```

### 账号已被封禁
```json
{
  "code": 500,
  "message": "账号已被封禁",
  "data": null
}
```

### 未授权（token无效或过期）
```json
{
  "code": 401,
  "message": "未授权",
  "data": null
}
```
