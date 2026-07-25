# 任务：VIP 用户额度展示对齐 SQL 真实字段语义

**任务编号**: TASK_CORE_BUGFIX_ROUND_7C_VIP_QUOTA_DISPLAY_ALIGN_WITH_CURRENT_SQL

**任务类型**: 核心业务 Bug 修复（字段语义对齐）

**优先级**: P0 - 最高

## 一、问题背景

### 1.1 当前 SQL 真实字段
`user_quota` 表中真实存在的字段：
- `resume_quota` - 用户当前剩余简历额度
- `interview_quota` - 用户当前剩余面试额度
- `daily_resume_used` - 今日简历已用次数
- `daily_interview_used` - 今日面试已用次数
- `total_resume_used` - 总简历已用次数
- `total_interview_used` - 总面试已用次数
- `last_refresh_date` - 上次刷新日期

### 1.2 发现的问题
- 后端之前返回的是衍生计算值，不是 SQL 真实字段
- VIP 首页第一行展示了 `vipTotalResumeQuota`（衍生兜底值：1）
- 而不是 SQL 真实字段 `user_quota.resume_quota`

### 1.3 用户要求的展示规则
1. VIP 首页第一行：直接使用 SQL 真实字段（resume_quota / interview_quota）
2. VIP 首页下方"今日剩余"：展示会员规则计算的日额度
3. 不允许用衍生值替代真实 SQL 字段

## 二、根因分析

### 2.1 后端问题
- `getCurrentUserInfo()` 方法使用了 `getRemainingResumeQuota()` 等计算方法
- 这些方法返回的是基于常量的计算值，不是 SQL 字段值
- 导致前端展示的不是用户真实的剩余额度

### 2.2 修复策略
- 直接读取 `userQuota.getResumeQuota()` / `userQuota.getInterviewQuota()`
- 这两个是 SQL 真实字段，与 user_quota 表结构一一对应

## 三、修复内容

### 3.1 后端修复

#### AuthServiceImpl.java
- 修改 `getCurrentUserInfo()` 方法
- 直接使用 `userQuota.getResumeQuota()` 读取 SQL 真实字段
- 直接使用 `userQuota.getInterviewQuota()` 读取 SQL 真实字段
- `vipTotalResumeQuota` / `vipTotalInterviewQuota` 改为参考值（仅返回给前端做参考）

**字段语义说明：**
| 字段名 | SQL 真实字段 | 用途 |
|--------|------------|------|
| resumeQuota | user_quota.resume_quota | 简历总剩余（首页第一行主展示） |
| interviewQuota | user_quota.interview_quota | 面试总剩余（首页第一行主展示） |
| vipDailyResumeQuota | 会员规则计算 | 今日剩余日额度 |
| vipDailyInterviewQuota | 会员规则计算 | 今日剩余日额度 |
| vipTotalResumeQuota | 参考值（基于常量计算） | 保留，仅供参考 |
| vipTotalInterviewQuota | 参考值（基于常量计算） | 保留，仅供参考 |

### 3.2 前端修复

#### MembershipView.vue
- VIP 用户第一行额度卡：绑定 `resumeQuotaText` / `interviewQuotaText`（SQL 真实字段）
- 下方"今日剩余"：绑定 `vipDailyResumeQuota` / `vipDailyInterviewQuota`（会员规则计算）

**字段绑定关系：**
| 位置 | 绑定字段 | 说明 |
|------|----------|------|
| 第一行额度卡（左） | resumeQuota | SQL 真实字段：resume_quota |
| 第一行额度卡（右） | interviewQuota | SQL 真实字段：interview_quota |
| 下方额度卡（左） | vipDailyResumeQuota | 会员规则计算：今日剩余 |
| 下方额度卡（右） | vipDailyInterviewQuota | 会员规则计算：今日剩余 |

## 四、验证结果

### 4.1 后端验证
- [x] 编译通过：`mvn.cmd -q -DskipTests compile`

### 4.2 前端验证
- [x] 构建通过：`npm.cmd --prefix "F:\Code\ai-resume\frontend\app" run build`

### 4.3 页面展示验证
- [ ] VIP 用户首页第一行现在展示的是 SQL 真实字段（resume_quota / interview_quota）
- [ ] VIP 用户首页下方"今日剩余"展示的是会员规则计算的日额度
- [ ] 普通用户仍只展示 SQL 真实字段
- [ ] 不存在用��生值替代真实字段的问题

## 五、相关文件

### 5.1 后端文件
| 文件 | 修改说明 |
|------|----------|
| AuthServiceImpl.java | 直接读取 SQL 真实字段 |

### 5.2 前端文件
| 文件 | 修改说明 |
|------|----------|
| MembershipView.vue | 第一行使用 SQL 真实字段 |

---

**创建时间**: 2026-04-24
**修复状态**: 待验收
**后续建议**: 需要在测试环境验证页面展示