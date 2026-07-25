# 社区个人动态 DTO 类加载修复

## 当前任务所属模块
- 后端社区模块。
- 关联接口：`GET /api/community/my/comments`、`GET /api/community/my/interactions`。

## 后端文件定位
- `server/src/main/java/com/airesume/server/dto/community/MyCommentVO.java`
- `server/src/main/java/com/airesume/server/dto/community/ReceivedInteractionVO.java`
- `server/src/main/java/com/airesume/server/service/CommunityService.java`

## 本轮修改文件清单
- `server/src/main/java/com/airesume/server/dto/community/MyCommentVO.java`
- `server/src/main/java/com/airesume/server/dto/community/ReceivedInteractionVO.java`
- `server/src/main/java/com/airesume/server/service/CommunityService.java`

## 后端实现方案
- 修复个人动态中心“评论过的帖子”接口运行时报 `NoClassDefFoundError: MyCommentVO$MyCommentVOBuilder` 的问题。
- `MyCommentVO` 移除 Lombok `@Builder`，`CommunityService.listMyComments(...)` 改为显式 `new MyCommentVO()` + setter 组装，避免热更新或增量编译环境下依赖 Lombok 生成的内部 Builder 类。
- `ReceivedInteractionVO` 及内部互动条目类移除 Lombok `@Builder`，补充显式静态 Builder 类，保持现有服务层链式调用源码兼容，同时不再依赖 Lombok 生成的 `$Builder` 类。
- 不修改接口 URL、响应字段和数据库结构。

## 数据存储方案
- 不新增表。
- 不修改字段。
- 不新增迁移脚本。

## stage 更新说明
- 已在根目录 `runtime/STATE.md` 追加本轮后端修复状态。
- 前端虚拟滚动接入记录在 `frontend/tasks/TASK_COMMUNITY_ACTIVITY_VIRTUAL_SCROLL_2026_05_23_FRONTEND.md`。

## 编译与测试结果
- 后端定向测试：`mvn.cmd test "-Dtest=CommunityServiceInteractionTest,CommunityServiceReceivedInteractionsEmptyTest,CommunityServicePostQueryDeleteTest"` 通过，19 个用例通过。

## 当前功能验收说明
- 点击个人动态中心“评论过的帖子”时，后端不再因 `MyCommentVO$MyCommentVOBuilder` 类缺失返回 500。
- 点击“收到的点赞”等互动 Tab 时，互动响应 DTO 保持原有字段结构，服务层构造链路可正常编译和测试通过。

## 停止说明
- 本轮只修复社区个人动态 DTO 类加载问题，不新增社区业务能力，不调整数据库结构。
