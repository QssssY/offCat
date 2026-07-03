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
    /** 系统级 TTS 是否可用；前端可据此提示未配置个人 TTS 的用户仍可使用云端播报。 */
    private Boolean systemTtsAvailable;
}
