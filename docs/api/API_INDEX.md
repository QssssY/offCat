# API 接口文档索引

---

## 文档列表

| 文档 | 模块 | 接口数量 | 状态 |
|------|------|---------|------|
| [TASK_02_AUTH_API.md](./TASK_02_AUTH_API.md) | 认证模块 | 3 | 已完成 |
| [TASK_04_RESUME_API.md](./TASK_04_RESUME_API.md) | 简历诊断模块 | 3 | 已完成 |
| [TASK_05_INTERVIEW_API.md](./TASK_05_INTERVIEW_API.md) | 模拟面试模块 | 6 | 已完成 |
| [TASK_06A_ADMIN_API.md](./TASK_06A_ADMIN_API.md) | 管理端模块 | 23 | 已更新（监控总览字段补齐） |

---

## 接口总览

### 认证模块（3）
- `POST /auth/register`
- `POST /auth/login`
- `GET /auth/me`

### 简历诊断模块（3）
- `POST /api/resume/upload`
- `GET /api/resume/task/{taskId}`
- `GET /api/resume/history`

### 模拟面试模块（6）
- `GET /api/interview/job-roles`
- `POST /api/interview/session`
- `POST /api/interview/session/{sessionId}/message`
- `POST /api/interview/session/{sessionId}/end`
- `GET /api/interview/session/{sessionId}`
- `GET /api/interview/history`

### 管理端模块（23）
- `GET /api/admin/job-roles`
- `POST /api/admin/job-roles`
- `PUT /api/admin/job-roles`
- `PUT /api/admin/job-roles/{id}/active`
- `GET /api/admin/ai-engines`
- `POST /api/admin/ai-engines`
- `PUT /api/admin/ai-engines`
- `PUT /api/admin/ai-engines/{id}/active`
- `GET /api/admin/prompts`
- `POST /api/admin/prompts`
- `PUT /api/admin/prompts`
- `PUT /api/admin/prompts/{id}/active`
- `GET /api/admin/users`
- `GET /api/admin/users/{userId}/rights`
- `PUT /api/admin/users/{userId}/rights`
- `PUT /api/admin/users/{userId}/status`
- `GET /api/admin/users/{userId}/quota`
- `PUT /api/admin/users/quota`
- `GET /api/admin/dashboard/overview`
- `GET /api/admin/dashboard/trends`
- `GET /api/admin/dashboard/hot-job-roles`
- `GET /api/admin/dashboard/business-distribution`
- `GET /api/admin/monitor/overview`

---

## 鉴权说明
- 无需鉴权：`POST /auth/register`、`POST /auth/login`
- 其余接口均需 `Authorization: Bearer <token>`
- `/api/admin/**` 需管理员角色
