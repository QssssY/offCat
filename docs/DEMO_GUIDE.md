# 演示流程指南

本文档提供智能模拟面试与简历诊断系统后端的演示流程建议。

---

## 演示前准备

### 环境检查清单

- [ ] MySQL数据库已启动
- [ ] Redis已启动
- [ ] RabbitMQ已启动
- [ ] 应用已成功启动在8080端口
- [ ] 数据库表结构已初始化
- [ ] Postman/ApiFox等接口测试工具已准备

### 数据准备

可预先创建以下测试用户（可选）：

```sql
-- 普通用户（密码：123456）
-- 会员用户（密码：123456，VIP未过期）
-- 管理员用户（密码：123456，role=9）
```

---

## 演示流程一：用户认证与额度

### 1. 用户注册

**接口**: `POST /auth/register`

**请求示例**:
```json
{
  "username": "demo_user",
  "password": "123456"
}
```

**演示要点**:
- 说明用户名唯一性校验
- 说明密码BCrypt加密存储
- 展示user_quota记录自动初始化

---

### 2. 用户登录

**接口**: `POST /auth/login`

**请求示例**:
```json
{
  "username": "demo_user",
  "password": "123456"
}
```

**演示要点**:
- 展示JWT Token返回
- 说明Token有效期（24小时）
- 复制Token用于后续接口

---

### 3. 获取当前用户

**接口**: `GET /auth/me`

**请求头**:
```
Authorization: Bearer <your-token>
```

**演示要点**:
- 展示从Token解析userId
- 说明无需传参即可获取用户信息

---

## 演示流程二：简历诊断

### 1. 上传简历创建诊断任务

**接口**: `POST /api/resume/upload`

**请求头**:
```
Authorization: Bearer <your-token>
```

**请求示例**:
```json
{
  "fileUrl": "https://example.com/demo-resume.pdf"
}
```

**演示要点**:
- 说明额度校验（检查次数是否足够）
- 说明任务创建成功
- 展示返回的taskId
- 说明消息已发送到RabbitMQ

---

### 2. 查询任务状态

**接口**: `GET /api/resume/task/{taskId}`

**请求头**:
```
Authorization: Bearer <your-token>
```

**演示要点**:
- 实时刷新展示状态变化（排队中→处理中→完成）
- 说明异步处理机制
- 展示Mock生成的diagnosis_result（JSON格式）
- 说明isMock标记

---

### 3. 查询历史记录

**接口**: `GET /api/resume/history`

**请求头**:
```
Authorization: Bearer <your-token>
```

**演示要点**:
- 展示历史记录列表
- 说明按时间倒序排列
- 展示状态描述

---

## 演示流程三：模拟面试

### 1. 创建面试会话

**接口**: `POST /api/interview/session`

**请求头**:
```
Authorization: Bearer <your-token>
```

**请求示例**:
```json
{
  "jobRole": "Java开发工程师",
  "difficulty": 2
}
```

**演示要点**:
- 说明难度级别（1初级/2中级/3高级）
- 展示返回的sessionId
- 说明额度扣减成功
- 说明初始化欢迎消息

---

### 2. 查询会话详情（查看欢迎消息）

**接口**: `GET /api/interview/session/{sessionId}`

**请求头**:
```
Authorization: Bearer <your-token>
```

**演示要点**:
- 展示会话状态"进行中"
- 展示欢迎消息（assistant角色）
- 说明聊天记录按时间正序

---

### 3. 发送第一条消息

**接口**: `POST /api/interview/message`

**请求头**:
```
Authorization: Bearer <your-token>
```

**请求示例**:
```json
{
  "sessionId": "<your-session-id>",
  "content": "你好，我叫张三，有3年Java开发经验，熟悉Spring Boot、MySQL、Redis等技术栈。"
}
```

**演示要点**:
- 展示用户消息已保存
- 展示Mock面试官回复
- 说明回复的连贯性

---

### 4. 发送第二条消息（可选）

**请求示例**:
```json
{
  "sessionId": "<your-session-id>",
  "content": "我最有成就感的项目是电商平台的订单模块重构，当时使用了Redis缓存来提升性能，QPS从100提升到了500。"
}
```

**演示要点**:
- 展示多轮对话
- 说明Mock回复的多样性

---

### 5. 结束面试

**接口**: `POST /api/interview/session/{sessionId}/end`

**请求头**:
```
Authorization: Bearer <your-token>
```

**演示要点**:
- 展示会话状态更新为"已结束"
- 展示综合评分（60-95分）
- 展示评价报告（JSON格式）
- 说明各维度评分、优缺点、建议

---

### 6. 查询会话详情（完整记录）

**接口**: `GET /api/interview/session/{sessionId}`

**演示要点**:
- 展示完整聊天记录
- 展示最终状态和评分
- 说明评价报告JSON结构

---

### 7. 查询面试历史

**接口**: `GET /api/interview/history`

**演示要点**:
- 展示历史记录列表
- 展示已结束会话的评分
- 说明按时间倒序排列

---

## 演示流程四：额度控制演示

### 1. 展示用户额度

**数据库查询**:
```sql
SELECT * FROM user_quota WHERE user_id = ?;
```

**演示要点**:
- 展示total_*字段（普通用户累计）
- 展示daily_*字段（会员用户每日）
- 展示last_refresh_date

---

### 2. 演示次数扣减

**操作**:
1. 创建简历诊断任务
2. 查询user_quota表
3. 创建面试会话
4. 再次查询user_quota表

**演示要点**:
- 展示total_resume_used增加
- 展示total_interview_used增加

---

### 3. 跨天刷新演示（可选）

**数据库操作**:
```sql
UPDATE user_quota SET last_refresh_date = DATE_SUB(CURDATE(), INTERVAL 1 DAY) WHERE user_id = ?;
```

**操作**:
- 再次发起请求触发刷新
- 查询daily_*字段已清零

---

## 演示流程五：管理端功能

### 前置说明
管理端接口需要管理员权限（role=9），请预先创建管理员用户并使用管理员账号登录获取Token。

### 1. 管理员登录

**接口**: `POST /auth/login`

**请求示例**:
```json
{
  "username": "admin",
  "password": "123456"
}
```

**演示要点**:
- 使用管理员账号登录
- 获取管理员Token
- 说明管理员Token用于后续管理端接口

---

### 2. 查询Prompt列表

**接口**: `GET /api/admin/prompts`

**请求头**:
```
Authorization: Bearer <admin-token>
```

**演示要点**:
- 展示所有Prompt模板列表
- 说明场景类型、岗位、难度、启用状态
- 展示Prompt内容

---

### 3. 新增Prompt模板

**接口**: `POST /api/admin/prompts`

**请求头**:
```
Authorization: Bearer <admin-token>
```

**请求示例**:
```json
{
  "scenarioType": 1,
  "jobRole": "前端开发工程师",
  "difficulty": 2,
  "promptContent": "你是一位资深的前端技术面试官..."
}
```

**演示要点**:
- 展示新增Prompt成功
- 返回新增的Prompt ID
- 说明场景类型：1-面试，2-简历诊断

---

### 4. 修改Prompt模板

**接口**: `PUT /api/admin/prompts`

**请求头**:
```
Authorization: Bearer <admin-token>
```

**请求示例**:
```json
{
  "id": 1789012345678901001,
  "promptContent": "你是一位资深的前端技术面试官...（已更新）"
}
```

**演示要点**:
- 展示修改Prompt成功
- 说明可修改的字段

---

### 5. 启用/禁用Prompt模板

**接口**: `PUT /api/admin/prompts/{id}/active`

**请求头**:
```
Authorization: Bearer <admin-token>
```

**示例请求**:
```
PUT /api/admin/prompts/1789012345678901001/active?isActive=0
```

**演示要点**:
- 展示启用/禁用操作成功
- 说明isActive参数：1-启用，0-禁用

---

### 6. 查询用户列表

**接口**: `GET /api/admin/users`

**请求头**:
```
Authorization: Bearer <admin-token>
```

**演示要点**:
- 展示所有用户列表
- 说明用户角色、状态、VIP到期时间
- 展示创建时间

---

### 7. 封禁/解封用户

**接口**: `PUT /api/admin/users/{userId}/status`

**请求头**:
```
Authorization: Bearer <admin-token>
```

**示例请求**:
```
PUT /api/admin/users/1789012345678901234/status?status=0
```

**演示要点**:
- 展示封禁用户（status=0）
- 展示解封用户（status=1）
- 说明被封禁用户无法登录

---

### 8. 查询用户额度

**接口**: `GET /api/admin/users/{userId}/quota`

**请求头**:
```
Authorization: Bearer <admin-token>
```

**演示要点**:
- 展示指定用户的额度详情
- 展示累计使用次数、每日使用次数
- 展示最后刷新日期

---

### 9. 调整用户额度

**接口**: `PUT /api/admin/users/quota`

**请求头**:
```
Authorization: Bearer <admin-token>
```

**请求示例**:
```json
{
  "userId": 1789012345678901234,
  "totalInterviewUsed": 0,
  "totalResumeUsed": 0
}
```

**演示要点**:
- 展示调整用户额度成功
- 说明可调整的字段
- 展示调整后的值

---

## Mock实现说明

### 当前Mock点

| 模块 | Mock类 | 说明 |
|------|--------|------|
| 简历诊断 | MockDiagnosisResultGenerator | 生成结构化诊断结果JSON |
| 模拟面试 | MockInterviewService | 生成面试官回复 |
| 模拟面试 | MockInterviewService | 生成综合评分 |
| 模拟面试 | MockInterviewService | 生成评价报告 |

### Mock数据特征

所有Mock生成的数据包含标记：
```json
{
  "isMock": true,
  "mockGeneratedAt": 1712345678901
}
```

### 后续AI接入

接入真实大模型后，需要替换：
- MockDiagnosisResultGenerator → Spring AI + 真实大模型
- MockInterviewService → Spring AI + 真实大模型

---

## 演示FAQ

### Q: 为什么没有真实PDF解析？
A: 当前版本仅接受fileUrl占位，真实PDF解析属于增强功能，后续可接入。

### Q: 为什么没有SSE流式推送？
A: 当前版本简化为请求响应模式，SSE流式推送属于增强功能，后续可实现。

### Q: 为什么不直接接入真实大模型？
A: 当前版本优先跑通核心业务链路，真实大模型接入需要API Key，属于可选增强项。

### Q: Mock数据可以修改吗？
A: 可以修改MockInterviewService和MockDiagnosisResultGenerator中的模板。

### Q: 管理端接口需要什么权限？
A: 所有管理端接口需要管理员权限（role=9），普通用户无法访问。

---

## 演示检查清单

| 检查项 | 状态 |
|--------|------|
| 用户注册/登录演示成功 | ⬜ |
| 简历诊断完整流程演示成功 | ⬜ |
| 模拟面试完整流程演示成功 | ⬜ |
| Mock实现说明清楚 | ⬜ |
| 额度控制演示成功 | ⬜ |
| 历史记录查询演示成功 | ⬜ |
| 管理端功能演示成功 | ⬜ |
