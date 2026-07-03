package com.airesume.server.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminNotificationCreateRequest {

    @NotBlank(message = "公告标题不能为空")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    private String content;

    @NotBlank(message = "公告类型不能为空")
    @Pattern(regexp = "system|activity|update|maintenance", message = "公告类型仅支持 system/activity/update/maintenance")
    private String type;

    @NotBlank(message = "目标用户不能为空")
    @Pattern(regexp = "all|vip|normal", message = "目标用户仅支持 all/vip/normal")
    private String targetType;

    private Integer status;
}
