package com.airesume.server.dto.community;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建评论请求DTO
 */
@Data
public class CreateCommentRequest {

    /** 评论内容 */
    @Size(max = 500, message = "评论内容不能超过500字")
    private String content;

    /** 父评论ID（回复时传入，不传则为顶级评论） */
    private Long parentCommentId;

    /** 被回复用户ID（回复时传入） */
    private Long replyToUserId;

    /** 评论图片URL列表（已上传的图片URL） */
    private List<String> images;
}
