# STAGE_TASK_CORE_BUGFIX_ROUND_7D

## 当前阶段结论

- 已完成 `/me` 接口额度字段语义回正
- 已完成 VIP 个人中心总额度与日额度拆分展示
- 已完成 VIP 先扣日额度、后扣总额度的数据库真实扣减
- 已完成普通用户只扣总额度的逻辑收口
- 本轮到此停止，等待验收

## 本轮关键事实

### 修复前

- `/me.resumeQuota` 对 VIP 实际返回今日日额度剩余
- `/me.interviewQuota` 对 VIP 实际返回今日日额度剩余
- 个人中心第一行错误显示了日额度
- VIP 扣减逻辑不支持日额度耗尽后继续扣总额度

### 修复后

- `/me.resumeQuota = user_quota.resume_quota`
- `/me.interviewQuota = user_quota.interview_quota`
- `/me.vipDailyResumeQuota = 会员日限额 - daily_resume_used`
- `/me.vipDailyInterviewQuota = 会员日限额 - daily_interview_used`
- `/me.isVip = 后端统一判断结果`

## 代码落点

- 后端：
  - `server/src/main/java/com/airesume/server/service/impl/AuthServiceImpl.java`
  - `server/src/main/java/com/airesume/server/service/impl/UserQuotaServiceImpl.java`
  - `server/src/main/java/com/airesume/server/service/UserQuotaService.java`
  - `server/src/main/java/com/airesume/server/dto/auth/UserInfoResponse.java`
- 前端：
  - `frontend/app/src/views/DashboardView.vue`

## 验证结果

- 后端编译：`mvn.cmd -q -DskipTests compile` 通过
- 前端构建：`npm.cmd run build` 通过
- 会员中心页面：未修改代码

## 风险备注

- 会员中心页面代码本轮未修改，严格符合“本轮不修改会员中心页面”的约束
- 本轮后端 `/me` 语义已统一到 SQL 真实总额度，后续若要调整会员中心展示口径，应单独开轮处理
