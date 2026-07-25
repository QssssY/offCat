# 会员套餐权益乱码修复

## 当前任务所属模块
- 后端会员模块。
- 关联接口：`GET /api/membership/plans`。
- 关联数据表：`membership_plan`。

## 后端文件定位
- `server/src/main/java/com/airesume/server/service/impl/MembershipServiceImpl.java`
- `server/src/main/java/com/airesume/server/entity/MembershipPlan.java`
- `server/src/main/java/com/airesume/server/vo/membership/MembershipPlanVO.java`

## 本轮修改文件清单
- `db/migrations/fix_benefits_encoding_hex.sql`
- `db/migrations/TASK_57_MEMBERSHIP_QUOTA_ENHANCEMENT.sql`
- `server/db/migrations/fix_benefits_encoding_hex.sql`
- `server/db/migrations/TASK_57_MEMBERSHIP_QUOTA_ENHANCEMENT.sql`
- `server/db/schema.sql`
- `server/src/test/java/com/airesume/server/db/MembershipBenefitsEncodingMigrationTest.java`
- `tasks/fixes/TASK_MEMBERSHIP_BENEFITS_ENCODING_2026_05_28_BACKEND.md`
- `stage.md`

## 后端实现方案
- 确认接口层只是把 `membership_plan.benefits` 的 JSON 字符串反序列化为 `List<String>`，没有额外转码逻辑。
- 因为 `planName`、`description`、`message` 中文正常，只有 `benefits` 乱码，所以问题定位为 `benefits` 字段入库内容已被错误编码污染。
- 不在接口中增加猜测式乱码反转码，避免掩盖脏数据并影响后台套餐编辑、缓存和后续展示链路。
- 将一次性修复脚本改为 `CONVERT(0x... USING utf8mb4)`，用真实 UTF-8 十六进制字节写入 JSON 字符串，绕过 SQL 客户端文件编码差异。
- 同步修正 TASK-57 两份迁移脚本和 `server/db/schema.sql` 种子数据，避免新环境重放迁移或导入 schema 时再次写入乱码。

## 数据存储方案
- 不新增表。
- 不修改字段。
- 修复 `membership_plan.benefits` 现有数据时，执行：
  - `db/migrations/fix_benefits_encoding_hex.sql`
  - 或等价的 `server/db/migrations/fix_benefits_encoding_hex.sql`
- 修复后接口应返回：
  - `AI 简历润色（每份简历 1 次）`
  - `JD 岗位匹配分析（每日 3 次）`
  - `简历模板库（每日 5 次使用）`
  - `Offer 薪资谈判辅助（每日 3 次）`
  - `模拟面试（每日 10 次）`
  - `简历诊断（每日 5 次）`

## stage 更新说明
- 已在根目录 `stage.md` 追加本轮会员套餐权益乱码修复状态。

## 编译与测试结果
- SQL 十六进制校验：通过，5 个 SQL 文件中的 hex 内容均可还原为正确中文 JSON，且不再包含 `\x` 伪十六进制写法。
- 后端编译：`mvn.cmd -DskipTests compile` 通过。
- 定向 Maven 测试：`mvn.cmd test "-Dtest=MembershipBenefitsEncodingMigrationTest"` 未执行到新增测试，当前仓库已有 test 源码存在 8 处编译错误，均为既有测试与当前生产代码签名不匹配，阻塞 `testCompile`。

## 当前功能验收说明
- 对当前运行数据库执行 `fix_benefits_encoding_hex.sql` 后，再刷新会员套餐缓存或重启后端，`GET /api/membership/plans` 的 `benefits` 应恢复正常中文。
- 本轮不改接口 URL、不改响应结构、不改会员业务规则。

## 停止说明
- 本轮只处理会员套餐权益 `benefits` 乱码数据修复与脚本防回归，不继续扩展会员中心功能、不调整套餐额度规则、不处理仓库既有测试签名不匹配问题。
