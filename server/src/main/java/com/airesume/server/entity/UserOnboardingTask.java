package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户新手任务实体
 * 记录每位用户的新手任务完成状态
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_onboarding_task")
public class UserOnboardingTask extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 任务标识: resume_uploaded / report_viewed / jd_compared / interview_completed */
    private String taskKey;

    /** 是否完成: 0-否 1-是 */
    private Integer completed;

    /** 完成时间 */
    private LocalDateTime completedTime;
}
