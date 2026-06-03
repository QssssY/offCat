package com.airesume.server.service;

import com.airesume.server.dto.user.UserAiUsageResponse;
import com.airesume.server.entity.UserAiDailyUsage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户自定义 AI 每日用量限制服务。
 */
public interface UserAiUsageLimitService extends IService<UserAiDailyUsage> {

    /**
     * 检查并递增今日调用次数；超限时抛业务异常。
     */
    void checkAndIncrement(Long userId);

    /**
     * 检查并按功能口径递增今日调用次数；超限时抛业务异常。
     */
    void checkAndIncrement(Long userId, String usageType);

    /**
     * AI 调用失败时回滚今日调用次数。
     */
    void rollback(Long userId);

    /**
     * AI 调用失败时回滚指定功能口径的今日调用次数。
     */
    void rollback(Long userId, String usageType);

    /**
     * 查询今日用量和上限。
     */
    UserAiUsageResponse getUsage(Long userId);
}
