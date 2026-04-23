package com.airesume.server.service.impl;

import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.dto.interview.InterviewEvaluationReport;
import com.airesume.server.entity.SysAiEngineConfig;
import com.airesume.server.service.InterviewAiService;
import com.airesume.server.service.SysAiEngineConfigService;
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
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模拟面试 AI 服务实现类（真实 AI 模式）
 *
 * 所属模块：模拟面试模块 - AI 接入层
 * 职责：调用大模型 API 生成面试官回复、开场白、评价报告
 * 激活条件：当 app.interview.mode=real 时激活
 *
 * 【重要】baseUrl 配置说明：
 * - 优先使用配置文件中的 app.interview.base-url
 * - 仅当配置为空时才使用代码默认值
 * - 本类不会擅自修改用户配置的 baseUrl
 *
 * 【URL 拼接说明】
 * - baseUrl：用户配置的完整基础地址（如 https://ark.cn-beijing.volces.com/api/coding/v3）
 * - endpoint：/chat/completions
 * - 最终请求地址：baseUrl + endpoint
 *
 * @author AI Resume Team
 */
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

    /**
     * 是否开启流式调试日志（详细逐行日志）
     * - true：输出逐行追踪日志（reasoning_content、◆发射、行汇总等）
     * - false（默认）：只输出关键生命周期日志和异常日志
     * 通过 application.yml 中 app.interview.stream-debug-log 配置
     */
    @Value("${app.interview.stream-debug-log:false}")
    private boolean streamDebugLog;

    /**
     * 构造函数，初始化模拟面试 AI 服务
     *
     * 【配置读取优先级】
     * 1. 直接读取 @Value 注解注入的配置值
     * 2. 如果 baseUrl 不为空，优先使用配置值
     * 3. 仅当 baseUrl 为空时才使用默认值
     *
     * 【启动日志打印】
     * 会打印以下关键配置信息：
     * - 配置的 baseUrl（用户原始配置）
     * - 最终使用的 resolvedBaseUrl
     * - endpoint
     * - 完整请求地址（baseUrl + endpoint）
     * - model
     * - thinking-mode
     *
     * @param provider           AI 提供商（从配置 app.interview.provider 读取，默认 doubao）
     * @param configuredBaseUrl  API 基础地址（从配置 app.interview.base-url 读取，用户原始配置）
     * @param model             模型名称（从配置 app.interview.model 读取）
     * @param thinkingMode      思考模式（从配置 app.interview.thinking-mode 读取，默认 none）
     * @param restClientBuilder RestClient 构造器
     * @param objectMapper      JSON 解析工具
     */
    public InterviewAiServiceImpl(
            @Value("${app.interview.provider:doubao}") String provider,
            @Value("${app.interview.base-url:}") String configuredBaseUrl,
            @Value("${app.interview.model:}") String model,
            @Value("${app.interview.thinking-mode:none}") String thinkingMode,
            WebClient.Builder webClientBuilder,
            RestClient.Builder restClientBuilder,
            SysAiEngineConfigService sysAiEngineConfigService,
            ObjectMapper objectMapper) {
        this.provider = provider == null ? "doubao" : provider.toLowerCase();
        this.model = model;
        this.configuredBaseUrl = configuredBaseUrl;
        this.thinkingMode = thinkingMode;
        this.objectMapper = objectMapper;
        this.restClientBuilder = restClientBuilder;
        this.webClientBuilder = webClientBuilder;
        this.sysAiEngineConfigService = sysAiEngineConfigService;

        // 【重要】优先使用用户配置的 baseUrl，仅当配置为空时才使用默认值
        this.resolvedBaseUrl = resolveBaseUrl(this.provider, configuredBaseUrl);

        // 获取 endpoint
        this.endpoint = getEndpoint();

        // 初始化 RestClient 和 WebClient，严格使用 resolvedBaseUrl
        this.restClient = restClientBuilder
                .baseUrl(this.resolvedBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        this.webClient = WebClient.builder()
                .baseUrl(this.resolvedBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String tag = this.provider.toUpperCase();

        // 【启动日志】详细打印所有配置信息
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

    /**
     * 判断当前模型是否支持 thinking 参数
     *
     * 【支持的模型列表】
     * - 豆包 Doubao-Seed-2.0 系列模型
     *
     * @param modelName 模型名称
     * @return true 表示支持，false 表示不支持
     */
    private boolean supportsThinking(String modelName) {
        if (modelName == null) return false;
        String lowerModel = modelName.toLowerCase();
        // 豆包 Doubao-Seed-2.0 系列模型支持 thinking 参数
        return lowerModel.contains("doubao-seed-2.0");
    }

    /**
     * 根据配置和模型支持情况构建 thinking 配置
     *
     * 【配置含义】
     * - enabled：显式开启思考模式；仅模型支持时传 thinking.type=enabled
     * - disabled：显式关闭思考模式；仅模型支持时传 thinking.type=disabled
     * - none：不传 thinking 参数
     *
     * @param modelName 模型名称
     * @param thinkingModeConfig 配置的 thinking-mode
     * @return Thinking 对象，或 null（表示不传该字段）
     */
    private Thinking buildThinkingConfig(String modelName, String thinkingModeConfig) {
        boolean modelSupportsThinking = supportsThinking(modelName);

        // 配置为 none，直接返回 null
        if ("none".equalsIgnoreCase(thinkingModeConfig)) {
            return null;
        }

        // 模型不支持 thinking，但配置了 enabled 或 disabled
        if (!modelSupportsThinking) {
            log.warn("[{}] 当前模型 {} 不支持 thinking 参数，已忽略配置: {}",
                    provider.toUpperCase(), modelName, thinkingModeConfig);
            return null;
        }

        // 模型支持，根据配置返回对应的 thinking 对象
        if ("enabled".equalsIgnoreCase(thinkingModeConfig)) {
            return new Thinking("enabled");
        } else if ("disabled".equalsIgnoreCase(thinkingModeConfig)) {
            return new Thinking("disabled");
        }

        // 未知配置值，返回 null
        log.warn("[{}] 未知的 thinking-mode 配置: {}, 使用 none",
                provider.toUpperCase(), thinkingModeConfig);
        return null;
    }

    /**
     * 解析 API 基础地址
     *
     * 【优先级说明】
     * 1. 如果 configuredUrl 不为空且不为空白字符串 → 直接使用用户配置值
     * 2. 否则根据 provider 选择默认值（仅当用户未配置时才使用）
     *
     * 【重要】本方法不会擅自修改用户配置
     *
     * @param provider        AI 提供商
     * @param configuredUrl  用户配置的 URL（从配置文件读取）
     * @return 解析后的基础 URL
     */
    private String resolveBaseUrl(String provider, String configuredUrl) {
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            log.debug("使用用户配置的 baseUrl: {}", configuredUrl);
            return configuredUrl;
        }
        log.debug("用户未配置 baseUrl，使用默认值");
        return switch (provider) {
            case "doubao", "openai" -> "https://ark.cn-beijing.volces.com/api/v3";
            case "qwen"     -> "https://dashscope.aliyuncs.com/compatible-mode/v3";
            case "ernie"    -> "https://qianfan.baidubce.com/v2";
            case "deepseek" -> "https://api.deepseek.com";
            case "minimax"  -> "https://api.minimax.chat/v2";
            default -> "https://ark.cn-beijing.volces.com/api/v3";
        };
    }

    /**
     * 获取 API Key
     *
     * 优先级：DOUBAO_API_KEY > API_KEY > AI_API_KEY
     *
     * @return API Key，未设置时返回 null
     */
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

    /**
     * 获取 API endpoint
     *
     * 【URL 拼接说明】
     * - baseUrl 由用户配置（如 https://ark.cn-beijing.volces.com/api/coding/v3）
     * - endpoint 返回 /chat/completions
     * - 最终完整 URL = baseUrl + endpoint
     *
     * @return API endpoint 路径
     */
    private String getEndpoint() {
        return switch (provider) {
            case "ernie" -> "/chat/completions";
            default -> "/chat/completions";
        };
    }

    /**
     * 生成面试开场白
     * 根据岗位和难度级别生成定制化的欢迎语和第一个问题
     *
     * @param jobRole     面试岗位
     * @param difficulty  难度级别（1初级/2中级/3高级）
     * @return 开场白文本
     */
    @Override
    public String generateOpening(String jobRole, Integer difficulty) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig();
        String tag = runtimeConfig.provider().toUpperCase();
        log.info("[{}] 生成面试开场白, jobRole: {}, difficulty: {}", tag, jobRole, difficulty);
        String systemPrompt = buildSystemPrompt(jobRole, difficulty);
        String userPrompt = buildOpeningUserPrompt(jobRole, difficulty);
        return chat(systemPrompt, userPrompt);
    }

    /**
     * 生成面试官回复（非流式）
     * 根据对话历史和用户消息生成下一轮面试官回复
     *
     * @param sessionId   会话 ID
     * @param history     历史消息列表
     * @param userMessage 当前用户消息
     * @return 面试官回复文本
     * @throws RuntimeException AI 调用失败时抛出
     */
    @Override
    public String generateReply(String sessionId, List<ChatMessageItem> history, String userMessage) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig();
        String tag = runtimeConfig.provider().toUpperCase();
        log.info("[{}] 生成面试官回复, sessionId: {}, historySize: {}, userMessageLength: {}",
                tag, sessionId, history == null ? 0 : history.size(),
                userMessage == null ? 0 : userMessage.length());

        List<Message> messages = buildConversationMessages(history, userMessage, null);

        try {
            return chatWithMessages(messages);
        } catch (Exception e) {
            log.error("[{}] 生成回复失败, sessionId: {}", tag, sessionId, e);
            throw new RuntimeException("AI 面试回复生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成面试官回复（流式）
     *
     * 【核心设计：逐条 JSON 解析，不累积事件块】
     *
     * 旧方案的问题：
     * - 旧代码使用 accumulateSSEEvent() 按 '\n' 累积事件块后再统一解析
     * - 当 bodyToFlux(String.class) 按 '\n' 分割时，单个 SSE 事件块（以 '\n\n' 分隔）
     *   可能被切分成多行，导致累积逻辑混乱
     *
     * 新方案（逐条解析）：
     * - bodyToFlux(String.class) 按 '\n' 分割，每行一个 String
     * - 每行独立判断：是否为 data: 行？是否为 [DONE]？是否为注释行？
     * - 每个有效 data: 行独立解析 JSON，提取 delta.content 和 delta.reasoning_content
     * - 只将 delta.content 非空片段通过 sink.next() 发给下游
     * - delta.reasoning_content 只写调试日志，不参与输出
     *
     * 【字段语义说明】
     * - delta.content：普通模型的正式输出字段，发送给前端并落库
     * - delta.reasoning_content：Reasoning/R1 模型的思维链，仅记录调试日志，不输出
     *
     * 【统计报告】
     * 流结束时（sink.complete()）输出最终统计：
     * - model、总 chunk 数、content 非空 chunk 数、reasoning 非空 chunk 数
     * - 实际发给下游的 chunk 数、最终 content 长度
     * - 是否成功（如果 content 非空 chunk 数 > 0 则成功，否则失败）
     *
     * @param sessionId   会话 ID
     * @param history     历史消息列表
     * @param userMessage 当前用户消息
     * @return Publisher<String> 逐条输出的文本片段流
     */
    @Override
    public Publisher<String> generateReplyStream(String sessionId, List<ChatMessageItem> history, String userMessage) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig();
        String tag = runtimeConfig.provider().toUpperCase();
        // 【版本指纹】全项目唯一字符串，用于确认运行时代码是否为最新版本
        log.info("[{}] ═══════════════════════════════════════════════", tag);
        log.info("[{}] ║  新版逐条JSON解析V3已生效  ║", tag);
        log.info("[{}] ║  兼容: data:{...} 和純JSON  ║", tag);
        log.info("[{}] ═══════════════════════════════════════════════", tag);
        log.info("[{}] 流式生成面试官回复, sessionId: {}, historySize: {}",
                tag, sessionId, history == null ? 0 : history.size());

        List<Message> messages = buildConversationMessages(history, userMessage, null);

        String apiKey = runtimeConfig.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未找到可用的面试 AI 密钥，请检查管理端激活配置或环境变量");
        }

        log.info("[{}] 流式请求地址: {}{}, model: {}, source: {}",
                tag, runtimeConfig.baseUrl(), runtimeConfig.endpoint(), runtimeConfig.model(), runtimeConfig.source());

        StreamRequestBody reqBody = new StreamRequestBody(runtimeConfig.model(), messages, true);
        // 根据配置和模型支持情况设置 thinking 参数
        reqBody.thinking = buildThinkingConfig(runtimeConfig.model(), thinkingMode);

        // 打印完整请求体 JSON
        try {
            String requestJson = objectMapper.writeValueAsString(reqBody);
            log.info("[{}] 请求体JSON: {}", tag, requestJson);
        } catch (Exception e) {
            log.warn("[{}] 请求体序列化失败", tag, e);
        }

        // 【关键日志】打印请求参数
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

        // 【第一层】WebClient 发起请求，返回原始 SSE 行流（按 '\n' 分割）
        // 运行时按当前生效配置构造客户端，确保管理端切换后立即生效。
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

        // 【统计计数器】
        AtomicInteger totalLines = new AtomicInteger(0);
        AtomicInteger parsedJsonLines = new AtomicInteger(0);
        AtomicInteger contentChunkCount = new AtomicInteger(0);
        AtomicInteger reasoningChunkCount = new AtomicInteger(0);
        AtomicInteger emittedCount = new AtomicInteger(0);
        AtomicInteger parseErrorCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);
        // 记录首个 content chunk 到达时间（用于判断流是否正常开始）
        AtomicBoolean firstContentArrived = new AtomicBoolean(false);

        // 【第三层】Flux.create() + 匿名 Subscriber 逐行处理
        final String logTag = tag;
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

                        // 步骤1：null / blank → 跳过
                        if (rawLine == null || rawLine.isBlank()) {
                            if (streamDebugLog) log.debug("[{}] 【跳过-空白行】lineNo={}", logTag, lineNo);
                            skippedCount.incrementAndGet();
                            upstream.request(1);
                            return;
                        }

                        // 步骤2：以 ":" 开头 → SSE 注释行（如 ": ping"）
                        if (rawLine.startsWith(":")) {
                            isComment = true;
                            if (streamDebugLog) log.debug("[{}] 【跳过-注释行】lineNo={}", logTag, lineNo);
                            skippedCount.incrementAndGet();
                            upstream.request(1);
                            return;
                        }

                        // 步骤3：等于 "[DONE]" → SSE 结束标记
                        if ("[DONE]".equals(rawLine.trim())) {
                            isDone = true;
                            if (streamDebugLog) log.debug("[{}] 【收到DONE】lineNo={}", logTag, lineNo);
                            upstream.request(1);
                            return;
                        }

                        // 步骤4：以 "data:" 开头 → 标准 SSE 格式，去掉前缀取 JSON
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
                        }
                        // 步骤5：不以 "data:" 开头，但以 "{" 开头 → 纯 JSON 格式
                        else if (rawLine.trim().startsWith("{")) {
                            isPureJson = true;
                            normalizedJson = rawLine.trim();
                        }
                        // 步骤6：其余情况 → 未知格式，跳过
                        else {
                            if (streamDebugLog) log.debug("[{}] 【跳过-未知格式】lineNo={}, preview={}", logTag, lineNo,
                                        rawLine.length() > 80 ? rawLine.substring(0, 80) : rawLine);
                            skippedCount.incrementAndGet();
                            upstream.request(1);
                            return;
                        }

                        // 【JSON 解析步骤】
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

                        // 提取 delta
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

                        // 【核心提取】分别检查 content 和 reasoning_content
                        JsonNode contentNode = delta.path("content");
                        JsonNode reasoningNode = delta.path("reasoning_content");

                        boolean hasContent = contentNode.isTextual() && !contentNode.asText().isBlank();
                        boolean hasReasoning = reasoningNode.isTextual() && !reasoningNode.asText().isBlank();

                        if (hasContent) contentChunkCount.incrementAndGet();
                        if (hasReasoning) reasoningChunkCount.incrementAndGet();

                        // 【决策步骤】reasoning_content 只写调试日志（if streamDebugLog），content 才发给下游
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
                            // 【关键日志】首个 content 到达时才打印 INFO（表示流正常开始）
                            if (firstContentArrived.compareAndSet(false, true)) {
                                log.info("[{}] 【首个content到达】lineNo={}, preview={}", logTag, lineNo, contentText);
                            }
                            // 详细逐行发射日志（仅在开启调试时打印）
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

                        // 【每行汇总日志】仅在开启调试时打印
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
                    sink.error(t);
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
     * 根据统计结果生成文字结论
     *
     * @param cNonEmpty   delta.content 非空 chunk 数
     * @param rNonEmpty   delta.reasoning_content 非空 chunk 数
     * @param parseErrors JSON 解析失败次数
     * @param parsed      成功解析为 JSON 的行数
     * @param total      总接收行数
     * @return 结论描述
     */
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

    /**
     * 生成面试评价报告（旧版兼容，返回字符串JSON）
     * 【注意】已废弃，请使用 generateEvaluationReport 方法
     *
     * @param sessionId 会话 ID
     * @param history   历史消息列表
     * @return 评价结果（包含分数和报告）
     * @deprecated 请使用 generateEvaluationReport 方法
     */
    @Override
    @Deprecated
    public EvaluationResult generateEvaluation(String sessionId, List<ChatMessageItem> history) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig();
        String tag = runtimeConfig.provider().toUpperCase();
        log.info("[{}] 调用旧版评价接口, sessionId: {}, historySize: {}",
                tag, sessionId, history == null ? 0 : history.size());
        // 调用新版方法并转换为旧版格式
        InterviewEvaluationReport report = generateEvaluationReport(sessionId, history, "软件工程师", 2, "normal");
        try {
            String jsonReport = objectMapper.writeValueAsString(report);
            return new EvaluationResult(report.getOverallScore(), jsonReport);
        } catch (Exception e) {
            log.error("[{}] 序列化评价报告失败", tag, e);
            throw new RuntimeException("评价报告序列化失败", e);
        }
    }

    /**
     * 生成面试评价报告（新版，返回结构化对象）
     *
     * 【核心实现】
     * 1. 构建严格的大厂标准评价提示词
     * 2. 传入完整面试对话历史
     * 3. 要求 AI 按指定 JSON 格式返回
     * 4. 解析 AI 返回并构建结构化对象
     *
     * @param sessionId     会话 ID
     * @param history       历史消息列表
     * @param jobRole       面试岗位
     * @param difficulty    难度级别
     * @param interviewMode 面试模式
     * @return 结构化评价报告
     */
    @Override
    public InterviewEvaluationReport generateEvaluationReport(
            String sessionId,
            List<ChatMessageItem> history,
            String jobRole,
            Integer difficulty,
            String interviewMode
    ) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig();
        String tag = runtimeConfig.provider().toUpperCase();
        log.info("[{}] ═══════════════════════════════════════════════", tag);
        log.info("[{}] ║  开始生成 AI 面试评价报告  ║", tag);
        log.info("[{}] ═══════════════════════════════════════════════", tag);
        log.info("[{}] sessionId: {}, jobRole: {}, difficulty: {}, mode: {}, historySize: {}",
                tag, sessionId, jobRole, difficulty, interviewMode,
                history == null ? 0 : history.size());

        // 1. 构建评价提示词和对话上下文
        String systemPrompt = buildEvaluationSystemPrompt(jobRole, difficulty, interviewMode);
        String userPrompt = buildEvaluationUserPrompt(history);

        // 2. 调用 AI 生成评价
        String aiResponse = chat(systemPrompt, userPrompt);
        log.info("[{}] AI 评价原始响应长度: {}", tag, aiResponse == null ? 0 : aiResponse.length());

        // 3. 解析 AI 返回的 JSON
        InterviewEvaluationReport report = parseEvaluationResponse(aiResponse);

        // 4. 计算综合分数（从各维度平均得出）
        calculateOverallScore(report);

        // 5. 兼容旧版字段映射
        mapLegacyFields(report);

        log.info("[{}] 评价报告生成完成, overallScore: {}, hireRecommendation: {}",
                tag, report.getOverallScore(), report.getHireRecommendation());

        return report;
    }

    /**
     * 构建评价系统提示词
     *
     * 【评价标准】
     * - 严格按照大厂（字节/阿里/腾讯/美团）招聘标准
     * - 不做鼓励式表扬，实事求是
     * - 直接指出基础不扎实、答非所问、项目不清等问题
     * - 评分宁可保守，不要虚高
     */
    private String buildEvaluationSystemPrompt(String jobRole, Integer difficulty, String interviewMode) {
        String difficultyDesc = switch (difficulty == null ? 2 : difficulty) {
            case 1 -> "初级（1-3年经验）";
            case 3 -> "高级（5年以上经验）";
            default -> "中级（3-5年经验）";
        };
        String modeDesc = "stress".equalsIgnoreCase(interviewMode) ? "压力面试" : "普通面试";

        return """
                你是一位经验丰富的大厂技术面试官，擅长严格评估候选人的真实水平。

                【评估背景】
                - 面试岗位：%s
                - 难度级别：%s
                - 面试模式：%s

                【评价原则（必须严格遵守）】
                1. 严格标准：按照字节/阿里/腾讯/美团等一线大厂的真实招聘标准评估
                2. 实事求是：不做鼓励式表扬，不因为回答篇幅长而加分
                3. 直接尖锐：若发现基础不扎实、答非所问、项目描述不清、逻辑混乱、缺乏深度，必须直接指出，不要含糊其辞
                4. 保守评分：评分宁可偏低，不要虚高。60分以下表示未达到录用门槛
                5. 录用决策：以真实招聘视角判断是否建议进入下一轮，不要放水

                【评分标准（0-100分）】
                - 90-100：S级，远超预期，可直接录用
                - 80-89：A级，表现优秀，强烈推荐进入下一轮
                - 70-79：B级，基本达标，可考虑进入下一轮
                - 60-69：C级，勉强及格，需要综合考量
                - 0-59：D级，未达到录用标准，建议淘汰

                【输出要求】
                请严格按照以下 JSON 格式输出，不要包含任何其他文本说明：
                {
                  "overallScore": 综合评分0-100,
                  "level": "S/A/B/C/D",
                  "finalVerdict": "最终结论一句话",
                  "summary": "总体评价200字以内",
                  "strengths": ["优势1", "优势2"],
                  "weaknesses": ["短板1", "短板2"],
                  "criticalIssues": ["严重问题1", "严重问题2"],
                  "questionPerformance": [
                    {
                      "question": "面试官问题",
                      "answer": "候选人回答",
                      "score": 0-100,
                      "comment": "评价",
                      "knowledgeTags": ["知识点1", "知识点2"]
                    }
                  ],
                  "technicalDepth": {"score": 0-100, "comment": "评价"},
                  "communication": {"score": 0-100, "comment": "评价"},
                  "problemSolving": {"score": 0-100, "comment": "评价"},
                  "pressureResistance": {"score": 0-100, "comment": "评价"},
                  "jobMatch": {"score": 0-100, "comment": "评价"},
                  "hireRecommendation": "强烈推荐/推荐/待定/不推荐",
                  "improvementSuggestions": ["建议1", "建议2"],
                  "redFlags": ["红旗警示1", "红旗警示2"],
                  "missingCompetencies": ["缺失能力1", "缺失能力2"],
                  "inflationRisk": "低/中/高 - 说明",
                  "answerAuthenticity": "可信/存疑/不可信 - 说明",
                  "interviewPerformanceTags": ["标签1", "标签2"],
                  "passProbability": 0-100,
                  "rejectionReasons": ["拒录理由1", "拒录理由2"]
                }

                注意：
                - 所有字段必须返回，无内容时返回空数组或空字符串
                - level 根据 overallScore 自动判定：>=90为S，>=80为A，>=70为B，>=60为C，<60为D
                - hireRecommendation：>=80强烈推荐，>=70推荐，>=60待定，<60不推荐
                - passProbability：与overallScore保持一致
                - questionPerformance 至少记录前3轮对话的表现
                """.formatted(jobRole, difficultyDesc, modeDesc);
    }

    /**
     * 构建评价用户提示词（包含完整对话历史）
     */
    private String buildEvaluationUserPrompt(List<ChatMessageItem> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是完整的面试对话记录，请根据上述标准进行严格评估：\n\n");

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

    /**
     * 解析 AI 返回的评价报告 JSON
     *
     * 【容错处理】
     * - AI 可能在 JSON 前后添加额外文本
     * - 尝试提取第一个 { 到最后一个 } 之间的内容
     * - 解析失败时返回默认报告
     */
    private InterviewEvaluationReport parseEvaluationResponse(String aiResponse) {
        String tag = provider.toUpperCase();
        if (aiResponse == null || aiResponse.isBlank()) {
            log.warn("[{}] AI 返回空响应，使用默认评价报告", tag);
            return buildDefaultEvaluationReport();
        }

        // 尝试提取 JSON 部分（去除 AI 可能添加的额外文本）
        String jsonContent = extractJsonFromResponse(aiResponse);
        log.debug("[{}] 提取的 JSON 内容长度: {}", tag, jsonContent.length());

        try {
            InterviewEvaluationReport report = objectMapper.readValue(jsonContent, InterviewEvaluationReport.class);
            log.info("[{}] 评价报告 JSON 解析成功", tag);
            return report;
        } catch (Exception e) {
            log.error("[{}] 评价报告 JSON 解析失败，使用默认报告: {}", tag, e.getMessage());
            log.debug("[{}] 解析失败的 JSON 内容: {}", tag, jsonContent);
            return buildDefaultEvaluationReport();
        }
    }

    /**
     * 从 AI 响应中提取 JSON 内容
     */
    private String extractJsonFromResponse(String response) {
        // 找到第一个 { 和最后一个 }
        int firstBrace = response.indexOf('{');
        int lastBrace = response.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return response.substring(firstBrace, lastBrace + 1);
        }
        return response;
    }

    /**
     * 构建默认评价报告（当 AI 调用失败时使用）
     */
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

    /**
     * 计算综合分数（从各维度平均得出）
     */
    private void calculateOverallScore(InterviewEvaluationReport report) {
        if (report.getOverallScore() != null && report.getOverallScore() > 0) {
            // AI 已返回总分，直接使用
            return;
        }

        // 从各维度计算平均分
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

        // 确保 level 字段正确
        if (report.getLevel() == null || report.getLevel().isBlank()) {
            int score = report.getOverallScore() != null ? report.getOverallScore() : 60;
            if (score >= 90) report.setLevel("S");
            else if (score >= 80) report.setLevel("A");
            else if (score >= 70) report.setLevel("B");
            else if (score >= 60) report.setLevel("C");
            else report.setLevel("D");
        }
    }

    /**
     * 映射旧版前端字段（兼容处理）
     */
    private void mapLegacyFields(InterviewEvaluationReport report) {
        // 旧版 dimensions 字段
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

        // 旧版 suggestions 字段 = improvementSuggestions
        report.setSuggestions(new ArrayList<>(report.getImprovementSuggestions()));

        // 旧版 improvements 字段 = weaknesses
        report.setImprovements(new ArrayList<>(report.getWeaknesses()));
    }

    /**
     * 单次非流式对话（用于开场白等简单场景）
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return AI 返回的文本
     * @throws IllegalStateException API Key 未设置时抛出
     * @throws RuntimeException AI 调用失败时抛出
     */
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
        // 根据配置和模型支持情况设置 thinking 参数
        request.thinking = buildThinkingConfig(runtimeConfig.model(), thinkingMode);

        try {
            // 【关键日志】打印请求参数
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

    /**
     * 多轮对话（用于上下文对话场景）
     *
     * @param messages 完整的消息列表（包含历史）
     * @return AI 返回的文本
     * @throws IllegalStateException API Key 未设置时抛出
     * @throws RuntimeException AI 调用失败时抛出
     */
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
        // 根据配置和模型支持情况设置 thinking 参数
        request.thinking = buildThinkingConfig(runtimeConfig.model(), thinkingMode);

        try {
            // 【关键日志】打印请求参数
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

    /**
     * 构建对话消息列表
     * 将历史消息转换为 AI API 所需的格式
     *
     * 【修复说明】
     * 旧实现：把所有历史拼接成一个大字符串放在单条 user 消息里 → AI 无法正确理解多轮对话
     * 新实现：按时间顺序构建完整的多轮对话历史 → AI 具备完整上下文记忆
     *
     * @param history           历史消息列表
     * @param currentUserMessage 当前用户消息
     * @param jobRole           面试岗位（可选）
     * @return 格式化后的消息列表
     */
    private List<Message> buildConversationMessages(List<ChatMessageItem> history, String currentUserMessage, String jobRole) {
        java.util.List<Message> messages = new java.util.ArrayList<>();

        // 1. 添加系统提示词（从历史中推断岗位，或使用默认值）
        String systemPrompt = buildSystemPromptFromJobRole(history);
        messages.add(new Message("system", systemPrompt));

        int historyUserCount = 0;
        int historyAssistantCount = 0;

        // 2. 添加历史对话消息（按时间顺序）
        if (history != null && !history.isEmpty()) {
            for (ChatMessageItem item : history) {
                String role = item.role();
                String content = item.content();
                if (content == null || content.isBlank()) {
                    continue;
                }
                // 角色映射：确保符合 OpenAI 格式要求
                String mappedRole = "user".equalsIgnoreCase(role) ? "user" : "assistant";
                messages.add(new Message(mappedRole, content));
                if ("user".equalsIgnoreCase(mappedRole)) {
                    historyUserCount++;
                } else {
                    historyAssistantCount++;
                }
            }
        }

        // 3. 添加当前用户消息
        if (currentUserMessage != null && !currentUserMessage.isBlank()) {
            messages.add(new Message("user", currentUserMessage));
        }

        // 【关键调试日志】输出消息组装情况，确认上下文是否正确
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
     * 从历史消息中推断岗位并构建系统提示词
     *
     * @param history 历史消息列表
     * @return 系统提示词
     */
    private String buildSystemPromptFromJobRole(List<ChatMessageItem> history) {
        String jobRole = "软件工程师";
        if (history != null && !history.isEmpty()) {
            String first = history.get(0).content();
            int idx = first.indexOf("岗位");
            if (idx >= 0) {
                int start = idx;
                int end = Math.min(start + 50, first.length());
                jobRole = first.substring(Math.max(0, start - 10), end);
            }
        }
        return buildSystemPrompt(jobRole, 2);
    }

    /**
     * 构建面试官系统提示词
     *
     * @param jobRole    面试岗位
     * @param difficulty 难度级别
     * @return 完整的系统提示词
     */
    private String buildSystemPrompt(String jobRole, Integer difficulty) {
        String difficultyDesc = switch (difficulty == null ? 2 : difficulty) {
            case 1 -> "初级（1-3年经验）";
            case 3 -> "高级（5年以上经验）";
            default -> "中级（3-5年经验）";
        };

        return """
                你是一位专业、友好、经验丰富的面试官，正在对候选人进行模拟面试。

                角色设定：
                - 你是一名专业的技术面试官，擅长考察候选人的技术能力、项目经验和问题解决能力
                - 面试岗位：%s
                - 难度级别：%s
                - 你会逐步提出有针对性的问题，并根据候选人的回答进行深入追问

                沟通要求：
                - 使用简洁、专业的语言
                - 每次回复控制在100字以内，聚焦在一个问题上
                - 不要一次性问太多问题
                - 如果候选人的回答很好，可以适当给予肯定并继续追问
                - 如果回答不够清晰，要温和但专业地要求对方进一步解释

                当前面试模式：
                - 用户说一句话，你回复一句（单轮对话模式）
                - 不要做开场白，直接开始提问第一个问题
                """.formatted(jobRole, difficultyDesc);
    }

    /**
     * 构建开场白的用户提示词
     *
     * @param jobRole    面试岗位
     * @param difficulty 难度级别
     * @return 用户提示词
     */
    private String buildOpeningUserPrompt(String jobRole, Integer difficulty) {
        return "请开始面试，岗位是" + jobRole + "，难度级别为" +
                (difficulty == null ? 2 : difficulty) + "。请先做一个简短的自我介绍，然后提出第一个技术问题。";
    }

/**
     * 解析面试业务运行时 AI 配置。
     *
     * 作用：
     * 1. 优先读取管理端"当前激活"的 interview 配置，确保切换后立即生效；
     * 2. 当数据库配置缺失字段时，回退到 application.yml/环境变量，保证服务可用。
     *
     * 【运行时配置读取优先级】（确保 403 问题不再复现）：
     * 1. 优先读取数据库激活配置的 apiKey（可能是 SiliconFlow 等新 provider）
     * 2. 兜底读取本地环境变量的 apiKey
     * 3. 最后兜底到本地配置 model（避免空值导致请求失败）
     *
     * 【关键修复】对 runtimeApiKey 补充兜底处理：
     * - 旧逻辑：仅当数据库返回非空时才覆盖，不为空时保持 null，导致后续请求带空 key
     * - 新逻辑：始终确保 runtimeApiKey 不为 null，防止 403 Forbidden
     */
    private RuntimeAiConfig resolveRuntimeConfig() {
        // 先准备本地兜底配置，避免数据库不可用时影响核心链路。
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

        // 尝试读取 interview 业务的激活配置；读取失败时继续使用本地兜底。
        SysAiEngineConfig activeConfig = null;
        try {
            activeConfig = sysAiEngineConfigService.getActiveByBusinessType(AiEngineConstants.BUSINESS_TYPE_INTERVIEW);
        } catch (Exception e) {
            log.warn("读取面试业务激活 AI 配置失败，回退本地配置: {}", e.getMessage());
        }

        if (activeConfig != null) {
            // 使用管理端激活配置覆盖运行时参数，确保切换配置后立即生效。
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
            // 【关键修复】只要数据库返回的 apiKey 非空就使用，否则继续使用本地兜底
            if (dbApiKey != null) {
                runtimeApiKey = dbApiKey;
                // 【调试】打印数据库读取到的 apiKey 状态
                log.info("[DEBUG] 从数据库读取到 apiKey, 长度: {}, 前5位: {}",
                        dbApiKey.length(), dbApiKey.substring(0, Math.min(5, dbApiKey.length())));
            } else {
                // 数据库 apiKey 为空时，保持使用本地兜底，不要覆盖为 null
                log.warn("数据库 apiKey 为空，使用本地兜底");
            }
            source = "db-active:" + activeConfig.getEngineCode();
        }

        // 【关键修复】对关键字段做最后兜底，确保不传空值导致 403
        // 1. model 最后兜底
        if (runtimeModel == null) {
            runtimeModel = fallbackModel;
        }
        // 2. baseUrl 最后兜底
        if (runtimeBaseUrl == null) {
            runtimeBaseUrl = fallbackBaseUrl;
        }
        // 3. apiKey 最后兜底（最重要，防止 403）
        if (runtimeApiKey == null || runtimeApiKey.isBlank()) {
            log.warn("[INTERVIEW] runtimeApiKey 仍为空，尝试从环境变量兜底获取");
            runtimeApiKey = getApiKey();
        }
        // 4. 如果所有兜底都失败，当前 provider 不可用（抛出明确错误而非静默失败）
        if (runtimeApiKey == null || runtimeApiKey.isBlank()) {
            throw new IllegalStateException("面试 AI 密钥不可用：数据库和管理端均无有效配置。"
                    + "请在管理端激活 AI 引擎配置，或设置环境变量 DOUBAO_API_KEY");
        }

        // 【调试】打印最终运行时配置状态（用于排查 403 问题）
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

    /**
     * 按 provider 获取请求 endpoint。
     *
     * 说明：
     * 当前已接入供应商都兼容 /chat/completions，保留分支是为了后续扩展差异化路径。
     */
    private String getEndpointByProvider(String providerType) {
        return switch (providerType) {
            case "ernie" -> "/chat/completions";
            default -> "/chat/completions";
        };
    }

    /**
     * 统一归一化配置字符串。
     *
     * 作用：
     * 把 null、空串、纯空白统一处理为 null，降低配置分支判断复杂度。
     */
    private String normalizeConfigValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 非流式请求体
     *
     * 【thinking 参数说明】
     * - 支持 thinking 参数的模型可通过 thinking.type 控制思考模式
     * - type = "enabled": 开启深度思考
     * - type = "disabled": 关闭深度思考
     * - 该字段是否传递由配置和模型支持情况共同决定
     */
    /**
     * 面试 AI 运行时配置快照。
     * 用于把数据库激活配置与本地兜底配置统一封装后下发到单次请求。
     */
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
            // thinking 字段由 buildThinkingConfig 方法动态设置，此处不初始化
        }
    }

    /**
     * 流式请求体（包含 stream 标志和 thinking 控制）
     *
     * 【thinking 参数说明】
     * - 支持 thinking 参数的模型可通过 thinking.type 控制思考模式
     * - type = "enabled": 开启深度思考
     * - type = "disabled": 关闭深度思考
     * - 该字段是否传递由配置和模型支持情况共同决定
     */
    private static class StreamRequestBody {
        public String model;
        public List<Message> messages;
        public boolean stream = true;
        public Thinking thinking;

        public StreamRequestBody(String model, List<Message> messages, boolean stream) {
            this.model = model;
            this.messages = messages;
            this.stream = stream;
            // thinking 字段由 buildThinkingConfig 方法动态设置，此处不初始化
        }
    }

    /**
     * thinking 配置对象
     * 用于控制模型是否开启深度思考模式
     */
    private static class Thinking {
        public String type;

        public Thinking(String type) {
            this.type = type;
        }
    }

    /**
     * 单个消息对象
     */
    private static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    /**
     * 非流式响应体
     */
    private static class ResponseBody {
        public List<Choice> choices;

        public static class Choice {
            public MessageContent message;

            public static class MessageContent {
                public String content;
            }
        }
    }
}
