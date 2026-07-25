# TASK_24_V11_JOB_TARGETED_INTERVIEW_BACKEND

## 1. 当前任务所属模块
- V1.1 第三个功能：岗位定向模拟面试

## 2. 前端文件定位
- `frontend/app/src/views/interview/InterviewEntryView.vue`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `frontend/app/src/views/interview/InterviewHistoryView.vue`
- `frontend/app/src/api/interview.js`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/InterviewAiService.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/MockInterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/MockInterviewJobTargetService.java`
- `server/src/main/java/com/airesume/server/service/impl/MockInterviewJobTargetServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/ResumeJobMatchService.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeJobMatchServiceImpl.java`

## 4. 本轮修改文件清单
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/main/java/com/airesume/server/dto/interview/CreateSessionRequest.java`
- `server/src/main/java/com/airesume/server/dto/interview/InterviewHistoryResponse.java`
- `server/src/main/java/com/airesume/server/dto/interview/InterviewJobTargetContext.java`
- `server/src/main/java/com/airesume/server/dto/interview/InterviewJobTargetedFeedback.java`
- `server/src/main/java/com/airesume/server/dto/interview/InterviewSessionResponse.java`
- `server/src/main/java/com/airesume/server/entity/MockInterviewJobTargetRecord.java`
- `server/src/main/java/com/airesume/server/mapper/MockInterviewJobTargetRecordMapper.java`
- `server/src/main/java/com/airesume/server/mock/MockInterviewService.java`
- `server/src/main/java/com/airesume/server/service/InterviewAiService.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/MockInterviewJobTargetService.java`
- `server/src/main/java/com/airesume/server/service/ResumeJobMatchService.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewSessionServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/MockInterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/MockInterviewJobTargetServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/ResumeJobMatchServiceImpl.java`
- `server/db/schema.sql`
- `db/schema.sql`
- `db/migrations/TASK_14_JOB_TARGETED_INTERVIEW.sql`
- `runtime/STATE.md`

## 5. 前端实现方案
- 入口页新增岗位定向配置区，支持开启岗位定向、输入 JD、关联简历任务、优先复用最近一次 JD 对比结果。
- 会话页保留原有聊天结构，只增加岗位定向状态提示，不重做主流程。
- 报告页增加岗位相关反馈区，展示岗位匹配表现、优势、不足和改进建议。
- 历史页增加岗位定向标识与来源标识，便于区分普通模拟面试与岗位定向模拟面试。

## 6. 后端实现方案
- 扩展创建会话请求参数，支持 `jobTargeted`、`resumeTaskId`、`jdText`、`useLatestJobMatch`、`jobMatchRecordId`。
- 在 `InterviewService` 中统一解析岗位定向上下文，普通模拟面试与岗位定向模拟共用原有主链路。
- AI 开场、追问、流式回复、评估报告均复用既有 `InterviewAiService`，仅追加岗位定向上下文。
- 评估报告生成后，从同一份结构化结果中提取岗位反馈并回写，避免另起一套 AI 链路。
- 若没有可用 JD，则自动回退为原有普通模拟面试，不强制依赖 JD。

## 7. 数据存储方案
- 采用独立表 `mock_interview_job_target_record`，避免直接改动 `interview_session` 主表。
- 独立表保存：
  - `user_id`
  - `session_id`
  - `resume_task_id`
  - `jd_text`
  - `job_match_record_id`
  - `generated_questions`
  - `job_targeted_feedback`
  - `source_type`
- 历史与结果回显时，通过 `session_id` 查询岗位定向上下文和反馈。

## 8. AI 调用与 Mock 实现说明
- 真实 AI：在既有 `InterviewAiServiceImpl` 中补充岗位 JD、简历核心经历、已匹配关键词、缺失关键词、优化建议等上下文。
- Mock AI：同步补齐岗位定向开场、岗位相关追问、岗位反馈结构，保证本地开发与测试可运行。
- 普通模拟面试仍按原逻辑调用，不会因为未提供 JD 而报错。

## 9. stage 更新说明
- 开发前已将功能二标记为“已完成、已验收通过”，将功能三标记为“开发中”。
- 本轮完成后已将 `runtime/STATE.md` 更新为：
  - 功能三“岗位定向模拟面试”已完成
  - 当前状态为等待人工验收

## 10. 编译结果
- 命令：`mvn.cmd -q -DskipTests compile`
- 结果：通过

## 11. 构建结果
- 命令：`npm.cmd run build`
- 结果：通过
- 说明：前端首次在沙箱内构建时触发 `esbuild spawn EPERM`，属于环境权限限制；在获批后使用无沙箱构建验证通过，不属于本轮功能缺陷。

## 12. 当前功能验收说明
- 普通模拟面试仍可正常创建、问答、结束和查看报告。
- 用户可在面试开始前开启岗位定向模拟。
- 用户可手动输入 JD，也可复用最近一次 JD 对比结果。
- 面试问题生成链路已支持结合简历、JD 与 JD 对比结果上下文。
- 评估报告中已增加岗位相关反馈区。
- 岗位定向上下文和反馈已独立落库，刷新和历史查看时不会丢失关键岗位信息。
- 本轮未扩展社区、真题库、下载报告、视频分析等范围外功能。

## 13. 停止，不继续下一个功能
- 本轮仅完成“岗位定向模拟面试”最小增量开发，到此停止，等待人工验收。
