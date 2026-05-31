package com.airesume.server.dto.community;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 社区内容创建响应。
 * 作用：返回新内容 ID 与最终审核状态，方便前端按真实状态提示和决定是否乐观展示。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommunityContentResponse {

    /** 新创建的帖子或评论 ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 创建后的审核状态：approved / pending */
    private String reviewStatus;
}
