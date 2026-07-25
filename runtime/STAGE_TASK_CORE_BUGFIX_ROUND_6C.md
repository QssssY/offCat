# TASK_CORE_BUGFIX_ROUND_6C - Stage Document

## Task Information
- Task ID: TASK_CORE_BUGFIX_ROUND_6C_QUOTA_RETURN_ALIGN_WITH_SQL_SCHEMA
- Status: Completed
- Date: 2026-04-24

## Problem Summary

### 问题描述
后端用户额度返回字段没有按数据库真实语义返回，错误地把套餐额度/会员日额度混入了个人中心展示。

### 已知现象
- 数据库 user_quota 表字段配置：resume_quota=15, interview_quota=15
- 后端返回：resumeRemaining=5, interviewRemaining=10（错误）
- 根因：后端用硬编码常量替代了数据库真实字段

## Root Causes Identified

1. AuthServiceImpl 用 QuotaConstants 硬编码常量作为额度来源
2. 没读取数据库 user_quota.resume_quota / interview_quota 字段
3. 把会员日额度(5/10)错误地当成当前剩余额度返回

## Fixes Applied

### AuthServiceImpl.java
- 直接从数据库 user_quota 表读取真实字段
- 使用 userQuota.getResumeQuota() 作为 resumeRemaining
- 使用 userQuota.getInterviewQuota() 作为 interviewRemaining

## Database Field Mapping

| SQL字段 | 用途 |
|--------|------|
| user_quota.resume_quota | → resumeRemaining |
| user_quota.interview_quota | → interviewRemaining |
| QuotaConstants (仅展示用) | → resumeTotalQuota / resumeDailyQuota |

## Verification
- Backend: mvn.cmd -q -DskipTests compile ✅
- Frontend: npm.cmd run build ✅

## Acceptance Criteria

1. ✅ 当前剩余额度取自 SQL 真实字段
2. ✅ 会员日额度单独返回
3. ✅ 套餐额度不直接当作用户当前剩余
4. ✅ 后端编译通过
5. ✅ 前端构建通过

## Next Steps
- 等待人工验收