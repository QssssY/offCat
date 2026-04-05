package com.airesume.server.service;

import com.airesume.server.entity.UserQuota;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户额度服务接口
 * 提供用户额度查询、校验和扣减功能
 */
public interface UserQuotaService extends IService<UserQuota> {

    /**
     * 根据用户ID获取用户额度记录
     *
     * @param userId 用户ID
     * @return 用户额度记录
     */
    UserQuota getByUserId(Long userId);

    /**
     * 初始化用户额度记录
     *
     * @param userId 用户ID
     */
    void initUserQuota(Long userId);

    /**
     * 检查用户是否有足够的面试次数
     *
     * @param userId 用户ID
     * @return 有足够次数返回true，否则返回false
     */
    boolean checkInterviewQuota(Long userId);

    /**
     * 检查用户是否有足够的简历诊断次数
     *
     * @param userId 用户ID
     * @return 有足够次数返回true，否则返回false
     */
    boolean checkResumeQuota(Long userId);

    /**
     * 扣减面试次数
     *
     * @param userId 用户ID
     */
    void deductInterviewQuota(Long userId);

    /**
     * 扣减简历诊断次数
     *
     * @param userId 用户ID
     */
    void deductResumeQuota(Long userId);

    /**
     * 刷新用户每日额度（跨天自动刷新）
     *
     * @param userId 用户ID
     * @param userQuota 用户额度记录
     */
    void refreshDailyQuotaIfNeeded(Long userId, UserQuota userQuota);

    /**
     * 获取用户剩余简历诊断次数
     *
     * @param userId 用户ID
     * @return 剩余次数
     */
    int getRemainingResumeQuota(Long userId);

    /**
     * 获取用户剩余模拟面试次数
     *
     * @param userId 用户ID
     * @return 剩余次数
     */
    int getRemainingInterviewQuota(Long userId);

}
