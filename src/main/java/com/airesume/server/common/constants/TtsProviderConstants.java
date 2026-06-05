package com.airesume.server.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Locale;

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

    /** Microsoft Edge Read Aloud 在线音色。 */
    public static final String PROVIDER_EDGE = "edge";

    /** Gemini 原生语音生成，返回 PCM，需要后端封装 WAV。 */
    public static final String PROVIDER_GEMINI = "gemini";

    /** MiniMax T2A HTTP，返回十六进制 MP3 音频。 */
    public static final String PROVIDER_MINIMAX = "minimax";

    /** 通义千问 Qwen TTS，返回阿里云临时音频 URL。 */
    public static final String PROVIDER_QWEN = "qwen";

    /** xAI TTS，直接返回音频字节。 */
    public static final String PROVIDER_XAI = "xai";

    // ==================== API 格式枚举 ====================

    /**
     * TTS API 协议格式。
     * <p>
     * OPENAI_SPEECH: POST /audio/speech, {model, voice, input}, 二进制音频响应, Bearer 认证
     * CHAT_COMPLETIONS_TTS: POST /chat/completions, {model, messages, audio:{format,voice}}, base64 JSON 响应
     * GEMINI_SPEECH: POST /models/{model}:generateContent，base64 PCM 响应
     * MINIMAX_T2A: POST /v1/t2a_v2，十六进制 MP3 响应
     * QWEN_TTS: POST DashScope multimodal-generation，音频 URL 响应
     * XAI_TTS: POST /v1/tts，二进制音频响应
     * EDGE_READALOUD: Edge Read Aloud WebSocket 协议，无需用户 API Key
     */
    @Getter
    @AllArgsConstructor
    public enum TtsApiFormat {
        /** OpenAI 标准：POST /audio/speech，二进制音频响应 */
        OPENAI_SPEECH("Authorization", "Bearer "),
        /** Chat Completions TTS：POST /chat/completions，base64 JSON 音频响应 */
        CHAT_COMPLETIONS_TTS("api-key", ""),
        /** Gemini 语音生成：通过 x-goog-api-key 认证，响应为 base64 PCM */
        GEMINI_SPEECH("x-goog-api-key", ""),
        /** MiniMax T2A：Bearer 认证，响应为十六进制 MP3 */
        MINIMAX_T2A("Authorization", "Bearer "),
        /** Qwen TTS：Bearer 认证，响应为阿里云音频 URL */
        QWEN_TTS("Authorization", "Bearer "),
        /** xAI TTS：Bearer 认证，响应为二进制音频 */
        XAI_TTS("Authorization", "Bearer "),
        /** Edge Read Aloud：WebSocket 在线朗读协议，不需要认证头 */
        EDGE_READALOUD("", "");

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
        private final boolean apiKeyRequired;
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
            true,
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
            true,
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

    /** Edge Read Aloud 预设 */
    public static final ProviderPreset EDGE_PRESET = new ProviderPreset(
            PROVIDER_EDGE,
            "EdgeTTS",
            TtsApiFormat.EDGE_READALOUD,
            "/consumer/speech/synthesize/readaloud/edge/v1",
            "https://speech.platform.bing.com",
            "edge-tts",
            "zh-CN-XiaoxiaoNeural",
            false,
            List.of(
                    new VoiceOption("zh-CN-XiaoxiaoNeural", "晓晓（女声，普通话）"),
                    new VoiceOption("zh-CN-XiaoyiNeural", "晓伊（女声，普通话）"),
                    new VoiceOption("zh-CN-YunjianNeural", "云健（男声，普通话）"),
                    new VoiceOption("zh-CN-YunxiNeural", "云希（男声，普通话）"),
                    new VoiceOption("zh-CN-YunxiaNeural", "云夏（男声，普通话）"),
                    new VoiceOption("zh-CN-YunyangNeural", "云扬（男声，普通话）"),
                    new VoiceOption("zh-CN-liaoning-XiaobeiNeural", "辽宁小北（女声，东北话）"),
                    new VoiceOption("zh-CN-shaanxi-XiaoniNeural", "陕西小妮（女声，陕西话）"),
                    new VoiceOption("zh-HK-HiuGaaiNeural", "晓佳（女声，粤语）"),
                    new VoiceOption("zh-HK-HiuMaanNeural", "晓曼（女声，粤语）"),
                    new VoiceOption("zh-HK-WanLungNeural", "云龙（男声，粤语）"),
                    new VoiceOption("zh-TW-HsiaoChenNeural", "晓臻（女声，台湾普通话）"),
                    new VoiceOption("zh-TW-HsiaoYuNeural", "晓雨（女声，台湾普通话）"),
                    new VoiceOption("zh-TW-YunJheNeural", "云哲（男声，台湾普通话）")
            )
    );

    /** Gemini 预设 */
    public static final ProviderPreset GEMINI_PRESET = new ProviderPreset(
            PROVIDER_GEMINI,
            "Gemini",
            TtsApiFormat.GEMINI_SPEECH,
            "/v1beta/models/{model}:generateContent",
            "https://generativelanguage.googleapis.com",
            "gemini-2.5-flash-preview-tts",
            "Kore",
            true,
            List.of(
                    new VoiceOption("Kore", "Kore"),
                    new VoiceOption("Puck", "Puck"),
                    new VoiceOption("Charon", "Charon"),
                    new VoiceOption("Fenrir", "Fenrir"),
                    new VoiceOption("Aoede", "Aoede")
            )
    );

    /** MiniMax 预设 */
    public static final ProviderPreset MINIMAX_PRESET = new ProviderPreset(
            PROVIDER_MINIMAX,
            "MiniMax",
            TtsApiFormat.MINIMAX_T2A,
            "/v1/t2a_v2",
            "https://api.minimax.chat",
            "speech-02-turbo",
            "male-qn-qingse",
            true,
            List.of(
                    new VoiceOption("male-qn-qingse", "青涩男声"),
                    new VoiceOption("male-qn-jingying", "精英男声"),
                    new VoiceOption("female-shaonv", "少女女声"),
                    new VoiceOption("female-yujie", "御姐女声"),
                    new VoiceOption("presenter_male", "主持男声"),
                    new VoiceOption("presenter_female", "主持女声")
            )
    );

    /** Qwen 预设 */
    public static final ProviderPreset QWEN_PRESET = new ProviderPreset(
            PROVIDER_QWEN,
            "Qwen",
            TtsApiFormat.QWEN_TTS,
            "/api/v1/services/aigc/multimodal-generation/generation",
            "https://dashscope.aliyuncs.com",
            "qwen3-tts-flash",
            "Cherry",
            true,
            List.of(
                    new VoiceOption("Cherry", "Cherry"),
                    new VoiceOption("Serena", "Serena"),
                    new VoiceOption("Ethan", "Ethan"),
                    new VoiceOption("Chelsie", "Chelsie")
            )
    );

    /** xAI 预设 */
    public static final ProviderPreset XAI_PRESET = new ProviderPreset(
            PROVIDER_XAI,
            "xAI",
            TtsApiFormat.XAI_TTS,
            "/v1/tts",
            "https://api.x.ai",
            "grok-tts",
            "Fritz-PlayAI",
            true,
            List.of(
                    new VoiceOption("Fritz-PlayAI", "Fritz"),
                    new VoiceOption("Aiden-PlayAI", "Aiden"),
                    new VoiceOption("Luna-PlayAI", "Luna")
            )
    );

    /** 全部预设列表 */
    public static final List<ProviderPreset> ALL_PRESETS = List.of(
            OPENAI_PRESET,
            MIMO_PRESET,
            EDGE_PRESET,
            GEMINI_PRESET,
            MINIMAX_PRESET,
            QWEN_PRESET,
            XAI_PRESET
    );

    // ==================== 工具方法 ====================

    /**
     * 根据 providerId 查找预设，未匹配时返回 null（按 OpenAI 兜底）。
     */
    public static ProviderPreset findPreset(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return OPENAI_PRESET;
        }
        String normalized = providerId.trim().toLowerCase(Locale.ROOT);
        return ALL_PRESETS.stream()
                .filter(p -> p.getProviderId().equals(normalized))
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
