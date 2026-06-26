package com.airesume.server.service.impl;

import com.airesume.server.common.util.PublicHttpsUrlValidator;
import com.airesume.server.dto.ai.AiModelDiscoveryResponse;
import com.airesume.server.dto.ai.AiModelOption;
import com.airesume.server.service.AiModelDiscoveryService;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * OpenAI 兼容 /models 模型列表获取服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelDiscoveryServiceImpl implements AiModelDiscoveryService {

    private static final int MIN_TIMEOUT_MS = 1000;
    private static final int MAX_TIMEOUT_MS = 300000;
    private static final int DEFAULT_TIMEOUT_MS = 10000;
    private static final int MAX_MODEL_COUNT = 500;
    private static final String MODELS_ENDPOINT = "/models";

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public AiModelDiscoveryResponse fetchModels(String baseUrl, String apiKey, Integer timeoutMs, String providerType) {
        String normalizedProviderType = normalizeProviderType(providerType);
        if ("mock".equals(normalizedProviderType)) {
            return AiModelDiscoveryResponse.builder()
                    .success(true)
                    .message("Mock 引擎无需外部模型列表，已返回本地测试模型。")
                    .models(List.of(AiModelOption.builder().id("mock-model").name("mock-model").build()))
                    .endpoint("mock://local/models")
                    .latencyMs(0L)
                    .build();
        }

        String configuredBaseUrl = normalizeRequired(baseUrl, "基础地址不能为空");
        String normalizedBaseUrl;
        try {
            normalizedBaseUrl = PublicHttpsUrlValidator.validate(configuredBaseUrl, "基础地址不能为空");
        } catch (IllegalArgumentException e) {
            return buildInvalidBaseUrlFailure(configuredBaseUrl, e.getMessage());
        }
        String normalizedApiKey = normalizeRequired(apiKey, "API Key 不能为空");
        int normalizedTimeoutMs = normalizeTimeout(timeoutMs);
        String endpoint = normalizedBaseUrl + MODELS_ENDPOINT;

        long start = System.currentTimeMillis();
        try {
            RestClient restClient = createRestClient(normalizedBaseUrl, normalizedTimeoutMs);
            // 模型列表获取只读取 id 字段，API Key 仅放在 Authorization header 中，不写日志、不回显。
            String rawResponse = restClient.get()
                    .uri(MODELS_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + normalizedApiKey)
                    .retrieve()
                    .body(String.class);

            List<AiModelOption> models = parseModelOptions(rawResponse);
            long latencyMs = System.currentTimeMillis() - start;
            return AiModelDiscoveryResponse.builder()
                    .success(true)
                    .message("模型列表获取成功")
                    .models(models)
                    .endpoint(endpoint)
                    .latencyMs(latencyMs)
                    .build();
        } catch (RestClientResponseException ex) {
            // 上游网关经常返回 HTML 错误页，这里只按 HTTP 状态码生成用户可理解的提示，避免把原始响应体暴露给前端。
            return buildFailure(endpoint, start, buildHttpFailureMessage(ex));
        } catch (Exception ex) {
            return buildFailure(endpoint, start, ex.getMessage());
        }
    }

    /**
     * 解析 OpenAI 兼容 /models 响应中的 data[].id，并按返回顺序去重。
     */
    private List<AiModelOption> parseModelOptions(String rawResponse) throws Exception {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalArgumentException("模型列表响应为空");
        }
        JsonNode dataNode = objectMapper.readTree(rawResponse).path("data");
        if (!dataNode.isArray()) {
            throw new IllegalArgumentException("模型列表响应缺少 data 数组");
        }

        Set<String> modelIds = new LinkedHashSet<>();
        for (JsonNode item : dataNode) {
            String id = item.path("id").asText("").trim();
            if (!id.isEmpty()) {
                modelIds.add(id);
            }
            if (modelIds.size() >= MAX_MODEL_COUNT) {
                break;
            }
        }
        if (modelIds.isEmpty()) {
            throw new IllegalArgumentException("模型列表为空");
        }

        List<AiModelOption> models = new ArrayList<>();
        for (String id : modelIds) {
            models.add(AiModelOption.builder().id(id).name(id).build());
        }
        return models;
    }

    /**
     * 创建带超时和 JSON 头的 RestClient。
     */
    protected RestClient createRestClient(String baseUrl, int timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return restClientBuilder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private String buildHttpFailureMessage(RestClientResponseException ex) {
        int statusCode = ex.getStatusCode().value();
        if (statusCode == 401 || statusCode == 403) {
            return "上游返回 HTTP " + statusCode + "：API Key 无效或没有模型列表权限，请检查 API Key 后重试。";
        }
        if (statusCode == 404) {
            return "上游返回 HTTP 404：模型列表接口不存在，请检查 API 基础地址是否应以 /v1 结尾，或该服务商是否支持 /models；你仍可手动输入模型名。";
        }
        if (statusCode == 429) {
            return "上游返回 HTTP 429：请求过于频繁，请稍后再试；你仍可手动输入模型名。";
        }
        if (statusCode >= 500) {
            return "上游返回 HTTP " + statusCode + "：服务商模型列表服务暂时不可用，请稍后重试或手动输入模型名。";
        }
        return "上游返回 HTTP " + statusCode + "：模型列表获取失败，请检查 API 基础地址、API Key，或手动输入模型名。";
    }

    private AiModelDiscoveryResponse buildFailure(String endpoint, long start, String errorMessage) {
        long latencyMs = System.currentTimeMillis() - start;
        String safeMessage = trimText(errorMessage == null || errorMessage.isBlank() ? "未知错误" : errorMessage, 300);
        log.warn("AI 模型列表获取失败, endpoint: {}, error: {}", endpoint, safeMessage);
        return AiModelDiscoveryResponse.builder()
                .success(false)
                .message("模型列表获取失败，请检查基础地址、API Key 或手动输入模型名。")
                .models(List.of())
                .endpoint(endpoint)
                .latencyMs(latencyMs)
                .errorMessage(safeMessage)
                .build();
    }

    private AiModelDiscoveryResponse buildInvalidBaseUrlFailure(String configuredBaseUrl, String validationError) {
        String safeBaseUrl = trimText(configuredBaseUrl, 120);
        String safeMessage = "基础地址不合法：" + validationError;
        log.warn("AI 模型列表获取拦截非法 baseUrl, baseUrl: {}, error: {}", safeBaseUrl, safeMessage);
        return AiModelDiscoveryResponse.builder()
                .success(false)
                .message("基础地址不合法，模型列表获取未发起外部请求。")
                .models(List.of())
                .endpoint(safeBaseUrl)
                .latencyMs(0L)
                .errorMessage(trimText(safeMessage, 300))
                .build();
    }

    private int normalizeTimeout(Integer timeoutMs) {
        if (timeoutMs == null) {
            return DEFAULT_TIMEOUT_MS;
        }
        return Math.max(MIN_TIMEOUT_MS, Math.min(MAX_TIMEOUT_MS, timeoutMs));
    }

    private String normalizeProviderType(String providerType) {
        String normalized = providerType == null ? null : providerType.trim();
        return normalized == null || normalized.isEmpty() ? "openai" : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
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
