package com.airesume.server.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.admin.AdminSttConfigRequest;
import com.airesume.server.dto.admin.AdminSttConfigResponse;
import com.airesume.server.dto.user.ResolvedSttConfig;
import com.airesume.server.entity.SysSttConfig;
import com.airesume.server.mapper.SysSttConfigMapper;
import com.airesume.server.service.AiCredentialCrypto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * SysSttConfigServiceImpl 单元测试。
 */
class SysSttConfigServiceImplTest {

    private SysSttConfigMapper mapper;
    private AiCredentialCrypto crypto;
    private SysSttConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SysSttConfigMapper.class);
        crypto = mock(AiCredentialCrypto.class);
        service = new SysSttConfigServiceImpl(mapper, crypto, RestClient.builder());
    }

    @Test
    void getCurrentConfigMasksApiKey() {
        SysSttConfig config = buildConfig("enc-key", 1);
        when(mapper.selectCurrent()).thenReturn(config);
        when(crypto.decrypt("enc-key")).thenReturn("secret-abcd-1234");

        AdminSttConfigResponse response = service.getCurrentConfig();

        assertTrue(response.getEnabled());
        assertTrue(response.getConfigured());
        assertEquals("sec****1234", response.getApiKey());
        assertEquals("https://api.siliconflow.cn/v1", response.getBaseUrl());
    }

    @Test
    void getCurrentConfigReturnsDefaultsWhenMissing() {
        when(mapper.selectCurrent()).thenReturn(null);

        AdminSttConfigResponse response = service.getCurrentConfig();

        assertFalse(response.getEnabled());
        assertFalse(response.getConfigured());
        assertNull(response.getApiKey());
        assertEquals("/audio/transcriptions", response.getEndpointPath());
    }

    @Test
    void saveEncryptsPlainApiKey() {
        AdminSttConfigRequest request = new AdminSttConfigRequest();
        request.setEnabled(true);
        request.setBaseUrl("https://api.siliconflow.cn/v1");
        request.setApiKey("real-plain-key");
        request.setModel("FunAudioLLM/SenseVoiceSmall");
        when(mapper.selectCurrent()).thenReturn(null);
        when(crypto.encrypt("real-plain-key")).thenReturn("enc-key");
        when(crypto.decrypt("enc-key")).thenReturn("real-plain-key");

        service.saveConfig(request);

        verify(crypto).encrypt("real-plain-key");
        verify(mapper).insert(any(SysSttConfig.class));
    }

    @Test
    void saveEnabledWithoutApiKeyThrows() {
        AdminSttConfigRequest request = new AdminSttConfigRequest();
        request.setEnabled(true);
        request.setBaseUrl("https://api.siliconflow.cn/v1");
        request.setModel("FunAudioLLM/SenseVoiceSmall");
        when(mapper.selectCurrent()).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.saveConfig(request));
    }

    @Test
    void resolveEnabledConfigDecryptsApiKey() {
        SysSttConfig config = buildConfig("enc-key", 1);
        when(mapper.selectEnabled()).thenReturn(config);
        when(crypto.decrypt("enc-key")).thenReturn("secret-abcd-1234");

        ResolvedSttConfig resolved = service.resolveEnabledConfig();

        assertEquals("https://api.siliconflow.cn/v1", resolved.getBaseUrl());
        assertEquals("secret-abcd-1234", resolved.getApiKey());
        assertEquals("FunAudioLLM/SenseVoiceSmall", resolved.getModel());
    }

    @Test
    void resolveEnabledConfigReturnsNullWhenDisabled() {
        when(mapper.selectEnabled()).thenReturn(null);
        assertNull(service.resolveEnabledConfig());
    }

    @Test
    void isCloudSttAvailableReflectsEnabledConfig() {
        SysSttConfig config = buildConfig("enc-key", 1);
        when(mapper.selectEnabled()).thenReturn(config);
        when(crypto.decrypt("enc-key")).thenReturn("secret-abcd-1234");

        assertTrue(service.isCloudSttAvailable());
    }

    private SysSttConfig buildConfig(String encryptedApiKey, int enabled) {
        SysSttConfig config = new SysSttConfig();
        config.setSingletonKey(1);
        config.setBaseUrl("https://api.siliconflow.cn/v1");
        config.setApiKey(encryptedApiKey);
        config.setModel("FunAudioLLM/SenseVoiceSmall");
        config.setEndpointPath("/audio/transcriptions");
        config.setEnabled(enabled);
        return config;
    }
}
