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

/**
 * 用户额度服务实现类
 * 实现用户额度查询、校验和扣减功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserQuotaServiceImpl extends ServiceImpl<UserQuotaMapper, UserQuota> implements UserQuotaService {

    private final SysUserService sysUserService;

    @Override
    public UserQuota getByUserId(Long userId) {
        LambdaQueryWrapper<UserQuota> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserQuota::getUserId, userId);
        return getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initUserQuota(Long userId) {
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
        UserQuota userQuota = getByUserId(userId);
        if (userQuota == null) {
            log.warn("User quota not found for userId: {}", userId);
            return false;
        }

        // 先刷新每日额度（如果需要）
        refreshDailyQuotaIfNeeded(userId, userQuota);

        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            log.warn("User not found for userId: {}", userId);
            return false;
        }

        boolean isVip = sysUserService.isVipUser(userId);

        if (isVip) {
            // 会员用户：检查每日次数
            return userQuota.getDailyInterviewUsed() < QuotaConstants.VIP_USER_DAILY_INTERVIEW_LIMIT;
        } else {
            // 普通用户：检查累计免费次数
            return userQuota.getTotalInterviewUsed() < QuotaConstants.NORMAL_USER_FREE_INTERVIEW_LIMIT;
        }
    }

    @Override
    public boolean checkResumeQuota(Long userId) {
        UserQuota userQuota = getByUserId(userId);
        if (userQuota == null) {
            log.warn("User quota not found for userId: {}", userId);
            return false;
        }

        // 先刷新每日额度（如果需要）
        refreshDailyQuotaIfNeeded(userId, userQuota);

        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            log.warn("User not found for userId: {}", userId);
            return false;
        }

        boolean isVip = sysUserService.isVipUser(userId);

        if (isVip) {
            // 会员用户：检查每日次数
            return userQuota.getDailyResumeUsed() < QuotaConstants.VIP_USER_DAILY_RESUME_LIMIT;
        } else {
            // 普通用户：检查累计免费次数
            return userQuota.getTotalResumeUsed() < QuotaConstants.NORMAL_USER_FREE_RESUME_LIMIT;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductInterviewQuota(Long userId) {
        UserQuota userQuota = getByUserId(userId);
        if (userQuota == null) {
            throw new BusinessException("用户额度记录不存在");
        }

        // 先刷新每日额度（如果需要）
        refreshDailyQuotaIfNeeded(userId, userQuota);

        // 再次校验额度是否足够
        if (!checkInterviewQuota(userId)) {
            throw new BusinessException("面试次数已用完");
        }

        SysUser user = sysUserService.getById(userId);
        boolean isVip = user != null && sysUserService.isVipUser(userId);

        if (isVip) {
            // 会员用户：扣减每日次数
            userQuota.setDailyInterviewUsed(userQuota.getDailyInterviewUsed() + 1);
        } else {
            // 普通用户：扣减累计次数
            userQuota.setTotalInterviewUsed(userQuota.getTotalInterviewUsed() + 1);
        }

        updateById(userQuota);
        log.info("Deducted interview quota for userId: {}, isVip: {}", userId, isVip);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductResumeQuota(Long userId) {
        UserQuota userQuota = getByUserId(userId);
        if (userQuota == null) {
            throw new BusinessException("用户额度记录不存在");
        }

        // 先刷新每日额度（如果需要）
        refreshDailyQuotaIfNeeded(userId, userQuota);

        // 再次校验额度是否足够
        if (!checkResumeQuota(userId)) {
            throw new BusinessException("简历诊断次数已用完");
        }

        SysUser user = sysUserService.getById(userId);
        boolean isVip = user != null && sysUserService.isVipUser(userId);

        if (isVip) {
            // 会员用户：扣减每日次数
            userQuota.setDailyResumeUsed(userQuota.getDailyResumeUsed() + 1);
        } else {
            // 普通用户：扣减累计次数
            userQuota.setTotalResumeUsed(userQuota.getTotalResumeUsed() + 1);
        }

        updateById(userQuota);
        log.info("Deducted resume quota for userId: {}, isVip: {}", userId, isVip);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshDailyQuotaIfNeeded(Long userId, UserQuota userQuota) {
        LocalDate today = LocalDate.now();
        LocalDate lastRefresh = userQuota.getLastRefreshDate();

        if (!today.equals(lastRefresh)) {
            // 跨天了，刷新每日计数
            userQuota.setDailyInterviewUsed(0);
            userQuota.setDailyResumeUsed(0);
            userQuota.setLastRefreshDate(today);
            updateById(userQuota);
            log.info("Refreshed daily quota for userId: {}, from: {} to: {}", userId, lastRefresh, today);
        }
    }

}
