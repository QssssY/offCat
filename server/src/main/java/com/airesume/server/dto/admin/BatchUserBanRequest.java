package com.airesume.server.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理员批量封禁用户请求。
 */
@Data
public class BatchUserBanRequest {

    @NotEmpty(message = "封禁用户列表不能为空")
    @Size(max = 100, message = "批量封禁最多支持100个用户")
    private List<Long> ids;

    @NotBlank(message = "封禁时长不能为空")
    private String duration;

    @NotBlank(message = "封禁原因不能为空")
    @Size(max = 200, message = "封禁原因不能超过200字")
    private String reason;
}
