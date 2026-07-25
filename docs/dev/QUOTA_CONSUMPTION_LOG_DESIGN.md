# 用户额度消费记录与展示增强 — 开发设计文档

> 版本: v1.0 | 创建日期: 2026-06-04 | 状态: 待评审

---

## 一、需求概述

### 1.1 背景

当前系统的额度管理存在两个核心缺陷：

1. **无消费溯源能力**：`user_quota` 表只维护计数器（`daily_xxx_used`、`total_xxx_used`、`cycle_xxx_used`），每次扣减原地覆盖，无法回答"我的额度是怎么消耗的"。
2. **前端额度展示不完整**：用户在 Dashboard 只能看到简历诊断和模拟面试两种额度；AI润色、JD匹配、模板库、Offer辅助四种额度对用户完全不可见（虽然后端 `UserInfoResponse` 已计算 `vipDailyPolishQuota` 等字段，但前端未展示）。

### 1.2 目标

| # | 目标 | 度量 |
|---|------|------|
| 1 | 新增统一消费记录表，覆盖全部 6 种额度类型 | 每次扣减/退款均有对应记录 |
| 2 | 用户可在成长中心查看额度消费明细 | 支持按类型筛选 + 标准分页 |
| 3 | Dashboard 展示全部 6 种额度的当日剩余 | 6 宫格卡片布局 |
| 4 | 管理后台可查看任意用户的消费记录 | 管理员按用户搜索 |
| 5 | 90 天自动清理历史记录 | 定时任务清理过期数据 |

### 1.3 额度类型清单

| 类型代码 | 中文名 | 免费用户 | VIP每日上限 |
|----------|--------|---------|------------|
| `INTERVIEW` | 模拟面试 | 3 次（终身） | 计划定义（默认 10） |
| `RESUME` | 简历诊断 | 1 次（终身） | 计划定义（默认 5） |
| `POLISH` | AI润色 | 1 次（终身） | 计划定义（默认 1） |
| `JD_MATCH` | JD匹配 | 1 次（终身） | 计划定义（默认 3） |
| `TEMPLATE` | 模板库 | 2 次（终身） | 计划定义（默认 5） |
| `OFFER` | Offer辅助 | 1 次（终身） | 计划定义（默认 3） |

---

## 二、数据库设计

### 2.1 新增表：`user_quota_consumption_log`

```sql
-- 用户额度消费记录表
-- 记录每一次额度扣减和退款，支持消费溯源
CREATE TABLE user_quota_consumption_log (
    id              BIGINT          NOT NULL COMMENT '雪花ID（主键）',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    quota_type      VARCHAR(32)     NOT NULL COMMENT '额度类型: INTERVIEW/RESUME/POLISH/JD_MATCH/TEMPLATE/OFFER',
    change_amount   INT             NOT NULL COMMENT '变动数量（正数=消耗，负数=退款）',
    balance_after   INT             DEFAULT NULL COMMENT '变动后该类型额度余额（当日剩余或免费剩余）',
    source          VARCHAR(32)     NOT NULL COMMENT '扣减来源: FREE/VIP_DAILY/VIP_CYCLE',
    billing_source  VARCHAR(32)     DEFAULT NULL COMMENT 'AI计费来源: PLATFORM/USER_CUSTOM/PLATFORM_FALLBACK（仅AI功能有值）',
    business_id     BIGINT          DEFAULT NULL COMMENT '关联业务ID（面试sessionID/简历taskID/润色recordID等）',
    business_type   VARCHAR(32)     DEFAULT NULL COMMENT '业务类型标识（见下方枚举）',
    description     VARCHAR(255)    DEFAULT NULL COMMENT '操作描述（如"模拟面试-第3轮对话"、"简历诊断失败退款"）',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    INDEX idx_user_type_time (user_id, quota_type, create_time),
    INDEX idx_user_time (user_id, create_time),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户额度消费记录表';
```

**字段说明：**

| 字段 | 说明 | 示例 |
|------|------|------|
| `change_amount` | 正数为消耗，负数为退款 | `1` 或 `-1` |
| `balance_after` | 扣减后的剩余额度，可为 NULL | VIP: `9`（剩余9次），免费: `0`（已用完） |
| `source` | 额度来源 | `FREE`=免费额度, `VIP_DAILY`=VIP每日, `VIP_CYCLE`=VIP周期 |
| `billing_source` | AI 调用的计费方 | `PLATFORM`=平台, `USER_CUSTOM`=用户自带Key, `PLATFORM_FALLBACK`=降级到平台 |
| `business_type` | 业务标识 | `INTERVIEW_SESSION`, `RESUME_DIAGNOSIS`, `RESUME_POLISH`, `JOB_MATCH`, `TEMPLATE_USE`, `OFFER_ASSIST` |
| `business_id` | 关联业务记录的主键 | 如 `interview_session.id` 或 `resume_diagnosis_task.id` |

### 2.2 `source` 枚举值

| 值 | 含义 | 判断条件 |
|----|------|---------|
| `FREE` | 使用免费额度 | 非 VIP 用户，或 VIP 已过期 |
| `VIP_DAILY` | 使用 VIP 每日额度 | VIP 有效期内，扣减 `daily_xxx_used` |
| `VIP_CYCLE` | 使用 VIP 周期额度 | VIP 有效期内，扣减 `cycle_xxx_used`（目前仅面试/简历诊断） |

### 2.3 `business_type` 枚举值

| 值 | 对应业务 |
|----|---------|
| `INTERVIEW_SESSION` | 模拟面试会话 |
| `RESUME_DIAGNOSIS` | 简历诊断任务 |
| `RESUME_POLISH` | AI 润色操作 |
| `JOB_MATCH` | JD 匹配分析 |
| `TEMPLATE_USE` | 模板使用 |
| `OFFER_ASSIST` | Offer 辅助 |

### 2.4 数据保留策略

- **保留天数**: 90 天
- **清理方式**: Spring 定时任务（`@Scheduled`），每天凌晨 3:00 执行
- **清理条件**: `WHERE create_time < DATE_SUB(NOW(), INTERVAL 90 DAY) AND is_deleted = 0`
- **清理方式**: 逻辑删除（`UPDATE is_deleted = 1`），非物理删除
- **配置化**: 保留天数通过 `sys_config` 表配置（key = `consumption_log_retention_days`，默认值 `90`）

### 2.5 迁移脚本

文件位置: `db/migrations/TASK_QUOTA_CONSUMPTION_LOG.sql`

---

## 三、后端设计

### 3.1 新增文件清单

| 层 | 文件 | 说明 |
|----|------|------|
| Entity | `entity/QuotaConsumptionLog.java` | 消费记录实体，继承 `BaseEntity` |
| Mapper | `mapper/QuotaConsumptionLogMapper.java` | MyBatis-Plus Mapper |
| DTO | `dto/quota/ConsumptionLogQueryRequest.java` | 用户查询请求（类型筛选 + 分页） |
| DTO | `dto/quota/ConsumptionLogResponse.java` | 消费记录响应 |
| DTO | `dto/quota/QuotaOverviewResponse.java` | 6 种额度总览响应（Dashboard 用） |
| Service | `service/QuotaConsumptionLogService.java` | 服务接口 |
| ServiceImpl | `service/impl/QuotaConsumptionLogServiceImpl.java` | 服务实现 |
| Controller | `controller/QuotaConsumptionLogController.java` | 用户端 API |
| 定时任务 | 在 `QuotaConsumptionLogServiceImpl` 中 | `@Scheduled` 清理过期记录 |

### 3.2 修改文件清单

| 文件 | 修改内容 |
|------|---------|
| `service/impl/UserQuotaServiceImpl.java` | 在所有 `deductXxxQuota` 和 `refundXxxQuota` 方法中，调用 `QuotaConsumptionLogService.logConsumption()` |
| `dto/auth/UserInfoResponse.java` | 新增非 VIP 用户 4 种免费额度剩余字段 |
| `service/impl/AuthServiceImpl.java` | `getCurrentUserInfo()` 方法补充非 VIP 的 4 种免费额度计算 |
| `controller/AdminController.java` | 新增管理员查询用户消费记录的接口 |

### 3.3 核心接口设计

#### 3.3.1 消费记录写入 — `QuotaConsumptionLogService`

```java
public interface QuotaConsumptionLogService extends IService<QuotaConsumptionLog> {

    /**
     * 记录额度消费（扣减或退款）
     *
     * @param userId        用户ID
     * @param quotaType     额度类型（INTERVIEW/RESUME/POLISH/JD_MATCH/TEMPLATE/OFFER）
     * @param changeAmount  变动数量（正=消耗，负=退款）
     * @param balanceAfter  变动后余额
     * @param source        来源（FREE/VIP_DAILY/VIP_CYCLE）
     * @param billingSource AI计费来源（可为null）
     * @param businessId    关联业务ID（可为null）
     * @param businessType  业务类型（可为null）
     * @param description   描述
     */
    void logConsumption(Long userId, String quotaType, int changeAmount,
                        Integer balanceAfter, String source,
                        String billingSource, Long businessId,
                        String businessType, String description);

    /**
     * 用户查询自己的消费记录（分页 + 类型筛选）
     */
    PageResult<ConsumptionLogResponse> getUserConsumptionLog(Long userId,
                                                              String quotaType,
                                                              int pageNum,
                                                              int pageSize);

    /**
     * 管理员查询指定用户的消费记录
     */
    PageResult<ConsumptionLogResponse> getAdminConsumptionLog(Long userId,
                                                               String quotaType,
                                                               int pageNum,
                                                               int pageSize);

    /**
     * 定时清理过期消费记录
     */
    void cleanExpiredLogs();
}
```

#### 3.3.2 写入时机（集成点）

以下方法在扣减/退款成功后调用 `logConsumption()`：

| UserQuotaServiceImpl 方法 | 额度类型 | business_type | 说明 |
|--------------------------|---------|---------------|------|
| `checkInterviewQuota` → 扣减成功后 | `INTERVIEW` | `INTERVIEW_SESSION` | 面试开始时扣减 |
| `deductResumeQuota` | `RESUME` | `RESUME_DIAGNOSIS` | 创建诊断任务时扣减 |
| `refundResumeQuota` | `RESUME` | `RESUME_DIAGNOSIS` | 诊断失败时退款（changeAmount=-1） |
| `checkAndDeductPolishQuota` | `POLISH` | `RESUME_POLISH` | AI润色时扣减 |
| `checkAndDeductJdMatchQuota` | `JD_MATCH` | `JOB_MATCH` | JD匹配时扣减 |
| `checkAndDeductTemplateQuota` | `TEMPLATE` | `TEMPLATE_USE` | 使用模板时扣减 |
| `checkAndDeductOfferQuota` | `OFFER` | `OFFER_ASSIST` | Offer辅助时扣减 |

**写入方式**：同步写入（与额度扣减在同一事务中）。消费记录表是 append-only，不涉及并发冲突，写入开销极低。

**注意事项**：
- `logConsumption()` 不应影响主业务流程。建议在额度扣减成功后、事务提交前写入。
- 如果 `logConsumption()` 抛异常，整个事务回滚（符合预期：扣减和记录必须一致）。
- `balance_after` 的计算：扣减前先查当前余额，扣减后计算 `原余额 - changeAmount`。

#### 3.3.3 用户端 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/quota/consumption-log` | 查询消费记录（分页 + 类型筛选） |

**请求参数（Query）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `quotaType` | String | 否 | 类型筛选（不传=全部） |
| `pageNum` | int | 否 | 页码（默认 1） |
| `pageSize` | int | 否 | 每页条数（默认 20） |

**响应结构：**

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 123456789,
        "quotaType": "INTERVIEW",
        "quotaTypeName": "模拟面试",
        "changeAmount": 1,
        "balanceAfter": 9,
        "source": "VIP_DAILY",
        "sourceName": "VIP每日额度",
        "billingSource": "PLATFORM",
        "businessType": "INTERVIEW_SESSION",
        "description": "模拟面试会话",
        "createTime": "2026-06-04 14:30:00"
      }
    ],
    "total": 42,
    "pageNum": 1,
    "pageSize": 20
  }
}
```

#### 3.3.4 管理端 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/users/{userId}/consumption-log` | 管理员查询用户消费记录 |

请求参数和响应结构同用户端，增加 `userId` 路径参数。

### 3.4 Dashboard 额度数据补充

#### 3.4.1 问题

当前 `UserInfoResponse` 已返回 VIP 用户的 6 种每日剩余额度（`vipDailyPolishQuota` 等），但**非 VIP 用户**的 4 种免费额度剩余（`freePolishLeft` 等）**未返回**。

#### 3.4.2 修改方案

**`UserInfoResponse.java` 新增字段：**

```java
/** 非VIP用户免费AI润色剩余次数。 */
private Integer freePolishLeft;
/** 非VIP用户免费JD匹配剩余次数。 */
private Integer freeJdMatchLeft;
/** 非VIP用户免费模板使用剩余次数。 */
private Integer freeTemplateLeft;
/** 非VIP用户免费Offer辅助剩余次数。 */
private Integer freeOfferLeft;
```

**`AuthServiceImpl.getCurrentUserInfo()` 补充计算：**

非 VIP 用户（或 VIP 已过期）时，从 `user_quota` 读取 `free_polish_left`、`free_jd_match_left`、`free_template_left`、`free_offer_left` 填入上述字段。

### 3.5 定时清理任务

```java
@Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3:00
public void cleanExpiredLogs() {
    // 从 sys_config 读取保留天数，默认 90
    int retentionDays = getRetentionDays(); // 默认 90
    LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

    // 逻辑删除
    lambdaUpdate()
        .lt(QuotaConsumptionLog::getCreateTime, cutoff)
        .eq(QuotaConsumptionLog::getIsDeleted, 0)
        .set(QuotaConsumptionLog::getIsDeleted, 1)
        .update();

    log.info("清理过期消费记录完成，清理时间线: {}", cutoff);
}
```

---

## 四、前端设计

### 4.1 Dashboard 改造 — 6 宫格额度卡片

#### 4.1.1 当前结构

[DashboardView.vue](frontend/app/src/views/DashboardView.vue) 的 `quota-card` 区域目前只有 2 个额度项（简历诊断 + 模拟面试），使用 flex 布局 + 分隔线。

#### 4.1.2 改造方案

将 `quota-card` 从 2 项扩展为 6 项的 2×3 网格布局：

```
┌────────────┐  ┌────────────┐  ┌────────────┐
│  🎯 面试    │  │  📄 简历    │  │  ✨ AI润色  │
│   7/10     │  │   3/5      │  │   0/1      │
│ 今日剩余   │  │ 今日剩余   │  │ 今日已用完 │
└────────────┘  └────────────┘  └────────────┘
┌────────────┐  ┌────────────┐  ┌────────────┐
│  🔍 JD匹配  │  │  📋 模板库  │  │  💼 Offer   │
│   2/3      │  │   4/5      │  │   1/3      │
│ 今日剩余   │  │ 今日剩余   │  │ 今日剩余   │
└────────────┘  └────────────┘  └────────────┘
```

**每个卡片数据来源：**

| 卡片 | VIP 用户 | 非 VIP 用户 |
|------|---------|------------|
| 模拟面试 | `vipDailyInterviewQuota` / 计划每日上限 | `interviewQuota` / 3 |
| 简历诊断 | `vipDailyResumeQuota` / 计划每日上限 | `resumeQuota` / 1 |
| AI润色 | `vipDailyPolishQuota` / 计划每日上限 | `freePolishLeft` / 1 |
| JD匹配 | `vipDailyJdMatchQuota` / 计划每日上限 | `freeJdMatchLeft` / 1 |
| 模板库 | `vipDailyTemplateQuota` / 计划每日上限 | `freeTemplateLeft` / 2 |
| Offer辅助 | `vipDailyOfferQuota` / 计划每日上限 | `freeOfferLeft` / 1 |

> **注意**：非 VIP 的 4 个新字段（`freePolishLeft` 等）需要后端在 `UserInfoResponse` 中新增（见 3.4 节）。VIP 的 `vipDailyXxxQuota` 字段**已经存在**。

**CSS 改造要点：**
- 使用 `CSS Grid`: `grid-template-columns: repeat(3, 1fr)` + `gap`
- 响应式：768px 以下切换为 `repeat(2, 1fr)`，480px 以下切换为 `repeat(1, 1fr)`
- 额度耗尽的卡片：数字变红 + 显示"升级会员"链接
- 每个 card 保持使用 `FeatureIcon` 组件

#### 4.1.3 需要的计划上限数据

VIP 用户的"上限"数字来自会员计划。当前 `/api/auth/me` 不返回计划上限。有两种方案：

**方案 A（推荐）：前端从 userInfo 中已有的数据推断**
- VIP 用户的计划上限可以从 `/api/membership/plans` 缓存中获取（已有 `api/membership.js`）
- 或者后端在 `UserInfoResponse` 中增加计划上限字段

**方案 B：后端直接返回上下文**
- 在 `UserInfoResponse` 中增加 `planDailyLimits` 对象，包含 6 种类型的每日上限

> 本文档按方案 A 实现前端。如果后续觉得复杂度太高，可切换为方案 B。

### 4.2 成长中心 — 新增「额度明细」Tab

#### 4.2.1 当前成长中心结构

[GrowthCenterView.vue](frontend/app/src/views/growth/GrowthCenterView.vue) 是单页结构，包含：
- 概要卡片（最新简历评分、面试评分、诊断次数、面试次数）
- 简历诊断评分趋势图
- 面试评分趋势图
- 面试维度雷达图
- 最新记录区域

#### 4.2.2 改造方案

在页面顶部增加 Tab 切换，将现有内容放在「成长概览」Tab 下，新增「额度明细」Tab：

```
┌─────────────────────────────────────────────────┐
│  [成长概览]  [额度明细]        ← Tab 切换        │
├─────────────────────────────────────────────────┤
│                                                  │
│  ┌──────────────────────────────────────────┐   │
│  │ 类型筛选: [全部▼] [面试] [简历] [润色]... │   │
│  ├──────────────────────────────────────────┤   │
│  │                                          │   │
│  │  消费记录列表（每条包含：                  │   │
│  │    额度类型标签、消耗数量、                │   │
│  │    来源（免费/VIP每日）、                  │   │
│  │    业务描述、操作时间                      │   │
│  │  )                                        │   │
│  │                                          │   │
│  ├──────────────────────────────────────────┤   │
│  │  ← 1  2  3  4  5 →     标准分页器         │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

#### 4.2.3 新增/修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `views/growth/GrowthCenterView.vue` | 修改 | 添加 Tab 容器，拆分为两个 Tab 面板 |
| `components/growth/ConsumptionLogPanel.vue` | 新增 | 额度明细面板组件（筛选 + 列表 + 分页） |
| `api/quota.js` | 新增 | 额度相关 API 调用（`/api/user/quota/consumption-log`） |

#### 4.2.4 ConsumptionLogPanel 组件设计

**功能：**
1. 类型筛选栏（全部 / 模拟面试 / 简历诊断 / AI润色 / JD匹配 / 模板库 / Offer辅助）
2. 消费记录列表，每条记录展示：
   - 左侧：额度类型图标 + 类型名称
   - 中间：消耗数量（红色 +1 或绿色 -1）、来源标签、业务描述
   - 右侧：操作时间
3. 底部分页器（el-pagination）

**API 调用：**

```javascript
// api/quota.js
import request from '@/utils/request'

export function getConsumptionLog(params) {
  return request({
    url: '/api/user/quota/consumption-log',
    method: 'get',
    params // quotaType, pageNum, pageSize
  })
}
```

### 4.3 管理端 — 用户消费记录查看

#### 4.3.1 修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `views/admin/AdminUserRightsView.vue` | 修改 | 在用户权益详情中增加「消费记录」Tab 或 Dialog |
| `api/admin/users.js` | 修改 | 新增 `GET /api/admin/users/{userId}/consumption-log` 调用 |

#### 4.3.2 交互方式

在 `AdminUserRightsView` 的用户权益详情 Dialog 中，增加一个「消费记录」Tab 页：
- 复用 `ConsumptionLogPanel` 的列表 UI（可抽取为通用组件）
- 支持按类型筛选 + 分页
- 增加用户 ID 显示

---

## 五、实现步骤（任务分解）

### Phase 1：数据库 + 后端基础（约 3 小时）

| # | 任务 | 产出文件 | 验证标准 |
|---|------|---------|---------|
| 1.1 | 编写迁移 SQL | `db/migrations/TASK_QUOTA_CONSUMPTION_LOG.sql` | 表创建成功，索引正确 |
| 1.2 | 创建 Entity | `entity/QuotaConsumptionLog.java` | 继承 BaseEntity，字段完整 |
| 1.3 | 创建 Mapper | `mapper/QuotaConsumptionLogMapper.java` | 继承 BaseMapper |
| 1.4 | 创建 DTO | `dto/quota/ConsumptionLogQueryRequest.java`, `ConsumptionLogResponse.java` | 字段完整 |
| 1.5 | 创建 Service 接口 | `service/QuotaConsumptionLogService.java` | 方法签名正确 |
| 1.6 | 实现 Service | `service/impl/QuotaConsumptionLogServiceImpl.java` | logConsumption + 分页查询 + 定时清理 |

### Phase 2：后端集成（约 3 小时）

| # | 任务 | 修改文件 | 验证标准 |
|---|------|---------|---------|
| 2.1 | 注入 LogService 到 UserQuotaServiceImpl | `UserQuotaServiceImpl.java` | 构造器注入 |
| 2.2 | 面试额度扣减记录 | `UserQuotaServiceImpl.java` | 扣减成功后调用 logConsumption |
| 2.3 | 简历诊断额度扣减/退款记录 | `UserQuotaServiceImpl.java` | 扣减 + 退款均有记录 |
| 2.4 | AI润色额度扣减记录 | `UserQuotaServiceImpl.java` | 扣减成功后记录 |
| 2.5 | JD匹配额度扣减记录 | `UserQuotaServiceImpl.java` | 扣减成功后记录 |
| 2.6 | 模板额度扣减记录 | `UserQuotaServiceImpl.java` | 扣减成功后记录 |
| 2.7 | Offer辅助额度扣减记录 | `UserQuotaServiceImpl.java` | 扣减成功后记录 |
| 2.8 | 补充非VIP用户4种免费额度字段 | `UserInfoResponse.java` + `AuthServiceImpl.java` | `/api/auth/me` 返回完整额度 |
| 2.9 | 创建用户端 Controller | `controller/QuotaConsumptionLogController.java` | API 测试通过 |
| 2.10 | 管理端 Controller 接口 | `controller/AdminController.java` | 管理员 API 测试通过 |

### Phase 3：前端 Dashboard 改造（约 2 小时）

| # | 任务 | 修改文件 | 验证标准 |
|---|------|---------|---------|
| 3.1 | 改造 quota-card 为 6 宫格 | `DashboardView.vue` | 6 种额度全部展示 |
| 3.2 | 添加 4 种额度的 computed 属性 | `DashboardView.vue` | 正确区分 VIP/非VIP 数据 |
| 3.3 | 响应式 CSS | `DashboardView.vue` | 3 列 → 2 列 → 1 列适配 |
| 3.4 | 额度耗尽提示 + 升级链接 | `DashboardView.vue` | 耗尽时显示红色 + 升级会员 |

### Phase 4：前端成长中心 Tab（约 3 小时）

| # | 任务 | 产出/修改文件 | 验证标准 |
|---|------|-------------|---------|
| 4.1 | 新增 API 模块 | `api/quota.js` | API 调用正确 |
| 4.2 | GrowthCenterView 添加 Tab 框架 | `GrowthCenterView.vue` | Tab 切换正常 |
| 4.3 | 开发 ConsumptionLogPanel 组件 | `components/growth/ConsumptionLogPanel.vue` | 列表 + 筛选 + 分页 |
| 4.4 | 集成到成长中心 | `GrowthCenterView.vue` | 完整流程可用 |

### Phase 5：管理端 + 定时任务 + 测试（约 2 小时）

| # | 任务 | 修改文件 | 验证标准 |
|---|------|---------|---------|
| 5.1 | 管理端消费记录查看 | `AdminUserRightsView.vue` + `api/admin/users.js` | 管理员可查看任意用户记录 |
| 5.2 | sys_config 插入保留天数配置 | `db/migrations/TASK_QUOTA_CONSUMPTION_LOG.sql` | 配置存在且默认值 90 |
| 5.3 | 定时清理任务测试 | `QuotaConsumptionLogServiceImpl.java` | 清理逻辑正确 |
| 5.4 | 后端单元测试 | `QuotaConsumptionLogServiceImplTest.java` | 覆盖核心方法 |
| 5.5 | 前端构建验证 | — | `npm run build` 通过 |

---

## 六、风险与注意事项

### 6.1 性能风险

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 消费记录表数据量增长 | 单用户每天最多 6 条，10 万活跃用户 = 60 万条/天 | 90 天清理 + 索引优化 + 逻辑删除 |
| 每次额度扣减多一次 INSERT | 增加约 1-2ms 延迟 | 同事务内执行，append-only 表写入极快 |
| Dashboard 查询 plan 上限多一次请求 | 可能增加首页加载时间 | 方案 A 利用已缓存的 `/api/membership/plans`，无额外请求 |

### 6.2 数据一致性

- **事务保证**：消费记录写入与额度扣减在同一事务内，保证原子性。
- **退款场景**：简历诊断失败退款时，新增一条 `change_amount = -1` 的记录，不删除原记录。
- **balance_after 字段**：在高并发场景下可能出现短暂不一致（两次扣减之间的余额快照），这是可接受的——该字段仅供用户参考，不用于精确对账。

### 6.3 向后兼容

- `UserInfoResponse` 新增 4 个字段（`freePolishLeft` 等），不影响现有前端——新字段为 null 时前端使用默认值。
- 现有 API 无破坏性变更，所有新增均为新端点或新字段。

### 6.4 事务边界注意事项

`UserQuotaServiceImpl` 使用了自注入（`@Lazy @Autowired`）来处理 `@Cacheable` 自调用问题。新增的 `QuotaConsumptionLogService` 注入不会引入循环依赖。

---

## 七、验收标准

### 7.1 功能验收

- [ ] 用户执行 6 种额度消费后，`user_quota_consumption_log` 表有对应记录
- [ ] 简历诊断失败退款后，有 `change_amount = -1` 的退款记录
- [ ] 用户可在成长中心查看消费明细，支持按类型筛选
- [ ] 消费记录列表正确分页（每页 20 条）
- [ ] Dashboard 展示全部 6 种额度的当日剩余（6 宫格）
- [ ] VIP 用户显示"今日剩余 X/Y"，非 VIP 用户显示"免费剩余 X/Y"
- [ ] 管理员可在后台查看任意用户的消费记录
- [ ] 90 天过期记录被定时任务逻辑删除

### 7.2 技术验收

- [ ] `mvn clean compile` 通过
- [ ] `npm run build` 通过
- [ ] 单元测试覆盖核心方法
- [ ] 无安全漏洞（API 鉴权正确，用户只能查自己的记录）

---

## 八、需求确认记录

| 轮次 | 问题 | 决策 |
|------|------|------|
| 第一轮 | 消费记录覆盖范围 | 全部 6 种额度 |
| 第一轮 | 记录粒度 | 详细模式（含关联业务ID、AI计费来源等） |
| 第一轮 | 消费记录展示位置 | 成长中心新增 Tab 页 |
| 第一轮 | 缺失额度展示位置 | 仪表盘统一展示全部 6 种（6 宫格） |
| 第二轮 | 写入时机 | 每次扣减时即时写入 |
| 第二轮 | 数据保留策略 | 保留 90 天（逻辑删除） |
| 第二轮 | 退款记录 | 退款也记录（负数记录） |
| 第二轮 | 管理端 | 管理员可查看任意用户的消费记录 |
| 第二轮 | 列表加载方式 | 标准分页（每页 20 条） |
| 第二轮 | 页面功能 | 按类型筛选（未选时间筛选、统计概览） |
