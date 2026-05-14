package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.onboarding.OnboardingStatusResponse;
import com.airesume.server.dto.onboarding.OnboardingUpdateRequest;
import com.airesume.server.service.UserOnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户新手引导控制器
 * 提供引导状态查询和更新接口
 */
@Slf4j
@RestController
@RequestMapping("/api/user/onboarding")
@RequiredArgsConstructor
public class UserOnboardingController {

    private final UserOnboardingService userOnboardingService;

    /** 默认引导版本标识 */
    private static final String DEFAULT_GUIDE_KEY = "v1_2_main_onboarding";

    /**
     * 查询当前用户的新手引导状态
     * 如果用户没有引导记录，返回 not_started 状态
     *
     * @param authentication Spring Security 认证对象，包含当前用户ID
     * @return 引导状态信息
     */
    @GetMapping("/status")
    public Result<OnboardingStatusResponse> getStatus(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.debug("查询引导状态，userId: {}", userId);
        OnboardingStatusResponse response = userOnboardingService.getStatus(userId, DEFAULT_GUIDE_KEY);
        return Result.success(response);
    }

    /**
     * 更新当前用户的新手引导状态
     * 支持更新进度（in_progress）、完成（completed）和跳过（skipped）
     *
     * @param request        更新请求，包含状态和步骤信息
     * @param authentication Spring Security 认证对象，包含当前用户ID
     * @return 操作结果
     */
    @PutMapping("/status")
    public Result<Void> updateStatus(
            @Valid @RequestBody OnboardingUpdateRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("更新引导状态，userId: {}, status: {}, currentStep: {}",
                userId, request.getStatus(), request.getCurrentStep());
        userOnboardingService.updateStatus(userId, request);
        return Result.success();
    }
}
