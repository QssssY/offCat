package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("membership_order")
public class MembershipOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private Long userId;

    private Long planId;

    private String planCode;

    private String planName;

    private String orderStatus;

    private String payChannel;

    private BigDecimal orderAmount;

    private Integer durationDays;

    private Integer grantedResumeQuota;

    private Integer grantedInterviewQuota;

    private LocalDateTime expireTimeBefore;

    private LocalDateTime expireTimeAfter;

    private LocalDateTime paidAt;
}
