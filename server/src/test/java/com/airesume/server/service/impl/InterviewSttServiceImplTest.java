package com.airesume.server.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.airesume.server.common.exception.BusinessException;
import com.airesume.server.dto.user.ResolvedSttConfig;
import com.airesume.server.service.SysSttConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClient;

/**
 * InterviewSttServiceImpl 单元测试。
 * <p>
 * 云端 STT 运行时改为读取管理端在数据库中维护的系统级配置（SysSttConfigService），
 * 这里只验证可用性判定与入参校验，不发起真实网络请求。
 */
class InterviewSttServiceImplTest {

    private SysSttConfigService sysSttConfigService;
    private RestClient.Builder restClientBuilder;
    private ObjectMapper objectMapper;
    private InterviewSttServiceImpl service;

    @BeforeEach
    void setUp() {
        sysSttConfigService = mock(SysSttConfigService.class);
        restClientBuilder = RestClient.builder();
        objectMapper = new ObjectMapper();
        service = new InterviewSttServiceImpl(sysSttConfigService, restClientBuilder, objectMapper);
    }

    @Test
    void transcribeThrowsWhenCloudSttDisabled() {
        when(sysSttConfigService.resolveEnabledConfig()).thenReturn(null);
        MockMultipartFile audio = new MockMultipartFile(
                "audio", "speech.webm", "audio/webm", new byte[] {1, 2, 3});
        assertThrows(BusinessException.class, () -> service.transcribe(audio, "zh-CN"));
    }

    @Test
    void transcribeRejectsEmptyAudio() {
        when(sysSttConfigService.resolveEnabledConfig()).thenReturn(
                ResolvedSttConfig.builder()
                        .baseUrl("https://api.example.com/v1")
                        .apiKey("k")
                        .model("m")
                        .endpointPath("/audio/transcriptions")
                        .build());
        MockMultipartFile audio = new MockMultipartFile(
                "audio", "empty.webm", "audio/webm", new byte[0]);
        assertThrows(BusinessException.class, () -> service.transcribe(audio, "zh-CN"));
    }

    @Test
    void isCloudSttAvailableDelegatesToSysConfig() {
        when(sysSttConfigService.isCloudSttAvailable()).thenReturn(true);
        assertTrue(service.isCloudSttAvailable());
    }
}
