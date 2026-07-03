package com.airesume.server.entity;

import com.airesume.server.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_admin_notification")
public class SysAdminNotification extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String title;

    private String content;

    private String type;

    private String targetType;

    private Integer status;

    private LocalDateTime publishedAt;
}
