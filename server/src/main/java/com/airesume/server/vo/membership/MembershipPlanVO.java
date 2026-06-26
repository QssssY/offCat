package com.airesume.server.vo.membership;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

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

    /** 每日AI润色次数限制。 */
    private Integer dailyPolishLimit;

    /** 每日JD匹配次数限制。 */
    private Integer dailyJdMatchLimit;

    /** 每日模板使用次数限制。 */
    private Integer dailyTemplateLimit;

    /** 每日Offer辅助次数限制。 */
    private Integer dailyOfferLimit;

    /** 购买赠送简历诊断额度。 */
    private Integer bonusResumeQuota;

    /** 购买赠送面试额度。 */
    private Integer bonusInterviewQuota;

    /** 套餐权益描述列表。 */
    private List<String> benefits;

    /** 排序值，越大等级越高。 */
    private Integer sort;
}
