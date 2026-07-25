# V1.2 功能三：消息通知 — 后端 Task

## 当前任务所属模块
V1.2 用户体验增强版，功能三：消息通知（后端）

## 后端文件定位
- 项目路径：`server/src/main/java/com/airesume/server/`
- 数据库脚本：`db/migrations/`

## 本轮修改文件清单

### 新建文件
| 文件 | 说明 |
|------|------|
| `db/migrations/TASK_17_USER_NOTIFICATION.sql` | 用户站内通知表建表脚本 |
| `server/.../entity/UserNotification.java` | 通知实体类 |
| `server/.../mapper/UserNotificationMapper.java` | MyBatis-Plus Mapper |
| `server/.../dto/notification/NotificationQueryRequest.java` | 分页查询请求 DTO |
| `server/.../dto/notification/NotificationVO.java` | 通知响应 VO |
| `server/.../dto/notification/NotificationListResponse.java` | 列表响应（含未读数量） |
| `server/.../service/NotificationService.java` | 通知服务（创建、查询、标记已读） |
| `server/.../controller/NotificationController.java` | 通知控制器（4 个接口） |

### 修改文件
| 文件 | 修改内容 |
|------|----------|
| `db/schema.sql` | 添加 user_notification 表的 DROP 和 CREATE 定义 |
| `server/db/schema.sql` | 同步添加 user_notification 表的 DROP 和 CREATE 定义 |
| `server/.../mq/ResumeDiagnosisConsumer.java` | 注入 NotificationService，简历诊断完成后创建通知 |
| `server/.../service/impl/ResumePolishServiceImpl.java` | 注入 NotificationService，AI 润色完成后创建通知 |
| `server/.../service/InterviewService.java` | 注入 NotificationService，模拟面试完成后创建通知 + 额度不足时创建通知 |
| `server/.../service/impl/ResumeDiagnosisTaskServiceImpl.java` | 注入 NotificationService，简历额度不足时创建通知 |

## 后端实现方案

### 数据存储
新增独立表 `user_notification`，不修改任何现有表。字段包括：id, user_id, type, title, content, biz_type, biz_id, read_status, read_time, create_time, update_time, is_deleted。

### 通知服务（NotificationService）
- `createNotification(userId, type, title, content, bizType, bizId)` — 创建通知，内部 try-catch 安全降级
- `countUnread(userId)` — 统计未读数量
- `markAsRead(userId, notificationId)` — 单条标记已读，校验归属，幂等
- `markAllAsRead(userId)` — 全部标记已读，幂等
- `listNotifications(userId, page, size, readStatus, type)` — 分页查询
- `hasRecentUnreadNotification(userId, type)` — 防重检查（24h 内同类型未读）

### 通知控制器（NotificationController）
路径前缀：`/api/user/notifications`（已被 SecurityConfig 的 `/api/user/**` 规则覆盖）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 分页查询通知列表，支持 readStatus/type 筛选 |
| GET | `/unread-count` | 获取未读数量 |
| POST | `/{id}/read` | 单条标记已读 |
| POST | `/read-all` | 全部标记已读 |

### 通知类型
| type | bizType | 触发场景 |
|------|---------|----------|
| resume | resume_diagnosis | 简历诊断任务完成 |
| polish | resume_polish | AI 润色结果落库后 |
| interview | mock_interview | 模拟面试评估报告落库后 |
| quota | quota | 额度不足时（简历/面试） |

### 业务触发点接入
1. **简历诊断完成** — `ResumeDiagnosisConsumer.java` 第 69 行后，`updateStatusToCompleted` 之后
2. **AI 润色完成** — `ResumePolishServiceImpl.java` 第 91 行后，`save(record)` 之后
3. **模拟面试完成** — `InterviewService.java` 第 347 行后，评估报告落库之后
4. **额度不足（简历）** — `ResumeDiagnosisTaskServiceImpl.java` 第 64 行前
5. **额度不足（面试）** — `InterviewService.java` 第 78 行前

### 安全降级
所有通知创建调用在 NotificationService 内部用 try-catch 包裹，失败时 log.error 但不抛异常，确保不阻断主业务流程。

### 额度不足防重
使用 `hasRecentUnreadNotification` 检查最近 24 小时是否已有同类型未读通知，避免重复创建。

## stage 更新说明
- `stage.md` 中 V1.2 功能一、二状态更新为"已完成，已验收通过"
- `stage.md` 中 V1.2 功能三状态更新为"开发中"

## 编译结果
后端 `mvn compile` 编译通过，无错误。

## 当前功能验收说明
1. 用户通知列表接口（GET /api/user/notifications）可正常查询
2. 未读数量接口（GET /api/user/notifications/unread-count）可正常返回
3. 单条标记已读接口（POST /api/user/notifications/{id}/read）可正常操作
4. 全部标记已读接口（POST /api/user/notifications/read-all）可正常操作
5. 简历诊断完成后自动创建通知
6. AI 润色完成后自动创建通知
7. 模拟面试完成后自动创建通知
8. 额度不足时自动创建通知
9. 通知创建失败不影响主业务流程
10. 用户只能查看和操作自己的通知
11. 数据正常落库到 user_notification 表

## 停止，不继续下一个功能
