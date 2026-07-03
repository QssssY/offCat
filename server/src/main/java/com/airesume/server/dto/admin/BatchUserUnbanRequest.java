package com.airesume.server.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理员批量解封用户请求。
 */
@Data
public class BatchUserUnbanRequest {

    @NotEmpty(message = "解封用户列表不能为空")
    @Size(max = 100, message = "批量解封最多支持100个用户")
    private List<Long> ids;

    @Size(max = 200, message = "解封原因不能超过200字")
    private String reason;
}
