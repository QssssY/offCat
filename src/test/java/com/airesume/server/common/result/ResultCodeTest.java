package com.airesume.server.common.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ResultCode 枚举测试。
 * 验证各分段错误码范围、消息非空、消息为中文、现有值未改变。
 */
class ResultCodeTest {

    @Test
    @DisplayName("现有 HTTP 语义码未被修改")
    void shouldPreserveExistingHttpCodes() {
        assertEquals(200, ResultCode.SUCCESS.getCode());
        assertEquals(500, ResultCode.ERROR.getCode());
        assertEquals(400, ResultCode.PARAM_ERROR.getCode());
        assertEquals(401, ResultCode.UNAUTHORIZED.getCode());
        assertEquals(403, ResultCode.FORBIDDEN.getCode());
        assertEquals(404, ResultCode.NOT_FOUND.getCode());
        assertEquals(429, ResultCode.TOO_MANY_REQUESTS.getCode());
        assertEquals(500, ResultCode.SYSTEM_ERROR.getCode());
        assertEquals(500, ResultCode.BUSINESS_ERROR.getCode());
    }

    @Test
    @DisplayName("所有枚举值的 code 和 message 非空")
    void shouldHaveNonNullCodeAndMessage() {
        for (ResultCode rc : ResultCode.values()) {
            assertNotNull(rc.getCode(), rc.name() + " code 不应为 null");
            assertNotNull(rc.getMessage(), rc.name() + " message 不应为 null");
            assertFalse(rc.getMessage().isBlank(), rc.name() + " message 不应为空字符串");
        }
    }

    @Test
    @DisplayName("简历模块错误码在 2xxx 范围")
    void shouldResumeCodesInRange2xxx() {
        assertInRange(ResultCode.RESUME_FILE_EMPTY, 2000, 2999);
        assertInRange(ResultCode.RESUME_FORMAT_UNSUPPORTED, 2000, 2999);
        assertInRange(ResultCode.RESUME_FILE_TOO_LARGE, 2000, 2999);
        assertInRange(ResultCode.RESUME_PARSE_FAILED, 2000, 2999);
        assertInRange(ResultCode.RESUME_QUOTA_EXHAUSTED, 2000, 2999);
        assertInRange(ResultCode.RESUME_FILE_ILLEGAL_PATH, 2000, 2999);
        assertInRange(ResultCode.RESUME_TASK_NOT_FOUND, 2000, 2999);
        assertInRange(ResultCode.RESUME_TASK_ACCESS_DENIED, 2000, 2999);
        assertInRange(ResultCode.RESUME_FILE_SAVE_FAILED, 2000, 2999);
        assertInRange(ResultCode.RESUME_FILE_CLEANUP_FAILED, 2000, 2999);
    }

    @Test
    @DisplayName("面试模块错误码在 3xxx 范围")
    void shouldInterviewCodesInRange3xxx() {
        assertInRange(ResultCode.INTERVIEW_QUOTA_EXHAUSTED, 3000, 3999);
        assertInRange(ResultCode.INTERVIEW_SESSION_NOT_FOUND, 3000, 3999);
        assertInRange(ResultCode.INTERVIEW_SESSION_ACCESS_DENIED, 3000, 3999);
        assertInRange(ResultCode.INTERVIEW_SESSION_ENDED, 3000, 3999);
        assertInRange(ResultCode.INTERVIEW_AI_TIMEOUT, 3000, 3999);
    }

    @Test
    @DisplayName("AI 服务错误码在 4xxx 范围")
    void shouldAiCodesInRange4xxx() {
        assertInRange(ResultCode.AI_SERVICE_UNAVAILABLE, 4000, 4999);
        assertInRange(ResultCode.AI_RESPONSE_EMPTY, 4000, 4999);
        assertInRange(ResultCode.AI_RESPONSE_PARSE_FAILED, 4000, 4999);
        assertInRange(ResultCode.AI_QUOTA_INSUFFICIENT, 4000, 4999);
    }

    @Test
    @DisplayName("会员模块错误码在 5xxx 范围")
    void shouldMembershipCodesInRange5xxx() {
        assertInRange(ResultCode.MEMBERSHIP_PLAN_NOT_FOUND, 5000, 5999);
        assertInRange(ResultCode.MEMBERSHIP_ACCOUNT_DISABLED, 5000, 5999);
        assertInRange(ResultCode.MEMBERSHIP_USER_NOT_FOUND, 5000, 5999);
        assertInRange(ResultCode.MEMBERSHIP_USER_NOT_LOGGED_IN, 5000, 5999);
    }

    @Test
    @DisplayName("管理端错误码在 6xxx 范围")
    void shouldAdminCodesInRange6xxx() {
        assertInRange(ResultCode.ADMIN_CONFIG_NOT_FOUND, 6000, 6999);
        assertInRange(ResultCode.ADMIN_CODE_DUPLICATE, 6000, 6999);
        assertInRange(ResultCode.ADMIN_BATCH_SIZE_EXCEEDED, 6000, 6999);
        assertInRange(ResultCode.ADMIN_PROMPT_NOT_FOUND, 6000, 6999);
        assertInRange(ResultCode.ADMIN_AI_ENGINE_NOT_FOUND, 6000, 6999);
        assertInRange(ResultCode.ADMIN_JOB_ROLE_NOT_FOUND, 6000, 6999);
    }

    @Test
    @DisplayName("新增业务码的 message 为中文")
    void shouldNewBusinessCodesHaveChineseMessages() {
        // 抽检几个：确保非纯英文
        assertContainsChinese(ResultCode.RESUME_FILE_EMPTY.getMessage());
        assertContainsChinese(ResultCode.INTERVIEW_QUOTA_EXHAUSTED.getMessage());
        assertContainsChinese(ResultCode.AI_RESPONSE_EMPTY.getMessage());
        assertContainsChinese(ResultCode.MEMBERSHIP_PLAN_NOT_FOUND.getMessage());
        assertContainsChinese(ResultCode.ADMIN_CONFIG_NOT_FOUND.getMessage());
    }

    private void assertInRange(ResultCode rc, int min, int max) {
        int code = rc.getCode();
        assertTrue(code >= min && code <= max,
                rc.name() + " code=" + code + " 不在 " + min + "-" + max + " 范围内");
    }

    private void assertContainsChinese(String message) {
        assertTrue(message.codePoints().anyMatch(cp ->
                Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN),
                "消息应包含中文字符: " + message);
    }
}
