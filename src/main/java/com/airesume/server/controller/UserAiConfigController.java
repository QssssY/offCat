package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.ai.AiModelDiscoveryResponse;
import com.airesume.server.dto.user.UserAiConfigRequest;
import com.airesume.server.dto.user.UserAiConfigResponse;
import com.airesume.server.dto.user.UserAiConfigToggleRequest;
import com.airesume.server.dto.user.UserAiConnectivityTestRequest;
import com.airesume.server.dto.user.UserAiConnectivityTestResponse;
import com.airesume.server.dto.user.UserAiModelsRequest;
import com.airesume.server.dto.user.UserAiUsageResponse;
import com.airesume.server.dto.user.UserTtsConnectivityTestRequest;
import com.airesume.server.dto.user.UserTtsConnectivityTestResponse;
import com.airesume.server.dto.user.UserTtsDiscoveryRequest;
import com.airesume.server.dto.user.UserTtsDiscoveryResponse;
import com.airesume.server.service.AiModelDiscoveryService;
import com.airesume.server.service.UserAiConfigService;
import com.airesume.server.service.UserAiUsageLimitService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final AiModelDiscoveryService aiModelDiscoveryService;

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

    @PostMapping("/models")
    public Result<AiModelDiscoveryResponse> fetchModels(
            @Valid @RequestBody UserAiModelsRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("用户自定义 AI 模型列表获取, userId: {}", userId);
        AiModelDiscoveryResponse response =
                aiModelDiscoveryService.fetchModels(request.getBaseUrl(), request.getApiKey(), 10000, "openai");
        return Result.success(response.getMessage(), response);
    }

    /**
     * 独立 TTS 连通测试，不保存配置。
     */
    @PostMapping("/test-tts-connectivity")
    public Result<UserTtsConnectivityTestResponse> testTtsConnectivity(
            @Valid @RequestBody UserTtsConnectivityTestRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("用户自定义 TTS 连通测试, userId: {}, model: {}, voiceId: {}",
                userId, request.getModel(), request.getVoiceId());
        UserTtsConnectivityTestResponse response = userAiConfigService.testTtsConnectivity(request);
        return Result.success(response.getMessage(), response);
    }

    /**
     * TTS 模型/音色发现，不保存配置。
     */
    @PostMapping("/tts-discovery")
    public Result<UserTtsDiscoveryResponse> ttsDiscovery(
            @Valid @RequestBody UserTtsDiscoveryRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("用户 TTS 模型/音色发现, userId: {}", userId);
        UserTtsDiscoveryResponse response = userAiConfigService.discoverTtsModelsAndVoices(request);
        return Result.success(response.getMessage(), response);
    }

    /**
     * TTS 音色试听：用当前表单参数合成最短音频，返回 audio/mpeg 二进制流。
     */
    @PostMapping(value = "/tts-preview", produces = "audio/mpeg")
    public ResponseEntity<byte[]> previewTtsVoice(
            @Valid @RequestBody UserTtsConnectivityTestRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("用户 TTS 音色试听, userId: {}, model: {}, voiceId: {}",
                userId, request.getModel(), request.getVoiceId());
        byte[] audioBytes = userAiConfigService.previewTtsVoice(request);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("audio/mpeg"))
                .cacheControl(CacheControl.noStore())
                .body(audioBytes);
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
