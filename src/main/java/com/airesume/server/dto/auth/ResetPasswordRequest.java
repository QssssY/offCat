package com.airesume.server.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 通过安全问题重置密码的请求 DTO
 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "安全问题答案不能为空")
    private String securityAnswer;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度需要在6-100之间")
    private String newPassword;

}
