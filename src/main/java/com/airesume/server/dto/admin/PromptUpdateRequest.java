package com.airesume.server.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新Prompt模板请求DTO
 */
@Data
public class PromptUpdateRequest {

    /**
     * Prompt模板ID
     */
    @NotNull(message = "Prompt ID不能为空")
    private Long id;

    /**
     * 场景类型：1-面试系统设定，2-简历诊断设定
     */
    private Integer scenarioType;

    /**
     * 岗位编码
     */
    private String jobRoleCode;

    /**
     * 兼容字段：岗位名称
     */
    private String jobRole;

    /**
     * 难度级别：1-初级，2-中级，3-高级
     */
    private Integer difficulty;

    /**
     * 具体的Prompt模板内容
     */
    private String promptContent;

    /**
     * 是否启用当前模板：1-启用，0-禁用
     */
    private Integer isActive;
}
