# 简历诊断状态轮询与 SQL 日志脱敏后端优化

## 当前任务所属模块

- 后端模块：简历诊断任务、任务状态查询、运行日志安全。
- 触发原因：最新 `debug.txt` 显示简历诊断流程功能正常，但等待页轮询完整详情接口会读取 `resume_text`、`diagnosis_result` 大字段；开发环境 MyBatis `StdOutImpl` 会把 API Key、简历原文和诊断 JSON 打到控制台日志。

## 前端文件定位

- 前端联动任务见 `frontend/tasks/TASK_73_RESUME_DIAGNOSIS_STATUS_POLLING_FRONTEND.md`。

## 后端文件定位

- 控制器：`server/src/main/java/com/airesume/server/controller/ResumeDiagnosisController.java`
- 服务接口：`server/src/main/java/com/airesume/server/service/ResumeDiagnosisTaskService.java`
- 服务实现：`server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImpl.java`
- DTO：`server/src/main/java/com/airesume/server/dto/resume/ResumeDiagnosisTaskStatusResponse.java`
- 配置：`server/src/main/resources/application.yml`、`server/src/main/resources/application-dev.yml`
- 测试：`server/src/test/java/com/airesume/server/controller/ResumeDiagnosisControllerTest.java`、`server/src/test/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImplTest.java`、`server/src/test/java/com/airesume/server/config/RuntimeProtectionConfigTest.java`

## 本轮修改文件清单

- 新增 `ResumeDiagnosisTaskStatusResponse`，用于等待页轻量状态轮询。
- 新增 `GET /api/resume/task/{taskId}/status`，只返回任务状态、阶段、失败信息和时间字段。
- 新增 `ResumeDiagnosisTaskService.getTaskStatusById(...)`，保留任务归属校验。
- 服务实现中轻量状态查询显式选择小字段，不选择 `resume_text` 和 `diagnosis_result`。
- 默认和开发配置的 MyBatis 日志实现改为 `org.apache.ibatis.logging.nologging.NoLoggingImpl`，避免 SQL 明细和参数直接输出到控制台。

## 前端实现方案

- 前端结果页等待阶段改为轮询轻量状态接口；状态完成后再拉取一次完整详情。
- 详见前端任务文件。

## 后端实现方案

- 控制器新增轻量状态接口，与完整详情接口保持相同的登录用户归属校验。
- 服务层用 `LambdaQueryWrapper.select(...)` 显式限制字段，避免轮询阶段访问简历原文和完整诊断结果。
- 完整详情接口保持不变，结果页真正需要展示诊断内容时仍可读取完整数据。
- MyBatis 明细 SQL 输出在默认/dev/prod 三类配置中统一关闭；业务日志继续保留必要任务 ID、用户 ID 和阶段信息。

## 数据存储方案

- 不修改数据库结构。
- 不新增表、字段、索引或迁移脚本。
- 仅调整读取字段和日志配置。

## stage 更新说明

- 已在 `tasks/stage.md` 顶部记录本轮后端优化、验证命令和停止边界。

## 编译结果

- `mvn.cmd -DskipTests compile` 通过。

## 构建结果

- 后端无前端构建产物。

## 当前功能验收说明

- `mvn.cmd test "-Dtest=ResumeDiagnosisControllerTest,ResumeDiagnosisTaskServiceImplTest,RuntimeProtectionConfigTest"` 通过，24 个用例全绿。
- 回归覆盖：
  - 状态接口委托轻量服务方法。
  - 轻量查询不选择 `diagnosis_result`、`resume_text`。
  - 默认/dev/prod 均不使用 MyBatis `StdOutImpl` 输出 SQL 明细。

## 停止，不继续下一功能

本轮只处理简历诊断等待轮询降载和 MyBatis SQL 明细日志泄露风险，不继续扩展诊断报告内容、数据库结构、AI 生成策略或其它简历功能。
