package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户自定义 AI 每日调用次数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_ai_daily_usage")
public class UserAiDailyUsage extends BaseEntity {

    /** 配置归属用户。 */
    @TableField("user_id")
    private Long userId;

    /** 统计日期。 */
    @TableField("usage_date")
    private LocalDate usageDate;

    /** 当日已调用次数，所有自定义 AI 功能合计。 */
    @TableField("call_count")
    private Integer callCount;
}
