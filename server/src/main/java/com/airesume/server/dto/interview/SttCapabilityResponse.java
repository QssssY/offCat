package com.airesume.server.dto.interview;

import lombok.Builder;
import lombok.Data;

/**
 * 语音面试云端语音识别（STT）可用性响应。
 * <p>
 * 前端据此决定浏览器识别失败时是否启用云端识别兜底：不可用时保持现状（降级手动输入）。
 */
@Data
@Builder
public class SttCapabilityResponse {

    /** 云端语音识别是否可用（需后端启用且已配置 API Key，且为语音面试会话）。 */
    private Boolean available;

    /** 识别引擎标识，可用时为 "cloud"，不可用时为 "none"。 */
    private String engine;
}
