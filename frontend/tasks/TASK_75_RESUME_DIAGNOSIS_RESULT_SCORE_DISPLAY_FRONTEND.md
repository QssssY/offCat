# 简历诊断结果维度分数展示前端修复

## 当前任务所属模块

- 前端模块：简历诊断结果页、评分概览、五维能力雷达图。
- 触发原因：后端诊断完成并返回完整详情后，如果某个维度确实缺失，前端不应使用总分推导该维度分数。旧逻辑会在 `educationEvaluation.score` 缺失时用总分兜底，导致页面可能显示错误的“教育背景 100 分”，掩盖真实缺失问题。

## 前端文件定位

- 结果页：`frontend/app/src/views/resume/ResultView.vue`
- 结果页测试：`frontend/app/src/__tests__/views/ResumeResultView.test.js`

## 后端文件定位

- 后端配套修复见 `tasks/TASK_75_RESUME_DIAGNOSIS_RESULT_ALIAS_NORMALIZATION_BACKEND.md`。

## 本轮修改文件清单

- `frontend/app/src/views/resume/ResultView.vue`
- `frontend/app/src/__tests__/views/ResumeResultView.test.js`

## 前端实现方案

- 删除教育背景分数的 `computeEducationFallback` 兜底逻辑。
- `educationScore` 只读取 `parsedDiagnosisResult.value?.educationEvaluation?.score || 0`。
- 雷达图 `education` 维度只读取 `result.educationEvaluation?.score || 0`。
- 保留任务等待阶段的轻量级状态轮询逻辑：状态为完成后仍然调用完整详情接口，不改变本轮后端修复的触发路径。

## 后端实现方案

- 后端增加 AI 字段别名兼容，确保新诊断结果入库前保留技能、经历、教育、定位和建议等维度。
- 前端不修改后端接口协议。

## 数据存储方案

- 不修改前端持久化数据。
- 不修改后端数据库结构。

## stage 更新说明

- 已在 `frontend/tasks/stage.md` 顶部记录本轮前端修复、验证命令和停止边界。

## 编译结果

- 前端无单独编译命令，构建结果见下方。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

- RED 验证：新增 `does not infer education score from total score when education dimension is missing` 后，旧实现仍包含 `computeEducationFallback`，测试失败。
- GREEN 验证：移除兜底后，`npm.cmd test -- --run src/__tests__/views/ResumeResultView.test.js` 通过，7 个用例全绿。
- 构建验证：`npm.cmd run build` 通过。

## 停止，不继续下一个功能

本轮只修复简历诊断结果页维度分数展示误导问题，不继续扩展报告导出、报告视觉重构、AI 生成策略或其它简历功能。