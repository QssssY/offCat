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
     * 适用岗位
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
