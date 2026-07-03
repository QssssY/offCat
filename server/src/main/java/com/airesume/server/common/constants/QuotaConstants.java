package com.airesume.server.common.constants;

/**
 * Quota constants for the current business rules.
 *
 * Normal user:
 * - 1 total free resume diagnosis
 * - 3 total free mock interviews
 *
 * VIP user:
 * - valid only while vipExpireTime is active
 * - 5 resume diagnoses per day
 * - 10 mock interviews per day
 */
public final class QuotaConstants {

    /**
     * Total free interview count for normal users.
     */
    public static final int NORMAL_USER_FREE_INTERVIEW_LIMIT = 3;

    /**
     * Total free resume diagnosis count for normal users.
     */
    public static final int NORMAL_USER_FREE_RESUME_LIMIT = 1;

    /**
     * Daily interview limit for valid VIP users.
     */
    public static final int VIP_USER_DAILY_INTERVIEW_LIMIT = 10;

    /**
     * Daily resume diagnosis limit for valid VIP users.
     * Business rule changed from 10/day to 5/day.
     * VIP users are no longer modeled as receiving cumulative package counts,
     * so this constant represents the fixed daily ceiling that refreshes next day.
     */
    public static final int VIP_USER_DAILY_RESUME_LIMIT = 5;

    /** 非会员免费AI润色次数。 */
    public static final int FREE_USER_POLISH_LIMIT = 1;

    /** 非会员免费JD匹配次数。 */
    public static final int FREE_USER_JD_MATCH_LIMIT = 1;

    /** 非会员免费模板使用次数。 */
    public static final int FREE_USER_TEMPLATE_LIMIT = 2;

    /** 非会员免费Offer辅助次数。 */
    public static final int FREE_USER_OFFER_LIMIT = 1;

    private QuotaConstants() {
    }
}
