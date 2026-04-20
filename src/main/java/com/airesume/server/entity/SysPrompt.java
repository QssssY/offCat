package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI提示词模板实体类
 * 对应数据库表 sys_prompt
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_prompt")
public class SysPrompt extends BaseEntity {

    /**
     * 场景类型：1-面试系统设定，2-简历诊断设定
     */
    @TableField("scenario_type")
    private Integer scenarioType;

    /**
     * 适用岗位编码
     *
     * 作用：
     * 让 Prompt 和岗位配置表形成稳定关联，避免继续依赖自由输入的岗位字符串。
     * 当前采用兼容升级方案，因此仍然保留 jobRole 作为岗位名称快照。
     */
    @TableField("job_role_code")
    private String jobRoleCode;

    /**
     * 适用岗位名称快照
     *
     * 作用：
     * 保留原有文本字段，兼容旧数据和既有查询展示。
     * 新数据会由后端根据 job_role_code 自动回填，不允许前端自由输入直接落库。
     */
    @TableField("job_role")
    private String jobRole;

    /**
     * 难度级别：1-初级，2-中级，3-高级
     */
    @TableField("difficulty")
    private Integer difficulty;

    /**
     * 具体的Prompt模板内容
     */
    @TableField("prompt_content")
    private String promptContent;

    /**
     * 是否启用当前模板：1-启用，0-禁用
     */
    @TableField("is_active")
    private Integer isActive;
}
