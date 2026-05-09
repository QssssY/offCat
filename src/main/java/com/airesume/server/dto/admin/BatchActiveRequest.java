package com.airesume.server.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量启用/禁用请求DTO
 */
@Data
public class BatchActiveRequest {
    @NotEmpty(message = "操作对象列表不能为空")
    private List<Long> ids;

    @NotNull(message = "isActive 不能为空")
    @Min(value = 0, message = "isActive 只能为 0 或 1")
    @Max(value = 1, message = "isActive 只能为 0 或 1")
    private Integer isActive;
}