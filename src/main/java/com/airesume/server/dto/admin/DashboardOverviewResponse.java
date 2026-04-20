package com.airesume.server.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端看板总览响应参数。
 *
 * 用于顶部汇总卡片展示的核心业务总量数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewResponse {

    /**
     * 用户总数。
     */
    private Long totalUserCount;

    /**
     * 会员用户总数（role = vip）。
     */
    private Long vipUserCount;

    /**
     * 启用 Prompt 总数。
     */
    private Long activePromptCount;

    /**
     * 启用岗位总数。
     */
    private Long activeJobRoleCount;

    /**
     * 启用 AI 引擎配置总数。
     */
    private Long activeAiEngineCount;

    /**
     * 当日面试会话数。
     */
    private Long todayInterviewSessionCount;

    /**
     * 当日简历诊断任务数。
     */
    private Long todayResumeDiagnosisCount;
}
