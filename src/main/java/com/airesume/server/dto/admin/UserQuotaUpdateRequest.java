package com.airesume.server.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

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
    /**
     * 最后刷新日期。
     *
     * 作用：
     * 管理员可以通过这个字段修正每日额度刷新边界，
     * 避免 daily_*_used 与 lastRefreshDate 出现不一致。
     */
    private LocalDate lastRefreshDate;
}
