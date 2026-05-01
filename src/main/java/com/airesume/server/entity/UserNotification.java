package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户站内通知实体
 * 记录系统发送给用户的通知消息，支持标记已读
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_notification")
public class UserNotification extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 所属用户ID */
    private Long userId;

    /** 通知类型: resume/polish/interview/quota/system */
    private String type;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 关联业务类型: resume_diagnosis/resume_polish/mock_interview/quota */
    private String bizType;

    /** 关联业务ID */
    private String bizId;

    /** 已读状态: 0未读 1已读 */
    private Integer readStatus;

    /** 已读时间 */
    private LocalDateTime readTime;
}
