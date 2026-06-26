package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_quota")
public class UserQuota extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private Integer totalInterviewUsed;

    private Integer totalResumeUsed;

    private Integer interviewQuota;

    private Integer resumeQuota;

    private Integer dailyInterviewUsed;

    private Integer dailyResumeUsed;

    /** 今日AI润色使用次数。 */
    private Integer dailyPolishUsed;

    /** 今日JD匹配使用次数。 */
    private Integer dailyJdMatchUsed;

    /** 今日模板使用次数。 */
    private Integer dailyTemplateUsed;

    /** 今日Offer辅助使用次数。 */
    private Integer dailyOfferUsed;

    /** 周期内简历诊断已用。 */
    private Integer cycleResumeUsed;

    /** 周期内面试已用。 */
    private Integer cycleInterviewUsed;

    /** 周期内AI润色已用。 */
    private Integer cyclePolishUsed;

    /** 周期内JD匹配已用。 */
    private Integer cycleJdMatchUsed;

    /** 周期内模板已用。 */
    private Integer cycleTemplateUsed;

    /** 周期内Offer已用。 */
    private Integer cycleOfferUsed;

    /** 当前周期开始时间。 */
    private java.time.LocalDateTime cycleStartTime;

    /** 非会员免费润色剩余次数。 */
    private Integer freePolishLeft;

    /** 非会员免费JD匹配剩余次数。 */
    private Integer freeJdMatchLeft;

    /** 非会员免费模板剩余次数。 */
    private Integer freeTemplateLeft;

    /** 非会员免费Offer辅助剩余次数。 */
    private Integer freeOfferLeft;

    private LocalDate lastRefreshDate;

}
