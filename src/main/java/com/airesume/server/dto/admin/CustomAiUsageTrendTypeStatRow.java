package com.airesume.server.dto.admin;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 自定义 AI 趋势按日期和功能聚合的 Mapper 行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomAiUsageTrendTypeStatRow {

    private LocalDate date;
    private String usageType;
    private Integer callCount;
}
