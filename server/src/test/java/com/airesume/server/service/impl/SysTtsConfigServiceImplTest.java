package com.airesume.server.service.impl;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.admin.AdminTtsConfigRequest;
import com.airesume.server.dto.admin.AdminTtsConfigResponse;
import com.airesume.server.dto.user.ResolvedTtsConfig;
import com.airesume.server.dto.user.UserTtsConnectivityTestResponse;
import com.airesume.server.entity.SysTtsConfig;
import com.airesume.server.mapper.SysTtsConfigMapper;
import com.airesume.server.service.AiCredentialCrypto;
import com.airesume.server.service.TtsDiscoveryService;
import com.airesume.server.service.UserTtsConnectivityTestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysTtsConfigServiceImplTest {

    private SysTtsConfigMapper mapper;
    private AiCredentialCrypto crypto;
    private UserTtsConnectivityTestService connectivityTestService;
    private SysTtsConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SysTtsConfigMapper.class);
        crypto = mock(AiCredentialCrypto.class);
        connectivityTestService = mock(UserTtsConnectivityTestService.class);
        service = new SysTtsConfigServiceImpl(
                mapper,
                crypto,
                connectivityTestService,
                mock(TtsDiscoveryService.class));
    }

    @Test
    void shouldReturnMaskedCurrentConfigWithoutPlainApiKey() {
        SysTtsConfig config = buildConfig("enc-system-key", true);
        when(mapper.selectCurrent()).thenReturn(config);
        when(crypto.decrypt("enc-system-key")).thenReturn("system-secret-1234");

        AdminTtsConfigResponse response = service.getCurrentConfig();

        assertEquals(true, response.getEnabled());
        assertEquals(true, response.getConfigured());
        assertEquals("sys****1234", response.getApiKey());
        assertEquals("https://8.8.8.8/v1", response.getBaseUrl());
    }

    @Test
    void shouldSaveEnabledConfigWithEncryptedApiKey() {
        AdminTtsConfigRequest request = buildRequest();
        when(crypto.encrypt("system-real-key")).thenReturn("enc-system-key");
        when(crypto.decrypt("enc-system-key")).thenReturn("system-real-key");

        AdminTtsConfigResponse response = service.saveConfig(request);

        ArgumentCaptor<SysTtsConfig> captor = ArgumentCaptor.forClass(SysTtsConfig.class);
        verify(mapper).insert(captor.capture());
        assertEquals("enc-system-key", captor.getValue().getApiKey());
        assertEquals(1, captor.getValue().getEnabled());
        assertEquals(1, captor.getValue().getSingletonKey());
        assertEquals(true, response.getEnabled());
        assertEquals(true, response.getConfigured());
        assertEquals("sys****-key", response.getApiKey());
    }

    @Test
    void shouldReuseStoredApiKeyWhenSavingWithMaskedKey() {
        SysTtsConfig existing = buildConfig("enc-old-key", true);
        existing.setId(99L);
        AdminTtsConfigRequest request = buildRequest();
        request.setApiKey("sys****1234");
        when(mapper.selectCurrent()).thenReturn(existing);

        service.saveConfig(request);

        ArgumentCaptor<SysTtsConfig> captor = ArgumentCaptor.forClass(SysTtsConfig.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(99L, captor.getValue().getId());
        assertEquals("enc-old-key", captor.getValue().getApiKey());
    }

    @Test
    void shouldRejectEnabledConfigWithoutCompleteFields() {
        AdminTtsConfigRequest request = buildRequest();
        request.setApiKey("");

        assertThrows(BusinessException.class, () -> service.saveConfig(request));
    }

    @Test
    void shouldResolveEnabledSystemConfigWithDecryptedApiKey() {
        SysTtsConfig config = buildConfig("enc-system-key", true);
        when(mapper.selectEnabled()).thenReturn(config);
        when(crypto.decrypt("enc-system-key")).thenReturn("system-real-key");

        ResolvedTtsConfig resolved = service.resolveEnabledConfig();

        assertEquals("system", resolved.getSource());
        assertEquals("system", resolved.getConfigType());
        assertEquals("system-real-key", resolved.getApiKey());
        assertEquals("alloy", resolved.getVoiceId());
    }

    @Test
    void shouldNormalizeConnectivityRequestAndReuseStoredApiKey() {
        SysTtsConfig existing = buildConfig("enc-system-key", true);
        AdminTtsConfigRequest request = buildRequest();
        request.setApiKey("");
        when(mapper.selectCurrent()).thenReturn(existing);
        when(crypto.decrypt("enc-system-key")).thenReturn("system-real-key");
        when(connectivityTestService.testConnectivity(any()))
                .thenReturn(UserTtsConnectivityTestResponse.builder()
                        .success(true)
                        .message("TTS 连通测试成功")
                        .endpointPath("/audio/speech")
                        .build());

        service.testConnectivity(request);

        ArgumentCaptor<com.airesume.server.dto.user.UserTtsConnectivityTestRequest> captor =
                ArgumentCaptor.forClass(com.airesume.server.dto.user.UserTtsConnectivityTestRequest.class);
        verify(connectivityTestService).testConnectivity(captor.capture());
        assertEquals("system-real-key", captor.getValue().getApiKey());
        assertEquals("openai", captor.getValue().getTtsProvider());
    }

    private AdminTtsConfigRequest buildRequest() {
        AdminTtsConfigRequest request = new AdminTtsConfigRequest();
        request.setEnabled(true);
        request.setTtsProvider("openai");
        request.setBaseUrl("https://8.8.8.8/v1");
        request.setApiKey("system-real-key");
        request.setModel("tts-1");
        request.setVoiceId("alloy");
        request.setEndpointPath("/audio/speech");
        return request;
    }

    private SysTtsConfig buildConfig(String encryptedApiKey, boolean enabled) {
        SysTtsConfig config = new SysTtsConfig();
        config.setSingletonKey(1);
        config.setEnabled(enabled ? 1 : 0);
        config.setTtsProvider("openai");
        config.setBaseUrl("https://8.8.8.8/v1");
        config.setApiKey(encryptedApiKey);
        config.setModel("tts-1");
        config.setVoiceId("alloy");
        config.setEndpointPath("/audio/speech");
        return config;
    }
}
