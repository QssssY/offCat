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

    /** 每日AI润色次数限制。 */
    private Integer dailyPolishLimit;

    /** 每日JD匹配次数限制。 */
    private Integer dailyJdMatchLimit;

    /** 每日模板使用次数限制。 */
    private Integer dailyTemplateLimit;

    /** 每日Offer辅助次数限制。 */
    private Integer dailyOfferLimit;

    /** 套餐周期内简历诊断总额度（0=不限）。 */
    private Integer totalResumeQuota;

    /** 套餐周期内面试总额度（0=不限）。 */
    private Integer totalInterviewQuota;

    /** 套餐周期内AI润色总额度（0=不限）。 */
    private Integer totalPolishQuota;

    /** 套餐周期内JD匹配总额度（0=不限）。 */
    private Integer totalJdMatchQuota;

    /** 套餐周期内模板总额度（0=不限）。 */
    private Integer totalTemplateQuota;

    /** 套餐周期内Offer总额度（0=不限）。 */
    private Integer totalOfferQuota;

    /** 购买赠送简历诊断额度。 */
    private Integer bonusResumeQuota;

    /** 购买赠送面试额度。 */
    private Integer bonusInterviewQuota;

    /** 套餐权益描述列表（JSON 数组字符串）。 */
    private String benefits;

    private Integer status;

    private Integer sort;
}
