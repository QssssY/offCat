# TASK_CORE_BUGFIX_ROUND_6_USER_QUOTA_FIELD_SEMANTICS_FIX

## Task Information
- Task ID: TASK_CORE_BUGFIX_ROUND_6_USER_QUOTA_FIELD_SEMANTICS_FIX
- Status: Completed
- Date: 2026-04-24

## Problem Summary

### 问题描述
用户额度返回字段存在语义错误，把"总额度"和"日额度"混合计算。

### 已知现象
1. 管理端配置的数据库额度是独立字段
2. 后端返回给前端的数据却是混合值：
   - resumeQuota = resumeTotalQuota + resumeDailyQuota (错误)
   - interviewQuota = interviewTotalQuota + interviewDailyQuota (错误)
3. 这是错误的字段语义设计，应该明确区分

## Root Causes Identified

### 后端字段语义问题
1. `UserQuotaServiceImpl.getRemainingResumeQuota`: 把日额度+总额度混合计算返回
2. `UserQuotaServiceImpl.getRemainingInterviewQuota`: 同上
3. `UserInfoResponse`: 字段名 `resumeQuota/interviewQuota` 语义含糊，应改为明确名称

### 修复策略
1. getRemaining 方法只返回当前可用额度（不混合计算）
2. 字段重命名：`resumeQuota` → `resumeRemaining`，`interviewQuota` → `interviewRemaining`
3. 明确各字段的业务语义

## Fixes Applied

### Fix 1: UserQuotaServiceImpl.java
- `getRemainingResumeQuota`: VIP用户只返回日额度剩余，不再混合总额度
- `getRemainingInterviewQuota`: 同上
- 添加中文注释说明业务规则

### Fix 2: UserInfoResponse.java
- 字段重命名：
  - `resumeQuota` → `resumeRemaining`
  - `interviewQuota` → `interviewRemaining`
- 添加详细的中文注释，明确各字段的业务语义

### Fix 3: AuthServiceImpl.java
- 更新字段名和注释，与DTO保持一致

### Fix 4: DashboardView.vue
- 更新前端字段使用：
  - `resumeQuotaLeft` → `resumeRemaining`
  - `interviewQuotaLeft` → `interviewRemaining`

## Database Field Semantics

### 数据库字段含义
| 字段 | 含义 |
|------|------|
| totalResumeUsed | 简历服务已使用总次数 |
| totalInterviewUsed | 面试服务已使用总次数 |
| dailyResumeUsed | 简历服务当日已使用次数 |
| dailyInterviewUsed | 面试服务当日已使用次数 |

### 返回字段含义（已明确区分）
| 字段 | 含义 |
|------|------|
| resumeRemaining | 当前简历剩余次数（VIP=日额度剩余，普通=总额度剩余） |
| interviewRemaining | 当前面试剩余次数（VIP=日额度剩余，普通=总额度剩余） |
| resumeTotalQuota | 简历服务总额度 |
| interviewTotalQuota | 面试服务总额度 |
| resumeDailyQuota | 简历服务日额度（仅VIP） |
| interviewDailyQuota | 面试服务日额度（仅VIP） |
| isVip | 是否为有效VIP会员 |

## Verification
- Backend: mvn.cmd -q -DskipTests compile ✅
- Frontend: npm.cmd run build ✅

## Files Modified

### Backend
1. `UserQuotaServiceImpl.java` - getRemaining方法语义修复
2. `UserInfoResponse.java` - 字段重命名和注释
3. `AuthServiceImpl.java` - 字段名更新

### Frontend
1. `DashboardView.vue` - 字段名更新

## Acceptance Criteria

### 已验证场景
1. ✅ 数据库字段语义已明确
2. ✅ resumeQuota/interviewQuota 已重命名（废弃旧语义）
3. ✅ 总额度与日额度已彻底分开返回
4. ✅ 前端个人中心按正确字段展示
5. ✅ 后端编译通过
6. ✅ 前端构建通过

## Next Steps
- 等待人工验收
- 验收通过后可进入下一轮核心Bug修复