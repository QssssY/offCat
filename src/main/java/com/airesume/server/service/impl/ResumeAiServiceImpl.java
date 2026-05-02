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

    @Override
    public String diagnoseJobMatch(String resumeText, String jdText) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig();
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
        request.thinking = buildThinkingConfig(runtimeConfig.model(), thinkingMode);
        try {
            log.info("[{}] JD 匹配分析请求, model: {}, 配置来源: {}",
                    tag, runtimeConfig.model(), runtimeConfig.source());
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
            log.info("[{}] JD 匹配分析成功, responseLength: {}",
                    tag, result == null ? 0 : result.length());
            return extractJsonFromResponse(result);
        } catch (Exception e) {
            log.error("[{}] JD 匹配分析失败", tag, e);
            throw new RuntimeException("AI JD 匹配分析失败: " + e.getMessage(), e);
        }
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
                得分明细规则：每个维度（basicInfoEvaluation、skillEvaluation、workExperienceEvaluation、projectExperienceEvaluation、educationEvaluation）必须包含strengths和weaknesses数组，strengths列出2-4条加分项（做得好的具体方面），weaknesses列出2-4条扣分项（需要改进的具体方面及扣分原因），每项一句话简洁具体，不得为空数组。
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
                        6.每个维度必须包含evaluation字段：一段80-150字的评价文本，结构为"先说明分数由来→列出主要加分项→列出主要扣分项→给出改进建议"，语气专业客观，不要泛泛而谈。

                        summary要求：写一段200-350字的简历总结评价。必须包含：先肯定简历中具体的优势（如哪些项目写得好、哪些能力突出），再指出具体的问题（哪些描述空洞、哪些维度缺失、哪些地方需要改进）。语气专业客观，有理有据，不要泛泛而谈。

                        返回JSON格式(不要额外文本)：
                        {"overallEvaluation":{"totalScore":0-100,"level":"S/A/B/C/D","summary":"200-350字详细评价，先说优点再说问题","evaluation":"80-150字综合评价，说明分数由来、主要优缺点及改进建议","strengths":["整体优势1","整体优势2"],"weaknesses":["整体不足1","整体不足2"]},
                        "highlights":["亮点1"],
                        "basicInfoEvaluation":{"score":0-100,"hasName":true/false,"hasPhone":true/false,"hasEmail":true/false,"hasGithub":true/false,"hasBlog":true/false,"evaluation":"80-150字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":["建议1"]},
                        "basicInfoDetails":{"name":"","email":"","phone":"","location":"","currentCompany":"","github":"","blog":""},
                        "skillEvaluation":{"score":0-100,"skillList":[""],"evaluation":"80-150字评价文本","strengths":[""],"weaknesses":[""],"suggestions":[""]},
                        "workExperienceEvaluation":{"score":0-100,"totalYears":0,"companyCount":0,"hasQuantifiableResults":true/false,"experiences":[{"company":"","position":"","duration":"","highlights":[""]}],"evaluation":"80-150字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":[""]},
                        "projectExperienceEvaluation":{"score":0-100,"projectCount":0,"hasTechStack":true/false,"hasResponsibilities":true/false,"projects":[{"name":"","role":"","techStack":"","highlights":[""]}],"evaluation":"80-150字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":[""]},
                        "educationEvaluation":{"score":0-100,"degree":"","school":"","major":"","hasRelevantMajor":true/false,"evaluation":"80-150字评价文本","strengths":["加分项"],"weaknesses":["扣分项"],"suggestions":[""]},
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
        String systemPrompt = buildResumePolishSystemPromptV2();
        String userPrompt = buildResumePolishUserPromptV2(compressedResume, compressedJd, latestJobMatchAnalysis);

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
                userPrompt = buildResumePolishUserPromptV2(compressedResume, compressedJd, latestJobMatchAnalysis);
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

    /**
     * AI 润色提示词 V2：
     * 强制 AI 输出统一章节和稳定头部格式，避免不同岗位简历使用大量近义标题导致前端渲染失稳。
     */
    private String buildResumePolishSystemPromptV2() {
        return """
                角色：中文求职简历优化顾问。
                任务：在不编造任何信息的前提下，把原始简历改写成更适合单页正式投递、且便于前端模板稳定渲染的纯文本简历。

                强制规则：
                1. 只能返回 JSON，禁止输出 JSON 之外的解释、Markdown、代码块。
                2. JSON 结构固定为 {"polishedResumeText":"...","modificationNotes":["..."]}。
                3. polishedResumeText 必须是纯文本简历，不要使用 Markdown 标题、表格、额外说明文字。
                4. 章节标题只能从以下集合中选择，并按该顺序输出存在内容的章节：
                   个人信息
                   教育背景
                   实习经历 或 工作经历（二选一）
                   项目经历
                   专业技能
                   校园经历
                   荣誉证书
                   个人评价
                5. 严禁额外发明近义标题，例如：基本信息、求职意向、专业能力、核心优势、教学实践、科研与实践经历、项目成果、技术资质、职业优势等；这些内容必须并入上述标准章节内部。
                6. 个人信息区格式必须稳定：
                   第一行只写姓名；
                   第二行写求职方向/目标岗位，没有则省略；
                   后续行只写联系方式与基础信息，每行 1-2 项，可使用“ | ”连接；
                   不要写“AI润色简历”等字样，不要提及照片占位。
                7. 如果原始简历中同时出现科研、教学实践、课程实践、校内实践、项目成果、技能证书等内容，必须按语义拆入标准章节：
                   科研课题/建模/开发项目/产品项目 -> 项目经历
                   实习/工作/任职/助教岗位职责 -> 实习经历或工作经历
                   校内实践/课程实践/社团/学生工作/教学辅助 -> 校园经历
                   奖项/证书/资质 -> 荣誉证书
                8. 教育背景需根据内容自适应篇幅：
                   985/211/双一流/重点院校/硕博/高匹配专业可以适当展开；
                   学校普通或信息价值较低时，只保留关键信息，不堆砌描述。
                9. 经历描述优先采用“动作 + 方法 + 结果”表达，突出岗位相关能力与成果，但绝对不能虚构经历、技术栈、数据或奖项。
                10. 删除原文中的占位说明、可补充提示、AI 生成提示、排版备注等非正式投递内容。
                11. modificationNotes 至少输出 3 条，说明本次结构归一、内容取舍和表达优化原因。
                """;
    }

    /**
     * 用户提示词 V2：
     * 结合 JD 与最近一次匹配结果，要求 AI 主动完成章节归一和跨岗位内容拆分。
     */
    private String buildResumePolishUserPromptV2(String resumeText, String jdText,
            ResumeJobMatchAnalyzeResponse latestJobMatchAnalysis) {
        StringBuilder builder = new StringBuilder();
        builder.append("请对以下简历进行润色优化，并严格输出标准化章节。\n");
        builder.append("【原始简历】\n").append(resumeText).append("\n\n");
        if (jdText != null && !jdText.isBlank()) {
            builder.append("【目标岗位 JD】\n").append(jdText).append("\n\n");
        }
        if (latestJobMatchAnalysis != null) {
            builder.append("【最近一次岗位匹配分析】\n");
            builder.append("匹配度评分：").append(latestJobMatchAnalysis.getMatchScore()).append("\n");
            builder.append("已匹配关键词：").append(latestJobMatchAnalysis.getMatchedKeywords()).append("\n");
            // 过滤无效的泛化关键词，避免误导简历润色方向。
            List<String> filteredMissing = latestJobMatchAnalysis.getMissingKeywords() == null
                    ? List.of()
                    : latestJobMatchAnalysis.getMissingKeywords().stream()
                            .filter(kw -> kw != null && !kw.isBlank() && !kw.equalsIgnoreCase("JD"))
                            .toList();
            builder.append("缺失关键词：").append(filteredMissing).append("\n");
            builder.append("优化建议：").append(latestJobMatchAnalysis.getSuggestions()).append("\n\n");
        }
        builder.append("""
                请输出更适合投递的版本，并满足以下要求：
                1. 对标题体系做归一化，只保留标准章节标题，不得保留原文中的近义标题。
                2. 如果原文出现“科研与实践”“教学实践”“课程实践”“项目成果”“技术资质”“职业优势”等表达，请按语义并入标准章节，而不是原样保留成大标题。
                3. 如果教育背景竞争力强，可以适度展开院校与学术亮点；如果教育背景普通，则保持简洁，把篇幅优先给更有说服力的经历与成果。
                4. 如果提供了 JD，请优先突出与 JD 最相关的能力、经历与成果，但不能杜撰。
                5. 如果缺少实习/工作经历，不要硬编；可将校内实践、课程实践、科研项目分别整理到“校园经历”或“项目经历”。
                6. 所有经历内容保持简洁专业，避免空泛套话，避免“负责开发”“参与项目”这类没有方法和结果支撑的弱表达。
                7. modificationNotes 至少输出 3 条，且要让用户看懂你做了哪些结构和表达层面的调整。
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
