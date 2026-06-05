package com.airesume.server.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * TTS 音频合成结果。
 * <p>
 * 不同 Provider 返回的音频封装格式不同：常规 TTS 返回 mp3，Gemini PCM 需要转为 wav。
 * 该对象把音频字节和 HTTP Content-Type 绑定，避免控制器固定写死 audio/mpeg。
 */
@Getter
@AllArgsConstructor(staticName = "of")
public class TtsAudioResult {

    /** 可直接返回给前端播放的音频字节。 */
    private final byte[] audioBytes;

    /** HTTP Content-Type，例如 audio/mpeg 或 audio/wav。 */
    private final String contentType;
}
