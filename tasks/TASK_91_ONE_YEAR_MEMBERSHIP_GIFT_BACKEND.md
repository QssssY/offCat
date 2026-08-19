# 新老用户赠送一年会员（后端）

## 当前任务所属模块
- 后端注册、会员身份、站内通知与存量用户迁移。

## 前端文件定位
- 本轮不修改前端。用户端继续通过 `/api/auth/me` 和现有通知接口读取会员状态与站内通知。

## 后端文件定位
- `server/src/main/java/com/airesume/server/common/constants/MembershipConstants.java`
- `server/src/main/java/com/airesume/server/service/impl/AuthServiceImpl.java`
- `server/src/main/java/com/airesume/server/service/NotificationService.java`
- `server/src/test/java/com/airesume/server/service/impl/AuthServiceImplTest.java`
- `server/src/test/java/com/airesume/server/service/NotificationServiceTest.java`
- `server/src/test/java/com/airesume/server/db/OneYearMembershipGiftMigrationTest.java`

## 本轮修改文件清单
- `MembershipConstants.java`：新增年度套餐和会员赠送业务标识常量。
- `AuthServiceImpl.java`：注册时校验年度套餐启用，写入一年会员身份，并在同一事务创建站内通知。
- `NotificationService.java`：新增异常不吞、加入调用方事务的严格通知入口，同时保留既有宽松通知行为。
- `AuthServiceImplTest.java`、`NotificationServiceTest.java`：覆盖注册赠送、套餐缺失、通知失败和通知零行写入。
- `OneYearMembershipGiftMigrationTest.java`：验证双份迁移一致、事务、管理员保护和幂等标识。
- `db/migrations/TASK_91_ONE_YEAR_MEMBERSHIP_GIFT.sql`、`server/db/migrations/TASK_91_ONE_YEAR_MEMBERSHIP_GIFT.sql`：补发存量非管理员用户。

## 前端实现方案
- 无前端改动。会员字段与通知数据结构均复用现有接口，不新增页面或协议。

## 后端实现方案
- 新用户注册时直接写入 `role=1`、`membership_plan_code=vip_year`、`vip_expire_time=注册时间+1年`。
- 注册前确认 `vip_year` 套餐处于启用状态，配置异常时快速失败，不创建权益无效的账号。
- 用户、额度和赠送通知共用注册事务；通知写入异常或影响行数不是 1 时向上抛出，注册整体回滚。
- 不调用模拟付费升级，不创建会员订单，避免污染订单和收入统计。

## 数据存储方案
- 不新增表或字段，两份 schema 无需修改。
- 存量迁移选择未获本活动赠送的有效非管理员账号，补到至少迁移时点后一年，并统一为 `vip_year`。
- 已有更长会员期限不会被缩短；固定 `biz_type=membership_gift` 与 `biz_id=existing_user_one_year_20260819` 作为幂等标识。
- 管理员继续保持 `role=9`，不参与迁移，避免覆盖角色后丢失管理权限。
- 不重置免费额度、每日用量、周期用量或历史消费记录。

## stage 更新说明
- 已在 `tasks/stage.md` 记录本轮实现和本地验证状态。

## 编译结果
- `mvn.cmd -q -DskipTests package` 通过，包含编译验证。

## 构建结果
- 后端 Jar 已在本地构建成功，服务器不执行 Maven 或前端构建。

## 测试结果
- RED：旧代码缺少严格通知入口，新增注册与通知测试编译失败。
- RED：严格通知在 Mapper 返回 0 行时未抛异常，回归测试失败。
- GREEN：`AuthServiceImplTest,NotificationServiceTest,OneYearMembershipGiftMigrationTest,SchemaConsistencyTest,MembershipServiceImplTest` 全部通过。
- 全量测试运行 861 个用例，本任务相关测试通过；仅既有 `InterviewAiServiceImplTest.shouldReplaceStreamingPromptLeakSplitAcrossChunks` 因本机未配置 `DOUBAO_API_KEY` 报错，与本轮会员逻辑无关。

## 当前功能验收说明
- 新用户只有在用户、额度、一年会员和通知全部成功后才算注册成功。
- 老用户迁移可重复执行，不重复延期或重复通知，管理员权限不受影响。
- 不修改前端、会员订单、免费 100 次额度和图床禁用策略。

## 生产部署结果
- 2026-08-19 使用本地构建 Jar 部署，服务器未执行 Maven、Node 或前端构建，未停止 Nginx、MySQL 或其它项目。
- 备份目录为 `/opt/offercat/backups/membership-20260819-224238`，包含旧 Jar、迁移脚本以及 `sys_user`、`user_notification` 完整备份。
- 生产迁移首次补发 9 个非管理员账号并创建 9 条通知；第二次执行后用户会员字段哈希及通知数量完全不变，验证迁移幂等。
- 管理员仍为 `role=9`，会员字段保持未设置；9 个非管理员账号均为有效 `vip_year` 会员。
- 生产 Jar SHA-256 为 `3d13c0c8f6dc3d0e89a3745f7ee56c692e9d06c4d02635a0d0ecbd86f11f827a`，属主和权限为 `offercat:offercat 640`。
- `offercat.service`、`nginx`、`mysql` 均为 `active`，本机健康检查返回 `200 UP`，启动期无 systemd ERROR 日志。
- `https://kelin.cyou/`、`/api/stats`、`/api/version-logs/latest?limit=5`、`/actuator/health` 均返回 `200`。
- 部署时发现公开版本日志修复此前只存在本地 stash、未进入 master；已通过提交 `3beffd3` 将精确匿名 GET 规则正式纳入仓库，未放行写操作或管理端路径。
- 迁移安全复审后通过提交 `4aaab90` 在更新和通知阶段再次检查非管理员状态，防止迁移并发期间覆盖管理员角色。
- 上线后服务器约有 2.4 GiB 可用内存、24 GiB 可用磁盘，无 Maven、Gradle、npm、Vite 或 Webpack 构建进程。

## 停止说明
- 本轮只实现一年会员赠送与通知，不继续扩展支付、角色模型、前端会员页面或其它功能。
