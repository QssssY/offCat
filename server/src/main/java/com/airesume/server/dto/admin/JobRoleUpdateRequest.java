package com.airesume.server.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改岗位配置请求
 */
@Data
public class JobRoleUpdateRequest {

    /**
     * 主键 ID
     */
    @NotNull(message = "岗位ID不能为空")
    private Long id;

    /**
     * 岗位编码
     */
    private String roleCode;

    /**
     * 岗位名称
     */
    private String roleName;

    /**
     * 面试入口页展示标签
     */
    private String interviewTag;

    /**
     * 标签样式类型
     */
    private String tagType;

    /**
     * 是否启用：1-启用，0-禁用
     */
    private Integer isActive;

    /**
     * 排序值
     */
    private Integer sort;
}
