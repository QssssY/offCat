package com.airesume.server.dto.resume;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OverallEvaluation {
        @JsonProperty("totalScore")
        private Integer totalScore;

        @JsonProperty("level")
        private String level;

        @JsonProperty("summary")
        @Builder.Default
        private String summary = "";

        @JsonProperty("strengths")
        @Builder.Default
        private List<String> strengths = List.of();

        @JsonProperty("weaknesses")
        @Builder.Default
        private List<String> weaknesses = List.of();
    }

    /**
     * 基础信息评价内部类（保持原有结构兼容）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
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

        @JsonProperty("evaluation")
        @Builder.Default
        private String evaluation = "";

        /**
         * AI 分析的加分项
         */
        @JsonProperty("strengths")
        @Builder.Default
        private List<String> strengths = List.of();

        /**
         * AI 分析的扣分项
         */
        @JsonProperty("weaknesses")
        @Builder.Default
        private List<String> weaknesses = List.of();

        @JsonProperty("suggestions")
        @Builder.Default
        private List<String> suggestions = List.of();
    }

    /**
     * 基础信息详情内部类（新增，用于展示具体值）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SkillEvaluation {
        @JsonProperty("score")
        private Integer score;

        @JsonProperty("skillList")
        @Builder.Default
        private List<String> skillList = List.of();

        @JsonProperty("evaluation")
        @Builder.Default
        private String evaluation = "";

        @JsonProperty("strengths")
        @Builder.Default
        private List<String> strengths = List.of();

        @JsonProperty("weaknesses")
        @Builder.Default
        private List<String> weaknesses = List.of();

        @JsonProperty("suggestions")
        @Builder.Default
        private List<String> suggestions = List.of();
    }

    /**
     * 工作经验评价内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
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
        @Builder.Default
        private List<Map<String, Object>> experiences = List.of();

        @JsonProperty("evaluation")
        @Builder.Default
        private String evaluation = "";

        /**
         * AI 分析的加分项
         */
        @JsonProperty("strengths")
        @Builder.Default
        private List<String> strengths = List.of();

        /**
         * AI 分析的扣分项
         */
        @JsonProperty("weaknesses")
        @Builder.Default
        private List<String> weaknesses = List.of();

        @JsonProperty("suggestions")
        @Builder.Default
        private List<String> suggestions = List.of();
    }

    /**
     * 项目经验评价内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
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
        @Builder.Default
        private List<Map<String, Object>> projects = List.of();

        @JsonProperty("evaluation")
        @Builder.Default
        private String evaluation = "";

        /**
         * AI 分析的加分项
         */
        @JsonProperty("strengths")
        @Builder.Default
        private List<String> strengths = List.of();

        /**
         * AI 分析的扣分项
         */
        @JsonProperty("weaknesses")
        @Builder.Default
        private List<String> weaknesses = List.of();

        @JsonProperty("suggestions")
        @Builder.Default
        private List<String> suggestions = List.of();
    }

    /**
     * 教育背景评价内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
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

        @JsonProperty("evaluation")
        @Builder.Default
        private String evaluation = "";

        /**
         * AI 分析的加分项
         */
        @JsonProperty("strengths")
        @Builder.Default
        private List<String> strengths = List.of();

        /**
         * AI 分析的扣分项
         */
        @JsonProperty("weaknesses")
        @Builder.Default
        private List<String> weaknesses = List.of();

        @JsonProperty("suggestions")
        @Builder.Default
        private List<String> suggestions = List.of();
    }
}
