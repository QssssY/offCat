package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.offer.SalaryNegotiationSimulationRequest;
import com.airesume.server.dto.offer.SalaryNegotiationSimulationResponse;
import com.airesume.server.dto.offer.SalaryScriptRequest;
import com.airesume.server.dto.offer.SalaryScriptResponse;
import com.airesume.server.service.OfferAssistService;
import com.airesume.server.service.UserQuotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Offer 辅助控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/offer")
@RequiredArgsConstructor
public class OfferAssistController {

    private final OfferAssistService offerAssistService;
    private final UserQuotaService userQuotaService;

    /**
     * 薪资谈判模拟接口。
     */
    @PostMapping("/salary-negotiation/simulate")
    @Transactional(rollbackFor = Exception.class)
    public Result<SalaryNegotiationSimulationResponse> simulateSalaryNegotiation(
            @Valid @RequestBody SalaryNegotiationSimulationRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("薪资谈判模拟请求, userId: {}, jobTitle: {}", userId, request.getJobTitle());

        // Offer辅助配额检查与扣减
        userQuotaService.checkAndDeductOfferQuota(userId);

        SalaryNegotiationSimulationResponse response =
                offerAssistService.simulateSalaryNegotiation(userId, request);
        return Result.success("薪资谈判模拟完成", response);
    }

    /**
     * 谈薪话术模板接口。
     */
    @PostMapping("/salary-negotiation/script")
    @Transactional(rollbackFor = Exception.class)
    public Result<SalaryScriptResponse> generateSalaryScript(
            @Valid @RequestBody SalaryScriptRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("谈薪话术模板请求, userId: {}, jobTitle: {}", userId, request.getJobTitle());

        // Offer辅助配额检查与扣减
        userQuotaService.checkAndDeductOfferQuota(userId);

        SalaryScriptResponse response = offerAssistService.generateSalaryScript(userId, request);
        return Result.success("谈薪话术模板生成完成", response);
    }
}
