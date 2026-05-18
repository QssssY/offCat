package com.airesume.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 社区帖子收藏实体
 * 对应数据库表 community_post_favorite
 * 通过 UNIQUE(post_id, user_id) 保证幂等性
 *
 * 注意：此实体不继承BaseEntity，也不包含isDeleted字段。
 * 原因：收藏表有唯一约束 UNIQUE(post_id, user_id)，
 * 如果使用逻辑删除，取消收藏后记录仍占位（is_deleted=1），
 * 再次收藏时唯一约束会冲突。因此采用物理删除。
 */
@Data
@TableName(value = "community_post_favorite", autoResultMap = false)
public class CommunityPostFavorite implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（雪花算法） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 帖子ID */
    private Long postId;

    /** 收藏用户ID */
    private Long userId;

    /** 创建时间 */
    private LocalDateTime createTime;
}
