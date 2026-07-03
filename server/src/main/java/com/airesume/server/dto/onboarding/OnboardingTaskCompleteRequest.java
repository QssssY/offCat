package com.airesume.server.dto.onboarding;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新手任务完成上报请求
 */
@Data
public class OnboardingTaskCompleteRequest {

    /** 任务标识 */
    @NotBlank(message = "任务标识不能为空")
    private String taskKey;
}
