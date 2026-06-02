package com.airesume.server.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码ID（失败3次后需要提供） */
    private String captchaId;

    /** 验证码（失败3次后需要提供） */
    private String captchaCode;

}
