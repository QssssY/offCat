package com.airesume.server.common.constants;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 免费用户功能额度常量测试。
 */
class QuotaConstantsTest {

    @Test
    void shouldSetEveryFreeUserFeatureLimitToOneHundred() {
        assertAll(
                () -> assertEquals(100, QuotaConstants.NORMAL_USER_FREE_INTERVIEW_LIMIT),
                () -> assertEquals(100, QuotaConstants.NORMAL_USER_FREE_RESUME_LIMIT),
                () -> assertEquals(100, QuotaConstants.FREE_USER_POLISH_LIMIT),
                () -> assertEquals(100, QuotaConstants.FREE_USER_JD_MATCH_LIMIT),
                () -> assertEquals(100, QuotaConstants.FREE_USER_TEMPLATE_LIMIT),
                () -> assertEquals(100, QuotaConstants.FREE_USER_OFFER_LIMIT)
        );
    }
}
