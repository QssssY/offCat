# TASK 55: 新手引导改任务式 — 后端

## 状态: 已完成

## 概述
将新手引导从纯 UI tour 模态框改为任务清单式引导。新增 `user_onboarding_task` 独立表，4 个任务粒度记录完成状态，支持幂等上报和旧用户向后兼容。

## 已完成内容

### 数据库
- 新增 `user_onboarding_task` 表（id/user_id/task_key/completed/completed_time/create_time/update_time/is_deleted）
- UNIQUE INDEX `uk_user_task` (user_id, task_key)，INDEX `idx_task_user` (user_id, is_deleted)
- 迁移脚本：`db/migrations/TASK_55_USER_ONBOARDING_TASK.sql` + `server/db/migrations/TASK_55_USER_ONBOARDING_TASK.sql`
- 同步更新 `db/schema.sql` 和 `server/db/schema.sql`

### Entity + Mapper
- `UserOnboardingTask` 实体：extends BaseEntity，字段 userId/taskKey/completed/completedTime
- `UserOnboardingTaskMapper`：extends BaseMapper + logicalDeleteByUserId 方法

### DTO
- `OnboardingTasksResponse`：tasks/completedCount/totalCount/allCompleted/visible + 内部 TaskItem
- `OnboardingTaskCompleteRequest`：taskKey（@NotBlank）

### Service
- `UserOnboardingService` 接口新增 getTasks(userId) 和 completeTask(userId, taskKey)
- `UserOnboardingServiceImpl` 实现：
  - 4 个任务定义：resume_uploaded / report_viewed / jd_compared / interview_completed
  - 旧引导已完成/跳过用户返回 visible=false
  - 幂等完成 + DuplicateKeyException 并发兜底

### Controller
- `GET /api/user/onboarding/tasks` → 查询任务列表和完成进度
- `POST /api/user/onboarding/tasks/complete` → 上报任务完成（幂等）

### 账号注销
- `UserAccountServiceImpl` 注销时调用 `userOnboardingTaskMapper.logicalDeleteByUserId(userId)`

### 测试
- `UserOnboardingServiceImplTest`：10 个测试覆盖新用户/旧用户/部分完成/全部完成/幂等/非法key/并发/更新
- `UserOnboardingControllerTest`：2 个测试覆盖查询和上报
- `UserAccountServiceImplTest`：更新覆盖新增的注销清理

## 验证
- 后端编译：`mvn clean compile` 通过
- 目标测试：`mvn test -Dtest=UserOnboardingServiceImplTest,UserOnboardingControllerTest,UserAccountServiceImplTest` 通过，18 个测试
