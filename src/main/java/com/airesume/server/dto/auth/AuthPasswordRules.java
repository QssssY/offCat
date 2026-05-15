package com.airesume.server.dto.auth;

/**
 * 认证密码规则常量。
 */
public final class AuthPasswordRules {

    public static final String LETTER_AND_DIGIT_REGEX = "^(?=.*[a-zA-Z])(?=.*\\d).+$";
    public static final String LETTER_AND_DIGIT_MESSAGE = "密码必须包含字母和数字";

    private AuthPasswordRules() {
    }
}
