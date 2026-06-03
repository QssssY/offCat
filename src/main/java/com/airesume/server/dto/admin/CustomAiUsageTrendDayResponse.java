package com.airesume.server.dto.admin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户自定义 AI 趋势中的单日统计项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomAiUsageTrendDayResponse {

    private LocalDate date;
    private Integer totalCalls;
    private Integer activeUserCount;
    @Builder.Default
    private List<CustomAiUsageTypeStatResponse> typeStats = new ArrayList<>();
}
