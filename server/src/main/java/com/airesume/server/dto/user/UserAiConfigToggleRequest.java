package com.airesume.server.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户自定义 AI 启停请求。
 */
@Data
public class UserAiConfigToggleRequest {

    /** true 表示启用，false 表示禁用。 */
    @NotNull(message = "启停状态不能为空")
    private Boolean enabled;
}
