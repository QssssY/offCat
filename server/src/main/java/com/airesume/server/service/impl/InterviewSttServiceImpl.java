package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.common.result.ResultCode;
import com.airesume.server.dto.user.ResolvedSttConfig;
import com.airesume.server.service.InterviewSttService;
import com.airesume.server.service.SysSttConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

/**
 * 云端语音识别（STT）服务实现。
 * <p>
 * 调用 OpenAI 兼容的 /audio/transcriptions 接口（默认硅基流动 SenseVoiceSmall）。
 * 只在浏览器 Web Speech 不可用时作为兜底，配置与面试对话 AI、TTS 完全隔离。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSttServiceImpl implements InterviewSttService {

    /** 上传给上游的默认文件名，部分 OpenAI 兼容服务要求带扩展名以推断音频格式。 */
    private static final String DEFAULT_AUDIO_FILENAME = "speech.webm";

    /** 单次识别超时时间（毫秒）。语音面试单段音频仅几秒，20 秒足够覆盖网络抖动。 */
    private static final int STT_TIMEOUT_MS = 20000;

    /** 允许上传的单段音频最大字节数，防止异常大文件推高识别成本。默认 5MB。 */
    private static final long MAX_AUDIO_BYTES = 5L * 1024 * 1024;

    private final SysSttConfigService sysSttConfigService;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public boolean isCloudSttAvailable() {
        return sysSttConfigService.isCloudSttAvailable();
    }

    @Override
    public String transcribe(MultipartFile audio, String language) {
        // 运行时读取管理端在数据库中维护的系统级 STT 配置；未启用或未配置完整时直接判为不可用。
        ResolvedSttConfig config = sysSttConfigService.resolveEnabledConfig();
        if (config == null) {
            throw new BusinessException(ResultCode.CUSTOM_AI_CONFIG_INVALID, "云端语音识别未配置或未启用");
        }
        if (audio == null || audio.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "语音音频不能为空");
        }
        if (audio.getSize() > MAX_AUDIO_BYTES) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "语音音频过大，请缩短单次说话时长");
        }

        byte[] audioBytes = readAudioBytes(audio);

        try {
            String responseJson = createRestClient(config.getBaseUrl(), STT_TIMEOUT_MS)
                    .post()
                    .uri(resolveEndpointPath(config.getEndpointPath()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(buildMultipartBody(audio, audioBytes, config.getModel(), language))
                    .retrieve()
                    .body(String.class);
            return extractTranscribedText(responseJson);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.warn("云端语音识别失败, status: {}, model: {}",
                    ex.getStatusCode().value(), config.getModel());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "语音识别失败，请检查语音识别配置");
        } catch (Exception ex) {
            log.warn("云端语音识别异常, errorType: {}", ex.getClass().getSimpleName());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "语音识别失败，请稍后重试");
        }
    }

    private byte[] readAudioBytes(MultipartFile audio) {
        try {
            return audio.getBytes();
        } catch (IOException ex) {
            log.warn("读取上传语音音频失败, errorType: {}", ex.getClass().getSimpleName());
            throw new BusinessException(ResultCode.PARAM_ERROR, "语音音频读取失败");
        }
    }

    /**
     * 构造 OpenAI 兼容 /audio/transcriptions 的 multipart 请求体。
     * <p>
     * file 为音频字节，model 指向识别模型，language 为空时不发送、由上游自动判定。
     */
    private MultiValueMap<String, HttpEntity<?>> buildMultipartBody(MultipartFile audio, byte[] audioBytes, String model, String language) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        String filename = resolveFilename(audio.getOriginalFilename());
        Resource audioResource = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        builder.part("file", audioResource)
                .contentType(resolveAudioContentType(audio.getContentType()));
        builder.part("model", model);
        String normalizedLanguage = normalizeLanguage(language);
        if (normalizedLanguage != null) {
            builder.part("language", normalizedLanguage);
        }
        return builder.build();
    }

    /**
     * 解析识别响应文本。OpenAI 兼容格式为 {"text": "..."}。
     */
    private String extractTranscribedText(String responseJson) {
        if (responseJson == null || responseJson.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode textNode = root.path("text");
            if (textNode.isMissingNode()) {
                log.warn("云端语音识别响应缺少 text 字段, preview: {}", trimText(responseJson, 200));
                return "";
            }
            return textNode.asText("").trim();
        } catch (Exception ex) {
            log.warn("云端语音识别响应解析失败, errorType: {}", ex.getClass().getSimpleName());
            throw new BusinessException(ResultCode.CUSTOM_AI_CALL_FAILED, "语音识别响应解析失败");
        }
    }

    protected RestClient createRestClient(String baseUrl, int timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return restClientBuilder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 解析识别端点路径，为空或非法时回落到 OpenAI 标准 /audio/transcriptions。
     */
    private String resolveEndpointPath(String endpointPath) {
        String trimmed = trimToNull(endpointPath);
        if (trimmed == null || !trimmed.startsWith("/")) {
            return "/audio/transcriptions";
        }
        return trimmed;
    }

    private String resolveFilename(String originalFilename) {
        String trimmed = trimToNull(originalFilename);
        return trimmed == null ? DEFAULT_AUDIO_FILENAME : trimmed;
    }

    private MediaType resolveAudioContentType(String contentType) {
        String trimmed = trimToNull(contentType);
        if (trimmed == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.valueOf(trimmed);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * 归一化语言参数。auto 或空表示交给上游自动判定，返回 null 不发送 language 字段。
     */
    private String normalizeLanguage(String language) {
        String trimmed = trimToNull(language);
        if (trimmed == null || "auto".equalsIgnoreCase(trimmed)) {
            return null;
        }
        // 只取主语言子标签（zh-CN -> zh），OpenAI 兼容接口按 ISO-639-1 语言码识别。
        int dashIndex = trimmed.indexOf('-');
        return dashIndex > 0 ? trimmed.substring(0, dashIndex).toLowerCase(java.util.Locale.ROOT)
                : trimmed.toLowerCase(java.util.Locale.ROOT);
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
