package com.airesume.server.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端岗位配置响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRoleResponse {

    /**
     * 主键 ID
     */
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
     * 面试入口展示标签
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
     * 启用状态描述
     */
    private String isActiveDesc;

    /**
     * 排序值
     */
    private Integer sort;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
