package com.airesume.server.dto.growth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 面试维度雷达响应 DTO。
 * 包含最新雷达数据、维度趋势和盲区提示。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRadarResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 最新一次面试的维度雷达数据 */
    private RadarDataVO latestRadar;

    /** 各维度趋势（最近多次面试） */
    private List<DimensionTrendVO> dimensionTrends;

    /** 盲区提示列表 */
    private List<BlindSpotTipVO> blindSpotTips;

    /** 有评估报告的候选面试次数 */
    private Integer sessionCount;

    /**
     * 雷达图数据：最新一次面试的 6 维度评分。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RadarDataVO implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 维度标识 -> 评分详情 */
        private Map<String, DimensionScoreVO> dimensions;

        /** 来源面试会话ID */
        private String sessionId;

        /** 面试时间 */
        private String createTime;
    }

    /**
     * 单维度评分详情。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionScoreVO implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 分数 0-100 */
        private Integer score;

        /** 评价说明 */
        private String comment;

        /** 加分项 */
        private List<String> strengths;

        /** 扣分项 */
        private List<String> weaknesses;
    }

    /**
     * 单维度趋势：包含该维度在多次面试中的得分变化。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionTrendVO implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 维度标识 */
        private String dimensionKey;

        /** 维度中文名 */
        private String dimensionLabel;

        /** 各次面试的分数点 */
        private List<ScorePoint> points;
    }

    /**
     * 趋势中的单个分数点。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScorePoint implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 日期（MM/dd 格式） */
        private String date;

        /** 分数 */
        private Integer score;
    }

    /**
     * 盲区提示：标记持续低分或下降趋势的维度。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlindSpotTipVO implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 维度标识 */
        private String dimensionKey;

        /** 维度中文名 */
        private String dimensionLabel;

        /** 盲区类型：persistent_low / declining_trend */
        private String type;

        /** 提示文案 */
        private String tip;

        /** 改进建议 */
        private List<String> suggestions;

        /** 最近平均分 */
        private Double averageScore;
    }
}
