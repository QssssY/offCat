package com.airesume.server.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新手引导状态响应 DTO
 * 返回当前用户的引导状态和是否需要展示引导
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingStatusResponse {

    /** 引导版本标识 */
    private String guideKey;

    /** 引导状态：not_started / in_progress / completed / skipped */
    private String status;

    /** 当前步骤索引 */
    private Integer currentStep;

    /** 是否需要展示引导（状态为 not_started 或 in_progress 时为 true） */
    private Boolean showGuide;
}
