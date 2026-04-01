package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_quota")
public class UserQuota extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private Integer totalInterviewUsed;

    private Integer totalResumeUsed;

    private Integer dailyInterviewUsed;

    private Integer dailyResumeUsed;

    private LocalDate lastRefreshDate;

}
