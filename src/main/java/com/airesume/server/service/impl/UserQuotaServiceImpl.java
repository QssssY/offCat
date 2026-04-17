package com.airesume.server.service.impl;

import com.airesume.server.common.constants.QuotaConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.entity.UserQuota;
import com.airesume.server.mapper.UserQuotaMapper;
import com.airesume.server.service.SysUserService;
import com.airesume.server.service.UserQuotaService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserQuotaServiceImpl extends ServiceImpl<UserQuotaMapper, UserQuota> implements UserQuotaService {

    private final SysUserService sysUserService;

    @Override
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
    public void initUserQuota(Long userId) {
        if (userId == null) {
            throw new BusinessException("userId can not be null");
        }

        UserQuota existed = getByUserId(userId);
        if (existed != null) {
            return;
        }

        UserQuota quota = new UserQuota();
        quota.setUserId(userId);
        quota.setTotalInterviewUsed(0);
        quota.setTotalResumeUsed(0);

        // Legacy fields from the old "buy package and stack remaining count" model.
        // The current business rule does not use these columns as the source of truth anymore.
        quota.setInterviewQuota(0);
        quota.setResumeQuota(0);

        quota.setDailyInterviewUsed(0);
        quota.setDailyResumeUsed(0);
        quota.setLastRefreshDate(LocalDate.now());
        save(quota);
        log.info("Initialized user quota for userId: {}", userId);
    }

    @Override
    public boolean checkInterviewQuota(Long userId) {
        UserQuota userQuota = ensureUserQuota(userId);
        refreshDailyQuotaIfNeeded(userId, userQuota);

        if (sysUserService.isVipUser(userId)) {
            // VIP quota must be based on today's usage, because VIP rules reset every day.
            return safeValue(userQuota.getDailyInterviewUsed()) < QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT;
        }

        // Normal users use a one-time total free quota and do not refresh daily.
        return safeValue(userQuota.getTotalInterviewUsed()) < QuotaConstants.NORMAL_USER_FREE_INTERVIEW_LIMIT;
    }

    @Override
    public boolean checkResumeQuota(Long userId) {
        UserQuota userQuota = ensureUserQuota(userId);
        refreshDailyQuotaIfNeeded(userId, userQuota);

        if (sysUserService.isVipUser(userId)) {
            // VIP quota must be based on today's usage, because VIP rules reset every day.
            return safeValue(userQuota.getDailyResumeUsed()) < QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT;
        }

        // Normal users use a one-time total free quota and do not refresh daily.
        return safeValue(userQuota.getTotalResumeUsed()) < QuotaConstants.NORMAL_USER_FREE_RESUME_LIMIT;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductInterviewQuota(Long userId) {
        UserQuota userQuota = ensureUserQuota(userId);
        refreshDailyQuotaIfNeeded(userId, userQuota);

        if (sysUserService.isVipUser(userId)) {
            // The old logic deducted a stored "remaining total" field.
            // That does not match the current business rule, because VIP users are limited by
            // daily_interview_used and the count should refresh on the next day.
            if (safeValue(userQuota.getDailyInterviewUsed()) >= QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT) {
                throw new BusinessException("Today's VIP interview quota is exhausted");
            }

            userQuota.setDailyInterviewUsed(safeValue(userQuota.getDailyInterviewUsed()) + 1);
        } else {
            if (safeValue(userQuota.getTotalInterviewUsed()) >= QuotaConstants.NORMAL_USER_FREE_INTERVIEW_LIMIT) {
                throw new BusinessException("Free interview quota is exhausted");
            }

            userQuota.setTotalInterviewUsed(safeValue(userQuota.getTotalInterviewUsed()) + 1);

            // Keep daily usage in sync as operational data.
            // This also makes same-day upgrade behavior consistent with actual usage.
            userQuota.setDailyInterviewUsed(safeValue(userQuota.getDailyInterviewUsed()) + 1);
        }

        updateById(userQuota);
        log.info("Deducted interview quota for userId: {}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductResumeQuota(Long userId) {
        UserQuota userQuota = ensureUserQuota(userId);
        refreshDailyQuotaIfNeeded(userId, userQuota);

        if (sysUserService.isVipUser(userId)) {
            // The old logic deducted a stored "remaining total" field.
            // That does not match the current business rule, because VIP users are limited by
            // daily_resume_used and the count should refresh on the next day.
            if (safeValue(userQuota.getDailyResumeUsed()) >= QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT) {
                throw new BusinessException("Today's VIP resume quota is exhausted");
            }

            userQuota.setDailyResumeUsed(safeValue(userQuota.getDailyResumeUsed()) + 1);
        } else {
            if (safeValue(userQuota.getTotalResumeUsed()) >= QuotaConstants.NORMAL_USER_FREE_RESUME_LIMIT) {
                throw new BusinessException("Free resume quota is exhausted");
            }

            userQuota.setTotalResumeUsed(safeValue(userQuota.getTotalResumeUsed()) + 1);

            // Keep daily usage in sync as operational data.
            // This also makes same-day upgrade behavior consistent with actual usage.
            userQuota.setDailyResumeUsed(safeValue(userQuota.getDailyResumeUsed()) + 1);
        }

        updateById(userQuota);
        log.info("Deducted resume quota for userId: {}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshDailyQuotaIfNeeded(Long userId, UserQuota userQuota) {
        if (userId == null || userQuota == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate lastRefresh = userQuota.getLastRefreshDate();

        if (lastRefresh == null || !today.equals(lastRefresh)) {
            userQuota.setDailyInterviewUsed(0);
            userQuota.setDailyResumeUsed(0);
            userQuota.setLastRefreshDate(today);
            updateById(userQuota);
        }
    }

    @Override
    public int getRemainingResumeQuota(Long userId) {
        UserQuota userQuota = ensureUserQuota(userId);
        refreshDailyQuotaIfNeeded(userId, userQuota);

        if (sysUserService.isVipUser(userId)) {
            // VIP remaining quota is "today's remaining count", not a cumulative purchased total.
            return Math.max(0, QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT - safeValue(userQuota.getDailyResumeUsed()));
        }

        return Math.max(0, QuotaConstants.NORMAL_USER_FREE_RESUME_LIMIT - safeValue(userQuota.getTotalResumeUsed()));
    }

    @Override
    public int getRemainingInterviewQuota(Long userId) {
        UserQuota userQuota = ensureUserQuota(userId);
        refreshDailyQuotaIfNeeded(userId, userQuota);

        if (sysUserService.isVipUser(userId)) {
            // VIP remaining quota is "today's remaining count", not a cumulative purchased total.
            return Math.max(0, QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT - safeValue(userQuota.getDailyInterviewUsed()));
        }

        return Math.max(0, QuotaConstants.NORMAL_USER_FREE_INTERVIEW_LIMIT - safeValue(userQuota.getTotalInterviewUsed()));
    }

    private UserQuota ensureUserQuota(Long userId) {
        if (userId == null) {
            throw new BusinessException("userId can not be null");
        }

        UserQuota userQuota = getByUserId(userId);
        if (userQuota != null) {
            return userQuota;
        }

        initUserQuota(userId);
        userQuota = getByUserId(userId);
        if (userQuota == null) {
            throw new BusinessException("User quota record not found");
        }
        return userQuota;
    }

    private int safeValue(Integer value) {
        return value == null ? 0 : value;
    }
}
