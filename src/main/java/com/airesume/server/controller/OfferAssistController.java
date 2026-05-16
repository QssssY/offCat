package com.airesume.server.controller;

import com.airesume.server.common.result.Result;
import com.airesume.server.dto.offer.SalaryNegotiationSimulationRequest;
import com.airesume.server.dto.offer.SalaryNegotiationSimulationResponse;
import com.airesume.server.dto.offer.SalaryScriptRequest;
import com.airesume.server.dto.offer.SalaryScriptResponse;
import com.airesume.server.service.OfferAssistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
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

    /**
     * 薪资谈判模拟接口。
     */
    @PostMapping("/salary-negotiation/simulate")
    public Result<SalaryNegotiationSimulationResponse> simulateSalaryNegotiation(
            @Valid @RequestBody SalaryNegotiationSimulationRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("薪资谈判模拟请求, userId: {}, jobTitle: {}", userId, request.getJobTitle());
        SalaryNegotiationSimulationResponse response =
                offerAssistService.simulateSalaryNegotiation(userId, request);
        return Result.success("薪资谈判模拟完成", response);
    }

    /**
     * 谈薪话术模板接口。
     */
    @PostMapping("/salary-negotiation/script")
    public Result<SalaryScriptResponse> generateSalaryScript(
            @Valid @RequestBody SalaryScriptRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("谈薪话术模板请求, userId: {}, jobTitle: {}", userId, request.getJobTitle());
        SalaryScriptResponse response = offerAssistService.generateSalaryScript(userId, request);
        return Result.success("谈薪话术模板生成完成", response);
    }
}
