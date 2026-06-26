package com.airesume.server.dto.admin;

import lombok.Builder;
import lombok.Data;

/**
 * 用户自定义 AI 每日上限响应。
 */
@Data
@Builder
public class CustomAiDailyLimitResponse {

    private Integer limit;
}
