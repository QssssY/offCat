package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.common.util.PublicHttpsUrlValidator;
import com.airesume.server.dto.admin.AdminTtsConfigRequest;
import com.airesume.server.dto.admin.AdminTtsConfigResponse;
import com.airesume.server.dto.user.ResolvedTtsConfig;
import com.airesume.server.dto.user.UserTtsConnectivityTestRequest;
import com.airesume.server.dto.user.UserTtsConnectivityTestResponse;
import com.airesume.server.dto.user.UserTtsDiscoveryResponse;
import com.airesume.server.entity.SysTtsConfig;
import com.airesume.server.mapper.SysTtsConfigMapper;
import com.airesume.server.service.AiCredentialCrypto;
import com.airesume.server.service.SysTtsConfigService;
import com.airesume.server.service.TtsDiscoveryService;
import com.airesume.server.service.UserTtsConnectivityTestService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统级 TTS 配置服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysTtsConfigServiceImpl implements SysTtsConfigService {

    private static final int SINGLETON_KEY = 1;
    private static final String DEFAULT_TTS_PROVIDER = "openai";
    private static final String DEFAULT_TTS_ENDPOINT = "/audio/speech";
    private static final String SYSTEM_TTS_SOURCE = "system";

    private final SysTtsConfigMapper sysTtsConfigMapper;
    private final AiCredentialCrypto aiCredentialCrypto;
    private final UserTtsConnectivityTestService userTtsConnectivityTestService;
    private final TtsDiscoveryService ttsDiscoveryService;

    @Override
    public AdminTtsConfigResponse getCurrentConfig() {
        return buildResponse(sysTtsConfigMapper.selectCurrent());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "config:systemTts", allEntries = true)
    public AdminTtsConfigResponse saveConfig(AdminTtsConfigRequest request) {
        SysTtsConfig existing = sysTtsConfigMapper.selectCurrent();
        SysTtsConfig config = existing == null ? new SysTtsConfig() : existing;
        boolean enabled = Boolean.TRUE.equals(request.getEnabled());
        boolean shouldValidateCompleteFields = enabled || hasAnyConfigField(request);

        config.setSingletonKey(SINGLETON_KEY);
        config.setEnabled(enabled ? 1 : 0);
        config.setTtsProvider(normalizeProvider(request.getTtsProvider()));

        if (!shouldValidateCompleteFields) {
            // 禁用且未填写任何配置时允许保存空配置，便于管理员先关闭系统 TTS。
            config.setBaseUrl(null);
            config.setApiKey(null);
            config.setModel(null);
            config.setVoiceId(null);
            config.setEndpointPath(DEFAULT_TTS_ENDPOINT);
        } else {
            String baseUrl = validateBaseUrl(normalizeRequired(request.getBaseUrl(), "TTS 地址不能为空"));
            String apiKey = normalizeApiKeyForSave(request, existing);
            config.setBaseUrl(baseUrl);
            config.setApiKey(apiKey);
            config.setModel(normalizeRequired(request.getModel(), "TTS 模型不能为空"));
            config.setVoiceId(normalizeRequired(request.getVoiceId(), "TTS 音色不能为空"));
            config.setEndpointPath(normalizeEndpointPath(request.getEndpointPath()));
        }

        if (existing == null) {
            sysTtsConfigMapper.insert(config);
        } else {
            sysTtsConfigMapper.updateById(config);
        }
        return buildResponse(config);
    }

    @Override
    public UserTtsConnectivityTestResponse testConnectivity(AdminTtsConfigRequest request) {
        return userTtsConnectivityTestService.testConnectivity(buildConnectivityRequest(request));
    }

    @Override
    public byte[] previewVoice(AdminTtsConfigRequest request) {
        return userTtsConnectivityTestService.previewVoice(buildConnectivityRequest(request));
    }

    @Override
    public UserTtsDiscoveryResponse discover(AdminTtsConfigRequest request) {
        String baseUrl = validateBaseUrl(normalizeRequired(request.getBaseUrl(), "TTS 地址不能为空"));
        String apiKey = normalizePlainApiKey(request, sysTtsConfigMapper.selectCurrent());
        return ttsDiscoveryService.discover(baseUrl, apiKey, normalizeProvider(request.getTtsProvider()));
    }

    @Override
    @Cacheable(value = "config:systemTts", key = "'active'", unless = "#result == null")
    public SysTtsConfig getEnabledConfigEntity() {
        return sysTtsConfigMapper.selectEnabled();
    }

    @Override
    public ResolvedTtsConfig resolveEnabledConfig() {
        return buildResolvedConfig(sysTtsConfigMapper.selectEnabled());
    }

    private UserTtsConnectivityTestRequest buildConnectivityRequest(AdminTtsConfigRequest request) {
        SysTtsConfig existing = sysTtsConfigMapper.selectCurrent();
        UserTtsConnectivityTestRequest normalized = new UserTtsConnectivityTestRequest();
        normalized.setBaseUrl(validateBaseUrl(normalizeRequired(request.getBaseUrl(), "TTS 地址不能为空")));
        normalized.setApiKey(normalizePlainApiKey(request, existing));
        normalized.setModel(normalizeRequired(request.getModel(), "TTS 模型不能为空"));
        normalized.setVoiceId(normalizeRequired(request.getVoiceId(), "TTS 音色不能为空"));
        normalized.setEndpointPath(normalizeEndpointPath(request.getEndpointPath()));
        normalized.setTtsProvider(normalizeProvider(request.getTtsProvider()));
        return normalized;
    }

    private ResolvedTtsConfig buildResolvedConfig(SysTtsConfig config) {
        if (config == null || !isConfigured(config) || !Integer.valueOf(1).equals(config.getEnabled())) {
            return null;
        }
        String baseUrl;
        try {
            baseUrl = PublicHttpsUrlValidator.validate(config.getBaseUrl(), "TTS 地址不能为空");
        } catch (IllegalArgumentException ex) {
            log.warn("忽略非法系统级 TTS 地址, configId: {}", config.getId());
            return null;
        }
        String apiKey = trimToNull(aiCredentialCrypto.decrypt(config.getApiKey()));
        if (apiKey == null) {
            return null;
        }
        return ResolvedTtsConfig.builder()
                .source(SYSTEM_TTS_SOURCE)
                .configType(SYSTEM_TTS_SOURCE)
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .model(config.getModel().trim())
                .voiceId(config.getVoiceId().trim())
                .endpointPath(normalizeEndpointPath(config.getEndpointPath()))
                .ttsProvider(normalizeProvider(config.getTtsProvider()))
                .build();
    }

    private AdminTtsConfigResponse buildResponse(SysTtsConfig config) {
        if (config == null) {
            return AdminTtsConfigResponse.builder()
                    .enabled(false)
                    .configured(false)
                    .ttsProvider(DEFAULT_TTS_PROVIDER)
                    .endpointPath(DEFAULT_TTS_ENDPOINT)
                    .build();
        }
        return AdminTtsConfigResponse.builder()
                .id(config.getId())
                .enabled(Integer.valueOf(1).equals(config.getEnabled()))
                .configured(isConfigured(config))
                .ttsProvider(normalizeProvider(config.getTtsProvider()))
                .baseUrl(config.getBaseUrl())
                .apiKey(maskApiKey(trimToNull(aiCredentialCrypto.decrypt(config.getApiKey()))))
                .model(config.getModel())
                .voiceId(config.getVoiceId())
                .endpointPath(normalizeEndpointPath(config.getEndpointPath()))
                .updateTime(config.getUpdateTime())
                .build();
    }

    private String normalizeApiKeyForSave(AdminTtsConfigRequest request, SysTtsConfig existing) {
        String incoming = trimToNull(request.getApiKey());
        if (incoming == null || isMaskedApiKey(incoming)) {
            if (existing == null || trimToNull(existing.getApiKey()) == null) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "TTS API Key 不能为空");
            }
            return existing.getApiKey();
        }
        return aiCredentialCrypto.encrypt(incoming);
    }

    private String normalizePlainApiKey(AdminTtsConfigRequest request, SysTtsConfig existing) {
        String incoming = trimToNull(request.getApiKey());
        if (incoming != null && !isMaskedApiKey(incoming)) {
            return incoming;
        }
        if (existing == null || trimToNull(existing.getApiKey()) == null) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "TTS API Key 不能为空");
        }
        String decrypted = trimToNull(aiCredentialCrypto.decrypt(existing.getApiKey()));
        if (decrypted == null) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "TTS API Key 不能为空");
        }
        return decrypted;
    }

    private boolean isConfigured(SysTtsConfig config) {
        return config != null
                && trimToNull(config.getBaseUrl()) != null
                && trimToNull(config.getApiKey()) != null
                && trimToNull(config.getModel()) != null
                && trimToNull(config.getVoiceId()) != null;
    }

    private boolean hasAnyConfigField(AdminTtsConfigRequest request) {
        return trimToNull(request.getBaseUrl()) != null
                || trimToNull(request.getApiKey()) != null
                || trimToNull(request.getModel()) != null
                || trimToNull(request.getVoiceId()) != null;
    }

    private String normalizeProvider(String provider) {
        String normalized = trimToNull(provider);
        return normalized == null ? DEFAULT_TTS_PROVIDER : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeEndpointPath(String endpointPath) {
        String normalized = trimToNull(endpointPath);
        if (normalized == null) {
            return DEFAULT_TTS_ENDPOINT;
        }
        if (!normalized.startsWith("/")) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "TTS 端点路径必须以 / 开头");
        }
        return normalized;
    }

    private String validateBaseUrl(String baseUrl) {
        try {
            return PublicHttpsUrlValidator.validate(baseUrl, "TTS 地址不能为空");
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, ex.getMessage());
        }
    }

    private String normalizeRequired(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, message);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String maskApiKey(String apiKey) {
        String normalized = trimToNull(apiKey);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() <= 4) {
            return "****";
        }
        if (normalized.length() <= 8) {
            return normalized.substring(0, 2) + "****";
        }
        return normalized.substring(0, 3) + "****" + normalized.substring(normalized.length() - 4);
    }

    private boolean isMaskedApiKey(String apiKey) {
        return apiKey != null && apiKey.contains("****") && apiKey.trim().length() <= 32;
    }
}
