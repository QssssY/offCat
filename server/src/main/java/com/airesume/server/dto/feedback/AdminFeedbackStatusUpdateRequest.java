package com.airesume.server.dto.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端更新反馈处理状态的请求体。
 */
@Data
public class AdminFeedbackStatusUpdateRequest {

    @NotNull(message = "反馈状态不能为空")
    @Min(value = 0, message = "反馈状态不合法")
    @Max(value = 3, message = "反馈状态不合法")
    private Integer status;

    @Size(max = 1000, message = "处理备注不能超过 1000 个字符")
    private String adminRemark;
}
