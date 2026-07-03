# TASK_06A_ADMIN 管理端接口文档

本文档覆盖当前管理端模块全部 **23 个接口**，包含岗位配置、AI 引擎配置、Prompt 管理、用户与权益管理、业务监控、数据看板增强能力。

---

## 0. 通用说明

### 鉴权规则
- 所有 `/api/admin/**` 接口都要求管理员权限
- 请求头必须携带：

```http
Authorization: Bearer <your-token>
```

### 统一返回结构

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1712345678901
}
```

### 常见状态字段
- `isActive`: `1` 启用，`0` 禁用
- `businessType`: `interview` / `resume`
- `role`: `0` 普通用户，`1` VIP，`9` 管理员

---

## 1. 岗位配置接口（4）
1. `GET /api/admin/job-roles`
2. `POST /api/admin/job-roles`
3. `PUT /api/admin/job-roles`
4. `PUT /api/admin/job-roles/{id}/active`

---

## 2. AI 引擎配置接口（4）
1. `GET /api/admin/ai-engines`
2. `POST /api/admin/ai-engines`
3. `PUT /api/admin/ai-engines`
4. `PUT /api/admin/ai-engines/{id}/active`

规则补充：
- 同一 `businessType` 同时最多只有一个启用配置
- 列表接口返回的 `apiKey` 为脱敏值

---

## 3. Prompt 管理接口（4）
1. `GET /api/admin/prompts`
2. `POST /api/admin/prompts`
3. `PUT /api/admin/prompts`
4. `PUT /api/admin/prompts/{id}/active`

规则补充：
- Prompt 岗位字段必须来自 `sys_job_role`
- 返回结构包含 `jobRoleCode` 与 `jobRoleName`

---

## 4. 用户与权益管理接口（6）
1. `GET /api/admin/users`
2. `GET /api/admin/users/{userId}/rights`
3. `PUT /api/admin/users/{userId}/rights`
4. `PUT /api/admin/users/{userId}/status`
5. `GET /api/admin/users/{userId}/quota`
6. `PUT /api/admin/users/quota`

---

## 5. 数据看板接口（4）

### 5.1 GET `/api/admin/dashboard/overview`
作用：看板总览统计，支持日期范围筛选。

Query 参数：
- `startDate`（可选，`yyyy-MM-dd`）
- `endDate`（可选，`yyyy-MM-dd`）

兼容默认：
- 不传参数时默认统计“今天”

返回字段：
- `totalUserCount`
- `vipUserCount`
- `activePromptCount`
- `activeJobRoleCount`
- `activeAiEngineCount`
- `todayInterviewSessionCount`
- `todayResumeDiagnosisCount`

### 5.2 GET `/api/admin/dashboard/trends`
作用：趋势统计，支持日期范围筛选。

Query 参数：
- `startDate`（可选，`yyyy-MM-dd`）
- `endDate`（可选，`yyyy-MM-dd`）

兼容默认：
- 不传参数时默认最近 7 天

返回字段：
- `date`
- `interviewSessionCount`
- `resumeDiagnosisCount`

### 5.3 GET `/api/admin/dashboard/hot-job-roles`
作用：热门岗位排行，支持日期范围和条数限制。

Query 参数：
- `startDate`（可选，`yyyy-MM-dd`）
- `endDate`（可选，`yyyy-MM-dd`）
- `limit`（可选，默认 10，最大 50）

返回字段：
- `jobRole`
- `sessionCount`

### 5.4 GET `/api/admin/dashboard/business-distribution`
作用：业务分布统计（面试/简历），用于饼图或占比图。

Query 参数：
- `startDate`（可选，`yyyy-MM-dd`）
- `endDate`（可选，`yyyy-MM-dd`）

默认行为：
- 不传参数时默认最近 7 天

返回字段：
- `startDate`
- `endDate`
- `interviewCount`
- `resumeCount`
- `totalCount`
- `interviewPercent`
- `resumePercent`

---

## 6. 业务监控接口（1）

### 6.1 GET `/api/admin/monitor/overview`
作用：业务监控总览（应用层统计版）。

返回字段：
- `pendingResumeTaskCount`
- `processingResumeTaskCount`
- `failedResumeTaskCount`
- `completedResumeTaskCount`
- `activeInterviewSessionCount`
- `todayInterviewSessionCount`
- `todayResumeDiagnosisCount`
- `todayResumePolishCount`
- `todayJobMatchCount`
- `todayCommunityPostCount`
- `pendingFeedbackCount`
- `processingFeedbackCount`
- `todayFeedbackCount`
- `pendingCommunityPostCount`
- `pendingCommunityCommentCount`
- `pendingCommunityReviewCount`
- `todayOrderCount`

口径说明：
- 当前接口仍为应用层业务表统计版，不依赖 Redis、RabbitMQ、JVM 或数据库连接池探活。
- `pendingCommunityReviewCount = pendingCommunityPostCount + pendingCommunityCommentCount`。
- 今日类指标统一按 `[今日 00:00, 明日 00:00)` 的 `create_time` 半开区间统计。

---

## 7. 统一参数校验规则（看板接口）
- `startDate` 不能大于 `endDate`
- 单次查询范围不能超过 90 天
- 统计口径统一：
  - 面试统计按 `InterviewSession.createTime`
  - 简历诊断按 `ResumeDiagnosisTask.createTime`

---

## 8. 错误响应示例

```json
{
  "code": 500,
  "message": "startDate 不能大于 endDate",
  "data": null
}
```
