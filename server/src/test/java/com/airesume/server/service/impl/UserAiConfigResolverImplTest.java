package com.airesume.server.service.impl;

import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.entity.UserAiConfig;
import com.airesume.server.service.AiCredentialCrypto;
import com.airesume.server.service.UserAiConfigService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserAiConfigResolverImplTest {

    private final UserAiConfigService userAiConfigService = mock(UserAiConfigService.class);
    private final AiCredentialCrypto crypto = mock(AiCredentialCrypto.class);
    private final UserAiConfigResolverImpl resolver = new UserAiConfigResolverImpl(userAiConfigService, crypto);

    @Test
    void shouldResolveExactBusinessConfigBeforeDefault() {
        UserAiConfig resumeConfig = config("resume", "enc-key", 1);
        when(userAiConfigService.findEnabledConfig(10L, "resume")).thenReturn(resumeConfig);
        when(crypto.decrypt("enc-key")).thenReturn("sk-real");

        var resolved = resolver.resolve(10L, AiEngineConstants.BUSINESS_TYPE_RESUME, false);

        assertEquals("resume", resolved.getConfigType());
        assertEquals("sk-real", resolved.getApiKey());
        assertTrue(resolved.isSupportsMultimodal());
    }

    @Test
    void shouldFallbackToDefaultWhenExactConfigMissing() {
        UserAiConfig defaultConfig = config("default", "enc-default", 0);
        when(userAiConfigService.findEnabledConfig(10L, "resume")).thenReturn(null);
        when(userAiConfigService.findEnabledConfig(10L, "default")).thenReturn(defaultConfig);
        when(crypto.decrypt("enc-default")).thenReturn("sk-default");

        var resolved = resolver.resolve(10L, AiEngineConstants.BUSINESS_TYPE_RESUME, false);

        assertEquals("default", resolved.getConfigType());
        assertEquals("sk-default", resolved.getApiKey());
    }

    @Test
    void shouldReturnNullWhenFallbackToPlatformIsTrue() {
        assertNull(resolver.resolve(10L, AiEngineConstants.BUSINESS_TYPE_INTERVIEW, true));
    }

    private UserAiConfig config(String type, String apiKey, int multimodal) {
        UserAiConfig config = new UserAiConfig();
        config.setConfigType(type);
        config.setBaseUrl("https://api.example.com/v1");
        config.setModel("test-model");
        config.setApiKey(apiKey);
        config.setSupportsMultimodal(multimodal);
        return config;
    }
}
