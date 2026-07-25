package com.airesume.server.dto.user;

import lombok.Builder;
import lombok.Data;

/**
 * 系统级 STT 运行时配置（已解密），供语音面试云端识别调用。
 */
@Data
@Builder
public class ResolvedSttConfig {

    /** 语音识别服务基础地址（OpenAI 兼容）。 */
    private String baseUrl;

    /** 已解密的 API Key。 */
    private String apiKey;

    /** 语音识别模型标识。 */
    private String model;

    /** 语音转文字端点路径，如 /audio/transcriptions。 */
    private String endpointPath;
}
