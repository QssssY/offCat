package com.airesume.server.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员修改用户自定义 AI 每日上限请求。
 */
@Data
public class CustomAiDailyLimitRequest {

    @NotNull(message = "每日上限不能为空")
    @Min(value = 1, message = "每日上限不能小于 1")
    @Max(value = 10000, message = "每日上限不能超过 10000")
    private Integer limit;
}
