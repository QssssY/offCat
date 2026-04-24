package com.airesume.server.dto.admin;

import jakarta.validation.constraints.NotNull;

/**
 * 创建Prompt模板请求DTO
 */
public class PromptCreateRequest {

    @NotNull(message = "场景类型不能为空")
    private Integer scenarioType;

    private String jobRoleCode;
    private String jobRole;

    @NotNull(message = "难度级别不能为空")
    private Integer difficulty;

    @jakarta.validation.constraints.NotBlank(message = "Prompt内容不能为空")
    private String promptContent;

    private Integer activeStatus;

    public Integer getScenarioType() { return scenarioType; }
    public String getJobRoleCode() { return jobRoleCode; }
    public String getJobRole() { return jobRole; }
    public Integer getDifficulty() { return difficulty; }
    public String getPromptContent() { return promptContent; }
    public Integer getActiveStatus() { return activeStatus; }

    public void setScenarioType(Integer v) { this.scenarioType = v; }
    public void setJobRoleCode(String v) { this.jobRoleCode = v; }
    public void setJobRole(String v) { this.jobRole = v; }
    public void setDifficulty(Integer v) { this.difficulty = v; }
    public void setPromptContent(String v) { this.promptContent = v; }
    public void setActiveStatus(Integer v) { this.activeStatus = v; }
}