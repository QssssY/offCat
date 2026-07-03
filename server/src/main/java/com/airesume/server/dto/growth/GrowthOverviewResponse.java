package com.airesume.server.dto.growth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 个人成长中心概览响应。
 * 聚合用户在简历诊断、岗位匹配、AI润色、模拟面试等维度的成长数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthOverviewResponse implements Serializable {

    private static final long serialVersionUID = 1L;

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

    /** 管理端维护的成长中心运营配置 */
    private GrowthConfigVO growthConfig;

    /**
     * 成长概览摘要VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryVO implements Serializable {
        private static final long serialVersionUID = 1L;
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
    public static class ScoreTrendItem implements Serializable {
        private static final long serialVersionUID = 1L;
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
    public static class LatestJobMatchVO implements Serializable {
        private static final long serialVersionUID = 1L;
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
    public static class LatestPolishVO implements Serializable {
        private static final long serialVersionUID = 1L;
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
    public static class LatestInterviewFeedbackVO implements Serializable {
        private static final long serialVersionUID = 1L;
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
    public static class WeaknessSummaryVO implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 简历侧短板 */
        private List<String> resumeWeaknesses;
        /** 岗位匹配侧短板 */
        private List<String> jobMatchWeaknesses;
        /** 面试表现侧短板 */
        private List<String> interviewWeaknesses;
        /** 改进建议 */
        private List<String> suggestions;
    }

    /**
     * 成长中心运营配置VO。
     * 由管理端成长配置中心维护，用于驱动用户端激励文案和里程碑展示。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrowthConfigVO implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 激励文案列表 */
        private List<String> encouragementMessages;
        /** 里程碑配置列表 */
        private List<MilestoneConfigVO> milestones;
    }

    /**
     * 里程碑配置VO。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MilestoneConfigVO implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 配置键，作为前端渲染稳定 key */
        private String configKey;
        /** 里程碑标题，来自配置值 */
        private String title;
        /** 里程碑说明，来自配置说明 */
        private String description;
        /** 排序值 */
        private Integer sort;
    }
}
