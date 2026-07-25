# TASK_CORE_BUGFIX_ROUND_2_INTERVIEW_FINAL_STATE

## 1. 任务目标
- 修复面试会话结束后的终态失效问题，确保后端与前端都进入不可继续操作的只读状态。
- 防止重复结束、重复生成报告造成的重复 token 消耗。

## 2. 根因定位
- 后端 `endSession` 在事务提交前启动异步报告生成，异步线程可能读到旧会话快照并整实体 `save` 回库，导致 `status` 被覆盖回进行中。
- 结束接口缺少并发幂等保护，重复点击可能触发多次结束与多次报告生成链路。
- 流式消息链路在用户消息落库前缺少最终状态校验，存在终态绕过窗口。

## 3. 修复实现
- `InterviewSessionRepository` 新增条件状态更新 `updateStatusIfCurrentStatus`，仅在进行中转结束时生效。
- `InterviewService.endSession` 改为条件更新 + 事务提交后触发异步报告任务，消除状态回滚风险。
- `InterviewService.generateAndPersistEvaluationReport` 改为仅回写评分/报告字段并强制保持结束态，避免覆盖 `status`。
- `InterviewService` 统一在普通发消息、流式发消息、用户消息落库前执行终态拦截。
- 结束接口改为幂等行为：已结束会话重复调用直接忽略，不重复生成报告。

## 4. 影响范围
- 后端接口：
  - `POST /api/interview/session/{sessionId}/message`
  - `POST /api/interview/session/{sessionId}/message/stream`
  - `POST /api/interview/session/{sessionId}/end`
  - `GET /api/interview/session/{sessionId}`
- 前端页面：
  - 会话页终态只读与按钮禁用
  - 历史页“已结束，报告生成中”状态展示

## 5. 验证要求
- 后端编译：`mvn.cmd -q -DskipTests compile`
- 前端构建：`npm.cmd run build`

## 6. 当前状态
- 本轮修复已完成代码改造与构建验证，等待人工验收。
