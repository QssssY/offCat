package com.airesume.server.dto.community;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论视图对象（返回给前端）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO {

    /** 评论ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 所属帖子ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long postId;

    /** 评论者用户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /** 评论者昵称 */
    private String authorName;

    /** 评论者头像URL */
    private String authorAvatar;

    /** 评论内容 */
    private String content;

    /** 评论图片URL列表 */
    private List<String> images;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 是否为帖子作者 */
    private Boolean isPostAuthor;

    /** 当前用户是否可删除此评论 */
    private Boolean deletable;

    /** 父评论ID（null表示顶级评论） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentCommentId;

    /** 被回复者昵称（用于显示"@xxx"） */
    private String replyToUserName;

    /** 回复数量（仅顶级评论有值） */
    private Integer replyCount;
}
