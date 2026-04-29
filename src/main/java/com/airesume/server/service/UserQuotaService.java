package com.airesume.server.service;

import com.airesume.server.entity.UserQuota;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserQuotaService extends IService<UserQuota> {

    UserQuota getByUserId(Long userId);

    void initUserQuota(Long userId);

    boolean checkInterviewQuota(Long userId);

    boolean checkResumeQuota(Long userId);

    void deductInterviewQuota(Long userId);

    void deductResumeQuota(Long userId);

    /**
     * 退还简历诊断配额
     * 用于任务处理失败时回退已扣除的配额
     *
     * @param userId 用户ID
     */
    void refundResumeQuota(Long userId);

    void refreshDailyQuotaIfNeeded(Long userId, UserQuota userQuota);

    int getRemainingResumeQuota(Long userId);

    int getRemainingInterviewQuota(Long userId);
}
