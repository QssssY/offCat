package com.airesume.server.dto.admin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端用户自定义 AI 按日趋势响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomAiUsageTrendResponse {

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalCalls;
    private Integer activeUserCount;
    @Builder.Default
    private List<CustomAiUsageTrendDayResponse> days = new ArrayList<>();
}
