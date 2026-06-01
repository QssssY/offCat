package com.airesume.server.service.impl;

import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.common.util.PublicHttpsUrlValidator;
import com.airesume.server.config.AiTokenLimitConfig;
import com.airesume.server.dto.ai.ResolvedAiConfig;
import com.airesume.server.dto.resume.ResumeJobMatchAnalyzeResponse;
import com.airesume.server.dto.resume.ResumePolishAiResult;
import com.airesume.server.entity.SysAiEngineConfig;
import com.airesume.server.service.AiCircuitBreaker;
import com.airesume.server.service.AiCredentialCrypto;
import com.airesume.server.service.ResumeAiService;
import com.airesume.server.service.SysAiEngineConfigService;
import com.airesume.server.service.SysPromptService;
import com.airesume.server.service.UserAiConfigResolver;
import com.airesume.server.util.AiInputCompressor;
import com.airesume.server.util.TokenEstimator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("resumeAiService")
@Slf4j
@ConditionalOnProperty(name = "app.ai.mode", havingValue = "real")
public class ResumeAiServiceImpl implements ResumeAiService {

    private static final String RESUME_AI_BREAKER = "resume-ai";
    private static final BigDecimal STABLE_DIAGNOSIS_TEMPERATURE = BigDecimal.ZERO;

    private final RestClient restClient;
    private final String provider;
    private final String model;
    private final String configuredBaseUrl;
    private final String resolvedBaseUrl;
    private final String endpoint;
    private final SysPromptService sysPromptService;
    private final String thinkingMode;
    private final RestClient.Builder restClientBuilder;
    private final WebClient.Builder webClientBuilder;
    private final SysAiEngineConfigService sysAiEngineConfigService;
    private final ObjectMapper objectMapper;
    private final AiTokenLimitConfig tokenLimitConfig;
    private final AiCircuitBreaker aiCircuitBreaker;
    private final AiCredentialCrypto aiCredentialCrypto;
    private final UserAiConfigResolver userAiConfigResolver;
    private static final Pattern POLISHED_TEXT_PATTERN = Pattern.compile(
            "\"polishedResumeText\"\\s*:\\s*\"(.*?)\"\\s*,\\s*\"modificationNotes\"",
            Pattern.DOTALL);
    private static final Pattern MODIFICATION_NOTES_PATTERN = Pattern.compile(
            "\"modificationNotes\"\\s*:\\s*\\[(.*?)]",
            Pattern.DOTALL);
    private static final Pattern QUOTED_TEXT_PATTERN = Pattern.compile(
            "\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.DOTALL);
    private static final List<String> POLISH_SECTION_TITLES = List.of(
            "个人信息", "教育背景", "实习经历", "工作经历", "项目经历", "专业技能", "校园经历", "荣誉证书", "个人评价");
    private static final List<String> RESUME_START_KEYS = List.of(
            "个人信息", "教育背景", "实习经历", "工作经历", "项目经历", "专业技能", "荣誉证书", "个人评价");
    private static final Pattern POLISH_SECTION_TITLE_PATTERN = Pattern.compile(
            "(个人信息|教育背景|实习经历|工作经历|项目经历|专业技能|校园经历|荣誉证书|个人评价)(?=[:：\\s]|$)");
    private static final Pattern TRAILING_METADATA_PATTERN = Pattern.compile(
            "\\s*(?:\\(String\\)|\\(LocalDateTime\\)|\\(Integer\\)|<==\\s*Updates:|仅基于简历\\(String\\)|\\[(?=\")|\\{(?=\"))",
            Pattern.CASE_INSENSITIVE);

    public ResumeAiServiceImpl(
            @Value("${app.ai.provider:doubao}") String provider,
            @Value("${app.ai.base-url:}") String configuredBaseUrl,
            @Value("${app.ai.model:}") String model,
            @Value("${app.ai.thinking-mode:none}") String thinkingMode,
            @Autowired SysPromptService sysPromptService,
            SysAiEngineConfigService sysAiEngineConfigService,
            ObjectMapper objectMapper,
            AiTokenLimitConfig tokenLimitConfig,
            RestClient.Builder restClientBuilder,
            WebClient.Builder webClientBuilder,
            AiCircuitBreaker aiCircuitBreaker,
            AiCredentialCrypto aiCredentialCrypto) {
        this(provider, configuredBaseUrl, model, thinkingMode, sysPromptService, sysAiEngineConfigService,
                objectMapper, tokenLimitConfig, restClientBuilder, webClientBuilder, aiCircuitBreaker,
                aiCredentialCrypto, null);
    }

    @Autowired
    public ResumeAiServiceImpl(
            @Value("${app.ai.provider:doubao}") String provider,
            @Value("${app.ai.base-url:}") String configuredBaseUrl,
            @Value("${app.ai.model:}") String model,
            @Value("${app.ai.thinking-mode:none}") String thinkingMode,
            @Autowired SysPromptService sysPromptService,
            SysAiEngineConfigService sysAiEngineConfigService,
            ObjectMapper objectMapper,
            AiTokenLimitConfig tokenLimitConfig,
            RestClient.Builder restClientBuilder,
            WebClient.Builder webClientBuilder,
            AiCircuitBreaker aiCircuitBreaker,
            AiCredentialCrypto aiCredentialCrypto,
            @Autowired(required = false) UserAiConfigResolver userAiConfigResolver) {
        this.provider = provider == null ? "doubao" : provider.toLowerCase();
        this.model = model;
        this.configuredBaseUrl = configuredBaseUrl;
        this.thinkingMode = thinkingMode;
        this.sysPromptService = sysPromptService;
        this.sysAiEngineConfigService = sysAiEngineConfigService;
        this.objectMapper = objectMapper;
        this.tokenLimitConfig = tokenLimitConfig;
        this.restClientBuilder = restClientBuilder;
        this.webClientBuilder = webClientBuilder;
        this.aiCircuitBreaker = aiCircuitBreaker;
        this.aiCredentialCrypto = aiCredentialCrypto;
        this.userAiConfigResolver = userAiConfigResolver;
        this.resolvedBaseUrl = resolveBaseUrlForStartup(this.provider, configuredBaseUrl);
        this.endpoint = getEndpoint();
        this.restClient = restClientBuilder
                .baseUrl(this.resolvedBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
        String tag = this.provider.toUpperCase();
        log.info("============================================================");
        log.info("[{}] 简历诊断 AI 服务初始化", tag);
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
        if (modelName == null)
            return false;
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
            case "qwen" -> "https://dashscope.aliyuncs.com/compatible-mode/v1";
            case "ernie" -> "https://qianfan.baidubce.com/v2";
            case "deepseek" -> "https://api.deepseek.com";
            case "minimax" -> "https://api.minimax.chat/v1";
            case "mimo" -> "https://token-plan-cn.xiaomimimo.com/v1";
            default -> throw new IllegalArgumentException("未知的 AI 服务商: " + provider + "，请在管理端配置 base_url");
        };
    }

    @Override
    public String diagnose(String resumeText) {
        return diagnose(resumeText, null, false, false);
    }

    @Override
    public String diagnose(String resumeText, Long userId, boolean fallbackToPlatform, boolean requireUserCustom) {
        long startTime = System.currentTimeMillis();
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig(userId, fallbackToPlatform, requireUserCustom);
        String tag = runtimeConfig.provider().toUpperCase();
        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException("简历文本不能为空");
        }
        String apiKey = runtimeConfig.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请在管理端配置简历 AI 密钥，或设置环境变量 "
                    + getEnvKeyName(runtimeConfig.provider()) + " / API_KEY");
        }

        String compressedResume = compressResumeIfEnabled(resumeText, tag);
        try {
            DiagnosisPrompt primaryPrompt = buildDiagnosisPrompt(compressedResume, tag, false);
            String result = executeDiagnosisAttempt(runtimeConfig, apiKey, primaryPrompt, tag, 0);
            logDiagnosisSuccess(tag, startTime, primaryPrompt.version(), 0, result);
            return result;
        } catch (Exception primaryEx) {
            if (shouldRetryWithLeanPrompt(primaryEx)) {
                try {
                    DiagnosisPrompt retryPrompt = buildDiagnosisPrompt(compressedResume, tag, true);
                    String retryResult = executeDiagnosisAttempt(runtimeConfig, apiKey, retryPrompt, tag, 1);
                    logDiagnosisSuccess(tag, startTime, retryPrompt.version(), 1, retryResult);
                    return retryResult;
                } catch (Exception retryEx) {
                    logDiagnosisFailure(tag, startTime, "primary+retry", retryEx);
                    throwCustomAiFailureIfNeeded(runtimeConfig, retryEx);
                    throw new RuntimeException("AI 简历诊断失败: " + retryEx.getMessage(), retryEx);
                }
            }
            logDiagnosisFailure(tag, startTime, "primary", primaryEx);
            throwCustomAiFailureIfNeeded(runtimeConfig, primaryEx);
            throw new RuntimeException("AI 简历诊断失败: " + primaryEx.getMessage(), primaryEx);
        }
    }

    @Override
    public boolean supportsVisionExtraction() {
        return supportsVisionExtraction(null, false);
    }

    @Override
    public boolean supportsVisionExtraction(Long userId, boolean fallbackToPlatform) {
        return supportsVisionExtraction(userId, fallbackToPlatform, false);
    }

    @Override
    public boolean supportsVisionExtraction(Long userId, boolean fallbackToPlatform, boolean requireUserCustom) {
        RuntimeAiConfig userRuntimeConfig = resolveUserRuntimeConfig(userId, fallbackToPlatform, requireUserCustom);
        if (userRuntimeConfig != null) {
            return userRuntimeConfig.supportsMultimodal();
        }
        try {
            SysAiEngineConfig activeConfig = sysAiEngineConfigService
                    .getActiveByBusinessType(AiEngineConstants.BUSINESS_TYPE_RESUME);
            return activeConfig != null && Integer.valueOf(1).equals(activeConfig.getSupportsMultimodal());
        } catch (Exception e) {
            // 这里按保守策略处理，避免在配置不明时误走多模态。
            log.warn("读取简历多模态能力配置失败，默认按不支持处理: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String extractTextFromImage(String imageDataUrl, String pageHint) {
        return extractTextFromImage(imageDataUrl, pageHint, null, false);
    }

    @Override
    public String extractTextFromImage(String imageDataUrl, String pageHint, Long userId, boolean fallbackToPlatform) {
        return extractTextFromImage(imageDataUrl, pageHint, userId, fallbackToPlatform, false);
    }

    @Override
    public String extractTextFromImage(String imageDataUrl, String pageHint, Long userId,
                                       boolean fallbackToPlatform, boolean requireUserCustom) {
        if (imageDataUrl == null || imageDataUrl.isBlank()) {
            throw new IllegalArgumentException("图片内容不能为空");
        }
        if (!supportsVisionExtraction(userId, fallbackToPlatform, requireUserCustom)) {
            throw new IllegalStateException("当前简历 AI 引擎未开启多模态识别能力");
        }

        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig(userId, fallbackToPlatform, requireUserCustom);
        String apiKey = runtimeConfig.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请先配置简历 AI 密钥");
        }

        RequestBody request = new RequestBody();
        request.model = runtimeConfig.model();
        request.messages = List.of(
                new Message("system", buildVisionExtractSystemPrompt()),
                Message.userWithImage(buildVisionExtractUserPrompt(pageHint), imageDataUrl));
        request.thinking = buildThinkingConfig(runtimeConfig.model(), runtimeConfig.thinkingMode());

        // 【视觉模型超时保障】多模态识别需要更长处理时间，最低 180 秒。
        int configuredTimeout = resolveReadTimeoutMs(runtimeConfig.timeoutMs(), 180_000);
        int readTimeout = Math.max(configuredTimeout, 180_000);
        if (configuredTimeout < 180_000) {
            log.info("视觉 API 超时从 {}ms 提升至 {}ms（多模态识别最低保障）", configuredTimeout, readTimeout);
        }
        RestClient runtimeRestClient = restClientBuilder
                .baseUrl(runtimeConfig.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(buildRequestFactory(readTimeout))
                .build();

        try {
            // 使用 exchange 获取完整响应，便于捕获非 JSON 格式的错误响应。
            org.springframework.http.ResponseEntity<String> responseEntity = aiCircuitBreaker.execute(RESUME_AI_BREAKER, () -> runtimeRestClient.post()
                    .uri(runtimeConfig.endpoint())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, resp) -> {
                        String errorBody = "";
                        try {
                            errorBody = new String(resp.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        } catch (Exception readEx) {
                            log.debug("读取 API 错误响应体失败: {}", readEx.getMessage());
                        }
                        throw new RuntimeException("API 返回 HTTP " + resp.getStatusCode().value() + ": " + errorBody);
                    })
                    .toEntity(String.class));
            ResponseBody response = tryReadResponseBody(responseEntity.getBody());
            if (response == null || response.choices == null || response.choices.isEmpty()) {
                throw new RuntimeException("多模态识别返回内容为空");
            }

            String result = normalizeVisionExtractedText(
                    extractResponseText(response.choices.get(0).message.content));
            if (result.isBlank()) {
                throw new RuntimeException("多模态识别返回内容为空");
            }
            return result;
        } catch (Exception e) {
            // 检查是否为超时异常（RestClient 会将 SocketTimeoutException 包装为 ResourceAccessException）
            boolean isTimeout = false;
            Throwable cause = e;
            while (cause != null) {
                if (cause instanceof java.net.SocketTimeoutException) {
                    isTimeout = true;
                    break;
                }
                cause = cause.getCause();
            }
            if (isTimeout) {
                throw new RuntimeException("多模态识别超时（" + readTimeout / 1000 + "秒），视觉模型处理大图片需要较长时间。"
                        + "建议在管理端增大引擎超时时间，或降低 OCR DPI 配置减小图片体积", e);
            }
            throwCustomAiFailureIfNeeded(runtimeConfig, e);
            throw new RuntimeException("多模态识别失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String diagnoseJobMatch(String resumeText, String jdText) {
        return diagnoseJobMatch(resumeText, jdText, null, false);
    }

    @Override
    public String diagnoseJobMatch(String resumeText, String jdText, Long userId, boolean fallbackToPlatform) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig(userId, fallbackToPlatform, false);
        String tag = runtimeConfig.provider().toUpperCase();
        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException("简历文本不能为空");
        }
        if (jdText == null || jdText.isBlank()) {
            throw new IllegalArgumentException("岗位 JD 文本不能为空");
        }
        String apiKey = runtimeConfig.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请在管理端配置简历 AI 密钥，或设置环境变量 "
                    + getEnvKeyName(runtimeConfig.provider()) + " / API_KEY");
        }

        String systemPrompt = buildJobMatchSystemPrompt();
        String userPrompt = buildJobMatchUserPrompt(resumeText, jdText);

        int systemTokens = TokenEstimator.estimateTokens(systemPrompt);
        int userTokens = TokenEstimator.estimateTokens(userPrompt);
        log.info("[{}] JD 匹配分析调用, 预估token: {}(system:{}, user:{})",
                tag, systemTokens + userTokens, systemTokens, userTokens);

        RequestBody request = new RequestBody();
        request.model = runtimeConfig.model();
        request.messages = List.of(
                new Message("system", systemPrompt),
                new Message("user", userPrompt));
        request.thinking = buildThinkingConfig(runtimeConfig.model(), runtimeConfig.thinkingMode());
        try {
            log.info("[{}] JD 匹配分析请求, model: {}, 配置来源: {}",
                    tag, runtimeConfig.model(), runtimeConfig.source());
            RestClient.Builder builder = restClientBuilder
                    .baseUrl(runtimeConfig.baseUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            // 【超时保障】默认 5 分钟，DB 配置值取较大者，确保大型思考模型有足够响应时间。
            int readTimeout = 300_000;
            if (runtimeConfig.timeoutMs() != null && runtimeConfig.timeoutMs() > 0) {
                readTimeout = Math.max(runtimeConfig.timeoutMs(), 300_000);
            }
            SimpleClientHttpRequestFactory customFactory = new SimpleClientHttpRequestFactory();
            customFactory.setConnectTimeout(10000);
            customFactory.setReadTimeout(readTimeout);
            builder = builder.requestFactory(customFactory);
            RestClient runtimeRestClient = builder.build();
            String responseText = aiCircuitBreaker.execute(
                    RESUME_AI_BREAKER,
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
            String result = extractResponseText(response.choices.get(0).message.content);
            log.info("[{}] JD 匹配分析成功, responseLength: {}",
                    tag, result == null ? 0 : result.length());
            return extractJsonFromResponse(result);
        } catch (Exception e) {
            log.error("[{}] JD 匹配分析失败", tag, e);
            throwCustomAiFailureIfNeeded(runtimeConfig, e);
            throw new RuntimeException("AI JD 匹配分析失败: " + e.getMessage(), e);
        }
    }

    private DiagnosisPrompt buildDiagnosisPrompt(String resumeText, String tag, boolean leanRetry) {
        String promptVersion = leanRetry ? "lean-retry-v1" : "primary-v3";
        String systemPrompt = leanRetry ? getLeanDiagnosisSystemPrompt() : resolvePrimaryDiagnosisSystemPrompt(tag);
        String finalResumeText = resumeText;
        String userPrompt = leanRetry
                ? buildLeanDiagnosisUserPrompt(finalResumeText)
                : buildPrimaryDiagnosisUserPrompt(finalResumeText);

        int systemTokens = TokenEstimator.estimateTokens(systemPrompt);
        int userTokens = TokenEstimator.estimateTokens(userPrompt);
        int totalTokens = systemTokens + userTokens;
        log.info("[{}] 简历诊断请求, promptVersion: {}, resumeLength: {}, estimatedTokens: {}(system:{}, user:{})",
                tag, promptVersion, finalResumeText.length(), totalTokens, systemTokens, userTokens);

        if (tokenLimitConfig.isTokenLimitEnabled()) {
            int maxTokens = tokenLimitConfig.getResumeDiagnosisMax();
            if (totalTokens > maxTokens) {
                int resumeMaxTokens = maxTokens - systemTokens - 500;
                finalResumeText = TokenEstimator.safeTruncate(finalResumeText, Math.max(500, resumeMaxTokens));
                userPrompt = leanRetry
                        ? buildLeanDiagnosisUserPrompt(finalResumeText)
                        : buildPrimaryDiagnosisUserPrompt(finalResumeText);
                userTokens = TokenEstimator.estimateTokens(userPrompt);
                totalTokens = systemTokens + userTokens;
                log.warn("[{}] 简历诊断 prompt 超出 token 限制，已截断简历文本, promptVersion: {}, maxTokens: {}, estimatedAfterTrim: {}",
                        tag, promptVersion, maxTokens, totalTokens);
            }
        }

        return new DiagnosisPrompt(promptVersion, systemPrompt, userPrompt, totalTokens);
    }

    private String executeDiagnosisAttempt(RuntimeAiConfig runtimeConfig, String apiKey, DiagnosisPrompt prompt,
            String tag, int retryCount) {
        try {
            return callDiagnosisStream(runtimeConfig, apiKey, prompt, tag, retryCount);
        } catch (Exception streamEx) {
            if (shouldFallbackToNonStream(streamEx)) {
                log.warn("[{}] 简历诊断流式调用失败，回退非流式, promptVersion: {}, retryCount: {}, error: {}",
                        tag, prompt.version(), retryCount, streamEx.getMessage());
                return callDiagnosisNonStream(runtimeConfig, apiKey, prompt, tag, retryCount);
            }
            throw streamEx;
        }
    }

    private String callDiagnosisStream(RuntimeAiConfig runtimeConfig, String apiKey, DiagnosisPrompt prompt,
            String tag, int retryCount) {
        // 【超时保障】默认 5 分钟，确保流式响应有足够时间
        int readTimeout = resolveReadTimeoutMs(runtimeConfig.timeoutMs(), 300_000);
        StreamRequestBody request = new StreamRequestBody(runtimeConfig.model(), List.of(
                new Message("system", prompt.systemPrompt()),
                new Message("user", prompt.userPrompt())), true);
        request.thinking = buildThinkingConfig(runtimeConfig.model(), runtimeConfig.thinkingMode());
        applyStableDiagnosisOptions(request);
        logDiagnosisRequest(tag, runtimeConfig, prompt.version(), retryCount, true, readTimeout, prompt.totalTokens());

        WebClient runtimeWebClient = webClientBuilder
                .baseUrl(runtimeConfig.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().resolver(DefaultAddressResolverGroup.INSTANCE)))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Flux<String> rawLineFlux = aiCircuitBreaker.executeFlux(
                RESUME_AI_BREAKER,
                () -> runtimeWebClient.post()
                        .uri(runtimeConfig.endpoint())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToFlux(String.class)
                        .timeout(Duration.ofMillis(readTimeout)));

        StringBuilder content = new StringBuilder();
        int lineNo = 0;
        long callStartTime = System.currentTimeMillis();
        boolean firstContentLogged = false;

        for (String rawLine : rawLineFlux.toIterable()) {
            lineNo++;
            if (rawLine == null || rawLine.isBlank() || rawLine.startsWith(":")) {
                continue;
            }

            String payload = rawLine.trim();
            if (payload.startsWith("data:")) {
                payload = payload.substring("data:".length()).trim();
            }
            if ("[DONE]".equals(payload)) {
                break;
            }
            if (!payload.startsWith("{")) {
                continue;
            }

            try {
                JsonNode root = objectMapper.readTree(payload);
                String chunk = extractDiagnosisStreamContent(root);
                if (chunk == null || chunk.isBlank()) {
                    continue;
                }
                if (!firstContentLogged) {
                    firstContentLogged = true;
                    log.info("[{}] 简历诊断流式首包到达, promptVersion: {}, retryCount: {}, lineNo: {}, elapsedMs: {}",
                            tag, prompt.version(), retryCount, lineNo, System.currentTimeMillis() - callStartTime);
                }
                content.append(chunk);
            } catch (Exception parseEx) {
                throw new DiagnosisStreamException("Stream payload parse failed", parseEx);
            }
        }

        String result = extractJsonFromResponse(content.toString());
        if (result == null || result.isBlank()) {
            throw new EmptyAiResponseException("AI 流式返回内容为空");
        }
        return result;
    }

    private String callDiagnosisNonStream(RuntimeAiConfig runtimeConfig, String apiKey, DiagnosisPrompt prompt,
            String tag, int retryCount) {
        // 【超时保障】默认 5 分钟，确保非流式响应有足够时间
        int readTimeout = resolveReadTimeoutMs(runtimeConfig.timeoutMs(), 300_000);
        RequestBody request = new RequestBody();
        request.model = runtimeConfig.model();
        request.messages = List.of(
                new Message("system", prompt.systemPrompt()),
                new Message("user", prompt.userPrompt()));
        request.thinking = buildThinkingConfig(runtimeConfig.model(), runtimeConfig.thinkingMode());
        applyStableDiagnosisOptions(request);
        logDiagnosisRequest(tag, runtimeConfig, prompt.version(), retryCount, false, readTimeout, prompt.totalTokens());

        RestClient runtimeRestClient = restClientBuilder
                .baseUrl(runtimeConfig.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(buildRequestFactory(readTimeout))
                .build();

        String responseText = aiCircuitBreaker.execute(
                RESUME_AI_BREAKER,
                () -> runtimeRestClient.post()
                        .uri(runtimeConfig.endpoint())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(String.class));
        ResponseBody response = tryReadResponseBody(responseText);

        if (response == null || response.choices == null || response.choices.isEmpty()) {
            throw new EmptyAiResponseException("AI 非流式返回内容为空");
        }
        String result = extractJsonFromResponse(extractResponseText(response.choices.get(0).message.content));
        if (result == null || result.isBlank()) {
            throw new EmptyAiResponseException("AI 非流式返回内容为空");
        }
        return result;
    }

    private void applyStableDiagnosisOptions(RequestBody request) {
        // 简历诊断是评分任务，固定低随机性参数，避免同一份简历重复诊断时分数大幅漂移。
        request.temperature = STABLE_DIAGNOSIS_TEMPERATURE;
    }

    private String extractDiagnosisStreamContent(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return "";
        }
        JsonNode firstChoice = choices.get(0);
        JsonNode deltaContent = firstChoice.path("delta").path("content");
        if (deltaContent.isTextual()) {
            return deltaContent.asText("");
        }
        JsonNode messageContent = firstChoice.path("message").path("content");
        if (messageContent.isTextual()) {
            return messageContent.asText("");
        }
        return "";
    }

    private void logDiagnosisRequest(String tag, RuntimeAiConfig runtimeConfig, String promptVersion, int retryCount,
            boolean stream, int readTimeout, int estimatedTokens) {
        log.info("[{}] 简历诊断请求准备完成, promptVersion: {}, retryCount: {}, stream: {}, source: {}, model: {}, estimatedTokens: {}",
                tag, promptVersion, retryCount, stream, runtimeConfig.source(), runtimeConfig.model(), estimatedTokens);
        log.info("[{}] HTTP 超时配置: configuredTimeoutMs={}, effectiveReadTimeoutMs={}, connectTimeoutMs=10000, endpoint: {}{}",
                tag, runtimeConfig.timeoutMs(), readTimeout, runtimeConfig.baseUrl(), runtimeConfig.endpoint());
    }

    private void logDiagnosisSuccess(String tag, long startTime, String promptVersion, int retryCount, String result) {
        long elapsed = System.currentTimeMillis() - startTime;
        long elapsedSec = elapsed / 1000;
        log.info("[{}] 简历诊断成功, promptVersion: {}, retryCount: {}, responseLength: {}, totalMs: {} ({}m {}s)",
                tag, promptVersion, retryCount, result == null ? 0 : result.length(), elapsed, elapsedSec / 60, elapsedSec % 60);
    }

    private void logDiagnosisFailure(String tag, long startTime, String stage, Exception exception) {
        long elapsed = System.currentTimeMillis() - startTime;
        long elapsedSec = elapsed / 1000;
        log.error("[{}] 简历诊断失败, stage: {}, totalMs: {} ({}m {}s)",
                tag, stage, elapsed, elapsedSec / 60, elapsedSec % 60, exception);
    }

    /**
     * 多模态识别只负责看图转文本，不返回诊断结论。
     */
    private String buildVisionExtractSystemPrompt() {
        return """
                你是简历图片文字提取助手。
                任务：从简历页面图片中完整提取可读文本。
                要求：
                1. 只输出提取出的纯文本，不要解释，不要 JSON，不要 Markdown。
                2. 保留原有段落顺序，尽量保留标题、时间、公司、项目、技能等结构。
                3. 若存在中英文混排，按原文输出。
                4. 如果图片中某些内容看不清，跳过无法确认的噪声，不要编造。
                """;
    }

    /**
     * 给模型补充页码提示，便于在多页场景下稳定输出。
     */
    private String buildVisionExtractUserPrompt(String pageHint) {
        if (pageHint == null || pageHint.isBlank()) {
            return "请提取这张简历图片中的全部文字内容。";
        }
        return pageHint + "，请提取该页中的全部文字内容。";
    }

    /**
     * 兼容 OpenAI 风格多模态返回：content 可能是字符串，也可能是内容块数组。
     */
    private String executeAiRequestForText(RestClient runtimeRestClient, String endpoint, String apiKey, RequestBody request)
            throws Exception {
        String rawText = aiCircuitBreaker.execute(
                RESUME_AI_BREAKER,
                () -> runtimeRestClient.post()
                        .uri(endpoint)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(String.class));
        if (rawText == null || rawText.isBlank()) {
            throw new RuntimeException("AI 返回内容为空");
        }

        rawText = rawText.trim();
        ResponseBody response = tryReadResponseBody(rawText);
        if (response != null && response.choices != null && !response.choices.isEmpty()) {
            return extractResponseText(response.choices.get(0).message.content);
        }
        return rawText;
    }

    private ResponseBody tryReadResponseBody(String rawText) {
        try {
            return objectMapper.readValue(rawText, ResponseBody.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private String extractResponseText(Object content) {
        if (content == null) {
            return "";
        }
        if (content instanceof String text) {
            return text;
        }
        JsonNode contentNode = objectMapper.valueToTree(content);
        return extractResponseText(contentNode);
    }

    private String extractResponseText(JsonNode contentNode) {
        if (contentNode == null || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (contentNode.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode itemNode : contentNode) {
                String part = extractResponseText(itemNode);
                if (!part.isBlank()) {
                    parts.add(part);
                }
            }
            return String.join("\n", parts).trim();
        }
        if (contentNode.isObject()) {
            String directText = firstNonBlankField(contentNode, "text", "content", "value");
            if (!directText.isBlank()) {
                return directText;
            }
        }
        return contentNode.asText("");
    }

    /**
     * 模型偶尔会包裹代码块，这里统一去掉外围噪声。
     */
    private String normalizeVisionExtractedText(String rawText) {
        if (rawText == null) {
            return "";
        }
        String normalized = rawText.trim();
        if (normalized.startsWith("```text")) {
            normalized = normalized.substring(7).trim();
        } else if (normalized.startsWith("```")) {
            normalized = normalized.substring(3).trim();
        }
        if (normalized.endsWith("```")) {
            normalized = normalized.substring(0, normalized.length() - 3).trim();
        }
        return normalized;
    }

    private boolean shouldFallbackToNonStream(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof DiagnosisStreamException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean shouldRetryWithLeanPrompt(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof EmptyAiResponseException) {
                return true;
            }
            if (current instanceof java.net.SocketTimeoutException) {
                return true;
            }
            String simpleName = current.getClass().getSimpleName();
            if ("WebClientRequestException".equals(simpleName)
                    || "ReadTimeoutException".equals(simpleName)
                    || "ConnectException".equals(simpleName)) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String lowerMessage = message.toLowerCase(Locale.ROOT);
                if (lowerMessage.contains("timeout")
                        || lowerMessage.contains("connection reset")
                        || lowerMessage.contains("connection refused")
                        || lowerMessage.contains("premature close")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    static int resolveReadTimeoutMs(Integer configuredTimeoutMs, int defaultTimeoutMs) {
        return configuredTimeoutMs != null && configuredTimeoutMs > 0 ? configuredTimeoutMs : defaultTimeoutMs;
    }

    private SimpleClientHttpRequestFactory buildRequestFactory(int readTimeout) {
        SimpleClientHttpRequestFactory customFactory = new SimpleClientHttpRequestFactory();
        customFactory.setConnectTimeout(10000);
        customFactory.setReadTimeout(readTimeout);
        return customFactory;
    }

    private String getLeanDiagnosisSystemPrompt() {
        return """
                你是一位严格、直接的中文简历诊断顾问。
                只返回合法 JSON，不要解释，不要 Markdown，不要代码块。
                仅输出这些顶层字段：overallEvaluation、highlights、basicInfoEvaluation、skillEvaluation、workExperienceEvaluation、projectExperienceEvaluation、educationEvaluation、optimizationSuggestions。
                不要输出 basicInfoDetails。
                每个维度只保留 score、evaluation、strengths、weaknesses、suggestions 等核心字段。
                strengths 最多 2 条，weaknesses 最多 2 条，suggestions 最多 2 条。
                workExperienceEvaluation.experiences 和 projectExperienceEvaluation.projects 可以为空数组。
                overallEvaluation.summary 控制在 90-180 字，每个 evaluation 控制在 30-70 字。
                如果缺少信息，返回空字符串、null 或空数组，不要编造。
                """;
    }

    private String resolvePrimaryDiagnosisSystemPrompt(String tag) {
        String dbPrompt = sysPromptService.getActivePromptContent(ResumeDiagnosisConstants.SCENARIO_TYPE_RESUME);
        if (dbPrompt != null && !dbPrompt.isBlank()) {
            log.info("[{}] 使用数据库中的简历诊断 Prompt, length: {}", tag, dbPrompt.length());
            return dbPrompt;
        }
        return """
                角色：跨行业资深职业顾问。任务：严格诊断简历问题。
                原则：1)根据简历实际岗位方向评价，不预设技术岗标准 2)优点缺点都直说 3)项目必须有业务价值+量化成果 4)学历放宽但项目要硬。
                评分标准：S(90+)顶尖 A(75-89)优秀 B(60-74)合格 C(40-59)偏弱 D(<40)问题严重。多数简历应在B-C区间，仅真正出色者得A以上。
                权重：工作/实习经历30% 项目经历27% 核心技能23% 教育背景10% 个人定位5% 个人信息5%。
                跨行业原则：按岗位方向评价核心能力，不默认技术标准。与岗位无关的字段不扣分。
                规则：只返回JSON，无额外文本；JSON完整闭合；缺信息返回null/空数组；basicInfoDetails从原文提取真实值。
                得分明细规则：每个维度（basicInfoEvaluation、skillEvaluation、workExperienceEvaluation、projectExperienceEvaluation、educationEvaluation、positioningEvaluation）必须包含strengths和weaknesses数组，strengths列出2-4条加分项（做得好的具体方面），weaknesses列出2-4条扣分项（需要改进的具体方面及扣分原因），每项一句话简洁具体，不得为空数组。
                """;
    }

    private String buildPrimaryDiagnosisUserPrompt(String resumeText) {
        return """
                请对以下简历进行诊断，暴露所有问题。严格按评分标准打分，不要偏高。

                简历内容：
                """ + resumeText
                + """

                        诊断重点：
                        1.项目经历：有无量化成果？个人贡献是否明确？"负责XX开发"等空洞描述直接扣分。
                        2.工作经历：有无业务成果数据？职业成长轨迹是否清晰？
                        3.与岗位无关的字段（如非技术岗的hasGithub/hasBlog）直接填false，不扣分。
                        4.简历结构：逻辑是否清晰，有无错别字/排版问题。
                        5.每个维度的strengths列出该维度的加分项（做得好的地方），weaknesses列出扣分项（需要改进的地方），每项一句话，简洁具体。
                        6.除overallEvaluation外，每个维度必须包含evaluation字段：一段80-150字的评价文本，结构为"先说明分数由来→列出主要加分项→列出主要扣分项→给出改进建议"，语气专业客观，不要泛泛而谈。
                        7.highlights至少输出3条确定性的简历亮点，最多7条；亮点需有简历原文证据支撑，如量化成果、技术亮点、项目成就等；表达自信明确，不使用模糊措辞。
                        8.个人定位维度（positioningEvaluation）评估职业定位清晰度、核心竞争力突出程度、职业叙事连贯性与差异化亮点等。
                        9.每个维度的suggestions至少输出3条建议，最多7条；必须针对具体问题给出可执行的改进方向。
                        10.optimizationSuggestions至少输出5条核心优化建议，最多8条；按优先级排列，优先写最关键的问题。

                        summary要求：写一份300-400字的"评判官视角"总体评价，采用连贯的自然段落叙述，不使用模块编号或小标题。具体规则：
                        1.内容维度：覆盖岗位匹配度（一句话定位竞争力层级）→经历深度（实习>项目>教育优先级，无实习则提升项目权重，均无则转向教育背景）→技能可信度（是否具体到工具/场景/效果）→核心改进方向（优先级明确的2-3条建议）。
                        2.语气：客观正式，使用"该简历""候选人""呈现""缺乏"等表述，禁止"你""您的"；批评直指问题不贬损，建议用指令句式。
                        3.篇幅：总字数300-400字，1-2个自然段；实习经历分析占40%以上（若存在）。
                        岗位类型动态规则：技术岗侧重技术栈规范性；教师/文科侧重教学实习细节；市场/运营侧重活动数据；设计侧重作品集；职能岗侧重流程处理。引用简历原文作为证据，不要泛泛而谈。

                        返回JSON格式(不要额外文本)：
                        {"overallEvaluation":{"totalScore":0-100,"level":"S/A/B/C/D","summary":"按上方summary要求填写","strengths":["整体优势1","整体优势2"],"weaknesses":["整体不足1","整体不足2"]},
                        "highlights":["亮点1","亮点2","亮点3"],
                        "basicInfoEvaluation":{"score":0-100,"hasName":true/false,"hasPhone":true/false,"hasEmail":true/false,"hasGithub":true/false,"hasBlog":true/false,"evaluation":"80-150字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":["建议1","建议2","建议3"]},
                        "basicInfoDetails":{"name":"","email":"","phone":"","location":"","currentCompany":"","github":"","blog":""},
                        "skillEvaluation":{"score":0-100,"skillList":[""],"evaluation":"80-150字评价文本","strengths":[""],"weaknesses":[""],"suggestions":[""]},
                        "workExperienceEvaluation":{"score":0-100,"totalYears":0,"companyCount":0,"hasQuantifiableResults":true/false,"experiences":[{"company":"","position":"","duration":"","highlights":[""]}],"evaluation":"80-150字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":[""]},
                        "projectExperienceEvaluation":{"score":0-100,"projectCount":0,"hasTechStack":true/false,"hasResponsibilities":true/false,"projects":[{"name":"","role":"","techStack":"","highlights":[""]}],"evaluation":"80-150字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":[""]},
                        "educationEvaluation":{"score":0-100,"degree":"","school":"","major":"","hasRelevantMajor":true/false,"evaluation":"80-150字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":[""]},
                        "positioningEvaluation":{"score":0-100,"evaluation":"80-150字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":["建议1"]},
                        "optimizationSuggestions":["建议1","建议2","建议3","建议4","建议5"]}
                        """;
    }

    private String buildLeanDiagnosisUserPrompt(String resumeText) {
        return """
                请诊断以下简历，返回精简 JSON，优先指出最关键的问题和最有价值的建议。

                简历内容：
                """ + resumeText
                + """

                        返回格式：
                        {
                          "overallEvaluation":{"totalScore":0,"level":"B","summary":"","strengths":[],"weaknesses":[]},
                          "highlights":[],
                          "basicInfoEvaluation":{"score":0,"hasName":false,"hasPhone":false,"hasEmail":false,"hasGithub":false,"hasBlog":false,"evaluation":"","strengths":[],"weaknesses":[],"suggestions":[]},
                          "skillEvaluation":{"score":0,"skillList":[],"evaluation":"","strengths":[],"weaknesses":[],"suggestions":[]},
                          "workExperienceEvaluation":{"score":0,"totalYears":0,"companyCount":0,"hasQuantifiableResults":false,"experiences":[],"evaluation":"","strengths":[],"weaknesses":[],"suggestions":[]},
                          "projectExperienceEvaluation":{"score":0,"projectCount":0,"hasTechStack":false,"hasResponsibilities":false,"projects":[],"evaluation":"","strengths":[],"weaknesses":[],"suggestions":[]},
                          "educationEvaluation":{"score":0,"degree":"","school":"","major":"","hasRelevantMajor":false,"evaluation":"","strengths":[],"weaknesses":[],"suggestions":[]},
                          "positioningEvaluation":{"score":0,"evaluation":"","strengths":[],"weaknesses":[],"suggestions":[]},
                          "optimizationSuggestions":[]
                        }
                        """;
    }

    private String buildJobMatchSystemPrompt() {
        return """
                你是一位资深HR和招聘专家，精通各行业岗位要求分析和人才评估。
                请分析候选人简历与目标岗位JD的匹配程度，返回严格的JSON格式结果。

                【返回格式】
                {
                  "matchScore": 85,
                  "matchedKeywords": ["已匹配的能力点1", "已匹配的能力点2"],
                  "missingKeywords": ["缺失的能力点1", "缺失的能力点2"],
                  "suggestions": ["具体可执行的优化建议1", "具体可执行的优化建议2", "具体可执行的优化建议3"],
                  "analysisSummary": "一段话总结匹配情况"
                }

                【评分标准】
                matchScore 基于语义理解综合评分（0-100），不是简单的关键词计数：
                - 90-100：高度匹配，简历完全覆盖JD核心要求，经验深度足够
                - 75-89：较好匹配，覆盖大部分核心要求，部分能力有提升空间
                - 60-74：基本匹配，覆盖部分核心要求，存在明显能力缺口
                - 45-59：匹配度偏低，仅覆盖少量核心要求
                - 0-44：匹配度低，简历与岗位要求差距较大

                评分需综合考虑：技能匹配度、经验深度、行业相关性、项目复杂度、软技能匹配

                【分析要求】
                1. matchedKeywords：提取简历中真正体现的与岗位匹配的能力点，不是简单匹配字面关键词
                2. missingKeywords：识别JD核心要求中简历未覆盖或体现不足的能力项
                3. suggestions：针对具体缺失给出可操作的优化建议，每条建议需具体、可执行，避免泛泛而谈
                4. analysisSummary：简洁总结匹配情况，包含核心优势和主要差距，2-3句话

                【注意事项】
                - 只返回JSON，不要返回其他内容
                - matchedKeywords 和 missingKeywords 各不超过10项
                - suggestions 至少3条，不超过6条
                - 不要编造简历中没有的信息
                """;
    }

    private String buildJobMatchUserPrompt(String resumeText, String jdText) {
        StringBuilder builder = new StringBuilder();
        builder.append("【候选人简历】\n").append(resumeText).append("\n\n");
        builder.append("【目标岗位 JD】\n").append(jdText).append("\n\n");
        builder.append("请分析该简历与岗位JD的匹配程度，返回JSON格式结果。");
        return builder.toString();
    }

    private String getEnvKeyName(String providerType) {
        String normalizedProvider = normalizeConfigValue(providerType);
        if (normalizedProvider == null) {
            normalizedProvider = "doubao";
        }
        normalizedProvider = normalizedProvider.toLowerCase(Locale.ROOT);
        return switch (normalizedProvider) {
            case "doubao" -> "DOUBAO_API_KEY";
            case "qwen" -> "DASHSCOPE_API_KEY";
            case "ernie" -> "ERNIE_API_KEY";
            case "deepseek" -> "DEEPSEEK_API_KEY";
            case "minimax" -> "MINIMAX_API_KEY";
            default -> "AI_API_KEY";
        };
    }

    private String resolveApiKey(String providerType) {
        String key = System.getenv(getEnvKeyName(providerType));
        if (key != null && !key.isBlank())
            return key;
        key = System.getenv("API_KEY");
        if (key != null && !key.isBlank())
            return key;
        key = System.getenv("AI_API_KEY");
        if (key != null && !key.isBlank())
            return key;
        return null;
    }

    private String getEndpoint() {
        return switch (provider) {
            case "ernie" -> "/chat/completions";
            default -> "/chat/completions";
        };
    }

    private RuntimeAiConfig resolveRuntimeConfig() {
        return resolveRuntimeConfig(null, false, false);
    }

    private RuntimeAiConfig resolveRuntimeConfig(Long userId, boolean fallbackToPlatform, boolean requireUserCustom) {
        RuntimeAiConfig userRuntimeConfig = resolveUserRuntimeConfig(userId, fallbackToPlatform, requireUserCustom);
        if (userRuntimeConfig != null) {
            return userRuntimeConfig;
        }
        String fallbackProvider = normalizeConfigValue(provider);
        if (fallbackProvider == null) {
            fallbackProvider = "doubao";
        }
        fallbackProvider = fallbackProvider.toLowerCase(Locale.ROOT);
        String fallbackModel = normalizeConfigValue(model);
        String fallbackBaseUrl = resolveBaseUrl(fallbackProvider, configuredBaseUrl);
        String fallbackApiKey = resolveApiKey(fallbackProvider);
        String runtimeProvider = fallbackProvider;
        String runtimeModel = fallbackModel;
        String runtimeBaseUrl = fallbackBaseUrl;
        String runtimeApiKey = fallbackApiKey;
        String source = "application";
        Integer runtimeTimeoutMs = null;
        String runtimeThinkingMode = this.thinkingMode;
        SysAiEngineConfig activeConfig = null;
        try {
            activeConfig = sysAiEngineConfigService.getActiveByBusinessType(AiEngineConstants.BUSINESS_TYPE_RESUME);
        } catch (Exception e) {
            log.warn("读取简历业务激活 AI 配置失败，回退本地配置: {}", e.getMessage());
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
            } else {
                log.debug("数据库 apiKey 为空，使用本地兜底");
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
            log.warn("[RESUME] runtimeApiKey 仍为空，尝试从环境变量兜底获取");
            runtimeApiKey = resolveApiKey(runtimeProvider);
        }
        if (runtimeApiKey == null || runtimeApiKey.isBlank()) {
            throw new IllegalStateException("简历 AI 密钥不可用：数据库和管理端均无有效配置。"
                    + "请在管理端激活 AI 引擎配置，或设置环境变量 " + getEnvKeyName(runtimeProvider));
        }
        return new RuntimeAiConfig(
                runtimeProvider,
                runtimeModel,
                runtimeBaseUrl,
                getEndpointByProvider(runtimeProvider),
                runtimeApiKey,
                source,
                runtimeTimeoutMs,
                runtimeThinkingMode,
                activeConfig != null && Integer.valueOf(1).equals(activeConfig.getSupportsMultimodal()));
    }

    private RuntimeAiConfig resolveUserRuntimeConfig(Long userId, boolean fallbackToPlatform, boolean requireUserCustom) {
        ResolvedAiConfig userConfig = userAiConfigResolver == null
                ? null
                : userAiConfigResolver.resolve(userId, AiEngineConstants.BUSINESS_TYPE_RESUME, fallbackToPlatform);
        if (userConfig == null) {
            if (requireUserCustom) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "用户自定义 AI 配置不可用");
            }
            return null;
        }
        // 用户自定义配置已在保存时完成公网 HTTPS 校验，这里直接构造运行时配置并跳过平台链路。
        return new RuntimeAiConfig(
                userConfig.getProvider(),
                userConfig.getModel(),
                userConfig.getBaseUrl(),
                getEndpointByProvider(userConfig.getProvider()),
                userConfig.getApiKey(),
                UserAiConstants.BILLING_SOURCE_USER_CUSTOM,
                null,
                "none",
                userConfig.isSupportsMultimodal());
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

    private String resolveSystemPrompt(String tag) {
        String dbPrompt = sysPromptService.getActivePromptContent(ResumeDiagnosisConstants.SCENARIO_TYPE_RESUME);
        if (dbPrompt != null && !dbPrompt.isBlank()) {
            log.info("[{}] 使用数据库中的简历诊断Prompt, length: {}", tag, dbPrompt.length());
            return dbPrompt;
        }
        log.warn("[{}] 数据库中未找到启用的简历诊断Prompt，使用内置默认Prompt", tag);
        return getCompactDefaultSystemPrompt();
    }

    /**
     * 诊断任务默认走紧凑版 Prompt，保留现有结果结构，但收紧篇幅要求，减少无效生成耗时。
     */
    private String getCompactDefaultSystemPrompt() {
        return """
                角色：跨行业资深职业顾问。任务：严格诊断简历问题。
                原则：1)根据简历实际岗位方向评价，不预设技术岗标准 2)优点缺点都直说 3)项目必须有业务价值和量化成果 4)学历可以放宽，但经历必须真实可信。
                评分标准：S(90+)顶尖 A(75-89)优秀 B(60-74)合格 C(40-59)偏弱 D(<40)问题严重。多数简历应落在B-C区间，仅真正出色者得A以上。
                权重：工作/实习经历30% 项目经历27% 核心技能23% 教育背景10% 个人定位5% 个人信息5%。
                规则：只返回JSON，不要额外文本；JSON必须完整闭合；缺信息返回null或空数组；basicInfoDetails必须从原文提取真实值。
                输出约束：
                1. 每个维度的 strengths 和 weaknesses 各输出1-3条，必须简洁具体，不得为空数组。
                2. 除 overallEvaluation 外，每个维度的 evaluation 控制在50-90字。
                3. overallEvaluation.summary 控制在180-260字，使用1个自然段。
                4. optimizationSuggestions 输出5-8条，优先写最关键的问题。
                """;
    }

    /**
     * 紧凑版用户 Prompt 继续沿用原有 JSON 结构，只压缩说明性文本篇幅。
     */
    private String buildCompactDiagnosisUserPrompt(String resumeText) {
        return """
                请对以下简历进行诊断，暴露所有问题。严格按评分标准打分，不要偏高。

                简历内容：
                """ + resumeText
                + """

                        诊断重点：
                        1. 项目经历：是否有量化成果，个人贡献是否明确。
                        2. 工作经历：是否有业务结果数据，职业成长轨迹是否清晰。
                        3. 与岗位无关的字段直接填 false，不扣分。
                        4. 简历结构：逻辑、错别字、排版问题都要指出。
                        5. 每个维度的 strengths 和 weaknesses 各输出1-3条，每条一句话。
                        6. 除 overallEvaluation 外，每个维度必须包含 evaluation，长度50-90字，结构为"分数由来 + 主要优点 + 主要问题 + 改进建议"。
                        7. highlights至少输出3条确定性的简历亮点，最多7条；亮点需有简历原文证据支撑，如量化成果、技术亮点、项目成就等；表达自信明确，不使用模糊措辞。
                        8. 个人定位维度（positioningEvaluation）评估职业定位清晰度、核心竞争力突出程度、职业叙事连贯性与差异化亮点等。
                        9. 每个维度的suggestions至少输出3条建议，最多7条；必须针对具体问题给出可执行的改进方向。
                        10. optimizationSuggestions至少输出5条核心优化建议，最多8条；按优先级排列。

                        summary要求：
                        1. 写一段180-260字的总体评价，不使用编号或小标题。
                        2. 内容覆盖岗位匹配度、经历深度、技能可信度、最关键改进方向。
                        3. 语气客观正式，使用"该简历""候选人""呈现""缺乏"等表达。

                        返回JSON格式(不要额外文本)：
                        {"overallEvaluation":{"totalScore":0-100,"level":"S/A/B/C/D","summary":"180-260字总体评价","strengths":["整体优势1"],"weaknesses":["整体不足1"]},
                        "highlights":["亮点1","亮点2","亮点3"],
                        "basicInfoEvaluation":{"score":0-100,"hasName":true/false,"hasPhone":true/false,"hasEmail":true/false,"hasGithub":true/false,"hasBlog":true/false,"evaluation":"50-90字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":["建议1","建议2","建议3"]},
                        "basicInfoDetails":{"name":"","email":"","phone":"","location":"","currentCompany":"","github":"","blog":""},
                        "skillEvaluation":{"score":0-100,"skillList":[""],"evaluation":"50-90字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":["建议1","建议2","建议3"]},
                        "workExperienceEvaluation":{"score":0-100,"totalYears":0,"companyCount":0,"hasQuantifiableResults":true/false,"experiences":[{"company":"","position":"","duration":"","highlights":[""]}],"evaluation":"50-90字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":["建议1","建议2","建议3"]},
                        "projectExperienceEvaluation":{"score":0-100,"projectCount":0,"hasTechStack":true/false,"hasResponsibilities":true/false,"projects":[{"name":"","role":"","techStack":"","highlights":[""]}],"evaluation":"50-90字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":["建议1","建议2","建议3"]},
                        "educationEvaluation":{"score":0-100,"degree":"","school":"","major":"","hasRelevantMajor":true/false,"evaluation":"50-90字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":["建议1","建议2","建议3"]},
                        "positioningEvaluation":{"score":0-100,"evaluation":"50-90字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":["建议1","建议2","建议3"]},
                        "optimizationSuggestions":["建议1","建议2","建议3","建议4","建议5"]}
                        """;
    }

    private String buildUserPrompt(String resumeText) {
        return """
                请对以下简历进行诊断，暴露所有问题。严格按评分标准打分，不要偏高。

                简历内容：
                """ + resumeText
                + """

                        诊断重点：
                        1.项目经历：有无量化成果？个人贡献是否明确？"负责XX开发"等空洞描述直接扣分。
                        2.工作经历：有无业务成果数据？职业成长轨迹是否清晰？
                        3.与岗位无关的字段（如非技术岗的hasGithub/hasBlog）直接填false，不扣分。
                        4.简历结构：逻辑是否清晰，有无错别字/排版问题。
                        5.每个维度的strengths列出该维度的加分项（做得好的地方），weaknesses列出扣分项（需要改进的地方），每项一句话，简洁具体。
                        6.除overallEvaluation外，每个维度必须包含evaluation字段：一段80-150字的评价文本，结构为"先说明分数由来→列出主要加分项→列出主要扣分项→给出改进建议"，语气专业客观，不要泛泛而谈。
                        7.highlights至少输出3条确定性的简历亮点，最多7条；亮点需有简历原文证据支撑，如量化成果、技术亮点、项目成就等；表达自信明确，不使用模糊措辞。
                        8.个人定位维度（positioningEvaluation）评估职业定位清晰度、核心竞争力突出程度、职业叙事连贯性与差异化亮点等。
                        9.每个维度的suggestions至少输出3条建议，最多7条；必须针对具体问题给出可执行的改进方向。
                        10.optimizationSuggestions至少输出5条核心优化建议，最多8条；按优先级排列，优先写最关键的问题。

                        summary要求：写一份300-400字的"评判官视角"总体评价，采用连贯的自然段落叙述，不使用模块编号或小标题。具体规则：
                        1.内容维度：覆盖岗位匹配度（一句话定位竞争力层级）→经历深度（实习>项目>教育优先级，无实习则提升项目权重，均无则转向教育背景）→技能可信度（是否具体到工具/场景/效果）→核心改进方向（优先级明确的2-3条建议）。
                        2.语气：客观正式，使用"该简历""候选人""呈现""缺乏"等表述，禁止"你""您的"；批评直指问题不贬损，建议用指令句式。
                        3.篇幅：总字数300-400字，1-2个自然段；实习经历分析占40%以上（若存在）。
                        岗位类型动态规则：技术岗侧重技术栈规范性；教师/文科侧重教学实习细节；市场/运营侧重活动数据；设计侧重作品集；职能岗侧重流程处理。引用简历原文作为证据，不要泛泛而谈。

                        返回JSON格式(不要额外文本)：
                        {"overallEvaluation":{"totalScore":0-100,"level":"S/A/B/C/D","summary":"按上方summary要求填写","strengths":["整体优势1","整体优势2"],"weaknesses":["整体不足1","整体不足2"]},
                        "highlights":["亮点1","亮点2","亮点3"],
                        "basicInfoEvaluation":{"score":0-100,"hasName":true/false,"hasPhone":true/false,"hasEmail":true/false,"hasGithub":true/false,"hasBlog":true/false,"evaluation":"80-150字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":["建议1","建议2","建议3"]},
                        "basicInfoDetails":{"name":"","email":"","phone":"","location":"","currentCompany":"","github":"","blog":""},
                        "skillEvaluation":{"score":0-100,"skillList":[""],"evaluation":"80-150字评价文本","strengths":[""],"weaknesses":[""],"suggestions":[""]},
                        "workExperienceEvaluation":{"score":0-100,"totalYears":0,"companyCount":0,"hasQuantifiableResults":true/false,"experiences":[{"company":"","position":"","duration":"","highlights":[""]}],"evaluation":"80-150字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":[""]},
                        "projectExperienceEvaluation":{"score":0-100,"projectCount":0,"hasTechStack":true/false,"hasResponsibilities":true/false,"projects":[{"name":"","role":"","techStack":"","highlights":[""]}],"evaluation":"80-150字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":[""]},
                        "educationEvaluation":{"score":0-100,"degree":"","school":"","major":"","hasRelevantMajor":true/false,"evaluation":"80-150字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":[""]},
                        "positioningEvaluation":{"score":0-100,"evaluation":"80-150字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":["建议1","建议2","建议3"]},
                        "optimizationSuggestions":["建议1","建议2","建议3","建议4","建议5"]}
                        """;
    }

    @Override
    public ResumePolishAiResult polishResume(String resumeText, String jdText,
            ResumeJobMatchAnalyzeResponse latestJobMatchAnalysis) {
        return polishResume(resumeText, jdText, latestJobMatchAnalysis, null, false);
    }

    @Override
    public ResumePolishAiResult polishResume(String resumeText, String jdText,
            ResumeJobMatchAnalyzeResponse latestJobMatchAnalysis, Long userId, boolean fallbackToPlatform) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig(userId, fallbackToPlatform, false);
        String tag = runtimeConfig.provider().toUpperCase();
        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException("简历文本不能为空");
        }
        String apiKey = runtimeConfig.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请先配置简历 AI 密钥");
        }
        // 润色优先保留原始简历结构，只在超限时再降级压缩，避免把时间/章节语义压坏。
        String preparedResume = resumeText;
        String preparedJd = (jdText != null && !jdText.isBlank())
                ? AiInputCompressor.toStructuredFormat(jdText, AiInputCompressor.ContentType.JD)
                : jdText;
        String systemPrompt = buildResumePolishSystemPrompt();
        String userPrompt = buildResumePolishUserPrompt(preparedResume, preparedJd, latestJobMatchAnalysis);

        // 【Token 优化】步骤2：预估输入 token 数
        int systemTokens = TokenEstimator.estimateTokens(systemPrompt);
        int userTokens = TokenEstimator.estimateTokens(userPrompt);
        int totalTokens = systemTokens + userTokens;
        log.info("[{}] AI 简历润色调用, 原始简历长度: {}, 压缩后: {}, JD长度: {}, 压缩后: {}, 预估token: {}(system:{}, user:{})",
                tag, resumeText.length(), preparedResume.length(),
                jdText == null ? 0 : jdText.length(),
                preparedJd == null ? 0 : preparedJd.length(),
                totalTokens, systemTokens, userTokens);

        // 超限时再逐级压缩，尽量先保留原始排版结构。
        if (tokenLimitConfig.isTokenLimitEnabled()) {
            int maxTokens = tokenLimitConfig.getPolishResumeMax();
            if (totalTokens > maxTokens) {
                log.warn("[{}] 简历润色 token 预估({})超过限制({})，开始降级压缩输入", tag, totalTokens, maxTokens);
                preparedResume = compressResumeIfEnabled(resumeText, tag);
                userPrompt = buildResumePolishUserPrompt(preparedResume, preparedJd, latestJobMatchAnalysis);
                userTokens = TokenEstimator.estimateTokens(userPrompt);
                totalTokens = systemTokens + userTokens;
                log.info("[{}] 压缩简历后预估token: {}(system:{}, user:{})", tag, totalTokens, systemTokens, userTokens);
            }
            if (totalTokens > maxTokens) {
                int resumeMaxTokens = maxTokens - systemTokens - 500;
                preparedResume = TokenEstimator.safeTruncate(preparedResume, Math.max(500, resumeMaxTokens));
                userPrompt = buildResumePolishUserPrompt(preparedResume, preparedJd, latestJobMatchAnalysis);
                userTokens = TokenEstimator.estimateTokens(userPrompt);
                totalTokens = systemTokens + userTokens;
                log.info("[{}] 截断后预估token: {}(system:{}, user:{})", tag, totalTokens, systemTokens, userTokens);
            }
        }
        RequestBody request = new RequestBody();
        request.model = runtimeConfig.model();
        request.messages = List.of(
                new Message("system", systemPrompt),
                new Message("user", userPrompt));
        request.thinking = buildThinkingConfig(runtimeConfig.model(), runtimeConfig.thinkingMode());
        try {
            RestClient.Builder builder = restClientBuilder
                    .baseUrl(runtimeConfig.baseUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            // 【超时保障】默认 5 分钟，DB 配置值取较大者，确保大型思考模型有足够响应时间。
            int readTimeout = 300_000;
            if (runtimeConfig.timeoutMs() != null && runtimeConfig.timeoutMs() > 0) {
                readTimeout = Math.max(runtimeConfig.timeoutMs(), 300_000);
            }
            SimpleClientHttpRequestFactory customFactory = new SimpleClientHttpRequestFactory();
            customFactory.setConnectTimeout(10000);
            customFactory.setReadTimeout(readTimeout);
            builder = builder.requestFactory(customFactory);
            RestClient runtimeRestClient = builder.build();
            String responseText = executeAiRequestForText(runtimeRestClient, runtimeConfig.endpoint(), apiKey, request);
            String result = extractJsonFromResponse(responseText);
            return parseResumePolishAiResult(result);
        } catch (Exception e) {
            log.error("[{}] AI 简历润色失败", tag, e);
            throwCustomAiFailureIfNeeded(runtimeConfig, e);
            throw new RuntimeException("AI 简历润色失败: " + e.getMessage(), e);
        }
    }

    /**
     * AI 润色提示词 V2：
     * 强制 AI 输出统一章节和稳定头部格式，避免不同岗位简历使用大量近义标题导致前端渲染失稳。
     */
    private String buildResumePolishSystemPromptV2() {
        return """
                你是一名资深中文简历优化顾问，负责把原始简历整理为稳定、可解析、适合模板填充的纯文本简历正文。

                严格要求：
                1. 只允许基于原始简历事实润色，不得编造公司、岗位、项目、学历、证书、成绩、时间或量化结果。
                2. polishedResumeText 只能包含一份完整简历正文，绝不能输出两份简历、摘要块、调试说明、字段解释、类型标注、时间戳、来源说明或 modificationNotes 内容。
                3. 只能输出纯文本正文，不要 Markdown，不要代码块，不要 JSON 之外的额外说明。
                4. 章节标题必须且只能使用以下标准标题，并且每个标题独占一行：
                   个人信息
                   教育背景
                   实习经历
                   工作经历
                   项目经历
                   专业技能
                   校园经历
                   荣誉证书
                   个人评价
                5. 不要输出“摘要”章节；如果原文有摘要信息，只能吸收进其他合适章节，不能单独输出摘要标题或顶部摘要块。
                6. 同一章节只输出一次，不要重复章节，不要在正文后再次输出另一份结构化简历。
                7. 教育、实习、工作、项目的标题行优先使用稳定分隔格式：
                   学校 | 专业/状态 | 学历/年级 | 时间
                   公司 | 岗位 | 时间
                   项目 | 角色 | 时间
                8. 荣誉证书一项一行；证书不要混入专业技能。
                9. 个人评价只保留一个章节，内容精炼、真实、岗位相关。

                返回格式：
                {
                  "polishedResumeText": "润色后的完整简历纯文本正文",
                  "modificationNotes": [
                    "说明改了什么，以及为什么这样改",
                    "说明改了什么，以及为什么这样改",
                    "说明改了什么，以及为什么这样改"
                  ]
                }
                """;
    }

    private String buildResumePolishSystemPrompt() {
        return """
                你是一名资深中文简历优化顾问，负责把原始简历整理为稳定、可解析、适合模板填充的纯文本简历正文。

                严格要求：
                1. 只允许基于原始简历事实润色，不得编造公司、岗位、项目、学历、证书、成绩、时间或量化结果。
                2. polishedResumeText 只能包含一份完整简历正文，绝不能输出两份简历、摘要块、调试说明、字段解释、类型标注、时间戳、来源说明或 modificationNotes 内容。
                3. 只能输出纯文本正文，不要 Markdown，不要代码块，不要 JSON 之外的额外说明。
                4. 章节标题必须且只能使用以下标准标题，并且每个标题独占一行：
                   个人信息
                   教育背景
                   实习经历
                   工作经历
                   项目经历
                   专业技能
                   校园经历
                   荣誉证书
                   个人评价
                5. 不要输出“摘要”章节；如果原文有摘要信息，只能吸收进其他合适章节，不能单独输出摘要标题或顶部摘要块。
                6. 同一章节只输出一次，不要重复章节，不要在正文后再次输出另一份结构化简历。
                7. 教育、实习、工作、项目的标题行优先使用稳定分隔格式：
                   学校 | 专业/状态 | 学历/年级 | 时间
                   公司 | 岗位 | 时间
                   项目 | 角色 | 时间
                8. 荣誉证书一项一行；证书不要混入专业技能。
                9. 个人评价只保留一个章节，内容精炼、真实、岗位相关。

                返回格式：
                {
                  "polishedResumeText": "润色后的完整简历纯文本正文",
                  "modificationNotes": [
                    "说明改了什么，以及为什么这样改",
                    "说明改了什么，以及为什么这样改",
                    "说明改了什么，以及为什么这样改"
                  ]
                }
                """;
    }

    /**
     * 用户提示词 V2：
     * 结合 JD 与最近一次匹配结果，要求 AI 主动完成章节归一和跨岗位内容拆分。
     */
    private String buildResumePolishUserPromptV2(String resumeText, String jdText,
            ResumeJobMatchAnalyzeResponse latestJobMatchAnalysis) {
        StringBuilder builder = new StringBuilder();
        builder.append("请对以下简历进行润色优化，并严格输出一份可直接用于模板填充的纯文本简历正文。\n");
        builder.append("【原始简历】\n").append(resumeText).append("\n\n");

        if (jdText != null && !jdText.isBlank()) {
            builder.append("【目标岗位 JD】\n").append(jdText).append("\n\n");
        }

        if (latestJobMatchAnalysis != null) {
            builder.append("【最近一次岗位匹配分析】\n");
            builder.append("匹配度评分：").append(latestJobMatchAnalysis.getMatchScore()).append("\n");
            builder.append("已匹配关键词：").append(latestJobMatchAnalysis.getMatchedKeywords()).append("\n");
            List<String> filteredMissing = latestJobMatchAnalysis.getMissingKeywords() == null
                    ? List.of()
                    : latestJobMatchAnalysis.getMissingKeywords().stream()
                            .filter(kw -> kw != null && !kw.isBlank() && !kw.equalsIgnoreCase("JD"))
                            .toList();
            builder.append("缺失关键词：").append(filteredMissing).append("\n");
            builder.append("优化建议：").append(latestJobMatchAnalysis.getSuggestions()).append("\n\n");
        }

        builder.append("""
                请输出更适合求职投递的版本，并满足以下要求：
                1. polishedResumeText 只能输出一份完整正文，不要重复简历，不要在正文后再输出另一份结构化版本。
                2. 章节标题必须独占一行，且只能使用：个人信息、教育背景、实习经历、工作经历、项目经历、专业技能、校园经历、荣誉证书、个人评价。
                3. 不要输出“摘要”标题，不要把摘要放在顶部，不要输出 modificationNotes 到正文。
                4. 教育/实习/工作/项目标题行优先使用带竖线的稳定格式：
                   学校 | 专业/状态 | 学历/年级 | 时间
                   公司 | 岗位 | 时间
                   项目 | 角色 | 时间
                5. 荣誉证书一项一行，证书不要混入专业技能。
                6. 尽量用“能力/动作/结果”方式重写经历描述；如果提供了 JD，请体现更强的岗位针对性。
                7. modificationNotes 至少输出 3 条，必须能让用户理解改动原因。
                """);
        return builder.toString();
    }

    private String buildResumePolishUserPrompt(String resumeText, String jdText,
            ResumeJobMatchAnalyzeResponse latestJobMatchAnalysis) {
        StringBuilder builder = new StringBuilder();
        builder.append("请对以下简历进行润色优化，并严格输出一份可直接用于模板填充的纯文本简历正文。\n");
        builder.append("【原始简历】\n").append(resumeText).append("\n\n");

        if (jdText != null && !jdText.isBlank()) {
            builder.append("【目标岗位 JD】\n").append(jdText).append("\n\n");
        }

        if (latestJobMatchAnalysis != null) {
            builder.append("【最近一次岗位匹配分析】\n");
            builder.append("匹配度评分：").append(latestJobMatchAnalysis.getMatchScore()).append("\n");
            builder.append("已匹配关键词：").append(latestJobMatchAnalysis.getMatchedKeywords()).append("\n");
            List<String> filteredMissing = latestJobMatchAnalysis.getMissingKeywords() == null
                    ? List.of()
                    : latestJobMatchAnalysis.getMissingKeywords().stream()
                            .filter(kw -> kw != null && !kw.isBlank() && !kw.equalsIgnoreCase("JD"))
                            .toList();
            builder.append("缺失关键词：").append(filteredMissing).append("\n");
            builder.append("优化建议：").append(latestJobMatchAnalysis.getSuggestions()).append("\n\n");
        }

        builder.append("""
                请输出更适合求职投递的版本，并满足以下要求：
                1. polishedResumeText 只能输出一份完整正文，不要重复简历，不要在正文后再输出另一份结构化版本。
                2. 章节标题必须独占一行，且只能使用：个人信息、教育背景、实习经历、工作经历、项目经历、专业技能、校园经历、荣誉证书、个人评价。
                3. 不要输出“摘要”标题，不要把摘要放在顶部，不要输出 modificationNotes 到正文。
                4. 教育/实习/工作/项目标题行优先使用带竖线的稳定格式：
                   学校 | 专业/状态 | 学历/年级 | 时间
                   公司 | 岗位 | 时间
                   项目 | 角色 | 时间
                5. 荣誉证书一项一行，证书不要混入专业技能。
                6. 尽量用“能力/动作/结果”方式重写经历描述；如果提供了 JD，请体现更强的岗位针对性。
                7. modificationNotes 至少输出 3 条，必须能让用户理解改动原因。
                """);
        return builder.toString();
    }

    private String extractJsonFromResponse(String raw) {
        if (raw == null)
            return raw;
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        int lastBacktick = trimmed.lastIndexOf("```");
        if (lastBacktick > 0) {
            trimmed = trimmed.substring(0, lastBacktick);
        }
        return trimmed.trim();
    }

    private ResumePolishAiResult parseResumePolishAiResult(String rawJson) throws Exception {
        try {
            ResumePolishAiResult strictResult = objectMapper.readValue(rawJson, ResumePolishAiResult.class);
            if (isValidPolishResult(strictResult)) {
                return normalizePolishResult(strictResult);
            }
        } catch (Exception ex) {
            log.warn("AI 润色结果严格 JSON 解析失败，进入容错解析。rawPreview: {}",
                    buildRawPreview(rawJson), ex);
        }
        JsonNode rootNode = tryReadJsonNode(rawJson);
        if (rootNode != null) {
            ResumePolishAiResult jsonNodeResult = new ResumePolishAiResult();
            jsonNodeResult.setPolishedResumeText(readTextNode(rootNode, "polishedResumeText"));
            jsonNodeResult.setModificationNotes(readPrimaryModificationNotes(rootNode));
            if (isValidPolishResult(jsonNodeResult)) {
                return normalizePolishResult(jsonNodeResult);
            }
        }
        ResumePolishAiResult regexFallbackResult = new ResumePolishAiResult();
        regexFallbackResult.setPolishedResumeText(extractPolishedResumeText(rawJson));
        regexFallbackResult.setModificationNotes(extractModificationNotes(rawJson));
        if (isValidPolishResult(regexFallbackResult)) {
            return normalizePolishResult(regexFallbackResult);
        }
        throw new IllegalStateException("AI 润色结果解析失败，返回内容不是可用的结构化结果");
    }

    private ResumePolishAiResult normalizePolishResult(ResumePolishAiResult result) {
        ResumePolishAiResult normalized = new ResumePolishAiResult();
        normalized.setPolishedResumeText(sanitizePolishedResumeText(result.getPolishedResumeText()));
        normalized.setModificationNotes(normalizeModificationNotes(result.getModificationNotes(), normalized.getPolishedResumeText()));
        return normalized;
    }

    private String readPrimaryPolishText(JsonNode rootNode) {
        String polishedText = readTextNode(rootNode, "polishedResumeText");
        if (!polishedText.isBlank()) {
            return polishedText;
        }
        return firstNonBlankField(rootNode,
                "resumeText", "optimizedResumeText", "optimizedResume", "resume", "content", "text", "result");
    }

    private List<String> readPrimaryModificationNotes(JsonNode rootNode) {
        List<String> notes = readTextListNode(rootNode, "modificationNotes");
        if (!notes.isEmpty()) {
            return notes;
        }
        notes = readTextListNode(rootNode, "notes");
        if (!notes.isEmpty()) {
            return notes;
        }
        notes = readTextListNode(rootNode, "suggestions");
        if (!notes.isEmpty()) {
            return notes;
        }
        notes = readTextListNode(rootNode, "reasons");
        if (!notes.isEmpty()) {
            return notes;
        }
        String summary = readTextNode(rootNode, "summary");
        return summary.isBlank() ? List.of() : List.of(summary);
    }

    private JsonNode tryReadJsonNode(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (Exception ex) {
            return null;
        }
    }

    private String extractPolishedResumeText(String rawJson) {
        Matcher matcher = POLISHED_TEXT_PATTERN.matcher(rawJson);
        if (!matcher.find()) {
            return "";
        }
        return normalizePolishText(unescapeLooseJsonText(matcher.group(1)));
    }

    private List<String> extractModificationNotes(String rawJson) {
        Matcher matcher = MODIFICATION_NOTES_PATTERN.matcher(rawJson);
        if (!matcher.find()) {
            return List.of();
        }
        String notesBody = matcher.group(1).trim();
        if (notesBody.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue("[" + notesBody + "]", new TypeReference<List<String>>() {
            });
        } catch (Exception parseEx) {
            log.debug("JSON 解析 notes 失败，回退到正则提取: {}", parseEx.getMessage());
        }
        List<String> notes = new ArrayList<>();
        Matcher quotedTextMatcher = QUOTED_TEXT_PATTERN.matcher(notesBody);
        while (quotedTextMatcher.find()) {
            String item = normalizeSingleNote(unescapeLooseJsonText(quotedTextMatcher.group(1)));
            if (!item.isBlank()) {
                notes.add(item);
            }
        }
        if (!notes.isEmpty()) {
            return notes;
        }
        String normalizedNotesBody = notesBody
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        String[] lines = normalizedNotesBody.split("\n");
        for (String line : lines) {
            String item = normalizeSingleNote(line);
            if (!item.isBlank()) {
                notes.add(item);
            }
        }
        return notes;
    }

    private String unescapeLooseJsonText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String normalizePolishText(String text) {
        return text == null ? "" : text.trim();
    }

    private String sanitizePolishedResumeText(String text) {
        if (text == null) {
            return "";
        }

        String sanitized = text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\uFEFF", "")
                .trim();
        sanitized = Normalizer.normalize(sanitized, Normalizer.Form.NFKC);

        if (sanitized.startsWith("```json")) {
            sanitized = sanitized.substring(7).trim();
        } else if (sanitized.startsWith("```")) {
            sanitized = sanitized.substring(3).trim();
        }
        if (sanitized.endsWith("```")) {
            sanitized = sanitized.substring(0, sanitized.length() - 3).trim();
        }

        sanitized = sanitized.replaceFirst("(?m)^AI润色简历\\s*$\\n?", "");
        sanitized = sanitized.replaceFirst("(?m)^个人简历\\s*$\\n?", "");
        sanitized = sanitized.replaceFirst("(?m)^求职简历\\s*$\\n?", "");
        sanitized = sanitized.replaceFirst("(?m)^简历\\s*$\\n?", "");
        sanitized = sanitized.replaceAll("(?m)^#{1,6}\\s*", "");
        sanitized = stripLeadingSummaryBlock(sanitized);
        sanitized = normalizeInlineSectionTitles(sanitized);
        sanitized = keepFirstResumeBody(sanitized);
        sanitized = stripTrailingPolishMetadata(sanitized);
        sanitized = sanitized.replaceAll("\n{3,}", "\n\n");
        return sanitized.trim();
    }

    private String sanitizeAndNormalizePolishedResumeText(String text) {
        String sanitized = sanitizePolishedResumeText(text);
        if (sanitized.isBlank()) {
            return sanitized;
        }
        sanitized = sanitized.replaceAll("(?m)^[-*•]\\s+(?=个人信息|教育背景|实习经历|工作经历|项目经历|专业技能|校园经历|荣誉证书|个人评价)", "");
        sanitized = normalizePolishedResumeLayout(sanitized);
        sanitized = sanitized.replaceAll("\n{3,}", "\n\n");
        return sanitized.trim();
    }

    private String stripLeadingSummaryBlock(String text) {
        List<String> keptLines = new ArrayList<>();
        boolean skipSummary = false;

        for (String rawLine : text.split("\n")) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            String cleanLine = stripTrailingMetadataFromLine(line);
            if (cleanLine.isBlank()) {
                continue;
            }

            if ("摘要".equals(cleanLine) || cleanLine.startsWith("摘要：") || cleanLine.startsWith("摘要:")) {
                skipSummary = true;
                continue;
            }

            if (skipSummary && isResumeSectionTitle(cleanLine)) {
                skipSummary = false;
            }

            if (skipSummary) {
                continue;
            }

            keptLines.add(cleanLine);
        }

        return String.join("\n", keptLines);
    }

    private String normalizeInlineSectionTitles(String text) {
        String normalized = text;
        for (String title : POLISH_SECTION_TITLES) {
            normalized = normalized.replaceAll("(?<!\\n)" + Pattern.quote(title) + "(?=[:：\\s])", "\n" + title);
            normalized = normalized.replaceAll(Pattern.quote(title) + "\\s*[：:]", title + "\n");
        }
        return normalized.replaceAll("\n{3,}", "\n\n").trim();
    }

    private String keepFirstResumeBody(String text) {
        List<String> keptLines = new ArrayList<>();
        List<String> seenSectionOrder = new ArrayList<>();
        boolean startedResumeBody = false;

        for (String rawLine : text.split("\n")) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isEmpty()) {
                if (!keptLines.isEmpty() && !keptLines.get(keptLines.size() - 1).isEmpty()) {
                    keptLines.add("");
                }
                continue;
            }

            String cleanLine = stripTrailingMetadataFromLine(line);
            if (cleanLine.isBlank()) {
                continue;
            }

            String sectionTitle = normalizeSectionTitle(cleanLine);
            if (!sectionTitle.isBlank()) {
                if (startedResumeBody && RESUME_START_KEYS.contains(sectionTitle) && seenSectionOrder.contains(sectionTitle)) {
                    break;
                }
                startedResumeBody = startedResumeBody || RESUME_START_KEYS.contains(sectionTitle);
                if (!seenSectionOrder.contains(sectionTitle)) {
                    seenSectionOrder.add(sectionTitle);
                }
                if (!keptLines.isEmpty() && !keptLines.get(keptLines.size() - 1).isEmpty()) {
                    keptLines.add("");
                }
                keptLines.add(sectionTitle);
                continue;
            }

            keptLines.add(cleanLine);
        }

        while (!keptLines.isEmpty() && keptLines.get(keptLines.size() - 1).isEmpty()) {
            keptLines.remove(keptLines.size() - 1);
        }

        return String.join("\n", keptLines);
    }

    private String stripTrailingPolishMetadata(String text) {
        List<String> keptLines = new ArrayList<>();

        for (String rawLine : text.split("\n")) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isEmpty()) {
                if (!keptLines.isEmpty() && !keptLines.get(keptLines.size() - 1).isEmpty()) {
                    keptLines.add("");
                }
                continue;
            }

            if (looksLikeTrailingResumeMetadata(line)) {
                break;
            }

            String cleanLine = stripTrailingMetadataFromLine(line);
            if (cleanLine.isBlank()) {
                continue;
            }
            keptLines.add(cleanLine);
        }

        while (!keptLines.isEmpty() && keptLines.get(keptLines.size() - 1).isEmpty()) {
            keptLines.remove(keptLines.size() - 1);
        }

        return String.join("\n", keptLines);
    }

    private String stripTrailingMetadataFromLine(String line) {
        if (line == null || line.isBlank()) {
            return "";
        }
        Matcher matcher = TRAILING_METADATA_PATTERN.matcher(line);
        if (!matcher.find()) {
            return line.trim();
        }
        return line.substring(0, matcher.start()).trim().replaceAll("[,，;；]+$", "").trim();
    }

    private boolean looksLikeTrailingResumeMetadata(String line) {
        String normalized = line == null ? "" : line.trim();
        if (normalized.isEmpty()) {
            return false;
        }
        return normalized.startsWith("[")
                || normalized.startsWith("{")
                || normalized.contains("仅基于简历(String)")
                || normalized.contains("(LocalDateTime)")
                || normalized.contains("(Integer)")
                || normalized.contains("(String),")
                || normalized.startsWith("<== Updates:");
    }

    private boolean isResumeSectionTitle(String line) {
        return !normalizeSectionTitle(line).isBlank();
    }

    private String normalizePolishedResumeLayout(String text) {
        List<String> normalizedLines = new ArrayList<>();
        String currentSection = "";

        for (String rawLine : text.split("\n")) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isEmpty()) {
                if (!normalizedLines.isEmpty() && !normalizedLines.get(normalizedLines.size() - 1).isEmpty()) {
                    normalizedLines.add("");
                }
                continue;
            }

            String sectionTitle = normalizeSectionTitle(line);
            if (!sectionTitle.isBlank()) {
                if (!normalizedLines.isEmpty() && !normalizedLines.get(normalizedLines.size() - 1).isEmpty()) {
                    normalizedLines.add("");
                }
                normalizedLines.add(sectionTitle);
                currentSection = sectionTitle;
                continue;
            }

            if (isSkillsOrHonorSection(currentSection) && line.contains("|")) {
                for (String item : line.split("\\|")) {
                    String normalizedItem = normalizeBulletItem(item);
                    if (!normalizedItem.isBlank()) {
                        normalizedLines.add("- " + normalizedItem);
                    }
                }
                continue;
            }

            if (shouldMoveLeadingDate(currentSection, line)) {
                line = moveLeadingDateToTail(line);
            }

            normalizedLines.add(line);
        }

        while (!normalizedLines.isEmpty() && normalizedLines.get(normalizedLines.size() - 1).isEmpty()) {
            normalizedLines.remove(normalizedLines.size() - 1);
        }

        return String.join("\n", normalizedLines);
    }

    private String normalizeSectionTitle(String line) {
        String normalized = line.replaceAll("^[\\[【(（]+", "")
                .replaceAll("[\\]】)）:：\\s]+$", "")
                .trim();

        if (normalized.equals("个人信息") || normalized.equals("基本信息")) {
            return "个人信息";
        }
        if (normalized.equals("教育背景") || normalized.equals("教育经历") || normalized.equals("学历背景")) {
            return "教育背景";
        }
        if (normalized.equals("实习经历") || normalized.equals("实习经验")) {
            return "实习经历";
        }
        if (normalized.equals("工作经历") || normalized.equals("工作经验") || normalized.equals("职业经历")) {
            return "工作经历";
        }
        if (normalized.equals("项目经历") || normalized.equals("项目经验") || normalized.equals("项目成果")) {
            return "项目经历";
        }
        if (normalized.equals("专业技能") || normalized.equals("专业能力") || normalized.equals("核心技能")
                || normalized.equals("技能清单") || normalized.equals("技能特长")) {
            return "专业技能";
        }
        if (normalized.equals("校园经历") || normalized.equals("校内经历") || normalized.equals("学生工作")
                || normalized.equals("社团经历")) {
            return "校园经历";
        }
        if (normalized.equals("荣誉证书") || normalized.equals("荣誉奖项") || normalized.equals("证书资质")
                || normalized.equals("技术资质") || normalized.equals("技能证书") || normalized.equals("获奖情况")) {
            return "荣誉证书";
        }
        if (normalized.equals("个人评价") || normalized.equals("自我评价") || normalized.equals("职业优势")
                || normalized.equals("个人优势") || normalized.equals("个人总结") || normalized.equals("专业能力总结")) {
            return "个人评价";
        }
        return "";
    }

    private boolean isSkillsOrHonorSection(String sectionTitle) {
        return "专业技能".equals(sectionTitle) || "荣誉证书".equals(sectionTitle);
    }

    private boolean shouldMoveLeadingDate(String sectionTitle, String line) {
        if (!"教育背景".equals(sectionTitle)
                && !"实习经历".equals(sectionTitle)
                && !"工作经历".equals(sectionTitle)
                && !"项目经历".equals(sectionTitle)
                && !"校园经历".equals(sectionTitle)) {
            return false;
        }
        if (line.contains("|") || line.startsWith("- ") || line.startsWith("* ") || line.startsWith("•")) {
            return false;
        }
        return line.matches("^(\\d{2,4}[./-]\\d{1,2}(?:\\s*[-~至]\\s*\\d{2,4}[./-]\\d{1,2})?)\\s+.+$");
    }

    private String moveLeadingDateToTail(String line) {
        Matcher matcher = Pattern
                .compile("^(\\d{2,4}[./-]\\d{1,2}(?:\\s*[-~至]\\s*\\d{2,4}[./-]\\d{1,2})?)\\s+(.+)$")
                .matcher(line);
        if (!matcher.matches()) {
            return line;
        }

        String date = matcher.group(1).replaceAll("\\s+", "");
        String rest = matcher.group(2).trim().replaceAll("\\s{2,}", " ");
        String[] parts = rest.split("\\s+");
        if (parts.length >= 2) {
            String main = parts[0];
            String sub = rest.substring(main.length()).trim();
            return main + " | " + sub + " | " + date;
        }
        return rest + " | " + date;
    }

    private String normalizeBulletItem(String item) {
        return item == null ? "" : item.trim()
                .replaceFirst("^[-*•\\s]+", "")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private List<String> normalizeModificationNotes(List<String> notes, String polishedResumeText) {
        List<String> normalized = new ArrayList<>();
        if (notes != null) {
            for (String note : notes) {
                String item = normalizeSingleNote(note);
                if (!item.isBlank()) {
                    normalized.add(item);
                }
            }
        }
        if (!normalized.isEmpty()) {
            return normalized;
        }
        if (polishedResumeText == null || polishedResumeText.isBlank()) {
            return List.of();
        }
        return List.of(
                "已保留原简历中的有效事实信息，并统一整理为更适合投递的结构。",
                "已清理占位说明、冗余表述和不稳定排版内容，减少模板渲染异常。",
                "已优先突出岗位相关经历、技能和结果表达，便于招聘方快速阅读。");
    }

    private String normalizeSingleNote(String note) {
        if (note == null) {
            return "";
        }
        return note.trim()
                .replaceFirst("^[\"'\\s]+", "")
                .replaceFirst("[\"'\\s]+$", "")
                .replaceFirst("^[0-9]+[.、:：\\-\\s]+", "")
                .replaceFirst("^[一二三四五六七八九十]+[、:：\\-\\s]+", "")
                .replaceFirst("^[-*•·\\s]+", "")
                .trim();
    }

    private String readTextNode(JsonNode rootNode, String fieldName) {
        JsonNode fieldNode = rootNode.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return "";
        }
        return normalizePolishText(convertNodeToReadableText(fieldNode));
    }

    private List<String> readTextListNode(JsonNode rootNode, String fieldName) {
        JsonNode fieldNode = rootNode.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return List.of();
        }
        if (fieldNode.isArray()) {
            List<String> values = new ArrayList<>();
            for (JsonNode itemNode : fieldNode) {
                String item = normalizeSingleNote(convertNodeToReadableText(itemNode));
                if (!item.isBlank()) {
                    values.add(item);
                }
            }
            return values;
        }
        String singleValue = normalizeSingleNote(convertNodeToReadableText(fieldNode));
        return singleValue.isBlank() ? List.of() : List.of(singleValue);
    }

    private String convertNodeToReadableText(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText("");
        }
        if (node.isNumber() || node.isBoolean()) {
            return node.asText("");
        }
        if (node.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode itemNode : node) {
                String itemText = normalizeSingleNote(convertNodeToReadableText(itemNode));
                if (!itemText.isBlank()) {
                    parts.add(itemText);
                }
            }
            return String.join("\n", parts);
        }
        String preferredText = firstNonBlankField(node,
                "text", "content", "value", "desc", "description", "note", "summary", "reason");
        if (!preferredText.isBlank()) {
            return preferredText;
        }
        List<String> objectParts = new ArrayList<>();
        node.fields().forEachRemaining(entry -> {
            String valueText = normalizeSingleNote(convertNodeToReadableText(entry.getValue()));
            if (!valueText.isBlank()) {
                objectParts.add(valueText);
            }
        });
        if (!objectParts.isEmpty()) {
            return String.join("；", objectParts);
        }
        return node.toString();
    }

    private String firstNonBlankField(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode valueNode = node.get(fieldName);
            if (valueNode == null || valueNode.isNull()) {
                continue;
            }
            String valueText = normalizeSingleNote(convertNodeToReadableText(valueNode));
            if (!valueText.isBlank()) {
                return valueText;
            }
        }
        return "";
    }

    private boolean isValidPolishResult(ResumePolishAiResult result) {
        return result != null
                && result.getPolishedResumeText() != null
                && !result.getPolishedResumeText().isBlank();
    }

    private String buildRawPreview(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return "";
        }
        String preview = rawJson.replace("\r", "\\r").replace("\n", "\\n");
        return preview.length() > 300 ? preview.substring(0, 300) + "..." : preview;
    }

    /**
     * 根据配置决定是否压缩简历文本
     *
     * @param resumeText 原始简历文本
     * @param tag        日志标签（用于区分不同 AI 提供商）
     * @return 压缩后的简历文本（若压缩未启用则返回原文）
     *
     *         【逻辑说明】
     *         1. 若配置 compressionEnabled=false，直接返回原文（用于回滚或调试）
     *         2. 调用 AiInputCompressor 进行结构化压缩（去除空白、去重、章节优化）
     *         3. 记录压缩前后的字符数差异，便于监控压缩效果
     */
    private String compressResumeIfEnabled(String resumeText, String tag) {
        if (!tokenLimitConfig.isCompressionEnabled()) {
            return resumeText;
        }
        String compressed = AiInputCompressor.toStructuredFormat(resumeText, AiInputCompressor.ContentType.RESUME);
        int saved = resumeText.length() - compressed.length();
        if (saved > 0) {
            log.info("[{}] 简历文本已压缩，原始{}字符，压缩后{}字符，节省{}字符",
                    tag, resumeText.length(), compressed.length(), saved);
        }
        return compressed;
    }

    /**
     * 用户自定义 AI 失败必须显式返回 4090，前端据此展示“使用平台 AI”手动回退入口。
     */
    private void throwCustomAiFailureIfNeeded(RuntimeAiConfig runtimeConfig, Exception exception) {
        if (runtimeConfig != null && UserAiConstants.BILLING_SOURCE_USER_CUSTOM.equals(runtimeConfig.source())) {
            String message = exception == null || exception.getMessage() == null
                    ? "自定义AI调用失败"
                    : "自定义AI调用失败: " + exception.getMessage();
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, message);
        }
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
            boolean supportsMultimodal) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class RequestBody {
        public String model;
        public List<Message> messages;
        public Thinking thinking;
        public BigDecimal temperature;

        public RequestBody() {
        }
    }

    private static class StreamRequestBody extends RequestBody {
        public boolean stream;

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
        public Object content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public Message(String role, Object content) {
            this.role = role;
            this.content = content;
        }

        public static Message userWithImage(String prompt, String imageDataUrl) {
            return new Message("user", List.of(
                    ContentPart.text(prompt),
                    ContentPart.image(imageDataUrl)));
        }
    }

    private static class ContentPart {
        public String type;
        public String text;
        @JsonProperty("image_url")
        public ImageUrl imageUrl;

        public static ContentPart text(String text) {
            ContentPart part = new ContentPart();
            part.type = "text";
            part.text = text;
            return part;
        }

        public static ContentPart image(String imageDataUrl) {
            ContentPart part = new ContentPart();
            part.type = "image_url";
            part.imageUrl = new ImageUrl(imageDataUrl);
            return part;
        }
    }

    private static class ImageUrl {
        public String url;

        public ImageUrl(String url) {
            this.url = url;
        }
    }

    private static class ResponseBody {
        public List<Choice> choices;

        public static class Choice {
            public MessageContent message;

            public static class MessageContent {
                public Object content;
            }
        }
    }

    private record DiagnosisPrompt(
            String version,
            String systemPrompt,
            String userPrompt,
            int totalTokens) {
    }

    private static class DiagnosisStreamException extends RuntimeException {
        public DiagnosisStreamException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static class EmptyAiResponseException extends RuntimeException {
        public EmptyAiResponseException(String message) {
            super(message);
        }
    }
}
