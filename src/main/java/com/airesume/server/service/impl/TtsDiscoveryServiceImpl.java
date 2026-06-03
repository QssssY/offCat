package com.airesume.server.service.impl;

import com.airesume.server.common.constants.TtsProviderConstants;
import com.airesume.server.common.util.PublicHttpsUrlValidator;
import com.airesume.server.dto.user.TtsModelOption;
import com.airesume.server.dto.user.TtsVoiceOption;
import com.airesume.server.dto.user.UserTtsDiscoveryResponse;
import com.airesume.server.service.TtsDiscoveryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/**
 * TTS 模型/音色发现服务实现。
 * <p>
 * 模型发现：调用 GET {baseUrl}/models，过滤 ID 含 "tts" 的模型，无匹配则返回全部。
 * 音色发现：依次探测 /audio/voices、/v1/audio/voices，全部失败回落到 OpenAI 预设音色。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TtsDiscoveryServiceImpl implements TtsDiscoveryService {

    /** 模型列表获取超时 */
    private static final int DISCOVERY_TIMEOUT_MS = 10000;
    /** 单次端点探测超时 */
    private static final int PROBE_TIMEOUT_MS = 5000;
    /** 最大模型数量 */
    private static final int MAX_MODEL_COUNT = 500;
    /** 模型列表端点 */
    private static final String MODELS_ENDPOINT = "/models";
    /** 默认 TTS 合成端点（OpenAI 标准） */
    private static final String DEFAULT_TTS_ENDPOINT = "/audio/speech";
    /** 音色发现端点列表（按优先级排列） */
    private static final String[] VOICE_ENDPOINTS = {"/audio/voices", "/v1/audio/voices"};
    /** TTS 合成端点探测路径列表（按优先级排列） */
    private static final String[] TTS_SYNTHESIS_PATHS = {"/audio/speech", "/v1/audio/speech", "/v1/tts", "/tts"};
    /** TTS 模型 ID 过滤关键字 */
    private static final String TTS_MODEL_KEYWORD = "tts";

    /** OpenAI 标准预设音色 */
    private static final List<TtsVoiceOption> PRESET_VOICES = List.of(
            TtsVoiceOption.builder().id("alloy").name("Alloy").build(),
            TtsVoiceOption.builder().id("echo").name("Echo").build(),
            TtsVoiceOption.builder().id("fable").name("Fable").build(),
            TtsVoiceOption.builder().id("onyx").name("Onyx").build(),
            TtsVoiceOption.builder().id("nova").name("Nova").build(),
            TtsVoiceOption.builder().id("shimmer").name("Shimmer").build()
    );

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public UserTtsDiscoveryResponse discover(String baseUrl, String apiKey, String provider) {
        TtsProviderConstants.ProviderPreset preset = TtsProviderConstants.findPreset(provider);
        boolean isNonOpenAiPreset = preset != null
                && !TtsProviderConstants.PROVIDER_OPENAI.equals(preset.getProviderId());

        long start = System.currentTimeMillis();

        // 1. URL 校验
        String normalizedBaseUrl;
        try {
            normalizedBaseUrl = PublicHttpsUrlValidator.validate(baseUrl, "TTS 地址不能为空");
        } catch (IllegalArgumentException e) {
            log.warn("TTS 发现拦截非法 baseUrl: {}", trimText(baseUrl, 120));
            return buildFailure("基础地址不合法：" + e.getMessage(), start);
        }

        // 2. API Key 验证 — 部分 Provider 的 /models 端点无需认证，错误 Key 也能拿到模型列表，
        //    必须先用需要认证的端点验证 Key 有效性，避免返回误导性的"发现成功"。
        try {
            validateApiKey(normalizedBaseUrl, apiKey, preset);
        } catch (Exception e) {
            if (isAuthError(e)) {
                log.warn("TTS API Key 验证失败, provider: {}, error: {}", preset.getProviderId(), e.getMessage());
                return buildFailure("API Key 无效或已过期，请检查后重试", start);
            }
            // 非认证错误（404/网络超时等）不阻断，继续尝试模型发现
            log.debug("TTS API Key 验证端点不可用, 跳过验证继续发现, error: {}", e.getMessage());
        }

        // 3. 模型发现
        List<TtsModelOption> models = null;
        try {
            models = fetchTtsModels(normalizedBaseUrl, apiKey, preset);
        } catch (Exception e) {
            String safeMsg = e instanceof RestClientResponseException restEx
                    ? "上游返回 HTTP " + restEx.getStatusCode().value()
                    : trimText(e.getMessage(), 200);
            log.warn("TTS 模型发现失败, baseUrl: {}, provider: {}, error: {}",
                    trimText(normalizedBaseUrl, 80), preset.getProviderId(), safeMsg);
            // 认证失败（401/403）说明 API Key 有误，不能回落到预设，直接返回失败
            if (isAuthError(e)) {
                return buildFailure("API Key 无效或已过期，请检查后重试", start);
            }
            // 非认证错误（超时、404、500 等）：非 OpenAI Provider 回落到预设模型
            if (isNonOpenAiPreset) {
                models = List.of(TtsModelOption.builder()
                        .id(preset.getDefaultModel()).name(preset.getDefaultModel()).build());
                log.info("回落到 {} 预设模型: {}", preset.getDisplayName(), preset.getDefaultModel());
            }
        }
        // 模型获取失败且无预设可回落
        if (models == null) {
            return buildFailure("模型列表获取失败，请检查地址和 API Key", start);
        }

        // 4. 音色发现
        List<TtsVoiceOption> voices = null;
        boolean voiceDiscoverySupported = false;
        for (String endpoint : VOICE_ENDPOINTS) {
            try {
                voices = probeVoiceEndpoint(normalizedBaseUrl, apiKey, endpoint, preset);
                if (voices != null && !voices.isEmpty()) {
                    voiceDiscoverySupported = true;
                    log.info("TTS 音色发现成功, provider: {}, endpoint: {}, 发现 {} 个音色",
                            preset.getProviderId(), endpoint, voices.size());
                    break;
                }
            } catch (Exception e) {
                if (isAuthError(e)) {
                    return buildFailure("API Key 无效或已过期，请检查后重试", start);
                }
                log.debug("TTS 音色端点探测失败, provider: {}, endpoint: {}, error: {}",
                        preset.getProviderId(), endpoint, e.getMessage());
            }
        }
        // 音色预设回落：非 OpenAI Provider 用 Provider 自带预设，其余用 OpenAI 预设
        if (voices == null || voices.isEmpty()) {
            if (isNonOpenAiPreset && preset.getPresetVoices() != null && !preset.getPresetVoices().isEmpty()) {
                voices = preset.getPresetVoices().stream()
                        .map(v -> TtsVoiceOption.builder().id(v.getId()).name(v.getName()).build())
                        .toList();
                log.info("TTS Provider {} 音色端点不可用，回落到 Provider 预设音色（{} 个）",
                        preset.getDisplayName(), voices.size());
            } else {
                voices = PRESET_VOICES;
                log.info("TTS 音色端点均不可用，回落到 OpenAI 预设音色列表（{} 个）", PRESET_VOICES.size());
            }
        }

        // 5. TTS 合成端点路径
        String ttsEndpointPath;
        if (isNonOpenAiPreset) {
            ttsEndpointPath = preset.getDefaultEndpointPath();
        } else {
            ttsEndpointPath = probeSynthesisEndpoint(normalizedBaseUrl, apiKey);
        }

        long latencyMs = System.currentTimeMillis() - start;
        int modelCount = models.size();
        int voiceCount = voices.size();
        log.info("TTS 发现完成, provider: {}, 模型: {} 个, 音色: {} 个 ({}), 合成端点: {}, 耗时: {}ms",
                preset.getProviderId(), modelCount, voiceCount,
                voiceDiscoverySupported ? "预设/实时发现" : "预设回落", ttsEndpointPath, latencyMs);

        return UserTtsDiscoveryResponse.builder()
                .success(true)
                .message(String.format("发现 %d 个模型、%d 个音色", modelCount, voiceCount))
                .models(models)
                .voices(voices)
                .voiceDiscoverySupported(voiceDiscoverySupported)
                .ttsEndpointPath(ttsEndpointPath)
                .build();
    }

    /**
     * 调用 /models 端点获取模型列表，过滤出 TTS 相关模型。
     * 若过滤结果为空则返回全部模型（部分 Provider 不按 tts 命名）。
     * 按 Provider 使用对应的认证头（OpenAI: Bearer, MiMo: api-key）。
     */
    private List<TtsModelOption> fetchTtsModels(String baseUrl, String apiKey,
                                                 TtsProviderConstants.ProviderPreset preset) throws Exception {
        RestClient restClient = createRestClient(baseUrl, DISCOVERY_TIMEOUT_MS);
        String authHeaderName = preset.getApiFormat().getAuthHeaderName();
        String authHeaderValue = preset.getApiFormat().getAuthHeaderPrefix() + apiKey;
        String rawResponse = restClient.get()
                .uri(MODELS_ENDPOINT)
                .header(authHeaderName, authHeaderValue)
                .retrieve()
                .body(String.class);

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalArgumentException("模型列表响应为空");
        }

        JsonNode dataNode = objectMapper.readTree(rawResponse).path("data");
        if (!dataNode.isArray()) {
            throw new IllegalArgumentException("模型列表响应缺少 data 数组");
        }

        // 第一轮：收集所有模型 ID
        Set<String> allIds = new LinkedHashSet<>();
        for (JsonNode item : dataNode) {
            String id = item.path("id").asText("").trim();
            if (!id.isEmpty()) {
                allIds.add(id);
            }
            if (allIds.size() >= MAX_MODEL_COUNT) {
                break;
            }
        }

        if (allIds.isEmpty()) {
            throw new IllegalArgumentException("模型列表为空");
        }

        // 第二轮：过滤 TTS 相关模型
        List<TtsModelOption> ttsModels = allIds.stream()
                .filter(id -> id.toLowerCase(Locale.ROOT).contains(TTS_MODEL_KEYWORD))
                .map(id -> TtsModelOption.builder().id(id).name(id).build())
                .toList();

        // 有 TTS 模型则返回过滤结果，否则返回全部模型
        if (!ttsModels.isEmpty()) {
            return ttsModels;
        }

        return allIds.stream()
                .map(id -> TtsModelOption.builder().id(id).name(id).build())
                .toList();
    }

    /**
     * 探测单个音色端点，解析返回的音色列表。
     * 支持两种响应格式：{"data": [...]} 和直接 [...]
     * 按 Provider 使用对应的认证头。
     */
    private List<TtsVoiceOption> probeVoiceEndpoint(String baseUrl, String apiKey, String endpoint,
                                                     TtsProviderConstants.ProviderPreset preset) throws Exception {
        RestClient restClient = createRestClient(baseUrl, PROBE_TIMEOUT_MS);
        String authHeaderName = preset.getApiFormat().getAuthHeaderName();
        String authHeaderValue = preset.getApiFormat().getAuthHeaderPrefix() + apiKey;
        String rawResponse = restClient.get()
                .uri(endpoint)
                .header(authHeaderName, authHeaderValue)
                .retrieve()
                .body(String.class);

        if (rawResponse == null || rawResponse.isBlank()) {
            return null;
        }

        JsonNode root = objectMapper.readTree(rawResponse);
        // 尝试 {"data": [...]} 格式
        JsonNode dataNode = root.path("data");
        // 若无 data 字段，尝试直接数组格式
        if (!dataNode.isArray()) {
            dataNode = root;
        }
        if (!dataNode.isArray()) {
            return null;
        }

        List<TtsVoiceOption> voices = new ArrayList<>();
        for (JsonNode item : dataNode) {
            String id = extractVoiceId(item);
            if (id != null && !id.isEmpty()) {
                String name = item.path("name").asText(id);
                voices.add(TtsVoiceOption.builder().id(id).name(name).build());
            }
        }
        return voices;
    }

    /**
     * 从音色 JSON 节点中提取 ID，依次尝试 id、voice_id、name 字段。
     */
    private String extractVoiceId(JsonNode item) {
        // 优先取 id 字段
        String id = item.path("id").asText("").trim();
        if (!id.isEmpty()) {
            return id;
        }
        // 其次取 voice_id 字段（部分 Provider 使用）
        id = item.path("voice_id").asText("").trim();
        if (!id.isEmpty()) {
            return id;
        }
        // 最后取 name 字段作为兜底
        return item.path("name").asText("").trim();
    }

    /**
     * 自动探测 TTS 合成端点路径。
     * 发送最小化 POST 请求到常见路径，非 404 响应即视为端点存在。
     * 探测顺序：/audio/speech → /v1/audio/speech → /v1/tts → /tts
     * 全部返回 404 时回落到 OpenAI 默认 /audio/speech。
     */
    private String probeSynthesisEndpoint(String baseUrl, String apiKey) {
        // 构造最小探测请求体，不会产生实际音频成本
        Map<String, String> probeBody = Map.of(
                "model", "probe",
                "voice", "probe",
                "input", "t"
        );

        for (String path : TTS_SYNTHESIS_PATHS) {
            try {
                RestClient restClient = createRestClient(baseUrl, PROBE_TIMEOUT_MS);
                restClient.post()
                        .uri(path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(probeBody)
                        .retrieve()
                        .toBodilessEntity();
                // 200 响应，端点存在
                log.info("TTS 合成端点探测成功, path: {}, 响应: 200", path);
                return path;
            } catch (RestClientResponseException ex) {
                int status = ex.getStatusCode().value();
                if (status == 404) {
                    // 404 = 端点不存在，尝试下一个
                    log.debug("TTS 合成端点探测 404, path: {}", path);
                    continue;
                }
                // 非 404（400/401/403/422 等）说明端点存在，只是参数不对
                log.info("TTS 合成端点探测命中, path: {}, 状态码: {} (非 404 视为端点存在)", path, status);
                return path;
            } catch (Exception e) {
                // 网络超时等异常，跳过此路径
                log.debug("TTS 合成端点探测异常, path: {}, error: {}", path, e.getMessage());
            }
        }

        // 全部探测失败，回落到 OpenAI 默认路径
        log.info("TTS 合成端点全部探测失败，回落到默认路径: {}", DEFAULT_TTS_ENDPOINT);
        return DEFAULT_TTS_ENDPOINT;
    }

    /**
     * 创建带超时和 JSON 头的 RestClient。
     */
    private RestClient createRestClient(String baseUrl, int timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return restClientBuilder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private UserTtsDiscoveryResponse buildFailure(String errorMessage, long start) {
        long latencyMs = System.currentTimeMillis() - start;
        String safeMessage = trimText(errorMessage == null || errorMessage.isBlank() ? "未知错误" : errorMessage, 300);
        return UserTtsDiscoveryResponse.builder()
                .success(false)
                .message("TTS 模型/音色发现失败，请检查地址和 API Key，或手动输入模型名和音色 ID。")
                .models(List.of())
                .voices(PRESET_VOICES)
                .voiceDiscoverySupported(false)
                .errorMessage(safeMessage)
                .build();
    }

    /**
     * 用需要认证的端点验证 API Key 是否有效。
     * 向 TTS 合成端点发送最小请求：Key 有效时返回 400/422（参数不对），Key 无效时返回 401/403。
     */
    private void validateApiKey(String baseUrl, String apiKey,
                                TtsProviderConstants.ProviderPreset preset) {
        String endpoint = preset.getDefaultEndpointPath();
        RestClient restClient = createRestClient(baseUrl, PROBE_TIMEOUT_MS);
        String authHeaderName = preset.getApiFormat().getAuthHeaderName();
        String authHeaderValue = preset.getApiFormat().getAuthHeaderPrefix() + apiKey;
        Map<String, String> probeBody = Map.of(
                "model", "probe",
                "voice", "probe",
                "input", "t"
        );
        try {
            restClient.post()
                    .uri(endpoint)
                    .header(authHeaderName, authHeaderValue)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(probeBody)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 401 || status == 403) {
                throw ex;
            }
            // 400/422/500 等非认证错误说明端点存在且 Key 已通过认证
        }
    }

    /**
     * 判断异常是否属于认证失败（401/403），此时 API Key 必定有误，不应回落到预设。
     */
    private boolean isAuthError(Exception e) {
        if (e instanceof RestClientResponseException restEx) {
            int status = restEx.getStatusCode().value();
            return status == 401 || status == 403;
        }
        return false;
    }

    private String trimText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
