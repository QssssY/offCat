package com.airesume.server.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 看板业务分布响应参数。
 *
 * 用于展示所选时间范围内各业务线流量占比分布。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessDistributionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 查询开始日期（含），格式 yyyy-MM-dd。 */
    private String startDate;

    /** 查询结束日期（含），格式 yyyy-MM-dd。 */
    private String endDate;

    /** 范围内面试会话总数。 */
    private Long interviewCount;

    /** 范围内简历诊断任务总数。 */
    private Long resumeCount;

    /** 范围内简历润色总数。 */
    private Long resumePolishCount;

    /** 范围内JD匹配分析总数。 */
    private Long jdMatchCount;

    /** 范围内社区帖子总数。 */
    private Long communityPostCount;

    /** 范围内总数，用于前端图表标注。 */
    private Long totalCount;

    /** 面试占比，范围 [0,100]，保留两位小数。 */
    private Double interviewPercent;

    /** 简历诊断占比。 */
    private Double resumePercent;

    /** 简历润色占比。 */
    private Double polishPercent;

    /** JD匹配占比。 */
    private Double jdMatchPercent;

    /** 社区帖子占比。 */
    private Double communityPercent;
}
