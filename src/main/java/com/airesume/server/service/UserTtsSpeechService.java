package com.airesume.server.service;

import com.airesume.server.dto.user.ResolvedTtsConfig;

/**
 * 语音面试 TTS 运行时合成服务。
 */
public interface UserTtsSpeechService {

    /**
     * 解析语音面试可用的 TTS 配置，优先用户 interview/default，随后系统级配置。
     */
    ResolvedTtsConfig resolveInterviewTtsConfig(Long userId);

    /**
     * 判断当前用户是否具备可用于语音面试的完整 TTS 配置。
     */
    boolean hasInterviewTtsConfig(Long userId);

    /**
     * 判断当前是否存在可用系统级 TTS 配置。
     */
    boolean hasSystemTtsConfig();

    /**
     * 使用解析后的 TTS 配置合成面试官播报音频。
     */
    byte[] synthesizeInterviewSpeech(Long userId, String text);
}
