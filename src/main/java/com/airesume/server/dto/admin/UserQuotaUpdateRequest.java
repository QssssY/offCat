package com.airesume.server.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 调整用户额度请求DTO
 */
@Data
public class UserQuotaUpdateRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 累计已使用的免费面试次数（调整后的值）
     */
    private Integer totalInterviewUsed;

    /**
     * 累计已使用的免费简历诊断次数（调整后的值）
     */
    private Integer totalResumeUsed;

    /**
     * 今日已使用面试次数（调整后的值）
     */
    private Integer dailyInterviewUsed;

    /**
     * 今日已使用简历诊断次数（调整后的值）
     */
    private Integer dailyResumeUsed;
}
