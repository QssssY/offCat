package com.airesume.server.dto.admin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端用户自定义 AI 每日统计响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomAiUsageStatsResponse {

    private LocalDate date;
    private Integer configuredUserCount;
    private Integer activeUserCount;
    private Integer totalCalls;
    private Long totalUsers;
    private Integer page;
    private Integer pageSize;
    @Builder.Default
    private List<CustomAiUsageTypeStatResponse> typeStats = new ArrayList<>();
    @Builder.Default
    private List<CustomAiUserUsageStatResponse> userStats = new ArrayList<>();
}
