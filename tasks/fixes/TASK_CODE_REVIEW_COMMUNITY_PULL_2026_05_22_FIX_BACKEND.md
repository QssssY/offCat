# TASK：2026-05-22 社区 Pull 代码审查修复（后端）

## 当前任务所属模块
- 社区功能后端、安全配置、上传样例治理。

## 后端文件定位
- `server/src/main/java/com/airesume/server/config/SecurityConfig.java`
- `server/src/main/java/com/airesume/server/service/CommunityService.java`
- `server/src/main/java/com/airesume/server/dto/community/CreateCommentRequest.java`
- `server/src/test/java/com/airesume/server/service/CommunityServiceInteractionTest.java`
- `server/uploads/`
- `.gitignore`

## 本轮修改文件清单
- 恢复 Actuator 和诊断接口安全边界，仅公开 `/actuator/health` 与 `/actuator/info`。
- 将 `CriticalEndpointRateLimitFilter` 挂回 JWT 过滤器之后，恢复关键路径限流。
- 修复社区互动、未读统计、评论过帖子查询、删除级联和上传文件名校验测试失败。
- 收紧上传静态资源边界，仅公开 `/uploads/community/**`，避免简历上传文件经静态路径匿名访问。
- 修复评论过帖子分页总数和非法页码兜底，已删除帖子不计入有效总数。
- 评论请求恢复文本必填校验，避免空白评论进入互动链路。
- 删除 `server/uploads` 下样例上传文件，并在根 `.gitignore` 增加上传目录忽略。

## 后端实现方案
- 安全链路保持最小改动：保留现有 JWT 过滤器位置，限流过滤器通过 `addFilterAfter` 接入认证上下文之后。
- 社区互动查询先限定当前用户帖子集合，再查询点赞、评论、回复、收藏，避免全表互动数据无边界扫描。
- 评论过帖子查询先过滤有效帖子再分页，服务层分页入口对非法页码和页大小做最小兜底。
- 未读统计按点赞、顶级评论、回复、收藏四类分别计数，保留规则可读性。
- 删除帖子先逻辑删除主帖，再清理评论、点赞、收藏；异常继续向上抛出，依赖事务回滚。

## 数据存储方案
- 不新增数据库表或字段。
- 不修改本轮已完成的 TASK_56 社区整合迁移。

## 验证记录
- 定向后端测试：`mvn.cmd test "-Dtest=CommunityServicePostQueryDeleteTest,SecurityConfigTest,CommunityServiceInteractionTest"` 通过，22 个测试通过。
- 完整后端测试：`mvn.cmd test` 通过，511 个测试通过。

## 停止说明
- 本轮只处理 `docs/CODE_REVIEW_COMMUNITY_PULL_2026_05_22.md` 中发布阻断项和测试失败项，不继续扩展社区功能。
