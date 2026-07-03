# 模拟面试接口

本文档对应 `InterviewController` 的当前实现。

## 通用说明

- 前缀：`/api/interview`
- 鉴权：除 `GET /api/interview/job-roles` 外均需 `Authorization: Bearer <token>`
- 面试支持同步消息与 SSE 流式消息两种调用方式。
- 语音面试依赖浏览器语音识别、系统 TTS 或用户自定义 TTS 配置。

## 接口列表

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| GET | `/api/interview/job-roles` | 否 | 查询启用岗位 |
| POST | `/api/interview/session` | 是 | 创建面试会话 |
| POST | `/api/interview/session/{sessionId}/message` | 是 | 同步发送面试消息 |
| POST | `/api/interview/session/{sessionId}/message/stream` | 是 | SSE 流式发送面试消息 |
| GET | `/api/interview/session/{sessionId}` | 是 | 查询会话详情 |
| GET | `/api/interview/session/{sessionId}/status` | 是 | 查询会话状态 |
| GET | `/api/interview/session/{sessionId}/tts-capability` | 是 | 查询 TTS 能力 |
| POST | `/api/interview/session/{sessionId}/tts` | 是 | 生成面试语音 |
| POST | `/api/interview/session/{sessionId}/end` | 是 | 结束面试并生成报告 |
| GET | `/api/interview/history` | 是 | 分页查询面试历史 |
| GET | `/api/interview/history/all` | 是 | 查询全部面试历史 |
| DELETE | `/api/interview/history` | 是 | 清空面试历史 |
| DELETE | `/api/interview/history/{sessionId}` | 是 | 删除单条面试历史 |

## 岗位选项

```http
GET /api/interview/job-roles
```

返回当前启用的岗位配置，前端面试入口使用该接口展示岗位名称、岗位编码和标签。

## 创建面试会话

```http
POST /api/interview/session
Authorization: Bearer <token>
Content-Type: application/json
```

请求体由 `CreateSessionRequest` 约束，常用字段：

| 字段 | 说明 |
|------|------|
| `jobRole` | 岗位名称 |
| `jobRoleCode` | 岗位编码 |
| `difficulty` | 难度：1 初级、2 中级、3 高级 |
| `interviewMode` | `normal`、`stress`、`job_targeted`、`big_company_hr`、`tech_leader`、`foreign_interviewer` |
| `resumeTaskId` | 关联简历诊断任务 |
| `jdText` | 岗位 JD 文本 |
| `useLatestJobMatch` | 是否复用最近一次 JD 匹配 |
| `jobMatchRecordId` | 指定 JD 匹配记录 |
| `feedbackMode` | `after_interview` 或 `immediate` |
| `interactionType` | 0 文字面试，1 语音面试 |

创建会话会校验用户额度，初始化会话，并生成或异步生成开场白。

## 发送消息

### 同步消息

```http
POST /api/interview/session/{sessionId}/message
Authorization: Bearer <token>
Content-Type: application/json
```

请求体由 `SendMessageRequest` 约束，核心字段为 `content`。响应一次性返回 AI 面试官回复。

### SSE 流式消息

```http
POST /api/interview/session/{sessionId}/message/stream
Authorization: Bearer <token>
Content-Type: application/json
Accept: text/event-stream
```

响应为 `text/event-stream`，典型事件数据：

| 类型 | 说明 |
|------|------|
| `{"type":"content","content":"..."}` | AI 回复分片 |
| `{"type":"done"}` | 本轮回复结束 |
| `{"type":"error","message":"..."}` | 本轮回复失败 |

前端应使用 `fetch` 读取流式响应，并在组件卸载、用户取消或重复发送前通过 `AbortController` 中止旧请求。

## 会话查询

```http
GET /api/interview/session/{sessionId}
GET /api/interview/session/{sessionId}/status
```

会话详情包含岗位、难度、面试模式、反馈模式、交互方式、状态、综合评分、评价报告、岗位定向上下文、聊天记录与回放轮次。

状态接口用于轻量轮询，适合处理开场白异步生成与结束状态同步。

## TTS 能力

```http
GET /api/interview/session/{sessionId}/tts-capability
POST /api/interview/session/{sessionId}/tts
```

TTS 能力会综合系统 TTS 配置、用户自定义 TTS 配置和当前会话设置。生成语音接口返回音频结果，前端由 `useVoiceCall` 和 `useCloudTextToSpeech` 播放。

## 结束面试

```http
POST /api/interview/session/{sessionId}/end
Authorization: Bearer <token>
```

结束后服务端会生成综合评分、六维度评分和评价报告。已结束会话不能继续发送面试消息。

## 历史管理

```http
GET /api/interview/history?pageNum=1&pageSize=10
GET /api/interview/history/all
DELETE /api/interview/history
DELETE /api/interview/history/{sessionId}
```

历史数据按当前用户隔离。清理接口用于用户设置中的数据管理能力。

## 业务约束

- 会话创建会消耗面试额度。
- AI 基础设施异常可按配置降级到本地 Mock，业务错误不会静默降级。
- 面试流式消息有独立限流策略。
- 评价报告和维度评分仅在结束面试后稳定可用。
