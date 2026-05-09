package com.airesume.server.vo.membership;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipPlanVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String planCode;

    private String planName;

    private String description;

    private BigDecimal priceAmount;

    private Integer durationDays;

    private Integer resumeQuota;

    private Integer interviewQuota;
}
