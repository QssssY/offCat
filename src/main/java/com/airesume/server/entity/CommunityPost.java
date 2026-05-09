package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 社区帖子实体
 * 对应数据库表 community_post
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("community_post")
public class CommunityPost extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 发布者用户ID */
    private Long userId;

    /** 帖子板块：interview_exp-面试经验分享，referral-内推广场 */
    private String category;

    /** 帖子内容 */
    private String content;

    /** 图片URL列表JSON数组 */
    private String images;

    /** 点赞数 */
    private Integer likeCount;

    /** 评论数 */
    private Integer commentCount;
}
