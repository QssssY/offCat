package com.airesume.server.dto.user;

import lombok.Builder;
import lombok.Data;

/**
 * 用户自定义 TTS 连通测试响应。
 */
@Data
@Builder
public class UserTtsConnectivityTestResponse {

    /** 是否连通成功。 */
    private Boolean success;

    /** 前端展示消息。 */
    private String message;

    /** 固定测试路径，避免向前端暴露完整密钥或请求体。 */
    private String endpointPath;

    /** 请求耗时。 */
    private Long latencyMs;

    /** 失败分类。 */
    private String errorType;
}
