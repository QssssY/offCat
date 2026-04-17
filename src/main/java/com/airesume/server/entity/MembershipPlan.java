package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("membership_plan")
public class MembershipPlan extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String planCode;

    private String planName;

    private String description;

    private BigDecimal priceAmount;

    private Integer durationDays;

    private Integer resumeQuota;

    private Integer interviewQuota;

    private Integer status;

    private Integer sort;
}
