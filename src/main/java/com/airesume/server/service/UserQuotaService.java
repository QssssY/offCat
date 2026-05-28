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

    /** 检查并扣减AI润色配额（VIP日额度+周期总额度，非会员免费体验次数）。每份简历只能润色一次。 */
    void checkAndDeductPolishQuota(Long userId, Long resumeTaskId);

    /** 检查并扣减JD匹配配额。 */
    void checkAndDeductJdMatchQuota(Long userId);

    /** 检查并扣减模板使用配额。 */
    void checkAndDeductTemplateQuota(Long userId);

    /** 检查并扣减Offer辅助配额。 */
    void checkAndDeductOfferQuota(Long userId);

    /** VIP购买/续订时重置周期计数器。 */
    void resetCycleQuota(Long userId);

    /** 充入赠送额度（bonusResumeQuota/bonusInterviewQuota）。 */
    void addBonusQuota(Long userId, int bonusResume, int bonusInterview);
}
