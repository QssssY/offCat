package com.airesume.server.service.impl;

import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.dto.offer.SalaryNegotiationSimulationRequest;
import com.airesume.server.dto.offer.SalaryNegotiationSimulationResponse;
import com.airesume.server.dto.offer.SalaryScriptRequest;
import com.airesume.server.dto.offer.SalaryScriptResponse;
import com.airesume.server.service.AiChatClient;
import com.airesume.server.service.OfferAssistService;
import com.airesume.server.service.UserAiConfigResolver;
import com.airesume.server.service.UserAiUsageLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Offer 辅助服务实现。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfferAssistServiceImpl implements OfferAssistService {

    private static final int OFFER_AI_TIMEOUT_MS = 180_000;

    private final AiChatClient aiChatClient;
    private final ObjectMapper objectMapper;
    private final UserAiConfigResolver userAiConfigResolver;
    private final UserAiUsageLimitService userAiUsageLimitService;

    /**
     * 薪资谈判模拟只基于用户输入生成建议，不接入实时薪资行情，避免给出伪市场数据。
     */
    @Override
    public SalaryNegotiationSimulationResponse simulateSalaryNegotiation(
            Long userId,
            SalaryNegotiationSimulationRequest request) {
        log.info("生成薪资谈判模拟, userId: {}, jobTitle: {}", userId, request.getJobTitle());
        String raw = callOfferAiWithUserBilling(userId, buildSimulationUserPrompt(request));
        return parseAiJson(raw, SalaryNegotiationSimulationResponse.class);
    }

    /**
     * 谈薪话术模板按固定字段输出，前端可以稳定渲染和复制。
     */
    @Override
    public SalaryScriptResponse generateSalaryScript(Long userId, SalaryScriptRequest request) {
        log.info("生成谈薪话术模板, userId: {}, jobTitle: {}", userId, request.getJobTitle());
        String raw = callOfferAiWithUserBilling(userId, buildScriptUserPrompt(request));
        return parseAiJson(raw, SalaryScriptResponse.class);
    }

    private String callOfferAiWithUserBilling(Long userId, String userPrompt) {
        boolean useCustomAi = userAiConfigResolver != null
                && userAiConfigResolver.resolve(userId, AiEngineConstants.BUSINESS_TYPE_INTERVIEW, false) != null;
        if (useCustomAi) {
            // Offer 辅助复用轻量聊天客户端的 interview/default 配置，命中自定义 AI 时纳入独立每日次数。
            userAiUsageLimitService.checkAndIncrement(userId, UserAiConstants.USAGE_TYPE_OFFER_ASSIST);
        }
        try {
            return aiChatClient.chat(buildSalarySystemPrompt(), userPrompt, OFFER_AI_TIMEOUT_MS, userId, false);
        } catch (RuntimeException e) {
            if (useCustomAi) {
                userAiUsageLimitService.rollback(userId, UserAiConstants.USAGE_TYPE_OFFER_ASSIST);
            }
            throw e;
        }
    }

    private String buildSalarySystemPrompt() {
        return """
                你是面向中高端求职者的 Offer 谈判教练。
                只根据用户输入做谈判策略和话术建议，不引用实时薪资行情，不编造市场分位数或公司薪资数据。
                输出必须是严格 JSON，不要 Markdown，不要代码块，不要额外解释。
                话术要专业、克制、尊重对方且保留回旋空间，可直接发给 HR。
                每段可发送话术不要太短，需要包含感谢、入职意愿、价值依据、明确请求和可协商余地。
                避免威胁式表达，避免虚构竞品 Offer，避免承诺虚假信息，避免把用户未提供的信息当成事实。
                """;
    }

    private String buildSimulationUserPrompt(SalaryNegotiationSimulationRequest request) {
        return """
                请基于以下信息生成薪资谈判模拟结果。
                公司：%s
                岗位：%s
                经验：%s
                当前薪资：%s
                期望薪资：%s
                对方报价：%s
                候选人背景：%s
                HR 当前问题：%s

                输出要求：
                - sceneSummary：判断当前局面、筹码强弱和对方问题背后的含义，控制在一句话。
                - candidateReply：写成候选人可以直接发送给 HR 的完整回复，语气真诚、坚定、留余地。
                - responseStrategy：解释为什么这样回，以及下一轮如果 HR 压预算应如何推进。
                - riskReminders：识别至少 3 个容易扣分或压价的风险，避免空泛提醒。
                - nextActions：给出 3 个明天就能准备的具体动作。
                - 不要输出实时行情、市场分位数、公司薪资数据或任何未由用户提供的事实。

                JSON 字段：
                {
                  "sceneSummary": "一句话判断当前谈判局面",
                  "candidateReply": "候选人可以直接回复 HR 的完整话术",
                  "responseStrategy": "为什么这样回复，以及后续怎么推进",
                  "riskReminders": ["风险提醒1", "风险提醒2", "风险提醒3"],
                  "nextActions": ["下一步1", "下一步2", "下一步3"]
                }
                """.formatted(
                blankToDash(request.getCompanyName()),
                request.getJobTitle(),
                blankToDash(request.getExperienceYears()),
                blankToDash(request.getCurrentSalary()),
                request.getExpectedSalary(),
                blankToDash(request.getOfferSalary()),
                request.getCandidateBackground(),
                request.getHrMessage());
    }

    private String buildScriptUserPrompt(SalaryScriptRequest request) {
        return """
                请基于以下信息生成谈薪话术模板。
                公司：%s
                岗位：%s
                经验：%s
                候选人背景：%s
                期望薪资：%s
                当前报价：%s
                谈判目标：%s

                输出要求：
                - openingScript：先确认 Offer 细节和薪酬结构，表达感谢和入职意愿。
                - counterOfferScript：先做价值锚定，再提出期望薪资或目标区间，避免生硬要价。
                - benefitTradeoffScript：当现金空间有限时，提供签字费、调薪节点、职级、试用期薪资、假期或发展机会等交换项话术。
                - closingScript：给出双方可推进的收口表达，既明确诉求，也保留继续沟通空间。
                - usageTips：提醒用户如何选择电话/文字、何时发送、哪些表达不要用。
                - 每段话术都应可直接复制给 HR，不要太短，不要用威胁、虚构竞品 Offer 或编造市场数据的方式施压。

                JSON 字段：
                {
                  "openingScript": "开场确认话术",
                  "counterOfferScript": "争取更高报价的话术",
                  "benefitTradeoffScript": "薪资外可交换项话术",
                  "closingScript": "收口确认话术",
                  "usageTips": ["使用提醒1", "使用提醒2", "使用提醒3"]
                }
                """.formatted(
                blankToDash(request.getCompanyName()),
                request.getJobTitle(),
                blankToDash(request.getExperienceYears()),
                request.getCandidateBackground(),
                request.getExpectedSalary(),
                blankToDash(request.getOfferSalary()),
                request.getNegotiationGoal());
    }

    private <T> T parseAiJson(String raw, Class<T> targetType) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Offer 辅助 AI 返回为空");
        }
        try {
            String json = raw.trim();
            if (json.startsWith("```")) {
                json = json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
            }
            return objectMapper.readValue(json, targetType);
        } catch (Exception e) {
            throw new IllegalStateException("Offer 辅助 AI 返回格式错误", e);
        }
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "未提供" : value.trim();
    }
}
