# TASK_24_V11_JOB_TARGETED_INTERVIEW_FRONTEND

## 1. 当前任务所属模块
- V1.1 第三个功能：岗位定向模拟面试

## 2. 前端文件定位
- `frontend/app/src/views/interview/InterviewEntryView.vue`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/app/src/views/interview/InterviewHistoryView.vue`
- `frontend/app/src/api/interview.js`
- `frontend/app/src/api/resume.js`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/dto/interview/InterviewSessionResponse.java`

## 4. 本轮修改文件清单
- `frontend/app/src/api/interview.js`
- `frontend/app/src/views/interview/InterviewEntryView.vue`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/app/src/views/interview/InterviewHistoryView.vue`
- `frontend/runtime/STATE.md`

## 5. 前端实现方案
- 面试入口页新增岗位定向配置区，清楚区分普通模拟面试与岗位定向模拟面试。
- 若从简历诊断结果页携带 `resumeTaskId` 进入，则自动查询任务详情并提示可复用最近一次 JD 对比结果。
- 会话页继续沿用原有聊天页结构，仅增加岗位定向提示横幅。
- 报告页新增岗位相关反馈区，并保留既有评分、总结、逐题表现展示。
- 历史页为岗位定向会话增加标识和来源标签，便于识别。

## 6. 后端实现方案
- 前端按后端扩展参数提交岗位定向配置，不要求新增复杂前端状态机。
- 结果页与历史页直接消费后端返回的 `jobTargeted` 与 `jobTargetContext`，避免重复拼装数据。

## 7. 数据存储方案
- 前端不新增本地持久化结构。
- 岗位定向上下文、反馈与来源信息全部依赖后端独立表落库后回显。

## 8. stage 更新说明
- 开发前已将前端功能三标记为“开发中”。
- 本轮完成后已将 `frontend/runtime/STATE.md` 更新为：
  - 功能三“岗位定向模拟面试”已完成
  - 当前状态为等待人工验收

## 9. 编译结果
- 后端编译命令：`mvn.cmd -q -DskipTests compile`
- 结果：通过

## 10. 构建结果
- 前端构建命令：`npm.cmd run build`
- 结果：通过
- 说明：首次沙箱构建受 `esbuild spawn EPERM` 限制，获批后重跑通过。

## 11. 当前功能验收说明
- 页面上已能清楚区分普通模拟面试与岗位定向模拟面试。
- 可输入 JD 或复用最近一次 JD 对比结果。
- 会话页和报告页已展示岗位定向相关信息。
- 刷新和历史查看时，岗位定向标识与反馈不会丢失。
- 未破坏原有历史入口、聊天主流程和普通模拟面试能力。

## 12. 停止，不继续下一个功能
- 本轮仅完成“岗位定向模拟面试”前端接入，到此停止，等待人工验收。

## 13. 本轮补充修复说明
- 修复范围：仅限用户端模拟面试会话页 `InterviewSessionView.vue`。
- 修复原因：会话消息超过两轮后，后端 SSE 已返回完整数据，但前端按 chunk 逐行解析 `data:`，在 JSON 被拆包时会丢失 AI 回复，导致页面只显示用户消息。
- 修复内容：
  - 将流式解析改为“缓冲区 + SSE 事件块”消费方式。
  - 保留原有打字机效果，不重做面试页面结构。
  - 顺带清理该页面原有模板乱码和标签破损，保证本轮构建可通过。
- 影响范围：仅修复模拟面试会话展示与流式回复，不扩展 V1.1 其他功能。
- 补充修复：参考 `frontend/app` 仓库中该文件最近一次提交版本，恢复会话页滚动布局；聊天区改回独立滚动容器，解决消息过多时无下拉条、内容溢出屏幕外的问题。
