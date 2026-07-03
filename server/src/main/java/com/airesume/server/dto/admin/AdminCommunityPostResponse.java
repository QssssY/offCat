package com.airesume.server.dto.admin;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端社区帖子审核列表响应。
 */
@Data
@Builder
public class AdminCommunityPostResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String authorName;

    private String category;

    private String title;

    private String content;

    private String images;

    private String reviewStatus;

    private String reviewReason;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long reviewedBy;

    private LocalDateTime reviewedTime;

    private LocalDateTime createTime;
}
