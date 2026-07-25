# TASK_25_V11_RESUME_DIAGNOSIS_V2_BACKEND

> 2026-05-04 补充记录：本轮新增“简历分析超时与等待过长”修复，核心改动为任务详情接口轻量化、诊断消费者耗时分段日志、超时错误收敛，以及紧凑版诊断 Prompt 接入。接口与结果结构保持兼容，数据库表结构不变。

## 1. 当前任务所属模块
- V1.1 简历诊断结果升级：问题诊断工作台

## 2. 前端文件定位
- `frontend/app/src/views/resume/ResultView.vue`
- `frontend/app/src/api/resume.js`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/dto/resume/ResumeDiagnosisResult.java`
- `server/src/main/java/com/airesume/server/dto/resume/ResumeDiagnosisTaskResponse.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImpl.java`
- `server/src/main/java/com/airesume/server/mock/MockDiagnosisResultGenerator.java`

## 4. 本轮修改文件清单
- `server/src/main/java/com/airesume/server/dto/resume/ResumeDiagnosisResult.java`
- `server/src/main/java/com/airesume/server/dto/resume/ResumeDiagnosisTaskResponse.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImpl.java`
- `server/src/main/java/com/airesume/server/mock/MockDiagnosisResultGenerator.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeAiServiceImpl.java`

## 5. 后端实现方案
- 新增 ResumeDiagnosisResult V2 结构内部类（DiagnosisSummary、RadarDimension、DeductionDetail、DiagnosisIssue、RewriteExample、KeywordAnalysis 等）
- 修改 ResumeDiagnosisTaskResponse.diagnosisResult 字段从 String 改为 Object
- 在 ResumeDiagnosisTaskServiceImpl.buildTaskResponse() 中增加 JSON 反序列化处理，失败时记录 log.warn 并返回兼容空结构
- 修改 MockDiagnosisResultGenerator 生成完整 V2 结构（包含 summary、radarDimensions、deductionDetails、issues、sections、keywordAnalysis、rewriteSuggestions、actionPlan、metadata）
- 修改 ResumeAiServiceImpl 的 systemPrompt 和 userPrompt，要求 AI 返回 V2 结构：
  - 必须输出 JSON，不输出 Markdown
  - 每个 issue 必须包含 evidence.sourceText（来自原文，不允许编造）
  - radarDimensions 固定五个维度，总分按权重计算
  - keywordAnalysis 禁止输出无效关键词（JD、XX、岗位职责等）
  - metadata.warnings 中提示文本质量差的问题

## 6. 数据存储方案
- 保持数据库 diagnosis_result 字段为 JSON 字符串存储
- 接口返回前在 Service 层反序列化为对象

## 7. 验证结果
- 后端编译：成功
- 前端构建：成功（使用 chart.js 的 Radar 组件）

## 8. 验收说明
- 后端返回的 diagnosisResult 是对象，不是 JSON 字符串
- Mock 简历诊断结果能生成 V2 结构
- 前端可兼容旧结构和新结构
- 原有功能（岗位匹配分析、AI 简历润色等）未被破坏
