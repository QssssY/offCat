package com.airesume.server.dto.interview;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 岗位定向模拟上下文。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewJobTargetContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前会话是否真正启用了岗位定向能力。
     */
    @Builder.Default
    private Boolean jobTargeted = false;

    /**
     * 上下文来源类型。
     */
    private String sourceType;

    /**
     * 关联的简历诊断任务 ID。
     */
    private String resumeTaskId;

    /**
     * 本次会话实际使用的 JD 文本。
     */
    private String jdText;

    /**
     * 复用的岗位 JD 对比分析记录 ID。
     */
    private String jobMatchRecordId;

    /**
     * 已匹配关键词。
     */
    @Builder.Default
    private List<String> matchedKeywords = new ArrayList<>();

    /**
     * 缺失关键词或能力项。
     */
    @Builder.Default
    private List<String> missingKeywords = new ArrayList<>();

    /**
     * 复用的岗位优化建议。
     */
    @Builder.Default
    private List<String> suggestions = new ArrayList<>();

    /**
     * 面试完成后的岗位定向反馈。
     */
    private InterviewJobTargetedFeedback jobTargetedFeedback;

    /**
     * 简历文本仅供后端 AI 上下文使用，不回传给前端。
     */
    @JsonIgnore
    private String resumeText;
}
