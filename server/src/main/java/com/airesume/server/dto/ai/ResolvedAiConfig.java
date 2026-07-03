package com.airesume.server.dto.ai;

import lombok.Builder;
import lombok.Data;

/**
 * 用户自定义 AI 解析结果。
 */
@Data
@Builder
public class ResolvedAiConfig {

    private String provider;
    private String baseUrl;
    private String apiKey;
    private String model;
    private boolean supportsMultimodal;
    private String source;
    private String configType;
}
