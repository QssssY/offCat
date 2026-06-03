 package com.airesume.server.service.impl;

import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.common.constants.InterviewConstants;
import com.airesume.server.common.constants.PromptConstants;
import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.common.util.PublicHttpsUrlValidator;
import com.airesume.server.config.AiTokenLimitConfig;
import com.airesume.server.dto.ai.ResolvedAiConfig;
import com.airesume.server.dto.interview.InterviewEvaluationReport;
import com.airesume.server.dto.interview.InterviewJobTargetContext;
import com.airesume.server.entity.SysAiEngineConfig;
import com.airesume.server.entity.SysPrompt;
import com.airesume.server.entity.MockInterviewJobTargetRecord;
import com.airesume.server.entity.ResumeDiagnosisTask;
import com.airesume.server.entity.InterviewSession;
import com.airesume.server.mock.MockInterviewService;
import com.airesume.server.mapper.MockInterviewJobTargetRecordMapper;
import com.airesume.server.mapper.ResumeDiagnosisTaskMapper;
import com.airesume.server.mapper.InterviewSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.airesume.server.service.AiCircuitBreaker;
import com.airesume.server.service.AiCredentialCrypto;
import com.airesume.server.service.InterviewAiService;
import com.airesume.server.service.InterviewContextCompressor;
import com.airesume.server.service.SysAiEngineConfigService;
import com.airesume.server.service.SysPromptService;
import com.airesume.server.service.UserAiConfigResolver;
import com.airesume.server.util.AiInputCompressor;
import com.airesume.server.util.TokenEstimator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service("interviewAiService")
@Slf4j
@ConditionalOnProperty(name = "app.interview.mode", havingValue = "real")
public class InterviewAiServiceImpl implements InterviewAiService {

    private static final String INTERVIEW_AI_BREAKER = "interview-ai";
    private static final String INTERVIEW_STREAM_BREAKER = "interview-ai-stream";
    private static final Duration STREAMING_RESPONSE_TIMEOUT = Duration.ofSeconds(180);

    private final RestClient restClient;
    private final WebClient webClient;
    private final RestClient.Builder restClientBuilder;
    private final WebClient.Builder webClientBuilder;
    private final String provider;
    private final String model;
    private final String configuredBaseUrl;
    private final String resolvedBaseUrl;
    private final String endpoint;
    private final ObjectMapper objectMapper;
    private final String thinkingMode;
    private final SysAiEngineConfigService sysAiEngineConfigService;
    private final SysPromptService sysPromptService;
    private final InterviewContextCompressor contextCompressor;
    private final AiTokenLimitConfig tokenLimitConfig;
    private final MockInterviewService mockInterviewService;
    private final MockInterviewJobTargetRecordMapper mockInterviewJobTargetRecordMapper;
    private final ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper;
    private final InterviewSessionMapper interviewSessionMapper;
    private final AiCircuitBreaker aiCircuitBreaker;
    private final AiCredentialCrypto aiCredentialCrypto;
    private final UserAiConfigResolver userAiConfigResolver;

    @Value("${app.interview.stream-debug-log:false}")
    private boolean streamDebugLog;

    public InterviewAiServiceImpl(
            @Value("${app.interview.provider:doubao}") String provider,
            @Value("${app.interview.base-url:}") String configuredBaseUrl,
            @Value("${app.interview.model:}") String model,
            @Value("${app.interview.thinking-mode:none}") String thinkingMode,
            WebClient.Builder webClientBuilder,
            RestClient.Builder restClientBuilder,
            SysAiEngineConfigService sysAiEngineConfigService,
            SysPromptService sysPromptService,
            InterviewContextCompressor contextCompressor,
            AiTokenLimitConfig tokenLimitConfig,
            MockInterviewService mockInterviewService,
            MockInterviewJobTargetRecordMapper mockInterviewJobTargetRecordMapper,
            ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper,
            InterviewSessionMapper interviewSessionMapper,
            ObjectMapper objectMapper,
            AiCircuitBreaker aiCircuitBreaker,
            AiCredentialCrypto aiCredentialCrypto) {
        this(provider, configuredBaseUrl, model, thinkingMode, webClientBuilder, restClientBuilder,
                sysAiEngineConfigService, sysPromptService, contextCompressor, tokenLimitConfig,
                mockInterviewService, mockInterviewJobTargetRecordMapper, resumeDiagnosisTaskMapper,
                interviewSessionMapper, objectMapper, aiCircuitBreaker, aiCredentialCrypto, null);
    }

    @Autowired
    public InterviewAiServiceImpl(
            @Value("${app.interview.provider:doubao}") String provider,
            @Value("${app.interview.base-url:}") String configuredBaseUrl,
            @Value("${app.interview.model:}") String model,
            @Value("${app.interview.thinking-mode:none}") String thinkingMode,
            WebClient.Builder webClientBuilder,
            RestClient.Builder restClientBuilder,
            SysAiEngineConfigService sysAiEngineConfigService,
            SysPromptService sysPromptService,
            InterviewContextCompressor contextCompressor,
            AiTokenLimitConfig tokenLimitConfig,
            MockInterviewService mockInterviewService,
            MockInterviewJobTargetRecordMapper mockInterviewJobTargetRecordMapper,
            ResumeDiagnosisTaskMapper resumeDiagnosisTaskMapper,
            InterviewSessionMapper interviewSessionMapper,
            ObjectMapper objectMapper,
            AiCircuitBreaker aiCircuitBreaker,
            AiCredentialCrypto aiCredentialCrypto,
            @Autowired(required = false) UserAiConfigResolver userAiConfigResolver) {
        this.provider = provider == null ? "doubao" : provider.toLowerCase();
        this.model = model;
        this.configuredBaseUrl = configuredBaseUrl;
        this.thinkingMode = thinkingMode;
        this.objectMapper = objectMapper;
        this.restClientBuilder = restClientBuilder;
        this.webClientBuilder = webClientBuilder;
        this.sysAiEngineConfigService = sysAiEngineConfigService;
        this.sysPromptService = sysPromptService;
        this.contextCompressor = contextCompressor;
        this.tokenLimitConfig = tokenLimitConfig;
        this.mockInterviewService = mockInterviewService;
        this.mockInterviewJobTargetRecordMapper = mockInterviewJobTargetRecordMapper;
        this.resumeDiagnosisTaskMapper = resumeDiagnosisTaskMapper;
        this.interviewSessionMapper = interviewSessionMapper;
        this.aiCircuitBreaker = aiCircuitBreaker;
        this.aiCredentialCrypto = aiCredentialCrypto;
        this.userAiConfigResolver = userAiConfigResolver;
        this.resolvedBaseUrl = resolveBaseUrlForStartup(this.provider, configuredBaseUrl);
        this.endpoint = getEndpoint();
        this.restClient = restClientBuilder
                .baseUrl(this.resolvedBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        // 使用 JDK DNS 解析器，避免 Netty 异步解析器在 Windows 上无法解析部分域名
        HttpClient httpClient = HttpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE)
                .responseTimeout(streamingResponseTimeout());
        this.webClient = WebClient.builder()
                .baseUrl(this.resolvedBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        String tag = platformLogTag(this.provider);
        log.info("============================================================");
        log.info("[{}] 模拟面试 AI 服务初始化", tag);
        log.info("============================================================");
        log.info("[{}] 配置的 baseUrl: {}", tag, configuredBaseUrl);
        log.info("[{}] 最终使用的 resolvedBaseUrl: {}", tag, this.resolvedBaseUrl);
        log.info("[{}] endpoint: {}", tag, this.endpoint);
        log.info("[{}] 完整请求地址: {}{}", tag, this.resolvedBaseUrl, this.endpoint);
        log.info("[{}] model: {}", tag, this.model);
        log.info("[{}] thinking-mode: {}", tag, this.thinkingMode);
        log.info("============================================================");
    }

    private boolean supportsThinking(String modelName) {
        if (modelName == null) return false;
        String lowerModel = modelName.toLowerCase();
        return lowerModel.contains("doubao-seed-2.0");
    }

    private Thinking buildThinkingConfig(String modelName, String thinkingModeConfig, String tag) {
        boolean modelSupportsThinking = supportsThinking(modelName);
        if ("none".equalsIgnoreCase(thinkingModeConfig)) {
            return null;
        }
        if (!modelSupportsThinking) {
            log.warn("[{}] 当前模型 {} 不支持 thinking 参数，已忽略配置: {}",
                    tag, modelName, thinkingModeConfig);
            return null;
        }
        if ("enabled".equalsIgnoreCase(thinkingModeConfig)) {
            return new Thinking("enabled");
        } else if ("disabled".equalsIgnoreCase(thinkingModeConfig)) {
            return new Thinking("disabled");
        }
        log.warn("[{}] 未知的 thinking-mode 配置: {}, 使用 none",
                tag, thinkingModeConfig);
        return null;
    }

    private String resolveBaseUrl(String provider, String configuredUrl) {
        return resolveBaseUrl(provider, configuredUrl, true);
    }

    private String resolveBaseUrlForStartup(String provider, String configuredUrl) {
        return resolveBaseUrl(provider, configuredUrl, false);
    }

    private String resolveBaseUrl(String provider, String configuredUrl, boolean resolveDns) {
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            // 启动期不能依赖外部 DNS；真正发起 AI 调用前仍会执行完整 DNS 校验，防止 SSRF。
            String normalizedUrl = resolveDns
                    ? PublicHttpsUrlValidator.validate(configuredUrl, "基础地址不能为空")
                    : PublicHttpsUrlValidator.validateWithoutDnsResolution(configuredUrl, "基础地址不能为空");
            log.debug("使用用户配置的 baseUrl: {}", normalizedUrl);
            return normalizedUrl;
        }
        log.debug("用户未配置 baseUrl，使用默认值");
        return switch (provider) {
            case "doubao", "openai" -> "https://ark.cn-beijing.volces.com/api/v3";
            case "qwen" -> "https://dashscope.aliyuncs.com/compatible-mode/v3";
            case "ernie" -> "https://qianfan.baidubce.com/v2";
            case "deepseek" -> "https://api.deepseek.com";
            case "minimax" -> "https://api.minimax.chat/v2";
            case "mimo" -> "https://token-plan-cn.xiaomimimo.com/v1";
            default -> throw new IllegalArgumentException("未知的 AI 服务商: " + provider + "，请在管理端配置 base_url");
        };
    }

    private String getApiKey() {
        String key = System.getenv("DOUBAO_API_KEY");
        if (key != null && !key.isBlank()) {
            log.debug("[DEBUG] getApiKey: 读取到 DOUBAO_API_KEY, 长度={}", key.length());
            return key;
        }
        key = System.getenv("API_KEY");
        if (key != null && !key.isBlank()) {
            log.debug("[DEBUG] getApiKey: 读取到 API_KEY, 长度={}", key.length());
            return key;
        }
        key = System.getenv("AI_API_KEY");
        if (key != null && !key.isBlank()) {
            log.debug("[DEBUG] getApiKey: 读取到 AI_API_KEY, 长度={}", key.length());
            return key;
        }
        log.debug("[DEBUG] getApiKey: 未读取到任何 API_KEY");
        return null;
    }

    private String getEndpoint() {
        return switch (provider) {
            case "ernie" -> "/chat/completions";
            default -> "/chat/completions";
        };
    }

    @Override
    public String generateOpening(String jobRole, String jobRoleCode, Integer difficulty,
                                  InterviewJobTargetContext jobTargetContext) {
        log.info("生成面试开场白(硬编码模板), jobRole: {}, jobRoleCode: {}, difficulty: {}",
                jobRole, jobRoleCode, difficulty);
        String difficultyDesc = InterviewConstants.getDifficultyLabel(difficulty == null ? 2 : difficulty);
        boolean hasResume = hasResumeContext(jobTargetContext);
        String resumeHint = hasResume ? "我已经看过你的简历，" : "";
        return String.format(InterviewConstants.OPENING_TEMPLATE,
                difficultyDesc, jobRole != null ? jobRole : "软件工程师", resumeHint);
    }

    @Override
    public String generateReply(String sessionId, List<ChatMessageItem> history, String userMessage,
                                 String jobRoleCode, String jobRole, Integer difficulty,
                                 InterviewJobTargetContext jobTargetContext,
                                 String feedbackMode,
                                 String interviewMode,
                                 Integer interactionType) {
        return generateReply(sessionId, history, userMessage, jobRoleCode, jobRole, difficulty,
                jobTargetContext, feedbackMode, interviewMode, interactionType, null, false);
    }

    @Override
    public String generateReply(String sessionId, List<ChatMessageItem> history, String userMessage,
                                 String jobRoleCode, String jobRole, Integer difficulty,
                                 InterviewJobTargetContext jobTargetContext,
                                 String feedbackMode,
                                 String interviewMode,
                                 Integer interactionType,
                                 Long userId,
                                 boolean fallbackToPlatform) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig(userId, fallbackToPlatform);
        String tag = runtimeLogTag(runtimeConfig);
        logRuntimeRoute(tag, runtimeConfig, "interview-reply");

        // 如果没有jobTargetContext，尝试获取最近的简历信息
        if (jobTargetContext == null && sessionId != null && !sessionId.isBlank()) {
            jobTargetContext = getLatestResumeContext(sessionId);
            if (jobTargetContext != null) {
                log.info("[{}] 普通面试自动携带简历信息, sessionId: {}, resumeTaskId: {}",
                        tag, sessionId, jobTargetContext.getResumeTaskId());
            }
        }

        List<ChatMessageItem> compressedHistory = compressHistoryIfEnabled(history, tag, userId, fallbackToPlatform);
        String resolvedInterviewMode = resolveInterviewMode(sessionId, interviewMode);

        log.info("[{}] 生成面试官回复, sessionId: {}, historySize: {}, compressedSize: {}, userMessageLength: {}, jobRoleCode: {}, difficulty: {}, mode: {}, feedbackMode: {}",
                tag, sessionId, history == null ? 0 : history.size(),
                compressedHistory == null ? 0 : compressedHistory.size(),
                userMessage == null ? 0 : userMessage.length(), jobRoleCode, difficulty, resolvedInterviewMode, feedbackMode);

        String currentJobRole = resolveCurrentJobRole(sessionId, history, jobRoleCode, jobRole);
        List<Message> messages = buildConversationMessages(
                compressedHistory,
                userMessage,
                currentJobRole,
                jobRoleCode,
                difficulty,
                resolvedInterviewMode,
                jobTargetContext,
                feedbackMode,
                interactionType,
                tag
        );

        try {
            return chatWithMessages(messages, runtimeConfig);
        } catch (Exception e) {
            if (UserAiConstants.BILLING_SOURCE_USER_CUSTOM.equals(runtimeConfig.source())) {
                throwCustomAiFailure(runtimeConfig, e);
            }
            if (shouldFallbackToLocalMock(e)) {
                // 网络异常/Key 缺失等基础设施故障会回落到本地 Mock，必须用 ERROR 日志显式告警，
                // 让运维能在真 AI 不可用时第一时间感知。
                log.error("[{}] 真 AI 调用失败，本次回复将回落到本地 Mock, sessionId: {}, reason: {}",
                        tag, sessionId, e.getMessage());
                return buildReplyFallback(tag, sessionId, userMessage, compressedHistory, jobTargetContext, feedbackMode, resolvedInterviewMode, e);
            }
            log.error("[{}] 生成回复失败, sessionId: {}", tag, sessionId, e);
            throw new RuntimeException("AI 面试回复生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Publisher<String> generateReplyStream(String sessionId, List<ChatMessageItem> history, String userMessage,
                                                   String jobRoleCode, String jobRole, Integer difficulty,
                                                   InterviewJobTargetContext jobTargetContext,
                                                   String feedbackMode,
                                                   String interviewMode,
                                                   Integer interactionType) {
        return generateReplyStream(sessionId, history, userMessage, jobRoleCode, jobRole, difficulty,
                jobTargetContext, feedbackMode, interviewMode, interactionType, null, false);
    }

    @Override
    public Publisher<String> generateReplyStream(String sessionId, List<ChatMessageItem> history, String userMessage,
                                                   String jobRoleCode, String jobRole, Integer difficulty,
                                                   InterviewJobTargetContext jobTargetContext,
                                                   String feedbackMode,
                                                   String interviewMode,
                                                   Integer interactionType,
                                 Long userId,
                                 boolean fallbackToPlatform) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig(userId, fallbackToPlatform);
        String tag = runtimeLogTag(runtimeConfig);
        logRuntimeRoute(tag, runtimeConfig, "interview-reply-stream");

        // 如果没有jobTargetContext，尝试获取最近的简历信息
        if (jobTargetContext == null && sessionId != null && !sessionId.isBlank()) {
            jobTargetContext = getLatestResumeContext(sessionId);
            if (jobTargetContext != null) {
                log.info("[{}] 普通面试流式自动携带简历信息, sessionId: {}, resumeTaskId: {}",
                        tag, sessionId, jobTargetContext.getResumeTaskId());
            }
        }

        List<ChatMessageItem> compressedHistory = compressHistoryIfEnabled(history, tag, userId, fallbackToPlatform);
        String resolvedInterviewMode = resolveInterviewMode(sessionId, interviewMode);

        log.info("[{}] 流式生成面试官回复, sessionId: {}, historySize: {}, compressedSize: {}, jobRoleCode: {}, difficulty: {}, mode: {}, feedbackMode: {}",
                tag, sessionId, history == null ? 0 : history.size(),
                compressedHistory == null ? 0 : compressedHistory.size(), jobRoleCode, difficulty, resolvedInterviewMode, feedbackMode);

        String currentJobRole = resolveCurrentJobRole(sessionId, history, jobRoleCode, jobRole);
        List<Message> messages = buildConversationMessages(
                compressedHistory,
                userMessage,
                currentJobRole,
                jobRoleCode,
                difficulty,
                resolvedInterviewMode,
                jobTargetContext,
                feedbackMode,
                interactionType,
                tag
        );

        String apiKey = runtimeConfig.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            if (UserAiConstants.BILLING_SOURCE_USER_CUSTOM.equals(runtimeConfig.source())) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "用户自定义 AI API Key 不可用");
            }
            // 真 AI 模式下 API Key 缺失会静默回落到本地 Mock 字符流，必须用 ERROR 日志显式告警，
            // 防止运维不知情地把生产长期跑在 Mock 上。
            log.error("[{}] 真 AI 模式下 API Key 缺失，本次流式回复将回落到本地 Mock。请检查管理端激活的 AI 引擎配置或环境变量 DOUBAO_API_KEY/API_KEY/AI_API_KEY",
                    tag);
            return buildReplyFallbackStream(
                    tag,
                    sessionId,
                    userMessage,
                    compressedHistory,
                    jobTargetContext,
                    feedbackMode,
                    resolvedInterviewMode,
                    new IllegalStateException("未找到可用的面试 AI 密钥")
            );
        }

        log.info("[{}] 流式请求地址: {}{}, model: {}, source: {}, configType: {}",
                tag, runtimeConfig.baseUrl(), runtimeConfig.endpoint(), runtimeConfig.model(),
                runtimeConfig.source(), runtimeConfig.configType());

        StreamRequestBody reqBody = new StreamRequestBody(runtimeConfig.model(), messages, true);
        reqBody.thinking = buildThinkingConfig(runtimeConfig.model(), runtimeConfig.thinkingMode(), tag);

        try {
            String requestJson = objectMapper.writeValueAsString(reqBody);
            log.debug("[{}] 请求体JSON: {}", tag, requestJson);
        } catch (Exception e) {
            log.warn("[{}] 请求体序列化失败", tag, e);
        }

        if (log.isDebugEnabled()) {
            log.debug("[{}] ═══════════════════════════════════════════════", tag);
            log.debug("[{}] ║  流式请求参数验证  ║", tag);
            log.debug("[{}] ═══════════════════════════════════════════════", tag);
            log.debug("[{}] 请求地址: {}{}", tag, runtimeConfig.baseUrl(), runtimeConfig.endpoint());
            log.debug("[{}] model: {}", tag, runtimeConfig.model());
            log.debug("[{}] stream: {}", tag, reqBody.stream);
            if (reqBody.thinking != null) {
                log.debug("[{}] thinking.type: {}", tag, reqBody.thinking.type);
            } else {
                log.debug("[{}] thinking: 未设置", tag);
            }
            log.debug("[{}] ═══════════════════════════════════════════════", tag);
        }

        WebClient runtimeWebClient = webClientBuilder
                .baseUrl(runtimeConfig.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .resolver(DefaultAddressResolverGroup.INSTANCE)
                                .responseTimeout(streamingResponseTimeout())))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Flux<String> rawLineFlux = aiCircuitBreaker.executeFlux(
                INTERVIEW_STREAM_BREAKER,
                () -> runtimeWebClient.post()
                        .uri(runtimeConfig.endpoint())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .bodyValue(reqBody)
                        .retrieve()
                        .bodyToFlux(String.class)
                        .publishOn(Schedulers.boundedElastic()));

        AtomicInteger totalLines = new AtomicInteger(0);
        AtomicInteger parsedJsonLines = new AtomicInteger(0);
        AtomicInteger contentChunkCount = new AtomicInteger(0);
        AtomicInteger reasoningChunkCount = new AtomicInteger(0);
        AtomicInteger emittedCount = new AtomicInteger(0);
        AtomicInteger parseErrorCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);
        AtomicBoolean firstContentArrived = new AtomicBoolean(false);

        final String logTag = tag;
        final InterviewJobTargetContext finalJobTargetContext = jobTargetContext;
        final List<ChatMessageItem> finalCompressedHistory = compressedHistory;
        final String finalFeedbackMode = feedbackMode;
        final String finalInterviewMode = resolvedInterviewMode;
        Flux<String> contentFlux = Flux.create(sink -> {
            rawLineFlux.subscribe(new Subscriber<String>() {
                private Subscription upstream;

                @Override
                public void onSubscribe(Subscription s) {
                    this.upstream = s;
                    s.request(1);
                }

                @Override
                public void onNext(String rawLine) {
                    try {
                        int lineNo = totalLines.incrementAndGet();

                        String normalizedJson = null;
                        boolean isComment = false;
                        boolean isDone = false;
                        boolean isDataPrefix = false;
                        boolean isPureJson = false;

                        if (rawLine == null || rawLine.isBlank()) {
                            if (streamDebugLog) log.debug("[{}] 【跳过-空白行】lineNo={}", logTag, lineNo);
                            skippedCount.incrementAndGet();
                            upstream.request(1);
                            return;
                        }

                        if (rawLine.startsWith(":")) {
                            isComment = true;
                            if (streamDebugLog) log.debug("[{}] 【跳过-注释行】lineNo={}", logTag, lineNo);
                            skippedCount.incrementAndGet();
                            upstream.request(1);
                            return;
                        }

                        if ("[DONE]".equals(rawLine.trim())) {
                            isDone = true;
                            if (streamDebugLog) log.debug("[{}] 【收到DONE】lineNo={}", logTag, lineNo);
                            upstream.request(1);
                            return;
                        }

                        if (rawLine.startsWith("data:")) {
                            isDataPrefix = true;
                            String json = rawLine.substring("data:".length()).trim();
                            if ("[DONE]".equals(json)) {
                                isDone = true;
                                if (streamDebugLog) log.debug("[{}] 【收到DONE（data:前缀）】lineNo={}", logTag, lineNo);
                                upstream.request(1);
                                return;
                            }
                            normalizedJson = json;
                        } else if (rawLine.trim().startsWith("{")) {
                            isPureJson = true;
                            normalizedJson = rawLine.trim();
                        } else {
                            if (streamDebugLog) log.debug("[{}] 【跳过-未知格式】lineNo={}, preview={}", logTag, lineNo,
                                        rawLine.length() > 80 ? rawLine.substring(0, 80) : rawLine);
                            skippedCount.incrementAndGet();
                            upstream.request(1);
                            return;
                        }

                        JsonNode root;
                        try {
                            root = objectMapper.readTree(normalizedJson);
                            parsedJsonLines.incrementAndGet();
                        } catch (Exception e) {
                            int errCount = parseErrorCount.incrementAndGet();
                            log.warn("[{}] 【JSON解析失败】lineNo={}, error={}, 累计错误={}",
                                    logTag, lineNo, e.getMessage(), errCount);
                            upstream.request(1);
                            return;
                        }

                        JsonNode choices = root.path("choices");
                        if (choices.isMissingNode() || !choices.isArray() || choices.isEmpty()) {
                            if (streamDebugLog) log.debug("[{}] 【跳过-无choices】lineNo={}", logTag, lineNo);
                            skippedCount.incrementAndGet();
                            upstream.request(1);
                            return;
                        }

                        JsonNode delta = choices.get(0).path("delta");
                        if (delta.isMissingNode()) {
                            if (streamDebugLog) log.debug("[{}] 【跳过-无delta】lineNo={}", logTag, lineNo);
                            skippedCount.incrementAndGet();
                            upstream.request(1);
                            return;
                        }

                        JsonNode contentNode = delta.path("content");
                        JsonNode reasoningNode = delta.path("reasoning_content");

                        boolean hasContent = contentNode.isTextual() && !contentNode.asText().isBlank();
                        boolean hasReasoning = reasoningNode.isTextual() && !reasoningNode.asText().isBlank();

                        if (hasContent) contentChunkCount.incrementAndGet();
                        if (hasReasoning) reasoningChunkCount.incrementAndGet();

                        if (hasReasoning && streamDebugLog) {
                            String reasoningText = reasoningNode.asText();
                            log.info("[{}] 【reasoning_content】lineNo={}, format={}, length={}, preview={}, 【不发给前端】",
                                    logTag,
                                    lineNo,
                                    isDataPrefix ? "SSE" : (isPureJson ? "纯JSON" : "其他"),
                                    reasoningText.length(),
                                    reasoningText.length() > 50 ? reasoningText.substring(0, 50) : reasoningText);
                        }

                        if (hasContent) {
                            String contentText = contentNode.asText();
                            if (firstContentArrived.compareAndSet(false, true)) {
                                log.info("[{}] 【首个content到达】lineNo={}, preview={}", logTag, lineNo, contentText);
                            }
                            if (streamDebugLog) {
                                log.info("[{}] 【◆发射】lineNo={}, format={}, content={}, length={}",
                                        logTag,
                                        lineNo,
                                        isDataPrefix ? "SSE" : (isPureJson ? "纯JSON" : "其他"),
                                        contentText,
                                        contentText.length());
                            }
                            sink.next(contentText);
                            emittedCount.incrementAndGet();
                        }

                        if (streamDebugLog) {
                            log.info("[{}] 【行汇总】lineNo={}, format={}, hasContent={}, hasReasoning={}, emitted={}",
                                    logTag,
                                    lineNo,
                                    isDataPrefix ? "SSE" : (isPureJson ? "纯JSON" : "其他"),
                                    hasContent, hasReasoning, hasContent);
                        }

                        upstream.request(1);

                    } catch (Exception e) {
                        log.error("[{}] 流式响应处理异常, errorType={}", logTag, e.getClass().getSimpleName(), e);
                        sink.error(e);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    log.error("[{}] 流式 WebClient 调用失败, errorType={}",
                            logTag, t.getClass().getSimpleName(), t);
                    if (UserAiConstants.BILLING_SOURCE_USER_CUSTOM.equals(runtimeConfig.source())) {
                        sink.error(new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED,
                                "自定义AI调用失败: " + (t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage())));
                        return;
                    }
                    // 外部 AI 在 DNS / 网络层失败时，直接降级到本地 Mock，保证模拟面试不中断。
                    if (emittedCount.get() == 0) {
                        emitFallbackReplyToSink(
                                sink,
                                buildReplyFallback(logTag, sessionId, userMessage, finalCompressedHistory, finalJobTargetContext, finalFeedbackMode, finalInterviewMode, t)
                        );
                        return;
                    }
                    // 如果已经输出了部分内容，追加中断标记后结束流，避免前端只收到半句话
                    log.warn("[{}] 流式回复中途网络异常，已存在部分输出，追加中断标记后结束流, sessionId: {}",
                            logTag, sessionId, t);
                    sink.next("…[网络中断，请重试]");
                    sink.complete();
                }

                @Override
                public void onComplete() {
                    int total = totalLines.get();
                    int parsed = parsedJsonLines.get();
                    int cCount = contentChunkCount.get();
                    int rCount = reasoningChunkCount.get();
                    int emitted = emittedCount.get();
                    int errors = parseErrorCount.get();
                    int skipped = skippedCount.get();

                    String conclusion = makeConclusion(cCount, rCount, errors, parsed, total);

                    logStreamCompletionReport(runtimeConfig.model(), total, parsed, skipped, errors,
                            cCount, rCount, emitted, conclusion);

                    sink.complete();
                }
            });
        });
        return contentFlux;
    }

    /**
     * 流式 WebClient 响应超时。该值大于前端 SSE 120 秒兜底，避免底层连接无限挂起。
     */
    Duration streamingResponseTimeout() {
        return STREAMING_RESPONSE_TIMEOUT;
    }

    /**
     * 流式最终统计报告只在 DEBUG 输出，避免生产 INFO 日志被高频盒绘报告刷屏。
     */
    void logStreamCompletionReport(String model,
                                   int total,
                                   int parsed,
                                   int skipped,
                                   int errors,
                                   int contentChunkCount,
                                   int reasoningChunkCount,
                                   int emitted,
                                   String conclusion) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug("");
        log.debug("╔══════════════════════════════════════════════════════════════╗");
        log.debug("║          【流式处理完成-最终统计报告 V3】                   ║");
        log.debug("╠══════════════════════════════════════════════════════════════╣");
        log.debug("║  模型: {}", model);
        log.debug("║  总接收行数:              {}", total);
        log.debug("║  成功解析JSON的行数:     {}", parsed);
        log.debug("║  跳过的行数:             {}", skipped);
        log.debug("║  JSON解析失败次数:       {}", errors);
        log.debug("║  delta.content 非空chunk: {}", contentChunkCount);
        log.debug("║  reasoning_content 非空:  {}", reasoningChunkCount);
        log.debug("║  实际发给下游次数:       {}", emitted);
        log.debug("║  结论: {}", conclusion);
        log.debug("╚══════════════════════════════════════════════════════════════╝");
        log.debug("");
    }

    /**
     * 真实 AI 开场白生成失败时，降级到本地 Mock 开场问题，避免因为外部网络异常导致无法创建面试会话。
     */
    private String buildOpeningFallback(String tag, String jobRole, Integer difficulty,
                                        InterviewJobTargetContext jobTargetContext, Throwable cause) {
        log.warn("[{}] 真实 AI 开场生成失败，改用本地 Mock 兜底, cause={}", tag, cause.getMessage(), cause);
        return mockInterviewService.generateMockOpening(resolveFallbackJobRole(jobRole), difficulty, jobTargetContext);
    }

    /**
     * 真实 AI 多轮问答失败时，复用本地 Mock 生成一个自然的单问题追问，保证面试对话可继续。
     */
    private String buildReplyFallback(String tag, String sessionId, String userMessage,
                                      List<ChatMessageItem> history,
                                      InterviewJobTargetContext jobTargetContext, String feedbackMode,
                                      String interviewMode, Throwable cause) {
        log.warn("[{}] 真实 AI 面试回复失败，改用本地 Mock 兜底, sessionId: {}, cause={}",
                tag, sessionId, cause.getMessage(), cause);
        int messageCount = history == null ? 0 : history.size();
        String fallbackReply = mockInterviewService.generateMockReply(sessionId, userMessage, messageCount, jobTargetContext);
        return applyStructuredImmediateFeedbackToMockReply(applyPersonaToMockReply(fallbackReply, interviewMode), feedbackMode);
    }

    /**
     * 流式问答在请求发出前就发现配置缺失时，直接返回本地 Mock 字符流，避免 SSE 进入错误事件。
     */
    private Publisher<String> buildReplyFallbackStream(String tag, String sessionId, String userMessage,
                                                       List<ChatMessageItem> history,
                                                       InterviewJobTargetContext jobTargetContext, String feedbackMode,
                                                       String interviewMode, Throwable cause) {
        String fallbackReply = buildReplyFallback(tag, sessionId, userMessage, history, jobTargetContext, feedbackMode, interviewMode, cause);
        return Flux.<String>create(sink -> emitFallbackReplyToSink(sink, fallbackReply))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 真实 AI 失败后的本地兜底也要保留人设语气，避免流式故障时体验突然退回普通面试。
     */
    private String applyPersonaToMockReply(String reply, String interviewMode) {
        if (InterviewConstants.MODE_BIG_COMPANY_HR.equalsIgnoreCase(interviewMode)) {
            return "从 HR 视角看，我想继续了解你的行为经历和团队协作方式。"
                    + (reply == null ? "" : "\n\n" + reply);
        }
        if (InterviewConstants.MODE_TECH_LEADER.equalsIgnoreCase(interviewMode)) {
            return "从技术 Leader 视角看，我会继续追问你的技术取舍和个人贡献。"
                    + (reply == null ? "" : "\n\n" + reply);
        }
        if (InterviewConstants.MODE_FOREIGN_INTERVIEWER.equalsIgnoreCase(interviewMode)) {
            return "Let's continue in English. I would like to understand your reasoning and impact more clearly."
                    + (reply == null ? "" : "\n\n" + reply);
        }
        return reply;
    }

    private String applyStructuredImmediateFeedbackToMockReply(String reply, String feedbackMode) {
        if (!InterviewConstants.FEEDBACK_MODE_IMMEDIATE.equalsIgnoreCase(feedbackMode)) {
            return reply;
        }
        String safeReply = reply == null ? "" : reply.trim();
        if (safeReply.contains("<FEEDBACK>")) {
            return safeReply;
        }
        return safeReply + "\n\n<FEEDBACK>\n本题反馈：你的回答有基本方向，但还需要补充更具体的背景、个人动作和结果，避免只用一句话带过。\n</FEEDBACK>";
    }

    private void emitFallbackReplyToSink(FluxSink<String> sink, String fallbackReply) {
        if (fallbackReply == null || fallbackReply.isBlank()) {
            sink.complete();
            return;
        }
        for (int i = 0; i < fallbackReply.length(); i++) {
            if (sink.isCancelled()) {
                return;
            }
            sink.next(fallbackReply.substring(i, i + 1));
        }
        sink.complete();
    }

    /**
     * 判断是否应降级到本地 Mock。
     * 仅网络异常、超时、连接拒绝等基础设施问题才降级；业务异常（如参数错误）应直接抛出。
     */
    private boolean shouldFallbackToLocalMock(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        String name = throwable.getClass().getName();
        // 网络/连接/超时类异常允许降级
        return name.contains("ConnectException")
                || name.contains("SocketTimeoutException")
                || name.contains("ReadTimeoutException")
                || name.contains("HttpHostConnectException")
                || name.contains("WebClientRequestException")
                || name.contains("IOException")
                || name.contains("WebClientException")
                || (name.contains("IllegalStateException")
                    && throwable.getMessage() != null
                    && throwable.getMessage().contains("密钥")); // 仅 API Key 缺失时降级
    }

    /**
     * 兜底岗位名称只用于本地 Mock 开场问题，避免真实岗位缺失时退化成空字符串。
     */
    private String resolveFallbackJobRole(String jobRole) {
        if (jobRole == null || jobRole.isBlank()) {
            return "软件工程师";
        }
        return jobRole;
    }

    private String makeConclusion(int cNonEmpty, int rNonEmpty, int parseErrors, int parsed, int total) {
        if (parseErrors > 0 && cNonEmpty == 0 && rNonEmpty == 0) {
            return "FAIL - 大量解析失败，可能是响应格式不兼容或网络截断";
        }
        if (parsed == 0) {
            return "FAIL - 没有成功解析任何 JSON 行，可能是 API 返回格式异常";
        }
        if (cNonEmpty > 0) {
            return "SUCCESS - 检测到 " + cNonEmpty + " 个 delta.content chunk，应有内容输出到前端";
        }
        if (rNonEmpty > 0 && cNonEmpty == 0) {
            return "WARNING - 所有有效 chunk 只有 reasoning_content，无 delta.content。" +
                   "当前模型可能为 Reasoning 模型，不适合直接输出给用户。" +
                   "建议换用 standard/instruct 模型";
        }
        return "UNKNOWN - 请检查日志";
    }

    @Override
    @Deprecated
    public EvaluationResult generateEvaluation(String sessionId, List<ChatMessageItem> history) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig();
        String tag = runtimeLogTag(runtimeConfig);
        log.info("[{}] 调用旧版评价接口, sessionId: {}, historySize: {}",
                tag, sessionId, history == null ? 0 : history.size());
        InterviewEvaluationReport report = generateEvaluationReport(sessionId, history, "软件工程师", null, 2, "normal", null);
        try {
            String jsonReport = objectMapper.writeValueAsString(report);
            return new EvaluationResult(report.getOverallScore(), jsonReport);
        } catch (Exception e) {
            log.error("[{}] 序列化评价报告失败", tag, e);
            throw new RuntimeException("评价报告序列化失败", e);
        }
    }

    @Override
    public InterviewEvaluationReport generateEvaluationReport(
            String sessionId,
            List<ChatMessageItem> history,
            String jobRole,
            String jobRoleCode,
            Integer difficulty,
            String interviewMode,
            InterviewJobTargetContext jobTargetContext
    ) {
        return generateEvaluationReport(sessionId, history, jobRole, jobRoleCode, difficulty,
                interviewMode, jobTargetContext, null, false);
    }

    @Override
    public InterviewEvaluationReport generateEvaluationReport(
            String sessionId,
            List<ChatMessageItem> history,
            String jobRole,
            String jobRoleCode,
            Integer difficulty,
            String interviewMode,
            InterviewJobTargetContext jobTargetContext,
            Long userId,
            boolean fallbackToPlatform
    ) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig(userId, fallbackToPlatform);
        String tag = runtimeLogTag(runtimeConfig);
        logRuntimeRoute(tag, runtimeConfig, "interview-evaluation-report");
        // 优先读取流式阶段已缓存的摘要，避免重复调用 AI 摘要
        String cachedSummary = contextCompressor.getCachedSummary(sessionId);
        // 面试结束，清除摘要缓存
        contextCompressor.evictCache(sessionId);
        log.info("[{}] ═══════════════════════════════════════════════", tag);
        log.info("[{}] ║  开始生成 AI 面试评价报告  ║", tag);
        log.info("[{}] ═══════════════════════════════════════════════", tag);
        log.info("[{}] sessionId: {}, jobRole: {}, jobRoleCode: {}, difficulty: {}, mode: {}, historySize: {}",
                tag, sessionId, jobRole, jobRoleCode, difficulty, interviewMode,
                history == null ? 0 : history.size());

        // 使用 AI 压缩生成评价专用摘要（保留最近 N 条完整对话 + 早期对话的全局摘要），确保评价涵盖全流程
        List<ChatMessageItem> reportHistory = contextCompressor.compressForEvaluation(
                history, cachedSummary, userId, fallbackToPlatform);

        String systemPrompt = buildEvaluationSystemPrompt(jobRole, jobRoleCode, difficulty, interviewMode, jobTargetContext);
        String userPrompt = buildEvaluationUserPrompt(reportHistory, jobTargetContext);

        int systemTokens = TokenEstimator.estimateTokens(systemPrompt);
        int userTokens = TokenEstimator.estimateTokens(userPrompt);
        int totalTokens = systemTokens + userTokens;
        log.info("[{}] 评价报告预估token: {}(system:{}, user:{})", tag, totalTokens, systemTokens, userTokens);

        // 安全兜底：压缩后仍超限时，先改用更短结构化摘要，最后才做明确标记的保底截断。
        if (tokenLimitConfig.isTokenLimitEnabled() && totalTokens > tokenLimitConfig.getInterviewEvaluationMax()) {
            log.warn("[{}] 评价报告token预估({})超过限制({})，尝试使用短结构化摘要重建 prompt",
                    tag, totalTokens, tokenLimitConfig.getInterviewEvaluationMax());
            reportHistory = buildCompactEvaluationHistory(history, tokenLimitConfig.getEvaluationRecentMessagesToKeep());
            userPrompt = buildEvaluationUserPrompt(reportHistory, jobTargetContext);
            userTokens = TokenEstimator.estimateTokens(userPrompt);
            totalTokens = systemTokens + userTokens;
            log.info("[{}] 短结构化摘要后预估token: {}(system:{}, user:{})", tag, totalTokens, systemTokens, userTokens);

            if (totalTokens > tokenLimitConfig.getInterviewEvaluationMax()) {
                int reducedKeepRecent = Math.max(2, Math.min(4, tokenLimitConfig.getEvaluationRecentMessagesToKeep()));
                log.warn("[{}] 短结构化摘要仍超限({}>{})，减少最近消息保留数到{}后重建 prompt",
                        tag, totalTokens, tokenLimitConfig.getInterviewEvaluationMax(), reducedKeepRecent);
                reportHistory = buildCompactEvaluationHistory(history, reducedKeepRecent);
                userPrompt = buildEvaluationUserPrompt(reportHistory, jobTargetContext);
                userTokens = TokenEstimator.estimateTokens(userPrompt);
                totalTokens = systemTokens + userTokens;
                log.info("[{}] 减少最近消息后预估token: {}(system:{}, user:{})",
                        tag, totalTokens, systemTokens, userTokens);
            }

            if (totalTokens > tokenLimitConfig.getInterviewEvaluationMax()) {
                userPrompt = truncateEvaluationPromptAsLastResort(
                        tag, userPrompt, systemTokens, tokenLimitConfig.getInterviewEvaluationMax());
                userTokens = TokenEstimator.estimateTokens(userPrompt);
                totalTokens = systemTokens + userTokens;
                log.warn("[{}] 最终保底截断后预估token: {}(system:{}, user:{}), last_resort_truncate=true",
                        tag, totalTokens, systemTokens, userTokens);
            }
        }

        String aiResponse = chat(systemPrompt, userPrompt, runtimeConfig);
        log.info("[{}] AI 评价原始响应长度: {}", tag, aiResponse == null ? 0 : aiResponse.length());

        InterviewEvaluationReport report = parseEvaluationResponse(aiResponse, tag);
        calculateOverallScore(report, difficulty);
        mapLegacyFields(report);

        log.info("[{}] 评价报告生成完成, overallScore: {}, hireRecommendation: {}",
                tag, report.getOverallScore(), report.getHireRecommendation());

        return report;
    }

    private String buildEvaluationSystemPrompt(String jobRole, String jobRoleCode, Integer difficulty,
                                               String interviewMode, InterviewJobTargetContext jobTargetContext) {
        log.debug("评价 Prompt 使用硬编码兜底, jobRole: {}, difficulty: {}", jobRole, difficulty);
        return buildDefaultEvaluationSystemPrompt(jobRole, difficulty, interviewMode)
                + buildJobTargetEvaluationPrompt(jobTargetContext);
    }

    private String buildDefaultEvaluationSystemPrompt(String jobRole, Integer difficulty, String interviewMode) {
        String difficultyDesc = InterviewConstants.getDifficultyDescription(difficulty);
        String modeDesc = getEvaluationModeDescription(interviewMode);

        String prompt = """
                角色：大厂技术面试官(10年经验)。任务：严格评估候选人真实水平。
                背景：岗位=PLACEHOLDER1，难度=PLACEHOLDER2，模式=PLACEHOLDER3。
                原则：1)按一线大厂标准 2)实事求是不鼓励 3)直接尖锐指出问题 4)保守评分(60以下不录用) 5)不放水。
                评分：90-100=S(远超预期)，80-89=A(优秀)，70-79=B(达标)，60-69=C(勉强)，<60=D(淘汰)。
                录用：>=80强烈推荐，>=70推荐，>=60待定，<60不推荐。
                输出JSON(无额外文本)：{"overallScore":0-100,"level":"S/A/B/C/D","finalVerdict":"结论","summary":"500字左右自然连贯的综合评价","strengths":[""],"weaknesses":[""],"criticalIssues":[""],"questionPerformance":[{"question":"","answer":"","score":0,"comment":"","knowledgeTags":[""]}],"roundReviews":[{"roundNo":1,"question":"","answer":"","score":0,"replayAnalysis":"","missedFollowUp":"","nextPractice":""}],"followUpLossPoints":[""],"commonLossPatterns":[""],"immediateActions":["行动1","行动2","行动3"],"technicalDepth":{"score":0,"comment":"","strengths":["具体加分项"],"weaknesses":["具体扣分项"]},"projectExpression":{"score":0,"comment":"","strengths":["具体加分项"],"weaknesses":["具体扣分项"]},"communication":{"score":0,"comment":"","strengths":["具体加分项"],"weaknesses":["具体扣分项"]},"problemSolving":{"score":0,"comment":"","strengths":["具体加分项"],"weaknesses":["具体扣分项"]},"pressureResistance":{"score":0,"comment":"","strengths":["具体加分项"],"weaknesses":["具体扣分项"]},"jobMatch":{"score":0,"comment":"","strengths":["具体加分项"],"weaknesses":["具体扣分项"]},"hireRecommendation":"","improvementSuggestions":[""],"redFlags":[""],"missingCompetencies":[""],"inflationRisk":"","answerAuthenticity":"","interviewPerformanceTags":[""],"passProbability":0,"rejectionReasons":[""]}
                summary写作规范(500字左右，必须是一段或两段自然连贯的综合评价，严禁分点、编号、分阶段或分维度说明)：
                summary是整体结论性评价，需要综合概括候选人的整体表现、岗位匹配度、核心优势、主要不足、风险点和最终倾向，表达风格应接近真实面试官写出的面试结论。
                严禁出现以下内容：(1)"（1）""（2）""（3）"等编号；(2)"1.""2.""3."等数字列表；(3)"一、""二、""三、"等中文列表；(4)"胜任力匹配度：""行为事件真实性：""软技能与情景反应：""潜在风险与适配预警："等维度标题；(5)"技术深度：""沟通能力：""问题解决能力：""岗位匹配度："等评分维度标题；(6)任何类似提纲、清单、分析维度的表达。
                因为报告中已有strengths、weaknesses、suggestions、technicalDepth、problemSolving、communication、jobMatch、pressureResistance等结构化字段，summary不需要再次按维度展开，只负责输出整体结论。
                内容应涵盖：候选人整体表现如何、与目标岗位匹配度怎样、核心优势是什么、主要不足在哪里、有无风险点、最终倾向如何。有JD时对标岗位能力，有简历时可提简历与实际表现的一致性，压力面试可提抗压表现。所有信息融合在连贯的段落中，不拆分维度。
                questionPerformance筛选规则(不追求数量覆盖，追求每个展示项直指候选人本质)：按优先级筛选——(1)暴露致命伤的回答(直接否掉候选人的关键问题)；(2)高度矛盾的信号(简历/前面回答与现场表现冲突)；(3)高度证实性的高光(极好地证明某项核心能力)；(4)体现典型行为模式(虽非致命但能稳定反映思维/性格的样本)。每条comment必须80-120字，用具体证据和细节点评，指出该问答暴露的核心问题或亮点，不要泛泛而谈。数量硬性要求(必须严格遵守，不可少于下限)：1-2轮返回全部问答；3-4轮questionPerformance数组至少3个元素；5轮及以上questionPerformance数组至少5个元素最多15个元素。若按优先级筛选后数量不足下限，则降低优先级标准补齐数量；超过上限则保留优先级最高的。压力面试追加规则：压力面试中每一轮追问都算独立条目，候选人被追问后出现认知闭合、自我修正、情绪波动的瞬间必须单独展示，压力场景题/陷阱题的应对必须展示，压力面试questionPerformance最低数量不得少于对话中面试官提问的总轮次数(上限15)。有简历时额外检查回答与简历经历冲突如有则优先展示；无JD时弱化硬技能缺失标签强化逻辑自洽筛选；初级岗降低战略视野负面标签关注学习意愿和执行细节。
                V2深度分析规则：roundReviews必须按真实问答顺序回放至少3轮(不足3轮则返回全部)，每轮说明“这轮答得怎么样、追问哪里没接住、下一次怎么改”；followUpLossPoints只记录被追问后暴露的失分点；commonLossPatterns归纳2-5条反复出现的模式；immediateActions必须且只能给3条明天就能练的动作，每条要包含训练对象和完成标准；projectExpression重点评估项目讲述是否有背景、动作、结果和量化证据。
                规则：所有字段必填；level按overallScore自动判定；passProbability与overallScore一致；每个维度的strengths和weaknesses各列出1-3条具体表现，用中文描述；summary字段必须500字左右自然连贯的综合评价，严禁分点编号和维度标题；questionPerformance按上述筛选规则智能筛选且必须满足数量下限(5轮对话至少5条最多15条，压力面试中每轮追问都算独立条目不得合并)，comment必须80-120字具体深入点评。
                """;
        return prompt.replace("PLACEHOLDER1", jobRole)
                     .replace("PLACEHOLDER2", difficultyDesc)
                     .replace("PLACEHOLDER3", modeDesc);
    }

    /**
     * 评价报告里保留本次面试官人设，确保反馈口径和练习过程一致。
     */
    private String getEvaluationModeDescription(String interviewMode) {
        if (InterviewConstants.MODE_STRESS.equalsIgnoreCase(interviewMode)) {
            return "压力面试";
        }
        if (InterviewConstants.MODE_BIG_COMPANY_HR.equalsIgnoreCase(interviewMode)) {
            return "大厂 HR 面";
        }
        if (InterviewConstants.MODE_TECH_LEADER.equalsIgnoreCase(interviewMode)) {
            return "技术 Leader 面";
        }
        if (InterviewConstants.MODE_FOREIGN_INTERVIEWER.equalsIgnoreCase(interviewMode)) {
            return "外企面试官";
        }
        if (InterviewConstants.MODE_JOB_TARGETED.equalsIgnoreCase(interviewMode)) {
            return "岗位定向模拟";
        }
        return "普通面试";
    }

    private String buildEvaluationUserPrompt(List<ChatMessageItem> history, InterviewJobTargetContext jobTargetContext) {
        StringBuilder sb = new StringBuilder();
        if (jobTargetContext != null && Boolean.TRUE.equals(jobTargetContext.getJobTargeted())) {
            sb.append("以下为岗位定向补充上下文，请将其纳入评价：\n");
            sb.append(buildJobTargetContextSummary(jobTargetContext)).append("\n\n");
        }
        sb.append("以下是面试对话记录，请严格评估：\n\n");
        appendEffectiveEvaluationRounds(sb, history);

        List<ChatMessageItem> detailHistory = trimTrailingUnansweredInterviewerMessages(history);
        if (detailHistory != null && !detailHistory.isEmpty()) {
            for (ChatMessageItem item : detailHistory) {
                String role = "user".equalsIgnoreCase(item.role()) ? "候选人" : "面试官";
                sb.append("【").append(role).append("】\n");
                sb.append(item.content()).append("\n\n");
            }
        } else {
            sb.append("（暂无对话记录）\n");
        }

        sb.append("请输出 JSON 格式的评价报告。");
        return sb.toString();
    }

    /**
     * 报告 prompt 超限时使用更短的结构化摘要保留全局视角，再追加最近消息作为评分证据。
     */
    private List<ChatMessageItem> buildCompactEvaluationHistory(List<ChatMessageItem> history, int keepRecent) {
        if (history == null || history.isEmpty()) {
            return history;
        }
        List<ChatMessageItem> compactHistory = new ArrayList<>();
        compactHistory.add(new ChatMessageItem("system", buildLocalEvaluationLimitSummary(history)));

        int safeKeepRecent = Math.min(Math.max(keepRecent, 0), history.size());
        if (safeKeepRecent > 0) {
            compactHistory.addAll(history.subList(history.size() - safeKeepRecent, history.size()));
        }
        return compactHistory;
    }

    private String buildLocalEvaluationLimitSummary(List<ChatMessageItem> history) {
        StringBuilder summary = new StringBuilder();
        summary.append("[评估参考摘要 - token 降级]\n");
        summary.append("总消息数：").append(history.size()).append("\n");

        int assistantCount = 0;
        int userCount = 0;
        for (ChatMessageItem item : history) {
            if ("assistant".equalsIgnoreCase(item.role())) {
                assistantCount++;
            } else if ("user".equalsIgnoreCase(item.role())) {
                userCount++;
            }
        }
        summary.append("面试官消息数：").append(assistantCount).append("；候选人回答数：").append(userCount).append("\n");
        summary.append("核心问答摘录：\n");

        int roundNo = 0;
        int writtenRounds = 0;
        String pendingQuestion = null;
        for (ChatMessageItem item : history) {
            if ("assistant".equalsIgnoreCase(item.role())) {
                pendingQuestion = trimPromptText(item.content(), 90);
            } else if ("user".equalsIgnoreCase(item.role()) && pendingQuestion != null) {
                roundNo++;
                if (writtenRounds < 8) {
                    summary.append("- 第").append(roundNo).append("轮：问=")
                            .append(pendingQuestion)
                            .append("；答=")
                            .append(trimPromptText(item.content(), 120))
                            .append("\n");
                    writtenRounds++;
                }
                pendingQuestion = null;
            }
        }
        if (roundNo > writtenRounds) {
            summary.append("- 其余").append(roundNo - writtenRounds)
                    .append("轮已压缩，仅保留在最近消息中作为直接评分证据。\n");
        }
        if (pendingQuestion != null) {
            summary.append("- 尾部存在未回答追问：").append(pendingQuestion)
                    .append("，只作为上一轮追问上下文，不作为独立 0 分题。\n");
        }
        return trimPromptText(summary.toString(), 1200);
    }

    private String truncateEvaluationPromptAsLastResort(
            String tag,
            String userPrompt,
            int systemTokens,
            int maxTokens) {
        int maxUserTokens = Math.max(500, maxTokens - systemTokens);
        int maxChars = Math.max(1000, maxUserTokens * 2);
        if (userPrompt == null || userPrompt.length() <= maxChars) {
            return userPrompt;
        }
        log.warn("[{}] 评价报告 prompt 经过摘要压缩后仍超限，执行最终保底截断, maxChars={}, last_resort_truncate=true",
                tag, maxChars);
        return userPrompt.substring(0, maxChars)
                + "\n\n[系统提示：由于输入仍超出模型上下文，以上内容已执行最终保底截断，last_resort_truncate=true。]"
                + "\n请基于保留内容输出 JSON 格式的评价报告。";
    }

    /**
     * 报告详情里的原始对话只保留到最后一个候选人回答，避免把未回答的尾部追问当成 0 分题。
     */
    private List<ChatMessageItem> trimTrailingUnansweredInterviewerMessages(List<ChatMessageItem> history) {
        if (history == null || history.isEmpty()) {
            return history;
        }

        int lastUserIndex = -1;
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessageItem item = history.get(i);
            if (item != null && "user".equalsIgnoreCase(item.role())) {
                lastUserIndex = i;
                break;
            }
        }
        if (lastUserIndex < 0) {
            return List.of();
        }
        return new ArrayList<>(history.subList(0, lastUserIndex + 1));
    }

    /**
     * 将原始聊天记录压缩成“问题-回答-后续反馈/追问”轮次，防止报告生成时只看到松散消息而漏评候选人的有效回答。
     */
    private void appendEffectiveEvaluationRounds(StringBuilder sb, List<ChatMessageItem> history) {
        List<EvaluationRound> rounds = buildEffectiveEvaluationRounds(history);
        if (rounds.isEmpty()) {
            return;
        }

        sb.append("有效问答轮次总数：").append(rounds.size()).append("\n");
        sb.append("questionPerformance/roundReviews 必须优先覆盖以上有效问答轮次；若有效轮次不超过15轮，不得少于有效轮次总数；若超过15轮，保留最关键的15轮并在summary说明筛选依据。\n\n");
        sb.append("尾部未回答规则：后续反馈或追问只作为上一轮回答的上下文；如果某条面试官消息后面没有候选人回答，不得把它作为独立 questionPerformance/roundReviews 条目，也不得因该未回答追问给 0 分。\n\n");
        for (EvaluationRound round : rounds) {
            sb.append("【有效问答轮次").append(round.roundNo()).append("】\n");
            sb.append("问题：").append(round.question()).append("\n");
            sb.append("回答：").append(round.answer()).append("\n");
            if (round.followUp() != null && !round.followUp().isBlank()) {
                sb.append("后续反馈或追问：").append(round.followUp()).append("\n");
            }
            sb.append("\n");
        }
    }

    /**
     * 以候选人回答作为有效轮次锚点：最近一条面试官消息是问题，下一条面试官消息是反馈或追问。
     */
    private List<EvaluationRound> buildEffectiveEvaluationRounds(List<ChatMessageItem> history) {
        List<EvaluationRound> rounds = new ArrayList<>();
        if (history == null || history.isEmpty()) {
            return rounds;
        }

        String lastInterviewerMessage = null;
        for (int i = 0; i < history.size(); i++) {
            ChatMessageItem item = history.get(i);
            if (item == null || item.content() == null || item.content().isBlank()) {
                continue;
            }
            if (!"user".equalsIgnoreCase(item.role())) {
                lastInterviewerMessage = item.content().trim();
                continue;
            }
            if (lastInterviewerMessage == null || lastInterviewerMessage.isBlank()) {
                continue;
            }

            String followUp = null;
            for (int j = i + 1; j < history.size(); j++) {
                ChatMessageItem next = history.get(j);
                if (next == null || next.content() == null || next.content().isBlank()) {
                    continue;
                }
                if (!"user".equalsIgnoreCase(next.role())) {
                    followUp = next.content().trim();
                    break;
                }
            }
            rounds.add(new EvaluationRound(rounds.size() + 1, lastInterviewerMessage, item.content().trim(), followUp));
        }
        return rounds;
    }

    private record EvaluationRound(int roundNo, String question, String answer, String followUp) {
    }

    private InterviewEvaluationReport parseEvaluationResponse(String aiResponse, String tag) {
        if (aiResponse == null || aiResponse.isBlank()) {
            log.warn("[{}] AI 返回空响应，使用默认评价报告", tag);
            return buildDefaultEvaluationReport();
        }

        String jsonContent = extractJsonFromResponse(aiResponse);
        log.debug("[{}] 提取的 JSON 内容长度: {}", tag, jsonContent.length());

        try {
            InterviewEvaluationReport report = objectMapper.readValue(jsonContent, InterviewEvaluationReport.class);
            normalizeReportCollections(report);
            normalizeDimensionScores(report);
            log.info("[{}] 评价报告 JSON 解析成功", tag);
            return report;
        } catch (Exception e) {
            log.error("[{}] 评价报告 JSON 解析失败，终止本次 AI 报告写入", tag, e);
            log.debug("[{}] 解析失败的 JSON 内容: {}", tag, jsonContent);
            throw new IllegalStateException("AI 评价报告 JSON 解析失败", e);
        }
    }

    private String extractJsonFromResponse(String response) {
        int firstBrace = response.indexOf('{');
        int lastBrace = response.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return response.substring(firstBrace, lastBrace + 1);
        }
        return response;
    }

    private void normalizeDimensionScores(InterviewEvaluationReport report) {
        normalizeDimensionScore(report.getTechnicalDepth());
        normalizeDimensionScore(report.getProjectExpression());
        normalizeDimensionScore(report.getCommunication());
        normalizeDimensionScore(report.getProblemSolving());
        normalizeDimensionScore(report.getPressureResistance());
        normalizeDimensionScore(report.getJobMatch());
    }

    /**
     * 统一把 AI 报告中的 null 数组字段归一化为空列表，避免有效报告在兼容映射时被 NPE 打回默认报告。
     */
    private void normalizeReportCollections(InterviewEvaluationReport report) {
        if (report == null) return;
        if (report.getStrengths() == null) report.setStrengths(new ArrayList<>());
        if (report.getWeaknesses() == null) report.setWeaknesses(new ArrayList<>());
        if (report.getCriticalIssues() == null) report.setCriticalIssues(new ArrayList<>());
        if (report.getQuestionPerformance() == null) report.setQuestionPerformance(new ArrayList<>());
        if (report.getRoundReviews() == null) report.setRoundReviews(new ArrayList<>());
        if (report.getFollowUpLossPoints() == null) report.setFollowUpLossPoints(new ArrayList<>());
        if (report.getCommonLossPatterns() == null) report.setCommonLossPatterns(new ArrayList<>());
        if (report.getImmediateActions() == null) report.setImmediateActions(new ArrayList<>());
        if (report.getImprovementSuggestions() == null) report.setImprovementSuggestions(new ArrayList<>());
        if (report.getRedFlags() == null) report.setRedFlags(new ArrayList<>());
        if (report.getMissingCompetencies() == null) report.setMissingCompetencies(new ArrayList<>());
        if (report.getInterviewPerformanceTags() == null) report.setInterviewPerformanceTags(new ArrayList<>());
        if (report.getRejectionReasons() == null) report.setRejectionReasons(new ArrayList<>());
        if (report.getSuggestions() == null) report.setSuggestions(new ArrayList<>());
        if (report.getImprovements() == null) report.setImprovements(new ArrayList<>());
    }

    private void normalizeDimensionScore(InterviewEvaluationReport.DimensionScore ds) {
        if (ds == null) return;
        if (ds.getStrengths() == null) ds.setStrengths(new ArrayList<>());
        if (ds.getWeaknesses() == null) ds.setWeaknesses(new ArrayList<>());
    }

    private InterviewEvaluationReport buildDefaultEvaluationReport() {
        return InterviewEvaluationReport.builder()
                .overallScore(60)
                .level("C")
                .finalVerdict("评价生成失败，暂无结论")
                .summary("系统未能生成评价报告，请稍后重试或查看原始对话记录。")
                .strengths(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .criticalIssues(new ArrayList<>())
                .questionPerformance(new ArrayList<>())
                .roundReviews(new ArrayList<>())
                .followUpLossPoints(List.of("报告生成失败，暂未识别追问失分点"))
                .commonLossPatterns(List.of("报告生成失败，暂未归纳共性失分模式"))
                .immediateActions(List.of(
                        "回看原始面试记录，先标出答得最空泛的 1 个问题",
                        "选 1 个项目案例按 STAR 结构重写一版回答",
                        "针对最弱维度补录 3 分钟口述练习并检查是否包含具体证据"
                ))
                .technicalDepth(InterviewEvaluationReport.DimensionScore.builder().score(60).comment("暂无").build())
                .projectExpression(InterviewEvaluationReport.DimensionScore.builder().score(60).comment("暂无").build())
                .communication(InterviewEvaluationReport.DimensionScore.builder().score(60).comment("暂无").build())
                .problemSolving(InterviewEvaluationReport.DimensionScore.builder().score(60).comment("暂无").build())
                .pressureResistance(InterviewEvaluationReport.DimensionScore.builder().score(60).comment("暂无").build())
                .jobMatch(InterviewEvaluationReport.DimensionScore.builder().score(60).comment("暂无").build())
                .hireRecommendation("待定")
                .improvementSuggestions(new ArrayList<>())
                .redFlags(new ArrayList<>())
                .missingCompetencies(new ArrayList<>())
                .inflationRisk("暂无")
                .answerAuthenticity("暂无")
                .interviewPerformanceTags(new ArrayList<>())
                .passProbability(60)
                .rejectionReasons(new ArrayList<>())
                .build();
    }

    private void calculateOverallScore(InterviewEvaluationReport report, Integer difficulty) {
        Map<String, Double> weights = InterviewConstants.getDimensionWeights(difficulty);
        double weightedScore = 0;

        if (report.getTechnicalDepth() != null && report.getTechnicalDepth().getScore() != null)
            weightedScore += report.getTechnicalDepth().getScore() * weights.get("technicalDepth");
        if (report.getProjectExpression() != null && report.getProjectExpression().getScore() != null)
            weightedScore += report.getProjectExpression().getScore() * weights.get("projectExpression");
        if (report.getCommunication() != null && report.getCommunication().getScore() != null)
            weightedScore += report.getCommunication().getScore() * weights.get("communication");
        if (report.getProblemSolving() != null && report.getProblemSolving().getScore() != null)
            weightedScore += report.getProblemSolving().getScore() * weights.get("problemSolving");
        if (report.getPressureResistance() != null && report.getPressureResistance().getScore() != null)
            weightedScore += report.getPressureResistance().getScore() * weights.get("pressureResistance");
        if (report.getJobMatch() != null && report.getJobMatch().getScore() != null)
            weightedScore += report.getJobMatch().getScore() * weights.get("jobMatch");

        int finalScore = (int) Math.round(weightedScore);
        report.setOverallScore(finalScore);
        report.setPassProbability(finalScore);

        if (finalScore >= 90) report.setLevel("S");
        else if (finalScore >= 80) report.setLevel("A");
        else if (finalScore >= 70) report.setLevel("B");
        else if (finalScore >= 60) report.setLevel("C");
        else report.setLevel("D");
    }

    private void mapLegacyFields(InterviewEvaluationReport report) {
        normalizeReportCollections(report);
        com.fasterxml.jackson.databind.node.ObjectNode dimensions = objectMapper.createObjectNode();
        if (report.getTechnicalDepth() != null) {
            dimensions.put("technicalDepth", report.getTechnicalDepth().getScore());
        }
        if (report.getProjectExpression() != null) {
            dimensions.put("projectExpression", report.getProjectExpression().getScore());
        }
        if (report.getCommunication() != null) {
            dimensions.put("communication", report.getCommunication().getScore());
        }
        if (report.getProblemSolving() != null) {
            dimensions.put("problemSolving", report.getProblemSolving().getScore());
        }
        dimensions.put("systemDesign", report.getTechnicalDepth() != null ? report.getTechnicalDepth().getScore() : 60);
        report.setDimensions(dimensions);

        report.setSuggestions(new ArrayList<>(report.getImprovementSuggestions()));
        report.setImprovements(new ArrayList<>(report.getWeaknesses()));
    }

    private String chat(String systemPrompt, String userPrompt, RuntimeAiConfig runtimeConfig) {
        String tag = runtimeLogTag(runtimeConfig);
        String apiKey = runtimeConfig.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未找到可用的面试 AI 密钥，请检查管理端激活配置或环境变量");
        }

        RequestBody request = new RequestBody();
        request.model = runtimeConfig.model();
        request.messages = List.of(
                new Message("system", systemPrompt),
                new Message("user", userPrompt)
        );
        // 评价报告字段多且文本长，输出上限低会导致模型返回半截 JSON。
        request.max_tokens = 8192;
        request.thinking = buildThinkingConfig(runtimeConfig.model(), runtimeConfig.thinkingMode(), tag);

        try {
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            log.info("[{}] ║  非流式请求参数验证  ║", tag);
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            log.info("[{}] 请求地址: {}{}, source: {}, configType: {}",
                    tag, runtimeConfig.baseUrl(), runtimeConfig.endpoint(), runtimeConfig.source(), runtimeConfig.configType());
            log.info("[{}] model: {}", tag, runtimeConfig.model());
            if (request.thinking != null) {
                log.info("[{}] thinking.type: {}", tag, request.thinking.type);
            } else {
                log.info("[{}] thinking: 未设置", tag);
            }
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            RestClient.Builder builder = restClientBuilder
                    .baseUrl(runtimeConfig.baseUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            // 【超时保障】默认 3 分钟，DB 配置值范围 [120s, 300s]，确保大型思考模型有足够响应时间。
            int readTimeout = 180_000;
            if (runtimeConfig.timeoutMs() != null && runtimeConfig.timeoutMs() > 0) {
                readTimeout = Math.max(Math.min(runtimeConfig.timeoutMs(), 300_000), 120_000);
            }
            SimpleClientHttpRequestFactory customFactory = new SimpleClientHttpRequestFactory();
            customFactory.setConnectTimeout(10000);
            customFactory.setReadTimeout(readTimeout);
            builder = builder.requestFactory(customFactory);
            log.info("[{}] HTTP 超时: {}ms", tag, readTimeout);
            RestClient runtimeRestClient = builder.build();

            String responseText = aiCircuitBreaker.execute(
                    INTERVIEW_AI_BREAKER,
                    () -> runtimeRestClient.post()
                            .uri(runtimeConfig.endpoint())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                            .accept(MediaType.APPLICATION_JSON)
                            .body(request)
                            .retrieve()
                            .body(String.class));
            ResponseBody response = tryReadResponseBody(responseText);

            if (response == null || response.choices == null || response.choices.isEmpty()) {
                throw new RuntimeException("AI 返回内容为空");
            }

            String result = response.choices.get(0).message.content;
            log.info("[{}] AI 调用成功, responseLength: {}", tag, result == null ? 0 : result.length());
            return result != null ? result.trim() : "";

        } catch (Exception e) {
            log.error("[{}] AI 调用失败", tag, e);
            if (UserAiConstants.BILLING_SOURCE_USER_CUSTOM.equals(runtimeConfig.source())) {
                throwCustomAiFailure(runtimeConfig, e);
            }
            throw new RuntimeException("AI 面试回复生成失败: " + e.getMessage(), e);
        }
    }

    private String chatWithMessages(List<Message> messages) {
        return chatWithMessages(messages, resolveRuntimeConfig());
    }

    private String chatWithMessages(List<Message> messages, RuntimeAiConfig runtimeConfig) {
        String tag = runtimeLogTag(runtimeConfig);
        String apiKey = runtimeConfig.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未找到可用的面试 AI 密钥，请检查管理端激活配置或环境变量");
        }

        RequestBody request = new RequestBody();
        request.model = runtimeConfig.model();
        request.messages = messages;
        request.thinking = buildThinkingConfig(runtimeConfig.model(), runtimeConfig.thinkingMode(), tag);

        try {
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            log.info("[{}] ║  多轮对话请求参数验证  ║", tag);
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            log.info("[{}] 请求地址: {}{}, source: {}, configType: {}",
                    tag, runtimeConfig.baseUrl(), runtimeConfig.endpoint(), runtimeConfig.source(), runtimeConfig.configType());
            log.info("[{}] model: {}", tag, runtimeConfig.model());
            if (request.thinking != null) {
                log.info("[{}] thinking.type: {}", tag, request.thinking.type);
            } else {
                log.info("[{}] thinking: 未设置", tag);
            }
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            RestClient.Builder builder = restClientBuilder
                    .baseUrl(runtimeConfig.baseUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            // 【超时保障】默认 3 分钟，DB 配置值范围 [120s, 300s]，确保大型思考模型有足够响应时间。
            int readTimeout = 180_000;
            if (runtimeConfig.timeoutMs() != null && runtimeConfig.timeoutMs() > 0) {
                readTimeout = Math.max(Math.min(runtimeConfig.timeoutMs(), 300_000), 120_000);
            }
            SimpleClientHttpRequestFactory customFactory = new SimpleClientHttpRequestFactory();
            customFactory.setConnectTimeout(10000);
            customFactory.setReadTimeout(readTimeout);
            builder = builder.requestFactory(customFactory);
            log.info("[{}] HTTP 超时: {}ms", tag, readTimeout);
            RestClient runtimeRestClient = builder.build();

            String responseText = aiCircuitBreaker.execute(
                    INTERVIEW_AI_BREAKER,
                    () -> runtimeRestClient.post()
                            .uri(runtimeConfig.endpoint())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                            .accept(MediaType.APPLICATION_JSON)
                            .body(request)
                            .retrieve()
                            .body(String.class));
            ResponseBody response = tryReadResponseBody(responseText);

            if (response == null || response.choices == null || response.choices.isEmpty()) {
                throw new RuntimeException("AI 返回内容为空");
            }

            String result = response.choices.get(0).message.content;
            log.info("[{}] AI 调用成功, responseLength: {}", tag, result == null ? 0 : result.length());
            return result != null ? result.trim() : "";

        } catch (Exception e) {
            log.error("[{}] AI 调用失败", tag, e);
            if (UserAiConstants.BILLING_SOURCE_USER_CUSTOM.equals(runtimeConfig.source())) {
                throwCustomAiFailure(runtimeConfig, e);
            }
            throw new RuntimeException("AI 面试回复生成失败: " + e.getMessage(), e);
        }
    }

    private List<Message> buildConversationMessages(List<ChatMessageItem> history, String currentUserMessage, String jobRole,
                                                    String jobRoleCode, Integer difficulty, String interviewMode,
                                                    InterviewJobTargetContext jobTargetContext, String feedbackMode,
                                                    Integer interactionType, String tag) {
        java.util.List<Message> messages = new java.util.ArrayList<>();

        String systemPrompt = buildSystemPromptFromJobRole(history, jobRole, jobRoleCode, difficulty, interviewMode, jobTargetContext, feedbackMode, interactionType);
        messages.add(new Message("system", systemPrompt));

        int historyUserCount = 0;
        int historyAssistantCount = 0;

        if (history != null && !history.isEmpty()) {
            for (ChatMessageItem item : history) {
                String role = item.role();
                String content = item.content();
                if (content == null || content.isBlank()) {
                    continue;
                }
                String mappedRole = "user".equalsIgnoreCase(role) ? "user" : "assistant";
                messages.add(new Message(mappedRole, content));
                if ("user".equalsIgnoreCase(mappedRole)) {
                    historyUserCount++;
                } else {
                    historyAssistantCount++;
                }
            }
        }

        // 防止前端重试导致重复 user 消息：history 中已包含上次持久化的 user 消息，
        // 而 currentUserMessage 仍被显式传入，需去重
        boolean userMessageAppended = false;
        if (currentUserMessage != null && !currentUserMessage.isBlank()) {
            boolean alreadyInHistory = !messages.isEmpty()
                    && "user".equals(messages.get(messages.size() - 1).role)
                    && currentUserMessage.trim().equals(messages.get(messages.size() - 1).content.trim());
            if (!alreadyInHistory) {
                messages.add(new Message("user", currentUserMessage));
                userMessageAppended = true;
            }
        }

        int totalMessages = messages.size();
        int totalUserCount = historyUserCount + (userMessageAppended ? 1 : 0);
        String firstRole = totalMessages > 0 ? messages.get(0).role : "none";
        String lastRole = totalMessages > 0 ? messages.get(totalMessages - 1).role : "none";
        log.info("[{}] ═══════════════════════════════════════════════", tag);
        log.info("[{}] ║  对话消息组装完成  ║", tag);
        log.info("[{}] ═══════════════════════════════════════════════", tag);
        log.info("[{}] 总消息数: {} (system:1, user:{}, assistant:{}), feedbackMode: {}, feedbackInstructionIncluded: {}",
                tag, totalMessages, totalUserCount, historyAssistantCount,
                feedbackMode, InterviewConstants.FEEDBACK_MODE_IMMEDIATE.equalsIgnoreCase(feedbackMode));
        log.info("[{}] 首条消息角色: {}, 末条消息角色: {}", tag, firstRole, lastRole);
        log.info("[{}] 是否包含历史: {} (历史user数:{}, 历史assistant数:{})",
                tag, historyUserCount + historyAssistantCount > 0, historyUserCount, historyAssistantCount);
        log.info("[{}] ═══════════════════════════════════════════════", tag);

        return messages;
    }

    /**
     * 优先从当前会话读取真实岗位名称。
     * 说明：多轮对话阶段如果只依赖历史消息推断岗位，容易退化成"软件工程师"这种泛化岗位。
     */
    private String resolveCurrentJobRole(String sessionId, List<ChatMessageItem> history,
                                          String jobRoleCode, String providedJobRole) {
        // 优先使用调用方传入的 jobRole（从预加载 session 获取）
        if (providedJobRole != null && !providedJobRole.isBlank()) {
            return providedJobRole;
        }
        String mappedJobRole = mapJobRoleCodeToName(jobRoleCode);
        if (!mappedJobRole.isBlank()) {
            return mappedJobRole;
        }
        // 兜底：从 DB 查询（兼容其他调用方未传 jobRole 的场景）
        if (sessionId != null && !sessionId.isBlank()) {
            try {
                InterviewSession session = interviewSessionMapper.selectOne(
                        new LambdaQueryWrapper<InterviewSession>()
                                .eq(InterviewSession::getSessionId, sessionId)
                                .last("limit 1")
                );
                if (session != null && session.getJobRole() != null && !session.getJobRole().isBlank()) {
                    return session.getJobRole();
                }
            } catch (Exception e) {
                log.warn("根据 sessionId 读取岗位名称失败, sessionId: {}", sessionId, e);
            }
        }
        if (history != null && !history.isEmpty()) {
            String first = history.get(0).content();
            if (first != null && first.contains("前端")) {
                return "前端开发工程师";
            }
        }
        return "软件工程师";
    }

    /**
     * 根据岗位编码做最小范围的名称映射。
     * 用途：当数据库 Prompt 缺失时，至少保证系统 Prompt 仍然贴近用户选择的真实岗位。
     */
    /**
     * 解析面试模式。
     * 优先使用 Service 层传入的已持久化模式；缺失时再按 sessionId 兜底读取，兼容旧调用链。
     */
    private String resolveInterviewMode(String sessionId, String providedInterviewMode) {
        String providedMode = normalizeInterviewMode(providedInterviewMode);
        if (providedMode != null) {
            return providedMode;
        }
        if (sessionId == null || sessionId.isBlank()) return InterviewConstants.MODE_NORMAL;
        try {
            InterviewSession session = interviewSessionMapper.selectOne(
                    new LambdaQueryWrapper<InterviewSession>()
                            .eq(InterviewSession::getSessionId, sessionId)
                            .last("limit 1"));
            String storedMode = normalizeInterviewMode(session == null ? null : session.getInterviewMode());
            if (storedMode != null) {
                return storedMode;
            }
        } catch (Exception e) {
            log.warn("读取 interviewMode 失败, sessionId: {}", sessionId, e);
        }
        return InterviewConstants.MODE_NORMAL;
    }

    private String normalizeInterviewMode(String interviewMode) {
        if (interviewMode == null || interviewMode.isBlank()) {
            return null;
        }
        String normalizedMode = interviewMode.toLowerCase(Locale.ROOT).trim();
        return InterviewConstants.isSupportedInterviewMode(normalizedMode)
                ? normalizedMode
                : InterviewConstants.MODE_NORMAL;
    }

    private String mapJobRoleCodeToName(String jobRoleCode) {
        if (jobRoleCode == null || jobRoleCode.isBlank()) {
            return "";
        }
        String normalized = jobRoleCode.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "frontend", "frontend_engineer", "web_frontend" -> "前端开发工程师";
            case "backend", "backend_engineer", "java_engineer" -> "后端开发工程师";
            case "test_engineer", "qa_engineer", "software_test" -> "软件测试工程师";
            case "product_manager" -> "产品经理";
            case "algorithm_engineer" -> "算法工程师";
            default -> "";
        };
    }

    private String buildSystemPromptFromJobRole(List<ChatMessageItem> history, String jobRole, String jobRoleCode, Integer difficulty,
                                                String interviewMode, InterviewJobTargetContext jobTargetContext,
                                                String feedbackMode, Integer interactionType) {
        String resolvedJobRole = jobRole;
        if (resolvedJobRole == null || resolvedJobRole.isBlank()) {
            resolvedJobRole = mapJobRoleCodeToName(jobRoleCode);
        }
        if ((resolvedJobRole == null || resolvedJobRole.isBlank()) && history != null && !history.isEmpty()) {
            String first = history.get(0).content();
            if (first != null) {
                int idx = first.indexOf("岗位");
                if (idx >= 0) {
                    int start = idx;
                    int end = Math.min(start + 50, first.length());
                    resolvedJobRole = first.substring(Math.max(0, start - 10), end);
                }
            }
        }
        if (resolvedJobRole == null || resolvedJobRole.isBlank()) {
            resolvedJobRole = "软件工程师";
        }
        return buildSystemPrompt(resolvedJobRole, jobRoleCode, difficulty, interviewMode, jobTargetContext, feedbackMode, interactionType);
    }

    private String buildSystemPrompt(String jobRole, String jobRoleCode, Integer difficulty,
                                     String interviewMode, InterviewJobTargetContext jobTargetContext, String feedbackMode,
                                     Integer interactionType) {
        String voiceInstruction = buildVoiceInteractionInstruction(interactionType);
        if (InterviewConstants.FEEDBACK_MODE_IMMEDIATE.equalsIgnoreCase(feedbackMode)) {
            log.info("使用每题反馈独立 Prompt, jobRole: {}, difficulty: {}", jobRole, difficulty);
            return buildImmediateFeedbackSystemPrompt(jobRole, difficulty)
                    + buildJobTargetInstruction(jobTargetContext, jobRole)
                    + buildInterviewerPersonaInstruction(interviewMode)
                    + voiceInstruction;
        }
        // 压力面试：使用独立的硬编码 prompt，不查数据库
        if ("stress".equalsIgnoreCase(interviewMode)) {
            log.info("使用压力面试 Prompt, jobRole: {}, difficulty: {}", jobRole, difficulty);
            return buildStressSystemPrompt(jobRole, difficulty)
                    + buildJobTargetInstruction(jobTargetContext, jobRole)
                    + buildInterviewerPersonaInstruction(interviewMode)
                    + voiceInstruction;
        }
        // 普通面试：原有逻辑不变
        SysPrompt dbPrompt = null;
        if (jobRoleCode != null && !jobRoleCode.isBlank()) {
            dbPrompt = sysPromptService.getActivePromptByJobRole(
                    PromptConstants.SCENARIO_INTERVIEW, jobRoleCode, difficulty);
        }
        if (dbPrompt != null && dbPrompt.getPromptContent() != null && !dbPrompt.getPromptContent().isBlank()) {
            log.info("使用数据库配置的 Prompt, jobRoleCode: {}, difficulty: {}, promptId: {}",
                    jobRoleCode, difficulty, dbPrompt.getId());
            return dbPrompt.getPromptContent()
                    + buildJobTargetInstruction(jobTargetContext, jobRole)
                    + buildInterviewerPersonaInstruction(interviewMode)
                    + voiceInstruction;
        }
        log.debug("使用硬编码兜底 Prompt, jobRole: {}, difficulty: {}", jobRole, difficulty);
        return buildDefaultSystemPrompt(jobRole, difficulty)
                + buildJobTargetInstruction(jobTargetContext, jobRole)
                + buildInterviewerPersonaInstruction(interviewMode)
                + voiceInstruction;
    }

    /**
     * 语音面试专用输出约束。
     * 说明：语音模式会逐句朗读 SSE 内容，因此必须避免 Markdown、表格和长段落影响播报体验。
     */
    private String buildVoiceInteractionInstruction(Integer interactionType) {
        if (!Integer.valueOf(InterviewConstants.INTERACTION_TYPE_VOICE).equals(interactionType)) {
            return "";
        }
        return """

                【语音面试输出要求】
                当前为语音面试模式。请使用口语化、简洁、适合直接朗读的方式回复。
                每次回复控制在 3-5 句话以内，只问一个主问题；避免 Markdown、代码块、表格、编号清单和复杂符号。
                回答结束时使用句号、问号或感叹号，便于前端按句子边界进行语音播报。
                """;
    }

    /**
     * 固定面试官人设补充指令。
     * 本轮只支持三个受控人设，不接受用户自定义，避免 Prompt 风格不可控。
     */
    private String buildImmediateFeedbackSystemPrompt(String jobRole, Integer difficulty) {
        String difficultyDesc = InterviewConstants.getDifficultyLabel(difficulty == null ? 2 : difficulty);
        return """
                角色：真实面试官。岗位：PLACEHOLDER_JOB。难度：PLACEHOLDER_DIFF。

                【任务】
                你正在进行每题反馈模式。每次收到候选人回答后，必须先像正常面试官一样承接并提出下一轮追问，然后在下方给出本题反馈。

                【输出格式，必须严格遵守】
                第一段只输出面试官自然追问，不要输出“追问：”“问题：”等标签。追问必须自然、简短，只问一个主问题。
                追问后空一行，然后输出：
                <FEEDBACK>
                本题反馈：一句话指出本题 1 个亮点和 1 个改进点。
                </FEEDBACK>
                <FEEDBACK> 标签必须原样输出，不能改名，不能省略。

                【反馈规则】
                反馈必须基于候选人的上一条回答，不能泛泛而谈。
                不输出分数、通过结论、长报告、模板化清单或示范答案。
                候选人回答很短时，追问要要求补充具体背景、个人动作和结果；反馈中明确指出回答过短。
                你的输出只能是面试官对候选人说的话，不要解释规则，不要输出思考过程。
                """.replace("PLACEHOLDER_JOB", jobRole != null ? jobRole : "软件工程师")
                .replace("PLACEHOLDER_DIFF", difficultyDesc);
    }

    private String buildInterviewerPersonaInstruction(String interviewMode) {
        if (InterviewConstants.MODE_BIG_COMPANY_HR.equalsIgnoreCase(interviewMode)) {
            return """

                    【面试官人设：大厂 HR 面】
                    你现在是大厂 HR 面试官。问题应侧重行为面试、职业动机、团队协作、冲突处理、价值观和文化匹配。
                    每轮仍只问一个主问题；可以要求候选人用 STAR 结构补充事实，但不要深入追问底层技术实现。
                    反馈口径更关注表达清晰度、动机可信度、协作方式和岗位稳定性。
                    """;
        }
        if (InterviewConstants.MODE_TECH_LEADER.equalsIgnoreCase(interviewMode)) {
            return """

                    【面试官人设：技术 Leader 面】
                    你现在是技术 Leader。问题应侧重技术深度、项目架构、关键取舍、边界条件、故障处理和个人贡献。
                    对候选人的泛泛回答要继续追问实现细节、数据依据和替代方案，但每轮仍只问一个主问题。
                    反馈口径更关注技术判断力、工程落地能力和项目复盘深度。
                    """;
        }
        if (InterviewConstants.MODE_FOREIGN_INTERVIEWER.equalsIgnoreCase(interviewMode)) {
            return """

                    【面试官人设：外企面试官】
                    You are now an interviewer from an international company. Conduct the interview mainly in English.
                    Focus on structured communication, concise reasoning, cross-functional collaboration, ownership, and business impact.
                    Ask one main question each turn. If the candidate answers in Chinese, you may briefly acknowledge it and continue in clear English.
                    """;
        }
        return "";
    }

    private String buildDefaultSystemPrompt(String jobRole, Integer difficulty) {
        String difficultyDesc = InterviewConstants.getDifficultyLabel(difficulty == null ? 2 : difficulty);

        String prompt = """
                角色：大厂面试官(10年经验)。岗位：PLACEHOLDER_JOB。难度：PLACEHOLDER_DIFF。

                【最高优先级 - 输出格式】
                你的每一次输出只能是面试官对候选人说的话。绝对禁止输出以下内容：
                - 括号内的思考过程或分析（如"（发现矛盾...）""（根据原则...）""（注意到...）"）
                - 任何解释你行为逻辑的文字
                - 任何内部推理、规则引用或策略说明
                - 直接引用简历原文、摘要或字段串。简历只是你的参考信息，提问时必须用自己的话自然地提及。
                - 禁止把简历文件名、姓名、性别、电话、邮箱等元信息说给候选人，也不能把这些内容用于语音播报。
                违反此规则等于严重错误。你的输出必须像真人面试官说话一样自然，不带任何元信息。

                【核心原则】
                1.你是面试官，不是候选人。只负责提问，绝对不要回答问题、解释技术概念或给出示范答案。
                2.每次只提一个主问题，可带一句很短的承接说明，禁止列清单。
                3.候选人的任何回答（包括"好的""嗯""可以"等简短回应）都由你判断质量并追问，不要代替候选人作答。
                4.每轮提问前必须回顾候选人上一轮的回答内容，基于其回答中的具体信息追问。严禁忽略候选人已提供的信息重复提问。

                【面试节奏控制 - 分阶段策略】
                面试分为三个阶段，严格按顺序推进：

                阶段一：技能热身（第 1-5 轮）
                - 有简历：围绕简历中列出的专业技能逐项验证（如"简历中写熟练掌握 HTML5/CSS3/JS，能说说 Flex 布局和 Grid 布局的区别吗？"）。目的是确认技能掌握程度，避免"写了但不会"的情况。前 5 轮不问项目、不问实习。
                - 无简历：围绕岗位要求的核心技术提问（如"后端开发岗位通常需要掌握 Java，你平时用哪些 Java 框架？"）。5 轮技术问题后必须在阶段二转为询问项目/实习经历。

                阶段二：项目/实习深挖（第 6 轮起）
                - 自然过渡：技能热身结束后，用一句话自然过渡（如"基础不错，我们聊聊你做过的项目。你有做过什么项目或者实习经历吗？可以详细说说。"）。
                - 如果候选人有实习经历：优先问实习内容、工作职责、团队协作、技术选型。
                - 如果候选人有项目经历：问项目背景、技术栈选型原因、遇到的难点、如何解决、个人负责的模块。
                - 如果候选人既没有项目也没有实习：不要跳过此阶段，继续围绕岗位核心能力提问（如技术深度问题、需求分析能力、团队协作经验、抗压能力、学习能力等），并可以在后续轮次中再次询问是否有过相关实践（课程设计、个人练习、比赛等也算）。
                - 深挖细节：追问具体实现（如"你说用了 Redis 缓存，具体缓存了什么数据？缓存失效策略是什么？"）。

                阶段三：综合评估（最后 1-2 轮）
                - 问职业规划、优劣势、期望薪资等收尾问题。
                - 根据整体表现准备结束。

                【特殊情况处理】
                - 提前结束：如果候选人的回答明显不符合岗位要求（连续 3 轮以上答非所问或完全不会），可在至少 5 轮对话后提前结束面试，用自然的方式收尾（如"好的，今天的面试就到这里，感谢你的时间"）。
                - 鼓励与安慰：候选人回答得好时，用一句话简短夸奖并紧跟下一个问题（如"掌握得很扎实，那我们看看你在实际项目中的应用"）。回答得不好时，用一句话安慰并换角度追问（如"这个知识点确实比较深，换个角度聊聊你熟悉的部分"）。夸奖和安慰各限一句，必须附带下一个问题。
                - 简短回应：候选人回复"好的""嗯""可以"时，简短确认后追问具体细节（如"具体用了什么技术？""能举个例子吗？"），不跳过当前问题。

                【岗位类型】
                技术类岗位（开发/工程师/测试/运维/算法/前端/后端/全栈/Java/Python/Go/数据/安全）-> 技术栈和工程经验
                综合类岗位（教师/设计/运营/销售/管理/电气/电工）-> 专业背景、从业经历和核心技能

                【简历与岗位不匹配处理】
                如果系统提供的简历明显与当前面试岗位不符（如简历是前端开发，但面试的是后端开发），你必须：
                1.在开场时简要提及（如"我注意到你简历是前端方向，今天面试的是后端岗位，想了解下你的转型考虑"），不要假装简历和岗位一致。
                2.后续所有问题必须围绕当前面试岗位的要求提问，不要问简历中与岗位无关的技能（如面试后端时不要问Vue3/CSS布局）。
                3.可以适当询问简历与岗位的相通之处（如"你学过前端，对HTTP协议应该比较熟悉，能说说吗？"），但重点仍是岗位核心能力。
                4.严禁根据简历内容编造候选人不具备的岗位相关技能。简历只是参考，候选人的实际能力以面试中的回答为准。

                【禁止事项】
                - 不输出评分/报告/建议/点评
                - 不告诉候选人是否通过
                - 不输出脚本式文字（如"等候选人回答后""追问：""[具体模块]"）
                - 如果系统已提供简历或 JD，代表你已看过，严禁说"看不到简历""无法查看简历"
                - 没有简历时严禁编造任何不存在的项目或经历
                - 严禁输出任何内部推理、思考过程或规则引用（如括号内的"根据XX原则""根据XX规则"等元信息），这些内容绝不能展示给候选人。你的输出只能是面试官说的话，不能包含任何解释你行为逻辑的文字
                """;
        return prompt.replace("PLACEHOLDER_JOB", jobRole)
                     .replace("PLACEHOLDER_DIFF", difficultyDesc);
    }

    /**
     * 压力面试专用 System Prompt
     * 目标：通过有意图的施压行为，观察候选人被质疑、被推向边界时的真实反应
     */
    private String buildStressSystemPrompt(String jobRole, Integer difficulty) {
        String difficultyDesc = InterviewConstants.getDifficultyDescription(difficulty);

        // 分级施压策略
        String pressureLevel = switch (difficulty == null ? 2 : difficulty) {
            case 1 -> """
                    【分级施压：初级岗 — 轻度质疑】
                    - 适度追问细节，观察基本功是否扎实
                    - 质疑语气温和但坚定，不摧毁自信心
                    - 重点看学习意愿和反应速度，不苛求战略视野
                    - 遇到明显不会的问题，换个角度再给一次机会，而非穷追猛打
                    """;
            case 3 -> """
                    【分级施压：高级岗 — 尖锐挑战】
                    - 对每个回答进行战略层面的质疑："这个决策的依据是什么？有没有考虑过反面？"
                    - 质问个人贡献 vs 团队光环："你说的这个成果，如果换一个人来做，结果会不同吗？"
                    - 追问后不给缓冲，直接抛出下一个高难度问题，制造持续压迫感
                    - 直接挑战简历中的夸大表述："从你的描述，我无法判断这是你的独立贡献"
                    - 追问领导力和决策力："如果团队成员强烈反对你的方案，你怎么办？"
                    """;
            default -> """
                    【分级施压：中级岗 — 标准压力】
                    - 追问到逻辑自洽层面，不允许模糊表述蒙混过关
                    - 可以打断冗长回答，要求精简
                    - 质疑其方法论和独立思考能力
                    - 对"我们做了什么"追问"你个人做了什么"
                    """;
        };

        String prompt = """
                角色：高压面试官(15年大厂经验)。岗位：PLACEHOLDER_JOB。难度：PLACEHOLDER_DIFF。
                目标：不是全面了解候选人，而是**检验抗压能力、情绪控制、真实性格、思维底线**。以疑为主，验证表述真实性。

                【最高优先级 - 输出格式】
                你的每一次输出只能是面试官对候选人说的话。绝对禁止输出以下内容：
                - 括号内的思考过程或分析（如"（发现矛盾...）""（根据原则...）"）
                - 括号内的动作、表情、语气描述（如"（冰冷注视）""（快速打断）""（严肃地）""（沉默5秒）"）
                - 任何解释你行为逻辑的文字
                - 任何内部推理、规则引用或策略说明
                - 直接引用简历原文，简历只是参考，提问时用自己的话自然提及
                - 禁止把简历文件名、姓名、性别、电话、邮箱等元信息说给候选人，也不能把这些内容用于语音播报。
                违反此规则等于严重错误。这是文字聊天面试，所有施压必须通过文字内容本身实现，不能通过括号内的元描述实现。

                【核心原则】
                1.你是面试官，不是候选人。只负责提问，绝对不要回答问题或给出示范答案。
                2.每次只提一个主问题，可带一句很短的承接说明，禁止列清单。
                3.候选人的任何回答都由你判断质量并追问，不要代替候选人作答。
                4.每轮提问前必须回顾候选人上一轮的回答内容，基于其回答中的具体信息追问。

                PRESSURE_LEVEL_PLACEHOLDER

                【输出格式补充 — 严禁行为描述】
                这是文字聊天面试，不是视频面试。严禁在输出中使用括号描述动作、表情或语气（如"（冰冷注视）""（快速打断）""（沉默5秒）""（严肃地）"等）。所有施压必须通过文字内容本身实现，不能通过元描述实现。

                【七大施压技巧 — 必须在面试中综合运用】

                ① 连续追问逼向细节（3-5层深挖）
                每个主问题必须预设追问路径，像剥洋葱一样层层深入：
                - 第1层：问主问题（如"你做过最成功的项目是什么？"）
                - 第2层：质疑指标（"这个提升30%具体怎么算的？数据来源？"）
                - 第3层：质疑归因（"这个成果是你独立完成的还是团队的？你的具体贡献？"）
                - 第4层：假设否定（"如果我告诉你这个提升完全是市场自然增长，你怎么证明是你的功劳？"）
                - 第5层：认知闭合（直到候选人无法自圆其说或坦诚承认）
                追问不要停在表面，必须追问到候选人出现**认知闭合（无法自圆其说）或情绪反应**为止。

                ② 打断与节奏压迫
                - 候选人回答冗长未说到重点时，直接打断："请用一句话概括你的核心观点。"
                - 追问后不给思考缓冲，立即要求回答，不接受"让我想想"作为回应
                - 适时频繁切换话题："这个先不谈，换个方向——"
                - 制造节奏不对称，让候选人无法预判你的提问节奏

                ③ 质疑式回应（无论回答质量如何）
                不给予任何正面肯定，所有回应都是中性或质疑：
                - "这听起来很普通，大多数人都能做到。"
                - "你确定这是你的真实想法？还是你以为我想听这个？"
                - "从你刚才的描述，我没看出任何特别的贡献，你再想想。"
                - "你这个回答和上一个问题的回答似乎矛盾了，能解释一下吗？"
                即使回答很好，也不说"不错"、"很好"，仅说"好，下一个问题。"

                ④ 追问压迫
                候选人回答后，不给任何缓冲或过渡，直接抛出下一个尖锐问题。用连续追问制造压迫感，让候选人没有喘息空间。不回应候选人的"让我想想""我需要思考一下"等请求，直接追问"这有什么好想的？你做过的事情不应该马上能说出来吗？"

                ⑤ 压力场景题 / 陷阱题（占比不低于30%）
                穿插以下类型的问题：
                - 假设负面情景："如果你发现主管做了错误决策会导致项目失败，而主管正在气头上，你怎么办？"
                - 挑动自我认知矛盾："你说善于团队合作，但你刚才所有例子都是自己决定的，这不矛盾吗？"
                - 简历质疑："从简历看你的进步速度其实偏慢，你自己怎么看？"
                - 前任暗示："你这个岗位上一任就是因为和团队合不来离开的，你怎么避免同样问题？"
                - 归因挑战："你提到的这个失败，你觉得主要原因是你自己还是外部环境？"

                ⑥ 态度压力
                - 全程使用简洁、冷淡的措辞，不使用任何鼓励性话术（"不错""很好""加油""挺好的"）
                - 对优质回答仅说"好，下一个问题"或"记录下来了"，绝不给予正面评价
                - 对一般回答直接说"继续"或"然后呢"
                - 制造信息不对称：不告诉候选人回答得好不好，让候选人无法判断自己的表现

                ⑦ 矛盾追问（实时检测）
                如果简历中写过某个能力（如"善于团队合作"），但面试回答中多次出现"我个人的决策"、"我自己判断"等表述，立即指出矛盾："你说善于合作，但你刚才所有的例子都是自己决定的，这不矛盾吗？"。简历声称与实际回答的任何不一致都必须被追问。

                【面试节奏控制 — 三阶段】
                压力面试同样分三阶段，但施压贯穿始终：

                阶段一：技能热身 + 质疑验证（第 1-5 轮）
                - 有简历且匹配岗位：围绕简历中与岗位相关的技能逐项验证，每项追问2-3层质疑掌握深度，用质疑语气开场（如"简历上写熟练掌握XX，那我考你几个问题"）
                - 有简历但不匹配岗位：开场直接质疑转型动机（如"你简历是XX方向，今天面的是YY岗位，你觉得自己凭什么能胜任？"），后续围绕当前岗位核心技术提问，不问简历中与岗位无关的技能
                - 无简历：围绕岗位核心技术提问，追问到候选人答不上来为止，严禁编造任何不存在的项目或经历
                - 阶段一不问项目、不问实习，只验证技能真实水平

                阶段二：项目/实习深挖 + 施压追问（第 6 轮起）
                - 有简历：从简历中的项目/实习经历切入，每项追问3-5层，质疑个人贡献（"这个成果是你独立完成的还是团队的？"）、数据真实性（"这个数字怎么来的？核算方式有被质疑过吗？"）、决策合理性
                - 无简历：自然过渡询问项目/实习（如"基础部分就到这里，聊聊你做过的项目——不过我先提醒你，我会追问很多细节"）。如果有项目/实习则重点深挖；如果没有，继续围绕岗位能力提问（技术深度、团队协作、抗压能力、学习能力等），后续可再次询问是否有课程设计、个人练习、比赛等相关实践
                - 穿插压力场景题和陷阱题（占比不低于30%）
                - 对模糊表述（"大概"、"好像"、"我们"）立即打断要求具体化

                阶段三：综合评估 + 终极施压（最后 1-2 轮）
                - 问职业规划时质疑动机："你说想深耕技术，但从你的回答我看不到持续学习的痕迹"
                - 问优劣势时挑战自我认知："你说的优势我没在面试中看到"
                - 有简历时质疑简历与面试表现的一致性
                - 最后一个问题可以是高难度的压力场景题

                【特殊情况处理】
                - 候选人明显扛不住（连续3轮以上答非所问或情绪失控）：可在至少5轮后结束，但保持冷淡语气收尾，不做任何安慰
                - 候选人要求解释或重复问题：用简短冷淡的语气重复，不加任何安慰或鼓励
                - 候选人反问面试官：不回答，直接拉回"这个问题由我来问，请你回答"
                - 候选人回复过短或敷衍（如"嗯""好的""可以"）：直接追问"就这些？你确定不需要补充？"

                【岗位类型】
                技术类岗位（开发/工程师/测试/运维/算法）-> 技术深度质疑 + 工程决策追问
                综合类岗位（教师/设计/运营/销售/管理）-> 专业能力质疑 + 情景压力测试

                【简历与岗位不匹配处理】
                如果系统提供的简历明显与当前面试岗位不符（如简历是前端开发，但面试的是后端开发），必须：
                1.开场直接指出并质疑转型动机："你简历是XX方向，今天面的是YY岗位，你觉得自己凭什么能胜任？"
                2.后续所有问题围绕当前岗位要求提问，不问简历中与岗位无关的技能
                3.可以追问简历与岗位的相通之处，但语气必须是质疑而非鼓励

                【岗位定向面试处理】
                如果系统提供了岗位描述（JD），你必须：
                1.围绕JD中的核心能力项设计压力问题，逐项质疑候选人是否具备
                2.引用JD中的具体要求追问（如"这个岗位要求有微服务架构经验，你能说说你在这方面做了什么？"）
                3.对候选人声称具备但无法证明的JD能力项，连续追问3层以上直到认知闭合

                【禁止事项】
                - 不输出评分/报告/建议/点评
                - 不告诉候选人是否通过
                - 不输出脚本式文字
                - 不说"看不到简历"——如果系统提供了简历，代表你已看过
                - 没有简历时严禁编造任何不存在的项目或经历
                - 严禁输出任何内部推理、思考过程或规则引用
                - 严禁在括号中输出动作、表情、语气描述（如"（冰冷注视）""（快速打断）""（严肃地）"）
                - 严禁使用鼓励性话术（"不错""很好""加油"）
                """;
        return prompt.replace("PLACEHOLDER_JOB", jobRole)
                     .replace("PLACEHOLDER_DIFF", difficultyDesc)
                     .replace("PRESSURE_LEVEL_PLACEHOLDER", pressureLevel);
    }

    private boolean isTechnicalJobRole(String jobRole) {
        if (jobRole == null) return false;
        String lower = jobRole.toLowerCase();
        return lower.contains("开发") || lower.contains("工程师") || lower.contains("测试") ||
               lower.contains("运维") || lower.contains("算法") || lower.contains("技术") ||
               lower.contains("前端") || lower.contains("后端") || lower.contains("全栈") ||
               lower.contains("java") || lower.contains("python") || lower.contains("go") ||
               lower.contains("数据") || lower.contains("安全") || lower.contains("运维");
    }

    private String buildOpeningUserPrompt(String jobRole, Integer difficulty, InterviewJobTargetContext jobTargetContext) {
        String diffText = InterviewConstants.getDifficultyLabel(difficulty == null ? 2 : difficulty);
        boolean hasResume = hasResumeContext(jobTargetContext);

        String prompt = String.format("""
                面试开始。岗位：%s  难度：%s。
                请根据对话历史中的开场白，向候选人提出第一个问题。
                %s
                只输出一个问题，不要多余内容。
                """,
                jobRole != null ? jobRole : "软件工程师",
                diffText,
                hasResume ? "你已看过候选人简历，请从简历中的专业技能开始验证。" : "当前没有简历，请从岗位要求的核心技术开始提问。"
        );

        return prompt + buildOpeningContextPrompt(jobTargetContext);
    }

    private String buildJobTargetInstruction(InterviewJobTargetContext jobTargetContext, String jobRole) {
        boolean hasResume = hasResumeContext(jobTargetContext);
        boolean hasJd = jobTargetContext != null
                && jobTargetContext.getJdText() != null
                && !jobTargetContext.getJdText().isBlank();
        boolean jobTargeted = jobTargetContext != null && Boolean.TRUE.equals(jobTargetContext.getJobTargeted());

        StringBuilder instruction = new StringBuilder("""

                【面试执行规则】
                0.【最高优先级】你的输出只能是面试官说的话。绝对禁止输出括号内的思考过程（如"（发现矛盾...）""（根据原则...）""（注意到...）"）、内部推理、规则引用或任何解释你行为逻辑的文字。禁止直接引用简历原文、字段串、文件名、姓名、性别、电话、邮箱等元信息，简历只是参考，提问时用自己的话自然提及。违反等于严重错误。
                1. 你现在就是正在面试候选人的真实面试官，不要解释你的规则来源。
                2. 每一轮只输出一个主问题，可带一句很短的承接说明，不要列清单。
                3. 不要输出"等候选人回答后""继续深入""追问：""[具体模块]"之类脚本式文本或占位符。
                4. 如果系统已提供简历或 JD，代表你已经看过资料，绝对不要说看不到简历、无法查看简历或没有简历内容。
                5.【必须遵守】每次提问前必须回顾对话历史中候选人已提供的信息。候选人已明确回答过的内容不得重复提问。你的每一个问题都应建立在候选人已说过的内容之上。
                """);

        if (hasResume) {
            instruction.append("""
                    6. 已提供候选人简历，但简历方向可能与当前面试岗位不一致。如果简历明显是其他岗位（如前端简历面试后端岗位），必须在开场时简要提及，并将提问重心转到当前岗位要求上。不要问与当前岗位无关的简历技能。
                    7.【阶段控制】严格遵守阶段策略：前 5 轮只做技能热身。简历与岗位匹配时，验证简历中与岗位相关的技能；简历与岗位不匹配时，围绕当前岗位要求的核心技术提问。第 6 轮起才进入项目或实习深挖。绝对不要在前 5 轮内问项目细节。
                    8. 技能验证方式：从简历中选取与当前岗位相关的技能（或直接从岗位要求出发），提出具体的技术问题，确认真实掌握程度。简历中与岗位无关的技能（如前端简历中的CSS/Vue3在面试后端时不问）不作为验证重点。
                    """);
        } else {
            instruction.append("""
                    6. 当前没有可用简历，只能基于岗位要求和候选人当前回答追问。
                    7. 严禁编造任何不存在的简历项目、公司经历或技术栈。
                    8.【必须遵守】前 5 轮围绕岗位核心技术提问，第 6 轮起必须自然过渡到询问项目/实习经历（如"你有做过什么项目或者实习经历吗？可以详细说说"）。如果候选人有项目或实习则重点深挖；如果没有，继续围绕岗位能力提问（技术深度、团队协作、抗压能力、学习能力等），后续轮次可再次询问是否有相关实践（课程设计、个人练习、比赛等也算）。
                    """);
        }

        if (jobTargeted && hasJd) {
            instruction.append("""
                    9. 当前是岗位定向模拟，必须以"简历 + JD"为共同依据，更偏向验证目标岗位能力。
                    10. 如果简历与 JD 不完全匹配，先从简历里的真实经历切入，再映射到目标岗位的可迁移能力与能力缺口。
                    11. 要围绕 JD 的职责、技能要求和能力模型提问，但不能因此忽略已有简历经历。
                    """);
        } else if (jobTargeted) {
            instruction.append("""
                    9. 当前岗位定向上下文里没有有效 JD，请回退为普通岗位面试逻辑，但仍遵守简历使用规则。
                    """);
        } else if (hasResume) {
            instruction.append("""
                    9. 当前是普通模拟面试，但你已拿到候选人简历，应更多围绕简历内容进行岗位相关追问。
                    """);
        } else {
            instruction.append("""
                    9. 当前既没有 JD 也没有简历，请按岗位核心能力进行正常面试。
                    """);
        }

        String contextSummary = buildJobTargetContextSummary(jobTargetContext);
        if (!contextSummary.isBlank()) {
            instruction.append("\n【可用上下文】\n").append(contextSummary);
        }
        instruction.append("\n你正在面试的岗位是：").append(jobRole != null ? jobRole : "软件工程师");
        return instruction.toString();
    }

    private String buildOpeningContextPrompt(InterviewJobTargetContext jobTargetContext) {
        if (jobTargetContext == null) {
            return "";
        }
        String summary = buildJobTargetContextSummary(jobTargetContext);
        if (summary.isBlank()) {
            return "";
        }
        String title = Boolean.TRUE.equals(jobTargetContext.getJobTargeted())
                ? "请优先使用以下岗位定向上下文开始提问："
                : "请使用以下简历上下文开始提问：";
        return "\n\n" + title + "\n" + summary;
    }

    private String buildJobTargetEvaluationPrompt(InterviewJobTargetContext jobTargetContext) {
        if (jobTargetContext == null || !Boolean.TRUE.equals(jobTargetContext.getJobTargeted())) {
            return "";
        }
        return "\n\n【岗位定向评价要求】\n"
                + buildJobTargetContextSummary(jobTargetContext)
                + "\n请在评价中明确体现岗位匹配表现、回答优势、暴露短板和针对目标岗位的改进建议。";
    }

    private String buildJobTargetContextSummary(InterviewJobTargetContext jobTargetContext) {
        if (jobTargetContext == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (jobTargetContext.getJdText() != null && !jobTargetContext.getJdText().isBlank()) {
            // JD保留3000字符
            sb.append("- 目标岗位 JD：").append(trimPromptText(jobTargetContext.getJdText(), 3000)).append("\n");
        }

        String resumeText = resolveResumeTextForPrompt(jobTargetContext);
        if (resumeText != null && !resumeText.isBlank()) {
            String structuredResume = AiInputCompressor.toStructuredFormat(
                    resumeText,
                    AiInputCompressor.ContentType.RESUME
            );
            sb.append("- 简历结构化摘要：").append(trimPromptText(structuredResume, 4500)).append("\n");
        }

        if (jobTargetContext.getMatchedKeywords() != null && !jobTargetContext.getMatchedKeywords().isEmpty()) {
            sb.append("- 已匹配关键词：").append(String.join("、", jobTargetContext.getMatchedKeywords())).append("\n");
        }
        if (jobTargetContext.getMissingKeywords() != null && !jobTargetContext.getMissingKeywords().isEmpty()) {
            sb.append("- 缺失关键词：").append(String.join("、", jobTargetContext.getMissingKeywords())).append("\n");
        }
        if (jobTargetContext.getSuggestions() != null && !jobTargetContext.getSuggestions().isEmpty()) {
            sb.append("- 岗位优化建议：").append(String.join("；", jobTargetContext.getSuggestions())).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * 解析当前上下文里的简历文本。
     * 说明：部分旧会话只存了 resumeTaskId，没有把 resumeText 一并带入，因此这里要做一次数据库兜底。
     */
    private String resolveResumeTextForPrompt(InterviewJobTargetContext jobTargetContext) {
        if (jobTargetContext == null) {
            return "";
        }
        String resumeText = jobTargetContext.getResumeText();
        if ((resumeText == null || resumeText.isBlank()) && jobTargetContext.getResumeTaskId() != null) {
            try {
                ResumeDiagnosisTask resumeTask = resumeDiagnosisTaskMapper.selectOne(
                        new LambdaQueryWrapper<ResumeDiagnosisTask>()
                                // Prompt 兜底需要 resume_text，显式补回默认不加载的大字段。
                                .select(ResumeDiagnosisTask::getId, ResumeDiagnosisTask::getResumeText)
                                .eq(ResumeDiagnosisTask::getId, Long.parseLong(jobTargetContext.getResumeTaskId()))
                                .last("limit 1"));
                if (resumeTask != null && resumeTask.getResumeText() != null
                        && !resumeTask.getResumeText().isBlank()) {
                    resumeText = resumeTask.getResumeText();
                    log.info("从数据库加载简历文本，resumeTaskId: {}", jobTargetContext.getResumeTaskId());
                }
            } catch (Exception e) {
                log.warn("加载简历文本失败，resumeTaskId: {}", jobTargetContext.getResumeTaskId(), e);
            }
        }
        if (resumeText == null || resumeText.isBlank()) {
            return "";
        }
        String cleaned = resumeText.replaceAll("[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]", "")
                .replace("\u0001", " ")
                .trim();
        jobTargetContext.setResumeText(cleaned);
        return cleaned;
    }

    /**
     * 判断本轮是否存在可用简历上下文。
     * 这里必须走统一兜底逻辑，避免 resumeTaskId 已存在但 prompt 仍误判成"无简历模式"。
     */
    private boolean hasResumeContext(InterviewJobTargetContext jobTargetContext) {
        return !resolveResumeTextForPrompt(jobTargetContext).isBlank();
    }

    private String trimPromptText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    /**
     * 根据配置决定是否压缩面试历史对话
     *
     * @param history 原始历史对话列表
     * @param tag     日志标签（用于区分不同 AI 提供商）
     * @return 压缩后的对话列表（若压缩未启用则返回原文）
     *
     * 【逻辑说明】
     * 1. 若配置 compressionEnabled=false，直接返回原文（用于回滚或调试）
     * 2. 统计当前用户轮次，调用 InterviewContextCompressor 进行分层压缩
     * 3. 若发生压缩，记录压缩前后的 token 数差异，便于监控效果
     */
    private List<ChatMessageItem> compressHistoryIfEnabled(
            List<ChatMessageItem> history,
            String tag,
            Long userId,
            boolean fallbackToPlatform) {
        if (!tokenLimitConfig.isCompressionEnabled() || history == null || history.isEmpty()) {
            return history;
        }

        // 常规问答压缩继续沿用本次会话 AI 来源，避免摘要阶段因为缺少 userId 切回平台配置。
        List<ChatMessageItem> compressed = contextCompressor.compressHistory(
                history, history.size(), tag, userId, fallbackToPlatform);

        // 若发生压缩（返回对象与原文不同），记录压缩效果
        if (compressed != history) {
            int originalTokens = estimateHistoryTokens(history);
            int compressedTokens = estimateHistoryTokens(compressed);
            log.info("[{}] 面试历史对话已压缩: 原始{}条/{}token -> 压缩后{}条/{}token",
                    tag, history.size(), originalTokens, compressed.size(), compressedTokens);
        } else {
            // 未压缩时，输出调试信息
            int totalTokens = estimateHistoryTokens(history);
            log.debug("[{}] 面试历史对话未压缩: 当前{}条/{}token, 阈值{}条, maxTokens={}",
                    tag, history.size(), totalTokens,
                    tokenLimitConfig.getHistorySummaryThreshold(),
                    tokenLimitConfig.getInterviewRoundMax());
        }

        return compressed;
    }

    /**
     * 估算历史对话的 token 数量
     *
     * @param history 历史对话列表
     * @return 预估总 token 数（角色 + 内容）
     */
    private int estimateHistoryTokens(List<ChatMessageItem> history) {
        if (history == null || history.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (ChatMessageItem item : history) {
            total += TokenEstimator.estimateTokens(item.role());
            total += TokenEstimator.estimateTokens(item.content());
        }
        return total;
    }

    private RuntimeAiConfig resolveRuntimeConfig() {
        return resolveRuntimeConfig(null, false);
    }

    private String runtimeLogTag(RuntimeAiConfig runtimeConfig) {
        if (runtimeConfig != null
                && UserAiConstants.BILLING_SOURCE_USER_CUSTOM.equals(runtimeConfig.source())) {
            return "USER_CUSTOM/openai-compatible";
        }
        if (runtimeConfig == null || runtimeConfig.provider() == null || runtimeConfig.provider().isBlank()) {
            return "UNKNOWN";
        }
        return platformLogTag(runtimeConfig.provider());
    }

    private String platformLogTag(String providerName) {
        String normalizedProvider = providerName == null ? "" : providerName.trim();
        if (normalizedProvider.isBlank()) {
            return "PLATFORM/UNKNOWN";
        }
        return "PLATFORM/" + normalizedProvider.toUpperCase(Locale.ROOT);
    }

    private void logRuntimeRoute(String tag, RuntimeAiConfig runtimeConfig, String stage) {
        log.info("[{}] AI 路由: stage={}, source={}, baseUrl={}, endpoint={}, model={}, configType={}",
                tag,
                stage,
                runtimeConfig.source(),
                runtimeConfig.baseUrl(),
                runtimeConfig.endpoint(),
                runtimeConfig.model(),
                runtimeConfig.configType());
    }

    private RuntimeAiConfig resolveRuntimeConfig(Long userId, boolean fallbackToPlatform) {
        ResolvedAiConfig userConfig = userAiConfigResolver == null
                ? null
                : userAiConfigResolver.resolve(userId, AiEngineConstants.BUSINESS_TYPE_INTERVIEW, fallbackToPlatform);
        if (userConfig != null) {
            // 命中用户自定义 interview/default 配置时，直接跳过平台配置和平台额度链路。
            return new RuntimeAiConfig(
                    userConfig.getProvider(),
                    userConfig.getModel(),
                    userConfig.getBaseUrl(),
                    getEndpointByProvider(userConfig.getProvider()),
                    userConfig.getApiKey(),
                    UserAiConstants.BILLING_SOURCE_USER_CUSTOM,
                    null,
                    "none",
                    userConfig.getConfigType()
            );
        }
        String fallbackProvider = normalizeConfigValue(provider);
        if (fallbackProvider == null) {
            fallbackProvider = "doubao";
        }
        fallbackProvider = fallbackProvider.toLowerCase(Locale.ROOT);
        String fallbackModel = normalizeConfigValue(model);
        String fallbackBaseUrl = resolveBaseUrl(fallbackProvider, configuredBaseUrl);
        String fallbackApiKey = getApiKey();

        String runtimeProvider = fallbackProvider;
        String runtimeModel = fallbackModel;
        String runtimeBaseUrl = fallbackBaseUrl;
        String runtimeApiKey = fallbackApiKey;
        String source = "application";
        Integer runtimeTimeoutMs = null;
        String runtimeThinkingMode = this.thinkingMode;

        SysAiEngineConfig activeConfig = null;
        try {
            activeConfig = sysAiEngineConfigService.getActiveByBusinessType(AiEngineConstants.BUSINESS_TYPE_INTERVIEW);
        } catch (Exception e) {
            log.warn("读取面试业务激活 AI 配置失败，回退本地配置: {}", e.getMessage());
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
            String dbApiKey = normalizeConfigValue(aiCredentialCrypto.decrypt(activeConfig.getApiKey()));
            if (dbApiKey != null) {
                runtimeApiKey = dbApiKey;
                log.debug("[DEBUG] 从数据库读取到 apiKey, 长度: {}", dbApiKey.length());
            } else {
                log.warn("数据库 apiKey 为空，使用本地兜底");
            }
            runtimeTimeoutMs = activeConfig.getTimeoutMs();
            // DB 思考模式优先，为空时沿用 YAML 注入值。
            String dbThinkingMode = normalizeConfigValue(activeConfig.getThinkingMode());
            if (dbThinkingMode != null) {
                runtimeThinkingMode = dbThinkingMode;
            }
            source = "db-active:" + activeConfig.getEngineCode();
        }

        if (runtimeModel == null) {
            runtimeModel = fallbackModel;
        }
        if (runtimeBaseUrl == null) {
            runtimeBaseUrl = fallbackBaseUrl;
        }
        if (runtimeApiKey == null || runtimeApiKey.isBlank()) {
            log.warn("[INTERVIEW] runtimeApiKey 仍为空，尝试从环境变量兜底获取");
            runtimeApiKey = getApiKey();
        }
        if (runtimeApiKey == null || runtimeApiKey.isBlank()) {
            throw new IllegalStateException("面试 AI 密钥不可用：数据库和管理端均无有效配置。"
                    + "请在管理端激活 AI 引擎配置，或设置环境变量 DOUBAO_API_KEY");
        }

        log.debug("[DEBUG] runtimeApiKey 最终状态: 长度={}", runtimeApiKey.length());

        return new RuntimeAiConfig(
                runtimeProvider,
                runtimeModel,
                runtimeBaseUrl,
                getEndpointByProvider(runtimeProvider),
                runtimeApiKey,
                source,
                runtimeTimeoutMs,
                runtimeThinkingMode,
                "platform"
        );
    }

    private String getEndpointByProvider(String providerType) {
        return switch (providerType) {
            case "ernie" -> "/chat/completions";
            default -> "/chat/completions";
        };
    }

    private String normalizeConfigValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 用户自定义 AI 失败不自动回落本地 Mock，必须让前端展示手动平台回退入口。
     */
    private void throwCustomAiFailure(RuntimeAiConfig runtimeConfig, Exception exception) {
        String message = exception == null || exception.getMessage() == null
                ? "自定义AI调用失败"
                : "自定义AI调用失败: " + exception.getMessage();
        throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, message);
    }

    private record RuntimeAiConfig(
            String provider,
            String model,
            String baseUrl,
            String endpoint,
            String apiKey,
            String source,
            Integer timeoutMs,
            String thinkingMode,
            String configType
    ) {
    }

    private static class RequestBody {
        public String model;
        public List<Message> messages;
        public Integer max_tokens;
        public Thinking thinking;

        public RequestBody() {
        }
    }

    private static class StreamRequestBody {
        public String model;
        public List<Message> messages;
        public boolean stream = true;
        public Thinking thinking;

        public StreamRequestBody(String model, List<Message> messages, boolean stream) {
            this.model = model;
            this.messages = messages;
            this.stream = stream;
        }
    }

    private static class Thinking {
        public String type;

        public Thinking(String type) {
            this.type = type;
        }
    }

    private static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    private static class ResponseBody {
        public List<Choice> choices;

        private static class Choice {
            public MessageContent message;

            private static class MessageContent {
                public String content;
            }
        }
    }

    private ResponseBody tryReadResponseBody(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(rawText.trim(), ResponseBody.class);
        } catch (Exception ex) {
            log.warn("AI 响应 JSON 解析失败: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * 根据 sessionId 获取最近的简历信息，用于普通面试携带简历上下文
     * 
     * @param sessionId 面试会话ID
     * @return InterviewJobTargetContext 包含简历文本等信息，如果未找到则返回null
     */
    private InterviewJobTargetContext getLatestResumeContext(String sessionId) {
        try {
            // 1. 先尝试通过 sessionId 查 mock_interview_job_target_record（岗位定向面试）
            MockInterviewJobTargetRecord latestRecord = mockInterviewJobTargetRecordMapper.selectOne(
                    new LambdaQueryWrapper<MockInterviewJobTargetRecord>()
                            .eq(MockInterviewJobTargetRecord::getSessionId, sessionId)
                            // 旧链路按 JD 判断岗位定向，必须读取 jd_text 快照。
                            .select(MockInterviewJobTargetRecord::getId,
                                    MockInterviewJobTargetRecord::getSessionId,
                                    MockInterviewJobTargetRecord::getResumeTaskId,
                                    MockInterviewJobTargetRecord::getJdText,
                                    MockInterviewJobTargetRecord::getCreateTime)
                            .orderByDesc(MockInterviewJobTargetRecord::getCreateTime)
                            .last("limit 1")
            );
            
            if (latestRecord != null && latestRecord.getResumeTaskId() != null) {
                // 根据 jdText 是否存在判断是否为岗位定向面试
                boolean isJobTargeted = latestRecord.getJdText() != null && !latestRecord.getJdText().isBlank();
                ResumeDiagnosisTask resumeTask = resumeDiagnosisTaskMapper.selectOne(
                        new LambdaQueryWrapper<ResumeDiagnosisTask>()
                                // 普通/岗位面试兜底上下文需要简历文本。
                                .select(ResumeDiagnosisTask::getId, ResumeDiagnosisTask::getResumeText)
                                .eq(ResumeDiagnosisTask::getId, latestRecord.getResumeTaskId())
                                .last("limit 1"));
                if (resumeTask != null && resumeTask.getResumeText() != null && !resumeTask.getResumeText().isBlank()) {
                    log.debug("通过岗位定向记录找到简历，sessionId: {}, resumeTaskId: {}, jobTargeted: {}",
                            sessionId, resumeTask.getId(), isJobTargeted);
                    return InterviewJobTargetContext.builder()
                            .resumeTaskId(String.valueOf(resumeTask.getId()))
                            .resumeText(resumeTask.getResumeText())
                            .jobTargeted(isJobTargeted)
                            .build();
                }
            }
            
            // 2. Fallback: 通过 sessionId 查 interview_session 获取 userId，然后查最近的 resume_diagnosis_task
            InterviewSession session = interviewSessionMapper.selectOne(
                    new LambdaQueryWrapper<InterviewSession>()
                            .eq(InterviewSession::getSessionId, sessionId)
                            .last("limit 1")
            );
            
            if (session == null || session.getUserId() == null) {
                log.debug("未找到面试会话或userId为空，sessionId: {}", sessionId);
                return null;
            }
            
            // 3. 通过 userId 查最近的 resume_diagnosis_task
            ResumeDiagnosisTask resumeTask = resumeDiagnosisTaskMapper.selectOne(
                    new LambdaQueryWrapper<ResumeDiagnosisTask>()
                            .eq(ResumeDiagnosisTask::getUserId, session.getUserId())
                            .select(ResumeDiagnosisTask::getId, ResumeDiagnosisTask::getUserId,
                                    ResumeDiagnosisTask::getResumeText, ResumeDiagnosisTask::getCreateTime)
                            .orderByDesc(ResumeDiagnosisTask::getCreateTime)
                            .last("limit 1")
            );
            
            if (resumeTask == null || resumeTask.getResumeText() == null || resumeTask.getResumeText().isBlank()) {
                log.debug("未找到简历任务或简历文本为空，userId: {}", session.getUserId());
                return null;
            }
            
            log.info("通过userId找到最近简历，sessionId: {}, userId: {}, resumeTaskId: {}", 
                    sessionId, session.getUserId(), resumeTask.getId());
            
            // 构造InterviewJobTargetContext（普通面试只携带简历信息，不携带JD）
            return InterviewJobTargetContext.builder()
                    .resumeTaskId(String.valueOf(resumeTask.getId()))
                    .resumeText(resumeTask.getResumeText())
                    .jobTargeted(Boolean.FALSE)  // 普通面试不启用岗位定向
                    .build();
        } catch (Exception e) {
            log.warn("获取最近简历信息失败，sessionId: {}", sessionId, e);
            return null;
        }
    }
}
