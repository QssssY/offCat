package com.airesume.server.dto.community;

import com.airesume.server.common.constants.CommunityConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员在用户端下架社区评论的请求。
 */
@Data
public class AdminHideCommentRequest {

    @NotBlank(message = "下架原因不能为空")
    @Size(max = CommunityConstants.MAX_ADMIN_HIDE_REASON_LENGTH, message = "下架原因不能超过200字")
    private String reason;
}
