# TASK_CORE_BUGFIX_ROUND_5_USER_QUOTA_RULE_AND_PROFILE_DISPLAY

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
- 当前系统只判断了日额度，没有正确兜底到总额度

### 问题2: 个人中心额度展示口径错误且重复
- 当前个人中心有两处额度展示重复混乱
- 普通用户与会员用户的展示没有区分开

## Root Causes Identified

### 后端额度检查逻辑问题
1. `checkInterviewQuota` / `checkResumeQuota`: VIP用户只检查日额度，没有检查总额度兜底
2. `deductInterviewQuota` / `deductResumeQuota`: VIP用户只扣减日额度，日额度用完直接报错
3. `getRemainingInterviewQuota` / `getRemainingResumeQuota`: 只返回日额度剩余，不返回合并后的额度

### 前端展示逻辑问题
1. 右侧权益卡和统计卡使用相同的 `resumeQuotaLeft` / `interviewQuotaLeft`，没有差异化
2. 缺少 `resumeTotalQuota` / `interviewTotalQuota` / `resumeDailyQuota` / `interviewDailyQuota` 等字段
3. 缺少 `isVip` 字段用于前端判断用户身份

## Fixes Applied

### Fix 1: QuotaConstants.java
- 添加了 VIP 用户总额度常量：
  - `VIP_USER_TOTAL_INTERVIEW_LIMIT = 100`
  - `VIP_USER_TOTAL_RESUME_LIMIT = 50`

### Fix 2: UserQuotaServiceImpl.java - 额度检查逻辑
- `checkInterviewQuota`: VIP用户先检查日额度，日额度不足时继续检查总额度兜底
- `checkResumeQuota`: 同上
- 添加了详细的中文注释说明业务规则

### Fix 3: UserQuotaServiceImpl.java - 额度扣减逻辑
- `deductInterviewQuota`: VIP用户先扣日额度，日额度不足时扣总额度作为兜底
- `deductResumeQuota`: 同上

### Fix 4: UserQuotaServiceImpl.java - 剩余额度获取
- `getRemainingResumeQuota`: VIP用户返回日额度+总额度的合并剩余
- `getRemainingInterviewQuota`: 同上

### Fix 5: UserInfoResponse.java
- 新增字段：
  - `resumeTotalQuota` - 简历总额度
  - `interviewTotalQuota` - 面试总额度
  - `resumeDailyQuota` - 简历日额度
  - `interviewDailyQuota` - 面试日额度
  - `isVip` - 是否为有效VIP会员

### Fix 6: AuthServiceImpl.java
- 返回完整的额度字段供前端差异化展示
- 添加了 `QuotaConstants` 导入

### Fix 7: DashboardView.vue - 前端展示
- 差异化展示：
  - 右侧权益卡：展示合并后的剩余额度
  - 统计卡：普通用户展示总额度，VIP用户展示日额度
- 新增 computed：
  - `resumeTotalQuota` / `interviewTotalQuota`
  - `resumeDailyQuota` / `interviewDailyQuota`
  - `isVipUser`: 优先使用后端 `isVip` 字段
  - `resumeStatValue` / `interviewStatValue`: 差异化展示数值
  - `resumeStatLabel` / `interviewStatLabel`: 差异化展示标签

## Verification
- Backend compilation: ✅ PASSED (`mvn.cmd -q -DskipTests compile`)
- Frontend build: ✅ PASSED (`npm.cmd run build`)

## Files Modified

### Backend
1. `server/src/main/java/com/airesume/server/common/constants/QuotaConstants.java`
2. `server/src/main/java/com/airesume/server/service/impl/UserQuotaServiceImpl.java`
3. `server/src/main/java/com/airesume/server/dto/auth/UserInfoResponse.java`
4. `server/src/main/java/com/airesume/server/service/impl/AuthServiceImpl.java`

### Frontend
1. `frontend/app/src/views/DashboardView.vue`

## Acceptance Criteria

### 验证场景1: 普通用���额度消费
- 仅按总额度判断与展示
- 总额度用完后提示"没有额度"

### 验证场景2: 会员用户额度消费
- 先走日额度
- 日额度用完后仍可使用总额度（兜底）
- 只有当日额度和总额度都不足时，才提示"没有额度"

### 验证场景3: 个人中心额度展示 - 普通用户
- 右侧权益卡：展示"免费简历诊断剩余" / "免费模拟面试剩余"
- 统计卡：展示"简历免费额度" / "面试免费额度"（固定值）

### 验证场景4: 个人中心额度展示 - VIP用户
- 右侧权益卡：展示"今日剩余简历诊断" / "今日剩余模拟面试"（合并后）
- 统计卡：展示"今日简历剩余" / "今日面试剩余"（日额度）

### 验证场景5: 无重复展示
- 右侧权益卡和统计卡表达不同含义，不会让用户困惑

## Business Rules Summary

### 额度消费规则
| 用户类型 | 检查顺序 | 扣减顺序 |
|---------|---------|---------|
| 普通用户 | 总额度 | 总额度 |
| VIP用户 | 日额度 → 总额度（兜底） | 日额度 → 总额度（兜底） |

### 额度展示规则
| 用户类型 | 右侧权益卡 | 统计卡 |
|---------|-----------|--------|
| 普通用户 | 合并后剩余额度 | 总额度（固定值） |
| VIP用户 | 合并后剩余额度 | 日额度（每日刷新） |

## Next Steps
- 等待人工验收
- 验收通过后可进入下一轮核心Bug修复