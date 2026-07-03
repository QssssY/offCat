package com.airesume.server.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增岗位配置请求
 */
@Data
public class JobRoleCreateRequest {

    /**
     * 岗位编码
     * 作用：作为后台管理用的稳定标识，避免后续只靠中文名称做唯一键。
     */
    @NotBlank(message = "岗位编码不能为空")
    private String roleCode;

    /**
     * 岗位名称
     * 作用：用户端展示和面试会话实际使用的岗位名。
     */
    @NotBlank(message = "岗位名称不能为空")
    private String roleName;

    /**
     * 面试入口页展示标签，例如“热门”。
     */
    private String interviewTag;

    /**
     * 标签样式类型，例如 hot/common。
     */
    private String tagType;

    /**
     * 排序值
     */
    @NotNull(message = "排序值不能为空")
    private Integer sort;
}
