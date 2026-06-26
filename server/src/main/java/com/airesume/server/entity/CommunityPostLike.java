package com.airesume.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 社区帖子点赞实体
 * 对应数据库表 community_post_like
 * 通过 UNIQUE(post_id, user_id) 保证幂等性
 *
 * 注意：此实体不继承BaseEntity，也不包含isDeleted字段。
 * 原因：点赞表有唯一约束 UNIQUE(post_id, user_id)，
 * 如果使用逻辑删除，取消点赞后记录仍占位（is_deleted=1），
 * 再次点赞时唯一约束会冲突。因此采用物理删除。
 */
@Data
@TableName(value = "community_post_like", autoResultMap = false)
public class CommunityPostLike implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（雪花算法） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 帖子ID */
    private Long postId;

    /** 点赞用户ID */
    private Long userId;

    /** 创建时间 */
    private LocalDateTime createTime;
}
