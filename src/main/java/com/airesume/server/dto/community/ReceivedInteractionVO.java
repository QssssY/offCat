package com.airesume.server.dto.community;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 收到的互动信息VO（个人动态中心-互动信息tab）
 * 分组展示：点赞列表 + 评论列表
 */
@Data
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

    public static ReceivedInteractionVOBuilder builder() {
        return new ReceivedInteractionVOBuilder();
    }

    public static class ReceivedInteractionVOBuilder {
        private final ReceivedInteractionVO target = new ReceivedInteractionVO();

        public ReceivedInteractionVOBuilder likes(List<LikeItem> likes) {
            target.setLikes(likes);
            return this;
        }

        public ReceivedInteractionVOBuilder totalLikes(Integer totalLikes) {
            target.setTotalLikes(totalLikes);
            return this;
        }

        public ReceivedInteractionVOBuilder comments(List<CommentItem> comments) {
            target.setComments(comments);
            return this;
        }

        public ReceivedInteractionVOBuilder totalComments(Integer totalComments) {
            target.setTotalComments(totalComments);
            return this;
        }

        public ReceivedInteractionVOBuilder replies(List<ReplyItem> replies) {
            target.setReplies(replies);
            return this;
        }

        public ReceivedInteractionVOBuilder totalReplies(Integer totalReplies) {
            target.setTotalReplies(totalReplies);
            return this;
        }

        public ReceivedInteractionVOBuilder favorites(List<FavoriteItem> favorites) {
            target.setFavorites(favorites);
            return this;
        }

        public ReceivedInteractionVOBuilder totalFavorites(Integer totalFavorites) {
            target.setTotalFavorites(totalFavorites);
            return this;
        }

        public ReceivedInteractionVOBuilder hasMore(Boolean hasMore) {
            target.setHasMore(hasMore);
            return this;
        }

        public ReceivedInteractionVO build() {
            return target;
        }
    }

    /**
     * 点赞条目
     */
    @Data
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

        public static LikeItemBuilder builder() {
            return new LikeItemBuilder();
        }

        public static class LikeItemBuilder {
            private final LikeItem target = new LikeItem();

            public LikeItemBuilder userId(Long userId) { target.setUserId(userId); return this; }
            public LikeItemBuilder userName(String userName) { target.setUserName(userName); return this; }
            public LikeItemBuilder postId(Long postId) { target.setPostId(postId); return this; }
            public LikeItemBuilder postContent(String postContent) { target.setPostContent(postContent); return this; }
            public LikeItemBuilder postCategory(String postCategory) { target.setPostCategory(postCategory); return this; }
            public LikeItemBuilder createTime(LocalDateTime createTime) { target.setCreateTime(createTime); return this; }
            public LikeItem build() { return target; }
        }
    }

    /**
     * 评论条目
     */
    @Data
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

        public static CommentItemBuilder builder() {
            return new CommentItemBuilder();
        }

        public static class CommentItemBuilder {
            private final CommentItem target = new CommentItem();

            public CommentItemBuilder commentId(Long commentId) { target.setCommentId(commentId); return this; }
            public CommentItemBuilder userId(Long userId) { target.setUserId(userId); return this; }
            public CommentItemBuilder userName(String userName) { target.setUserName(userName); return this; }
            public CommentItemBuilder commentContent(String commentContent) { target.setCommentContent(commentContent); return this; }
            public CommentItemBuilder postId(Long postId) { target.setPostId(postId); return this; }
            public CommentItemBuilder postContent(String postContent) { target.setPostContent(postContent); return this; }
            public CommentItemBuilder postCategory(String postCategory) { target.setPostCategory(postCategory); return this; }
            public CommentItemBuilder createTime(LocalDateTime createTime) { target.setCreateTime(createTime); return this; }
            public CommentItem build() { return target; }
        }
    }

    /**
     * 回复条目（别人回复了我的评论）
     */
    @Data
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

        public static ReplyItemBuilder builder() {
            return new ReplyItemBuilder();
        }

        public static class ReplyItemBuilder {
            private final ReplyItem target = new ReplyItem();

            public ReplyItemBuilder replyId(Long replyId) { target.setReplyId(replyId); return this; }
            public ReplyItemBuilder userId(Long userId) { target.setUserId(userId); return this; }
            public ReplyItemBuilder userName(String userName) { target.setUserName(userName); return this; }
            public ReplyItemBuilder replyContent(String replyContent) { target.setReplyContent(replyContent); return this; }
            public ReplyItemBuilder parentCommentContent(String parentCommentContent) { target.setParentCommentContent(parentCommentContent); return this; }
            public ReplyItemBuilder parentCommentId(Long parentCommentId) { target.setParentCommentId(parentCommentId); return this; }
            public ReplyItemBuilder postId(Long postId) { target.setPostId(postId); return this; }
            public ReplyItemBuilder postContent(String postContent) { target.setPostContent(postContent); return this; }
            public ReplyItemBuilder postCategory(String postCategory) { target.setPostCategory(postCategory); return this; }
            public ReplyItemBuilder createTime(LocalDateTime createTime) { target.setCreateTime(createTime); return this; }
            public ReplyItem build() { return target; }
        }
    }

    /**
     * 收藏条目（别人收藏了我的帖子）
     */
    @Data
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

        public static FavoriteItemBuilder builder() {
            return new FavoriteItemBuilder();
        }

        public static class FavoriteItemBuilder {
            private final FavoriteItem target = new FavoriteItem();

            public FavoriteItemBuilder userId(Long userId) { target.setUserId(userId); return this; }
            public FavoriteItemBuilder userName(String userName) { target.setUserName(userName); return this; }
            public FavoriteItemBuilder postId(Long postId) { target.setPostId(postId); return this; }
            public FavoriteItemBuilder postContent(String postContent) { target.setPostContent(postContent); return this; }
            public FavoriteItemBuilder postCategory(String postCategory) { target.setPostCategory(postCategory); return this; }
            public FavoriteItemBuilder createTime(LocalDateTime createTime) { target.setCreateTime(createTime); return this; }
            public FavoriteItem build() { return target; }
        }
    }
}
