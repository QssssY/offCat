package com.airesume.server.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户昵称更新请求参数
 * 用于接收前端传来的昵称修改请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NicknameUpdateRequest {

    /**
     * 新昵称
     * 长度限制：2-12个字符
     */
    @NotBlank(message = "昵称不能为空")
    @Size(min = 2, max = 12, message = "昵称长度应为2-12个字符")
    private String nickname;

}