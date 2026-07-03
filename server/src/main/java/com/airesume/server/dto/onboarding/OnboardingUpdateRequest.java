package com.airesume.server.dto.onboarding;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新手引导状态更新请求 DTO
 * 用于更新引导进度、完成或跳过引导
 */
@Data
public class OnboardingUpdateRequest {

    /** 引导版本标识 */
    @NotBlank(message = "guideKey不能为空")
    private String guideKey;

    /** 目标状态：in_progress / completed / skipped */
    @NotBlank(message = "status不能为空")
    private String status;

    /** 当前步骤索引，status=in_progress 时必填 */
    private Integer currentStep;
}
