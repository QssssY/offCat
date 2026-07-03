package com.airesume.server.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员解封单个用户请求。
 */
@Data
public class AdminUserUnbanRequest {

    @Size(max = 200, message = "解封原因不能超过200字")
    private String reason;
}
