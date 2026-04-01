package com.airesume.server.common.constants;

/**
 * 额度限制常量
 * 定义普通用户和会员用户的免费次数限制
 */
public class QuotaConstants {

    /**
     * 普通用户免费面试次数上限
     */
    public static final int NORMAL_USER_FREE_INTERVIEW_LIMIT = 3;

    /**
     * 普通用户免费简历诊断次数上限
     */
    public static final int NORMAL_USER_FREE_RESUME_LIMIT = 3;

    /**
     * 会员用户每日面试次数上限
     */
    public static final int VIP_USER_DAILY_INTERVIEW_LIMIT = 10;

    /**
     * 会员用户每日简历诊断次数上限
     */
    public static final int VIP_USER_DAILY_RESUME_LIMIT = 10;

    private QuotaConstants() {
    }

}
