package com.airesume.server.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户额度响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserQuotaResponse {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 累计已使用的免费面试次数
     */
    private Integer totalInterviewUsed;

    /**
     * 累计已使用的免费简历诊断次数
     */
    private Integer totalResumeUsed;

    /**
     * 今日已使用面试次数
     */
    private Integer dailyInterviewUsed;

    /**
     * 今日已使用简历诊断次数
     */
    private Integer dailyResumeUsed;

    private Integer interviewQuota;

    private Integer resumeQuota;

    /**
     * 最后刷新日期记录
     */
    private LocalDate lastRefreshDate;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
