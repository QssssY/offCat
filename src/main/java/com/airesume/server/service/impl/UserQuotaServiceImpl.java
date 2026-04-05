package com.airesume.server.service.impl;

import com.airesume.server.common.constants.QuotaConstants;
import com.airesume.server.common.constants.UserRoleConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.entity.SysUser;
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
            log.error("initUserQuota called with null userId");
            throw new BusinessException("用户ID不能为空");
        }
        UserQuota quota = new UserQuota();
        quota.setUserId(userId);
        quota.setTotalInterviewUsed(0);
        quota.setTotalResumeUsed(0);
        quota.setDailyInterviewUsed(0);
        quota.setDailyResumeUsed(0);
        quota.setLastRefreshDate(LocalDate.now());
        save(quota);
        log.info("Initialized user quota for userId: {}", userId);
    }

    @Override
    public boolean checkInterviewQuota(Long userId) {
        if (userId == null) {
            log.warn("checkInterviewQuota called with null userId");
            return false;
        }

        UserQuota userQuota = getByUserId(userId);
        if (userQuota == null) {
            log.warn("User quota not found for userId: {}, initializing now", userId);
            initUserQuota(userId);
            userQuota = getByUserId(userId);
            if (userQuota == null) {
                log.error("Failed to initialize user quota for userId: {}", userId);
                return false;
            }
        }

        refreshDailyQuotaIfNeeded(userId, userQuota);

        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            log.warn("User not found for userId: {}", userId);
            return false;
        }

        boolean isVip = sysUserService.isVipUser(userId);

        if (isVip) {
            return userQuota.getDailyInterviewUsed() < QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT;
        } else {
            return userQuota.getTotalInterviewUsed() < QuotaConstants.NORMAL_USER_FREE_INTERVIEW_LIMIT;
        }
    }

    @Override
    public boolean checkResumeQuota(Long userId) {
        if (userId == null) {
            log.warn("checkResumeQuota called with null userId");
            return false;
        }

        UserQuota userQuota = getByUserId(userId);
        if (userQuota == null) {
            log.warn("User quota not found for userId: {}, initializing now", userId);
            initUserQuota(userId);
            userQuota = getByUserId(userId);
            if (userQuota == null) {
                log.error("Failed to initialize user quota for userId: {}", userId);
                return false;
            }
        }

        refreshDailyQuotaIfNeeded(userId, userQuota);

        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            log.warn("User not found for userId: {}", userId);
            return false;
        }

        boolean isVip = sysUserService.isVipUser(userId);

        if (isVip) {
            return userQuota.getDailyResumeUsed() < QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT;
        } else {
            return userQuota.getTotalResumeUsed() < QuotaConstants.NORMAL_USER_FREE_RESUME_LIMIT;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductInterviewQuota(Long userId) {
        if (userId == null) {
            log.error("deductInterviewQuota called with null userId");
            throw new BusinessException("用户ID不能为空");
        }

        UserQuota userQuota = getByUserId(userId);
        if (userQuota == null) {
            log.warn("User quota not found for userId: {}, initializing now", userId);
            initUserQuota(userId);
            userQuota = getByUserId(userId);
            if (userQuota == null) {
                throw new BusinessException("用户额度记录不存在");
            }
        }

        refreshDailyQuotaIfNeeded(userId, userQuota);

        if (!checkInterviewQuota(userId)) {
            throw new BusinessException("面试次数已用完");
        }

        SysUser user = sysUserService.getById(userId);
        boolean isVip = user != null && sysUserService.isVipUser(userId);

        if (isVip) {
            userQuota.setDailyInterviewUsed(userQuota.getDailyInterviewUsed() + 1);
        } else {
            userQuota.setTotalInterviewUsed(userQuota.getTotalInterviewUsed() + 1);
        }

        updateById(userQuota);
        log.info("Deducted interview quota for userId: {}, isVip: {}", userId, isVip);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductResumeQuota(Long userId) {
        if (userId == null) {
            log.error("deductResumeQuota called with null userId");
            throw new BusinessException("用户ID不能为空");
        }

        UserQuota userQuota = getByUserId(userId);
        if (userQuota == null) {
            log.warn("User quota not found for userId: {}, initializing now", userId);
            initUserQuota(userId);
            userQuota = getByUserId(userId);
            if (userQuota == null) {
                throw new BusinessException("用户额度记录不存在");
            }
        }

        refreshDailyQuotaIfNeeded(userId, userQuota);

        if (!checkResumeQuota(userId)) {
            throw new BusinessException("简历诊断次数已用完");
        }

        SysUser user = sysUserService.getById(userId);
        boolean isVip = user != null && sysUserService.isVipUser(userId);

        if (isVip) {
            userQuota.setDailyResumeUsed(userQuota.getDailyResumeUsed() + 1);
        } else {
            userQuota.setTotalResumeUsed(userQuota.getTotalResumeUsed() + 1);
        }

        updateById(userQuota);
        log.info("Deducted resume quota for userId: {}, isVip: {}", userId, isVip);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshDailyQuotaIfNeeded(Long userId, UserQuota userQuota) {
        if (userId == null || userQuota == null) {
            log.warn("refreshDailyQuotaIfNeeded called with null userId or userQuota");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate lastRefresh = userQuota.getLastRefreshDate();

        if (!today.equals(lastRefresh)) {
            userQuota.setDailyInterviewUsed(0);
            userQuota.setDailyResumeUsed(0);
            userQuota.setLastRefreshDate(today);
            updateById(userQuota);
            log.info("Refreshed daily quota for userId: {}, from: {} to: {}", userId, lastRefresh, today);
        }
    }

    @Override
    public int getRemainingResumeQuota(Long userId) {
        if (userId == null) {
            log.warn("getRemainingResumeQuota called with null userId");
            return 0;
        }

        UserQuota userQuota = getByUserId(userId);
        if (userQuota == null) {
            log.warn("User quota not found for userId: {}, initializing now", userId);
            initUserQuota(userId);
            userQuota = getByUserId(userId);
            if (userQuota == null) {
                log.error("Failed to initialize user quota for userId: {}", userId);
                return 0;
            }
        }

        refreshDailyQuotaIfNeeded(userId, userQuota);

        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            log.warn("User not found for userId: {}", userId);
            return 0;
        }

        boolean isVip = sysUserService.isVipUser(userId);

        if (isVip) {
            return Math.max(0, QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT - userQuota.getDailyResumeUsed());
        } else {
            return Math.max(0, QuotaConstants.NORMAL_USER_FREE_RESUME_LIMIT - userQuota.getTotalResumeUsed());
        }
    }

    @Override
    public int getRemainingInterviewQuota(Long userId) {
        if (userId == null) {
            log.warn("getRemainingInterviewQuota called with null userId");
            return 0;
        }

        UserQuota userQuota = getByUserId(userId);
        if (userQuota == null) {
            log.warn("User quota not found for userId: {}, initializing now", userId);
            initUserQuota(userId);
            userQuota = getByUserId(userId);
            if (userQuota == null) {
                log.error("Failed to initialize user quota for userId: {}", userId);
                return 0;
            }
        }

        refreshDailyQuotaIfNeeded(userId, userQuota);

        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            log.warn("User not found for userId: {}", userId);
            return 0;
        }

        boolean isVip = sysUserService.isVipUser(userId);

        if (isVip) {
            return Math.max(0, QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT - userQuota.getDailyInterviewUsed());
        } else {
            return Math.max(0, QuotaConstants.NORMAL_USER_FREE_INTERVIEW_LIMIT - userQuota.getTotalInterviewUsed());
        }
    }

}
