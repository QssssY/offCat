package com.airesume.server.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户密码修改请求参数
 * 用于接收前端传来的密码修改请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordUpdateRequest {

    /**
     * 原密码，用于验证用户身份
     */
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    /**
     * 新密码
     * 长度限制：6-100个字符
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度应为6-100个字符")
    private String newPassword;

}
