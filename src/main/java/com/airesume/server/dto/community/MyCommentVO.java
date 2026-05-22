package com.airesume.server.dto.community;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 我的评论视图对象（个人动态中心-评论过的帖子）
 * 主体是评论，附带所属帖子信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyCommentVO {

    // ===== 评论信息（主要） =====

    /** 评论ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long commentId;

    /** 评论内容 */
    private String commentContent;

    /** 评论创建时间 */
    private LocalDateTime commentTime;

    /** 父评论ID，NULL表示顶级评论 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentCommentId;

    // ===== 所属帖子信息（次要） =====

    /** 帖子ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long postId;

    /** 帖子板块 */
    private String postCategory;

    /** 帖子内容摘要 */
    private String postContent;

    /** 帖子作者昵称 */
    private String postAuthorName;

    /** 帖子图片JSON */
    private String postImages;

    /** 帖子是否已被删除 */
    private Boolean postDeleted;
}
