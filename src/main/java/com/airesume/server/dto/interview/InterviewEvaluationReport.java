package com.airesume.server.dto.interview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 面试评价报告 DTO
 * 严格按照大厂招聘标准的结构化评价结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewEvaluationReport {

    /**
     * 综合评分（0-100）
     */
    private Integer overallScore;

    /**
     * 等级：S/A/B/C/D
     */
    private String level;

    /**
     * 最终结论描述
     */
    private String finalVerdict;

    /**
     * 总体评价总结
     */
    private String summary;

    /**
     * 优势亮点列表
     */
    @Builder.Default
    private List<String> strengths = new ArrayList<>();

    /**
     * 短板/不足列表
     */
    @Builder.Default
    private List<String> weaknesses = new ArrayList<>();

    /**
     * 严重问题/风险点列表
     */
    @Builder.Default
    private List<String> criticalIssues = new ArrayList<>();

    /**
     * 每道题的表现详情
     */
    @Builder.Default
    private List<QuestionPerformance> questionPerformance = new ArrayList<>();

    /**
     * 技术深度评分
     */
    private DimensionScore technicalDepth;

    /**
     * 沟通表达评分
     */
    private DimensionScore communication;

    /**
     * 问题解决能力评分
     */
    private DimensionScore problemSolving;

    /**
     * 抗压能力评分（压力面试模式下重点评估）
     */
    private DimensionScore pressureResistance;

    /**
     * 岗位匹配度评分
     */
    private DimensionScore jobMatch;

    /**
     * 录用建议：强烈推荐/推荐/待定/不推荐
     */
    private String hireRecommendation;

    /**
     * 改进建议列表
     */
    @Builder.Default
    private List<String> improvementSuggestions = new ArrayList<>();

    /**
     * 红旗警示（严重问题）
     */
    @Builder.Default
    private List<String> redFlags = new ArrayList<>();

    /**
     * 缺失的关键能力
     */
    @Builder.Default
    private List<String> missingCompetencies = new ArrayList<>();

    /**
     * 水分风险评估（是否有背书/造假嫌疑）
     */
    private String inflationRisk;

    /**
     * 回答真实性评估
     */
    private String answerAuthenticity;

    /**
     * 面试表现标签
     */
    @Builder.Default
    private List<String> interviewPerformanceTags = new ArrayList<>();

    /**
     * 通过概率（0-100）
     */
    private Integer passProbability;

    /**
     * 拒录理由（如果不推荐）
     */
    @Builder.Default
    private List<String> rejectionReasons = new ArrayList<>();

    /**
     * 兼容旧版前端的维度评分（保留）
     */
    private Object dimensions;

    /**
     * 兼容旧版前端的改进建议（保留）
     */
    @Builder.Default
    private List<String> suggestions = new ArrayList<>();

    /**
     * 兼容旧版前端的待提升方向（保留）
     */
    @Builder.Default
    private List<String> improvements = new ArrayList<>();

    /**
     * 单题表现详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionPerformance {
        /**
         * 问题内容
         */
        private String question;

        /**
         * 候选人回答
         */
        private String answer;

        /**
         * 本题评分（0-100）
         */
        private Integer score;

        /**
         * 评价
         */
        private String comment;

        /**
         * 知识点标签
         */
        @Builder.Default
        private List<String> knowledgeTags = new ArrayList<>();
    }

    /**
     * 维度评分
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionScore {
        /**
         * 分数（0-100）
         */
        private Integer score;

        /**
         * 评价说明
         */
        private String comment;
    }
}
