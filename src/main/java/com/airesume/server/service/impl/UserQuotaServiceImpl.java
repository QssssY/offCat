package com.airesume.server.service.impl;

import com.airesume.server.common.constants.QuotaConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.entity.MembershipPlan;
import com.airesume.server.entity.UserQuota;
import com.airesume.server.mapper.ResumePolishRecordMapper;
import com.airesume.server.mapper.UserQuotaMapper;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserQuotaService;
import com.airesume.server.entity.ResumePolishRecord;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserQuotaServiceImpl extends ServiceImpl<UserQuotaMapper, UserQuota> implements UserQuotaService {

    private final SysUserService sysUserService;
    private final ResumePolishRecordMapper polishRecordMapper;

    /** 自注入：通过 Spring 代理调用自身方法，使 @Cacheable 在自调用时生效 */
    @Lazy
    @Autowired
    private UserQuotaServiceImpl self;

    @Override
    @Cacheable(value = "user:quota", key = "#userId", unless = "#result == null")
    public UserQuota getByUserId(Long userId) {
        if (userId == null) {
            log.warn("getByUserId called with null userId");
            return null;
        }

        LambdaQueryWrapper<UserQuota> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserQuota::getUserId, userId);
        return getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user:quota", key = "#userId")
    public void initUserQuota(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        UserQuota existed = self.getByUserId(userId);
        if (existed != null) {
            return;
        }

        UserQuota quota = new UserQuota();
        quota.setUserId(userId);
        quota.setTotalInterviewUsed(0);
        quota.setTotalResumeUsed(0);
        quota.setInterviewQuota(QuotaConstants.NORMAL_USER_FREE_INTERVIEW_LIMIT);
        quota.setResumeQuota(QuotaConstants.NORMAL_USER_FREE_RESUME_LIMIT);
        quota.setDailyInterviewUsed(0);
        quota.setDailyResumeUsed(0);
        quota.setDailyPolishUsed(0);
        quota.setDailyJdMatchUsed(0);
        quota.setDailyTemplateUsed(0);
        quota.setDailyOfferUsed(0);
        quota.setCycleResumeUsed(0);
        quota.setCycleInterviewUsed(0);
        quota.setCyclePolishUsed(0);
        quota.setCycleJdMatchUsed(0);
        quota.setCycleTemplateUsed(0);
        quota.setCycleOfferUsed(0);
        quota.setFreePolishLeft(QuotaConstants.FREE_USER_POLISH_LIMIT);
        quota.setFreeJdMatchLeft(QuotaConstants.FREE_USER_JD_MATCH_LIMIT);
        quota.setFreeTemplateLeft(QuotaConstants.FREE_USER_TEMPLATE_LIMIT);
        quota.setFreeOfferLeft(QuotaConstants.FREE_USER_OFFER_LIMIT);
        quota.setLastRefreshDate(LocalDate.now());
        try {
            save(quota);
            log.info("Initialized user quota for userId: {}", userId);
        } catch (DuplicateKeyException e) {
            log.debug("User quota already exists for userId: {}", userId);
        }
    }

    @Override
    public boolean checkInterviewQuota(Long userId) {
        UserQuota userQuota = ensureUserQuota(userId);
        refreshDailyQuotaIfNeeded(userId, userQuota);

        // VIP 检查直接复用当前生效套餐，避免 isVipUser + dailyLimit 两次重复查询。
        MembershipPlan plan = sysUserService.getActiveMembershipPlan(userId);
        if (plan != null) {
            int dailyLimit = Math.max(0, safeValue(plan.getInterviewQuota()));
            int dailyRemaining = Math.max(0, dailyLimit - safeValue(userQuota.getDailyInterviewUsed()));
            return dailyRemaining > 0 || safeValue(userQuota.getInterviewQuota()) > 0;
        }

        return safeValue(userQuota.getInterviewQuota()) > 0;
    }

    @Override
    public boolean checkResumeQuota(Long userId) {
        UserQuota userQuota = ensureUserQuota(userId);
        refreshDailyQuotaIfNeeded(userId, userQuota);

        // VIP 检查直接复用当前生效套餐，避免 isVipUser + dailyLimit 两次重复查询。
        MembershipPlan plan = sysUserService.getActiveMembershipPlan(userId);
        if (plan != null) {
            int dailyLimit = Math.max(0, safeValue(plan.getResumeQuota()));
            int dailyRemaining = Math.max(0, dailyLimit - safeValue(userQuota.getDailyResumeUsed()));
            return dailyRemaining > 0 || safeValue(userQuota.getResumeQuota()) > 0;
        }

        return safeValue(userQuota.getResumeQuota()) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"auth:userInfo", "user:quota"}, key = "#userId")
    public void deductInterviewQuota(Long userId) {
        UserQuota userQuota = ensureUserQuota(userId);
        refreshDailyQuotaIfNeeded(userId, userQuota);

        // VIP 扣减直接读取一次生效套餐，套餐为空即按普通额度原子扣减。
        MembershipPlan plan = sysUserService.getActiveMembershipPlan(userId);
        if (plan != null) {
            int dailyLimit = Math.max(0, safeValue(plan.getInterviewQuota()));
            if (getBaseMapper().consumeVipDailyInterviewQuotaAtomic(userId, dailyLimit) > 0) {
                log.info("VIP deducted interview daily quota for userId: {}", userId);
                return;
            }
        }

        int affected = getBaseMapper().deductInterviewQuotaAtomic(userId);
        if (affected == 0) {
            throw new BusinessException(ResultCode.INTERVIEW_QUOTA_EXHAUSTED);
        }
        log.info("Deducted interview quota atomically for userId: {}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"auth:userInfo", "user:quota"}, key = "#userId")
    public void refundResumeQuota(Long userId) {
        ensureUserQuota(userId);
        // 退还时只读取一次生效套餐，用套餐日额度判断是否需要恢复总额度。
        MembershipPlan plan = sysUserService.getActiveMembershipPlan(userId);
        int dailyLimit = plan == null ? 0 : Math.max(0, safeValue(plan.getResumeQuota()));
        getBaseMapper().refundResumeQuotaAtomic(userId, dailyLimit);
        log.info("Refunded resume quota for userId: {}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"auth:userInfo", "user:quota"}, key = "#userId")
    public void deductResumeQuota(Long userId) {
        UserQuota userQuota = ensureUserQuota(userId);
        refreshDailyQuotaIfNeeded(userId, userQuota);

        // VIP 扣减直接读取一次生效套餐，套餐为空即按普通额度原子扣减。
        MembershipPlan plan = sysUserService.getActiveMembershipPlan(userId);
        if (plan != null) {
            int dailyLimit = Math.max(0, safeValue(plan.getResumeQuota()));
            if (getBaseMapper().consumeVipDailyResumeQuotaAtomic(userId, dailyLimit) > 0) {
                log.info("VIP deducted resume daily quota for userId: {}", userId);
                return;
            }
        }

        int affected = getBaseMapper().deductResumeQuotaAtomic(userId);
        if (affected == 0) {
            throw new BusinessException(ResultCode.RESUME_QUOTA_EXHAUSTED);
        }
        log.info("Deducted resume quota atomically for userId: {}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user:quota", key = "#userId")
    public void refreshDailyQuotaIfNeeded(Long userId, UserQuota userQuota) {
        if (userId == null || userQuota == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate lastRefresh = userQuota.getLastRefreshDate();

        if (lastRefresh == null || !today.equals(lastRefresh)) {
            UpdateWrapper<UserQuota> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("user_id", userId)
                    .eq("is_deleted", 0)
                    .and(wrapper -> wrapper.isNull("last_refresh_date")
                            .or()
                            .ne("last_refresh_date", today))
                    .set("daily_interview_used", 0)
                    .set("daily_resume_used", 0)
                    .set("daily_polish_used", 0)
                    .set("daily_jd_match_used", 0)
                    .set("daily_template_used", 0)
                    .set("daily_offer_used", 0)
                    .set("last_refresh_date", today);
            update(updateWrapper);

            userQuota.setDailyInterviewUsed(0);
            userQuota.setDailyResumeUsed(0);
            userQuota.setDailyPolishUsed(0);
            userQuota.setDailyJdMatchUsed(0);
            userQuota.setDailyTemplateUsed(0);
            userQuota.setDailyOfferUsed(0);
            userQuota.setLastRefreshDate(today);
        }
    }

    @Override
    public int getRemainingResumeQuota(Long userId) {
        UserQuota userQuota = ensureUserQuota(userId);
        refreshDailyQuotaIfNeeded(userId, userQuota);
        return Math.max(0, safeValue(userQuota.getResumeQuota()));
    }

    @Override
    public int getRemainingInterviewQuota(Long userId) {
        UserQuota userQuota = ensureUserQuota(userId);
        refreshDailyQuotaIfNeeded(userId, userQuota);
        return Math.max(0, safeValue(userQuota.getInterviewQuota()));
    }

    private UserQuota ensureUserQuota(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        // 通过代理调用，使 @Cacheable 生效
        UserQuota userQuota = self.getByUserId(userId);
        if (userQuota != null) {
            return userQuota;
        }

        initUserQuota(userId);
        userQuota = self.getByUserId(userId);
        if (userQuota == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return userQuota;
    }

    // ==================== 新功能配额：AI润色 / JD匹配 / 模板 / Offer ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"auth:userInfo", "user:quota"}, key = "#userId")
    public void checkAndDeductPolishQuota(Long userId, Long resumeTaskId) {
        UserQuota userQuota = ensureUserQuota(userId);
        refreshDailyQuotaIfNeeded(userId, userQuota);

        // 每份简历只能润色一次：查询已有润色记录
        Long existingCount = polishRecordMapper.selectCount(
                new LambdaQueryWrapper<ResumePolishRecord>()
                        .eq(ResumePolishRecord::getResumeTaskId, resumeTaskId)
                        .eq(ResumePolishRecord::getIsDeleted, 0));
        if (existingCount != null && existingCount > 0) {
            throw new BusinessException(ResultCode.POLISH_ALREADY_USED);
        }

        // VIP 配额一次性读取当前生效套餐，避免在同一次扣减中重复查询用户和套餐。
        MembershipPlan plan = sysUserService.getActiveMembershipPlan(userId);
        if (plan != null) {
            int dailyLimit = Math.max(0, safeValue(plan.getDailyPolishLimit()));
            int cycleLimit = safeValue(plan.getTotalPolishQuota());
            int affected = getBaseMapper().consumeVipDailyPolishQuotaAtomic(userId, dailyLimit, cycleLimit);
            if (affected == 0) {
                throw new BusinessException(ResultCode.POLISH_QUOTA_EXHAUSTED);
            }
            log.info("VIP 扣减AI润色配额成功，userId={}", userId);
            return;
        }

        // 非会员：扣减免费体验次数
        int affected = getBaseMapper().deductFreePolishAtomic(userId);
        if (affected == 0) {
            throw new BusinessException(ResultCode.VIP_FEATURE_REQUIRED);
        }
        log.info("非会员扣减免费润色次数成功，userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"auth:userInfo", "user:quota"}, key = "#userId")
    public void checkAndDeductJdMatchQuota(Long userId) {
        UserQuota userQuota = ensureUserQuota(userId);
        refreshDailyQuotaIfNeeded(userId, userQuota);

        // VIP 配额一次性读取当前生效套餐，避免在同一次扣减中重复查询用户和套餐。
        MembershipPlan plan = sysUserService.getActiveMembershipPlan(userId);
        if (plan != null) {
            int dailyLimit = Math.max(0, safeValue(plan.getDailyJdMatchLimit()));
            int cycleLimit = safeValue(plan.getTotalJdMatchQuota());
            int affected = getBaseMapper().consumeVipDailyJdMatchQuotaAtomic(userId, dailyLimit, cycleLimit);
            if (affected == 0) {
                throw new BusinessException(ResultCode.JD_MATCH_QUOTA_EXHAUSTED);
            }
            log.info("VIP 扣减JD匹配配额成功，userId={}", userId);
            return;
        }

        int affected = getBaseMapper().deductFreeJdMatchAtomic(userId);
        if (affected == 0) {
            throw new BusinessException(ResultCode.VIP_FEATURE_REQUIRED);
        }
        log.info("非会员扣减免费JD匹配次数成功，userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"auth:userInfo", "user:quota"}, key = "#userId")
    public void checkAndDeductTemplateQuota(Long userId) {
        UserQuota userQuota = ensureUserQuota(userId);
        refreshDailyQuotaIfNeeded(userId, userQuota);

        // VIP 配额一次性读取当前生效套餐，避免在同一次扣减中重复查询用户和套餐。
        MembershipPlan plan = sysUserService.getActiveMembershipPlan(userId);
        if (plan != null) {
            int dailyLimit = Math.max(0, safeValue(plan.getDailyTemplateLimit()));
            int cycleLimit = safeValue(plan.getTotalTemplateQuota());
            int affected = getBaseMapper().consumeVipDailyTemplateQuotaAtomic(userId, dailyLimit, cycleLimit);
            if (affected == 0) {
                throw new BusinessException(ResultCode.TEMPLATE_QUOTA_EXHAUSTED);
            }
            log.info("VIP 扣减模板使用配额成功，userId={}", userId);
            return;
        }

        int affected = getBaseMapper().deductFreeTemplateAtomic(userId);
        if (affected == 0) {
            throw new BusinessException(ResultCode.VIP_FEATURE_REQUIRED);
        }
        log.info("非会员扣减免费模板次数成功，userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"auth:userInfo", "user:quota"}, key = "#userId")
    public void checkAndDeductOfferQuota(Long userId) {
        UserQuota userQuota = ensureUserQuota(userId);
        refreshDailyQuotaIfNeeded(userId, userQuota);

        // VIP 配额一次性读取当前生效套餐，避免在同一次扣减中重复查询用户和套餐。
        MembershipPlan plan = sysUserService.getActiveMembershipPlan(userId);
        if (plan != null) {
            int dailyLimit = Math.max(0, safeValue(plan.getDailyOfferLimit()));
            int cycleLimit = safeValue(plan.getTotalOfferQuota());
            int affected = getBaseMapper().consumeVipDailyOfferQuotaAtomic(userId, dailyLimit, cycleLimit);
            if (affected == 0) {
                throw new BusinessException(ResultCode.OFFER_QUOTA_EXHAUSTED);
            }
            log.info("VIP 扣减Offer辅助配额成功，userId={}", userId);
            return;
        }

        int affected = getBaseMapper().deductFreeOfferAtomic(userId);
        if (affected == 0) {
            throw new BusinessException(ResultCode.VIP_FEATURE_REQUIRED);
        }
        log.info("非会员扣减免费Offer次数成功，userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"auth:userInfo", "user:quota"}, key = "#userId")
    public void resetCycleQuota(Long userId) {
        UpdateWrapper<UserQuota> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("is_deleted", 0)
                .set("cycle_resume_used", 0)
                .set("cycle_interview_used", 0)
                .set("cycle_polish_used", 0)
                .set("cycle_jd_match_used", 0)
                .set("cycle_template_used", 0)
                .set("cycle_offer_used", 0)
                .set("cycle_start_time", java.time.LocalDateTime.now());
        update(wrapper);
        log.info("重置VIP周期配额，userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"auth:userInfo", "user:quota"}, key = "#userId")
    public void addBonusQuota(Long userId, int bonusResume, int bonusInterview) {
        if (bonusResume <= 0 && bonusInterview <= 0) {
            return;
        }
        UpdateWrapper<UserQuota> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId).eq("is_deleted", 0);
        if (bonusResume > 0) {
            wrapper.setSql("resume_quota = resume_quota + " + bonusResume);
        }
        if (bonusInterview > 0) {
            wrapper.setSql("interview_quota = interview_quota + " + bonusInterview);
        }
        update(wrapper);
        log.info("充入赠送额度，userId={}, bonusResume={}, bonusInterview={}", userId, bonusResume, bonusInterview);
    }

    private int safeValue(Integer value) {
        return value == null ? 0 : value;
    }
}
