# TASK 79 管理端自定义 AI 用量统计日期范围后端

## 当前任务所属模块

管理端 AI 引擎配置页、自定义 AI 用量统计、功能分布聚合、用户明细分页统计。

## 前端文件定位

- `frontend/app/src/api/admin/aiEngines.js`
- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/__tests__/api/admin.aiEngines.test.js`
- `frontend/app/src/__tests__/views/AdminAiEngineView.test.js`
- 前端完整记录见 `frontend/tasks/TASK_79_CUSTOM_AI_USAGE_STATS_DATE_RANGE_FRONTEND.md`。

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/AdminCustomAiStatsController.java`
- `server/src/main/java/com/airesume/server/dto/admin/CustomAiUsageStatsResponse.java`
- `server/src/main/java/com/airesume/server/mapper/UserAiUsageDetailMapper.java`
- `server/src/main/java/com/airesume/server/service/UserAiUsageStatsService.java`
- `server/src/main/java/com/airesume/server/service/impl/UserAiUsageStatsServiceImpl.java`
- `server/src/test/java/com/airesume/server/controller/AdminCustomAiStatsControllerTest.java`
- `server/src/test/java/com/airesume/server/service/impl/UserAiUsageStatsServiceImplTest.java`

## 本轮修改文件清单

- `AdminCustomAiStatsController` 的 `GET /api/admin/custom-ai/usage-stats` 新增 `startDate/endDate` 参数，并保留旧 `date` 参数。
- `UserAiUsageStatsService` 与 `UserAiUsageStatsServiceImpl` 将统计口径扩展为日期范围；`date` 传入时按 `startDate=endDate=date` 兼容单日查询。
- `CustomAiUsageStatsResponse` 新增 `startDate/endDate` 回显字段，`date` 仅作为旧版单日回显字段保留。
- `UserAiUsageDetailMapper` 将功能分布、总调用、活跃用户、用户分页和用户功能拆分 SQL 从单日条件扩展为 `usage_date BETWEEN #{startDate} AND #{endDate}`。
- 后端测试补齐范围聚合、旧 `date` 单日兼容、页码边界、非法日期范围和 90 天范围上限。

## 前端实现方案

前端将管理端自定义 AI 用量统计的单日 DatePicker 替换为日期范围选择器，并提供“今天 / 近 7 天 / 近 30 天 / 自定义”快捷切换。功能分布与用户明细共用同一范围；分页、刷新和范围切换都会携带同一组 `startDate/endDate`。

## 后端实现方案

- Controller 只负责解析 `date/startDate/endDate/page/pageSize` 和管理员权限校验。
- Service 负责日期范围归一化：`date` 优先、单侧范围按单日、无参兼容为今天、范围最大 90 天。
- Mapper 只接收 Service 已归一化的 `startDate/endDate`，所有统计 SQL 使用参数化 `BETWEEN`，不拼接 SQL。

## 数据存储方案

无数据库结构变更。不新增表、字段、索引或迁移脚本；继续复用 `user_ai_usage_detail` 现有明细表进行范围聚合。

## stage 更新说明

`tasks/stage.md` 与 `frontend/tasks/stage.md` 已记录本轮功能 1 完成状态、验证结果和停止边界；功能 2（系统级 TTS 配置）尚未开始。

## 编译结果

- 后端 RED 验证：`mvn.cmd -q "-Dtest=AdminCustomAiStatsControllerTest,UserAiUsageStatsServiceImplTest" test` 在旧实现下编译失败，复现新范围签名、DTO 字段和 Mapper 范围方法尚不存在。
- 后端 GREEN 验证：同一目标测试命令通过。
- 后端编译验证：`mvn.cmd -q -DskipTests compile` 通过。
- 后端全量测试：`mvn.cmd -q test` 通过。

## 构建结果

- 前端目标 GREEN 验证：`npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 通过，2 个测试文件 / 20 个用例。
- 前端全量测试：`npm.cmd test` 通过，80 个测试文件 / 574 个用例。
- 前端构建验证：`npm.cmd run build` 通过。

## 当前功能验收说明

管理员进入“自定义 AI 用量”分区后，功能分布和用户明细默认查询近 7 天；可以切换今天、近 30 天或自定义日期范围。旧客户端继续传 `date` 时仍按单日统计，不破坏兼容行为。

## 停止，不继续下一个功能

本轮只完成 `develop-project.txt` 功能 1：功能分布日期范围扩展。不继续推进功能 2 系统级 TTS 配置、TTS 播放链路、计费统计、音频存储或新的管理端统计页面。
