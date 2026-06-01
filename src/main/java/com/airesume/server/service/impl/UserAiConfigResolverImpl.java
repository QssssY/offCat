package com.airesume.server.service.impl;

import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.dto.ai.ResolvedAiConfig;
import com.airesume.server.entity.UserAiConfig;
import com.airesume.server.service.AiCredentialCrypto;
import com.airesume.server.service.UserAiConfigResolver;
import com.airesume.server.service.UserAiConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户自定义 AI 配置解析器实现。
 */
@Service
@RequiredArgsConstructor
public class UserAiConfigResolverImpl implements UserAiConfigResolver {

    private final UserAiConfigService userAiConfigService;
    private final AiCredentialCrypto aiCredentialCrypto;

    @Override
    public ResolvedAiConfig resolve(Long userId, String businessType, boolean fallbackToPlatform) {
        if (fallbackToPlatform || userId == null) {
            return null;
        }
        String exactType = normalizeBusinessType(businessType);
        UserAiConfig config = null;
        if (exactType != null) {
            config = userAiConfigService.findEnabledConfig(userId, exactType);
        }
        if (config == null) {
            config = userAiConfigService.findEnabledConfig(userId, UserAiConstants.CONFIG_TYPE_DEFAULT);
        }
        return config == null ? null : buildResolvedConfig(config);
    }

    private String normalizeBusinessType(String businessType) {
        if (AiEngineConstants.BUSINESS_TYPE_RESUME.equals(businessType)) {
            return UserAiConstants.CONFIG_TYPE_RESUME;
        }
        if (AiEngineConstants.BUSINESS_TYPE_INTERVIEW.equals(businessType)) {
            return UserAiConstants.CONFIG_TYPE_INTERVIEW;
        }
        return null;
    }

    private ResolvedAiConfig buildResolvedConfig(UserAiConfig config) {
        return ResolvedAiConfig.builder()
                .provider("openai")
                .baseUrl(config.getBaseUrl())
                .apiKey(aiCredentialCrypto.decrypt(config.getApiKey()))
                .model(config.getModel())
                .supportsMultimodal(Integer.valueOf(1).equals(config.getSupportsMultimodal()))
                .source("USER_CUSTOM")
                .configType(config.getConfigType())
                .build();
    }
}
