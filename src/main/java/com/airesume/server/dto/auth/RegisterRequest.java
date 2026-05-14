package com.airesume.server.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度需要在3-50之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度需要在6-100之间")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$", message = "密码必须包含字母和数字")
    private String password;

    /** 安全问题文本（前端必填，后端不强制校验以保持向后兼容） */
    @Size(max = 50, message = "安全问题长度不能超过50个字符")
    private String securityQuestion;

    /** 安全问题答案 */
    @Size(max = 100, message = "安全问题答案长度不能超过100个字符")
    private String securityAnswer;

}
