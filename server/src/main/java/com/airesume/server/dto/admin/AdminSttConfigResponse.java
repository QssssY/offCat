package com.airesume.server.dto.admin;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 管理端系统 STT 配置响应，API Key 字段仅返回脱敏结果。
 */
@Data
@Builder
public class AdminSttConfigResponse {

    private Long id;
    private Boolean enabled;
    /** 配置是否完整可用（地址/模型/密钥齐全）。 */
    private Boolean configured;
    private String baseUrl;
    /** 脱敏后的 API Key，仅用于展示。 */
    private String apiKey;
    private String model;
    private String endpointPath;
    private LocalDateTime updateTime;
}
