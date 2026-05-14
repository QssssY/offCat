package com.airesume.server.dto.resume;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 简历润色请求。
 */
@Data
public class ResumePolishAnalyzeRequest {

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
     * 可选的岗位 JD 文本。
     */
    private String jdText;
}
