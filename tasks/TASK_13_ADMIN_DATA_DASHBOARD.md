# TASK_13_ADMIN_DATA_DASHBOARD

## 所属模块
- 管理端模块
- 子模块：数据看板增强（可筛选、可扩展）

## 背景
当前已完成：
- `TASK_08_ADMIN_JOB_ROLE_CONFIG`
- `TASK_09_ADMIN_PROMPT_JOB_ROLE_LINK`
- `TASK_10_ADMIN_AI_ENGINE_CONFIG`
- `TASK_11_ADMIN_USER_RIGHTS_ENHANCE`
- `TASK_12_ADMIN_MONITORING_AND_DASHBOARD`

目前管理端已经有基础看板接口，但参数能力不足，无法支撑日期筛选与扩展统计。

## 本轮目标
1. 增强 `GET /api/admin/dashboard/overview`，支持 `startDate/endDate`
2. 增强 `GET /api/admin/dashboard/trends`，支持 `startDate/endDate`
3. 增强 `GET /api/admin/dashboard/hot-job-roles`，支持 `startDate/endDate/limit`
4. 新增 `GET /api/admin/dashboard/business-distribution`
5. 增加查询参数校验：
   - `startDate` 不能大于 `endDate`
   - 查询范围不能超过 90 天
6. 统计口径统一：
   - 面试统计按 `InterviewSession.createTime`
   - 简历诊断按 `ResumeDiagnosisTask.createTime`
7. 同步更新 task/API/stage 文档并通过编译验证

## 本轮 Task 拆分
1. 接口层：为 dashboard 相关接口增加 query 参数
2. 服务层：统一封装日期范围解析与校验逻辑
3. 统计层：按统一口径实现筛选版趋势与排行统计
4. 新能力：新增业务分布统计 DTO 与服务方法
5. 工程资产：更新 task、API 文档、stage 文档
6. 验证：执行 `mvn.cmd -q -DskipTests compile`

## 实现清单
- `server/src/main/java/com/airesume/server/dto/admin/BusinessDistributionResponse.java`
- `server/src/main/java/com/airesume/server/service/AdminDashboardService.java`
- `server/src/main/java/com/airesume/server/service/impl/AdminDashboardServiceImpl.java`
- `server/src/main/java/com/airesume/server/controller/AdminController.java`
- `tasks/TASK_13_ADMIN_DATA_DASHBOARD.md`
- `docs/api/TASK_06A_ADMIN_API.md`
- `docs/api/API_INDEX.md`
- `runtime/STATE.md`

## 关键实现说明
- 通过 `resolveDateRange(...)` 统一处理日期参数默认值与合法性校验，避免控制器重复逻辑
- 默认行为保持兼容：
  - `overview`：不传参数时默认“今天”
  - `trends`：不传参数时默认“最近 7 天”
- `hot-job-roles` 支持 `limit`，默认 10，限制最大 50，防止大范围重排行查询
- 新增业务分布接口输出面试/简历统计量及占比，便于后续前端饼图/环图直接渲染

## 验收标准
- 看板 3 个原有接口支持日期范围筛选
- 新增业务分布接口可用
- 参数非法时会抛出明确业务异常
- 统计口径与需求一致（按 createTime）
- 文档资产与代码保持一致
- 编译通过（`-DskipTests`）
