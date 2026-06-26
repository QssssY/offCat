package com.airesume.server.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 管理端看板总览响应参数。
 *
 * 用于顶部汇总卡片展示的核心业务总量数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户总数。 */
    private Long totalUserCount;

    /** 会员用户总数（role = vip）。 */
    private Long vipUserCount;

    /** 启用 Prompt 总数。 */
    private Long activePromptCount;

    /** 启用岗位总数。 */
    private Long activeJobRoleCount;

    /** 启用 AI 引擎配置总数。 */
    private Long activeAiEngineCount;

    /** 范围内面试会话数。 */
    private Long todayInterviewSessionCount;

    /** 范围内简历诊断任务数。 */
    private Long todayResumeDiagnosisCount;

    /** 范围内用户反馈数。 */
    private Long feedbackCount;

    /** 范围内社区帖子数。 */
    private Long communityPostCount;

    /** 范围内简历润色数。 */
    private Long resumePolishCount;

    /** 范围内JD匹配分析数。 */
    private Long jdMatchCount;

    /** 范围内订单总数。 */
    private Long orderCount;

    /** 范围内已支付订单总收入。 */
    private BigDecimal orderRevenue;
}
