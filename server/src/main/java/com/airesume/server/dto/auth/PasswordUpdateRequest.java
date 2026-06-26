package com.airesume.server.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户修改密码请求参数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordUpdateRequest {

    /**
     * 原密码，用于校验当前用户身份。
     */
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    /**
     * 新密码，需与注册和找回密码规则保持一致。
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度应为6-100个字符")
    @Pattern(regexp = AuthPasswordRules.LETTER_AND_DIGIT_REGEX, message = AuthPasswordRules.LETTER_AND_DIGIT_MESSAGE)
    private String newPassword;
}
