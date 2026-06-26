package com.airesume.server.dto.admin;

import jakarta.validation.constraints.NotNull;

/**
 * 更新Prompt模板请求DTO
 */
public class PromptUpdateRequest {

    @NotNull(message = "Prompt ID不能为空")
    private Long id;

    private Integer scenarioType;
    private String jobRoleCode;
    private String jobRole;
    private Integer difficulty;
    private String promptContent;
    private Integer activeStatus;

    public Long getId() { return id; }
    public Integer getScenarioType() { return scenarioType; }
    public String getJobRoleCode() { return jobRoleCode; }
    public String getJobRole() { return jobRole; }
    public Integer getDifficulty() { return difficulty; }
    public String getPromptContent() { return promptContent; }
    public Integer getActiveStatus() { return activeStatus; }

    public void setId(Long v) { this.id = v; }
    public void setScenarioType(Integer v) { this.scenarioType = v; }
    public void setJobRoleCode(String v) { this.jobRoleCode = v; }
    public void setJobRole(String v) { this.jobRole = v; }
    public void setDifficulty(Integer v) { this.difficulty = v; }
    public void setPromptContent(String v) { this.promptContent = v; }
    public void setActiveStatus(Integer v) { this.activeStatus = v; }
}