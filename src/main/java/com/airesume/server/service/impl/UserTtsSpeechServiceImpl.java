package com.airesume.server.service.impl;

import com.airesume.server.common.constants.TtsProviderConstants;
import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.common.util.PublicHttpsUrlValidator;
import com.airesume.server.dto.user.ResolvedTtsConfig;
import com.airesume.server.entity.UserAiConfig;
import com.airesume.server.service.AiCredentialCrypto;
import com.airesume.server.service.UserAiConfigService;
import com.airesume.server.service.UserTtsSpeechService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 用户自定义 TTS 运行时服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserTtsSpeechServiceImpl implements UserTtsSpeechService {

    /** 默认 TTS 合成端点路径（OpenAI 标准）。 */
    private static final String DEFAULT_TTS_ENDPOINT = "/audio/speech";
    private static final int TTS_TIMEOUT_MS = 15000;

    private final UserAiConfigService userAiConfigService;
    private final AiCredentialCrypto aiCredentialCrypto;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public ResolvedTtsConfig resolveInterviewTtsConfig(Long userId) {
        if (userId == null) {
            return null;
        }
        // TTS 只服务语音面试播报，解析顺序固定为 interview -> default，永远不读取 resume 配置。
        UserAiConfig interviewConfig = userAiConfigService.findEnabledConfig(userId, UserAiConstants.CONFIG_TYPE_INTERVIEW);
        ResolvedTtsConfig resolved = buildResolvedConfig(interviewConfig);
        if (resolved != null) {
            return resolved;
        }
        return buildResolvedConfig(userAiConfigService.findEnabledConfig(userId, UserAiConstants.CONFIG_TYPE_DEFAULT));
    }

    @Override
    public boolean hasInterviewTtsConfig(Long userId) {
        return resolveInterviewTtsConfig(userId) != null;
    }

    @Override
    public byte[] synthesizeInterviewSpeech(Long userId, String text) {
        ResolvedTtsConfig config = resolveInterviewTtsConfig(userId);
        if (config == null) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "未配置可用于语音面试的 TTS");
        }
        String normalizedText = normalizeRequired(text, "TTS 文本不能为空");

        // 根据 Provider 分发到不同的协议处理器
        TtsProviderConstants.TtsApiFormat apiFormat = TtsProviderConstants.resolveApiFormat(config.getTtsProvider());
        if (apiFormat == TtsProviderConstants.TtsApiFormat.CHAT_COMPLETIONS_TTS) {
            return synthesizeViaChatCompletions(userId, config, normalizedText);
        }
        return synthesizeViaAudioSpeech(userId, config, normalizedText);
    }

    /**
     * OpenAI 标准 /audio/speech 合成（二进制音频响应）。
     */
    private byte[] synthesizeViaAudioSpeech(Long userId, ResolvedTtsConfig config, String text) {
        String endpointPath = resolveEndpointPath(config.getEndpointPath());
        try {
            byte[] audioBytes = createRestClient(config.getBaseUrl(), TTS_TIMEOUT_MS)
                    .post()
                    .uri(endpointPath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                    .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.valueOf("audio/mpeg"))
                    .body(buildOpenAiRequestBody(config, text))
                    .retrieve()
                    .body(byte[].class);
            if (audioBytes == null || audioBytes.length == 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 返回音频为空");
            }
            return audioBytes;
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.warn("用户自定义 TTS 合成失败 (OpenAI), userId: {}, configType: {}, status: {}",
                    userId, config.getConfigType(), ex.getStatusCode().value());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 合成失败，请检查语音合成配置");
        } catch (Exception ex) {
            log.warn("用户自定义 TTS 合成异常 (OpenAI), userId: {}, configType: {}, errorType: {}",
                    userId, config.getConfigType(), ex.getClass().getSimpleName());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 合成失败，请稍后重试");
        }
    }

    /**
     * Chat Completions TTS 合成（base64 JSON 响应），用于 MiMo 等 Provider。
     * <p>
     * 端点：POST /chat/completions
     * 认证头：api-key: {key}
     * 请求体：{model, messages: [{role:"assistant", content: text}], audio: {format:"mp3", voice: voiceId}}
     * 响应：JSON choices[0].message.audio.data（base64 编码音频）
     */
    private byte[] synthesizeViaChatCompletions(Long userId, ResolvedTtsConfig config, String text) {
        TtsProviderConstants.ProviderPreset preset = TtsProviderConstants.findPreset(config.getTtsProvider());
        String endpointPath = preset.getDefaultEndpointPath();
        try {
            // 构造 Chat Completions TTS 请求体
            Map<String, Object> audio = new LinkedHashMap<>();
            audio.put("format", "mp3");
            audio.put("voice", config.getVoiceId());
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", "assistant");
            message.put("content", text);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", config.getModel());
            body.put("messages", List.of(message));
            body.put("audio", audio);

            String responseJson = createRestClient(config.getBaseUrl(), TTS_TIMEOUT_MS)
                    .post()
                    .uri(endpointPath)
                    .header(preset.getApiFormat().getAuthHeaderName(),
                            preset.getApiFormat().getAuthHeaderPrefix() + config.getApiKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (responseJson == null || responseJson.isBlank()) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 返回响应为空");
            }

            // 解析 JSON 响应：choices[0].message.audio.data → base64 → byte[]
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode audioDataNode = root.path("choices").path(0).path("message").path("audio").path("data");
            if (audioDataNode.isMissingNode() || audioDataNode.asText("").isEmpty()) {
                log.warn("Chat Completions TTS 响应缺少音频数据, userId: {}, response preview: {}",
                        userId, trimText(responseJson, 300));
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 响应中未找到音频数据");
            }
            byte[] audioBytes = Base64.getDecoder().decode(audioDataNode.asText());
            if (audioBytes.length == 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 返回音频为空");
            }
            return audioBytes;
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.warn("用户自定义 TTS 合成失败 (ChatCompletions), userId: {}, configType: {}, status: {}",
                    userId, config.getConfigType(), ex.getStatusCode().value());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 合成失败，请检查语音合成配置");
        } catch (Exception ex) {
            log.warn("用户自定义 TTS 合成异常 (ChatCompletions), userId: {}, configType: {}, errorType: {}",
                    userId, config.getConfigType(), ex.getClass().getSimpleName());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 合成失败，请稍后重试");
        }
    }

    protected RestClient createRestClient(String baseUrl, int timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return restClientBuilder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 构造 OpenAI 兼容 /audio/speech 请求体。
     */
    private Map<String, Object> buildOpenAiRequestBody(ResolvedTtsConfig config, String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModel());
        body.put("voice", config.getVoiceId());
        body.put("input", text);
        body.put("response_format", "mp3");
        return body;
    }

    private ResolvedTtsConfig buildResolvedConfig(UserAiConfig config) {
        if (config == null || !hasCompleteTtsFields(config)) {
            return null;
        }
        String baseUrl;
        try {
            baseUrl = PublicHttpsUrlValidator.validate(config.getTtsBaseUrl(), "TTS 地址不能为空");
        } catch (IllegalArgumentException ex) {
            log.warn("忽略非法用户自定义 TTS 地址, userId: {}, configType: {}",
                    config.getUserId(), config.getConfigType());
            return null;
        }
        String apiKey = aiCredentialCrypto.decrypt(config.getTtsApiKey());
        if (trimToNull(apiKey) == null) {
            return null;
        }
        return ResolvedTtsConfig.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .model(config.getTtsModel().trim())
                .voiceId(config.getTtsVoiceId().trim())
                .endpointPath(config.getTtsEndpointPath())
                .ttsProvider(config.getTtsProvider())
                .configType(config.getConfigType())
                .build();
    }

    private boolean hasCompleteTtsFields(UserAiConfig config) {
        return trimToNull(config.getTtsBaseUrl()) != null
                && trimToNull(config.getTtsApiKey()) != null
                && trimToNull(config.getTtsModel()) != null
                && trimToNull(config.getTtsVoiceId()) != null;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, message);
        }
        return normalized;
    }

    /**
     * 解析 TTS 端点路径，为空或无效时回落到 OpenAI 标准 /audio/speech。
     */
    private String resolveEndpointPath(String endpointPath) {
        if (endpointPath == null || endpointPath.isBlank()) {
            return DEFAULT_TTS_ENDPOINT;
        }
        String trimmed = endpointPath.trim();
        if (!trimmed.startsWith("/")) {
            return DEFAULT_TTS_ENDPOINT;
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

    private String trimText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
