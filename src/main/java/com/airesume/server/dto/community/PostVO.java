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
 * 帖子视图对象（返回给前端）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostVO {

    /** 帖子ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 发布者用户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /** 发布者昵称 */
    private String authorName;

    /** 发布者头像URL */
    private String authorAvatar;

    /** 帖子板块 */
    private String category;

    /** 帖子内容 */
    private String content;

    /** 图片URL列表 */
    private List<String> images;

    /** 点赞数 */
    private Integer likeCount;

    /** 评论数 */
    private Integer commentCount;

    /** 当前用户是否已点赞 */
    private Boolean liked;

    /** 当前用户是否已收藏 */
    private Boolean favorited;

    /** 创建时间 */
    private LocalDateTime createTime;
}
