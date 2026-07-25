# 社区帖子标题与面试报告分享链接修复

## 当前任务所属模块
- 后端社区模块。
- 后端模拟面试报告详情读取授权链路。
- 关联前端任务：`frontend/tasks/TASK_COMMUNITY_POST_TITLE_AND_REPORT_LINK_FRONTEND.md`。

## 前端文件定位
- `frontend/app/src/components/community/PostEditor.vue`
- `frontend/app/src/components/community/PostCard.vue`
- `frontend/app/src/components/community/ShareReportDialog.vue`
- `frontend/app/src/views/community/PostDetailView.vue`
- `frontend/app/src/views/community/MyActivity.vue`

## 后端文件定位
- `server/src/main/java/com/airesume/server/service/CommunityService.java`
- `server/src/main/java/com/airesume/server/service/InterviewService.java`
- `server/src/main/java/com/airesume/server/dto/community/CreatePostRequest.java`
- `server/src/main/java/com/airesume/server/dto/community/PostVO.java`
- `server/src/main/java/com/airesume/server/entity/CommunityPost.java`

## 本轮修改文件清单
- 后端社区发帖请求、帖子实体和帖子 VO 增加 `title` 与 `sharedInterviewSessionId` 字段。
- 社区发帖服务增加标题必填校验，并在报告分享帖创建前校验会话归属。
- 面试报告详情读取链路增加社区分享授权判断，跨用户访问只允许读取已由报告所有者发布到社区且未删除的报告。
- SQL 双副本同步增加 `community_post.title`、`community_post.shared_interview_session_id` 和对应索引。
- 已补充旧库专用增量迁移 `TASK_59_COMMUNITY_POST_TITLE_AND_REPORT_LINK_INCREMENTAL.sql`，用于已经执行过旧版 TASK_56 的数据库。
- 社区种子脚本补齐标题字段，避免新增非空标题字段后初始化数据缺失。
- 前端社区发布、帖子卡片、帖子详情、报告分享弹窗和个人动态中心已同步适配，具体记录见前端任务文件。

## 前端实现方案
- 普通社区发帖要求输入标题，提交时把标题传给后端。
- 面试报告分享到社区时不再拼接完整报告正文，只提交报告标题、简短说明和 `sharedInterviewSessionId`。
- 社区帖子列表、详情页和个人动态中心根据 `sharedInterviewSessionId` 渲染站内报告链接。
- 个人动态中心默认分页大小从 2 调整为 5。

## 后端实现方案
- `CreatePostRequest`、`CommunityPost`、`PostVO` 增加标题和报告会话 ID 字段，保持列表、详情和个人动态接口响应结构稳定扩展。
- `CommunityService.createPost(...)` 对标题做空值和长度校验；当存在 `sharedInterviewSessionId` 时，通过 `InterviewSessionMapper` 校验该会话属于当前发布用户且未删除。
- `InterviewService.getSessionDetail(...)` 保留本人查看完整会话详情的原链路；非本人访问时，只在存在未删除社区分享帖且分享帖 `user_id` 等于报告所有者时返回报告级数据。
- 跨用户报告响应不返回聊天记录、复盘轮次和岗位上下文，避免社区分享扩大敏感会话数据暴露范围。

## 数据存储方案
- `db/schema.sql` 与 `server/db/schema.sql` 已同步增加：
  - `title VARCHAR(120) NOT NULL DEFAULT '未命名帖子'`
  - `shared_interview_session_id VARCHAR(64) NULL`
  - `idx_community_post_shared_interview_session_id`
- `db/migrations/TASK_56_COMMUNITY_PULL_FULL_MIGRATION.sql` 与 `server/db/migrations/TASK_56_COMMUNITY_PULL_FULL_MIGRATION.sql` 已同步新增字段和索引补齐逻辑。
- `db/migrations/TASK_59_COMMUNITY_POST_TITLE_AND_REPORT_LINK_INCREMENTAL.sql` 与 `server/db/migrations/TASK_59_COMMUNITY_POST_TITLE_AND_REPORT_LINK_INCREMENTAL.sql` 已新增旧库增量迁移；如果目标库此前已执行旧版 TASK_56，只需要执行 TASK_59。
- `server/db/migrations/alter_v1.4_add_community_tables.sql` 保留为历史建表脚本，并已在文件头标明不用于已有社区表补字段。
- `db/community_seed_existing_users.sql` 与 `db/community_seed_root_user.sql` 已补齐 seed 标题字段。

## 数据库执行说明
- 已执行过旧版 TASK_56 的数据库：执行 `server/db/migrations/TASK_59_COMMUNITY_POST_TITLE_AND_REPORT_LINK_INCREMENTAL.sql`。
- 尚未执行过社区整合迁移的新数据库：执行 `server/db/migrations/TASK_56_COMMUNITY_PULL_FULL_MIGRATION.sql`。
- 不建议用 `server/db/migrations/alter_v1.4_add_community_tables.sql` 给已有社区表补本轮字段，因为 `CREATE TABLE IF NOT EXISTS` 在表已存在时会跳过，不会追加字段。
- `schema.sql` 只作为全量建库参考，不作为线上增量迁移执行。

## stage 更新说明
- 根目录 `stage.md` 已更新本轮后端与数据库完成状态。
- 前端阶段记录已写入 `frontend/tasks/stage.md`。

## 编译与测试结果
- 后端定向测试：`mvn.cmd test "-Dtest=CommunityServiceValidationTest,CommunityServicePostQueryDeleteTest,InterviewServiceTest,CommunityServiceLikeFavoriteTest,CommunityServiceInteractionTest,CommunityServiceReceivedInteractionsEmptyTest"` 通过，79 个用例通过。
- 后端编译：`mvn.cmd compile` 通过。
- 前端定向测试：`npm.cmd test -- --run src/__tests__/components/community/PostEditor.test.js src/__tests__/components/community/PostCard.test.js src/__tests__/components/community/ShareReportDialog.test.js src/__tests__/views/community/MyActivity.test.js src/__tests__/views/community/PostDetailView.test.js` 通过，5 个测试文件 / 13 个用例通过。
- 前端构建：`npm.cmd run build` 通过。

## 当前功能验收说明
- 社区新发帖必须有标题，列表、详情和个人动态中心均展示标题。
- 个人动态中心每次默认加载 5 条记录。
- 面试报告分享到社区后以链接卡片形式展示，其他登录用户点击可进入该发布用户分享的报告页。
- 只有报告所有者发布的未删除社区分享帖可以授权他人访问报告；用户不能构造他人 `sessionId` 来公开他人报告。
- 跨用户报告访问只返回报告页展示所需字段，不返回原始聊天记录和复盘过程数据。

## 停止说明
- 本轮只完成社区标题、个人动态默认加载数量和面试报告社区链接分享修复，不继续扩展报告下载、富文本分享、公开报告目录或其它社区能力。
