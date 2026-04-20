package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 面试岗位配置实体
 *
 * 作用：
 * 把模拟面试可选岗位从前端硬编码迁移到后台可配置表。
 * 之后管理员通过管理端维护岗位，用户端统一从这里读取选项。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job_role")
public class SysJobRole extends BaseEntity {

    /**
     * 稳定岗位编码
     * 用于后台管理、排序和后续扩展，不直接展示给用户。
     */
    @TableField("role_code")
    private String roleCode;

    /**
     * 岗位展示名称
     * 当前用户端创建面试会话时，仍然继续传这个名字给现有业务链路，
     * 这样可以最小改动兼容现有 interview_session.job_role、Prompt 和报告逻辑。
     */
    @TableField("role_name")
    private String roleName;

    /**
     * 面试入口页展示的短标签，例如“热门”“常见”。
     */
    @TableField("interview_tag")
    private String interviewTag;

    /**
     * 标签样式类型，例如 hot/common/competitive/normal。
     * 前端用它映射不同颜色，但数据来源必须来自后端。
     */
    @TableField("tag_type")
    private String tagType;

    /**
     * 是否启用：1-启用，0-禁用。
     */
    @TableField("is_active")
    private Integer isActive;

    /**
     * 排序值，越小越靠前。
     */
    @TableField("sort")
    private Integer sort;
}
