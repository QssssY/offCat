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
     * 对简历文本执行诊断，支持用户自定义 AI 配置与显式平台回退。
     */
    default String diagnose(String resumeText, Long userId, boolean fallbackToPlatform, boolean requireUserCustom) {
        return diagnose(resumeText);
    }

    /**
     * 判断当前启用的简历引擎是否支持多模态图片识别。
     *
     * @return true 表示允许使用多模态转文本
     */
    boolean supportsVisionExtraction();

    /**
     * 判断指定用户当前简历配置是否支持多模态。
     */
    default boolean supportsVisionExtraction(Long userId, boolean fallbackToPlatform) {
        return supportsVisionExtraction();
    }

    /**
     * 判断指定用户当前简历配置是否支持多模态，并可要求必须命中用户自定义配置。
     */
    default boolean supportsVisionExtraction(Long userId, boolean fallbackToPlatform, boolean requireUserCustom) {
        return supportsVisionExtraction(userId, fallbackToPlatform);
    }

    /**
     * 使用多模态模型将图片内容转换为文本。
     *
     * @param imageDataUrl 图片 Data URL
     * @param pageHint 页码等辅助提示
     * @return 识别得到的文本
     */
    String extractTextFromImage(String imageDataUrl, String pageHint);

    /**
     * 使用指定用户 AI 配置执行图片识别。
     */
    default String extractTextFromImage(String imageDataUrl, String pageHint, Long userId, boolean fallbackToPlatform) {
        return extractTextFromImage(imageDataUrl, pageHint);
    }

    /**
     * 使用指定用户 AI 配置执行图片识别，并可要求必须命中用户自定义配置。
     */
    default String extractTextFromImage(String imageDataUrl, String pageHint, Long userId,
                                        boolean fallbackToPlatform, boolean requireUserCustom) {
        return extractTextFromImage(imageDataUrl, pageHint, userId, fallbackToPlatform);
    }

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
     * 基于指定用户 AI 配置生成润色结果。
     */
    default ResumePolishAiResult polishResume(String resumeText, String jdText,
                                              ResumeJobMatchAnalyzeResponse latestJobMatchAnalysis,
                                              Long userId,
                                              boolean fallbackToPlatform) {
        return polishResume(resumeText, jdText, latestJobMatchAnalysis);
    }

    /**
     * 分析简历与 JD 的匹配程度。
     *
     * @param resumeText 简历文本
     * @param jdText JD 文本
     * @return 匹配分析 JSON
     */
    String diagnoseJobMatch(String resumeText, String jdText);

    /**
     * 基于指定用户 AI 配置执行 JD 匹配分析。
     */
    default String diagnoseJobMatch(String resumeText, String jdText, Long userId, boolean fallbackToPlatform) {
        return diagnoseJobMatch(resumeText, jdText);
    }
}
