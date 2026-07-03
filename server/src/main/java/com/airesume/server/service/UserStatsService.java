package com.airesume.server.service;

import java.util.Map;

/**
 * 用户维度统计服务接口
 */
public interface UserStatsService {

    /**
     * 获取当前用户的月度统计数据
     *
     * @param userId 用户ID
     * @return 统计数据（resumeCountThisMonth, interviewCountThisMonth）
     */
    Map<String, Long> getMonthlyStats(Long userId);
}
