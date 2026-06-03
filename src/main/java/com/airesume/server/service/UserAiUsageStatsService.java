package com.airesume.server.service;

import com.airesume.server.dto.admin.CustomAiUsageStatsResponse;
import com.airesume.server.dto.admin.CustomAiUsageTrendResponse;
import java.time.LocalDate;

/**
 * 用户自定义 AI 用量统计服务。
 */
public interface UserAiUsageStatsService {

    /**
     * 按日期查询自定义 AI 总览、功能拆分和用户明细。
     */
    CustomAiUsageStatsResponse getDailyStats(LocalDate date, int page, int pageSize);

    /**
     * 按日期范围查询自定义 AI 每日趋势。
     */
    CustomAiUsageTrendResponse getUsageTrends(LocalDate startDate, LocalDate endDate);
}
