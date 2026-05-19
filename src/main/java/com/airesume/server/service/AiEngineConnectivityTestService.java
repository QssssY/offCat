package com.airesume.server.service;

import com.airesume.server.dto.admin.AiEngineConnectivityTestRequest;
import com.airesume.server.dto.admin.AiEngineConnectivityTestResponse;

/**
 * 管理端 AI 引擎连通测试服务。
 */
public interface AiEngineConnectivityTestService {

    /**
     * 使用当前表单配置发起一次最小 AI 调用，验证模型配置是否可用。
     *
     * @param request 表单配置
     * @param apiKey 明文 API Key
     * @return 连通测试结果
     */
    AiEngineConnectivityTestResponse testConnectivity(AiEngineConnectivityTestRequest request, String apiKey);
}
