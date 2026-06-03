package com.airesume.server.service.impl;

import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.common.util.PublicHttpsUrlValidator;
import com.airesume.server.dto.admin.AiEngineConnectivityTestRequest;
import com.airesume.server.dto.admin.AiEngineConnectivityTestResponse;
import com.airesume.server.dto.user.UserAiConfigRequest;
import com.airesume.server.dto.user.UserAiConfigResponse;
import com.airesume.server.dto.user.UserAiConnectivityTestRequest;
import com.airesume.server.dto.user.UserAiConnectivityTestResponse;
import com.airesume.server.dto.user.UserTtsConnectivityTestRequest;
import com.airesume.server.dto.user.UserTtsConnectivityTestResponse;
import com.airesume.server.dto.user.UserTtsDiscoveryRequest;
import com.airesume.server.dto.user.UserTtsDiscoveryResponse;
import com.airesume.server.entity.UserAiConfig;
import com.airesume.server.mapper.UserAiConfigMapper;
import com.airesume.server.service.AiCredentialCrypto;
import com.airesume.server.service.AiEngineConnectivityTestService;
import com.airesume.server.service.TtsDiscoveryService;
import com.airesume.server.service.UserAiConfigService;
import com.airesume.server.service.UserTtsConnectivityTestService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户自定义 AI 配置管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserAiConfigServiceImpl extends ServiceImpl<UserAiConfigMapper, UserAiConfig>
        implements UserAiConfigService {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_VERIFIED = "verified";
    private static final String STATUS_FAILED = "failed";
    private static final String CONFIG_TYPE_DEFAULT = "default";
    private static final String CONFIG_TYPE_INTERVIEW = "interview";

    private final AiCredentialCrypto aiCredentialCrypto;
    private final AiEngineConnectivityTestService aiEngineConnectivityTestService;
    private final UserTtsConnectivityTestService userTtsConnectivityTestService;
    private final TtsDiscoveryService ttsDiscoveryService;

    @Override
    public List<UserAiConfigResponse> listUserConfigs(Long userId) {
        return list(new LambdaQueryWrapper<UserAiConfig>()
                .eq(UserAiConfig::getUserId, userId)
                .orderByAsc(UserAiConfig::getConfigType))
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAiConfigResponse saveUserConfig(Long userId, UserAiConfigRequest request) {
        String configType = normalizeConfigType(request.getConfigType());
        String baseUrl = validateBaseUrl(request.getBaseUrl());
        String apiKey = normalizeRequired(request.getApiKey(), "API Key 不能为空");
        if (isMaskedApiKey(apiKey)) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "API Key 不能使用脱敏值保存");
        }
        String model = normalizeRequired(request.getModel(), "模型不能为空");

        // 保存前先连通测试，避免把明显不可用或被 SSRF 拦截的地址落库。
        UserAiConnectivityTestResponse testResponse = testConnectivity(toConnectivityRequest(baseUrl, apiKey, model,
                Boolean.TRUE.equals(request.getSupportsMultimodal())));
        if (!Boolean.TRUE.equals(testResponse.getSuccess())) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONNECTIVITY_FAILED, testResponse.getMessage());
        }

        UserAiConfig config = getOne(new LambdaQueryWrapper<UserAiConfig>()
                .eq(UserAiConfig::getUserId, userId)
                .eq(UserAiConfig::getConfigType, configType)
                .last("limit 1"), false);
        if (config == null) {
            config = new UserAiConfig();
            config.setUserId(userId);
            config.setConfigType(configType);
            config.setIsEnabled(1);
        }
        config.setProviderName(trimToNull(request.getProviderName()));
        config.setBaseUrl(baseUrl);
        config.setApiKey(aiCredentialCrypto.encrypt(apiKey));
        config.setModel(model);
        config.setSupportsMultimodal(Boolean.TRUE.equals(request.getSupportsMultimodal()) ? 1 : 0);
        applyTtsFields(config, request);
        config.setVerificationStatus(STATUS_VERIFIED);
        config.setLastVerifiedAt(LocalDateTime.now());
        saveOrUpdate(config);
        return buildResponse(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserConfig(Long userId, String configType) {
        String normalizedType = normalizeConfigType(configType);
        // API Key 属于用户敏感凭据，删除时物理清除当前有效记录，避免逻辑删除占用唯一键。
        int removed = getBaseMapper().deleteActiveConfig(userId, normalizedType);
        if (removed <= 0) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "配置不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleUserConfig(Long userId, String configType, boolean enabled) {
        String normalizedType = normalizeConfigType(configType);
        boolean updated = update(new LambdaUpdateWrapper<UserAiConfig>()
                .eq(UserAiConfig::getUserId, userId)
                .eq(UserAiConfig::getConfigType, normalizedType)
                .set(UserAiConfig::getIsEnabled, enabled ? 1 : 0));
        if (!updated) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "配置不存在");
        }
    }

    @Override
    public UserAiConnectivityTestResponse testConnectivity(UserAiConnectivityTestRequest request) {
        String baseUrl = validateBaseUrl(request.getBaseUrl());
        String apiKey = normalizeRequired(request.getApiKey(), "API Key 不能为空");
        String model = normalizeRequired(request.getModel(), "模型不能为空");
        AiEngineConnectivityTestRequest adminRequest = new AiEngineConnectivityTestRequest();
        adminRequest.setProviderType("openai");
        adminRequest.setBaseUrl(baseUrl);
        adminRequest.setModelName(model);
        adminRequest.setApiKey(apiKey);
        adminRequest.setMaxTokens(8);
        adminRequest.setTimeoutMs(10000);

        AiEngineConnectivityTestResponse response =
                aiEngineConnectivityTestService.testConnectivity(adminRequest, apiKey);
        return UserAiConnectivityTestResponse.builder()
                .success(Boolean.TRUE.equals(response.getSuccess()))
                .message(Boolean.TRUE.equals(response.getSuccess()) ? "连通测试成功" : response.getMessage())
                .errorType(Boolean.TRUE.equals(response.getSuccess()) ? null : "NETWORK_ERROR")
                .latencyMs(response.getLatencyMs())
                .responsePreview(response.getResponsePreview())
                .build();
    }

    @Override
    public UserTtsConnectivityTestResponse testTtsConnectivity(UserTtsConnectivityTestRequest request) {
        UserTtsConnectivityTestRequest normalized = normalizeTtsRequest(request);
        return userTtsConnectivityTestService.testConnectivity(normalized);
    }

    @Override
    public byte[] previewTtsVoice(UserTtsConnectivityTestRequest request) {
        UserTtsConnectivityTestRequest normalized = normalizeTtsRequest(request);
        return userTtsConnectivityTestService.previewVoice(normalized);
    }

    @Override
    public UserTtsDiscoveryResponse discoverTtsModelsAndVoices(UserTtsDiscoveryRequest request) {
        String baseUrl = normalizeRequired(request.getBaseUrl(), "TTS 地址不能为空");
        String apiKey = normalizeRequired(request.getApiKey(), "TTS API Key 不能为空");
        return ttsDiscoveryService.discover(baseUrl, apiKey, request.getProvider());
    }

    @Override
    public UserAiConfig findEnabledConfig(Long userId, String configType) {
        if (userId == null) {
            return null;
        }
        String normalizedType = normalizeConfigType(configType);
        return getOne(new LambdaQueryWrapper<UserAiConfig>()
                .eq(UserAiConfig::getUserId, userId)
                .eq(UserAiConfig::getConfigType, normalizedType)
                .eq(UserAiConfig::getIsEnabled, 1)
                .last("limit 1"), false);
    }

    private UserAiConnectivityTestRequest toConnectivityRequest(
            String baseUrl,
            String apiKey,
            String model,
            boolean supportsMultimodal) {
        UserAiConnectivityTestRequest request = new UserAiConnectivityTestRequest();
        request.setBaseUrl(baseUrl);
        request.setApiKey(apiKey);
        request.setModel(model);
        request.setSupportsMultimodal(supportsMultimodal);
        return request;
    }

    private UserAiConfigResponse buildResponse(UserAiConfig config) {
        boolean allowTts = supportsTtsConfigType(config.getConfigType());
        return UserAiConfigResponse.builder()
                .configType(config.getConfigType())
                .providerName(config.getProviderName())
                .baseUrl(config.getBaseUrl())
                .apiKey(maskApiKey(aiCredentialCrypto.decrypt(config.getApiKey())))
                .model(config.getModel())
                .enabled(Integer.valueOf(1).equals(config.getIsEnabled()))
                .supportsMultimodal(Integer.valueOf(1).equals(config.getSupportsMultimodal()))
                .ttsBaseUrl(allowTts ? config.getTtsBaseUrl() : null)
                .ttsApiKey(allowTts ? maskApiKey(decryptNullable(config.getTtsApiKey())) : null)
                .ttsModel(allowTts ? config.getTtsModel() : null)
                .ttsVoiceId(allowTts ? config.getTtsVoiceId() : null)
                .ttsEndpointPath(allowTts ? config.getTtsEndpointPath() : null)
                .ttsProvider(allowTts ? config.getTtsProvider() : null)
                .ttsConfigured(allowTts && isTtsConfigured(config))
                .lastVerifiedAt(config.getLastVerifiedAt())
                .verificationStatus(config.getVerificationStatus() == null ? STATUS_PENDING : config.getVerificationStatus())
                .updateTime(config.getUpdateTime())
                .build();
    }

    /**
     * TTS 只服务通用兜底与面试对话播报预留能力，简历诊断保存时一律清空，避免无关配置挤入简历链路。
     */
    private void applyTtsFields(UserAiConfig config, UserAiConfigRequest request) {
        if (!supportsTtsConfigType(config.getConfigType()) || !hasAnyTtsField(request)) {
            config.setTtsBaseUrl(null);
            config.setTtsApiKey(null);
            config.setTtsModel(null);
            config.setTtsVoiceId(null);
            config.setTtsEndpointPath(null);
            config.setTtsProvider(null);
            return;
        }
        UserTtsConnectivityTestRequest ttsRequest = new UserTtsConnectivityTestRequest();
        ttsRequest.setBaseUrl(request.getTtsBaseUrl());
        ttsRequest.setApiKey(request.getTtsApiKey());
        ttsRequest.setModel(request.getTtsModel());
        ttsRequest.setVoiceId(request.getTtsVoiceId());
        ttsRequest.setEndpointPath(request.getTtsEndpointPath());
        ttsRequest.setTtsProvider(request.getTtsProvider());
        UserTtsConnectivityTestRequest normalized = normalizeTtsRequest(ttsRequest);
        UserTtsConnectivityTestResponse response = userTtsConnectivityTestService.testConnectivity(normalized);
        if (!Boolean.TRUE.equals(response.getSuccess())) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONNECTIVITY_FAILED, response.getMessage());
        }
        config.setTtsBaseUrl(normalized.getBaseUrl());
        config.setTtsApiKey(aiCredentialCrypto.encrypt(normalized.getApiKey()));
        config.setTtsModel(normalized.getModel());
        config.setTtsVoiceId(normalized.getVoiceId());
        config.setTtsEndpointPath(normalized.getEndpointPath());
        config.setTtsProvider(trimToNull(request.getTtsProvider()));
    }

    private UserTtsConnectivityTestRequest normalizeTtsRequest(UserTtsConnectivityTestRequest request) {
        UserTtsConnectivityTestRequest normalized = new UserTtsConnectivityTestRequest();
        normalized.setBaseUrl(validateBaseUrl(normalizeRequired(request.getBaseUrl(), "TTS 地址不能为空")));
        String apiKey = normalizeRequired(request.getApiKey(), "TTS API Key 不能为空");
        if (isMaskedApiKey(apiKey)) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "TTS API Key 不能使用脱敏值保存或测试");
        }
        normalized.setApiKey(apiKey);
        normalized.setModel(normalizeRequired(request.getModel(), "TTS 模型不能为空"));
        normalized.setVoiceId(normalizeRequired(request.getVoiceId(), "TTS 音色不能为空"));
        // TTS 端点和 Provider 是下游合成协议选择的关键参数，测试、试听、保存前验证必须一起透传。
        normalized.setEndpointPath(trimToNull(request.getEndpointPath()));
        normalized.setTtsProvider(trimToNull(request.getTtsProvider()));
        return normalized;
    }

    private boolean hasAnyTtsField(UserAiConfigRequest request) {
        return trimToNull(request.getTtsBaseUrl()) != null
                || trimToNull(request.getTtsApiKey()) != null
                || trimToNull(request.getTtsModel()) != null
                || trimToNull(request.getTtsVoiceId()) != null;
    }

    private boolean isTtsConfigured(UserAiConfig config) {
        return trimToNull(config.getTtsBaseUrl()) != null
                && trimToNull(config.getTtsApiKey()) != null
                && trimToNull(config.getTtsModel()) != null
                && trimToNull(config.getTtsVoiceId()) != null;
    }

    private boolean supportsTtsConfigType(String configType) {
        return CONFIG_TYPE_DEFAULT.equals(configType) || CONFIG_TYPE_INTERVIEW.equals(configType);
    }

    private String decryptNullable(String encryptedValue) {
        return trimToNull(encryptedValue) == null ? null : aiCredentialCrypto.decrypt(encryptedValue);
    }

    private String normalizeConfigType(String configType) {
        String normalized = trimToNull(configType);
        normalized = normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
        if (!UserAiConstants.SUPPORTED_CONFIG_TYPES.contains(normalized)) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "配置类型只支持 default/resume/interview");
        }
        return normalized;
    }

    private String validateBaseUrl(String baseUrl) {
        try {
            return PublicHttpsUrlValidator.validate(normalizeRequired(baseUrl, "API 地址不能为空"), "API 地址不能为空");
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, e.getMessage());
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
        return apiKey != null && apiKey.contains("****") && apiKey.trim().length() <= 20;
    }
}
