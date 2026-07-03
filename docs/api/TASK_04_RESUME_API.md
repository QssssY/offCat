# TASK_04_RESUME 接口文档

## 1. 上传简历并创建诊断任务

### 基本信息
- 请求路径：`/api/resume/upload`
- 请求方法：`POST`
- 接口说明：上传PDF简历并创建诊断任务，任务会异步处理
- 是否鉴权：是

### 请求头

| 名称          | 是否必填 | 说明         |
| ------------- | -------- | ------------ |
| Authorization | 是       | Bearer Token |

### 请求参数

| 字段名  | 类型   | 是否必填 | 说明               |
|---------|--------|----------|--------------------|
| fileUrl | string | 是       | PDF简历存储地址    |

### 示例请求
```json
{
  "fileUrl": "https://example.com/resumes/user-resume.pdf"
}
```

### 返回结果说明
- `code`：业务状态码
- `message`：响应信息
- `data`：任务ID（Long类型）
- `timestamp`：响应时间

### 示例响应
```json
{
  "code": 200,
  "message": "简历诊断任务已提交",
  "data": 1789012345678901234,
  "timestamp": 1712345678901
}
```

---

## 2. 查询任务详情

### 基本信息
- 请求路径：`/api/resume/task/{taskId}`
- 请求方法：`GET`
- 接口说明：根据任务ID查询简历诊断任务的详细信息，包括状态和结果
- 是否鉴权：是

### 请求头

| 名称          | 是否必填 | 说明         |
| ------------- | -------- | ------------ |
| Authorization | 是       | Bearer Token |

### 路径参数

| 字段名 | 类型   | 是否必填 | 说明     |
|--------|--------|----------|----------|
| taskId | Long   | 是       | 任务ID   |

### 请求参数
- 无请求参数

### 示例请求
```
GET /api/resume/task/1789012345678901234
```

### 返回结果说明
- `code`：业务状态码
- `message`：响应信息
- `data`：任务详情对象
- `timestamp`：响应时间

data 字段说明：

| 字段名           | 类型     | 说明                     |
|------------------|----------|--------------------------|
| taskId           | Long     | 任务ID                   |
| userId           | Long     | 用户ID                   |
| fileUrl          | string   | PDF简历存储地址          |
| status           | Integer  | 任务状态：0-排队中，1-解析分析中，2-完成，3-失败 |
| statusDesc       | string   | 任务状态描述             |
| diagnosisResult  | string   | AI返回的结构化诊断报告（JSON格式，仅完成时有值） |
| errorMsg         | string   | 失败时的异常记录（仅失败时有值） |
| createTime       | DateTime | 创建时间                 |
| updateTime       | DateTime | 更新时间                 |

### 示例响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 1789012345678901234,
    "userId": 1789012345678901001,
    "fileUrl": "https://example.com/resumes/user-resume.pdf",
    "status": 2,
    "statusDesc": "已完成",
    "diagnosisResult": "{\"basicInfo\":{\"score\":85,\"hasName\":true},\"overall\":{\"totalScore\":78}}",
    "errorMsg": null,
    "createTime": "2024-04-01T10:00:00",
    "updateTime": "2024-04-01T10:00:05"
  },
  "timestamp": 1712345678901
}
```

---

## 3. 查询历史记录

### 基本信息
- 请求路径：`/api/resume/history`
- 请求方法：`GET`
- 接口说明：查询当前登录用户的简历诊断历史记录列表
- 是否鉴权：是

### 请求头

| 名称          | 是否必填 | 说明         |
| ------------- | -------- | ------------ |
| Authorization | 是       | Bearer Token |

### 请求参数
- 无请求参数

### 示例请求
```
GET /api/resume/history
```

### 返回结果说明
- `code`：业务状态码
- `message`：响应信息
- `data`：历史记录数组
- `timestamp`：响应时间

data 数组元素说明：

| 字段名     | 类型     | 说明                     |
|------------|----------|--------------------------|
| taskId     | Long     | 任务ID                   |
| fileUrl    | string   | PDF简历存储地址          |
| status     | Integer  | 任务状态：0-排队中，1-解析分析中，2-完成，3-失败 |
| statusDesc | string   | 任务状态描述             |
| createTime | DateTime | 创建时间                 |
| updateTime | DateTime | 更新时间                 |

### 示例响应
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "taskId": 1789012345678901234,
      "fileUrl": "https://example.com/resumes/user-resume.pdf",
      "status": 2,
      "statusDesc": "已完成",
      "createTime": "2024-04-01T10:00:00",
      "updateTime": "2024-04-01T10:00:05"
    },
    {
      "taskId": 1789012345678901235,
      "fileUrl": "https://example.com/resumes/user-resume-v2.pdf",
      "status": 0,
      "statusDesc": "排队中",
      "createTime": "2024-04-01T11:00:00",
      "updateTime": "2024-04-01T11:00:00"
    }
  ],
  "timestamp": 1712345678901
}
```
