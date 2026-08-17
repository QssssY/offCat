package com.airesume.server.common.constants;

/**
 * 当前业务使用的用户额度常量。
 *
 * <p>免费用户六项功能各有 100 次一次性额度；VIP 用户的每日额度仍按套餐规则计算。</p>
 */
public final class QuotaConstants {

    /** 普通用户模拟面试一次性免费次数。 */
    public static final int NORMAL_USER_FREE_INTERVIEW_LIMIT = 100;

    /** 普通用户简历诊断一次性免费次数。 */
    public static final int NORMAL_USER_FREE_RESUME_LIMIT = 100;

    /** 有效 VIP 用户每日模拟面试次数。 */
    public static final int VIP_USER_DAILY_INTERVIEW_LIMIT = 10;

    /**
     * 有效 VIP 用户每日简历诊断次数。
     * 当前固定上限为每日 5 次，次日刷新，不再按累计套餐次数计算。
     */
    public static final int VIP_USER_DAILY_RESUME_LIMIT = 5;

    /** 普通用户 AI 润色一次性免费次数。 */
    public static final int FREE_USER_POLISH_LIMIT = 100;

    /** 普通用户 JD 匹配一次性免费次数。 */
    public static final int FREE_USER_JD_MATCH_LIMIT = 100;

    /** 普通用户模板使用一次性免费次数。 */
    public static final int FREE_USER_TEMPLATE_LIMIT = 100;

    /** 普通用户 Offer 辅助一次性免费次数。 */
    public static final int FREE_USER_OFFER_LIMIT = 100;

    private QuotaConstants() {
    }
}
