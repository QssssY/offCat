package com.airesume.server.service;

import com.airesume.server.dto.ai.AiModelDiscoveryResponse;

/**
 * OpenAI 兼容模型列表获取服务。
 */
public interface AiModelDiscoveryService {

    /**
     * 根据当前表单中的基础地址和明文 API Key 拉取模型列表，不保存任何配置。
     *
     * @param baseUrl OpenAI 兼容 API 基础地址
     * @param apiKey 明文 API Key
     * @param timeoutMs 超时时间，允许为空
     * @param providerType provider 类型，mock 类型不会出网
     * @return 模型列表获取结果
     */
    AiModelDiscoveryResponse fetchModels(String baseUrl, String apiKey, Integer timeoutMs, String providerType);
}
