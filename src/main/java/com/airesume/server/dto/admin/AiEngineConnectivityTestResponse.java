package com.airesume.server.dto.admin;

import lombok.Builder;
import lombok.Data;

/**
 * AI 引擎连通测试结果。
 */
@Data
@Builder
public class AiEngineConnectivityTestResponse {

    /**
     * 是否连通成功。
     */
    private Boolean success;

    /**
     * 管理端展示消息。
     */
    private String message;

    /**
     * 实际测试的 provider。
     */
    private String providerType;

    /**
     * 实际测试的模型名。
     */
    private String modelName;

    /**
     * 实际测试的接口地址。
     */
    private String endpoint;

    /**
     * 本次测试耗时毫秒。
     */
    private Long latencyMs;

    /**
     * 上游返回内容摘要，失败时为空。
     */
    private String responsePreview;

    /**
     * 失败原因，成功时为空。
     */
    private String errorMessage;
}
