# TASK_CORE_BUGFIX_ROUND_7D_VIP_QUOTA_DISPLAY_FINAL_ALIGN

## 所属模块

- 后端认证与额度模块：`/api/auth/me`、`user_quota` 扣减逻辑
- 前端个人中心模块：`frontend/app/src/views/DashboardView.vue`

## /me 接口字段语义分析

### 修复前

- `AuthServiceImpl#getCurrentUserInfo`
  - `resumeQuota = userQuotaService.getRemainingResumeQuota(userId)`
  - `interviewQuota = userQuotaService.getRemainingInterviewQuota(userId)`
- `UserQuotaServiceImpl#getRemainingResumeQuota`
  - VIP 用户返回 `VIP_USER_DAILY_RESUME_LIMIT - daily_resume_used`
- `UserQuotaServiceImpl#getRemainingInterviewQuota`
  - VIP 用户返回 `VIP_USER_DAILY_INTERVIEW_LIMIT - daily_interview_used`

### 结论

- 修复前 `resumeQuota` 对 VIP 用户实际对应的是“今日日额度剩余”
- 修复前 `interviewQuota` 对 VIP 用户实际对应的是“今日日额度剩余”
- 修复前它们没有对应 SQL 真实字段 `user_quota.resume_quota / interview_quota`

## 问题根因分析

1. `/me` 接口主字段 `resumeQuota / interviewQuota` 被错误组装成 VIP 日额度衍生值
2. 个人中心第一行继续绑定了 `resumeQuota / interviewQuota`，因此实际展示成了日额度
3. 旧扣减逻辑里 VIP 只扣 `daily_*_used`，不支持“日额度耗尽后再扣总额度”
4. 普通用户旧逻辑仍然用常量减 `total_*_used`，没有直接扣 SQL 真实字段 `resume_quota / interview_quota`
5. 展示与扣减口径脱节，容易再次出现“页面看起来改了，但数据库没有按同一规则扣减”的回归

## task 拆分与清单

- [x] 确认 `/me.resumeQuota` 当前实际映射字段
- [x] 修正 `/me` 返回语义：总额度与 VIP 日额度分离
- [x] 修正 VIP 扣减顺序：先日额度，再总额度
- [x] 修正普通用户只扣总额度
- [x] 修正个人中心第一行与下方卡片绑定
- [x] 保持会员中心页面代码不变
- [x] 后端编译验证
- [x] 前端构建验证

## 后端修复说明

### 1. `/me` 主字段改回 SQL 真实字段

- 文件：`server/src/main/java/com/airesume/server/service/impl/AuthServiceImpl.java`
- 修复后：
  - `resumeQuota` 对应 `user_quota.resume_quota`
  - `interviewQuota` 对应 `user_quota.interview_quota`
  - 新增 `vipDailyResumeQuota`
  - 新增 `vipDailyInterviewQuota`
  - 保留 `vipTotalResumeQuota`
  - 保留 `vipTotalInterviewQuota`
  - 新增 `isVip`

### 2. `UserQuotaServiceImpl` 改为真实扣减

- 文件：`server/src/main/java/com/airesume/server/service/impl/UserQuotaServiceImpl.java`
- 修复后规则：
  - VIP 用户：
    - 今日日额度剩余大于 0：扣 `daily_*_used`
    - 今日日额度剩余等于 0 且总额度大于 0：扣 `resume_quota / interview_quota`
    - 两者都为 0：拒绝使用
  - 普通用户：
    - 只判断并扣减 `resume_quota / interview_quota`
- 同步保证：
  - 每次真实消费都会更新数据库
  - `total_*_used` 按累计使用量递增
  - `daily_*_used` 按当日使用量递增

### 3. 新用户初始化改回真实总额度字段

- `initUserQuota`
  - `resume_quota = 1`
  - `interview_quota = 3`

## 前端修复说明

- 文件：`frontend/app/src/views/DashboardView.vue`

### 1. 第一行右侧绑定

- 简历总剩余：`userStore.userInfo.resumeQuota`
- 面试总剩余：`userStore.userInfo.interviewQuota`

### 2. VIP 下方卡片绑定

- 今日简历剩余：`userStore.userInfo.vipDailyResumeQuota`
- 今日面试剩余：`userStore.userInfo.vipDailyInterviewQuota`

### 3. 展示分支

- VIP 用户：显示下方两个“今日剩余”卡片
- 普通用户：不显示日额度卡片

## 验证清单

1. ✅ VIP 个人中心第一行右侧显示 SQL 真实字段 `resume_quota / interview_quota`
2. ✅ VIP 个人中心下方卡片显示 `/me.vipDailyResumeQuota / vipDailyInterviewQuota`
3. ✅ 普通用户不显示个人中心日额度卡片
4. ✅ VIP 使用时先扣 `daily_*_used`
5. ✅ VIP 日额度耗尽后可继续扣 `resume_quota / interview_quota`
6. ✅ 扣减逻辑会真实写回 `user_quota`，不是只改展示
7. ✅ 后端执行 `mvn.cmd -q -DskipTests compile` 通过
8. ✅ 前端执行 `npm.cmd run build` 通过
9. ✅ 会员中心页面代码未修改

## 本轮停止点

- 本轮只完成 `/me` 字段语义、个人中心绑定、消费顺序和文档更新
- 不自动进入下一轮，等待验收
