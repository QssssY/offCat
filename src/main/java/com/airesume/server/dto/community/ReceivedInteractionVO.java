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
 * 收到的互动信息VO（个人动态中心-互动信息tab）
 * 分组展示：点赞列表 + 评论列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivedInteractionVO {

    /** 收到的点赞列表 */
    private List<LikeItem> likes;

    /** 点赞总数 */
    private Integer totalLikes;

    /** 收到的评论列表 */
    private List<CommentItem> comments;

    /** 评论总数 */
    private Integer totalComments;

    /** 收到的回复列表（别人回复了我的评论） */
    private List<ReplyItem> replies;

    /** 回复总数 */
    private Integer totalReplies;

    /** 收到的收藏列表（别人收藏了我的帖子） */
    private List<FavoriteItem> favorites;

    /** 收藏总数 */
    private Integer totalFavorites;

    /** 是否还有更多数据（任一类型还有更多时为 true） */
    private Boolean hasMore;

    /**
     * 点赞条目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LikeItem {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long userId;
        private String userName;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long postId;
        private String postContent;
        private String postCategory;
        private LocalDateTime createTime;
    }

    /**
     * 评论条目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentItem {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long commentId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long userId;
        private String userName;
        private String commentContent;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long postId;
        private String postContent;
        private String postCategory;
        private LocalDateTime createTime;
    }

    /**
     * 回复条目（别人回复了我的评论）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReplyItem {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long replyId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long userId;
        private String userName;
        private String replyContent;
        /** 被回复的原评论内容 */
        private String parentCommentContent;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long parentCommentId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long postId;
        private String postContent;
        private String postCategory;
        private LocalDateTime createTime;
    }

    /**
     * 收藏条目（别人收藏了我的帖子）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FavoriteItem {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long userId;
        private String userName;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long postId;
        private String postContent;
        private String postCategory;
        private LocalDateTime createTime;
    }
}
