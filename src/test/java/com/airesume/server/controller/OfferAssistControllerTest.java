package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.offer.SalaryNegotiationSimulationRequest;
import com.airesume.server.dto.offer.SalaryNegotiationSimulationResponse;
import com.airesume.server.dto.offer.SalaryScriptRequest;
import com.airesume.server.dto.offer.SalaryScriptResponse;
import com.airesume.server.service.OfferAssistService;
import com.airesume.server.service.UserQuotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class OfferAssistControllerTest {

    private OfferAssistService offerAssistService;
    private UserQuotaService userQuotaService;
    private Authentication authentication;
    private OfferAssistController controller;

    @BeforeEach
    void setUp() {
        offerAssistService = mock(OfferAssistService.class);
        userQuotaService = mock(UserQuotaService.class);
        authentication = mock(Authentication.class);
        controller = new OfferAssistController(offerAssistService, userQuotaService);
        when(authentication.getPrincipal()).thenReturn(1L);
    }

    @Test
    void shouldSimulateSalaryNegotiation() {
        SalaryNegotiationSimulationRequest request = new SalaryNegotiationSimulationRequest();
        request.setJobTitle("产品经理");
        SalaryNegotiationSimulationResponse response = SalaryNegotiationSimulationResponse.builder()
                .sceneSummary("仍有谈判空间")
                .candidateReply("建议回复")
                .responseStrategy("先表达意愿")
                .riskReminders(List.of("不要虚构 Offer"))
                .nextActions(List.of("确认薪资结构"))
                .build();
        when(offerAssistService.simulateSalaryNegotiation(1L, request)).thenReturn(response);

        Result<SalaryNegotiationSimulationResponse> result =
                controller.simulateSalaryNegotiation(request, authentication);

        assertEquals(200, result.getCode());
        assertEquals("仍有谈判空间", result.getData().getSceneSummary());
        verify(offerAssistService).simulateSalaryNegotiation(1L, request);
        verify(userQuotaService).checkAndDeductOfferQuota(1L);
    }

    @Test
    void shouldGenerateSalaryScript() {
        SalaryScriptRequest request = new SalaryScriptRequest();
        request.setJobTitle("Java后端开发工程师");
        SalaryScriptResponse response = SalaryScriptResponse.builder()
                .openingScript("开场话术")
                .counterOfferScript("争取话术")
                .benefitTradeoffScript("交换项话术")
                .closingScript("收口话术")
                .usageTips(List.of("先电话沟通"))
                .build();
        when(offerAssistService.generateSalaryScript(1L, request)).thenReturn(response);

        Result<SalaryScriptResponse> result = controller.generateSalaryScript(request, authentication);

        assertEquals(200, result.getCode());
        assertEquals("开场话术", result.getData().getOpeningScript());
        verify(offerAssistService).generateSalaryScript(1L, request);
        verify(userQuotaService).checkAndDeductOfferQuota(1L);
    }
}
