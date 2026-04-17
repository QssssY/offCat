package com.airesume.server.vo.membership;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipPlanVO {

    private String planCode;

    private String planName;

    private String description;

    private BigDecimal priceAmount;

    private Integer durationDays;

    private Integer resumeQuota;

    private Integer interviewQuota;
}
