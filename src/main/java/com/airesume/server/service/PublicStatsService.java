package com.airesume.server.service;

import java.util.Map;

/**
 * 公开统计服务接口
 */
public interface PublicStatsService {

    /**
     * 获取平台公开统计数据
     *
     * @return 统计数据（userCount, diagnosisCount, interviewCount）
     */
    Map<String, Long> getPublicStats();
}
