package com.airesume.server.common.constants;

public final class MembershipConstants {

    /** 年度会员套餐代码。 */
    public static final String YEARLY_PLAN_CODE = "vip_year";

    /** 新用户注册赠送一年会员的业务标识。 */
    public static final String REGISTRATION_GIFT_BIZ_ID = "registration_one_year";

    /** 会员赠送通知的业务类型。 */
    public static final String MEMBERSHIP_GIFT_BIZ_TYPE = "membership_gift";

    public static final int PLAN_STATUS_ENABLED = 1;

    public static final String ORDER_STATUS_CREATED = "CREATED";

    public static final String ORDER_STATUS_PAID = "PAID";

    public static final String PAY_CHANNEL_MOCK = "MOCK";

    private MembershipConstants() {
    }
}
