package com.airesume.server.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 看板趋势数据项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardTrendResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 趋势日期。 */
    private LocalDate date;

    /** 当天面试会话数。 */
    private Long interviewSessionCount;

    /** 当天简历诊断任务数。 */
    private Long resumeDiagnosisCount;

    /** 当天订单数。 */
    private Long orderCount;

    /** 当天已支付订单收入。 */
    private BigDecimal orderRevenue;
}
