package com.airesume.server.service.impl;

import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.config.AiTokenLimitConfig;
import com.airesume.server.dto.resume.ResumeJobMatchAnalyzeResponse;
import com.airesume.server.dto.resume.ResumePolishAiResult;
import com.airesume.server.entity.SysAiEngineConfig;
import com.airesume.server.service.ResumeAiService;
import com.airesume.server.service.SysAiEngineConfigService;
import com.airesume.server.service.SysPromptService;
import com.airesume.server.util.AiInputCompressor;
import com.airesume.server.util.TokenEstimator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("resumeAiService")
@Slf4j
@ConditionalOnProperty(name = "app.ai.mode", havingValue = "real")
public class ResumeAiServiceImpl implements ResumeAiService {

    private final RestClient restClient;
    private final String provider;
    private final String model;
    private final String configuredBaseUrl;
    private final String resolvedBaseUrl;
    private final String endpoint;
    private final SysPromptService sysPromptService;
    private final String thinkingMode;
    private final RestClient.Builder restClientBuilder;
    private final SysAiEngineConfigService sysAiEngineConfigService;
    private final ObjectMapper objectMapper;
    private final AiTokenLimitConfig tokenLimitConfig;
    private static final Pattern POLISHED_TEXT_PATTERN = Pattern.compile(
            "\"polishedResumeText\"\\s*:\\s*\"(.*?)\"\\s*,\\s*\"modificationNotes\"",
            Pattern.DOTALL);
    private static final Pattern MODIFICATION_NOTES_PATTERN = Pattern.compile(
            "\"modificationNotes\"\\s*:\\s*\\[(.*?)]",
            Pattern.DOTALL);
    private static final Pattern QUOTED_TEXT_PATTERN = Pattern.compile(
            "\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.DOTALL);

    public ResumeAiServiceImpl(
            @Value("${app.ai.provider:doubao}") String provider,
            @Value("${app.ai.base-url:}") String configuredBaseUrl,
            @Value("${app.ai.model:}") String model,
            @Value("${app.ai.thinking-mode:none}") String thinkingMode,
            @Autowired SysPromptService sysPromptService,
            SysAiEngineConfigService sysAiEngineConfigService,
            ObjectMapper objectMapper,
            AiTokenLimitConfig tokenLimitConfig,
            RestClient.Builder restClientBuilder) {
        this.provider = provider == null ? "doubao" : provider.toLowerCase();
        this.model = model;
        this.configuredBaseUrl = configuredBaseUrl;
        this.thinkingMode = thinkingMode;
        this.sysPromptService = sysPromptService;
        this.sysAiEngineConfigService = sysAiEngineConfigService;
        this.objectMapper = objectMapper;
        this.tokenLimitConfig = tokenLimitConfig;
        this.restClientBuilder = restClientBuilder;
        this.resolvedBaseUrl = resolveBaseUrl(this.provider, configuredBaseUrl);
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
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            log.debug("使用用户配置的 baseUrl: {}", configuredUrl);
            return configuredUrl;
        }
        log.debug("用户未配置 baseUrl，使用默认值");
        return switch (provider) {
            case "doubao", "openai" -> "https://ark.cn-beijing.volces.com/api/v3";
            case "qwen" -> "https://dashscope.aliyuncs.com/compatible-mode/v1";
            case "ernie" -> "https://qianfan.baidubce.com/v2";
            case "deepseek" -> "https://api.deepseek.com";
            case "minimax" -> "https://api.minimax.chat/v1";
            default -> "https://ark.cn-beijing.volces.com/api/v3";
        };
    }

    @Override
    public String diagnose(String resumeText) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig();
        String tag = runtimeConfig.provider().toUpperCase();
        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException("简历文本不能为空");
        }
        String apiKey = runtimeConfig.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请在管理端配置简历 AI 密钥，或设置环境变量 "
                    + getEnvKeyName(runtimeConfig.provider()) + " / API_KEY");
        }
        // 【Token 优化】步骤1：压缩简历文本（去除冗余空白、重复内容、章节优化）
        String compressedResume = compressResumeIfEnabled(resumeText, tag);
        String systemPrompt = resolveSystemPrompt(tag);
        String userPrompt = buildUserPrompt(compressedResume);

        // 【Token 优化】步骤2：预估输入 token 数（系统 Prompt + 用户 Prompt）
        int systemTokens = TokenEstimator.estimateTokens(systemPrompt);
        int userTokens = TokenEstimator.estimateTokens(userPrompt);
        int totalTokens = systemTokens + userTokens;
        log.info("[{}] 简历诊断调用, 原始简历长度: {}, 压缩后长度: {}, 预估token: {}(system:{}, user:{})",
                tag, resumeText.length(), compressedResume.length(), totalTokens, systemTokens, userTokens);

        // 【Token 优化】步骤3：若启用 token 限制且超过阈值，自动截断简历文本
        if (tokenLimitConfig.isTokenLimitEnabled()) {
            int maxTokens = tokenLimitConfig.getResumeDiagnosisMax();
            if (totalTokens > maxTokens) {
                log.warn("[{}] 简历诊断token预估({})超过限制({})，自动截断简历文本", tag, totalTokens, maxTokens);
                // 为简历文本分配剩余 token 空间（总限制 - 系统 Prompt - 500 字安全余量）
                int resumeMaxTokens = maxTokens - systemTokens - 500;
                compressedResume = TokenEstimator.safeTruncate(compressedResume, Math.max(500, resumeMaxTokens));
                userPrompt = buildUserPrompt(compressedResume);
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
        request.thinking = buildThinkingConfig(runtimeConfig.model(), thinkingMode);
        try {
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            log.info("[{}] ║  简历诊断请求参数验证  ║", tag);
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            log.info("[{}] 请求地址: {}{}", tag, runtimeConfig.baseUrl(), runtimeConfig.endpoint());
            log.info("[{}] model: {}", tag, runtimeConfig.model());
            log.info("[{}] 配置来源: {}", tag, runtimeConfig.source());
            if (request.thinking != null) {
                log.info("[{}] thinking.type: {}", tag, request.thinking.type);
            } else {
                log.info("[{}] thinking: 未设置", tag);
            }
            log.info("[{}] ═══════════════════════════════════════════════", tag);
            RestClient runtimeRestClient = restClientBuilder
                    .baseUrl(runtimeConfig.baseUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
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
            log.info("[{}] 简历诊断成功, responseLength: {}",
                    tag, result == null ? 0 : result.length());
            return extractJsonFromResponse(result);
        } catch (Exception e) {
            log.error("[{}] 简历诊断失败", tag, e);
            throw new RuntimeException("AI 简历诊断失败: " + e.getMessage(), e);
        }
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
            String dbApiKey = normalizeConfigValue(activeConfig.getApiKey());
            if (dbApiKey != null) {
                runtimeApiKey = dbApiKey;
            } else {
                log.debug("数据库 apiKey 为空，使用本地兜底");
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
                source);
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
        return getDefaultSystemPrompt();
    }

    private String getDefaultSystemPrompt() {
        return """
                角色：跨行业资深职业顾问。任务：严格诊断简历问题。
                原则：1)根据简历实际岗位方向评价，不预设技术岗标准 2)优点缺点都直说 3)项目必须有业务价值+量化成果 4)学历放宽但项目要硬。
                评分标准：S(90+)顶尖 A(75-89)优秀 B(60-74)合格 C(40-59)偏弱 D(<40)问题严重。多数简历应在B-C区间，仅真正出色者得A以上。
                权重：基本信息10% 岗位核心能力15% 工作25% 项目40% 教育10%。
                跨行业原则：按岗位方向评价核心能力，不默认技术标准。与岗位无关的字段不扣分。
                规则：只返回JSON，无额外文本；JSON完整闭合；缺信息返回null/空数组；basicInfoDetails从原文提取真实值。
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

                        summary要求：写一段200-350字的简历总结评价。必须包含：先肯定简历中具体的优势（如哪些项目写得好、哪些能力突出），再指出具体的问题（哪些描述空洞、哪些维度缺失、哪些地方需要改进）。语气专业客观，有理有据，不要泛泛而谈。

                        返回JSON格式(不要额外文本)：
                        {"overallEvaluation":{"totalScore":0-100,"level":"S/A/B/C/D","summary":"200-350字详细评价，先说优点再说问题"},
                        "highlights":["亮点1"],
                        "basicInfoEvaluation":{"score":0-100,"hasName":true/false,"hasPhone":true/false,"hasEmail":true/false,"hasGithub":true/false,"hasBlog":true/false,"suggestions":["建议1"]},
                        "basicInfoDetails":{"name":"","email":"","phone":"","location":"","currentCompany":"","github":"","blog":""},
                        "skillEvaluation":{"score":0-100,"skillList":[""],"strengths":[""],"weaknesses":[""],"suggestions":[""]},
                        "workExperienceEvaluation":{"score":0-100,"totalYears":0,"companyCount":0,"hasQuantifiableResults":true/false,"experiences":[{"company":"","position":"","duration":"","highlights":[""]}],"suggestions":[""]},
                        "projectExperienceEvaluation":{"score":0-100,"projectCount":0,"hasTechStack":true/false,"hasResponsibilities":true/false,"projects":[{"name":"","role":"","techStack":"","highlights":[""]}],"suggestions":[""]},
                        "educationEvaluation":{"score":0-100,"degree":"","school":"","major":"","hasRelevantMajor":true/false,"suggestions":[""]},
                        "optimizationSuggestions":["建议1"]}
                        """;
    }

    @Override
    public ResumePolishAiResult polishResume(String resumeText, String jdText,
            ResumeJobMatchAnalyzeResponse latestJobMatchAnalysis) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig();
        String tag = runtimeConfig.provider().toUpperCase();
        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException("简历文本不能为空");
        }
        String apiKey = runtimeConfig.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请先配置简历 AI 密钥");
        }
        // 【Token 优化】步骤1：压缩简历文本和 JD 文本
        String compressedResume = compressResumeIfEnabled(resumeText, tag);
        String compressedJd = (jdText != null && !jdText.isBlank())
                ? AiInputCompressor.toStructuredFormat(jdText, AiInputCompressor.ContentType.JD)
                : jdText;
        String systemPrompt = buildResumePolishSystemPrompt();
        String userPrompt = buildResumePolishUserPrompt(compressedResume, compressedJd, latestJobMatchAnalysis);

        // 【Token 优化】步骤2：预估输入 token 数
        int systemTokens = TokenEstimator.estimateTokens(systemPrompt);
        int userTokens = TokenEstimator.estimateTokens(userPrompt);
        int totalTokens = systemTokens + userTokens;
        log.info("[{}] AI 简历润色调用, 原始简历长度: {}, 压缩后: {}, JD长度: {}, 压缩后: {}, 预估token: {}(system:{}, user:{})",
                tag, resumeText.length(), compressedResume.length(),
                jdText == null ? 0 : jdText.length(),
                compressedJd == null ? 0 : compressedJd.length(),
                totalTokens, systemTokens, userTokens);

        // 【Token 优化】步骤3：若超过润色专用 token 限制，自动截断
        if (tokenLimitConfig.isTokenLimitEnabled()) {
            // 使用简历润色专用的 token 限制（polishResumeMax），与诊断限制分离
            int maxTokens = tokenLimitConfig.getPolishResumeMax();
            if (totalTokens > maxTokens) {
                log.warn("[{}] 简历润色token预估({})超过限制({})，自动截断", tag, totalTokens, maxTokens);
                int resumeMaxTokens = maxTokens - systemTokens - 500;
                compressedResume = TokenEstimator.safeTruncate(compressedResume, Math.max(500, resumeMaxTokens));
                userPrompt = buildResumePolishUserPrompt(compressedResume, compressedJd, latestJobMatchAnalysis);
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
        request.thinking = buildThinkingConfig(runtimeConfig.model(), thinkingMode);
        try {
            RestClient runtimeRestClient = restClientBuilder
                    .baseUrl(runtimeConfig.baseUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .build();
            ResponseBody response = runtimeRestClient.post()
                    .uri(runtimeConfig.endpoint())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(ResponseBody.class);
            if (response == null || response.choices == null || response.choices.isEmpty()) {
                throw new RuntimeException("AI 润色返回内容为空");
            }
            String result = extractJsonFromResponse(response.choices.get(0).message.content);
            return parseResumePolishAiResult(result);
        } catch (Exception e) {
            log.error("[{}] AI 简历润色失败", tag, e);
            throw new RuntimeException("AI 简历润色失败: " + e.getMessage(), e);
        }
    }

    private String buildResumePolishSystemPrompt() {
        return """
                Role: senior resume editor and layout consultant. Task: rewrite the source resume into a clean Chinese resume draft that is suitable for printing or saving as PDF.
                Rules:
                1. Never invent experience, metrics, education, titles, projects, awards, or skills.
                2. Keep the tone concise, restrained, and professional. Avoid filler, hype, and generic praise.
                3. Allocate more space to the strongest and most role-relevant content instead of expanding every section evenly.
                4. Education rule: if the school or academic background is genuinely strong, such as 985, 211, Double First-Class, top overseas schools, strong ranking, GPA, or awards, keep one extra highlight line; otherwise keep education brief and factual.
                5. Ordering rule: decide section order by content strength and relevance. If internship content is stronger than project content, place internship before projects. If full-time work is strongest, place work or internship before projects. Compress weak or low-value projects.
                6. Write each experience point with action, method, and result whenever possible. Keep quantifiable outcomes, scope, ownership, and business value.
                7. The output must fit a single-column, black-and-white, easy-to-print, easy-to-edit resume layout.
                8. polishedResumeText must follow a plain-text structure that is easy for frontend rendering and manual editing:
                   - The first 1 to 3 lines only contain name, target role or short headline, and contact information
                   - Section titles must stay in Chinese and only use necessary items from: \u57fa\u672c\u4fe1\u606f, \u6559\u80b2\u80cc\u666f, \u5de5\u4f5c\u7ecf\u5386, \u5b9e\u4e60\u7ecf\u5386, \u9879\u76ee\u7ecf\u5386, \u804c\u4e1a\u6280\u80fd, \u8363\u8a89\u5956\u9879, \u81ea\u6211\u8bc4\u4ef7
                   - Main education or experience lines use: name | role/major/degree | date
                   - Supporting lines use: \u6807\u7b7e\uff1a\u5185\u5bb9
                   - Achievement bullets use: - content
                9. Output JSON only. Do not output Markdown code fences or explanatory text.
                Format: {"polishedResumeText":"final resume text","modificationNotes":["note1","note2","note3"]}
                """;
    }

    private String buildResumePolishUserPrompt(String resumeText, String jdText,
            ResumeJobMatchAnalyzeResponse latestJobMatchAnalysis) {
        StringBuilder builder = new StringBuilder();
        builder.append("Please polish the following resume into a concise Chinese resume draft.\n");
        builder.append("[Original Resume]\n").append(resumeText).append("\n\n");
        if (jdText != null && !jdText.isBlank()) {
            builder.append("[Target JD]\n").append(jdText).append("\n\n");
        }
        if (latestJobMatchAnalysis != null) {
            builder.append("[Latest Job Match Analysis]\n");
            builder.append("Match score: ").append(latestJobMatchAnalysis.getMatchScore()).append("\n");
            builder.append("Matched keywords: ").append(latestJobMatchAnalysis.getMatchedKeywords()).append("\n");
            // Filter out useless generic keywords so they do not mislead the rewrite direction.
            List<String> filteredMissing = latestJobMatchAnalysis.getMissingKeywords() == null
                    ? List.of()
                    : latestJobMatchAnalysis.getMissingKeywords().stream()
                            .filter(kw -> kw != null && !kw.isBlank() && !kw.equalsIgnoreCase("JD"))
                            .toList();
            builder.append("Missing keywords: ").append(filteredMissing).append("\n");
            builder.append("Optimization suggestions: ").append(latestJobMatchAnalysis.getSuggestions()).append("\n\n");
        }
        builder.append("""
                Please generate a version that is better for job application delivery and PDF-style resume layout.
                1. Allocate space by content strength instead of expanding every module evenly.
                2. If education is a true advantage, keep one more highlight line. If education is ordinary, keep it short.
                3. If internship content is stronger or more relevant than project content, place internship before projects. If projects are stronger, projects may come first, but the decision must be content-based.
                4. If a JD is provided, make the resume more targeted without inventing experience.
                5. The output should look like a real deliverable resume draft rather than a generic AI summary.
                6. Use Chinese section titles and Chinese writing for the final polishedResumeText.
                7. modificationNotes must contain at least 3 items and clearly explain changes in structure, ordering, and emphasis.
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
            return objectMapper.readValue(rawJson, ResumePolishAiResult.class);
        } catch (Exception ex) {
            log.warn("AI 润色结果严格 JSON 解析失败，进入容错解析。rawPreview: {}",
                    buildRawPreview(rawJson), ex);
        }
        JsonNode rootNode = tryReadJsonNode(rawJson);
        if (rootNode != null) {
            ResumePolishAiResult jsonNodeResult = new ResumePolishAiResult();
            jsonNodeResult.setPolishedResumeText(readTextNode(rootNode, "polishedResumeText"));
            jsonNodeResult.setModificationNotes(readTextListNode(rootNode, "modificationNotes"));
            if (isValidPolishResult(jsonNodeResult)) {
                return jsonNodeResult;
            }
        }
        ResumePolishAiResult regexFallbackResult = new ResumePolishAiResult();
        regexFallbackResult.setPolishedResumeText(extractPolishedResumeText(rawJson));
        regexFallbackResult.setModificationNotes(extractModificationNotes(rawJson));
        if (isValidPolishResult(regexFallbackResult)) {
            return regexFallbackResult;
        }
        throw new IllegalStateException("AI 润色结果解析失败，返回内容不是可用的结构化结果");
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
        } catch (Exception ignored) {
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
                && !result.getPolishedResumeText().isBlank()
                && result.getModificationNotes() != null
                && !result.getModificationNotes().isEmpty();
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

    private record RuntimeAiConfig(
            String provider,
            String model,
            String baseUrl,
            String endpoint,
            String apiKey,
            String source) {
    }

    private static class RequestBody {
        public String model;
        public List<Message> messages;
        public Thinking thinking;

        public RequestBody() {
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

        public static class Choice {
            public MessageContent message;

            public static class MessageContent {
                public String content;
            }
        }
    }
}
