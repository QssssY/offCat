package com.airesume.server.service.impl;

import com.airesume.server.common.constants.ResumeDiagnosisConstants;
import com.airesume.server.service.ResumeAiService;
import com.airesume.server.service.SysPromptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service("resumeAiService")
@Slf4j
@ConditionalOnProperty(name = "app.ai.mode", havingValue = "real")
public class DoubaoResumeAiServiceImpl implements ResumeAiService {

    private final RestClient restClient;
    private final String provider;
    private final String model;
    private final SysPromptService sysPromptService;

    public DoubaoResumeAiServiceImpl(
            @Value("${app.ai.provider:doubao}") String provider,
            @Value("${app.ai.base-url:}") String baseUrl,
            @Value("${app.ai.model:}") String model,
            @Autowired SysPromptService sysPromptService,
            RestClient.Builder restClientBuilder) {
        this.provider = provider == null ? "doubao" : provider.toLowerCase();
        this.model = model;
        this.sysPromptService = sysPromptService;

        String resolvedBaseUrl = resolveBaseUrl(this.provider, baseUrl);
        this.restClient = restClientBuilder
                .baseUrl(resolvedBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();

        log.info("[{}] Resume AI Service initialized, baseUrl: {}, model: {}",
                provider == null ? "DOUBAO" : provider.toUpperCase(), resolvedBaseUrl, model);
    }

    private String resolveBaseUrl(String provider, String configuredUrl) {
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            return configuredUrl;
        }
        return switch (provider) {
            case "doubao", "openai" -> "https://ark.cn-beijing.volces.com/api/v3";
            case "qwen"     -> "https://dashscope.aliyuncs.com/compatible-mode/v1";
            case "ernie"    -> "https://qianfan.baidubce.com/v2";
            case "deepseek" -> "https://api.deepseek.com";
            case "minimax"  -> "https://api.minimax.chat/v1";
            default -> "https://ark.cn-beijing.volces.com/api/v3";
        };
    }

    @Override
    public String diagnose(String resumeText) {
        String tag = provider == null ? "DOUBAO" : provider.toUpperCase();
        log.info("[{}] 简历诊断调用, resumeTextLength: {}",
                tag, resumeText == null ? 0 : resumeText.length());

        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException("简历文本不能为空");
        }

        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请设置环境变量 " + getEnvKeyName() + " 或 AI_API_KEY");
        }

        String systemPrompt = resolveSystemPrompt(tag);

        RequestBody request = new RequestBody();
        request.model = model;
        request.messages = List.of(
                new Message("system", systemPrompt),
                new Message("user", buildUserPrompt(resumeText))
        );

        try {
            String endpoint = getEndpoint();
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
            log.info("[{}] 简历诊断成功, responseLength: {}",
                    tag, result == null ? 0 : result.length());
            return extractJsonFromResponse(result);

        } catch (Exception e) {
            log.error("[{}] 简历诊断失败", tag, e);
            throw new RuntimeException("AI 简历诊断失败: " + e.getMessage(), e);
        }
    }

    private String getEnvKeyName() {
        return switch (provider) {
            case "doubao"  -> "DOUBAO_API_KEY";
            case "qwen"    -> "DASHSCOPE_API_KEY";
            case "ernie"   -> "ERNIE_API_KEY";
            case "deepseek" -> "DEEPSEEK_API_KEY";
            case "minimax" -> "MINIMAX_API_KEY";
            default        -> "AI_API_KEY";
        };
    }

    private String resolveApiKey() {
        String key = System.getenv(getEnvKeyName());
        if (key != null && !key.isBlank()) return key;
        key = System.getenv("API_KEY");
        if (key != null && !key.isBlank()) return key;
        key = System.getenv("AI_API_KEY");
        if (key != null && !key.isBlank()) return key;
        return null;
    }

    private String getEndpoint() {
        return switch (provider) {
            case "ernie" -> "/chat/completions";
            default -> "/chat/completions";
        };
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
                """;
    }

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
                """;
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

    private static class RequestBody {
        public String model;
        public List<Message> messages;
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
