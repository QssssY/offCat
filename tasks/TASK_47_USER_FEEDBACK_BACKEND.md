# TASK 47 问题反馈/建议模块后端

## 当前任务所属模块

问题反馈/建议模块，覆盖用户提交反馈与管理端受理接口。

## 前端文件定位

前端实现见 `frontend/tasks/TASK_47_USER_FEEDBACK_FRONTEND.md`。

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/UserFeedbackController.java`
- `server/src/main/java/com/airesume/server/controller/AdminFeedbackController.java`
- `server/src/main/java/com/airesume/server/entity/UserFeedback.java`
- `server/src/main/java/com/airesume/server/dto/feedback/*`
- `server/db/migrations/TASK_47_USER_FEEDBACK.sql`

## 本轮修改文件清单

- 新增 `user_feedback` 表结构与迁移脚本。
- 新增用户反馈 Entity、Mapper、Service。
- 新增用户端 `POST /api/user/feedback`。
- 新增管理端 `GET /api/admin/feedback`、`GET /api/admin/feedback/{id}`、`PUT /api/admin/feedback/{id}/status`、`POST /api/admin/feedback/batch-delete`。
- 新增 `UserFeedbackControllerTest` 和 `AdminFeedbackControllerTest`。
- 最小修复既有 `UserAccountServiceImplTest` 构造器参数，使测试编译与当前生产代码一致。

## 后端实现方案

用户提交反馈时按当前登录用户写入 `user_feedback`，默认状态为 `0` 待处理。管理端接口沿用现有 `Result` 响应、MyBatis Plus 分页、`BatchValidator` 批量校验和 `@PreAuthorize("hasRole('ADMIN')")` 权限保护。

## 数据存储方案

新增独立表 `user_feedback`，字段包含用户、类型、标题、内容、联系方式、状态、管理员备注、处理人、处理时间和通用审计字段；不修改核心业务表。

## 编译与测试结果

- `mvn.cmd "-Dtest=UserFeedbackControllerTest,AdminFeedbackControllerTest" test` 通过。
- `mvn.cmd test` 通过，372 个测试通过。

## 当前功能验收说明

- 登录用户可以提交问题反馈/建议。
- 管理员可以分页筛选、查看详情、更新状态和备注、批量删除反馈。
- 第一版不支持附件、用户反馈列表和管理员回复用户通知。

## 停止说明

本轮只完成问题反馈/建议模块最小闭环，不继续扩展客服对话、附件上传或消息回复能力。
