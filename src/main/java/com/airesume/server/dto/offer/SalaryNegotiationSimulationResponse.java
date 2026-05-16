package com.airesume.server.dto.offer;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 薪资谈判模拟结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryNegotiationSimulationResponse {

    /**
     * 场景判断摘要。
     */
    private String sceneSummary;

    /**
     * 建议候选人直接回复 HR 的话术。
     */
    private String candidateReply;

    /**
     * 谈判策略说明。
     */
    private String responseStrategy;

    /**
     * 风险提醒，避免强硬或失真表达。
     */
    private List<String> riskReminders;

    /**
     * 下一步行动建议。
     */
    private List<String> nextActions;
}
