# TASK_CORE_BUGFIX_ROUND_6 - Stage Document

## Task Information
- Task ID: TASK_CORE_BUGFIX_ROUND_6_USER_QUOTA_FIELD_SEMANTICS_FIX
- Status: Completed
- Date: 2026-04-24

## Problem Summary

### 问题描述
用户额度返回字段存在语义错误，把"总额度"和"日额度"混合计算。

### 已知现象
- 后端返回：resumeQuota = 55, interviewQuota = 110
- 实际应该是：resumeTotalQuota(50) + resumeDailyQuota(5) = 55 （错误混合）
- 这是错误的字段语义设计

## Root Causes Identified

1. UserQuotaServiceImpl.getRemaining 方法把日额度+总额度混合计算
2. UserInfoResponse 字段名 resumeQuota/interviewQuota 语义含糊
3. 前端使用了混合字段

## Fixes Applied

### Fix 1: UserQuotaServiceImpl.java
- getRemainingResumeQuota: VIP用户只返回日额度剩余
- getRemainingInterviewQuota: 同上

### Fix 2: UserInfoResponse.java
- 字段重命名：
  - resumeQuota → resumeRemaining
  - interviewQuota → interviewRemaining
- 添加详细注释，明确字段语义

### Fix 3: AuthServiceImpl.java
- 更新字段名和注释

### Fix 4: DashboardView.vue
- 前端字段名更新

## Database Field Semantics

| 字段 | 含义 |
|------|------|
| totalResumeUsed | 简历服务已使用总次数 |
| totalInterviewUsed | 面试服务已使用总次数 |
| dailyResumeUsed | 简历服务当日已使用次数 |
| dailyInterviewUsed | 面试服务当日已使用次数 |

## Response Field Semantics

| 字段 | 含义 |
|------|------|
| resumeRemaining | 当前简历剩余次数 |
| interviewRemaining | 当前面试剩余次数 |
| resumeTotalQuota | 简历服务总额度 |
| interviewTotalQuota | 面试服务总额度 |
| resumeDailyQuota | 简历服务日额度（仅VIP） |
| interviewDailyQuota | 面试服务日额度（仅VIP） |
| isVip | 是否为有效VIP会员 |

## Verification
- Backend: mvn.cmd -q -DskipTests compile ✅
- Frontend: npm.cmd run build ✅

## Acceptance Criteria

1. ✅ 数据库字段语义已明确
2. ✅ resumeQuota/interviewQuota 已重命名
3. ✅ 总额度与日额度已彻底分开返回
4. ✅ 前端按正确字段展示
5. ✅ 后端编译通过
6. ✅ 前端构建通过

## Next Steps
- 等待人工验收
- 验收通过后可进入下一轮核心Bug修复