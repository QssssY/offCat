package com.airesume.server.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Prompt模板响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptResponse {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 场景类型：1-面试系统设定，2-简历诊断设定
     */
    private Integer scenarioType;

    /**
     * 场景类型描述
     */
    private String scenarioTypeDesc;

    /**
     * 岗位编码
     */
    private String jobRoleCode;

    /**
     * 岗位名称
     */
    private String jobRoleName;

    /**
     * 兼容字段：历史上对外暴露的岗位名
     */
    private String jobRole;

    /**
     * 难度级别：1-初级，2-中级，3-高级
     */
    private Integer difficulty;

    /**
     * 难度级别描述
     */
    private String difficultyDesc;

    /**
     * 具体的Prompt模板内容
     */
    private String promptContent;

    /**
     * 是否启用当前模板：1-启用，0-禁用
     */
    private Integer isActive;

    /**
     * 启用状态描述
     */
    private String isActiveDesc;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
