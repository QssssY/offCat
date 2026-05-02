package com.airesume.server.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 岗位 JD 对比分析响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeJobMatchAnalyzeResponse {

    /**
     * 分析记录 ID。
     */
    private String analysisId;

    /**
     * 简历诊断任务 ID。
     */
    private String resumeTaskId;

    /**
     * 匹配度评分。
     */
    private Integer matchScore;

    /**
     * 已匹配关键词。
     */
    private List<String> matchedKeywords;

    /**
     * 缺失关键词或能力项。
     */
    private List<String> missingKeywords;

    /**
     * 优化建议。
     */
    private List<String> suggestions;

    /**
     * AI 生成的匹配情况总结。
     */
    private String analysisSummary;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;
}
