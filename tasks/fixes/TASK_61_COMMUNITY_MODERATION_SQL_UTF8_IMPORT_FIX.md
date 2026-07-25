# TASK_61 社区审核迁移脚本 UTF-8 导入修复

## 当前任务所属模块

社区内容审核后端数据迁移兼容性修复。目标是修复 `TASK_61_COMMUNITY_CONTENT_MODERATION.sql` 在部分 MySQL 数据库默认字符集不是 `utf8mb4` 时，导入中文列注释触发 `ERROR 1366 Incorrect string value` 的问题。

## 后端文件定位

- `db/migrations/TASK_61_COMMUNITY_CONTENT_MODERATION.sql`
- `server/db/migrations/TASK_61_COMMUNITY_CONTENT_MODERATION.sql`
- `server/src/test/java/com/airesume/server/db/SchemaConsistencyTest.java`

## 本轮修改文件清单

- 修改：`db/migrations/TASK_61_COMMUNITY_CONTENT_MODERATION.sql`
- 修改：`server/db/migrations/TASK_61_COMMUNITY_CONTENT_MODERATION.sql`
- 修改：`server/src/test/java/com/airesume/server/db/SchemaConsistencyTest.java`
- 新增：`tasks/fixes/TASK_61_COMMUNITY_MODERATION_SQL_UTF8_IMPORT_FIX.md`
- 修改：`tasks/stage.md`

## 后端实现方案

- 在 TASK_61 两份迁移脚本开头补充 `SET NAMES utf8mb4;`，确保 MySQL 客户端会话按 UTF-8 解析脚本中的中文注释和中文列说明。
- 将 `add_column_if_missing` 和 `add_index_if_missing` 存储过程的表名、列名、索引名、动态 DDL 参数显式声明为 `CHARACTER SET utf8mb4`，避免目标库默认字符集不是 UTF-8 时，中文 `COMMENT` 文本在传入过程参数阶段被拒收。
- 保持迁移脚本幂等逻辑不变，仍按字段名和索引名检查是否已存在；用户可直接重新执行修复后的脚本补齐失败的审核字段和索引。

## 数据存储方案

本轮不新增业务字段，不改变表结构设计，仅修复已有迁移脚本的字符集导入兼容性。审核字段、审核原因、审核人、审核时间和审核索引仍沿用 TASK_61 原方案。

## stage 更新说明

已在 `tasks/stage.md` 记录 TASK_61 社区审核迁移脚本 UTF-8 导入修复状态和验证命令。

## 编译结果

已通过：

```bash
mvn.cmd -q "-Dtest=SchemaConsistencyTest#shouldKeepCommunityModerationMigrationUtf8SafeAndInSync" test
```

## 当前功能验收说明

- 新增回归测试先复现 TASK_61 迁移脚本缺少 UTF-8 参数保护的问题。
- 修复后测试通过，确认 `db/` 与 `server/db/` 两份 TASK_61 迁移脚本保持一致，并且包含 `SET NAMES utf8mb4;` 与显式 `CHARACTER SET utf8mb4` 参数声明。
- 用户之前失败的数据库可重新执行：

```sql
source F:\Code\ai-resume\server\db\migrations\TASK_61_COMMUNITY_CONTENT_MODERATION.sql
```

## 停止，不继续下一个功能

本轮只修复社区审核迁移脚本导入失败问题，不扩展云审核、图片 AI 鉴黄、举报中心或批量审核能力。
