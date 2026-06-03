package com.airesume.server.dto.interview;

import lombok.Builder;
import lombok.Data;

/**
 * 语音面试 TTS 可用性响应。
 */
@Data
@Builder
public class TtsCapabilityResponse {

    private Boolean available;
    private String engine;
    private String configType;
}
