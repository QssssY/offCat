package com.airesume.server.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VersionLogUpdateRequest {

    @NotNull(message = "ID不能为空")
    private Long id;

    private String version;
    private String title;
    private String content;
    private String type;
    private Integer status;
}
