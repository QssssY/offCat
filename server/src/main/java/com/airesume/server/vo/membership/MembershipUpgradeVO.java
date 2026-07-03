package com.airesume.server.vo.membership;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipUpgradeVO {

    private String orderNo;

    private String orderStatus;

    private String payChannel;

    private String planCode;

    private String planName;

    private Integer role;

    private String membershipPlanCode;

    private LocalDateTime vipExpireTime;

    private Integer resumeQuota;

    private Integer interviewQuota;
}
