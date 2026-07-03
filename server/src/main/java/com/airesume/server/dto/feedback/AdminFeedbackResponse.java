package com.airesume.server.dto.feedback;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端反馈列表与详情响应。
 */
@Data
@Builder
public class AdminFeedbackResponse {

    private Long id;
    private Long userId;
    private String username;
    private String type;
    private String typeDesc;
    private String title;
    private String content;
    private String contact;
    private Integer status;
    private String statusDesc;
    private String adminRemark;
    private Long handledBy;
    private String handlerName;
    private LocalDateTime handledAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
