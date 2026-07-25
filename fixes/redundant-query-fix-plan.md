# 重复查询优化方案

> 背景：`getCurrentUserInfo` 的 6 次重复 VIP 查询已修复（见 SysUserService.getActiveMembershipPlan）。
> 本文档覆盖项目中 **剩余全部** 同类问题。

## 核心根因

`isVipUser(userId)` + `getVipDaily*Limit(userId)` + `getVipCycleLimit(userId, type)` 三者各自独立调 `self.getById(userId)` + `membershipPlanService.getActiveByCode()`，同一个请求中组合调用时产生大量重复查询。

虽然 `getById` 有 Redis 缓存，但每次仍是一次 Redis 往返 + 反序列化开销；在 `@CacheEvict` 后（deduct/refund 场景）则直接打 DB。

---

## 优先级 P0：UserQuotaServiceImpl（影响最大，每次业务操作都经过）

**文件**: `server/src/main/java/com/airesume/server/service/impl/UserQuotaServiceImpl.java`

### P0-1. checkAndDeduct 四件套（Polish / JdMatch / Template / Offer）

**现状**（以 `checkAndDeductPolishQuota` L262-292 为例）：

```java
if (sysUserService.isVipUser(userId)) {                      // → getById #1
    int dailyLimit = sysUserService.getVipDailyPolishLimit(userId);  // → getById #2 + getActiveByCode #1
    int cycleLimit = sysUserService.getVipCycleLimit(userId, "polish"); // → getById #3 + getActiveByCode #2
    ...
}
```

每次调用：**3 次用户查询 + 2 次套餐查询**，4 个方法共 **12 次多余用户查询 + 8 次多余套餐查询**。

**修复方案**：用一次 `getActiveMembershipPlan(userId)` 取代三次分散调用。plan 为 null 即非 VIP。

```java
// 修复后
MembershipPlan plan = sysUserService.getActiveMembershipPlan(userId);
if (plan != null) {
    int dailyLimit = Math.max(0, plan.getDailyPolishLimit() == null ? 0 : plan.getDailyPolishLimit());
    int cycleLimit = plan.getTotalPolishQuota() == null ? 0 : plan.getTotalPolishQuota();
    int affected = getBaseMapper().consumeVipDailyPolishQuotaAtomic(userId, dailyLimit, cycleLimit);
    ...
}
```

4 个方法同理，字段映射：

| 方法 | dailyLimit 字段 | cycleLimit 字段 |
|------|----------------|-----------------|
| checkAndDeductPolishQuota | `plan.getDailyPolishLimit()` | `plan.getTotalPolishQuota()` |
| checkAndDeductJdMatchQuota | `plan.getDailyJdMatchLimit()` | `plan.getTotalJdMatchQuota()` |
| checkAndDeductTemplateQuota | `plan.getDailyTemplateLimit()` | `plan.getTotalTemplateQuota()` |
| checkAndDeductOfferQuota | `plan.getDailyOfferLimit()` | `plan.getTotalOfferQuota()` |

**效果**：每个方法 3 次查询 → 1 次查询。总计减少 **8 次用户查询 + 8 次套餐查询**。

### P0-2. deductInterviewQuota / deductResumeQuota

**现状**（以 `deductInterviewQuota` L124-139 为例）：

```java
if (sysUserService.isVipUser(userId)                              // → getById #1
        && getBaseMapper().consumeVipDailyInterviewQuotaAtomic(
                userId, getVipDailyInterviewLimit(userId))        // → getById #2 + getActiveByCode
        > 0) {
```

每次调用：**2 次用户查询 + 1 次套餐查询**。

**修复方案**：

```java
MembershipPlan plan = sysUserService.getActiveMembershipPlan(userId);
if (plan != null) {
    int dailyLimit = Math.max(0, plan.getInterviewQuota() == null ? 0 : plan.getInterviewQuota());
    if (getBaseMapper().consumeVipDailyInterviewQuotaAtomic(userId, dailyLimit) > 0) {
        log.info("VIP deducted interview daily quota for userId: {}", userId);
        return;
    }
}
```

`deductResumeQuota` 同理，用 `plan.getResumeQuota()`。

**效果**：每个方法减少 **1 次用户查询 + 1 次套餐查询**。

### P0-3. refundResumeQuota

**现状** (L144-149)：

```java
int dailyLimit = sysUserService.isVipUser(userId) ? getVipDailyResumeLimit(userId) : 0;
// isVipUser → getById #1; getVipDailyResumeLimit → getById #2 + getActiveByCode
```

**修复方案**：

```java
MembershipPlan plan = sysUserService.getActiveMembershipPlan(userId);
int dailyLimit = plan == null || plan.getResumeQuota() == null ? 0 : Math.max(0, plan.getResumeQuota());
```

**效果**：减少 **1 次用户查询 + 1 次套餐查询**。

### P0-4. checkInterviewQuota / checkResumeQuota

**现状**（以 `checkInterviewQuota` L98-107 为例）：

```java
if (sysUserService.isVipUser(userId)) {                    // → getById #1
    return getVipDailyInterviewRemaining(userQuota) > 0    // → getVipDailyInterviewLimit → getById #2 + getActiveByCode
           || safeValue(userQuota.getInterviewQuota()) > 0;
}
```

**修复方案**：

```java
MembershipPlan plan = sysUserService.getActiveMembershipPlan(userId);
if (plan != null) {
    int dailyLimit = Math.max(0, plan.getInterviewQuota() == null ? 0 : plan.getInterviewQuota());
    int dailyRemaining = Math.max(0, dailyLimit - safeValue(userQuota.getDailyInterviewUsed()));
    return dailyRemaining > 0 || safeValue(userQuota.getInterviewQuota()) > 0;
}
```

`checkResumeQuota` 同理。

**效果**：每个方法减少 **1 次用户查询 + 1 次套餐查询**。

### P0 总效果

优化前每次 getUserInfo 以外的配额操作约 2-3 次多余查询，P0 全部修完后 **UserQuotaServiceImpl 所有方法每次仅查 1 次 plan**。

---

## 优先级 P1：AdminUserRightsServiceImpl.getUserRights

**文件**: `server/src/main/java/com/airesume/server/service/impl/AdminUserRightsServiceImpl.java`
**方法**: `getUserRights(Long userId)` L45-77

**现状**：

```java
SysUser user = getExistingUser(userId);                           // getById #1
int resumeQuota = userQuotaService.getRemainingResumeQuota(userId);   // getByUserId #1 + refreshDaily
int interviewQuota = userQuotaService.getRemainingInterviewQuota(userId); // getByUserId #2 + refreshDaily
boolean vipActive = sysUserService.isVipUser(userId);             // getById #2
UserQuota userQuota = userQuotaService.getByUserId(userId);       // getByUserId #3
```

**2 次多余用户查询 + 2 次多余配额查询 + 1 次多余 refreshDaily**。

**修复方案**：

```java
SysUser user = getExistingUser(userId);

// 一次查配额 + 一次刷新
UserQuota userQuota = userQuotaService.getByUserId(userId);
if (userQuota == null) {
    throw new BusinessException("用户额度记录不存在");
}
userQuotaService.refreshDailyQuotaIfNeeded(userId, userQuota);
int resumeQuota = Math.max(0, safeValue(userQuota.getResumeQuota()));
int interviewQuota = Math.max(0, safeValue(userQuota.getInterviewQuota()));

// 用已有 user 对象判断 VIP，无需再查
boolean vipActive = user.getRole() != null
        && user.getRole() == UserRoleConstants.ROLE_VIP
        && user.getVipExpireTime() != null
        && user.getVipExpireTime().isAfter(LocalDateTime.now());
```

**效果**：5 次查询 → 2 次（1 getById + 1 getByUserId）。

---

## 优先级 P1：AdminController.getUserQuota

**文件**: `server/src/main/java/com/airesume/server/controller/AdminController.java`
**方法**: `getUserQuota(Long userId, Authentication)` L1124-1156

**现状**：

```java
UserQuota quota = userQuotaService.getByUserId(userId);          // 配额查询 #1
SysUser user = sysUserService.getById(userId);
...
.interviewQuota(userQuotaService.getRemainingInterviewQuota(userId))  // 配额查询 #2 + refreshDaily
.resumeQuota(userQuotaService.getRemainingResumeQuota(userId))        // 配额查询 #3 + refreshDaily
```

**修复方案**：用已有 `quota` 对象，只调一次 `refreshDailyQuotaIfNeeded`：

```java
UserQuota quota = userQuotaService.getByUserId(userId);
if (quota == null) {
    throw new BusinessException("用户额度记录不存在");
}
userQuotaService.refreshDailyQuotaIfNeeded(userId, quota);

SysUser user = sysUserService.getById(userId);
String username = user != null ? user.getUsername() : "";

UserQuotaResponse response = UserQuotaResponse.builder()
        ...
        .interviewQuota(Math.max(0, quota.getInterviewQuota() == null ? 0 : quota.getInterviewQuota()))
        .resumeQuota(Math.max(0, quota.getResumeQuota() == null ? 0 : quota.getResumeQuota()))
        ...
        .build();
```

**效果**：3 次配额查询 → 1 次。

---

## 优先级 P2：ResumeDiagnosisTaskServiceImpl.createTask

**文件**: `server/src/main/java/com/airesume/server/service/impl/ResumeDiagnosisTaskServiceImpl.java`
**方法**: `createTask(Long userId, ...)` L148-195

**现状**：

```java
userQuotaService.checkResumeQuota(userId);   // L160: ensureUserQuota + refreshDaily + isVipUser + getVipDailyResumeLimit
...
userQuotaService.deductResumeQuota(userId);  // L184: 再做一遍完全相同的查询链
```

check 和 deduct 整条查询链执行两遍。而 `deductResumeQuota` 内部的 `deductResumeQuotaAtomic` / `consumeVipDailyResumeQuotaAtomic` 本身就是原子操作 —— 余额不足时 affected=0 会抛异常。

**修复方案**：删除 L160 的 `checkResumeQuota` 调用，让 `deductResumeQuota` 的原子扣减同时负责检查。需要确保 `deductResumeQuota` 在额度不足时抛出的异常码与 `checkResumeQuota` 路径一致（已确认都是 `RESUME_QUOTA_EXHAUSTED`）。

```java
// 修复后：去掉 checkResumeQuota，deductResumeQuota 的原子 SQL 已包含余额校验
if (!useCustomAi) {
    // deductResumeQuota 内部原子扣减，余额不足抛 RESUME_QUOTA_EXHAUSTED
    userQuotaService.deductResumeQuota(userId);
}
```

同时保留额度通知逻辑（移到 catch 中或 deduct 后检查）：

```java
if (!useCustomAi) {
    try {
        userQuotaService.deductResumeQuota(userId);
    } catch (BusinessException e) {
        if (ResultCode.RESUME_QUOTA_EXHAUSTED.getCode() == e.getCode()) {
            notificationService.createQuotaNotificationIfNeeded(userId);
        }
        throw e;
    }
}
```

**效果**：一次请求减少 ~5 次重复查询。

**风险提示**：原来 check 在 save 之前，deduct 在 save 之后。删掉 check 后，如果扣减失败，已创建的 task 记录会被事务回滚（方法有 `@Transactional`），行为一致。

---

## 优先级 P2：MembershipServiceImpl.mockUpgrade

**文件**: `server/src/main/java/com/airesume/server/service/impl/MembershipServiceImpl.java`
**方法**: `mockUpgrade(Long userId, ...)` L63-139

**现状**：

```java
// L110: sysUserService.updateById(user)  → @CacheEvict 清除 user 缓存
// L115-118: resetCycleQuota / addBonusQuota → @CacheEvict 清除 quota 缓存
int resumeQuota = userQuotaService.getRemainingResumeQuota(userId);    // L121: 缓存已清，打 DB
int interviewQuota = userQuotaService.getRemainingInterviewQuota(userId); // L122: 又打 DB
```

写操作清缓存后两次 `getRemainingXxxQuota` 各自查 DB + refreshDaily。

**修复方案**：

```java
UserQuota updatedQuota = userQuotaService.getByUserId(userId);
userQuotaService.refreshDailyQuotaIfNeeded(userId, updatedQuota);
int resumeQuota = updatedQuota == null ? 0 : Math.max(0, updatedQuota.getResumeQuota() == null ? 0 : updatedQuota.getResumeQuota());
int interviewQuota = updatedQuota == null ? 0 : Math.max(0, updatedQuota.getInterviewQuota() == null ? 0 : updatedQuota.getInterviewQuota());
```

**效果**：2 次查询 → 1 次。

---

## 不改动的部分

| 项目 | 原因 |
|------|------|
| `SysUserService` 接口上的 6 个 `getVipDaily*Limit` 方法 | 其他调用方仍在使用，签名和行为不变 |
| `isVipUser()` 方法签名 | 保持向后兼容 |
| `getVipCycleLimit()` 方法签名 | 保持向后兼容 |
| AdminController 的 `checkAdminPermission` | 独立缓存命中，不与业务 userId 重复 |

---

## 实施顺序

| 步骤 | 改动 | 风险 |
|------|------|------|
| 1 | P0-1: checkAndDeduct 四件套 | 低 — 纯内部优化，原子 SQL 不变 |
| 2 | P0-2/3/4: deduct / refund / check 方法 | 低 — 同上 |
| 3 | P1: AdminUserRightsServiceImpl + AdminController | 低 — 只读方法 |
| 4 | P2: ResumeDiagnosisTaskServiceImpl | 中 — 删掉 precheck 需验证事务回滚 |
| 5 | P2: MembershipServiceImpl | 低 — 写后读优化 |

## 验证

1. `mvn compile` 编译通过
2. `mvn test` 全部测试通过
3. 启动后端，执行以下操作并观察日志：
   - 登录 test001 调用 getUserInfo → "Checking if user is VIP" 出现 1 次（已修复）
   - 触发 AI 润色/JD匹配/模板/Offer → 每个操作 "Checking if user is VIP" 不再出现（改用 getActiveMembershipPlan）
   - 触发简历诊断 → 不再有 checkResumeQuota 的重复查询日志
   - 管理端查看用户权益 → 配额相关查询日志只出现 1 次
