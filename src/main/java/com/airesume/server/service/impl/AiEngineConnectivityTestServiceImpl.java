package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.util.PublicHttpsUrlValidator;
import com.airesume.server.dto.admin.AiEngineConnectivityTestRequest;
import com.airesume.server.dto.admin.AiEngineConnectivityTestResponse;
import com.airesume.server.service.AiEngineConnectivityTestService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 管理端 AI 引擎连通测试服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiEngineConnectivityTestServiceImpl implements AiEngineConnectivityTestService {

    private static final int MIN_TIMEOUT_MS = 1000;
    private static final int MAX_TIMEOUT_MS = 300000;
    private static final int TEST_MAX_TOKENS = 64;
    private static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public AiEngineConnectivityTestResponse testConnectivity(AiEngineConnectivityTestRequest request, String apiKey) {
        String providerType = normalizeRequiredValue(request.getProviderType(), "Provider 不能为空").toLowerCase(Locale.ROOT);
        String modelName = normalizeRequiredValue(request.getModelName(), "模型名称不能为空");
        if ("mock".equals(providerType)) {
            return AiEngineConnectivityTestResponse.builder()
                    .success(true)
                    .message("Mock 引擎无需外部网络连通，配置格式有效。")
                    .providerType(providerType)
                    .modelName(modelName)
                    .endpoint("mock://local")
                    .latencyMs(0L)
                    .responsePreview("mock-ok")
                    .build();
        }

        String configuredBaseUrl = normalizeRequiredValue(request.getBaseUrl(), "基础地址不能为空");
        String baseUrl;
        try {
            baseUrl = PublicHttpsUrlValidator.validate(configuredBaseUrl, "基础地址不能为空");
        } catch (IllegalArgumentException e) {
            // 连通测试需要把配置错误作为测试结果返回给管理端，避免管理员只看到一次请求异常。
            return buildInvalidBaseUrlFailure(providerType, modelName, configuredBaseUrl, e.getMessage());
        }
        String normalizedApiKey = normalizeRequiredValue(apiKey, "API Key 不能为空");
        int timeoutMs = normalizeTimeout(request.getTimeoutMs());
        String endpoint = baseUrl + CHAT_COMPLETIONS_ENDPOINT;

        long start = System.currentTimeMillis();
        try {
            Map<String, Object> requestBody = buildRequestBody(request, modelName);
            RestClient restClient = createRestClient(baseUrl, timeoutMs);

            // 连通测试只发送一次极小 token 的 chat/completions 请求，避免测试动作产生明显成本。
            String rawResponse = restClient.post()
                    .uri(CHAT_COMPLETIONS_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + normalizedApiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            long latencyMs = System.currentTimeMillis() - start;
            String preview = extractResponsePreview(rawResponse);
            return AiEngineConnectivityTestResponse.builder()
                    .success(true)
                    .message("连通测试成功，当前配置可以正常调用模型。")
                    .providerType(providerType)
                    .modelName(modelName)
                    .endpoint(endpoint)
                    .latencyMs(latencyMs)
                    .responsePreview(preview)
                    .build();
        } catch (RestClientResponseException ex) {
            return buildFailure(providerType, modelName, endpoint, start,
                    "上游返回 HTTP " + ex.getStatusCode().value() + "：" + trimText(ex.getResponseBodyAsString(), 200));
        } catch (Exception ex) {
            return buildFailure(providerType, modelName, endpoint, start, ex.getMessage());
        }
    }

    /**
     * 构造 OpenAI 兼容 chat/completions 请求体。
     */
    private Map<String, Object> buildRequestBody(AiEngineConnectivityTestRequest request, String modelName) {
        int maxTokens = request.getMaxTokens() == null
                ? TEST_MAX_TOKENS
                : Math.max(1, Math.min(TEST_MAX_TOKENS, request.getMaxTokens()));
        BigDecimal temperature = request.getTemperature() == null ? BigDecimal.ZERO : request.getTemperature();
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("model", modelName);
        body.put("messages", List.of(
                Map.of("role", "system", "content", "You are a connectivity test assistant. Reply only: ok"),
                Map.of("role", "user", "content", "Reply only: ok")
        ));
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);

        String thinkingMode = request.getThinkingMode();
        if ("enabled".equalsIgnoreCase(thinkingMode) || "disabled".equalsIgnoreCase(thinkingMode)) {
            body.put("thinking", Map.of("type", thinkingMode.toLowerCase(Locale.ROOT)));
        }
        return body;
    }

    private AiEngineConnectivityTestResponse buildFailure(String providerType, String modelName, String endpoint,
                                                          long start, String errorMessage) {
        long latencyMs = System.currentTimeMillis() - start;
        String safeMessage = trimText(errorMessage == null || errorMessage.isBlank() ? "未知错误" : errorMessage, 300);
        log.warn("AI 引擎连通测试失败, provider: {}, model: {}, endpoint: {}, error: {}",
                providerType, modelName, endpoint, safeMessage);
        return AiEngineConnectivityTestResponse.builder()
                .success(false)
                .message("连通测试失败，请检查基础地址、模型名、API Key 和网络可达性。")
                .providerType(providerType)
                .modelName(modelName)
                .endpoint(endpoint)
                .latencyMs(latencyMs)
                .errorMessage(safeMessage)
                .build();
    }

    private AiEngineConnectivityTestResponse buildInvalidBaseUrlFailure(String providerType, String modelName,
                                                                        String configuredBaseUrl,
                                                                        String validationError) {
        String safeBaseUrl = trimText(configuredBaseUrl, 120);
        String safeMessage = "基础地址不合法：" + validationError;
        log.warn("AI 引擎连通测试拦截非法 baseUrl, provider: {}, model: {}, baseUrl: {}, error: {}",
                providerType, modelName, safeBaseUrl, safeMessage);
        return AiEngineConnectivityTestResponse.builder()
                .success(false)
                .message("基础地址不合法，连通测试未发起外部请求。")
                .providerType(providerType)
                .modelName(modelName)
                .endpoint(safeBaseUrl)
                .latencyMs(0L)
                .errorMessage(trimText(safeMessage, 300))
                .build();
    }

    /**
     * 创建带超时设置的 RestClient。
     */
    protected RestClient createRestClient(String baseUrl, int timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return restClientBuilder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 提取上游响应摘要，避免管理端展示完整模型输出。
     */
    private String extractResponsePreview(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new BusinessException("AI returned an empty response");
        }
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode choiceNode = root.path("choices").path(0);
            if (choiceNode.isMissingNode()) {
                throw new BusinessException("AI returned an empty response");
            }

            JsonNode messageNode = choiceNode.path("message");
            String content = messageNode.path("content").asText("");
            if (content != null && !content.isBlank()) {
                return trimText(content.trim(), 80);
            }

            String reasoningContent = messageNode.path("reasoning_content").asText("");
            if (reasoningContent != null && !reasoningContent.isBlank()) {
                return trimText(reasoningContent.trim(), 80);
            }

            String finishReason = choiceNode.path("finish_reason").asText("");
            if (messageNode.isObject() || finishReason != null && !finishReason.isBlank()) {
                return "Upstream returned a valid response without text content";
            }
            throw new BusinessException("AI returned an empty response");
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("AI response parse failed: " + ex.getMessage());
        }
    }

    private int normalizeTimeout(Integer timeoutMs) {
        if (timeoutMs == null) {
            return 30000;
        }
        return Math.max(MIN_TIMEOUT_MS, Math.min(MAX_TIMEOUT_MS, timeoutMs));
    }

    private String normalizeRequiredValue(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String trimText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
