# 免费用户六项功能额度统一为 100 次（后端）

## 当前任务所属模块
- 后端用户额度、数据库迁移与高成本 AI 接口限流。

## 前端文件定位
- 本轮不修改前端。用户端继续读取 `/api/auth/me` 返回的六项剩余额度，无需适配接口或展示结构。

## 后端文件定位
- `server/src/main/java/com/airesume/server/common/constants/QuotaConstants.java`
- `server/src/main/java/com/airesume/server/service/impl/UserQuotaServiceImpl.java`
- `server/src/main/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilter.java`
- `server/src/test/java/com/airesume/server/common/constants/QuotaConstantsTest.java`
- `server/src/test/java/com/airesume/server/service/impl/UserQuotaServiceImplTest.java`
- `server/src/test/java/com/airesume/server/infrastructure/security/CriticalEndpointRateLimitFilterTest.java`
- `server/src/test/java/com/airesume/server/db/FreeUserQuotaMigrationTest.java`

## 本轮修改文件清单
- `QuotaConstants.java`：简历诊断、模拟面试、AI 润色、JD 匹配、模板使用、Offer 辅助的免费额度统一为 100。
- `CriticalEndpointRateLimitFilter.java`：为润色和 JD 匹配新增每账号每项 10 次/10 分钟的突发限频。
- `UserQuotaServiceImplTest.java`、`QuotaConstantsTest.java`：验证常量与新用户真实初始化落库对象的六项额度均为 100。
- `CriticalEndpointRateLimitFilterTest.java`：验证润色和 JD 匹配第 11 次突发请求返回 429。
- `FreeUserQuotaMigrationTest.java`：验证双份迁移和 schema 同步、免费用户判定边界与不降低高额度规则。
- `db/schema.sql`、`server/db/schema.sql`：同步六个额度字段的数据库默认值为 100。
- `db/migrations/TASK_90_FREE_USER_QUOTA_100.sql`、`server/db/migrations/TASK_90_FREE_USER_QUOTA_100.sql`：新增存量数据迁移。

## 前端实现方案
- 无前端改动。当前前端已直接使用后端返回的剩余额度，数据更新后会自然展示新值。

## 后端实现方案
- 继续复用现有 `UserQuotaServiceImpl.initUserQuota()` 初始化链路，只调整其引用的六项业务常量。
- 保留现有原子扣减 SQL、消费日志、接口响应和异常行为；额度扣到 0 后仍会拒绝继续使用。
- 免费 100 次为一次性剩余额度，不做每日刷新，也不改 VIP 套餐额度。
- 润色和 JD 匹配仍保留合理的突发限频，防止免费额度提高后被短时间批量刷取平台 AI 成本。

## 数据存储方案
- 两份 schema 的六列默认值统一为 100，避免脱离应用初始化写入时出现旧默认值。
- 存量迁移为所有非管理员账号补足免费基础储备，保证当前会员日后到期也能获得新基线；VIP 套餐日额度不变。
- 使用 `GREATEST(当前值, 100)` 补足额度，不降低管理员已手工配置的高于 100 的额度。
- 不重置累计使用量、每日使用量、周期使用量和消费日志，历史统计保持不变。

## stage 更新说明
- 已在 `tasks/stage.md` 记录本轮实现、测试与生产验收状态。

## 编译结果
- `mvn.cmd -q -DskipTests compile` 通过。

## 构建结果
- `mvn.cmd -q -DskipTests package` 通过，后端 jar 构建成功。

## 测试结果
- RED：额度常量测试在旧值 `3/1/1/1/2/1` 下六项失败；迁移测试因脚本不存在且 schema 默认值不符而失败。
- RED：润色/JD 限频测试确认旧实现第 11 次请求仍被放行。
- GREEN：额度、迁移、schema、初始化、原子扣减和限频目标测试全部通过。
- 全量测试使用测试占位 `DOUBAO_API_KEY` 通过，855 个用例全部通过；不设置该测试环境变量时，仅既有 `InterviewAiServiceImplTest.shouldReplaceStreamingPromptLeakSplitAcrossChunks` 因缺少测试 Key 报错，与本轮额度逻辑无关。

## 当前功能验收说明
- 新注册用户会初始化为六项各 100 次。
- 存量非管理员账号会补足到至少 100 次，已有更高人工额度不降低；VIP 套餐日额度不变。
- 管理员不被存量迁移改动。
- 润色与 JD 匹配每账号每项最多 10 次/10 分钟，窗口后可继续使用剩余额度。
- 生产数据库迁移、服务替换和域名验收已完成，具体结果见下方生产部署结果。

## 生产部署结果
- 2026-08-18 使用源码提交 `4b1b887` 对应的本地已验证 Jar 上传部署，服务器未执行 Maven、Node 或前端构建，未停止 Nginx、MySQL 或其它项目。
- 上线前备份目录为 `/opt/offercat/backups/quota100-retry-20260818-010705`，包含旧 `app.jar` 和完整 `user_quota` 表结构及数据。
- 迁移在 OfferCat 服务停止期间执行：首次补足 7 个非管理员账号，第二次执行更新 0 行，证明数据更新幂等。
- 迁移后 7 个非管理员账号的六项最低额度均为 100；6 个数据库列默认值均为 100；管理员额度保持 `3/1/1/1/2/1` 不变。
- 生产 Jar SHA-256 为 `7d9856d9a9953aff065f8ca9a1212de025ba819b6cba24299d0759d3cadf6320`，属主和权限为 `offercat:offercat 640`。
- `offercat.service` 与 `nginx` 均为 `active`，`/actuator/health` 返回 `200` 且状态为 `UP`。
- `https://kelin.cyou/` 与 `https://kelin.cyou/api/stats` 均返回 `200`；图片上传接口继续返回 `code=451` 和“图床服务现在还未开放”，未改变既有禁用策略。
- 上线后服务器约有 2.4 GiB 可用内存、24 GiB 可用磁盘，未发现 Maven、Gradle、npm、Vite 或 Webpack 构建进程。

## 停止说明
- 本轮只调整免费额度及其直接安全边界，不继续修改会员套餐、支付、前端页面或其它功能。
