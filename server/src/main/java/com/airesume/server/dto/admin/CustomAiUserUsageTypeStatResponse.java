package com.airesume.server.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端用户自定义 AI 明细中的用户-功能统计行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomAiUserUsageTypeStatResponse {

    private Long userId;
    private String usageType;
    private String usageTypeDesc;
    private Integer callCount;
}
