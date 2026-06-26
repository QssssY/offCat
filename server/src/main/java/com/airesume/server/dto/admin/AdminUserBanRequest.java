package com.airesume.server.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员封禁单个用户请求。
 */
@Data
public class AdminUserBanRequest {

    @NotBlank(message = "封禁时长不能为空")
    private String duration;

    @NotBlank(message = "封禁原因不能为空")
    @Size(max = 200, message = "封禁原因不能超过200字")
    private String reason;
}
