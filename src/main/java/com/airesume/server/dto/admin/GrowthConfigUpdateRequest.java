package com.airesume.server.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GrowthConfigUpdateRequest {

    @NotNull(message = "ID不能为空")
    private Long id;

    private String configKey;
    private String configValue;
    private String description;
    private String groupName;
    private Integer sort;
}
