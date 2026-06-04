package com.airesume.server.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 管理端看板聚合响应。
 *
 * 一次返回看板首屏需要的四块数据，前端可用单请求替代旧的四个并发请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 顶部总览卡片数据。 */
    private DashboardOverviewResponse overview;

    /** 日期趋势图数据。 */
    private List<DashboardTrendResponse> trends;

    /** 热门岗位排行。 */
    private List<HotJobRoleResponse> hotJobRoles;

    /** 业务分布统计。 */
    private BusinessDistributionResponse businessDistribution;
}
