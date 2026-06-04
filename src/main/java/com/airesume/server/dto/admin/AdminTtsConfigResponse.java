package com.airesume.server.dto.admin;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端系统级 TTS 配置响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTtsConfigResponse {

    private Long id;
    private Boolean enabled;
    private Boolean configured;
    private String ttsProvider;
    private String baseUrl;
    /** 脱敏后的 API Key，禁止返回明文。 */
    private String apiKey;
    private String model;
    private String voiceId;
    private String endpointPath;
    private LocalDateTime updateTime;
}
