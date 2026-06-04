package com.airesume.server.service.impl;

import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.user.ResolvedTtsConfig;
import com.airesume.server.entity.SysTtsConfig;
import com.airesume.server.entity.UserAiConfig;
import com.airesume.server.service.AiCredentialCrypto;
import com.airesume.server.service.SysTtsConfigService;
import com.airesume.server.service.UserAiConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.http.HttpMethod.POST;

class UserTtsSpeechServiceImplTest {

    @Test
    void shouldResolveInterviewTtsConfigBeforeDefaultFallback() {
        UserAiConfigService configService = mock(UserAiConfigService.class);
        AiCredentialCrypto crypto = mock(AiCredentialCrypto.class);
        UserTtsSpeechServiceImpl service = new UserTtsSpeechServiceImpl(
                configService, mock(SysTtsConfigService.class), crypto, RestClient.builder(), new ObjectMapper());
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
                configService, mock(SysTtsConfigService.class), crypto, RestClient.builder(), new ObjectMapper());
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
                configService, sysTtsConfigService, crypto, RestClient.builder(), new ObjectMapper());
        SysTtsConfig systemConfig = buildSystemTtsConfig();
        when(sysTtsConfigService.getEnabledConfigEntity()).thenReturn(systemConfig);
        when(crypto.decrypt("enc-system-tts")).thenReturn("system-real-key");

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
                configService, sysTtsConfigService, crypto, RestClient.builder(), new ObjectMapper());
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
    void shouldReturnUnavailableWhenTtsFieldsIncomplete() {
        UserAiConfigService configService = mock(UserAiConfigService.class);
        UserTtsSpeechServiceImpl service = new UserTtsSpeechServiceImpl(
                configService, mock(SysTtsConfigService.class), mock(AiCredentialCrypto.class), RestClient.builder(), new ObjectMapper());
        UserAiConfig incompleteConfig = buildTtsConfig("interview", "https://8.8.8.8/v1", "enc-interview", "", "alloy");
        when(configService.findEnabledConfig(10L, UserAiConstants.CONFIG_TYPE_INTERVIEW)).thenReturn(incompleteConfig);

        assertFalse(service.hasInterviewTtsConfig(10L));
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

    private UserAiConfig buildTtsConfig(String configType, String baseUrl, String apiKey, String model, String voiceId) {
        UserAiConfig config = new UserAiConfig();
        config.setConfigType(configType);
        config.setTtsBaseUrl(baseUrl);
        config.setTtsApiKey(apiKey);
        config.setTtsModel(model);
        config.setTtsVoiceId(voiceId);
        return config;
    }

    private SysTtsConfig buildSystemTtsConfig() {
        SysTtsConfig config = new SysTtsConfig();
        config.setEnabled(1);
        config.setBaseUrl("https://8.8.8.8/v1");
        config.setApiKey("enc-system-tts");
        config.setModel("tts-1");
        config.setVoiceId("alloy");
        config.setEndpointPath("/audio/speech");
        config.setTtsProvider("openai");
        return config;
    }

    private UserTtsSpeechServiceImpl buildService(UserAiConfigService configService,
                                                  AiCredentialCrypto crypto,
                                                  RestClient.Builder builder) {
        return new UserTtsSpeechServiceImpl(configService, mock(SysTtsConfigService.class), crypto, builder, new ObjectMapper()) {
            @Override
            protected RestClient createRestClient(String baseUrl, int timeoutMs) {
                return builder.clone()
                        .baseUrl(baseUrl)
                        .build();
            }
        };
    }
}
