package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户设置实体。
 * 用于保存需要服务端生效的用户偏好，避免浏览器本地缓存被误当成真实清理策略。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_settings")
public class UserSettings extends BaseEntity {

    /** 当前设置所属用户 ID，表内保持唯一。 */
    @TableField("user_id")
    private Long userId;

    /** 面试记录自动保留天数，0 表示不自动清理。 */
    @TableField("interview_retention_days")
    private Integer interviewRetentionDays;

    /** 简历诊断记录自动保留天数，0 表示不自动清理。 */
    @TableField("resume_retention_days")
    private Integer resumeRetentionDays;
}
