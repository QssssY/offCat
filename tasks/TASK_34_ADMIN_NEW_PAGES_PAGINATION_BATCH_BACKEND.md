# TASK_34_ADMIN_NEW_PAGES_PAGINATION_BATCH_BACKEND

## 1. 当前任务所属模块
- 管理端模块
- 子模块：新增管理端页面批量操作后端接口

## 2. 前端文件定位
- `frontend/app/src/views/admin/AdminNotificationView.vue`
- `frontend/app/src/views/admin/AdminVersionLogView.vue`
- `frontend/app/src/views/admin/AdminGrowthConfigView.vue`
- `frontend/app/src/views/admin/AdminMembershipPlanView.vue`
- `frontend/tasks/TASK_34_ADMIN_NEW_PAGES_PAGINATION_BATCH_FRONTEND.md`

## 3. 后端文件定位
- `server/src/main/java/com/airesume/server/controller/AdminNotificationController.java`
- `server/src/main/java/com/airesume/server/controller/AdminVersionLogController.java`
- `server/src/main/java/com/airesume/server/controller/AdminGrowthConfigController.java`
- `server/src/main/java/com/airesume/server/controller/AdminMembershipController.java`
- `server/src/test/java/com/airesume/server/controller/AdminNotificationControllerTest.java`
- `server/src/test/java/com/airesume/server/controller/AdminVersionLogControllerTest.java`
- `server/src/test/java/com/airesume/server/controller/AdminGrowthConfigControllerTest.java`
- `server/src/test/java/com/airesume/server/controller/AdminMembershipControllerTest.java`

## 4. 本轮修改文件清单
- 通知公告新增批量发布、批量删除接口。
- 版本日志新增批量发布、批量删除接口。
- 成长配置新增批量删除接口。
- 会员套餐新增批量启停、批量删除接口。
- 对应 Controller 单元测试补充正常路径和空 ID 异常路径。

## 5. 前端实现方案
- 前端通过管理端 API 封装调用批量接口。
- 审计日志和订单管理保持只读，不调用任何批量变更接口。

## 6. 后端实现方案
- 批量删除使用 `removeByIds`，避免前端循环调用单条删除。
- 批量发布使用 `listByIds` 读取记录，已发布记录跳过，草稿记录更新为发布状态。
- 会员套餐批量启停复用 `BatchActiveRequest`，仅更新 `status` 字段。
- 批量 ID 校验限制为空拒绝、最多 100 条、过滤空 ID 并去重。

## 7. 数据存储方案
- 不新增表和字段。
- 通知公告批量发布会沿用原有广播逻辑，向目标用户生成用户通知。
- 版本日志批量发布会写入发布时间。
- 成长配置和会员套餐批量删除沿用当前服务层删除语义。

## 8. stage 更新说明
- 本轮后端没有发现独立 backend stage 文件，验收状态记录在本任务文件与前端 `frontend/tasks/stage.md` 中。

## 9. 编译结果
- 命令：`mvn.cmd test`
- 结果：编译通过。

## 10. 构建结果
- 前端构建结果见 `frontend/tasks/TASK_34_ADMIN_NEW_PAGES_PAGINATION_BATCH_FRONTEND.md`。

## 11. 测试结果
- 命令：`mvn.cmd test`
- 结果：336 个测试通过，0 失败，0 错误。

## 12. 当前功能验收说明
- 新增批量接口只覆盖通知公告、版本日志、成长配置、会员套餐。
- 审计日志和订单管理不新增批量删除或状态变更，避免破坏审计和交易数据语义。
- 批量发布重复提交已发布记录时保持幂等，不返回错误。

## 13. 停止，不继续下一个功能
- 本轮仅完成管理端新增页面批量接口支撑。
- 未继续开发导出、搜索、订单状态变更或审计日志清理等额外功能。
