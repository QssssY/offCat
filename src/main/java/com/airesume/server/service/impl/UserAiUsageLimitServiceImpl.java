package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.dto.user.UserAiUsageResponse;
import com.airesume.server.entity.UserAiDailyUsage;
import com.airesume.server.mapper.UserAiDailyUsageMapper;
import com.airesume.server.service.SysConfigService;
import com.airesume.server.service.UserAiUsageLimitService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户自定义 AI 每日用量限制服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserAiUsageLimitServiceImpl extends ServiceImpl<UserAiDailyUsageMapper, UserAiDailyUsage>
        implements UserAiUsageLimitService {

    private final SysConfigService sysConfigService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkAndIncrement(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        int limit = sysConfigService.getCustomAiDailyLimit();
        LocalDate today = LocalDate.now();

        // 先保证当天记录存在，再使用带上限条件的 UPDATE 原子递增，避免并发超卖。
        getBaseMapper().insertEmptyUsageIfAbsent(IdWorker.getId(), userId, today);
        int affected = getBaseMapper().incrementIfBelowLimit(userId, today, limit);
        if (affected <= 0) {
            throw new BusinessException(ResultCode.CUSTOM_AI_DAILY_LIMIT_EXCEEDED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollback(Long userId) {
        if (userId == null) {
            return;
        }
        getBaseMapper().rollbackToday(userId, LocalDate.now());
    }

    @Override
    public UserAiUsageResponse getUsage(Long userId) {
        int limit = sysConfigService.getCustomAiDailyLimit();
        int used = 0;
        if (userId != null) {
            // 用量查询只需要当天单条记录，直接走 Mapper 便于单测精确锁定查询条件。
            UserAiDailyUsage usage = getBaseMapper().selectOne(new LambdaQueryWrapper<UserAiDailyUsage>()
                    .eq(UserAiDailyUsage::getUserId, userId)
                    .eq(UserAiDailyUsage::getUsageDate, LocalDate.now())
                    .last("limit 1"));
            if (usage != null && usage.getCallCount() != null) {
                used = usage.getCallCount();
            }
        }
        return UserAiUsageResponse.builder()
                .used(used)
                .limit(limit)
                .remaining(Math.max(0, limit - used))
                .build();
    }
}
