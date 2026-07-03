package com.airesume.server.dto.user;

import lombok.Builder;
import lombok.Data;

/**
 * 用户自定义 AI 连通测试响应。
 */
@Data
@Builder
public class UserAiConnectivityTestResponse {

    private Boolean success;
    private String message;
    private String errorType;
    private Long latencyMs;
    private String responsePreview;
}
