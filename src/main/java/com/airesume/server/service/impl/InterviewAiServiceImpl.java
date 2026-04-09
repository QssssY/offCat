package com.airesume.server.service.impl;

import com.airesume.server.service.InterviewAiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

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
    private final String provider;
    private final String model;
    private final String configuredBaseUrl;
    private final String resolvedBaseUrl;
    private final String endpoint;
    private final ObjectMapper objectMapper;
    private final String thinkingMode;

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
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        this.provider = provider == null ? "doubao" : provider.toLowerCase();
        this.model = model;
        this.configuredBaseUrl = configuredBaseUrl;
        this.thinkingMode = thinkingMode;
        this.objectMapper = objectMapper;

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
        if (key != null && !key.isBlank()) return key;
        key = System.getenv("API_KEY");
        if (key != null && !key.isBlank()) return key;
        key = System.getenv("AI_API_KEY");
        if (key != null && !key.isBlank()) return key;
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
        String tag = provider.toUpperCase();
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
        String tag = provider.toUpperCase();
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
        String tag = provider.toUpperCase();
        // 【版本指纹】全项目唯一字符串，用于确认运行时代码是否为最新版本
        log.info("[{}] ═══════════════════════════════════════════════", tag);
        log.info("[{}] ║  新版逐条JSON解析V3已生效  ║", tag);
        log.info("[{}] ║  兼容: data:{...} 和純JSON  ║", tag);
        log.info("[{}] ═══════════════════════════════════════════════", tag);
        log.info("[{}] 流式生成面试官回复, sessionId: {}, historySize: {}",
                tag, sessionId, history == null ? 0 : history.size());

        List<Message> messages = buildConversationMessages(history, userMessage, null);

        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请设置环境变量 DOUBAO_API_KEY 或 API_KEY");
        }

        log.info("[{}] 流式请求地址: {}{}, model: {}", tag, resolvedBaseUrl, endpoint, model);

        StreamRequestBody reqBody = new StreamRequestBody(model, messages, true);
        // 根据配置和模型支持情况设置 thinking 参数
        reqBody.thinking = buildThinkingConfig(model, thinkingMode);

        // 【关键日志】打印请求参数
        log.info("[{}] ═══════════════════════════════════════════════", tag);
        log.info("[{}] ║  流式请求参数验证  ║", tag);
        log.info("[{}] ═══════════════════════════════════════════════", tag);
        log.info("[{}] 请求地址: {}{}", tag, resolvedBaseUrl, endpoint);
        log.info("[{}] model: {}", tag, model);
        log.info("[{}] stream: {}", tag, reqBody.stream);
        if (reqBody.thinking != null) {
            log.info("[{}] thinking.type: {}", tag, reqBody.thinking.type);
        } else {
            log.info("[{}] thinking: 未设置", tag);
        }
        log.info("[{}] ═══════════════════════════════════════════════", tag);

        // 【第一层】WebClient 发起请求，返回原始 SSE 行流（按 '\n' 分割）
        Flux<String> rawLineFlux = webClient.post()
                .uri(endpoint)
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
                    log.info("║  模型: {}", model);
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
     * 生成面试评价报告
     * 【注意】当前尚未实现，调用会抛出 UnsupportedOperationException
     *
     * @param sessionId 会话 ID
     * @param history   历史消息列表
     * @return 评价结果（包含分数和报告）
     * @throws UnsupportedOperationException 当前未实现时抛出
     */
    @Override
    public EvaluationResult generateEvaluation(String sessionId, List<ChatMessageItem> history) {
        String tag = provider.toUpperCase();
        log.info("[{}] 生成面试评价, sessionId: {}, historySize: {}",
                tag, sessionId, history == null ? 0 : history.size());
        throw new UnsupportedOperationException("真实 AI 评价功能尚未实现");
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
        String tag = provider.toUpperCase();
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请设置环境变量 DOUBAO_API_KEY 或 API_KEY");
        }

        RequestBody request = new RequestBody();
        request.model = model;
        request.messages = List.of(
                new Message("system", systemPrompt),
                new Message("user", userPrompt)
        );
        // 根据配置和模型支持情况设置 thinking 参数
        request.thinking = buildThinkingConfig(model, thinkingMode);

        try {
            // 【关键日志】打印请求参数
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            log.info("[{}] ║  非流式请求参数验证  ║", tag);
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            log.info("[{}] 请求地址: {}{}", tag, resolvedBaseUrl, endpoint);
            log.info("[{}] model: {}", tag, model);
            if (request.thinking != null) {
                log.info("[{}] thinking.type: {}", tag, request.thinking.type);
            } else {
                log.info("[{}] thinking: 未设置", tag);
            }
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            ResponseBody response = restClient.post()
                    .uri(endpoint)
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
        String tag = provider.toUpperCase();
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请设置环境变量 DOUBAO_API_KEY 或 API_KEY");
        }

        RequestBody request = new RequestBody();
        request.model = model;
        request.messages = messages;
        // 根据配置和模型支持情况设置 thinking 参数
        request.thinking = buildThinkingConfig(model, thinkingMode);

        try {
            // 【关键日志】打印请求参数
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            log.info("[{}] ║  多轮对话请求参数验证  ║", tag);
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            log.info("[{}] 请求地址: {}{}", tag, resolvedBaseUrl, endpoint);
            log.info("[{}] model: {}", tag, model);
            if (request.thinking != null) {
                log.info("[{}] thinking.type: {}", tag, request.thinking.type);
            } else {
                log.info("[{}] thinking: 未设置", tag);
            }
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            ResponseBody response = restClient.post()
                    .uri(endpoint)
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
     * 非流式请求体
     *
     * 【thinking 参数说明】
     * - 支持 thinking 参数的模型可通过 thinking.type 控制思考模式
     * - type = "enabled": 开启深度思考
     * - type = "disabled": 关闭深度思考
     * - 该字段是否传递由配置和模型支持情况共同决定
     */
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
