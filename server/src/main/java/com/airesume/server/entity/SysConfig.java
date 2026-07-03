package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 轻量系统配置表，用于保存运营可调整的 key-value 配置。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    /** 配置键。 */
    @TableField("config_key")
    private String configKey;

    /** 配置值。 */
    @TableField("config_value")
    private String configValue;

    /** 配置说明。 */
    @TableField("description")
    private String description;
}
