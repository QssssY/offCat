package com.airesume.server.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 简历诊断任务响应 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDiagnosisTaskResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务 ID。
     */
    private String taskId;

    /**
     * 用户 ID。
     */
    private Long userId;

    /**
     * PDF 简历存储地址。
     */
    private String fileUrl;

    /**
     * 任务状态。
     */
    private Integer status;

    /**
     * 任务状态描述。
     */
    private String statusDesc;

    /**
     * AI 诊断结果。
     */
    private String diagnosisResult;

    /**
     * 失败原因。
     */
    private String errorMsg;

    /**
     * 简历原文文本。
     */
    private String resumeText;

    /**
     * 解析模式：TEXT / MULTIMODAL / OCR / MIXED。
     */
    private String parseMode;

    /**
     * 解析来源提示信息，用于结果页展示。
     */
    private String parseMessage;

    /**
     * 最近一次岗位 JD 对比分析结果。
     */
    private ResumeJobMatchAnalyzeResponse latestJobMatchAnalysis;

    /**
     * 最近一次 AI 简历润色结果。
     */
    private ResumePolishAnalyzeResponse latestPolishResult;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;
}
