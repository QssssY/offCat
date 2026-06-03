package com.airesume.server.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * TTS 提供商常量定义。
 * <p>
 * 每种 Provider 有独立的 API 格式（端点路径、请求体、响应解析、认证头），
 * 本类集中定义 Provider 标识、API 格式枚举和预设参数。
 */
public final class TtsProviderConstants {

    private TtsProviderConstants() {}

    // ==================== Provider 标识 ====================

    /** OpenAI 及 OpenAI 兼容的 /audio/speech 标准 */
    public static final String PROVIDER_OPENAI = "openai";

    /** 小米 MiMo，使用 /chat/completions + messages + audio 参数 */
    public static final String PROVIDER_MIMO = "mimo";

    // ==================== API 格式枚举 ====================

    /**
     * TTS API 协议格式。
     * <p>
     * OPENAI_SPEECH: POST /audio/speech, {model, voice, input}, 二进制音频响应, Bearer 认证
     * CHAT_COMPLETIONS_TTS: POST /chat/completions, {model, messages, audio:{format,voice}}, base64 JSON 响应
     */
    @Getter
    @AllArgsConstructor
    public enum TtsApiFormat {
        /** OpenAI 标准：POST /audio/speech，二进制音频响应 */
        OPENAI_SPEECH("Authorization", "Bearer "),
        /** Chat Completions TTS：POST /chat/completions，base64 JSON 音频响应 */
        CHAT_COMPLETIONS_TTS("api-key", "");

        /** 认证头名称 */
        private final String authHeaderName;
        /** 认证头前缀（如 "Bearer "），空字符串表示无前缀 */
        private final String authHeaderPrefix;
    }

    // ==================== 预设音色 VO ====================

    @Getter
    @AllArgsConstructor
    public static class VoiceOption {
        private final String id;
        private final String name;
    }

    // ==================== Provider 预设 ====================

    @Getter
    @AllArgsConstructor
    public static class ProviderPreset {
        private final String providerId;
        private final String displayName;
        private final TtsApiFormat apiFormat;
        private final String defaultEndpointPath;
        private final String defaultBaseUrl;
        private final String defaultModel;
        private final String defaultVoiceId;
        private final List<VoiceOption> presetVoices;
    }

    // ==================== 预设实例 ====================

    /** OpenAI 预设 */
    public static final ProviderPreset OPENAI_PRESET = new ProviderPreset(
            PROVIDER_OPENAI,
            "OpenAI",
            TtsApiFormat.OPENAI_SPEECH,
            "/audio/speech",
            "https://api.openai.com/v1",
            "tts-1",
            "alloy",
            List.of(
                    new VoiceOption("alloy", "Alloy"),
                    new VoiceOption("echo", "Echo"),
                    new VoiceOption("fable", "Fable"),
                    new VoiceOption("onyx", "Onyx"),
                    new VoiceOption("nova", "Nova"),
                    new VoiceOption("shimmer", "Shimmer")
            )
    );

    /** MiMo（小米）预设 */
    public static final ProviderPreset MIMO_PRESET = new ProviderPreset(
            PROVIDER_MIMO,
            "MiMo（小米）",
            TtsApiFormat.CHAT_COMPLETIONS_TTS,
            "/chat/completions",
            "https://api.xiaomimimo.com/v1",
            "mimo-v2.5-tts",
            "mimo_default",
            List.of(
                    new VoiceOption("mimo_default", "MiMo-默认"),
                    new VoiceOption("冰糖", "冰糖"),
                    new VoiceOption("茉莉", "茉莉"),
                    new VoiceOption("苏打", "苏打"),
                    new VoiceOption("白桦", "白桦"),
                    new VoiceOption("Mia", "Mia"),
                    new VoiceOption("Chloe", "Chloe"),
                    new VoiceOption("Milo", "Milo"),
                    new VoiceOption("Dean", "Dean")
            )
    );

    /** 全部预设列表 */
    public static final List<ProviderPreset> ALL_PRESETS = List.of(OPENAI_PRESET, MIMO_PRESET);

    // ==================== 工具方法 ====================

    /**
     * 根据 providerId 查找预设，未匹配时返回 null（按 OpenAI 兜底）。
     */
    public static ProviderPreset findPreset(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return OPENAI_PRESET;
        }
        return ALL_PRESETS.stream()
                .filter(p -> p.getProviderId().equals(providerId.trim()))
                .findFirst()
                .orElse(OPENAI_PRESET);
    }

    /**
     * 根据 providerId 获取对应的 API 格式。
     */
    public static TtsApiFormat resolveApiFormat(String providerId) {
        return findPreset(providerId).getApiFormat();
    }
}
