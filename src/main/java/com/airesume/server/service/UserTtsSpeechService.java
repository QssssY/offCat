package com.airesume.server.service;

import com.airesume.server.dto.user.ResolvedTtsConfig;

/**
 * 用户自定义 TTS 运行时合成服务。
 */
public interface UserTtsSpeechService {

    /**
     * 解析语音面试可用的 TTS 配置，优先 interview，随后 default。
     */
    ResolvedTtsConfig resolveInterviewTtsConfig(Long userId);

    /**
     * 判断当前用户是否具备可用于语音面试的完整 TTS 配置。
     */
    boolean hasInterviewTtsConfig(Long userId);

    /**
     * 使用用户自定义 OpenAI 兼容 TTS 合成面试官播报音频。
     */
    byte[] synthesizeInterviewSpeech(Long userId, String text);
}
