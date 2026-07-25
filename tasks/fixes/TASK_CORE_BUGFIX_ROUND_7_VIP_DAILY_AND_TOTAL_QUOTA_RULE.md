# 任务：VIP 用户日额度与总额度展示及消费规则修复

**任务编号**: TASK_CORE_BUGFIX_ROUND_7_VIP_DAILY_AND_TOTAL_QUOTA_RULE

**任务类型**: 核心业务 Bug 修复

**优先级**: P0 - 最高

## 一、当前问题背景

### 1.1 回退版本现状
- 管理端可以正常修改用户额度
- 前端页面能正常展示与使用
- 首页/会员中心只显示日额度，没有同时展示总额度

### 1.2 上一轮引入的问题（需要避免回归）
- 使用简历诊断和模拟面试后不扣减次数
- 会员中心展示错误

### 1.3 当前业务规则（需要实现）

#### VIP 用户
1. 首页/会员中心必须同时显示：
   - 日额度
   - 总额度

2. 消费顺序：
   - 先消耗日额度
   - 日额度用完后，再消耗总额度
   - 只有"日额度 = 0 且总额度 = 0"时，才提示没有额度

3. 业务规则：
   - 如果日额度已经用完，但总额度还有剩余，VIP 用户仍然可以继续使用

#### 普通用户
1. 首页/会员中心只显示总额度
2. 不展示会员日额度
3. 只按总额度判断是否可用

## 二、根因分析

### 2.1 后端问题
- `UserQuotaServiceImpl` 的 `checkQuota` 方法只检查日额度，没有实现总额度兜底
- `deductInterviewQuota` / `deductResumeQuota` 只扣日额度，没有实现总额度兜底
- `getRemaining` 方法只返回日额度，没有返回总额度

### 2.2 前端问题
- `MembershipView.vue` 只显示 `resumeQuota` / `interviewQuota`，没有区分 VIP 日额度和总额度

## 三、修复内容

### 3.1 后端修复

#### 3.1.1 UserQuotaService.java
- 新增方法：
  - `getVipRemainingResumeQuota()` - 获取VIP日额度剩余
  - `getVipRemainingInterviewQuota()` - 获取VIP日额度剩余
  - `getVipTotalRemainingResumeQuota()` - 获取VIP总额度剩余
  - `getVipTotalRemainingInterviewQuota()` - 获取VIP总额度剩余

#### 3.1.2 UserQuotaServiceImpl.java
- 修改 `checkInterviewQuota`：VIP 用户先检查日额度，日额度为0再检查总额度
- 修改 `checkResumeQuota`：VIP 用户先检查日额度，日额度为0再检查总额度
- 修改 `deductInterviewQuota`：VIP 用户先扣日额度，日额度为0再扣总额度
- 修改 `deductResumeQuota`：VIP 用户先扣日额度，日额度为0再扣总额度
- 新增6个 `getVip*` 方法用于返回VIP专用额度

#### 3.1.3 UserInfoResponse.java
- 新增字段：
  - `vipDailyResumeQuota` - VIP日额度剩余（简历）
  - `vipDailyInterviewQuota` - VIP日额度剩余（面试）
  - `vipTotalResumeQuota` - VIP总额度剩余（简历）
  - `vipTotalInterviewQuota` - VIP总额度剩余（面试）

#### 3.1.4 AuthServiceImpl.java
- 修改 `getCurrentUserInfo`：填充VIP专用字段

### 3.2 前端修复

#### 3.2.1 MembershipView.vue
- 修改模板：VIP用户同时显示日额度和总额度，普通用户只显示总额度
- 新增计算属性：
  - `vipDailyResumeQuota`
  - `vipDailyInterviewQuota`
  - `vipTotalResumeQuota`
  - `vipTotalInterviewQuota`

## 四、验证结果

### 4.1 后端验证
- [x] 编译通过：`mvn.cmd -q -DskipTests compile`

### 4.2 前端验证
- [x] 构建通过：`npm.cmd --prefix "F:\Code\ai-resume\frontend\app" run build`

### 4.3 业务场景验证
- [ ] VIP用户首页/会员中心是否同时显示日额度和总额度
- [ ] 普通用户首页/会员中心是否只显示总额度
- [ ] VIP用户使用一次简历诊断时，若日额度还有剩余，是否优先扣日额度
- [ ] VIP用户在日额度用完后，若总额度还有剩余，是否仍可继续使用并扣总额度
- [ ] 只有日额度和总额度都为0时，是否才提示没有额度
- [ ] 普通用户使��后是否正常扣减总额度

## 五、业务规则说明

### 5.1 VIP用户消费顺序
1. 先检查日额度剩余是否 > 0
2. 如果日额度剩余 > 0，则扣日额度
3. 如果日额度剩余 = 0，检查总额度剩余是否 > 0
4. 如果总额度剩余 > 0，则扣总额度
5. 如果两者都为 0，则提示没有额度

### 5.2 普通用户消费规则
- 只使用总额度
- 不涉及日额度概念

### 5.3 额度来源说明
- VIP 日额度：每日 5 次简历诊断、10 次模拟面试
- VIP 总额度：等于普通用户免费额度（1次简历诊断、3次模拟面试），作为VIP的兜底额度
- 普通用户额度：1次简历诊断、3次模拟面试

## 六、修复文件清单

### 6.1 后端文件
| 文件 | 修改类型 |
|------|----------|
| UserQuotaService.java | 修改 |
| UserQuotaServiceImpl.java | 修改 |
| UserInfoResponse.java | 修改 |
| AuthServiceImpl.java | 修改 |

### 6.2 前端文件
| 文件 | 修改类型 |
|------|----------|
| MembershipView.vue | 修改（Round 7A：添加 VIP 字段） |
| MembershipView.vue | 修改（Round 7B：调整卡片顺序） |

---

**创建时间**: 2026-04-24
**修复状态**: 待验收（Round 7B 已修复字段绑定顺序）
**后续建议**: 需要在测试环境验证业务场景