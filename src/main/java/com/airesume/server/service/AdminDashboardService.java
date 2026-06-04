package com.airesume.server.service;

import com.airesume.server.dto.admin.DashboardOverviewResponse;
import com.airesume.server.dto.admin.DashboardSummaryResponse;
import com.airesume.server.dto.admin.DashboardTrendResponse;
import com.airesume.server.dto.admin.BusinessDistributionResponse;
import com.airesume.server.dto.admin.HotJobRoleResponse;
import com.airesume.server.dto.admin.MonitorOverviewResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 管理端看板与监控聚合服务。
 */
public interface AdminDashboardService {

    /**
     * 查询看板总览统计。
     *
     * @return 总览统计结果
     */
    DashboardOverviewResponse getDashboardOverview(LocalDate startDate, LocalDate endDate);

    /**
     * 查询看板首屏聚合数据。
     *
     * @return 总览、趋势、热门岗位和业务分布四块数据
     */
    DashboardSummaryResponse getDashboardSummary(LocalDate startDate, LocalDate endDate, Integer hotRoleLimit);

    /**
     * 查询趋势统计。
     *
     * @return 按日期升序的趋势列表
     */
    List<DashboardTrendResponse> getDashboardTrends(LocalDate startDate, LocalDate endDate);

    /**
     * 查询热门岗位排行。
     *
     * @return 岗位排行结果
     */
    List<HotJobRoleResponse> getHotJobRoles(LocalDate startDate, LocalDate endDate, Integer limit);

    /**
     * 查询日期范围内 interview/resume 业务分布。
     *
     * @param startDate 查询开始日期（含）
     * @param endDate 查询结束日期（含）
     * @return 业务分布统计结果
     */
    BusinessDistributionResponse getBusinessDistribution(LocalDate startDate, LocalDate endDate);

    /**
     * 查询应用层监控总览。
     *
     * @return 监控统计结果
     */
    MonitorOverviewResponse getMonitorOverview();
}
