# 任务：VIP 用户首页额度展示字段绑定修正

**任务编号**: TASK_CORE_BUGFIX_ROUND_7B_VIP_PROFILE_DISPLAY_FIELD_BINDING_FIX

**任务类型**: 核心业务 Bug 修复（字段绑定修正）

**优先级**: P0 - 最高

## 一、问题背景

### 1.1 上一轮修复后的状态
- 后端已返回 VIP 专用字段：
  - `vipDailyResumeQuota` = 4（日额度剩余）
  - `vipDailyInterviewQuota` = 9（日额度剩余）
  - `vipTotalResumeQuota` = 1（总额度剩余）
  - `vipTotalInterviewQuota` = 3（总额度剩余）

### 1.2 发现的问题
- 前端额度卡片的展示顺序不对
- VIP 用户第一行额度卡仍然显示日额度（4和9）
- 而不是用户要的总额度（1和3）

### 1.3 用户要求的展示规则
1. VIP 用户首页第一行额度卡片：必须展示"总额度剩余"
2. VIP 用户首页下方"今日剩余"卡片：展示"日额度剩余"
3. 不允许第一行继续显示日额度

## 二、根因分析

### 2.1 字段绑定问题
- 上一轮修复只是添加了字段，但没有正确调整卡片顺序
- 页面模板中先渲染的是日额度卡片，后渲染的是总额度卡片
- 导致用户看到的第一行是日额度，不是总额度

### 2.2 修复策略
- 调整模板中额度卡片的渲染顺序
- 第一行：总额度（日额度用完后作为兜底的额度）
- 第二行：日额度（今日可用额度）

## 三、修复内容

### 3.1 前端修复

#### MembershipView.vue
- 调整模板中的额度卡片顺序：
  - 先渲染总额度卡片
  - 后渲染日额度卡片

#### 字段绑定关系：
| 位置 | 绑定字段 | 说明 |
|------|----------|------|
| 第一行额度卡（左） | vipTotalResumeQuota | VIP 总额度剩余（简历） |
| 第一行额度卡（右） | vipTotalInterviewQuota | VIP 总额度剩余（面试） |
| 下方额度卡（左） | vipDailyResumeQuota | VIP 今日日额度剩余（简历） |
| 下方额度卡（右） | vipDailyInterviewQuota | VIP 今日日额度剩余（面试） |

### 3.2 普通用户展示（保持不变）
- 只显示总额度（使用 `resumeQuota` / `interviewQuota` 字段）
- 不涉及 VIP 专用字段

## 四、验证结果

### 4.1 前端验证
- [x] 构建通过：`npm.cmd --prefix "F:\Code\ai-resume\frontend\app" run build`

### 4.2 页面展示验证
- [ ] VIP 用户首页第一行额度卡现在展示的是 `vipTotalResumeQuota` / `vipTotalInterviewQuota`
- [ ] VIP 用户首页下方"今日剩余"展示的是 `vipDailyResumeQuota` / `vipDailyInterviewQuota`
- [ ] 普通用户仍只展示总额度
- [ ] 页面不存在重复展示日额度的问题

## 五、字段语义说明

| 字段名 | 含义 | 数据来源 |
|--------|------|----------|
| vipDailyResumeQuota | VIP 今日日额度剩余（简历） | VIP_USER_DAILY_RESUME_LIMIT - dailyResumeUsed |
| vipDailyInterviewQuota | VIP 今日日额度剩余（面试） | VIP_USER_DAILY_INTERVIEW_LIMIT - dailyInterviewUsed |
| vipTotalResumeQuota | VIP 总额度剩余（简历）= 普通用户免费额度 | NORMAL_USER_FREE_RESUME_LIMIT - totalResumeUsed |
| vipTotalInterviewQuota | VIP 总额度剩余（面试）= 普通用户免费额度 | NORMAL_USER_FREE_INTERVIEW_LIMIT - totalInterviewUsed |

### VIP 消费顺序说明
1. 优先使用日额度（日额度用完则次日刷新）
2. 日额度用完后，使用总额度作为兜底
3. 只有两者都为0时才提示没有额度

---

**创建时间**: 2026-04-24
**修复状态**: 待验收
**后续建议**: 需要在测试环境验证页面展示