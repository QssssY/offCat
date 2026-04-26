package com.airesume.server.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NicknameUpdateRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(min = 2, max = 12, message = "昵称长度应为2-12个字符")
    private String nickname;

}