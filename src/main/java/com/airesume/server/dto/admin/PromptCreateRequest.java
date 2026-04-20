package com.airesume.server.dto.admin;

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
     * 岗位编码
     *
     * 作用：
     * 管理端创建 Prompt 时应优先传岗位编码，岗位选项必须来源于 sys_job_role。
     * 这样后端可以稳定关联岗位配置，不再依赖自由输入字符串。
     */
    private String jobRoleCode;

    /**
     * 兼容字段：岗位名称
     *
     * 作用：
     * 保留旧字段作为兼容升级方案，避免老请求直接失败。
     * 但后端不会直接信任这个字符串，而是会回查 sys_job_role 做合法性校验。
     */
    private String jobRole;

    /**
     * 难度级别：1-初级，2-中级，3-高级
     */
    @NotNull(message = "难度级别不能为空")
    private Integer difficulty;

    /**
     * 具体的Prompt模板内容
     */
    @jakarta.validation.constraints.NotBlank(message = "Prompt内容不能为空")
    private String promptContent;
}
