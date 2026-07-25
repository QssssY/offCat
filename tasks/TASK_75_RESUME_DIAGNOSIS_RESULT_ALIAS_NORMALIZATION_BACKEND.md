# 简历诊断结果字段别名归一化后端修复

## 当前任务所属模块

- 后端模块：简历诊断任务、AI 诊断结果归一化、诊断结果持久化。
- 触发原因：`debug.txt` 显示任务已经从排队、解析、AI 分析、增强到完成全部走完，并且完成后前端已经请求完整详情；因此结果页只显示综合评价和总分，不是等待阶段轻量级状态轮询没有拉取详情导致的。真正风险点在后端把 AI 原始 JSON 归一化为 `ResumeDiagnosisResult` 时，模型返回的常见字段别名会被 `@JsonIgnoreProperties(ignoreUnknown = true)` 忽略，导致技能、工作经历、项目经历、教育背景、个人定位和优化建议在入库前被丢弃。

## 前端文件定位

- 前端配套展示修复见 `frontend/tasks/TASK_75_RESUME_DIAGNOSIS_RESULT_SCORE_DISPLAY_FRONTEND.md`。

## 后端文件定位

- 诊断结果 DTO：`server/src/main/java/com/airesume/server/dto/resume/ResumeDiagnosisResult.java`
- 诊断处理器测试：`server/src/test/java/com/airesume/server/service/impl/ResumeDiagnosisProcessorTest.java`

## 本轮修改文件清单

- `server/src/main/java/com/airesume/server/dto/resume/ResumeDiagnosisResult.java`
- `server/src/test/java/com/airesume/server/service/impl/ResumeDiagnosisProcessorTest.java`

## 前端实现方案

- 前端不参与后端归一化和入库逻辑。
- 前端配套处理为移除教育背景分数的总分兜底展示，避免后端缺失教育维度时显示误导性分数。

## 后端实现方案

- 在 `ResumeDiagnosisResult` 顶层字段增加 `@JsonAlias`，兼容模型常见输出字段：`skills`、`workExperience`、`projectExperience`、`education`、`positioning`、`suggestions` 等。
- 在各维度内部字段增加常见别名兼容：`strengths/advantages/highlights`、`weaknesses/issues/problems/shortcomings`、`suggestions/recommendations/improvements/advice`、`skillList/skills/items`、`experiences/workExperience/items`、`projects/projectExperience/items` 等。
- 保持最终入库 JSON 的标准字段名不变，仍然归一化为前端已经使用的 `skillEvaluation`、`workExperienceEvaluation`、`projectExperienceEvaluation`、`educationEvaluation`、`positioningEvaluation`、`optimizationSuggestions`。
- 不修改 AI 调用、Prompt、任务状态流转、完整详情接口或轻量级状态接口。

## 数据存储方案

- 不修改数据库结构。
- 不新增表、字段、索引或迁移脚本。
- 新生成的诊断结果会在入库前保留别名字段对应的维度内容；已经入库且字段已被旧逻辑丢弃的历史报告，需要重新诊断或重新处理原始 AI 响应才可能恢复缺失维度。

## stage 更新说明

- 已在 `tasks/stage.md` 顶部记录本轮后端修复、验证命令和停止边界。

## 编译结果

- `mvn.cmd -DskipTests compile` 通过。

## 构建结果

- 后端无前端构建产物。

## 当前功能验收说明

- RED 验证：新增 `processTaskShouldPreserveCommonAiFieldAliasesDuringNormalization` 后，旧实现下 `skills.score` 未能进入 `skillEvaluation.score`，测试失败。
- GREEN 验证：增加 `@JsonAlias` 后，`mvn.cmd test "-Dtest=ResumeDiagnosisProcessorTest#processTaskShouldPreserveCommonAiFieldAliasesDuringNormalization"` 通过。
- 回归验证：`mvn.cmd test "-Dtest=ResumeDiagnosisProcessorTest"` 通过，10 个用例全绿。
- 编译验证：`mvn.cmd -DskipTests compile` 通过。

## 停止，不继续下一个功能

本轮只修复简历诊断结果字段别名在后端归一化时被丢弃的问题，不继续扩展新的报告模块、Prompt 策略、数据库结构、导出能力或其它简历功能。