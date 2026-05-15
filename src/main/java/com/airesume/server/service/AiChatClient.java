package com.airesume.server.service;

import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.entity.SysAiEngineConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * 轻量级 AI 调用客户端。
 * 主要供面试上下文摘要等轻量任务复用。
 */
@Service
@Slf4j
public class AiChatClient {

    private static final String AI_SUMMARY_BREAKER = "ai-summary";
    private static final int DEFAULT_READ_TIMEOUT = 180_000;
    private static final int MIN_READ_TIMEOUT = 10_000;
    private static final int MAX_READ_TIMEOUT = 300_000;

    private final String defaultProvider;
    private final String configuredBaseUrl;
    private final String defaultModel;
    private final String defaultThinkingMode;
    private final SysAiEngineConfigService sysAiEngineConfigService;
    private final ObjectMapper objectMapper;
    private final AiCircuitBreaker aiCircuitBreaker;

    public AiChatClient(
            @Value("${app.interview.provider:doubao}") String provider,
            @Value("${app.interview.base-url:}") String configuredBaseUrl,
            @Value("${app.interview.model:}") String model,
            @Value("${app.interview.thinking-mode:none}") String thinkingMode,
            SysAiEngineConfigService sysAiEngineConfigService,
            ObjectMapper objectMapper,
            AiCircuitBreaker aiCircuitBreaker) {
        this.defaultProvider = provider == null ? "doubao" : provider.toLowerCase(Locale.ROOT);
        this.configuredBaseUrl = configuredBaseUrl;
        this.defaultModel = model;
        this.defaultThinkingMode = thinkingMode;
        this.sysAiEngineConfigService = sysAiEngineConfigService;
        this.objectMapper = objectMapper;
        this.aiCircuitBreaker = aiCircuitBreaker;
        log.info("[AiChatClient] 初始化完成, 默认 provider={}, model={}", this.defaultProvider, this.defaultModel);
    }

    /**
     * 非流式 AI 调用，使用默认超时。
     */
    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, DEFAULT_READ_TIMEOUT);
    }

    /**
     * 非流式 AI 调用，允许调用方自定义超时。
     */
    public String chat(String systemPrompt, String userPrompt, int timeoutMs) {
        return chatWithMessages(List.of(
                new Message("system", systemPrompt),
                new Message("user", userPrompt)
        ), timeoutMs);
    }

    /**
     * 多轮消息版非流式 AI 调用。
     */
    public String chatWithMessages(List<Message> messages, int timeoutMs) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig();
        String tag = runtimeConfig.provider().toUpperCase(Locale.ROOT);
        String apiKey = runtimeConfig.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未找到可用的 AI 密钥，请检查管理端激活配置或环境变量");
        }

        RequestBody request = new RequestBody();
        request.model = runtimeConfig.model();
        request.messages = messages;
        request.thinking = null;

        try {
            return aiCircuitBreaker.execute(AI_SUMMARY_BREAKER, () -> {
                log.info("[{}] AiChatClient 调用: model={}, timeout={}ms", tag, runtimeConfig.model(), timeoutMs);

                // 摘要链路也要走熔断，避免上游连续故障时每次都把超时跑满。
                int readTimeout = Math.max(Math.min(timeoutMs, MAX_READ_TIMEOUT), MIN_READ_TIMEOUT);
                WebClient webClient = WebClient.builder()
                        .baseUrl(runtimeConfig.baseUrl())
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .build();

                String rawJson = webClient.post()
                        .uri(runtimeConfig.endpoint())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofMillis(readTimeout))
                        .block();

                if (rawJson == null || rawJson.isBlank()) {
                    throw new RuntimeException("AI 返回内容为空");
                }

                ResponseBody response;
                try {
                    response = objectMapper.readValue(rawJson, ResponseBody.class);
                } catch (Exception parseEx) {
                    throw new RuntimeException("AI 响应解析失败", parseEx);
                }
                if (response == null || response.choices == null || response.choices.isEmpty()) {
                    throw new RuntimeException("AI 返回内容为空");
                }

                String result = response.choices.get(0).message.content;
                log.info("[{}] AiChatClient 调用成功, responseLength={}", tag, result == null ? 0 : result.length());
                return result == null ? "" : result.trim();
            });
        } catch (Exception e) {
            log.error("[{}] AiChatClient 调用失败", tag, e);
            throw new RuntimeException("AI 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析运行时 AI 配置。
     * 顺序：数据库激活配置 -> YAML -> 环境变量兜底。
     */
    private RuntimeAiConfig resolveRuntimeConfig() {
        String runtimeProvider = defaultProvider;
        String runtimeModel = defaultModel;
        String runtimeBaseUrl = resolveBaseUrl(runtimeProvider, configuredBaseUrl);
        String runtimeApiKey = getApiKey();
        String source = "application";
        Integer runtimeTimeoutMs = null;
        String runtimeThinkingMode = defaultThinkingMode;

        try {
            SysAiEngineConfig activeConfig =
                    sysAiEngineConfigService.getActiveByBusinessType(AiEngineConstants.BUSINESS_TYPE_INTERVIEW);
            if (activeConfig != null) {
                String dbProvider = normalizeConfigValue(activeConfig.getProviderType());
                if (dbProvider != null) {
                    runtimeProvider = dbProvider.toLowerCase(Locale.ROOT);
                }
                String dbModel = normalizeConfigValue(activeConfig.getModelName());
                if (dbModel != null) {
                    runtimeModel = dbModel;
                }
                String dbBaseUrl = normalizeConfigValue(activeConfig.getBaseUrl());
                runtimeBaseUrl = resolveBaseUrl(runtimeProvider, dbBaseUrl != null ? dbBaseUrl : configuredBaseUrl);
                String dbApiKey = normalizeConfigValue(activeConfig.getApiKey());
                if (dbApiKey != null) {
                    runtimeApiKey = dbApiKey;
                }
                runtimeTimeoutMs = activeConfig.getTimeoutMs();
                String dbThinkingMode = normalizeConfigValue(activeConfig.getThinkingMode());
                if (dbThinkingMode != null) {
                    runtimeThinkingMode = dbThinkingMode;
                }
                source = "db-active:" + activeConfig.getEngineCode();
            }
        } catch (Exception e) {
            log.warn("[AiChatClient] 读取 DB 激活 AI 配置失败，回退本地配置", e);
        }

        if (runtimeModel == null) {
            runtimeModel = defaultModel;
        }
        if (runtimeBaseUrl == null) {
            runtimeBaseUrl = resolveBaseUrl(runtimeProvider, configuredBaseUrl);
        }
        if (runtimeApiKey == null || runtimeApiKey.isBlank()) {
            runtimeApiKey = getApiKey();
        }
        if (runtimeApiKey == null || runtimeApiKey.isBlank()) {
            throw new IllegalStateException("AI 密钥不可用：数据库和环境变量均无有效配置");
        }

        return new RuntimeAiConfig(
                runtimeProvider,
                runtimeModel,
                runtimeBaseUrl,
                getEndpointByProvider(runtimeProvider),
                runtimeApiKey,
                source,
                runtimeTimeoutMs,
                runtimeThinkingMode
        );
    }

    private String resolveBaseUrl(String provider, String configuredUrl) {
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            return configuredUrl;
        }
        return switch (provider) {
            case "doubao", "openai" -> "https://ark.cn-beijing.volces.com/api/v3";
            case "qwen" -> "https://dashscope.aliyuncs.com/compatible-mode/v3";
            case "ernie" -> "https://qianfan.baidubce.com/v2";
            case "deepseek" -> "https://api.deepseek.com";
            case "minimax" -> "https://api.minimax.chat/v2";
            default -> "https://ark.cn-beijing.volces.com/api/v3";
        };
    }

    private String getApiKey() {
        String key = System.getenv("DOUBAO_API_KEY");
        if (key != null && !key.isBlank()) {
            return key;
        }
        key = System.getenv("API_KEY");
        if (key != null && !key.isBlank()) {
            return key;
        }
        key = System.getenv("AI_API_KEY");
        if (key != null && !key.isBlank()) {
            return key;
        }
        return null;
    }

    private String getEndpointByProvider(String providerType) {
        return "/chat/completions";
    }

    private String normalizeConfigValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record RuntimeAiConfig(
            String provider,
            String model,
            String baseUrl,
            String endpoint,
            String apiKey,
            String source,
            Integer timeoutMs,
            String thinkingMode) {
    }

    static class RequestBody {
        public String model;
        public List<Message> messages;
        public Object thinking;
    }

    public static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    static class ResponseBody {
        public List<Choice> choices;

        static class Choice {
            public MessageContent message;

            static class MessageContent {
                public String content;
            }
        }
    }
}
