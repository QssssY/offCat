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
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.net.BindException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * 豆包模拟面试 AI 服务实现类（真实 AI 模式）
 *
 * 所属模块：模拟面试模块 - AI 接入层
 * 职责：调用豆包大模型 API 生成面试官回复、开场白、评价报告
 * 激活条件：当 app.interview.mode=real 时激活
 *
 * 【重要】baseUrl 配置说明：
 * - 优先使用配置文件中的 app.interview.base-url
 * - 仅当配置为空时才使用代码默认值
 * - 当前用户配置的 baseUrl：https://ark.cn-beijing.volces.com/api/coding/v3
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
public class DoubaoInterviewAiServiceImpl implements InterviewAiService {

    private final RestClient restClient;
    private final WebClient webClient;
    private final String provider;
    private final String model;
    private final String configuredBaseUrl;
    private final String resolvedBaseUrl;
    private final String endpoint;
    private final ObjectMapper objectMapper;

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
     *
     * @param provider           AI 提供商（从配置 app.interview.provider 读取，默认 doubao）
     * @param configuredBaseUrl  API 基础地址（从配置 app.interview.base-url 读取，用户原始配置）
     * @param model             模型名称（从配置 app.interview.model 读取）
     * @param restClientBuilder RestClient 构造器
     * @param objectMapper      JSON 解析工具
     */
    public DoubaoInterviewAiServiceImpl(
            @Value("${app.interview.provider:doubao}") String provider,
            @Value("${app.interview.base-url:}") String configuredBaseUrl,
            @Value("${app.interview.model:}") String model,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        this.provider = provider == null ? "doubao" : provider.toLowerCase();
        this.model = model;
        this.configuredBaseUrl = configuredBaseUrl;
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
        log.info("============================================================");
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
        log.info("[INTERVIEW-REAL] 生成面试开场白, jobRole: {}, difficulty: {}", jobRole, difficulty);
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
        log.info("[INTERVIEW-REAL] 生成面试官回复, sessionId: {}, historySize: {}, userMessageLength: {}",
                sessionId, history == null ? 0 : history.size(),
                userMessage == null ? 0 : userMessage.length());

        List<Message> messages = buildConversationMessages(history, userMessage, null);

        try {
            return chatWithMessages(messages);
        } catch (Exception e) {
            log.error("[INTERVIEW-REAL] 生成回复失败, sessionId: {}", sessionId, e);
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
        // 【版本指纹】全项目唯一字符串，用于确认运行时代码是否为最新版本
        log.info("[INTERVIEW-REAL] ═══════════════════════════════════════════════");
        log.info("[INTERVIEW-REAL] ║  新版逐条JSON解析V3已生效  ║");
        log.info("[INTERVIEW-REAL] ║  兼容: data:{...} 和純JSON  ║");
        log.info("[INTERVIEW-REAL] ═══════════════════════════════════════════════");
        log.info("[INTERVIEW-REAL] 流式生成面试官回复, sessionId: {}, historySize: {}",
                sessionId, history == null ? 0 : history.size());

        List<Message> messages = buildConversationMessages(history, userMessage, null);

        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请设置环境变量 DOUBAO_API_KEY 或 API_KEY");
        }

        log.info("[INTERVIEW-REAL] 流式请求地址: {}{}, model: {}", resolvedBaseUrl, endpoint, model);

        StreamRequestBody reqBody = new StreamRequestBody(model, messages, true);

        // 【第一层】WebClient 发起请求，返回原始 SSE 行流（按 '\n' 分割）
        Flux<String> rawLineFlux = webClient.post()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(reqBody)
                .retrieve()
                .bodyToFlux(String.class)
                .publishOn(Schedulers.boundedElastic());

        // 【统计计数器】
        AtomicInteger totalLines = new AtomicInteger(0);       // 总行数
        AtomicInteger parsedJsonLines = new AtomicInteger(0);// 成功解析为 JSON 的行数
        AtomicInteger contentChunkCount = new AtomicInteger(0);// delta.content 非空 chunk 数
        AtomicInteger reasoningChunkCount = new AtomicInteger(0); // delta.reasoning_content 非空 chunk 数
        AtomicInteger emittedCount = new AtomicInteger(0);     // 实际 sink.next() 发射次数
        AtomicInteger parseErrorCount = new AtomicInteger(0); // JSON 解析失败次数
        AtomicInteger skippedCount = new AtomicInteger(0);    // 跳过的行数

        // 【第三层】Flux.create() + 匿名 Subscriber 逐行处理
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

                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        // 【规范化步骤】将原始行转为可解析的 JSON 字符串
                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        String normalizedJson = null;
                        boolean isComment = false;
                        boolean isDone = false;
                        boolean isDataPrefix = false;
                        boolean isPureJson = false;

                        // 步骤1：null / blank → 跳过
                        if (rawLine == null || rawLine.isBlank()) {
                            log.debug("[INTERVIEW-REAL] 【跳过-空白行】lineNo={}", lineNo);
                            skippedCount.incrementAndGet();
                            upstream.request(1);
                            return;
                        }

                        // 步骤2：以 ":" 开头 → SSE 注释行（如 ": ping"）
                        if (rawLine.startsWith(":")) {
                            isComment = true;
                            log.debug("[INTERVIEW-REAL] 【跳过-注释行】lineNo={}, content={}", lineNo, rawLine);
                            skippedCount.incrementAndGet();
                            upstream.request(1);
                            return;
                        }

                        // 步骤3：等于 "[DONE]" → SSE 结束标记
                        if ("[DONE]".equals(rawLine.trim())) {
                            isDone = true;
                            log.info("[INTERVIEW-REAL] 【收到DONE】lineNo={}", lineNo);
                            upstream.request(1);
                            return;
                        }

                        // 步骤4：以 "data:" 开头 → 标准 SSE 格式，去掉前缀取 JSON
                        if (rawLine.startsWith("data:")) {
                            isDataPrefix = true;
                            String json = rawLine.substring("data:".length()).trim();
                            if ("[DONE]".equals(json)) {
                                isDone = true;
                                log.info("[INTERVIEW-REAL] 【收到DONE（data:前缀）】lineNo={}", lineNo);
                                upstream.request(1);
                                return;
                            }
                            normalizedJson = json;
                        }
                        // 步骤5：不以 "data:" 开头，但以 "{" 开头 → 纯 JSON 格式
                        // 这是当前豆包接口实际返回的格式
                        else if (rawLine.trim().startsWith("{")) {
                            isPureJson = true;
                            normalizedJson = rawLine.trim();
                        }
                        // 步骤6：其余情况 → 未知格式，跳过
                        else {
                            log.debug("[INTERVIEW-REAL] 【跳过-未知格式】lineNo={}, preview={}", lineNo,
                                    rawLine.length() > 80 ? rawLine.substring(0, 80) : rawLine);
                            skippedCount.incrementAndGet();
                            upstream.request(1);
                            return;
                        }

                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        // 【JSON 解析步骤】从 normalizedJson 提取 delta.content / delta.reasoning_content
                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        JsonNode root;
                        try {
                            root = objectMapper.readTree(normalizedJson);
                            parsedJsonLines.incrementAndGet();
                        } catch (Exception e) {
                            int errCount = parseErrorCount.incrementAndGet();
                            log.warn("[INTERVIEW-REAL] 【JSON解析失败】lineNo={}, preview={}, error={}, 累计错误={}",
                                    lineNo,
                                    normalizedJson.length() > 80 ? normalizedJson.substring(0, 80) : normalizedJson,
                                    e.getMessage(),
                                    errCount);
                            upstream.request(1);
                            return;
                        }

                        // 提取 delta
                        JsonNode choices = root.path("choices");
                        if (choices.isMissingNode() || !choices.isArray() || choices.isEmpty()) {
                            log.debug("[INTERVIEW-REAL] 【跳过-无choices】lineNo={}", lineNo);
                            skippedCount.incrementAndGet();
                            upstream.request(1);
                            return;
                        }

                        JsonNode delta = choices.get(0).path("delta");
                        if (delta.isMissingNode()) {
                            log.debug("[INTERVIEW-REAL] 【跳过-无delta】lineNo={}", lineNo);
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

                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        // 【决策步骤】reasoning_content 只写日志，content 才发给下游
                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        boolean emitted = false;

                        if (hasReasoning) {
                            // reasoning_content 是思维链，仅记录，禁止发给前端
                            String reasoningText = reasoningNode.asText();
                            log.info("[INTERVIEW-REAL] 【reasoning_content】lineNo={}, format={}, length={}, preview={}, 【不发给前端】",
                                    lineNo,
                                    isDataPrefix ? "SSE" : (isPureJson ? "纯JSON" : "其他"),
                                    reasoningText.length(),
                                    reasoningText.length() > 50 ? reasoningText.substring(0, 50) : reasoningText);
                        }

                        if (hasContent) {
                            // 只有 delta.content 才发给前端和落库
                            String contentText = contentNode.asText();
                            log.info("[INTERVIEW-REAL] 【◆发射】lineNo={}, format={}, content={}, length={}",
                                    lineNo,
                                    isDataPrefix ? "SSE" : (isPureJson ? "纯JSON" : "其他"),
                                    contentText,
                                    contentText.length());
                            sink.next(contentText);
                            emittedCount.incrementAndGet();
                            emitted = true;
                        }

                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        // 【每行汇总日志】完整的逐行追踪
                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        log.info("[INTERVIEW-REAL] 【行汇总】lineNo={}, format={}, hasContent={}, hasReasoning={}, emitted={}, preview={}",
                                lineNo,
                                isDataPrefix ? "SSE" : (isPureJson ? "纯JSON" : "其他"),
                                hasContent, hasReasoning, emitted,
                                (normalizedJson != null && normalizedJson.length() > 60)
                                        ? normalizedJson.substring(0, 60) : (normalizedJson != null ? normalizedJson : ""));

                        upstream.request(1);

                    } catch (Exception e) {
                        log.error("[INTERVIEW-REAL] 【处理异常】error={}, message={}", e.getClass().getSimpleName(), e.getMessage());
                        sink.error(e);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    log.error("[INTERVIEW-REAL] 【第七层-WebClient错误】type={}, message={}",
                            t.getClass().getSimpleName(), t.getMessage());
                    sink.error(t);
                }

                @Override
                public void onComplete() {
                    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    // 【第八层-最终统计报告】
                    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
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
                    log.info("║  ────────────────────────────────────────────");
                    log.info("║  总接收行数:              {}", total);
                    log.info("║  成功解析JSON的行数:     {}", parsed);
                    log.info("║  跳过的行数:             {}", skipped);
                    log.info("║  JSON解析失败次数:       {}", errors);
                    log.info("║  ────────────────────────────────────────────");
                    log.info("║  delta.content 非空chunk: {}", cCount);
                    log.info("║  reasoning_content 非空:  {}", rCount);
                    log.info("║  实际发给下游次数:       {}", emitted);
                    log.info("║  ────────────────────────────────────────────");
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
                   "当前模型可能为 Reasoning 模型（如 Doubao-Seed-2.0-pro），不适合直接输出给用户。" +
                   "建议换用 standard/instruct 模型（如 Doubao-pro-4k）";
        }
        return "UNKNOWN - 请检查日志";
    }

    /**
     * 查找异常链的根本原因
     *
     * 【用途】
     * Reactor/Netty 的异常通常包装多层，需要找到最底层的真实异常
     *
     * @param throwable 顶层异常
     * @return 根本原因异常
     */
    private Throwable findRootCause(Throwable throwable) {
        Throwable root = throwable;
        int maxDepth = 20;
        int depth = 0;
        while (root.getCause() != null && depth < maxDepth) {
            root = root.getCause();
            depth++;
        }
        return root;
    }

    /**
     * 将按行分割的 chunk 流累积为完整的 SSE 事件块，再提取 content
     *
     * 【问题背景】
     * WebClient.bodyToFlux(String.class) 按 '\n' 分割返回 Flux<String>
     * 但 SSE 事件块的边界是 '\n\n'，单个 chunk 可能包含：
     *   - 半条事件行（event: content\ndata: x）
     *   - 完整事件块（event: content\ndata: hello\n\n）
     *   - 多个事件块（event: content\ndata: a\n\ndata: b\n\n）
     *
     * 【累积逻辑】
     * - 遇到空行 '' 时：将累积的事件块作为完整事件处理，提取 content
     * - 正常行：追加到当前事件块缓冲区
     * - 跳过 ':' 开头的 ping/comment 行
     *
     * @param flux 原始行流
     * @return 提取出的 content 片段流
     */
    private Flux<String> accumulateSSEEvent(Flux<String> flux) {
        return Flux.create(sink -> {
            StringBuilder eventBuffer = new StringBuilder();

            // 【统计计数器】用于分析流式返回结构
            AtomicReference<Integer> totalChunks = new AtomicReference<>(0);         // 总 chunk 数
            AtomicReference<Integer> contentNonEmptyChunks = new AtomicReference<>(0); // content 非空 chunk 数
            AtomicReference<Integer> reasoningNonEmptyChunks = new AtomicReference<>(0); // reasoning_content 非空 chunk 数

            flux.subscribe(new Subscriber<String>() {
                private Subscription subscription;

                @Override
                public void onSubscribe(Subscription s) {
                    this.subscription = s;
                    s.request(1);
                }

                @Override
                public void onNext(String line) {
                    try {
                        // 【第三层日志】accumulateSSEEvent 内部收到原始数据
                        log.info("[INTERVIEW-REAL] 【第三层-累积收到】type={}, value={}",
                                line == null ? "null" : line.getClass().getSimpleName(),
                                line == null ? "null" : (line.length() > 80 ? line.substring(0, 80) : line));

                        if (line == null || line.isEmpty()) {
                            // 【空行】事件块结束，提取 content
                            if (eventBuffer.length() > 0) {
                                String block = eventBuffer.toString();
                                eventBuffer.setLength(0);

                                // 统计
                                analyzeChunkForStats(block, totalChunks, contentNonEmptyChunks, reasoningNonEmptyChunks);

                                // 【第三.1层日志】完整事件块
                                log.info("[INTERVIEW-REAL] 【第三.1层-完整事件块】blockLength={}, blockPreview={}",
                                        block.length(), block.length() > 100 ? block.substring(0, 100) : block);

                                // 提取 content
                                String content = extractContentFromSSEData(block);

                                // 【第三.2层日志】extractContentFromSSEData 返回值
                                log.info("[INTERVIEW-REAL] 【第三.2层-提取结果】extractContentFromSSEData 返回: {}",
                                        content == null ? "null" : ("\"" + content + "\" (length=" + content.length() + ")"));

                                if (content != null && !content.isBlank()) {
                                    // 【第三.3层日志】准备调用 sink.next
                                    log.info("[INTERVIEW-REAL] 【第三.3层-准备sink.next】extracted={}, length={}",
                                            content, content.length());
                                    sink.next(content);
                                } else {
                                    log.debug("[INTERVIEW-REAL] 【第三.3层-sink跳过】content 为空");
                                }
                            }
                        } else {
                            eventBuffer.append(line).append("\n");
                        }
                        subscription.request(1);
                    } catch (Exception e) {
                        log.error("[INTERVIEW-REAL] 【第三层-异常】处理行时出错: line={}, error={}", line, e.getMessage(), e);
                        sink.error(e);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    log.error("[INTERVIEW-REAL] SSE流错误: {}", t.getMessage(), t);
                    sink.error(t);
                }

                @Override
                public void onComplete() {
                    // 处理缓冲区中剩余的最后一个事件块
                    if (eventBuffer.length() > 0) {
                        String block = eventBuffer.toString();
                        analyzeChunkForStats(block, totalChunks, contentNonEmptyChunks, reasoningNonEmptyChunks);
                        String content = extractContentFromSSEData(block);
                        if (content != null && !content.isBlank()) {
                            sink.next(content);
                        }
                    }

                    // 【流结束统计日志】这是定位问题的关键日志
                    log.info("============================================================");
                    log.info("[INTERVIEW-REAL] 【流式统计报告】");
                    log.info("[INTERVIEW-REAL]   模型: {}", model);
                    log.info("[INTERVIEW-REAL]   总 chunk 数: {}", totalChunks.get());
                    log.info("[INTERVIEW-REAL]   delta.content 非空 chunk 数: {}", contentNonEmptyChunks.get());
                    log.info("[INTERVIEW-REAL]   delta.reasoning_content 非空 chunk 数: {}", reasoningNonEmptyChunks.get());
                    if (contentNonEmptyChunks.get() == 0 && reasoningNonEmptyChunks.get() > 0) {
                        log.error("[INTERVIEW-REAL] 【严重】所有 chunk 的 content 均为空！");
                        log.error("[INTERVIEW-REAL] 【严重】当前模型（{}）可能返回的是 Reasoning 内容而非正式回答", model);
                        log.error("[INTERVIEW-REAL] 【严重】建议：换用非 Reasoning 模型（如 Doubao-pro-4k）或改用 deepseek-r1 等");
                    }
                    log.info("============================================================");

                    sink.complete();
                }
            });
        });
    }

    /**
     * 统计 SSE 事件块中的字段分布（不影响主流程，仅用于分析）
     *
     * 【用途】
     * 在不干扰主提取流程的情况下，分析每个 chunk 的字段情况
     * 帮助判断模型返回的是 content 还是 reasoning_content
     *
     * @param block                      完整事件块
     * @param totalChunks                总 chunk 计数
     * @param contentNonEmptyChunks      content 非空计数
     * @param reasoningNonEmptyChunks    reasoning_content 非空计数
     */
    private void analyzeChunkForStats(String block,
                                      AtomicReference<Integer> totalChunks,
                                      AtomicReference<Integer> contentNonEmptyChunks,
                                      AtomicReference<Integer> reasoningNonEmptyChunks) {
        try {
            totalChunks.updateAndGet(v -> v + 1);
            String jsonLine = null;
            for (String line : block.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("data:")) {
                    String json = trimmed.substring("data:".length()).trim();
                    if (!"[DONE]".equals(json)) {
                        jsonLine = json;
                        break;
                    }
                }
            }
            if (jsonLine == null) return;
            JsonNode root = objectMapper.readTree(jsonLine);
            JsonNode choices = root.path("choices");
            if (choices.isMissingNode() || !choices.isArray() || choices.isEmpty()) return;
            JsonNode delta = choices.get(0).path("delta");
            if (delta.isMissingNode()) return;
            if (delta.path("content").isTextual() && !delta.path("content").asText().isBlank()) {
                contentNonEmptyChunks.updateAndGet(v -> v + 1);
            }
            if (delta.path("reasoning_content").isTextual() && !delta.path("reasoning_content").asText().isBlank()) {
                reasoningNonEmptyChunks.updateAndGet(v -> v + 1);
            }
        } catch (Exception e) {
            log.debug("[INTERVIEW-REAL] 统计 chunk 字段时异常: {}", e.getMessage());
        }
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
        log.info("[INTERVIEW-REAL] 生成面试评价, sessionId: {}, historySize: {}",
                sessionId, history == null ? 0 : history.size());
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

        try {
            log.debug("[INTERVIEW-REAL] 请求地址: {}{}", resolvedBaseUrl, endpoint);
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
            log.info("[INTERVIEW-REAL] AI 调用成功, responseLength: {}", result == null ? 0 : result.length());
            return result != null ? result.trim() : "";

        } catch (Exception e) {
            log.error("[INTERVIEW-REAL] AI 调用失败", e);
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
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请设置环境变量 DOUBAO_API_KEY 或 API_KEY");
        }

        RequestBody request = new RequestBody();
        request.model = model;
        request.messages = messages;

        try {
            log.debug("[INTERVIEW-REAL] 请求地址: {}{}", resolvedBaseUrl, endpoint);
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
            log.info("[INTERVIEW-REAL] AI 调用成功, responseLength: {}", result == null ? 0 : result.length());
            return result != null ? result.trim() : "";

        } catch (Exception e) {
            log.error("[INTERVIEW-REAL] AI 调用失败", e);
            throw new RuntimeException("AI 面试回复生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建对话消息列表
     * 将历史消息转换为 AI API 所需的格式
     *
     * @param history           历史消息列表
     * @param currentUserMessage 当前用户消息
     * @param jobRole           面试岗位（可选）
     * @return 格式化后的消息列表
     */
    private List<Message> buildConversationMessages(List<ChatMessageItem> history, String currentUserMessage, String jobRole) {
        StringBuilder sb = new StringBuilder();
        if (history != null) {
            for (ChatMessageItem item : history) {
                sb.append(item.role()).append(": ").append(item.content()).append("\n");
            }
        }
        sb.append("user: ").append(currentUserMessage);
        return List.of(
                new Message("system", buildSystemPromptFromJobRole(history)),
                new Message("user", sb.toString())
        );
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
     * 从 SSE 完整事件块中提取最终可见文本
     *
     * 【问题背景：Doubao-Seed-2.0-pro 是 Reasoning 模型】
     * 该模型流式输出时，同一个 chunk 的 delta 结构如下：
     *   delta.content = ""            （始终为空）
     *   delta.reasoning_content = "用户 / 现在 / 说 / 你好 / ..." （实际文本在这里）
     *
     * 如果只提取 content，会导致所有 chunk 被过滤 → 前端无任何显示
     *
     * 【提取策略】
     * 1. 优先提取 delta.content（普通模型的输出字段）
     * 2. 如果 content 为空/空白，则提取 delta.reasoning_content（Reasoning/R1 类模型的输出字段）
     * 3. 最终只展示一个字段，不能两个都展示（reasoning_content 是思维链，不应直接展示给用户）
     *
     * 【重要结论】
     * - reasoning_content = 模型的推理过程/思考链，是内部过程，不应直接展示
     * - content = 模型最终输出的正式回答，应该展示给用户
     * - 如果 content 为空但 reasoning_content 有内容，说明模型配置有问题，应该换模型
     *
     * 【正确做法】
     * 只用 content 字段；reasoning_content 无论是否有值都不发送给前端
     * 如果 content 全程为空，应该：记录日志 + 抛异常，而不是用 reasoning_content 凑数
     *
     * @param eventBlock 完整的 SSE 事件块字符串
     * @return 提取的 content，无有效内容时返回 null
     */
    private String extractContentFromSSEData(String eventBlock) {
        if (eventBlock == null || eventBlock.isBlank()) {
            return null;
        }
        try {
            // 从事件块中提取 data: 开头的行
            String jsonLine = null;
            for (String line : eventBlock.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("data:")) {
                    String json = trimmed.substring("data:".length()).trim();
                    if (!"[DONE]".equals(json)) {
                        jsonLine = json;
                        break;
                    }
                }
            }
            if (jsonLine == null || jsonLine.isBlank()) {
                log.debug("[INTERVIEW-REAL] 事件块中无有效 data JSON: {}", eventBlock);
                return null;
            }

            JsonNode root = objectMapper.readTree(jsonLine);
            JsonNode choices = root.path("choices");
            if (choices.isMissingNode() || !choices.isArray() || choices.isEmpty()) {
                log.debug("[INTERVIEW-REAL] JSON 无 choices 或为空: {}", jsonLine);
                return null;
            }
            JsonNode firstChoice = choices.get(0);
            JsonNode delta = firstChoice.path("delta");
            if (delta.isMissingNode()) {
                log.debug("[INTERVIEW-REAL] JSON choices[0] 无 delta: {}", jsonLine);
                return null;
            }

            // 【核心修复】同时检查 content 和 reasoning_content 两个字段
            // Doubao-Seed-2.0-pro 属于 Reasoning 模型，输出在 reasoning_content 字段
            JsonNode contentNode = delta.path("content");
            JsonNode reasoningNode = delta.path("reasoning_content");

            boolean hasContent = contentNode.isTextual() && !contentNode.asText().isBlank();
            boolean hasReasoning = reasoningNode.isTextual() && !reasoningNode.asText().isBlank();

            log.debug("[INTERVIEW-REAL] 【chunk 字段检查】content={} ({}), reasoning_content={} ({})",
                    hasContent, hasContent ? contentNode.asText() : "空",
                    hasReasoning, hasReasoning ? reasoningNode.asText() : "空");

            // 【调试日志】如果 content 为空但 reasoning_content 有值，打印警告
            if (!hasContent && hasReasoning) {
                log.warn("[INTERVIEW-REAL] 【警告】delta.content 为空，但 delta.reasoning_content 有值！");
                log.warn("[INTERVIEW-REAL] 【警告】这说明当前模型（{}）返回的是 Reasoning 过程文本，不是最终回答", model);
                log.warn("[INTERVIEW-REAL] 【警告】reasoning_content 内容: {}", reasoningNode.asText());
            }

            // 【关键策略】只提取 content 字段作为最终输出
            // 不使用 reasoning_content（那是思维链，不是最终回答）
            if (hasContent) {
                String text = contentNode.asText();
                return text;
            }

            // content 为空：返回 null（不降级到 reasoning_content）
            log.debug("[INTERVIEW-REAL] delta.content 为空，跳过此 chunk");
            return null;

        } catch (Exception e) {
            log.warn("[INTERVIEW-REAL] 解析 SSE 事件块失败: {}, error: {}", eventBlock, e.getMessage());
            return null;
        }
    }

    /**
     * 非流式请求体
     */
    private static class RequestBody {
        public String model;
        public List<Message> messages;
    }

    /**
     * 流式请求体（包含 stream 标志）
     */
    private static class StreamRequestBody {
        public String model;
        public List<Message> messages;
        public boolean stream = true;

        public StreamRequestBody(String model, List<Message> messages, boolean stream) {
            this.model = model;
            this.messages = messages;
            this.stream = stream;
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
