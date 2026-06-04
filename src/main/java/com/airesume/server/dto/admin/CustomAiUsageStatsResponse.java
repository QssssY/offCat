package com.airesume.server.dto.admin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端用户自定义 AI 统计响应。
 *
 * date 保留给旧版单日统计回显；startDate/endDate 表示当前统计实际覆盖的日期范围。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomAiUsageStatsResponse {

    private LocalDate date;
    private LocalDate startDate;
    private LocalDate endDate;
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
