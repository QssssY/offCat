package com.airesume.server.dto.resume;

import lombok.Data;

/**
 * 岗位 JD 对比分析请求。
 */
@Data
public class ResumeJobMatchAnalyzeRequest {

    /**
     * 简历诊断任务 ID。
     */
    private String resumeTaskId;

    /**
     * 简历原文文本。
     */
    private String resumeText;

    /**
     * 岗位 JD 文本。
     */
    private String jdText;
}
