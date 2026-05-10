package com.airesume.server.service;

import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.entity.SysAiEngineConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Locale;

/**
 * 轻量级 AI 调用客户端
 *
 * 所属模块：AI 基础设施层
 * 职责：封装非流式 AI 调用逻辑（RestClient 构建、配置解析、请求/响应序列化）
 * 用途：供 InterviewContextCompressor（对话摘要）等需要轻量 AI 调用的组件使用
 * 注意：InterviewAiServiceImpl 有独立的非流式调用逻辑（含 thinking 模式支持），不使用本类
 *
 * 【设计原则】
 * - 无 @ConditionalOnProperty，始终可用（纯基础设施，不涉及 mock/real 切换）
 * - 三级配置解析：DB sys_ai_engine_config → YAML → 环境变量
 * - 自定义超时支持：摘要等轻量任务可使用更短超时
 *
 * @author AI Resume Team
 */
@Service
@Slf4j
public class AiChatClient {

    private final SysAiEngineConfigService sysAiEngineConfigService;
    private final ObjectMapper objectMapper;

    /** YAML 注入的默认 AI 提供商 */
    private final String defaultProvider;
    /** YAML 注入的默认 baseUrl */
    private final String configuredBaseUrl;
    /** YAML 注入的默认模型名 */
    private final String defaultModel;
    /** YAML 注入的默认思考模式 */
    private final String defaultThinkingMode;

    /** 默认非流式超时时间（毫秒） */
    private static final int DEFAULT_READ_TIMEOUT = 180_000;
    /** 超时下限（毫秒），摘要等轻量任务允许更短超时 */
    private static final int MIN_READ_TIMEOUT = 10_000;
    /** 超时上限（毫秒） */
    private static final int MAX_READ_TIMEOUT = 300_000;
    /** 连接超时（毫秒） */
    private static final int CONNECT_TIMEOUT = 10_000;

    public AiChatClient(
            @Value("${app.interview.provider:doubao}") String provider,
            @Value("${app.interview.base-url:}") String configuredBaseUrl,
            @Value("${app.interview.model:}") String model,
            @Value("${app.interview.thinking-mode:none}") String thinkingMode,
            SysAiEngineConfigService sysAiEngineConfigService,
            ObjectMapper objectMapper) {
        this.defaultProvider = provider == null ? "doubao" : provider.toLowerCase();
        this.configuredBaseUrl = configuredBaseUrl;
        this.defaultModel = model;
        this.defaultThinkingMode = thinkingMode;
        this.sysAiEngineConfigService = sysAiEngineConfigService;
        this.objectMapper = objectMapper;
        log.info("[AiChatClient] 初始化完成, 默认 provider={}, model={}", this.defaultProvider, this.defaultModel);
    }

    /**
     * 非流式 AI 调用（默认超时 180 秒）
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return AI 回复文本
     */
    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, DEFAULT_READ_TIMEOUT);
    }

    /**
     * 非流式 AI 调用（自定义超时，用于摘要等轻量任务）
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @param timeoutMs    读取超时时间（毫秒）
     * @return AI 回复文本
     */
    public String chat(String systemPrompt, String userPrompt, int timeoutMs) {
        List<Message> messages = List.of(
                new Message("system", systemPrompt),
                new Message("user", userPrompt)
        );
        return chatWithMessages(messages, timeoutMs);
    }

    /**
     * 非流式多轮对话 AI 调用（自定义超时）
     *
     * @param messages  完整的消息列表（含 system、user、assistant 角色）
     * @param timeoutMs 读取超时时间（毫秒）
     * @return AI 回复文本
     */
    public String chatWithMessages(List<Message> messages, int timeoutMs) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig();
        String tag = runtimeConfig.provider().toUpperCase();
        String apiKey = runtimeConfig.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未找到可用的 AI 密钥，请检查管理端激活配置或环境变量");
        }

        RequestBody request = new RequestBody();
        request.model = runtimeConfig.model();
        request.messages = messages;
        // 摘要场景不使用 thinking 模式，节省 token
        request.thinking = null;

        try {
            log.info("[{}] AiChatClient 调用: model={}, timeout={}ms", tag, runtimeConfig.model(), timeoutMs);

            // 【关键修复】使用 WebClient 替代 RestClient，WebClient 底层 Reactor Netty 自动处理 gzip 解压，
            // 彻底解决 SiliconFlow 等供应商返回 application/octet-stream + gzip 导致反序列化失败的问题。
            int readTimeout = Math.max(Math.min(timeoutMs, MAX_READ_TIMEOUT), MIN_READ_TIMEOUT);

            WebClient webClient = WebClient.builder()
                    .baseUrl(runtimeConfig.baseUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            // 以 String 接收原始响应，手动用 Jackson 解析，兼容任意 Content-Type
            String rawJson = webClient.post()
                    .uri(runtimeConfig.endpoint())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofMillis(readTimeout))
                    .block();

            if (rawJson == null || rawJson.isBlank()) {
                throw new RuntimeException("AI 返回内容为空");
            }

            ResponseBody response = objectMapper.readValue(rawJson, ResponseBody.class);

            if (response == null || response.choices == null || response.choices.isEmpty()) {
                throw new RuntimeException("AI 返回内容为空");
            }

            String result = response.choices.get(0).message.content;
            log.info("[{}] AiChatClient 调用成功, responseLength={}", tag, result == null ? 0 : result.length());
            return result != null ? result.trim() : "";

        } catch (Exception e) {
            log.error("[{}] AiChatClient 调用失败: {}", tag, e.getMessage());
            throw new RuntimeException("AI 调用失败: " + e.getMessage(), e);
        }
    }

    // ==================== 配置解析（三级回退） ====================

    /**
     * 解析运行时 AI 配置（三级回退：DB → YAML → 环境变量）
     *
     * @return 运行时配置记录
     */
    private RuntimeAiConfig resolveRuntimeConfig() {
        // 第一级：YAML 默认值
        String runtimeProvider = defaultProvider;
        String runtimeModel = defaultModel;
        String runtimeBaseUrl = resolveBaseUrl(runtimeProvider, configuredBaseUrl);
        String runtimeApiKey = getApiKey();
        String source = "application";
        Integer runtimeTimeoutMs = null;
        String runtimeThinkingMode = defaultThinkingMode;

        // 第二级：DB 激活配置覆盖
        SysAiEngineConfig activeConfig = null;
        try {
            activeConfig = sysAiEngineConfigService.getActiveByBusinessType(AiEngineConstants.BUSINESS_TYPE_INTERVIEW);
        } catch (Exception e) {
            log.warn("[AiChatClient] 读取 DB 激活 AI 配置失败，回退本地配置: {}", e.getMessage());
        }

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

        // 第三级：兜底检查
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
                runtimeProvider, runtimeModel, runtimeBaseUrl,
                getEndpointByProvider(runtimeProvider),
                runtimeApiKey, source, runtimeTimeoutMs, runtimeThinkingMode
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
        if (key != null && !key.isBlank()) return key;
        key = System.getenv("API_KEY");
        if (key != null && !key.isBlank()) return key;
        key = System.getenv("AI_API_KEY");
        if (key != null && !key.isBlank()) return key;
        return null;
    }

    private String getEndpointByProvider(String providerType) {
        // 所有支持的厂商均兼容 OpenAI /chat/completions 端点
        return "/chat/completions";
    }

    private String normalizeConfigValue(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // ==================== 内部数据类 ====================

    /** 运行时 AI 配置记录 */
    private record RuntimeAiConfig(
            String provider, String model, String baseUrl,
            String endpoint, String apiKey, String source,
            Integer timeoutMs, String thinkingMode) {}

    /** OpenAI 兼容请求体 */
    static class RequestBody {
        public String model;
        public List<Message> messages;
        public Object thinking; // 可为 null，摘要场景不使用
    }

    /** OpenAI 兼容消息 */
    public static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    /** OpenAI 兼容响应体 */
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
