package com.airesume.server.service.impl;

import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.dto.ai.ResolvedAiConfig;
import com.airesume.server.dto.offer.SalaryNegotiationSimulationRequest;
import com.airesume.server.dto.offer.SalaryNegotiationSimulationResponse;
import com.airesume.server.dto.offer.SalaryScriptRequest;
import com.airesume.server.dto.offer.SalaryScriptResponse;
import com.airesume.server.service.AiChatClient;
import com.airesume.server.service.UserAiConfigResolver;
import com.airesume.server.service.UserAiUsageLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OfferAssistServiceImplTest {

    private AiChatClient aiChatClient;
    private UserAiConfigResolver userAiConfigResolver;
    private UserAiUsageLimitService userAiUsageLimitService;
    private OfferAssistServiceImpl service;

    @BeforeEach
    void setUp() {
        aiChatClient = mock(AiChatClient.class);
        userAiConfigResolver = mock(UserAiConfigResolver.class);
        userAiUsageLimitService = mock(UserAiUsageLimitService.class);
        service = new OfferAssistServiceImpl(
                aiChatClient,
                new ObjectMapper(),
                userAiConfigResolver,
                userAiUsageLimitService);
    }

    @Test
    void shouldGenerateSalaryNegotiationSimulation() {
        when(aiChatClient.chat(anyString(), anyString(), anyInt(), eq(1L), eq(false))).thenReturn("""
                {
                  "sceneSummary": "当前报价低于期望，但仍有沟通空间。",
                  "candidateReply": "感谢认可，我希望结合岗位职责再沟通整体包。",
                  "responseStrategy": "先表达意愿，再说明价值和期望区间。",
                  "riskReminders": ["不要虚构其他 Offer", "不要直接否定当前报价"],
                  "nextActions": ["确认薪资结构", "准备项目成果数据"]
                }
                """);

        SalaryNegotiationSimulationResponse response =
                service.simulateSalaryNegotiation(1L, buildSimulationRequest());

        assertEquals("当前报价低于期望，但仍有沟通空间。", response.getSceneSummary());
        assertTrue(response.getCandidateReply().contains("感谢认可"));
        assertEquals(2, response.getRiskReminders().size());
        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiChatClient).chat(systemPromptCaptor.capture(), userPromptCaptor.capture(), eq(180_000), eq(1L), eq(false));
        assertTrue(systemPromptCaptor.getValue().contains("保留回旋空间"));
        assertTrue(systemPromptCaptor.getValue().contains("避免虚构竞品 Offer"));
        assertTrue(userPromptCaptor.getValue().contains("HR 当前问题"));
        assertTrue(userPromptCaptor.getValue().contains("明天就能准备的具体动作"));
    }

    @Test
    void shouldGenerateSalaryScript() {
        when(aiChatClient.chat(anyString(), anyString(), anyInt(), eq(1L), eq(false))).thenReturn("""
                {
                  "openingScript": "感谢推进 Offer，我想确认整体薪酬结构。",
                  "counterOfferScript": "结合我的项目经验，期望年包能到 45 万。",
                  "benefitTradeoffScript": "如果现金部分空间有限，也可以讨论签字费或调薪节点。",
                  "closingScript": "期待我们能尽快确认一个双方都认可的方案。",
                  "usageTips": ["先电话沟通再文字确认", "保留积极入职意愿"]
                }
                """);

        SalaryScriptResponse response = service.generateSalaryScript(1L, buildScriptRequest());

        assertTrue(response.getOpeningScript().contains("薪酬结构"));
        assertTrue(response.getBenefitTradeoffScript().contains("签字费"));
        assertEquals(2, response.getUsageTips().size());
        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiChatClient).chat(systemPromptCaptor.capture(), userPromptCaptor.capture(), eq(180_000), eq(1L), eq(false));
        assertTrue(systemPromptCaptor.getValue().contains("不引用实时薪资行情"));
        assertTrue(systemPromptCaptor.getValue().contains("价值依据"));
        assertTrue(userPromptCaptor.getValue().contains("谈判目标"));
        assertTrue(userPromptCaptor.getValue().contains("价值锚定"));
        assertTrue(userPromptCaptor.getValue().contains("可直接复制给 HR"));
    }

    @Test
    void shouldRejectEmptyAiResponse() {
        when(aiChatClient.chat(anyString(), anyString(), anyInt(), eq(1L), eq(false))).thenReturn(" ");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.generateSalaryScript(1L, buildScriptRequest()));

        assertEquals("Offer 辅助 AI 返回为空", ex.getMessage());
    }

    @Test
    void shouldRejectInvalidAiJson() {
        when(aiChatClient.chat(anyString(), anyString(), anyInt(), eq(1L), eq(false))).thenReturn("不是 JSON");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.simulateSalaryNegotiation(1L, buildSimulationRequest()));

        assertEquals("Offer 辅助 AI 返回格式错误", ex.getMessage());
    }

    @Test
    void shouldCountCustomAiUsageForOfferAssist() {
        when(userAiConfigResolver.resolve(1L, AiEngineConstants.BUSINESS_TYPE_INTERVIEW, false))
                .thenReturn(ResolvedAiConfig.builder().configType("default").build());
        when(aiChatClient.chat(anyString(), anyString(), anyInt(), eq(1L), eq(false))).thenReturn("""
                {
                  "openingScript": "感谢推进 Offer，我想确认整体薪酬结构。",
                  "counterOfferScript": "结合我的项目经验，期望年包能到 45 万。",
                  "benefitTradeoffScript": "如果现金部分空间有限，也可以讨论签字费或调薪节点。",
                  "closingScript": "期待我们能尽快确认一个双方都认可的方案。",
                  "usageTips": ["先电话沟通再文字确认"]
                }
                """);

        service.generateSalaryScript(1L, buildScriptRequest());

        verify(userAiUsageLimitService).checkAndIncrement(1L, UserAiConstants.USAGE_TYPE_OFFER_ASSIST);
        verify(userAiUsageLimitService, never()).rollback(1L, UserAiConstants.USAGE_TYPE_OFFER_ASSIST);
    }

    @Test
    void shouldRollbackCustomAiUsageWhenOfferAssistFails() {
        when(userAiConfigResolver.resolve(1L, AiEngineConstants.BUSINESS_TYPE_INTERVIEW, false))
                .thenReturn(ResolvedAiConfig.builder().configType("default").build());
        when(aiChatClient.chat(anyString(), anyString(), anyInt(), eq(1L), eq(false)))
                .thenThrow(new IllegalStateException("AI 调用失败"));

        assertThrows(IllegalStateException.class, () -> service.generateSalaryScript(1L, buildScriptRequest()));

        verify(userAiUsageLimitService).checkAndIncrement(1L, UserAiConstants.USAGE_TYPE_OFFER_ASSIST);
        verify(userAiUsageLimitService).rollback(1L, UserAiConstants.USAGE_TYPE_OFFER_ASSIST);
    }

    private SalaryNegotiationSimulationRequest buildSimulationRequest() {
        SalaryNegotiationSimulationRequest request = new SalaryNegotiationSimulationRequest();
        request.setCompanyName("示例科技");
        request.setJobTitle("Java后端开发工程师");
        request.setExperienceYears("5年");
        request.setCurrentSalary("30万年包");
        request.setExpectedSalary("45万年包");
        request.setOfferSalary("38万年包");
        request.setCandidateBackground("负责过交易系统、库存系统和性能优化。");
        request.setHrMessage("你的期望薪资是多少？");
        return request;
    }

    private SalaryScriptRequest buildScriptRequest() {
        SalaryScriptRequest request = new SalaryScriptRequest();
        request.setCompanyName("示例科技");
        request.setJobTitle("Java后端开发工程师");
        request.setExperienceYears("5年");
        request.setCandidateBackground("负责过交易系统、库存系统和性能优化。");
        request.setExpectedSalary("45万年包");
        request.setOfferSalary("38万年包");
        request.setNegotiationGoal("争取年包到45万，或补充签字费。");
        return request;
    }
}
