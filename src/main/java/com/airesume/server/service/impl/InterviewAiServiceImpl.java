package com.airesume.server.service.impl;

import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.common.constants.InterviewConstants;
import com.airesume.server.common.constants.PromptConstants;
import com.airesume.server.config.AiTokenLimitConfig;
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
import com.airesume.server.service.InterviewAiService;
import com.airesume.server.service.InterviewContextCompressor;
import com.airesume.server.service.SysAiEngineConfigService;
import com.airesume.server.service.SysPromptService;
import com.airesume.server.util.AiInputCompressor;
import com.airesume.server.util.TokenEstimator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service("interviewAiService")
@Slf4j
@ConditionalOnProperty(name = "app.interview.mode", havingValue = "real")
public class InterviewAiServiceImpl implements InterviewAiService {

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
            ObjectMapper objectMapper) {
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
        this.resolvedBaseUrl = resolveBaseUrl(this.provider, configuredBaseUrl);
        this.endpoint = getEndpoint();
        this.restClient = restClientBuilder
                .baseUrl(this.resolvedBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.webClient = WebClient.builder()
                .baseUrl(this.resolvedBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        String tag = this.provider.toUpperCase();
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

    private Thinking buildThinkingConfig(String modelName, String thinkingModeConfig) {
        boolean modelSupportsThinking = supportsThinking(modelName);
        if ("none".equalsIgnoreCase(thinkingModeConfig)) {
            return null;
        }
        if (!modelSupportsThinking) {
            log.warn("[{}] 当前模型 {} 不支持 thinking 参数，已忽略配置: {}",
                    provider.toUpperCase(), modelName, thinkingModeConfig);
            return null;
        }
        if ("enabled".equalsIgnoreCase(thinkingModeConfig)) {
            return new Thinking("enabled");
        } else if ("disabled".equalsIgnoreCase(thinkingModeConfig)) {
            return new Thinking("disabled");
        }
        log.warn("[{}] 未知的 thinking-mode 配置: {}, 使用 none",
                provider.toUpperCase(), thinkingModeConfig);
        return null;
    }

    private String resolveBaseUrl(String provider, String configuredUrl) {
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            log.debug("使用用户配置的 baseUrl: {}", configuredUrl);
            return configuredUrl;
        }
        log.debug("用户未配置 baseUrl，使用默认值");
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
            log.info("[DEBUG] getApiKey: 读取到 DOUBAO_API_KEY, 长度={}, 前5位={}", key.length(), key.substring(0, 5));
            return key;
        }
        key = System.getenv("API_KEY");
        if (key != null && !key.isBlank()) {
            log.info("[DEBUG] getApiKey: 读取到 API_KEY, 长度={}, 前5位={}", key.length(), key.substring(0, 5));
            return key;
        }
        key = System.getenv("AI_API_KEY");
        if (key != null && !key.isBlank()) {
            log.info("[DEBUG] getApiKey: 读取到 AI_API_KEY, 长度={}, 前5位={}", key.length(), key.substring(0, 5));
            return key;
        }
        log.info("[DEBUG] getApiKey: 未读取到任何 API_KEY");
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
        String difficultyDesc = switch (difficulty == null ? 2 : difficulty) {
            case 1 -> "初级";
            case 3 -> "高级";
            default -> "中级";
        };
        boolean hasResume = hasResumeContext(jobTargetContext);
        String resumeHint = hasResume ? "我已经看过你的简历，" : "";
        return String.format(InterviewConstants.OPENING_TEMPLATE,
                difficultyDesc, jobRole != null ? jobRole : "软件工程师", resumeHint);
    }

    @Override
    public String generateReply(String sessionId, List<ChatMessageItem> history, String userMessage,
                                 String jobRoleCode, Integer difficulty,
                                 InterviewJobTargetContext jobTargetContext) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig();
        String tag = runtimeConfig.provider().toUpperCase();

        // 如果没有jobTargetContext，尝试获取最近的简历信息
        if (jobTargetContext == null && sessionId != null && !sessionId.isBlank()) {
            jobTargetContext = getLatestResumeContext(sessionId);
            if (jobTargetContext != null) {
                log.info("[{}] 普通面试自动携带简历信息, sessionId: {}, resumeTaskId: {}",
                        tag, sessionId, jobTargetContext.getResumeTaskId());
            }
        }

        List<ChatMessageItem> compressedHistory = compressHistoryIfEnabled(history, tag);

        log.info("[{}] 生成面试官回复, sessionId: {}, historySize: {}, compressedSize: {}, userMessageLength: {}, jobRoleCode: {}, difficulty: {}",
                tag, sessionId, history == null ? 0 : history.size(),
                compressedHistory == null ? 0 : compressedHistory.size(),
                userMessage == null ? 0 : userMessage.length(), jobRoleCode, difficulty);

        String currentJobRole = resolveCurrentJobRole(sessionId, history, jobRoleCode);
        List<Message> messages = buildConversationMessages(
                compressedHistory,
                userMessage,
                currentJobRole,
                jobRoleCode,
                difficulty,
                jobTargetContext
        );

        try {
            return chatWithMessages(messages);
        } catch (Exception e) {
            if (shouldFallbackToLocalMock(e)) {
                return buildReplyFallback(tag, sessionId, userMessage, compressedHistory, jobTargetContext, e);
            }
            log.error("[{}] 生成回复失败, sessionId: {}", tag, sessionId, e);
            throw new RuntimeException("AI 面试回复生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Publisher<String> generateReplyStream(String sessionId, List<ChatMessageItem> history, String userMessage,
                                                   String jobRoleCode, Integer difficulty,
                                                   InterviewJobTargetContext jobTargetContext) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig();
        String tag = runtimeConfig.provider().toUpperCase();

        // 如果没有jobTargetContext，尝试获取最近的简历信息
        if (jobTargetContext == null && sessionId != null && !sessionId.isBlank()) {
            jobTargetContext = getLatestResumeContext(sessionId);
            if (jobTargetContext != null) {
                log.info("[{}] 普通面试流式自动携带简历信息, sessionId: {}, resumeTaskId: {}",
                        tag, sessionId, jobTargetContext.getResumeTaskId());
            }
        }

        List<ChatMessageItem> compressedHistory = compressHistoryIfEnabled(history, tag);

        log.info("[{}] 流式生成面试官回复, sessionId: {}, historySize: {}, compressedSize: {}, jobRoleCode: {}, difficulty: {}",
                tag, sessionId, history == null ? 0 : history.size(),
                compressedHistory == null ? 0 : compressedHistory.size(), jobRoleCode, difficulty);

        String currentJobRole = resolveCurrentJobRole(sessionId, history, jobRoleCode);
        List<Message> messages = buildConversationMessages(
                compressedHistory,
                userMessage,
                currentJobRole,
                jobRoleCode,
                difficulty,
                jobTargetContext
        );

        String apiKey = runtimeConfig.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return buildReplyFallbackStream(
                    tag,
                    sessionId,
                    userMessage,
                    compressedHistory,
                    jobTargetContext,
                    new IllegalStateException("未找到可用的面试 AI 密钥")
            );
        }

        log.info("[{}] 流式请求地址: {}{}, model: {}, source: {}",
                tag, runtimeConfig.baseUrl(), runtimeConfig.endpoint(), runtimeConfig.model(), runtimeConfig.source());

        StreamRequestBody reqBody = new StreamRequestBody(runtimeConfig.model(), messages, true);
        reqBody.thinking = buildThinkingConfig(runtimeConfig.model(), thinkingMode);

        try {
            String requestJson = objectMapper.writeValueAsString(reqBody);
            log.info("[{}] 请求体JSON: {}", tag, requestJson);
        } catch (Exception e) {
            log.warn("[{}] 请求体序列化失败", tag, e);
        }

        log.info("[{}] ═══════════════════════════════════════════════", tag);
        log.info("[{}] ║  流式请求参数验证  ║", tag);
        log.info("[{}] ═══════════════════════════════════════════════", tag);
        log.info("[{}] 请求地址: {}{}", tag, runtimeConfig.baseUrl(), runtimeConfig.endpoint());
        log.info("[{}] model: {}", tag, runtimeConfig.model());
        log.info("[{}] stream: {}", tag, reqBody.stream);
        if (reqBody.thinking != null) {
            log.info("[{}] thinking.type: {}", tag, reqBody.thinking.type);
        } else {
            log.info("[{}] thinking: 未设置", tag);
        }
        log.info("[{}] ═══════════════════════════════════════════════", tag);

        WebClient runtimeWebClient = webClientBuilder
                .baseUrl(runtimeConfig.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Flux<String> rawLineFlux = runtimeWebClient.post()
                .uri(runtimeConfig.endpoint())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(reqBody)
                .retrieve()
                .bodyToFlux(String.class)
                .publishOn(Schedulers.boundedElastic());

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
        return Flux.create(sink -> {
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
                        log.error("[{}] 【处理异常】error={}, message={}", logTag, e.getClass().getSimpleName(), e.getMessage());
                        sink.error(e);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    log.error("[{}] 【WebClient错误】type={}, message={}",
                            logTag, t.getClass().getSimpleName(), t.getMessage());
                    // 外部 AI 在 DNS / 网络层失败时，直接降级到本地 Mock，保证模拟面试不中断。
                    if (emittedCount.get() == 0) {
                        emitFallbackReplyToSink(
                                sink,
                                buildReplyFallback(logTag, sessionId, userMessage, finalCompressedHistory, finalJobTargetContext, t)
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

                    log.info("");
                    log.info("╔══════════════════════════════════════════════════════════════╗");
                    log.info("║          【流式处理完成-最终统计报告 V3】                   ║");
                    log.info("╠══════════════════════════════════════════════════════════════╣");
                    log.info("║  模型: {}", runtimeConfig.model());
                    log.info("║  总接收行数:              {}", total);
                    log.info("║  成功解析JSON的行数:     {}", parsed);
                    log.info("║  跳过的行数:             {}", skipped);
                    log.info("║  JSON解析失败次数:       {}", errors);
                    log.info("║  delta.content 非空chunk: {}", cCount);
                    log.info("║  reasoning_content 非空:  {}", rCount);
                    log.info("║  实际发给下游次数:       {}", emitted);
                    log.info("║  结论: {}", conclusion);
                    log.info("╚══════════════════════════════════════════════════════════════╝");
                    log.info("");

                    sink.complete();
                }
            });
        });
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
                                      InterviewJobTargetContext jobTargetContext, Throwable cause) {
        log.warn("[{}] 真实 AI 面试回复失败，改用本地 Mock 兜底, sessionId: {}, cause={}",
                tag, sessionId, cause.getMessage(), cause);
        int messageCount = history == null ? 0 : history.size();
        return mockInterviewService.generateMockReply(sessionId, userMessage, messageCount, jobTargetContext);
    }

    /**
     * 流式问答在请求发出前就发现配置缺失时，直接返回本地 Mock 字符流，避免 SSE 进入错误事件。
     */
    private Publisher<String> buildReplyFallbackStream(String tag, String sessionId, String userMessage,
                                                       List<ChatMessageItem> history,
                                                       InterviewJobTargetContext jobTargetContext, Throwable cause) {
        String fallbackReply = buildReplyFallback(tag, sessionId, userMessage, history, jobTargetContext, cause);
        return Flux.<String>create(sink -> emitFallbackReplyToSink(sink, fallbackReply))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 将本地兜底文案按字符流输出给 SSE 下游，保持与真实流式回复一致的消费方式。
     */
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
                || name.contains("IllegalStateException"); // 未配置 API Key 等启动期异常
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
        String tag = runtimeConfig.provider().toUpperCase();
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
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig();
        String tag = runtimeConfig.provider().toUpperCase();
        log.info("[{}] ═══════════════════════════════════════════════", tag);
        log.info("[{}] ║  开始生成 AI 面试评价报告  ║", tag);
        log.info("[{}] ═══════════════════════════════════════════════", tag);
        log.info("[{}] sessionId: {}, jobRole: {}, jobRoleCode: {}, difficulty: {}, mode: {}, historySize: {}",
                tag, sessionId, jobRole, jobRoleCode, difficulty, interviewMode,
                history == null ? 0 : history.size());

        List<ChatMessageItem> compressedHistory = contextCompressor.compressForEvaluation(history, null);

        int originalTokens = estimateHistoryTokens(history);
        int compressedTokens = estimateHistoryTokens(compressedHistory);
        log.info("[{}] 评价报告历史对话已压缩: 原始{}token -> 压缩后{}token",
                tag, originalTokens, compressedTokens);

        String systemPrompt = buildEvaluationSystemPrompt(jobRole, jobRoleCode, difficulty, interviewMode, jobTargetContext);
        String userPrompt = buildEvaluationUserPrompt(compressedHistory, jobTargetContext);

        // 【Token 优化】预估评价报告的输入 token 数
        int systemTokens = TokenEstimator.estimateTokens(systemPrompt);
        int userTokens = TokenEstimator.estimateTokens(userPrompt);
        int totalTokens = systemTokens + userTokens;
        log.info("[{}] 评价报告预估token: {}(system:{}, user:{})", tag, totalTokens, systemTokens, userTokens);

        // 【Token 优化】若超过评价报告专用限制，进行二次压缩（更激进的压缩策略）
        if (tokenLimitConfig.isTokenLimitEnabled() && totalTokens > tokenLimitConfig.getInterviewEvaluationMax()) {
            log.warn("[{}] 评价报告token预估({})超过限制({})，进一步截断历史对话", tag, totalTokens, tokenLimitConfig.getInterviewEvaluationMax());
            compressedHistory = contextCompressor.compressForEvaluation(compressedHistory, "前期对话已大幅压缩");
            userPrompt = buildEvaluationUserPrompt(compressedHistory, jobTargetContext);
            userTokens = TokenEstimator.estimateTokens(userPrompt);
            totalTokens = systemTokens + userTokens;
            log.info("[{}] 二次压缩后预估token: {}(system:{}, user:{})", tag, totalTokens, systemTokens, userTokens);
        }

        String aiResponse = chat(systemPrompt, userPrompt);
        log.info("[{}] AI 评价原始响应长度: {}", tag, aiResponse == null ? 0 : aiResponse.length());

        InterviewEvaluationReport report = parseEvaluationResponse(aiResponse);
        calculateOverallScore(report);
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
        String difficultyDesc = switch (difficulty == null ? 2 : difficulty) {
            case 1 -> "初级（1-3年经验）";
            case 3 -> "高级（5年以上经验）";
            default -> "中级（3-5年经验）";
        };
        String modeDesc = "stress".equalsIgnoreCase(interviewMode) ? "压力面试" : "普通面试";

        String prompt = """
                角色：大厂技术面试官(10年经验)。任务：严格评估候选人真实水平。
                背景：岗位=PLACEHOLDER1，难度=PLACEHOLDER2，模式=PLACEHOLDER3。
                原则：1)按一线大厂标准 2)实事求是不鼓励 3)直接尖锐指出问题 4)保守评分(60以下不录用) 5)不放水。
                评分：90-100=S(远超预期)，80-89=A(优秀)，70-79=B(达标)，60-69=C(勉强)，<60=D(淘汰)。
                录用：>=80强烈推荐，>=70推荐，>=60待定，<60不推荐。
                输出JSON(无额外文本)：{"overallScore":0-100,"level":"S/A/B/C/D","finalVerdict":"结论","summary":"500字结构化深度评估","strengths":[""],"weaknesses":[""],"criticalIssues":[""],"questionPerformance":[{"question":"","answer":"","score":0,"comment":"","knowledgeTags":[""]}],"technicalDepth":{"score":0,"comment":"","strengths":["具体加分项"],"weaknesses":["具体扣分项"]},"communication":{"score":0,"comment":"","strengths":["具体加分项"],"weaknesses":["具体扣分项"]},"problemSolving":{"score":0,"comment":"","strengths":["具体加分项"],"weaknesses":["具体扣分项"]},"pressureResistance":{"score":0,"comment":"","strengths":["具体加分项"],"weaknesses":["具体扣分项"]},"jobMatch":{"score":0,"comment":"","strengths":["具体加分项"],"weaknesses":["具体扣分项"]},"hireRecommendation":"","improvementSuggestions":[""],"redFlags":[""],"missingCompetencies":[""],"inflationRisk":"","answerAuthenticity":"","interviewPerformanceTags":[""],"passProbability":0,"rejectionReasons":[""]}
                summary写作规范(500字左右，按4维度撰写，每部分100-150字，只写发现和证据不写过程赘述)：(1)胜任力匹配度(30%)：有JD时直接对标岗位关键能力项点出达标/未达标及差距，无JD时依据行业通用标准对比该级别应有表现。级别差异：初级重基础扎实和学习意愿，中级重独立操盘和方法论沉淀，高级重战略视野和架构决策力。有简历时可加入简历声称与实际回答的矛盾点。(2)行为事件真实性(30%)：用STAR简略拆解1-2个关键事例专戳模糊处，如"主导项目"实际只是参与、"提升50%"无法复现计算逻辑。压力面试重点标注追问下前后矛盾或情绪失控瞬间。无简历侧重逻辑自洽看是否有"画饼"感。(3)软技能与情景反应(20%)：沟通逻辑是否跑题语焉不详，情绪智力针对冲突和失败归因看成熟度。压力面试直接引用语速停顿等非言语信号。岗位区分：销售看重共情和说服力，研发看重复杂问题阐述能力。(4)潜在风险与适配预警(20%)：动机稳定性、价值观风险。有简历时关注频繁跳槽经历断层。级别越高越关注战略分歧风险和向上管理风格。场景适配：无简历时真实性权重升为40%侧重现场证实/证伪，无JD时胜任力改为通用潜力判断，初级岗弱化战略强化学习敏锐度，高级岗深挖个人贡献vs团队光环，压力面试将软技能与真实性合并大幅引用临场反应细节。
                questionPerformance筛选规则(不追求数量覆盖，追求每个展示项直指候选人本质)：按优先级筛选——(1)暴露致命伤的回答(直接否掉候选人的关键问题)；(2)高度矛盾的信号(简历/前面回答与现场表现冲突)；(3)高度证实性的高光(极好地证明某项核心能力)；(4)体现典型行为模式(虽非致命但能稳定反映思维/性格的样本)。每条comment必须80-120字，用具体证据和细节点评，指出该问答暴露的核心问题或亮点，不要泛泛而谈。数量硬性要求(必须严格遵守)：1-2轮返回全部问答；3-4轮questionPerformance数组至少3个元素；5轮及以上questionPerformance数组至少5个元素最多15个元素。若按优先级筛选后数量不足下限，则降低优先级标准补齐数量；超过上限则保留优先级最高的。有简历时额外检查回答与简历经历冲突如有则优先展示；无JD时弱化硬技能缺失标签强化逻辑自洽筛选；初级岗降低战略视野负面标签关注学习意愿和执行细节。
                规则：所有字段必填；level按overallScore自动判定；passProbability与overallScore一致；每个维度的strengths和weaknesses各列出1-3条具体表现，用中文描述；summary字段必须500字左右，严格按四维度结构撰写；questionPerformance按上述筛选规则智能筛选且必须满足数量下限(5轮对话至少5条最多15条)，comment必须80-120字具体深入点评。
                """;
        return prompt.replace("PLACEHOLDER1", jobRole)
                     .replace("PLACEHOLDER2", difficultyDesc)
                     .replace("PLACEHOLDER3", modeDesc);
    }

    private String buildEvaluationUserPrompt(List<ChatMessageItem> history, InterviewJobTargetContext jobTargetContext) {
        StringBuilder sb = new StringBuilder();
        if (jobTargetContext != null && Boolean.TRUE.equals(jobTargetContext.getJobTargeted())) {
            sb.append("以下为岗位定向补充上下文，请将其纳入评价：\n");
            sb.append(buildJobTargetContextSummary(jobTargetContext)).append("\n\n");
        }
        sb.append("以下是面试对话记录，请严格评估：\n\n");

        if (history != null && !history.isEmpty()) {
            int round = 1;
            for (ChatMessageItem item : history) {
                String role = "user".equalsIgnoreCase(item.role()) ? "候选人" : "面试官";
                sb.append("【").append(role).append("】\n");
                sb.append(item.content()).append("\n\n");
                if ("面试官".equals(role)) {
                    round++;
                }
            }
        } else {
            sb.append("（暂无对话记录）\n");
        }

        sb.append("请输出 JSON 格式的评价报告。");
        return sb.toString();
    }

    private InterviewEvaluationReport parseEvaluationResponse(String aiResponse) {
        String tag = provider.toUpperCase();
        if (aiResponse == null || aiResponse.isBlank()) {
            log.warn("[{}] AI 返回空响应，使用默认评价报告", tag);
            return buildDefaultEvaluationReport();
        }

        String jsonContent = extractJsonFromResponse(aiResponse);
        log.debug("[{}] 提取的 JSON 内容长度: {}", tag, jsonContent.length());

        try {
            InterviewEvaluationReport report = objectMapper.readValue(jsonContent, InterviewEvaluationReport.class);
            normalizeDimensionScores(report);
            log.info("[{}] 评价报告 JSON 解析成功", tag);
            return report;
        } catch (Exception e) {
            log.error("[{}] 评价报告 JSON 解析失败，使用默认报告: {}", tag, e.getMessage());
            log.debug("[{}] 解析失败的 JSON 内容: {}", tag, jsonContent);
            return buildDefaultEvaluationReport();
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
        normalizeDimensionScore(report.getCommunication());
        normalizeDimensionScore(report.getProblemSolving());
        normalizeDimensionScore(report.getPressureResistance());
        normalizeDimensionScore(report.getJobMatch());
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
                .technicalDepth(InterviewEvaluationReport.DimensionScore.builder().score(60).comment("暂无").build())
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

    private void calculateOverallScore(InterviewEvaluationReport report) {
        if (report.getOverallScore() != null && report.getOverallScore() > 0) {
            return;
        }

        int total = 0;
        int count = 0;

        if (report.getTechnicalDepth() != null && report.getTechnicalDepth().getScore() != null) {
            total += report.getTechnicalDepth().getScore();
            count++;
        }
        if (report.getCommunication() != null && report.getCommunication().getScore() != null) {
            total += report.getCommunication().getScore();
            count++;
        }
        if (report.getProblemSolving() != null && report.getProblemSolving().getScore() != null) {
            total += report.getProblemSolving().getScore();
            count++;
        }
        if (report.getPressureResistance() != null && report.getPressureResistance().getScore() != null) {
            total += report.getPressureResistance().getScore();
            count++;
        }
        if (report.getJobMatch() != null && report.getJobMatch().getScore() != null) {
            total += report.getJobMatch().getScore();
            count++;
        }

        if (count > 0) {
            int avgScore = total / count;
            report.setOverallScore(avgScore);
            report.setPassProbability(avgScore);
        }

        if (report.getLevel() == null || report.getLevel().isBlank()) {
            int score = report.getOverallScore() != null ? report.getOverallScore() : 60;
            if (score >= 90) report.setLevel("S");
            else if (score >= 80) report.setLevel("A");
            else if (score >= 70) report.setLevel("B");
            else if (score >= 60) report.setLevel("C");
            else report.setLevel("D");
        }
    }

    private void mapLegacyFields(InterviewEvaluationReport report) {
        com.fasterxml.jackson.databind.node.ObjectNode dimensions = objectMapper.createObjectNode();
        if (report.getTechnicalDepth() != null) {
            dimensions.put("technicalDepth", report.getTechnicalDepth().getScore());
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

    private String chat(String systemPrompt, String userPrompt) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig();
        String tag = runtimeConfig.provider().toUpperCase();
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
        request.thinking = buildThinkingConfig(runtimeConfig.model(), thinkingMode);

        try {
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            log.info("[{}] ║  非流式请求参数验证  ║", tag);
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            log.info("[{}] 请求地址: {}{}", tag, runtimeConfig.baseUrl(), runtimeConfig.endpoint());
            log.info("[{}] model: {}", tag, runtimeConfig.model());
            if (request.thinking != null) {
                log.info("[{}] thinking.type: {}", tag, request.thinking.type);
            } else {
                log.info("[{}] thinking: 未设置", tag);
            }
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            RestClient runtimeRestClient = restClientBuilder
                    .baseUrl(runtimeConfig.baseUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            ResponseBody response = runtimeRestClient.post()
                    .uri(runtimeConfig.endpoint())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(ResponseBody.class);

            if (response == null || response.choices == null || response.choices.isEmpty()) {
                throw new RuntimeException("AI 返回内容为空");
            }

            String result = response.choices.get(0).message.content;
            log.info("[{}] AI 调用成功, responseLength: {}", tag, result == null ? 0 : result.length());
            return result != null ? result.trim() : "";

        } catch (Exception e) {
            log.error("[{}] AI 调用失败", tag, e);
            throw new RuntimeException("AI 面试回复生成失败: " + e.getMessage(), e);
        }
    }

    private String chatWithMessages(List<Message> messages) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig();
        String tag = runtimeConfig.provider().toUpperCase();
        String apiKey = runtimeConfig.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未找到可用的面试 AI 密钥，请检查管理端激活配置或环境变量");
        }

        RequestBody request = new RequestBody();
        request.model = runtimeConfig.model();
        request.messages = messages;
        request.thinking = buildThinkingConfig(runtimeConfig.model(), thinkingMode);

        try {
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            log.info("[{}] ║  多轮对话请求参数验证  ║", tag);
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            log.info("[{}] 请求地址: {}{}", tag, runtimeConfig.baseUrl(), runtimeConfig.endpoint());
            log.info("[{}] model: {}", tag, runtimeConfig.model());
            if (request.thinking != null) {
                log.info("[{}] thinking.type: {}", tag, request.thinking.type);
            } else {
                log.info("[{}] thinking: 未设置", tag);
            }
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            RestClient runtimeRestClient = restClientBuilder
                    .baseUrl(runtimeConfig.baseUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            ResponseBody response = runtimeRestClient.post()
                    .uri(runtimeConfig.endpoint())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(ResponseBody.class);

            if (response == null || response.choices == null || response.choices.isEmpty()) {
                throw new RuntimeException("AI 返回内容为空");
            }

            String result = response.choices.get(0).message.content;
            log.info("[{}] AI 调用成功, responseLength: {}", tag, result == null ? 0 : result.length());
            return result != null ? result.trim() : "";

        } catch (Exception e) {
            log.error("[{}] AI 调用失败", tag, e);
            throw new RuntimeException("AI 面试回复生成失败: " + e.getMessage(), e);
        }
    }

    private List<Message> buildConversationMessages(List<ChatMessageItem> history, String currentUserMessage, String jobRole,
                                                    String jobRoleCode, Integer difficulty,
                                                    InterviewJobTargetContext jobTargetContext) {
        java.util.List<Message> messages = new java.util.ArrayList<>();

        String systemPrompt = buildSystemPromptFromJobRole(history, jobRole, jobRoleCode, difficulty, jobTargetContext);
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

        if (currentUserMessage != null && !currentUserMessage.isBlank()) {
            messages.add(new Message("user", currentUserMessage));
        }

        String tag = provider.toUpperCase();
        int totalMessages = messages.size();
        String firstRole = totalMessages > 0 ? messages.get(0).role : "none";
        String lastRole = totalMessages > 0 ? messages.get(totalMessages - 1).role : "none";
        log.info("[{}] ═══════════════════════════════════════════════", tag);
        log.info("[{}] ║  对话消息组装完成  ║", tag);
        log.info("[{}] ═══════════════════════════════════════════════", tag);
        log.info("[{}] 总消息数: {} (system:1, user:{}, assistant:{})",
                tag, totalMessages, historyUserCount + 1, historyAssistantCount);
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
    private String resolveCurrentJobRole(String sessionId, List<ChatMessageItem> history, String jobRoleCode) {
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
        String mappedJobRole = mapJobRoleCodeToName(jobRoleCode);
        if (!mappedJobRole.isBlank()) {
            return mappedJobRole;
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
                                                InterviewJobTargetContext jobTargetContext) {
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
        return buildSystemPrompt(resolvedJobRole, jobRoleCode, difficulty, jobTargetContext);
    }

    private String buildSystemPrompt(String jobRole, String jobRoleCode, Integer difficulty,
                                     InterviewJobTargetContext jobTargetContext) {
        SysPrompt dbPrompt = null;
        if (jobRoleCode != null && !jobRoleCode.isBlank()) {
            dbPrompt = sysPromptService.getActivePromptByJobRole(
                    PromptConstants.SCENARIO_INTERVIEW, jobRoleCode, difficulty);
        }
        if (dbPrompt != null && dbPrompt.getPromptContent() != null && !dbPrompt.getPromptContent().isBlank()) {
            log.info("使用数据库配置的 Prompt, jobRoleCode: {}, difficulty: {}, promptId: {}",
                    jobRoleCode, difficulty, dbPrompt.getId());
            return dbPrompt.getPromptContent() + buildJobTargetInstruction(jobTargetContext, jobRole);
        }
        log.debug("使用硬编码兜底 Prompt, jobRole: {}, difficulty: {}", jobRole, difficulty);
        return buildDefaultSystemPrompt(jobRole, difficulty) + buildJobTargetInstruction(jobTargetContext, jobRole);
    }

    private String buildDefaultSystemPrompt(String jobRole, Integer difficulty) {
        String difficultyDesc = switch (difficulty == null ? 2 : difficulty) {
            case 1 -> "初级";
            case 3 -> "高级";
            default -> "中级";
        };

        String prompt = """
                角色：大厂面试官(10年经验)。岗位：PLACEHOLDER_JOB。难度：PLACEHOLDER_DIFF。

                【最高优先级 - 输出格式】
                你的每一次输出只能是面试官对候选人说的话。绝对禁止输出以下内容：
                - 括号内的思考过程或分析（如"（发现矛盾...）""（根据原则...）""（注意到...）"）
                - 任何解释你行为逻辑的文字
                - 任何内部推理、规则引用或策略说明
                - 直接引用简历原文或摘要（如"林映⽂-前端开发⼯程师求职简历姓名：林映⽂|性别：男/..."这种原文照搬），简历只是你的参考信息，提问时必须用自己的话自然地提及（如"我注意到你简历里提到..."）
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
        String diffText = switch (difficulty == null ? 2 : difficulty) {
            case 1 -> "初级";
            case 3 -> "高级";
            default -> "中级";
        };
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
                0.【最高优先级】你的输出只能是面试官说的话。绝对禁止输出括号内的思考过程（如"（发现矛盾...）""（根据原则...）""（注意到...）"）、内部推理、规则引用或任何解释你行为逻辑的文字。禁止直接引用简历原文（如"林映⽂-前端开发⼯程师求职简历姓名：..."），简历只是参考，提问时用自己的话自然提及。违反等于严重错误。
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
                ResumeDiagnosisTask resumeTask = resumeDiagnosisTaskMapper.selectById(
                        Long.parseLong(jobTargetContext.getResumeTaskId()));
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
    private List<ChatMessageItem> compressHistoryIfEnabled(List<ChatMessageItem> history, String tag) {
        if (!tokenLimitConfig.isCompressionEnabled() || history == null || history.isEmpty()) {
            return history;
        }

        // 直接调用压缩，由compressHistory内部判断是否需要压缩
        List<ChatMessageItem> compressed = contextCompressor.compressHistory(history, history.size());

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
            String dbApiKey = normalizeConfigValue(activeConfig.getApiKey());
            if (dbApiKey != null) {
                runtimeApiKey = dbApiKey;
                log.info("[DEBUG] 从数据库读取到 apiKey, 长度: {}, 前5位: {}",
                        dbApiKey.length(), dbApiKey.substring(0, Math.min(5, dbApiKey.length())));
            } else {
                log.warn("数据库 apiKey 为空，使用本地兜底");
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

        log.info("[DEBUG] runtimeApiKey 最终状态: 长度={}, 前5位={}",
                runtimeApiKey.length(), runtimeApiKey.substring(0, Math.min(5, runtimeApiKey.length())));
        log.info("[DEBUG] Authorization 头将会是: Bearer {}****",
                runtimeApiKey.substring(0, Math.min(4, runtimeApiKey.length())));

        return new RuntimeAiConfig(
                runtimeProvider,
                runtimeModel,
                runtimeBaseUrl,
                getEndpointByProvider(runtimeProvider),
                runtimeApiKey,
                source
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

    private record RuntimeAiConfig(
            String provider,
            String model,
            String baseUrl,
            String endpoint,
            String apiKey,
            String source
    ) {
    }

    private static class RequestBody {
        public String model;
        public List<Message> messages;
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
                            .orderByDesc(MockInterviewJobTargetRecord::getCreateTime)
                            .last("limit 1")
            );
            
            if (latestRecord != null && latestRecord.getResumeTaskId() != null) {
                // 根据 jdText 是否存在判断是否为岗位定向面试
                boolean isJobTargeted = latestRecord.getJdText() != null && !latestRecord.getJdText().isBlank();
                ResumeDiagnosisTask resumeTask = resumeDiagnosisTaskMapper.selectById(latestRecord.getResumeTaskId());
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
