# 管理端接口

本文档对应当前 `/api/admin/**` 管理端 Controller。所有接口均需要管理员权限，前端管理端页面位于 `/admin`。

## 通用说明

```http
Authorization: Bearer <admin-token>
```

- 管理员角色：`role=9`
- 长整型 ID 前端需按字符串处理，避免 JavaScript 精度丢失。
- API Key、TTS Key 等敏感字段返回时应脱敏，脱敏值不会被重新写回数据库。

## 岗位配置

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/job-roles` | 岗位列表 |
| POST | `/api/admin/job-roles` | 新增岗位 |
| PUT | `/api/admin/job-roles` | 修改岗位 |
| PUT | `/api/admin/job-roles/{id}/active` | 启用或禁用岗位 |
| DELETE | `/api/admin/job-roles/{id}` | 删除岗位 |
| POST | `/api/admin/job-roles/batch-delete` | 批量删除岗位 |
| PUT | `/api/admin/job-roles/batch/active` | 批量启用或禁用岗位 |

## Prompt 管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/prompts` | Prompt 列表 |
| POST | `/api/admin/prompts` | 新增 Prompt |
| PUT | `/api/admin/prompts` | 修改 Prompt |
| PUT | `/api/admin/prompts/{id}/active` | 启用或禁用 Prompt |
| DELETE | `/api/admin/prompts/{id}` | 删除 Prompt |
| POST | `/api/admin/prompts/batch-delete` | 批量删除 Prompt |
| PUT | `/api/admin/prompts/batch/active` | 批量启用或禁用 Prompt |

规则：同一岗位、同一难度、同一场景下只能保留符合业务要求的启用配置。

## AI 引擎配置

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/ai-engines` | AI 引擎列表 |
| POST | `/api/admin/ai-engines` | 新增 AI 引擎 |
| PUT | `/api/admin/ai-engines` | 修改 AI 引擎 |
| POST | `/api/admin/ai-engines/connectivity-test` | 连通性测试 |
| POST | `/api/admin/ai-engines/models` | 模型发现 |
| PUT | `/api/admin/ai-engines/{id}/active` | 启用或禁用引擎 |
| DELETE | `/api/admin/ai-engines/{id}` | 删除引擎 |
| POST | `/api/admin/ai-engines/batch-delete` | 批量删除引擎 |
| PUT | `/api/admin/ai-engines/batch/active` | 批量启用或禁用引擎 |

## 系统 TTS 配置

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/tts-config` | 获取系统 TTS 配置 |
| PUT | `/api/admin/tts-config` | 保存系统 TTS 配置 |
| POST | `/api/admin/tts-config/test-connectivity` | 测试 TTS 连通性 |
| POST | `/api/admin/tts-config/discover` | 发现 TTS 音色 |
| POST | `/api/admin/tts-config/preview` | TTS 试听 |

## 用户与权益

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/users` | 用户列表 |
| GET | `/api/admin/users/stats` | 用户统计 |
| GET | `/api/admin/users/{userId}/rights` | 用户权益 |
| PUT | `/api/admin/users/{userId}/rights` | 修改用户权益 |
| PUT | `/api/admin/users/{userId}/status` | 修改用户状态 |
| PUT | `/api/admin/users/batch/status` | 批量修改状态 |
| PUT | `/api/admin/users/{userId}/ban` | 封禁用户 |
| PUT | `/api/admin/users/{userId}/unban` | 解封用户 |
| PUT | `/api/admin/users/batch/ban` | 批量封禁 |
| PUT | `/api/admin/users/batch/unban` | 批量解封 |
| GET | `/api/admin/users/{userId}/quota` | 用户配额 |
| PUT | `/api/admin/users/quota` | 调整用户配额 |
| GET | `/api/admin/users/{userId}/consumption-log` | 用户消耗记录 |
| GET | `/api/admin/users/{userId}/interviews` | 用户面试记录 |
| GET | `/api/admin/users/{userId}/resume-tasks` | 用户简历任务 |

## 看板与监控

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/dashboard/overview` | 看板总览 |
| GET | `/api/admin/dashboard/summary` | 看板摘要 |
| GET | `/api/admin/dashboard/trends` | 趋势统计 |
| GET | `/api/admin/dashboard/hot-job-roles` | 热门岗位 |
| GET | `/api/admin/dashboard/business-distribution` | 业务分布 |
| GET | `/api/admin/monitor/overview` | 业务监控总览 |

看板日期参数通常为 `startDate`、`endDate`，具体范围校验以 Controller 和 Service 实现为准。

## 会员与订单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/membership/plans` | 套餐列表 |
| POST | `/api/admin/membership/plans` | 新增套餐 |
| PUT | `/api/admin/membership/plans` | 修改套餐 |
| PUT | `/api/admin/membership/plans/{id}/active` | 启用或禁用套餐 |
| PUT | `/api/admin/membership/plans/batch/active` | 批量启用或禁用套餐 |
| DELETE | `/api/admin/membership/plans/{id}` | 删除套餐 |
| POST | `/api/admin/membership/plans/batch-delete` | 批量删除套餐 |
| GET | `/api/admin/membership/orders` | 订单列表 |

## 通知与版本日志

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/notifications` | 通知列表 |
| GET | `/api/admin/notifications/{id}` | 通知详情 |
| POST | `/api/admin/notifications` | 创建通知 |
| PUT | `/api/admin/notifications/{id}/publish` | 发布或撤回通知 |
| PUT | `/api/admin/notifications/batch/publish` | 批量发布或撤回 |
| DELETE | `/api/admin/notifications/{id}` | 删除通知 |
| POST | `/api/admin/notifications/batch-delete` | 批量删除通知 |
| GET | `/api/admin/version-logs` | 版本日志列表 |
| POST | `/api/admin/version-logs` | 创建版本日志 |
| PUT | `/api/admin/version-logs` | 修改版本日志 |
| PUT | `/api/admin/version-logs/{id}/publish` | 发布或撤回版本日志 |
| PUT | `/api/admin/version-logs/batch/publish` | 批量发布或撤回 |
| DELETE | `/api/admin/version-logs/{id}` | 删除版本日志 |
| POST | `/api/admin/version-logs/batch-delete` | 批量删除版本日志 |

## 审计、成长、社区与反馈

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/audit-logs` | 用户权益变更审计日志 |
| GET | `/api/admin/growth-config` | 成长配置列表 |
| POST | `/api/admin/growth-config` | 新增成长配置 |
| PUT | `/api/admin/growth-config` | 修改成长配置 |
| DELETE | `/api/admin/growth-config/{id}` | 删除成长配置 |
| POST | `/api/admin/growth-config/batch-delete` | 批量删除成长配置 |
| GET | `/api/admin/community/posts` | 帖子审核列表 |
| GET | `/api/admin/community/comments` | 评论审核列表 |
| PUT | `/api/admin/community/posts/{postId}/review` | 审核帖子 |
| PUT | `/api/admin/community/comments/{commentId}/review` | 审核评论 |
| GET | `/api/admin/feedback` | 反馈列表 |
| GET | `/api/admin/feedback/{id}` | 反馈详情 |
| PUT | `/api/admin/feedback/{id}/status` | 更新反馈状态 |
| POST | `/api/admin/feedback/batch-delete` | 批量删除反馈 |

## 自定义 AI 统计与限额

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/custom-ai/usage-stats` | 自定义 AI 用量统计 |
| GET | `/api/admin/custom-ai/usage-trends` | 自定义 AI 用量趋势 |
| GET | `/api/admin/custom-ai/daily-limit` | 查询每日限额 |
| PUT | `/api/admin/custom-ai/daily-limit` | 修改每日限额 |

## 开发调试接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/debug/ai-config` | 查看当前 AI 配置解析状态，仅用于开发排查 |
