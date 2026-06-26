package com.airesume.server.dto.user;

import lombok.Builder;
import lombok.Data;

/**
 * 用户自定义 AI 今日用量响应。
 */
@Data
@Builder
public class UserAiUsageResponse {

    private Integer used;
    private Integer limit;
    private Integer remaining;
}
