package com.airesume.server.service.impl;

import com.airesume.server.common.constants.TtsProviderConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.common.util.PublicHttpsUrlValidator;
import com.airesume.server.dto.user.UserTtsConnectivityTestRequest;
import com.airesume.server.dto.user.UserTtsConnectivityTestResponse;
import com.airesume.server.dto.user.TtsAudioResult;
import com.airesume.server.service.EdgeTtsClient;
import com.airesume.server.service.UserTtsConnectivityTestService;
import com.fasterxml.jackson.core.JsonProcessingException;
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
 * OpenAI 兼容 TTS 连通测试实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserTtsConnectivityTestServiceImpl implements UserTtsConnectivityTestService {

    /** 默认 TTS 合成端点路径（OpenAI 标准）。 */
    private static final String DEFAULT_TTS_ENDPOINT = "/audio/speech";
    private static final int TEST_TIMEOUT_MS = 10000;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestClient.Builder restClientBuilder;
    private final EdgeTtsClient edgeTtsClient;

    @Override
    public UserTtsConnectivityTestResponse testConnectivity(UserTtsConnectivityTestRequest request) {
        String provider = normalizeProvider(request.getTtsProvider());
        if ("gemini".equals(provider)) {
            return testViaGemini(request);
        }
        if ("minimax".equals(provider)) {
            return testViaMiniMax(request);
        }
        if ("qwen".equals(provider)) {
            return testViaQwen(request);
        }
        if ("xai".equals(provider)) {
            return testViaXai(request);
        }
        TtsProviderConstants.TtsApiFormat apiFormat = TtsProviderConstants.resolveApiFormat(request.getTtsProvider());
        if (apiFormat == TtsProviderConstants.TtsApiFormat.EDGE_READALOUD) {
            return testViaEdgeReadAloud(request);
        }
        // MiMo 等 Chat Completions 格式的 Provider 走独立测试路径
        if (apiFormat == TtsProviderConstants.TtsApiFormat.CHAT_COMPLETIONS_TTS) {
            return testViaChatCompletions(request);
        }
        return testViaAudioSpeech(request);
    }

    /**
     * Gemini TTS 连通测试：真实合成最短音频，并确认可解析为 WAV。
     */
    private UserTtsConnectivityTestResponse testViaGemini(UserTtsConnectivityTestRequest request) {
        String model = normalizeRequired(request.getModel(), "TTS 模型不能为空");
        String endpointPath = resolveModelEndpointPath(request.getEndpointPath(), model);
        long start = System.currentTimeMillis();
        try {
            TtsAudioResult audio = synthesizePreviewViaGemini(request);
            if (audio.getAudioBytes().length == 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CONNECTIVITY_FAILED, "TTS 返回音频为空");
            }
            return UserTtsConnectivityTestResponse.builder()
                    .success(true)
                    .message("TTS 连通测试成功")
                    .endpointPath(endpointPath)
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        } catch (BusinessException ex) {
            return buildFailure(start, "API_ERROR", ex.getMessage(), endpointPath);
        } catch (Exception ex) {
            return buildFailure(start, "NETWORK_ERROR", ex.getMessage(), endpointPath);
        }
    }

    /**
     * MiniMax TTS 连通测试：真实合成最短音频，并确认十六进制音频可解码。
     */
    private UserTtsConnectivityTestResponse testViaMiniMax(UserTtsConnectivityTestRequest request) {
        String endpointPath = resolveEndpointPath(request.getEndpointPath());
        long start = System.currentTimeMillis();
        try {
            TtsAudioResult audio = synthesizePreviewViaMiniMax(request);
            if (audio.getAudioBytes().length == 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CONNECTIVITY_FAILED, "TTS 返回音频为空");
            }
            return UserTtsConnectivityTestResponse.builder()
                    .success(true)
                    .message("TTS 连通测试成功")
                    .endpointPath(endpointPath)
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        } catch (BusinessException ex) {
            return buildFailure(start, "API_ERROR", ex.getMessage(), endpointPath);
        } catch (Exception ex) {
            return buildFailure(start, "NETWORK_ERROR", ex.getMessage(), endpointPath);
        }
    }

    /**
     * Qwen TTS 连通测试。
     * <p>
     * DashScope 返回音频 URL，本服务只接受阿里云官方域名，避免后端后续下载任意外部地址。
     */
    private UserTtsConnectivityTestResponse testViaQwen(UserTtsConnectivityTestRequest request) {
        String baseUrl = validateBaseUrl(request.getBaseUrl());
        String apiKey = normalizeRequired(request.getApiKey(), "TTS API Key 不能为空");
        String model = normalizeRequired(request.getModel(), "TTS 模型不能为空");
        String voiceId = normalizeRequired(request.getVoiceId(), "TTS 音色不能为空");
        String endpointPath = resolveEndpointPath(request.getEndpointPath());
        long start = System.currentTimeMillis();
        try {
            String responseJson = createRestClient(baseUrl)
                    .post()
                    .uri(endpointPath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(buildQwenRequestBody(model, voiceId, "你好"))
                    .retrieve()
                    .body(String.class);
            String audioUrl = extractQwenAudioUrl(responseJson);
            if (!isTrustedQwenAudioUrl(audioUrl)) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CONNECTIVITY_FAILED, "Qwen TTS 返回了不可信的音频地址");
            }
            return UserTtsConnectivityTestResponse.builder()
                    .success(true)
                    .message("TTS 连通测试成功")
                    .endpointPath(endpointPath)
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        } catch (BusinessException ex) {
            return buildFailure(start, "API_ERROR", ex.getMessage(), endpointPath);
        } catch (Exception ex) {
            return buildFailure(start, "NETWORK_ERROR", ex.getMessage(), endpointPath);
        }
    }

    /**
     * xAI TTS 使用 /v1/tts，测试请求体不能发送 model 字段。
     */
    private UserTtsConnectivityTestResponse testViaXai(UserTtsConnectivityTestRequest request) {
        String baseUrl = validateBaseUrl(request.getBaseUrl());
        String apiKey = normalizeRequired(request.getApiKey(), "TTS API Key 不能为空");
        String voiceId = normalizeRequired(request.getVoiceId(), "TTS 音色不能为空");
        String endpointPath = resolveEndpointPath(request.getEndpointPath());
        long start = System.currentTimeMillis();
        try {
            byte[] audioBytes = createRestClient(baseUrl)
                    .post()
                    .uri(endpointPath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.valueOf("audio/mpeg"))
                    .body(buildXaiRequestBody(voiceId, "你好"))
                    .retrieve()
                    .body(byte[].class);
            if (audioBytes == null || audioBytes.length == 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CONNECTIVITY_FAILED, "TTS 返回音频为空");
            }
            return UserTtsConnectivityTestResponse.builder()
                    .success(true)
                    .message("TTS 连通测试成功")
                    .endpointPath(endpointPath)
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        } catch (BusinessException ex) {
            return buildFailure(start, "API_ERROR", ex.getMessage(), endpointPath);
        } catch (Exception ex) {
            return buildFailure(start, "NETWORK_ERROR", ex.getMessage(), endpointPath);
        }
    }

    /**
     * EdgeTTS 连通测试。
     * <p>
     * Edge Read Aloud 不需要用户 API Key，连通性以能否合成最短 mp3 音频为准。
     */
    private UserTtsConnectivityTestResponse testViaEdgeReadAloud(UserTtsConnectivityTestRequest request) {
        validateBaseUrl(request.getBaseUrl());
        normalizeRequired(request.getModel(), "TTS 模型不能为空");
        String voiceId = normalizeRequired(request.getVoiceId(), "TTS 音色不能为空");
        String endpointPath = TtsProviderConstants.EDGE_PRESET.getDefaultEndpointPath();
        long start = System.currentTimeMillis();
        try {
            byte[] audioBytes = edgeTtsClient.synthesize("你好", voiceId, Duration.ofMillis(TEST_TIMEOUT_MS));
            if (audioBytes == null || audioBytes.length == 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CONNECTIVITY_FAILED, "EdgeTTS 返回音频为空");
            }
            return UserTtsConnectivityTestResponse.builder()
                    .success(true)
                    .message("TTS 连通测试成功")
                    .endpointPath(endpointPath)
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        } catch (BusinessException ex) {
            return buildFailure(start, "API_ERROR", ex.getMessage(), endpointPath);
        } catch (Exception ex) {
            return buildFailure(start, "NETWORK_ERROR", ex.getMessage(), endpointPath);
        }
    }

    /**
     * OpenAI 标准 /audio/speech 连通测试（二进制音频响应）。
     */
    private UserTtsConnectivityTestResponse testViaAudioSpeech(UserTtsConnectivityTestRequest request) {
        String baseUrl = validateBaseUrl(request.getBaseUrl());
        String apiKey = normalizeRequired(request.getApiKey(), "TTS API Key 不能为空");
        String model = normalizeRequired(request.getModel(), "TTS 模型不能为空");
        String voiceId = normalizeRequired(request.getVoiceId(), "TTS 音色不能为空");
        String endpointPath = resolveEndpointPath(request.getEndpointPath());
        long start = System.currentTimeMillis();
        try {
            byte[] audioBytes = createRestClient(baseUrl)
                    .post()
                    .uri(endpointPath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.valueOf("audio/mpeg"))
                    .body(buildOpenAiRequestBody(model, voiceId))
                    .retrieve()
                    .body(byte[].class);
            if (audioBytes == null || audioBytes.length == 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CONNECTIVITY_FAILED, "TTS 返回音频为空");
            }
            return UserTtsConnectivityTestResponse.builder()
                    .success(true)
                    .message("TTS 连通测试成功")
                    .endpointPath(endpointPath)
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        } catch (RestClientResponseException ex) {
            int statusCode = ex.getStatusCode().value();
            String detail = switch (statusCode) {
                case 404 -> "TTS 接口不存在（404），该 Provider 可能不支持 OpenAI 标准的 /audio/speech 端点";
                case 401, 403 -> "TTS API Key 无权限（" + statusCode + "），请检查密钥是否正确";
                case 400 -> "TTS 请求参数有误（400），请检查模型名和音色 ID 是否正确";
                default -> "TTS 上游返回 HTTP " + statusCode;
            };
            return buildFailure(start, "HTTP_" + statusCode, detail);
        } catch (Exception ex) {
            return buildFailure(start, "NETWORK_ERROR", ex.getMessage());
        }
    }

    /**
     * Chat Completions TTS 连通测试（base64 JSON 响应），用于 MiMo 等 Provider。
     * <p>
     * 端点：POST /chat/completions
     * 认证头：api-key: {key}（非 Bearer）
     * 请求体：{model, messages: [{role:"assistant", content:"你好"}], audio: {format:"mp3", voice: voiceId}}
     * 响应：JSON choices[0].message.audio.data（base64 编码音频）
     */
    private UserTtsConnectivityTestResponse testViaChatCompletions(UserTtsConnectivityTestRequest request) {
        String baseUrl = validateBaseUrl(request.getBaseUrl());
        String apiKey = normalizeRequired(request.getApiKey(), "TTS API Key 不能为空");
        String model = normalizeRequired(request.getModel(), "TTS 模型不能为空");
        String voiceId = normalizeRequired(request.getVoiceId(), "TTS 音色不能为空");
        TtsProviderConstants.ProviderPreset preset = TtsProviderConstants.findPreset(request.getTtsProvider());
        String endpointPath = preset.getDefaultEndpointPath();
        long start = System.currentTimeMillis();
        try {
            // 构造 Chat Completions TTS 请求体
            Map<String, Object> audio = new LinkedHashMap<>();
            audio.put("format", "mp3");
            audio.put("voice", voiceId);
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", "assistant");
            message.put("content", "你好");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", List.of(message));
            body.put("audio", audio);

            String responseJson = createRestClient(baseUrl)
                    .post()
                    .uri(endpointPath)
                    .header(preset.getApiFormat().getAuthHeaderName(),
                            preset.getApiFormat().getAuthHeaderPrefix() + apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (responseJson == null || responseJson.isBlank()) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CONNECTIVITY_FAILED, "TTS 返回响应为空");
            }
            // 连通测试必须验证真实音频载荷，避免上游只返回普通文本也被误判成功。
            extractChatCompletionsAudioBytes(responseJson);
            return UserTtsConnectivityTestResponse.builder()
                    .success(true)
                    .message("TTS 连通测试成功")
                    .endpointPath(endpointPath)
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        } catch (RestClientResponseException ex) {
            int statusCode = ex.getStatusCode().value();
            String detail = switch (statusCode) {
                case 404 -> "TTS 接口不存在（404），请检查地址和 Provider 类型";
                case 401, 403 -> "TTS API Key 无权限（" + statusCode + "），请检查密钥是否正确";
                case 400 -> "TTS 请求参数有误（400），请检查模型名和音色 ID 是否正确";
                default -> "TTS 上游返回 HTTP " + statusCode;
            };
            return buildFailure(start, "HTTP_" + statusCode, detail);
        } catch (BusinessException ex) {
            return buildFailure(start, "API_ERROR", ex.getMessage());
        } catch (Exception ex) {
            return buildFailure(start, "NETWORK_ERROR", ex.getMessage());
        }
    }

    /**
     * 构造 OpenAI 兼容 /audio/speech 请求体，只发送最短测试文本，降低连通测试成本。
     */
    private Map<String, Object> buildOpenAiRequestBody(String model, String voiceId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("voice", voiceId);
        body.put("input", "你好");
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

    /**
     * 解析 Chat Completions TTS 返回的 base64 音频，缺失、非法或空音频都视为上游协议不符合预期。
     */
    private byte[] extractChatCompletionsAudioBytes(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String audioData = trimToNull(root.path("choices").path(0)
                    .path("message").path("audio").path("data").asText(null));
            if (audioData == null) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 响应中未找到音频数据");
            }
            byte[] audioBytes = Base64.getDecoder().decode(audioData);
            if (audioBytes.length == 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 返回音频为空");
            }
            return audioBytes;
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 响应中未找到有效音频数据");
        }
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

    private RestClient createRestClient(String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(TEST_TIMEOUT_MS));
        requestFactory.setReadTimeout(Duration.ofMillis(TEST_TIMEOUT_MS));
        return restClientBuilder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private RestClient createStandaloneRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(TEST_TIMEOUT_MS));
        requestFactory.setReadTimeout(Duration.ofMillis(TEST_TIMEOUT_MS));
        return restClientBuilder.clone()
                .requestFactory(requestFactory)
                .build();
    }

    private UserTtsConnectivityTestResponse buildFailure(long start, String errorType, String errorMessage) {
        return buildFailure(start, errorType, errorMessage, DEFAULT_TTS_ENDPOINT);
    }

    private UserTtsConnectivityTestResponse buildFailure(long start, String errorType, String errorMessage,
                                                         String endpointPath) {
        String safeMessage = errorMessage == null || errorMessage.isBlank() ? "未知错误" : errorMessage;
        log.warn("用户自定义 TTS 连通测试失败, errorType: {}, error: {}", errorType, trimText(safeMessage, 180));
        return UserTtsConnectivityTestResponse.builder()
                .success(false)
                .message(safeMessage)
                .endpointPath(endpointPath)
                .latencyMs(System.currentTimeMillis() - start)
                .errorType(errorType)
                .build();
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

    private String validateBaseUrl(String baseUrl) {
        try {
            return PublicHttpsUrlValidator.validate(normalizeRequired(baseUrl, "TTS 地址不能为空"), "TTS 地址不能为空");
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, e.getMessage());
        }
    }

    private String normalizeRequired(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, message);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeProvider(String provider) {
        String normalized = trimToNull(provider);
        return normalized == null ? TtsProviderConstants.PROVIDER_OPENAI : normalized.toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * 使用当前表单参数合成最短试听音频，返回原始音频字节。
     * <p>
     * 复用连通测试的合成逻辑（OpenAI /audio/speech 或 Chat Completions），
     * 但直接返回音频 byte[] 而非仅返回连通元数据。
     */
    @Override
    public TtsAudioResult previewVoiceAudio(UserTtsConnectivityTestRequest request) {
        String provider = normalizeProvider(request.getTtsProvider());
        if ("gemini".equals(provider)) {
            return synthesizePreviewViaGemini(request);
        }
        if ("minimax".equals(provider)) {
            return synthesizePreviewViaMiniMax(request);
        }
        if ("xai".equals(provider)) {
            return TtsAudioResult.of(synthesizePreviewViaXai(request), "audio/mpeg");
        }
        if ("qwen".equals(provider)) {
            return synthesizePreviewViaQwen(request);
        }
        TtsProviderConstants.TtsApiFormat apiFormat = TtsProviderConstants.resolveApiFormat(request.getTtsProvider());
        if (apiFormat == TtsProviderConstants.TtsApiFormat.EDGE_READALOUD) {
            return TtsAudioResult.of(synthesizePreviewViaEdgeReadAloud(request), "audio/mpeg");
        }
        if (apiFormat == TtsProviderConstants.TtsApiFormat.CHAT_COMPLETIONS_TTS) {
            return TtsAudioResult.of(synthesizePreviewViaChatCompletions(request), "audio/mpeg");
        }
        return TtsAudioResult.of(synthesizePreviewViaAudioSpeech(request), "audio/mpeg");
    }

    /**
     * Gemini TTS 返回 L16 PCM，需要转换为 wav 后给浏览器播放。
     */
    private TtsAudioResult synthesizePreviewViaGemini(UserTtsConnectivityTestRequest request) {
        String baseUrl = validateBaseUrl(request.getBaseUrl());
        String apiKey = normalizeRequired(request.getApiKey(), "TTS API Key 不能为空");
        String model = normalizeRequired(request.getModel(), "TTS 模型不能为空");
        String voiceId = normalizeRequired(request.getVoiceId(), "TTS 音色不能为空");
        String endpointPath = resolveModelEndpointPath(request.getEndpointPath(), model);
        try {
            String responseJson = createRestClient(baseUrl)
                    .post()
                    .uri(endpointPath)
                    .header("x-goog-api-key", apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(buildGeminiRequestBody(voiceId, "你好"))
                    .retrieve()
                    .body(String.class);
            return decodeGeminiAudioResult(responseJson);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("TTS 试听合成异常 (Gemini), error: {}", ex.getMessage());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 试听失败，请稍后重试");
        }
    }

    /**
     * MiniMax TTS 返回十六进制字符串音频，需要解码为 mp3 字节。
     */
    private TtsAudioResult synthesizePreviewViaMiniMax(UserTtsConnectivityTestRequest request) {
        String baseUrl = validateBaseUrl(request.getBaseUrl());
        String apiKey = normalizeRequired(request.getApiKey(), "TTS API Key 不能为空");
        String model = normalizeRequired(request.getModel(), "TTS 模型不能为空");
        String voiceId = normalizeRequired(request.getVoiceId(), "TTS 音色不能为空");
        String endpointPath = resolveEndpointPath(request.getEndpointPath());
        try {
            String responseJson = createRestClient(baseUrl)
                    .post()
                    .uri(endpointPath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(buildMiniMaxRequestBody(model, voiceId, "你好"))
                    .retrieve()
                    .body(String.class);
            return TtsAudioResult.of(decodeMiniMaxAudio(responseJson), "audio/mpeg");
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("TTS 试听合成异常 (MiniMax), error: {}", ex.getMessage());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 试听失败，请稍后重试");
        }
    }

    private byte[] synthesizePreviewViaXai(UserTtsConnectivityTestRequest request) {
        String baseUrl = validateBaseUrl(request.getBaseUrl());
        String apiKey = normalizeRequired(request.getApiKey(), "TTS API Key 不能为空");
        String voiceId = normalizeRequired(request.getVoiceId(), "TTS 音色不能为空");
        String endpointPath = resolveEndpointPath(request.getEndpointPath());
        byte[] audioBytes = createRestClient(baseUrl)
                .post()
                .uri(endpointPath)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.valueOf("audio/mpeg"))
                .body(buildXaiRequestBody(voiceId, "你好"))
                .retrieve()
                .body(byte[].class);
        if (audioBytes == null || audioBytes.length == 0) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONNECTIVITY_FAILED, "TTS 返回音频为空");
        }
        return audioBytes;
    }

    /**
     * Qwen 试听：先获取阿里云临时音频 URL，再校验域名并下载音频字节。
     */
    private TtsAudioResult synthesizePreviewViaQwen(UserTtsConnectivityTestRequest request) {
        String baseUrl = validateBaseUrl(request.getBaseUrl());
        String apiKey = normalizeRequired(request.getApiKey(), "TTS API Key 不能为空");
        String model = normalizeRequired(request.getModel(), "TTS 模型不能为空");
        String voiceId = normalizeRequired(request.getVoiceId(), "TTS 音色不能为空");
        String endpointPath = resolveEndpointPath(request.getEndpointPath());
        try {
            String responseJson = createRestClient(baseUrl)
                    .post()
                    .uri(endpointPath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(buildQwenRequestBody(model, voiceId, "你好"))
                    .retrieve()
                    .body(String.class);
            String audioUrl = extractQwenAudioUrl(responseJson);
            if (!isTrustedQwenAudioUrl(audioUrl)) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CONNECTIVITY_FAILED, "Qwen TTS 返回了不可信的音频地址");
            }
            ResponseEntity<byte[]> audioResponse = createStandaloneRestClient()
                    .get()
                    .uri(URI.create(audioUrl))
                    .accept(MediaType.APPLICATION_OCTET_STREAM,
                            MediaType.valueOf("audio/mpeg"),
                            MediaType.valueOf("audio/wav"))
                    .retrieve()
                    .toEntity(byte[].class);
            byte[] audioBytes = audioResponse.getBody();
            if (audioBytes == null || audioBytes.length == 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CONNECTIVITY_FAILED, "TTS 返回音频为空");
            }
            return TtsAudioResult.of(audioBytes,
                    resolveDownloadedAudioContentType(audioResponse.getHeaders().getContentType(), audioUrl));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("TTS 试听合成异常 (Qwen), error: {}", ex.getMessage());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 试听失败，请稍后重试");
        }
    }

    /**
     * EdgeTTS 试听合成，返回真实 mp3 字节供前端播放。
     */
    private byte[] synthesizePreviewViaEdgeReadAloud(UserTtsConnectivityTestRequest request) {
        validateBaseUrl(request.getBaseUrl());
        normalizeRequired(request.getModel(), "TTS 模型不能为空");
        String voiceId = normalizeRequired(request.getVoiceId(), "TTS 音色不能为空");
        try {
            byte[] audioBytes = edgeTtsClient.synthesize("你好", voiceId, Duration.ofMillis(TEST_TIMEOUT_MS));
            if (audioBytes == null || audioBytes.length == 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CONNECTIVITY_FAILED, "EdgeTTS 返回音频为空");
            }
            return audioBytes;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("TTS 试听合成异常 (EdgeTTS), error: {}", ex.getMessage());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 试听失败，请稍后重试");
        }
    }

    /**
     * OpenAI 标准 /audio/speech 试听合成（二进制音频响应）。
     */
    private byte[] synthesizePreviewViaAudioSpeech(UserTtsConnectivityTestRequest request) {
        String baseUrl = validateBaseUrl(request.getBaseUrl());
        String apiKey = normalizeRequired(request.getApiKey(), "TTS API Key 不能为空");
        String model = normalizeRequired(request.getModel(), "TTS 模型不能为空");
        String voiceId = normalizeRequired(request.getVoiceId(), "TTS 音色不能为空");
        String endpointPath = resolveEndpointPath(request.getEndpointPath());
        try {
            byte[] audioBytes = createRestClient(baseUrl)
                    .post()
                    .uri(endpointPath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.valueOf("audio/mpeg"))
                    .body(buildOpenAiRequestBody(model, voiceId))
                    .retrieve()
                    .body(byte[].class);
            if (audioBytes == null || audioBytes.length == 0) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CONNECTIVITY_FAILED, "TTS 返回音频为空");
            }
            return audioBytes;
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            log.warn("TTS 试听合成失败 (OpenAI), status: {}, model: {}, voice: {}", status, model, voiceId);
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED,
                    "TTS 试听失败，上游返回 HTTP " + status);
        } catch (Exception ex) {
            log.warn("TTS 试听合成异常 (OpenAI), error: {}", ex.getMessage());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 试听失败，请稍后重试");
        }
    }

    /**
     * Chat Completions TTS 试听合成（base64 JSON 响应），用于 MiMo 等 Provider。
     * <p>
     * 端点：POST /chat/completions，认证头：api-key: {key}
     * 响应：JSON choices[0].message.audio.data（base64 编码音频）
     */
    private byte[] synthesizePreviewViaChatCompletions(UserTtsConnectivityTestRequest request) {
        String baseUrl = validateBaseUrl(request.getBaseUrl());
        String apiKey = normalizeRequired(request.getApiKey(), "TTS API Key 不能为空");
        String model = normalizeRequired(request.getModel(), "TTS 模型不能为空");
        String voiceId = normalizeRequired(request.getVoiceId(), "TTS 音色不能为空");
        TtsProviderConstants.ProviderPreset preset = TtsProviderConstants.findPreset(request.getTtsProvider());
        String endpointPath = preset.getDefaultEndpointPath();
        try {
            Map<String, Object> audio = new LinkedHashMap<>();
            audio.put("format", "mp3");
            audio.put("voice", voiceId);
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", "assistant");
            message.put("content", "你好");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", List.of(message));
            body.put("audio", audio);

            String responseJson = createRestClient(baseUrl)
                    .post()
                    .uri(endpointPath)
                    .header(preset.getApiFormat().getAuthHeaderName(),
                            preset.getApiFormat().getAuthHeaderPrefix() + apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (responseJson == null || responseJson.isBlank()) {
                throw new BusinessException(ResultCode.CUSTOM_AI_CONNECTIVITY_FAILED, "TTS 返回响应为空");
            }
            return extractChatCompletionsAudioBytes(responseJson);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            log.warn("TTS 试听合成失败 (ChatCompletions), status: {}, model: {}, voice: {}", status, model, voiceId);
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED,
                    "TTS 试听失败，上游返回 HTTP " + status);
        } catch (Exception ex) {
            log.warn("TTS 试听合成异常 (ChatCompletions), error: {}", ex.getMessage());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "TTS 试听失败，请稍后重试");
        }
    }

    private String trimText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
