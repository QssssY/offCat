# TASK_05_INTERVIEW 接口文档

## 0. 查询岗位选项

### 基本信息
- 请求路径：`/api/interview/job-roles`
- 请求方法：`GET`
- 接口说明：查询当前启用的模拟面试岗位选项
- 是否鉴权：否

### 查询参数

| 字段名   | 类型    | 是否必填 | 说明 |
|----------|---------|----------|------|
| pageNum  | integer | 否       | 页码，默认 `1` |
| pageSize | integer | 否       | 每页条数，默认 `5` |

### 示例请求
```http
GET /api/interview/job-roles
```

### 返回结果说明
- `code`：业务状态码
- `message`：响应信息
- `data`：岗位选项数组

data 数组元素说明：

| 字段名       | 类型   | 说明 |
|--------------|--------|------|
| roleCode     | string | 岗位编码 |
| roleName     | string | 岗位名称 |
| interviewTag | string | 面试入口展示标签 |
| tagType      | string | 标签样式类型 |

### 示例响应
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "roleCode": "frontend_engineer",
      "roleName": "前端开发工程师",
      "interviewTag": "热门",
      "tagType": "hot"
    },
    {
      "roleCode": "product_manager",
      "roleName": "产品经理",
      "interviewTag": "常见",
      "tagType": "common"
    }
  ]
}
```

---

## 1. 创建面试会话

### 基本信息
- 请求路径：`/api/interview/session`
- 请求方法：`POST`
- 接口说明：创建新的模拟面试会话
- 是否鉴权：是

### 请求头

| 名称          | 是否必填 | 说明         |
| ------------- | -------- | ------------ |
| Authorization | 是       | Bearer Token |

### 请求参数

| 字段名            | 类型    | 是否必填 | 说明 |
|-------------------|---------|----------|------|
| jobRole           | string  | 是       | 面试岗位，必须来自 `/api/interview/job-roles` 返回的启用岗位；长度不超过 100 |
| jobRoleCode       | string  | 否       | 岗位编码，长度不超过 100 |
| difficulty        | integer | 是       | 难度级别：1-初级，2-中级，3-高级（取值范围 1-3） |
| interviewMode     | string  | 否       | 面试模式白名单：`normal` / `stress` / `job_targeted` / `big_company_hr` / `tech_leader` / `foreign_interviewer`；未传或非白名单值会被服务端拒绝或回退 |
| jobTargeted       | boolean | 否       | 是否开启岗位定向模拟 |
| resumeTaskId      | string  | 否       | 关联的简历诊断任务 ID，普通模拟面试和岗位定向模拟都可传 |
| jdText            | string  | 否       | 手动输入的岗位 JD 文本，长度不超过 8000 |
| useLatestJobMatch | boolean | 否       | 是否优先复用最近一次 JD 对比结果 |
| jobMatchRecordId  | string  | 否       | 指定复用的 JD 对比记录 ID |
| feedbackMode      | string  | 否       | 反馈模式：`immediate` / `after_interview`，默认 `after_interview` |
| interactionType   | integer | 否       | 交互方式：0-文字面试（默认）/ 1-语音面试；非 0/1 会被拒绝 |

### 示例请求
```json
{
  "jobRole": "Java开发工程师",
  "jobRoleCode": "java_engineer",
  "difficulty": 2,
  "interviewMode": "normal",
  "jobTargeted": true,
  "resumeTaskId": "1892233001001",
  "jdText": "负责接口测试、自动化测试和缺陷定位",
  "useLatestJobMatch": true
}
```

### 返回结果说明
- `code`：业务状态码
- `message`：响应信息
- `data`：会话详情对象
- `timestamp`：响应时间

data 字段说明：

| 字段名            | 类型    | 说明 |
|-------------------|---------|------|
| sessionId         | string  | 会话 ID |
| jobRole           | string  | 面试岗位 |
| jobRoleCode       | string  | 岗位编码 |
| difficulty        | integer | 难度 |
| difficultyDesc    | string  | 难度描述 |
| interviewMode     | string  | 面试模式：`normal` / `stress` / `job_targeted` / `big_company_hr` / `tech_leader` / `foreign_interviewer` |
| interviewModeDesc | string  | 面试模式描述 |
| jobTargeted       | boolean | 是否为岗位定向模拟 |
| jobTargetContext  | object  | 岗位定向上下文；普通模拟面试时可能为空 |
| feedbackMode      | string  | 反馈模式：`immediate` / `after_interview` |
| interactionType   | integer | 交互方式：0-文字面试 / 1-语音面试 |
| openingPending    | boolean | 开场白是否还在异步生成中；前端据此决定是否轮询 `GET /api/interview/session/{sessionId}` 直到为 false |
| status            | integer | 会话状态：0-进行中，1-已结束 |
| statusDesc        | string  | 状态描述 |

### 示例响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sessionId": "abc123def456",
    "jobRole": "Java开发工程师",
    "jobRoleCode": "java_engineer",
    "difficulty": 2,
    "difficultyDesc": "中级",
    "interviewMode": "job_targeted",
    "interviewModeDesc": "岗位定向模拟",
    "jobTargeted": true,
    "jobTargetContext": {
      "jobTargeted": true,
      "sourceType": "manual_jd_with_job_match",
      "resumeTaskId": "1892233001001",
      "jdText": "负责接口测试、自动化测试和缺陷定位",
      "jobMatchRecordId": "1892233002001",
      "matchedKeywords": [
        "接口联调"
      ],
      "missingKeywords": [
        "自动化测试"
      ],
      "suggestions": [
        "把项目经历映射到质量保障视角"
      ]
    },
    "status": 0,
    "statusDesc": "进行中"
  },
  "timestamp": 1712345678901
}
```

---

## 2. 发送面试消息

### 基本信息
- 请求路径：`/api/interview/session/{sessionId}/message`
- 请求方法：`POST`
- 接口说明：发送用户消息并获取面试官回复
- 是否鉴权：是

### 请求头

| 名称          | 是否必填 | 说明         |
| ------------- | -------- | ------------ |
| Authorization | 是       | Bearer Token |

### 路径参数

| 字段名    | 类型   | 是否必填 | 说明   |
|-----------|--------|----------|--------|
| sessionId | string | 是       | 会话 ID |

### 请求参数

| 字段名       | 类型   | 是否必填 | 说明 |
|--------------|--------|----------|------|
| content      | string | 是       | 用户消息内容，长度 1-5000 |
| feedbackMode | string | 否       | 反馈模式：`immediate` / `after_interview`；仅影响本次 AI 回复口径，不会落库 |

### 示例请求
```json
{
  "content": "你好，我叫张三，有3年Java开发经验。"
}
```

### 返回结果说明
- `code`：业务状态码
- `message`：响应信息
- `data`：发送消息响应对象
- `timestamp`：响应时间

data 字段说明：

| 字段名       | 类型   | 说明             |
|--------------|--------|------------------|
| sessionId    | string | 会话ID           |
| replyContent | string | 面试官回复内容   |

### 示例响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sessionId": "abc123def456",
    "replyContent": "感谢你的介绍！接下来我们来聊聊技术问题..."
  },
  "timestamp": 1712345678901
}
```

---

## 2.1 发送面试消息（流式 SSE）

### 基本信息
- 请求路径：`/api/interview/session/{sessionId}/message/stream`
- 请求方法：`POST`
- 接口说明：发送用户消息并以 Server-Sent Events 流式返回面试官回复
- 是否鉴权：是
- 限流：每用户 60 次 / 10 分钟（独立 `interview_stream` 策略），优先级高于通用面试限频；超限返回 429，前端提示用户稍后继续

### 请求头

| 名称          | 是否必填 | 说明         |
| ------------- | -------- | ------------ |
| Authorization | 是       | Bearer Token |
| Content-Type  | 是       | `application/json` |

### 路径参数

| 字段名    | 类型   | 是否必填 | 说明   |
|-----------|--------|----------|--------|
| sessionId | string | 是       | 会话 ID |

### 请求参数

| 字段名       | 类型   | 是否必填 | 说明 |
|--------------|--------|----------|------|
| content      | string | 是       | 用户消息内容，长度 1-5000 |
| feedbackMode | string | 否       | 反馈模式：`immediate` / `after_interview` |

### 响应格式（SSE）

响应体为 `text/event-stream`，按以下事件帧顺序发送（每帧之间以空行分隔）：

| 事件类型 | data 形状 | 说明 |
|----------|-----------|------|
| `event: message` 携带 `data: {"type":"content","content":"<chunk>"}` | JSON | AI 回复增量分片，可能连续多条 |
| `event: message` 携带 `data: {"type":"done"}` | JSON | 当前回复全部下发完成，前端可解锁输入 |
| `event: message` 携带 `data: {"type":"error","message":"AI 服务暂时不可用，请稍后重试"}` | JSON | 上游 AI 异常或落库失败，详细原因仅打在服务端日志，message 为通用文案不泄露内部信息 |
| `event: error` 携带 `data: 系统异常，请稍后重试` | string | 异步任务在订阅前抛错时的兜底事件 |

### 调用约束（前端）

- 必须使用裸 `fetch`（Axios 不支持流式响应）。
- 必须维护 `AbortController` 并在以下场景调用 `abort()`：
  - 组件卸载（`onBeforeUnmount`）
  - 同一会话连发下一条消息前（先中止上一条仍在读取的流）
  - 用户主动取消
- 收到 HTTP `401` 时跳转登录页：项目自带 Axios 拦截器不会覆盖这里的裸 fetch，必须手动处理。
- 收到 `event: message` 且 `type === "error"` 时按业务错误展示（不要直接 throw），收到 `type === "done"` 才视为成功完成。

### 基本信息
- 请求路径：`/api/interview/session/{sessionId}/end`
- 请求方法：`POST`
- 接口说明：结束当前面试并生成综合评分与评价报告
- 是否鉴权：是

### 请求头

| 名称          | 是否必填 | 说明         |
| ------------- | -------- | ------------ |
| Authorization | 是       | Bearer Token |

### 路径参数

| 字段名    | 类型   | 是否必填 | 说明     |
|-----------|--------|----------|----------|
| sessionId | string | 是       | 会话ID   |

### 请求参数
- 无请求参数

### 示例请求
```
POST /api/interview/session/abc123def456/end
```

### 返回结果说明
- `code`：业务状态码
- `message`：响应信息
- `data`：null
- `timestamp`：响应时间

### 示例响应
```json
{
  "code": 200,
  "message": "面试已结束",
  "data": null,
  "timestamp": 1712345678901
}
```

---

## 4. 查询会话详情

### 基本信息
- 请求路径：`/api/interview/session/{sessionId}`
- 请求方法：`GET`
- 接口说明：查询面试会话详情，包含完整聊天记录
- 是否鉴权：是

### 请求头

| 名称          | 是否必填 | 说明         |
| ------------- | -------- | ------------ |
| Authorization | 是       | Bearer Token |

### 路径参数

| 字段名    | 类型   | 是否必填 | 说明     |
|-----------|--------|----------|----------|
| sessionId | string | 是       | 会话ID   |

### 请求参数
- 无请求参数

### 示例请求
```
GET /api/interview/session/abc123def456
```

### 返回结果说明
- `code`：业务状态码
- `message`：响应信息
- `data`：会话详情对象
- `timestamp`：响应时间

data 字段说明：

| 字段名           | 类型       | 说明                     |
|------------------|------------|--------------------------|
| id               | Long       | 主键ID                   |
| sessionId        | string     | 会话ID                   |
| jobRole          | string     | 面试岗位                 |
| jobRoleCode      | string     | 岗位编码                 |
| difficulty       | integer    | 难度级别                 |
| difficultyDesc   | string     | 难度描述                 |
| interviewMode    | string     | 面试模式：`normal` / `stress` / `job_targeted` / `big_company_hr` / `tech_leader` / `foreign_interviewer` |
| interviewModeDesc| string     | 面试模式描述             |
| status           | integer    | 会话状态：0-进行中，1-已结束 |
| statusDesc       | string     | 状态描述                 |
| comprehensiveScore | integer  | AI综合打分（已结束时有值） |
| evaluationReport | string     | 评价报告JSON（已结束时有值） |
| jobTargeted      | boolean    | 是否为岗位定向模拟       |
| jobTargetContext | object     | 岗位定向上下文与反馈     |
| feedbackMode     | string     | 反馈模式：`immediate` / `after_interview` |
| interactionType  | integer    | 交互方式：0-文字面试 / 1-语音面试 |
| openingPending   | boolean    | 开场白是否还在异步生成中 |
| chatLogs         | array      | 聊天记录列表             |
| replayRounds     | array      | 回放轮次（按"问题-回答-反馈"组织） |
| createTime       | DateTime   | 创建时间                 |
| updateTime       | DateTime   | 更新时间                 |

> 注：响应中不再返回 `userId` 字段。当前登录用户身份由 JWT Bearer Token 标识，无需在响应体里重复下发。

chatLogs 数组元素说明：

| 字段名       | 类型     | 说明                     |
|--------------|----------|--------------------------|
| id           | Long     | 消息ID                   |
| messageRole  | string   | 角色：user/assistant/system |
| content      | string   | 消息内容                 |
| createTime   | DateTime | 创建时间                 |

### 示例响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1789012345678901001,
    "sessionId": "abc123def456",
    "userId": 1789012345678901234,
    "jobRole": "Java开发工程师",
    "jobRoleCode": "java_engineer",
    "difficulty": 2,
    "difficultyDesc": "中级",
    "interviewMode": "job_targeted",
    "interviewModeDesc": "岗位定向模拟",
    "status": 1,
    "statusDesc": "已结束",
    "comprehensiveScore": 78,
    "jobTargeted": true,
    "jobTargetContext": {
      "jobTargeted": true,
      "sourceType": "manual_jd",
      "resumeTaskId": "1892233001001",
      "jdText": "负责接口测试、自动化测试和缺陷定位",
      "matchedKeywords": [
        "接口联调"
      ],
      "missingKeywords": [
        "自动化测试"
      ],
      "suggestions": [
        "把项目经历映射到质量保障视角"
      ]
    },
    "evaluationReport": "{\"overallScore\":78,\"level\":\"B - 中等\"}",
    "chatLogs": [
      {
        "id": 1789012345678901101,
        "messageRole": "assistant",
        "content": "你好！欢迎参加本次模拟面试...",
        "createTime": "2024-04-01T10:00:00"
      },
      {
        "id": 1789012345678901102,
        "messageRole": "user",
        "content": "你好，我叫张三...",
        "createTime": "2024-04-01T10:00:05"
      }
    ],
    "createTime": "2024-04-01T10:00:00",
    "updateTime": "2024-04-01T10:30:00"
  },
  "timestamp": 1712345678901
}
```

---

## 5. 查询面试历史

### 基本信息
- 请求路径：`/api/interview/history`
- 请求方法：`GET`
- 接口说明：查询当前用户的面试历史记录列表
- 是否鉴权：是

### 请求头

| 名称          | 是否必填 | 说明         |
| ------------- | -------- | ------------ |
| Authorization | 是       | Bearer Token |

### 请求参数
- 无请求参数

### 示例请求
```
GET /api/interview/history?pageNum=1&pageSize=5
```

### 返回结果说明
- `code`：业务状态码
- `message`：响应信息
- `data`：分页结果对象
- `timestamp`：响应时间

data 字段说明：

| 字段名   | 类型    | 说明 |
|----------|---------|------|
| list     | array   | 历史记录列表 |
| total    | integer | 总记录数 |
| pageNum  | integer | 当前页码 |
| pageSize | integer | 每页条数 |

list 数组元素说明：

| 字段名           | 类型     | 说明                     |
|------------------|----------|--------------------------|
| id               | Long     | 主键ID                   |
| sessionId        | string   | 会话ID                   |
| jobRole          | string   | 面试岗位                 |
| difficulty       | integer  | 难度级别                 |
| difficultyDesc   | string   | 难度描述                 |
| interviewMode    | string   | 面试模式：`normal` / `stress` / `job_targeted` / `big_company_hr` / `tech_leader` / `foreign_interviewer` |
| interviewModeDesc| string   | 面试模式描述             |
| status           | integer  | 会话状态：0-进行中，1-已结束 |
| statusDesc       | string   | 状态描述                 |
| comprehensiveScore | integer | AI综合打分（已结束时有值） |
| messageCount     | integer  | 消息数量                 |
| jobTargeted      | boolean  | 是否为岗位定向模拟       |
| feedbackMode     | string   | 反馈模式：`immediate` / `after_interview` |
| interactionType  | integer  | 交互方式：0-文字面试 / 1-语音面试 |
| sourceType       | string   | 岗位定向来源类型         |
| createTime       | DateTime | 创建时间                 |
| updateTime       | DateTime | 更新时间                 |

### 示例响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1789012345678901001,
        "sessionId": "abc123def456",
        "jobRole": "Java开发工程师",
        "difficulty": 2,
        "difficultyDesc": "中级",
        "interviewMode": "job_targeted",
        "interviewModeDesc": "岗位定向模拟",
        "status": 1,
        "statusDesc": "已结束",
        "comprehensiveScore": 78,
        "messageCount": 12,
        "jobTargeted": true,
        "sourceType": "manual_jd",
        "createTime": "2024-04-01T10:00:00",
        "updateTime": "2024-04-01T10:30:00"
      },
      {
        "id": 1789012345678901002,
        "sessionId": "xyz789abc012",
        "jobRole": "前端开发工程师",
        "difficulty": 1,
        "difficultyDesc": "初级",
        "interviewMode": "normal",
        "interviewModeDesc": "普通面试",
        "status": 0,
        "statusDesc": "进行中",
        "comprehensiveScore": null,
        "messageCount": 3,
        "jobTargeted": false,
        "sourceType": null,
        "createTime": "2024-04-02T14:00:00",
        "updateTime": "2024-04-02T14:05:00"
      }
    ],
    "total": 2,
    "pageNum": 1,
    "pageSize": 5
  },
  "timestamp": 1712345678901
}
```
