package com.airesume.server.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GrowthConfigResponse {

    private Long id;
    private String configKey;
    private String configValue;
    private String description;
    private String groupName;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
