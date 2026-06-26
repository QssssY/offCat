package com.airesume.server.service;

import java.time.Duration;

/**
 * Edge Read Aloud 在线 TTS 客户端。
 * <p>
 * 该接口只暴露“文本 + 音色 -> MP3 字节”的最小能力，业务层不直接感知 Edge WebSocket 协议细节。
 */
public interface EdgeTtsClient {

    /**
     * 使用 Edge Read Aloud 在线音色合成 MP3。
     *
     * @param text 待播报文本
     * @param voiceId Edge 音色 ID，例如 zh-CN-XiaoxiaoNeural
     * @param timeout 单次合成超时时间
     * @return MP3 音频字节
     */
    byte[] synthesize(String text, String voiceId, Duration timeout);
}
