package com.airesume.server.dto.interview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 岗位定向模拟面试反馈。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewJobTargetedFeedback {

    /**
     * 岗位匹配表现总结。
     */
    private String jobMatchPerformance;

    /**
     * 回答中体现出的岗位相关优势。
     */
    @Builder.Default
    private List<String> strengths = new ArrayList<>();

    /**
     * 回答中暴露出的岗位相关短板。
     */
    @Builder.Default
    private List<String> weaknesses = new ArrayList<>();

    /**
     * 针对目标岗位的改进建议。
     */
    @Builder.Default
    private List<String> improvementSuggestions = new ArrayList<>();
}
