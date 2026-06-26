package com.airesume.server.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GrowthConfigCreateRequest {

    @NotBlank(message = "配置键不能为空")
    private String configKey;

    @NotBlank(message = "配置值不能为空")
    private String configValue;

    private String description;

    private String groupName;

    private Integer sort;
}
