package com.airesume.server.service.impl;

import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.user.ResolvedTtsConfig;
import com.airesume.server.dto.user.TtsAudioResult;
import com.airesume.server.entity.UserAiConfig;
import com.airesume.server.service.AiCredentialCrypto;
import com.airesume.server.service.EdgeTtsClient;
import com.airesume.server.service.SysTtsConfigService;
import com.airesume.server.service.UserAiConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

class UserTtsSpeechServiceImplTest {

    @Test
    void shouldResolveInterviewTtsConfigBeforeDefaultFallback() {
        UserAiConfigService configService = mock(UserAiConfigService.class);
        AiCredentialCrypto crypto = mock(AiCredentialCrypto.class);
        UserTtsSpeechServiceImpl service = new UserTtsSpeechServiceImpl(
                configService, mock(SysTtsConfigService.class), crypto, RestClient.builder(), new ObjectMapper(),
                mock(EdgeTtsClient.class));
        UserAiConfig interviewConfig = buildTtsConfig("interview", "https://8.8.8.8/v1", "enc-interview", "tts-1", "alloy");
        UserAiConfig defaultConfig = buildTtsConfig("default", "https://1.1.1.1/v1", "enc-default", "tts-1", "nova");
        when(configService.findEnabledConfig(7L, UserAiConstants.CONFIG_TYPE_INTERVIEW)).thenReturn(interviewConfig);
        when(configService.findEnabledConfig(7L, UserAiConstants.CONFIG_TYPE_DEFAULT)).thenReturn(defaultConfig);
        when(crypto.decrypt("enc-interview")).thenReturn("tts-real-key");

        ResolvedTtsConfig resolved = service.resolveInterviewTtsConfig(7L);

        assertEquals("interview", resolved.getConfigType());
        assertEquals("https://8.8.8.8/v1", resolved.getBaseUrl());
        assertEquals("tts-real-key", resolved.getApiKey());
        assertEquals("alloy", resolved.getVoiceId());
        verify(configService, never()).findEnabledConfig(7L, UserAiConstants.CONFIG_TYPE_RESUME);
    }

    @Test
    void shouldFallbackToDefaultTtsConfigWhenInterviewMissing() {
        UserAiConfigService configService = mock(UserAiConfigService.class);
        AiCredentialCrypto crypto = mock(AiCredentialCrypto.class);
        UserTtsSpeechServiceImpl service = new UserTtsSpeechServiceImpl(
                configService, mock(SysTtsConfigService.class), crypto, RestClient.builder(), new ObjectMapper(),
                mock(EdgeTtsClient.class));
        UserAiConfig defaultConfig = buildTtsConfig("default", "https://1.1.1.1/v1", "enc-default", "tts-1", "nova");
        when(configService.findEnabledConfig(9L, UserAiConstants.CONFIG_TYPE_DEFAULT)).thenReturn(defaultConfig);
        when(crypto.decrypt("enc-default")).thenReturn("default-tts-key");

        ResolvedTtsConfig resolved = service.resolveInterviewTtsConfig(9L);

        assertEquals("default", resolved.getConfigType());
        assertEquals("default-tts-key", resolved.getApiKey());
        assertEquals("nova", resolved.getVoiceId());
    }

    @Test
    void shouldFallbackToSystemTtsConfigWhenUserTtsMissing() {
        UserAiConfigService configService = mock(UserAiConfigService.class);
        SysTtsConfigService sysTtsConfigService = mock(SysTtsConfigService.class);
        AiCredentialCrypto crypto = mock(AiCredentialCrypto.class);
        UserTtsSpeechServiceImpl service = new UserTtsSpeechServiceImpl(
                configService, sysTtsConfigService, crypto, RestClient.builder(), new ObjectMapper(),
                mock(EdgeTtsClient.class));
        ResolvedTtsConfig systemConfig = ResolvedTtsConfig.builder()
                .source("system")
                .configType("system")
                .baseUrl("https://8.8.8.8/v1")
                .apiKey("system-real-key")
                .model("tts-1")
                .voiceId("alloy")
                .endpointPath("/audio/speech")
                .ttsProvider("openai")
                .build();
        when(sysTtsConfigService.resolveEnabledConfig()).thenReturn(systemConfig);

        ResolvedTtsConfig resolved = service.resolveInterviewTtsConfig(13L);

        assertEquals("system", resolved.getSource());
        assertEquals("system", resolved.getConfigType());
        assertEquals("https://8.8.8.8/v1", resolved.getBaseUrl());
        assertEquals("system-real-key", resolved.getApiKey());
    }

    @Test
    void shouldPreferUserCustomTtsBeforeSystemTtsConfig() {
        UserAiConfigService configService = mock(UserAiConfigService.class);
        SysTtsConfigService sysTtsConfigService = mock(SysTtsConfigService.class);
        AiCredentialCrypto crypto = mock(AiCredentialCrypto.class);
        UserTtsSpeechServiceImpl service = new UserTtsSpeechServiceImpl(
                configService, sysTtsConfigService, crypto, RestClient.builder(), new ObjectMapper(),
                mock(EdgeTtsClient.class));
        UserAiConfig interviewConfig = buildTtsConfig("interview", "https://8.8.8.8/v1", "enc-interview", "tts-1", "nova");
        when(configService.findEnabledConfig(14L, UserAiConstants.CONFIG_TYPE_INTERVIEW)).thenReturn(interviewConfig);
        when(crypto.decrypt("enc-interview")).thenReturn("user-tts-key");

        ResolvedTtsConfig resolved = service.resolveInterviewTtsConfig(14L);

        assertEquals("user_custom", resolved.getSource());
        assertEquals("interview", resolved.getConfigType());
        assertEquals("user-tts-key", resolved.getApiKey());
        verify(sysTtsConfigService, never()).resolveEnabledConfig();
    }

    @Test
    void shouldFallbackToBuiltinEdgeWhenUserTtsFieldsIncompleteAndNoSystemConfig() {
        UserAiConfigService configService = mock(UserAiConfigService.class);
        SysTtsConfigService sysTtsConfigService = mock(SysTtsConfigService.class);
        UserTtsSpeechServiceImpl service = new UserTtsSpeechServiceImpl(
                configService, sysTtsConfigService, mock(AiCredentialCrypto.class), RestClient.builder(),
                new ObjectMapper(), mock(EdgeTtsClient.class));
        UserAiConfig incompleteConfig = buildTtsConfig("interview", "https://8.8.8.8/v1", "enc-interview", "", "alloy");
        when(configService.findEnabledConfig(10L, UserAiConstants.CONFIG_TYPE_INTERVIEW)).thenReturn(incompleteConfig);
        when(sysTtsConfigService.resolveEnabledConfig()).thenReturn(null);

        // 用户配置不完整且系统未配置时，回落到内置 EdgeTTS 晓晓，保证语音面试开箱即用地云端播报。
        ResolvedTtsConfig resolved = service.resolveInterviewTtsConfig(10L);

        assertTrue(service.hasInterviewTtsConfig(10L));
        assertEquals("builtin", resolved.getSource());
        assertEquals("edge", resolved.getTtsProvider());
        assertEquals("zh-CN-XiaoxiaoNeural", resolved.getVoiceId());
    }

    @Test
    void shouldOverrideEdgeVoiceWithWhitelistedRequestedVoice() {
        UserAiConfigService configService = mock(UserAiConfigService.class);
        SysTtsConfigService sysTtsConfigService = mock(SysTtsConfigService.class);
        EdgeTtsClient edgeTtsClient = mock(EdgeTtsClient.class);
        UserTtsSpeechServiceImpl service = new UserTtsSpeechServiceImpl(
                configService, sysTtsConfigService, mock(AiCredentialCrypto.class),
                RestClient.builder(), new ObjectMapper(), edgeTtsClient);
        when(configService.findEnabledConfig(20L, UserAiConstants.CONFIG_TYPE_INTERVIEW)).thenReturn(null);
        when(configService.findEnabledConfig(20L, UserAiConstants.CONFIG_TYPE_DEFAULT)).thenReturn(null);
        when(sysTtsConfigService.resolveEnabledConfig()).thenReturn(null);
        when(edgeTtsClient.synthesize("你好", "zh-CN-YunxiNeural", Duration.ofMillis(15000)))
                .thenReturn(new byte[]{5, 6, 7});

        // 内置 EdgeTTS 兜底默认晓晓，但前端选择的白名单音色（云希）应逐请求覆盖。
        TtsAudioResult result = service.synthesizeInterviewSpeechAudio(20L, "你好", "zh-CN-YunxiNeural");

        assertArrayEquals(new byte[]{5, 6, 7}, result.getAudioBytes());
        verify(edgeTtsClient).synthesize("你好", "zh-CN-YunxiNeural", Duration.ofMillis(15000));
    }

    @Test
    void shouldIgnoreNonWhitelistedRequestedVoiceForEdge() {
        UserAiConfigService configService = mock(UserAiConfigService.class);
        SysTtsConfigService sysTtsConfigService = mock(SysTtsConfigService.class);
        EdgeTtsClient edgeTtsClient = mock(EdgeTtsClient.class);
        UserTtsSpeechServiceImpl service = new UserTtsSpeechServiceImpl(
                configService, sysTtsConfigService, mock(AiCredentialCrypto.class),
                RestClient.builder(), new ObjectMapper(), edgeTtsClient);
        when(configService.findEnabledConfig(21L, UserAiConstants.CONFIG_TYPE_INTERVIEW)).thenReturn(null);
        when(configService.findEnabledConfig(21L, UserAiConstants.CONFIG_TYPE_DEFAULT)).thenReturn(null);
        when(sysTtsConfigService.resolveEnabledConfig()).thenReturn(null);
        when(edgeTtsClient.synthesize("你好", "zh-CN-XiaoxiaoNeural", Duration.ofMillis(15000)))
                .thenReturn(new byte[]{1});

        // 非白名单音色（可能被注入的任意字符串）必须忽略，回退到默认晓晓，避免恶意 SSML voice 透传上游。
        service.synthesizeInterviewSpeechAudio(21L, "你好", "evil-voice");

        verify(edgeTtsClient).synthesize("你好", "zh-CN-XiaoxiaoNeural", Duration.ofMillis(15000));
    }

    @Test
    void shouldSynthesizeSpeechWithOpenAiCompatibleAudioEndpoint() {
        UserAiConfigService configService = mock(UserAiConfigService.class);
        AiCredentialCrypto crypto = mock(AiCredentialCrypto.class);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UserTtsSpeechServiceImpl service = buildService(configService, crypto, builder);
        UserAiConfig config = buildTtsConfig("interview", "https://8.8.8.8/v1", "enc-tts", "tts-1", "alloy");
        when(configService.findEnabledConfig(11L, UserAiConstants.CONFIG_TYPE_INTERVIEW)).thenReturn(config);
        when(crypto.decrypt("enc-tts")).thenReturn("tts-real-key");
        byte[] audio = new byte[]{1, 2, 3};

        server.expect(once(), requestTo("https://8.8.8.8/v1/audio/speech"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer tts-real-key"))
                .andExpect(jsonPath("$.model").value("tts-1"))
                .andExpect(jsonPath("$.voice").value("alloy"))
                .andExpect(jsonPath("$.input").value("你好，请介绍一下自己。"))
                .andExpect(jsonPath("$.response_format").value("mp3"))
                .andRespond(withSuccess(audio, MediaType.valueOf("audio/mpeg")));

        byte[] result = service.synthesizeInterviewSpeech(11L, "你好，请介绍一下自己。");

        assertArrayEquals(audio, result);
        server.verify();
    }

    @Test
    void shouldRejectUpstreamTtsFailureWithoutLeakingProviderConfig() {
        UserAiConfigService configService = mock(UserAiConfigService.class);
        AiCredentialCrypto crypto = mock(AiCredentialCrypto.class);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UserTtsSpeechServiceImpl service = buildService(configService, crypto, builder);
        UserAiConfig config = buildTtsConfig("interview", "https://8.8.8.8/v1", "enc-tts", "tts-1", "alloy");
        when(configService.findEnabledConfig(12L, UserAiConstants.CONFIG_TYPE_INTERVIEW)).thenReturn(config);
        when(crypto.decrypt("enc-tts")).thenReturn("tts-real-key");
        server.expect(once(), requestTo("https://8.8.8.8/v1/audio/speech"))
                .andRespond(withServerError());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.synthesizeInterviewSpeech(12L, "请继续。"));

        assertTrue(ex.getMessage().contains("TTS"));
        assertFalse(ex.getMessage().contains("tts-real-key"));
        assertFalse(ex.getMessage().contains("8.8.8.8"));
    }

    @Test
    void shouldSynthesizeGeminiSpeechAsWavAudio() {
        UserAiConfigService configService = mock(UserAiConfigService.class);
        AiCredentialCrypto crypto = mock(AiCredentialCrypto.class);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UserTtsSpeechServiceImpl service = buildService(configService, crypto, builder);
        UserAiConfig config = buildTtsConfig("interview", "https://generativelanguage.googleapis.com",
                "enc-gemini", "gemini-2.5-flash-preview-tts", "Kore");
        config.setTtsProvider("gemini");
        config.setTtsEndpointPath("/v1beta/models/{model}:generateContent");
        when(configService.findEnabledConfig(16L, UserAiConstants.CONFIG_TYPE_INTERVIEW)).thenReturn(config);
        when(crypto.decrypt("enc-gemini")).thenReturn("gemini-real-key");
        String pcmData = Base64.getEncoder().encodeToString(new byte[]{1, 0, 2, 0});
        server.expect(once(), requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent"))
                .andExpect(method(POST))
                .andExpect(header("x-goog-api-key", "gemini-real-key"))
                .andExpect(jsonPath("$.generationConfig.responseModalities[0]").value("AUDIO"))
                .andExpect(jsonPath("$.generationConfig.speechConfig.voiceConfig.prebuiltVoiceConfig.voiceName").value("Kore"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("请继续。"))
                .andRespond(withSuccess(
                        "{\"candidates\":[{\"content\":{\"parts\":[{\"inlineData\":{\"mimeType\":\"audio/L16;codec=pcm;rate=24000\",\"data\":\""
                                + pcmData + "\"}}]}}]}",
                        MediaType.APPLICATION_JSON));

        TtsAudioResult result = service.synthesizeInterviewSpeechAudio(16L, "请继续。");

        assertEquals("audio/wav", result.getContentType());
        assertEquals('R', result.getAudioBytes()[0]);
        assertEquals('I', result.getAudioBytes()[1]);
        server.verify();
    }

    @Test
    void shouldSynthesizeMiniMaxSpeechFromHexAudio() {
        UserAiConfigService configService = mock(UserAiConfigService.class);
        AiCredentialCrypto crypto = mock(AiCredentialCrypto.class);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UserTtsSpeechServiceImpl service = buildService(configService, crypto, builder);
        UserAiConfig config = buildTtsConfig("interview", "https://api.minimax.chat",
                "enc-minimax", "speech-02-turbo", "male-qn-qingse");
        config.setTtsProvider("minimax");
        config.setTtsEndpointPath("/v1/t2a_v2");
        when(configService.findEnabledConfig(17L, UserAiConstants.CONFIG_TYPE_INTERVIEW)).thenReturn(config);
        when(crypto.decrypt("enc-minimax")).thenReturn("mini-real-key");
        server.expect(once(), requestTo("https://api.minimax.chat/v1/t2a_v2"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer mini-real-key"))
                .andExpect(jsonPath("$.model").value("speech-02-turbo"))
                .andExpect(jsonPath("$.text").value("你好，请介绍一下自己。"))
                .andExpect(jsonPath("$.voice_setting.voice_id").value("male-qn-qingse"))
                .andRespond(withSuccess("{\"data\":{\"audio\":\"010203\"}}", MediaType.APPLICATION_JSON));

        TtsAudioResult result = service.synthesizeInterviewSpeechAudio(17L, "你好，请介绍一下自己。");

        assertEquals("audio/mpeg", result.getContentType());
        assertArrayEquals(new byte[]{1, 2, 3}, result.getAudioBytes());
        server.verify();
    }

    @Test
    void shouldSynthesizeXaiSpeechWithoutModelField() {
        UserAiConfigService configService = mock(UserAiConfigService.class);
        AiCredentialCrypto crypto = mock(AiCredentialCrypto.class);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UserTtsSpeechServiceImpl service = buildService(configService, crypto, builder);
        UserAiConfig config = buildTtsConfig("interview", "https://api.x.ai",
                "enc-xai", "grok-tts", "Fritz-PlayAI");
        config.setTtsProvider("xai");
        config.setTtsEndpointPath("/v1/tts");
        when(configService.findEnabledConfig(18L, UserAiConstants.CONFIG_TYPE_INTERVIEW)).thenReturn(config);
        when(crypto.decrypt("enc-xai")).thenReturn("xai-real-key");
        byte[] audio = new byte[]{8, 7, 6};
        server.expect(once(), requestTo("https://api.x.ai/v1/tts"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer xai-real-key"))
                .andExpect(jsonPath("$.model").doesNotExist())
                .andExpect(jsonPath("$.input").value("请继续。"))
                .andExpect(jsonPath("$.voice").value("Fritz-PlayAI"))
                .andRespond(withSuccess(audio, MediaType.valueOf("audio/mpeg")));

        TtsAudioResult result = service.synthesizeInterviewSpeechAudio(18L, "请继续。");

        assertEquals("audio/mpeg", result.getContentType());
        assertArrayEquals(audio, result.getAudioBytes());
        server.verify();
    }

    @Test
    void shouldSynthesizeQwenSpeechAsWavFromOfficialHttpOssUrl() {
        UserAiConfigService configService = mock(UserAiConfigService.class);
        AiCredentialCrypto crypto = mock(AiCredentialCrypto.class);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UserTtsSpeechServiceImpl service = buildService(configService, crypto, builder);
        UserAiConfig config = buildTtsConfig("interview", "https://dashscope.aliyuncs.com",
                "enc-qwen", "qwen3-tts-flash", "Cherry");
        config.setTtsProvider("qwen");
        config.setTtsEndpointPath("/api/v1/services/aigc/multimodal-generation/generation");
        when(configService.findEnabledConfig(19L, UserAiConstants.CONFIG_TYPE_INTERVIEW)).thenReturn(config);
        when(crypto.decrypt("enc-qwen")).thenReturn("qwen-real-key");
        byte[] audio = new byte[]{82, 73, 70, 70};
        String audioUrl = "http://dashscope-result-bj.oss-cn-beijing.aliyuncs.com/qwen/demo.wav";
        server.expect(once(), requestTo("https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer qwen-real-key"))
                .andExpect(jsonPath("$.model").value("qwen3-tts-flash"))
                .andExpect(jsonPath("$.input.text").value("hello"))
                .andExpect(jsonPath("$.input.voice").value("Cherry"))
                .andRespond(withSuccess("{\"output\":{\"audio\":{\"url\":\"" + audioUrl + "\"}}}",
                        MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(audioUrl))
                .andExpect(method(GET))
                .andRespond(withSuccess(audio, MediaType.valueOf("audio/wav")));

        TtsAudioResult result = service.synthesizeInterviewSpeechAudio(19L, "hello");

        assertEquals("audio/wav", result.getContentType());
        assertArrayEquals(audio, result.getAudioBytes());
        server.verify();
    }

    @Test
    void shouldSynthesizeSpeechWithEdgeReadAloudProvider() {
        UserAiConfigService configService = mock(UserAiConfigService.class);
        AiCredentialCrypto crypto = mock(AiCredentialCrypto.class);
        EdgeTtsClient edgeTtsClient = mock(EdgeTtsClient.class);
        UserTtsSpeechServiceImpl service = new UserTtsSpeechServiceImpl(
                configService, mock(SysTtsConfigService.class), crypto,
                RestClient.builder(), new ObjectMapper(), edgeTtsClient);
        UserAiConfig config = buildTtsConfig("interview", "https://speech.platform.bing.com",
                null, "edge-tts", "zh-CN-XiaoxiaoNeural");
        config.setTtsProvider("edge");
        when(configService.findEnabledConfig(15L, UserAiConstants.CONFIG_TYPE_INTERVIEW)).thenReturn(config);
        when(edgeTtsClient.synthesize("你好，请介绍一下自己。", "zh-CN-XiaoxiaoNeural",
                Duration.ofMillis(15000))).thenReturn(new byte[]{9, 8, 7});

        byte[] result = service.synthesizeInterviewSpeech(15L, "你好，请介绍一下自己。");

        assertArrayEquals(new byte[]{9, 8, 7}, result);
        verify(crypto, never()).decrypt(null);
        verify(edgeTtsClient).synthesize("你好，请介绍一下自己。", "zh-CN-XiaoxiaoNeural",
                Duration.ofMillis(15000));
    }

    private UserAiConfig buildTtsConfig(String configType, String baseUrl, String apiKey, String model, String voiceId) {
        UserAiConfig config = new UserAiConfig();
        config.setConfigType(configType);
        config.setTtsBaseUrl(baseUrl);
        config.setTtsApiKey(apiKey);
        config.setTtsModel(model);
        config.setTtsVoiceId(voiceId);
        return config;
    }

    private UserTtsSpeechServiceImpl buildService(UserAiConfigService configService,
                                                  AiCredentialCrypto crypto,
                                                  RestClient.Builder builder) {
        return new UserTtsSpeechServiceImpl(configService, mock(SysTtsConfigService.class), crypto,
                builder, new ObjectMapper(), mock(EdgeTtsClient.class)) {
            @Override
            protected RestClient createRestClient(String baseUrl, int timeoutMs) {
                return builder.clone()
                        .baseUrl(baseUrl)
                        .build();
            }

            @Override
            protected RestClient createStandaloneRestClient(int timeoutMs) {
                return builder.clone().build();
            }
        };
    }
}
