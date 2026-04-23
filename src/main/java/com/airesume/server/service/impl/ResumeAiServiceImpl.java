package com.airesume.server.service.impl;

import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.entity.SysAiEngineConfig;
import com.airesume.server.service.ResumeAiService;
import com.airesume.server.service.SysAiEngineConfigService;
import com.airesume.server.service.SysPromptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;

/**
 * 简历诊断 AI 服务实现类（真实 AI 模式）
 *
 * 所属模块：简历诊断模块 - AI 接入层
 * 职责：调用大模型 API 生成简历诊断结果
 * 激活条件：当 app.ai.mode=real 时激活，替代 MockResumeAiServiceImpl
 * 依赖：SysPromptService（Prompt 管理）、RestClient（HTTP 客户端）
 *
 * 【重要】baseUrl 配置说明：
 * - 优先使用配置文件中的 app.ai.base-url
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

    /**
     * 构造函数，初始化简历诊断 AI 服务
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
     * @param provider          AI 提供商（从配置 app.ai.provider 读取，默认 doubao）
     * @param configuredBaseUrl API 基础地址（从配置 app.ai.base-url 读取，用户原始配置）
     * @param model            模型名称（从配置 app.ai.model 读取）
     * @param thinkingMode     思考模式（从配置 app.ai.thinking-mode 读取，默认 none）
     * @param sysPromptService Prompt 服务（从数据库读取 Prompt）
     * @param restClientBuilder RestClient 构造器
     */
    public ResumeAiServiceImpl(
            @Value("${app.ai.provider:doubao}") String provider,
            @Value("${app.ai.base-url:}") String configuredBaseUrl,
            @Value("${app.ai.model:}") String model,
            @Value("${app.ai.thinking-mode:none}") String thinkingMode,
            @Autowired SysPromptService sysPromptService,
            SysAiEngineConfigService sysAiEngineConfigService,
            RestClient.Builder restClientBuilder) {
        this.provider = provider == null ? "doubao" : provider.toLowerCase();
        this.model = model;
        this.configuredBaseUrl = configuredBaseUrl;
        this.thinkingMode = thinkingMode;
        this.sysPromptService = sysPromptService;
        this.sysAiEngineConfigService = sysAiEngineConfigService;
        this.restClientBuilder = restClientBuilder;

        // 【重要】优先使用用户配置的 baseUrl，仅当配置为空时才使用默认值
        this.resolvedBaseUrl = resolveBaseUrl(this.provider, configuredBaseUrl);

        // 获取 endpoint
        this.endpoint = getEndpoint();

        // 初始化 RestClient，严格使用 resolvedBaseUrl
        this.restClient = restClientBuilder
                .baseUrl(this.resolvedBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();

        String tag = this.provider.toUpperCase();

        // 【启动日志】详细打印所有配置信息
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
            case "qwen"     -> "https://dashscope.aliyuncs.com/compatible-mode/v1";
            case "ernie"    -> "https://qianfan.baidubce.com/v2";
            case "deepseek" -> "https://api.deepseek.com";
            case "minimax"  -> "https://api.minimax.chat/v1";
            default -> "https://ark.cn-beijing.volces.com/api/v3";
        };
    }

    /**
     * 调用 AI 进行简历诊断
     *
     * 功能：传入简历文本，调用 AI API 生成结构化诊断结果
     *
     * @param resumeText 简历文本（从 PDF 提取）
     * @return JSON 格式的诊断结果字符串
     * @throws IllegalArgumentException 简历文本为空时抛出
     * @throws IllegalStateException API Key 未设置时抛出
     * @throws RuntimeException AI 调用失败时抛出
     *
     * 调用时机：用户上传 PDF 简历后，异步任务调用此方法
     * 副作用：调用外部 AI API，消耗 API 额度
     */
    @Override
    public String diagnose(String resumeText) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig();
        String tag = runtimeConfig.provider().toUpperCase();
        log.info("[{}] 简历诊断调用, resumeTextLength: {}",
                tag, resumeText == null ? 0 : resumeText.length());

        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException("简历文本不能为空");
        }

        String apiKey = runtimeConfig.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请在管理端配置简历 AI 密钥，或设置环境变量 "
                    + getEnvKeyName(runtimeConfig.provider()) + " / API_KEY");
        }

        String systemPrompt = resolveSystemPrompt(tag);

        RequestBody request = new RequestBody();
        request.model = runtimeConfig.model();
        request.messages = List.of(
                new Message("system", systemPrompt),
                new Message("user", buildUserPrompt(resumeText))
        );

        // 根据配置和模型支持情况设置 thinking 参数
        request.thinking = buildThinkingConfig(runtimeConfig.model(), thinkingMode);

        try {
            // 【关键日志】打印完整请求参数
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
            // 按当前生效配置动态创建客户端，确保管理端切换后马上生效。
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

    /**
     * 获取环境变量名
     *
     * @return 对应 provider 的环境变量名
     */
    private String getEnvKeyName(String providerType) {
        String normalizedProvider = normalizeConfigValue(providerType);
        if (normalizedProvider == null) {
            normalizedProvider = "doubao";
        }
        normalizedProvider = normalizedProvider.toLowerCase(Locale.ROOT);
        return switch (normalizedProvider) {
            case "doubao"  -> "DOUBAO_API_KEY";
            case "qwen"    -> "DASHSCOPE_API_KEY";
            case "ernie"   -> "ERNIE_API_KEY";
            case "deepseek" -> "DEEPSEEK_API_KEY";
            case "minimax" -> "MINIMAX_API_KEY";
            default        -> "AI_API_KEY";
        };
    }

    /**
     * 读取 API Key
     *
     * 优先级：DOUBAO_API_KEY > API_KEY > AI_API_KEY
     *
     * @return API Key，未设置时返回 null
     */
    private String resolveApiKey(String providerType) {
        String key = System.getenv(getEnvKeyName(providerType));
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
     * 解析简历业务运行时 AI 配置。
     *
     * 作用：
     * 1. 优先读取管理端“当前激活”的 resume 配置；
     * 2. 缺失字段时回退到本地配置与环境变量，保证链路稳定。
     */
    private RuntimeAiConfig resolveRuntimeConfig() {
        // 本地兜底配置：数据库没有激活配置时仍可继续服务。
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

        // 优先应用管理端激活配置，确保切换后无需重启即可生效。
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
            }
            source = "db-active:" + activeConfig.getEngineCode();
        }

        // 最后兜底：避免关键字段为空导致请求参数不可用。
        if (runtimeModel == null) {
            runtimeModel = fallbackModel;
        }
        if (runtimeBaseUrl == null) {
            runtimeBaseUrl = fallbackBaseUrl;
        }
        if (runtimeApiKey == null) {
            runtimeApiKey = resolveApiKey(runtimeProvider);
        }

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
     * 按 provider 获取 endpoint，预留后续差异化扩展能力。
     */
    private String getEndpointByProvider(String providerType) {
        return switch (providerType) {
            case "ernie" -> "/chat/completions";
            default -> "/chat/completions";
        };
    }

    /**
     * 统一处理配置字符串空值，减少各处重复判空逻辑。
     */
    private String normalizeConfigValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 解析系统 Prompt
     *
     * 优先从数据库读取（sys_prompt 表，scenario_type=RESUME）
     * 数据库没有时使用内置默认 Prompt
     *
     * @param tag 日志标签
     * @return 系统 Prompt 文本
     */
    private String resolveSystemPrompt(String tag) {
        String dbPrompt = sysPromptService.getActivePromptContent(ResumeDiagnosisConstants.SCENARIO_TYPE_RESUME);
        if (dbPrompt != null && !dbPrompt.isBlank()) {
            log.info("[{}] 使用数据库中的简历诊断Prompt, length: {}", tag, dbPrompt.length());
            return dbPrompt;
        }
        log.warn("[{}] 数据库中未找到启用的简历诊断Prompt，使用内置默认Prompt", tag);
        return getDefaultSystemPrompt();
    }

    /**
     * 获取默认系统 Prompt
     *
     * @return 默认系统 Prompt 文本
     */
    private String getDefaultSystemPrompt() {
        return """
                你是一个专业的简历诊断分析师，擅长评估简历质量并给出改进建议。

                你的职责：
                1. 严格分析用户提供的简历内容
                2. 按照要求的JSON格式返回结构化诊断结果
                3. 评分要客观公正，结合简历完整度、表述清晰度、内容量化程度综合评估

                重要规则：
                - 只返回JSON，不要有任何额外解释、问候语或结尾总结
                - 不要在JSON外添加任何markdown代码块标记（如 ``` json）
                - JSON字段必须完整，所有数组和对象都要正确闭合
                - 如果简历中缺少某项信息，对应字段返回null或空数组，不要编造内容
                - basicInfoDetails 字段必须从简历原文中提取真实值，提取不到返回空字符串
                """;
    }

    /**
     * 构建用户 Prompt
     *
     * @param resumeText 简历文本
     * @return 用户 Prompt 文本
     */
    private String buildUserPrompt(String resumeText) {
        return """
                请对以下简历内容进行诊断分析，并返回结构化的评价结果。

                简历内容：
                """ + resumeText + """

                请严格按照以下JSON格式返回分析结果，不要添加任何其他内容：

                {
                  "overallEvaluation": {
                    "totalScore": 综合分数(0-100整数),
                    "level": "等级，如S/A/B/C/D",
                    "summary": "一段话的总体评价"
                  },
                  "highlights": ["亮点1", "亮点2", "亮点3"],
                  "basicInfoEvaluation": {
                    "score": 分数(0-100整数),
                    "hasName": 是否包含姓名(true或false),
                    "hasPhone": 是否包含手机号(true或false),
                    "hasEmail": 是否包含邮箱(true或false),
                    "hasGithub": 是否有GitHub链接(true或false),
                    "hasBlog": 是否有博客链接(true或false),
                    "suggestions": ["针对基本信息格式的建议1", "建议2"]
                  },
                  "basicInfoDetails": {
                    "name": "姓名（从简历原文中提取，提取不到返回空字符串）",
                    "email": "邮箱（从简历原文中提取，提取不到返回空字符串）",
                    "phone": "电话（从简历原文中提取，提取不到返回空字符串）",
                    "location": "所在地（从简历原文中提取，提取不到返回空字符串）",
                    "currentCompany": "当前公司（从简历原文中提取，提取不到返回空字符串）",
                    "github": "GitHub链接（从简历原文中提取，提取不到返回空字符串）",
                    "blog": "博客/网站链接（从简历原文中提取，提取不到返回空字符串）"
                  },
                  "skillEvaluation": {
                    "score": 分数(0-100整数),
                    "skillList": ["技能1", "技能2", "技能3"],
                    "strengths": ["技能相关亮点1"],
                    "weaknesses": ["技能相关不足1"],
                    "suggestions": ["技能提升建议1"]
                  },
                  "workExperienceEvaluation": {
                    "score": 分数(0-100整数),
                    "totalYears": 工作年限(整数),
                    "companyCount": 公司数量(整数),
                    "hasQuantifiableResults": 是否有量化成果描述(true或false),
                    "experiences": [
                      {
                        "company": "公司名",
                        "position": "职位",
                        "duration": "在职时长",
                        "highlights": ["工作亮点1"]
                      }
                    ],
                    "suggestions": ["工作经历改进建议1"]
                  },
                  "projectExperienceEvaluation": {
                    "score": 分数(0-100整数),
                    "projectCount": 项目数量(整数),
                    "hasTechStack": 是否明确写出技术栈(true或false),
                    "hasResponsibilities": 是否写明职责(true或false),
                    "projects": [
                      {
                        "name": "项目名称",
                        "role": "个人角色",
                        "techStack": "技术栈",
                        "highlights": ["项目成果1"]
                      }
                    ],
                    "suggestions": ["项目经历改进建议1"]
                  },
                  "optimizationSuggestions": ["综合优化建议1", "优化建议2", "优化建议3"]
                }

                【重要提示】basicInfoDetails 字段必须严格从简历原文中提取：
                - 能提取到真实值则返回真实内容
                - 提取不到则返回空字符串 ""
                - 绝对不要编造或猜测任何内容
                """;
    }

    /**
     * 从 AI 响应中提取 JSON
     *
     * 处理 AI 可能返回的 markdown 代码块标记
     *
     * @param raw AI 原始响应
     * @return 提取后的 JSON 字符串
     */
    private String extractJsonFromResponse(String raw) {
        if (raw == null) return raw;
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

    /**
     * 简历 AI 运行时配置快照。
     * 用于把数据库激活配置和本地兜底配置合并后传入单次请求。
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

    /**
     * API 请求体
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
     * 单个消息
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
     * API 响应体
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
