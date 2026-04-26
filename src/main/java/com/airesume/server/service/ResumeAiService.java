package com.airesume.server.service;

import com.airesume.server.dto.resume.ResumeJobMatchAnalyzeResponse;
import com.airesume.server.dto.resume.ResumePolishAiResult;

public interface ResumeAiService {

    String diagnose(String resumeText);

    /**
     * 基于简历原文和可选 JD 上下文生成润色结果。
     */
    ResumePolishAiResult polishResume(String resumeText, String jdText, ResumeJobMatchAnalyzeResponse latestJobMatchAnalysis);
}
