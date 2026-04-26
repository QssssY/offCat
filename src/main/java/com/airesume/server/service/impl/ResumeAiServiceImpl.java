package com.airesume.server.service.impl;

import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.dto.resume.ResumeJobMatchAnalyzeResponse;
import com.airesume.server.dto.resume.ResumePolishAiResult;
import com.airesume.server.entity.SysAiEngineConfig;
import com.airesume.server.service.ResumeAiService;
import com.airesume.server.service.SysAiEngineConfigService;
import com.airesume.server.service.SysPromptService;
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
    private final ObjectMapper objectMapper;
    private static final Pattern POLISHED_TEXT_PATTERN = Pattern.compile(
            "\"polishedResumeText\"\\s*:\\s*\"(.*?)\"\\s*,\\s*\"modificationNotes\"",
            Pattern.DOTALL
    );
    private static final Pattern MODIFICATION_NOTES_PATTERN = Pattern.compile(
            "\"modificationNotes\"\\s*:\\s*\\[(.*?)]",
            Pattern.DOTALL
    );
    private static final Pattern QUOTED_TEXT_PATTERN = Pattern.compile(
            "\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.DOTALL
    );

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
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder) {
        this.provider = provider == null ? "doubao" : provider.toLowerCase();
        this.model = model;
        this.configuredBaseUrl = configuredBaseUrl;
        this.thinkingMode = thinkingMode;
        this.sysPromptService = sysPromptService;
        this.sysAiEngineConfigService = sysAiEngineConfigService;
        this.objectMapper = objectMapper;
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
     * 1. 优先读取管理端"当前激活"的 resume 配置；
     * 2. 缺失字段时回退到本地配置与环境变量，保证链路稳定。
     *
     * 【运行时配置读取优先级】（确保 403 问题不再复现）：
     * 1. 优先读取数据库激活配置的 apiKey（可能是 SiliconFlow 等新 provider）
     * 2. 兜底读取本地环境变量的 apiKey
     * 3. 最后兜底到本地配置 model（避免空值导致请求失败）
     *
     * 【关键修复】对 runtimeApiKey 和关键字段补充兜底处理：
     * - 旧逻辑：仅当数据库返回非空时才覆盖，不为空时保持 null，导致后续请求带空 key
     * - 新逻辑：始终确保关键字段不为 null，防止 403 Forbidden
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
            // 【关键修复】只要数据库返回的 apiKey 非空就使用，否则继续使用本地兜底
            if (dbApiKey != null) {
                runtimeApiKey = dbApiKey;
            } else {
                // 数据库 apiKey 为空时，保持使用本地兜底，不要覆盖为 null
                log.debug("数据库 apiKey 为空，使用本地兜底");
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
            log.warn("[RESUME] runtimeApiKey 仍为空，尝试从环境变量兜底获取");
            runtimeApiKey = resolveApiKey(runtimeProvider);
        }
// 4. 如果所有兜底都失败，当前 provider 不可用（抛出明确错误而非静默失败）
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
                你是一位拥有10年大厂招聘经验的资深HRBP，精通互联网大厂技术岗位招聘。
                你的任务是以大厂HR的严格视角，对简历进行深度诊断和问题暴露。

                诊断原则：
                1. 严苛标准：按大厂P6/P7级技术岗位要求评估，不符合岗位价值的一律指出
                2. 实话实说：优点要明确说优点，缺点要毫不留情地暴露，不当老好人
                3. 项目为王：项目经历必须有实际业务价值、技术挑战和可量化成果，空洞描述一律批评
                4. 量化优先：所有成果必须用数据说话，无法量化的成果要扣分
                5. 学历宽容：对学历要求放宽，二本/专科有亮点项目可接受，但项目内容必须硬

                评估维度权重：
                - 基本信息(10%)：联系方式完整即可，不强求GitHub/Blog
                - 技术技能(15%)：技术栈深度和匹配度，有新技术加分
                - 工作经历(25%)：业务价值、技术深度、成长性
                - 项目经历(40%)：核心重点，业务意义、技术难点、量化成果、个人贡献
                - 教育学历(10%)：可以放宽，但有清晰的教育背景可追溯

                重要规则：
                - 只返回JSON，不要有任何额外解释、问候语或结尾总结
                - 不要在JSON外添加任何markdown代码块标记
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
                请对以下简历内容进行严格的HR视角诊断，暴露所有问题和不足。

                简历内容：
                """ + resumeText + """

                诊断要求：
                1. 项目经历是核心评审重点，必须严格审视：
                   - 项目是否有真实业务价值？解决了什么问题？
                   - 技术挑战在哪里？有什么技术难点？
                   - 成果如何量化？数据是否可信？
                   - 个人贡献是什么？是否是核心参与者？
                   - 如项目描述空洞（如"负责XXX功能开发"）一律扣分
                2. 工作经历需评估：
                   - 业务贡献是否具体可量化？
                   - 技术深度是否足够？
                   - 是否有成长性？
                3. 技能评估需检查技术栈匹配度和深度
                4. 学历可放宽但必须有清晰的Timeline

                请严格按照以下JSON格式返回分析结果，不要添加任何其他内容：

                {
                  "overallEvaluation": {
                    "totalScore": 综合分数(0-100整数),
                    "level": "等级，如S/A/B/C/D",
                    "summary": "一段话的总体评价，必须明确指出不足"
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
                    "weaknesses": ["技能相关不足1，如：技术栈偏旧、缺少主流技术、深度不足等"],
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
                    "suggestions": ["项目经历改进建议，如：缺乏业务价值、无量化数据、技术挑战不足等"]
                  },
                  "optimizationSuggestions": ["综合优化建议1", "优化建议2", "优化建议3"]
                }

                【关键】项目经历的评审重点：
                - 避免"负责XX功能开发"这种空洞描述
                - 必须有业务背景、技术难点、个人贡献、量化成果
                - 如果项目描述无法体现以上任何一点，score要从严给分
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
    @Override
    public ResumePolishAiResult polishResume(String resumeText, String jdText, ResumeJobMatchAnalyzeResponse latestJobMatchAnalysis) {
        RuntimeAiConfig runtimeConfig = resolveRuntimeConfig();
        String tag = runtimeConfig.provider().toUpperCase();
        log.info("[{}] AI 简历润色调用, resumeTextLength: {}, hasJd: {}",
                tag, resumeText == null ? 0 : resumeText.length(), jdText != null && !jdText.isBlank());

        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException("简历文本不能为空");
        }

        String apiKey = runtimeConfig.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请先配置简历 AI 密钥");
        }

        RequestBody request = new RequestBody();
        request.model = runtimeConfig.model();
        request.messages = List.of(
                new Message("system", buildResumePolishSystemPrompt()),
                new Message("user", buildResumePolishUserPrompt(resumeText, jdText, latestJobMatchAnalysis))
        );
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
     * 润色场景固定输出结构化 JSON，便于结果页稳定展示和回显。
     */
    private String buildResumePolishSystemPrompt() {
        return """
                你是一名资深求职简历优化顾问。
                你的任务是基于用户原始简历内容，生成更适合投递的润色版本。
                必须严格遵守以下要求：
                1. 只输出 JSON，不要输出 markdown 代码块，不要额外解释。
                2. 不要编造用户不存在的经历、公司、项目、学历或成果。
                3. 可以优化表达顺序、结构、措辞和成果呈现方式，但不能虚构事实。
                4. 如果提供了岗位 JD 或岗位匹配分析结果，要优先强化岗位相关技能、项目关键词和成果表达。
                5. modificationNotes 必须可解释，说明改了什么以及为什么这样改。
                返回格式必须为：
                {
                  "polishedResumeText": "润色后的完整简历文本",
                  "modificationNotes": ["说明1", "说明2", "说明3"]
                }
                """;
    }

    /**
     * 润色场景优先拼接最近一次 JD 对比结果，保证定向润色逻辑尽量复用已完成链路。
     */
    private String buildResumePolishUserPrompt(String resumeText, String jdText, ResumeJobMatchAnalyzeResponse latestJobMatchAnalysis) {
        StringBuilder builder = new StringBuilder();
        builder.append("请对以下简历进行润色优化。\n");
        builder.append("【原始简历】\n").append(resumeText).append("\n\n");

        if (jdText != null && !jdText.isBlank()) {
            builder.append("【目标岗位 JD】\n").append(jdText).append("\n\n");
        }

        if (latestJobMatchAnalysis != null) {
            builder.append("【最近一次岗位匹配分析】\n");
            builder.append("匹配度评分：").append(latestJobMatchAnalysis.getMatchScore()).append("\n");
            builder.append("已匹配关键词：").append(latestJobMatchAnalysis.getMatchedKeywords()).append("\n");
            builder.append("缺失关键词：").append(latestJobMatchAnalysis.getMissingKeywords()).append("\n");
            builder.append("优化建议：").append(latestJobMatchAnalysis.getSuggestions()).append("\n\n");
        }

        builder.append("""
                请输出更适合求职投递的版本，并满足以下要求：
                1. 优先优化摘要、技能和项目表达的清晰度。
                2. 尽量用“能力/动作/结果”方式重写经历描述。
                3. 如果提供了 JD，请体现更强的岗位针对性。
                4. modificationNotes 至少输出 3 条，必须能让用户理解改动原因。
                """);
        return builder.toString();
    }

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
    /**
     * 润色结果先按严格 JSON 解析；如果模型输出了弱格式数组或未完全转义的文本，
     * 则进入最小容错解析，避免整次润色链路直接失败。
     */
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

    /**
     * 先尝试按 JsonNode 解析，兼容后续字段扩展或字段顺序变化。
     */
    private JsonNode tryReadJsonNode(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 从原始返回中提取润色后的简历正文，允许正文里包含多行文本。
     */
    private String extractPolishedResumeText(String rawJson) {
        Matcher matcher = POLISHED_TEXT_PATTERN.matcher(rawJson);
        if (!matcher.find()) {
            return "";
        }
        return normalizePolishText(unescapeLooseJsonText(matcher.group(1)));
    }

    /**
     * 润色说明优先按标准 JSON 数组读取，失败后降级为逐条弱解析。
     */
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
            return objectMapper.readValue("[" + notesBody + "]", new TypeReference<List<String>>() {});
        } catch (Exception ignored) {
            // 标准数组解析失败时，继续执行弱格式兜底
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

    /**
     * 统一还原常见 JSON 转义，保证前端看到的是正常文本。
     */
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

    /**
     * 去掉 AI 常见的编号、项目符号和包裹引号，尽量保留用户可读的说明正文。
     */
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

    /**
     * AI 有时会把本应为字符串的字段返回成对象或数组，这里统一转成可读文本，
     * 避免 Jackson 在 DTO 绑定阶段因为类型不一致直接失败。
     */
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

    /**
     * 优先读取常见语义字段，减少把整个对象 JSON 原样展示给用户。
     */
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

    /**
     * 只截取部分原始返回做日志预览，避免日志里完整打印简历正文。
     */
    private String buildRawPreview(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return "";
        }
        String preview = rawJson.replace("\r", "\\r").replace("\n", "\\n");
        return preview.length() > 300 ? preview.substring(0, 300) + "..." : preview;
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
