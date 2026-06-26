package com.airesume.server.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改安全问题请求参数
 * 用于登录用户修改安全问题和答案，需验证原密码
 */
@Data
public class SecurityQuestionUpdateRequest {

    /** 原密码，用于验证用户身份 */
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    /** 安全问题文本 */
    @NotBlank(message = "安全问题不能为空")
    @Size(max = 50, message = "安全问题长度不能超过50个字符")
    private String securityQuestion;

    /** 安全问题答案 */
    @NotBlank(message = "安全答案不能为空")
    @Size(max = 100, message = "安全答案长度不能超过100个字符")
    private String securityAnswer;

}
