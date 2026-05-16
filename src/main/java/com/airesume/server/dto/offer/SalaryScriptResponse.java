package com.airesume.server.dto.offer;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 谈薪话术模板结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryScriptResponse {

    /**
     * 开场确认话术。
     */
    private String openingScript;

    /**
     * 争取更高报价的话术。
     */
    private String counterOfferScript;

    /**
     * 福利、职级、试用期等可交换项话术。
     */
    private String benefitTradeoffScript;

    /**
     * 收口确认话术。
     */
    private String closingScript;

    /**
     * 使用提醒。
     */
    private List<String> usageTips;
}
