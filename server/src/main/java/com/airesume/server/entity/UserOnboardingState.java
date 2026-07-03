package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户新手引导状态实体
 * 记录每位用户在不同引导流程中的当前状态
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_onboarding_state")
public class UserOnboardingState extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 引导版本标识，如 v1_2_main_onboarding */
    private String guideKey;

    /** 引导状态：not_started / in_progress / completed / skipped */
    private String status;

    /** 当前步骤索引（从0开始） */
    private Integer currentStep;

    /** 完成时间 */
    private LocalDateTime completedTime;

    /** 跳过时间 */
    private LocalDateTime skipTime;
}
