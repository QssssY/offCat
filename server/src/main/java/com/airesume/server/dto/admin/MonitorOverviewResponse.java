package com.airesume.server.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

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
public class MonitorOverviewResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
     * 已完成简历诊断任务数。
     */
    private Long completedResumeTaskCount;

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

    /**
     * 当日 AI 简历润色次数。
     */
    private Long todayResumePolishCount;

    /**
     * 当日 JD 匹配分析次数。
     */
    private Long todayJobMatchCount;

    /**
     * 当日社区发帖数。
     */
    private Long todayCommunityPostCount;

    /**
     * 待处理用户反馈数。
     */
    private Long pendingFeedbackCount;

    /**
     * 处理中用户反馈数。
     */
    private Long processingFeedbackCount;

    /**
     * 当日用户反馈提交数。
     */
    private Long todayFeedbackCount;

    /**
     * 待审核社区帖子数。
     */
    private Long pendingCommunityPostCount;

    /**
     * 待审核社区评论数。
     */
    private Long pendingCommunityCommentCount;

    /**
     * 社区待审核总数，等于待审核帖子数 + 待审核评论数。
     */
    private Long pendingCommunityReviewCount;

    /**
     * 当日会员订单数。
     */
    private Long todayOrderCount;
}
