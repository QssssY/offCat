# TASK_23_V11_RESUME_AI_POLISH_BACKEND

## 1. 当前任务所属模块
- V1.1 第二个功能
- 模块：AI 简历润色

## 2. 前端文件定位
- `frontend/app/src/views/resume/ResultView.vue`
- `frontend/app/src/components/resume/ResumeTemplate.vue`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/service/impl/ResumeAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumePolishServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImpl.java`

## 4. 本轮修改文件清单
- `server/src/main/java/com/airesume/server/service/impl/ResumeAiServiceImpl.java`
- `tasks/TASK_23_V11_RESUME_AI_POLISH_BACKEND.md`
- `runtime/STATE.md`

## 5. 前端实现方案
- 继续由结果页消费 `latestPolishResult`，不新增并行接口。
- 前端模板将依据后端返回的润色文本做可编辑排版展示，因此后端需要输出更适合排版的简历草稿。

## 6. 后端实现方案
- 保持 `POST /api/resume/polish/analyze` 接口不变，避免破坏现有调用链路。
- 仅调整 `ResumeAiServiceImpl` 中的简历润色 prompt 规则，让 AI 输出更适合单栏简历模板渲染的纯文本结果。
- 新增的润色规则重点包括：
  - 学校层次和教育亮点强时，允许在教育背景保留一行额外亮点；
  - 学校普通且无明显优势时，教育部分保持简洁；
  - 实习、工作、项目的先后顺序按内容强弱和岗位相关度决定；
  - 要求 AI 输出更接近真实投递简历，而不是通用总结稿；
  - 要求最终文本便于打印、便于前端二次编辑、便于头像留白。

## 7. 数据存储方案
- 继续复用 `resume_polish_record`。
- 不新增字段，不改数据库结构。
- 仍只落库润色后的简历文本和修改说明，保持最小增量。

## 8. stage 更新说明
- 已同步更新 `runtime/STATE.md`。
- 本轮属于功能二的补充完善：AI 简历润色规则升级，服务于新版简洁模板排版。

## 9. 编译结果
- 后端编译命令：`mvn.cmd -q -DskipTests compile`
- 结果：通过

## 10. 构建结果
- 前端构建命令：`npm.cmd run build`
- 结果：通过

## 11. 当前功能验收说明
- 后端仍可基于简历和可选 JD 生成润色结果。
- AI 润色规则已补充教育强弱、经历顺序、篇幅控制和打印导向要求。
- 接口结构未变，前端可直接消费新版润色文本。
- 本轮未扩展数据库字段、模板市场、导出历史、多版本草稿管理等超范围功能。

## 12. 停止，不继续下一个功能
- 本轮到 AI 简历润色规则升级为止，完成后停止，等待验收。
