package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.onboarding.OnboardingStatusResponse;
import com.airesume.server.dto.onboarding.OnboardingUpdateRequest;
import com.airesume.server.entity.UserOnboardingState;
import com.airesume.server.mapper.UserOnboardingStateMapper;
import com.airesume.server.service.UserOnboardingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 用户新手引导服务实现类
 * 管理用户引导状态的查询、进度更新、完成和跳过
 */
@Slf4j
@Service
public class UserOnboardingServiceImpl extends ServiceImpl<UserOnboardingStateMapper, UserOnboardingState>
        implements UserOnboardingService {

    /** 合法的状态值 */
    private static final String STATUS_NOT_STARTED = "not_started";
    private static final String STATUS_IN_PROGRESS = "in_progress";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_SKIPPED = "skipped";

    /** 允许通过 API 设置的状态（终态和进行中） */
    private static final List<String> VALID_UPDATE_STATUSES = Arrays.asList(
            STATUS_IN_PROGRESS, STATUS_COMPLETED, STATUS_SKIPPED
    );

    @Override
    public OnboardingStatusResponse getStatus(Long userId, String guideKey) {
        log.debug("查询引导状态，userId: {}, guideKey: {}", userId, guideKey);

        // 查询用户的引导状态记录
        UserOnboardingState state = getStateByUserAndKey(userId, guideKey);

        if (state == null) {
            // 无记录，返回默认的 not_started 状态
            log.debug("未找到引导记录，返回默认状态，userId: {}", userId);
            return OnboardingStatusResponse.builder()
                    .guideKey(guideKey)
                    .status(STATUS_NOT_STARTED)
                    .currentStep(0)
                    .showGuide(true)
                    .build();
        }

        // 有记录，根据状态判断是否需要展示引导
        boolean showGuide = STATUS_NOT_STARTED.equals(state.getStatus())
                || STATUS_IN_PROGRESS.equals(state.getStatus());

        log.debug("引导状态查询完成，userId: {}, status: {}, currentStep: {}, showGuide: {}",
                userId, state.getStatus(), state.getCurrentStep(), showGuide);

        return OnboardingStatusResponse.builder()
                .guideKey(state.getGuideKey())
                .status(state.getStatus())
                .currentStep(state.getCurrentStep())
                .showGuide(showGuide)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, OnboardingUpdateRequest request) {
        String guideKey = request.getGuideKey();
        String newStatus = request.getStatus();
        Integer currentStep = request.getCurrentStep();

        log.info("更新引导状态，userId: {}, guideKey: {}, status: {}, currentStep: {}",
                userId, guideKey, newStatus, currentStep);

        // 校验状态值合法性
        if (!VALID_UPDATE_STATUSES.contains(newStatus)) {
            log.warn("无效的引导状态值: {}", newStatus);
            throw new BusinessException("无效的引导状态值: " + newStatus);
        }

        // 进行中状态必须携带步骤信息
        if (STATUS_IN_PROGRESS.equals(newStatus) && currentStep == null) {
            log.warn("进行中状态缺少 currentStep 参数，userId: {}", userId);
            throw new BusinessException("进行中状态必须提供当前步骤");
        }

        // 查询已有记录
        UserOnboardingState existingState = getStateByUserAndKey(userId, guideKey);

        if (existingState == null) {
            // 首次记录，创建新状态
            createNewState(userId, guideKey, newStatus, currentStep);
            return;
        }

        // 已完成或已跳过的记录不允许重新打开（幂等处理）
        if (STATUS_COMPLETED.equals(existingState.getStatus())
                || STATUS_SKIPPED.equals(existingState.getStatus())) {
            log.info("引导已完成或已跳过，忽略更新请求，userId: {}, currentStatus: {}",
                    userId, existingState.getStatus());
            return;
        }

        // 更新已有记录
        updateExistingState(existingState, newStatus, currentStep);
    }

    /**
     * 根据用户ID和引导标识查询状态记录
     */
    private UserOnboardingState getStateByUserAndKey(Long userId, String guideKey) {
        LambdaQueryWrapper<UserOnboardingState> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserOnboardingState::getUserId, userId)
                .eq(UserOnboardingState::getGuideKey, guideKey);
        return getOne(wrapper);
    }

    /**
     * 创建新的引导状态记录
     */
    private void createNewState(Long userId, String guideKey, String status, Integer currentStep) {
        UserOnboardingState newState = new UserOnboardingState();
        newState.setUserId(userId);
        newState.setGuideKey(guideKey);
        newState.setStatus(status);
        newState.setCurrentStep(currentStep != null ? currentStep : 0);

        // 设置终态时间
        if (STATUS_COMPLETED.equals(status)) {
            newState.setCompletedTime(LocalDateTime.now());
        } else if (STATUS_SKIPPED.equals(status)) {
            newState.setSkipTime(LocalDateTime.now());
        }

        save(newState);
        log.info("创建引导状态记录成功，userId: {}, status: {}", userId, status);
    }

    /**
     * 更新已有的引导状态记录
     */
    private void updateExistingState(UserOnboardingState state, String newStatus, Integer currentStep) {
        state.setStatus(newStatus);
        if (currentStep != null) {
            state.setCurrentStep(currentStep);
        }

        // 设置终态时间
        if (STATUS_COMPLETED.equals(newStatus)) {
            state.setCompletedTime(LocalDateTime.now());
        } else if (STATUS_SKIPPED.equals(newStatus)) {
            state.setSkipTime(LocalDateTime.now());
        }

        updateById(state);
        log.info("更新引导状态成功，userId: {}, newStatus: {}", state.getUserId(), newStatus);
    }
}
