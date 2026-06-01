package com.airesume.server.dto.resume;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 岗位 JD 对比分析请求。
 */
@Data
public class ResumeJobMatchAnalyzeRequest {

    /**
     * 简历诊断任务 ID。
     */
    @NotBlank(message = "简历诊断任务ID不能为空")
    private String resumeTaskId;

    /**
     * 简历原文文本。
     */
    private String resumeText;

    /**
     * 岗位 JD 文本。
     */
    private String jdText;

    /**
     * 是否显式回退到平台 AI。默认 false，只有用户点击回退按钮时才消耗平台额度。
     */
    private Boolean fallbackToPlatform;
}
