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
        quota.setInterviewQuota(QuotaConstants.NORMAL_USER_FREE_INTERVIEW_LIMIT);
        quota.setResumeQuota(QuotaConstants.NORMAL_USER_FREE_RESUME_LIMIT);
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
            return getVipDailyInterviewRemaining(userQuota) > 0 || safeValue(userQuota.getInterviewQuota()) > 0;
        }

        return safeValue(userQuota.getInterviewQuota()) > 0;
    }

    @Override
    public boolean checkResumeQuota(Long userId) {
        UserQuota userQuota = ensureUserQuota(userId);
        refreshDailyQuotaIfNeeded(userId, userQuota);

        if (sysUserService.isVipUser(userId)) {
            return getVipDailyResumeRemaining(userQuota) > 0 || safeValue(userQuota.getResumeQuota()) > 0;
        }

        return safeValue(userQuota.getResumeQuota()) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductInterviewQuota(Long userId) {
        UserQuota userQuota = ensureUserQuota(userId);
        refreshDailyQuotaIfNeeded(userId, userQuota);

        if (sysUserService.isVipUser(userId)) {
            if (getVipDailyInterviewRemaining(userQuota) > 0) {
                userQuota.setDailyInterviewUsed(safeValue(userQuota.getDailyInterviewUsed()) + 1);
            } else if (safeValue(userQuota.getInterviewQuota()) > 0) {
                userQuota.setInterviewQuota(safeValue(userQuota.getInterviewQuota()) - 1);
                userQuota.setDailyInterviewUsed(safeValue(userQuota.getDailyInterviewUsed()) + 1);
            } else {
                throw new BusinessException("Interview quota is exhausted");
            }
        } else {
            if (safeValue(userQuota.getInterviewQuota()) <= 0) {
                throw new BusinessException("Interview quota is exhausted");
            }
            userQuota.setInterviewQuota(safeValue(userQuota.getInterviewQuota()) - 1);
            userQuota.setDailyInterviewUsed(safeValue(userQuota.getDailyInterviewUsed()) + 1);
        }

        userQuota.setTotalInterviewUsed(safeValue(userQuota.getTotalInterviewUsed()) + 1);
        updateById(userQuota);
        log.info("Deducted interview quota for userId: {}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundResumeQuota(Long userId) {
        UserQuota userQuota = ensureUserQuota(userId);
        // 退还总使用次数
        userQuota.setTotalResumeUsed(Math.max(0, safeValue(userQuota.getTotalResumeUsed()) - 1));
        // 退还每日使用次数
        userQuota.setDailyResumeUsed(Math.max(0, safeValue(userQuota.getDailyResumeUsed()) - 1));
        // 恢复可用配额
        userQuota.setResumeQuota(safeValue(userQuota.getResumeQuota()) + 1);
        updateById(userQuota);
        log.info("Refunded resume quota for userId: {}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductResumeQuota(Long userId) {
        UserQuota userQuota = ensureUserQuota(userId);
        refreshDailyQuotaIfNeeded(userId, userQuota);

        if (sysUserService.isVipUser(userId)) {
            if (getVipDailyResumeRemaining(userQuota) > 0) {
                userQuota.setDailyResumeUsed(safeValue(userQuota.getDailyResumeUsed()) + 1);
            } else if (safeValue(userQuota.getResumeQuota()) > 0) {
                userQuota.setResumeQuota(safeValue(userQuota.getResumeQuota()) - 1);
                userQuota.setDailyResumeUsed(safeValue(userQuota.getDailyResumeUsed()) + 1);
            } else {
                throw new BusinessException("Resume quota is exhausted");
            }
        } else {
            if (safeValue(userQuota.getResumeQuota()) <= 0) {
                throw new BusinessException("Resume quota is exhausted");
            }
            userQuota.setResumeQuota(safeValue(userQuota.getResumeQuota()) - 1);
            userQuota.setDailyResumeUsed(safeValue(userQuota.getDailyResumeUsed()) + 1);
        }

        userQuota.setTotalResumeUsed(safeValue(userQuota.getTotalResumeUsed()) + 1);
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

    private int getVipDailyInterviewRemaining(UserQuota userQuota) {
        return Math.max(0, QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT - safeValue(userQuota.getDailyInterviewUsed()));
    }

    private int getVipDailyResumeRemaining(UserQuota userQuota) {
        return Math.max(0, QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT - safeValue(userQuota.getDailyResumeUsed()));
    }

    private int safeValue(Integer value) {
        return value == null ? 0 : value;
    }
}
