package com.airesume.server.service;

import com.airesume.server.dto.resume.ResumeJobMatchAnalyzeResponse;
import com.airesume.server.dto.resume.ResumePolishAiResult;

/**
 * 简历相关 AI 服务接口。
 * 同时承载原有纯文本诊断能力和新增的多模态转文本能力。
 */
public interface ResumeAiService {

    /**
     * 对简历文本执行诊断。
     *
     * @param resumeText 简历纯文本
     * @return 诊断结果 JSON
     */
    String diagnose(String resumeText);

    /**
     * 判断当前启用的简历引擎是否支持多模态图片识别。
     *
     * @return true 表示允许使用多模态转文本
     */
    boolean supportsVisionExtraction();

    /**
     * 使用多模态模型将图片内容转换为文本。
     *
     * @param imageDataUrl 图片 Data URL
     * @param pageHint 页码等辅助提示
     * @return 识别得到的文本
     */
    String extractTextFromImage(String imageDataUrl, String pageHint);

    /**
     * 基于简历原文和可选 JD 上下文生成润色结果。
     *
     * @param resumeText 简历文本
     * @param jdText JD 文本
     * @param latestJobMatchAnalysis 最近一次 JD 匹配结果
     * @return 润色结果
     */
    ResumePolishAiResult polishResume(String resumeText, String jdText, ResumeJobMatchAnalyzeResponse latestJobMatchAnalysis);

    /**
     * 分析简历与 JD 的匹配程度。
     *
     * @param resumeText 简历文本
     * @param jdText JD 文本
     * @return 匹配分析 JSON
     */
    String diagnoseJobMatch(String resumeText, String jdText);
}
