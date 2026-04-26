package com.airesume.server.dto.resume;

import lombok.Data;

/**
 * AI 简历润色请求。
 */
@Data
public class ResumePolishAnalyzeRequest {

    /**
     * 简历诊断任务 ID。
     */
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
