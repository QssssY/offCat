# TASK 79 管理端自定义 AI 用量统计日期范围前端

## 当前任务所属模块

管理端 AI 引擎配置页、自定义 AI 用量统计分区、功能分布范围筛选、用户明细分页展示。

## 前端文件定位

- `frontend/app/src/api/admin/aiEngines.js`
- `frontend/app/src/views/admin/AdminAiEngineView.vue`
- `frontend/app/src/__tests__/api/admin.aiEngines.test.js`
- `frontend/app/src/__tests__/views/AdminAiEngineView.test.js`

## 后端文件定位

- `server/src/main/java/com/airesume/server/controller/AdminCustomAiStatsController.java`
- `server/src/main/java/com/airesume/server/service/impl/UserAiUsageStatsServiceImpl.java`
- `server/src/main/java/com/airesume/server/mapper/UserAiUsageDetailMapper.java`
- 后端完整记录见 `tasks/TASK_79_CUSTOM_AI_USAGE_STATS_DATE_RANGE_BACKEND.md`。

## 本轮修改文件清单

- `aiEngines.js` 的 `getCustomAiUsageStats` 支持 `startDate/endDate` 查询参数，并保留 `date` 单日兼容参数。
- `AdminAiEngineView.vue` 将自定义 AI 用量统计区的单日日期选择器改为 `daterange`。
- `AdminAiEngineView.vue` 新增“今天 / 近 7 天 / 近 30 天 / 自定义”快捷范围切换，默认近 7 天。
- `AdminAiEngineView.vue` 功能分布、汇总卡片、用户明细和分页请求全部共用同一日期范围。
- `AdminAiEngineView.test.js` 补齐默认近 7 天、近 30 天切换、自定义范围、分页同范围和页面文案断言。
- `admin.aiEngines.test.js` 补齐旧 `date` 兼容和新 `startDate/endDate` 参数断言。

## 前端实现方案

- 继续复用管理端 AI 引擎配置页的“自定义 AI 用量”分区，不新增页面、路由或首页图表。
- 范围状态由 `customAiUsageRangePreset` 与 `customAiUsageRange` 管理，默认值为近 7 天。
- 日期范围变化后重置用户明细页码到第一页，避免沿用旧范围下的分页位置。
- 控件行允许换行，移动端日期范围选择器占满可用宽度，避免新增按钮组挤压布局。

## 后端实现方案

前端调用后端扩展后的接口：

- `GET /api/admin/custom-ai/usage-stats`

查询参数：

- `date?: yyyy-MM-dd`，旧版单日兼容参数
- `startDate?: yyyy-MM-dd`
- `endDate?: yyyy-MM-dd`
- `page?: number`
- `pageSize?: number`

## 数据存储方案

前端不新增本地持久化字段。统计结果以后端范围聚合为准；本轮不新增数据库表、字段、索引或迁移脚本。

## stage 更新说明

`frontend/tasks/stage.md` 已记录本轮功能 1 完成状态、验证结果和停止范围；根目录 `tasks/stage.md` 同步记录后端范围聚合。

## 编译结果

- 前端 RED 验证：旧实现下 `npm.cmd test -- --run src/__tests__/api/admin.aiEngines.test.js src/__tests__/views/AdminAiEngineView.test.js` 失败，复现仍发送单日 `date`、缺少范围切换 handler 和默认近 7 天范围请求。
- 前端 GREEN 验证：同一目标测试命令通过，2 个测试文件 / 20 个用例。
- 前端全量测试：`npm.cmd test` 通过，80 个测试文件 / 574 个用例。

## 构建结果

- `npm.cmd run build` 通过。

## 当前功能验收说明

管理员切换到“自定义 AI 用量”分区后，功能分布和用户明细默认展示近 7 天数据；点击今天、近 30 天或选择自定义范围后，汇总、功能分布、用户明细和分页都以同一范围重新请求。

## 停止，不继续下一个功能

本轮只完成 `develop-project.txt` 功能 1 的前端范围筛选和接口参数联动，不继续推进系统级 TTS 配置、TTS UI、云端 TTS、音频存储或新的管理端统计页面。
