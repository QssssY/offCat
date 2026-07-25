# 最新 Pull 代码审查报告（前后端社区功能）

审查日期：2026-05-22

审查范围：
- 后端工作区：`server`
- 前端工作区：`frontend/app`
- 本次 pull 主要内容：社区帖子、评论、回复、图片上传、点赞、收藏、个人动态与互动入口。

## 结论

本次 pull 的社区功能存在发布阻断问题。已在本轮修复数据库全量 schema 的损坏问题，并新增整合迁移脚本；但代码层仍存在测试失败、安全配置放宽和部分交互测试不稳定问题，建议在合入生产前继续修复。

## Critical

### 1. 后端全量 schema 被写坏

位置：
- `server/db/schema.sql`

问题：
- 社区表插入到了管理端表结构中间，导致 `community_post`、`sys_admin_notification`、`sys_version_log`、`sys_growth_config` 等表定义互相串行。
- 全量初始化数据库时会出现 SQL 语法错误或错误字段/索引落到错误表。

本轮处理：
- 已修复 `server/db/schema.sql`。
- 已同步更新根目录 `db/schema.sql`。
- 已新增单文件整合迁移：`db/migrations/TASK_56_COMMUNITY_PULL_FULL_MIGRATION.sql`。
- 已同步到：`server/db/migrations/TASK_56_COMMUNITY_PULL_FULL_MIGRATION.sql`。

### 2. 安全边界被放宽

位置：
- `server/src/main/java/com/airesume/server/config/SecurityConfig.java`

问题：
- `/actuator/**` 被设置为 `permitAll`，不再只开放 health/info。
- `/api/diagnostic/**` 被设置为 `permitAll`，诊断接口可被未登录用户访问。
- `CriticalEndpointRateLimitFilter` 未进入安全过滤链，注册、重置密码、AI/面试等关键路径失去限流保护。

建议：
- 仅开放 `/actuator/health` 和 `/actuator/info`。
- `/actuator/**` 和 `/api/diagnostic/**` 恢复管理员权限。
- 将 `CriticalEndpointRateLimitFilter` 重新挂回 JWT 之后的过滤链。

## Major

### 1. 后端测试失败

命令：
- `mvn test`

结果：
- 507 tests，7 failures，9 errors。

失败集中在：
- `CommunityServiceInteractionTest`
- `CommunityServicePostQueryDeleteTest`
- `CommunityServiceValidationTest`

主要问题：
- 收到互动和未读互动统计不符合测试预期。
- 评论过的帖子分页查询在 mock 返回空时出现 NPE。
- 删除帖子级联逻辑与测试期望不一致。
- 评论内容校验契约不清晰：当前实现允许“无文本但有图片”的评论，但测试期望文本不可为空。

### 2. 前端测试失败

命令：
- `npm.cmd test`

结果：
- 37 个测试文件中 2 个失败。
- 240 tests，7 failed，另有 10 个 unhandled errors。

失败集中在：
- `src/__tests__/components/community/ImageGrid.test.js`
- `src/__tests__/views/SettingsView.test.js`

主要问题：
- `ImageGrid` 的 Teleport + Transition 关闭流程在测试环境中残留 overlay。
- Escape 关闭图片预览触发 Vue DOM patch 异常。
- `SettingsView` 的账号注销文案和提交流程测试与当前实现不一致。

### 3. 上传样例文件进入 Git

位置：
- `server/uploads/community/*`
- `server/uploads/resumes/1778240639716_简历.pdf`

问题：
- 用户上传内容和运行样例文件不应进入代码仓库。
- 简历 PDF 可能包含个人信息。

建议：
- 将上传样例从 Git 移除。
- 确认 `.gitignore` 覆盖 `uploads/` 目录。
- 若已推送公开仓库，评估是否需要清理历史记录。

## 本轮已完成的修复

1. 修复 `server/db/schema.sql` 中损坏的表结构。
2. 更新 `db/schema.sql`，补齐社区表结构并保持与后端 schema 一致。
3. 新增单文件整合迁移 SQL：
   - `db/migrations/TASK_56_COMMUNITY_PULL_FULL_MIGRATION.sql`
   - `server/db/migrations/TASK_56_COMMUNITY_PULL_FULL_MIGRATION.sql`

## 验证记录

已执行：
- `npm.cmd run build`：通过。
- `npm.cmd test`：失败，见前端测试失败说明。
- `mvn test`：失败，见后端测试失败说明。

本轮数据库 SQL 已做静态结构检查；尚未连接真实 MySQL 执行迁移。
