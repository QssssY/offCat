# offcat API 文档索引

本文档按当前后端 `server/src/main/java/com/airesume/server/controller` 中的 Controller 路由整理，作为公开 API 导航。详细请求体、响应体与业务约束以对应模块文档和代码 DTO 为准。

## 通用约定

- 基础地址：本地开发默认 `http://localhost:8080`
- 统一响应：`Result<T>`，典型结构为 `{"code":200,"message":"success","data":{...}}`
- 分页响应：`PageResult<T>` 或包含 `list/total/pageNum/pageSize` 的分页对象
- 鉴权方式：`Authorization: Bearer <token>`
- 管理端权限：`/api/admin/**` 需要管理员角色

## 模块导航

| 文档 | 主要路径 | 当前说明 |
|------|----------|----------|
| [认证接口](./TASK_02_AUTH_API.md) | `/api/auth` | 注册、登录、当前用户、验证码、昵称、密码、密保问题、密码重置 |
| [简历接口](./TASK_04_RESUME_API.md) | `/api/resume` | 文件上传、任务查询、阶段状态、失败重试、JD 匹配、AI 润色、PDF 导出 |
| [面试接口](./TASK_05_INTERVIEW_API.md) | `/api/interview` | 岗位列表、会话、同步/流式消息、TTS、报告、历史清理 |
| [管理端接口](./TASK_06A_ADMIN_API.md) | `/api/admin` | 后台配置、用户权益、看板、监控、会员、通知、版本、审核、反馈、TTS、统计 |

## 当前接口总览

### 公开接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/stats` | 平台公开统计 |
| GET | `/api/version-logs` | 版本日志分页列表 |
| GET | `/api/version-logs/latest` | 最新版本日志 |
| GET | `/api/interview/job-roles` | 启用岗位选项 |
| GET | `/api/diagnostic/network` | 网络诊断 |
| GET | `/api/diagnostic/dns` | DNS 诊断 |
| GET | `/api/diagnostic/proxy` | 代理诊断 |
| GET | `/api/diagnostic/ports` | 常用端口诊断 |
| GET | `/api/diagnostic/http` | HTTP 连通性诊断 |
| GET | `/api/diagnostic/ping` | Ping 诊断 |
| GET | `/api/diagnostic/nslookup` | 域名解析诊断 |
| GET | `/api/diagnostic/port/{port}` | 指定端口诊断 |

### 认证与用户

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/login` | 用户登录 |
| GET | `/api/auth/me` | 当前登录用户 |
| PUT | `/api/auth/nickname` | 修改昵称 |
| PUT | `/api/auth/password` | 修改密码 |
| GET | `/api/auth/captcha` | 获取验证码 |
| GET | `/api/auth/security-question` | 获取密保问题 |
| PUT | `/api/auth/security-question` | 设置或修改密保问题 |
| POST | `/api/auth/reset-password` | 通过密保重置密码 |
| GET | `/api/user/settings` | 获取用户设置 |
| PUT | `/api/user/settings` | 保存用户设置 |
| GET | `/api/user/account/security-question` | 注销流程密保校验信息 |
| POST | `/api/user/account/delete` | 注销账号 |
| POST | `/api/user/feedback` | 提交用户反馈 |
| GET | `/api/user/stats/monthly` | 用户月度统计 |
| GET | `/api/user/quota/consumption-log` | 配额消耗记录 |

### 简历与模板

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/resume/upload` | 上传简历并创建诊断任务，multipart |
| GET | `/api/resume/task/{taskId}` | 任务详情 |
| GET | `/api/resume/task/{taskId}/status` | 任务阶段状态 |
| GET | `/api/resume/history` | 诊断历史 |
| DELETE | `/api/resume/history` | 清空历史 |
| DELETE | `/api/resume/history/{taskId}` | 删除单条历史 |
| POST | `/api/resume/task/{taskId}/retry` | 失败任务重试 |
| POST | `/api/resume/job-match/analyze` | JD 匹配分析 |
| POST | `/api/resume/polish/analyze` | AI 简历润色 |
| PUT | `/api/resume/polish-records/{polishRecordId}/document` | 更新润色文档结构 |
| POST | `/api/resume/export-pdf` | 导出 PDF |
| GET | `/api/resume/download-pdf/{fileId}` | 下载导出的 PDF |
| POST | `/api/template/use` | 使用模板并扣减模板额度 |

### 模拟面试

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/interview/job-roles` | 岗位选项 |
| POST | `/api/interview/session` | 创建面试会话 |
| POST | `/api/interview/session/{sessionId}/message` | 同步发送消息 |
| POST | `/api/interview/session/{sessionId}/message/stream` | SSE 流式发送消息 |
| GET | `/api/interview/session/{sessionId}` | 会话详情 |
| GET | `/api/interview/session/{sessionId}/status` | 会话状态 |
| GET | `/api/interview/session/{sessionId}/tts-capability` | 当前会话 TTS 能力 |
| POST | `/api/interview/session/{sessionId}/tts` | 生成语音 |
| POST | `/api/interview/session/{sessionId}/end` | 结束面试并生成报告 |
| GET | `/api/interview/history` | 面试历史 |
| GET | `/api/interview/history/all` | 全量历史 |
| DELETE | `/api/interview/history` | 清空面试历史 |
| DELETE | `/api/interview/history/{sessionId}` | 删除单条面试历史 |

### 会员、通知、成长与 Offer

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/membership/plans` | 会员套餐 |
| POST | `/api/membership/upgrade/mock` | 开发环境模拟升级 |
| GET | `/api/user/notifications` | 通知列表 |
| GET | `/api/user/notifications/unread-count` | 未读通知数量 |
| POST | `/api/user/notifications/{id}/read` | 标记单条已读 |
| POST | `/api/user/notifications/read-all` | 标记全部已读 |
| DELETE | `/api/user/notifications/{id}` | 删除通知 |
| POST | `/api/user/notifications/batch-delete` | 批量删除通知 |
| GET | `/api/user/notifications/stream` | 通知 SSE |
| DELETE | `/api/user/notifications/stream` | 关闭通知 SSE |
| GET | `/api/user/onboarding/status` | 新手引导状态 |
| PUT | `/api/user/onboarding/status` | 更新新手引导状态 |
| GET | `/api/user/onboarding/tasks` | 新手任务 |
| POST | `/api/user/onboarding/tasks/complete` | 完成新手任务 |
| GET | `/api/user/growth/overview` | 成长中心概览 |
| GET | `/api/user/growth/interview-radar` | 面试雷达 |
| POST | `/api/offer/salary-negotiation/simulate` | 薪资谈判模拟 |
| POST | `/api/offer/salary-negotiation/script` | 薪资谈判话术 |

### 用户自定义 AI 与 TTS

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/ai-config` | 配置列表 |
| POST | `/api/user/ai-config` | 创建或更新配置 |
| DELETE | `/api/user/ai-config/{configType}` | 删除配置 |
| PUT | `/api/user/ai-config/{configType}/toggle` | 启用或禁用配置 |
| POST | `/api/user/ai-config/test-connectivity` | AI 连通性测试 |
| POST | `/api/user/ai-config/models` | 模型发现 |
| POST | `/api/user/ai-config/test-tts-connectivity` | TTS 连通性测试 |
| POST | `/api/user/ai-config/tts-discovery` | TTS 音色发现 |
| POST | `/api/user/ai-config/tts-preview` | TTS 试听 |
| GET | `/api/user/ai-config/usage` | 今日自定义 AI 用量 |
| GET | `/api/user/ai-config/system-tts-status` | 系统 TTS 状态 |

### 社区

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/community/posts` | 帖子列表 |
| GET | `/api/community/posts/{postId}` | 帖子详情 |
| POST | `/api/community/posts` | 创建帖子 |
| DELETE | `/api/community/posts/{postId}` | 删除帖子 |
| PUT | `/api/community/posts/{postId}/admin-hide` | 管理员隐藏帖子 |
| POST | `/api/community/posts/{postId}/like` | 点赞/取消点赞 |
| POST | `/api/community/posts/{postId}/favorite` | 收藏/取消收藏 |
| GET | `/api/community/my/comments` | 我的评论 |
| GET | `/api/community/posts/{postId}/comments` | 评论列表 |
| GET | `/api/community/posts/{postId}/comments/{commentId}/detail` | 评论详情 |
| POST | `/api/community/posts/{postId}/comments` | 创建评论 |
| DELETE | `/api/community/posts/{postId}/comments/{commentId}` | 删除评论 |
| PUT | `/api/community/posts/{postId}/comments/{commentId}/admin-hide` | 管理员隐藏评论 |
| GET | `/api/community/posts/{postId}/comments/{commentId}/replies` | 评论回复 |
| GET | `/api/community/my/interactions` | 收到的互动 |
| GET | `/api/community/my/interactions/unread-count` | 未读互动数 |
| POST | `/api/community/images/upload` | 上传社区图片 |
| GET | `/api/community/images/{objectKey}` | 代理访问社区图片 |

### 管理端

管理端接口较多，详见 [管理端接口文档](./TASK_06A_ADMIN_API.md)。主要覆盖：

- 岗位、Prompt、AI 引擎、TTS 配置
- 用户、权益、封禁、配额、消耗记录
- 看板、趋势、监控、自定义 AI 用量
- 会员套餐与订单
- 通知、版本日志、审计日志
- 社区审核、反馈管理、成长中心配置
