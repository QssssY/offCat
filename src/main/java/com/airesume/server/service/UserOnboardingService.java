package com.airesume.server.service;

import com.airesume.server.dto.onboarding.OnboardingStatusResponse;
import com.airesume.server.dto.onboarding.OnboardingUpdateRequest;
import com.airesume.server.entity.UserOnboardingState;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户新手引导服务接口
 * 提供引导状态查询和更新能力
 */
public interface UserOnboardingService extends IService<UserOnboardingState> {

    /**
     * 获取指定用户的引导状态
     * 如果没有记录，返回默认的 not_started 状态
     *
     * @param userId   用户ID
     * @param guideKey 引导版本标识
     * @return 引导状态响应
     */
    OnboardingStatusResponse getStatus(Long userId, String guideKey);

    /**
     * 更新用户引导状态
     * 支持进行中（记录步骤）、完成和跳过操作
     * 已完成或已跳过的记录不会被重新打开
     *
     * @param userId  用户ID
     * @param request 更新请求
     */
    void updateStatus(Long userId, OnboardingUpdateRequest request);
}
