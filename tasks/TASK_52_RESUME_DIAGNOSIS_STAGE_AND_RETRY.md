# TASK-52：简历诊断进度可视化 + 失败重试 — 后端

## 所属模块
简历诊断模块

## 功能目标
为简历诊断任务增加 stage 子阶段字段，使前端能实时展示 4 阶段进度（排队 → 提取文本 → AI 分析 → 生成报告）；新增 retry 接口，允许 24 小时内失败任务复用原文件创建新任务。

## 后端变更清单

### 数据库
- `db/migrations/TASK_52_RESUME_DIAGNOSIS_STAGE_AND_RETRY.sql` — 新建
- `server/db/migrations/TASK_52_RESUME_DIAGNOSIS_STAGE_AND_RETRY.sql` — 新建
- `db/schema.sql` / `server/db/schema.sql` — 加 stage 列

### 常量 & 错误码
- `ResumeDiagnosisConstants.java` — 加 STAGE_EXTRACTING / STAGE_AI_ANALYZING / STAGE_ENHANCING
- `ResultCode.java` — 加 RESUME_TASK_NOT_RETRYABLE(2011) / RESUME_TASK_RETRY_EXPIRED(2012)

### 实体 & DTO
- `ResumeDiagnosisTask.java` — 加 stage 字段
- `ResumeDiagnosisTaskResponse.java` — 加 stage / stageDesc
- `ResumeDiagnosisHistoryResponse.java` — 加 errorMsg

### Service 层
- `ResumeDiagnosisTaskService.java` — 加 updateStage / retryFailedTask 接口
- `ResumeDiagnosisTaskServiceImpl.java` — 实现新方法 + buildResponse 更新 + getStageDescription + completed/failed 清除 stage
- `ResumeDiagnosisTaskServiceImpl.java` — 修复失败重试窗口改用 failed_at，避免 update_time 被后续维护更新延长
- `ResumeDiagnosisTaskServiceImpl.java` — 修复 updateStage 仅允许更新 PROCESSING 任务，避免延迟阶段更新污染终态任务

### Processor
- `ResumeDiagnosisProcessor.java` — 3 处 updateStage 调用
- `ResumeDiagnosisProcessor.java` — AI 错误提示改为优先基于 BusinessException/ResultCode 映射，不再依赖 HTTP 数字字符串匹配

### 修复补充
- `ResumeDiagnosisTask.java` / `db/schema.sql` / `server/db/schema.sql` — 新增 failed_at 字段
- `db/migrations/TASK_52_RETRY_FAILED_AT_FIX.sql` — 新增独立 failed_at 迁移
- `server/db/migrations/TASK_52_RETRY_FAILED_AT_FIX.sql` — 新增独立 failed_at 迁移
- `SysUserServiceImpl.java` — removeById 增加 sys_user 缓存驱逐

### Controller
- `ResumeDiagnosisController.java` — POST /api/resume/task/{taskId}/retry

## 验证状态
- [x] mvn compile 通过
- [x] mvn test 通过（426 tests）
- [x] 修复回归测试通过：`mvn.cmd -q "-Dtest=ResumeDiagnosisTaskServiceImplTest,ResumeDiagnosisProcessorTest,SysUserServiceImplTest" test`
