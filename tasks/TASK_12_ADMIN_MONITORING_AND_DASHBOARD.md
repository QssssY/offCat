# TASK_12_ADMIN_MONITORING_AND_DASHBOARD

## 所属模块
- 管理端模块
- 子模块：业务监控 + 基础数据看板

## 背景
当前项目已完成：
- `TASK_08_ADMIN_JOB_ROLE_CONFIG`
- `TASK_09_ADMIN_PROMPT_JOB_ROLE_LINK`
- `TASK_10_ADMIN_AI_ENGINE_CONFIG`
- `TASK_11_ADMIN_USER_RIGHTS_ENHANCE`

管理端已具备岗位、Prompt、AI 引擎、用户权益配置能力，但还缺少统一的运营统计接口，管理端前端无法直接读取看板和监控数据。

## 本轮目标
1. 新增管理端看板总览接口：`GET /api/admin/dashboard/overview`
2. 新增近 7 日趋势接口：`GET /api/admin/dashboard/trends`
3. 新增热门岗位排行接口：`GET /api/admin/dashboard/hot-job-roles`
4. 新增业务监控总览接口：`GET /api/admin/monitor/overview`
5. 先实现应用层统计版，不依赖 RabbitMQ/Redis 深度监控
6. 同步更新 task、API 文档、stage 文档

## 本轮 Task 拆分
1. 设计 DTO：定义看板与监控接口返回结构
2. 实现聚合服务：从业务表聚合统计数据
3. 控制器落地：新增 4 个管理端接口并复用管理员鉴权
4. 文档同步：更新管理端 API 文档、API 索引、stage 状态
5. 编译验证：执行 `mvn.cmd -q -DskipTests compile`

## 实现清单
- `server/src/main/java/com/airesume/server/dto/admin/DashboardOverviewResponse.java`
- `server/src/main/java/com/airesume/server/dto/admin/DashboardTrendResponse.java`
- `server/src/main/java/com/airesume/server/dto/admin/HotJobRoleResponse.java`
- `server/src/main/java/com/airesume/server/dto/admin/MonitorOverviewResponse.java`
- `server/src/main/java/com/airesume/server/service/AdminDashboardService.java`
- `server/src/main/java/com/airesume/server/service/impl/AdminDashboardServiceImpl.java`
- `server/src/main/java/com/airesume/server/controller/AdminController.java`
- `docs/api/TASK_06A_ADMIN_API.md`
- `docs/api/API_INDEX.md`
- `runtime/STATE.md`
- `tasks/TASK_12_ADMIN_MONITORING_AND_DASHBOARD.md`

## 关键实现说明
- 看板统计统一走业务表聚合：
  - 用户：`sys_user`
  - Prompt：`sys_prompt`
  - 岗位：`sys_job_role`
  - AI 引擎：`sys_ai_engine_config`
  - 面试会话：`interview_session`
  - 简历诊断任务：`resume_diagnosis_task`
- 趋势接口固定返回近 7 天，按日期升序输出，方便前端图表直接渲染
- 热门岗位按面试会话的 `job_role` 聚合计数，默认取 Top 10
- 监控总览优先返回可直接从应用层统计的数据，避免阻塞管理端开发节奏

## 验收标准
- 管理端 4 个新接口可访问并返回结构化数据
- 返回数据覆盖总览、趋势、热门岗位、运行态监控
- 接口复用管理员权限校验
- 文档资产（task/API/stage）与实现一致
- 编译通过（`-DskipTests`）

---

## 监控总览业务链路补齐（2026-05-31）

### 当前任务所属模块
- 管理端模块
- 子模块：业务监控总览

### 本轮修改文件清单
- `server/src/main/java/com/airesume/server/dto/admin/MonitorOverviewResponse.java`
- `server/src/main/java/com/airesume/server/service/impl/AdminDashboardServiceImpl.java`
- `server/src/test/java/com/airesume/server/service/impl/AdminDashboardServiceImplTest.java`
- `docs/api/TASK_06A_ADMIN_API.md`
- `docs/api/API_INDEX.md`
- `tasks/TASK_12_ADMIN_MONITORING_AND_DASHBOARD.md`
- `tasks/stage.md`

### 后端实现方案
- 在 `GET /api/admin/monitor/overview` 原响应上追加业务链路字段，保留原有字段与接口路径不变。
- 监控总览继续使用 `dashboardExecutor` 并行统计，并复用 `admin:monitorOverview` 缓存。
- 新增统计范围仅来自已有业务表：简历任务完成数、今日简历润色、今日 JD 匹配、今日社区发帖、今日反馈、今日订单、反馈待处理/处理中、社区帖子/评论待审数。
- 社区待审总数由 `pendingCommunityPostCount + pendingCommunityCommentCount` 计算，避免前端重复推导口径。

### 前端文件定位
- 前端展示实现见 `frontend/tasks/TASK_14_ADMIN_FRONTEND_MONITOR_ENHANCE.md`。

### 数据存储方案
- 本轮不新增表、字段或索引。
- 新增查询使用已有 `status`、`review_status`、`create_time` 相关索引。

### stage 更新说明
- 已在 `tasks/stage.md` 增加“管理端监控总览业务链路补齐”记录。
- 前端阶段记录见 `frontend/tasks/stage.md`。

### 编译结果
- `mvn.cmd -q "-Dtest=AdminDashboardServiceImplTest" test` 通过。
- `mvn.cmd -q -DskipTests compile` 通过。

### 构建结果
- 后端任务不涉及前端构建；前端构建结果见前端 task 文件。

### 当前功能验收说明
- 后端测试覆盖新增字段回填、今日半开区间参数、社区待审总数计算与缓存同步加载。
- 本轮只补业务链路监控，不接入 RabbitMQ、Redis、JVM、数据库连接池或外部 AI 连通性监控。

### 停止，不继续下一个功能
- 当前仅完成监控总览业务链路补齐，等待验收，不继续推进基础设施深度监控。
