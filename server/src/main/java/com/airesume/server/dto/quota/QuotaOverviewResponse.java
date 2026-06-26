package com.airesume.server.dto.quota;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 6种额度总览响应 DTO（Dashboard 用）
 * 每种额度包含：类型代码、名称、剩余次数、上限、来源类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaOverviewResponse {

    /** 额度类型代码 */
    private String quotaType;

    /** 额度类型中文名 */
    private String quotaTypeName;

    /** 图标名称（前端 FeatureIcon 用） */
    private String iconName;

    /** 剩余次数 */
    private Integer remaining;

    /** 上限次数 */
    private Integer limit;

    /** 来源类型: FREE / VIP_DAILY */
    private String source;

    /** 是否已耗尽 */
    private Boolean exhausted;
}
