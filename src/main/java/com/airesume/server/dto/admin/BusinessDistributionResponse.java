package com.airesume.server.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 看板业务分布响应参数。
 *
 * 用于展示所选时间范围内 interview/resume 业务流量占比分布。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessDistributionResponse {

    /**
     * 查询开始日期（含），格式 yyyy-MM-dd。
     */
    private String startDate;

    /**
     * 查询结束日期（含），格式 yyyy-MM-dd。
     */
    private String endDate;

    /**
     * 范围内面试会话总数。
     */
    private Long interviewCount;

    /**
     * 范围内简历诊断任务总数。
     */
    private Long resumeCount;

    /**
     * 范围内总数，用于前端图表标注。
     */
    private Long totalCount;

    /**
     * 面试占比，范围 [0,100]，保留两位小数。
     */
    private Double interviewPercent;

    /**
     * 简历占比，范围 [0,100]，保留两位小数。
     */
    private Double resumePercent;
}
