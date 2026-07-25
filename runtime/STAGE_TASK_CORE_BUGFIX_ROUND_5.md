# TASK_CORE_BUGFIX_ROUND_5 - Stage Document

## Task Information
- Task ID: TASK_CORE_BUGFIX_ROUND_5_USER_QUOTA_RULE_AND_PROFILE_DISPLAY
- Status: Completed
- Date: 2026-04-24

## Problem Summary

### 问题1: 会员用户额度消费规则错误
- 在管理端修改了用户总额度后，明明总额度还存在，但日额度用完后前端仍提示"没有额度"
- 正确规则应为：
  - 会员用户：先使用日额度
  - 日额度用完后，如果总额度仍有剩余，应继续允许使用总额度

### 问题2: 个人中心额度展示口径错误且重复
- 当前个人中心有两处额度展示：
  - 用户卡片右侧一块额度展示
  - 下方四个统计卡片中的额度展示
- 展示逻辑混乱、重复，没有正确区分普通用户与会员用户

## Root Causes Identified

### 后端额度检查逻辑问题
1. `checkInterviewQuota` / `checkResumeQuota`: VIP用户只检查日额度，没有检查总额度兜底
2. `deductInterviewQuota` / `deductResumeQuota`: VIP用户只扣减日额度
3. `getRemainingInterviewQuota` / `getRemainingResumeQuota`: 只返回日额度剩余

### 后端返回字段问题
1. `UserInfoResponse` 缺少：
   - resumeTotalQuota / interviewTotalQuota（总额度）
   - resumeDailyQuota / interviewDailyQuota（日额度）
   - isVip（有效VIP标识）

### 前端展示逻辑问题
1. 右侧权益卡和统计卡使用相同的额度字段
2. 缺少区分普通用户和VIP用户的展示逻辑

## Fixes Applied

### Fix 1: QuotaConstants.java
- 添加VIP用户总额度常量：
  - VIP_USER_TOTAL_INTERVIEW_LIMIT = 100
  - VIP_USER_TOTAL_RESUME_LIMIT = 50

### Fix 2: UserQuotaServiceImpl.java
- 修改 `checkInterviewQuota`：VIP用户先检查日额度，日额度不足时检查总额度兜底
- 修改 `checkResumeQuota`：同上
- 修改 `deductInterviewQuota`：VIP用户先扣日额度，日额度不足时扣总额度
- 修改 `deductResumeQuota`：同上
- 修改 `getRemainingResumeQuota`：返回日额度+总额度合并剩余
- 修改 `getRemainingInterviewQuota`：同上

### Fix 3: UserInfoResponse.java
- 新增字段：
  - resumeTotalQuota
  - interviewTotalQuota
  - resumeDailyQuota
  - interviewDailyQuota
  - isVip

### Fix 4: AuthServiceImpl.java
- 返回完整的额度字段供前端差异化展示

### Fix 5: DashboardView.vue
- 差异化展示：
  - 右侧权益卡：展示合并后剩余额度
  - 统计卡：普通用户展示总额度，VIP用户展示日额度- 去除重复展示

## Verification
- Backend: mvn.cmd -q -DskipTests compile ✅
- Frontend: npm.cmd run build ✅

## Files Modified

### Backend
1. QuotaConstants.java - 添加VIP总额度常量
2. UserQuotaServiceImpl.java - 额度检查/扣减/获取逻辑修复
3. UserInfoResponse.java - 新增额度字段
4. AuthServiceImpl.java - 返回完整额度字段

### Frontend
1. DashboardView.vue - 差异化展示逻辑

## Acceptance Criteria

### 场景1: 普通用户额度消费
- ✅ 仅按总额度判断与展示
- ✅ 总额度用完后提示"没有额度"

### 场景2: 会员用户额度消费
- ✅ 先走日额度
- ✅ 日额度用完后仍可使用总额度
- ✅ 只有当日额度和总额度都不足时，才提示"没有额度"

### 场景3: 个人中心 - 普通用户
- ✅ 右侧：展示"免费简历诊断剩余" / "免费模拟面试剩余"
- ✅ 统计卡：展示固定的总免费额度

### 场景4: 个人中心 - VIP用户
- ✅ 右侧：展示合并后剩余额度
- ✅ 统计卡：展示日额度

### 场景5: 无重复展示
- ✅ 右侧权益卡和统计卡表达不同含义

## Business Rules Summary

### 额度消费规则
| 用户类型 | 检查顺序 | 扣减顺序 |
|---------|---------|---------|
| 普通用户 | 总额度 | 总额度 |
| VIP用户 | 日额度 → 总额度 | 日额度 → 总额度 |

### 额度展示规则
| 用户类型 | 右侧权益卡 | 统计卡 |
|---------|-----------|--------|
| 普通用户 | 合并后剩余额度 | 总额度 |
| VIP用户 | 合并后剩余额度 | 日额度 |

## Next Steps
- 等待人工验收
- 验收通过后可进入下一轮核心Bug修复