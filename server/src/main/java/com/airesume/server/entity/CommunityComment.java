package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 社区评论实体
 * 对应数据库表 community_comment
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("community_comment")
public class CommunityComment extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 所属帖子ID */
    private Long postId;

    /** 评论者用户ID */
    private Long userId;

    /** 父评论ID，NULL表示顶级评论 */
    private Long parentCommentId;

    /** 被回复用户ID */
    private Long replyToUserId;

    /** 评论内容 */
    private String content;

    /** 评论图片URL列表（JSON数组） */
    private String images;

    /** 内容审核状态：pending-待审，approved-通过，rejected-拒绝，hidden-隐藏 */
    private String reviewStatus;

    /** 审核原因或违规说明 */
    private String reviewReason;

    /** 审核管理员用户ID */
    private Long reviewedBy;

    /** 审核时间 */
    private LocalDateTime reviewedTime;
}
