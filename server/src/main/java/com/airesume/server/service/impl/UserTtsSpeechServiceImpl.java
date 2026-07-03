package com.airesume.server.service.impl;

import com.airesume.server.common.constants.TtsProviderConstants;
import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.common.util.PublicHttpsUrlValidator;
import com.airesume.server.dto.user.ResolvedTtsConfig;
import com.airesume.server.dto.user.TtsAudioResult;

import com.airesume.server.entity.UserAiConfig;
import com.airesume.server.service.AiCredentialCrypto;
import com.airesume.server.service.EdgeTtsClient;
import com.airesume.server.service.SysTtsConfigService;
import com.airesume.server.service.UserAiConfigService;
import com.airesume.server.service.UserTtsSpeechService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 语音面试 TTS 运行时服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserTtsSpeechServiceImpl implements UserTtsSpeechService {

    /** 默认 TTS 合成端点路径（OpenAI 标准）。 */
    private static final String DEFAULT_TTS_ENDPOINT = "/audio/speech";
    private static final int TTS_TIMEOUT_MS = 15000;

    private final UserAiConfigService userAiConfigService;
    private final SysTtsConfigService sysTtsConfigService;
    private final AiCredentialCrypto aiCredentialCrypto;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final EdgeTtsClient edgeTtsClient;

    @Override
    public ResolvedTtsConfig resolveInterviewTtsConfig(Long userId) {
        if (userId == null) {
            return null;
        }
        // TTS 只服务语音面试播报，用户配置优先级固定为 interview -> default，永远不读取 resume 配置。
        UserAiConfig interviewConfig = userAiConfigService.findEnabledConfig(userId, UserAiConstants.CONFIG_TYPE_INTERVIEW);
        ResolvedTtsConfig resolved = buildResolvedConfig(interviewConfig);
        if (resolved != null) {
            return resolved;
        }
        resolved = buildResolvedConfig(userAiConfigService.findEnabledConfig(userId, UserAiConstants.CONFIG_TYPE_DEFAULT));
        if (resolved != null) {
            return resolved;
        }
        // 用户未配置可用 TTS 时，才启用系统级 TTS 兜底；用户自定义始终优先于系统默认。
        return sysTtsConfigService.resolveEnabledConfig();
    }

    @Override
    public boolean hasInterviewTtsConfig(Long userId) {
        return resolveInterviewTtsConfig(userId) != null;
    }

    @Override
    public boolean hasSystemTtsConfig() {
        return sysTtsConfigService.resolveEnabledConfig() != null;
    }

    @Override
    public byte[] synthesizeInterviewSpeech(Long userId, String text) {
        return synthesizeInterviewSpeechAudio(userId, text).getAudioBytes();
    }

    @Override
    public TtsAudioResult synthesizeInterviewSpeechAudio(Long userId, String text) {
        ResolvedTtsConfig config = resolveInterviewTtsConfig(userId);
        if (config == null) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "未配置可用于语音面试的 TTS");
        }
        String normalizedText = normalizeRequired(text, "TTS 文本不能为空");

        // 根据 Provider 分发到不同的协议处理器
        String provider = normalizeProvider(config.getTtsProvider());
        if ("gemini".equals(provider)) {
            return synthesizeViaGemini(userId, config, normalizedText);
        }
        if ("minimax".equals(provider)) {
            return TtsAudioResult.of(synthesizeViaMiniMax(userId, config, normalizedText), "audio/mpeg");
        }
        if ("xai".equals(provider)) {
            return TtsAudioResult.of(synthesizeViaXai(userId, config, normalizedText), "audio/mpeg");
        }
        if ("qwen".equals(provider)) {
            return synthesizeViaQwen(userId, config, normalizedText);
        }
        TtsProviderConstants.TtsApiFormat apiFormat = TtsProviderConstants.resolveApiFormat(config.getTtsProvider());
        if (apiFormat == TtsProviderConstants.TtsApiFormat.EDGE_READALOUD) {
            return TtsAudioResult.of(synthesizeViaEdgeReadAloud(userId, config, normalizedText), "audio/mpeg");
        }
        if (apiFormat == TtsProviderConstants.TtsApiFormat.CHAT_COMPLETIONS_TTS) {
            return TtsAudioResult.of(synthesizeViaChatCompletions(userId, config, normalizedText), "audio/mpeg");
        }
        return TtsAudioResult.of(synthesizeViaAudioSpeech(userId, config, normalizedText), "audio/mpeg");
    }

    /**
     * Gemini TTS 合成。Gemini 返回 L16 PCM 时转为 wav，避免浏览器收到裸 PCM 无法播放。
     */
    private TtsAudioResult synthesizeViaGemini(Long userId, ResolvedTtsConfig config, String text) {
        String endpointPath = resolveModelEndpointPath(config.getEndpointPath(), config.getModel());
        try {
            String responseJson = createRestClient(config.getBaseUrl(), TTS_TIMEOUT_MS)
                    .post()
                    .uri(endpointPath)
                    .header("x-goog-api-key", config.getApiKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .body(buildGeminiRequestBody(config.getVoiceId(), text))
                    .retrieve()
                    .body(String.class);
            return decodeGeminiAudioResult(responseJson);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.warn("语音面试 TTS 合成失败 (Gemini), userId: {}, status: {}",
                    userId, ex.getStatusCode().value());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 合成失败，请检查语音合成配置");
        } catch (Exception ex) {
            log.warn("语音面试 TTS 合成异常 (Gemini), userId: {}, errorType: {}",
                    userId, ex.getClass().getSimpleName());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 合成失败，请稍后重试");
        }
    }

    /**
     * MiniMax TTS 合成，响应中的十六进制音频字符串解码为 mp3 字节。
     */
    private byte[] synthesizeViaMiniMax(Long userId, ResolvedTtsConfig config, String text) {
        String endpointPath = resolveEndpointPath(config.getEndpointPath());
        try {
            String responseJson = createRestClient(config.getBaseUrl(), TTS_TIMEOUT_MS)
                    .post()
                    .uri(endpointPath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .body(buildMiniMaxRequestBody(config.getModel(), config.getVoiceId(), text))
                    .retrieve()
                    .body(String.class);
            return decodeMiniMaxAudio(responseJson);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.warn("语音面试 TTS 合成失败 (MiniMax), userId: {}, status: {}",
                    userId, ex.getStatusCode().value());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 合成失败，请检查语音合成配置");
        } catch (Exception ex) {
            log.warn("语音面试 TTS 合成异常 (MiniMax), userId: {}, errorType: {}",
                    userId, ex.getClass().getSimpleName());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 合成失败，请稍后重试");
        }
    }

    /**
     * xAI TTS 合成，请求体按上游要求不发送 model 字段。
     */
    private byte[] synthesizeViaXai(Long userId, ResolvedTtsConfig config, String text) {
        String endpointPath = resolveEndpointPath(config.getEndpointPath());
        try {
            byte[] audioBytes = createRestClient(config.getBaseUrl(), TTS_TIMEOUT_MS)
                    .post()
                    .uri(endpointPath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                    .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.valueOf("audio/mpeg"))
                    .body(buildXaiRequestBody(config.getVoiceId(), text))
                    .retrieve()
                    .body(byte[].class);
            if (audioBytes == null || audioBytes.length == 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 返回音频为空");
            }
            return audioBytes;
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.warn("语音面试 TTS 合成失败 (xAI), userId: {}, status: {}",
                    userId, ex.getStatusCode().value());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 合成失败，请检查语音合成配置");
        } catch (Exception ex) {
            log.warn("语音面试 TTS 合成异常 (xAI), userId: {}, errorType: {}",
                    userId, ex.getClass().getSimpleName());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 合成失败，请稍后重试");
        }
    }

    /**
     * Qwen TTS 合成。DashScope 返回阿里云临时音频 URL，后端校验 URL 后再下载音频字节。
     */
    private TtsAudioResult synthesizeViaQwen(Long userId, ResolvedTtsConfig config, String text) {
        String endpointPath = resolveEndpointPath(config.getEndpointPath());
        try {
            String responseJson = createRestClient(config.getBaseUrl(), TTS_TIMEOUT_MS)
                    .post()
                    .uri(endpointPath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .body(buildQwenRequestBody(config.getModel(), config.getVoiceId(), text))
                    .retrieve()
                    .body(String.class);
            String audioUrl = extractQwenAudioUrl(responseJson);
            if (!isTrustedQwenAudioUrl(audioUrl)) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "Qwen TTS 返回了不可信的音频地址");
            }
            ResponseEntity<byte[]> audioResponse = createStandaloneRestClient(TTS_TIMEOUT_MS)
                    .get()
                    .uri(URI.create(audioUrl))
                    .accept(MediaType.APPLICATION_OCTET_STREAM,
                            MediaType.valueOf("audio/mpeg"),
                            MediaType.valueOf("audio/wav"))
                    .retrieve()
                    .toEntity(byte[].class);
            byte[] audioBytes = audioResponse.getBody();
            if (audioBytes == null || audioBytes.length == 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 返回音频为空");
            }
            return TtsAudioResult.of(audioBytes,
                    resolveDownloadedAudioContentType(audioResponse.getHeaders().getContentType(), audioUrl));
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.warn("语音面试 TTS 合成失败 (Qwen), userId: {}, status: {}",
                    userId, ex.getStatusCode().value());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 合成失败，请检查语音合成配置");
        } catch (Exception ex) {
            log.warn("语音面试 TTS 合成异常 (Qwen), userId: {}, errorType: {}",
                    userId, ex.getClass().getSimpleName());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 合成失败，请稍后重试");
        }
    }

    /**
     * Edge Read Aloud 在线合成。
     * <p>
     * EdgeTTS 不使用用户 API Key，但仍复用既有配置解析和音频返回链路，确保语音面试前端继续接收 mp3 Blob。
     */
    private byte[] synthesizeViaEdgeReadAloud(Long userId, ResolvedTtsConfig config, String text) {
        try {
            byte[] audioBytes = edgeTtsClient.synthesize(text, config.getVoiceId(), Duration.ofMillis(TTS_TIMEOUT_MS));
            if (audioBytes == null || audioBytes.length == 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "EdgeTTS 返回音频为空");
            }
            return audioBytes;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("语音面试 TTS 合成异常 (EdgeTTS), userId: {}, source: {}, configType: {}, errorType: {}",
                    userId, config.getSource(), config.getConfigType(), ex.getClass().getSimpleName());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 合成失败，请稍后重试");
        }
    }

    /**
     * OpenAI 标准 /audio/speech 合成（二进制音频响应）。
     */
    private byte[] synthesizeViaAudioSpeech(Long userId, ResolvedTtsConfig config, String text) {
        String endpointPath = resolveEndpointPath(config.getEndpointPath());
        try {
            byte[] audioBytes = createRestClient(config.getBaseUrl(), TTS_TIMEOUT_MS)
                    .post()
                    .uri(endpointPath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                    .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.valueOf("audio/mpeg"))
                    .body(buildOpenAiRequestBody(config, text))
                    .retrieve()
                    .body(byte[].class);
            if (audioBytes == null || audioBytes.length == 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 返回音频为空");
            }
            return audioBytes;
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.warn("语音面试 TTS 合成失败 (OpenAI), userId: {}, source: {}, configType: {}, status: {}",
                    userId, config.getSource(), config.getConfigType(), ex.getStatusCode().value());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 合成失败，请检查语音合成配置");
        } catch (Exception ex) {
            log.warn("语音面试 TTS 合成异常 (OpenAI), userId: {}, source: {}, configType: {}, errorType: {}",
                    userId, config.getSource(), config.getConfigType(), ex.getClass().getSimpleName());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 合成失败，请稍后重试");
        }
    }

    /**
     * Chat Completions TTS 合成（base64 JSON 响应），用于 MiMo 等 Provider。
     * <p>
     * 端点：POST /chat/completions
     * 认证头：api-key: {key}
     * 请求体：{model, messages: [{role:"assistant", content: text}], audio: {format:"mp3", voice: voiceId}}
     * 响应：JSON choices[0].message.audio.data（base64 编码音频）
     */
    private byte[] synthesizeViaChatCompletions(Long userId, ResolvedTtsConfig config, String text) {
        TtsProviderConstants.ProviderPreset preset = TtsProviderConstants.findPreset(config.getTtsProvider());
        String endpointPath = preset.getDefaultEndpointPath();
        try {
            // 构造 Chat Completions TTS 请求体
            Map<String, Object> audio = new LinkedHashMap<>();
            audio.put("format", "mp3");
            audio.put("voice", config.getVoiceId());
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", "assistant");
            message.put("content", text);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", config.getModel());
            body.put("messages", List.of(message));
            body.put("audio", audio);

            String responseJson = createRestClient(config.getBaseUrl(), TTS_TIMEOUT_MS)
                    .post()
                    .uri(endpointPath)
                    .header(preset.getApiFormat().getAuthHeaderName(),
                            preset.getApiFormat().getAuthHeaderPrefix() + config.getApiKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (responseJson == null || responseJson.isBlank()) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 返回响应为空");
            }

            // 解析 JSON 响应：choices[0].message.audio.data → base64 → byte[]
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode audioDataNode = root.path("choices").path(0).path("message").path("audio").path("data");
            if (audioDataNode.isMissingNode() || audioDataNode.asText("").isEmpty()) {
                log.warn("Chat Completions TTS 响应缺少音频数据, userId: {}, response preview: {}",
                        userId, trimText(responseJson, 300));
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 响应中未找到音频数据");
            }
            byte[] audioBytes = Base64.getDecoder().decode(audioDataNode.asText());
            if (audioBytes.length == 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 返回音频为空");
            }
            return audioBytes;
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.warn("语音面试 TTS 合成失败 (ChatCompletions), userId: {}, source: {}, configType: {}, status: {}",
                    userId, config.getSource(), config.getConfigType(), ex.getStatusCode().value());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 合成失败，请检查语音合成配置");
        } catch (Exception ex) {
            log.warn("语音面试 TTS 合成异常 (ChatCompletions), userId: {}, source: {}, configType: {}, errorType: {}",
                    userId, config.getSource(), config.getConfigType(), ex.getClass().getSimpleName());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 合成失败，请稍后重试");
        }
    }

    protected RestClient createRestClient(String baseUrl, int timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return restClientBuilder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    protected RestClient createStandaloneRestClient(int timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return restClientBuilder.clone()
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 构造 OpenAI 兼容 /audio/speech 请求体。
     */
    private Map<String, Object> buildOpenAiRequestBody(ResolvedTtsConfig config, String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModel());
        body.put("voice", config.getVoiceId());
        body.put("input", text);
        body.put("response_format", "mp3");
        return body;
    }

    private Map<String, Object> buildGeminiRequestBody(String voiceId, String text) {
        Map<String, Object> prebuiltVoiceConfig = new LinkedHashMap<>();
        prebuiltVoiceConfig.put("voiceName", voiceId);
        Map<String, Object> voiceConfig = new LinkedHashMap<>();
        voiceConfig.put("prebuiltVoiceConfig", prebuiltVoiceConfig);
        Map<String, Object> speechConfig = new LinkedHashMap<>();
        speechConfig.put("voiceConfig", voiceConfig);
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("responseModalities", List.of("AUDIO"));
        generationConfig.put("speechConfig", speechConfig);
        Map<String, Object> part = new LinkedHashMap<>();
        part.put("text", text);
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("parts", List.of(part));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(content));
        body.put("generationConfig", generationConfig);
        return body;
    }

    private Map<String, Object> buildMiniMaxRequestBody(String model, String voiceId, String text) {
        Map<String, Object> voiceSetting = new LinkedHashMap<>();
        voiceSetting.put("voice_id", voiceId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("text", text);
        body.put("voice_setting", voiceSetting);
        return body;
    }

    private Map<String, Object> buildXaiRequestBody(String voiceId, String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("input", text);
        body.put("voice", voiceId);
        return body;
    }

    private Map<String, Object> buildQwenRequestBody(String model, String voiceId, String text) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("text", text);
        input.put("voice", voiceId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", input);
        return body;
    }

    private TtsAudioResult decodeGeminiAudioResult(String responseJson) {
        try {
            JsonNode inlineData = objectMapper.readTree(responseJson).path("candidates").path(0)
                    .path("content").path("parts").path(0).path("inlineData");
            String mimeType = inlineData.path("mimeType").asText("");
            String audioData = trimToNull(inlineData.path("data").asText(null));
            if (audioData == null) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 响应中未找到音频数据");
            }
            byte[] audioBytes = Base64.getDecoder().decode(audioData);
            if (audioBytes.length == 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 返回音频为空");
            }
            if (mimeType.toLowerCase().startsWith("audio/l16")) {
                return TtsAudioResult.of(wrapPcmAsWav(audioBytes, 24000, 1, 16), "audio/wav");
            }
            return TtsAudioResult.of(audioBytes, mimeType.isBlank() ? "audio/mpeg" : mimeType);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 响应中未找到有效音频数据");
        }
    }

    private byte[] decodeMiniMaxAudio(String responseJson) {
        try {
            String hexAudio = trimToNull(objectMapper.readTree(responseJson).path("data").path("audio").asText(null));
            if (hexAudio == null || hexAudio.length() % 2 != 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 响应中未找到音频数据");
            }
            byte[] audioBytes = new byte[hexAudio.length() / 2];
            for (int i = 0; i < audioBytes.length; i++) {
                int index = i * 2;
                audioBytes[i] = (byte) Integer.parseInt(hexAudio.substring(index, index + 2), 16);
            }
            if (audioBytes.length == 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 返回音频为空");
            }
            return audioBytes;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 响应中未找到有效音频数据");
        }
    }

    private String extractQwenAudioUrl(String responseJson) {
        try {
            String audioUrl = trimToNull(objectMapper.readTree(responseJson)
                    .path("output").path("audio").path("url").asText(null));
            if (audioUrl == null) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 响应中未找到音频地址");
            }
            return audioUrl;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 响应中未找到有效音频地址");
        }
    }

    private boolean isTrustedQwenAudioUrl(String audioUrl) {
        try {
            URI uri = URI.create(audioUrl);
            String host = uri.getHost();
            String scheme = uri.getScheme();
            String normalizedHost = host == null ? "" : host.toLowerCase(java.util.Locale.ROOT);
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && normalizedHost.matches(".+\\.oss-[a-z0-9-]+\\.aliyuncs\\.com");
        } catch (Exception ex) {
            return false;
        }
    }

    private String resolveDownloadedAudioContentType(MediaType responseContentType, String audioUrl) {
        if (responseContentType != null) {
            return responseContentType.toString();
        }
        String path = URI.create(audioUrl).getPath();
        String lowerPath = path == null ? "" : path.toLowerCase(java.util.Locale.ROOT);
        if (lowerPath.endsWith(".wav")) {
            return "audio/wav";
        }
        return "audio/mpeg";
    }

    private byte[] wrapPcmAsWav(byte[] pcmBytes, int sampleRate, int channels, int bitsPerSample) {
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(44 + pcmBytes.length);
            writeAscii(out, "RIFF");
            writeLittleEndianInt(out, 36 + pcmBytes.length);
            writeAscii(out, "WAVE");
            writeAscii(out, "fmt ");
            writeLittleEndianInt(out, 16);
            writeLittleEndianShort(out, 1);
            writeLittleEndianShort(out, channels);
            writeLittleEndianInt(out, sampleRate);
            writeLittleEndianInt(out, byteRate);
            writeLittleEndianShort(out, blockAlign);
            writeLittleEndianShort(out, bitsPerSample);
            writeAscii(out, "data");
            writeLittleEndianInt(out, pcmBytes.length);
            out.writeBytes(pcmBytes);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 音频封装失败");
        }
    }

    private void writeAscii(ByteArrayOutputStream out, String value) {
        out.writeBytes(value.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    private void writeLittleEndianInt(ByteArrayOutputStream out, int value) {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
        out.write((value >> 16) & 0xff);
        out.write((value >> 24) & 0xff);
    }

    private void writeLittleEndianShort(ByteArrayOutputStream out, int value) {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
    }

    private ResolvedTtsConfig buildResolvedConfig(UserAiConfig config) {
        if (config == null || !hasCompleteTtsFields(config)) {
            return null;
        }
        String baseUrl;
        try {
            baseUrl = PublicHttpsUrlValidator.validate(config.getTtsBaseUrl(), "TTS 地址不能为空");
        } catch (IllegalArgumentException ex) {
            log.warn("忽略非法用户自定义 TTS 地址, userId: {}, configType: {}",
                    config.getUserId(), config.getConfigType());
            return null;
        }
        // EdgeTTS 是无用户密钥的在线朗读协议，不能因为 ttsApiKey 为空而丢弃配置。
        String apiKey = "";
        if (!isEdgeProvider(config.getTtsProvider())) {
            apiKey = aiCredentialCrypto.decrypt(config.getTtsApiKey());
        }
        if (!isEdgeProvider(config.getTtsProvider()) && trimToNull(apiKey) == null) {
            return null;
        }
        return ResolvedTtsConfig.builder()
                .source("user_custom")
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .model(config.getTtsModel().trim())
                .voiceId(config.getTtsVoiceId().trim())
                .endpointPath(config.getTtsEndpointPath())
                .ttsProvider(config.getTtsProvider())
                .configType(config.getConfigType())
                .build();
    }

    private boolean hasCompleteTtsFields(UserAiConfig config) {
        boolean hasCommonFields = trimToNull(config.getTtsBaseUrl()) != null
                && trimToNull(config.getTtsModel()) != null
                && trimToNull(config.getTtsVoiceId()) != null;
        if (isEdgeProvider(config.getTtsProvider())) {
            return hasCommonFields;
        }
        return hasCommonFields && trimToNull(config.getTtsApiKey()) != null;
    }

    private boolean isEdgeProvider(String provider) {
        String normalized = trimToNull(provider);
        return normalized != null && TtsProviderConstants.PROVIDER_EDGE.equalsIgnoreCase(normalized);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, message);
        }
        return normalized;
    }

    /**
     * 解析 TTS 端点路径，为空或无效时回落到 OpenAI 标准 /audio/speech。
     */
    private String resolveEndpointPath(String endpointPath) {
        if (endpointPath == null || endpointPath.isBlank()) {
            return DEFAULT_TTS_ENDPOINT;
        }
        String trimmed = endpointPath.trim();
        if (!trimmed.startsWith("/")) {
            return DEFAULT_TTS_ENDPOINT;
        }
        return trimmed;
    }

    private String resolveModelEndpointPath(String endpointPath, String model) {
        String resolved = resolveEndpointPath(endpointPath);
        return resolved.replace("{model}", model);
    }

    private String normalizeProvider(String provider) {
        String normalized = trimToNull(provider);
        return normalized == null ? TtsProviderConstants.PROVIDER_OPENAI : normalized.toLowerCase(java.util.Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
