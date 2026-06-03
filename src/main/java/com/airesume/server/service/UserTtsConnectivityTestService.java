package com.airesume.server.service;

import com.airesume.server.dto.user.UserTtsConnectivityTestRequest;
import com.airesume.server.dto.user.UserTtsConnectivityTestResponse;

/**
 * 用户自定义 TTS 连通测试服务。
 */
public interface UserTtsConnectivityTestService {

    /**
     * 使用 OpenAI 兼容 /audio/speech 发起最小语音合成请求。
     */
    UserTtsConnectivityTestResponse testConnectivity(UserTtsConnectivityTestRequest request);

    /**
     * 使用当前表单参数合成最短试听音频，返回原始音频字节。
     */
    byte[] previewVoice(UserTtsConnectivityTestRequest request);
}
