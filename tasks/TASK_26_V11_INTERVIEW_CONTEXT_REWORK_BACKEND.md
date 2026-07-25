# TASK_26_V11_INTERVIEW_CONTEXT_REWORK_BACKEND

## 1. 当前任务所属模块
- V1.1 第三个功能返修：岗位定向模拟面试 / 普通模拟面试简历上下文修复

## 2. 问题原因定位
- 普通模拟面试创建会话时，后端只在 `jobTargeted=true` 时解析上下文，导致首轮问题经常拿不到简历。
- 普通模拟面试后续追问时，Mock / Controller 链路没有给 AI 兜底传最近一次简历上下文。
- 岗位定向模拟读取会话上下文时只回查了 `resumeTaskId`、`jdText`、关键词等字段，没有把 `resumeText` 一并恢复，真实 AI Prompt 因此误判为“无简历模式”。
- `InterviewService` 返回结构直接使用会话表中的 `interviewMode`，历史旧数据里 `jobTargeted=true` 仍可能展示为 `normal / 普通面试`。
- 真实 AI Prompt 与 Mock AI 文案约束不够强，容易生成多问题脚本式首轮内容，或者出现“看不到简历”类错误话术。

## 3. 前端修改文件定位
- `frontend/app/src/api/interview.js`
- `frontend/app/src/views/interview/InterviewEntryView.vue`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/views/interview/InterviewHistoryView.vue`
- `frontend/app/src/views/interview/InterviewReportView.vue`

## 4. 后端修改文件定位
- `server/src/main/java/com/airesume/server/common/constants/InterviewConstants.java`
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/main/java/com/airesume/server/dto/interview/CreateSessionRequest.java`
- `server/src/main/java/com/airesume/server/dto/interview/InterviewHistoryResponse.java`
- `server/src/main/java/com/airesume/server/dto/interview/InterviewSessionResponse.java`
- `server/src/main/java/com/airesume/server/mock/MockInterviewService.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/MockInterviewJobTargetService.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/MockInterviewJobTargetServiceImpl.java`

## 5. 本轮修改文件清单
- `server/src/main/java/com/airesume/server/common/constants/InterviewConstants.java`
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/main/java/com/airesume/server/dto/interview/CreateSessionRequest.java`
- `server/src/main/java/com/airesume/server/dto/interview/InterviewHistoryResponse.java`
- `server/src/main/java/com/airesume/server/dto/interview/InterviewSessionResponse.java`
- `server/src/main/java/com/airesume/server/mock/MockInterviewService.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/service/MockInterviewJobTargetService.java`
- `server/src/main/java/com/airesume/server/service/impl/InterviewAiServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/impl/MockInterviewJobTargetServiceImpl.java`
- `frontend/app/src/api/interview.js`
- `frontend/app/src/views/interview/InterviewEntryView.vue`
- `frontend/app/src/views/interview/InterviewSessionView.vue`
- `frontend/app/src/views/interview/InterviewHistoryView.vue`
- `frontend/app/src/views/interview/InterviewReportView.vue`
- `docs/api/TASK_05_INTERVIEW_API.md`
- `docs/api/API_INDEX.md`
- `stage.md`
- `frontend/runtime/STATE.md`
- `tasks/TASK_26_V11_INTERVIEW_CONTEXT_REWORK_BACKEND.md`
- `frontend/tasks/TASK_26_V11_INTERVIEW_CONTEXT_REWORK_FRONTEND.md`

## 6. 普通模拟面试上下文修复说明
- `MockInterviewJobTargetServiceImpl.resolveContext()` 不再只处理岗位定向请求；普通模拟面试会优先复用显式 `resumeTaskId`，否则兜底查询最近一次已完成简历诊断。
- `InterviewEntryView.vue` 在普通模拟面试场景也会透传 `resumeTaskId`，减少“明明是从简历结果页进入却没带上任务 ID”的误差。
- `InterviewService.sendMessage()` 与流式控制器在会话无岗位定向上下文时，会额外调用 `resolveLatestResumeContext()`，保证普通模拟面试后续多轮也能继续围绕简历追问。
- 普通模拟面试如果确实没有简历，则仍按岗位通用面试逻辑提问，不编造项目经历。

## 7. 岗位定向模拟面试上下文修复说明
- 岗位定向请求会优先使用 `resumeTaskId`，其次复用 JD 对比记录中的 `resumeTaskId`，仍为空时再回退最近一次已完成简历诊断。
- 岗位定向会话详情读取时会重新恢复 `resumeText`，避免只拿到 `resumeTaskId` 却丢失简历正文。
- 当岗位定向请求没有可用 JD 时，后端会自动回退普通模拟面试，但仍尽量保留简历上下文，不会直接降成无简历模式。
- 如果简历偏前端而 JD 偏测试，Prompt 与 Mock AI 都会优先从前端项目切入，再映射到测试能力验证，而不是忽略简历。

## 8. AI Prompt 修复说明
- 真实 AI 默认 Prompt 新增强约束：只允许每轮一个主问题、禁止脚本提示、禁止占位符、禁止“看不到简历/无法查看简历”等错误话术。
- 普通模拟面试 + 有简历：Prompt 明确要求“已看过简历”，优先围绕项目、技术、实习、经历提问，再结合岗位方向追问。
- 岗位定向模拟 + 简历 + JD：Prompt 明确要求以“简历 + JD”为共同依据，更偏向目标岗位能力验证。
- 岗位定向模拟 + 有 JD 无简历：Prompt 明确要求围绕 JD 提问，但不能假装看过简历。
- 对旧会话只存 `resumeTaskId` 的场景，`InterviewAiServiceImpl` 会在构造 Prompt 时回查数据库中的 `resume_text`，防止误判成无简历。

## 9. Mock AI 修复说明
- `MockInterviewService.generateMockOpening()` 改为单问题输出，不再生成欢迎词和长脚本。
- 普通模拟面试 + 有简历时，Mock 首轮和追问会从简历里的项目/经历锚点切入，不再只给泛化问题。
- 岗位定向模拟 + 简历 + JD 时，Mock 会从简历锚点切入，再把问题映射到 JD 关键词或缺口能力。
- 岗位定向模拟 + 有 JD 无简历时，Mock 只围绕 JD 提问，不会伪造简历项目。

## 10. 返回结构修复说明
- 新增 `InterviewConstants.MODE_JOB_TARGETED = job_targeted` 作为岗位定向模拟的统一返回值。
- 创建岗位定向会话时会把 `interviewMode` 持久化为 `job_targeted`。
- 即使历史旧数据里仍存的是 `normal`，`InterviewService` 也会根据 `jobTargeted=true` 在响应层统一改写为：
  - `interviewMode = job_targeted`
  - `interviewModeDesc = 岗位定向模拟`
- 普通模拟面试保持：
  - `interviewMode = normal` 或 `stress`
  - `interviewModeDesc = 普通面试` 或 `压力面试`
  - `jobTargeted = false`

## 11. stage 更新说明
- 已更新项目根目录 `stage.md`：
  - 当前版本改为 `V1.1`
  - 当前阶段改为“V1.1 第三阶段返修：模拟面试上下文修复”
  - 状态改为“返修完成，等待人工复验”
- 已更新 `frontend/runtime/STATE.md`：
  - 记录前端配合返修已完成
  - 当前状态改为“等待人工复验”

## 12. task 更新说明
- 新增后端任务记录：`tasks/TASK_26_V11_INTERVIEW_CONTEXT_REWORK_BACKEND.md`
- 新增前端任务记录：`frontend/tasks/TASK_26_V11_INTERVIEW_CONTEXT_REWORK_FRONTEND.md`
- 已同步更新接口文档：`docs/api/TASK_05_INTERVIEW_API.md`、`docs/api/API_INDEX.md`
- 项目根目录未找到用户指定的 `DEVELOPMENT_RULES.txt`，本轮实际按以下规则文件执行：
  - `runtime/RULES.md`
  - `runtime/TASK_FLOW_RULES.md`
  - `runtime/COMMENT_RULES.md`
  - `runtime/LOG_RULES.md`
  - `runtime/API_DOC_RULES.md`
  - `frontend/runtime/RULES.md`

## 13. 编译结果
- 命令：`mvn.cmd -q -DskipTests compile`
- 结果：通过

## 14. 构建结果
- 命令：`npm.cmd run build`
- 结果：通过

## 15. 自测场景与结果
- 场景一：普通模拟面试 + 有简历
  - 结果：通过代码链路核对。创建会话会优先拿显式 `resumeTaskId` 或最近一次已完成简历，首轮 Prompt 与后续追问都会带简历上下文。
- 场景二：普通模拟面试 + 无简历
  - 结果：通过代码链路核对。`buildGeneralResumeContext()` 取不到简历时返回空上下文，真实 AI / Mock AI 都会退回岗位通用问题，且禁止编造简历。
- 场景三：岗位定向模拟 + 有简历 + 测试工程师 JD
  - 结果：通过代码链路核对。岗位定向上下文会同时带入 `resumeText + jdText + matchedKeywords + missingKeywords`，Prompt 明确要求从简历经历切入再映射到测试能力。
- 场景四：岗位定向模拟 + 有 JD + 无简历
  - 结果：通过代码链路核对。岗位定向上下文仍保留 JD，Prompt 与 Mock AI 都只围绕 JD 提问，不再假装看过简历。

## 15A. 本轮补充返修记录（2026-04-29）
- 依据用户提供的原始 PDF：`uploads/resumes/1777395102739_林映文 - 前端开发工程师求职简历.pdf`
- 依据用户提供的调试日志：`re.txt`
- 现象确认：
  - 多轮流式追问阶段虽然 `jobRoleCode=frontend_engineer`，但系统 Prompt 实际退化成了“岗位：软件工程师”。
  - 简历上下文直接用长原文截断拼接，结构不够清晰，不利于模型稳定识别“技能栈优先、项目后切入”的面试节奏。
  - 普通模拟面试开场过于直接切项目细节，不符合“先聊技术栈，再切项目”的预期。
- 补充修复：
  - 新增会话岗位名兜底解析，优先从 `interview_session` 读取真实岗位，其次按 `jobRoleCode` 映射，避免多轮对话退化成“软件工程师”。
  - 简历上下文改为 `AiInputCompressor` 结构化摘要后再拼 Prompt，减少原文截断造成的信息不完整感。
  - 开场 Prompt 调整为：技术岗位前 1 到 2 轮优先从简历中的核心技术栈、基础能力或工程化经验热身，再自然切入项目经历。

## 15B. 本轮补充返修记录（2026-04-29，流式可用性）
- 新增问题来源：
  - 模拟对话阶段真实 AI 流式请求命中 `WebClientRequestException`，控制台报错为 `Failed to resolve 'api.siliconflow.cn' [A(1), AAAA(28)] after 2 queries`。
  - 该异常发生在 `InterviewAiServiceImpl.generateReplyStream()` 的外部 AI 流式调用阶段，原逻辑会直接向 SSE 下游抛错，导致前端无法继续面试。
- 根因确认：
  - 当前真实 AI 流式链路只有 `sink.error(t)`，没有本地兜底。
  - 一旦外部 AI DNS / 网络失败，`InterviewController -> InterviewAiServiceImpl -> InterviewService.subscribeAndWriteStream()` 整条面试链路会直接进入错误事件。
- 本轮补充修复：
  - `InterviewAiServiceImpl` 注入 `MockInterviewService`，将真实 AI 失败后的兜底能力统一复用到 real 模式。
  - `generateOpening()` 在真实 AI 失败时自动降级到本地 Mock 开场问题，避免创建会话时被外部网络阻断。
  - `generateReply()` 在真实 AI 多轮问答失败时自动降级到本地 Mock 单问题追问，避免非流式链路直接报错。
  - `generateReplyStream()` 在外部 AI 尚未输出任何内容前若发生 DNS / 网络异常，会自动把本地 Mock 回复按字符流输出给 SSE 下游，保证面试不中断。
  - `generateReplyStream()` 在已经输出部分内容后若再次发生网络异常，则保留已有输出并正常结束流，避免前端再次进入错误状态。
- 本轮补充验证：
  - 后端重新执行 `mvn.cmd -q -DskipTests compile` 通过。
  - 前端重新执行 `npm.cmd run build` 通过。

## 15C. 本轮补充返修记录（2026-05-21，岗位上下文缓存空值）
- 新增问题来源：
  - 用户点击模拟面试会话时，后端日志出现 `Cache 'interview:jobTarget' does not allow 'null' values`。
  - 异常发生在 `MockInterviewJobTargetServiceImpl.getSessionContext()` 查询普通模拟面试会话上下文时。
- 根因确认：
  - 普通模拟面试没有 `mock_interview_job_target_record` 记录，方法按业务语义返回 `null`。
  - 该方法使用 `@Cacheable(value = "interview:jobTarget")`，但未声明空结果不缓存，Redis 缓存配置又禁止写入 `null`，因此 Spring Cache 在写缓存阶段抛出系统异常。
  - `RedisCacheManager` 原先启用了 `transactionAware()`，缓存写入被推迟到事务提交后的 `afterCommit` 执行，绕过了项目已有 `RedisCacheErrorHandler`，导致缓存异常升级为接口 500。
- 本轮补充修复：
  - 为 `getSessionContext()` 的 `@Cacheable` 增加 `unless = "#result == null"`，查不到岗位定向上下文时不写入 Redis。
  - 移除 `RedisCacheManager.transactionAware()`，让缓存写入异常回到 `RedisCacheErrorHandler` 统一降级路径，避免性能缓存故障影响业务接口。
  - 新增 `MockInterviewJobTargetServiceImplTest.shouldNotCacheNullSessionContext()`，通过反射校验注解约束，防止后续删除该条件导致普通会话再次报错。
  - 新增 `RedisConfigTest.shouldNotWrapCachesWithTransactionAwareDecorator()`，校验缓存不再被事务感知装饰器包装，防止 `afterCommit` 缓存异常绕过错误处理。
- 本轮补充验证：
  - 后端目标测试：`mvn.cmd -q "-Dtest=RedisConfigTest,MockInterviewJobTargetServiceImplTest,InterviewServiceTest" test` 通过。
  - 后端编译：`mvn.cmd -q -DskipTests compile` 通过。

## 15D. 本轮补充返修记录（2026-05-21，评价报告 JSON 截断兜底）
- 新增问题来源：
  - 用户点击模拟面试生成报告后，接口返回 `code=200`，但 `evaluationReport` 为“系统未能生成评价报告，请稍后重试或查看原始对话记录。”的默认报告。
  - `debug.txt` 显示 AI 调用成功且返回长度 4933，但 `InterviewAiServiceImpl.parseEvaluationResponse()` 报 `JsonEOFException: Unexpected end-of-input`，失败 JSON 在 `communication` 对象附近缺少闭合结构。
- 根因确认：
  - 评价报告 prompt 字段较多、结构较长，当前模型可能返回半截或结构不闭合 JSON。
  - 原解析失败逻辑直接返回默认 60 分报告，并继续打印“评价报告生成完成”，导致失败结果被当作成功报告写入和展示。
- 本轮补充修复：
  - `parseEvaluationResponse()` 在解析失败时改为抛出 `IllegalStateException`，避免把损坏 AI JSON 静默伪装成有效报告。
  - 非流式评价报告请求增加 `max_tokens=8192`，降低报告输出被截断概率。
  - `InterviewService.buildFallbackEvaluationReport()` 增加失败原因入参，兜底报告的 `finalVerdict` 明确标识“AI 深度报告生成失败，当前为基础评估”。
  - 新增 `InterviewAiServiceImplTest.parseEvaluationResponseShouldRejectBrokenJson()`，验证截断 JSON 会失败而不是返回默认报告。
  - 更新 `InterviewServiceTest.buildFallbackEvaluationReportShouldIncludeDeepAnalysisFields()`，验证兜底报告保留失败原因。
- 本轮补充验证：
  - 后端目标测试：`mvn.cmd -q "-Dtest=InterviewAiServiceImplTest,InterviewServiceTest" test` 通过。
  - 后端编译：`mvn.cmd -q -DskipTests compile` 通过。
## 15E. 本轮补充返修记录（2026-05-21，岗位上下文负缓存）
- 新增问题来源：
  - `debug.txt` 显示模拟面试报告生成期间，前端每 3 秒轮询会话详情。
  - 普通模拟面试没有 `mock_interview_job_target_record` 记录，但每次轮询都会重新查询该表，且结果均为 `Total: 0`。
- 根因确认：
  - 上一轮为避免 Redis 写入 `null` 报错，`getSessionContext()` 使用了 `unless = "#result == null"`。
  - 该策略能避免空值缓存异常，但空结果不会进入 Redis，导致普通模拟面试详情轮询持续穿透数据库。
- 本轮补充修复：
  - `MockInterviewJobTargetServiceImpl.getSessionContext()` 查不到记录时返回 `jobTargeted=false`、`sourceType=none` 的空上下文，不再返回 `null`。
  - 移除 `@Cacheable` 的 `unless = "#result == null"`，让空上下文可被缓存，形成负缓存。
  - 保留 `saveSessionContext()` 的缓存驱逐逻辑，真实岗位定向上下文保存后会清理旧的空上下文缓存。
  - `InterviewService.resolveConversationContext()` 与流式控制器追问链路把无简历内容的空上下文继续视为缺失，仍会回退最近简历上下文，避免普通面试追问质量回退。
  - 更新 `MockInterviewJobTargetServiceImplTest`，验证缓存注解不再跳过空结果，并验证查无记录时返回非空上下文。
  - 更新 `InterviewServiceTest`，验证非流式追问遇到空上下文时仍会回退最近简历上下文。
- 本轮补充验证：
  - 后端目标测试：`mvn.cmd -q "-Dtest=MockInterviewJobTargetServiceImplTest,InterviewServiceTest" test` 通过。
  - 后端编译：`mvn.cmd -q -DskipTests compile` 通过。
## 16. 当前返修验收说明
- 普通模拟面试在有简历时，会优先围绕简历经历提问。
- 普通模拟面试在无简历时，会回退到通用岗位面试。
- 岗位定向模拟在有简历和 JD 时，会围绕“简历 + JD”提问，并更偏向目标岗位能力验证。
- 岗位定向模拟在简历与 JD 不匹配时，会从简历经历切入并考察可迁移能力。
- 真实 AI Prompt 与 Mock AI 都已禁止输出“看不到简历”“等候选人回答后”“[具体模块]”等错误话术。
- 岗位定向模拟的返回模式与描述已和普通面试明确区分。
- 普通模拟面试多轮追问阶段已不再把前端岗位退化成“软件工程师”。
- 技术岗开场已调整为先问技术栈/基础能力，再切项目，不再一上来就深挖项目模块。
- 真实 AI 在 `api.siliconflow.cn` DNS / 网络失败时，流式模拟面试会自动降级到本地 Mock 继续对话，不再直接报错中断。
- 普通模拟面试没有岗位定向上下文时，不再把 `null` 写入 Redis 缓存，点击会话详情不会触发 `interview:jobTarget` 空值缓存异常。
- 本轮未改动 JD 对比分析和 AI 简历润色的数据结构与调用协议。

## 17. 停止，不继续下一个功能
- 本轮仅完成模拟面试上下文返修，到此停止，等待人工复验。
