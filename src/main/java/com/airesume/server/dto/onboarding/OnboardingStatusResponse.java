package com.airesume.server.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 新手引导状态响应 DTO
 * 返回当前用户的引导状态和是否需要展示引导
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingStatusResponse implements Serializable {

    /** 引导状态会进入 Redis JDK 序列化缓存，固定序列化版本避免运行期写缓存失败。 */
    private static final long serialVersionUID = 1L;

    /** 引导版本标识 */
    private String guideKey;

    /** 引导状态：not_started / in_progress / completed / skipped */
    private String status;

    /** 当前步骤索引 */
    private Integer currentStep;

    /** 是否需要展示引导（状态为 not_started 或 in_progress 时为 true） */
    private Boolean showGuide;
}
