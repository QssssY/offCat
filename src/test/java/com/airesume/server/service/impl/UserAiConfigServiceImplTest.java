package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.user.UserAiConfigResponse;
import com.airesume.server.dto.user.UserTtsConnectivityTestRequest;
import com.airesume.server.dto.user.UserTtsConnectivityTestResponse;
import com.airesume.server.entity.UserAiConfig;
import com.airesume.server.mapper.UserAiConfigMapper;
import com.airesume.server.service.AiCredentialCrypto;
import com.airesume.server.service.AiEngineConnectivityTestService;
import com.airesume.server.service.TtsDiscoveryService;
import com.airesume.server.service.UserTtsConnectivityTestService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserAiConfigServiceImplTest {

    private UserAiConfigMapper mapper;
    private UserAiConfigServiceImpl service;
    private UserTtsConnectivityTestService userTtsConnectivityTestService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                UserAiConfig.class);
        mapper = mock(UserAiConfigMapper.class);
        userTtsConnectivityTestService = mock(UserTtsConnectivityTestService.class);
        service = new UserAiConfigServiceImpl(
                mock(AiCredentialCrypto.class),
                mock(AiEngineConnectivityTestService.class),
                userTtsConnectivityTestService,
                mock(TtsDiscoveryService.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void shouldPhysicallyDeleteActiveUserAiConfig() {
        when(mapper.deleteActiveConfig(7L, "resume")).thenReturn(1);

        service.deleteUserConfig(7L, "resume");

        verify(mapper).deleteActiveConfig(7L, "resume");
    }

    @Test
    void shouldRejectDeletingMissingUserAiConfig() {
        when(mapper.deleteActiveConfig(7L, "resume")).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.deleteUserConfig(7L, "resume"));
    }

    @Test
    void shouldMaskTtsApiKeyWhenBuildingUserAiConfigResponse() {
        AiCredentialCrypto crypto = mock(AiCredentialCrypto.class);
        when(crypto.decrypt("enc-ai")).thenReturn("sk-real-1234");
        when(crypto.decrypt("enc-tts")).thenReturn("tts-secret-5678");
        service = new UserAiConfigServiceImpl(crypto, mock(AiEngineConnectivityTestService.class),
                userTtsConnectivityTestService, mock(TtsDiscoveryService.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        UserAiConfig config = new UserAiConfig();
        config.setConfigType("interview");
        config.setProviderName("面试模型");
        config.setBaseUrl("https://api.example.com/v1");
        config.setApiKey("enc-ai");
        config.setModel("gpt-4o-mini");
        config.setIsEnabled(1);
        config.setSupportsMultimodal(0);
        config.setTtsBaseUrl("https://tts.example.com/v1");
        config.setTtsApiKey("enc-tts");
        config.setTtsModel("tts-1");
        config.setTtsVoiceId("alloy");

        UserAiConfigResponse response = ReflectionTestUtils.invokeMethod(service, "buildResponse", config);

        assertEquals("https://tts.example.com/v1", response.getTtsBaseUrl());
        assertEquals("tts****5678", response.getTtsApiKey());
        assertEquals("tts-1", response.getTtsModel());
        assertEquals("alloy", response.getTtsVoiceId());
        assertEquals(true, response.getTtsConfigured());
    }

    @Test
    void shouldExposeTtsFieldsForDefaultFallbackConfigResponse() {
        AiCredentialCrypto crypto = mock(AiCredentialCrypto.class);
        when(crypto.decrypt("enc-ai")).thenReturn("sk-real-1234");
        when(crypto.decrypt("enc-tts")).thenReturn("tts-secret-5678");
        service = new UserAiConfigServiceImpl(crypto, mock(AiEngineConnectivityTestService.class),
                userTtsConnectivityTestService, mock(TtsDiscoveryService.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        UserAiConfig config = new UserAiConfig();
        config.setConfigType("default");
        config.setProviderName("通用模型");
        config.setBaseUrl("https://api.example.com/v1");
        config.setApiKey("enc-ai");
        config.setModel("gpt-4o-mini");
        config.setIsEnabled(1);
        config.setSupportsMultimodal(0);
        config.setTtsBaseUrl("https://tts.example.com/v1");
        config.setTtsApiKey("enc-tts");
        config.setTtsModel("tts-1");
        config.setTtsVoiceId("alloy");

        UserAiConfigResponse response = ReflectionTestUtils.invokeMethod(service, "buildResponse", config);

        assertEquals("https://tts.example.com/v1", response.getTtsBaseUrl());
        assertEquals("tts****5678", response.getTtsApiKey());
        assertEquals("tts-1", response.getTtsModel());
        assertEquals("alloy", response.getTtsVoiceId());
        assertEquals(true, response.getTtsConfigured());
    }

    @Test
    void shouldHideTtsFieldsForNonInterviewConfigResponse() {
        AiCredentialCrypto crypto = mock(AiCredentialCrypto.class);
        when(crypto.decrypt("enc-ai")).thenReturn("sk-real-1234");
        service = new UserAiConfigServiceImpl(crypto, mock(AiEngineConnectivityTestService.class),
                userTtsConnectivityTestService, mock(TtsDiscoveryService.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        UserAiConfig config = new UserAiConfig();
        config.setConfigType("resume");
        config.setProviderName("简历模型");
        config.setBaseUrl("https://api.example.com/v1");
        config.setApiKey("enc-ai");
        config.setModel("gpt-4o-mini");
        config.setIsEnabled(1);
        config.setSupportsMultimodal(1);
        config.setTtsBaseUrl("https://tts.example.com/v1");
        config.setTtsApiKey("enc-tts");
        config.setTtsModel("tts-1");
        config.setTtsVoiceId("alloy");

        UserAiConfigResponse response = ReflectionTestUtils.invokeMethod(service, "buildResponse", config);

        assertEquals(null, response.getTtsBaseUrl());
        assertEquals(null, response.getTtsApiKey());
        assertEquals(null, response.getTtsModel());
        assertEquals(null, response.getTtsVoiceId());
        assertEquals(false, response.getTtsConfigured());
    }

    @Test
    void shouldClearTtsFieldsWhenSavingResumeConfig() {
        UserAiConfig config = new UserAiConfig();
        config.setConfigType("resume");
        config.setTtsBaseUrl("https://tts.example.com/v1");
        config.setTtsApiKey("enc-tts");
        config.setTtsModel("tts-1");
        config.setTtsVoiceId("alloy");

        com.airesume.server.dto.user.UserAiConfigRequest request =
                new com.airesume.server.dto.user.UserAiConfigRequest();
        request.setTtsBaseUrl("https://tts.example.com/v1");
        request.setTtsApiKey("tts-real-key");
        request.setTtsModel("tts-1");
        request.setTtsVoiceId("alloy");

        ReflectionTestUtils.invokeMethod(service, "applyTtsFields", config, request);

        assertEquals(null, config.getTtsBaseUrl());
        assertEquals(null, config.getTtsApiKey());
        assertEquals(null, config.getTtsModel());
        assertEquals(null, config.getTtsVoiceId());
        verifyNoInteractions(userTtsConnectivityTestService);
    }

    @Test
    void shouldKeepTtsFieldsWhenSavingDefaultFallbackConfig() {
        AiCredentialCrypto crypto = mock(AiCredentialCrypto.class);
        when(crypto.encrypt("tts-real-key")).thenReturn("enc-tts");
        service = new UserAiConfigServiceImpl(crypto, mock(AiEngineConnectivityTestService.class),
                userTtsConnectivityTestService, mock(TtsDiscoveryService.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        UserAiConfig config = new UserAiConfig();
        config.setConfigType("default");

        com.airesume.server.dto.user.UserAiConfigRequest request =
                new com.airesume.server.dto.user.UserAiConfigRequest();
        request.setTtsBaseUrl("https://8.8.8.8/v1");
        request.setTtsApiKey("tts-real-key");
        request.setTtsModel("tts-1");
        request.setTtsVoiceId("alloy");
        request.setTtsEndpointPath("/v1/tts");
        request.setTtsProvider("openai");
        when(userTtsConnectivityTestService.testConnectivity(org.mockito.ArgumentMatchers.any()))
                .thenReturn(UserTtsConnectivityTestResponse.builder()
                        .success(true)
                        .message("TTS 连通测试成功")
                        .endpointPath("/v1/tts")
                        .build());

        ReflectionTestUtils.invokeMethod(service, "applyTtsFields", config, request);

        assertEquals("https://8.8.8.8/v1", config.getTtsBaseUrl());
        assertEquals("enc-tts", config.getTtsApiKey());
        assertEquals("tts-1", config.getTtsModel());
        assertEquals("alloy", config.getTtsVoiceId());
        assertEquals("/v1/tts", config.getTtsEndpointPath());
        assertEquals("openai", config.getTtsProvider());
    }

    @Test
    void shouldExposeOpenAiCompatibleTtsConnectivityTest() {
        UserTtsConnectivityTestRequest request = new UserTtsConnectivityTestRequest();
        request.setBaseUrl("https://8.8.8.8/v1");
        request.setApiKey("tts-real-key");
        request.setModel("tts-1");
        request.setVoiceId("alloy");
        when(userTtsConnectivityTestService.testConnectivity(org.mockito.ArgumentMatchers.any()))
                .thenReturn(UserTtsConnectivityTestResponse.builder()
                        .success(true)
                        .message("TTS 连通测试成功")
                        .endpointPath("/audio/speech")
                        .build());

        UserTtsConnectivityTestResponse response = service.testTtsConnectivity(request);

        assertEquals(true, response.getSuccess());
        assertEquals("/audio/speech", response.getEndpointPath());
    }

    @Test
    void shouldPreserveTtsEndpointPathWhenTestingTtsConnectivity() {
        UserTtsConnectivityTestRequest request = new UserTtsConnectivityTestRequest();
        request.setBaseUrl("https://8.8.8.8/v1");
        request.setApiKey("tts-real-key");
        request.setModel("custom-tts");
        request.setVoiceId("speaker-a");
        request.setEndpointPath("/v1/tts");
        request.setTtsProvider("openai");
        when(userTtsConnectivityTestService.testConnectivity(org.mockito.ArgumentMatchers.any()))
                .thenReturn(UserTtsConnectivityTestResponse.builder()
                        .success(true)
                        .message("TTS 连通测试成功")
                        .endpointPath("/v1/tts")
                        .build());

        UserTtsConnectivityTestResponse response = service.testTtsConnectivity(request);

        ArgumentCaptor<UserTtsConnectivityTestRequest> captor =
                ArgumentCaptor.forClass(UserTtsConnectivityTestRequest.class);
        verify(userTtsConnectivityTestService).testConnectivity(captor.capture());
        assertEquals(true, response.getSuccess());
        assertEquals("/v1/tts", captor.getValue().getEndpointPath());
        assertEquals("openai", captor.getValue().getTtsProvider());
    }
}
