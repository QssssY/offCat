package com.airesume.server.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminNotificationResponse {

    private Long id;
    private String title;
    private String content;
    private String type;
    private String typeDesc;
    private String targetType;
    private String targetTypeDesc;
    private Integer status;
    private String statusDesc;
    private LocalDateTime publishedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
