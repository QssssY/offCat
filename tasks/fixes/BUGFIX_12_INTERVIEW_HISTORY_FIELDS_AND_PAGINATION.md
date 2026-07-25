# BUGFIX_12_INTERVIEW_HISTORY_FIELDS_AND_PAGINATION

## 背景

当前模拟面试历史接口已经可以返回当前用户的历史记录列表，但在前端联调中发现，现有 `InterviewHistoryResponse` 字段不足以支撑正确展示与后续扩展，具体表现为：

1. 无法正确区分“普通面试 / 压力面试”
2. 无法正确展示消息数
3. 历史接口当前一次性返回全量数据，未支持分页
4. 前端若继续兼容兜底，会导致后续重复返工

当前 `InterviewHistoryResponse` 已有字段：

- id
- sessionId
- jobRole
- difficulty
- difficultyDesc
- status
- statusDesc
- comprehensiveScore
- createTime
- updateTime

缺失关键字段：

- interviewMode / interviewModeDesc
- messageCount
- 分页返回结构

---

## 任务目标

完成模拟面试历史接口的数据模型增强与分页支持，并同步考虑简历历史接口分页能力。

---

## 修改要求

### 1. 模拟面试历史返回字段增强

在 `InterviewHistoryResponse` 中新增：

- `interviewMode`：普通面试 / 压力面试的模式标识
- `interviewModeDesc`：模式描述
- `messageCount`：当前会话消息总数

说明：
- `difficulty` 继续只表示难度，不承担模式语义
- 前端不得再使用 difficulty 推断普通面试 / 压力面试

### 2. 模拟面试历史接口支持分页

将当前：

```java
@GetMapping("/history")
public Result<List<InterviewHistoryResponse>> getHistory(Authentication authentication)
```