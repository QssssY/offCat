package com.airesume.server.dto.ai;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * OpenAI 兼容模型列表获取结果。
 */
@Data
@Builder
public class AiModelDiscoveryResponse {

    /**
     * 是否成功获取模型列表。
     */
    private Boolean success;

    /**
     * 展示给用户的结果提示。
     */
    private String message;

    /**
     * 可选择的模型列表，失败时为空列表。
     */
    private List<AiModelOption> models;

    /**
     * 实际请求的模型列表端点。
     */
    private String endpoint;

    /**
     * 本次请求耗时毫秒数。
     */
    private Long latencyMs;

    /**
     * 失败原因，成功时为空。
     */
    private String errorMessage;
}
