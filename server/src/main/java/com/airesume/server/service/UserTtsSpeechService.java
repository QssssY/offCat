package com.airesume.server.service;

import com.airesume.server.dto.user.ResolvedTtsConfig;
import com.airesume.server.dto.user.TtsAudioResult;

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
     * 使用解析后的 TTS 配置合成面试官播报音频，返回音频字节与真实媒体类型。
     */
    TtsAudioResult synthesizeInterviewSpeechAudio(Long userId, String text);

    /**
     * 使用可选音色覆盖合成面试播报，覆盖值仅由 EdgeTTS Provider 处理。
     */
    default TtsAudioResult synthesizeInterviewSpeechAudio(Long userId, String text, String voiceIdOverride) {
        return synthesizeInterviewSpeechAudio(userId, text);
    }

    /**
     * 兼容旧调用方：只需要音频字节时从完整结果中取出 byte[]。
     */
    default byte[] synthesizeInterviewSpeech(Long userId, String text) {
        return synthesizeInterviewSpeechAudio(userId, text).getAudioBytes();
    }
}
