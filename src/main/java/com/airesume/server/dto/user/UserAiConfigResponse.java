package com.airesume.server.dto.user;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 用户自定义 AI 配置响应，API Key 只返回脱敏值。
 */
@Data
@Builder
public class UserAiConfigResponse {

    private String configType;
    private String providerName;
    private String baseUrl;
    private String apiKey;
    private String model;
    private Boolean enabled;
    private Boolean supportsMultimodal;
    private LocalDateTime lastVerifiedAt;
    private String verificationStatus;
    private LocalDateTime updateTime;
}
