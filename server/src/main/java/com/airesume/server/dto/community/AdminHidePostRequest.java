package com.airesume.server.dto.community;

import com.airesume.server.common.constants.CommunityConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员在用户端下架社区帖子的请求。
 * 作用：要求管理员填写明确原因，便于通知用户和后续追溯。
 */
@Data
public class AdminHidePostRequest {

    /** 下架原因，会写入审核原因并通知发帖用户 */
    @NotBlank(message = "下架原因不能为空")
    @Size(max = CommunityConstants.MAX_ADMIN_HIDE_REASON_LENGTH, message = "下架原因不能超过200字")
    private String reason;
}
