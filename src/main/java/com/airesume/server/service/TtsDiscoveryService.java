package com.airesume.server.service;

import com.airesume.server.dto.user.UserTtsDiscoveryResponse;

/**
 * TTS 模型/音色发现服务。
 * 模型列表通过 /models 端点获取并过滤 TTS 相关模型；
 * 音色列表依次尝试常见端点，全部失败时回落到 OpenAI 预设音色。
 */
public interface TtsDiscoveryService {

    /**
     * 发现 TTS 可用模型和音色。
     *
     * @param baseUrl TTS 服务基础地址（已校验 HTTPS）
     * @param apiKey  TTS 服务 API Key
     * @return 发现结果，包含模型列表、音色列表和发现状态
     */
    UserTtsDiscoveryResponse discover(String baseUrl, String apiKey, String provider);
}
