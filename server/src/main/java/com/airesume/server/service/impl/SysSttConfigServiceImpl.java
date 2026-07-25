package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.common.util.PublicHttpsUrlValidator;
import com.airesume.server.dto.admin.AdminSttConfigRequest;
import com.airesume.server.dto.admin.AdminSttConfigResponse;
import com.airesume.server.dto.admin.AdminSttConnectivityTestResponse;
import com.airesume.server.dto.user.ResolvedSttConfig;
import com.airesume.server.entity.SysSttConfig;
import com.airesume.server.mapper.SysSttConfigMapper;
import com.airesume.server.service.AiCredentialCrypto;
import com.airesume.server.service.SysSttConfigService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 系统级语音识别（STT）配置服务实现。
 * <p>
 * 管理端维护一套系统级 STT 配置，供语音面试在浏览器 Web Speech 不可用时做云端识别兜底。
 * 结构镜像 {@link SysTtsConfigServiceImpl}：单例存储、API Key 加密与脱敏、编辑态复用旧密钥。
 * 与面试对话 AI、TTS 配置完全独立，互不影响。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysSttConfigServiceImpl implements SysSttConfigService {

    private static final int SINGLETON_KEY = 1;
    private static final String DEFAULT_STT_ENDPOINT = "/audio/transcriptions";
    /** 连通性测试用的最短静音音频（1 帧 WAV），只验证鉴权和端点，不追求识别结果。 */
    private static final int TEST_TIMEOUT_MS = 10000;

    private final SysSttConfigMapper sysSttConfigMapper;
    private final AiCredentialCrypto aiCredentialCrypto;
    private final RestClient.Builder restClientBuilder;

    @Override
    public AdminSttConfigResponse getCurrentConfig() {
        return buildResponse(sysSttConfigMapper.selectCurrent());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "config:systemStt", allEntries = true)
    public AdminSttConfigResponse saveConfig(AdminSttConfigRequest request) {
        SysSttConfig existing = sysSttConfigMapper.selectCurrent();
        SysSttConfig config = existing == null ? new SysSttConfig() : existing;
        boolean enabled = Boolean.TRUE.equals(request.getEnabled());
        boolean shouldValidateCompleteFields = enabled || hasAnyConfigField(request);

        config.setSingletonKey(SINGLETON_KEY);
        config.setEnabled(enabled ? 1 : 0);

        if (!shouldValidateCompleteFields) {
            // 禁用且未填写任何配置时允许保存空配置，便于管理员先关闭系统 STT。
            config.setBaseUrl(null);
            config.setApiKey(null);
            config.setModel(null);
            config.setEndpointPath(DEFAULT_STT_ENDPOINT);
        } else {
            String baseUrl = validateBaseUrl(normalizeRequired(request.getBaseUrl(), "STT 地址不能为空"));
            String apiKey = normalizeApiKeyForSave(request, existing);
            config.setBaseUrl(baseUrl);
            config.setApiKey(apiKey);
            config.setModel(normalizeRequired(request.getModel(), "STT 模型不能为空"));
            config.setEndpointPath(normalizeEndpointPath(request.getEndpointPath()));
        }

        if (existing == null) {
            sysSttConfigMapper.insert(config);
        } else {
            sysSttConfigMapper.updateById(config);
        }
        return buildResponse(config);
    }

    @Override
    public AdminSttConnectivityTestResponse testConnectivity(AdminSttConfigRequest request) {
        String baseUrl;
        try {
            baseUrl = validateBaseUrl(normalizeRequired(request.getBaseUrl(), "STT 地址不能为空"));
        } catch (BusinessException ex) {
            return AdminSttConnectivityTestResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .errorType("CONFIG_ERROR")
                    .build();
        }
        String model = normalizeRequired(request.getModel(), "STT 模型不能为空");
        String endpointPath = resolveEndpointPath(request.getEndpointPath());
        String apiKey = normalizePlainApiKey(request, sysSttConfigMapper.selectCurrent());

        long start = System.currentTimeMillis();
        try {
            // 只发一段最短静音音频验证鉴权与端点可达；识别结果为空属正常，不视为失败。
            createRestClient(baseUrl, TEST_TIMEOUT_MS)
                    .post()
                    .uri(endpointPath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(buildTestMultipartBody(model))
                    .retrieve()
                    .body(String.class);
            return AdminSttConnectivityTestResponse.builder()
                    .success(true)
                    .message("STT 连通测试成功")
                    .endpointPath(endpointPath)
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        } catch (RestClientResponseException ex) {
            log.warn("系统 STT 连通测试失败, status: {}, model: {}", ex.getStatusCode().value(), model);
            return AdminSttConnectivityTestResponse.builder()
                    .success(false)
                    .message("STT 连通测试失败：上游返回 " + ex.getStatusCode().value())
                    .endpointPath(endpointPath)
                    .latencyMs(System.currentTimeMillis() - start)
                    .errorType("API_ERROR")
                    .build();
        } catch (Exception ex) {
            log.warn("系统 STT 连通测试异常, errorType: {}", ex.getClass().getSimpleName());
            return AdminSttConnectivityTestResponse.builder()
                    .success(false)
                    .message("STT 连通测试失败：无法连接语音识别服务")
                    .endpointPath(endpointPath)
                    .latencyMs(System.currentTimeMillis() - start)
                    .errorType("NETWORK_ERROR")
                    .build();
        }
    }

    @Override
    @CacheEvict(value = "config:systemStt", allEntries = true)
    public boolean isCloudSttAvailable() {
        return resolveEnabledConfig() != null;
    }

    @Override
    public ResolvedSttConfig resolveEnabledConfig() {
        return buildResolvedConfig(sysSttConfigMapper.selectEnabled());
    }

    private ResolvedSttConfig buildResolvedConfig(SysSttConfig config) {
        if (config == null || !isConfigured(config) || !Integer.valueOf(1).equals(config.getEnabled())) {
            return null;
        }
        String baseUrl;
        try {
            baseUrl = PublicHttpsUrlValidator.validate(config.getBaseUrl(), "STT 地址不能为空");
        } catch (IllegalArgumentException ex) {
            log.warn("忽略非法系统级 STT 地址, configId: {}", config.getId());
            return null;
        }
        String apiKey = trimToNull(aiCredentialCrypto.decrypt(config.getApiKey()));
        if (apiKey == null) {
            return null;
        }
        return ResolvedSttConfig.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .model(config.getModel().trim())
                .endpointPath(normalizeEndpointPath(config.getEndpointPath()))
                .build();
    }

    /**
     * 构造 OpenAI 兼容 /audio/transcriptions 连通测试请求体：最短静音 WAV + 模型。
     */
    private org.springframework.util.MultiValueMap<String, org.springframework.http.HttpEntity<?>> buildTestMultipartBody(String model) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        Resource silentWav = new ByteArrayResource(buildSilentWav()) {
            @Override
            public String getFilename() {
                return "probe.wav";
            }
        };
        builder.part("file", silentWav).contentType(MediaType.valueOf("audio/wav"));
        builder.part("model", model);
        return builder.build();
    }

    /**
     * 生成一段极短静音 WAV（约 100ms，8kHz 单声道），仅用于连通性探测。
     */
    private byte[] buildSilentWav() {
        int sampleRate = 8000;
        int samples = sampleRate / 10;
        int dataSize = samples * 2;
        byte[] wav = new byte[44 + dataSize];
        writeAscii(wav, 0, "RIFF");
        writeLittleEndianInt(wav, 4, 36 + dataSize);
        writeAscii(wav, 8, "WAVE");
        writeAscii(wav, 12, "fmt ");
        writeLittleEndianInt(wav, 16, 16);
        writeLittleEndianShort(wav, 20, 1);
        writeLittleEndianShort(wav, 22, 1);
        writeLittleEndianInt(wav, 24, sampleRate);
        writeLittleEndianInt(wav, 28, sampleRate * 2);
        writeLittleEndianShort(wav, 32, 2);
        writeLittleEndianShort(wav, 34, 16);
        writeAscii(wav, 36, "data");
        writeLittleEndianInt(wav, 40, dataSize);
        // data 段全 0 即静音，无需再写。
        return wav;
    }

    private void writeAscii(byte[] target, int offset, String value) {
        byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, target, offset, bytes.length);
    }

    private void writeLittleEndianInt(byte[] target, int offset, int value) {
        target[offset] = (byte) (value & 0xff);
        target[offset + 1] = (byte) ((value >> 8) & 0xff);
        target[offset + 2] = (byte) ((value >> 16) & 0xff);
        target[offset + 3] = (byte) ((value >> 24) & 0xff);
    }

    private void writeLittleEndianShort(byte[] target, int offset, int value) {
        target[offset] = (byte) (value & 0xff);
        target[offset + 1] = (byte) ((value >> 8) & 0xff);
    }

    protected RestClient createRestClient(String baseUrl, int timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return restClientBuilder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    private AdminSttConfigResponse buildResponse(SysSttConfig config) {
        if (config == null) {
            return AdminSttConfigResponse.builder()
                    .enabled(false)
                    .configured(false)
                    .endpointPath(DEFAULT_STT_ENDPOINT)
                    .build();
        }
        return AdminSttConfigResponse.builder()
                .enabled(Integer.valueOf(1).equals(config.getEnabled()))
                .configured(isConfigured(config))
                .baseUrl(config.getBaseUrl())
                .apiKey(maskApiKey(trimToNull(aiCredentialCrypto.decrypt(config.getApiKey()))))
                .model(config.getModel())
                .endpointPath(normalizeEndpointPath(config.getEndpointPath()))
                .build();
    }

    private String normalizeApiKeyForSave(AdminSttConfigRequest request, SysSttConfig existing) {
        String incoming = trimToNull(request.getApiKey());
        if (incoming == null || isMaskedApiKey(incoming)) {
            if (existing == null || trimToNull(existing.getApiKey()) == null) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "STT API Key 不能为空");
            }
            return existing.getApiKey();
        }
        return aiCredentialCrypto.encrypt(incoming);
    }

    private String normalizePlainApiKey(AdminSttConfigRequest request, SysSttConfig existing) {
        String incoming = trimToNull(request.getApiKey());
        if (incoming != null && !isMaskedApiKey(incoming)) {
            return incoming;
        }
        if (existing == null || trimToNull(existing.getApiKey()) == null) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "STT API Key 不能为空");
        }
        String decrypted = trimToNull(aiCredentialCrypto.decrypt(existing.getApiKey()));
        if (decrypted == null) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "STT API Key 不能为空");
        }
        return decrypted;
    }

    private boolean isConfigured(SysSttConfig config) {
        if (config == null) {
            return false;
        }
        return trimToNull(config.getBaseUrl()) != null
                && trimToNull(config.getModel()) != null
                && trimToNull(config.getApiKey()) != null;
    }

    private boolean hasAnyConfigField(AdminSttConfigRequest request) {
        return trimToNull(request.getBaseUrl()) != null
                || trimToNull(request.getApiKey()) != null
                || trimToNull(request.getModel()) != null;
    }

    private String normalizeEndpointPath(String endpointPath) {
        String normalized = trimToNull(endpointPath);
        if (normalized == null) {
            return DEFAULT_STT_ENDPOINT;
        }
        if (!normalized.startsWith("/")) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "STT 端点路径必须以 / 开头");
        }
        return normalized;
    }

    private String validateBaseUrl(String baseUrl) {
        try {
            return PublicHttpsUrlValidator.validate(baseUrl, "STT 地址不能为空");
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

    private String resolveEndpointPath(String endpointPath) {
        String trimmed = trimToNull(endpointPath);
        if (trimmed == null || !trimmed.startsWith("/")) {
            return DEFAULT_STT_ENDPOINT;
        }
        return trimmed;
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
