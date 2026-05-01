package com.airesume.server.dto.growth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 个人成长中心概览响应。
 * 聚合用户在简历诊断、岗位匹配、AI润色、模拟面试等维度的成长数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthOverviewResponse {

    /** 成长概览摘要 */
    private SummaryVO summary;

    /** 简历诊断分数趋势 */
    private List<ScoreTrendItem> resumeScoreTrend;

    /** 模拟面试评分趋势 */
    private List<ScoreTrendItem> interviewScoreTrend;

    /** 最近一次岗位JD匹配结果 */
    private LatestJobMatchVO latestJobMatch;

    /** 最近一次AI简历润色记录 */
    private LatestPolishVO latestPolish;

    /** 最近一次模拟面试反馈 */
    private LatestInterviewFeedbackVO latestInterviewFeedback;

    /** 当前主要短板与建议 */
    private WeaknessSummaryVO weaknessSummary;

    /**
     * 成长概览摘要VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryVO {
        /** 最近一次简历诊断分数 */
        private Integer latestResumeScore;
        /** 最近一次面试评分 */
        private Integer latestInterviewScore;
        /** 最近一次JD匹配分数 */
        private Integer latestJobMatchScore;
        /** 累计简历诊断次数 */
        private Integer resumeDiagnosisCount;
        /** 累计模拟面试次数 */
        private Integer mockInterviewCount;
        /** 累计JD匹配次数 */
        private Integer jobMatchCount;
        /** 累计AI润色次数 */
        private Integer polishCount;
    }

    /**
     * 趋势图数据点VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreTrendItem {
        /** 日期标签，格式 MM/dd */
        private String date;
        /** 分数 0-100 */
        private Integer score;
    }

    /**
     * 最近JD匹配结果VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LatestJobMatchVO {
        /** 匹配分数 */
        private Integer matchScore;
        /** 已匹配关键词 */
        private List<String> matchedKeywords;
        /** 缺失关键词 */
        private List<String> missingKeywords;
        /** 优化建议 */
        private List<String> suggestions;
        /** 创建时间 */
        private String createTime;
    }

    /**
     * 最近AI润色记录VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LatestPolishVO {
        /** 来源类型 */
        private String sourceType;
        /** 修改说明摘要 */
        private List<String> modificationNotes;
        /** 创建时间 */
        private String createTime;
    }

    /**
     * 最近模拟面试反馈VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LatestInterviewFeedbackVO {
        /** 面试岗位 */
        private String jobRole;
        /** 面试模式 */
        private String interviewMode;
        /** 综合评分 */
        private Integer comprehensiveScore;
        /** 评价报告摘要 */
        private String evaluationReport;
        /** 岗位定向反馈摘要 */
        private String jobTargetedFeedback;
        /** 创建时间 */
        private String createTime;
    }

    /**
     * 短板与建议VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeaknessSummaryVO {
        /** 简历侧短板 */
        private List<String> resumeWeaknesses;
        /** 岗位匹配侧短板 */
        private List<String> jobMatchWeaknesses;
        /** 面试表现侧短板 */
        private List<String> interviewWeaknesses;
        /** 改进建议 */
        private List<String> suggestions;
    }
}
