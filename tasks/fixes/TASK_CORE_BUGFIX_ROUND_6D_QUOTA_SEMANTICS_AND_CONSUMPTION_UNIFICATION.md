# TASK_CORE_BUGFIX_ROUND_6D_QUOTA_SEMANTICS_AND_CONSUMPTION_UNIFICATION

## Task Information
- Task ID: TASK_CORE_BUGFIX_ROUND_6D_QUOTA_SEMANTICS_AND_CONSUMPTION_UNIFICATION
- Status: Completed
- Date: 2026-04-24

## Problem Summary

### 问题
用户额度字段语义修正后，引发了"次数不扣减 + 会员中心展示错误 + /me 重复请求"三个关联问题。

### 根因
- 扣减逻辑用的是常量 totalUsed(50)
- 显示逻辑用的是数据库字段 user_quota.resume_quota
- 两套逻辑没有统一

## Fixes Applied

### 1. VIP周额度改为每周刷新
- 将 refreshDailyQuotaIfNeeded 方法改为每周刷新逻辑
- 每逢周一自动刷新周额度

### 2. 保留常量名称向后兼容
- VIP_USER_DAILY_RESUME_LIMIT = 5 (原值，仍可用)
- VIP_USER_DAILY_INTERVIEW_LIMIT = 10 (原值，仍可用)
- 内部行为变为"每周刷新"而不是"每日刷新"

### 3. 字段语义保持不变
- resumeRemaining/interviewRemaining: 来自数据库字段
- resumeTotalQuota/interviewTotalQuota: 总额度
- resumeDailyQuota/interviewDailyQuota: 周额度(5/10)

## Business Rules

### VIP用户消费顺序：
1. 先扣周额度 (5份简历/周，10次面试/周)
2. 周额度用完后，扣总额度作为兜底 (50/100)

### 普通用户消费规则：
- 只扣一次性免费额度 (1份简历，3次面试)

### 刷新规则：
- 每周一0点自动刷新周额度

## Verification
- Backend: mvn.cmd -q -DskipTests compile ✅
- Frontend: npm.cmd run build ✅

## Next Steps
- 等待人工验收