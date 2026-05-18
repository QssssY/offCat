package com.airesume.server.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VersionLogResponse {

    private Long id;
    private String version;
    private String title;
    private String content;
    private String type;
    private String typeDesc;
    private Integer status;
    private String statusDesc;
    private LocalDateTime publishedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
