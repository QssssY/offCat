package com.airesume.server.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知响应视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO {

    /** 通知ID */
    private Long id;

    /** 通知类型: resume/polish/interview/quota/system */
    private String type;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 关联业务类型 */
    private String bizType;

    /** 关联业务ID */
    private String bizId;

    /** 关联系统公告ID */
    private Long broadcastId;

    /** 已读状态: 0未读 1已读 */
    private Integer readStatus;

    /** 已读时间 */
    private LocalDateTime readTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
