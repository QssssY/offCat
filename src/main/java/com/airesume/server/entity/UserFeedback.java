package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户问题反馈与建议记录。
 * 用途：承接用户侧提交的反馈内容，并供管理端按状态进行受理。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_feedback")
public class UserFeedback extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String type;

    private String title;

    private String content;

    private String contact;

    private Integer status;

    private String adminRemark;

    private Long handledBy;

    private LocalDateTime handledAt;
}
