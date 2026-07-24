package com.airesume.server.dto.user;

import lombok.Builder;
import lombok.Data;

/**
 * 用户自定义 TTS 运行时配置。
 */
@Data
@Builder(toBuilder = true)
public class ResolvedTtsConfig {

    /** 配置来源：user_custom/system，用于控制层和前端区分展示。 */
    private String source;
    private String baseUrl;
    private String apiKey;
    private String model;
    private String voiceId;
    /** TTS 合成端点路径，如 /audio/speech 或 /v1/tts */
    private String endpointPath;
    /** TTS 提供商标识，用于运行时分发到不同协议处理器 */
    private String ttsProvider;
    private String configType;
}
