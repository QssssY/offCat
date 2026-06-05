package com.airesume.server.service;

import com.airesume.server.dto.user.UserTtsConnectivityTestRequest;
import com.airesume.server.dto.user.UserTtsConnectivityTestResponse;
import com.airesume.server.dto.user.TtsAudioResult;

/**
 * 用户自定义 TTS 连通测试服务。
 */
public interface UserTtsConnectivityTestService {

    /**
     * 使用 OpenAI 兼容 /audio/speech 发起最小语音合成请求。
     */
    UserTtsConnectivityTestResponse testConnectivity(UserTtsConnectivityTestRequest request);

    /**
     * 使用当前表单参数合成最短试听音频，返回原始音频字节与媒体类型。
     */
    TtsAudioResult previewVoiceAudio(UserTtsConnectivityTestRequest request);

    /**
     * 兼容旧调用方：只需要音频字节时从完整结果中取出 byte[]。
     */
    default byte[] previewVoice(UserTtsConnectivityTestRequest request) {
        return previewVoiceAudio(request).getAudioBytes();
    }
}
