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
     * 逐轮回放式复盘，用于展示每一轮问答暴露出的具体问题与下一次改法。
     */
    @Builder.Default
    private List<RoundReview> roundReviews = new ArrayList<>();

    /**
     * 追问环节的失分点，帮助用户定位“被追问后为什么没接住”。
     */
    @Builder.Default
    private List<String> followUpLossPoints = new ArrayList<>();

    /**
     * 本次面试反复出现的失分模式，例如表达模糊、案例缺证据、技术边界说不清。
     */
    @Builder.Default
    private List<String> commonLossPatterns = new ArrayList<>();

    /**
     * 看完报告后可以马上执行的三条训练动作，必须足够具体。
     */
    @Builder.Default
    private List<String> immediateActions = new ArrayList<>();

    /**
     * 技术深度评分
     */
    private DimensionScore technicalDepth;

    /**
     * 项目表达能力评分，补足原有雷达图对项目案例讲述质量的呈现。
     */
    private DimensionScore projectExpression;

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
     * 单轮复盘详情。
     * 该结构不替代原有 questionPerformance，只负责承载 V2 报告的回放式分析。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundReview {
        /**
         * 轮次序号，从 1 开始。
         */
        private Integer roundNo;

        /**
         * 本轮面试官问题。
         */
        private String question;

        /**
         * 本轮候选人回答。
         */
        private String answer;

        /**
         * 本轮评分（0-100）。
         */
        private Integer score;

        /**
         * 回放式复盘，说明这轮答得好/差在哪里。
         */
        private String replayAnalysis;

        /**
         * 本轮追问或继续深挖时没有接住的点。
         */
        private String missedFollowUp;

        /**
         * 针对这一轮下次可直接替换使用的改进建议。
         */
        private String nextPractice;
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

        /**
         * 加分项列表
         */
        @Builder.Default
        private List<String> strengths = new ArrayList<>();

        /**
         * 扣分项列表
         */
        @Builder.Default
        private List<String> weaknesses = new ArrayList<>();
    }
}
