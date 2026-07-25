# 用户额度消费记录与展示增强

## 当前任务所属模块
- 后端额度管理模块。
- 前端仪表盘 + 成长中心 + 管理端。
- 关联前端任务：`frontend/tasks/task-用户额度消费记录与展示增强.md`。

## 后端文件定位
- `server/src/main/java/com/airesume/server/`
- 数据库脚本：`db/migrations/TASK_QUOTA_CONSUMPTION_LOG.sql`

## 前端文件定位
- `frontend/app/src/views/DashboardView.vue`
- `frontend/app/src/views/growth/GrowthCenterView.vue`
- `frontend/app/src/views/admin/AdminUserRightsView.vue`
- `frontend/app/src/components/growth/ConsumptionLogPanel.vue`
- `frontend/app/src/components/admin/AdminConsumptionLog.vue`
- `frontend/app/src/api/quota.js`
- `frontend/app/src/api/admin/users.js`

## 本轮修改文件清单

### 新建文件
| 文件 | 说明 |
|------|------|
| `db/migrations/TASK_QUOTA_CONSUMPTION_LOG.sql` | 消费记录表建表脚本 + sys_config 保留天数配置 |
| `server/.../entity/QuotaConsumptionLog.java` | 消费记录实体，继承 BaseEntity |
| `server/.../mapper/QuotaConsumptionLogMapper.java` | MyBatis-Plus Mapper |
| `server/.../dto/quota/ConsumptionLogResponse.java` | 消费记录响应 DTO |
| `server/.../dto/quota/QuotaOverviewResponse.java` | 额度总览响应 DTO |
| `server/.../service/QuotaConsumptionLogService.java` | 消费记录服务接口 |
| `server/.../service/impl/QuotaConsumptionLogServiceImpl.java` | 服务实现（含 @Scheduled 定时清理） |
| `server/.../controller/QuotaConsumptionLogController.java` | 用户端 API |
| `frontend/.../api/quota.js` | 前端额度 API 模块 |
| `frontend/.../components/growth/ConsumptionLogPanel.vue` | 成长中心消费记录面板组件 |
| `frontend/.../components/admin/AdminConsumptionLog.vue` | 管理端消费记录组件 |
| `server/.../test/.../QuotaConsumptionLogServiceImplTest.java` | 单元测试 |

### 修改文件
| 文件 | 修改内容 |
|------|----------|
| `server/.../service/impl/UserQuotaServiceImpl.java` | 注入 QuotaConsumptionLogService，7 个扣减/退款方法后调用 logConsumption() |
| `server/.../dto/auth/UserInfoResponse.java` | 新增 freePolishLeft、freeJdMatchLeft、freeTemplateLeft、freeOfferLeft 4 个字段 |
| `server/.../service/impl/AuthServiceImpl.java` | getCurrentUserInfo() 补充非 VIP 用户的 4 种免费额度计算 |
| `server/.../controller/AdminController.java` | 新增 GET /api/admin/users/{userId}/consumption-log 管理端接口 |
| `db/schema.sql` | 添加 user_quota_consumption_log 表定义 |
| `server/db/schema.sql` | 同步添加 user_quota_consumption_log 表定义 |
| `frontend/.../views/DashboardView.vue` | quota-card 从 2 项改为 6 宫格 CSS Grid 布局 |
| `frontend/.../views/growth/GrowthCenterView.vue` | 添加 Tab 切换（成长概览/额度明细），集成 ConsumptionLogPanel |
| `frontend/.../views/admin/AdminUserRightsView.vue` | 用户详情 Drawer 新增「消费记录」Tab |
| `frontend/.../api/admin/users.js` | 新增 getAdminConsumptionLog() 管理端 API 函数 |

## 前端实现方案

### Dashboard 6 宫格
- quota-card 从 2 项 flex 布局改为 6 宫格 CSS Grid（`grid-template-columns: repeat(3, 1fr)`）
- 响应式：1023px 以下 2 列，767px 以下 2 列（更紧凑）
- 新增 `quotaItems` computed 属性，统一处理 VIP/非 VIP 数据源
- 额度耗尽数字变红 + 显示「升级会员」链接

### 成长中心 Tab
- GrowthCenterView 顶部新增 Tab 切换栏（成长概览/额度明细）
- ConsumptionLogPanel 组件：类型筛选栏 + 消费记录列表 + el-pagination 分页
- API 模块 `api/quota.js` 调用 `/api/user/quota/consumption-log`

### 管理端消费记录
- AdminUserRightsView 用户详情 Drawer 新增「消费记录」Tab
- AdminConsumptionLog 组件：el-select 类型筛选 + el-table 记录表格 + el-pagination 分页
- `api/admin/users.js` 新增 `getAdminConsumptionLog(userId, params)`

## 后端实现方案

### 消费记录写入
- `QuotaConsumptionLogService.logConsumption()` 在额度扣减成功后、同一事务提交前调用
- 7 个扣减点全部集成：checkInterviewQuota、deductResumeQuota、refundResumeQuota、checkAndDeductPolishQuota、checkAndDeductJdMatchQuota、checkAndDeductTemplateQuota、checkAndDeductOfferQuota
- 每个扣减点区分 VIP/非 VIP 来源（source 字段：FREE/VIP_DAILY）
- balance_after 计算：扣减前查当前余额，扣减后计算 `原余额 - changeAmount`

### 消费记录查询
- 用户端 GET /api/user/quota/consumption-log：分页 + 类型筛选，通过 Authentication 获取 userId
- 管理端 GET /api/admin/users/{userId}/consumption-log：管理员权限校验
- 中文映射：quotaType/source/businessType 统一在 ServiceImpl 中映射

### UserInfo 扩展
- UserInfoResponse 新增 freePolishLeft、freeJdMatchLeft、freeTemplateLeft、freeOfferLeft
- AuthServiceImpl.getCurrentUserInfo() 在非 VIP 时填充这 4 个字段

### 定时清理
- @Scheduled(cron = "0 0 3 * * ?") 每天凌晨 3:00 执行
- 从 sys_config 读取保留天数（默认 90 天），逻辑删除过期记录

## 数据存储方案
- 新增独立表 `user_quota_consumption_log`，不修改现有表
- 字段：id, user_id, quota_type, change_amount, balance_after, source, billing_source, business_id, business_type, description, create_time, update_time, is_deleted
- 索引：idx_user_type_time, idx_user_time, idx_create_time
- sys_config 新增 consumption_log_retention_days 配置项（默认 90）
- db/schema.sql 和 server/db/schema.sql 已同步

## stage 更新说明
- 本轮完成用户额度消费记录与展示增强功能
- 涉及后端（数据库+Service+Controller）和前端（Dashboard+成长中心+管理端）

## 编译结果
- `mvn clean compile` ✅ 通过（exit 0）

## 构建结果
- `npm run build` ✅ 通过（5182 modules, 19.17s）

## 当前功能验收说明
- 用户执行 6 种额度消费后，user_quota_consumption_log 表有对应记录
- 简历诊断失败退款后，有 change_amount = -1 的退款记录
- Dashboard 展示全部 6 种额度的剩余（6 宫格）
- 成长中心「额度明细」Tab 支持按类型筛选 + 标准分页
- 管理端用户详情 Drawer 可查看消费记录
- 90 天过期记录由 @Scheduled 定时任务逻辑删除

## 停止，不继续下一个功能
本轮仅完成用户额度消费记录与展示增强，等待验收，不继续推进其他功能。
