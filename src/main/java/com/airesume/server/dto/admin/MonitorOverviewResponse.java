package com.airesume.server.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 应用层监控总览响应参数。
 *
 * 本响应故意不强依赖中间件指标，
 * 仅使用当前业务表可直接统计的数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorOverviewResponse {

    /**
     * 待处理简历诊断任务数。
     */
    private Long pendingResumeTaskCount;

    /**
     * 处理中简历诊断任务数。
     */
    private Long processingResumeTaskCount;

    /**
     * 失败简历诊断任务数。
     */
    private Long failedResumeTaskCount;

    /**
     * 活跃面试会话数（进行中）。
     */
    private Long activeInterviewSessionCount;

    /**
     * 当日面试会话数。
     */
    private Long todayInterviewSessionCount;

    /**
     * 当日简历诊断任务数。
     */
    private Long todayResumeDiagnosisCount;
}
