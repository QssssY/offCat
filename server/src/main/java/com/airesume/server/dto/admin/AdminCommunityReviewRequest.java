package com.airesume.server.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端社区内容审核请求。
 */
@Data
public class AdminCommunityReviewRequest {

    /** 目标审核状态：approved / rejected / hidden */
    @NotBlank(message = "审核状态不能为空")
    private String reviewStatus;

    /** 审核原因，拒绝或隐藏时用于回显给发布者 */
    @Size(max = 255, message = "审核原因不能超过255字")
    private String reviewReason;
}
