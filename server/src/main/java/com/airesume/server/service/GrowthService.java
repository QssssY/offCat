package com.airesume.server.service;

import com.airesume.server.dto.growth.GrowthOverviewResponse;
import com.airesume.server.dto.growth.InterviewRadarResponse;

/**
 * 个人成长中心服务接口。
 * 聚合用户在简历诊断、岗位匹配、AI润色、模拟面试等维度的成长数据。
 */
public interface GrowthService {

    /**
     * 获取用户成长中心概览数据。
     *
     * @param userId 用户ID
     * @return 成长中心概览响应
     */
    GrowthOverviewResponse getGrowthOverview(Long userId);

    /**
     * 获取面试维度雷达数据、趋势和盲区提示。
     * 如果历史面试会话尚未写入维度评分表，会按需从 evaluation_report JSON 回填。
     *
     * @param userId 用户ID
     * @return 面试雷达响应
     */
    InterviewRadarResponse getInterviewRadar(Long userId);
}
