# TASK_08_ADMIN_JOB_ROLE_CONFIG

## 所属模块
- 管理端模块
- 子模块：岗位配置模块

## 背景
当前模拟面试入口页的岗位选项仍然写死在前端，和“岗位由管理员配置”的真实业务规则冲突。

这会带来 3 个问题：
- 新增岗位必须改前端代码，管理端无法真正运营岗位
- 禁用岗位后，前端仍可能继续展示旧数据
- 用户可以绕过页面直接传入任意岗位，后端缺少统一校验源

因此，本轮任务需要建立“岗位配置最小闭环”：
- 管理员可维护岗位
- 用户端从后端读取岗位
- 创建面试时后端按岗位配置表校验

## 本轮目标
1. 新增岗位配置表 `sys_job_role`
2. 提供管理端岗位查询、新增、修改、启用/禁用接口
3. 提供用户端岗位只读接口
4. 移除前端 `InterviewEntryView` 硬编码岗位
5. 预置一批测试岗位
6. 同步更新 API 文档和 stage 进度文档

## 本轮 task 拆分
1. 数据层：新增岗位配置表和增量 SQL
2. 后端接口：补齐管理端岗位配置接口和用户端岗位列表接口
3. 业务校验：创建面试时校验岗位必须存在且启用
4. 前端联调：面试入口改为读取后端岗位接口
5. 文档资产：更新 task、API 文档、runtime stage 进度

## 实现清单
- `db/schema.sql`
- `db/migrations/TASK_08_ADMIN_JOB_ROLE_CONFIG.sql`
- `server/src/main/java/com/airesume/server/entity/SysJobRole.java`
- `server/src/main/java/com/airesume/server/mapper/SysJobRoleMapper.java`
- `server/src/main/java/com/airesume/server/service/SysJobRoleService.java`
- `server/src/main/java/com/airesume/server/service/impl/SysJobRoleServiceImpl.java`
- `server/src/main/java/com/airesume/server/dto/admin/JobRoleCreateRequest.java`
- `server/src/main/java/com/airesume/server/dto/admin/JobRoleUpdateRequest.java`
- `server/src/main/java/com/airesume/server/dto/admin/JobRoleResponse.java`
- `server/src/main/java/com/airesume/server/dto/interview/InterviewJobRoleResponse.java`
- `server/src/main/java/com/airesume/server/controller/AdminController.java`
- `server/src/main/java/com/airesume/server/controller/InterviewController.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/config/SecurityConfig.java`
- `frontend/app/src/api/interview.js`
- `frontend/app/src/views/interview/InterviewEntryView.vue`
- `docs/api/TASK_05_INTERVIEW_API.md`
- `docs/api/TASK_06A_ADMIN_API.md`
- `docs/api/API_INDEX.md`
- `runtime/STATE.md`

## 验收标准
- 管理员可以查询、新增、修改、启用/禁用岗位
- 用户端岗位下拉不再写死，改为读取接口
- 创建面试时，未配置或已禁用岗位会被后端拒绝
- 数据库和增量 SQL 中已经预置测试岗位
- API 文档和 runtime 状态文档已同步更新
