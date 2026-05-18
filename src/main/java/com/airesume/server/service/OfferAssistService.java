package com.airesume.server.service;

import com.airesume.server.dto.offer.SalaryNegotiationSimulationRequest;
import com.airesume.server.dto.offer.SalaryNegotiationSimulationResponse;
import com.airesume.server.dto.offer.SalaryScriptRequest;
import com.airesume.server.dto.offer.SalaryScriptResponse;

/**
 * Offer 辅助服务。
 */
public interface OfferAssistService {

    /**
     * 生成薪资谈判模拟回复。
     *
     * @param userId 当前用户 ID
     * @param request 谈判场景
     * @return 模拟回复与策略
     */
    SalaryNegotiationSimulationResponse simulateSalaryNegotiation(
            Long userId,
            SalaryNegotiationSimulationRequest request);

    /**
     * 生成谈薪话术模板。
     *
     * @param userId 当前用户 ID
     * @param request 谈薪目标
     * @return 分场景话术
     */
    SalaryScriptResponse generateSalaryScript(Long userId, SalaryScriptRequest request);
}
