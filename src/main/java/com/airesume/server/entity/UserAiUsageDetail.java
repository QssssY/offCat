package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户自定义 AI 按功能聚合的每日调用明细。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_ai_usage_detail")
public class UserAiUsageDetail extends BaseEntity {

    /** 配置归属用户。 */
    @TableField("user_id")
    private Long userId;

    /** 统计日期。 */
    @TableField("usage_date")
    private LocalDate usageDate;

    /** 功能统计口径，如 resume_diagnosis / interview_message。 */
    @TableField("usage_type")
    private String usageType;

    /** 当日该功能已调用次数。 */
    @TableField("call_count")
    private Integer callCount;
}
