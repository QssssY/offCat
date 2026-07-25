# TASK_CORE_BUGFIX_ROUND_6C_QUOTA_RETURN_ALIGN_WITH_SQL_SCHEMA

## Task Information
- Task ID: TASK_CORE_BUGFIX_ROUND_6C_QUOTA_RETURN_ALIGN_WITH_SQL_SCHEMA
- Status: Completed
- Date: 2026-04-24

## Problem Summary

### 问题描述
后端用户额度返回字段没有按数据库真实语义返回，错误地把套餐额度/会员日额度混入了个人中心展示。

### 已知现象
- 数据库 user_quota.interview_quota = 15, user_quota.resume_quota = 15
- 但后端返回：resumeRemaining = 5, interviewRemaining = 10
- 这说明后端忽略了数据库真实字段，用硬编码常量替代

## Root Causes Identified

### 问题根因
1. 后端 AuthServiceImpl.getCurrentUserInfo() 用硬编码常量 QuotaConstants 作为额度来源
2. 完全没读取数据库 user_quota.resume_quota / interview_quota 字段
3. 把会员日额度（5/10）错误地当成"当前剩余额度"返回

### 修复策略
直接使用数据库字段作为当前剩余额度来源：
- resumeRemaining: user_quota.resume_quota
- interviewRemaining: user_quota.interview_quota

## Fixes Applied

### Fix: AuthServiceImpl.java
- 直接从数据库 user_quota 表读取真实字段
- 使用 userQuotaService.getByUserId(userId) 获取 UserQuota 实体
- 使用 userQuota.getResumeQuota() 和 userQuota.getInterviewQuota() 作为当前剩余额度

## Database Field Semantics

| 数据库字段 | 含义 | 用途 |
|-----------|------|------|
| user_quota.resume_quota | 用户当前剩余简历总额度 | 直接作为 resumeRemaining 返回 |
| user_quota.interview_quota | 用户当前剩余面试总额度 | 直接作为 interviewRemaining 返回 |
| user_quota.daily_resume_used | 今日已使用简历次数 | 配合会员日额度计算 |
| user_quota.daily_interview_used | 今日已使用面试次数 | 配合会员日额度计算 |
| membership_plan.quota | 套餐配置值 | 非用户当前剩余，不能直接展示 |

## Response Field Semantics

| 返回字段 | 来源 | 说明 |
|----------|------|------|
| resumeRemaining | user_quota.resume_quota | 当前剩余简历额度 ✓ |
| interviewRemaining | user_quota.interview_quota | 当前剩余面试额度 ✓ |
| resumeTotalQuota | QuotaConstants (仅VIP) | 会员总使用额度上限（展示用）|
| interviewTotalQuota | QuotaConstants (仅VIP) | 会员总使用额度上限（展示用）|
| resumeDailyQuota | QuotaConstants (仅VIP) | 会员每日额度上限（展示用）|
| interviewDailyQuota | QuotaConstants (仅VIP) | 会员每日额度上限（展示用）|

## Verification
- Backend: mvn.cmd -q -DskipTests compile ✅
- Frontend: npm.cmd run build ✅

## Files Modified
1. AuthServiceImpl.java - 直接从数据库读取真实字段

## Acceptance Criteria

1. ✅ 当前剩余额度取自 user_quota.resume_quota / interview_quota
2. ✅ 会员日额度单独返回（不再混为当前剩余）
3. ✅ 套餐额度不直接当作用户当前剩余返回
4. ✅ 首页额度显示对应数据库真实字段
5. ✅ 后端编译通过
6. ✅ 前端构建通过

## Next Steps
- 等待人工验收