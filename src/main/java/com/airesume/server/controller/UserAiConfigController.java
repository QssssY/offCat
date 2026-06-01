package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.user.UserAiConfigRequest;
import com.airesume.server.dto.user.UserAiConfigResponse;
import com.airesume.server.dto.user.UserAiConfigToggleRequest;
import com.airesume.server.dto.user.UserAiConnectivityTestRequest;
import com.airesume.server.dto.user.UserAiConnectivityTestResponse;
import com.airesume.server.dto.user.UserAiUsageResponse;
import com.airesume.server.service.UserAiConfigService;
import com.airesume.server.service.UserAiUsageLimitService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户自定义 AI 配置接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/user/ai-config")
@RequiredArgsConstructor
public class UserAiConfigController {

    private final UserAiConfigService userAiConfigService;
    private final UserAiUsageLimitService userAiUsageLimitService;

    /**
     * 查询当前用户自定义 AI 配置列表。
     */
    @GetMapping
    public Result<List<UserAiConfigResponse>> listConfigs(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(userAiConfigService.listUserConfigs(userId));
    }

    /**
     * 创建或更新当前用户指定类型的 AI 配置。
     */
    @PostMapping
    public Result<UserAiConfigResponse> saveConfig(@Valid @RequestBody UserAiConfigRequest request,
                                                   Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("保存用户自定义 AI 配置, userId: {}, configType: {}", userId, request.getConfigType());
        return Result.success("AI 配置已保存", userAiConfigService.saveUserConfig(userId, request));
    }

    /**
     * 删除当前用户指定类型的 AI 配置。
     */
    @DeleteMapping("/{configType}")
    public Result<Void> deleteConfig(@PathVariable String configType, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        userAiConfigService.deleteUserConfig(userId, configType);
        return Result.success();
    }

    /**
     * 启用或禁用当前用户指定类型的 AI 配置。
     */
    @PutMapping("/{configType}/toggle")
    public Result<Void> toggleConfig(@PathVariable String configType,
                                     @Valid @RequestBody UserAiConfigToggleRequest request,
                                     Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        userAiConfigService.toggleUserConfig(userId, configType, Boolean.TRUE.equals(request.getEnabled()));
        return Result.success();
    }

    /**
     * 独立连通测试，不保存配置。
     */
    @PostMapping("/test-connectivity")
    public Result<UserAiConnectivityTestResponse> testConnectivity(
            @Valid @RequestBody UserAiConnectivityTestRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("用户自定义 AI 连通测试, userId: {}, model: {}", userId, request.getModel());
        UserAiConnectivityTestResponse response = userAiConfigService.testConnectivity(request);
        return Result.success(response.getMessage(), response);
    }

    /**
     * 查询当前用户今日自定义 AI 调用用量。
     */
    @GetMapping("/usage")
    public Result<UserAiUsageResponse> getUsage(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(userAiUsageLimitService.getUsage(userId));
    }
}
