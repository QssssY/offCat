package com.airesume.server.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建Prompt模板请求DTO
 */
@Data
public class PromptCreateRequest {

    /**
     * 场景类型：1-面试系统设定，2-简历诊断设定
     */
    @NotNull(message = "场景类型不能为空")
    private Integer scenarioType;

    /**
     * 适用岗位
     */
    @NotBlank(message = "适用岗位不能为空")
    private String jobRole;

    /**
     * 难度级别：1-初级，2-中级，3-高级
     */
    @NotNull(message = "难度级别不能为空")
    private Integer difficulty;

    /**
     * 具体的Prompt模板内容
     */
    @NotBlank(message = "Prompt内容不能为空")
    private String promptContent;
}
