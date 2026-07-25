# 当前开发状态

## 社区与个人动态虚拟滚动与 DTO 类加载修复（2026-05-23）
- 当前阶段：本轮已完成编码和验证，等待人工验收。
- 已完成内容：前端引入 `vue-virtual-scroller`，社区首页帖子流和个人动态中心 8 个标签列表均接入 `DynamicScroller` / `DynamicScrollerItem`，保留原有分页、加载更多和交互逻辑。后端去除 `MyCommentVO` 对 Lombok Builder 内部类的运行依赖，并为 `ReceivedInteractionVO` 显式补充 Builder，避免热更新或增量编译下出现 `NoClassDefFoundError`。
- 交互边界：本轮不改为瀑布流，不新增社区业务能力，不改接口 URL 或响应结构。
- 后端验证：`mvn.cmd test "-Dtest=CommunityServiceInteractionTest,CommunityServiceReceivedInteractionsEmptyTest,CommunityServicePostQueryDeleteTest"` 通过，19 个测试通过。
- 前端验证：`npm.cmd test -- --run src/__tests__/views/community/MyActivity.test.js src/__tests__/views/community/CommunityView.test.js` 通过，14 个测试通过；`npm.cmd run build` 通过。
- 关联任务文件：`tasks/fixes/TASK_COMMUNITY_ACTIVITY_VIRTUAL_SCROLL_AND_DTO_BUILDER_2026_05_23_BACKEND.md`、`frontend/tasks/TASK_COMMUNITY_ACTIVITY_VIRTUAL_SCROLL_2026_05_23_FRONTEND.md`。
- 停止说明：本轮只处理社区/个人动态列表虚拟滚动和当前报错，不继续扩展下一项功能。

## 登录态过期鉴权边界与管理端错误提示去重（2026-05-23）
- 当前阶段：本轮已完成编码和验证，等待人工验收。
- 已完成内容：后端收紧 `/api/auth/**` 匿名访问范围，仅保留注册、登录、找回密码和安全问题查询公开；`/api/auth/me`、改密和修改安全问题等登录态接口在 JWT 过期时由 Security 返回 401，不再进入 Controller 触发空指针。前端管理端请求层不再对普通业务错误和非 401 HTTP 错误直接弹窗，交由页面 catch 展示；401 会话失效仍统一清理、跳转和提示；管理端错误提示增加短时间相同文案去重，避免账号或密码错误等场景重复弹两次。
- 交互边界：本轮不新增刷新 token 流程，不改变现有接口 URL 和响应结构，不调整管理端页面布局或业务能力。
- 后端验证：`mvn.cmd test "-Dtest=SecurityConfigTest"` 通过，4 个测试通过；`mvn.cmd test` 通过，512 个测试通过。
- 前端验证：`npm.cmd test -- --run src/__tests__/utils/adminFeedback.test.js src/__tests__/utils/adminRequest.test.js` 通过，4 个测试通过；`npm.cmd test -- --run src/__tests__/views/SettingsView.test.js` 通过，25 个测试通过；`npm.cmd test` 通过，40 个测试文件 / 246 个测试通过；`npm.cmd run build` 通过。
- 关联任务文件：`tasks/fixes/TASK_AUTH_TOKEN_EXPIRED_AND_ADMIN_FEEDBACK_2026_05_23_BACKEND.md`、`frontend/tasks/TASK_ADMIN_FEEDBACK_DEDUPE_2026_05_23_FRONTEND.md`。
- 停止说明：本轮只处理登录态过期 500 和管理端重复错误提示问题，不继续扩展认证体系或管理端功能。

## 成长配置接入用户端成长中心（2026-05-22）
- 当前阶段：本轮已完成编码和验证，等待人工验收。
- 已完成内容：
  - `GrowthOverviewResponse` 新增 `growthConfig`，包含激励文案列表和里程碑配置列表。
  - `GrowthServiceImpl` 读取管理端 `sys_growth_config` 的 `encouragement` 与 `milestone` 分组，随成长概览接口返回给用户端。
  - `AdminGrowthConfigController` 在新增、更新、删除、批量删除成长配置后清理 `user:growthOverview` 缓存，避免配置变更后用户端继续展示旧数据。
  - `GrowthCenterView.vue` 在有配置时展示“成长激励”和“成长里程碑”，无配置时不占位。
  - 新增后端成长配置接入和 Redis 序列化回归测试；新增前端成长中心配置渲染测试。
- 本轮不做的边界：
  - 不新增数据库表或字段。
  - 不实现成就达成规则、积分、徽章或动态完成状态。
  - 不改管理端成长配置表单结构。
- 验证结果：
  - 后端目标测试：`mvn.cmd -q "-Dtest=GrowthServiceImplTest,RedisSerializationTest,AdminGrowthConfigControllerTest" test` 通过。
  - 前端目标测试：`npm.cmd test -- --run src/__tests__/views/GrowthCenterView.test.js` 通过。
  - 前端构建：`npm.cmd run build` 通过。
- 关联任务文件：
  - `tasks/TASK_56_GROWTH_CONFIG_INTEGRATION_BACKEND.md`
  - `frontend/tasks/TASK_56_GROWTH_CONFIG_INTEGRATION_FRONTEND.md`
- 停止说明：本轮只完成成长配置展示闭环，不继续扩展成长中心其它能力。

## 新手引导改任务式（2026-05-21）
- 当前阶段：本轮已完成，等待人工验收。
- 已完成内容：
  - 新增 `user_onboarding_task` 独立表和迁移脚本，每个用户每个任务一行记录。
  - 新增 `UserOnboardingTask` 实体和 `UserOnboardingTaskMapper`。
  - 新增 `OnboardingTasksResponse` DTO（含 TaskItem 内部类）和 `OnboardingTaskCompleteRequest` DTO。
  - `UserOnboardingService` 新增 `getTasks()` 和 `completeTask()` 接口和实现：
    - 4 个任务定义：上传简历、查看报告、岗位匹配、完成面试。
    - 旧引导已完成/跳过用户返回 visible=false，卡片不展示。
    - 幂等完成 + DuplicateKeyException 并发兜底。
  - `UserOnboardingController` 新增 `GET /api/user/onboarding/tasks` 和 `POST /api/user/onboarding/tasks/complete`。
  - `UserAccountServiceImpl` 注销时同步逻辑删除 `user_onboarding_task`。
  - 前端 `onboarding.js` 新增 `getOnboardingTasks()` 和 `completeOnboardingTask(taskKey)` API。
  - 新建 `OnboardingTaskCard.vue` 组件：标题+环形进度条+4个任务行+行动按钮。
  - `DashboardView.vue` 在数据概览和成长中心入口之间集成任务卡片，visible && !allCompleted 时展示。
  - 4 个核心页面静默上报：UploadView(resume_uploaded)、ResultView(report_viewed+jd_compared)、InterviewReportView(interview_completed)。
- 本轮不做的边界：
  - 不删除 OnboardingGuide 模态框，保持现有 UI tour 可从设置页重新查看。
  - 不做积分系统、会员券发放、运营活动后台。
  - 不改 `user_onboarding_state` 表结构。
- 数据存储：新增 `user_onboarding_task` 表，新增迁移脚本 `TASK_55_USER_ONBOARDING_TASK.sql`；同步更新 `db/schema.sql` 和 `server/db/schema.sql`。
- 验证结果：
  - 后端编译：`mvn clean compile` 通过。
  - 后端目标测试：`mvn test -Dtest=UserOnboardingServiceImplTest,UserOnboardingControllerTest,UserAccountServiceImplTest` 通过，18 个用例通过。
  - 前端构建：`npm run build` 通过。
- 关联任务文件：
  - `tasks/TASK_55_ONBOARDING_TASK_BASED_BACKEND.md`
  - `frontend/tasks/TASK_55_ONBOARDING_TASK_BASED_FRONTEND.md`
- 停止说明：本轮只实施 Feature 5 新手引导改任务式，不继续实施其他功能。

## 第二轮审查务实修复（2026-05-21）
- 当前阶段：本轮已完成，等待人工验收。
- 已完成内容：
  - `resumeDocxExport.js` 在 detached DOM 写入前统一经过 DOMPurify 消毒，降低富文本导出链路的 XSS 风险。
  - `ResultView.vue` 图片导出下载链路改为 `try/finally` 释放 Object URL。
  - `GrowthCenterView.vue` 去除本轮新增的生产 `console.error`，失败时改为更新页面状态。
  - `OnboardingTaskCard.vue` 对 `totalCount <= 0` 的环形进度做防护，避免 `stroke-dashoffset` 产生 `NaN`。
  - `InterviewService` 用 `CacheManager` 按逻辑缓存名驱逐 `user:interviewRadar` 与 `user:growthOverview`，不再依赖 Redis 物理 key 拼接格式。
  - `TASK_54_INTERVIEW_DIMENSION_SCORE.sql`、`db/schema.sql`、`server/db/schema.sql` 补充唯一索引中文注释，明确逻辑删除后的幂等写入约束。
  - `GrowthServiceImplTest` 增补正向雷达、盲区提示和成长概览场景；`ResumeDiagnosisProcessorTest` 提取公共 mock 初始化；`InterviewService` 收敛重复布尔判断；`GrowthServiceImpl` 去掉 `java.util.*`。
- 本轮不做的边界：
  - 不拆分 `InterviewService` 大文件。
  - 不删除 schema 快照或 migration 双路径文件。
  - 不新增 API 字段，不改变前端交互能力。
- 验证结果：
  - 后端目标测试：`mvn.cmd test -Dtest=InterviewServiceTest,GrowthServiceImplTest,ResumeDiagnosisProcessorTest` 通过，34 个用例通过。
  - 前端目标测试：`npm.cmd test -- --run src/__tests__/utils/resumeDocxExport.test.js src/__tests__/views/ResumeResultView.test.js src/__tests__/components/OnboardingTaskCard.test.js` 通过，28 个用例通过。
  - 后端全量测试：`mvn.cmd test` 通过，459 个用例通过。
  - 前端构建：`npm.cmd run build` 通过。
- 关联任务文件：
  - `tasks/TASK_54_INTERVIEW_DIMENSION_RADAR_BACKEND.md`
  - `frontend/tasks/TASK_53_RESUME_EXPORT_DOCX.md`
  - `frontend/tasks/stage.md`
- 停止说明：本轮只处理第二轮审查中已确认且低风险的问题，不继续扩展其他能力。

## 面试维度雷达 + 盲区提示（2026-05-21）
- 当前阶段：本轮已完成，等待人工验收。
- 已完成内容：
  - 新增 `interview_dimension_score` 独立表和迁移脚本，一条面试产生 6 行维度评分记录。
  - 新增 `InterviewDimensionScore` 实体和 `InterviewDimensionScoreMapper`。
  - 新增 `InterviewRadarResponse` DTO，包含雷达数据、维度趋势和盲区提示。
  - `GrowthService` 新增 `getInterviewRadar()` 接口和实现：
    - 查询最近已结束且有评估报告的面试会话。
    - 雷达读取路径只查询维度评分表，不在缓存读取中做回填写库。
    - 构建最新 session 的 6 维度雷达数据。
    - 构建各维度趋势折线（按时间正序）。
    - 盲区分析：近 3 次均分 < 60 → 持续低分；最新比上次下降 > 5 且 < 70 → 下滑趋势。
    - 每个盲区维度附带固定改进建议文案。
  - `InterviewService.generateAndPersistEvaluationReport()` 报告落库后同步写入 6 维度评分到 `interview_dimension_score`，并清除雷达和成长概览缓存。
  - `clearHistory` / `deleteSession` 同步逻辑删除维度评分并清除 `user:interviewRadar`、`user:growthOverview` 缓存。
  - `GrowthController` 新增 `GET /api/user/growth/interview-radar` 端点。
  - 前端 `growth.js` 新增 `getInterviewRadar()` API。
  - `LineChart.vue` 新增 `showLegend` prop（向后兼容，默认 false）。
  - `GrowthCenterView.vue` 在折线图区域和详情卡片之间新增"面试维度雷达"区块：
    - 雷达图 + 维度详情面板（复用 `RadarChart` 和 `RadarScorePanel`）。
    - 维度趋势折线（6 色折线，showLegend=true）。
    - 盲区提示卡片列表（持续低分红色 / 下滑趋势橙色）。
    - 无数据时展示引导文案和面试入口。
- 本轮不做的边界：
  - 不改 AI Prompt：维度评分结构已由现有 Prompt 生成。
  - 不改面试主流程：不影响创建/追问/结束会话。
  - 不改现有 overview 接口。
  - 不做题库、收藏、热点、命中率统计。
  - 不做目标岗位画像虚线。
- 数据存储：新增 `interview_dimension_score` 表，新增迁移脚本 `TASK_54_INTERVIEW_DIMENSION_SCORE.sql`；同步更新 `db/schema.sql` 和 `server/db/schema.sql`。
- 验证结果：
  - 后端编译：`mvn clean compile` 通过。
  - 前端构建：`npm run build` 通过。
  - 审查修复回归：`mvn.cmd test -Dtest=ResumeDiagnosisProcessorTest,InterviewServiceTest,GrowthServiceImplTest` 通过，32 个用例通过。
  - 后端全量测试：`mvn.cmd test` 通过，457 个用例通过。
- 关联任务文件：
  - `tasks/TASK_54_INTERVIEW_DIMENSION_RADAR_BACKEND.md`
  - `frontend/tasks/TASK_54_INTERVIEW_DIMENSION_RADAR_FRONTEND.md`
- 停止说明：本轮只实施 Feature 4 面试维度雷达 + 盲区提示，不继续实施任务式新手引导或其他功能。

## 简历导出 DOCX 快速止血方案（2026-05-21）
- 当前阶段：本轮已完成，等待人工验收。
- 已完成内容：
  - 新增 `docx` npm 依赖，客户端生成可编辑 Word 文件。
  - `vite.config.js` 新增 `docx-vendor` chunk，docx 库独立打包（346kB / 101kB gzipped）。
  - 新增 `src/utils/resumeDocxExport.js` 核心模块：
    - `parseHtmlToDocxRuns(html)` 解析 HTML 富文本为 docx TextRun 配置，支持 bold/italic/br。
    - `convertBlockToParagraphs(block)` 覆盖 text/bullet/heading/row/label/banner_title/section_title 全部 block 类型。
    - `exportResumeToDocx(jsonString, filename)` 主入口：JSON → 验证 → 动态 import('docx') → Document 构建 → Packer.toBlob → 浏览器下载。
    - 样式：Microsoft YaHei 字体、A4 页面、1.27cm 边距、28pt 姓名/14pt section heading/11pt 正文。
  - `ResultView.vue` 新增"导出 Word"按钮（位于"导出 PDF"和"导出图片"之间），含 loading 状态和成功/失败提示。
  - 新增 24 个单元测试覆盖 HTML 解析、block 转换、错误处理、完整模型导出和下载异常时释放 Object URL。
- 本轮不做的边界：
  - 现有截图式 PDF（html2canvas + jsPDF）保持不动，不改进 PDF 文本可选、ATS 识别、分页稳定。
  - ExportToolbar.vue 模板编辑器的 DOCX 导出（不同数据模型，需单独转换器）不在本轮范围。
  - block.style 自定义字号/字重映射、简历照片嵌入、页眉页脚/页码均留后续轮次。
- 数据存储：不新增表、不修改字段、不新增迁移脚本；纯前端方案。
- 验证结果：
  - 前端 DOCX 导出单元测试：24 个测试通过。
  - 前端构建：`npm run build` 通过。
  - 前端全量测试回归：194 通过，3 个 SettingsView 预存在失败（与本轮无关）。
- 关联任务文件：
  - `frontend/tasks/TASK_53_RESUME_EXPORT_DOCX.md`
- 停止说明：本轮只实施 Feature 3 DOCX 快速止血方案，不继续实施 PDF 质量改造、成长中心雷达或任务式新手引导。

## 模拟面试岗位上下文缓存空值修复（2026-05-21）
- 当前阶段：本轮已完成，等待人工验收。
- 问题确认：普通模拟面试会话没有岗位定向上下文记录，`MockInterviewJobTargetServiceImpl.getSessionContext()` 返回 `null`；原 `@Cacheable` 未跳过空结果，Redis 禁止缓存 `null`，因此点击会话详情时触发 `Cache 'interview:jobTarget' does not allow 'null' values`。
- 已完成内容：
  - `MockInterviewJobTargetServiceImpl.getSessionContext()` 的 `@Cacheable` 增加 `unless = "#result == null"`，查不到岗位定向上下文时不写入 Redis。
  - 移除 `RedisCacheManager.transactionAware()`，避免缓存写入延迟到事务 `afterCommit` 后绕过 `RedisCacheErrorHandler`。
  - 新增 `MockInterviewJobTargetServiceImplTest.shouldNotCacheNullSessionContext()`，校验空结果不缓存约束，防止回归。
  - 新增 `RedisConfigTest.shouldNotWrapCachesWithTransactionAwareDecorator()`，校验缓存写入异常仍走项目已有降级处理链路。
- 数据存储：不新增表、不修改字段、不新增迁移脚本；仅修复缓存注解行为。
- 验证结果：
  - 后端目标测试：`mvn.cmd -q "-Dtest=RedisConfigTest,MockInterviewJobTargetServiceImplTest,InterviewServiceTest" test` 通过。
  - 后端编译：`mvn.cmd -q -DskipTests compile` 通过。
- 关联任务文件：
  - `tasks/TASK_26_V11_INTERVIEW_CONTEXT_REWORK_BACKEND.md`
- 停止说明：本轮只修复模拟面试会话点击时的 `interview:jobTarget` 空值缓存异常，不继续扩展模拟面试功能。

## 简历诊断进度可视化 + 失败重试（2026-05-21）
- 当前阶段：本轮已完成，等待人工验收。
- 已完成内容：
  - 数据库 `resume_diagnosis_task` 表新增 `stage` 字段（VARCHAR(32)，可空）。
  - 后端新增 3 个阶段常量（STAGE_EXTRACTING / STAGE_AI_ANALYZING / STAGE_ENHANCING）。
  - 后端新增 2 个错误码（RESUME_TASK_NOT_RETRYABLE(2011) / RESUME_TASK_RETRY_EXPIRED(2012)）。
  - Entity/DTO 新增 stage/stageDesc/errorMsg 字段。
  - Service 层实现 `updateStage` 和 `retryFailedTask`（校验归属+失败状态+24h时效）。
  - `ResumeDiagnosisProcessor` 在处理管道 3 个步骤间调用 `updateStage`。
  - Controller 新增 `POST /api/resume/task/{taskId}/retry` 端点。
  - 前端 `resume.js` 新增 `retryResumeTask(taskId)` API。
  - 前端 `ResultView.vue` 使用后端 stage 驱动 4 阶段进度展示，失败区增加"重新诊断"按钮。
  - 前端 `HistoryView.vue` 失败卡片显示 errorMsg + 内联重试按钮。
  - 前端 `errorMessages.js` 新增 2011/2012 错误码映射。
- 本轮 review 修复补充：
  - 新增 `failed_at` 字段和独立迁移脚本，重试 24h 窗口改按失败时间判断，历史数据无 `failed_at` 时兼容回退 `update_time`。
  - `updateStage` 仅允许更新 `PROCESSING` 任务，避免延迟阶段更新污染完成/失败任务。
  - `ResumeDiagnosisProcessor` 用户友好错误提示改为优先按 `BusinessException` / `ResultCode` 映射，AI 空响应改抛 `AI_RESPONSE_EMPTY`。
  - `SysUserServiceImpl.removeById` 增加 `sys_user` 缓存驱逐。
  - 前端移除 `ResultView.vue` 空 `onMounted`，并为 `HistoryView.vue` 重试响应增加空任务 ID 校验。
- 验证结果：
  - 后端编译：`mvn compile` 通过。
  - 后端测试：`mvn test` 通过，426 个测试通过。
  - 前端构建：`npm run build` 通过。
  - 本轮后端回归：`mvn.cmd -q "-Dtest=ResumeDiagnosisTaskServiceImplTest,ResumeDiagnosisProcessorTest,SysUserServiceImplTest" test` 通过。
  - 本轮前端回归：`npm.cmd test -- --run src/__tests__/views/ResumeResultView.test.js src/__tests__/views/ResumeHistoryView.test.js` 通过。
- 关联任务文件：
  - `tasks/TASK_52_RESUME_DIAGNOSIS_STAGE_AND_RETRY.md`
  - `frontend/tasks/TASK_52_RESUME_DIAGNOSIS_STAGE_AND_RETRY.md`
- 停止说明：本轮只实施 Feature 2，不继续实施 DOCX 导出、成长中心雷达或任务式新手引导。

## 错误码人性化（2026-05-20）
- 当前阶段：本轮已完成，等待人工验收。
- 已完成内容：
  - `ResultCode` 枚举从 9 个 HTTP 语义码扩展为 35 个业务域错误码，覆盖通用(1xxx)、简历(2xxx)、面试(3xxx)、AI(4xxx)、会员(5xxx)、管理端(6xxx) 六大模块。
  - `BusinessException` 新增 `BusinessException(ResultCode, String dynamicMessage)` 构造函数，支持保留动态中文消息。
  - `ResumeDiagnosisTaskServiceImpl` 替换 16 处英文/中文 BusinessException 为 ResultCode 枚举。
  - `UserQuotaServiceImpl` 替换 5 处英文/中文 BusinessException 为 ResultCode 枚举。
  - `MembershipServiceImpl` 替换 4 处英文 BusinessException 为 ResultCode 枚举。
  - `AiEngineConnectivityTestServiceImpl` 替换 4 处英文 BusinessException 为 ResultCode 枚举。
  - 前端新增 `errorMessages.js` 错误码映射表和 `getErrorMessage()` 查找函数。
  - 前端 `request.js` 响应拦截器增加错误码映射查找，映射命中时展示结构化中文提示。
  - 后端新增 3 个测试文件（ResultCodeTest、BusinessExceptionTest、GlobalExceptionHandlerTest），19 个测试。
  - 前端新增 1 个测试文件（errorMessages.test.js），11 个测试。
- 数据存储：不新增表，不修改字段，不新增迁移脚本。
- 验证结果：
  - 后端新增测试：19 个测试通过。
  - 后端完整测试：`mvn.cmd test` 通过，426 个测试通过。
  - 前端新增测试：11 个测试通过。
  - 前端构建：`npm.cmd run build` 通过。
- 关联任务文件：
  - `tasks/TASK_52_ERROR_CODE_HUMANIZATION_BACKEND.md`
  - `frontend/tasks/TASK_52_ERROR_CODE_HUMANIZATION_FRONTEND.md`
- 停止说明：本轮只实施 Feature 1 错误码人性化，不继续实施诊断进度条、失败重试、DOCX 导出、成长中心雷达或任务式新手引导。

## Pull 后代码审查安全修复（2026-05-20）
- 当前阶段：本轮已完成，等待人工验收。
- 已完成内容：
  - `ResumePdfController` 恢复 PDF HTML 入参大小限制和服务端净化，拦截危险标签、事件属性、远程 URL、`file:` URL 和非 `data:image` 的 CSS URL，避免 Headless Chrome 渲染未净化 HTML。
  - `JwtAuthenticationFilter` 移除 URL query token 兜底，只接受 `Authorization` 请求头，避免长期登录 token 暴露在 URL、历史记录和代理日志中。
  - `application.yml` 的 JWT secret 恢复为 `${JWT_SECRET:dev-secret-key-change-in-production-123456}`，不再使用远程 pull 带入的固定明文密钥。
  - 后端测试补充 PDF HTML 净化、超大 HTML 拦截和 query token 忽略场景。
- 数据存储：不新增表，不修改字段，不新增迁移脚本。
- 验证结果：
  - 后端目标测试：`mvn.cmd test "-Dtest=ResumePdfControllerTest,JwtAuthenticationFilterTest"` 通过，8 个测试通过。
  - 后端完整测试：`mvn.cmd test` 通过，402 个测试通过。
- 关联前端修复：见 `frontend/tasks/stage.md` 的“Pull 后代码审查前端修复”。
- 停止说明：本轮只修复 pull 后代码审查发现的 PDF 导出与 JWT 安全问题，不继续扩展 PDF 分享、下载票据、导出历史或认证体系改造。

## 管理端 AI 引擎连通测试（2026-05-19）
- 当前阶段：本轮已完成，等待人工验收。
- 已完成内容：
  - 新增管理端接口 `POST /api/admin/ai-engines/connectivity-test`，用于在保存前验证 AI 引擎配置是否可真实调用。
  - 连通测试不落库；新增态使用表单完整 API Key，编辑态 API Key 留空时使用数据库中已保存的真实密钥。
  - 测试请求继续执行公网 HTTPS 地址校验，拒绝本机、内网和云元数据地址。
  - 非法 baseUrl 会作为连通测试失败结果返回，管理端可直接展示“基础地址不合法”的具体原因，且不会发起外部请求。
  - 非 Mock provider 发送一次极小 token 的 `chat/completions` 请求；返回成功状态、耗时、响应摘要或失败原因。
  - Mock provider 不发外部请求，直接返回格式有效。
- 数据存储：不新增表，不修改字段，不新增迁移脚本。
- 验证结果：
  - 后端目标测试：`mvn.cmd test "-Dtest=AiEngineConnectivityTestServiceImplTest,AdminAiEngineConnectivityControllerTest,PublicHttpsUrlValidatorTest"` 通过，12 个测试通过。
  - 后端完整测试：`mvn.cmd test` 通过，396 个测试通过。
- 关联任务文件：
  - `tasks/TASK_51_ADMIN_AI_ENGINE_CONNECTIVITY_TEST_BACKEND.md`
  - `frontend/tasks/TASK_51_ADMIN_AI_ENGINE_CONNECTIVITY_TEST_FRONTEND.md`
- 停止说明：本轮只实现 AI 引擎配置连通测试，不新增供应商管理、不自动修复配置、不保存测试历史、不扩展运行时模型路由。

## 管理端 AI 引擎公网 HTTPS 地址校验修复（2026-05-19）
- 当前阶段：本轮已完成，等待人工验收。
- 已完成内容：
  - 新增 `PublicHttpsUrlValidator` 公共工具，统一管理 AI baseUrl 公网 HTTPS 校验规则。
  - 移除 `InterviewAiServiceImpl` 中硬编码 AI 服务商 host 白名单，Mimo 等新公网 HTTPS 服务商地址不再被白名单拒绝。
  - 显式配置非法 baseUrl 时直接抛错，不再静默回退到默认豆包地址。
  - 管理端 AI 引擎保存逻辑复用同一套校验规则，避免保存阶段和运行阶段口径不一致。
  - 新增单元测试覆盖公网 HTTPS、新服务商域名、HTTP、本机、内网、云元数据地址和 IPv4 映射 IPv6 内网地址。
- 数据存储：不新增表，不修改字段，不新增迁移脚本，继续使用 `sys_ai_engine_config.base_url`。
- 验证结果：
  - 后端目标测试：`mvn test "-Dtest=PublicHttpsUrlValidatorTest,InterviewAiServiceImplTest"` 通过，38 个测试通过。
  - 后端完整测试：`mvn test` 通过，394 个测试通过。
- 关联任务文件：
  - `tasks/TASK_50_ADMIN_AI_ENGINE_PUBLIC_HTTPS_BACKEND.md`
- 停止说明：本轮只修复管理端 AI 引擎 baseUrl 白名单导致 Mimo 无法连通的问题，不新增前端页面、不扩展模型供应商枚举或网络诊断能力。

## 模拟面试语音通话简历元信息外露修复（2026-05-19）
- 当前阶段：本轮已完成，等待人工验收。
- 已完成内容：
  - 修复 Mock 面试追问直接引用简历原文片段的问题，避免出现“围绕 xxx 求职简历姓名/性别...”这类内容。
  - Mock 面试现在只使用“项目经历 / 实习经历 / 工作经历 / 相关经历”等泛化表述，不把简历文件名、姓名、性别、电话、邮箱等元信息展示或播报给用户。
  - 真实 AI Prompt 移除带具体姓名的负例样本，并增加禁止展示或朗读简历元信息的硬约束。
  - 新增后端回归测试覆盖 Mock 输出和真实 AI Prompt 约束。
- 数据存储：不新增表，不修改字段，不新增迁移脚本。
- 验证结果：
  - 后端目标测试：`mvn.cmd test "-Dtest=MockInterviewServiceTest,InterviewAiServiceImplTest"` 通过，31 个测试通过。
  - 后端完整测试：`mvn.cmd test` 通过，381 个测试通过。
- 关联任务文件：
  - `tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_BACKEND.md`
- 停止说明：本轮只修复模拟面试语音通话中面试官回复不口语化、简历元信息外露并被 TTS 播报的问题，不继续扩展新的面试模式或语音服务。

## 模拟面试语音通话后端支撑（2026-05-19）
- 当前阶段：本轮已完成，等待人工验收。
- 已完成内容：
  - `interview_session` 新增 `interaction_type` 字段，记录会话创建时选择的文字/语音交互方式。
  - 创建会话时校验 `interactionType` 只允许 `0/1`，空值默认文字面试。
  - 会话详情和历史记录返回 `interactionType`，供前端识别语音会话。
  - `InterviewAiService` 调用链显式传入 `interactionType`，语音模式追加口语化、简洁、适合朗读的 Prompt 指令。
  - Mock AI 服务同步适配语音模式，便于本地联调。
- 数据存储：新增 `db/migrations/TASK_49_INTERVIEW_VOICE_INTERACTION.sql` 和 `server/db/migrations/TASK_49_INTERVIEW_VOICE_INTERACTION.sql`，并同步更新 `server/db/schema.sql`；不新增通话时长字段，不新增索引。
- 验证结果：
  - 后端目标测试：`mvn.cmd test '-Dtest=InterviewServiceTest,InterviewAiServiceImplTest,MockInterviewAiServiceImplTest'` 通过，54 个测试通过。
  - 后端完整测试：`mvn.cmd test` 通过，378 个测试通过。
- 关联任务文件：
  - `tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_BACKEND.md`
  - `frontend/tasks/TASK_49_INTERVIEW_VOICE_INTERACTION_FRONTEND.md`
  - `server/db/migrations/TASK_49_INTERVIEW_VOICE_INTERACTION.sql`
  - `server/db/schema.sql`
- 停止说明：本轮只完成模拟面试语音通话后端最小支撑，不接第三方语音服务、不保存通话时长、不扩展实时音视频能力。

## 账号注册时间展示与注销账号独立验证（2026-05-18）
- 当前阶段：本轮已完成，等待人工验收。
- 已完成内容：
  - `/api/auth/me` 新增返回 `createTime`，用于首页和设置中心账号资料展示用户注册时间。
  - 设置中心账号资料区将会员到期时间改为注册时间。
  - 首页身份卡将会员到期时间改为注册时间。
  - 设置中心左侧导航新增独立“注销账号”页，移除账号安全底部危险操作行。
  - 注销页新增高危警告、三步说明、当前密码、确认密码、安全问题答案、15 秒冷静期和最终确认弹窗。
  - 新增已登录接口 `GET /api/user/account/security-question`，仅返回当前登录账号的真实安全问题。
  - `POST /api/user/account/delete` 增强为当前密码、确认密码和安全问题答案共同校验后才注销。
- 数据存储：不新增表和字段；注册时间复用 `sys_user.create_time`；账号注销仍沿用逻辑删除、匿名化和关联数据清理。
- 验证结果：
  - 后端：`mvn.cmd test` 通过，362 个测试通过。
  - 前端：`npm.cmd test` 通过，18 个测试文件、86 个测试用例通过。
  - 前端构建：`npm.cmd run build` 通过。
- 关联任务文件：
  - `tasks/TASK_45_ACCOUNT_PROFILE_AND_DELETE_VERIFICATION_BACKEND.md`
  - `frontend/tasks/TASK_45_ACCOUNT_PROFILE_AND_DELETE_VERIFICATION_FRONTEND.md`
- 停止说明：本轮只处理注册时间展示和账号注销验证增强，不继续开发账号恢复、数据导出、管理端代注销或物理删除能力。

## 设置中心数据管理显式保存与无用偏好移除（2026-05-18）
- 当前阶段：本轮已完成，等待人工验收。
- 已完成内容：
  - 设置中心移除“回复详略偏好”，前端本机偏好、服务端 DTO、实体、服务和接口返回均不再包含该字段。
  - 面试偏好修改只保存到本机 `settingsPreferences`，不会调用服务端用户设置接口。
  - 数据管理区新增显式“保存设置”按钮，面试记录保留天数和简历诊断保留天数只有点击保存后才写入服务端。
  - 新增 V4.1 迁移脚本，用于已执行 V4.0 的环境删除旧的 `response_detail_preference` 字段。
- 数据存储：`user_settings` 表当前只承载面试记录保留天数和简历诊断记录保留天数；自动清理仍沿用逻辑删除。
- 压力控制：不在面试偏好操作中产生服务端写入；自动清理任务策略不变，仍为低峰小批量执行。
- 验证结果：
  - 后端：`mvn.cmd test` 通过，357 个测试通过。
  - 前端：`npm.cmd test` 通过，18 个测试文件、85 个测试用例通过。
  - 前端构建：`npm.cmd run build` 通过。
- 关联任务文件：
  - `tasks/TASK_44_SETTINGS_DATA_MANAGEMENT_SAVE_AND_RESPONSE_DETAIL_REMOVAL_BACKEND.md`
  - `frontend/tasks/TASK_44_SETTINGS_DATA_MANAGEMENT_SAVE_AND_RESPONSE_DETAIL_REMOVAL_FRONTEND.md`
- 停止说明：本轮只处理设置中心保存时机和回复详略偏好移除，不继续开发 AI prompt 口径、清理策略扩展、数据导出或管理端能力。

## 设置中心危险操作后端接入（2026-05-18）
- 当前阶段：本轮已完成，等待人工验收。
- 已完成内容：
  - 新增账号注销接口 `POST /api/user/account/delete`，当前密码校验通过后清理当前用户关联业务数据，并将用户账号逻辑删除和匿名化。
  - 新增面试记录清理接口 `DELETE /api/interview/history`，逻辑删除当前用户面试会话、聊天记录和岗位定向上下文。
  - 新增简历诊断清理接口 `DELETE /api/resume/history`，逻辑删除当前用户简历诊断任务、JD 匹配记录和简历润色记录，并按安全路径校验删除上传文件。
  - JWT 过滤器补充账号有效性校验，已注销或禁用账号的旧 token 不再认证。
  - 设置中心前端启用三个危险操作，补齐确认弹窗、loading、成功反馈、失败兜底和账号注销后的登录态清理跳转。
- 数据存储：不新增数据库表；账号注销采用逻辑删除 + 敏感字段匿名化；历史清理沿用现有 `is_deleted` 逻辑删除字段；“面试记录保留天数”仍不接服务端自动清理。
- 验证结果：
  - 后端：`mvn.cmd test` 通过，348 个测试通过。
  - 前端：`npm.cmd test` 通过，18 个测试文件、82 个测试用例通过。
  - 前端构建：`npm.cmd run build` 通过。
- 关联任务文件：
  - `tasks/TASK_42_SETTINGS_DATA_MANAGEMENT_BACKEND.md`
  - `frontend/tasks/TASK_42_SETTINGS_DATA_MANAGEMENT_FRONTEND.md`
- 停止说明：本轮只接入账号注销、面试记录清理、简历诊断清理三个显式危险操作，不继续开发自动保留天数清理、数据导出、账号恢复或管理端代删能力。

## 当前版本
- Offer 加速器实施阶段

## 当前阶段
- 第 4 部分：Offer 辅助链路第一版 Prompt 优化与复制按钮二次修复
- 状态：本轮已完成，等待人工验收

## 已完成且已验收
- V1.1：岗位 JD 对比分析
- V1.1：AI 简历润色
- V1.1：岗位定向模拟面试
- V1.2：新手引导
- V1.2：个人成长中心
- V1.2：消息通知
- Offer 加速器第 0 部分：当前状态收口与验证
- Offer 加速器第 1 部分：深度面试分析报告 V2
- Offer 加速器第 2 部分：面试历史回放 + 即时反馈可选
- Offer 加速器第 3 部分：多面试官人设系统

## Offer 加速器功能状态
- 第 0 部分：当前状态收口与验证，已完成
- 第 1 部分：深度面试分析报告 V2，已完成
- 第 2 部分：面试历史回放 + 即时反馈可选，已完成
- 第 3 部分：多面试官人设系统，已完成
- 第 4 部分：Offer 辅助链路第一版，本轮 Prompt 优化与复制按钮二次修复已完成，等待验收
- 后续录用意向评估、背调准备指导：尚未开始
- 题库、热点、收藏、命中率统计：明确不做

## 本轮完成内容
- 已按要求先读取 `runtime/DEVELOPMENT_RULES.txt`
- 新增 Offer 辅助后端接口：
  - `POST /api/offer/salary-negotiation/simulate`
  - `POST /api/offer/salary-negotiation/script`
- 新增薪资谈判模拟能力：基于公司、岗位、薪资目标、候选人背景和 HR 当前问题生成场景判断、建议回复、推进策略、风险提醒和下一步行动
- 新增谈薪话术模板能力：生成开场确认、争取报价、交换项和收口确认四类话术
- AI 调用复用现有 `AiChatClient`，不新增 AI 调用链路
- 本轮不接实时薪资行情，不编造市场分位数或公司薪资数据
- 本轮不新增数据库表、字段或迁移脚本
- 本轮未实现录用意向评估、背调准备指导
- 本轮未新增题库、热点、收藏、命中率等功能
- 根据验收反馈优化 Offer 辅助 Prompt：增加中高端求职者谈判教练口径，要求话术包含感谢、入职意愿、价值依据、明确请求和可协商余地
- 根据验收反馈修复 `/offer` 页面复制按钮：改为图标型弱化操作按钮，复制成功后短暂显示完成状态，不再呈现浏览器默认文字按钮质感
- 根据本轮验收反馈继续修复 `/offer` 页面：复制按钮改为常显的图标加文字按钮，结果标题改为胶囊标签，提交接口失败时捕获异常并提示

## 验证结果
- 后端编译：`mvn.cmd -q -DskipTests compile` 通过
- 后端新增测试：`mvn.cmd -q "-Dtest=OfferAssistServiceImplTest,OfferAssistControllerTest" test` 通过
- 后端关键测试集合：`mvn.cmd -q "-Dtest=OfferAssistServiceImplTest,OfferAssistControllerTest,InterviewServiceTest,MockInterviewAiServiceImplTest,ResumeDiagnosisTaskServiceImplTest,ResumeDiagnosisProcessorTest,ResumeAiServiceImplTest,ResumePdfControllerTest" test` 通过
- 前端构建：`npm.cmd run build` 通过
- 前端测试：`frontend/app/package.json` 当前未配置 test 脚本，本轮以前端构建验证为准
- 本轮 Prompt 优化与复制按钮二次修复后已重新执行：
  - `mvn.cmd -q -DskipTests compile` 通过
  - `mvn.cmd -q "-Dtest=OfferAssistServiceImplTest,OfferAssistControllerTest" test` 通过
  - `npm.cmd run build` 通过
- 本轮复制按钮可见性、结果标签和提交异常处理修复后，`npm.cmd run build` 通过

## 关联任务文件
- `tasks/TASK_32_OFFER_ACCELERATOR_STAGE4_OFFER_ASSIST_BACKEND.md`
- `frontend/tasks/TASK_32_OFFER_ACCELERATOR_STAGE4_OFFER_ASSIST_FRONTEND.md`

## 下一步
- 等待人工验收第 4 部分 Prompt 优化与复制按钮二次修复
- 停止，不继续下一个功能
# 设置中心记录保留天数自动清理（2026-05-18）
- 当前阶段：本轮已完成，等待人工验收。
- 已完成内容：
  - 新增 `GET /api/user/settings`、`PUT /api/user/settings`，保存当前用户的面试记录保留天数和简历诊断保留天数。
  - 新增 `user_settings` 表，不修改 `sys_user` 主表。
  - 新增每日 03:30 自动清理任务，按用户设置小批量清理超过保留期的历史记录。
  - 面试自动清理只处理已结束会话，并同步逻辑删除聊天记录和岗位定向上下文。
  - 简历诊断自动清理只处理已完成或已失败任务，并同步逻辑删除 JD 匹配、AI 润色记录，上传文件按安全路径校验后删除。
  - 设置中心新增简历诊断保留天数，面试/简历保留天数保存到服务端，保存成功后同步本地偏好。
- 数据存储：新增 `user_settings` 表；新增面试与简历诊断保留期清理复合索引；业务记录继续使用 `is_deleted` 逻辑删除。
- 压力控制：默认关闭自动清理；每用户每类数据每次最多 10 批、每批 200 条；不在用户请求中执行自动清理；不物理删除数据库行。
- 验证结果：
  - 后端：`mvn.cmd test` 通过，358 个测试通过。
  - 前端：`npm.cmd test` 通过，18 个测试文件、84 个测试用例通过。
  - 前端构建：`npm.cmd run build` 通过。
- 关联任务文件：
  - `tasks/TASK_43_SETTINGS_RETENTION_AUTO_CLEANUP_BACKEND.md`
  - `frontend/tasks/TASK_43_SETTINGS_RETENTION_AUTO_CLEANUP_FRONTEND.md`
- 停止说明：本轮只实现保留天数自动清理和用户设置持久化，不继续实现按日期范围清理、勾选清理、数据导出、物理删除或 AI prompt 详略口径接入。
# 问题反馈/建议模块（2026-05-19）
- 当前阶段：本轮已完成，等待人工验收。
- 已完成内容：
  - 新增 `user_feedback` 独立表和迁移脚本，不修改核心业务表。
  - 新增用户端 `POST /api/user/feedback`，支持 `bug/suggestion/experience/other` 四类反馈。
  - 新增管理端 `/api/admin/feedback` 列表、详情、状态更新和批量删除接口。
  - 管理端反馈状态固定为：待处理、处理中、已处理、已关闭。
  - 设置中心新增“问题反馈”分组，用户提交成功后清空表单。
  - 管理端新增 `/admin/feedback` 页面和侧边栏入口，支持筛选、分页、详情、处理备注和批量删除。
- 数据存储：新增 `user_feedback` 表，记录用户、类型、标题、内容、联系方式、状态、处理备注、处理人、处理时间和通用审计字段。
- 验证结果：
  - 后端：`mvn.cmd test` 通过，372 个测试通过。
  - 前端：`npm.cmd test` 通过，96 个测试通过。
  - 前端构建：`npm.cmd run build` 通过。
- 关联任务文件：
  - `tasks/TASK_47_USER_FEEDBACK_BACKEND.md`
  - `frontend/tasks/TASK_47_USER_FEEDBACK_FRONTEND.md`
- 停止说明：本轮只实现问题反馈/建议最小闭环，不继续扩展附件上传、用户反馈历史、客服对话或管理员回复通知。
## 模拟面试岗位上下文负缓存修复（2026-05-21）
- 当前阶段：本轮已完成，等待人工验收。
- 问题确认：普通模拟面试没有 `mock_interview_job_target_record` 记录，报告生成期间前端轮询会话详情时，每次都会调用 `MockInterviewJobTargetServiceImpl.getSessionContext()` 并重复查询不存在的岗位定向记录。
- 根因说明：上一轮为避免 Redis 写入 `null` 报错，给 `@Cacheable` 增加了 `unless = "#result == null"`；该修复消除了 500，但也让空结果无法缓存，导致普通模拟面试轮询期间持续穿透数据库。
- 已完成内容：
  - `getSessionContext()` 查不到记录时不再返回 `null`，改为返回 `jobTargeted=false`、`sourceType=none` 的可序列化空上下文。
  - 移除 `getSessionContext()` 的 `unless = "#result == null"`，让 Spring Cache 可以缓存空上下文，形成负缓存。
  - `saveSessionContext()` 保持原有 `@CacheEvict`，后续如果创建真实岗位定向记录，会驱逐空上下文缓存。
  - 非流式追问和流式追问链路把无简历内容的空上下文继续视为缺失，仍会回退到最近简历上下文，避免负缓存影响普通面试追问质量。
  - 更新 `MockInterviewJobTargetServiceImplTest`，覆盖缓存注解和查无记录返回空上下文的行为。
- 数据存储：不新增表、不修改字段、不新增迁移脚本；仅调整缓存返回值语义。
- 验证结果：
  - 后端目标测试：`mvn.cmd -q "-Dtest=MockInterviewJobTargetServiceImplTest,InterviewServiceTest" test` 通过。
  - 后端编译：`mvn.cmd -q -DskipTests compile` 通过。
- 停止说明：本轮只修复普通模拟面试详情轮询导致的岗位上下文空结果重复查库问题，不继续扩展其他面试功能。

## 社区图片上传公网占位 URL 修复（2026-05-23）
- 当前阶段：本轮已完成，等待人工验收。
- 问题确认：社区图片数据保存 `/uploads/community/root-test-data.png` 这类本机静态路径时，浏览器会请求后端本地静态资源；文件不存在时触发 `NoResourceFoundException`，并且其他用户无法稳定访问本机上传文件。
- 已完成内容：
  - `POST /api/community/images/upload` 保留图片大小、扩展名和魔术字节校验。
  - 阿里云 OCS/OSS 密钥未配置前，上传成功不再落本机 `uploads/community`，改为返回公网占位图 URL。
  - 新增 `app.upload.community-placeholder-url` 配置默认值，后续接入真实对象存储时可替换上传返回链路。
  - 更新 root 和通用社区测试数据 SQL，图片字段统一写入公网占位图，不再写入 `/uploads/community/*.png`。
- 数据存储：不新增表、不修改字段、不新增迁移脚本；仅调整图片 URL 内容来源。
- 验证结果：
  - RED 验证：`mvn.cmd test "-Dtest=CommunityServiceValidationTest"` 先失败，确认旧实现返回本地 `/uploads/community/*.png`。
  - 后端定向测试：`mvn.cmd test "-Dtest=CommunityServiceValidationTest"` 通过，16 个测试通过。
- 关联任务文件：`tasks/fixes/TASK_COMMUNITY_IMAGE_UPLOAD_PLACEHOLDER_URL_2026_05_23_BACKEND.md`
- 停止说明：本轮只处理社区图片上传 URL 占位，不接入真实阿里云 OCS/OSS SDK，不继续扩展对象存储管理能力。

## 社区旧图片路径清理与静态资源 404 修复（2026-05-23）
- 当前阶段：本轮已完成，等待人工验收。
- 问题确认：`debug.txt` 显示 `/api/community/posts` 返回了旧 root seed 记录 `99400011`、`99400014`，前端随后请求 `/uploads/community/root-test-frontend.png`、`/uploads/community/root-test-data.png`；当前 SQL 文件已改为公网占位图，说明运行数据库仍残留旧 seed 图片路径。
- 已完成内容：
  - 新增 `db/community_cleanup_local_image_urls.sql`，用于把 `community_post.images` 与 `community_comment.images` JSON 数组中残留的 `/uploads/community/%` 替换为公网占位图。
  - 补充 `NoResourceFoundException` 专用异常处理，缺失静态资源返回 404，不再被 `Exception` 通用处理器兜成 500。
  - 更新 `GlobalExceptionHandlerTest` 覆盖缺失静态资源返回 `ResultCode.NOT_FOUND`。
- 数据存储：不新增表、不修改字段；仅提供一次性数据修复 SQL 清理旧 URL 内容。
- 验证结果：
  - 后端回归测试：`mvn.cmd test "-Dtest=GlobalExceptionHandlerTest,CommunityServiceValidationTest,SecurityConfigTest"` 通过，25 个测试通过。
- 关联任务文件：`tasks/fixes/TASK_COMMUNITY_IMAGE_UPLOAD_PLACEHOLDER_URL_2026_05_23_BACKEND.md`
- 停止说明：本轮只清理旧社区本地图片 URL 问题并修正静态资源缺失状态码，不接入真实对象存储 SDK。
