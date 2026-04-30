package com.airesume.server.dto.resume;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 简历诊断结果 DTO
 *
 * 用于解析和构建 diagnosisResult JSON 结构
 * 包含 basicInfoDetails 字段以支持前端展示具体个人信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDiagnosisResult {

    /**
     * 总体评价
     */
    @JsonProperty("overallEvaluation")
    private OverallEvaluation overallEvaluation;

    /**
     * 亮点列表
     */
    @JsonProperty("highlights")
    private List<String> highlights;

    /**
     * 基础信息评价（原有字段，保持兼容）
     */
    @JsonProperty("basicInfoEvaluation")
    private BasicInfoEvaluation basicInfoEvaluation;

    /**
     * 基础信息详情（新增字段，用于前端展示具体值）
     */
    @JsonProperty("basicInfoDetails")
    private BasicInfoDetails basicInfoDetails;

    /**
     * 技能评价
     */
    @JsonProperty("skillEvaluation")
    private SkillEvaluation skillEvaluation;

    /**
     * 工作经验评价
     */
    @JsonProperty("workExperienceEvaluation")
    private WorkExperienceEvaluation workExperienceEvaluation;

    /**
     * 项目经验评价
     */
    @JsonProperty("projectExperienceEvaluation")
    private ProjectExperienceEvaluation projectExperienceEvaluation;

    /**
     * 教育背景评价
     */
    @JsonProperty("educationEvaluation")
    private EducationEvaluation educationEvaluation;

    /**
     * 优化建议列表
     */
    @JsonProperty("optimizationSuggestions")
    private List<String> optimizationSuggestions;

    /**
     * 总体评价内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverallEvaluation {
        @JsonProperty("totalScore")
        private Integer totalScore;

        @JsonProperty("level")
        private String level;

        @JsonProperty("summary")
        private String summary;
    }

    /**
     * 基础信息评价内部类（保持原有结构兼容）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BasicInfoEvaluation {
        @JsonProperty("score")
        private Integer score;

        @JsonProperty("hasName")
        private Boolean hasName;

        @JsonProperty("hasPhone")
        private Boolean hasPhone;

        @JsonProperty("hasEmail")
        private Boolean hasEmail;

        @JsonProperty("hasGithub")
        private Boolean hasGithub;

        @JsonProperty("hasBlog")
        private Boolean hasBlog;

        /**
         * AI 分析的加分项
         */
        @JsonProperty("strengths")
        private List<String> strengths;

        /**
         * AI 分析的扣分项
         */
        @JsonProperty("weaknesses")
        private List<String> weaknesses;

        @JsonProperty("suggestions")
        private List<String> suggestions;
    }

    /**
     * 基础信息详情内部类（新增，用于展示具体值）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BasicInfoDetails {
        /**
         * 姓名
         */
        @JsonProperty("name")
        @Builder.Default
        private String name = "";

        /**
         * 邮箱
         */
        @JsonProperty("email")
        @Builder.Default
        private String email = "";

        /**
         * 电话
         */
        @JsonProperty("phone")
        @Builder.Default
        private String phone = "";

        /**
         * 所在地
         */
        @JsonProperty("location")
        @Builder.Default
        private String location = "";

        /**
         * 当前公司
         */
        @JsonProperty("currentCompany")
        @Builder.Default
        private String currentCompany = "";

        /**
         * GitHub链接
         */
        @JsonProperty("github")
        @Builder.Default
        private String github = "";

        /**
         * 博客/网站链接
         */
        @JsonProperty("blog")
        @Builder.Default
        private String blog = "";
    }

    /**
     * 技能评价内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillEvaluation {
        @JsonProperty("score")
        private Integer score;

        @JsonProperty("skillList")
        private List<String> skillList;

        @JsonProperty("strengths")
        private List<String> strengths;

        @JsonProperty("weaknesses")
        private List<String> weaknesses;

        @JsonProperty("suggestions")
        private List<String> suggestions;
    }

    /**
     * 工作经验评价内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkExperienceEvaluation {
        @JsonProperty("score")
        private Integer score;

        @JsonProperty("totalYears")
        private Integer totalYears;

        @JsonProperty("companyCount")
        private Integer companyCount;

        @JsonProperty("hasQuantifiableResults")
        private Boolean hasQuantifiableResults;

        @JsonProperty("experiences")
        private List<Map<String, Object>> experiences;

        /**
         * AI 分析的加分项
         */
        @JsonProperty("strengths")
        private List<String> strengths;

        /**
         * AI 分析的扣分项
         */
        @JsonProperty("weaknesses")
        private List<String> weaknesses;

        @JsonProperty("suggestions")
        private List<String> suggestions;
    }

    /**
     * 项目经验评价内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectExperienceEvaluation {
        @JsonProperty("score")
        private Integer score;

        @JsonProperty("projectCount")
        private Integer projectCount;

        @JsonProperty("hasTechStack")
        private Boolean hasTechStack;

        @JsonProperty("hasResponsibilities")
        private Boolean hasResponsibilities;

        @JsonProperty("projects")
        private List<Map<String, Object>> projects;

        /**
         * AI 分析的加分项
         */
        @JsonProperty("strengths")
        private List<String> strengths;

        /**
         * AI 分析的扣分项
         */
        @JsonProperty("weaknesses")
        private List<String> weaknesses;

        @JsonProperty("suggestions")
        private List<String> suggestions;
    }

    /**
     * 教育背景评价内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EducationEvaluation {
        @JsonProperty("score")
        private Integer score;

        @JsonProperty("degree")
        private String degree;

        @JsonProperty("school")
        private String school;

        @JsonProperty("major")
        private String major;

        @JsonProperty("hasRelevantMajor")
        private Boolean hasRelevantMajor;

        /**
         * AI 分析的加分项
         */
        @JsonProperty("strengths")
        private List<String> strengths;

        /**
         * AI 分析的扣分项
         */
        @JsonProperty("weaknesses")
        private List<String> weaknesses;

        @JsonProperty("suggestions")
        private List<String> suggestions;
    }
}
