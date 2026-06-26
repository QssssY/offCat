package com.airesume.server.dto.admin;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 自定义 AI 趋势按日期聚合的活跃用户 Mapper 行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomAiUsageTrendActiveUserRow {

    private LocalDate date;
    private Integer activeUserCount;
}
