package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_version_log")
public class SysVersionLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String version;

    private String title;

    private String content;

    private String type;

    private Integer status;

    private LocalDateTime publishedAt;
}
