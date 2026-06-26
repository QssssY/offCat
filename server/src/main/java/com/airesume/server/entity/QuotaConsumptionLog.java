package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户额度消费记录实体
 * 记录每一次额度扣减和退款，支持消费溯源
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_quota_consumption_log")
public class QuotaConsumptionLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 额度类型: INTERVIEW/RESUME/POLISH/JD_MATCH/TEMPLATE/OFFER */
    private String quotaType;

    /** 变动数量（正数=消耗，负数=退款） */
    private Integer changeAmount;

    /** 变动后该类型额度余额（当日剩余或免费剩余），可为 NULL */
    private Integer balanceAfter;

    /** 扣减来源: FREE/VIP_DAILY/VIP_CYCLE */
    private String source;

    /** AI计费来源: PLATFORM/USER_CUSTOM/PLATFORM_FALLBACK（仅AI功能有值） */
    private String billingSource;

    /** 关联业务ID（面试sessionID/简历taskID/润色recordID等） */
    private Long businessId;

    /** 业务类型标识 */
    private String businessType;

    /** 操作描述 */
    private String description;
}
