package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_growth_config")
public class SysGrowthConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String configKey;

    private String configValue;

    private String description;

    private String groupName;

    private Integer sort;
}
