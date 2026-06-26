package com.airesume.server.dto.quota;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消费记录响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumptionLogResponse {

    /** 记录ID */
    private Long id;

    /** 额度类型代码 */
    private String quotaType;

    /** 额度类型中文名 */
    private String quotaTypeName;

    /** 变动数量（正数=消耗，负数=退款） */
    private Integer changeAmount;

    /** 变动后余额 */
    private Integer balanceAfter;

    /** 扣减来源代码 */
    private String source;

    /** 扣减来源中文名 */
    private String sourceName;

    /** AI计费来源 */
    private String billingSource;

    /** 业务类型 */
    private String businessType;

    /** 业务类型中文名 */
    private String businessTypeName;

    /** 操作描述 */
    private String description;

    /** 记录时间 */
    private LocalDateTime createTime;
}
