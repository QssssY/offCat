package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 社区图片登记实体。
 * 用途：记录用户上传后尚未绑定到帖子/评论的图片，防止上传接口被当作匿名图床滥用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("community_image")
public class CommunityImage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 上传用户ID，绑定时必须与当前登录用户一致。 */
    private Long userId;

    /** OSS 对象键，例如 community/1001/20260605/abcdef.jpg。 */
    private String objectKey;

    /** 前端保存和提交的本站代理访问地址。 */
    private String proxyUrl;

    /** uploaded-已上传待绑定；bound-已绑定到业务内容。 */
    private String status;

    /** 绑定类型：post/comment。 */
    private String boundType;

    /** 绑定的帖子ID或评论ID。 */
    private Long boundId;

    /** 完成绑定的时间，用于审计和后续排查。 */
    private LocalDateTime boundTime;
}
