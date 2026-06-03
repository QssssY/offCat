package com.airesume.server.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 自定义 AI 按功能口径统计响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomAiUsageTypeStatResponse {

    private String usageType;
    private String usageTypeDesc;
    private Integer callCount;
}
