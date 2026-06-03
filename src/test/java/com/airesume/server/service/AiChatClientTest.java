package com.airesume.server.service;

import com.airesume.server.common.constants.AiEngineConstants;
import com.airesume.server.common.constants.UserAiConstants;
import com.airesume.server.dto.ai.ResolvedAiConfig;
import com.airesume.server.entity.SysAiEngineConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatClientTest {

    @Mock private SysAiEngineConfigService sysAiEngineConfigService;
    @Mock private AiCircuitBreaker aiCircuitBreaker;
    @Mock private AiCredentialCrypto aiCredentialCrypto;
    @Mock private WebClient.Builder webClientBuilder;
    @Mock private UserAiConfigResolver userAiConfigResolver;

    @Test
    void shouldResolveUserCustomConfigWhenUserContextProvided() throws Exception {
        Long userId = 123L;
        AiChatClient client = buildClient();
        when(userAiConfigResolver.resolve(userId, AiEngineConstants.BUSINESS_TYPE_INTERVIEW, false))
                .thenReturn(ResolvedAiConfig.builder()
                        .provider("openai")
                        .baseUrl("https://custom.example.com/v1")
                        .apiKey("custom-key")
                        .model("custom-model")
                        .configType(UserAiConstants.CONFIG_TYPE_INTERVIEW)
                        .build());

        Object runtimeConfig = resolveRuntimeConfig(client, userId, false);

        assertEquals("custom-model", readRecordValue(runtimeConfig, "model"));
        assertEquals("https://custom.example.com/v1", readRecordValue(runtimeConfig, "baseUrl"));
        assertEquals(UserAiConstants.BILLING_SOURCE_USER_CUSTOM, readRecordValue(runtimeConfig, "source"));
        assertEquals(UserAiConstants.CONFIG_TYPE_INTERVIEW, readRecordValue(runtimeConfig, "configType"));
    }

    @Test
    void shouldResolvePlatformConfigWhenUserIdMissing() throws Exception {
        AiChatClient client = buildClient();
        SysAiEngineConfig platformConfig = new SysAiEngineConfig();
        platformConfig.setEngineCode("mimo-prod");
        platformConfig.setProviderType("mimo");
        platformConfig.setModelName("platform-model");
        platformConfig.setBaseUrl("https://platform.example.com/v1");
        platformConfig.setApiKey("encrypted-platform-key");
        when(sysAiEngineConfigService.getActiveByBusinessType(AiEngineConstants.BUSINESS_TYPE_INTERVIEW))
                .thenReturn(platformConfig);
        when(aiCredentialCrypto.decrypt("encrypted-platform-key")).thenReturn("platform-key");

        Object runtimeConfig = resolveRuntimeConfig(client, null, false);

        assertEquals("platform-model", readRecordValue(runtimeConfig, "model"));
        assertEquals("https://platform.example.com/v1", readRecordValue(runtimeConfig, "baseUrl"));
        assertEquals("db-active:mimo-prod", readRecordValue(runtimeConfig, "source"));
        assertEquals("platform", readRecordValue(runtimeConfig, "configType"));
    }

    @Test
    void shouldUsePlatformPrefixForPlatformRuntimeLogTag() throws Exception {
        AiChatClient client = buildClient();
        SysAiEngineConfig platformConfig = new SysAiEngineConfig();
        platformConfig.setEngineCode("mimo-prod");
        platformConfig.setProviderType("mimo");
        platformConfig.setModelName("platform-model");
        platformConfig.setBaseUrl("https://platform.example.com/v1");
        platformConfig.setApiKey("encrypted-platform-key");
        when(sysAiEngineConfigService.getActiveByBusinessType(AiEngineConstants.BUSINESS_TYPE_INTERVIEW))
                .thenReturn(platformConfig);
        when(aiCredentialCrypto.decrypt("encrypted-platform-key")).thenReturn("platform-key");

        Object runtimeConfig = resolveRuntimeConfig(client, null, false);
        Method method = AiChatClient.class.getDeclaredMethod("runtimeLogTag", runtimeConfig.getClass());
        method.setAccessible(true);

        assertEquals("PLATFORM/MIMO", method.invoke(client, runtimeConfig));
    }

    private AiChatClient buildClient() {
        return new AiChatClient(
                "mimo",
                "https://platform.example.com/v1",
                "platform-model",
                "none",
                sysAiEngineConfigService,
                new ObjectMapper(),
                aiCircuitBreaker,
                aiCredentialCrypto,
                webClientBuilder,
                userAiConfigResolver);
    }

    private Object resolveRuntimeConfig(AiChatClient client, Long userId, boolean fallbackToPlatform) throws Exception {
        Method method = AiChatClient.class.getDeclaredMethod(
                "resolveRuntimeConfig", Long.class, boolean.class, boolean.class);
        method.setAccessible(true);
        return method.invoke(client, userId, fallbackToPlatform, false);
    }

    private Object readRecordValue(Object record, String accessor) throws Exception {
        Method method = record.getClass().getDeclaredMethod(accessor);
        method.setAccessible(true);
        return method.invoke(record);
    }
}
